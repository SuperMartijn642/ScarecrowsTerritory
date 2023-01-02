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

    /**
     * {@link MobSpawnerBaseLogic#isActivated()}
     */
    private static boolean isActivated(MobSpawnerBaseLogic spawner){
        try{
            return (boolean)isActivated.invoke(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * {@link MobSpawnerBaseLogic#resetTimer()}
     */
    private static void resetTimer(MobSpawnerBaseLogic spawner){
        try{
            resetTimer.invoke(spawner);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void tickAbstractSpawner(MobSpawnerBaseLogic spawner){
        if(!isActivated(spawner)){
            World level = spawner.getSpawnerWorld();
            BlockPos pos = spawner.getSpawnerPosition();
            if(!(level instanceof WorldServer)){
                double d3 = pos.getX() + level.rand.nextDouble();
                double d4 = pos.getY() + level.rand.nextDouble();
                double d5 = pos.getZ() + level.rand.nextDouble();
                level.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, d3, d4, d5, 0.0D, 0.0D, 0.0D);
                level.spawnParticle(EnumParticleTypes.FLAME, d3, d4, d5, 0.0D, 0.0D, 0.0D);
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
                    NBTTagCompound tag = getSpawnData(spawner).getNbt();

                    NBTTagList list = tag.getTagList("Pos", 6);
                    int j = list.tagCount();
                    double d0 = j >= 1 ? list.getDoubleAt(0) : (double)pos.getX() + (level.rand.nextDouble() - level.rand.nextDouble()) * getSpawnRange(spawner) + 0.5D;
                    double d1 = j >= 2 ? list.getDoubleAt(1) : (double)(pos.getY() + level.rand.nextInt(3) - 1);
                    double d2 = j >= 3 ? list.getDoubleAt(2) : (double)pos.getZ() + (level.rand.nextDouble() - level.rand.nextDouble()) * getSpawnRange(spawner) + 0.5D;
                    Entity entity = AnvilChunkLoader.readWorldEntityPos(tag, level, d0, d1, d2, false);

                    if(entity == null){
                        return;
                    }

                    int k = level.getEntitiesWithinAABB(entity.getClass(), (new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), (pos.getX() + 1), (pos.getY() + 1), (pos.getZ() + 1))).grow(getSpawnRange(spawner))).size();
                    if(k >= getMaxNearbyEntities(spawner)){
                        resetTimer(spawner);
                        return;
                    }

                    EntityLiving entityLiving = entity instanceof EntityLiving ? (EntityLiving)entity : null;
                    entity.setLocationAndAngles(entity.posX, entity.posY, entity.posZ, level.rand.nextFloat() * 360.0F, 0.0F);
                    if(entityLiving == null || net.minecraftforge.event.ForgeEventFactory.canEntitySpawnSpawner(entityLiving, level, (float)entity.posX, (float)entity.posY, (float)entity.posZ, spawner)){
                        if(getSpawnData(spawner).getNbt().getSize() == 1 && getSpawnData(spawner).getNbt().hasKey("id", 8) && entity instanceof EntityLiving){
                            if(!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(entityLiving, level, (float)entity.posX, (float)entity.posY, (float)entity.posZ, spawner))
                                ((EntityLiving)entity).onInitialSpawn(level.getDifficultyForLocation(new BlockPos(entity)), null);
                        }

                        AnvilChunkLoader.spawnEntity(entity, level);
                        level.playEvent(2004, pos, 0);

                        if(entityLiving != null){
                            entityLiving.spawnExplosionParticle();
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
