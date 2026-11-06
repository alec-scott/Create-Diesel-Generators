#!/usr/bin/perl
# Static mixin target audit for the 26.1 port.
#
# For every mixin listed in each module's *.mixins.json, resolves the @Mixin target classes and checks against the
# compiled target bytecode (via javap) that:
#   - every method named in `method = "..."` is declared in the target class,
#   - every @Shadow member exists in the target or one of its supertypes,
#   - every @Accessor / @Invoker target exists.
# Descriptor and injection-point mismatches are not checked (those still need a runtime launch).
#
# Usage: check_mixin_targets.pl <repo root> <javap path> <classpath file>
use strict;
use warnings;

my ($root, $javap, $cpFile) = @ARGV;
open my $cpfh, '<', $cpFile or die "classpath: $!";
my $cp = do { local $/; <$cpfh> };
chomp $cp;

my %members;   # FQN (dotted, $ for nested) => { methods => {name=>1}, fields => {name=>1}, supers => [..], found => 0/1 }

sub load_classes {
    my @want = grep { !exists $members{$_} } @_;
    return unless @want;
    for my $c (@want) {
        $members{$c} = { methods => {}, fields => {}, supers => [], found => 0 };
    }
    # javap in batches to keep the command line short.
    while (my @batch = splice(@want, 0, 40)) {
        my $args = join(' ', map { "'$_'" } @batch);
        my $out = `"$javap" -p -s -cp "$cp" $args 2>&1`;
        my $cur;
        my $lastMethod;
        my $lastField;
        for my $line (split /\n/, $out) {
            if ($line =~ /^\s+descriptor: (\S+)/) {
                $members{$cur}{descs}{$lastMethod}{$1} = 1 if defined $cur && defined $lastMethod;
                $members{$cur}{fdesc}{$lastField} = $1 if defined $cur && defined $lastField;
                next;
            }
            if ($line =~ /^(?:public |protected |private |abstract |final |static |sealed |non-sealed )*(?:class|interface|enum|record|\@interface) (\S+?)(?:<.*?>)?(?: extends ([^{]*?))?(?: implements ([^{]*?))?(?: permits [^{]*?)? \{/) {
                $cur = $1;
                my @supers;
                for my $list ($2, $3) {
                    next unless defined $list;
                    # strip generics, then split on commas
                    my $s = $list;
                    1 while $s =~ s/<[^<>]*>//g;
                    push @supers, map { s/^\s+|\s+$//gr } split /,/, $s;
                }
                $members{$cur} ||= { methods => {}, fields => {}, supers => [] };
                $members{$cur}{found} = 1;
                $members{$cur}{supers} = \@supers;
                next;
            }
            next unless defined $cur;
            if ($line =~ /^\s+.*?\b([\w\$]+)\((.*)\)/) {
                my $name = $1;
                $name = '<init>' if $cur =~ /(?:^|[.\$])\Q$name\E$/;
                $name = '<clinit>' if $line =~ /^\s+static \{\};/;
                $members{$cur}{methods}{$name} = 1;
                $lastMethod = $name;
                $lastField = undef;
            } elsif ($line =~ /^\s+static \{\};/) {
                $lastMethod = '<clinit>';
                $lastField = undef;
            } elsif ($line =~ /^\s+.*?\b([\w\$]+);\s*$/) {
                $members{$cur}{fields}{$1} = 1;
                $lastField = $1;
                $lastMethod = undef;
            }
        }
    }
}

sub has_member {
    my ($class, $kind, $name, $inherited, $seen) = @_;
    $seen ||= {};
    return 0 if $seen->{$class}++;
    load_classes($class);
    my $m = $members{$class};
    return 0 unless $m && $m->{found};
    return 1 if $m->{$kind}{$name};
    return 0 unless $inherited;
    my @supers = @{ $m->{supers} };
    load_classes(@supers);
    for my $s (@supers) {
        return 1 if has_member($s, $kind, $name, 1, $seen);
    }
    return 0;
}

sub field_desc {
    my ($class, $name, $seen) = @_;
    $seen ||= {};
    return undef if $seen->{$class}++;
    load_classes($class);
    my $m = $members{$class};
    return undef unless $m && $m->{found};
    return $m->{fdesc}{$name} if exists $m->{fdesc}{$name};
    load_classes(@{ $m->{supers} });
    for my $s (@{ $m->{supers} }) { my $d = field_desc($s, $name, $seen); return $d if defined $d; }
    return undef;
}

# Descriptors of a method in a class (optionally searching supertypes).
sub method_descs {
    my ($class, $name, $inherited, $seen) = @_;
    $seen ||= {};
    return () if $seen->{$class}++;
    load_classes($class);
    my $m = $members{$class};
    return () unless $m && $m->{found};
    my @d = keys %{ $m->{descs}{$name} || {} };
    if ($inherited) {
        load_classes(@{ $m->{supers} });
        push @d, method_descs($_, $name, 1, $seen) for @{ $m->{supers} };
    }
    return @d;
}

my %PRIM = (boolean => 'Z', byte => 'B', char => 'C', short => 'S', int => 'I', long => 'J', float => 'F', double => 'D', void => 'V');

# Convert a Java source parameter type to a descriptor, or undef when it cannot be resolved (type variables etc.).
sub type_desc {
    my ($type, $imports, $pkg) = @_;
    $type =~ s/\@[\w.]+(?:\([^)]*\))?\s*//g;
    1 while $type =~ s/<[^<>]*>//g;
    $type =~ s/\s+//g;
    my $dims = 0;
    $dims++ while $type =~ s/\[\]$//;
    $dims++ if $type =~ s/\.\.\.$//;
    my $d;
    if (exists $PRIM{$type}) { $d = $PRIM{$type}; }
    elsif ($type =~ /^[A-Z]$/) { return undef; }    # generic type variable
    else {
        my %javaLang = map { $_ => 1 } qw(Object String Integer Boolean Float Double Long Short Byte Character Void Runnable Iterable Class Number Comparable);
        my $bin = $javaLang{$type} && !$imports->{$type} ? "java.lang.$type" : resolve_type($type, $imports, $pkg);
        ($d = "L$bin;") =~ s/\./\//g;
    }
    return ('[' x $dims) . $d;
}

# Split a parameter list at top-level commas and return descriptors (undef entries for unresolved types).
sub param_descs {
    my ($params, $imports, $pkg) = @_;
    my @out; my $depth = 0; my $cur = '';
    for my $ch (split //, $params) {
        $depth++ if $ch eq '<' || $ch eq '(';
        $depth-- if $ch eq '>' || $ch eq ')';
        if ($ch eq ',' && $depth == 0) { push @out, $cur; $cur = ''; next; }
        $cur .= $ch;
    }
    push @out, $cur if $cur =~ /\S/;
    my @descs;
    for my $p (@out) {
        $p =~ s/^\s+|\s+$//g;
        my @annotations;
        while ($p =~ s/^\@([\w.]+)(?:\s*\((?:[^()]|\([^()]*\))*\))?\s*//) { push @annotations, $1; }
        $p =~ s/^final\s+//;
        $p =~ s/\s+[\w\$]+$//;    # drop the parameter name
        push @descs, { desc => type_desc($p, $imports, $pkg), sugar => scalar(grep { /^(Local|Share|Cancellable)$/ } @annotations) };
    }
    return @descs;
}

# Resolve a source-level type name (Foo, Foo.Bar, or fully qualified) to a binary class name.
sub resolve_type {
    my ($name, $imports, $pkg) = @_;
    $name =~ s/\s+//g;
    my @parts = split /\./, $name;
    if ($name =~ /^[a-z]/) {
        # fully qualified: package parts are lowercase, class parts capitalised
        my @pkgp; my @cls;
        for my $p (@parts) { if (@cls || $p =~ /^[A-Z]/) { push @cls, $p } else { push @pkgp, $p } }
        return join('.', @pkgp) . '.' . join('$', @cls);
    }
    my $first = shift @parts;
    my $base = $imports->{$first} // "$pkg.$first";
    return join('$', $base, @parts);
}

my @problems;
my $checked = 0;
for my $module (split /,/, ($ENV{MIXIN_MODULES} // 'sable,simulated,offroad,aeronautics')) {
    my $resDir = "$root/$module/src/main/resources";
    opendir my $dh, $resDir or next;
    my @configs = grep { /\.mixins\.json$/ } readdir $dh;
    closedir $dh;
    for my $cfg (@configs) {
        open my $fh, '<', "$resDir/$cfg" or die;
        my $json = do { local $/; <$fh> };
        my ($pkg) = $json =~ /"package"\s*:\s*"([^"]+)"/;
        next unless $pkg;
        my @names;
        while ($json =~ /"(mixins|client|server)"\s*:\s*\[(.*?)\]/sg) {
            my $arr = $2;
            push @names, $1 while $arr =~ /"([^"]+)"/g;
        }
        for my $mixinName (@names) {
            (my $rel = "$pkg.$mixinName") =~ s/\./\//g;
            my $file = "$root/$module/src/main/java/$rel.java";
            unless (-f $file) {
                push @problems, "$cfg: $mixinName -> source not found (excluded or deleted?)";
                next;
            }
            open my $sfh, '<', $file or die;
            my $src = do { local $/; <$sfh> };
            $checked++;
            my ($srcPkg) = $src =~ /^package\s+([\w.]+);/m;
            my %imports;
            while ($src =~ /^import\s+([\w.]+)\.(\w+);/mg) {
                my ($p, $c) = ($1, $2);
                # nested imports like a.b.Outer.Inner -> a.b.Outer$Inner
                my $full = "$p.$c";
                my @segs = split /\./, $full;
                my @pk; my @cl;
                for my $s (@segs) { if (@cl || $s =~ /^[A-Z]/) { push @cl, $s } else { push @pk, $s } }
                $imports{$c} = join('.', @pk) . '.' . join('$', @cl);
            }

            my ($mixinAnn) = $src =~ /\@Mixin\s*\(((?:[^()]|\([^()]*\))*)\)/s;
            next unless defined $mixinAnn;
            my @targets;
            push @targets, resolve_type($1, \%imports, $srcPkg) while $mixinAnn =~ /([\w.]+)\.class/g;
            if ($mixinAnn =~ /targets\s*=\s*\{?([^}]*?)\}?\s*(?:,|$)/s) {
                my $t = $1;
                while ($t =~ /"([^"]+)"/g) { (my $c = $1) =~ s/\//./g; push @targets, $c; }
            }
            next unless @targets;
            load_classes(@targets);
            for my $t (@targets) {
                push @problems, "$module $mixinName: target class $t not found" unless $members{$t}{found};
            }
            my @found = grep { $members{$_}{found} } @targets;
            next unless @found;

            # method = "..." / method = {"...", "..."}
            while ($src =~ /\bmethod\s*=\s*(\{[^}]*\}|"[^"]*")/g) {
                my $spec = $1;
                while ($spec =~ /"([^"]+)"/g) {
                    my $m = $1;
                    $m =~ s/^L[^;]+;//;      # owner-qualified
                    $m =~ s/\(.*$//;          # descriptor
                    $m =~ s/\*$//;            # wildcard suffix
                    next if $m eq '' || $m eq '<init>' || $m eq '<clinit>';
                    my $ok = grep { has_member($_, 'methods', $m, 0) } @found;
                    unless ($ok) {
                        my $inh = grep { has_member($_, 'methods', $m, 1) } @found;
                        push @problems, "$module $mixinName: method \"$m\" " . ($inh ? 'only inherited (not declared) in ' : 'not found in ') . join('/', @found);
                    }
                }
            }

            # @At(target = "Lowner;name(desc)ret") / "Lowner;name:Ltype;" and every class named in the descriptor.
            while ($src =~ /\btarget\s*=\s*((?:"[^"]*"\s*\+?\s*)+)/g) {
                (my $t = $1) =~ s/"\s*\+\s*"//g;
                $t =~ s/^"|"$//g;
                next unless $t =~ /^L([\w\/\$]+);([\w\$<>]+)(.*)$/;
                my ($owner, $name, $rest) = ($1, $2, $3);
                (my $ownerBin = $owner) =~ s/\//./g;
                load_classes($ownerBin);
                if (!$members{$ownerBin}{found}) {
                    push @problems, "$module $mixinName: target owner $ownerBin not found ($t)";
                    next;
                }
                if ($name ne '<init>') {
                    my $kind = $rest =~ /^:/ ? 'fields' : 'methods';
                    push @problems, "$module $mixinName: target " . ($kind eq 'fields' ? 'field' : 'method') . " $ownerBin.$name not found"
                        unless has_member($ownerBin, $kind, $name, 1);
                }
                while ($rest =~ /L([\w\/\$]+);/g) {
                    (my $c = $1) =~ s/\//./g;
                    load_classes($c);
                    push @problems, "$module $mixinName: descriptor class $c not found ($name$rest)" unless $members{$c}{found};
                }
                if ($rest =~ /^\(/ && $name ne '<init>') {
                    my %d = map { $_ => 1 } method_descs($ownerBin, $name, 1);
                    push @problems, "$module $mixinName: target $ownerBin.$name$rest has no such overload (have: " . join(' ', sort keys %d) . ")"
                        if %d && !$d{$rest};
                }
            }

            # @Inject / @WrapMethod handler parameters vs the target method's parameters.
            while ($src =~ /\@(Inject|WrapMethod)\s*\(/g) {
                my $ann = $1;
                my $start = pos($src);
                my $depth = 1; my $i = $start;
                while ($i < length($src) && $depth > 0) {
                    my $ch = substr($src, $i, 1);
                    $depth++ if $ch eq '('; $depth-- if $ch eq ')';
                    $i++;
                }
                my $annText = substr($src, $start, $i - $start - 1);
                my $after = substr($src, $i);
                next unless $after =~ /^\s*(?:\@[\w.]+(?:\([^)]*\))?\s*)*(?:(?:public|private|protected|static|final)\s+)*[\w<>\[\],.?\s]+?\s+[\w\$]+\s*\(/s;
                my $pStart = $+[0];
                $depth = 1; my $j = $pStart;
                while ($j < length($after) && $depth > 0) {
                    my $ch = substr($after, $j, 1);
                    $depth++ if $ch eq '('; $depth-- if $ch eq ')';
                    $j++;
                }
                my $params = substr($after, $pStart, $j - $pStart - 1);
                my @handler = grep { !$_->{sugar} } param_descs($params, \%imports, $srcPkg);
                next unless $annText =~ /\bmethod\s*=\s*(\{[^}]*\}|"[^"]*")/;
                my $spec = $1;
                my @specs; push @specs, $1 while $spec =~ /"([^"]+)"/g;
                for my $s (@specs) {
                    (my $mname = $s) =~ s/^L[^;]+;//;
                    my $mdesc = $mname =~ s/(\(.*)$// ? $1 : undef;
                    next if $mname =~ /[*<]/;
                    my @descs = defined $mdesc ? ($mdesc) : map { method_descs($_, $mname, 0) } @found;
                    next unless @descs == 1;                         # skip ambiguous overloads
                    my ($targetParams) = $descs[0] =~ /^\((.*)\)/;
                    my @tp; push @tp, $1 while $targetParams =~ /\G(\[*(?:L[^;]+;|[ZBCSIJFD]))/g;
                    my $ciIndex;
                    for my $k (0 .. $#handler) {
                        my $d = $handler[$k]{desc} // '';
                        if ($d =~ /CallbackInfo(?:Returnable)?;$|\/Operation;$/) { $ciIndex = $k; last; }
                    }
                    next unless defined $ciIndex;
                    next if $ann eq 'Inject' && $ciIndex == 0;
                    my $ok = $ciIndex == @tp;
                    if ($ok) {
                        for my $k (0 .. $#tp) {
                            my $hd = $handler[$k]{desc};
                            next unless defined $hd;                 # unresolved generic, can't compare
                            if ($hd ne $tp[$k]) { $ok = 0; last; }
                        }
                    }
                    push @problems, "$module $mixinName: \@$ann on $mname: handler params (" . join('', map { $_->{desc} // '?' } @handler[0 .. $ciIndex - 1]) . ") != target ($targetParams)"
                        unless $ok;
                }
            }

            # @Shadow members
            while ($src =~ /\@Shadow\b(?:\s*\([^)]*\))?\s+((?:\@[\w.]+(?:\([^)]*\))?\s+)*)((?:(?:public|protected|private|static|final|abstract|native|synchronized)\s+)*)([^;{=]+?)\s*(;|\{|=)/sg) {
                my $decl = $3;
                my $kind; my $name;
                if ($decl =~ /([\w\$]+)\s*\(/) { $kind = 'methods'; $name = $1; }
                elsif ($decl =~ /([\w\$]+)\s*$/) { $kind = 'fields'; $name = $1; }
                else { next; }
                $name =~ s/^shadow\$//;
                my $ok = grep { has_member($_, $kind, $name, 1) } @found;
                push @problems, "$module $mixinName: \@Shadow " . ($kind eq 'methods' ? 'method' : 'field') . " \"$name\" not found in " . join('/', @found) unless $ok;
                if ($ok && $kind eq 'fields' && $decl =~ /^(.*?)\s+[\w\$]+\s*$/s) {
                    my $want = type_desc($1, \%imports, $srcPkg);
                    my ($have) = grep { defined } map { field_desc($_, $name) } @found;
                    push @problems, "$module $mixinName: \@Shadow field $name type $want != target $have"
                        if defined $want && defined $have && $want ne $have;
                }
                if ($ok && $kind eq 'methods' && $decl =~ /\(([^)]*)\)\s*(?:throws[\w\s,.]*)?$/s) {
                    my @p = param_descs($1, \%imports, $srcPkg);
                    next if grep { !defined $_->{desc} } @p;
                    my $want = '(' . join('', map { $_->{desc} } @p) . ')';
                    my @have = map { method_descs($_, $name, 1) } @found;
                    push @problems, "$module $mixinName: \@Shadow method $name$want has no matching overload (have: " . join(' ', @have) . ")"
                        if @have && !grep { index($_, $want) == 0 } @have;
                }
            }

            # @Accessor / @Invoker
            while ($src =~ /\@(Accessor|Invoker)\b(?:\s*\(\s*(?:value\s*=\s*)?"([^"]+)"[^)]*\))?(?:\s*\([^)]*\))?\s+(?:\@[\w.]+(?:\([^)]*\))?\s+)*(?:(?:public|static|abstract|default)\s+)*[\w<>\[\],.?\s]+?\s+([\w\$]+)\s*\(/sg) {
                my ($ann, $explicit, $methodName) = ($1, $2, $3);
                my $name = $explicit;
                unless (defined $name) {
                    ($name = $methodName) =~ s/^[\w]+\$//;
                    $name =~ s/^(get|set|is|call|invoke)(?=[A-Z])//;
                    $name = lcfirst $name;
                }
                my $kind = $ann eq 'Accessor' ? 'fields' : 'methods';
                my $ok = grep { has_member($_, $kind, $name, 0) } @found;
                push @problems, "$module $mixinName: \@$ann target \"$name\" not found in " . join('/', @found) unless $ok;
            }
        }
    }
}

print "checked $checked mixin classes\n";
print "$_\n" for @problems;
print scalar(@problems), " problem(s)\n";
