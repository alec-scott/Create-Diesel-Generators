#!/usr/bin/perl
# Removes single-type imports whose simple name is no longer referenced in the file (after rewrites left dead
# NeoForge/Create imports behind). Static and wildcard imports are kept. Usage: prune_imports.pl <files...>
use strict; use warnings;
local $^I = ''; local $/;
while (<>) {
    my $s = $_;
    my @lines = split /\n/, $s, -1;
    my $body = join "\n", grep { !/^import\s/ } @lines;
    $body =~ s{//[^\n]*}{}g; $body =~ s{/\*.*?\*/}{}gs;
    my @out;
    for my $l (@lines) {
        if ($l =~ /^import\s+([\w.]+)\.(\w+)\s*;/ && $l !~ /^import\s+static/) {
            my $simple = $2;
            next unless $body =~ /\b\Q$simple\E\b/;
        }
        push @out, $l;
    }
    print join "\n", @out;
}
