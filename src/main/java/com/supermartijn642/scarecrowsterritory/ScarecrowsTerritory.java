package com.supermartijn642.scarecrowsterritory;

import com.google.common.base.Suppliers;
import com.supermartijn642.core.CommonUtils;
import com.supermartijn642.core.item.CreativeItemGroup;
import com.supermartijn642.core.registry.GeneratorRegistrationHandler;
import com.supermartijn642.core.registry.RegistrationHandler;
import com.supermartijn642.scarecrowsterritory.generators.*;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Supplier;

/**
 * Created 7/7/2020 by SuperMartijn642
 */
@Mod("scarecrowsterritory")
public class ScarecrowsTerritory {

    public static final CreativeItemGroup GROUP = CreativeItemGroup.create("scarecrowsterritory", () -> ScarecrowType.PRIMITIVE.blocks.get(DyeColor.PURPLE).asItem());
    public static final Supplier<Boolean> ENABLE_TROPHIES_INTEGRATION = Suppliers.memoize(() -> ScarecrowsTerritoryConfig.enableTrophyIntegration.get() && CommonUtils.isModLoaded("obtrophies"));

    public ScarecrowsTerritory(){
        register();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ScarecrowsTerritoryClient::register);
        registerGenerators();

        if(ENABLE_TROPHIES_INTEGRATION.get())
            ScarecrowTrophyHighlighter.registerListeners();
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
