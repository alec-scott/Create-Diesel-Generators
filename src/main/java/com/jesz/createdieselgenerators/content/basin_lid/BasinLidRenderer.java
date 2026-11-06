package com.jesz.createdieselgenerators.content.basin_lid;

import com.jesz.createdieselgenerators.CDGPartialModels;
import com.jesz.createdieselgenerators.client.render.PartsRenderer;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

import static com.jesz.createdieselgenerators.content.basin_lid.BasinLidBlock.FACING;
import static com.jesz.createdieselgenerators.content.basin_lid.BasinLidBlock.ON_A_BASIN;

/** PORT-NOTE(26.1): extract/submit; the progress dial on a basin. */
public class BasinLidRenderer extends PartsRenderer<BasinLidBlockEntity> {
    public BasinLidRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void extractParts(BasinLidBlockEntity be, State state, float partialTicks) {
        if (!be.getBlockState().getValue(ON_A_BASIN))
            return;
        Direction facing = be.getBlockState().getValue(FACING);
        state.parts.add(CachedBuffers.partial(CDGPartialModels.SMALL_GAUGE_DIAL, be.getBlockState())
                .center()
                .rotateYDegrees(-facing.toYRot() + 180)
                .translate(0.5625f, -0.375, 1.0625)
                .uncenter()
                .rotateZDegrees(be.progress * -90 + 90)
                .light(state.lightCoords)
                .extractRenderState());
    }
}
