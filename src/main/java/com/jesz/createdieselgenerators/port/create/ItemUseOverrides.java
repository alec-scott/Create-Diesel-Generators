package com.jesz.createdieselgenerators.port.create;

import com.zurrtum.create.AllItems;
import com.zurrtum.create.catnip.math.VecHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Upstream Create's {@code ItemUseOverrides}: right-clicking these blocks with any item but a wrench uses the block
 * (as if sneaking didn't stop it), for blocks whose interaction shouldn't depend on what's held.
 *
 * <p>PORT-NOTE(fabric): Create Fly dropped it; this runs on Fabric's {@link UseBlockCallback}. Since 1.20.5 a block's
 * "use" is split in two, so the item path runs first and falls back to the empty-hand path, like vanilla.</p>
 */
public final class ItemUseOverrides {
	private static final Set<Block> OVERRIDES = Collections.newSetFromMap(new IdentityHashMap<>());

	private ItemUseOverrides() {
	}

	public static void addBlock(Block block) {
		OVERRIDES.add(block);
	}

	public static void init() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (player.isSpectator())
				return InteractionResult.PASS;
			ItemStack stack = player.getItemInHand(hand);
			if (stack.is(AllItems.WRENCH))
				return InteractionResult.PASS;
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			if (!OVERRIDES.contains(state.getBlock()))
				return InteractionResult.PASS;

			BlockHitResult trace = new BlockHitResult(VecHelper.getCenterOf(pos), hit.getDirection(), pos, true);
			InteractionResult result = state.useItemOn(stack, level, player, hand, trace);
			if (result instanceof InteractionResult.TryEmptyHandInteraction)
				result = state.useWithoutItem(level, player, trace);
			return result.consumesAction() ? result : InteractionResult.PASS;
		});
	}
}
