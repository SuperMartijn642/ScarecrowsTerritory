package com.supermartijn642.scarecrowsterritory;

import net.minecraft.block.Block;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Created 7/7/2020 by SuperMartijn642
 */
@Mod("scarecrowsterritory")
public class ScarecrowsTerritory {

    public static final ItemGroup GROUP = new ItemGroup("scarecrowsterritory") {
        @Override
        public ItemStack makeIcon(){
            return new ItemStack(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.PURPLE));
        }
    };

    public ScarecrowsTerritory(){
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlockRegistry(final RegistryEvent.Register<Block> e){
            for(ScarecrowType type : ScarecrowType.values())
                type.registerBlock(e);
        }

        @SubscribeEvent
        public static void onTileRegistry(final RegistryEvent.Register<TileEntityType<?>> e){
            for(ScarecrowType type : ScarecrowType.values())
                type.registerTileType(e);
        }

        @SubscribeEvent
        public static void onItemRegistry(final RegistryEvent.Register<Item> e){
            for(ScarecrowType type : ScarecrowType.values())
                type.registerItem(e);
        }
    }

}
