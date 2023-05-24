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
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.MobSpawnEvent;

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
        spawnDelay = ReflectionUtil.findField(BaseSpawner.class, "f_45442_");
        nextSpawnData = ReflectionUtil.findField(BaseSpawner.class, "f_45444_");
        spin = ReflectionUtil.findField(BaseSpawner.class, "f_45445_");
        oSpin = ReflectionUtil.findField(BaseSpawner.class, "f_45446_");
        spawnCount = ReflectionUtil.findField(BaseSpawner.class, "f_45449_");
        maxNearbyEntities = ReflectionUtil.findField(BaseSpawner.class, "f_45451_");
        spawnRange = ReflectionUtil.findField(BaseSpawner.class, "f_45453_");

        isNearPlayer = ReflectionUtil.findMethod(BaseSpawner.class, "m_151343_", Level.class, BlockPos.class);
        delay = ReflectionUtil.findMethod(BaseSpawner.class, "m_151350_", Level.class, BlockPos.class);
    }

    private static int getSpawnDelay(BaseSpawner spawner){
        try{
            return spawnDelay.getInt(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    private static void setSpawnDelay(BaseSpawner spawner, int value){
        try{
            spawnDelay.setInt(spawner, value);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static SpawnData getNextSpawnData(BaseSpawner spawner){
        try{
            return (SpawnData)nextSpawnData.get(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private static void setSpin(BaseSpawner spawner, double value){
        try{
            spin.setDouble(spawner, value);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void setOSpin(BaseSpawner spawner, double value){
        try{
            oSpin.setDouble(spawner, value);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static int getSpawnCount(BaseSpawner spawner){
        try{
            return spawnCount.getInt(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    private static int getMaxNearbyEntities(BaseSpawner spawner){
        try{
            return maxNearbyEntities.getInt(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    private static int getSpawnRange(BaseSpawner spawner){
        try{
            return spawnRange.getInt(spawner);
        }catch(Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * {@link BaseSpawner#isNearPlayer(Level, BlockPos)}
     */
    private static boolean isNearPlayer(BaseSpawner spawner, Level level, BlockPos pos){
        try{
            return (boolean)isNearPlayer.invoke(spawner, level, pos);
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * {@link BaseSpawner#delay(Level, BlockPos)}
     */
    private static void delay(BaseSpawner spawner, Level level, BlockPos pos){
        try{
            delay.invoke(spawner, level, pos);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void tickAbstractSpawner(BaseSpawner spawner, Level level, BlockPos pos){
        if(level.isClientSide)
            tickSpawnerClient(spawner, level, pos);
        else
            tickSpawnerServer(spawner, (ServerLevel)level, pos);
    }

    private static void tickSpawnerClient(BaseSpawner spawner, Level level, BlockPos pos){
        if(isNearPlayer(spawner, level, pos)){
            double d3 = pos.getX() + level.random.nextDouble();
            double d4 = pos.getY() + level.random.nextDouble();
            double d5 = pos.getZ() + level.random.nextDouble();
            level.addParticle(ParticleTypes.SMOKE, d3, d4, d5, 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.FLAME, d3, d4, d5, 0.0D, 0.0D, 0.0D);
            if(getSpawnDelay(spawner) > 0)
                setSpawnDelay(spawner, getSpawnDelay(spawner) - 1);

            setOSpin(spawner, spawner.getSpin());
            setSpin(spawner, (spawner.getSpin() + (double)(1000.0F / ((float)getSpawnDelay(spawner) + 200.0F))) % 360.0D);
        }
    }

    private static void tickSpawnerServer(BaseSpawner spawner, ServerLevel level, BlockPos pos){
        if(!isNearPlayer(spawner, level, pos)){
            if(getSpawnDelay(spawner) == -1){
                delay(spawner, level, pos);
            }

            if(getSpawnDelay(spawner) > 0){
                setSpawnDelay(spawner, getSpawnDelay(spawner) - 1);
            }else{
                boolean flag = false;

                for(int i = 0; i < getSpawnCount(spawner); ++i){
                    CompoundTag compoundTag = getNextSpawnData(spawner).getEntityToSpawn();
                    Optional<EntityType<?>> optional = EntityType.by(compoundTag);
                    if(!optional.isPresent()){
                        delay(spawner, level, pos);
                        return;
                    }

                    ListTag listTag = compoundTag.getList("Pos", 6);
                    int j = listTag.size();
                    double d0 = j >= 1 ? listTag.getDouble(0) : (double)pos.getX() + (level.random.nextDouble() - level.random.nextDouble()) * getSpawnRange(spawner) + 0.5D;
                    double d1 = j >= 2 ? listTag.getDouble(1) : (double)(pos.getY() + level.random.nextInt(3) - 1);
                    double d2 = j >= 3 ? listTag.getDouble(2) : (double)pos.getZ() + (level.random.nextDouble() - level.random.nextDouble()) * getSpawnRange(spawner) + 0.5D;
                    if(level.noCollision(optional.get().getAABB(d0, d1, d2))){
                        BlockPos blockpos = new BlockPos((int)d0, (int)d1, (int)d2);
                        if(getNextSpawnData(spawner).getCustomSpawnRules().isPresent()){
                            if(!optional.get().getCategory().isFriendly() && level.getDifficulty() == Difficulty.PEACEFUL){
                                continue;
                            }

                            SpawnData.CustomSpawnRules spawndata$customspawnrules = getNextSpawnData(spawner).getCustomSpawnRules().get();
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
                            delay(spawner, level, pos);
                            return;
                        }

                        int k = level.getEntitiesOfClass(entity.getClass(), (new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)).inflate(getSpawnRange(spawner))).size();
                        if(k >= getMaxNearbyEntities(spawner)){
                            delay(spawner, level, pos);
                            return;
                        }

                        entity.moveTo(entity.getX(), entity.getY(), entity.getZ(), level.random.nextFloat() * 360.0F, 0.0F);
                        if(entity instanceof Mob){
                            Mob mob = (Mob)entity;
                            if(getNextSpawnData(spawner).getCustomSpawnRules().isEmpty() && !mob.checkSpawnRules(level, MobSpawnType.SPAWNER) || !mob.checkSpawnObstruction(level))
                                continue;

                            MobSpawnEvent.FinalizeSpawn event = ForgeEventFactory.onFinalizeSpawnSpawner(mob, level, level.getCurrentDifficultyAt(entity.blockPosition()), null, compoundTag, spawner);
                            if(event != null && getNextSpawnData(spawner).getEntityToSpawn().size() == 1 && getNextSpawnData(spawner).getEntityToSpawn().contains("id", 8))
                                ((Mob)entity).finalizeSpawn(level, event.getDifficulty(), event.getSpawnType(), event.getSpawnData(), event.getSpawnTag());
                        }

                        if(!level.tryAddFreshEntityWithPassengers(entity)){
                            delay(spawner, level, pos);
                            return;
                        }

                        level.levelEvent(2004, pos, 0);
                        if(entity instanceof Mob){
                            ((Mob)entity).spawnAnim();
                        }

                        flag = true;
                    }
                }

                if(flag){
                    delay(spawner, level, pos);
                }
            }
        }
    }

}
