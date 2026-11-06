package com.jesz.createdieselgenerators.content.diesel_engine.normal;

import com.jesz.createdieselgenerators.CDGPartialModels;
import com.jesz.createdieselgenerators.client.render.EngineUpgradeRenders;
import com.jesz.createdieselgenerators.client.render.PartsKineticRenderer;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.content.kinetics.base.KineticBlockEntityRenderer;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

import static com.jesz.createdieselgenerators.content.diesel_engine.normal.DieselEngineBlock.FACING;

/** PORT-NOTE(26.1): extract/submit (see PartsKineticRenderer); upgrade, piston frames and the shaft. */
public class DieselEngineRenderer extends PartsKineticRenderer<DieselEngineBlockEntity> {
    private static final PartialModel[] HORIZONTAL = {
            CDGPartialModels.ENGINE_PISTONS_0, CDGPartialModels.ENGINE_PISTONS_1, CDGPartialModels.ENGINE_PISTONS_2,
            CDGPartialModels.ENGINE_PISTONS_3, CDGPartialModels.ENGINE_PISTONS_4};
    private static final PartialModel[] VERTICAL = {
            CDGPartialModels.ENGINE_PISTONS_VERTICAL_0, CDGPartialModels.ENGINE_PISTONS_VERTICAL_1, CDGPartialModels.ENGINE_PISTONS_VERTICAL_2,
            CDGPartialModels.ENGINE_PISTONS_VERTICAL_3, CDGPartialModels.ENGINE_PISTONS_VERTICAL_4};

    public DieselEngineRenderer(BlockEntityRendererProvider.Context context) {
        super(context, true);
    }

    /** Upstream's frame selection: 10 steps per turn mirrored onto 5 piston frames. */
    public static int pistonFrame(int angle) {
        return switch (angle) {
            case 9, 2 -> 1;
            case 8, 3 -> 2;
            case 7, 4 -> 3;
            case 6, 5 -> 4;
            default -> 0;
        };
    }

    public static int angleStep(com.zurrtum.create.content.kinetics.base.KineticBlockEntity be) {
        return (int) (Math.abs(KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), KineticBlockEntityRenderer.getRotationAxisOf(be)) * 180 / Math.PI) * 3 % 360) / 36;
    }

    @Override
    protected void extractParts(DieselEngineBlockEntity be, State state, float partialTicks) {
        int frame = pistonFrame(angleStep(be));
        EngineUpgradeRenders.extract(be, be.upgrade, state.lightCoords, state.parts);

        Direction facing = be.getBlockState().getValue(FACING);
        if (facing.getAxis().isHorizontal()) {
            state.parts.add(CachedBuffers.partial(HORIZONTAL[frame], be.getBlockState()).center()
                    .rotateYDegrees(facing.toYRot()).uncenter()
                    .light(state.lightCoords).extractRenderState());
        } else {
            state.parts.add(CachedBuffers.partial(VERTICAL[frame], be.getBlockState()).center()
                    .rotateYDegrees(facing == Direction.DOWN ? 180 : 270)
                    .rotateZDegrees(facing == Direction.DOWN ? 180 : 0).uncenter()
                    .light(state.lightCoords).extractRenderState());
        }
    }
}
