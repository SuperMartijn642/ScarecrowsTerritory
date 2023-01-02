package com.supermartijn642.scarecrowsterritory.generators;

import com.supermartijn642.core.generator.LanguageGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.scarecrowsterritory.ScarecrowType;
import com.supermartijn642.scarecrowsterritory.ScarecrowsTerritory;
import net.minecraft.item.DyeColor;

/**
 * Created 02/01/2023 by SuperMartijn642
 */
public class ScarecrowLanguageGenerator extends LanguageGenerator {

    public ScarecrowLanguageGenerator(ResourceCache cache){
        super("scarecrowsterritory", cache, "en_us");
    }

    @Override
    public void generate(){
        // Creative tab
        this.itemGroup(ScarecrowsTerritory.GROUP, "Scarecrows' Territory");
        // Blocks
        this.block(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.BLUE), "Black Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.BLUE), "Blue Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.BROWN), "Brown Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.CYAN), "Cyan Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.GRAY), "Gray Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.GREEN), "Green Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.LIGHT_BLUE), "Light Blue Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.LIGHT_GRAY), "Light Gray Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.LIME), "Lime Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.MAGENTA), "Magenta Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.ORANGE), "Orange Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.PINK), "Pink Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.PURPLE), "Purple Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.RED), "Red Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.WHITE), "White Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(DyeColor.YELLOW), "Yellow Scarecrow");
        this.translation("scarecrowsterritory.primitive_scarecrow.info.spawners", "Keeps spawners in a %1$d block range activated");
        this.translation("scarecrowsterritory.primitive_scarecrow.info.passive", "Allows passive mob spawning in a %1$d block range");
        this.translation("scarecrowsterritory.primitive_scarecrow.info.both", "Keeps spawners in a %1$d block range activated and allows passive mob spawning in a %2$d block range");
    }
}
