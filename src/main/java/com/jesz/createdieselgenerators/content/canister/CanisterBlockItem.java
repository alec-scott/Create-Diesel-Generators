package com.jesz.createdieselgenerators.content.canister;

import net.minecraft.world.item.component.TooltipDisplay;

import com.jesz.createdieselgenerators.CDGConfig;
import com.jesz.createdieselgenerators.content.tools.FueledToolItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;


public class CanisterBlockItem extends BlockItem implements FueledToolItem {
    public CanisterBlockItem(Block block, Properties properties) {
        super(block, properties.stacksTo(1).enchantable(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, tooltipComponents, tooltipFlag);
        createTooltip(tooltipComponents, stack);
    }

    @Override
    public int getBaseCapacity(ItemStack stack) {
        return CDGConfig.CANISTER_CAPACITY.get() * DROPLETS_PER_MB;
    }

    @Override
    public int getCapacityEnchantmentAddition(ItemStack stack) {
        return CDGConfig.CANISTER_CAPACITY_ENCHANTMENT.get() * DROPLETS_PER_MB;
    }

    @Override
    public InteractionResult useOn(UseOnContext p_40581_) {
        return super.useOn(p_40581_);
    }
    @Override
    public int getBarColor(ItemStack stack) {
        return 0xEFEFEF;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCurrentFillLevel(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13 * (float) getCurrentFillLevel(stack) / getCapacity(stack));
    }
}
