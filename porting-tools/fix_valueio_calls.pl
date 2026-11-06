#!/usr/bin/perl
# For every "ValueInput x" / "ValueOutput x" parameter in a file, rewrites the CompoundTag-style calls on x that 26.1's
# ValueInput/ValueOutput lack into CDGValueIO helpers (raw tags through ExtraCodecs.NBT).
# Usage: fix_valueio_calls.pl <files...>
use strict; use warnings;
local $^I = '';
local $/;
while (<>) {
    my $s = $_; my $orig = $s;
    my %in; my %out;
    $in{$1} = 1 while $s =~ /\bValueInput\s+(\w+)\s*[,)]/g;
    $out{$1} = 1 while $s =~ /\bValueOutput\s+(\w+)\s*[,)]/g;
    for my $v (keys %in) {
        $s =~ s/(?<![\w.])\Q$v\E\.contains\(/CDGValueIO.contains($v, /g;
        $s =~ s/(?<![\w.])\Q$v\E\.getList\(([^,()]+),\s*(?:Tag\.)?TAG_\w+\)/CDGValueIO.getList($v, $1)/g;
        $s =~ s/(?<![\w.])\Q$v\E\.getCompound\(([^()]+)\)/CDGValueIO.getCompound($v, $1)/g;
        $s =~ s/(?<![\w.])\Q$v\E\.getCompoundOrEmpty\(([^()]+)\)/CDGValueIO.getCompound($v, $1)/g;
    }
    for my $v (keys %out) {
        $s =~ s/(?<![\w.])\Q$v\E\.put\(/CDGValueIO.put($v, /g;
        $s =~ s/(?<![\w.])\Q$v\E\.remove\(/$v.discard(/g;
    }
    if ($s ne $orig && $s !~ /^import com\.mrh0\.createaddition\.port\.minecraft\.CDGValueIO;/m) {
        $s =~ s/^(package [^;]+;[ \t]*\r?\n)/$1\nimport com.jesz.createdieselgenerators.port.minecraft.CDGValueIO;\n/m;
    }
    print $s;
}
