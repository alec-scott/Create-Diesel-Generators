#!/usr/bin/perl
# Converts 1.20.1 Block method declarations to 26.1 signatures (Steam 'n' Rails). Bodies are adjusted where the
# change is mechanical (super calls, scheduleTick); anything else is left for javac to flag.
# Usage: fix_block_methods.pl <files...>
use strict; use warnings;
local $^I = '';
local $/;

my $body_re;
$body_re = qr/\{(?:[^{}"']++|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|(??{$body_re}))*\}/;
my $A = qr/(?:\@[\w.]+(?:\([^)]*\))?\s+)*/;   # annotations on a parameter
my $T = qr/(?:final\s+)?/;

sub pname { my ($p) = @_; $p =~ /(\w+)\s*$/; return $1; }

while (<>) {
    my $s = $_;
    my $orig = $s;
    my %imports;

    # use(BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult) -> useItemOn(ItemStack, ...)
    $s =~ s{((?:public|protected)\s+)(?:\@\w+\s+)?InteractionResult\s+use\s*\(\s*($A$T BlockState\s+\w+\s*,\s*$A$T Level\s+\w+\s*,\s*$A$T BlockPos\s+\w+\s*,\s*$A$T Player\s+\w+\s*,\s*$A$T InteractionHand\s+\w+\s*,\s*$A$T BlockHitResult\s+\w+)\s*\)(\s*)($body_re)}
           { my ($mod, $params, $ws, $body) = ($1, $2, $3, $4);
             $body =~ s/\bsuper\.use\(/super.useItemOn(stack, /g;
             $imports{'net.minecraft.world.item.ItemStack'} = 1;
             "${mod}InteractionResult useItemOn(ItemStack stack, $params)$ws$body" }gex;

    # onRemove whose body only does IBE.onRemove / super.onRemove: vanilla removes block entities itself now.
    $s =~ s{\R[ \t]*(?:\@Override\s+)?(?:\@SuppressWarnings\([^)]*\)\s+)?(?:public|protected)\s+void\s+onRemove\s*\([^)]*\)\s*\{\s*(?:IBE\.onRemove\([^;]*\);\s*|super\.onRemove\([^;]*\);\s*|if\s*\(state\.hasBlockEntity\(\)[^;]*;\s*)+\}}{}gx;

    # neighborChanged(..., Block, BlockPos fromPos, boolean) -> (..., Block, @Nullable Orientation, boolean)
    $s =~ s{(void\s+neighborChanged\s*\(\s*$A$T BlockState\s+\w+\s*,\s*$A$T Level\s+\w+\s*,\s*$A$T BlockPos\s+\w+\s*,\s*$A$T Block\s+\w+\s*,\s*)$A$T BlockPos\s+(\w+)(\s*,\s*$A$T boolean\s+\w+\s*\))(\s*)($body_re)}
           { my ($pre, $from, $post, $ws, $body) = ($1, $2, $3, $4, $5);
             $body =~ s/\bsuper\.neighborChanged\(([^,]+,[^,]+,[^,]+,[^,]+,)\s*\Q$from\E\s*,/super.neighborChanged($1 orientation,/g;
             $imports{'net.minecraft.world.level.redstone.Orientation'} = 1;
             $imports{'org.jetbrains.annotations.Nullable'} = 1;
             "$pre\@Nullable Orientation orientation$post$ws$body" }gex;

    # updateShape(BlockState s, Direction d, BlockState n, LevelAccessor l, BlockPos p, BlockPos np)
    #   -> updateShape(BlockState s, LevelReader l, ScheduledTickAccess ticks, BlockPos p, Direction d, BlockPos np, BlockState n, RandomSource random)
    $s =~ s{(BlockState\s+updateShape\s*\()\s*$A$T BlockState\s+(\w+)\s*,\s*$A$T Direction\s+(\w+)\s*,\s*$A$T BlockState\s+(\w+)\s*,\s*$A$T (?:LevelAccessor|LevelReader)\s+(\w+)\s*,\s*$A$T BlockPos\s+(\w+)\s*,\s*$A$T BlockPos\s+(\w+)\s*\)(\s*)($body_re)}
           { my ($pre, $st, $dir, $ns, $lvl, $pos, $npos, $ws, $body) = ($1, $2, $3, $4, $5, $6, $7, $8, $9);
             $body =~ s/\bsuper\.updateShape\(\s*\Q$st\E\s*,\s*\Q$dir\E\s*,\s*\Q$ns\E\s*,\s*\Q$lvl\E\s*,\s*\Q$pos\E\s*,\s*\Q$npos\E\s*\)/super.updateShape($st, $lvl, ticks, $pos, $dir, $npos, $ns, random)/g;
             $body =~ s/\b\Q$lvl\E\.scheduleTick\(/ticks.scheduleTick(/g;
             $body =~ s/\bupdateWater\(\s*\Q$lvl\E\s*,/updateWater($lvl, ticks,/g;
             $imports{'net.minecraft.world.level.LevelReader'} = 1;
             $imports{'net.minecraft.world.level.ScheduledTickAccess'} = 1;
             $imports{'net.minecraft.util.RandomSource'} = 1;
             "${pre}BlockState $st, LevelReader $lvl, ScheduledTickAccess ticks, BlockPos $pos, Direction $dir, BlockPos $npos, BlockState $ns, RandomSource random)$ws$body" }gex;

    # getAnalogOutputSignal(BlockState, Level, BlockPos) -> + Direction
    $s =~ s{(int\s+getAnalogOutputSignal\s*\(\s*$A$T BlockState\s+\w+\s*,\s*$A$T Level\s+\w+\s*,\s*$A$T BlockPos\s+\w+)\s*\)}
           {$1, Direction direction)}gx and $imports{'net.minecraft.core.Direction'} = 1;

    # getCloneItemStack(BlockGetter, BlockPos, BlockState) -> (LevelReader, BlockPos, BlockState, boolean)
    $s =~ s{(ItemStack\s+getCloneItemStack\s*\(\s*)$A$T BlockGetter(\s+\w+\s*,\s*$A$T BlockPos\s+\w+\s*,\s*$A$T BlockState\s+\w+)\s*\)}
           {${1}LevelReader$2, boolean includeData)}gx and $imports{'net.minecraft.world.level.LevelReader'} = 1;
    $s =~ s/\bsuper\.getCloneItemStack\(([^;()]+)\)(?=;)/super.getCloneItemStack($1, includeData)/g;

    # isPathfindable(BlockState, BlockGetter, BlockPos, PathComputationType) -> (BlockState, PathComputationType)
    $s =~ s{(boolean\s+isPathfindable\s*\(\s*$A$T BlockState\s+\w+\s*,)\s*$A$T BlockGetter\s+\w+\s*,\s*$A$T BlockPos\s+\w+\s*,(\s*$A$T PathComputationType\s+\w+\s*\))}
           {$1$2}gx;

    if ($s ne $orig) {
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
