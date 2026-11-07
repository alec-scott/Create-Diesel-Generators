package com.jesz.createdieselgenerators;

import com.jesz.createdieselgenerators.content.basin_lid.BasinFermentingRecipe;
import com.jesz.createdieselgenerators.content.bulk_fermenter.BulkFermentingRecipe;
import com.jesz.createdieselgenerators.content.distillation.DistillationRecipe;
import com.jesz.createdieselgenerators.content.molds.CastingRecipe;
import com.jesz.createdieselgenerators.content.molds.CompressionMoldingRecipe;
import com.jesz.createdieselgenerators.content.tools.hammer.HammerRecipe;
import com.jesz.createdieselgenerators.content.tools.wire_cutters.WireCuttingRecipe;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import com.zurrtum.create.AllRecipeSets;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipePropertySet;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * PORT-NOTE(26.1): plain registry fields, as Create Fly's AllRecipeTypes/AllRecipeSerializers (was an enum over
 * Create's IRecipeTypeInfo and processing recipe serializers). Recipes are Create Fly-style records; see
 * PORTING-26.1.md for the JSON shapes.
 */
public class CDGRecipes {
    public static final RecipeType<BasinFermentingRecipe> BASIN_FERMENTING = registerType("basin_fermenting");
    public static final RecipeSerializer<BasinFermentingRecipe> BASIN_FERMENTING_SERIALIZER = registerSerializer("basin_fermenting", BasinFermentingRecipe.SERIALIZER);

    public static final RecipeType<BulkFermentingRecipe> BULK_FERMENTING = registerType("bulk_fermenting");
    public static final RecipeSerializer<BulkFermentingRecipe> BULK_FERMENTING_SERIALIZER = registerSerializer("bulk_fermenting", BulkFermentingRecipe.SERIALIZER);

    public static final RecipeType<DistillationRecipe> DISTILLATION = registerType("distillation");
    public static final RecipeSerializer<DistillationRecipe> DISTILLATION_SERIALIZER = registerSerializer("distillation", DistillationRecipe.SERIALIZER);

    public static final RecipeType<CompressionMoldingRecipe> COMPRESSION_MOLDING = registerType("compression_molding");
    public static final RecipeSerializer<CompressionMoldingRecipe> COMPRESSION_MOLDING_SERIALIZER = registerSerializer("compression_molding", CompressionMoldingRecipe.SERIALIZER);

    public static final RecipeType<CastingRecipe> CASTING = registerType("casting");
    public static final RecipeSerializer<CastingRecipe> CASTING_SERIALIZER = registerSerializer("casting", CastingRecipe.SERIALIZER);

    public static final RecipeType<WireCuttingRecipe> WIRE_CUTTING = registerType("wire_cutting");
    public static final RecipeSerializer<WireCuttingRecipe> WIRE_CUTTING_SERIALIZER = registerSerializer("wire_cutting", WireCuttingRecipe.SERIALIZER);

    public static final RecipeType<HammerRecipe> HAMMERING = registerType("hammering");
    public static final RecipeSerializer<HammerRecipe> HAMMERING_SERIALIZER = registerSerializer("hammering", HammerRecipe.SERIALIZER);

    // PORT-NOTE(26.1): ingredient sets synced to the client (RecipePropertySet, through Create Fly's AllRecipeSets), so
    // the hammer and wire cutters can check their off-hand item client side (the client has no recipe manager).
    public static final ResourceKey<RecipePropertySet> HAMMERING_SET = ResourceKey.create(RecipePropertySet.TYPE_KEY, CreateDieselGenerators.rl("hammering"));
    public static final ResourceKey<RecipePropertySet> WIRE_CUTTING_SET = ResourceKey.create(RecipePropertySet.TYPE_KEY, CreateDieselGenerators.rl("wire_cutting"));

    private static <T extends Recipe<?>> RecipeType<T> registerType(String id) {
        String name = CreateDieselGenerators.ID + ":" + id;
        return Registry.register(BuiltInRegistries.RECIPE_TYPE, CreateDieselGenerators.rl(id), new RecipeType<T>() {
            @Override
            public String toString() {
                return name;
            }
        });
    }

    private static <T extends Recipe<?>> RecipeSerializer<T> registerSerializer(String id, RecipeSerializer<T> serializer) {
        return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, CreateDieselGenerators.rl(id), serializer);
    }

    public static void register() {
        AllRecipeSets.ALL.put(HAMMERING_SET, recipe -> recipe instanceof HammerRecipe r ? Optional.of(r.ingredient()) : Optional.empty());
        AllRecipeSets.ALL.put(WIRE_CUTTING_SET, recipe -> recipe instanceof WireCuttingRecipe r ? Optional.of(r.ingredient()) : Optional.empty());
        // Always synced, so a dedicated server doesn't need JEI installed for its clients to see these in JEI
        RecipeSynchronization.synchronizeRecipeSerializer(BASIN_FERMENTING_SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(BULK_FERMENTING_SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(DISTILLATION_SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(COMPRESSION_MOLDING_SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(CASTING_SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(WIRE_CUTTING_SERIALIZER);
        RecipeSynchronization.synchronizeRecipeSerializer(HAMMERING_SERIALIZER);
    }
}
