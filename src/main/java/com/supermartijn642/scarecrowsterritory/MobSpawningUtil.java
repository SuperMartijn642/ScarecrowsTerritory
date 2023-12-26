package com.supermartijn642.scarecrowsterritory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.EventHooks;

import java.util.List;
import java.util.Optional;

/**
 * Created 1/14/2021 by SuperMartijn642
 */
public class MobSpawningUtil {

    /**
     * {@link NaturalSpawner#spawnForChunk(ServerLevel, LevelChunk, NaturalSpawner.SpawnState, boolean, boolean, boolean)}
     */
    public static void spawnEntitiesInChunk(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState densityManager, boolean spawnPassives, boolean spawnHostiles, boolean spawnAnimals){
        level.getProfiler().push("spawner");

        for(MobCategory classification : NaturalSpawner.SPAWNING_CATEGORIES){
            if((spawnPassives || !classification.isFriendly()) &&
                (spawnHostiles || classification.isFriendly()) &&
                (spawnAnimals || !classification.isPersistent()) &&
                canSpawnForCategory(densityManager, classification, level))
                spawnCategoryForChunk(classification, level, chunk, densityManager::canSpawn, densityManager::afterSpawn);
        }

        level.getProfiler().pop();
    }

    /**
     * {@link NaturalSpawner#spawnCategoryForChunk(MobCategory, ServerLevel, LevelChunk, NaturalSpawner.SpawnPredicate, NaturalSpawner.AfterSpawnCallback)}
     */
    private static void spawnCategoryForChunk(MobCategory classification, ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnPredicate densityCheck, NaturalSpawner.AfterSpawnCallback densityAdder){
        BlockPos blockpos = NaturalSpawner.getRandomPosWithin(level, chunk);
        if(blockpos.getY() >= level.getMinBuildHeight() + 1){
            spawnCategoryForPosition(classification, level, chunk, blockpos, densityCheck, densityAdder);
        }
    }

    /**
     * {@link NaturalSpawner#spawnCategoryForPosition(MobCategory, ServerLevel, ChunkAccess, BlockPos, NaturalSpawner.SpawnPredicate, NaturalSpawner.AfterSpawnCallback)}
     */
    private static void spawnCategoryForPosition(MobCategory classification, ServerLevel level, ChunkAccess chunk, BlockPos pos, NaturalSpawner.SpawnPredicate densityCheck, NaturalSpawner.AfterSpawnCallback densityAdder){
        BlockPos closestScarecrow = ScarecrowTracker.getClosestScarecrow(level, pos);
        if(closestScarecrow == null || !closestScarecrow.closerThan(pos, ScarecrowsTerritoryConfig.passiveMobRange.get() + 12))
            return;

        // Get the entity for trophies integration
        ScarecrowBlockEntity scarecrowEntity = null;
        if(ScarecrowsTerritory.ENABLE_TROPHIES_INTEGRATION.get()){
            BlockEntity entity = level.getBlockEntity(closestScarecrow);
            if(!(entity instanceof ScarecrowBlockEntity))
                return;
            scarecrowEntity = ((ScarecrowBlockEntity)entity);
            if(scarecrowEntity.getTrophySpawnableEntities(classification).isEmpty())
                return;
        }

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
                        //noinspection DataFlowIssue
                        Optional<MobSpawnSettings.SpawnerData> optional = ScarecrowsTerritory.ENABLE_TROPHIES_INTEGRATION.get() ?
                            findTrophySpawn(scarecrowEntity, level, structureManager, chunkgenerator, classification, spawnPos) :
                            NaturalSpawner.getRandomSpawnMobAt(level, structureManager, chunkgenerator, classification, level.random, spawnPos);
                        if(optional.isEmpty())
                            break;

                        spawner = optional.get();
                        groupSize = spawner.minCount + level.random.nextInt(1 + spawner.maxCount - spawner.minCount);
                    }

                    if(isValidSpawnPositionForType(level, classification, structureManager, chunkgenerator, spawner, spawnPos) && densityCheck.test(spawner.type, spawnPos, chunk)){
                        Mob entity = NaturalSpawner.getMobForSpawn(level, spawner.type);
                        if(entity == null)
                            return;

                        entity.getPersistentData().putBoolean("spawnedByScarecrow", true);
                        entity.moveTo(spawnXCenter, y, spawnZCenter, level.random.nextFloat() * 360.0F, 0.0F);
                        if(EventHooks.checkSpawnPosition(entity, level, MobSpawnType.NATURAL)){
                            entityData = EventHooks.onFinalizeSpawn(entity, level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.NATURAL, entityData, null);
                            entitiesSpawned++;
                            entitiesInGroup++;
                            level.addFreshEntityWithPassengers(entity);
                            densityAdder.run(entity, chunk);
                            if(entitiesSpawned >= EventHooks.getMaxSpawnPackSize(entity))
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

        if(entityType.canSummon() && NaturalSpawner.canSpawnMobAt(level, structureManager, chunkGenerator, classification, spawners, pos)){
            SpawnPlacements.Type placementType = SpawnPlacements.getPlacementType(entityType);
            if(!NaturalSpawner.isSpawnPositionOk(placementType, level, pos, entityType) ||
                !SpawnPlacements.checkSpawnRules(entityType, level, MobSpawnType.NATURAL, pos, level.random))
                return false;

            return level.noCollision(entityType.getAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
        }

        return false;
    }

    /**
     * {@link NaturalSpawner.SpawnState#canSpawnForCategory(MobCategory)}
     */
    private static boolean canSpawnForCategory(NaturalSpawner.SpawnState densityManager, MobCategory classification, LevelAccessor world){
        int spawnableChunks = Math.max(1, densityManager.getSpawnableChunkCount() / (17 * 17));
        return densityManager.getMobCategoryCounts().getInt(classification) < classification.getMaxInstancesPerChunk() * spawnableChunks;
    }

    private static Optional<MobSpawnSettings.SpawnerData> findTrophySpawn(ScarecrowBlockEntity scarecrow, ServerLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator, MobCategory category, BlockPos pos){
        // Pick a random entity from the scarecrow
        List<EntityType<?>> spawnableEntities = scarecrow.getTrophySpawnableEntities(category);
        if(spawnableEntities.isEmpty())
            return Optional.empty();
        EntityType<?> entity = spawnableEntities.get(level.random.nextInt(spawnableEntities.size()));
        // Check if that entity is in the spawn list
        Holder<Biome> biome = level.getBiome(pos);
        WeightedRandomList<MobSpawnSettings.SpawnerData> spawnList = NaturalSpawner.mobsAt(level, structureManager, chunkGenerator, category, pos, biome);
        return spawnList.items.stream()
            .filter(data -> data.type.equals(entity))
            .findAny();
    }
}
