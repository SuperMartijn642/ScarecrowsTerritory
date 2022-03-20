package com.supermartijn642.scarecrowsterritory;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Created 7/7/2020 by SuperMartijn642
 */
@Mod("scarecrowsterritory")
public class ScarecrowsTerritory {

    public static final CreativeModeTab GROUP = new CreativeModeTab("scarecrowsterritory") {
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
        public static void onTileRegistry(final RegistryEvent.Register<BlockEntityType<?>> e){
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
