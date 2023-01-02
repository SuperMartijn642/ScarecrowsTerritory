package com.supermartijn642.scarecrowsterritory.generators;

import com.supermartijn642.core.generator.BlockStateGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.scarecrowsterritory.ScarecrowBlock;
import com.supermartijn642.scarecrowsterritory.ScarecrowType;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;

/**
 * Created 02/01/2023 by SuperMartijn642
 */
public class ScarecrowBlockStateGenerator extends BlockStateGenerator {

    public ScarecrowBlockStateGenerator(ResourceCache cache){
        super("scarecrowsterritory", cache);
    }

    @Override
    public void generate(){
        for(EnumDyeColor color : EnumDyeColor.values()){
            String identifier = ScarecrowType.PRIMITIVE.getRegistryName(color);
            this.blockState(ScarecrowType.PRIMITIVE.blocks.get(color))
                .variantsForAll(
                    (state, variant) -> {
                        boolean bottom = state.get(ScarecrowBlock.BOTTOM);
                        EnumFacing facing = state.get(ScarecrowBlock.FACING);
                        variant.model("block/" + identifier + (bottom ? "_bottom" : "_top"), 0, ((int)facing.getHorizontalAngle() + 180) % 360);
                    }
                );
        }
    }
}
