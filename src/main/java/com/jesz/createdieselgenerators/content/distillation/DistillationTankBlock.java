package com.jesz.createdieselgenerators.content.distillation;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.redstone.Orientation;

import net.minecraft.world.level.ScheduledTickAccess;

import net.minecraft.util.RandomSource;

import com.jesz.createdieselgenerators.CDGBlockEntityTypes;
import com.jesz.createdieselgenerators.CDGItems;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.AllSoundEvents;
import com.zurrtum.create.api.connectivity.ConnectivityHandler;
import com.zurrtum.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.zurrtum.create.content.equipment.wrench.IWrenchable;
import com.zurrtum.create.content.fluids.tank.FluidTankBlock;
import com.zurrtum.create.content.fluids.tank.FluidTankBlockEntity;
import com.zurrtum.create.content.schematics.requirement.ItemRequirement;
import com.zurrtum.create.foundation.block.IBE;
import com.zurrtum.create.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.Capabilities;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

import static com.jesz.createdieselgenerators.CDGItems.DISTILLATION_CONTROLLER;

public class DistillationTankBlock extends Block implements IBE<DistillationTankBlockEntity>, IWrenchable, SpecialBlockItemRequirement {
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
    public static final EnumProperty<FluidTankBlock.Shape> SHAPE = EnumProperty.create("shape", FluidTankBlock.Shape.class);

    public DistillationTankBlock(Properties properties) {
        super(properties);
    }

    public static boolean isTank(BlockState state) {
        return state.getBlock() instanceof DistillationTankBlock;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {

        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof DistillationTankBlockEntity dtbe){
            int width = dtbe.getControllerBE().getWidth();
            BlockPos pos = dtbe.getController();
            DistillationTankBlockEntity controllerBE = dtbe.getControllerBE();
            FluidStack stackInTank = controllerBE == null ? FluidStack.EMPTY : controllerBE.tankInventory.getFluid().copy();

            for (int x = 0; x < width; x++) {
                for (int z = 0; z < width; z++) {
                    context.getLevel().setBlockAndUpdate(pos.offset(x, 0, z), AllBlocks.FLUID_TANK.defaultBlockState());
                    context.getLevel().updateNeighborsAt(pos.offset(x, 0, z), AllBlocks.FLUID_TANK);
                    if (context.getLevel().isClientSide()) {
                        for (int i = 0; i < 30; i++) {
                            Vec3 offset = VecHelper.offsetRandomly(VecHelper.getCenterOf(pos.offset(x, 0, z)), context.getLevel().getRandom(), .3f);
                            Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, context.getLevel().getRandom(), .1f);
                            context.getLevel().addParticle(new ItemParticleOption(ParticleTypes.ITEM, DISTILLATION_CONTROLLER.asStack()), offset.x(), offset.y(),
                                    offset.z(), motion.x(), motion.y(), motion.z());
                        }
                    }
                }
            }
            AllSoundEvents.WRENCH_REMOVE.playAt(context.getLevel(), pos.getX() + (double) width / 2, pos.getY() + 0.5, pos.getZ() + (double) width / 2, 2f, 1f, false);
            if (!stackInTank.isEmpty() && context.getLevel().getBlockEntity(pos) instanceof FluidTankBlockEntity be){
                be.getTankInventory().insert(stackInTank);
            }
            if (!context.getPlayer().isCreative())
                context.getPlayer().getInventory().placeItemBackInInventory(new ItemStack(DISTILLATION_CONTROLLER.get(), width * width));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.DOWN && neighbourState.getBlock() != this)
            withBlockEntityDo(level, pos, DistillationTankBlockEntity::updateTemperature);
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean p_60514_) {
        super.neighborChanged(state, level, pos, block, orientation, p_60514_);
        withBlockEntityDo(level, pos, DistillationTankBlockEntity::updateVerticalMulti);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP, BOTTOM, SHAPE);
    }
    @Override
    public Class<DistillationTankBlockEntity> getBlockEntityClass() {
        return DistillationTankBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DistillationTankBlockEntity> getBlockEntityType() {
        return CDGBlockEntityTypes.DISTILLATION_TANK.get();
    }
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        withBlockEntityDo(context.getLevel(), context.getClickedPos(), DistillationTankBlockEntity::toggleWindows);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(AllBlocks.FLUID_TANK);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (oldState.getBlock() == state.getBlock())
            return;
        if (moved)
            return;
        withBlockEntityDo(level, pos, DistillationTankBlockEntity::updateConnectivity);
        withBlockEntityDo(level, pos, DistillationTankBlockEntity::updateVerticalMulti);
    }
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE)
            return state;
        boolean x = mirror == Mirror.FRONT_BACK;
        return switch (state.getValue(SHAPE)) {
            case WINDOW_NE -> state.setValue(SHAPE, x ? FluidTankBlock.Shape.WINDOW_NW : FluidTankBlock.Shape.WINDOW_SE);
            case WINDOW_NW -> state.setValue(SHAPE, x ? FluidTankBlock.Shape.WINDOW_NE : FluidTankBlock.Shape.WINDOW_SW);
            case WINDOW_SE -> state.setValue(SHAPE, x ? FluidTankBlock.Shape.WINDOW_SW : FluidTankBlock.Shape.WINDOW_NE);
            case WINDOW_SW -> state.setValue(SHAPE, x ? FluidTankBlock.Shape.WINDOW_SE : FluidTankBlock.Shape.WINDOW_NW);
            default -> state;
        };
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        for (int i = 0; i < rotation.ordinal(); i++)
            state = rotateOnce(state);
        return state;
    }

    private BlockState rotateOnce(BlockState state) {
        return switch (state.getValue(SHAPE)) {
            case WINDOW_NE -> state.setValue(SHAPE, FluidTankBlock.Shape.WINDOW_SE);
            case WINDOW_NW -> state.setValue(SHAPE, FluidTankBlock.Shape.WINDOW_NE);
            case WINDOW_SE -> state.setValue(SHAPE, FluidTankBlock.Shape.WINDOW_SW);
            case WINDOW_SW -> state.setValue(SHAPE, FluidTankBlock.Shape.WINDOW_NW);
            default -> state;
        };
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity blockEntity) {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(AllBlocks.FLUID_TANK));
        list.add(DISTILLATION_CONTROLLER.asStack());
        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, list);
    }
}
