package com.supermartijn642.scarecrowsterritory.generators;

import com.supermartijn642.core.generator.RecipeGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.scarecrowsterritory.ScarecrowType;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * Created 02/01/2023 by SuperMartijn642
 */
public class ScarecrowRecipeGenerator extends RecipeGenerator {

    private static final Map<EnumDyeColor,String> DYE_ORE_DICTS = new HashMap<>();

    static{
        DYE_ORE_DICTS.put(EnumDyeColor.WHITE, "dyeWhite");
        DYE_ORE_DICTS.put(EnumDyeColor.ORANGE, "dyeOrange");
        DYE_ORE_DICTS.put(EnumDyeColor.MAGENTA, "dyeMagenta");
        DYE_ORE_DICTS.put(EnumDyeColor.LIGHT_BLUE, "dyeLightBlue");
        DYE_ORE_DICTS.put(EnumDyeColor.YELLOW, "dyeYellow");
        DYE_ORE_DICTS.put(EnumDyeColor.LIME, "dyeLime");
        DYE_ORE_DICTS.put(EnumDyeColor.PINK, "dyePink");
        DYE_ORE_DICTS.put(EnumDyeColor.GRAY, "dyeGray");
        DYE_ORE_DICTS.put(EnumDyeColor.SILVER, "dyeLightGray");
        DYE_ORE_DICTS.put(EnumDyeColor.CYAN, "dyeCyan");
        DYE_ORE_DICTS.put(EnumDyeColor.PURPLE, "dyePurple");
        DYE_ORE_DICTS.put(EnumDyeColor.BLUE, "dyeBlue");
        DYE_ORE_DICTS.put(EnumDyeColor.BROWN, "dyeBrown");
        DYE_ORE_DICTS.put(EnumDyeColor.GREEN, "dyeGreen");
        DYE_ORE_DICTS.put(EnumDyeColor.RED, "dyeRed");
        DYE_ORE_DICTS.put(EnumDyeColor.BLACK, "dyeBlack");
    }

    public ScarecrowRecipeGenerator(ResourceCache cache){
        super("scarecrowsterritory", cache);
    }

    @Override
    public void generate(){
        // Primitive scarecrow
        this.shaped("scarecrow", ScarecrowType.PRIMITIVE.items.get(EnumDyeColor.PURPLE))
            .pattern(" A ")
            .pattern("BCB")
            .pattern(" B ")
            .input('A', Item.getItemFromBlock(Blocks.PUMPKIN))
            .input('B', "stickWood")
            .input('C', Item.getItemFromBlock(Blocks.HAY_BLOCK))
            .unlockedByOreDict("stickWood");

        // Colored scarecrows
        String scarecrowTag = "primitive_scarecrows";
        for(EnumDyeColor color : EnumDyeColor.values()){
            this.shapeless(ScarecrowType.PRIMITIVE.items.get(color))
                .input(scarecrowTag)
                .input(DYE_ORE_DICTS.get(color))
                .unlockedByOreDict(scarecrowTag);
        }
    }
}
