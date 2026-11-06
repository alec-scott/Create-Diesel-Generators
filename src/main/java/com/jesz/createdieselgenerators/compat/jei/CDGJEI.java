package com.jesz.createdieselgenerators.compat.jei;

import com.jesz.createdieselgenerators.CDGBlocks;
import com.jesz.createdieselgenerators.CDGDataComponents;
import com.jesz.createdieselgenerators.CDGItems;
import com.jesz.createdieselgenerators.CDGRecipes;
import com.jesz.createdieselgenerators.CreateDieselGenerators;
import com.jesz.createdieselgenerators.content.basin_lid.BasinFermentingRecipe;
import com.jesz.createdieselgenerators.content.bulk_fermenter.BulkFermentingRecipe;
import com.jesz.createdieselgenerators.content.distillation.DistillationRecipe;
import com.jesz.createdieselgenerators.content.molds.CastingRecipe;
import com.jesz.createdieselgenerators.content.molds.CompressionMoldingRecipe;
import com.jesz.createdieselgenerators.content.tools.hammer.HammerRecipe;
import com.jesz.createdieselgenerators.content.tools.wire_cutters.WireCuttingRecipe;
import com.zurrtum.create.AllItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.common.Internal;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

/**
 * PORT-NOTE(26.1): registered through the {@code jei_mod_plugin} entrypoint (JEI discovers it, so the mod runs without
 * JEI). Follows Create Fly's JeiClientPlugin: categories work on {@link RecipeHolder}s; serializers are synced when JEI
 * is loaded (CDGRecipes). The AbstractSimiContainerScreen SlotMover gui handler is left to Create Fly's own plugin.
 */
public class CDGJEI implements IModPlugin {
    private static final Identifier ID = CreateDieselGenerators.rl("jei_plugin");

    public static final IRecipeType<RecipeHolder<BasinFermentingRecipe>> BASIN_FERMENTING = type("basin_fermenting");
    public static final IRecipeType<RecipeHolder<BulkFermentingRecipe>> BULK_FERMENTING = type("bulk_fermenting");
    public static final IRecipeType<RecipeHolder<CompressionMoldingRecipe>> COMPRESSION_MOLDING = type("compression_molding");
    public static final IRecipeType<RecipeHolder<CastingRecipe>> CASTING = type("casting");
    public static final IRecipeType<RecipeHolder<DistillationRecipe>> DISTILLATION = type("distillation");
    public static final IRecipeType<RecipeHolder<HammerRecipe>> HAMMERING = type("hammering");
    public static final IRecipeType<RecipeHolder<WireCuttingRecipe>> WIRE_CUTTING = type("wire_cutting");

    @SuppressWarnings("unchecked")
    private static <T> IRecipeType<T> type(String path) {
        return (IRecipeType<T>) IRecipeType.create(CreateDieselGenerators.rl(path), RecipeHolder.class);
    }

    @Override
    public Identifier getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new CDGCategories.BasinFermenting(),
                new CDGCategories.BulkFermenting(),
                new CDGCategories.CompressionMolding(),
                new CDGCategories.Casting(),
                new CDGCategories.Distillation(),
                new CDGCategories.Hammering(),
                new CDGCategories.WireCutting());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeMap recipes = Internal.getClientSyncedRecipes();
        registration.addRecipes(BASIN_FERMENTING, recipes.byType(CDGRecipes.BASIN_FERMENTING).stream().toList());
        registration.addRecipes(BULK_FERMENTING, recipes.byType(CDGRecipes.BULK_FERMENTING).stream().toList());
        registration.addRecipes(COMPRESSION_MOLDING, recipes.byType(CDGRecipes.COMPRESSION_MOLDING).stream().toList());
        registration.addRecipes(CASTING, recipes.byType(CDGRecipes.CASTING).stream().toList());
        registration.addRecipes(DISTILLATION, recipes.byType(CDGRecipes.DISTILLATION).stream().toList());
        registration.addRecipes(HAMMERING, recipes.byType(CDGRecipes.HAMMERING).stream().toList());
        registration.addRecipes(WIRE_CUTTING, recipes.byType(CDGRecipes.WIRE_CUTTING).stream().toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(BASIN_FERMENTING, CDGBlocks.BASIN_LID.asItem(), AllItems.BASIN);
        registration.addCraftingStation(BULK_FERMENTING, CDGBlocks.BULK_FERMENTER.asItem());
        registration.addCraftingStation(COMPRESSION_MOLDING, AllItems.MECHANICAL_PRESS, CDGItems.MOLD.get(), AllItems.BASIN);
        registration.addCraftingStation(CASTING, AllItems.SPOUT, CDGItems.MOLD.get(), AllItems.BASIN);
        registration.addCraftingStation(DISTILLATION, AllItems.FLUID_TANK, CDGItems.DISTILLATION_CONTROLLER.get());
        registration.addCraftingStation(HAMMERING, CDGItems.HAMMER.get());
        registration.addCraftingStation(WIRE_CUTTING, CDGItems.WIRE_CUTTERS.get());
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(CDGItems.MOLD.get(), new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return stack.get(CDGDataComponents.MOLD_TYPE);
            }
        });
    }
}
