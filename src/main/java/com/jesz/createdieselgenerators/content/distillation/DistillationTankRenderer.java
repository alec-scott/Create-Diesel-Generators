package com.jesz.createdieselgenerators.content.distillation;

import com.jesz.createdieselgenerators.CDGPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.catnip.animation.LerpedFloat;
import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.FluidRenderHelper;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * PORT-NOTE(26.1): extract/submit, after Create Fly's FluidTankRenderer: the fluid box of a controller (whole tank
 * layer) and, on the bottom layer, the four progress gauges. Lighter-than-air fluids are not flipped (as Create Fly).
 */
public class DistillationTankRenderer implements BlockEntityRenderer<DistillationTankBlockEntity, DistillationTankRenderer.State> {
    private final FluidStateModelSet fluidStateModelSet;

    public DistillationTankRenderer(BlockEntityRendererProvider.Context context) {
        fluidStateModelSet = context.blockModelResolver().modelManager.getFluidStateModelSet();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRender(DistillationTankBlockEntity be, Vec3 cameraPosition) {
        return be.isController() && BlockEntityRenderer.super.shouldRender(be, cameraPosition);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void extractRenderState(DistillationTankBlockEntity be, State state, float partialTicks, Vec3 cameraPos,
                                   @Nullable CrumblingOverlay crumblingOverlay) {
        Level level = SmartBlockEntityRenderer.extractBase(be, state, crumblingOverlay);
        state.parts.clear();
        state.fluid = null;
        if (!be.isController())
            return;

        if (be.isBottom())
            extractGauges(be, state, partialTicks);

        LerpedFloat fluidLevel = be.getFluidLevel();
        if (fluidLevel == null)
            return;

        float capHeight = 0.25f;
        float tankHullWidth = 1 / 16f + 1 / 128f;
        float minPuddleHeight = 1 / 16f;
        float totalHeight = be.getHeight() - 2 * capHeight - minPuddleHeight;

        float fill = fluidLevel.getValue(partialTicks);
        if (fill < 1 / (512f * totalHeight))
            return;
        float clampedLevel = Mth.clamp(fill * totalHeight, 0, totalHeight);

        FluidStack fluidStack = be.tankInventory.getFluid();
        if (fluidStack.isEmpty())
            return;

        float xMin = tankHullWidth;
        float xMax = xMin + be.getWidth() - 2 * tankHullWidth;
        float yMin = totalHeight + capHeight + minPuddleHeight - clampedLevel;
        float yMax = yMin + clampedLevel;
        float zMin = tankHullWidth;
        float zMax = zMin + be.getWidth() - 2 * tankHullWidth;

        state.fluidTranslate = clampedLevel - totalHeight;
        state.fluid = FluidRenderHelper.extractFluidRenderState(
                level instanceof BlockAndTintGetter getter ? getter : null, state.blockPos, fluidStateModelSet,
                fluidStack.getFluid(), fluidStack.getComponentChanges(),
                xMin, yMin, zMin, xMax, yMax, zMax, state.lightCoords, false, true);
    }

    private void extractGauges(DistillationTankBlockEntity be, State state, float partialTicks) {
        BlockState blockState = be.getBlockState();
        float dialPivotY = 6f / 16;
        float dialPivotZ = 8f / 16;
        float duration = be.recipeDuration;
        float progress = Mth.clamp(be.processingTime > -1 && duration > 0 ? (be.processingTime - partialTicks) / duration : be.progress, 0, 1);
        float half = be.getWidth() / 2f;

        for (Direction d : Iterate.horizontalDirections) {
            state.parts.add(CachedBuffers.partial(CDGPartialModels.DISTILLATION_GAUGE, blockState)
                    .translate(half, 0.5, half)
                    .rotateYDegrees(d.toYRot())
                    .uncenter()
                    .translate(half - 6 / 16f, 0, 0)
                    .light(state.lightCoords)
                    .extractRenderState());
            state.parts.add(CachedBuffers.partial(AllPartialModels.BOILER_GAUGE_DIAL, blockState)
                    .translate(half, 0.5, half)
                    .rotateYDegrees(d.toYRot())
                    .uncenter()
                    .translate(half - 6 / 16f, 0, 0)
                    .translate(0, dialPivotY, dialPivotZ)
                    .rotateXDegrees(-145 * progress + 90)
                    .translate(0, -dialPivotY, -dialPivotZ)
                    .light(state.lightCoords)
                    .extractRenderState());
        }
    }

    @Override
    public void submit(State state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        for (SuperByteBufferRenderState part : state.parts)
            part.submit(matrices, queue);
        if (state.fluid != null) {
            matrices.pushPose();
            matrices.translate(0, state.fluidTranslate, 0);
            state.fluid.submit(matrices, queue);
            matrices.popPose();
        }
    }

    public static class State extends BlockEntityRenderState {
        public final List<SuperByteBufferRenderState> parts = new ArrayList<>();
        public FluidRenderHelper.@Nullable FluidRenderState fluid;
        public float fluidTranslate;
    }
}
