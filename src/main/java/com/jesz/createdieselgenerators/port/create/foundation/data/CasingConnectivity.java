package com.jesz.createdieselgenerators.port.create.foundation.data;

import com.zurrtum.create.client.AllCasings;
import com.zurrtum.create.client.foundation.block.connected.CTSpriteShiftEntry;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiPredicate;

/**
 * Upstream Create's {@code CasingConnectivity} as handed to {@link CreateRegistrate#casingConnectivity}.
 *
 * <p>PORT-NOTE(26.1): Create Fly keeps casing entries in the client-only {@link AllCasings}; this only forwards there.</p>
 */
public class CasingConnectivity {
    public void make(Block block, CTSpriteShiftEntry casing) {
        AllCasings.make(block, casing);
    }

    public void make(Block block, CTSpriteShiftEntry casing, BiPredicate<BlockState, Direction> predicate) {
        AllCasings.make(block, casing, predicate);
    }

    public void makeCasing(Block block, CTSpriteShiftEntry casing) {
        AllCasings.make(block, casing);
    }
}
