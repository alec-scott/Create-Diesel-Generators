package com.jesz.createdieselgenerators.content.oil_barrel;

import com.zurrtum.create.infrastructure.fluids.FluidInventory;

import com.jesz.createdieselgenerators.port.CDGFluidUnits;

import com.zurrtum.create.foundation.fluid.FluidTank;


import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.level.storage.ValueInput;

import com.jesz.createdieselgenerators.CDGBlockEntityTypes;
import com.jesz.createdieselgenerators.CDGConfig;
import com.zurrtum.create.api.connectivity.ConnectivityHandler;
import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.Capabilities;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.zurrtum.create.infrastructure.fluids.FluidStack;

import java.util.List;
import java.util.Objects;

import static com.jesz.createdieselgenerators.content.oil_barrel.OilBarrelBlock.AXIS;

public class OilBarrelBlockEntity extends SmartBlockEntity implements IMultiBlockEntityContainer.Fluid, IHaveGoggleInformation {

    protected FluidInventory fluidCapability;
    protected FluidTank tankInventory;
    protected BlockPos controller;
    protected BlockPos lastKnownPos;
    protected boolean updateConnectivity;
    protected boolean updateCapability;
    protected int width;
    protected int height;

    private static final int SYNC_RATE = 8;
    protected int syncCooldown;
    protected boolean queuedSync;

    public OilBarrelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tankInventory = createInventory();
        updateConnectivity = false;
        updateCapability = false;
        height = 1;
        width = 1;
        refreshCapability();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {

    }

    protected FluidTank createInventory() {
        return new FluidTank(getCapacityMultiplier()) {
            @Override
            public void markDirty() {
                onFluidStackChanged(getFluid());
            }
        };
    }

    public void updateConnectivity() {
        updateConnectivity = false;
        if (level.isClientSide())
            return;
        if (!isController())
            return;
        ConnectivityHandler.formMulti(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (syncCooldown > 0) {
            syncCooldown--;
            if (syncCooldown == 0 && queuedSync)
                sendData();
        }

        if (lastKnownPos == null)
            lastKnownPos = getBlockPos();

        else if (!lastKnownPos.equals(worldPosition)) {
            onPositionChanged();
            return;
        }

        if (updateConnectivity)
            updateConnectivity();

        if (updateCapability) {
            updateCapability = false;
            refreshCapability();
        }
    }

    // PORT-NOTE(26.1): was OilBarrelBlock.onRemove (as Create Fly's FluidTankBlockEntity)
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        level.removeBlockEntity(pos);
        ConnectivityHandler.splitMulti(this);
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    @Override
    public boolean isController() {
        return controller == null || worldPosition.getX() == controller.getX()
                && worldPosition.getY() == controller.getY() && worldPosition.getZ() == controller.getZ();
    }

    @Override
    public void initialize() {
        super.initialize();
        sendData();
    }

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    protected void onFluidStackChanged(FluidStack newFluidStack) {
        if (!hasLevel())
            return;

        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
                    OilBarrelBlockEntity tankAt = ConnectivityHandler.partAt(getType(), level, pos);
                    if (tankAt == null)
                        continue;
                    level.updateNeighbourForOutputSignal(pos, tankAt.getBlockState()
                            .getBlock());
                }
            }
        }

        if (!level.isClientSide()) {
            setChanged();
            sendData();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public OilBarrelBlockEntity getControllerBE() {
        if (isController())
            return this;
        BlockEntity blockEntity = level.getBlockEntity(controller);
        if (blockEntity instanceof OilBarrelBlockEntity)
            return (OilBarrelBlockEntity) blockEntity;
        return null;
    }

    public void applyFluidTankSize(int blocks) {
        tankInventory.setCapacity(blocks * getCapacityMultiplier());
        int overflow = tankInventory.getFluid().getAmount() - tankInventory.getMaxAmountPerStack();
        if (overflow > 0)
            tankInventory.extract(tankInventory.getFluid(), overflow);
    }

    public void removeController(boolean keepFluids) {
        if (level.isClientSide())
            return;
        updateConnectivity = true;
        if (!keepFluids)
            applyFluidTankSize(1);
        controller = null;
        width = 1;
        height = 1;

        onFluidStackChanged(tankInventory.getFluid());

        refreshCapability();
        setChanged();
        sendData();
    }

    @Override
    public void sendData() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }
        super.sendData();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }
    @Override
    public void setController(BlockPos controller) {
        if (level.isClientSide() && !isVirtual())
            return;
        if (controller.equals(this.controller))
            return;
        this.controller = controller;

        refreshCapability();
        setChanged();
        sendData();
    }

    void refreshCapability() {
        fluidCapability = handlerForCapability();
        // PORT-NOTE(26.1): was invalidateCapabilities(); the Fabric storage is cached in a Create Fly behaviour
        var cached = getBehaviour(com.zurrtum.create.foundation.blockEntity.behaviour.CachedFluidInventoryBehaviour.TYPE);
        if (cached != null)
            cached.reset();
    }

    private FluidInventory handlerForCapability() {
        return isController() ? tankInventory
                : getControllerBE() != null ? getControllerBE().handlerForCapability() : new FluidTank(0);
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        OilBarrelBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return false;
        return containedFluidTooltip(tooltip, isPlayerSneaking, controllerBE.tankInventory);
    }
    @Override
    protected void read(ValueInput tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        BlockPos controllerBefore = controller;
        int prevSize = width;
        int prevHeight = height;

        updateConnectivity = tag.getBooleanOr("Uninitialized", false);
        lastKnownPos = tag.read("LastKnownPos", BlockPos.CODEC).orElse(null);
        controller = tag.read("Controller", BlockPos.CODEC).orElse(null);

        if (isController()) {
            width = tag.getIntOr("Size", 1);
            height = tag.getIntOr("Height", 1);
            tankInventory.setCapacity(getTotalTankSize() * getCapacityMultiplier());
            tankInventory.read(tag.childOrEmpty("TankContent"));
//            if (tankInventory.getSpace() < 0)
//                tankInventory.drain(-tankInventory.getSpace(), IFluidHandler.FluidAction.EXECUTE);
        }

        updateCapability = true;

        if (!clientPacket)
            return;

        boolean changeOfController =
                !Objects.equals(controllerBefore, controller);
        if (changeOfController || prevSize != width || prevHeight != height) {
            if (hasLevel())
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
            if (isController())
                tankInventory.setCapacity(getCapacityMultiplier() * getTotalTankSize());
            invalidateRenderBoundingBox();
        }
    }

    @Override
    protected void write(ValueOutput tag, boolean clientPacket) {
        super.write(tag, clientPacket);

        if (updateConnectivity)
            tag.putBoolean("Uninitialized", true);
        if (lastKnownPos != null)
            tag.store("LastKnownPos", BlockPos.CODEC, lastKnownPos);
        if (!isController())
            tag.store("Controller", BlockPos.CODEC, getController());
        if (isController()) {
            tankInventory.write(tag.child("TankContent"));
            tag.putInt("Size", width);
            tag.putInt("Height", height);
        }

        if (!clientPacket)
            return;
        if (queuedSync)
            tag.putBoolean("LazySync", true);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CDGBlockEntityTypes.OIL_BARREL.get(),
                (be, context) -> {
                    if (be.fluidCapability == null)
                        be.refreshCapability();
                    return be.fluidCapability;
                }
        );
    }

    public int getTotalTankSize() {
        return width * width * height;
    }

    public static int getCapacityMultiplier() {
        return AllConfigs.server().fluids.fluidTankCapacity.get() * CDGFluidUnits.BUCKET;
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    @Override
    public void notifyMultiUpdated() {
        onFluidStackChanged(tankInventory.getFluid());
        setChanged();
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        return getBlockState().getValue(AXIS);
    }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        return CDGConfig.MAX_OIL_BARREL_LENGTH_PER_WIDTH.get() * width;
    }

    @Override
    public int getMaxWidth() {
        return CDGConfig.MAX_OIL_BARREL_WIDTH.get();
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    @Override
    public int getTankSize(int tank) {
        return getCapacityMultiplier();
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        applyFluidTankSize(blocks);
    }

    @Override
    public FluidTank getTank(int tank) {
        return tankInventory;
    }

    @Override
    public FluidStack getFluid(int tank) {
        return tankInventory.getFluid()
                .copy();
    }
}
