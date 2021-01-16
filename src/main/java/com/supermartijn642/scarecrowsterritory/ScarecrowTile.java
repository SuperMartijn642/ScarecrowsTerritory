package com.supermartijn642.scarecrowsterritory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class ScarecrowTile extends TileEntity implements ITickable {

    public static class PrimitiveScarecrowTile extends ScarecrowTile {
        public PrimitiveScarecrowTile(){
            super(ScarecrowType.PRIMITIVE);
        }
    }

    private final ScarecrowType type;

    public ScarecrowTile(ScarecrowType type){
        super();
        this.type = type;
    }

    public boolean rightClick(EntityPlayer player){
        return false;
    }

    @Override
    public void update(){
        if(STConfig.loadSpawners){
            Set<BlockPos> spawners = SpawnerTracker.getSpawnersInRange(this.world, this.pos, STConfig.loadSpawnerRange);
            for(BlockPos spawnerPos : spawners){
                TileEntity tileEntity = this.world.getTileEntity(spawnerPos);
                if(tileEntity instanceof TileEntityMobSpawner)
                    AbstractSpawnerUtil.tickAbstractSpawner(((TileEntityMobSpawner)tileEntity).getSpawnerBaseLogic());
            }
        }
    }


}
