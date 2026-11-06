package com.jesz.createdieselgenerators.client.render;

import com.jesz.createdieselgenerators.CDGPartialModels;
import com.jesz.createdieselgenerators.content.diesel_engine.EngineUpgrades;
import com.jesz.createdieselgenerators.content.diesel_engine.huge.HugeDieselEngineBlock;
import com.jesz.createdieselgenerators.content.diesel_engine.huge.HugeDieselEngineBlockEntity;
import com.jesz.createdieselgenerators.content.diesel_engine.modular.ModularDieselEngineBlock;
import com.jesz.createdieselgenerators.content.diesel_engine.modular.ModularDieselEngineBlockEntity;
import com.jesz.createdieselgenerators.content.diesel_engine.normal.DieselEngineBlockEntity;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.catnip.render.SuperByteBufferRenderState;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

import static com.jesz.createdieselgenerators.content.diesel_engine.normal.DieselEngineBlock.FACING;

/** PORT-NOTE(26.1): was EngineUpgrades.render / renderPartial (client code moved out of the common upgrade classes). */
public final class EngineUpgradeRenders {
    private EngineUpgradeRenders() {
    }

    public static void extract(BlockEntity be, EngineUpgrades upgrade, int light, List<SuperByteBufferRenderState> out) {
        if (upgrade == EngineUpgrades.SILENCER)
            extractPartial(be, CDGPartialModels.ENGINE_SILENCER, CDGPartialModels.ENGINE_SILENCER_VERTICAL,
                    CDGPartialModels.MODULAR_ENGINE_SILENCER, CDGPartialModels.HUGE_ENGINE_SILENCER, light, out);
        else if (upgrade == EngineUpgrades.TURBOCHARGER)
            extractPartial(be, CDGPartialModels.ENGINE_TURBOCHARGER, CDGPartialModels.ENGINE_TURBOCHARGER_VERTICAL,
                    CDGPartialModels.MODULAR_TURBOCHARGER, CDGPartialModels.ENGINE_TURBOCHARGER, light, out);
    }

    private static void extractPartial(BlockEntity be, PartialModel normalModel, PartialModel normalVerticalModel,
                                       PartialModel modularModel, PartialModel hugeModel, int light,
                                       List<SuperByteBufferRenderState> out) {
        if (be instanceof DieselEngineBlockEntity) {
            Direction facing = be.getBlockState().getValue(FACING);
            if (facing.getAxis() == Direction.Axis.Y) {
                out.add(CachedBuffers.partial(normalVerticalModel, be.getBlockState())
                        .center()
                        .rotateYDegrees(facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 270 : 180)
                        .uncenter()
                        .light(light)
                        .extractRenderState());
            } else {
                out.add(CachedBuffers.partial(normalModel, be.getBlockState())
                        .center()
                        .rotateYDegrees(facing.toYRot())
                        .uncenter()
                        .light(light)
                        .extractRenderState());
            }
        } else if (be instanceof ModularDieselEngineBlockEntity) {
            Direction facing = be.getBlockState().getValue(ModularDieselEngineBlock.FACING);
            out.add(CachedBuffers.partial(modularModel, be.getBlockState())
                    .center()
                    .rotateYDegrees(facing.toYRot())
                    .uncenter()
                    .light(light)
                    .extractRenderState());
        } else if (be instanceof HugeDieselEngineBlockEntity) {
            Direction facing = be.getBlockState().getValue(HugeDieselEngineBlock.FACING);
            if (facing.getAxis() == Direction.Axis.Y) {
                out.add(CachedBuffers.partial(hugeModel, be.getBlockState())
                        .center().rotateZDegrees(90)
                        .rotateYDegrees(facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 270 : 90)
                        .uncenter()
                        .light(light)
                        .extractRenderState());
            } else {
                out.add(CachedBuffers.partial(hugeModel, be.getBlockState())
                        .center()
                        .rotateYDegrees(facing.getAxis() == Direction.Axis.X ? (facing.toYRot()) : (facing.toYRot()) + 180)
                        .uncenter()
                        .light(light)
                        .extractRenderState());
            }
        }
    }
}
