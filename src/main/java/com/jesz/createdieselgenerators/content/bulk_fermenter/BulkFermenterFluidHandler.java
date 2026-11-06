package com.jesz.createdieselgenerators.content.bulk_fermenter;

import com.zurrtum.create.foundation.fluid.FluidTank;
import com.zurrtum.create.infrastructure.fluids.FluidInventory;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PORT-NOTE(26.1): the fermenter's tanks (was a SmartFluidTank over NeoForge FluidTanks). Each slot is a Create Fly
 * {@link FluidTank} of the same capacity, in droplets. Inserting fills a tank holding the same fluid first, otherwise
 * an empty one (as upstream); extracting drains the first matching tank.
 */
public class BulkFermenterFluidHandler implements FluidInventory {
    final List<FluidTank> tanks = new ArrayList<>();
    final int tankCount;
    private final Runnable onChanged;
    private int capacity;

    public BulkFermenterFluidHandler(int tankCount, int capacity, Runnable onChanged) {
        this.tankCount = tankCount;
        this.capacity = capacity;
        this.onChanged = onChanged;
        for (int i = 0; i < tankCount; i++)
            tanks.add(new FluidTank(capacity));
    }

    public List<FluidTank> getTanks() {
        return tanks;
    }

    @Override
    public int size() {
        return tankCount;
    }

    @Override
    public FluidStack getStack(int slot) {
        if (slot < 0 || slot >= tankCount)
            return FluidStack.EMPTY;
        return tanks.get(slot).getFluid();
    }

    @Override
    public void setStack(int slot, FluidStack stack) {
        if (slot >= 0 && slot < tankCount)
            tanks.get(slot).setFluid(stack);
    }

    @Override
    public int getMaxAmountPerStack() {
        return capacity;
    }

    @Override
    public FluidStack onExtract(FluidStack stack) {
        return removeMaxSize(stack, Optional.of(capacity));
    }

    @Override
    public boolean isValid(int slot, FluidStack stack) {
        FluidStack inSlot = getStack(slot);
        if (!inSlot.isEmpty())
            return matches(inSlot, stack);
        // Only start a new tank when no tank holds this fluid yet
        for (int i = 0; i < tankCount; i++)
            if (i != slot && !getStack(i).isEmpty() && matches(getStack(i), stack))
                return false;
        return true;
    }

    @Override
    public void markDirty() {
        onChanged.run();
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        for (FluidTank tank : tanks)
            tank.setCapacity(capacity);
    }

    public int getCapacity() {
        return capacity;
    }

    public void write(ValueOutput view) {
        ValueOutput.TypedOutputList<FluidStack> list = view.list("Tanks", FluidStack.OPTIONAL_CODEC);
        for (FluidTank tank : tanks)
            list.add(tank.getFluid());
    }

    public void read(ValueInput view) {
        int i = 0;
        for (FluidStack stack : view.listOrEmpty("Tanks", FluidStack.OPTIONAL_CODEC)) {
            if (i >= tankCount)
                break;
            FluidStack copy = stack.copy();
            if (copy.getAmount() > capacity)
                copy.setAmount(capacity);
            tanks.get(i++).setFluid(copy);
        }
        for (; i < tankCount; i++)
            tanks.get(i).setFluid(FluidStack.EMPTY);
    }
}
