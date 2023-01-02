package com.supermartijn642.scarecrowsterritory.generators;

import com.supermartijn642.core.generator.LootTableGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.scarecrowsterritory.ScarecrowType;

/**
 * Created 02/01/2023 by SuperMartijn642
 */
public class ScarecrowLootTableGenerator extends LootTableGenerator {

    public ScarecrowLootTableGenerator(ResourceCache cache){
        super("scarecrowsterritory", cache);
    }

    @Override
    public void generate(){
        ScarecrowType.PRIMITIVE.blocks.values().forEach(this::dropSelf);
    }
}
