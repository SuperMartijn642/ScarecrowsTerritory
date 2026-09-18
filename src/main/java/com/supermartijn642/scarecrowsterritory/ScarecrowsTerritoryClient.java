package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.registry.ClientRegistrationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.world.item.DyeColor;

/**
 * Created 7/11/2020 by SuperMartijn642
 */
public class ScarecrowsTerritoryClient implements ClientModInitializer {

    @Override
    public void onInitializeClient(){
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ScarecrowTracker.get(true).onClientLevelChange(null));
        ClientChunkEvents.CHUNK_LOAD.register(ScarecrowTracker.get(true)::onChunkLoad);
        ClientChunkEvents.CHUNK_UNLOAD.register(ScarecrowTracker.get(true)::onChunkUnload);

        ClientRegistrationHandler handler = ClientRegistrationHandler.get("scarecrowsterritory");
        for(ScarecrowType type : ScarecrowType.values()){
            for(DyeColor color : DyeColor.values()){
                handler.registerBlockModelRenderType(() -> type.blocks.get(color), type::getRenderLayer);
            }
        }
    }
}
