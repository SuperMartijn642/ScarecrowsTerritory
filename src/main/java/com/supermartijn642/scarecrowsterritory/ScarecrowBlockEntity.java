package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.block.BaseBlockEntity;
import com.supermartijn642.core.block.TickableBlockEntity;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.EnumHand;
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

    public boolean rightClick(EntityPlayer player, EnumHand hand){
        ItemStack stack = player.getHeldItem(hand);
        if(!stack.isEmpty() && stack.getItem() instanceof ItemDye){
            EnumDyeColor color = EnumDyeColor.byDyeDamage(stack.getMetadata());
            IBlockState state = this.world.getBlockState(this.pos);
            if(state.getBlock() instanceof ScarecrowBlock){
                this.world.setBlockState(this.pos,
                    this.type.blocks.get(color).getDefaultState()
                        .withProperty(BlockHorizontal.FACING, state.getValue(BlockHorizontal.FACING))
                        .withProperty(ScarecrowBlock.BOTTOM, state.getValue(ScarecrowBlock.BOTTOM))
                );
                // other half
                BlockPos pos2 = state.getValue(ScarecrowBlock.BOTTOM) ? this.pos.up() : this.pos.down();
                IBlockState state2 = this.world.getBlockState(pos2);
                if(state2.getBlock() instanceof ScarecrowBlock || state2.getBlock() == Blocks.AIR){
                    this.world.setBlockState(pos2,
                        this.type.blocks.get(color).getDefaultState()
                            .withProperty(BlockHorizontal.FACING, state.getValue(BlockHorizontal.FACING))
                            .withProperty(ScarecrowBlock.BOTTOM, !state.getValue(ScarecrowBlock.BOTTOM))
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
            Set<BlockPos> spawners = SpawnerTracker.getSpawnersInRange(this.world, this.pos, ScarecrowsTerritoryConfig.loadSpawnerRange.get());
            for(BlockPos spawnerPos : spawners){
                TileEntity entity = this.world.getTileEntity(spawnerPos);
                if(entity instanceof TileEntityMobSpawner)
                    AbstractSpawnerUtil.tickAbstractSpawner(((TileEntityMobSpawner)entity).getSpawnerBaseLogic());
            }
        }
    }

    @Override
    protected NBTTagCompound writeData(){
        return null;
    }

    @Override
    protected void readData(NBTTagCompound compound){
    }
}
