package com.jesz.createdieselgenerators.content.bulk_fermenter;

import net.minecraft.world.level.ScheduledTickAccess;

import net.minecraft.util.RandomSource;

import com.jesz.createdieselgenerators.CDGBlockEntityTypes;
import com.zurrtum.create.api.connectivity.ConnectivityHandler;
import com.zurrtum.create.content.equipment.wrench.IWrenchable;
import com.zurrtum.create.foundation.block.IBE;
import com.zurrtum.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BulkFermenterBlock extends Block implements IBE<BulkFermenterBlockEntity>, IWrenchable {
    public BulkFermenterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.DOWN && neighbourState.getBlock() != this)
            withBlockEntityDo(level, pos, BulkFermenterBlockEntity::updateHeat);
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moved) {
        if (oldState.getBlock() == state.getBlock())
            return;
        if (moved)
            return;
        withBlockEntityDo(world, pos, BulkFermenterBlockEntity::updateConnectivity);
        withBlockEntityDo(world, pos, BulkFermenterBlockEntity::updateHeat);
    }
    @Override
    public Class<BulkFermenterBlockEntity> getBlockEntityClass() {
        return BulkFermenterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BulkFermenterBlockEntity> getBlockEntityType() {
        return CDGBlockEntityTypes.BULK_FERMENTER.get();
    }
    // PORT-NOTE(26.1): the batch-placement sound silencing (NeoForge persistent data, DeferredSoundType) is dropped

}
