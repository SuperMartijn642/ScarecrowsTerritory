package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.scarecrowsterritory.extensions.ScarecrowMobExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created 1/14/2021 by SuperMartijn642
 */
public class MobSpawningUtil {

    /**
     * {@link NaturalSpawner#getFilteredSpawningCategories(NaturalSpawner.SpawnState, boolean, boolean, boolean)}
     */
    public static List<MobCategory> getFilteredSpawningCategories(NaturalSpawner.SpawnState spawnState, boolean spawnFriendlies, boolean spawnEnemies, boolean spawnAnimals) {
        List<MobCategory> list = new ArrayList<>(NaturalSpawner.SPAWNING_CATEGORIES.length);
        for (MobCategory category : NaturalSpawner.SPAWNING_CATEGORIES) {
            if ((spawnFriendlies || !category.isFriendly())
                && (spawnEnemies || category.isFriendly())
                && (spawnAnimals || !category.isPersistent())) {
                list.add(category);
            }
        }
        return list;
    }

    /**
     * {@link NaturalSpawner#spawnForChunk(ServerLevel, LevelChunk, NaturalSpawner.SpawnState, List)}
     */
    public static void spawnEntitiesInChunk(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState densityManager, List<MobCategory> categories){
        ProfilerFiller profiler = Profiler.get();
        profiler.push("spawner");

        for(MobCategory category : categories){
            if(canSpawnForCategory(densityManager, category, level)){
                spawnCategoryForChunk(category, level, chunk,
                    densityManager::canSpawn,
                    densityManager::afterSpawn);
            }
        }

        profiler.pop();
    }

    /**
     * {@link NaturalSpawner#spawnCategoryForChunk(MobCategory, ServerLevel, LevelChunk, NaturalSpawner.SpawnPredicate, NaturalSpawner.AfterSpawnCallback)}
     */
    private static void spawnCategoryForChunk(MobCategory classification, ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnPredicate densityCheck, NaturalSpawner.AfterSpawnCallback densityAdder){
        BlockPos blockpos = NaturalSpawner.getRandomPosWithin(level, chunk);
        if(blockpos.getY() >= level.getMinY() + 1)
            spawnCategoryForPosition(classification, level, chunk, blockpos, densityCheck, densityAdder);
    }

    /**
     * {@link NaturalSpawner#spawnCategoryForPosition(MobCategory, ServerLevel, ChunkAccess, BlockPos, NaturalSpawner.SpawnPredicate, NaturalSpawner.AfterSpawnCallback)}
     */
    private static void spawnCategoryForPosition(MobCategory classification, ServerLevel level, ChunkAccess chunk, BlockPos pos, NaturalSpawner.SpawnPredicate densityCheck, NaturalSpawner.AfterSpawnCallback densityAdder){
        StructureManager structureManager = level.structureManager();
        ChunkGenerator chunkgenerator = level.getChunkSource().getGenerator();
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
                SpawnGroupData entityData = null;
                int groupSize = Mth.ceil(level.random.nextFloat() * 4.0F);
                int entitiesInGroup = 0;

                // try spawning entities in the group
                for(int i2 = 0; i2 < groupSize; ++i2){
                    spawnX += level.random.nextInt(6) - level.random.nextInt(6);
                    spawnZ += level.random.nextInt(6) - level.random.nextInt(6);
                    spawnPos.set(spawnX, y, spawnZ);
                    double spawnXCenter = (double)spawnX + 0.5D;
                    double spawnZCenter = (double)spawnZ + 0.5D;

                    if(!ScarecrowTracker.isScarecrowInRange(level, pos.getCenter(), ScarecrowsTerritoryConfig.passiveMobRange.get()))
                        continue;

                    if(spawner == null){
                        Optional<MobSpawnSettings.SpawnerData> optional = NaturalSpawner.getRandomSpawnMobAt(level, structureManager, chunkgenerator, classification, level.random, spawnPos);
                        if(optional.isEmpty())
                            break;

                        spawner = optional.get();
                        groupSize = spawner.minCount + level.random.nextInt(1 + spawner.maxCount - spawner.minCount);
                    }

                    if(isValidSpawnPositionForType(level, classification, structureManager, chunkgenerator, spawner, spawnPos) && densityCheck.test(spawner.type, spawnPos, chunk)){
                        Mob entity = NaturalSpawner.getMobForSpawn(level, spawner.type);
                        if(entity == null)
                            return;

                        ((ScarecrowMobExtension)entity).scarecrowsterritory$setSpawnedByScarecrow();
                        entity.moveTo(spawnXCenter, y, spawnZCenter, level.random.nextFloat() * 360.0F, 0.0F);
                        if(entity.checkSpawnRules(level, EntitySpawnReason.NATURAL) && entity.checkSpawnObstruction(level)){
                            entityData = entity.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), EntitySpawnReason.NATURAL, entityData);
                            entitiesSpawned++;
                            entitiesInGroup++;
                            level.addFreshEntityWithPassengers(entity);
                            densityAdder.run(entity, chunk);
                            if(entitiesSpawned >= entity.getMaxSpawnClusterSize())
                                return;

                            if(entity.isMaxGroupSizeReached(entitiesInGroup))
                                break;
                        }
                    }
                }
            }
        }
    }

    /**
     * {@link NaturalSpawner#isValidSpawnPostitionForType(ServerLevel, MobCategory, StructureManager, ChunkGenerator, MobSpawnSettings.SpawnerData, BlockPos.MutableBlockPos, double)}
     */
    private static boolean isValidSpawnPositionForType(ServerLevel level, MobCategory classification, StructureManager structureManager, ChunkGenerator chunkGenerator, MobSpawnSettings.SpawnerData spawners, BlockPos.MutableBlockPos pos){
        EntityType<?> entityType = spawners.type;
        if(entityType.getCategory() == MobCategory.MISC)
            return false;

        // removed the player distance check here

        if(entityType.canSummon()
            && NaturalSpawner.canSpawnMobAt(level, structureManager, chunkGenerator, classification, spawners, pos)
            && SpawnPlacements.isSpawnPositionOk(entityType, level, pos)
            && SpawnPlacements.checkSpawnRules(entityType, level, EntitySpawnReason.NATURAL, pos, level.random)){
            return level.noCollision(entityType.getSpawnAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
        }

        return false;
    }

    /**
     * {@link NaturalSpawner.SpawnState#canSpawnForCategoryGlobal(MobCategory)}
     */
    private static boolean canSpawnForCategory(NaturalSpawner.SpawnState densityManager, MobCategory classification, LevelAccessor world){
        int spawnableChunks = Math.max(1, densityManager.getSpawnableChunkCount() / (17 * 17));
        return densityManager.getMobCategoryCounts().getInt(classification) < classification.getMaxInstancesPerChunk() * spawnableChunks;
    }

}
