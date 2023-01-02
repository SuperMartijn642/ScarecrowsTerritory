package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.CommonUtils;
import com.supermartijn642.core.item.CreativeItemGroup;
import com.supermartijn642.core.registry.GeneratorRegistrationHandler;
import com.supermartijn642.core.registry.RegistrationHandler;
import com.supermartijn642.scarecrowsterritory.generators.*;
import net.minecraft.item.EnumDyeColor;
import net.minecraftforge.fml.common.Mod;

/**
 * Created 7/7/2020 by SuperMartijn642
 */
@Mod(modid = "@mod_id@", name = "@mod_name@", version = "@mod_version@", dependencies = "required-after:forge@@forge_dependency@;required-after:supermartijn642corelib@@core_library_dependency@;required-after:supermartijn642configlib@@config_library_dependency@")
public class ScarecrowsTerritory {

    public static final CreativeItemGroup GROUP = CreativeItemGroup.create("scarecrowsterritory", () -> ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.PURPLE).asItem());

    public ScarecrowsTerritory(){
        register();
        if(CommonUtils.getEnvironmentSide().isClient())
            ScarecrowsTerritoryClient.register();
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
