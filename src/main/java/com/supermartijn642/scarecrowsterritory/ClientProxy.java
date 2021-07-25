package com.supermartijn642.scarecrowsterritory;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Created 7/11/2020 by SuperMartijn642
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientProxy {

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e){
        for(ScarecrowType type : ScarecrowType.values())
            type.blocks.values().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block, type.getRenderLayer()));
    }

    public static String translate(String translationKey, Object... args){
        return I18n.get(translationKey, args);
    }

}
