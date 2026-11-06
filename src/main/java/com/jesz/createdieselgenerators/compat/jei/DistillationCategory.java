package com.jesz.createdieselgenerators.compat.jei;

import net.minecraft.world.item.ItemStack;

import com.jesz.createdieselgenerators.content.distillation.DistillationRecipe;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.AllItems;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedBlazeBurner;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock;
import com.zurrtum.create.content.processing.recipe.HeatCondition;
import com.zurrtum.create.client.foundation.gui.AllGuiTextures;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class DistillationCategory extends CreateRecipeCategory<DistillationRecipe> {

    private final AnimatedDistillationTower distillationTower = new AnimatedDistillationTower();
    private final AnimatedBlazeBurner heater = new AnimatedBlazeBurner();

    public DistillationCategory(Info<DistillationRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DistillationRecipe recipe, IFocusGroup focuses) {

        if(recipe.getFluidIngredients().isEmpty())
            return;
        SizedFluidIngredient fluidIngredient = recipe.getFluidIngredients().get(0);
        addFluidSlot(builder, 17, 145, fluidIngredient);


        int i = 1;

        for (FluidStack fluidResult : recipe.getFluidResults()) {
            int yPosition = -23 * i + 150;
            addFluidSlot(builder, 130, yPosition, fluidResult);
            i++;
        }

        HeatCondition requiredHeat = recipe.getRequiredHeat();
        if (!requiredHeat.testBlazeBurner(BlazeBurnerBlock.HeatLevel.NONE)) {
            builder
                    .addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 171)
                    .addItemStack(new ItemStack(AllBlocks.BLAZE_BURNER));
        }
        if (!requiredHeat.testBlazeBurner(BlazeBurnerBlock.HeatLevel.KINDLED)) {
            builder
                    .addSlot(RecipeIngredientRole.CATALYST, 153, 171)
                    .addItemStack(new ItemStack(AllItems.BLAZE_CAKE));
        }
    }

    @Override
    public void draw(DistillationRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        HeatCondition requiredHeat = recipe.getRequiredHeat();
        boolean noHeat = requiredHeat == HeatCondition.NONE;
        AllGuiTextures.JEI_ARROW.render(graphics, 40, 150);
        AllGuiTextures shadow = noHeat ? AllGuiTextures.JEI_SHADOW : AllGuiTextures.JEI_LIGHT;
        shadow.render(graphics, 81, 153 + (noHeat ? 10 : 30));
        distillationTower.draw(graphics, 91, 142, recipe.getFluidResults().size());

        if(!noHeat)
            heater.withHeat(requiredHeat.visualizeAsBlazeBurner()).draw(graphics, 91, 150);

        AllGuiTextures heatBar = noHeat ? AllGuiTextures.JEI_NO_HEAT_BAR : AllGuiTextures.JEI_HEAT_BAR;

        heatBar.render(graphics, 4, 170);
        graphics.drawString(Minecraft.getInstance().font, CreateLang.translateDirect(requiredHeat.getTranslationKey()), 9,
                176, requiredHeat.getColor(), false);
    }
}