package com.jesz.createdieselgenerators.content.tools.hammer;

import com.jesz.createdieselgenerators.CDGRecipes;
import com.jesz.createdieselgenerators.content.tools.ProcessingToolItem;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** PORT-NOTE(26.1): logic in ProcessingToolItem; attack attributes are set on the item properties (CDGItems). */
public class HammerItem extends ProcessingToolItem<HammerRecipe> {
    public HammerItem(Properties properties) {
        super(properties, CDGRecipes.HAMMERING, CDGRecipes.HAMMERING_SET);
    }

    @Override
    protected SoundEvent useSound() {
        return SoundEvents.ANVIL_PLACE;
    }
}
