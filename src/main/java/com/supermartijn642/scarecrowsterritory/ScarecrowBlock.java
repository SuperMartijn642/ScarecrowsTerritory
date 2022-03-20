package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.TextComponents;
import com.supermartijn642.core.block.BaseBlock;
import net.minecraft.block.*;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class ScarecrowBlock extends BaseBlock implements IWaterLoggable {

    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final ScarecrowType type;

    public ScarecrowBlock(ScarecrowType type, DyeColor color){
        super(type.getRegistryName(color), false, type.getBlockProperties(color));
        this.type = type;

        this.registerDefaultState(this.defaultBlockState().setValue(HorizontalBlock.FACING, Direction.NORTH).setValue(BOTTOM, true).setValue(WATERLOGGED, false));
    }

    @Override
    public boolean use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit){
        TileEntity tile = worldIn.getBlockEntity(pos);
        if(tile instanceof ScarecrowTile)
            return ((ScarecrowTile)tile).rightClick(player, handIn);
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context){
        return this.type.getBlockShape(state.getValue(BlockStateProperties.HORIZONTAL_FACING), state.getValue(BOTTOM));
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context){
        if(this.type.is2BlocksHigh() && !context.getLevel().isEmptyBlock(context.getClickedPos().above()) && context.getLevel().getBlockState(context.getClickedPos().above()).getBlock() != Blocks.WATER)
            return null;
        IFluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState().setValue(HorizontalBlock.FACING, context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public void setPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack){
        if(this.type.is2BlocksHigh() && (worldIn.isEmptyBlock(pos.above()) || worldIn.getBlockState(pos.above()).getBlock() == Blocks.WATER)){
            IFluidState fluidState = worldIn.getFluidState(pos.above());
            worldIn.setBlockAndUpdate(pos.above(), state.setValue(BOTTOM, false).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER));
        }
    }

    @Override
    public void onRemove(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving){
        if(this.type.is2BlocksHigh() && state.getBlock() != newState.getBlock()){
            boolean bottom = state.getValue(BOTTOM);
            BlockState state1 = worldIn.getBlockState(bottom ? pos.above() : pos.below());
            if(state1.getBlock() == state.getBlock() && state1.getValue(BOTTOM) != bottom)
                worldIn.setBlockAndUpdate(bottom ? pos.above() : pos.below(),
                    state1.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        super.onRemove(state, worldIn, pos, newState, isMoving);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block,BlockState> builder){
        builder.add(HorizontalBlock.FACING, BOTTOM, WATERLOGGED);
    }

    @Override
    public boolean hasTileEntity(BlockState state){
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world){
        return this.type.createTileEntity();
    }

    @Override
    public IFluidState getFluidState(BlockState state){
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos){
        if(stateIn.getValue(WATERLOGGED))
            worldIn.getLiquidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, IBlockReader worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn){
        boolean spawners = STConfig.loadSpawners.get();
        boolean passive = STConfig.passiveMobSpawning.get();

        if(spawners && passive){
            ITextComponent spawnerRange = TextComponents.number(Math.round(STConfig.loadSpawnerRange.get())).color(TextFormatting.GOLD).get();
            ITextComponent passiveRange = TextComponents.number(Math.round(STConfig.passiveMobRange.get())).color(TextFormatting.GOLD).get();
            tooltip.add(TextComponents.translation("scarecrowsterritory.primitive_scarecrow.info.both", spawnerRange, passiveRange).color(TextFormatting.GRAY).get());
        }else if(spawners){
            ITextComponent spawnerRange = TextComponents.number(Math.round(STConfig.loadSpawnerRange.get())).color(TextFormatting.GOLD).get();
            tooltip.add(TextComponents.translation("scarecrowsterritory.primitive_scarecrow.info.spawners", spawnerRange).color(TextFormatting.GRAY).get());
        }else if(passive){
            ITextComponent passiveRange = TextComponents.number(Math.round(STConfig.passiveMobRange.get())).color(TextFormatting.GOLD).get();
            tooltip.add(TextComponents.translation("scarecrowsterritory.primitive_scarecrow.info.passive", passiveRange).color(TextFormatting.GRAY).get());
        }
    }

    @Override
    public BlockRenderLayer getRenderLayer(){
        return this.type.getRenderLayer();
    }

    @Override
    public boolean isRedstoneConductor(BlockState state, IBlockReader worldIn, BlockPos pos){
        return false;
    }
}
