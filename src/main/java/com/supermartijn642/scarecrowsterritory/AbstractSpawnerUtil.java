package com.supermartijn642.scarecrowsterritory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.WeightedSpawnerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
        spawnDelay = ReflectionUtil.findField(MobSpawnerBaseLogic.class, "field_98286_b");
        spawnData = ReflectionUtil.findField(MobSpawnerBaseLogic.class, "field_98282_f");
        mobRotation = ReflectionUtil.findField(MobSpawnerBaseLogic.class, "field_98287_c");
        prevMobRotation = ReflectionUtil.findField(MobSpawnerBaseLogic.class, "field_98284_d");
        spawnCount = ReflectionUtil.findField(MobSpawnerBaseLogic.class, "field_98294_i");
        maxNearbyEntities = ReflectionUtil.findField(MobSpawnerBaseLogic.class, "field_98292_k");
        spawnRange = ReflectionUtil.findField(MobSpawnerBaseLogic.class, "field_98290_m");

        isActivated = ReflectionUtil.findMethod(MobSpawnerBaseLogic.class, "func_98279_f", boolean.class);
        resetTimer = ReflectionUtil.findMethod(MobSpawnerBaseLogic.class, "func_98273_j", void.class);
    }

    private static int getSpawnDelay(MobSpawnerBaseLogic spawner){
        try{
            return spawnDelay.getInt(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    private static void setSpawnDelay(MobSpawnerBaseLogic spawner, int value){
        try{
            spawnDelay.setInt(spawner, value);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static WeightedSpawnerEntity getSpawnData(MobSpawnerBaseLogic spawner){
        try{
            return (WeightedSpawnerEntity)spawnData.get(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private static void setMobRotation(MobSpawnerBaseLogic spawner, double value){
        try{
            mobRotation.setDouble(spawner, value);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void setPrevMobRotation(MobSpawnerBaseLogic spawner, double value){
        try{
            prevMobRotation.setDouble(spawner, value);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static int getSpawnCount(MobSpawnerBaseLogic spawner){
        try{
            return spawnCount.getInt(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    private static int getMaxNearbyEntities(MobSpawnerBaseLogic spawner){
        try{
            return maxNearbyEntities.getInt(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    private static int getSpawnRange(MobSpawnerBaseLogic spawner){
        try{
            return spawnRange.getInt(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    private static boolean isActivated(MobSpawnerBaseLogic spawner){
        try{
            return (boolean)isActivated.invoke(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    private static void resetTimer(MobSpawnerBaseLogic spawner){
        try{
            resetTimer.invoke(spawner);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void tickAbstractSpawner(MobSpawnerBaseLogic spawner){
        if(!isActivated(spawner)){
            World world = spawner.getSpawnerWorld();
            BlockPos blockpos = spawner.getSpawnerPosition();
            if(!(world instanceof WorldServer)){
                double d3 = (double)blockpos.getX() + world.rand.nextDouble();
                double d4 = (double)blockpos.getY() + world.rand.nextDouble();
                double d5 = (double)blockpos.getZ() + world.rand.nextDouble();
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, d3, d4, d5, 0.0D, 0.0D, 0.0D);
                world.spawnParticle(EnumParticleTypes.FLAME, d3, d4, d5, 0.0D, 0.0D, 0.0D);
                if(getSpawnDelay(spawner) > 0)
                    setSpawnDelay(spawner, getSpawnDelay(spawner) - 1);

                setPrevMobRotation(spawner, spawner.getMobRotation());
                setMobRotation(spawner, (spawner.getMobRotation() + (double)(1000.0F / ((float)getSpawnDelay(spawner) + 200.0F))) % 360.0D);
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
                    NBTTagCompound compound = getSpawnData(spawner).getNbt();
                    NBTTagList nbttaglist = compound.getTagList("Pos", 6);
                    int j = nbttaglist.tagCount();
                    double d0 = j >= 1 ? nbttaglist.getDoubleAt(0) : (double)blockpos.getX() + (world.rand.nextDouble() - world.rand.nextDouble()) * getSpawnRange(spawner) + 0.5D;
                    double d1 = j >= 2 ? nbttaglist.getDoubleAt(1) : (double)(blockpos.getY() + world.rand.nextInt(3) - 1);
                    double d2 = j >= 3 ? nbttaglist.getDoubleAt(2) : (double)blockpos.getZ() + (world.rand.nextDouble() - world.rand.nextDouble()) * getSpawnRange(spawner) + 0.5D;
                    Entity entity = AnvilChunkLoader.readWorldEntityPos(compound, world, d0, d1, d2, false);

                    if(entity == null){
                        return;
                    }

                    int k = world.getEntitiesWithinAABB(entity.getClass(), (new AxisAlignedBB(blockpos.getX(), blockpos.getY(), blockpos.getZ(), (blockpos.getX() + 1), (blockpos.getY() + 1), (blockpos.getZ() + 1))).grow(getSpawnRange(spawner))).size();
                    if(k >= getMaxNearbyEntities(spawner)){
                        resetTimer(spawner);
                        return;
                    }

                    EntityLiving entityliving = entity instanceof EntityLiving ? (EntityLiving)entity : null;
                    entity.setLocationAndAngles(entity.posX, entity.posY, entity.posZ, world.rand.nextFloat() * 360.0F, 0.0F);
                    if(entityliving == null || net.minecraftforge.event.ForgeEventFactory.canEntitySpawnSpawner(entityliving, world, (float)entity.posX, (float)entity.posY, (float)entity.posZ, spawner)){
                        if(getSpawnData(spawner).getNbt().getSize() == 1 && getSpawnData(spawner).getNbt().hasKey("id", 8) && entity instanceof EntityLiving){
                            if(!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(entityliving, world, (float)entity.posX, (float)entity.posY, (float)entity.posZ, spawner))
                                ((EntityLiving)entity).onInitialSpawn(world.getDifficultyForLocation(new BlockPos(entity)), null);
                        }

                        AnvilChunkLoader.spawnEntity(entity, world);
                        world.playEvent(2004, blockpos, 0);

                        if(entityliving != null){
                            entityliving.spawnExplosionParticle();
                        }

                        flag = true;
                    }
                }

                if(flag){
                    resetTimer(spawner);
                }
            }

        }
    }

}
