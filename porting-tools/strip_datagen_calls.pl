#!/usr/bin/perl
# Removes Registrate builder calls that only feed datagen (".recipe(...)", ".blockstate(...)", ...), balancing
# parentheses and skipping string/char literals. Usage: strip_datagen_calls.pl <names,comma,separated> <files...>
use strict; use warnings;
my @names = split /,/, shift @ARGV;
my $nameRe = join '|', map { quotemeta } @names;
for my $file (@ARGV) {
    open my $in, '<:raw', $file or die $file; local $/; my $s = <$in>; close $in;
    my $out = ''; my $n = 0;
    while ($s =~ /\G(.*?)(\s*\.(?:$nameRe)\()/gcs) {
        $out .= $1;
        my $start = pos($s);
        my $depth = 1; my $i = $start; my $len = length $s;
        while ($i < $len && $depth > 0) {
            my $c = substr($s, $i, 1);
            if ($c eq '"' || $c eq "'") {
                my $q = $c; $i++;
                while ($i < $len) { my $d = substr($s, $i, 1); if ($d eq chr(92)) { $i += 2; next } $i++; last if $d eq $q; }
                next;
            }
            if ($c eq '/' && substr($s, $i, 2) eq '//') { my $e = index($s, "\n", $i); $i = $e < 0 ? $len : $e; next; }
            $depth++ if $c eq '(';
            $depth-- if $c eq ')';
            $i++;
        }
        die "unbalanced in $file at $start" if $depth;
        pos($s) = $i; $n++;
    }
    $out .= substr($s, pos($s) // 0);
    if ($n) { open my $o, '>:raw', $file or die; print $o $out; close $o; print "$file: $n\n"; }
}
