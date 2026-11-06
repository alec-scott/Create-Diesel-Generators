#!/usr/bin/perl
# Second batch of mechanical Minecraft 1.21.1 -> 26.1 renames. Usage: fix_mc_renames2.pl <files...>
use strict; use warnings;
local $^I = '';
local $/;
while (<>) {
    my $s = $_;
    my $orig = $s;

    # LightTexture -> LightCoordsUtil
    $s =~ s/^import net\.minecraft\.client\.renderer\.LightTexture;/import net.minecraft.util.LightCoordsUtil;/mg;
    $s =~ s/\bLightTexture\.FULL_BLOCK\b/LightCoordsUtil.pack(15, 0)/g;
    $s =~ s/\bLightTexture\.(FULL_BRIGHT|FULL_SKY|pack|block|sky)\b/LightCoordsUtil.$1/g;

    # Shift/control state moved from Screen statics to the Minecraft instance
    $s =~ s/\b(?:Screen|AbstractContainerScreen|AbstractSimiScreen)\.has(Shift|Control|Alt)Down\(\)/Minecraft.getInstance().has$1Down()/g;
    $s =~ s/(?<![.\w])has(Shift|Control|Alt)Down\(\)/Minecraft.getInstance().has$1Down()/g;

    # Moved classes
    $s =~ s/\bnet\.minecraft\.client\.ParticleStatus\b/net.minecraft.server.level.ParticleStatus/g;
    $s =~ s/\bnet\.minecraft\.world\.level\.BlockAndTintGetter\b/net.minecraft.client.renderer.block.BlockAndTintGetter/g;

    # RenderType static factories moved to RenderTypes
    $s =~ s/\bRenderType\.(entity\w*|itemEntityTranslucentCull|solid|cutout|cutoutMipped|translucent\w*|lines\w*|text\w*|eyes|energySwirl|leash|waterMask|armor\w*|glint\w*|beaconBeam|dragonExplosionAlpha|debug\w*|lightning|tripwire|endPortal|endGateway|breezeWind|crumbling|outline|gui\w*|create)\(/RenderTypes.$1(/g;

    if ($s ne $orig) {
        my %need = (
            'net.minecraft.client.Minecraft' => qr/\bMinecraft\.getInstance\(\)\.has\w+Down/,
            'net.minecraft.client.renderer.rendertype.RenderTypes' => qr/\bRenderTypes\./,
        );
        for my $imp (sort keys %need) {
            next unless $s =~ $need{$imp} && $s !~ /^import \Q$imp\E;/m;
            $s =~ s/^(package [^;]+;[ \t]*\r?\n)/$1\nimport $imp;\n/m;
        }
    }
    print $s;
}
