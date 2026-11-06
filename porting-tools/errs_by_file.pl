#!/usr/bin/perl
# Counts javac errors per source file. Usage: errs_by_file.pl <build-output.txt> [limit]
use strict; use warnings;
my ($file, $limit) = @ARGV; $limit //= 60;
open my $fh, '<', $file or die; my %c;
while (<$fh>) {
    next unless m{Create-Diesel-Generators[\\/](.+?)\.java:\d+: error};
    (my $f = $1) =~ tr{\\}{/};
    $f =~ s{^.*?com/jesz/createdieselgenerators/}{};
    $c{$f}++;
}
my $i = 0;
for (sort { $c{$b} <=> $c{$a} || $a cmp $b } keys %c) { printf "%4d %s\n", $c{$_}, $_; last if ++$i >= $limit; }
print scalar(keys %c), " files\n";
