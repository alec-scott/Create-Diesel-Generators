package com.jesz.createdieselgenerators;

import com.jesz.createdieselgenerators.content.entity_filter.EntityFilterMenu;
import com.zurrtum.create.api.registry.CreateRegistries;
import com.zurrtum.create.foundation.gui.menu.MenuType;
import net.minecraft.core.Registry;
import net.minecraft.world.item.ItemStack;

/**
 * PORT-NOTE(26.1): Create Fly's own menu type registry (its OpenScreenPacket carries the holder); the screen is
 * registered with Create Fly's AllMenuScreens in the client entrypoint.
 */
public class CDGMenuTypes {
    public static final MenuType<ItemStack> ENTITY_FILTER = Registry.register(CreateRegistries.MENU_TYPE,
            CreateDieselGenerators.rl("entity_filter"), EntityFilterMenu::new);

    public static void register() {
    }
}
