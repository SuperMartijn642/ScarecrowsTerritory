package com.supermartijn642.scarecrowsterritory;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.RegistryEvent;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public enum ScarecrowType {

    PRIMITIVE;

    private static final VoxelShape PRIMITIVE_SHAPE = Shapes.or(
        Shapes.box(7.5 / 16d, 0, 7.5 / 16d, 8.5 / 16d, 26 / 16d, 8.5 / 16d),
        Shapes.box(4 / 16d, 9 / 16d, 6 / 16d, 12 / 16d, 22 / 16d, 10 / 16d),
        Shapes.box(4 / 16d, 21 / 16d, 4 / 16d, 12 / 16d, 29 / 16d, 12 / 16d));

    private static final VoxelShape[] PRIMITIVE_SHAPES_BOTTOM = new VoxelShape[4];
    private static final VoxelShape[] PRIMITIVE_SHAPES_TOP = new VoxelShape[4];

    static{
        PRIMITIVE_SHAPES_BOTTOM[Direction.NORTH.get2DDataValue()] = PRIMITIVE_SHAPE;
        PRIMITIVE_SHAPES_BOTTOM[Direction.EAST.get2DDataValue()] = rotateShape(Direction.NORTH, Direction.EAST, PRIMITIVE_SHAPE);
        PRIMITIVE_SHAPES_BOTTOM[Direction.SOUTH.get2DDataValue()] = rotateShape(Direction.NORTH, Direction.SOUTH, PRIMITIVE_SHAPE);
        PRIMITIVE_SHAPES_BOTTOM[Direction.WEST.get2DDataValue()] = rotateShape(Direction.NORTH, Direction.WEST, PRIMITIVE_SHAPE);
        for(int i = 0; i < 4; i++)
            PRIMITIVE_SHAPES_TOP[i] = PRIMITIVE_SHAPES_BOTTOM[i].move(0, -1, 0);
    }

    /**
     * Credits to wyn_price
     * @see <a href="https://forums.minecraftforge.net/topic/74979-1144-rotate-voxel-shapes/?do=findComment&comment=391969">Minecraft Forge forum post</a>
     */
    public static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape){
        VoxelShape[] buffer = new VoxelShape[]{shape, Shapes.empty()};

        int times = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
        for(int i = 0; i < times; i++){
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(buffer[1], Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }

        return buffer[0];
    }

    public final EnumMap<DyeColor,ScarecrowBlock> blocks = new EnumMap<>(DyeColor.class);
    public BlockEntityType<? extends ScarecrowTile> tileTileEntityType;
    private final EnumMap<DyeColor,BlockItem> items = new EnumMap<>(DyeColor.class);

    public void registerBlock(RegistryEvent.Register<Block> e){
        switch(this){
            case PRIMITIVE:
                Arrays.stream(DyeColor.values()).forEach(color -> this.blocks.put(color, new ScarecrowBlock(this, color)));
        }
        this.blocks.values().forEach(e.getRegistry()::register);
    }

    public void registerTileType(RegistryEvent.Register<BlockEntityType<?>> e){
        this.tileTileEntityType = BlockEntityType.Builder.of(this::createTileEntity, this.blocks.values().toArray(new Block[0])).build(null);
        this.tileTileEntityType.setRegistryName(this.name().toLowerCase(Locale.ROOT) + "_tile");
        e.getRegistry().register(this.tileTileEntityType);
    }

    public void registerItem(RegistryEvent.Register<Item> e){
        this.blocks.forEach((color, block) -> {
            BlockItem item = new BlockItem(block, new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS));
            item.setRegistryName(this.getRegistryName(color));
            this.items.put(color, item);
        });
        this.items.values().forEach(e.getRegistry()::register);
    }

    public String getRegistryName(DyeColor color){
        return (color == DyeColor.PURPLE ? this.name().toLowerCase(Locale.ROOT) : color.getName()) + "_scarecrow";
    }

    public BlockBehaviour.Properties getBlockProperties(DyeColor color){
        switch(this){
            case PRIMITIVE:
                return BlockBehaviour.Properties.of(Material.WOOL, color).sound(SoundType.WOOL);
        }
        return BlockBehaviour.Properties.of(Material.AIR);
    }

    public VoxelShape getBlockShape(Direction facing, boolean bottom){
        switch(this){
            case PRIMITIVE:
                return bottom ? PRIMITIVE_SHAPES_BOTTOM[facing.get2DDataValue()] : PRIMITIVE_SHAPES_TOP[facing.get2DDataValue()];
        }
        return Shapes.block();
    }

    public ScarecrowTile createTileEntity(BlockPos pos, BlockState state){
        switch(this){
            case PRIMITIVE:
                break;
        }
        return new ScarecrowTile(this, pos, state);
    }

    public RenderType getRenderLayer(){
        switch(this){
            case PRIMITIVE:
                return RenderType.translucent();
        }
        return RenderType.solid();
    }

    public boolean is2BlocksHigh(){
        return this == PRIMITIVE;
    }

}
