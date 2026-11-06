package com.jesz.createdieselgenerators.content.diesel_engine.modular;

import com.jesz.createdieselgenerators.port.create.foundation.data.SpecialBlockStateGen;
import com.jesz.createdieselgenerators.port.registrate.providers.DataGenContext;
import com.jesz.createdieselgenerators.port.registrate.providers.RegistrateBlockstateProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.jesz.createdieselgenerators.port.porting_lib.models.generators.ModelFile;

public class ModularDieselEngineGenerator extends SpecialBlockStateGen {

    @Override
    protected int getXRotation(BlockState state) {
        return 0;
    }

    @Override
    protected int getYRotation(BlockState state) {
        Direction direction = state.getValue(ModularDieselEngineBlock.FACING);
        return (int) (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? direction.toYRot() : direction.getOpposite().toYRot());
    }

    @Override
    public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
                                                BlockState state) {
        return state.getValue(ModularDieselEngineBlock.PIPE) ? prov.models().getExistingFile(prov.modLoc("block/modular_diesel_engine/block_pipe"))
                : prov.models().getExistingFile(prov.modLoc("block/modular_diesel_engine/block"));
    }
}
