#!/usr/bin/perl
# Maps upstream Create / Catnip / Ponder / Flywheel imports to Create Fly classes.
# Usage: map_create_imports.pl <create-fly-src-dir> <extra-src-dir>... -- <java files...>
# Prints "old<TAB>new<TAB>how" per import; "new" is empty when unresolved.
use strict;
use warnings;
use File::Find;

my @srcDirs;
while (@ARGV && $ARGV[0] ne '--') { push @srcDirs, shift @ARGV; }
shift @ARGV;

# Index every class in the given source trees: fqcn => 1, simple name => [fqcns]
my (%fq, %bySimple);
for my $dir (@srcDirs) {
    find(sub {
        return unless /\.java$/;
        my $p = $File::Find::name;
        $p =~ s{\\}{/}g;
        return unless $p =~ m{/java/(.+)\.java$};
        (my $c = $1) =~ s{/}{.}g;
        $fq{$c} = 1;
        my ($s) = $c =~ /([^.]+)$/;
        push @{ $bySimple{$s} }, $c;
    }, $dir);
}

my %imports;
for my $file (@ARGV) {
    open my $fh, '<', $file or next;
    while (<$fh>) {
        $imports{$1} = 1 if /^import\s+(?:static\s+)?((?:com\.simibubi\.create|net\.createmod\.(?:catnip|ponder)|dev\.engine_room\.flywheel)\.[\w.]+?)(?:\.\*)?;/;
    }
}

# Candidate prefixes, in order of preference.
sub candidates {
    my ($old) = @_;
    my @c;
    if ($old =~ /^com\.simibubi\.create\.(.+)$/) {
        push @c, "com.zurrtum.create.$1", "com.zurrtum.create.client.$1";
    } elsif ($old =~ /^net\.createmod\.catnip\.(.+)$/) {
        push @c, "com.zurrtum.create.catnip.$1", "com.zurrtum.create.client.catnip.$1";
    } elsif ($old =~ /^net\.createmod\.ponder\.(.+)$/) {
        push @c, "com.zurrtum.create.client.ponder.$1", "com.zurrtum.create.ponder.$1";
    } elsif ($old =~ /^dev\.engine_room\.flywheel\.(.+)$/) {
        push @c, "com.zurrtum.create.client.flywheel.$1", "com.zurrtum.create.flywheel.$1";
    }
    return @c;
}

for my $old (sort keys %imports) {
    my ($new, $how) = ('', 'unresolved');
    # Nested classes: try progressively shorter outer-class paths.
    my @parts = split /\./, $old;
    # Exact (or nested-class) path matches first, at every nesting depth...
    PATH: for my $cut (0 .. 2) {
        last if $cut >= @parts - 2;
        my $outer = join('.', @parts[0 .. $#parts - $cut]);
        my $suffix = $cut ? '.' . join('.', @parts[$#parts - $cut + 1 .. $#parts]) : '';
        for my $cand (candidates($outer)) {
            if ($fq{$cand}) { ($new, $how) = ($cand . $suffix, 'path'); last PATH; }
        }
    }
    # ...then a unique simple-name match anywhere (class moved to another package).
    OUTER: for my $cut (0 .. 2) {
        last if $new ne '' || $cut >= @parts - 2;
        my $outer = join('.', @parts[0 .. $#parts - $cut]);
        my $suffix = $cut ? '.' . join('.', @parts[$#parts - $cut + 1 .. $#parts]) : '';
        my ($simple) = $outer =~ /([^.]+)$/;
        my $hits = $bySimple{$simple} || [];
        my @create = grep { /^com\.zurrtum\.create\./ || /^rbasamoyai\./ } @$hits;
        if (@create == 1) { ($new, $how) = ($create[0] . $suffix, 'moved'); last OUTER; }
        if (@create > 1) { ($new, $how) = ('', 'ambiguous: ' . join(' ', @create)); last OUTER; }
    }
    print "$old\t$new\t$how\n";
}
