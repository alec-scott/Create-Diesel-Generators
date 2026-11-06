package com.jesz.createdieselgenerators.port.registrate.util.entry;

import java.util.function.Consumer;

import com.jesz.createdieselgenerators.port.registrate.AbstractRegistrate;

import com.jesz.createdieselgenerators.port.porting_lib.util.DeferredHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import org.jetbrains.annotations.Nullable;

public class MenuEntry<T extends AbstractContainerMenu> extends RegistryEntry<MenuType<?>, MenuType<T>> {

    public MenuEntry(AbstractRegistrate<?> owner, DeferredHolder<MenuType<?>, MenuType<T>> delegate) {
        super(owner, delegate);
    }

    public T create(int windowId, Inventory playerInv) {
        return get().create(windowId, playerInv);
    }

    public MenuConstructor asProvider() {
        return (window, playerinv, $) -> create(window, playerinv);
    }

    public void open(ServerPlayer player, Component displayName) {
        open(player, displayName, asProvider());
    }

    public void open(ServerPlayer player, Component displayName, Consumer<RegistryFriendlyByteBuf> extraData) {
        open(player, displayName, asProvider(), extraData);
    }

    public void open(ServerPlayer player, Component displayName, MenuConstructor provider) {
        player.openMenu(new SimpleMenuProvider(provider, displayName));
    }

    public void open(ServerPlayer player, Component displayName, MenuConstructor provider, Consumer<RegistryFriendlyByteBuf> extraData) {
        player.openMenu(new ExtendedMenuProvider<byte[]>() {
            // PORT-NOTE(fabric): the opening data is written to a buffer as on NeoForge and sent as bytes (see MenuBuilder).
            @Override
            public byte[] getScreenOpeningData(ServerPlayer player) {
                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
                try {
                    extraData.accept(buf);
                    byte[] bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    return bytes;
                } finally {
                    buf.release();
                }
            }

            @Override
            public Component getDisplayName() {
                return displayName;
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
                return provider.createMenu(i, inventory, player);
            }
        });
    }
}
