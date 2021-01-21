package com.supermartijn642.scarecrowsterritory;

import net.minecraft.block.*;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class ScarecrowBlock extends Block implements IWaterLoggable {

    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final ScarecrowType type;

    public ScarecrowBlock(ScarecrowType type){
        super(type.getBlockProperties());
        this.type = type;

        this.setRegistryName(type.getRegistryName());
        this.setDefaultState(this.getDefaultState().with(HorizontalBlock.HORIZONTAL_FACING, Direction.NORTH).with(BOTTOM, true).with(WATERLOGGED, false));
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit){
        TileEntity tile = worldIn.getTileEntity(pos);
        if(tile instanceof ScarecrowTile)
            return ((ScarecrowTile)tile).rightClick(player) ? ActionResultType.CONSUME : ActionResultType.PASS;
        return ActionResultType.PASS;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context){
        return this.type.getBlockShape(state.get(BlockStateProperties.HORIZONTAL_FACING), state.get(BOTTOM));
    }

    @Override
    public boolean isTransparent(BlockState state){
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context){
        if(this.type.is2BlocksHigh() && !context.getWorld().isAirBlock(context.getPos().up()) && context.getWorld().getBlockState(context.getPos().up()).getBlock() != Blocks.WATER)
            return null;
        FluidState fluidState = context.getWorld().getFluidState(context.getPos());
        return this.getDefaultState().with(HorizontalBlock.HORIZONTAL_FACING, context.getPlacementHorizontalFacing().getOpposite()).with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack){
        if(this.type.is2BlocksHigh() && !worldIn.isAirBlock(pos) && worldIn.getBlockState(pos).getBlock() != Blocks.WATER){
            FluidState fluidState = worldIn.getFluidState(pos.up());
            worldIn.setBlockState(pos.up(), state.with(BOTTOM, false).with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER));
        }
    }

    @Override
    public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving){
        if(this.type.is2BlocksHigh() && state.getBlock() != newState.getBlock()){
            boolean bottom = state.get(BOTTOM);
            BlockState state1 = worldIn.getBlockState(bottom ? pos.up() : pos.down());
            if(state1.getBlock() == state.getBlock() && state1.get(BOTTOM) != bottom)
                worldIn.setBlockState(bottom ? pos.up() : pos.down(),
                    state1.get(WATERLOGGED) ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState());
        }
        super.onReplaced(state, worldIn, pos, newState, isMoving);
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block,BlockState> builder){
        builder.add(HorizontalBlock.HORIZONTAL_FACING, BOTTOM, WATERLOGGED);
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
    public FluidState getFluidState(BlockState state){
        return state.get(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos){
        if(stateIn.get(WATERLOGGED))
            worldIn.getPendingFluidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickRate(worldIn));
        return super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, IBlockReader worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn){
        boolean spawners = STConfig.loadSpawners.get();
        boolean passive = STConfig.passiveMobSpawning.get();

        if(spawners && passive)
            tooltip.addAll(wrapTooltip("scarecrowsterritory.primitive_scarecrow.info.both", TextFormatting.AQUA, Math.round(STConfig.loadSpawnerRange.get()), Math.round(STConfig.passiveMobRange.get())));
        else if(spawners)
            tooltip.addAll(wrapTooltip("scarecrowsterritory.primitive_scarecrow.info.spawners", TextFormatting.AQUA, Math.round(STConfig.loadSpawnerRange.get())));
        else if(passive)
            tooltip.addAll(wrapTooltip("scarecrowsterritory.primitive_scarecrow.info.passive", TextFormatting.AQUA, Math.round(STConfig.passiveMobRange.get())));
    }

    @OnlyIn(Dist.CLIENT)
    private static List<ITextComponent> wrapTooltip(String translationKey, TextFormatting color, Object... args){
        List<ITextComponent> components = new ArrayList<>(1);
        String translation = ClientProxy.translate(translationKey, args).trim();
        StringTokenizer tokenizer = new StringTokenizer(translation, " ");
        StringBuilder builder = new StringBuilder(tokenizer.nextToken());

        while(tokenizer.hasMoreTokens()){
            String token = tokenizer.nextToken();
            if(builder.length() + token.length() + 1 < 25)
                builder.append(' ').append(token);
            else{
                components.add(new StringTextComponent(builder.toString()).mergeStyle(color));
                builder = new StringBuilder(token);
            }
        }

        components.add(new StringTextComponent(builder.toString()).mergeStyle(color));

        return components;
    }
}
