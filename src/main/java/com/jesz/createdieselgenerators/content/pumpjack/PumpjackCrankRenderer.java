package com.jesz.createdieselgenerators.content.pumpjack;

import com.jesz.createdieselgenerators.CDGPartialModels;
import com.jesz.createdieselgenerators.client.render.PartsKineticRenderer;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;

import static com.jesz.createdieselgenerators.content.pumpjack.PumpjackCrankBlock.HORIZONTAL_FACING;

/**
 * PORT-NOTE(26.1): extract/submit. Crank, rod and shaft are always drawn here (the Flywheel PumpjackCrankInstance is
 * not ported).
 */
public class PumpjackCrankRenderer extends PartsKineticRenderer<PumpjackCrankBlockEntity> {
    public PumpjackCrankRenderer(BlockEntityRendererProvider.Context context) {
        super(context, false);
    }

    @Override
    protected void extractParts(PumpjackCrankBlockEntity be, State state, float partialTicks) {
        state.parts.add(rotating(be, state, CachedBuffers.block(KINETIC_BLOCK, shaft(state.axis))));

        BlockState blockState = be.getBlockState();
        BlockPos pos = be.getBlockPos();
        float angle = AngleHelper.angleLerp(partialTicks, be.prevAngle, be.angle);
        boolean small = be.crankSize.getValue() == 0;

        boolean isXAxis = blockState.getValue(HORIZONTAL_FACING).getAxis() == Direction.Axis.X;
        double v = ((isXAxis ? angle : -angle) + 90) / 180 * Math.PI;

        double sin = Math.sin(v) * (small ? 0.8125 : 1.125);
        double cos = Math.cos(v) * (small ? 0.8125 : 1.125);
        SuperByteBuffer crank = CachedBuffers.partial(small ? CDGPartialModels.PUMPJACK_CRANK_SMALL : CDGPartialModels.PUMPJACK_CRANK_LARGE, blockState);
        SuperByteBuffer rod = CachedBuffers.partial(small ? CDGPartialModels.PUMPJACK_CRANK_ROD_SMALL : CDGPartialModels.PUMPJACK_CRANK_ROD_LARGE, blockState);

        double dstY = -1000 - sin - 1.25 - pos.getY();
        double dstX = pos.getX() - cos - 0.5 - pos.getX();
        double dstZ = pos.getZ() - cos - 0.5 - pos.getZ();

        if (be.bearingPos != null) {
            PumpjackBearingBlockEntity bearing = be.bearing.get();

            float interpolatedAngle = 0;
            if (bearing != null)
                interpolatedAngle = bearing.getInterpolatedAngle(partialTicks);
            if (be.inPonderAngle != Integer.MIN_VALUE)
                interpolatedAngle = be.inPonderAngle;

            if (!isXAxis)
                interpolatedAngle *= -1;
            double rad = interpolatedAngle / 180 * Math.PI;
            Vec2 crankBearingLocation = new Vec2(
                    (float) (be.crankBearingLocation.x * Math.cos(rad) - be.crankBearingLocation.y * Math.sin(rad)) + 0.5f,
                    (float) (be.crankBearingLocation.x * Math.sin(rad) + be.crankBearingLocation.y * Math.cos(rad)) + 0.5f);
            if (isXAxis)
                crankBearingLocation = crankBearingLocation.add(new Vec2((float) be.bearingPos.getX(), (float) be.bearingPos.getY()));
            else
                crankBearingLocation = crankBearingLocation.add(new Vec2((float) be.bearingPos.getZ(), (float) be.bearingPos.getY()));

            dstY = crankBearingLocation.y - sin - 1.25 - pos.getY();
            dstX = crankBearingLocation.x - cos - 0.5 - pos.getX();
            dstZ = crankBearingLocation.x - cos - 0.5 - pos.getZ();
        }

        if (isXAxis) {
            crank.translate(0.5, 1.25, 0).rotateZDegrees(angle);
            rod.translate(0.5, 1.25, 0).translate(cos, sin, 0).rotateZDegrees((float) (Math.atan2(dstY, dstX) * 180 / Math.PI - 90));
        } else {
            crank.translate(0, 1.25, 0.5).rotateYDegrees(90).rotateZDegrees(angle);
            rod.translate(0, 1.25, 0.5).translate(0, sin, cos).rotateYDegrees(90).rotateZDegrees((float) (Math.atan2(dstZ, dstY) * 180 / Math.PI));
        }

        state.parts.add(rod.light(state.lightCoords).extractRenderState());
        state.parts.add(crank.light(state.lightCoords).extractRenderState());
    }
}
