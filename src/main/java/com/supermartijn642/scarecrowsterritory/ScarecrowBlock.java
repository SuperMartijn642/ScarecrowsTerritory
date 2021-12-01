package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.block.BaseBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class ScarecrowBlock extends BaseBlock implements EntityBlock, SimpleWaterloggedBlock {

    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final ScarecrowType type;

    public ScarecrowBlock(ScarecrowType type, DyeColor color){
        super(type.getRegistryName(color), false, type.getBlockProperties(color));
        this.type = type;

        this.registerDefaultState(this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH).setValue(BOTTOM, true).setValue(WATERLOGGED, false));
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit){
        BlockEntity tile = worldIn.getBlockEntity(pos);
        if(tile instanceof ScarecrowTile)
            return ((ScarecrowTile)tile).rightClick(player, handIn) ? InteractionResult.sidedSuccess(worldIn.isClientSide) : InteractionResult.PASS;
        return InteractionResult.PASS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context){
        return this.type.getBlockShape(state.getValue(BlockStateProperties.HORIZONTAL_FACING), state.getValue(BOTTOM));
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state){
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context){
        if(this.type.is2BlocksHigh() && !context.getLevel().isEmptyBlock(context.getClickedPos().above()) && context.getLevel().getBlockState(context.getClickedPos().above()).getBlock() != Blocks.WATER)
            return null;
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack){
        if(this.type.is2BlocksHigh() && !worldIn.isEmptyBlock(pos) && worldIn.getBlockState(pos).getBlock() != Blocks.WATER){
            FluidState fluidState = worldIn.getFluidState(pos.above());
            worldIn.setBlockAndUpdate(pos.above(), state.setValue(BOTTOM, false).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER));
        }
    }

    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving){
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        builder.add(HorizontalDirectionalBlock.FACING, BOTTOM, WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state){
        return this.type.createTileEntity(pos, state);
    }

    @Override
    public FluidState getFluidState(BlockState state){
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos){
        if(stateIn.getValue(WATERLOGGED))
            worldIn.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, BlockGetter worldIn, List<Component> tooltip, TooltipFlag flagIn){
        boolean spawners = STConfig.loadSpawners.get();
        boolean passive = STConfig.passiveMobSpawning.get();

        if(spawners && passive)
            tooltip.addAll(wrapTooltip("scarecrowsterritory.primitive_scarecrow.info.both", ChatFormatting.AQUA, Math.round(STConfig.loadSpawnerRange.get()), Math.round(STConfig.passiveMobRange.get())));
        else if(spawners)
            tooltip.addAll(wrapTooltip("scarecrowsterritory.primitive_scarecrow.info.spawners", ChatFormatting.AQUA, Math.round(STConfig.loadSpawnerRange.get())));
        else if(passive)
            tooltip.addAll(wrapTooltip("scarecrowsterritory.primitive_scarecrow.info.passive", ChatFormatting.AQUA, Math.round(STConfig.passiveMobRange.get())));
    }

    @OnlyIn(Dist.CLIENT)
    private static List<Component> wrapTooltip(String translationKey, ChatFormatting color, Object... args){
        List<Component> components = new ArrayList<>(1);
        String translation = ClientProxy.translate(translationKey, args).trim();
        StringTokenizer tokenizer = new StringTokenizer(translation, " ");
        StringBuilder builder = new StringBuilder(tokenizer.nextToken());

        while(tokenizer.hasMoreTokens()){
            String token = tokenizer.nextToken();
            if(builder.length() + token.length() + 1 < 25)
                builder.append(' ').append(token);
            else{
                components.add(new TextComponent(builder.toString()).withStyle(color));
                builder = new StringBuilder(token);
            }
        }

        components.add(new TextComponent(builder.toString()).withStyle(color));

        return components;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> blockEntityType){
        return blockEntityType == this.type.tileTileEntityType ? (world2, pos, state2, blockEntity) -> ((ScarecrowTile)blockEntity).tick() : null;
    }
}
