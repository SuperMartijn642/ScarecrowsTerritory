package com.supermartijn642.scarecrowsterritory.generators;

import com.supermartijn642.core.generator.ItemInfoGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.scarecrowsterritory.ScarecrowType;
import net.minecraft.world.item.DyeColor;

/**
 * Created 26/12/2024 by SuperMartijn642
 */
public class ScarecrowItemInfoGenerator extends ItemInfoGenerator {

    public ScarecrowItemInfoGenerator(ResourceCache cache){
        super("scarecrowsterritory", cache);
    }

    @Override
    public void generate(){
        for(DyeColor color : DyeColor.values())
            this.simpleInfo(ScarecrowType.PRIMITIVE.items.get(color), "item/" + ScarecrowType.PRIMITIVE.getRegistryName(color));
    }
}
