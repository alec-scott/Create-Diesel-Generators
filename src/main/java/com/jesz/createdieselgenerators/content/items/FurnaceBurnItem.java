package com.jesz.createdieselgenerators.content.items;

import net.minecraft.world.item.Item;

public class FurnaceBurnItem extends Item {
    int burnTime;
    public FurnaceBurnItem(Properties properties, int burnTime) {
        super(properties);
        this.burnTime = burnTime;
    }
    // PORT-NOTE(26.1): was NeoForge getBurnTime; registered through Fabric FuelValueEvents (ModEvents)
    public int getBurnTime() {
        return burnTime;
    }
}
