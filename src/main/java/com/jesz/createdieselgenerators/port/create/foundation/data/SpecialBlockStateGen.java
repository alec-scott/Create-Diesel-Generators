package com.jesz.createdieselgenerators.port.create.foundation.data;

import com.jesz.createdieselgenerators.port.registrate.providers.DataGenContext;
import com.jesz.createdieselgenerators.port.registrate.providers.RegistrateBlockstateProvider;
import com.jesz.createdieselgenerators.port.porting_lib.models.generators.ModelFile;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class SpecialBlockStateGen {
    protected int horizontalAngle(Direction direction) { return (int) direction.toYRot(); }
    protected net.minecraft.world.level.block.state.properties.Property<?>[] getIgnoredProperties() { return new net.minecraft.world.level.block.state.properties.Property<?>[0]; }
    protected abstract int getXRotation(BlockState state);
    protected abstract int getYRotation(BlockState state);
    public abstract <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, BlockState state);
    public <T extends Block> void generate(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov) {
    }
}
