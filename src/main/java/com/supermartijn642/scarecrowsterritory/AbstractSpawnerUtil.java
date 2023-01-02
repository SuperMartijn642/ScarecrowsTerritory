package com.supermartijn642.scarecrowsterritory;

import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.WeightedSpawnerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.AbstractSpawner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class AbstractSpawnerUtil {

    private static final Field spawnDelay;
    private static final Field nextSpawnData;
    private static final Field spin;
    private static final Field oSpin;
    private static final Field spawnCount;
    private static final Field maxNearbyEntities;
    private static final Field spawnRange;

    private static final Method isNearPlayer;
    private static final Method delay;

    static{
        spawnDelay = ReflectionUtil.findField(AbstractSpawner.class, "field_98286_b");
        nextSpawnData = ReflectionUtil.findField(AbstractSpawner.class, "field_98282_f");
        spin = ReflectionUtil.findField(AbstractSpawner.class, "field_98287_c");
        oSpin = ReflectionUtil.findField(AbstractSpawner.class, "field_98284_d");
        spawnCount = ReflectionUtil.findField(AbstractSpawner.class, "field_98294_i");
        maxNearbyEntities = ReflectionUtil.findField(AbstractSpawner.class, "field_98292_k");
        spawnRange = ReflectionUtil.findField(AbstractSpawner.class, "field_98290_m");

        isNearPlayer = ReflectionUtil.findMethod(AbstractSpawner.class, "func_98279_f");
        delay = ReflectionUtil.findMethod(AbstractSpawner.class, "func_98273_j");
    }

    private static int getSpawnDelay(AbstractSpawner spawner){
        try{
            return spawnDelay.getInt(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    private static void setSpawnDelay(AbstractSpawner spawner, int value){
        try{
            spawnDelay.setInt(spawner, value);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static WeightedSpawnerEntity getNextSpawnData(AbstractSpawner spawner){
        try{
            return (WeightedSpawnerEntity)nextSpawnData.get(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private static void setSpin(AbstractSpawner spawner, double value){
        try{
            spin.setDouble(spawner, value);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void setOSpin(AbstractSpawner spawner, double value){
        try{
            oSpin.setDouble(spawner, value);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static int getSpawnCount(AbstractSpawner spawner){
        try{
            return spawnCount.getInt(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    private static int getMaxNearbyEntities(AbstractSpawner spawner){
        try{
            return maxNearbyEntities.getInt(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    private static int getSpawnRange(AbstractSpawner spawner){
        try{
            return spawnRange.getInt(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * {@link AbstractSpawner#isNearPlayer()}
     */
    private static boolean isNearPlayer(AbstractSpawner spawner){
        try{
            return (boolean)isNearPlayer.invoke(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * {@link AbstractSpawner#delay()}
     */
    private static void delay(AbstractSpawner spawner){
        try{
            delay.invoke(spawner);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void tickAbstractSpawner(AbstractSpawner spawner){
        if(!isNearPlayer(spawner)){
            World level = spawner.getLevel();
            BlockPos pos = spawner.getPos();
            if(!(level instanceof ServerWorld)){
                double d3 = pos.getX() + level.random.nextDouble();
                double d4 = pos.getY() + level.random.nextDouble();
                double d5 = pos.getZ() + level.random.nextDouble();
                level.addParticle(ParticleTypes.SMOKE, d3, d4, d5, 0.0D, 0.0D, 0.0D);
                level.addParticle(ParticleTypes.FLAME, d3, d4, d5, 0.0D, 0.0D, 0.0D);
                if(getSpawnDelay(spawner) > 0)
                    setSpawnDelay(spawner, getSpawnDelay(spawner) - 1);

                setOSpin(spawner, spawner.getSpin());
                setSpin(spawner, (spawner.getSpin() + (double)(1000.0F / ((float)getSpawnDelay(spawner) + 200.0F))) % 360.0D);
            }else{
                if(getSpawnDelay(spawner) == -1){
                    delay(spawner);
                }

                if(getSpawnDelay(spawner) > 0){
                    setSpawnDelay(spawner, getSpawnDelay(spawner) - 1);
                    return;
                }

                boolean flag = false;

                for(int i = 0; i < getSpawnCount(spawner); ++i){
                    CompoundNBT tag = getNextSpawnData(spawner).getTag();
                    Optional<EntityType<?>> optional = EntityType.by(tag);
                    if(!optional.isPresent()){
                        delay(spawner);
                        return;
                    }

                    ListNBT list = tag.getList("Pos", 6);
                    int j = list.size();
                    double d0 = j >= 1 ? list.getDouble(0) : (double)pos.getX() + (level.random.nextDouble() - level.random.nextDouble()) * getSpawnRange(spawner) + 0.5D;
                    double d1 = j >= 2 ? list.getDouble(1) : (double)(pos.getY() + level.random.nextInt(3) - 1);
                    double d2 = j >= 3 ? list.getDouble(2) : (double)pos.getZ() + (level.random.nextDouble() - level.random.nextDouble()) * getSpawnRange(spawner) + 0.5D;
                    if(level.noCollision(optional.get().getAABB(d0, d1, d2))){
                        ServerWorld serverLevel = (ServerWorld)level;
                        if(EntitySpawnPlacementRegistry.checkSpawnRules(optional.get(), serverLevel, SpawnReason.SPAWNER, new BlockPos(d0, d1, d2), level.getRandom())){
                            Entity entity = EntityType.loadEntityRecursive(tag, level, (p_221408_6_) -> {
                                p_221408_6_.moveTo(d0, d1, d2, p_221408_6_.yRot, p_221408_6_.xRot);
                                return p_221408_6_;
                            });
                            if(entity == null){
                                delay(spawner);
                                return;
                            }

                            int k = level.getEntitiesOfClass(entity.getClass(), (new AxisAlignedBB((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1))).inflate(getSpawnRange(spawner))).size();
                            if(k >= getMaxNearbyEntities(spawner)){
                                delay(spawner);
                                return;
                            }

                            entity.moveTo(entity.getX(), entity.getY(), entity.getZ(), level.random.nextFloat() * 360.0F, 0.0F);
                            if(entity instanceof MobEntity){
                                MobEntity mobentity = (MobEntity)entity;
                                if(!net.minecraftforge.event.ForgeEventFactory.canEntitySpawnSpawner(mobentity, level, (float)entity.getX(), (float)entity.getY(), (float)entity.getZ(), spawner)){
                                    continue;
                                }

                                if(getNextSpawnData(spawner).getTag().size() == 1 && getNextSpawnData(spawner).getTag().contains("id", 8)){
                                    if(!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(mobentity, level, (float)entity.getX(), (float)entity.getY(), (float)entity.getZ(), spawner, SpawnReason.SPAWNER))
                                        ((MobEntity)entity).finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(entity.blockPosition()), SpawnReason.SPAWNER, (ILivingEntityData)null, (CompoundNBT)null);
                                }
                            }

                            if(!serverLevel.tryAddFreshEntityWithPassengers(entity)){
                                delay(spawner);
                                return;
                            }

                            level.levelEvent(2004, pos, 0);
                            if(entity instanceof MobEntity){
                                ((MobEntity)entity).spawnAnim();
                            }

                            flag = true;
                        }
                    }
                }

                if(flag){
                    delay(spawner);
                }
            }
        }
    }

}
