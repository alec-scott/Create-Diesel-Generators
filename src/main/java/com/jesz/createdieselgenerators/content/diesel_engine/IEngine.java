package com.jesz.createdieselgenerators.content.diesel_engine;

import com.zurrtum.create.infrastructure.fluids.FluidInventory;

import com.jesz.createdieselgenerators.CDGConfig;
import com.jesz.createdieselgenerators.CDGRegistries;
import com.jesz.createdieselgenerators.content.diesel_engine.normal.DieselEngineBlock;
import com.jesz.createdieselgenerators.fuel_type.FuelType;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.infrastructure.fluids.FluidStack;

public interface IEngine {

    default boolean enabled() {
        if (!validFS())
            return false;
        if (CDGConfig.ANALOG_SPEED_CONTROL.get())
            return true;
        return !(CDGConfig.ENGINES_DISABLED_WITH_REDSTONE.get()
                && self().getBlockState().getValue(DieselEngineBlock.POWERED));
    }

    FuelType getCachedFuelType();
    void setCachedFuelType(FuelType type);
    FluidStack getLastCachedFluid();
    void setLastCachedFluid(FluidStack fluid);
    float getCachedFuelSpeed();
    void setCachedFuelSpeed(float speed);
    float getCachedFuelCapacity();
    void setCachedFuelCapacity(float capacity);
    float getCachedBurnRate();
    void setCachedBurnRate(float rate);

    default void invalidateFuelCache() {
        setLastCachedFluid(FluidStack.EMPTY);
    }

    default FuelType getFuelType() {
        FluidStack current = fs();
        if (!FluidStack.areFluidsAndComponentsEqual(current, getLastCachedFluid())) {
            setLastCachedFluid(current.copy());
            FuelType type = FuelType.getTypeFor(
                    self().getLevel().registryAccess().lookupOrThrow(CDGRegistries.FUEL_TYPE),
                    current.getFluid());
            setCachedFuelType(type);
            float speed = type.getGenerated(self()).speed();
            setCachedFuelSpeed(speed);
            setCachedFuelCapacity(speed == 0 ? 0 :
                    type.getGenerated(self()).strength() / speed);
            setCachedBurnRate(type.getGenerated(self()).burn());
        }
        return getCachedFuelType();
    }

    default boolean validFS() {
        if (fs().isEmpty()) return false;
        return getFuelType() != FuelType.EMPTY;
    }

    default FluidStack fs() {
        return getTank().getStack(0);
    }

    default float getFuelSpeed() { getFuelType(); return getCachedFuelSpeed(); }
    default float getFuelCapacity() { getFuelType(); return getCachedFuelCapacity(); }
    default float getFuelBurnRate() { getFuelType(); return getCachedBurnRate(); }
    default float getFuelSoundPitch() { return getFuelType().soundPitch(); }

    /**
     * PORT-NOTE(26.1): drains the whole droplets of {@code debt} from the tank's fluid (was 1 mB per whole unit of
     * debt) and returns the fractional remainder.
     */
    static float burnFuel(FluidInventory tank, float debt) {
        int whole = (int) debt;
        if (whole <= 0)
            return debt;
        FluidStack fluid = tank.getStack(0);
        if (!fluid.isEmpty())
            tank.extract(fluid, Math.min(whole, fluid.getAmount()));
        return debt - whole;
    }

    int getAnalogSignal();

    default float getThrottle() {
        return CDGConfig.ANALOG_SPEED_CONTROL.get() ? (15 - getAnalogSignal()) / 15f : 1f;
    }

    default float getFuelThrottle() {
        float throttle = getThrottle();
        if (throttle == 0f) return 0f;
        return 0.25f + (throttle * 0.75f);
    }

    SmartBlockEntity self();

    // PORT-NOTE(26.1): any single-slot Create Fly fluid inventory (TankSegment or FluidTank), droplets
    FluidInventory getTank();

    EngineUpgrades getUpgrade();
    void setUpgrade(EngineUpgrades upgrade);
}
