package com.jesz.createdieselgenerators.content.concrete;



import com.jesz.createdieselgenerators.CDGBlockEntityTypes;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.content.equipment.wrench.IWrenchable;
import com.zurrtum.create.content.fluids.FluidPropagator;
import com.zurrtum.create.content.fluids.pipes.EncasedPipeBlock;
import com.zurrtum.create.content.fluids.pipes.FluidPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class ConcreteEncasedFluidPipeBlock extends EncasedPipeBlock {
    public ConcreteEncasedFluidPipeBlock(Properties properties) {
        super(properties, AllBlocks.ANDESITE_CASING);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (context.getPlayer() != null && !context.getPlayer().isCreative())
            return InteractionResult.PASS;
        context.getLevel().setBlock(context.getClickedPos(), state.cycle(PipeBlock.PROPERTY_BY_DIRECTION.get(context.getClickedFace())), 3);
        IWrenchable.playRotateSound(context.getLevel(), context.getClickedPos());
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource r) {
        FluidPropagator.propagateChangedPipe(world, pos, state);
    }

    @Override
    public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
        return CDGBlockEntityTypes.CONCRETE_ENCASED_FLUID_PIPE.get();
    }
}
