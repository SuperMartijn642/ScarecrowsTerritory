package com.supermartijn642.scarecrowsterritory.generators;

import com.supermartijn642.core.generator.RecipeGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.scarecrowsterritory.ScarecrowType;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;

/**
 * Created 02/01/2023 by SuperMartijn642
 */
public class ScarecrowRecipeGenerator extends RecipeGenerator {

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
            .input('B', Tags.Items.RODS_WOODEN)
            .input('C', Items.HAY_BLOCK)
            .unlockedBy(Tags.Items.RODS_WOODEN);

        // Colored scarecrows
        ITag<Item> scarecrowTag = ItemTags.createOptional(new ResourceLocation("scarecrowsterritory", "primitive_scarecrows"));
        for(DyeColor color : DyeColor.values()){
            this.shapeless(ScarecrowType.PRIMITIVE.items.get(color))
                .input(scarecrowTag)
                .input(color.getTag())
                .unlockedBy(scarecrowTag);
        }
    }
}
