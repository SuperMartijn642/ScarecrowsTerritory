package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.TextComponents;
import com.supermartijn642.core.block.BaseBlock;
import com.supermartijn642.core.block.EntityHoldingBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class ScarecrowBlock extends BaseBlock implements EntityHoldingBlock, IWaterLoggable {

    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final ScarecrowType type;

    public ScarecrowBlock(ScarecrowType type, DyeColor color){
        super(false, type.getBlockProperties(color));
        this.type = type;

        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(BOTTOM, true).setValue(WATERLOGGED, false));
    }

    @Override
    protected InteractionFeedback interact(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, Direction hitSide, Vec3d hitLocation){
        TileEntity entity = level.getBlockEntity(pos);
        if(entity instanceof ScarecrowBlockEntity)
            return ((ScarecrowBlockEntity)entity).rightClick(player, hand) ? InteractionFeedback.SUCCESS : InteractionFeedback.PASS;
        return super.interact(state, level, pos, player, hand, hitSide, hitLocation);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader level, BlockPos pos, ISelectionContext context){
        return this.type.getBlockShape(state.getValue(FACING), state.getValue(BOTTOM)).getUnderlying();
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state){
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context){
        if(this.type.is2BlocksHigh() && !context.getLevel().isEmptyBlock(context.getClickedPos().above()) && context.getLevel().getBlockState(context.getClickedPos().above()).getBlock() != Blocks.WATER)
            return null;
        IFluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public void setPlacedBy(World level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack){
        if(this.type.is2BlocksHigh() && (level.isEmptyBlock(pos.above()) || level.getBlockState(pos.above()).getBlock() == Blocks.WATER)){
            IFluidState fluidState = level.getFluidState(pos.above());
            level.setBlockAndUpdate(pos.above(), state.setValue(BOTTOM, false).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER));
        }
    }

    @Override
    public void onRemove(BlockState state, World level, BlockPos pos, BlockState newState, boolean isMoving){
        if(this.type.is2BlocksHigh() && state.getBlock() != newState.getBlock()){
            boolean bottom = state.getValue(BOTTOM);
            BlockState state1 = level.getBlockState(bottom ? pos.above() : pos.below());
            if(state1.getBlock() == state.getBlock() && state1.getValue(BOTTOM) != bottom)
                level.setBlockAndUpdate(bottom ? pos.above() : pos.below(),
                    state1.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block,BlockState> builder){
        builder.add(FACING, BOTTOM, WATERLOGGED);
    }

    @Override
    public TileEntity createNewBlockEntity(){
        return this.type.createBlockEntity();
    }

    @Override
    public IFluidState getFluidState(BlockState state){
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, IWorld level, BlockPos currentPos, BlockPos facingPos){
        if(state.getValue(WATERLOGGED))
            level.getLiquidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    protected void appendItemInformation(ItemStack stack, @Nullable IBlockReader level, Consumer<ITextComponent> info, boolean advanced){
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
}
