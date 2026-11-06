package com.jesz.createdieselgenerators.content.distillation;

import com.zurrtum.create.content.fluids.tank.FluidTankBlock;
import com.jesz.createdieselgenerators.port.create.foundation.data.AssetLookup;
import com.jesz.createdieselgenerators.port.create.foundation.data.SpecialBlockStateGen;
import com.jesz.createdieselgenerators.port.registrate.providers.DataGenContext;
import com.jesz.createdieselgenerators.port.registrate.providers.RegistrateBlockstateProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.jesz.createdieselgenerators.port.porting_lib.models.generators.ModelFile;

public class DistillationTankGenerator extends SpecialBlockStateGen {
    @Override
    protected int getXRotation(BlockState state) {
        return 0;
    }

    @Override
    protected int getYRotation(BlockState state) {
        return 0;
    }

    @Override
    public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, BlockState state) {
        Boolean top = state.getValue(DistillationTankBlock.TOP);
        Boolean bottom = state.getValue(DistillationTankBlock.BOTTOM);
        FluidTankBlock.Shape shape = state.getValue(DistillationTankBlock.SHAPE);

        String modelName = "";
        if (bottom)
            modelName = "bottom";
        else
            if (shape == FluidTankBlock.Shape.WINDOW)
                modelName = "window";
            else if (shape == FluidTankBlock.Shape.WINDOW_NW)
                modelName = "window_nw";
            else if (shape == FluidTankBlock.Shape.WINDOW_SW)
                modelName = "window_sw";
            else if (shape == FluidTankBlock.Shape.WINDOW_NE)
                modelName = "window_ne";
            else if (shape == FluidTankBlock.Shape.WINDOW_SE)
                modelName = "window_se";

        return AssetLookup.partialBaseModel(ctx, prov, modelName);
    }
}
