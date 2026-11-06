package com.jesz.createdieselgenerators.content.entity_filter;

import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.AllItems;
import com.zurrtum.create.AllSoundEvents;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.content.logistics.filter.FilterItem;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.filtering.ServerFilteringBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * PORT-NOTE(26.1): a Create Fly {@link ServerFilteringBehaviour} (standard behaviour type, so Create Fly's client
 * FilteringBehaviour draws the slot and routes clicks here; the value box is registered client side). Only entity
 * filters, spawn eggs and other non-filter items are accepted, as upstream. The custom EntityFilteringRenderer overlay is
 * replaced by Create Fly's normal filter slot rendering.
 */
public class EntityFilteringBehaviour extends ServerFilteringBehaviour {
    public EntityFilteringBehaviour(SmartBlockEntity be) {
        super(be);
    }

    @Override
    public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
        Level level = getLevel();
        BlockPos pos = getPos();
        ItemStack itemInHand = player.getItemInHand(hand);
        ItemStack toApply = itemInHand.copy();

        if (toApply.is(AllItems.WRENCH))
            return;
        if (toApply.is(AllBlocks.MECHANICAL_ARM.asItem()))
            return;
        if (level.isClientSide())
            return;

        if (getFilter(side).getItem() instanceof EntityFilterItem) {
            Inventory inventory = player.getInventory();
            if (!player.isCreative() || inventory.count(getFilter(side), 1) == 0)
                inventory.placeItemBackInInventory(getFilter(side).copy());
        }

        if (toApply.getItem() instanceof EntityFilterItem)
            toApply.setCount(1);

        if (!setFilter(side, toApply)) {
            player.sendOverlayMessage(Component.translatable("create.logistics.filter.invalid_item"));
            AllSoundEvents.DENY.playOnServer(player.level(), player.blockPosition(), 1, 1);
            return;
        }

        if (!player.isCreative()) {
            if (toApply.getItem() instanceof EntityFilterItem) {
                if (itemInHand.getCount() == 1)
                    player.setItemInHand(hand, ItemStack.EMPTY);
                else
                    itemInHand.shrink(1);
            }
        }

        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, .25f, .1f);
    }

    @Override
    public boolean setFilter(ItemStack stack) {
        if (stack.getItem() instanceof FilterItem)
            return false;
        if (stack.getItem() instanceof EntityFilterItem || stack.isEmpty())
            return super.setFilter(stack);
        return false;
    }

    @Override
    public void destroy() {
        if (getFilter().getItem() instanceof EntityFilterItem) {
            Vec3 pos = VecHelper.getCenterOf(getPos());
            Level level = getLevel();
            level.addFreshEntity(new ItemEntity(level, pos.x, pos.y, pos.z, getFilter().copy()));
        }
        super.destroy();
    }
}
