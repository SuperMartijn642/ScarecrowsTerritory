package com.supermartijn642.scarecrowsterritory.generators;

import com.supermartijn642.core.generator.BlockStateGenerator;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.scarecrowsterritory.ScarecrowBlock;
import com.supermartijn642.scarecrowsterritory.ScarecrowType;
import net.minecraft.item.DyeColor;
import net.minecraft.util.Direction;

/**
 * Created 02/01/2023 by SuperMartijn642
 */
public class ScarecrowBlockStateGenerator extends BlockStateGenerator {

    public ScarecrowBlockStateGenerator(ResourceCache cache){
        super("scarecrowsterritory", cache);
    }

    @Override
    public void generate(){
        for(DyeColor color : DyeColor.values()){
            String identifier = ScarecrowType.PRIMITIVE.getRegistryName(color);
            this.blockState(ScarecrowType.PRIMITIVE.blocks.get(color))
                .variantsForAllExcept(
                    (state, variant) -> {
                        boolean bottom = state.get(ScarecrowBlock.BOTTOM);
                        Direction facing = state.get(ScarecrowBlock.FACING);
                        variant.model("block/" + identifier + (bottom ? "_bottom" : "_top"), 0, ((int)facing.toYRot() + 180) % 360);
                    },
                    ScarecrowBlock.WATERLOGGED
                );
        }
    }
}
