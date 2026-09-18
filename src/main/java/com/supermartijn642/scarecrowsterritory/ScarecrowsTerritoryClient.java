package com.supermartijn642.scarecrowsterritory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * Created 7/11/2020 by SuperMartijn642
 */
public class ScarecrowsTerritoryClient implements ClientModInitializer {

    @Override
    public void onInitializeClient(){
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> ScarecrowTracker.get(true).onClientLevelChange(level));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ScarecrowTracker.get(true).onClientLevelChange(null));
        ClientChunkEvents.CHUNK_LOAD.register(ScarecrowTracker.get(true)::onChunkLoad);
        ClientChunkEvents.CHUNK_UNLOAD.register(ScarecrowTracker.get(true)::onChunkUnload);
    }
}
