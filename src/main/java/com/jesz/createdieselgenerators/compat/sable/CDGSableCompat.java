package com.jesz.createdieselgenerators.compat.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Only loaded when Sable is present (see {@code port.CDGSable}). */
public final class CDGSableCompat {
    private CDGSableCompat() {
    }

    public static Vec3 projectOutOfSubLevel(Level level, Vec3 pos) {
        return SableCompanion.INSTANCE.projectOutOfSubLevel(level, pos);
    }
}
