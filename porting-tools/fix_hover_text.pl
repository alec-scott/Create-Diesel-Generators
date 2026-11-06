#!/usr/bin/perl
# 1.21.1 appendHoverText(ItemStack, TooltipContext, List<Component>, TooltipFlag)
#   -> 26.1 appendHoverText(ItemStack, TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)
# Rewrites list.add( to list.accept( in the body. Usage: fix_hover_text.pl <files...>
use strict; use warnings;
local $^I = ''; local $/;
my $body_re; $body_re = qr/\{(?:[^{}]++|(??{$body_re}))*\}/;
while (<>) {
    my $s = $_; my $orig = $s;
    $s =~ s{(void\s+appendHoverText\s*\(\s*(?:\@\w+\s+)?ItemStack\s+(\w+)\s*,\s*(?:Item\.)?TooltipContext\s+(\w+)\s*,\s*)(?:\@\w+\s+)?List<(?:\?\s+super\s+)?Component>\s+(\w+)\s*,\s*(?:\@\w+\s+)?TooltipFlag\s+(\w+)\s*\)(\s*)($body_re)}
           { my ($pre,$st,$ctx,$list,$flag,$ws,$body)=($1,$2,$3,$4,$5,$6,$7);
             $body =~ s/\b\Q$list\E\.add\(/$list.accept(/g;
             $body =~ s/super\.appendHoverText\(\s*\Q$st\E\s*,\s*\Q$ctx\E\s*,\s*\Q$list\E\s*,\s*\Q$flag\E\s*\)/super.appendHoverText($st, $ctx, display, $list, $flag)/g;
             "${pre}TooltipDisplay display, java.util.function.Consumer<Component> $list, TooltipFlag $flag)$ws$body" }gex;
    if ($s ne $orig && $s !~ /import net\.minecraft\.world\.item\.component\.TooltipDisplay;/) {
        $s =~ s/^(package [^;]+;[ \t]*\r?\n)/$1\nimport net.minecraft.world.item.component.TooltipDisplay;\n/m;
    }
    print $s;
}
