package com.jesz.createdieselgenerators.content.bulk_fermenter;

import com.jesz.createdieselgenerators.CDGPartialModels;
import com.jesz.createdieselgenerators.client.render.PartsRenderer;
import com.zurrtum.create.catnip.data.Iterate;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** PORT-NOTE(26.1): extract/submit; progress gauges of the controller (recipe duration synced, see the BE). */
public class BulkFermenterRenderer extends PartsRenderer<BulkFermenterBlockEntity> {
    public BulkFermenterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(BulkFermenterBlockEntity be, Vec3 cameraPosition) {
        return be.isController() && super.shouldRender(be, cameraPosition);
    }

    @Override
    protected void extractParts(BulkFermenterBlockEntity be, State state, float partialTicks) {
        if (!be.isController())
            return;

        BlockState blockState = be.getBlockState();
        float half = be.getWidth() / 2f;
        float dialPivotY = 6f / 16;
        float dialPivotZ = 8f / 16;
        float progress = be.recipeDuration <= 0 || be.processingTime < 0 ? 0 :
                (float) Mth.clamp(Mth.lerp(partialTicks, be.processingTime + Math.sqrt(be.width * be.height), be.processingTime) / be.recipeDuration, 0, 1);

        for (Direction d : Iterate.horizontalDirections) {
            state.parts.add(CachedBuffers.partial(CDGPartialModels.BULK_FERMENTER_GAUGE, blockState)
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
                    .rotateXDegrees(-180 * progress + 90)
                    .translate(0, -dialPivotY, -dialPivotZ)
                    .light(state.lightCoords)
                    .extractRenderState());
        }
    }
}
