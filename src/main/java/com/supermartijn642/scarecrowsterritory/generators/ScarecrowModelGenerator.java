package com.supermartijn642.scarecrowsterritory.generators;

import com.supermartijn642.core.generator.ModelGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.scarecrowsterritory.ScarecrowType;
import net.minecraft.item.DyeColor;

/**
 * Created 02/01/2023 by SuperMartijn642
 */
public class ScarecrowModelGenerator extends ModelGenerator {

    public ScarecrowModelGenerator(ResourceCache cache){
        super("scarecrowsterritory", cache);
    }

    @Override
    public void generate(){
        for(DyeColor color : DyeColor.values()){
            String identifier = ScarecrowType.PRIMITIVE.getRegistryName(color);
            String texture = color == DyeColor.PURPLE ? "purple_scarecrow" : identifier;
            // Bottom
            this.model("block/" + identifier + "_bottom")
                .parent("scarecrow_bottom")
                .texture("main", texture);
            // Top
            this.model("block/" + identifier + "_top")
                .parent("scarecrow_top")
                .texture("main", texture)
                .texture("hat", texture + "_hat");
            // Item
            this.model("item/" + identifier)
                .parent("scarecrow_item")
                .texture("main", texture)
                .texture("hat", texture + "_hat");
        }
    }
}
