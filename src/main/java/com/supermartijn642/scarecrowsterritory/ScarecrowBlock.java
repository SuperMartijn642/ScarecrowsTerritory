package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.block.BaseBlock;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class ScarecrowBlock extends BaseBlock {

    public static final PropertyBool BOTTOM = PropertyBool.create("bottom");

    private final ScarecrowType type;

    public ScarecrowBlock(ScarecrowType type, EnumDyeColor color){
        super(type.getRegistryName(color), false, type.getBlockProperties(color));
        this.setUnlocalizedName("scarecrowsterritory." + type.getRegistryName(color));
        this.type = type;

        this.setDefaultState(this.getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.NORTH).withProperty(BOTTOM, true));
        this.setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
        TileEntity tile = worldIn.getTileEntity(pos);
        if(tile instanceof ScarecrowTile)
            return ((ScarecrowTile)tile).rightClick(player, hand);
        return false;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entityIn, boolean isActualState){
        Arrays.stream(this.type.getBlockShape(state.getValue(BlockHorizontal.FACING), state.getValue(BOTTOM))).forEach(
            box -> addCollisionBoxToList(pos, entityBox, collidingBoxes, box)
        );
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos){
        AxisAlignedBB result = null;
        for(AxisAlignedBB box : this.type.getBlockShape(state.getValue(BlockHorizontal.FACING), state.getValue(BOTTOM))){
            if(result == null)
                result = box;
            else
                result = new AxisAlignedBB(
                    Math.min(result.minX, box.minX),
                    Math.min(result.minY, box.minY),
                    Math.min(result.minZ, box.minZ),
                    Math.max(result.maxX, box.maxX),
                    Math.max(result.maxY, box.maxY),
                    Math.max(result.maxZ, box.maxZ)
                );
        }
        return result;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand){
        if(this.type.is2BlocksHigh() && !world.isAirBlock(pos.up()))
            return null;
        return this.getDefaultState().withProperty(BlockHorizontal.FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack){
        if(this.type.is2BlocksHigh() && !worldIn.isAirBlock(pos)){
            worldIn.setBlockState(pos.up(), state.withProperty(BOTTOM, false));
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state){
        if(this.type.is2BlocksHigh()){
            boolean bottom = state.getValue(BOTTOM);
            IBlockState state1 = worldIn.getBlockState(bottom ? pos.up() : pos.down());
            if(state1.getBlock() == state.getBlock() && state1.getValue(BOTTOM) != bottom)
                worldIn.setBlockState(bottom ? pos.up() : pos.down(),
                    Blocks.AIR.getDefaultState());
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    protected BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, BlockHorizontal.FACING, BOTTOM);
    }

    @Override
    public boolean hasTileEntity(IBlockState state){
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state){
        return this.type.createTileEntity();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag advanced){
        boolean spawners = STConfig.loadSpawners.get();
        boolean passive = STConfig.passiveMobSpawning.get();

        if(spawners && passive)
            tooltip.addAll(wrapTooltip("scarecrowsterritory.primitive_scarecrow.info.both", TextFormatting.AQUA, Math.round(STConfig.loadSpawnerRange.get()), Math.round(STConfig.passiveMobRange.get())));
        else if(spawners)
            tooltip.addAll(wrapTooltip("scarecrowsterritory.primitive_scarecrow.info.spawners", TextFormatting.AQUA, Math.round(STConfig.loadSpawnerRange.get())));
        else if(passive)
            tooltip.addAll(wrapTooltip("scarecrowsterritory.primitive_scarecrow.info.passive", TextFormatting.AQUA, Math.round(STConfig.passiveMobRange.get())));
    }

    @SideOnly(Side.CLIENT)
    private static List<String> wrapTooltip(String translationKey, TextFormatting color, Object... args){
        List<ITextComponent> components = new ArrayList<>(1);
        String translation = ClientProxy.translate(translationKey, args).trim();
        StringTokenizer tokenizer = new StringTokenizer(translation, " ");
        StringBuilder builder = new StringBuilder(tokenizer.nextToken());

        while(tokenizer.hasMoreTokens()){
            String token = tokenizer.nextToken();
            if(builder.length() + token.length() + 1 < 25)
                builder.append(' ').append(token);
            else{
                components.add(new TextComponentString(builder.toString()).setStyle(new Style().setColor(color)));
                builder = new StringBuilder(token);
            }
        }

        components.add(new TextComponentString(builder.toString()).setStyle(new Style().setColor(color)));

        return components.stream().map(ITextComponent::getFormattedText).collect(Collectors.toList());
    }

    @Override
    public BlockRenderLayer getBlockLayer(){
        return this.type.getRenderLayer();
    }

    @Override
    public boolean isOpaqueCube(IBlockState state){
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state){
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face){
        return face == EnumFacing.DOWN && state.getValue(BOTTOM) ? BlockFaceShape.CENTER_SMALL : BlockFaceShape.UNDEFINED;
    }

    @Override
    public int getMetaFromState(IBlockState state){
        return state.getValue(BlockHorizontal.FACING).getHorizontalIndex() + (state.getValue(BOTTOM) ? 0 : 4);
    }

    @Override
    public IBlockState getStateFromMeta(int meta){
        return this.getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.getHorizontal(meta & 3)).withProperty(BOTTOM, (meta & 4) == 4);
    }
}
