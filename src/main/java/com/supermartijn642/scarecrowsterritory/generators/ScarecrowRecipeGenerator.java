package com.supermartijn642.scarecrowsterritory.generators;

import com.supermartijn642.core.generator.RecipeGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.scarecrowsterritory.ScarecrowType;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Created 02/01/2023 by SuperMartijn642
 */
public class ScarecrowRecipeGenerator extends RecipeGenerator {

    @SuppressWarnings("unchecked")
    private static final TagKey<Item>[] DYE_TAGS = new TagKey[]{
        ConventionalItemTags.WHITE_DYES,
        ConventionalItemTags.ORANGE_DYES,
        ConventionalItemTags.MAGENTA_DYES,
        ConventionalItemTags.LIGHT_BLUE_DYES,
        ConventionalItemTags.YELLOW_DYES,
        ConventionalItemTags.LIME_DYES,
        ConventionalItemTags.PINK_DYES,
        ConventionalItemTags.GRAY_DYES,
        ConventionalItemTags.LIGHT_GRAY_DYES,
        ConventionalItemTags.CYAN_DYES,
        ConventionalItemTags.PURPLE_DYES,
        ConventionalItemTags.BLUE_DYES,
        ConventionalItemTags.BROWN_DYES,
        ConventionalItemTags.GREEN_DYES,
        ConventionalItemTags.RED_DYES,
        ConventionalItemTags.BLACK_DYES
    };

    public ScarecrowRecipeGenerator(ResourceCache cache){
        super("scarecrowsterritory", cache);
    }

    @Override
    public void generate(){
        // Primitive scarecrow
        this.shaped("scarecrow", ScarecrowType.PRIMITIVE.items.get(DyeColor.PURPLE))
            .pattern(" A ")
            .pattern("BCB")
            .pattern(" B ")
            .input('A', Items.CARVED_PUMPKIN)
            .input('B', Items.STICK)
            .input('C', Items.HAY_BLOCK)
            .unlockedBy(Items.STICK);

        // Colored scarecrows
        TagKey<Item> scarecrowTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("scarecrowsterritory", "primitive_scarecrows"));
        for(DyeColor color : DyeColor.values()){
            this.shapeless(ScarecrowType.PRIMITIVE.items.get(color))
                .input(scarecrowTag)
                .input(DYE_TAGS[color.ordinal()])
                .unlockedBy(scarecrowTag);
        }
    }
}
