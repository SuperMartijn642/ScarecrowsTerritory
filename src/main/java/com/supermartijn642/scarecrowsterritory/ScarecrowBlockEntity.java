package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.block.BaseBlockEntity;
import com.supermartijn642.core.block.TickableBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.DyeColor;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class ScarecrowBlockEntity extends BaseBlockEntity implements TickableBlockEntity {

    private final ScarecrowType type;

    public ScarecrowBlockEntity(ScarecrowType type){
        super(type.blockEntityType);
        this.type = type;
    }

    public boolean rightClick(PlayerEntity player, Hand hand){
        ItemStack stack = player.getItemInHand(hand);
        if(!stack.isEmpty() && stack.getItem() instanceof DyeItem){
            DyeColor color = ((DyeItem)stack.getItem()).getDyeColor();
            BlockState state = this.level.getBlockState(this.worldPosition);
            if(state.getBlock() instanceof ScarecrowBlock){
                this.level.setBlockAndUpdate(this.worldPosition,
                    this.type.blocks.get(color).defaultBlockState()
                        .setValue(HorizontalBlock.FACING, state.getValue(HorizontalBlock.FACING))
                        .setValue(ScarecrowBlock.BOTTOM, state.getValue(ScarecrowBlock.BOTTOM))
                        .setValue(ScarecrowBlock.WATERLOGGED, state.getValue(ScarecrowBlock.WATERLOGGED))
                );
                // other half
                BlockPos pos2 = state.getValue(ScarecrowBlock.BOTTOM) ? this.worldPosition.above() : this.worldPosition.below();
                BlockState state2 = this.level.getBlockState(pos2);
                if(state2.getBlock() instanceof ScarecrowBlock || state2.isAir() || state2.getFluidState().getType().isSame(Fluids.WATER)){
                    this.level.setBlockAndUpdate(pos2,
                        this.type.blocks.get(color).defaultBlockState()
                            .setValue(HorizontalBlock.FACING, state.getValue(HorizontalBlock.FACING))
                            .setValue(ScarecrowBlock.BOTTOM, !state.getValue(ScarecrowBlock.BOTTOM))
                            .setValue(ScarecrowBlock.WATERLOGGED, state2.getFluidState().getType().isSame(Fluids.WATER))
                    );
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void update(){
        if(ScarecrowsTerritoryConfig.loadSpawners.get()){
            Set<BlockPos> spawners = SpawnerTracker.getSpawnersInRange(this.level, this.worldPosition, ScarecrowsTerritoryConfig.loadSpawnerRange.get());
            for(BlockPos spawnerPos : spawners){
                TileEntity entity = this.level.getBlockEntity(spawnerPos);
                if(entity instanceof MobSpawnerTileEntity)
                    AbstractSpawnerUtil.tickAbstractSpawner(((MobSpawnerTileEntity)entity).getSpawner());
            }
        }
    }

    @Override
    protected CompoundNBT writeData(){
        return null;
    }

    @Override
    protected void readData(CompoundNBT compound){
    }
}
