package com.jesz.createdieselgenerators.content.diesel_engine.modular;

import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollOptionBehaviour;

import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;

import com.zurrtum.create.foundation.fluid.FluidTank;

import com.zurrtum.create.infrastructure.fluids.FluidInventory;

import com.jesz.createdieselgenerators.port.CDGFluidUnits;


import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.level.storage.ValueInput;

import com.jesz.createdieselgenerators.CDGBlockEntityTypes;
import com.jesz.createdieselgenerators.CDGBlocks;
import com.jesz.createdieselgenerators.CDGConfig;
import com.jesz.createdieselgenerators.content.diesel_engine.EngineUpgrades;
import com.jesz.createdieselgenerators.content.diesel_engine.IEngine;
import com.jesz.createdieselgenerators.content.diesel_engine.normal.DieselEngineBlock;
import com.jesz.createdieselgenerators.fuel_type.FuelType;
import com.zurrtum.create.api.connectivity.ConnectivityHandler;
import com.zurrtum.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.zurrtum.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.zurrtum.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.Capabilities;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static com.jesz.createdieselgenerators.content.diesel_engine.modular.ModularDieselEngineBlock.FACING;
import static com.jesz.createdieselgenerators.content.diesel_engine.modular.ModularDieselEngineBlock.PIPE;

public class ModularDieselEngineBlockEntity extends GeneratingKineticBlockEntity implements IEngine, IMultiBlockEntityContainer.Fluid, IHaveGoggleInformation {
    public ServerScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> movementDirection;
    protected int length = 1;
    @NotNull
    protected EngineUpgrades upgrade = EngineUpgrades.EMPTY;
    protected FluidInventory fluidCapability;
    // PORT-NOTE(26.1): Create Fly tank, 1000 mB in droplets
    protected FluidTank tankInventory = new FluidTank(CDGFluidUnits.BUCKET) {
        @Override
        public void markDirty() {
            sendData();
        }
    };
    protected BlockPos controller;
    protected BlockPos lastKnownPos;
    protected boolean updateConnectivity = false;
    protected boolean updateCapability = false;
    private float lastCapacity;
    private float lastSpeed;
    public int analogSignal = 0;
    private float fuelDebt = 0f;
    private boolean signalChanged = false;
    private FuelType cachedFuelType = FuelType.EMPTY;
    private FluidStack lastCachedFluid = FluidStack.EMPTY;
    private float cachedFuelSpeed = 0f;
    private float cachedFuelCapacity = 0f;
    private float cachedBurnRate = 0f;

    public ModularDieselEngineBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public void resetConnectivity() {
        updateConnectivity = true;
        controller = null;
        length = 1;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
        movementDirection = new ServerScrollOptionBehaviour<>(WindmillBearingBlockEntity.RotationDirection.class, this);
        movementDirection.withCallback(this::onDirectionChanged);

        behaviours.add(movementDirection);
        super.addBehaviours(behaviours);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                CDGBlockEntityTypes.MODULAR_DIESEL_ENGINE.get(),
                (be, side) -> {
                    // PORT-NOTE(26.1): side-less through the shim; upstream exposed only the top of PIPE blocks
                    if (be.fluidCapability == null)
                        be.refreshCapability();
                    return be.fluidCapability;
                });
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (!isController()) {
            ModularDieselEngineBlockEntity controller = getControllerBE();
            if (controller == null)
                return false;
            return controller.addToGoggleTooltip(tooltip, isPlayerSneaking);
        }
        return containedFluidTooltip(tooltip, isPlayerSneaking, fluidCapability);
    }

    public void onDirectionChanged(int v) {
        ModularDieselEngineBlockEntity controller = getControllerBE();
        if (controller != null) {
            controller.movementDirection.setValue(v);
            controller.reActivateSource = true;

            for (int i = 0; i < controller.getHeight(); i++) {
                if (level.getBlockEntity(controller.getBlockPos().relative(controller.getBlockState().getValue(FACING).getAxis(), i)) instanceof ModularDieselEngineBlockEntity be && be.movementDirection.getValue() != v)
                    be.movementDirection.setValue(v);
            }
        }
    }

    @Override
    public float calculateAddedStressCapacity() {
        float baseFuelSpeed = getFuelSpeed();
        float speed = upgrade.getSpeed(baseFuelSpeed, this) * getThrottle();
        float capacity = upgrade.getCapacity(getFuelCapacity() * getHeight() * baseFuelSpeed, this) / Math.max(0.01f, speed);
        lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    public float getGeneratedSpeed() {
        if (!enabled() || !isController()) return 0;
        float throttle = getThrottle();
        if (throttle == 0f) return 0;
        return convertToDirection(
                (movementDirection.getValue() == 1 ? -1 : 1)
                        * upgrade.getSpeed(getFuelSpeed(), this)
                        * throttle,
                getBlockState().getValue(ModularDieselEngineBlock.FACING));
    }

    @Override
    public void tick() {
        super.tick();

        if (updateCapability) {
            updateCapability = false;
            refreshCapability();
        }
        if (updateConnectivity)
            updateConnectivity();

        if (!isController()) {
            if (upgrade == EngineUpgrades.EMPTY)
                return;
            ModularDieselEngineBlockEntity controller = getControllerBE();

            if (controller.upgrade == EngineUpgrades.EMPTY)
                controller.upgrade = upgrade;
            else
                Block.popResource(level, getBlockPos(), upgrade.getItem());
            upgrade = EngineUpgrades.EMPTY;

            return;
        }

        if (signalChanged) {
            signalChanged = false;
            reActivateSource = true;
            setChanged();
            sendData();
        }

        if (!level.isClientSide()) {
            float currentCapacity = 0;
            float currentSpeed = 0;
            if (validFS()) {
                float throttle = getThrottle();
                currentSpeed = getGeneratedSpeed();
                currentCapacity = upgrade.getCapacity(
                        getFuelCapacity() * getHeight() * (1 / Math.max(upgrade.getSpeed(getFuelSpeed(), this) * throttle, 0.001f))
                                * upgrade.getSpeed(getFuelSpeed(), this) * throttle, this);
            }

            if (lastSpeed != currentSpeed || lastCapacity != currentCapacity) {
                reActivateSource = true;
                lastSpeed = currentSpeed;
                lastCapacity = currentCapacity;
            }
        }

        if (isOverStressed())
            return;

        fuelDebt = IEngine.burnFuel(tankInventory, fuelDebt + (length * cachedBurnRate) * getFuelThrottle() * CDGFluidUnits.MB);
    }

    // For the client sound behaviour
    public int getLength() {
        return length;
    }

    void refreshCapability() {
        fluidCapability = handlerForCapability();
        // PORT-NOTE(26.1): was invalidateCapabilities(); the Fabric storage is cached in a Create Fly behaviour
        var cached = getBehaviour(com.zurrtum.create.foundation.blockEntity.behaviour.CachedFluidInventoryBehaviour.TYPE);
        if (cached != null)
            cached.reset();
    }
    private FluidInventory handlerForCapability() {
        return isController() ? (tankInventory)
                : ((getControllerBE() != null) ? getControllerBE().handlerForCapability() : new FluidTank(0));
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
    public SmartBlockEntity self() {
        return this;
    }

    @Override
    public FluidInventory getTank() {
        return tankInventory;
    }

    @Override
    public EngineUpgrades getUpgrade() {
        return upgrade;
    }

    @Override
    public void setUpgrade(EngineUpgrades upgrade) {
        this.upgrade = upgrade;
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    @Override
    public ModularDieselEngineBlockEntity getControllerBE() {
        if (isController() || !hasLevel())
            return this;
        BlockEntity be = level.getBlockEntity(controller);
        if (be instanceof ModularDieselEngineBlockEntity)
            return (ModularDieselEngineBlockEntity) be;
        return null;
    }

    @Override
    public boolean isController() {
        return controller == null || controller.equals(worldPosition);
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

    @Override
    public void removeController(boolean keepContents) {
        if (level.isClientSide())
            return;
        updateConnectivity = true;
        controller = null;
        length = 1;
        reActivateSource = true;

        refreshCapability();
        setChanged();
        sendData();
    }

    @Override
    protected void read(ValueInput compound, boolean clientPacket) {
        super.read(compound, clientPacket);

        BlockPos controllerBefore = controller;
        int prevHeight = length;

        updateConnectivity = compound.getBooleanOr("Uninitialized", false);
        Identifier upgradeId = Identifier.tryParse(compound.getStringOr("Upgrade", ""));
        upgrade = upgradeId == null ? EngineUpgrades.EMPTY : EngineUpgrades.get(upgradeId);
        lastKnownPos = compound.read("LastKnownPos", BlockPos.CODEC).orElse(null);
        controller = compound.read("Controller", BlockPos.CODEC).orElse(null);

        if (isController()) {
            length = compound.getIntOr("Height", 1);
            tankInventory.read(compound.childOrEmpty("TankContent"));
            analogSignal = compound.getIntOr("AnalogSignal", 0);
            fuelDebt = 0f;
            invalidateFuelCache();
        }

        updateCapability = true;

        if (!clientPacket)
            return;

        boolean changeOfController = !Objects.equals(controllerBefore, controller);
        if (changeOfController || prevHeight != length) {
            if (hasLevel())
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
            if (isController())
                tankInventory.setCapacity(CDGFluidUnits.BUCKET);
            invalidateRenderBoundingBox();
        }
    }

    @Override
    protected void write(ValueOutput compound, boolean clientPacket) {
        super.write(compound, clientPacket);

        if (updateConnectivity)
            compound.putBoolean("Uninitialized", true);
        if (lastKnownPos != null)
            compound.store("LastKnownPos", BlockPos.CODEC, lastKnownPos);
        if (!isController())
            compound.store("Controller", BlockPos.CODEC, controller);
        if (isController()) {
            compound.putString("Upgrade", upgrade.getId().toString());
            tankInventory.write(compound.child("TankContent"));
            compound.putInt("Height", length);
            compound.putInt("AnalogSignal", analogSignal);
        }
    }

    // PORT-NOTE(26.1): was ModularDieselEngineBlock.onRemove (drop the upgrade, split the multiblock)
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (upgrade != EngineUpgrades.EMPTY)
            Block.popResource(level, pos, upgrade.getItem());
        level.removeBlockEntity(pos);
        ConnectivityHandler.splitMulti(this);
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    @Override
    public void notifyMultiUpdated() {
        reActivateSource = true;
        setChanged();
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        return getBlockState().getValue(FACING).getAxis();
    }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        return 21;
    }

    @Override
    public int getMaxWidth() {
        return 1;
    }

    @Override
    public int getHeight() {
        return length;
    }

    @Override
    public void setHeight(int height) {
        length = height;
    }

    @Override
    public int getWidth() {
        return 1;
    }

    @Override
    public void setWidth(int width) {

    }

    @Override
    public boolean enabled() {
        if (!IEngine.super.enabled())
            return false;
        if (CDGConfig.ANALOG_SPEED_CONTROL.get())
            return true;
        if (!CDGConfig.ENGINES_DISABLED_WITH_REDSTONE.get())
            return true;
        for (int i = 1; i < length; i++) {
            BlockState state = level.getBlockState(getBlockPos().relative(getMainConnectionAxis(), i));
            if (CDGBlocks.MODULAR_DIESEL_ENGINE.has(state))
                if (state.getValue(DieselEngineBlock.POWERED))
                    return false;
        }
        return true;
    }

    @Override
    public int getAnalogSignal() {
        return analogSignal;
    }

    public void setAnalogSignal(int newSignal) { analogSignal = newSignal; }
    public void setSignalChanged(boolean newSignal) { signalChanged = newSignal; }

    @Override public FuelType getCachedFuelType() { return cachedFuelType; }
    @Override public void setCachedFuelType(FuelType t) { cachedFuelType = t; }
    @Override public FluidStack getLastCachedFluid() { return lastCachedFluid; }
    @Override public void setLastCachedFluid(FluidStack f) { lastCachedFluid = f; }
    @Override public float getCachedFuelSpeed() { return cachedFuelSpeed; }
    @Override public void setCachedFuelSpeed(float s) { cachedFuelSpeed = s; }
    @Override public float getCachedFuelCapacity() { return cachedFuelCapacity; }
    @Override public void setCachedFuelCapacity(float c) { cachedFuelCapacity = c; }
    @Override public float getCachedBurnRate() { return cachedBurnRate; }
    @Override public void setCachedBurnRate(float r) { cachedBurnRate = r; }
}

