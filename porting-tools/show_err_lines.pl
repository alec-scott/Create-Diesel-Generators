#!/usr/bin/perl
# Prints the source line (and the next one) for every javac error whose message matches a pattern.
# Usage: show_err_lines.pl <build-output.txt> <message-regex> [file-filter-regex]
use strict; use warnings;
my ($log, $pat, $filter) = @ARGV;
open my $fh, '<', $log or die; my %seen;
while (<$fh>) {
    next unless /^(\S.*\.java):(\d+): error: (.*)$/;
    my ($f, $n, $m) = ($1, $2, $3);
    next unless $m =~ /$pat/;
    next if defined $filter && $f !~ /$filter/;
    next if $seen{"$f:$n"}++;
    open my $s, '<', $f or next; my @l = <$s>; close $s;
    (my $short = $f) =~ s{.*[\\/]Create-Diesel-Generators[\\/]}{};
    my $a = $l[$n - 1] // ''; my $b = $l[$n] // '';
    s/^\s+// for $a, $b; chomp $a; chomp $b;
    print "$short:$n: $a | $b\n";
}
