package com.jesz.createdieselgenerators.port.neoforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Optional;

/**
 * PORT-NOTE(26.1): stand-in for NeoForge's immutable SimpleFluidContent data component. Create Fly's FluidStack is
 * mutable and has no equals/hashCode, so item components store this record instead. Amounts are droplets.
 */
public record SimpleFluidContent(Optional<Holder<Fluid>> fluid, int amount, DataComponentPatch components) {
    public static final SimpleFluidContent EMPTY = new SimpleFluidContent(Optional.empty(), 0, DataComponentPatch.EMPTY);

    public static final Codec<SimpleFluidContent> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.FLUID.holderByNameCodec().optionalFieldOf("id").forGetter(SimpleFluidContent::fluid),
            Codec.INT.optionalFieldOf("amount", 0).forGetter(SimpleFluidContent::amount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(SimpleFluidContent::components)
    ).apply(i, SimpleFluidContent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SimpleFluidContent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.holderRegistry(net.minecraft.core.registries.Registries.FLUID)), SimpleFluidContent::fluid,
            ByteBufCodecs.VAR_INT, SimpleFluidContent::amount,
            DataComponentPatch.STREAM_CODEC, SimpleFluidContent::components,
            SimpleFluidContent::new
    );

    public static SimpleFluidContent copyOf(FluidStack stack) {
        if (stack.isEmpty())
            return EMPTY;
        return new SimpleFluidContent(Optional.of(stack.getRegistryEntry()), stack.getAmount(), stack.getComponentChanges());
    }

    public FluidStack copy() {
        if (isEmpty())
            return FluidStack.EMPTY;
        return new FluidStack(fluid.get(), amount, components);
    }

    public boolean isEmpty() {
        return amount <= 0 || fluid.isEmpty() || fluid.get().value() == Fluids.EMPTY;
    }

    public int getAmount() {
        return isEmpty() ? 0 : amount;
    }

    public Fluid getFluid() {
        return isEmpty() ? Fluids.EMPTY : fluid.get().value();
    }
}
