package com.jesz.createdieselgenerators.port.minecraft;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.Consumer;

/**
 * PORT-NOTE(26.1): block entities no longer get a {@link CompoundTag} to save into. CDG's NBT-shaped data (wire nodes,
 * energy) is stored as raw tags through {@link ExtraCodecs#NBT}, so the saved layout is unchanged.
 */
public final class CDGValueIO {
    private CDGValueIO() {
    }

    public static boolean contains(ValueInput input, String key) {
        return input.read(key, ExtraCodecs.NBT).isPresent();
    }

    public static CompoundTag getCompound(ValueInput input, String key) {
        return input.read(key, CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }

    public static ListTag getList(ValueInput input, String key) {
        return input.read(key, ExtraCodecs.NBT).filter(ListTag.class::isInstance).map(ListTag.class::cast).orElseGet(ListTag::new);
    }

    public static void put(ValueOutput output, String key, Tag tag) {
        output.store(key, ExtraCodecs.NBT, tag);
    }

    /** Runs a save method against a fresh compound (for item data, packets and other NBT consumers). */
    public static CompoundTag save(HolderLookup.Provider registries, Consumer<ValueOutput> writer) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        writer.accept(output);
        return output.buildResult();
    }

    /** Wraps a compound for a load method. */
    public static ValueInput input(HolderLookup.Provider registries, CompoundTag tag) {
        return TagValueInput.create(ProblemReporter.DISCARDING, registries, tag);
    }
}
