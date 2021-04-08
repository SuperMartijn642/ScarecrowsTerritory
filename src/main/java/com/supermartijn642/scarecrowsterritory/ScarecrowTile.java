package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.block.BaseTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.DyeColor;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class ScarecrowTile extends BaseTileEntity implements ITickableTileEntity {

    private final ScarecrowType type;

    public ScarecrowTile(ScarecrowType type){
        super(type.tileTileEntityType);
        this.type = type;
    }

    public boolean rightClick(PlayerEntity player, Hand hand){
        ItemStack stack = player.getHeldItem(hand);
        if(!stack.isEmpty() && stack.getItem() instanceof DyeItem){
            DyeColor color = ((DyeItem)stack.getItem()).getDyeColor();
            BlockState state = this.world.getBlockState(this.pos);
            if(state.getBlock() instanceof ScarecrowBlock){
                this.world.setBlockState(this.pos,
                    this.type.blocks.get(color).getDefaultState()
                        .with(HorizontalBlock.HORIZONTAL_FACING, state.get(HorizontalBlock.HORIZONTAL_FACING))
                        .with(ScarecrowBlock.BOTTOM, state.get(ScarecrowBlock.BOTTOM))
                        .with(ScarecrowBlock.WATERLOGGED, state.get(ScarecrowBlock.WATERLOGGED))
                );
                // other half
                BlockPos pos2 = state.get(ScarecrowBlock.BOTTOM) ? this.pos.up() : this.pos.down();
                BlockState state2 = this.world.getBlockState(pos2);
                if(state2.getBlock() instanceof ScarecrowBlock || state2.isAir() || state2.getFluidState().getFluid().isEquivalentTo(Fluids.WATER)){
                    this.world.setBlockState(pos2,
                        this.type.blocks.get(color).getDefaultState()
                            .with(HorizontalBlock.HORIZONTAL_FACING, state.get(HorizontalBlock.HORIZONTAL_FACING))
                            .with(ScarecrowBlock.BOTTOM, !state.get(ScarecrowBlock.BOTTOM))
                            .with(ScarecrowBlock.WATERLOGGED, state2.getFluidState().getFluid().isEquivalentTo(Fluids.WATER))
                    );
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void tick(){
        if(STConfig.loadSpawners.get()){
            Set<BlockPos> spawners = SpawnerTracker.getSpawnersInRange(this.world, this.pos, STConfig.loadSpawnerRange.get());
            for(BlockPos spawnerPos : spawners){
                TileEntity tileEntity = this.world.getTileEntity(spawnerPos);
                if(tileEntity instanceof MobSpawnerTileEntity)
                    AbstractSpawnerUtil.tickAbstractSpawner(((MobSpawnerTileEntity)tileEntity).getSpawnerBaseLogic());
            }
        }
    }

    @Override
    protected CompoundNBT writeData(){
        return null;
    }

    @Override
    protected void readData(CompoundNBT compoundNBT){
    }
}
