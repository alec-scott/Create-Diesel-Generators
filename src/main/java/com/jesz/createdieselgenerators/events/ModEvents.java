package com.jesz.createdieselgenerators.events;

import com.jesz.createdieselgenerators.CDGBlockEntityTypes;
import com.jesz.createdieselgenerators.CDGItems;
import com.jesz.createdieselgenerators.CDGRegistries;
import com.jesz.createdieselgenerators.content.bulk_fermenter.BulkFermenterBlockEntity;
import com.jesz.createdieselgenerators.content.bulk_fermenter.BulkFermenterUnpackingHandler;
import com.jesz.createdieselgenerators.content.burner.BurnerBlockEntity;
import com.jesz.createdieselgenerators.content.canister.CanisterBlockEntity;
import com.jesz.createdieselgenerators.content.canister.SpoutCanisterFilling;
import com.jesz.createdieselgenerators.content.diesel_engine.huge.HugeDieselEngineBlockEntity;
import com.jesz.createdieselgenerators.content.diesel_engine.modular.ModularDieselEngineBlockEntity;
import com.jesz.createdieselgenerators.content.diesel_engine.normal.DieselEngineBlockEntity;
import com.jesz.createdieselgenerators.content.distillation.DistillationTankBlockEntity;
import com.jesz.createdieselgenerators.content.molds.BasinSpoutCasting;
import com.jesz.createdieselgenerators.content.oil_barrel.OilBarrelBlockEntity;
import com.jesz.createdieselgenerators.content.pumpjack.PumpjackHoleBlockEntity;
import com.jesz.createdieselgenerators.content.tools.FueledToolItem;
import com.jesz.createdieselgenerators.content.turret.ChemicalTurretBlockEntity;
import com.jesz.createdieselgenerators.fuel_type.FuelType;
import com.jesz.createdieselgenerators.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.zurrtum.create.AllBlockEntityTypes;
import com.zurrtum.create.api.behaviour.spouting.BlockSpoutingBehaviour;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;

/**
 * PORT-NOTE(26.1): the common mod-bus subscribers, run from {@code CreateDieselGenerators.onInitialize}. Datagen is
 * gone (committed resources); the client-only listeners (models, item renderers, tooltip components, render layers,
 * entity layers) are in {@code CreateDieselGeneratorsClient}. Fluid item capabilities (lighter, sprayers, canister)
 * are registered by {@link FueledToolItem#registerFluidStorage()}; concrete buckets are plain vanilla buckets, which
 * Fabric's transfer API handles itself.
 */
public class ModEvents {
    public static void register() {
        // Was DataPackRegistryEvent.NewRegistry
        DynamicRegistries.registerSynced(CDGRegistries.FUEL_TYPE, FuelType.CODEC, FuelType.NCODEC);

        registerCapabilities(RegisterCapabilitiesEvent.INSTANCE);

        // Was FMLCommonSetupEvent
        BlockSpoutingBehaviour.BY_BLOCK_ENTITY.register(CDGBlockEntityTypes.CANISTER.get(), new SpoutCanisterFilling());
        BlockSpoutingBehaviour.BY_BLOCK_ENTITY.register(AllBlockEntityTypes.BASIN, new BasinSpoutCasting());
        BulkFermenterUnpackingHandler.register();

        // Was FurnaceBurnItem.getBurnTime (NeoForge)
        net.fabricmc.fabric.api.registry.FuelValueEvents.BUILD.register((builder, context) -> builder.add(CDGItems.WOOD_CHIPS.get(), CDGItems.WOOD_CHIPS.get().getBurnTime()));
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        FueledToolItem.registerFluidStorage();
        BulkFermenterBlockEntity.registerCapabilities(event);
        BurnerBlockEntity.registerCapabilities(event);
        CanisterBlockEntity.registerCapabilities(event);
        DieselEngineBlockEntity.registerCapabilities(event);
        ModularDieselEngineBlockEntity.registerCapabilities(event);
        HugeDieselEngineBlockEntity.registerCapabilities(event);
        DistillationTankBlockEntity.registerCapabilities(event);
        OilBarrelBlockEntity.registerCapabilities(event);
        PumpjackHoleBlockEntity.registerCapabilities(event);
        ChemicalTurretBlockEntity.registerCapabilities(event);
    }
}
