package com.jesz.createdieselgenerators.client;

import com.zurrtum.create.client.api.goggles.IHaveGoggleInformation;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.GeneratingKineticTooltipBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.KineticTooltipBehaviour;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * PORT-NOTE(26.1): Create Fly's kinetic goggle stats (a client behaviour), followed by the CDG block entity's own
 * lines (was {@code super.addToGoggleTooltip} in the block entity).
 */
public final class CDGKineticTooltipBehaviour {
    private CDGKineticTooltipBehaviour() {
    }

    public static class Consuming<T extends KineticBlockEntity> extends KineticTooltipBehaviour<T> {
        public Consuming(T be) {
            super(be);
        }

        @Override
        public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
            boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
            return blockEntity instanceof IHaveGoggleInformation info && info.addToGoggleTooltip(tooltip, isPlayerSneaking) || added;
        }
    }

    public static class Generating<T extends KineticBlockEntity> extends GeneratingKineticTooltipBehaviour<T> {
        public Generating(T be) {
            super(be);
        }

        @Override
        public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
            boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
            return blockEntity instanceof IHaveGoggleInformation info && info.addToGoggleTooltip(tooltip, isPlayerSneaking) || added;
        }
    }
}
