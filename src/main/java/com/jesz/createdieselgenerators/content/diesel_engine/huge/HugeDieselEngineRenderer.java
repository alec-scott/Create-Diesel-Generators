package com.jesz.createdieselgenerators.content.diesel_engine.huge;

import com.jesz.createdieselgenerators.CDGBlocks;
import com.jesz.createdieselgenerators.CDGPartialModels;
import com.jesz.createdieselgenerators.client.render.EngineUpgradeRenders;
import com.jesz.createdieselgenerators.client.render.PartsRenderer;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import static com.jesz.createdieselgenerators.content.diesel_engine.huge.HugeDieselEngineBlock.FACING;

/**
 * PORT-NOTE(26.1): extract/submit. The piston, linkage and connector are always drawn here (the Flywheel
 * HugeDieselEngineInstance is not ported); {@link #getTargetAngle} was HugeDieselEngineBlockEntity.getTargetAngle.
 */
public class HugeDieselEngineRenderer extends PartsRenderer<HugeDieselEngineBlockEntity> {
    public HugeDieselEngineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    public static Float getTargetAngle(HugeDieselEngineBlockEntity be) {
        BlockState state = be.getBlockState();
        if (!CDGBlocks.HUGE_DIESEL_ENGINE.has(state))
            return null;

        Direction facing = state.getValue(FACING);
        PoweredEngineShaftBlockEntity shaft = be.getShaft();
        Direction.Axis facingAxis = facing.getAxis();
        if (shaft == null)
            return null;

        Direction.Axis axis = KineticBlockEntityRenderer.getRotationAxisOf(shaft);
        float angle = KineticBlockEntityRenderer.getAngleForBe(shaft, shaft.getBlockPos(), axis);
        if (axis == facingAxis)
            return null;
        if (axis.isHorizontal() && (facingAxis == Direction.Axis.X ^ facing.getAxisDirection() == Direction.AxisDirection.POSITIVE))
            angle *= -1;
        if (axis == Direction.Axis.X && facing == Direction.DOWN)
            angle *= -1;
        return angle;
    }

    @Override
    protected void extractParts(HugeDieselEngineBlockEntity be, State state, float partialTicks) {
        int light = state.lightCoords;
        EngineUpgradeRenders.extract(be, be.upgrade, light, state.parts);

        Float angle = getTargetAngle(be);
        BlockState blockState = be.getBlockState();
        Direction facing = blockState.getValue(FACING);
        Direction.Axis facingAxis = facing.getAxis();
        PoweredEngineShaftBlockEntity shaft = be.getShaft();
        if (angle == null || shaft == null) {
            state.parts.add(transformed(CDGPartialModels.ENGINE_PISTON, blockState, facing, false)
                    .translate(0, 0.53475, 0)
                    .light(light)
                    .extractRenderState());
            return;
        }
        Direction.Axis axis = KineticBlockEntityRenderer.getRotationAxisOf(shaft);

        boolean roll90 = facingAxis.isHorizontal() && axis == Direction.Axis.Y || facingAxis.isVertical() && axis == Direction.Axis.Z;
        float shaftR = facing == Direction.DOWN ? -90 : facing == Direction.UP ? 90 : facing == Direction.WEST ? -90 : facing == Direction.EAST ? 90 : 0;
        if (roll90)
            shaftR = facing == Direction.NORTH ? 180 : facing == Direction.SOUTH ? 0 : facing == Direction.EAST ? -90 : facing == Direction.WEST ? 90 : 0;
        angle += (float) (shaftR * Math.PI / 180);

        float sign = facingAxis == Direction.Axis.Y ? -1 : 1;
        float sine = Mth.sin(angle) * sign;
        float sine2 = Mth.sin(angle - Mth.HALF_PI) * sign;
        float piston = ((1 - sine) / 4) + 0.4375f;

        state.parts.add(transformed(CDGPartialModels.ENGINE_PISTON, blockState, facing, roll90)
                .translate(0, piston, 0)
                .light(light)
                .extractRenderState());

        state.parts.add(transformed(CDGPartialModels.ENGINE_PISTON_LINKAGE, blockState, facing, roll90)
                .center()
                .translate(0, 1, 0)
                .uncenter()
                .translate(0, piston, 0)
                .translate(0, 4 / 16f, 8 / 16f)
                .rotateXDegrees(sine2 * 23f)
                .translate(0, -4 / 16f, -8 / 16f)
                .light(light)
                .extractRenderState());
        if (shaft.isEngineForConnectorDisplay(be.getBlockPos()))
            state.parts.add(transformed(CDGPartialModels.ENGINE_PISTON_CONNECTOR, blockState, facing, roll90)
                    .translate(0, 2, 0)
                    .center()
                    .rotateX(-angle + Mth.HALF_PI)
                    .uncenter()
                    .light(light)
                    .extractRenderState());
    }

    private static SuperByteBuffer transformed(PartialModel model, BlockState blockState, Direction facing, boolean roll90) {
        return CachedBuffers.partial(model, blockState)
                .center()
                .rotateYDegrees(AngleHelper.horizontalAngle(facing))
                .rotateXDegrees(AngleHelper.verticalAngle(facing) + 90)
                .rotateYDegrees(roll90 ? -90 : 0)
                .uncenter();
    }
}
