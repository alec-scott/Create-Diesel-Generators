package com.jesz.createdieselgenerators.content.diesel_engine.modular;

import com.jesz.createdieselgenerators.CDGPartialModels;
import com.jesz.createdieselgenerators.client.render.EngineUpgradeRenders;
import com.jesz.createdieselgenerators.client.render.PartsKineticRenderer;
import com.jesz.createdieselgenerators.content.diesel_engine.normal.DieselEngineRenderer;
import com.zurrtum.create.client.catnip.render.CachedBuffers;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import java.util.Objects;

import static com.jesz.createdieselgenerators.content.diesel_engine.modular.ModularDieselEngineBlock.FACING;

/** PORT-NOTE(26.1): extract/submit (see PartsKineticRenderer). */
public class ModularDieselEngineRenderer extends PartsKineticRenderer<ModularDieselEngineBlockEntity> {
    private static final PartialModel[] PISTONS = {
            CDGPartialModels.MODULAR_ENGINE_PISTONS_0, CDGPartialModels.MODULAR_ENGINE_PISTONS_1, CDGPartialModels.MODULAR_ENGINE_PISTONS_2,
            CDGPartialModels.MODULAR_ENGINE_PISTONS_3, CDGPartialModels.MODULAR_ENGINE_PISTONS_4};

    public ModularDieselEngineRenderer(BlockEntityRendererProvider.Context context) {
        super(context, true);
    }

    @Override
    protected void extractParts(ModularDieselEngineBlockEntity be, State state, float partialTicks) {
        int frame = DieselEngineRenderer.pistonFrame(DieselEngineRenderer.angleStep(be));

        ModularDieselEngineBlockEntity controller = be.getControllerBE();
        EngineUpgradeRenders.extract(be, Objects.requireNonNullElse(controller, be).getUpgrade(), state.lightCoords, state.parts);

        state.parts.add(CachedBuffers.partial(PISTONS[frame], be.getBlockState()).center()
                .rotateYDegrees(be.getBlockState().getValue(FACING).toYRot()).uncenter()
                .light(state.lightCoords)
                .extractRenderState());
    }
}
