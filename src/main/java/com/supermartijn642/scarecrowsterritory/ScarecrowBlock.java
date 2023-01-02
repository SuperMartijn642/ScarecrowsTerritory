package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.TextComponents;
import com.supermartijn642.core.block.BaseBlock;
import com.supermartijn642.core.block.EntityHoldingBlock;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class ScarecrowBlock extends BaseBlock implements EntityHoldingBlock {

    public static final PropertyBool BOTTOM = PropertyBool.create("bottom");
    public static final PropertyEnum<EnumFacing> FACING = BlockHorizontal.FACING;

    private final ScarecrowType type;

    public ScarecrowBlock(ScarecrowType type, EnumDyeColor color){
        super(false, type.getBlockProperties(color));
        this.type = type;

        this.setDefaultState(this.getDefaultState().withProperty(FACING, EnumFacing.NORTH).withProperty(BOTTOM, true));
    }

    @Override
    protected InteractionFeedback interact(IBlockState state, World level, BlockPos pos, EntityPlayer player, EnumHand hand, EnumFacing hitSide, Vec3d hitLocation){
        TileEntity entity = level.getTileEntity(pos);
        if(entity instanceof ScarecrowBlockEntity)
            return ((ScarecrowBlockEntity)entity).rightClick(player, hand) ? InteractionFeedback.SUCCESS : InteractionFeedback.PASS;
        return super.interact(state, level, pos, player, hand, hitSide, hitLocation);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World level, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entity, boolean isActualState){
        this.type.getBlockShape(state.getValue(FACING), state.getValue(BOTTOM)).forEachBox(
            box -> addCollisionBoxToList(pos, entityBox, collidingBoxes, box)
        );
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess level, BlockPos pos){
        return this.type.getBlockShape(state.getValue(FACING), state.getValue(BOTTOM)).simplify();
    }

    @Override
    public IBlockState getStateForPlacement(World level, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand){
        if(this.type.is2BlocksHigh() && !level.isAirBlock(pos.up()))
            return null;
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public void onBlockPlacedBy(World level, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack){
        if(this.type.is2BlocksHigh() && !level.isAirBlock(pos))
            level.setBlockState(pos.up(), state.withProperty(BOTTOM, false));
    }

    @Override
    public void breakBlock(World level, BlockPos pos, IBlockState state){
        if(this.type.is2BlocksHigh()){
            boolean bottom = state.getValue(BOTTOM);
            IBlockState state1 = level.getBlockState(bottom ? pos.up() : pos.down());
            if(state1.getBlock() == state.getBlock() && state1.getValue(BOTTOM) != bottom)
                level.setBlockState(bottom ? pos.up() : pos.down(),
                    Blocks.AIR.getDefaultState());
        }
        super.breakBlock(level, pos, state);
    }

    @Override
    protected BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, FACING, BOTTOM);
    }

    @Override
    public TileEntity createNewBlockEntity(){
        return this.type.createBlockEntity();
    }

    @Override
    protected void appendItemInformation(ItemStack stack, @Nullable IBlockAccess level, Consumer<ITextComponent> info, boolean advanced){
        boolean spawners = ScarecrowsTerritoryConfig.loadSpawners.get();
        boolean passive = ScarecrowsTerritoryConfig.passiveMobSpawning.get();

        if(spawners && passive){
            ITextComponent spawnerRange = TextComponents.number(Math.round(ScarecrowsTerritoryConfig.loadSpawnerRange.get())).color(TextFormatting.GOLD).get();
            ITextComponent passiveRange = TextComponents.number(Math.round(ScarecrowsTerritoryConfig.passiveMobRange.get())).color(TextFormatting.GOLD).get();
            info.accept(TextComponents.translation("scarecrowsterritory.primitive_scarecrow.info.both", spawnerRange, passiveRange).color(TextFormatting.GRAY).get());
        }else if(spawners){
            ITextComponent spawnerRange = TextComponents.number(Math.round(ScarecrowsTerritoryConfig.loadSpawnerRange.get())).color(TextFormatting.GOLD).get();
            info.accept(TextComponents.translation("scarecrowsterritory.primitive_scarecrow.info.spawners", spawnerRange).color(TextFormatting.GRAY).get());
        }else if(passive){
            ITextComponent passiveRange = TextComponents.number(Math.round(ScarecrowsTerritoryConfig.passiveMobRange.get())).color(TextFormatting.GOLD).get();
            info.accept(TextComponents.translation("scarecrowsterritory.primitive_scarecrow.info.passive", passiveRange).color(TextFormatting.GRAY).get());
        }
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
