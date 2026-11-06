package com.jesz.createdieselgenerators.compat.jei;

import com.google.common.base.Suppliers;
import com.jesz.createdieselgenerators.CDGBlocks;
import com.jesz.createdieselgenerators.CDGDataComponents;
import com.jesz.createdieselgenerators.CDGItems;
import com.jesz.createdieselgenerators.CreateDieselGenerators;
import com.jesz.createdieselgenerators.content.basin_lid.BasinFermentingRecipe;
import com.jesz.createdieselgenerators.content.bulk_fermenter.BulkFermentingRecipe;
import com.jesz.createdieselgenerators.content.distillation.DistillationRecipe;
import com.jesz.createdieselgenerators.content.molds.CastingRecipe;
import com.jesz.createdieselgenerators.content.molds.CompressionMoldingRecipe;
import com.jesz.createdieselgenerators.content.tools.hammer.HammerRecipe;
import com.jesz.createdieselgenerators.content.tools.wire_cutters.WireCuttingRecipe;
import com.zurrtum.create.AllItems;
import com.zurrtum.create.client.compat.jei.CreateCategory;
import com.zurrtum.create.client.compat.jei.category.BasinCategory;
import com.zurrtum.create.client.compat.jei.renderer.IconRenderer;
import com.zurrtum.create.client.compat.jei.renderer.TwoIconRenderer;
import com.zurrtum.create.client.foundation.gui.AllGuiTextures;
import com.zurrtum.create.client.foundation.gui.render.BasinBlazeBurnerRenderState;
import com.zurrtum.create.client.foundation.gui.render.PressBasinRenderState;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.zurrtum.create.content.processing.recipe.HeatCondition;
import com.zurrtum.create.content.processing.recipe.ProcessingOutput;
import com.zurrtum.create.content.processing.recipe.SizedIngredient;
import com.zurrtum.create.foundation.fluid.FluidIngredient;
import com.zurrtum.create.foundation.recipe.CreateSingleStackRollableRecipe;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import org.joml.Matrix3x2f;

import java.util.List;
import java.util.function.Supplier;

/**
 * PORT-NOTE(26.1): JEI categories on Create Fly's CreateCategory/BasinCategory, with upstream's slot layouts. Upstream's
 * animated machine elements (AnimatedKinetics, GuiGameElement) have no 26.1 equivalent short of one picture-in-picture
 * renderer per machine: Create Fly's basin, press and blaze burner renderers are reused where they fit, the rest draw a
 * scaled item of the machine (see PORTING-26.1.md TODO).
 */
public final class CDGCategories {
    private CDGCategories() {
    }

    private static Component title(String name) {
        return Component.translatable(CreateDieselGenerators.ID + ".recipe." + name);
    }

    private static void drawMachine(GuiGraphicsExtractor graphics, ItemLike item, int x, int y, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.item(new ItemStack(item), 0, 0);
        graphics.pose().popMatrix();
    }

    /** The heat bar, its label and the blaze burner, at upstream's height offset. */
    private static void drawHeat(GuiGraphicsExtractor graphics, HeatCondition heat, int yOffset) {
        boolean noHeat = heat == HeatCondition.NONE;
        (noHeat ? AllGuiTextures.JEI_SHADOW : AllGuiTextures.JEI_LIGHT).render(graphics, 81, yOffset + (noHeat ? -12 : 8));
        (noHeat ? AllGuiTextures.JEI_NO_HEAT_BAR : AllGuiTextures.JEI_HEAT_BAR).render(graphics, 4, yOffset);
        if (!noHeat)
            graphics.guiRenderState.addPicturesInPictureState(new BasinBlazeBurnerRenderState(
                    new Matrix3x2f(graphics.pose()), 91, yOffset - 11, heat.visualizeAsBlazeBurner()));
        graphics.text(net.minecraft.client.Minecraft.getInstance().font, CreateLang.translateDirect(heat.getTranslationKey()), 9, yOffset + 6, heat.getColor(), false);
    }

    private static void addHeatSlots(IRecipeLayoutBuilder builder, HeatCondition heat, int y) {
        if (!heat.testBlazeBurner(HeatLevel.NONE))
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, y).add(AllItems.BLAZE_BURNER);
        if (!heat.testBlazeBurner(HeatLevel.KINDLED))
            builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 153, y).add(AllItems.BLAZE_CAKE);
    }

    private static ItemStack moldStack(Identifier mold) {
        ItemStack stack = CDGItems.MOLD.asStack();
        stack.set(CDGDataComponents.MOLD_TYPE, mold);
        return stack;
    }

    public static class BasinFermenting extends BasinCategory<BasinFermentingRecipe> {
        @Override
        public IRecipeType<RecipeHolder<BasinFermentingRecipe>> getRecipeType() {
            return CDGJEI.BASIN_FERMENTING;
        }

        @Override
        public Component getTitle() {
            return title("basin_fermenting");
        }

        @Override
        public IDrawable getIcon() {
            return new TwoIconRenderer(AllItems.BASIN, CDGBlocks.BASIN_LID.asItem());
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BasinFermentingRecipe> entry, IFocusGroup focuses) {
            BasinFermentingRecipe recipe = entry.value();
            addIngredientSlots(builder, recipe);
            int resultSize = recipe.results().size();
            int size = resultSize + recipe.fluidResults().size();
            addResultSlots(builder, recipe.results(), 0, size - 1, 51, size % 2 != 0);
            addFluidResultSlots(builder, recipe.fluidResults(), resultSize, size - 1, 51, size % 2 != 0);
            addHeatSlots(builder, recipe);
        }

        @Override
        public void draw(RecipeHolder<BasinFermentingRecipe> entry, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            BasinFermentingRecipe recipe = entry.value();
            drawBackground(recipe, graphics, recipe.results().size() + recipe.fluidResults().size());
            drawMachine(graphics, AllItems.BASIN, 75, 42, 2);
            drawMachine(graphics, CDGBlocks.BASIN_LID.asItem(), 75, 22, 2);
        }
    }

    public static class CompressionMolding extends BasinCategory<CompressionMoldingRecipe> {
        @Override
        public IRecipeType<RecipeHolder<CompressionMoldingRecipe>> getRecipeType() {
            return CDGJEI.COMPRESSION_MOLDING;
        }

        @Override
        public Component getTitle() {
            return title("compression_molding");
        }

        @Override
        public IDrawable getIcon() {
            return new TwoIconRenderer(AllItems.MECHANICAL_PRESS, CDGItems.MOLD.get());
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CompressionMoldingRecipe> entry, IFocusGroup focuses) {
            CompressionMoldingRecipe recipe = entry.value();
            addIngredientSlots(builder, recipe);
            int size = recipe.results().size();
            addResultSlots(builder, recipe.results(), 0, size - 1, 51, size % 2 != 0);
            addHeatSlots(builder, recipe);
            builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 36, 11).setBackground(SLOT, -1, -1).add(moldStack(recipe.mold()));
        }

        @Override
        public void draw(RecipeHolder<CompressionMoldingRecipe> entry, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            CompressionMoldingRecipe recipe = entry.value();
            drawBackground(recipe, graphics, recipe.results().size());
            graphics.guiRenderState.addPicturesInPictureState(new PressBasinRenderState(new Matrix3x2f(graphics.pose()), 91, -5));
        }
    }

    public static class BulkFermenting extends CreateCategory<RecipeHolder<BulkFermentingRecipe>> {
        @Override
        public IRecipeType<RecipeHolder<BulkFermentingRecipe>> getRecipeType() {
            return CDGJEI.BULK_FERMENTING;
        }

        @Override
        public Component getTitle() {
            return title("bulk_fermenting");
        }

        @Override
        public IDrawable getIcon() {
            return new IconRenderer(CDGBlocks.BULK_FERMENTER.asItem());
        }

        @Override
        public int getHeight() {
            return 103;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BulkFermentingRecipe> entry, IFocusGroup focuses) {
            BulkFermentingRecipe recipe = entry.value();
            int size = recipe.ingredients().size() + recipe.fluidIngredients().size();
            int xOffset = size < 3 ? 12 + (3 - size) * 19 / 2 : 12;
            int i = 0;
            Supplier<ContextMap> context = Suppliers.memoize(CreateCategory::createIngredientContext);
            for (SizedIngredient ingredient : recipe.ingredients()) {
                builder.addInputSlot(xOffset + (i % 3) * 19, 51 - (i / 3) * 19).setBackground(SLOT, -1, -1)
                        .addItemStacks(getStacks(ingredient, context));
                i++;
            }
            for (FluidIngredient fluidIngredient : recipe.fluidIngredients()) {
                addFluidSlot(builder, xOffset + (i % 3) * 19, 51 - (i / 3) * 19, fluidIngredient).setBackground(SLOT, -1, -1);
                i++;
            }
            int resultSize = recipe.results().size();
            int outputs = resultSize + recipe.fluidResults().size();
            BasinCategory.addResultSlots(builder, recipe.results(), 0, outputs - 1, 51, outputs % 2 != 0);
            BasinCategory.addFluidResultSlots(builder, recipe.fluidResults(), resultSize, outputs - 1, 51, outputs % 2 != 0);
            addHeatSlots(builder, recipe.heat(), 81);
        }

        @Override
        public void draw(RecipeHolder<BulkFermentingRecipe> entry, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, 32);
            drawHeat(graphics, entry.value().heat(), 80);
            drawMachine(graphics, CDGBlocks.BULK_FERMENTER.asItem(), 75, 38, 2);
        }
    }

    public static class Distillation extends CreateCategory<RecipeHolder<DistillationRecipe>> {
        @Override
        public IRecipeType<RecipeHolder<DistillationRecipe>> getRecipeType() {
            return CDGJEI.DISTILLATION;
        }

        @Override
        public Component getTitle() {
            return title("distillation");
        }

        @Override
        public IDrawable getIcon() {
            return new TwoIconRenderer(AllItems.FLUID_TANK, CDGItems.DISTILLATION_CONTROLLER.get());
        }

        @Override
        public int getHeight() {
            return 200;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<DistillationRecipe> entry, IFocusGroup focuses) {
            DistillationRecipe recipe = entry.value();
            addFluidSlot(builder, 17, 145, recipe.fluidIngredient()).setBackground(SLOT, -1, -1);
            int i = 1;
            for (FluidStack fluidResult : recipe.fluidResults()) {
                addFluidSlot(builder, 130, -23 * i + 150, fluidResult).setBackground(SLOT, -1, -1);
                i++;
            }
            addHeatSlots(builder, recipe.heat(), 171);
        }

        @Override
        public void draw(RecipeHolder<DistillationRecipe> entry, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            DistillationRecipe recipe = entry.value();
            AllGuiTextures.JEI_ARROW.render(graphics, 40, 150);
            drawHeat(graphics, recipe.heat(), 170);
            int layers = recipe.fluidResults().size() + 1;
            for (int i = 0; i < layers; i++)
                drawMachine(graphics, AllItems.FLUID_TANK, 75, 130 - i * 24, 1.5f);
        }
    }

    public static class Casting extends CreateCategory<RecipeHolder<CastingRecipe>> {
        @Override
        public IRecipeType<RecipeHolder<CastingRecipe>> getRecipeType() {
            return CDGJEI.CASTING;
        }

        @Override
        public Component getTitle() {
            return title("casting");
        }

        @Override
        public IDrawable getIcon() {
            return new TwoIconRenderer(AllItems.SPOUT, CDGItems.MOLD.get());
        }

        @Override
        public int getHeight() {
            return 103;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CastingRecipe> entry, IFocusGroup focuses) {
            CastingRecipe recipe = entry.value();
            builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 36, 11).setBackground(SLOT, -1, -1).add(moldStack(recipe.mold()));
            addFluidSlot(builder, 36, 51, recipe.fluidIngredient()).setBackground(SLOT, -1, -1);
            List<ProcessingOutput> results = recipe.results();
            int size = results.size();
            BasinCategory.addResultSlots(builder, results, 0, size - 1, 51, size % 2 != 0);
        }

        @Override
        public void draw(RecipeHolder<CastingRecipe> entry, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            AllGuiTextures.JEI_SHADOW.render(graphics, 81, 68);
            AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, 32);
            drawMachine(graphics, AllItems.BASIN, 75, 42, 2);
            drawMachine(graphics, AllItems.SPOUT, 75, 10, 2);
        }
    }

    private abstract static class Tool<T extends CreateSingleStackRollableRecipe> extends CreateCategory<RecipeHolder<T>> {
        private final String name;
        private final ItemLike tool;

        Tool(String name, ItemLike tool) {
            this.name = name;
            this.tool = tool;
        }

        @Override
        public Component getTitle() {
            return title(name);
        }

        @Override
        public IDrawable getIcon() {
            return new IconRenderer(tool.asItem());
        }

        @Override
        public int getHeight() {
            return 55;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<T> entry, IFocusGroup focuses) {
            T recipe = entry.value();
            Supplier<ContextMap> context = Suppliers.memoize(CreateCategory::createIngredientContext);
            builder.addInputSlot(27, 29).setBackground(SLOT, -1, -1).addItemStacks(getStacks(recipe.ingredient(), context));
            addChanceSlot(builder, 132, 29, recipe.results().getFirst());
        }

        @Override
        public void draw(RecipeHolder<T> entry, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            AllGuiTextures.JEI_SHADOW.render(graphics, 61, 21);
            AllGuiTextures.JEI_LONG_ARROW.render(graphics, 52, 32);
            drawMachine(graphics, tool, getWidth() / 2 - 16, 0, 2);
        }
    }

    public static class Hammering extends Tool<HammerRecipe> {
        public Hammering() {
            super("hammering", CDGItems.HAMMER.get());
        }

        @Override
        public IRecipeType<RecipeHolder<HammerRecipe>> getRecipeType() {
            return CDGJEI.HAMMERING;
        }
    }

    public static class WireCutting extends Tool<WireCuttingRecipe> {
        public WireCutting() {
            super("wire_cutting", CDGItems.WIRE_CUTTERS.get());
        }

        @Override
        public IRecipeType<RecipeHolder<WireCuttingRecipe>> getRecipeType() {
            return CDGJEI.WIRE_CUTTING;
        }
    }
}
