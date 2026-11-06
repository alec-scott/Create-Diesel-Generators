package com.jesz.createdieselgenerators.content.burner;

import com.jesz.createdieselgenerators.CDGPartialModels;
import com.jesz.createdieselgenerators.client.render.PartsKineticRenderer;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.content.kinetics.base.HorizontalAxisKineticBlock;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/** PORT-NOTE(26.1): extract/submit (see PartsKineticRenderer); the two valve dials. */
public class BurnerRenderer extends PartsKineticRenderer<BurnerBlockEntity> {
    public BurnerRenderer(BlockEntityRendererProvider.Context context) {
        super(context, true);
    }

    @Override
    protected void extractParts(BurnerBlockEntity be, State state, float partialTicks) {
        BlockState blockState = be.getBlockState();
        float rotation = Mth.lerp(Mth.lerp(partialTicks, be.prevValveState, be.valveState), -45, 45);
        float yRot = blockState.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS) == Direction.Axis.X ? 90 : 0;
        state.parts.add(CachedBuffers.partial(CDGPartialModels.SMALL_GAUGE_DIAL, blockState)
                .center().rotateYDegrees(yRot).uncenter()
                .translate(0.25, 0.25, 0.5)
                .rotateXDegrees(rotation)
                .light(state.lightCoords)
                .extractRenderState());
        state.parts.add(CachedBuffers.partial(CDGPartialModels.SMALL_GAUGE_DIAL, blockState)
                .center().rotateYDegrees(yRot).uncenter()
                .translate(0.75, 0.25, 0.5)
                .rotateXDegrees(-rotation)
                .light(state.lightCoords)
                .extractRenderState());
    }
}
