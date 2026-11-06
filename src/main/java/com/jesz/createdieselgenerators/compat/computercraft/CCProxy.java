package com.jesz.createdieselgenerators.compat.computercraft;

import com.jesz.createdieselgenerators.CDGBlockEntityTypes;
import com.jesz.createdieselgenerators.compat.computercraft.peripherals.ChemicalTurretPeripheral;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.compat.computercraft.AbstractComputerBehaviour;
import com.zurrtum.create.compat.computercraft.implementation.ComputerBehaviour;
import com.zurrtum.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Function;

/**
 * PORT-NOTE(26.1): upstream gave the turret a CDGComputerBehaviour (or Create's fallback) and exposed it through
 * NeoForge's PeripheralCapability. As Create Fly's AllComputerPeripherals (and the New Age port): Create Fly's
 * ComputerBehaviour is added per type and caches the peripheral, returned from CC:Tweaked's Fabric
 * {@link PeripheralLookup}, so re-queries get the same peripheral. Only called when CC:Tweaked is loaded
 * ({@code CreateDieselGenerators.CC_ACTIVE}); no CC classes load otherwise.
 */
public class CCProxy {
    public static void register() {
        registerPeripheral(CDGBlockEntityTypes.CHEMICAL_TURRET.get(), ChemicalTurretPeripheral::new);
    }

    private static <T extends SmartBlockEntity> void registerPeripheral(BlockEntityType<T> type, Function<T, SyncedPeripheral<T>> factory) {
        BlockEntityBehaviour.add(type, ComputerBehaviour::new);
        PeripheralLookup.get().registerForBlockEntity((blockEntity, direction) -> {
            if (blockEntity.getBehaviour(AbstractComputerBehaviour.TYPE) instanceof ComputerBehaviour behaviour) {
                if (behaviour.peripheral != null)
                    return behaviour.peripheral;
                return behaviour.peripheral = factory.apply(blockEntity);
            }
            return null;
        }, type);
    }
}
