package com.jesz.createdieselgenerators.port.create;

import com.jesz.createdieselgenerators.port.porting_lib.util.DeferredHolder;
import com.jesz.createdieselgenerators.port.registrate.AbstractRegistrate;
import com.jesz.createdieselgenerators.port.registrate.util.entry.BlockEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

/**
 * PORT-NOTE(26.1): upstream Create's blocks were Registrate {@link BlockEntry}s, and D&D mixes them into its own entry
 * lists (the andesite crushing wheel and millstone). Create Fly registers plain blocks, so they are wrapped here.
 */
public final class ExistingEntries {
    private ExistingEntries() {
    }

    public static <T extends Block> BlockEntry<T> block(AbstractRegistrate<?> owner, T block) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, BuiltInRegistries.BLOCK.getKey(block));
        return new BlockEntry<>(owner, DeferredHolder.<Block, T>create(key));
    }
}
