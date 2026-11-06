#!/usr/bin/perl
# Removes every method named <name> (with its preceding annotations) from the given files, balancing braces.
# Usage: remove_method.pl <name> <files...>
use strict; use warnings;
my $name = shift @ARGV;
my $body_re; $body_re = qr/\{(?:[^{}]++|(??{$body_re}))*\}/;
my $re = qr/\n[ \t]*(?:\@\w+(?:\([^)]*\))?\s*)*(?:public|protected|private)?\s*(?:static\s+)?(?:final\s+)?[\w<>\[\], .?]+\s+\Q$name\E\s*\([^;]*?\)\s*(?:throws [\w., ]+)?\s*$body_re[ \t]*/;
local $^I = ''; local $/;
while (<>) {
    s/$re//g;
    print;
}
