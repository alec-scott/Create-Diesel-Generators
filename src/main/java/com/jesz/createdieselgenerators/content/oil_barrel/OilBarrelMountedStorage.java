package com.jesz.createdieselgenerators.content.oil_barrel;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zurrtum.create.api.contraption.storage.SyncedMountedStorage;
import com.zurrtum.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.zurrtum.create.api.contraption.storage.fluid.WrapperMountedFluidStorage;
import com.zurrtum.create.content.contraptions.Contraption;
import com.zurrtum.create.foundation.fluid.FluidTank;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.jesz.createdieselgenerators.CDGMountedStorageTypes;

/** PORT-NOTE(26.1): as Create Fly's FluidTankMountedStorage (droplets); the barrel has no fill-level animation. */
public class OilBarrelMountedStorage extends WrapperMountedFluidStorage<OilBarrelMountedStorage.Handler> implements SyncedMountedStorage {
    public static final MapCodec<OilBarrelMountedStorage> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("capacity").forGetter(OilBarrelMountedStorage::getCapacity),
        FluidStack.OPTIONAL_CODEC.fieldOf("fluid").forGetter(OilBarrelMountedStorage::getFluid)
    ).apply(i, OilBarrelMountedStorage::new));

    private boolean dirty;

    protected OilBarrelMountedStorage(MountedFluidStorageType<?> type, int capacity, FluidStack stack) {
        super(type);
        wrapped = new Handler(capacity, stack);
    }

    public OilBarrelMountedStorage(int capacity, FluidStack stack) {
        this(CDGMountedStorageTypes.OIL_BARREL.get(), capacity, stack);
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof OilBarrelBlockEntity tank && tank.isController()) {
            FluidTank inventory = tank.tankInventory;
            // capacity shouldn't change, leave it
            inventory.setFluid(this.wrapped.getFluid());
        }
    }

    public FluidStack getFluid() {
        return this.wrapped.getFluid();
    }

    public int getCapacity() {
        return this.wrapped.getMaxAmountPerStack();
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void markClean() {
        this.dirty = false;
    }

    @Override
    public void markDirty() {
        dirty = true;
    }

    @Override
    public void afterSync(Contraption contraption, BlockPos localPos) {
    }

    public static OilBarrelMountedStorage fromTank(OilBarrelBlockEntity tank) {
        // tank has update callbacks, make an isolated copy
        FluidTank inventory = tank.tankInventory;
        return new OilBarrelMountedStorage(inventory.getMaxAmountPerStack(), inventory.getFluid().copy());
    }

    public final class Handler extends FluidTank {
        public Handler(int capacity, FluidStack stack) {
            super(capacity);
            setFluid(stack);
        }

        @Override
        public void markDirty() {
            dirty = true;
        }
    }
}