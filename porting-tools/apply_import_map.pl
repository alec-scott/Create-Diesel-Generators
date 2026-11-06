#!/usr/bin/perl
# Rewrites fully-qualified names using a "old<TAB>new" map. Usage: apply_import_map.pl <map.tsv> <files...>
use strict; use warnings;
my $map = shift @ARGV;
open my $m, '<', $map or die; my %map;
while (<$m>) { chomp; my ($old, $new) = split /\t/; $map{$old} = $new if defined $new && $new ne ''; }
# Longest names first so nested classes win over their outer class.
my @keys = sort { length($b) <=> length($a) } keys %map;
my $re = join '|', map { quotemeta } @keys;
local $^I = ''; local @ARGV = @ARGV;
while (<>) { s/(?<![\w.])($re)(?![\w])/$map{$1}/g; print; }
