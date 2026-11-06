package com.jesz.createdieselgenerators.content.tools.wire_cutters;

import com.jesz.createdieselgenerators.CDGRecipes;
import com.jesz.createdieselgenerators.content.tools.ProcessingToolItem;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** PORT-NOTE(26.1): logic in ProcessingToolItem. */
public class WireCuttersItem extends ProcessingToolItem<WireCuttingRecipe> {
    public WireCuttersItem(Properties properties) {
        super(properties.stacksTo(1).durability(32), CDGRecipes.WIRE_CUTTING, CDGRecipes.WIRE_CUTTING_SET);
    }

    @Override
    protected SoundEvent useSound() {
        return SoundEvents.SHEEP_SHEAR;
    }
}
