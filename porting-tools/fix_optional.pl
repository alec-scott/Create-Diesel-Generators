#!/usr/bin/perl
# Log-driven: on lines javac flags with Optional<...> conversions, switch to 26.1's *Or/OrEmpty getters.
use strict; use warnings;
my $log = shift; my %lines;
open my $fh, '<', $log or die;
while (<$fh>) {
  if (/^(\S.*\.java):(\d+): error: (?:incompatible types: Optional<|no suitable method found for max\(int,Optional|no suitable constructor found for PotionContents)/) { $lines{$1}{$2} = 1; }
}
my $K = qr/(?:"[^"]*"\s*\+\s*[\w.]+|"[^"]*"|[\w.]+(?:\([^()]*\))?)/;
for my $f (keys %lines) {
  open my $in, '<', $f or die "$f"; my @l = <$in>; close $in;
  for my $n (keys %{$lines{$f}}) {
    my $s = $l[$n-1];
    $s =~ s/(?<!CBCValueIO)\.getCompound\(($K)\)/.getCompoundOrEmpty($1)/g;
    $s =~ s/\.getInt\(($K)\)/.getIntOr($1, 0)/g;
    $s =~ s/\.getString\(($K)\)/.getStringOr($1, "")/g;
    $s =~ s/\.getBoolean\(($K)\)/.getBooleanOr($1, false)/g;
    $s =~ s/\.getDouble\(($K)\)/.getDoubleOr($1, 0)/g;
    $s =~ s/\.getFloat\(($K)\)/.getFloatOr($1, 0)/g;
    $s =~ s/\.getLong\(($K)\)/.getLongOr($1, 0)/g;
    $s =~ s/(?<!CBCValueIO)\.getIntArray\(($K)\)/.getIntArray($1).orElseGet(() -> new int[0])/g;
    $s =~ s/(BuiltInRegistries\.\w+|registry|REGISTRY)\.get\(/$1.getValue(/g;
    print "UNCHANGED $f:$n: $s" if $s eq $l[$n-1];
    $l[$n-1] = $s;
  }
  open my $out, '>', $f or die; print $out @l; close $out;
}
