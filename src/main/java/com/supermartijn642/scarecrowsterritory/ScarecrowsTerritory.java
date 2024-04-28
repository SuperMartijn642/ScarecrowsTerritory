package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.item.CreativeItemGroup;
import com.supermartijn642.core.registry.GeneratorRegistrationHandler;
import com.supermartijn642.core.registry.RegistrationHandler;
import com.supermartijn642.scarecrowsterritory.generators.*;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.item.DyeColor;

/**
 * Created 7/7/2020 by SuperMartijn642
 */
public class ScarecrowsTerritory implements ModInitializer {

    public static final CreativeItemGroup GROUP = CreativeItemGroup.create("scarecrowsterritory", () -> ScarecrowType.PRIMITIVE.blocks.get(DyeColor.PURPLE).asItem());

    @Override
    public void onInitialize(){
        ScarecrowTracker.registerListeners();

        register();
        registerGenerators();
    }

    private static void register(){
        RegistrationHandler handler = RegistrationHandler.get("scarecrowsterritory");
        for(ScarecrowType type : ScarecrowType.values()){
            handler.registerBlockCallback(type::registerBlock);
            handler.registerBlockEntityTypeCallback(type::registerBlockEntityType);
            handler.registerItemCallback(type::registerItem);
        }
    }

    private static void registerGenerators(){
        GeneratorRegistrationHandler handler = GeneratorRegistrationHandler.get("scarecrowsterritory");
        handler.addGenerator(ScarecrowModelGenerator::new);
        handler.addGenerator(ScarecrowBlockStateGenerator::new);
        handler.addGenerator(ScarecrowLanguageGenerator::new);
        handler.addGenerator(ScarecrowLootTableGenerator::new);
        handler.addGenerator(ScarecrowRecipeGenerator::new);
        handler.addGenerator(ScarecrowTagGenerator::new);
    }
}
