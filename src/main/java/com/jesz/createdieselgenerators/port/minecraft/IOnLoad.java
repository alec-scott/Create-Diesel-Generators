package com.jesz.createdieselgenerators.port.minecraft;

/**
 * PORT-NOTE(26.1): NeoForge's BlockEntity.onLoad. Called on the server from Fabric's
 * ServerBlockEntityEvents.BLOCK_ENTITY_LOAD (registered in GameEvents).
 */
public interface IOnLoad {
    void onLoad();
}
