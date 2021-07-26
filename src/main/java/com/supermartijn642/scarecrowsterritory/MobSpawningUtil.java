package com.supermartijn642.scarecrowsterritory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Random;

/**
 * Created 1/14/2021 by SuperMartijn642
 */
public class MobSpawningUtil {

    private static final Method canSpawnForCategory;
    private static final Method canSpawn;
    private static final Method afterSpawn;
    private static final Method getRandomSpawnMobAt;
    private static final Method canSpawnMobAt;
    private static final Method getMobForSpawn;
    private static final Method isRightDistanceToPlayerAndSpawnPoint;
    private static final Method getRandomPosWithin;

    static{
        canSpawnForCategory = ReflectionUtil.findMethod(NaturalSpawner.class, "m_47134_", MobCategory.class);
        canSpawn = ReflectionUtil.findMethod(NaturalSpawner.class, "m_47127_", EntityType.class, BlockPos.class, ChunkAccess.class);
        afterSpawn = ReflectionUtil.findMethod(NaturalSpawner.class, "m_47131_", Mob.class, ChunkAccess.class);
        getRandomSpawnMobAt = ReflectionUtil.findMethod(NaturalSpawner.class, "m_151598_", ServerLevel.class, StructureFeatureManager.class, ChunkGenerator.class, MobCategory.class, Random.class, BlockPos.class);
        canSpawnMobAt = ReflectionUtil.findMethod(NaturalSpawner.class, "m_47003_", ServerLevel.class, StructureFeatureManager.class, ChunkGenerator.class, MobCategory.class, MobSpawnSettings.SpawnerData.class, BlockPos.class);
        getMobForSpawn = ReflectionUtil.findMethod(NaturalSpawner.class, "m_46988_", ServerLevel.class, EntityType.class);
        isRightDistanceToPlayerAndSpawnPoint = ReflectionUtil.findMethod(NaturalSpawner.class, "m_47024_", ServerLevel.class, ChunkAccess.class, BlockPos.MutableBlockPos.class, double.class);
        getRandomPosWithin = ReflectionUtil.findMethod(NaturalSpawner.class, "m_47062_", Level.class, LevelChunk.class);
    }

    /**
     * {@link NaturalSpawner.SpawnState#canSpawnForCategory(MobCategory)}
     */
    private static boolean canSpawnForCategory(NaturalSpawner.SpawnState densityManager, MobCategory classification){
        try{
            return (boolean)canSpawnForCategory.invoke(densityManager, classification);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * {@link NaturalSpawner.SpawnState#canSpawn(EntityType, BlockPos, ChunkAccess)}
     */
    private static boolean canSpawn(NaturalSpawner.SpawnState densityManager, EntityType<?> type, BlockPos pos, ChunkAccess chunk){
        try{
            return (boolean)canSpawn.invoke(densityManager, type, pos, chunk);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * {@link NaturalSpawner.SpawnState#afterSpawn(Mob, ChunkAccess)}
     */
    private static void afterSpawn(NaturalSpawner.SpawnState densityManager, Mob entity, ChunkAccess chunk){
        try{
            afterSpawn.invoke(densityManager, entity, chunk);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
        }
    }

    /**
     * {@link NaturalSpawner#getRandomSpawnMobAt(ServerLevel, StructureFeatureManager, ChunkGenerator, MobCategory, Random, BlockPos)}
     */
    @SuppressWarnings("unchecked")
    private static Optional<MobSpawnSettings.SpawnerData> getRandomSpawnMobAt(ServerLevel world, StructureFeatureManager structureManager, ChunkGenerator chunkGenerator, MobCategory classification, Random random, BlockPos pos){
        try{
            return (Optional<MobSpawnSettings.SpawnerData>)getRandomSpawnMobAt.invoke(null, world, structureManager, chunkGenerator, classification, random, pos);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * {@link NaturalSpawner#canSpawnMobAt(ServerLevel, StructureFeatureManager, ChunkGenerator, MobCategory, MobSpawnSettings.SpawnerData, BlockPos)}
     */
    private static boolean canSpawnMobAt(ServerLevel world, StructureFeatureManager structureManager, ChunkGenerator chunkGenerator, MobCategory classification, MobSpawnSettings.SpawnerData spawnerData, BlockPos pos){
        try{
            return (boolean)canSpawnMobAt.invoke(null, world, structureManager, chunkGenerator, classification, spawnerData, pos);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * {@link NaturalSpawner#getMobForSpawn(ServerLevel, EntityType)}
     */
    private static Mob getMobForSpawn(ServerLevel world, EntityType<?> entityType){
        try{
            return (Mob)getMobForSpawn.invoke(null, world, entityType);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * {@link NaturalSpawner#isRightDistanceToPlayerAndSpawnPoint(ServerLevel, ChunkAccess, BlockPos.MutableBlockPos, double)}
     */
    private static boolean isRightDistanceToPlayerAndSpawnPoint(ServerLevel world, ChunkAccess chunkAccess, BlockPos.MutableBlockPos pos, double playerDistance){
        try{
            return (boolean)isRightDistanceToPlayerAndSpawnPoint.invoke(null, world, chunkAccess, pos, playerDistance);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * {@link NaturalSpawner#getRandomPosWithin(Level, LevelChunk)}
     */
    private static BlockPos getRandomPosWithin(Level world, LevelChunk chunk){
        try{
            return (BlockPos)getRandomPosWithin.invoke(null, world, chunk);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * {@link NaturalSpawner#spawnForChunk(ServerLevel, LevelChunk, NaturalSpawner.SpawnState, boolean, boolean, boolean)}
     */
    public static void spawnEntitiesInChunk(ServerLevel world, LevelChunk chunk, NaturalSpawner.SpawnState densityManager, boolean spawnPassives, boolean spawnHostiles, boolean spawnAnimals){
        world.getProfiler().push("spawner");

        for(MobCategory classification : MobCategory.values()){
            if(classification != MobCategory.MISC &&
                (spawnPassives || !classification.isFriendly()) &&
                (spawnHostiles || classification.isFriendly()) &&
                (spawnAnimals || !classification.isPersistent()) &&
                canSpawnForCategory(densityManager, classification)){

                spawnCategoryForChunk(classification, world, chunk,
                    (type, pos, c) -> canSpawn(densityManager, type, pos, c),
                    (entity, c) -> afterSpawn(densityManager, entity, c));
            }
        }

        world.getProfiler().pop();
    }

    /**
     * {@link NaturalSpawner#spawnCategoryForChunk(MobCategory, ServerLevel, LevelChunk, NaturalSpawner.SpawnPredicate, NaturalSpawner.AfterSpawnCallback)}
     */
    private static void spawnCategoryForChunk(MobCategory classification, ServerLevel world, LevelChunk chunk, NaturalSpawner.SpawnPredicate densityCheck, NaturalSpawner.AfterSpawnCallback densityAdder){
        BlockPos blockpos = getRandomPosWithin(world, chunk);
        if(blockpos != null && blockpos.getY() >= 1){
            spawnCategoryForPosition(classification, world, chunk, blockpos, densityCheck, densityAdder);
        }
    }

    /**
     * {@link NaturalSpawner#spawnCategoryForPosition(MobCategory, ServerLevel, ChunkAccess, BlockPos, NaturalSpawner.SpawnPredicate, NaturalSpawner.AfterSpawnCallback)}
     */
    private static void spawnCategoryForPosition(MobCategory classification, ServerLevel world, ChunkAccess chunk, BlockPos pos, NaturalSpawner.SpawnPredicate densityCheck, NaturalSpawner.AfterSpawnCallback densityAdder){
        StructureFeatureManager structuremanager = world.structureFeatureManager();
        ChunkGenerator chunkgenerator = world.getChunkSource().getGenerator();
        int y = pos.getY();
        BlockState blockstate = chunk.getBlockState(pos);
        if(!blockstate.isRedstoneConductor(chunk, pos)){
            BlockPos.MutableBlockPos spawnPos = new BlockPos.MutableBlockPos();
            int entitiesSpawned = 0;

            // try spawning a group 3 times
            for(int k = 0; k < 3; ++k){
                int spawnX = pos.getX();
                int spawnZ = pos.getZ();
                MobSpawnSettings.SpawnerData spawner = null;
                SpawnGroupData ilivingentitydata = null;
                int groupSize = Mth.ceil(world.random.nextFloat() * 4.0F);
                int entitiesInGroup = 0;

                // try spawning entities in the group
                for(int i2 = 0; i2 < groupSize; ++i2){
                    spawnX += world.random.nextInt(6) - world.random.nextInt(6);
                    spawnZ += world.random.nextInt(6) - world.random.nextInt(6);
                    spawnPos.set(spawnX, y, spawnZ);
                    double spawnXCenter = (double)spawnX + 0.5D;
                    double spawnZCenter = (double)spawnZ + 0.5D;
                    Player player = world.getNearestPlayer(spawnXCenter, y, spawnZCenter, -1.0D, false);
                    // removed the player != null check here
                    if(player == null || isRightDistanceToPlayerAndSpawnPoint(world, chunk, spawnPos, player.distanceToSqr(spawnXCenter, y, spawnZCenter))){
                        if(spawner == null){
                            Optional<MobSpawnSettings.SpawnerData> optional = getRandomSpawnMobAt(world, structuremanager, chunkgenerator, classification, world.random, spawnPos);
                            if(!optional.isPresent()){
                                break;
                            }

                            MobSpawnSettings.SpawnerData spawnerData = optional.get();
                            groupSize = spawnerData.minCount + world.random.nextInt(1 + spawnerData.maxCount - spawnerData.minCount);
                        }

                        if(isValidSpawnPostitionForType(world, classification, structuremanager, chunkgenerator, spawner, spawnPos) && densityCheck.test(spawner.type, spawnPos, chunk)){
                            Mob mobentity = getMobForSpawn(world, spawner.type);
                            if(mobentity == null){
                                return;
                            }

                            mobentity.moveTo(spawnXCenter, y, spawnZCenter, world.random.nextFloat() * 360.0F, 0.0F);
                            int canSpawn = net.minecraftforge.common.ForgeHooks.canEntitySpawn(mobentity, world, spawnXCenter, y, spawnZCenter, null, MobSpawnType.NATURAL);
                            if(canSpawn != -1 && (canSpawn == 1 || (mobentity.checkSpawnRules(world, MobSpawnType.NATURAL) && mobentity.checkSpawnObstruction(world)))){
                                if(!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(mobentity, world, (float)spawnXCenter, (float)y, (float)spawnZCenter, null, MobSpawnType.NATURAL))
                                    ilivingentitydata = mobentity.finalizeSpawn(world, world.getCurrentDifficultyAt(mobentity.blockPosition()), MobSpawnType.NATURAL, ilivingentitydata, (CompoundTag)null);
                                entitiesSpawned++;
                                entitiesInGroup++;
                                world.addFreshEntityWithPassengers(mobentity);
                                densityAdder.run(mobentity, chunk);
                                if(entitiesSpawned >= net.minecraftforge.event.ForgeEventFactory.getMaxSpawnPackSize(mobentity)){
                                    return;
                                }

                                if(mobentity.isMaxGroupSizeReached(entitiesInGroup)){
                                    break;
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    /**
     * {@link NaturalSpawner#isValidSpawnPostitionForType(ServerLevel, MobCategory, StructureFeatureManager, ChunkGenerator, MobSpawnSettings.SpawnerData, BlockPos.MutableBlockPos, double)}
     */
    private static boolean isValidSpawnPostitionForType(ServerLevel world, MobCategory classification, StructureFeatureManager structureManager, ChunkGenerator chunkGenerator, MobSpawnSettings.SpawnerData spawners, BlockPos.MutableBlockPos pos){
        EntityType<?> entityType = spawners.type;
        if(entityType.getCategory() == MobCategory.MISC)
            return false;

        // removed the player distance check here

        if(entityType.canSummon() && canSpawnMobAt(world, structureManager, chunkGenerator, classification, spawners, pos)){
            SpawnPlacements.Type placementType = SpawnPlacements.getPlacementType(entityType);
            if(!NaturalSpawner.isSpawnPositionOk(placementType, world, pos, entityType) ||
                !SpawnPlacements.checkSpawnRules(entityType, world, MobSpawnType.NATURAL, pos, world.random))
                return false;

            return world.noCollision(entityType.getAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
        }

        return false;
    }

}
