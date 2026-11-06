package com.jesz.createdieselgenerators.content.distillation;

import com.zurrtum.create.client.infrastructure.model.FluidTankModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel.UnbakedRoot;
import net.minecraft.world.level.block.state.BlockState;

/**
 * PORT-NOTE(26.1): was a NeoForge CTModel with cull ModelData. Create Fly's FluidTankModel does the same (connected
 * textures plus culling the inner faces between tank blocks), so this only supplies the distillation CT behaviour.
 */
public class DistillationTankModel extends FluidTankModel {
    public DistillationTankModel(BlockState state, UnbakedRoot unbaked) {
        super(state, unbaked, new DistillationTankCTBehavior());
    }
}
