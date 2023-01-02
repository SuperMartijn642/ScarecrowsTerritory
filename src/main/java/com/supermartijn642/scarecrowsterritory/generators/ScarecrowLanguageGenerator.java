package com.supermartijn642.scarecrowsterritory.generators;

import com.supermartijn642.core.generator.LanguageGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.scarecrowsterritory.ScarecrowType;
import com.supermartijn642.scarecrowsterritory.ScarecrowsTerritory;
import net.minecraft.item.EnumDyeColor;

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
        this.block(ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.BLACK), "Black Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.BLUE), "Blue Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.BROWN), "Brown Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.CYAN), "Cyan Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.GRAY), "Gray Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.GREEN), "Green Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.LIGHT_BLUE), "Light Blue Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.SILVER), "Light Gray Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.LIME), "Lime Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.MAGENTA), "Magenta Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.ORANGE), "Orange Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.PINK), "Pink Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.PURPLE), "Purple Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.RED), "Red Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.WHITE), "White Scarecrow");
        this.block(ScarecrowType.PRIMITIVE.blocks.get(EnumDyeColor.YELLOW), "Yellow Scarecrow");
        this.translation("scarecrowsterritory.primitive_scarecrow.info.spawners", "Keeps spawners in a %1$d block range activated");
        this.translation("scarecrowsterritory.primitive_scarecrow.info.passive", "Allows passive mob spawning in a %1$d block range");
        this.translation("scarecrowsterritory.primitive_scarecrow.info.both", "Keeps spawners in a %1$d block range activated and allows passive mob spawning in a %2$d block range");
    }
}
