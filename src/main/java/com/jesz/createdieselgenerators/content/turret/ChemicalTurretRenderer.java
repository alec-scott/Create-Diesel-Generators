package com.jesz.createdieselgenerators.content.turret;

import com.jesz.createdieselgenerators.CDGPartialModels;
import com.jesz.createdieselgenerators.client.render.PartsKineticRenderer;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * PORT-NOTE(26.1): extract/submit. The cog is drawn here (no Flywheel visual); the filter slot is Create Fly's
 * client FilteringBehaviour (was EntityFilteringRenderer.renderOnBlockEntity).
 */
public class ChemicalTurretRenderer extends PartsKineticRenderer<ChemicalTurretBlockEntity> {
    public ChemicalTurretRenderer(BlockEntityRendererProvider.Context context) {
        super(context, false);
    }

    @Override
    protected void extractParts(ChemicalTurretBlockEntity be, State state, float partialTicks) {
        BlockState blockState = be.getBlockState();
        int light = state.lightCoords;
        state.parts.add(rotating(be, state, CachedBuffers.partial(CDGPartialModels.CHEMICAL_TURRET_COG, blockState)));

        float horizontalRotation = AngleHelper.angleLerp(partialTicks, be.oldHorizontalRotation, be.horizontalRotation);
        float verticalRotation = AngleHelper.angleLerp(partialTicks, be.oldVerticalRotation, be.verticalRotation);

        state.parts.add(CachedBuffers.partial(CDGPartialModels.CHEMICAL_TURRET_CONNECTOR, blockState)
                .center()
                .rotateYDegrees(horizontalRotation)
                .uncenter()
                .light(light)
                .extractRenderState());
        state.parts.add(CachedBuffers.partial(CDGPartialModels.CHEMICAL_TURRET_BODY, blockState)
                .center()
                .rotateYDegrees(horizontalRotation + 180)
                .uncenter()
                .translate(0.5, 1.3125, 0.125)
                .rotateXDegrees(verticalRotation)
                .light(light)
                .extractRenderState());
        if (be.lighterUpgrade)
            state.parts.add(CachedBuffers.partial(CDGPartialModels.CHEMICAL_TURRET_LIGHTER, blockState)
                    .center()
                    .rotateYDegrees(horizontalRotation + 180)
                    .uncenter()
                    .translate(0.5, 1.3125, 0.125)
                    .rotateXDegrees(verticalRotation)
                    .light(light)
                    .extractRenderState());
        state.parts.add(CachedBuffers.partial(CDGPartialModels.CHEMICAL_TURRET_SMALL_COG, blockState)
                .center()
                .rotateYDegrees(horizontalRotation + 180)
                .uncenter()
                .translate(0.5, 1.3125, 0.125)
                .rotateXDegrees(verticalRotation)
                .rotateZDegrees(Mth.lerp(partialTicks, be.lastCogRotation, be.cogRotation))
                .light(light)
                .extractRenderState());
    }
}
