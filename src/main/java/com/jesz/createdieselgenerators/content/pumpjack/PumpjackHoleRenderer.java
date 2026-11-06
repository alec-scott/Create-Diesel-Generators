package com.jesz.createdieselgenerators.content.pumpjack;

import com.jesz.createdieselgenerators.CDGPartialModels;
import com.jesz.createdieselgenerators.client.render.PartsRenderer;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** PORT-NOTE(26.1): extract/submit; the rope down the well. */
public class PumpjackHoleRenderer extends PartsRenderer<PumpjackHoleBlockEntity> {
    public PumpjackHoleRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void extractParts(PumpjackHoleBlockEntity be, State state, float partialTicks) {
        if (be.pipeLength <= 0)
            return;
        state.parts.add(CachedBuffers.partial(CDGPartialModels.PUMPJACK_ROPE, be.getBlockState())
                .translate(0.5, 0, 0.5)
                .scale(1, be.pipeLength, 1)
                .light(state.lightCoords)
                .extractRenderState());
    }
}
