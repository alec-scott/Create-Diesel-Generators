package com.jesz.createdieselgenerators;

import com.jesz.createdieselgenerators.content.molds.MoldType;
import com.jesz.createdieselgenerators.events.GameEvents;
import com.jesz.createdieselgenerators.events.ModEvents;
import com.jesz.createdieselgenerators.packets.CDGPackets;
import com.jesz.createdieselgenerators.port.create.CreateTransforms;
import com.jesz.createdieselgenerators.port.create.foundation.data.CreateRegistrate;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.config.ModConfig;

/**
 * PORT-NOTE(26.1): the NeoForge {@code @Mod} constructor and mod-bus listeners became this {@link ModInitializer};
 * client setup (ponder, partials, tooltips, renderers) is in {@link CreateDieselGeneratorsClient}.
 */
public class CreateDieselGenerators implements ModInitializer {
    public static final String ID = "createdieselgenerators";

    public static boolean CC_ACTIVE = false;
    public static boolean SABLE_ACTIVE = false;
    public static boolean JEI_ACTIVE = false;

    // PORT-NOTE(26.1): the tooltip modifier factory moved to CreateDieselGeneratorsClient (Create Fly's tooltip
    // classes are client only)
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null);

    @Override
    public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        CC_ACTIVE = loader.isModLoaded("computercraft");
        // Sable Companion is bundled in the Sable jar on Fabric
        SABLE_ACTIVE = loader.isModLoaded("sable");
        JEI_ACTIVE = loader.isModLoaded("jei");

        ConfigRegistry.INSTANCE.register(ID, ModConfig.Type.SERVER, CDGConfig.SERVER_SPEC, ID + "-server.toml");
        ConfigRegistry.INSTANCE.register(ID, ModConfig.Type.COMMON, CDGConfig.COMMON_SPEC, ID + "-common.toml");

        CDGDataComponents.register();
        CDGItems.register();
        CDGBlocks.register();
        CDGFluids.register();
        CDGBlockEntityTypes.register();
        CDGEntityTypes.register();
        CDGSoundEvents.register();
        CDGRecipes.register();
        CDGMenuTypes.register();
        MoldType.register();
        CDGMountedStorageTypes.register();
        CDGDisplaySources.register();

        // Upstream gated the wood type finder on Moonlight (the library Every Compat builds on), not on Every Compat itself
        if (loader.isModLoaded("moonlight"))
            com.jesz.createdieselgenerators.compat.EveryCompatCompat.init();

        REGISTRATE.register();
        CreateTransforms.apply();
        CDGCreativeTab.register();

        CDGPackets.register();
        ModEvents.register();
        GameEvents.register();

        if (CC_ACTIVE)
            com.jesz.createdieselgenerators.compat.computercraft.CCProxy.register();
    }

    public static Identifier rl(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    public static Component lang(String path, Object... args) {
        return Component.translatable(ID + "." + path, args);
    }
}
