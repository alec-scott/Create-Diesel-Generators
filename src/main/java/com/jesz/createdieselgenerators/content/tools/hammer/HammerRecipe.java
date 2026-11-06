package com.jesz.createdieselgenerators.content.tools.hammer;

import com.jesz.createdieselgenerators.CDGRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zurrtum.create.content.processing.recipe.ProcessingOutput;
import com.zurrtum.create.foundation.recipe.CreateSingleStackRollableRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/** PORT-NOTE(26.1): Create Fly's single-ingredient recipe style, {@code {ingredient, results}}. */
public record HammerRecipe(List<ProcessingOutput> results, Ingredient ingredient) implements CreateSingleStackRollableRecipe {
    public static final MapCodec<HammerRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ProcessingOutput.CODEC.listOf(1, 4).fieldOf("results").forGetter(HammerRecipe::results),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(HammerRecipe::ingredient)
    ).apply(instance, HammerRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, HammerRecipe> STREAM_CODEC = StreamCodec.composite(
            ProcessingOutput.STREAM_CODEC.apply(ByteBufCodecs.list()), HammerRecipe::results,
            Ingredient.CONTENTS_STREAM_CODEC, HammerRecipe::ingredient,
            HammerRecipe::new
    );
    public static final RecipeSerializer<HammerRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public RecipeSerializer<HammerRecipe> getSerializer() {
        return CDGRecipes.HAMMERING_SERIALIZER;
    }

    @Override
    public RecipeType<HammerRecipe> getType() {
        return CDGRecipes.HAMMERING;
    }
}
