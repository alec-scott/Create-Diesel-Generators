package com.jesz.createdieselgenerators.compat.jei;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.client.AllPartialModels;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.catnip.gui.UIRenderHelper;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import com.zurrtum.create.infrastructure.fluids.FluidStack;

import java.util.List;

public class AnimatedSpoutCastingStation extends AnimatedKinetics {
    private List<FluidStack> fluids;

    public AnimatedSpoutCastingStation withFluids(List<FluidStack> fluids) {
        this.fluids = fluids;
        return this;
    }

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(xOffset, yOffset, 100);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));
        int scale = 23;

        blockElement(AllBlocks.SPOUT.defaultBlockState())
                .scale(scale)
                .render(graphics);

        float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 30;
        float squeeze = cycle < 20 ? Mth.sin((float) (cycle / 20f * Math.PI)) : 0;
        squeeze *= 20;

        matrixStack.pushPose();

        blockElement(AllPartialModels.SPOUT_TOP)
                .scale(scale)
                .render(graphics);
        matrixStack.translate(0, -3 * squeeze / 32f, 0);
        blockElement(AllPartialModels.SPOUT_MIDDLE)
                .scale(scale)
                .render(graphics);
        matrixStack.translate(0, -3 * squeeze / 32f, 0);
        blockElement(AllPartialModels.SPOUT_BOTTOM)
                .scale(scale)
                .render(graphics);
        matrixStack.translate(0, -3 * squeeze / 32f, 0);

        matrixStack.popPose();

        blockElement(AllBlocks.BASIN.defaultBlockState())
                .atLocal(0, 1.65, 0)
                .scale(scale)
                .render(graphics);

        AnimatedKinetics.DEFAULT_LIGHTING.applyLighting();
        matrixStack.pushPose();
        UIRenderHelper.flipForGuiRender(matrixStack);
        matrixStack.scale(16, 16, 16);
        float from = 3f / 16f;
        float to = 17f / 16f;
        FluidStack fluidStack = fluids.get(0);
        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, from, from, from, to, to, to, graphics.bufferSource(), matrixStack, LightCoordsUtil.FULL_BRIGHT, false, true);

        matrixStack.popPose();

        float width = 1 / 128f * squeeze;
        matrixStack.translate(scale / 2f, scale * 1.5f, scale / 2f);
        UIRenderHelper.flipForGuiRender(matrixStack);
        matrixStack.scale(16, 16, 16);
        matrixStack.translate(-0.5f, 0, -0.5f);
        from = -width / 2 + 0.5f;
        to = width / 2 + 0.5f;
        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, from, 0, from, to, 2, to, graphics.bufferSource(), matrixStack, LightCoordsUtil.FULL_BRIGHT, false, true);

        Lighting.setupFor3DItems();

        matrixStack.popPose();
    }
}
