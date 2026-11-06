package com.jesz.createdieselgenerators.content.tools;

import com.jesz.createdieselgenerators.CDGBlocks;
import com.jesz.createdieselgenerators.CDGConfig;
import com.jesz.createdieselgenerators.CDGDataComponents;
import com.jesz.createdieselgenerators.CDGItems;
import com.jesz.createdieselgenerators.port.neoforge.SimpleFluidContent;
import com.zurrtum.create.AllEnchantments;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantItemStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.function.Consumer;

/**
 * PORT-NOTE(26.1): fluid contents stay in the {@code fluid_contents} component (droplets, 81 per mB). The NeoForge
 * {@code FluidHandlerItemStack} capability became a Fabric {@link FluidStorage#ITEM} provider ({@link Storage}).
 * Config capacities are in mB and converted here.
 */
public interface FueledToolItem {
    int DROPLETS_PER_MB = 81;

    default int getBaseCapacity(ItemStack stack) {
        return CDGConfig.TOOL_CAPACITY.get() * DROPLETS_PER_MB;
    }

    default int getCapacityEnchantmentAddition(ItemStack stack) {
        return CDGConfig.TOOL_CAPACITY_ENCHANTMENT.get() * DROPLETS_PER_MB;
    }

    default int getCapacity(ItemStack stack) {
        int enchantmentLevel = 0;
        if (stack.has(DataComponents.ENCHANTMENTS))
            for (Object2IntMap.Entry<Holder<Enchantment>> enchantment : stack.get(DataComponents.ENCHANTMENTS).entrySet())
                if (enchantment.getKey().is(AllEnchantments.CAPACITY))
                    enchantmentLevel = enchantment.getIntValue();

        return getBaseCapacity(stack) + (getCapacityEnchantmentAddition(stack) * enchantmentLevel);
    }

    default FluidStack readFluid(ItemStack stack) {
        return stack.has(CDGDataComponents.FLUID_CONTENTS) ? stack.get(CDGDataComponents.FLUID_CONTENTS).copy() : FluidStack.EMPTY;
    }

    default void writeFluid(ItemStack stack, FluidStack fluid) {
        stack.set(CDGDataComponents.FLUID_CONTENTS, SimpleFluidContent.copyOf(fluid));
    }

    default int getCurrentFillLevel(ItemStack stack) {
        return readFluid(stack).getAmount();
    }

    /** Drains up to {@code amount} droplets from the stack, returns the amount drained. */
    default int drain(ItemStack stack, int amount) {
        FluidStack fluid = readFluid(stack);
        int drained = Math.min(amount, fluid.getAmount());
        if (drained <= 0)
            return 0;
        fluid.setAmount(fluid.getAmount() - drained);
        writeFluid(stack, fluid);
        return drained;
    }

    default void createTooltip(Consumer<Component> tooltip, ItemStack stack) {
        FluidStack fluid = readFluid(stack);
        if (fluid.isEmpty()) {
            tooltip.accept(Component.translatable("createdieselgenerators.tooltip.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.accept(fluid.getName().copy()
                .withStyle(ChatFormatting.GRAY)
                .append(" ")
                .append(Component.literal(String.valueOf(fluid.getAmount() / DROPLETS_PER_MB)).withStyle(ChatFormatting.GOLD))
                .append(Component.translatable("create.generic.unit.millibuckets").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" / "))
                .append(Component.literal(String.valueOf(getCapacity(stack) / DROPLETS_PER_MB)).withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("create.generic.unit.millibuckets").withStyle(ChatFormatting.GRAY)));
    }

    /** Was the NeoForge item fluid capability registration in ModEvents. */
    static void registerFluidStorage() {
        FluidStorage.ITEM.registerForItems((stack, context) -> stack.getItem() instanceof FueledToolItem tool ? new Storage(tool, context) : null,
                CDGItems.LIGHTER.get(), CDGItems.CHEMICAL_SPRAYER.get(), CDGItems.CHEMICAL_SPRAYER_LIGHTER.get(), CDGBlocks.CANISTER.get().asItem());
    }

    class Storage extends SingleVariantItemStorage<FluidVariant> {
        private final FueledToolItem tool;
        private final ContainerItemContext context;

        public Storage(FueledToolItem tool, ContainerItemContext context) {
            super(context);
            this.tool = tool;
            this.context = context;
        }

        @Override
        protected FluidVariant getBlankResource() {
            return FluidVariant.blank();
        }

        @Override
        protected FluidVariant getResource(ItemVariant variant) {
            SimpleFluidContent content = variant.getComponentMap().get(CDGDataComponents.FLUID_CONTENTS);
            if (content == null || content.isEmpty())
                return FluidVariant.blank();
            return FluidVariant.of(content.getFluid(), content.components());
        }

        @Override
        protected long getAmount(ItemVariant variant) {
            SimpleFluidContent content = variant.getComponentMap().get(CDGDataComponents.FLUID_CONTENTS);
            return content == null ? 0 : content.getAmount();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return tool.getCapacity(context.getItemVariant().toStack());
        }

        @Override
        protected ItemVariant getUpdatedVariant(ItemVariant currentVariant, FluidVariant newResource, long newAmount) {
            ItemStack stack = currentVariant.toStack();
            if (newResource.isBlank() || newAmount <= 0)
                stack.set(CDGDataComponents.FLUID_CONTENTS, SimpleFluidContent.EMPTY);
            else
                stack.set(CDGDataComponents.FLUID_CONTENTS, SimpleFluidContent.copyOf(
                        new FluidStack(newResource.getFluid(), (int) newAmount, newResource.getComponents())));
            return ItemVariant.of(stack);
        }
    }
}
