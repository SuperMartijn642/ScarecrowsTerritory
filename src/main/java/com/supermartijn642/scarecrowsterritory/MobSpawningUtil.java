package com.supermartijn642.scarecrowsterritory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
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
import net.minecraftforge.event.ForgeEventFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * Created 1/14/2021 by SuperMartijn642
 */
public class MobSpawningUtil {

    private static final Method canSpawn;
    private static final Method afterSpawn;
    private static final Method getRandomSpawnMobAt;
    private static final Method canSpawnMobAt;
    private static final Method getMobForSpawn;
    private static final Method getRandomPosWithin;

    static{
        canSpawn = ReflectionUtil.findMethod(NaturalSpawner.SpawnState.class, "m_47127_", EntityType.class, BlockPos.class, ChunkAccess.class);
        afterSpawn = ReflectionUtil.findMethod(NaturalSpawner.SpawnState.class, "m_47131_", Mob.class, ChunkAccess.class);
        getRandomSpawnMobAt = ReflectionUtil.findMethod(NaturalSpawner.class, "m_220429_", ServerLevel.class, StructureManager.class, ChunkGenerator.class, MobCategory.class, RandomSource.class, BlockPos.class);
        canSpawnMobAt = ReflectionUtil.findMethod(NaturalSpawner.class, "m_220436_", ServerLevel.class, StructureManager.class, ChunkGenerator.class, MobCategory.class, MobSpawnSettings.SpawnerData.class, BlockPos.class);
        getMobForSpawn = ReflectionUtil.findMethod(NaturalSpawner.class, "m_46988_", ServerLevel.class, EntityType.class);
        getRandomPosWithin = ReflectionUtil.findMethod(NaturalSpawner.class, "m_47062_", Level.class, LevelChunk.class);
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
     * {@link NaturalSpawner#getRandomSpawnMobAt(ServerLevel, StructureManager, ChunkGenerator, MobCategory, RandomSource, BlockPos)}
     */
    @SuppressWarnings("unchecked")
    private static Optional<MobSpawnSettings.SpawnerData> getRandomSpawnMobAt(ServerLevel world, StructureManager structureManager, ChunkGenerator chunkGenerator, MobCategory classification, RandomSource random, BlockPos pos){
        try{
            return (Optional<MobSpawnSettings.SpawnerData>)getRandomSpawnMobAt.invoke(null, world, structureManager, chunkGenerator, classification, random, pos);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * {@link NaturalSpawner#canSpawnMobAt(ServerLevel, StructureManager, ChunkGenerator, MobCategory, MobSpawnSettings.SpawnerData, BlockPos)}
     */
    private static boolean canSpawnMobAt(ServerLevel world, StructureManager structureManager, ChunkGenerator chunkGenerator, MobCategory classification, MobSpawnSettings.SpawnerData spawnerData, BlockPos pos){
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
    private static Mob getMobForSpawn(ServerLevel level, EntityType<?> entityType){
        try{
            return (Mob)getMobForSpawn.invoke(null, level, entityType);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * {@link NaturalSpawner#getRandomPosWithin(Level, LevelChunk)}
     */
    private static BlockPos getRandomPosWithin(Level level, LevelChunk chunk){
        try{
            return (BlockPos)getRandomPosWithin.invoke(null, level, chunk);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * {@link NaturalSpawner#spawnForChunk(ServerLevel, LevelChunk, NaturalSpawner.SpawnState, boolean, boolean, boolean)}
     */
    public static void spawnEntitiesInChunk(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState densityManager, boolean spawnPassives, boolean spawnHostiles, boolean spawnAnimals){
        level.getProfiler().push("spawner");

        for(MobCategory classification : NaturalSpawner.SPAWNING_CATEGORIES){
            if((spawnPassives || !classification.isFriendly()) &&
                (spawnHostiles || classification.isFriendly()) &&
                (spawnAnimals || !classification.isPersistent()) &&
                canSpawnForCategory(densityManager, classification, level)){

                spawnCategoryForChunk(classification, level, chunk,
                    (type, pos, c) -> canSpawn(densityManager, type, pos, c),
                    (entity, c) -> afterSpawn(densityManager, entity, c));
            }
        }

        level.getProfiler().pop();
    }

    /**
     * {@link NaturalSpawner#spawnCategoryForChunk(MobCategory, ServerLevel, LevelChunk, NaturalSpawner.SpawnPredicate, NaturalSpawner.AfterSpawnCallback)}
     */
    private static void spawnCategoryForChunk(MobCategory classification, ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnPredicate densityCheck, NaturalSpawner.AfterSpawnCallback densityAdder){
        BlockPos blockpos = getRandomPosWithin(level, chunk);
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
                            getRandomSpawnMobAt(level, structureManager, chunkgenerator, classification, level.random, spawnPos);
                        if(optional.isEmpty())
                            break;

                        spawner = optional.get();
                        groupSize = spawner.minCount + level.random.nextInt(1 + spawner.maxCount - spawner.minCount);
                    }

                    if(isValidSpawnPositionForType(level, classification, structureManager, chunkgenerator, spawner, spawnPos) && densityCheck.test(spawner.type, spawnPos, chunk)){
                        Mob entity = getMobForSpawn(level, spawner.type);
                        if(entity == null)
                            return;

                        entity.getPersistentData().putBoolean("spawnedByScarecrow", true);
                        entity.moveTo(spawnXCenter, y, spawnZCenter, level.random.nextFloat() * 360.0F, 0.0F);
                        if(ForgeEventFactory.checkSpawnPosition(entity, level, MobSpawnType.NATURAL)){
                            entityData = ForgeEventFactory.onFinalizeSpawn(entity, level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.NATURAL, entityData, null);
                            entitiesSpawned++;
                            entitiesInGroup++;
                            level.addFreshEntityWithPassengers(entity);
                            densityAdder.run(entity, chunk);
                            if(entitiesSpawned >= net.minecraftforge.event.ForgeEventFactory.getMaxSpawnPackSize(entity))
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

        if(entityType.canSummon() && canSpawnMobAt(level, structureManager, chunkGenerator, classification, spawners, pos)){
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
