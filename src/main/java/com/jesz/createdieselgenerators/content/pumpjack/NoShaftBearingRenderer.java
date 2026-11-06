package com.jesz.createdieselgenerators.content.pumpjack;

import com.jesz.createdieselgenerators.client.render.PartsKineticRenderer;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.content.contraptions.bearing.IBearingBlockEntity;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * PORT-NOTE(26.1): extract/submit; the bearing top only (no shaft), always drawn here (the Flywheel
 * NoShaftBearingInstance is not ported).
 */
public class NoShaftBearingRenderer<T extends KineticBlockEntity & IBearingBlockEntity> extends PartsKineticRenderer<T> {
    public NoShaftBearingRenderer(BlockEntityRendererProvider.Context context) {
        super(context, false);
    }

    @Override
    protected void extractParts(T be, State state, float partialTicks) {
        final Direction facing = be.getBlockState().getValue(BlockStateProperties.FACING);
        PartialModel top = be.isWoodenTop() ? AllPartialModels.BEARING_TOP_WOODEN : AllPartialModels.BEARING_TOP;
        SuperByteBuffer superBuffer = CachedBuffers.partial(top, be.getBlockState());

        float interpolatedAngle = be.getInterpolatedAngle(partialTicks - 1);
        superBuffer.rotateCentered((float) (interpolatedAngle / 180 * Math.PI), Direction.get(Direction.AxisDirection.POSITIVE, facing.getAxis()));
        if (facing.getAxis().isHorizontal())
            superBuffer.rotateCentered(AngleHelper.rad(AngleHelper.horizontalAngle(facing.getOpposite())), Direction.UP);
        superBuffer.rotateCentered(AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)), Direction.EAST);
        state.parts.add(superBuffer.light(state.lightCoords).extractRenderState());
    }
}
