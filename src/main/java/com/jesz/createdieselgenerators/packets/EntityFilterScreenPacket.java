package com.jesz.createdieselgenerators.packets;

import com.jesz.createdieselgenerators.content.entity_filter.EntityAttribute;
import com.jesz.createdieselgenerators.content.entity_filter.EntityFilterMenu;
import com.zurrtum.create.infrastructure.component.AttributeFilterWhitelistMode;
import com.zurrtum.create.infrastructure.packet.c2s.FilterScreenPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.jesz.createdieselgenerators.CreateDieselGenerators;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public record EntityFilterScreenPacket(FilterScreenPacket.Option option, EntityAttribute attribute) implements CustomPacketPayload {
    public static final Type<EntityFilterScreenPacket> TYPE = new Type<>(CreateDieselGenerators.rl("entity_filter_screen"));

    public static final StreamCodec<ByteBuf, EntityFilterScreenPacket> STREAM_CODEC = StreamCodec.composite(
            FilterScreenPacket.Option.STREAM_CODEC, EntityFilterScreenPacket::option,
            EntityAttribute.STREAM_CODEC, EntityFilterScreenPacket::attribute,
            EntityFilterScreenPacket::new
    );

    public void handle(ServerPlayer player) {
        if (player == null)
            return;

        if (player.containerMenu instanceof EntityFilterMenu c) {
            if (option == FilterScreenPacket.Option.WHITELIST)
                c.whitelistMode = AttributeFilterWhitelistMode.WHITELIST_DISJ;
            if (option == FilterScreenPacket.Option.WHITELIST2)
                c.whitelistMode = AttributeFilterWhitelistMode.WHITELIST_CONJ;
            if (option == FilterScreenPacket.Option.BLACKLIST)
                c.whitelistMode = AttributeFilterWhitelistMode.BLACKLIST;

            if (option == FilterScreenPacket.Option.ADD_TAG || option == FilterScreenPacket.Option.ADD_INVERTED_TAG)
                c.appendSelectedAttribute(attribute, option == FilterScreenPacket.Option.ADD_INVERTED_TAG);

        }
    }

    @Override
    public Type<EntityFilterScreenPacket> type() {
        return TYPE;
    }
}
