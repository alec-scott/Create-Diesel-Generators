package com.jesz.createdieselgenerators.client.render.gui;

import com.jesz.createdieselgenerators.CDGBlocks;
import com.jesz.createdieselgenerators.CDGPartialModels;
import com.jesz.createdieselgenerators.content.basin_lid.BasinLidBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.animation.AnimationTickHolder;
import com.zurrtum.create.client.catnip.render.FluidRenderHelper;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.joml.Matrix3x2f;

/**
 * The machines of the JEI categories (upstream: {@code AnimatedBulkFermenter}, {@code AnimatedDistillationTower},
 * {@code AnimatedSpoutCastingStation}, {@code BasinFermentingStationElement}) as picture-in-picture renderers, since 26.1
 * has no immediate-mode {@code GuiGameElement}. The states carry upstream's draw coordinates, so the categories keep
 * calling them with upstream's numbers. Registered through Fabric's {@link PictureInPictureRendererRegistry}, which
 * gives every state of a frame its own renderer instance. Nothing here refers to JEI.
 */
public final class CDGGuiMachines {
    private CDGGuiMachines() {
    }

    public static void register() {
        PictureInPictureRendererRegistry.register(context -> new BasinFermentingRenderer(context.bufferSource()));
        PictureInPictureRendererRegistry.register(context -> new BulkFermenterRenderer(context.bufferSource()));
        PictureInPictureRendererRegistry.register(context -> new DistillationRenderer(context.bufferSource()));
        PictureInPictureRendererRegistry.register(context -> new CastingRenderer(context.bufferSource()));
    }

    /** {@code AnimatedKinetics.getCurrentAngle()}. */
    private static float currentAngle() {
        return AnimationTickHolder.getRenderTime() * 4f % 360;
    }

    // BasinFermentingStationElement: lid on a basin and the slowly turning dial, drawn at upstream's (91, 47).
    public record BasinFermentingState(Matrix3x2f pose, int x0, int y0, int x1, int y1,
                                       ScreenRectangle bounds) implements MachineState {
        private static final int UP = 36, BELOW = 36;

        public static BasinFermentingState at(Matrix3x2f pose, int ux, int uy) {
            return new BasinFermentingState(pose, MachineState.x0(ux), MachineState.y0(uy, UP), MachineState.x0(ux) + WIDTH,
                    MachineState.y1(uy, BELOW), MachineState.box(pose, ux, uy, UP, BELOW));
        }
    }

    private static class BasinFermentingRenderer extends MachineRenderer<BasinFermentingState> {
        BasinFermentingRenderer(BufferSource bufferSource) {
            super(bufferSource, BasinFermentingState.BELOW);
        }

        @Override
        protected void draw(BasinFermentingState state, PoseStack matrices) {
            block(CDGBlocks.BASIN_LID.get().defaultBlockState().setValue(BasinLidBlock.ON_A_BASIN, true), 0, 0, 0, null);
            block(AllBlocks.BASIN.defaultBlockState(), 0, 1, 0, null);
            partial(CDGPartialModels.SMALL_GAUGE_DIAL, 0.5625, 0.375, 0.5625, aroundOriginZ(currentAngle() / 4));
        }

        @Override
        protected String getTextureLabel() {
            return "CDG Basin Fermenting";
        }

        @Override
        public Class<BasinFermentingState> getRenderStateClass() {
            return BasinFermentingState.class;
        }
    }

    // AnimatedBulkFermenter, drawn at upstream's (91, 27). Nothing in it moves.
    public record BulkFermenterState(Matrix3x2f pose, int x0, int y0, int x1, int y1,
                                     ScreenRectangle bounds) implements MachineState {
        private static final int UP = 36, BELOW = 56;

        public static BulkFermenterState at(Matrix3x2f pose, int ux, int uy) {
            return new BulkFermenterState(pose, MachineState.x0(ux), MachineState.y0(uy, UP), MachineState.x0(ux) + WIDTH,
                    MachineState.y1(uy, BELOW), MachineState.box(pose, ux, uy, UP, BELOW));
        }
    }

    private static class BulkFermenterRenderer extends MachineRenderer<BulkFermenterState> {
        BulkFermenterRenderer(BufferSource bufferSource) {
            super(bufferSource, BulkFermenterState.BELOW);
        }

        @Override
        protected void draw(BulkFermenterState state, PoseStack matrices) {
            partial(CDGPartialModels.JEI_BULK_FERMENTER, 0, 1, 0, aroundCentreY(90));
            partial(CDGPartialModels.BULK_FERMENTER_GAUGE, 0, 2, 0.125, aroundCentreY(-90));
            partial(AllPartialModels.BOILER_GAUGE_DIAL, 0, 2, 0.125, aroundCentreY(-90));
        }

        @Override
        protected String getTextureLabel() {
            return "CDG Bulk Fermenter";
        }

        @Override
        public Class<BulkFermenterState> getRenderStateClass() {
            return BulkFermenterState.class;
        }
    }

    // AnimatedDistillationTower, drawn at upstream's (91, 142) with as many layers as the recipe has outputs.
    public record DistillationState(Matrix3x2f pose, int x0, int y0, int x1, int y1, ScreenRectangle bounds,
                                    int height) implements MachineState {
        private static final int BELOW = 36;

        public static DistillationState at(Matrix3x2f pose, int ux, int uy, int height) {
            int up = 14 + height * 23;
            return new DistillationState(pose, MachineState.x0(ux), MachineState.y0(uy, up), MachineState.x0(ux) + WIDTH,
                    MachineState.y1(uy, BELOW), MachineState.box(pose, ux, uy, up, BELOW), height);
        }
    }

    private static class DistillationRenderer extends MachineRenderer<DistillationState> {
        DistillationRenderer(BufferSource bufferSource) {
            super(bufferSource, DistillationState.BELOW);
        }

        @Override
        protected void draw(DistillationState state, PoseStack matrices) {
            int height = state.height();
            partial(CDGPartialModels.JEI_DISTILLER_BOTTOM, 0, 1, 0, aroundCentreY(90));
            for (int i = 0; i < height - 1; i++)
                partial(CDGPartialModels.JEI_DISTILLER_MIDDLE, 0, -i, 0, aroundCentreY(90));
            partial(CDGPartialModels.JEI_DISTILLER_TOP, 0, -height + 1, 0, aroundCentreY(90));
            partial(CDGPartialModels.DISTILLATION_GAUGE, 1, 1, 0.125, aroundOriginY(-90));
            partial(CDGPartialModels.DISTILLATION_GAUGE, 1 - 0.125, 1, 1, aroundOriginY(180));
        }

        @Override
        protected String getTextureLabel() {
            return "CDG Distillation Tower";
        }

        @Override
        public Class<DistillationState> getRenderStateClass() {
            return DistillationState.class;
        }
    }

    // AnimatedSpoutCastingStation, drawn at upstream's (91, 34): the spout squeezes down on a basin, with the fluid.
    public record CastingState(Matrix3x2f pose, int x0, int y0, int x1, int y1, ScreenRectangle bounds, Fluid fluid,
                               DataComponentPatch components) implements MachineState {
        private static final int UP = 36, BELOW = 68;

        public static CastingState at(Matrix3x2f pose, int ux, int uy, Fluid fluid, DataComponentPatch components) {
            return new CastingState(pose, MachineState.x0(ux), MachineState.y0(uy, UP), MachineState.x0(ux) + WIDTH,
                    MachineState.y1(uy, BELOW), MachineState.box(pose, ux, uy, UP, BELOW), fluid, components);
        }
    }

    private static class CastingRenderer extends MachineRenderer<CastingState> {
        CastingRenderer(BufferSource bufferSource) {
            super(bufferSource, CastingState.BELOW);
        }

        @Override
        protected void draw(CastingState state, PoseStack matrices) {
            block(AllBlocks.SPOUT.defaultBlockState(), 0, 0, 0, null);

            float cycle = AnimationTickHolder.getRenderTime() % 30;
            float squeeze = cycle < 20 ? Mth.sin((float) (cycle / 20f * Math.PI)) : 0;
            squeeze *= 20;
            // Upstream moved the parts by 3 * squeeze / 32 pixels (up); a block is 23 pixels
            double move = 3 * squeeze / 32f / MachineState.BLOCK_SCALE;
            matrices.pushPose();
            matrices.translate(0, -move, 0);
            partial(AllPartialModels.SPOUT_MIDDLE, 0, 0, 0, null);
            matrices.translate(0, -move, 0);
            partial(AllPartialModels.SPOUT_BOTTOM, 0, 0, 0, null);
            matrices.popPose();

            block(AllBlocks.BASIN.defaultBlockState(), 0, 1.65, 0, null);

            Fluid fluid = state.fluid();
            if (fluid == Fluids.EMPTY)
                return;
            // Fluid boxes are 16 pixels to the unit upstream, against 23 for the blocks
            float unit = 16f / MachineState.BLOCK_SCALE;
            FluidStateModelSet fluidModels = Minecraft.getInstance().getModelManager().getFluidStateModelSet();

            matrices.pushPose();
            matrices.scale(unit, -unit, unit);
            float from = 3f / 16f;
            float to = 17f / 16f;
            FluidRenderHelper.extractFluidRenderState(null, null, fluidModels, fluid, state.components(), from, from, from, to, to, to,
                    LightCoordsUtil.FULL_BRIGHT, false, true).render(matrices, bufferSource);
            matrices.popPose();

            float width = 1 / 128f * squeeze;
            matrices.pushPose();
            matrices.translate(0.5, 1.5, 0.5);
            matrices.scale(unit, -unit, unit);
            matrices.translate(-0.5f, 0, -0.5f);
            from = -width / 2 + 0.5f;
            to = width / 2 + 0.5f;
            FluidRenderHelper.extractFluidRenderState(null, null, fluidModels, fluid, state.components(), from, 0, from, to, 2, to,
                    LightCoordsUtil.FULL_BRIGHT, false, true).render(matrices, bufferSource);
            matrices.popPose();
        }

        @Override
        protected String getTextureLabel() {
            return "CDG Casting Station";
        }

        @Override
        public Class<CastingState> getRenderStateClass() {
            return CastingState.class;
        }
    }
}
