#!/usr/bin/perl
# Adds imports for well-known 26.1 / Create Fly classes that a file uses but does not import (after rewrites).
# Usage: add_imports.pl <files...>
use strict; use warnings;
my %known = (
    InteractionResult => 'net.minecraft.world.InteractionResult',
    ItemStack => 'net.minecraft.world.item.ItemStack',
    ServerLevel => 'net.minecraft.server.level.ServerLevel',
    ValueInput => 'net.minecraft.world.level.storage.ValueInput',
    ValueOutput => 'net.minecraft.world.level.storage.ValueOutput',
    FluidInventory => 'com.zurrtum.create.infrastructure.fluids.FluidInventory',
    FluidStack => 'com.zurrtum.create.infrastructure.fluids.FluidStack',
    FluidTank => 'com.zurrtum.create.foundation.fluid.FluidTank',
    Container => 'net.minecraft.world.Container',
    SimpleContainer => 'net.minecraft.world.SimpleContainer',
    Identifier => 'net.minecraft.resources.Identifier',
    BlockPos => 'net.minecraft.core.BlockPos',
    Direction => 'net.minecraft.core.Direction',
    EquipmentSlot => 'net.minecraft.world.entity.EquipmentSlot',
    ItemUseAnimation => 'net.minecraft.world.item.ItemUseAnimation',
    TooltipDisplay => 'net.minecraft.world.item.component.TooltipDisplay',
    CDGValueIO => 'com.jesz.createdieselgenerators.port.minecraft.CDGValueIO',
    LevelReader => 'net.minecraft.world.level.LevelReader',
    ScheduledTickAccess => 'net.minecraft.world.level.ScheduledTickAccess',
    RandomSource => 'net.minecraft.util.RandomSource',
    Orientation => 'net.minecraft.world.level.redstone.Orientation',
    BlockEntityBehaviour => 'com.zurrtum.create.api.behaviour.BlockEntityBehaviour',
    SidedFluidInventory => 'com.zurrtum.create.infrastructure.fluids.SidedFluidInventory',
    FluidHelper => 'com.zurrtum.create.foundation.fluid.FluidHelper',
    CDGFluidUnits => 'com.jesz.createdieselgenerators.port.CDGFluidUnits',
);
local $^I = ''; local $/;
while (<>) {
    my $s = $_;
    my ($pkg) = $s =~ /^package\s+([\w.]+);/m;
    (my $body = $s) =~ s/^import[^\n]*\n//mg;
    $body =~ s{//[^\n]*}{}g; $body =~ s{/\*.*?\*/}{}gs; $body =~ s/"[^"\n]*"//g;
    for my $simple (sort keys %known) {
        my $fq = $known{$simple};
        next unless $body =~ /(?<![\w.])\Q$simple\E\b/;
        next if $s =~ /^import\s+[\w.]+\.\Q$simple\E\s*;/m;
        next if $s =~ /\b(?:class|interface|record|enum)\s+\Q$simple\E\b/;
        (my $fpkg = $fq) =~ s/\.\w+$//;
        next if defined $pkg && $pkg eq $fpkg;
        $s =~ s/^(package [^;]+;[ \t]*\r?\n)/$1\nimport $fq;\n/m;
    }
    print $s;
}
