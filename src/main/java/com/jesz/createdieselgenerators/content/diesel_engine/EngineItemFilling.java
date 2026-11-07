package com.jesz.createdieselgenerators.content.diesel_engine;

import com.jesz.createdieselgenerators.port.CDGFluidUnits;
import com.zurrtum.create.AllFluids;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.fluid.FluidHelper;
import com.zurrtum.create.infrastructure.fluids.FluidInventory;
import com.zurrtum.create.infrastructure.fluids.FluidItemInventory;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * PORT-NOTE(26.1): upstream's item filling for all three engines. Create Fly's {@code FluidHelper.tryEmptyItemIntoBE}
 * can't be used: it only inserts when the whole amount fits, and an engine tank is smaller than a bucket. As upstream, a
 * bucket fills an empty tank up to its capacity and the rest is lost, and fluid items give what fits.
 * <p>
 * One difference: upstream returned FAIL for a bucket on a tank that already holds fluid, and the client then goes on to
 * use the bucket, spilling it in front of the engine. Here that click does nothing.
 */
public final class EngineItemFilling {
    private EngineItemFilling() {
    }

    public static InteractionResult fill(Level level, Player player, InteractionHand hand, ItemStack stack, SmartBlockEntity be) {
        FluidInventory tank = FluidHelper.getFluidInventory(level, be.getBlockPos(), null, be, null);
        if (tank == null)
            return InteractionResult.TRY_WITH_EMPTY_HAND;

        boolean milk = stack.is(Items.MILK_BUCKET);
        if (milk || stack.getItem() instanceof BucketItem) {
            Fluid fluid = milk ? AllFluids.MILK : ((BucketItem) stack.getItem()).content;
            if (fluid == Fluids.EMPTY || !tank.getStack(0).isEmpty())
                return InteractionResult.SUCCESS;
            if (!level.isClientSide()) {
                tank.insert(new FluidStack(fluid, CDGFluidUnits.BUCKET));
                if (!player.isCreative())
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            return InteractionResult.SUCCESS;
        }

        // A single item, drained for real: getContainer() has to see the change
        try (FluidItemInventory itemTank = FluidHelper.getFluidInventory(stack.copyWithCount(1))) {
            if (itemTank == null)
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (level.isClientSide())
                return InteractionResult.SUCCESS;

            FluidStack contained = itemTank.countAny();
            int accepted = contained.isEmpty() ? 0 : tank.countSpace(contained);
            int drained = accepted > 0 ? itemTank.extract(contained, accepted) : 0;
            if (drained > 0) {
                tank.insert(contained.copyWithAmount(drained));
                if (!player.isCreative()) {
                    ItemStack container = itemTank.getContainer();
                    if (stack.getCount() == 1) {
                        player.setItemInHand(hand, container);
                    } else {
                        stack.shrink(1);
                        player.getInventory().placeItemBackInInventory(container);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
    }
}
