package com.jesz.createdieselgenerators.content.bulk_fermenter;

import com.jesz.createdieselgenerators.CDGRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock;
import com.zurrtum.create.content.processing.recipe.HeatCondition;
import com.zurrtum.create.content.processing.recipe.ProcessingOutput;
import com.zurrtum.create.content.processing.recipe.SizedIngredient;
import com.zurrtum.create.foundation.fluid.FluidIngredient;
import com.zurrtum.create.foundation.recipe.CreateRecipe;
import com.zurrtum.create.foundation.recipe.TimedRecipe;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * PORT-NOTE(26.1): Create Fly's mixing recipe shape ({@code ingredients}, {@code fluid_ingredients}, {@code results},
 * {@code fluid_results}, {@code heat_requirement}, {@code processing_time}), droplets. Applied to the bulk
 * fermenter's inventories by {@link #apply}, not through a recipe input.
 */
public record BulkFermentingRecipe(int time, List<ProcessingOutput> results, List<FluidStack> fluidResults,
                                   HeatCondition heat, List<FluidIngredient> fluidIngredients,
                                   List<SizedIngredient> ingredients) implements CreateRecipe<RecipeInput>, TimedRecipe {
    public static final MapCodec<BulkFermentingRecipe> MAP_CODEC = RecordCodecBuilder.<BulkFermentingRecipe>mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("processing_time", 100).forGetter(BulkFermentingRecipe::time),
            ProcessingOutput.CODEC.listOf(0, 4).optionalFieldOf("results", List.of()).forGetter(BulkFermentingRecipe::results),
            FluidStack.CODEC.listOf(0, 2).optionalFieldOf("fluid_results", List.of()).forGetter(BulkFermentingRecipe::fluidResults),
            HeatCondition.CODEC.optionalFieldOf("heat_requirement", HeatCondition.NONE).forGetter(BulkFermentingRecipe::heat),
            FluidIngredient.CODEC.listOf(0, 2).optionalFieldOf("fluid_ingredients", List.of()).forGetter(BulkFermentingRecipe::fluidIngredients),
            SizedIngredient.LIST_CODEC.optionalFieldOf("ingredients", List.of()).forGetter(BulkFermentingRecipe::ingredients)
    ).apply(instance, BulkFermentingRecipe::new)).validate(recipe -> {
        if (recipe.results.isEmpty() && recipe.fluidResults.isEmpty())
            return DataResult.error(() -> "BulkFermentingRecipe must have a result or a fluid result");
        if (recipe.fluidIngredients.isEmpty() && recipe.ingredients.isEmpty())
            return DataResult.error(() -> "BulkFermentingRecipe must have an ingredient or a fluid ingredient");
        return DataResult.success(recipe);
    });
    public static final StreamCodec<RegistryFriendlyByteBuf, BulkFermentingRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, BulkFermentingRecipe::time,
            ProcessingOutput.STREAM_CODEC.apply(ByteBufCodecs.list()), BulkFermentingRecipe::results,
            FluidStack.PACKET_CODEC.apply(ByteBufCodecs.list()), BulkFermentingRecipe::fluidResults,
            HeatCondition.PACKET_CODEC, BulkFermentingRecipe::heat,
            FluidIngredient.PACKET_CODEC.apply(ByteBufCodecs.list()), BulkFermentingRecipe::fluidIngredients,
            SizedIngredient.PACKET_CODEC.apply(ByteBufCodecs.list()), BulkFermentingRecipe::ingredients,
            BulkFermentingRecipe::new
    );
    public static final RecipeSerializer<BulkFermentingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public int getProcessingDuration() {
        return time;
    }

    public HeatCondition getRequiredHeat() {
        return heat;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input) {
        return ItemStack.EMPTY;
    }

    /** Checks ({@code test}) or performs one craft on the fermenter's controller inventories. */
    public boolean apply(BulkFermenterBlockEntity be, boolean test) {
        Container items = be.getItems();
        BulkFermenterFluidHandler fluids = be.tankInventory;
        if (items == null || fluids == null || be.getLevel() == null)
            return false;

        BlazeBurnerBlock.HeatLevel heatLevel = be.highestHeatLevel;
        if (!heat.testBlazeBurner(heatLevel))
            return false;

        // Items: find enough matching items per ingredient, without taking the same item twice
        int[] takenFromSlot = new int[items.getContainerSize()];
        for (SizedIngredient ingredient : ingredients) {
            int remaining = ingredient.getCount();
            for (int slot = 0; slot < items.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = items.getItem(slot);
                int available = stack.getCount() - takenFromSlot[slot];
                if (available <= 0 || !ingredient.getIngredient().test(stack))
                    continue;
                int take = Math.min(available, remaining);
                takenFromSlot[slot] += take;
                remaining -= take;
            }
            if (remaining > 0)
                return false;
        }

        // Fluids: each fluid ingredient from one tank, as upstream
        int[] takenFromTank = new int[fluids.size()];
        for (FluidIngredient ingredient : fluidIngredients) {
            boolean found = false;
            for (int tank = 0; tank < fluids.size(); tank++) {
                FluidStack stack = fluids.getStack(tank);
                if (stack.isEmpty() || !ingredient.test(stack))
                    continue;
                if (stack.getAmount() - takenFromTank[tank] < ingredient.amount())
                    continue;
                takenFromTank[tank] += ingredient.amount();
                found = true;
                break;
            }
            if (!found)
                return false;
        }

        List<ItemStack> outputItems = new ArrayList<>();
        ProcessingOutput.rollOutput(be.getLevel().getRandom(), results, outputItems::add);
        List<FluidStack> outputFluids = new ArrayList<>();
        for (FluidStack fluid : fluidResults)
            if (!fluid.isEmpty())
                outputFluids.add(fluid.copy());

        if (!outputItems.isEmpty() && !items.countSpace(outputItems))
            return false;
        if (!outputFluids.isEmpty() && !fluids.countSpace(outputFluids))
            return false;

        if (test)
            return true;

        for (int slot = 0; slot < takenFromSlot.length; slot++)
            if (takenFromSlot[slot] > 0)
                items.removeItem(slot, takenFromSlot[slot]);
        for (int tank = 0; tank < takenFromTank.length; tank++) {
            if (takenFromTank[tank] <= 0)
                continue;
            FluidStack stack = fluids.getStack(tank);
            stack.setAmount(stack.getAmount() - takenFromTank[tank]);
            if (stack.getAmount() <= 0)
                fluids.setStack(tank, FluidStack.EMPTY);
        }
        for (ItemStack stack : outputItems)
            items.insert(stack);
        for (FluidStack stack : outputFluids)
            fluids.insert(stack);
        items.setChanged();
        fluids.markDirty();
        return true;
    }

    @Override
    public RecipeSerializer<BulkFermentingRecipe> getSerializer() {
        return CDGRecipes.BULK_FERMENTING_SERIALIZER;
    }

    @Override
    public RecipeType<BulkFermentingRecipe> getType() {
        return CDGRecipes.BULK_FERMENTING;
    }
}
