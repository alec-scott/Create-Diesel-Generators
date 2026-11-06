#!/usr/bin/perl
# Converts CompoundTag-based save/load methods to 26.1's ValueOutput/ValueInput.
# Handles: Create SmartBlockEntity write/read(CompoundTag, Provider, boolean), writeSafe(CompoundTag, Provider),
# BlockEntity saveAdditional/loadAdditional(CompoundTag, Provider), Entity add/readAdditionalSaveData(CompoundTag).
# Rewrites primitive get/put on the tag variable inside those methods; anything else is left for javac to flag.
# Usage: fix_valueio.pl <files...>
use strict; use warnings;

my %getOr = (
    getInt => '0', getLong => '0L', getFloat => '0f', getDouble => '0d', getBoolean => 'false',
    getString => '""', getShort => '(short) 0', getByte => '(byte) 0',
);

my $body_re;
$body_re = qr/\{(?:[^{}]++|(??{$body_re}))*\}/;

sub convert_body {
    my ($body, $var, $prov, $isRead) = @_;
    if ($isRead) {
        for my $g (keys %getOr) {
            $body =~ s/\b\Q$var\E\.$g\(("[^"]*"|[A-Z_][A-Za-z0-9_.]*)\)/$var.${g}Or($1, $getOr{$g})/g;
        }
    }
    if (defined $prov) {
        # super calls with the provider argument
        $body =~ s/super\.(read|write)\(\s*\Q$var\E\s*,\s*\Q$prov\E\s*,\s*(\w+)\s*\)/super.$1($var, $2)/g;
        $body =~ s/super\.(writeSafe|saveAdditional|loadAdditional)\(\s*\Q$var\E\s*,\s*\Q$prov\E\s*\)/super.$1($var)/g;
    }
    return $body;
}

local $^I = '';
local $/;
while (<>) {
    my $s = $_;
    my $orig = $s;
    # Smart BE read/write(CompoundTag x, HolderLookup.Provider y, boolean z)
    $s =~ s{((?:public|protected)\s+void\s+(read|write)\s*\(\s*)CompoundTag\s+(\w+)\s*,\s*(?:HolderLookup\.)?Provider\s+(\w+)\s*,\s*(boolean\s+\w+)\s*\)(\s*)($body_re)}
           { my ($pre,$m,$v,$p,$b,$ws,$body)=($1,$2,$3,$4,$5,$6,$7);
             $pre . ($m eq 'read' ? 'ValueInput' : 'ValueOutput') . " $v, $b)$ws" . convert_body($body, $v, $p, $m eq 'read') }ge;
    # writeSafe / saveAdditional / loadAdditional (CompoundTag x, HolderLookup.Provider y)
    $s =~ s{((?:public|protected)\s+void\s+(writeSafe|saveAdditional|loadAdditional)\s*\(\s*)CompoundTag\s+(\w+)\s*,\s*(?:HolderLookup\.)?Provider\s+(\w+)\s*\)(\s*)($body_re)}
           { my ($pre,$m,$v,$p,$ws,$body)=($1,$2,$3,$4,$5,$6);
             $pre . ($m eq 'loadAdditional' ? 'ValueInput' : 'ValueOutput') . " $v)$ws" . convert_body($body, $v, $p, $m eq 'loadAdditional') }ge;
    # Entity add/readAdditionalSaveData(CompoundTag x)
    $s =~ s{((?:public|protected)\s+void\s+(addAdditionalSaveData|readAdditionalSaveData)\s*\(\s*)CompoundTag\s+(\w+)\s*\)(\s*)($body_re)}
           { my ($pre,$m,$v,$ws,$body)=($1,$2,$3,$4,$5);
             $pre . ($m eq 'readAdditionalSaveData' ? 'ValueInput' : 'ValueOutput') . " $v)$ws" . convert_body($body, $v, undef, $m eq 'readAdditionalSaveData') }ge;
    if ($s ne $orig) {
        for my $imp ('net.minecraft.world.level.storage.ValueInput', 'net.minecraft.world.level.storage.ValueOutput') {
            my ($simple) = $imp =~ /([^.]+)$/;
            next unless $s =~ /\b$simple\b/ && $s !~ /^import \Q$imp\E;/m;
            $s =~ s/^(package [^;]+;[ \t]*\r?\n)/$1\nimport $imp;\n/m;
        }
    }
    print $s;
}
