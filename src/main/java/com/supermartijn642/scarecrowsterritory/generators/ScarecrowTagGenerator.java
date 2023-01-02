package com.supermartijn642.scarecrowsterritory.generators;

import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.core.generator.TagGenerator;
import com.supermartijn642.scarecrowsterritory.ScarecrowType;

/**
 * Created 02/01/2023 by SuperMartijn642
 */
public class ScarecrowTagGenerator extends TagGenerator {

    public ScarecrowTagGenerator(ResourceCache cache){
        super("scarecrowsterritory", cache);
    }

    @Override
    public void generate(){
        // Mining tags
        ScarecrowType.PRIMITIVE.blocks.values().forEach(this.blockMineableWithAxe()::add);

        // Recipe tags
        ScarecrowType.PRIMITIVE.items.values().forEach(this.itemTag("primitive_scarecrows")::add);

        // Alex's Mobs integration
        ScarecrowType.PRIMITIVE.blocks.values().forEach(this.blockTag("alexsmobs", "crow_fears")::add);
    }
}
