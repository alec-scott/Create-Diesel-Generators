package com.jesz.createdieselgenerators.packets;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** PORT-NOTE(26.1): Catnip's packet registry became Fabric's payload registry and play networking. */
public class CDGPackets {
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(EntityFilterScreenPacket.TYPE, EntityFilterScreenPacket.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(EntityFilterScreenPacket.TYPE,
                (payload, context) -> payload.handle(context.player()));
    }
}
