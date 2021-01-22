package com.supermartijn642.scarecrowsterritory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Created 7/11/2020 by SuperMartijn642
 */
@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy {

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent e){
        for(ScarecrowType type : ScarecrowType.values())
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(type.block), 0, new ModelResourceLocation(type.block.getRegistryName(), "inventory"));
    }

    public static String translate(String translationKey, Object... args){
        return I18n.format(translationKey, args);
    }

    public static void enqueueTask(Runnable task){
        Minecraft.getMinecraft().addScheduledTask(task);
    }

}
