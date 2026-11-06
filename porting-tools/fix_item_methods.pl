#!/usr/bin/perl
# Converts 1.20.1 Item method declarations to 26.1 signatures (Steam 'n' Rails). Usage: fix_item_methods.pl <files...>
use strict; use warnings;
local $^I = '';
local $/;

my $body_re;
$body_re = qr/\{(?:[^{}"']++|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|(??{$body_re}))*\}/;
my $A = qr/(?:\@[\w.]+(?:\([^)]*\))?\s+)*/;

while (<>) {
    my $s = $_;
    my $orig = $s;
    my %imports;

    # appendHoverText(ItemStack, @Nullable Level, List<Component>, TooltipFlag)
    #   -> appendHoverText(ItemStack, Item.TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)
    $s =~ s{(void\s+appendHoverText\s*\(\s*$A ItemStack\s+(\w+)\s*,\s*)$A Level\s+(\w+)\s*,\s*$A List<(?:\?\s+super\s+)?Component>\s+(\w+)\s*,\s*$A TooltipFlag\s+(\w+)\s*\)(\s*)($body_re)}
           { my ($pre, $stack, $lvl, $list, $flag, $ws, $body) = ($1, $2, $3, $4, $5, $6, $7);
             $body =~ s/\b\Q$list\E\.add\(/$list.accept(/g;
             $body =~ s/\bsuper\.appendHoverText\(\s*\Q$stack\E\s*,\s*\Q$lvl\E\s*,\s*\Q$list\E\s*,\s*\Q$flag\E\s*\)/super.appendHoverText($stack, context, display, $list, $flag)/g;
             $imports{'net.minecraft.world.item.component.TooltipDisplay'} = 1;
             $imports{'java.util.function.Consumer'} = 1;
             "${pre}TooltipContext context, TooltipDisplay display, Consumer<Component> $list, TooltipFlag $flag)$ws$body" }gex;

    # getUseDuration(ItemStack) -> getUseDuration(ItemStack, LivingEntity)
    $s =~ s{(int\s+getUseDuration\s*\(\s*$A ItemStack\s+\w+)\s*\)}{$1, LivingEntity entity)}gx
        and $imports{'net.minecraft.world.entity.LivingEntity'} = 1;

    # onCraftedBy(ItemStack, Level, Player) -> onCraftedBy(ItemStack, Player)
    $s =~ s{(void\s+onCraftedBy\s*\(\s*$A ItemStack\s+\w+\s*,)\s*$A Level\s+\w+\s*,(\s*$A Player\s+\w+\s*\))}{$1$2}gx;

    # Vanishable is gone (the enchantable/vanishing tags decide now)
    $s =~ s/\s+implements\s+Vanishable\s*\{/ {/g;
    $s =~ s/,\s*Vanishable\b//g;
    $s =~ s/\bVanishable\s*,\s*//g;
    $s =~ s/^import net\.minecraft\.world\.item\.Vanishable;\r?\n//mg;

    if ($s ne $orig) {
        if ($s =~ /\bTooltipContext\b/ && $s !~ /^import [\w.]+\.TooltipContext;/m && $s !~ /^import net\.minecraft\.world\.item\.Item;/m) {
            $imports{'net.minecraft.world.item.Item'} = 1;
        }
        $s =~ s/(\W)TooltipContext context,/$1Item.TooltipContext context,/g;
        for my $imp (sort keys %imports) {
            my ($simple) = $imp =~ /([^.]+)$/;
            next unless $s =~ /\b\Q$simple\E\b/;
            next if $s =~ /^import \Q$imp\E;/m;
            next if $s =~ /^import [\w.]+\.\Q$simple\E;/m;
            $s =~ s/^(package [^;]+;[ \t]*\r?\n)/$1\nimport $imp;\n/m;
        }
    }
    print $s;
}
