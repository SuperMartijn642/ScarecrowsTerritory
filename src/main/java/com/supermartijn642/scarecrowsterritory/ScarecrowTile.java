package com.supermartijn642.scarecrowsterritory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class ScarecrowTile extends TileEntity implements ITickableTileEntity {

    private final ScarecrowType type;

    public ScarecrowTile(ScarecrowType type){
        super(type.tileTileEntityType);
        this.type = type;
    }

    public boolean rightClick(PlayerEntity player){
        return false;
    }

    @Override
    public void tick(){
        if(STConfig.INSTANCE.loadSpawners.get()){
            Set<BlockPos> spawners = SpawnerTracker.getSpawnersInRange(this.world, this.pos, STConfig.INSTANCE.loadSpawnerRange.get());
            for(BlockPos spawnerPos : spawners){
                TileEntity tileEntity = this.world.getTileEntity(spawnerPos);
                if(tileEntity instanceof MobSpawnerTileEntity)
                    AbstractSpawnerUtil.tickAbstractSpawner(((MobSpawnerTileEntity)tileEntity).getSpawnerBaseLogic());
            }
        }
    }


}
