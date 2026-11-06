package com.jesz.createdieselgenerators.content.canister;

import com.zurrtum.create.foundation.fluid.FluidTank;

import com.jesz.createdieselgenerators.CDGConfig;
import com.zurrtum.create.api.behaviour.spouting.BlockSpoutingBehaviour;
import com.zurrtum.create.content.fluids.spout.SpoutBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.zurrtum.create.infrastructure.fluids.FluidStack;

public class SpoutCanisterFilling implements BlockSpoutingBehaviour {
    @Override
    public int fillBlock(Level level, BlockPos pos, SpoutBlockEntity spout, FluidStack availableFluid, boolean simulate) {
        if (!CDGConfig.CANISTER_SPOUT_FILLING.get())
            return 0;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CanisterBlockEntity){
            FluidTank handler = ((CanisterBlockEntity) blockEntity).getTank();
            if (FluidStack.areFluidsAndComponentsEqual(handler.getFluid(), availableFluid) || handler.getFluid().isEmpty())
                return simulate ? handler.countSpace(availableFluid) : handler.insert(availableFluid);
        }
        return 0;
    }
}
