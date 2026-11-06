package com.jesz.createdieselgenerators.compat.jei;

import com.google.common.base.Suppliers;
import com.jesz.createdieselgenerators.client.render.gui.CDGGuiMachines;
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
import com.zurrtum.create.foundation.fluid.FluidStackIngredient;
import com.zurrtum.create.infrastructure.component.SandPaperItemComponent;
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
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import org.joml.Matrix3x2f;

import java.util.List;
import java.util.function.Supplier;

/**
 * PORT-NOTE(26.1): JEI categories on Create Fly's CreateCategory/BasinCategory, with upstream's slot, arrow, shadow and heat
 * bar coordinates and upstream's category sizes (177 x 100, distillation 200, tools 55). The machine drawings are
 * picture-in-picture states of client/render/gui/CDGGuiMachines (upstream: AnimatedKinetics elements) drawn at upstream's
 * coordinates; the blaze burner and the mechanical press are Create Fly's own, at the same place upstream's
 * AnimatedBlazeBurner / AnimatedPress were (the burner's box starts 14 pixels under upstream's y, as in Create Fly's
 * BasinCategory). The hammer and wire cutters are drawn as items, so their item models animate them.
 */
public final class CDGCategories {
    private CDGCategories() {
    }

    private static Component title(String name) {
        return Component.translatable(CreateDieselGenerators.ID + ".recipe." + name);
    }

    private static void drawMachine(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
    }

    /**
     * The shadow (or light), blaze burner, heat bar and its label, as every upstream category of this mod drew them:
     * {@code shadowY} is the shadow's y with no heat (it is 20 lower with heat), {@code burnerY} the y upstream passed to
     * {@code AnimatedBlazeBurner.draw}, {@code barY} the heat bar's.
     */
    private static void drawHeat(GuiGraphicsExtractor graphics, HeatCondition heat, int shadowY, int burnerY, int barY) {
        boolean noHeat = heat == HeatCondition.NONE;
        (noHeat ? AllGuiTextures.JEI_SHADOW : AllGuiTextures.JEI_LIGHT).render(graphics, 81, shadowY + (noHeat ? 10 : 30));
        if (!noHeat)
            graphics.guiRenderState.addPicturesInPictureState(new BasinBlazeBurnerRenderState(
                    new Matrix3x2f(graphics.pose()), 91, burnerY + 14, heat.visualizeAsBlazeBurner()));
        (noHeat ? AllGuiTextures.JEI_NO_HEAT_BAR : AllGuiTextures.JEI_HEAT_BAR).render(graphics, 4, barY);
        graphics.text(net.minecraft.client.Minecraft.getInstance().font, CreateLang.translateDirect(heat.getTranslationKey()), 9, barY + 6, heat.getColor(), false);
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

    /** The ingredient slots of the two fermenting categories: three to a row from the bottom, centred under three. */
    private static void addFermentingIngredients(IRecipeLayoutBuilder builder, List<SizedIngredient> ingredients, List<FluidIngredient> fluidIngredients, int x) {
        int size = ingredients.size() + fluidIngredients.size();
        int xOffset = size < 3 ? (3 - size) * 19 / 2 : 0;
        int i = 0;
        Supplier<ContextMap> context = Suppliers.memoize(CreateCategory::createIngredientContext);
        for (SizedIngredient ingredient : ingredients) {
            builder.addInputSlot(x + xOffset + (i % 3) * 19, 41 - (i / 3) * 19).setBackground(CreateCategory.SLOT, -1, -1)
                    .addItemStacks(CreateCategory.getStacks(ingredient, context));
            i++;
        }
        for (FluidIngredient fluidIngredient : fluidIngredients) {
            CreateCategory.addFluidSlot(builder, x + xOffset + (i % 3) * 19, 41 - (i / 3) * 19, fluidIngredient).setBackground(CreateCategory.SLOT, -1, -1);
            i++;
        }
    }

    private static void addFermentingResults(IRecipeLayoutBuilder builder, List<ProcessingOutput> results, List<FluidStack> fluidResults) {
        int resultSize = results.size();
        int size = resultSize + fluidResults.size();
        BasinCategory.addResultSlots(builder, results, 0, size - 1, 51, size % 2 != 0);
        BasinCategory.addFluidResultSlots(builder, fluidResults, resultSize, size - 1, 51, size % 2 != 0);
    }

    public static class BasinFermenting extends CreateCategory<RecipeHolder<BasinFermentingRecipe>> {
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
        public int getHeight() {
            return 100;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BasinFermentingRecipe> entry, IFocusGroup focuses) {
            BasinFermentingRecipe recipe = entry.value();
            addFermentingIngredients(builder, recipe.ingredients(), recipe.fluidIngredients(), 17);
            addFermentingResults(builder, recipe.results(), recipe.fluidResults());
            addHeatSlots(builder, recipe.heat(), 81);
        }

        @Override
        public void draw(RecipeHolder<BasinFermentingRecipe> entry, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            HeatCondition heat = entry.value().heat();
            AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, 20);
            drawHeat(graphics, heat, 58, 55, 80);
            graphics.guiRenderState.addPicturesInPictureState(CDGGuiMachines.BasinFermentingState.at(new Matrix3x2f(graphics.pose()), 91, 47));
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
        public int getHeight() {
            return 100;
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
            return new TwoIconRenderer(CDGBlocks.BULK_FERMENTER.asItem(), Items.CLOCK);
        }

        @Override
        public int getHeight() {
            return 100;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BulkFermentingRecipe> entry, IFocusGroup focuses) {
            BulkFermentingRecipe recipe = entry.value();
            addFermentingIngredients(builder, recipe.ingredients(), recipe.fluidIngredients(), 2);
            addFermentingResults(builder, recipe.results(), recipe.fluidResults());
            addHeatSlots(builder, recipe.heat(), 81);
        }

        @Override
        public void draw(RecipeHolder<BulkFermentingRecipe> entry, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, 20);
            drawHeat(graphics, entry.value().heat(), 58, 55, 80);
            graphics.guiRenderState.addPicturesInPictureState(CDGGuiMachines.BulkFermenterState.at(new Matrix3x2f(graphics.pose()), 91, 27));
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
            drawHeat(graphics, recipe.heat(), 153, 150, 170);
            graphics.guiRenderState.addPicturesInPictureState(CDGGuiMachines.DistillationState.at(
                    new Matrix3x2f(graphics.pose()), 91, 142, recipe.fluidResults().size()));
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
            return 100;
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
            CastingRecipe recipe = entry.value();
            AllGuiTextures.JEI_SHADOW.render(graphics, 81, 68);
            int vRows = (1 + recipe.results().size()) / 2;
            if (vRows <= 2)
                AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, -19 * (vRows - 1) + 32);
            // Upstream showed the first fluid of the ingredient
            Fluid fluid = Fluids.EMPTY;
            DataComponentPatch components = DataComponentPatch.EMPTY;
            List<Fluid> fluids = recipe.fluidIngredient().getMatchingFluids();
            if (!fluids.isEmpty())
                fluid = fluids.getFirst();
            if (recipe.fluidIngredient() instanceof FluidStackIngredient stackIngredient)
                components = stackIngredient.components();
            graphics.guiRenderState.addPicturesInPictureState(CDGGuiMachines.CastingState.at(
                    new Matrix3x2f(graphics.pose()), getWidth() / 2 + 3, 34, fluid, components));
        }
    }

    private abstract static class Tool<T extends CreateSingleStackRollableRecipe> extends CreateCategory<RecipeHolder<T>> {
        private final String name;
        private final Item tool;
        private final Item secondIcon;

        Tool(String name, Item tool, Item secondIcon) {
            this.name = name;
            this.tool = tool;
            this.secondIcon = secondIcon;
        }

        @Override
        public Component getTitle() {
            return title(name);
        }

        @Override
        public IDrawable getIcon() {
            return secondIcon == null ? new IconRenderer(tool) : new TwoIconRenderer(tool, secondIcon);
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
            List<ItemStack> matching = getStacks(entry.value().ingredient(), Suppliers.memoize(CreateCategory::createIngredientContext));
            if (matching.isEmpty())
                return;
            // The tool's item model animates the hammering / cutting while the processing item is set, in the GUI too
            ItemStack rendered = new ItemStack(tool);
            rendered.set(CDGDataComponents.PROCESSING_ITEM, new SandPaperItemComponent(matching.getFirst()));
            drawMachine(graphics, rendered, getWidth() / 2 - 16, 0, 2);
        }
    }

    public static class Hammering extends Tool<HammerRecipe> {
        public Hammering() {
            super("hammering", CDGItems.HAMMER.get(), AllItems.IRON_SHEET);
        }

        @Override
        public IRecipeType<RecipeHolder<HammerRecipe>> getRecipeType() {
            return CDGJEI.HAMMERING;
        }
    }

    public static class WireCutting extends Tool<WireCuttingRecipe> {
        public WireCutting() {
            super("wire_cutting", CDGItems.WIRE_CUTTERS.get(), null);
        }

        @Override
        public IRecipeType<RecipeHolder<WireCuttingRecipe>> getRecipeType() {
            return CDGJEI.WIRE_CUTTING;
        }
    }
}
