package com.jesz.createdieselgenerators.content.diesel_engine.huge;

import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollOptionBehaviour;

import com.zurrtum.create.infrastructure.fluids.FluidInventory;

import com.jesz.createdieselgenerators.port.CDGFluidUnits;

import net.minecraft.world.level.block.Block;



import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.level.storage.ValueInput;

import com.jesz.createdieselgenerators.CDGBlockEntityTypes;
import com.jesz.createdieselgenerators.CDGBlocks;
import com.jesz.createdieselgenerators.content.diesel_engine.EngineUpgrades;
import com.jesz.createdieselgenerators.content.diesel_engine.IEngine;
import com.jesz.createdieselgenerators.fuel_type.FuelType;
import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.zurrtum.create.content.kinetics.base.IRotate;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.zurrtum.create.client.foundation.item.TooltipHelper;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.catnip.data.Couple;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.client.catnip.lang.FontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.Capabilities;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.zurrtum.create.infrastructure.fluids.FluidStack;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.jesz.createdieselgenerators.content.diesel_engine.huge.HugeDieselEngineBlock.FACING;
import static net.minecraft.ChatFormatting.GOLD;

public class HugeDieselEngineBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IEngine {
    public ServerScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> movementDirection;
    public EngineUpgrades upgrade = EngineUpgrades.EMPTY;
    SmartFluidTankBehaviour tank;
    WeakReference<PoweredEngineShaftBlockEntity> target = new WeakReference<>(null);
    public int analogSignal = 0;
    private boolean signalChanged = false;
    private float fuelDebt = 0f;
    boolean overStressed = false;
    private FuelType cachedFuelType = FuelType.EMPTY;
    private FluidStack lastCachedFluid = FluidStack.EMPTY;
    private float cachedFuelSpeed = 0f;
    private float cachedFuelCapacity = 0f;
    private float cachedBurnRate = 0f;

    public HugeDieselEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void write(ValueOutput tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putString("Upgrade", upgrade.getId().toString());
        tag.putInt("AnalogSignal", analogSignal);
        tag.putBoolean("OverStressed", overStressed);
    }

    @Override
    protected void read(ValueInput tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        Identifier upgradeId = Identifier.tryParse(tag.getStringOr("Upgrade", ""));
        upgrade = upgradeId == null ? EngineUpgrades.EMPTY : EngineUpgrades.get(upgradeId);
        analogSignal = tag.getIntOr("AnalogSignal", 0);
        fuelDebt = 0f;
        signalChanged = true;
        overStressed = tag.getBooleanOr("OverStressed", false);
        invalidateFuelCache();
    }

    // PORT-NOTE(26.1): was HugeDieselEngineBlock.onRemove (drop the upgrade, update the shaft)
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (upgrade != EngineUpgrades.EMPTY)
            Block.popResource(level, pos, upgrade.getItem());
        BlockPos shaftPos = pos.relative(oldState.getValue(FACING), 2);
        BlockState shaftState = level.getBlockState(shaftPos);
        if (CDGBlocks.POWERED_ENGINE_SHAFT.has(shaftState))
            level.scheduleTick(shaftPos, shaftState.getBlock(), 1);
    }

    @Override
    public void remove() {
        PoweredEngineShaftBlockEntity shaft = getShaft();
        if (shaft != null)
            shaft.removeGenerator(worldPosition);
        super.remove();
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(2);
    }

    @Override
    public void tick() {
        super.tick();

        PoweredEngineShaftBlockEntity shaft = getShaft();
        boolean wasOverStressed = overStressed;
        overStressed = shaft != null && shaft.isOverStressed();
        if (wasOverStressed && !overStressed)
            signalChanged = true;

        if (shaft != null && enabled() && getThrottle() > 0) {
            float throttle = getThrottle();
            shaft.update(worldPosition,
                    movementDirection.getValue() == 0 ? 1 : -1,
                    upgrade.getCapacity(getFuelCapacity(), this),
                    cachedFuelSpeed * throttle);
        } else if (shaft != null && getThrottle() == 0f) {
            shaft.removeGenerator(worldPosition);
        }

        if (signalChanged) {
            signalChanged = false;
            setChanged();
            sendData();
        }

        if (overStressed)
            return;

        if (shaft == null)
            return;

        if (enabled() && getThrottle() > 0) {
            if (shaft.movementDirection != 0 && shaft.movementDirection !=
                    (movementDirection.get() == WindmillBearingBlockEntity.RotationDirection.CLOCKWISE ? 1 : -1)) {
                shaft.removeGenerator(worldPosition);
                onDirectionChanged(movementDirection.getValue());
                return;
            }

            fuelDebt = IEngine.burnFuel(getTank(), fuelDebt + cachedBurnRate * getFuelThrottle() * CDGFluidUnits.MB);
        } else {
            shaft.removeGenerator(worldPosition);
        }
    }

    public PoweredEngineShaftBlockEntity getShaft() {
        PoweredEngineShaftBlockEntity shaft = target.get();
        if (shaft == null || shaft.isRemoved()) {
            if (shaft != null) {
                target = new WeakReference<>(null);
            }
            BlockEntity anyShaftAt = level.getBlockEntity(worldPosition.relative(getBlockState().getValue(FACING), 2));
            if (anyShaftAt instanceof PoweredEngineShaftBlockEntity ps) {
                target = new WeakReference<>(shaft = ps);
            }
        }
        return shaft;
    }
    
    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
        movementDirection = new ServerScrollOptionBehaviour<>(WindmillBearingBlockEntity.RotationDirection.class, this);
        movementDirection.withCallback(this::onDirectionChanged);

        behaviours.add(movementDirection);
        tank = SmartFluidTankBehaviour.single(this, CDGFluidUnits.mb(100));
        behaviours.add(tank);
    }

    private void onDirectionChanged(int v) {
        PoweredEngineShaftBlockEntity shaft = getShaft();
        if(shaft == null)
            return;
        for (Pair<BlockPos, Couple<Float>> engine : shaft.engines)
            if(level.getBlockEntity(engine.getFirst()) instanceof HugeDieselEngineBlockEntity be)
                be.movementDirection.setValue(v);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                CDGBlockEntityTypes.HUGE_DIESEL_ENGINE.get(),
                (be, side) -> be.tank.getCapability());
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (overStressed) {
            CreateLang.translate("gui.stressometer.overstressed")
                    .style(GOLD)
                    .forGoggles(tooltip);
            Component hint = CreateLang.translateDirect("gui.contraptions.network_overstressed");
            List<Component> cutString = TooltipHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE);
            for (Component component : cutString)
                CreateLang.builder().add(component.copy()).forGoggles(tooltip);
            return containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());
        }

        if (IRotate.StressImpact.isEnabled() && enabled() && getThrottle() > 0) {
            PoweredEngineShaftBlockEntity shaft = getShaft();
            if (shaft != null) {
                float stressBase = upgrade.getCapacity(getFuelCapacity(), this) *
                        upgrade.getSpeed(getFuelSpeed(), this) * getThrottle();
                if (!Mth.equal(stressBase, 0)) {
                    CreateLang.translate("gui.goggles.generator_stats").forGoggles(tooltip);
                    CreateLang.translate("tooltip.capacityProvided")
                            .style(ChatFormatting.GRAY).forGoggles(tooltip);
                    CreateLang.number(Math.abs(stressBase))
                            .translate("generic.unit.stress")
                            .style(ChatFormatting.AQUA)
                            .space()
                            .add(CreateLang.translate("gui.goggles.at_current_speed")
                                    .style(ChatFormatting.DARK_GRAY))
                            .forGoggles(tooltip, 1);
                }
            }
        }

        return containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());
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

    public void setAnalogSignal(int newSignal) { analogSignal = newSignal; }
    public void setSignalChanged(boolean newSignal) { signalChanged = newSignal; }
}
