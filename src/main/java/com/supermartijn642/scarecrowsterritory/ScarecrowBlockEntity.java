package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.block.BaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class ScarecrowBlockEntity extends BaseBlockEntity {

    private final ScarecrowType type;

    public ScarecrowBlockEntity(ScarecrowType type, BlockPos pos, BlockState state){
        super(type.blockEntityType, pos, state);
        this.type = type;
    }

    public boolean rightClick(Player player, InteractionHand hand){
        ItemStack stack = player.getItemInHand(hand);
        DyeColor dye = stack.get(DataComponents.DYE);
        if(dye == null)
            return false;
        BlockState state = this.level.getBlockState(this.worldPosition);
        if(state.getBlock() instanceof ScarecrowBlock){
            this.level.setBlockAndUpdate(this.worldPosition,
                this.type.blocks.get(dye).defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, state.getValue(HorizontalDirectionalBlock.FACING))
                    .setValue(ScarecrowBlock.BOTTOM, state.getValue(ScarecrowBlock.BOTTOM))
                    .setValue(ScarecrowBlock.WATERLOGGED, state.getValue(ScarecrowBlock.WATERLOGGED))
            );
            // other half
            BlockPos pos2 = state.getValue(ScarecrowBlock.BOTTOM) ? this.worldPosition.above() : this.worldPosition.below();
            BlockState state2 = this.level.getBlockState(pos2);
            if(state2.getBlock() instanceof ScarecrowBlock || state2.isAir() || state2.getFluidState().getType().isSame(Fluids.WATER)){
                this.level.setBlockAndUpdate(pos2,
                    this.type.blocks.get(dye).defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, state.getValue(HorizontalDirectionalBlock.FACING))
                        .setValue(ScarecrowBlock.BOTTOM, !state.getValue(ScarecrowBlock.BOTTOM))
                        .setValue(ScarecrowBlock.WATERLOGGED, state2.getFluidState().getType().isSame(Fluids.WATER))
                );
            }
        }
        return true;
    }

    @Override
    protected void writeData(ValueOutput output){
    }

    @Override
    protected void readData(ValueInput input){
    }
}
