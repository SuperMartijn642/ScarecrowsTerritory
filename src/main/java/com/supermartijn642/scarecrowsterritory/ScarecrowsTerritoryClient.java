package com.supermartijn642.scarecrowsterritory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * Created 7/11/2020 by SuperMartijn642
 */
public class ScarecrowsTerritoryClient implements ClientModInitializer {

    @Override
    public void onInitializeClient(){
        // Fabric doesn't fire chunk unload events when the client switches or leaves a level, so drop stale client levels here
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> ScarecrowTracker.onClientLevelChange(level));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ScarecrowTracker.onClientLevelChange(null));
    }
}
