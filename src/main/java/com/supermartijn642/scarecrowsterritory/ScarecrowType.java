package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.block.BaseBlockEntityType;
import com.supermartijn642.core.block.BlockProperties;
import com.supermartijn642.core.block.BlockShape;
import com.supermartijn642.core.item.BaseBlockItem;
import com.supermartijn642.core.item.ItemProperties;
import com.supermartijn642.core.registry.RegistrationHandler;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public enum ScarecrowType {

    PRIMITIVE;

    private static final BlockShape PRIMITIVE_SHAPE = BlockShape.or(
        BlockShape.createBlockShape(7.5, 0, 7.5, 8.5, 26, 8.5),
        BlockShape.createBlockShape(4, 9, 6, 12, 22, 10),
        BlockShape.createBlockShape(4, 21, 4, 12, 29, 12)
    );

    private static final BlockShape[] PRIMITIVE_SHAPES_BOTTOM = new BlockShape[4];
    private static final BlockShape[] PRIMITIVE_SHAPES_TOP = new BlockShape[4];

    static{
        PRIMITIVE_SHAPES_BOTTOM[Direction.NORTH.get2DDataValue()] = PRIMITIVE_SHAPE;
        PRIMITIVE_SHAPES_BOTTOM[Direction.EAST.get2DDataValue()] = PRIMITIVE_SHAPE.rotate(Direction.Axis.Y);
        PRIMITIVE_SHAPES_BOTTOM[Direction.SOUTH.get2DDataValue()] = PRIMITIVE_SHAPE.rotate(Direction.Axis.Y).rotate(Direction.Axis.Y);
        PRIMITIVE_SHAPES_BOTTOM[Direction.WEST.get2DDataValue()] = PRIMITIVE_SHAPE.rotate(Direction.Axis.Y).rotate(Direction.Axis.Y).rotate(Direction.Axis.Y);
        for(int i = 0; i < 4; i++)
            PRIMITIVE_SHAPES_TOP[i] = PRIMITIVE_SHAPES_BOTTOM[i].offset(0, -1, 0);
    }

    public final EnumMap<DyeColor,ScarecrowBlock> blocks = new EnumMap<>(DyeColor.class);
    public BaseBlockEntityType<? extends ScarecrowBlockEntity> blockEntityType;
    public final EnumMap<DyeColor,BaseBlockItem> items = new EnumMap<>(DyeColor.class);

    public void registerBlock(RegistrationHandler.Helper<Block> helper){
        switch(this){
            case PRIMITIVE:
                Arrays.stream(DyeColor.values()).forEach(color -> this.blocks.put(color, new ScarecrowBlock(this, color)));
        }
        for(Map.Entry<DyeColor,ScarecrowBlock> entry : this.blocks.entrySet())
            helper.register(this.getRegistryName(entry.getKey()), entry.getValue());
    }

    public void registerBlockEntityType(RegistrationHandler.Helper<TileEntityType<?>> helper){
        this.blockEntityType = BaseBlockEntityType.create(this::createBlockEntity, this.blocks.values().toArray(new Block[0]));
        helper.register(this.name().toLowerCase(Locale.ROOT) + "_tile", this.blockEntityType);
    }

    public void registerItem(RegistrationHandler.Helper<Item> helper){
        this.blocks.forEach((color, block) -> this.items.put(color, new BaseBlockItem(block, ItemProperties.create().group(ScarecrowsTerritory.GROUP))));
        for(Map.Entry<DyeColor,BaseBlockItem> entry : this.items.entrySet())
            helper.register(this.getRegistryName(entry.getKey()), entry.getValue());
    }

    public String getRegistryName(DyeColor color){
        return (color == DyeColor.PURPLE ? this.name().toLowerCase(Locale.ROOT) : color.getName()) + "_scarecrow";
    }

    public BlockProperties getBlockProperties(DyeColor color){
        switch(this){
            case PRIMITIVE:
                return BlockProperties.create(Material.WOOL, color).sound(SoundType.WOOL).destroyTime(0.5f).explosionResistance(0.5f);
        }
        return BlockProperties.create(Material.AIR);
    }

    public BlockShape getBlockShape(Direction facing, boolean bottom){
        switch(this){
            case PRIMITIVE:
                return bottom ? PRIMITIVE_SHAPES_BOTTOM[facing.get2DDataValue()] : PRIMITIVE_SHAPES_TOP[facing.get2DDataValue()];
        }
        return BlockShape.empty();
    }

    public ScarecrowBlockEntity createBlockEntity(){
        switch(this){
            case PRIMITIVE:
                break;
        }
        return new ScarecrowBlockEntity(this);
    }

    public BlockRenderLayer getRenderLayer(){
        switch(this){
            case PRIMITIVE:
                return BlockRenderLayer.TRANSLUCENT;
        }
        return BlockRenderLayer.SOLID;
    }

    public boolean is2BlocksHigh(){
        return this == PRIMITIVE;
    }
}
