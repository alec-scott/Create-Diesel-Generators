package com.jesz.createdieselgenerators.port.neoforge.capabilities;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.CachedFluidInventoryBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.CachedInventoryBehaviour;
import com.zurrtum.create.infrastructure.fluids.FluidInventory;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * PORT-NOTE(26.1): keeps the per-block-entity {@code registerCapabilities(event)} methods. Items and fluids are
 * registered with Fabric's transfer API through Create Fly's cached inventory behaviours (as Create Fly's
 * {@code AllTransfer} does). Must run during mod init, before any of these block entities exist (the behaviours are
 * attached on construction).
 */
public final class RegisterCapabilitiesEvent {
    public static final RegisterCapabilitiesEvent INSTANCE = new RegisterCapabilitiesEvent();

    private RegisterCapabilitiesEvent() {
    }

    @SuppressWarnings("unchecked")
    public <T, BE extends BlockEntity> void registerBlockEntity(Capabilities.BlockCapability<T> capability, BlockEntityType<BE> type,
                                                                BiFunction<BE, @Nullable Direction, @Nullable T> provider) {
        if (capability == Capabilities.ItemHandler.BLOCK) {
            registerItems((BlockEntityType<SmartBlockEntity>) type, (BiFunction<SmartBlockEntity, Direction, Container>) provider);
        } else if (capability == Capabilities.FluidHandler.BLOCK) {
            registerFluids((BlockEntityType<SmartBlockEntity>) type, (BiFunction<SmartBlockEntity, Direction, FluidInventory>) provider);
        } else {
            throw new IllegalArgumentException("Unknown capability " + capability);
        }
    }

    private static <T extends SmartBlockEntity> void registerItems(BlockEntityType<T> type, BiFunction<T, Direction, Container> provider) {
        BlockEntityBehaviour.add(type, (T be) -> new CachedInventoryBehaviour<>(be, b -> provider.apply(b, null)));
        ItemStorage.SIDED.registerForBlockEntity(CachedInventoryBehaviour::get, type);
    }

    private static <T extends SmartBlockEntity> void registerFluids(BlockEntityType<T> type, BiFunction<T, Direction, FluidInventory> provider) {
        BlockEntityBehaviour.add(type, (T be) -> new CachedFluidInventoryBehaviour<>(be, b -> provider.apply(b, null)));
        FluidStorage.SIDED.registerForBlockEntity(CachedFluidInventoryBehaviour::get, type);
    }
}
