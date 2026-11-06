package com.jesz.createdieselgenerators.content.molds;

import com.jesz.createdieselgenerators.CDGRecipes;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zurrtum.create.content.processing.basin.BasinInput;
import com.zurrtum.create.content.processing.basin.BasinRecipe;
import com.zurrtum.create.content.processing.recipe.HeatCondition;
import com.zurrtum.create.content.processing.recipe.ProcessingOutput;
import com.zurrtum.create.content.processing.recipe.SizedIngredient;
import com.zurrtum.create.foundation.blockEntity.behaviour.filtering.ServerFilteringBehaviour;
import com.zurrtum.create.foundation.fluid.FluidIngredient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * PORT-NOTE(26.1): Create Fly's compacting recipe shape plus {@code mold} ({@code ingredients} (repeats merge into
 * sized ingredients), {@code fluid_ingredients}, {@code heat_requirement}, {@code results}). Was a subclass of Create's
 * CompactingRecipe plus BasinRecipeMixin; Create Fly's CompactingRecipe is a final record, so the mold check is in
 * {@link #matches}/{@link #apply}. The press picks these up through MechanicalPressBlockEntityMixin.
 */
public record CompressionMoldingRecipe(List<ProcessingOutput> results, HeatCondition heat,
                                       List<FluidIngredient> fluidIngredients, List<SizedIngredient> ingredients,
                                       Identifier mold) implements BasinRecipe {
    public static final MapCodec<CompressionMoldingRecipe> MAP_CODEC = RecordCodecBuilder.<CompressionMoldingRecipe>mapCodec(instance -> instance.group(
            ProcessingOutput.CODEC.listOf(1, 4).fieldOf("results").forGetter(CompressionMoldingRecipe::results),
            HeatCondition.CODEC.optionalFieldOf("heat_requirement", HeatCondition.NONE).forGetter(CompressionMoldingRecipe::heat),
            FluidIngredient.CODEC.listOf(1, 2).optionalFieldOf("fluid_ingredients", List.of()).forGetter(CompressionMoldingRecipe::fluidIngredients),
            SizedIngredient.LIST_CODEC.optionalFieldOf("ingredients", List.of()).forGetter(CompressionMoldingRecipe::ingredients),
            Identifier.CODEC.fieldOf("mold").forGetter(CompressionMoldingRecipe::mold)
    ).apply(instance, CompressionMoldingRecipe::new)).validate(recipe -> {
        if (recipe.fluidIngredients.isEmpty() && recipe.ingredients.isEmpty())
            return DataResult.error(() -> "CompressionMoldingRecipe must have an ingredient or a fluid ingredient");
        return DataResult.success(recipe);
    });
    public static final StreamCodec<RegistryFriendlyByteBuf, CompressionMoldingRecipe> STREAM_CODEC = StreamCodec.composite(
            ProcessingOutput.STREAM_CODEC.apply(ByteBufCodecs.list()), CompressionMoldingRecipe::results,
            HeatCondition.PACKET_CODEC, CompressionMoldingRecipe::heat,
            FluidIngredient.PACKET_CODEC.apply(ByteBufCodecs.list()), CompressionMoldingRecipe::fluidIngredients,
            SizedIngredient.PACKET_CODEC.apply(ByteBufCodecs.list()), CompressionMoldingRecipe::ingredients,
            Identifier.STREAM_CODEC, CompressionMoldingRecipe::mold,
            CompressionMoldingRecipe::new
    );
    public static final RecipeSerializer<CompressionMoldingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public MoldType moldType() {
        return MoldType.findById(mold);
    }

    /** The basin must hold a mold of the given type (was BasinRecipeMixin). */
    public static boolean hasMold(Container items, MoldType type) {
        if (type == null || items == null)
            return false;
        for (int i = 0; i < items.getContainerSize(); i++) {
            ItemStack stack = items.getItem(i);
            if (stack.getItem() instanceof MoldItem && MoldItem.getMold(stack) == type)
                return true;
        }
        return false;
    }

    @Override
    public int getIngredientSize() {
        return fluidIngredients.size() + ingredients.size();
    }

    @Override
    public boolean matches(BasinInput input, Level world) {
        if (!heat.testBlazeBurner(input.heat()))
            return false;
        if (!hasMold(input.items(), moldType()))
            return false;
        ServerFilteringBehaviour filter = input.filter();
        if (filter == null)
            return false;
        if (!filter.test(results.getFirst().create()))
            return false;
        List<ItemStack> outputs = BasinRecipe.tryCraft(input, ingredients);
        if (outputs == null)
            return false;
        if (!BasinRecipe.matchFluidIngredient(input, fluidIngredients))
            return false;
        ProcessingOutput.rollOutput(input.random(), results, outputs::add);
        return input.acceptOutputs(outputs, List.of(), true);
    }

    @Override
    public boolean apply(BasinInput input) {
        if (!heat.testBlazeBurner(input.heat()))
            return false;
        if (!hasMold(input.items(), moldType()))
            return false;
        Deque<Runnable> changes = new ArrayDeque<>();
        List<ItemStack> outputs = BasinRecipe.prepareCraft(input, ingredients, changes);
        if (outputs == null)
            return false;
        if (!BasinRecipe.prepareFluidCraft(input, fluidIngredients, changes))
            return false;
        ProcessingOutput.rollOutput(input.random(), results, outputs::add);
        if (!input.acceptOutputs(outputs, List.of(), true))
            return false;
        changes.forEach(Runnable::run);
        return input.acceptOutputs(outputs, List.of(), false);
    }

    @Override
    public RecipeSerializer<CompressionMoldingRecipe> getSerializer() {
        return CDGRecipes.COMPRESSION_MOLDING_SERIALIZER;
    }

    @Override
    public RecipeType<CompressionMoldingRecipe> getType() {
        return CDGRecipes.COMPRESSION_MOLDING;
    }
}
