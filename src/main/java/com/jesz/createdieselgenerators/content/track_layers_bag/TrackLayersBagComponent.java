package com.jesz.createdieselgenerators.content.track_layers_bag;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/** PORT-NOTE(26.1): tooltip data only; the client renderer is client/TrackLayersBagTooltip. */
public record TrackLayersBagComponent(ItemStack stack) implements TooltipComponent {
}
