#!/usr/bin/perl
# Mechanical Minecraft 1.21.1 -> 26.1 renames that are safe as plain text rewrites.
# Usage: fix_mc_renames.pl <files...>
use strict; use warnings;
local $^I = '';
local $/;
while (<>) {
    my $s = $_;
    my $orig = $s;

    # ItemInteractionResult was folded into InteractionResult (1.21.2).
    $s =~ s/\bItemInteractionResult\.sidedSuccess\([^()]*\)/InteractionResult.SUCCESS/g;
    $s =~ s/\bInteractionResult\.sidedSuccess\([^()]*\)/InteractionResult.SUCCESS/g;
    $s =~ s/\bItemInteractionResult\.PASS_TO_DEFAULT_BLOCK_INTERACTION\b/InteractionResult.TRY_WITH_EMPTY_HAND/g;
    $s =~ s/\bItemInteractionResult\.SKIP_DEFAULT_BLOCK_INTERACTION\b/InteractionResult.PASS/g;
    $s =~ s/\bItemInteractionResult\.(SUCCESS|CONSUME|FAIL)\b/InteractionResult.$1/g;
    $s =~ s/\bInteractionResult\.CONSUME_PARTIAL\b/InteractionResult.CONSUME/g;
    $s =~ s/^import net\.minecraft\.world\.ItemInteractionResult;\r?\n//mg;
    $s =~ s/\bItemInteractionResult\b/InteractionResult/g;

    # DirectionProperty became EnumProperty<Direction> (1.21.2).
    $s =~ s/\bDirectionProperty\.create\(\s*("[^"]*")\s*\)/EnumProperty.create($1, Direction.class)/g;
    $s =~ s/\bDirectionProperty\.create\(\s*("[^"]*")\s*,/EnumProperty.create($1, Direction.class,/g;
    $s =~ s/^import net\.minecraft\.world\.level\.block\.state\.properties\.DirectionProperty;/import net.minecraft.world.level.block.state.properties.EnumProperty;/mg;
    $s =~ s/\bDirectionProperty\b/EnumProperty<Direction>/g;

    # FriendlyByteBuf
    $s =~ s/\.readResourceLocation\(\)/.readIdentifier()/g;
    $s =~ s/\.writeResourceLocation\(/.writeIdentifier(/g;

    if ($s ne $orig) {
        # Make sure imports used by the rewrites exist (duplicates are harmless to javac only if absent, so check).
        for my $imp ('net.minecraft.world.InteractionResult', 'net.minecraft.core.Direction', 'net.minecraft.world.level.block.state.properties.EnumProperty') {
            my ($simple) = $imp =~ /([^.]+)$/;
            next unless $s =~ /\b\Q$simple\E\b/;
            next if $s =~ /^import \Q$imp\E;/m;
            next if $s =~ /^package net\.minecraft\./m;
            $s =~ s/^(package [^;]+;[ \t]*\r?\n)/$1\nimport $imp;\n/m;
        }
    }
    print $s;
}
