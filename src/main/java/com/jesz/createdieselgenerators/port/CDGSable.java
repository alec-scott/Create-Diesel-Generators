package com.jesz.createdieselgenerators.port;

import com.jesz.createdieselgenerators.CreateDieselGenerators;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * PORT-NOTE(26.1): upstream shipped Sable Companion jar-in-jar, so the turret always called it. On Fabric it comes with
 * Sable (our Aeronautics port), which is optional: this forwards to {@code compat.sable.CDGSableCompat} only when Sable
 * is loaded, so no Sable class loads otherwise.
 */
public final class CDGSable {
    private CDGSable() {
    }

    public static Vec3 projectOutOfSubLevel(Level level, Vec3 pos) {
        if (CreateDieselGenerators.SABLE_ACTIVE)
            return com.jesz.createdieselgenerators.compat.sable.CDGSableCompat.projectOutOfSubLevel(level, pos);
        return pos;
    }
}
