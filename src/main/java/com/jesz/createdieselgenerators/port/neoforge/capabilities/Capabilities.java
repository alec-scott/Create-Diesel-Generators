package com.jesz.createdieselgenerators.port.neoforge.capabilities;

import com.zurrtum.create.AllTransfer;
import com.zurrtum.create.infrastructure.fluids.FluidInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * PORT-NOTE(26.1): stand-in for NeoForge's capability constants. Items and fluids go through Create Fly's Fabric
 * transfer wrappers ({@link AllTransfer}). Lookups are {@link #getCapability} instead of {@code level.getCapability}.
 */
public final class Capabilities {
    private Capabilities() {
    }

    public static final class ItemHandler {
        public static final BlockCapability<Container> BLOCK = new BlockCapability<>("item");
    }

    public static final class FluidHandler {
        public static final BlockCapability<FluidInventory> BLOCK = new BlockCapability<>("fluid");
    }

    public record BlockCapability<T>(String name) {
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T getCapability(Level level, BlockCapability<T> capability, BlockPos pos, @Nullable Direction side) {
        if (capability == ItemHandler.BLOCK)
            return (T) AllTransfer.getInventory(level, pos, null, null, side);
        if (capability == FluidHandler.BLOCK)
            return (T) AllTransfer.getFluidInventory(level, pos, null, null, side);
        throw new IllegalArgumentException("Unknown capability " + capability);
    }
}
