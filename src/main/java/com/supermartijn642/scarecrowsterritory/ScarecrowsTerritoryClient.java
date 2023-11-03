package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.registry.ClientRegistrationHandler;
import com.supermartijn642.core.render.TextureAtlases;
import net.minecraft.world.item.DyeColor;

/**
 * Created 7/11/2020 by SuperMartijn642
 */
public class ScarecrowsTerritoryClient {

    public static void register(){
        ClientRegistrationHandler handler = ClientRegistrationHandler.get("scarecrowsterritory");
        for(ScarecrowType type : ScarecrowType.values()){
            for(DyeColor color : DyeColor.values()){
                handler.registerBlockModelRenderType(() -> type.blocks.get(color), type::getRenderLayer);
            }
        }

        if(ScarecrowsTerritory.ENABLE_TROPHIES_INTEGRATION.get())
            handler.registerAtlasSprite(TextureAtlases.getBlocks(), ScarecrowTrophyHighlighter.CONFIRMATION_SPRITE.getPath());
    }
}
