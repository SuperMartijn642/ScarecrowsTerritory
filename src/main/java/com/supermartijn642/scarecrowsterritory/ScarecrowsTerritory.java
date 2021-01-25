package com.supermartijn642.scarecrowsterritory;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Created 7/7/2020 by SuperMartijn642
 */
@Mod(modid = ScarecrowsTerritory.MODID, name = ScarecrowsTerritory.NAME, version = ScarecrowsTerritory.VERSION, dependencies = ScarecrowsTerritory.DEPENDENCIES)
public class ScarecrowsTerritory {

    public static final String MODID = "scarecrowsterritory";
    public static final String NAME = "Scarecrow's Territory";
    public static final String VERSION = "1.0.4";
    public static final String DEPENDENCIES = "required-after:forge@[14.23.5.2779,);required-after:supermartijn642configlib@[1.0.1,)";

    public ScarecrowsTerritory(){
    }

    @Mod.EventBusSubscriber
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlockRegistry(final RegistryEvent.Register<Block> e){
            for(ScarecrowType type : ScarecrowType.values()){
                type.registerBlock(e);
                type.registerTileEntity(e);
            }
        }

        @SubscribeEvent
        public static void onItemRegistry(final RegistryEvent.Register<Item> e){
            for(ScarecrowType type : ScarecrowType.values())
                type.registerItem(e);
        }
    }

}
