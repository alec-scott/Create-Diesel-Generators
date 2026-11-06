package com.jesz.createdieselgenerators.content.diesel_engine.normal;

import com.jesz.createdieselgenerators.CDGBlockEntityTypes;
import com.jesz.createdieselgenerators.content.diesel_engine.EngineUpgrades;
import com.jesz.createdieselgenerators.content.diesel_engine.IEngine;
import com.jesz.createdieselgenerators.fuel_type.FuelType;
import com.jesz.createdieselgenerators.port.CDGFluidUnits;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.Capabilities;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.zurrtum.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollOptionBehaviour;
import com.zurrtum.create.infrastructure.fluids.FluidInventory;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

import static com.jesz.createdieselgenerators.content.diesel_engine.normal.DieselEngineBlock.FACING;

/**
 * PORT-NOTE(26.1): the rotation direction is a server scroll option (its value box is a client behaviour, see
 * {@code client/CDGBlockEntityBehaviours}); the tank holds 1000 mB in droplets; fuel burn rates (mB per tick) are
 * drained in droplets. Kinetic goggle stats and the engine sound are client behaviours; this keeps the fluid lines.
 */
public class DieselEngineBlockEntity extends GeneratingKineticBlockEntity implements IEngine, IHaveGoggleInformation {
    public ServerScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> movementDirection;

    public EngineUpgrades upgrade = EngineUpgrades.EMPTY;
    SmartFluidTankBehaviour tank;
    private float lastCapacity;
    private float lastSpeed;
    private int analogSignal = 0;
    private boolean signalChanged = false;
    private float fuelDebt = 0f;
    private FuelType cachedFuelType = FuelType.EMPTY;
    private FluidStack lastCachedFluid = FluidStack.EMPTY;
    private float cachedFuelSpeed = 0f;
    private float cachedFuelCapacity = 0f;
    private float cachedBurnRate = 0f;

    public DieselEngineBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // PORT-NOTE(26.1): the capability shim registers side-less; the upstream port sides are not enforced
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                CDGBlockEntityTypes.DIESEL_ENGINE.get(),
                (be, side) -> be.tank.getCapability());
    }

    // PORT-NOTE(26.1): was DieselEngineBlock.onRemove (drop the installed upgrade)
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (upgrade != EngineUpgrades.EMPTY && level != null)
            Block.popResource(level, pos, upgrade.getItem());
    }

    @Override
    protected void write(ValueOutput tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putString("Upgrade", upgrade.getId().toString());
        tag.putInt("AnalogSignal", analogSignal);
    }

    @Override
    protected void read(ValueInput tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        Identifier id = Identifier.tryParse(tag.getStringOr("Upgrade", ""));
        upgrade = id == null ? EngineUpgrades.EMPTY : EngineUpgrades.get(id);
        analogSignal = tag.getIntOr("AnalogSignal", 0);
        fuelDebt = 0f;
        invalidateFuelCache();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
        movementDirection = new ServerScrollOptionBehaviour<>(WindmillBearingBlockEntity.RotationDirection.class, this);
        movementDirection.withCallback(v -> reActivateSource = true);
        tank = SmartFluidTankBehaviour.single(this, CDGFluidUnits.BUCKET);

        behaviours.add(movementDirection);
        behaviours.add(tank);
    }

    @Override
    public float calculateAddedStressCapacity() {
        float baseFuelSpeed = getFuelSpeed();
        float speed = upgrade.getSpeed(baseFuelSpeed, this) * getThrottle();
        float capacity = upgrade.getCapacity(getFuelCapacity() * baseFuelSpeed, this) / Math.max(0.01f, speed);
        lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    public float getGeneratedSpeed() {
        if (!enabled()) return 0;
        float throttle = getThrottle();
        if (throttle == 0f) return 0;
        return convertToDirection(
                (movementDirection.getValue() == 1 ? -1 : 1)
                        * upgrade.getSpeed(getFuelSpeed(), this)
                        * throttle,
                getBlockState().getValue(FACING));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());
        return true;
    }

    @Override
    public void tick() {
        super.tick();

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

                if (throttle != 0f || lastSpeed != 0f) {
                    float baseFuelSpeed = getFuelSpeed();
                    float baseSpeed = upgrade.getSpeed(getFuelSpeed(), this) * throttle;

                    currentCapacity = upgrade.getCapacity(getFuelCapacity() * baseFuelSpeed, this) / Math.max(0.01f, baseSpeed);
                    currentSpeed = getGeneratedSpeed();
                }
            }

            if (lastSpeed != currentSpeed || lastCapacity != currentCapacity) {
                reActivateSource = true;
                lastSpeed = currentSpeed;
                lastCapacity = currentCapacity;
            }
        }

        if (enabled() && !isOverStressed())
            fuelDebt = IEngine.burnFuel(getTank(), fuelDebt + cachedBurnRate * getFuelThrottle() * CDGFluidUnits.MB);
    }

    public void setAnalogSignal(int newSignal) {
        analogSignal = newSignal;
    }

    public void setSignalChanged(boolean newSignal) {
        signalChanged = newSignal;
    }

    @Override
    public int getAnalogSignal() {
        return analogSignal;
    }

    @Override
    public SmartBlockEntity self() {
        return this;
    }

    @Override
    public FluidInventory getTank() {
        return tank.getPrimaryHandler();
    }

    @Override
    public EngineUpgrades getUpgrade() {
        return upgrade;
    }

    @Override
    public void setUpgrade(EngineUpgrades upgrade) {
        this.upgrade = upgrade;
    }

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
