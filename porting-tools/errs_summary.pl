#!/usr/bin/perl
# One line per javac error: file:line message [symbol]. Usage: errs_summary.pl <build-output.txt>
my @l = <>; chomp @l;
for my $i (0..$#l) { next unless $l[$i] =~ /^(\S.*\.java):(\d+): error: (.*)$/; my ($f,$n,$m) = ($1,$2,$3);
  $f =~ s{.*com.jesz.createdieselgenerators.}{}; my $s = ""; for my $j ($i+1..$i+5) { last if $j > $#l; last if $l[$j] =~ /\.java:\d+: error/; if ($l[$j] =~ /symbol:\s+(.*)/) { $s = " [$1]"; last } }
  print "$f:$n  $m$s\n"; }
