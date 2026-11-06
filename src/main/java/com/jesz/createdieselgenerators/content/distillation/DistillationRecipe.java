package com.jesz.createdieselgenerators.content.distillation;

import com.jesz.createdieselgenerators.CDGRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zurrtum.create.content.processing.recipe.HeatCondition;
import com.zurrtum.create.foundation.fluid.FluidIngredient;
import com.zurrtum.create.foundation.recipe.CreateRecipe;
import com.zurrtum.create.foundation.recipe.TimedRecipe;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * PORT-NOTE(26.1): {@code {fluid_ingredient, fluid_results (1-6), heat_requirement?, processing_time}}, droplets.
 * Result {@code i} goes into the {@code i}-th tank layer above the bottom one.
 */
public record DistillationRecipe(int time, FluidIngredient fluidIngredient, List<FluidStack> fluidResults,
                                 HeatCondition heat) implements CreateRecipe<RecipeInput>, TimedRecipe {
    public static final MapCodec<DistillationRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("processing_time", 100).forGetter(DistillationRecipe::time),
            FluidIngredient.CODEC.fieldOf("fluid_ingredient").forGetter(DistillationRecipe::fluidIngredient),
            FluidStack.CODEC.listOf(1, 6).fieldOf("fluid_results").forGetter(DistillationRecipe::fluidResults),
            HeatCondition.CODEC.optionalFieldOf("heat_requirement", HeatCondition.NONE).forGetter(DistillationRecipe::heat)
    ).apply(instance, DistillationRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, DistillationRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, DistillationRecipe::time,
            FluidIngredient.PACKET_CODEC, DistillationRecipe::fluidIngredient,
            FluidStack.PACKET_CODEC.apply(ByteBufCodecs.list()), DistillationRecipe::fluidResults,
            HeatCondition.PACKET_CODEC, DistillationRecipe::heat,
            DistillationRecipe::new
    );
    public static final RecipeSerializer<DistillationRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public int getProcessingDuration() {
        return time;
    }

    public HeatCondition getRequiredHeat() {
        return heat;
    }

    public List<FluidStack> getFluidResults() {
        return fluidResults;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<DistillationRecipe> getSerializer() {
        return CDGRecipes.DISTILLATION_SERIALIZER;
    }

    @Override
    public RecipeType<DistillationRecipe> getType() {
        return CDGRecipes.DISTILLATION;
    }
}
