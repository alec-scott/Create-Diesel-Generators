package com.jesz.createdieselgenerators.events;

import com.jesz.createdieselgenerators.CDGConfig;
import com.jesz.createdieselgenerators.CDGRegistries;
import com.jesz.createdieselgenerators.commands.CDGCommands;
import com.jesz.createdieselgenerators.content.entity_filter.ReverseLootTable;
import com.jesz.createdieselgenerators.fuel_type.FuelType;
import com.jesz.createdieselgenerators.mixins.LootItemAccessor;
import com.jesz.createdieselgenerators.mixins.LootPoolAccessor;
import com.jesz.createdieselgenerators.mixins.LootTableAccessor;
import com.jesz.createdieselgenerators.port.minecraft.IOnLoad;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * PORT-NOTE(26.1): NeoForge game-bus subscribers became Fabric callbacks, registered from {@link #register()}. The
 * client tick and item tooltip handlers are in {@code CreateDieselGeneratorsClient}/{@code ClientEvents}. The
 * toolsmith lighter trade is gone (26.1 villager trades are data-driven; see PORTING-26.1.md TODO). Explosions come
 * from {@code ServerExplosionMixin} (Fabric has no explosion event).
 */
public class GameEvents {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> new CDGCommands(dispatcher));
        LootTableEvents.ALL_LOADED.register((resourceManager, lootRegistry) -> {
            ReverseLootTable.ALL.clear();
            lootRegistry.listElements().forEach(holder -> loadLootTable(holder.key().identifier(), holder.value()));
        });
        ServerTickEvents.END_LEVEL_TICK.register(GameEvents::onServerTick);
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((be, level) -> {
            if (be instanceof IOnLoad loadAware)
                loadAware.onLoad();
        });
    }

    // Was LootTableLoadEvent: index which entity drops which item (entity filter)
    private static void loadLootTable(Identifier tableId, net.minecraft.world.level.storage.loot.LootTable table) {
        if (!tableId.getPath().startsWith("entities/"))
            return;
        String path = tableId.getPath().replaceAll("entities/", "");
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.fromNamespaceAndPath(tableId.getNamespace(), path));
        if (type.isEmpty())
            return;
        ((LootTableAccessor) table).getPools().forEach(pool -> {
            for (LootPoolEntryContainer c : ((LootPoolAccessor) pool).getEntries())
                if (c instanceof LootItemAccessor lootItem)
                    ReverseLootTable.ALL.computeIfAbsent(lootItem.getItem().value(), s -> new ArrayList<>()).add(type.get());
        });
    }

    private static void onServerTick(ServerLevel level) {
        Set<BlockPos> set = toExplode.get(level);
        if (set == null || set.isEmpty())
            return;
        for (BlockPos pos : List.copyOf(set)) {
            level.explode(null, null, null, pos.getX(), pos.getY(), pos.getZ(), 1, true, Level.ExplosionInteraction.BLOCK);
            set.remove(pos);
        }
    }

    static Map<Level, Set<BlockPos>> toExplode = new HashMap<>();

    // Was ExplosionEvent.Detonate; called from ServerExplosionMixin
    public static void onExplosion(ServerLevel level, Vec3 center) {
        if (!CDGConfig.COMBUSTIBLES_BLOW_UP.get())
            return;
        for (int x = -2; x < 2; x++) {
            for (int y = -2; y < 2; y++) {
                for (int z = -2; z < 2; z++) {
                    BlockPos pos = new BlockPos((int) (x + center.x), (int) (y + center.y), (int) (z + center.z));

                    if (!level.isInWorldBounds(pos)) continue;
                    if (Math.abs(Math.sqrt(x * x + y * y + z * z)) < 2) {
                        FluidState fluidState = level.getFluidState(pos);
                        if (fluidState.isEmpty()) continue;
                        boolean flammable = FuelType.getTypeFor(level.registryAccess().lookupOrThrow(CDGRegistries.FUEL_TYPE), fluidState.getType()).normal().speed() != 0;

                        if (flammable) {
                            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                            toExplode.computeIfAbsent(level, l -> new HashSet<>()).add(pos);
                            return;
                        }
                    }
                }
            }
        }
    }
}
