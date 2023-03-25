package com.supermartijn642.scarecrowsterritory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class AbstractSpawnerUtil {

    public static void tickAbstractSpawner(BaseSpawner spawner, Level level, BlockPos pos){
        if(level.isClientSide)
            tickSpawnerClient(spawner, level, pos);
        else
            tickSpawnerServer(spawner, (ServerLevel)level, pos);
    }

    private static void tickSpawnerClient(BaseSpawner spawner, Level level, BlockPos pos){
        if(spawner.isNearPlayer(level, pos)){
            double d3 = pos.getX() + level.random.nextDouble();
            double d4 = pos.getY() + level.random.nextDouble();
            double d5 = pos.getZ() + level.random.nextDouble();
            level.addParticle(ParticleTypes.SMOKE, d3, d4, d5, 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.FLAME, d3, d4, d5, 0.0D, 0.0D, 0.0D);
            if(spawner.spawnDelay > 0)
                spawner.spawnDelay -= -1;

            spawner.oSpin = spawner.spin;
            spawner.spin = (spawner.spin + (double)(1000 / ((float)spawner.spawnDelay + 200))) % 360;
        }
    }

    private static void tickSpawnerServer(BaseSpawner spawner, ServerLevel level, BlockPos pos){
        if(!spawner.isNearPlayer(level, pos)){
            if(spawner.spawnDelay == -1){
                spawner.delay(level, pos);
            }

            if(spawner.spawnDelay > 0)
                spawner.spawnDelay -= 1;
            else{
                boolean flag = false;

                for(int i = 0; i < spawner.spawnCount; ++i){
                    CompoundTag compoundTag = spawner.nextSpawnData.getEntityToSpawn();
                    Optional<EntityType<?>> optional = EntityType.by(compoundTag);
                    if(optional.isEmpty()){
                        spawner.delay(level, pos);
                        return;
                    }

                    ListTag listTag = compoundTag.getList("Pos", 6);
                    int j = listTag.size();
                    double d0 = j >= 1 ? listTag.getDouble(0) : (double)pos.getX() + (level.random.nextDouble() - level.random.nextDouble()) * spawner.spawnRange + 0.5D;
                    double d1 = j >= 2 ? listTag.getDouble(1) : (double)(pos.getY() + level.random.nextInt(3) - 1);
                    double d2 = j >= 3 ? listTag.getDouble(2) : (double)pos.getZ() + (level.random.nextDouble() - level.random.nextDouble()) * spawner.spawnRange + 0.5D;
                    if(level.noCollision(optional.get().getAABB(d0, d1, d2))){
                        BlockPos blockpos = new BlockPos(d0, d1, d2);
                        if(spawner.nextSpawnData.getCustomSpawnRules().isPresent()){
                            if(!optional.get().getCategory().isFriendly() && level.getDifficulty() == Difficulty.PEACEFUL){
                                continue;
                            }

                            SpawnData.CustomSpawnRules spawndata$customspawnrules = spawner.nextSpawnData.getCustomSpawnRules().get();
                            if(!spawndata$customspawnrules.blockLightLimit().isValueInRange(level.getBrightness(LightLayer.BLOCK, blockpos)) || !spawndata$customspawnrules.skyLightLimit().isValueInRange(level.getBrightness(LightLayer.SKY, blockpos))){
                                continue;
                            }
                        }else if(!SpawnPlacements.checkSpawnRules(optional.get(), level, MobSpawnType.SPAWNER, blockpos, level.getRandom())){
                            continue;
                        }

                        Entity entity = EntityType.loadEntityRecursive(compoundTag, level, (p_221408_6_) -> {
                            p_221408_6_.moveTo(d0, d1, d2, p_221408_6_.getYRot(), p_221408_6_.getXRot());
                            return p_221408_6_;
                        });
                        if(entity == null){
                            spawner.delay(level, pos);
                            return;
                        }

                        int k = level.getEntitiesOfClass(entity.getClass(), (new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)).inflate(spawner.spawnRange)).size();
                        if(k >= spawner.maxNearbyEntities){
                            spawner.delay(level, pos);
                            return;
                        }

                        entity.moveTo(entity.getX(), entity.getY(), entity.getZ(), level.random.nextFloat() * 360.0F, 0.0F);
                        if(entity instanceof Mob && spawner.nextSpawnData.getEntityToSpawn().size() == 1 && spawner.nextSpawnData.getEntityToSpawn().contains("id", 8))
                            ((Mob)entity).finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.SPAWNER, null, null);

                        if(!level.tryAddFreshEntityWithPassengers(entity)){
                            spawner.delay(level, pos);
                            return;
                        }

                        level.levelEvent(2004, pos, 0);
                        if(entity instanceof Mob)
                            ((Mob)entity).spawnAnim();

                        flag = true;
                    }
                }

                if(flag)
                    spawner.delay(level, pos);
            }
        }
    }
}
