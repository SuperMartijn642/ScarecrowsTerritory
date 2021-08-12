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
    private static final Field spawnData;
    private static final Field mobRotation;
    private static final Field prevMobRotation;
    private static final Field spawnCount;
    private static final Field maxNearbyEntities;
    private static final Field spawnRange;

    private static final Method isActivated;
    private static final Method resetTimer;

    static{
        spawnDelay = ReflectionUtil.findField(AbstractSpawner.class, "spawnDelay");
        spawnData = ReflectionUtil.findField(AbstractSpawner.class, "nextSpawnData");
        mobRotation = ReflectionUtil.findField(AbstractSpawner.class, "spin");
        prevMobRotation = ReflectionUtil.findField(AbstractSpawner.class, "oSpin");
        spawnCount = ReflectionUtil.findField(AbstractSpawner.class, "spawnCount");
        maxNearbyEntities = ReflectionUtil.findField(AbstractSpawner.class, "maxNearbyEntities");
        spawnRange = ReflectionUtil.findField(AbstractSpawner.class, "spawnRange");

        isActivated = ReflectionUtil.findMethod(AbstractSpawner.class, "isNearPlayer");
        resetTimer = ReflectionUtil.findMethod(AbstractSpawner.class, "delay");
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

    private static WeightedSpawnerEntity getSpawnData(AbstractSpawner spawner){
        try{
            return (WeightedSpawnerEntity)spawnData.get(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private static void setMobRotation(AbstractSpawner spawner, double value){
        try{
            mobRotation.setDouble(spawner, value);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void setPrevMobRotation(AbstractSpawner spawner, double value){
        try{
            prevMobRotation.setDouble(spawner, value);
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

    private static boolean isActivated(AbstractSpawner spawner){
        try{
            return (boolean)isActivated.invoke(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    private static void resetTimer(AbstractSpawner spawner){
        try{
            resetTimer.invoke(spawner);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void tickAbstractSpawner(AbstractSpawner spawner){
        if(!isActivated(spawner)){
            World world = spawner.getLevel();
            BlockPos blockpos = spawner.getPos();
            if(!(world instanceof ServerWorld)){
                double d3 = (double)blockpos.getX() + world.random.nextDouble();
                double d4 = (double)blockpos.getY() + world.random.nextDouble();
                double d5 = (double)blockpos.getZ() + world.random.nextDouble();
                world.addParticle(ParticleTypes.SMOKE, d3, d4, d5, 0.0D, 0.0D, 0.0D);
                world.addParticle(ParticleTypes.FLAME, d3, d4, d5, 0.0D, 0.0D, 0.0D);
                if(getSpawnDelay(spawner) > 0)
                    setSpawnDelay(spawner, getSpawnDelay(spawner) - 1);

                setPrevMobRotation(spawner, spawner.getSpin());
                setMobRotation(spawner, (spawner.getSpin() + (double)(1000.0F / ((float)getSpawnDelay(spawner) + 200.0F))) % 360.0D);
            }else{
                if(getSpawnDelay(spawner) == -1){
                    resetTimer(spawner);
                }

                if(getSpawnDelay(spawner) > 0){
                    setSpawnDelay(spawner, getSpawnDelay(spawner) - 1);
                    return;
                }

                boolean flag = false;

                for(int i = 0; i < getSpawnCount(spawner); ++i){
                    CompoundNBT compoundnbt = getSpawnData(spawner).getTag();
                    Optional<EntityType<?>> optional = EntityType.by(compoundnbt);
                    if(!optional.isPresent()){
                        resetTimer(spawner);
                        return;
                    }

                    ListNBT listnbt = compoundnbt.getList("Pos", 6);
                    int j = listnbt.size();
                    double d0 = j >= 1 ? listnbt.getDouble(0) : (double)blockpos.getX() + (world.random.nextDouble() - world.random.nextDouble()) * getSpawnRange(spawner) + 0.5D;
                    double d1 = j >= 2 ? listnbt.getDouble(1) : (double)(blockpos.getY() + world.random.nextInt(3) - 1);
                    double d2 = j >= 3 ? listnbt.getDouble(2) : (double)blockpos.getZ() + (world.random.nextDouble() - world.random.nextDouble()) * getSpawnRange(spawner) + 0.5D;
                    if(world.noCollision(optional.get().getAABB(d0, d1, d2))){
                        ServerWorld serverworld = (ServerWorld)world;
                        if(EntitySpawnPlacementRegistry.checkSpawnRules(optional.get(), serverworld, SpawnReason.SPAWNER, new BlockPos(d0, d1, d2), world.getRandom())){
                            Entity entity = EntityType.loadEntityRecursive(compoundnbt, world, (p_221408_6_) -> {
                                p_221408_6_.moveTo(d0, d1, d2, p_221408_6_.yRot, p_221408_6_.xRot);
                                return p_221408_6_;
                            });
                            if(entity == null){
                                resetTimer(spawner);
                                return;
                            }

                            int k = world.getEntitiesOfClass(entity.getClass(), (new AxisAlignedBB((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ(), (double)(blockpos.getX() + 1), (double)(blockpos.getY() + 1), (double)(blockpos.getZ() + 1))).inflate(getSpawnRange(spawner))).size();
                            if(k >= getMaxNearbyEntities(spawner)){
                                resetTimer(spawner);
                                return;
                            }

                            entity.moveTo(entity.x, entity.y, entity.z, world.random.nextFloat() * 360.0F, 0.0F);
                            if(entity instanceof MobEntity){
                                MobEntity mobentity = (MobEntity)entity;
                                if(!net.minecraftforge.event.ForgeEventFactory.canEntitySpawnSpawner(mobentity, world, (float)entity.x, (float)entity.y, (float)entity.z, spawner)){
                                    continue;
                                }

                                if(getSpawnData(spawner).getTag().size() == 1 && getSpawnData(spawner).getTag().contains("id", 8)){
                                    if(!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(mobentity, world, (float)entity.x, (float)entity.y, (float)entity.z, spawner, SpawnReason.SPAWNER))
                                        ((MobEntity)entity).finalizeSpawn(serverworld, world.getCurrentDifficultyAt(entity.getCommandSenderBlockPosition()), SpawnReason.SPAWNER, (ILivingEntityData)null, (CompoundNBT)null);
                                }
                            }

                            addWithPassengers(world, entity);
                            world.levelEvent(2004, blockpos, 0);
                            if(entity instanceof MobEntity){
                                ((MobEntity)entity).spawnAnim();
                            }

                            flag = true;
                        }
                    }
                }

                if(flag){
                    resetTimer(spawner);
                }
            }

        }
    }

    private static void addWithPassengers(World world, Entity entityIn){
        if(world.addFreshEntity(entityIn)){
            for(Entity entity : entityIn.getPassengers()){
                world.addFreshEntity(entity);
            }

        }
    }

}
