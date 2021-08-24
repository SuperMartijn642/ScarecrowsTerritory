package com.supermartijn642.scarecrowsterritory;

import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.WorldEntitySpawner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

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
        canSpawn = ReflectionUtil.findMethod(WorldEntitySpawner.EntityDensityManager.class, "func_234989_a_", EntityType.class, BlockPos.class, IChunk.class);
        afterSpawn = ReflectionUtil.findMethod(WorldEntitySpawner.EntityDensityManager.class, "func_234990_a_", MobEntity.class, IChunk.class);
        getRandomSpawnMobAt = ReflectionUtil.findMethod(WorldEntitySpawner.class, "func_234977_a_", ServerWorld.class, StructureManager.class, ChunkGenerator.class, EntityClassification.class, Random.class, BlockPos.class);
        canSpawnMobAt = ReflectionUtil.findMethod(WorldEntitySpawner.class, "func_234976_a_", ServerWorld.class, StructureManager.class, ChunkGenerator.class, EntityClassification.class, MobSpawnInfo.Spawners.class, BlockPos.class);
        getMobForSpawn = ReflectionUtil.findMethod(WorldEntitySpawner.class, "func_234973_a_", ServerWorld.class, EntityType.class);
        getRandomPosWithin = ReflectionUtil.findMethod(WorldEntitySpawner.class, "func_222262_a", World.class, Chunk.class);
    }

    /**
     * {@link WorldEntitySpawner.EntityDensityManager#canSpawn(EntityType, BlockPos, IChunk)}
     */
    private static boolean canSpawn(WorldEntitySpawner.EntityDensityManager densityManager, EntityType<?> type, BlockPos pos, IChunk chunk){
        try{
            return (boolean)canSpawn.invoke(densityManager, type, pos, chunk);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * {@link WorldEntitySpawner.EntityDensityManager#afterSpawn(MobEntity, IChunk)}
     */
    private static void afterSpawn(WorldEntitySpawner.EntityDensityManager densityManager, MobEntity entity, IChunk chunk){
        try{
            afterSpawn.invoke(densityManager, entity, chunk);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
        }
    }

    /**
     * {@link WorldEntitySpawner#getRandomSpawnMobAt(ServerWorld, StructureManager, ChunkGenerator, EntityClassification, Random, BlockPos)}
     */
    private static MobSpawnInfo.Spawners getRandomSpawnMobAt(ServerWorld world, StructureManager structureManager, ChunkGenerator chunkGenerator, EntityClassification classification, Random random, BlockPos pos){
        try{
            return (MobSpawnInfo.Spawners)getRandomSpawnMobAt.invoke(null, world, structureManager, chunkGenerator, classification, random, pos);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * {@link WorldEntitySpawner#canSpawnMobAt(ServerWorld, StructureManager, ChunkGenerator, EntityClassification, MobSpawnInfo.Spawners, BlockPos)}
     */
    private static boolean canSpawnMobAt(ServerWorld world, StructureManager structureManager, ChunkGenerator chunkGenerator, EntityClassification classification, MobSpawnInfo.Spawners spawnerData, BlockPos pos){
        try{
            return (boolean)canSpawnMobAt.invoke(null, world, structureManager, chunkGenerator, classification, spawnerData, pos);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * {@link WorldEntitySpawner#getMobForSpawn(ServerWorld, EntityType)}
     */
    private static MobEntity getMobForSpawn(ServerWorld world, EntityType<?> entityType){
        try{
            return (MobEntity)getMobForSpawn.invoke(null, world, entityType);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * {@link WorldEntitySpawner#getRandomPosWithin(World, Chunk)}
     */
    private static BlockPos getRandomPosWithin(World world, Chunk chunk){
        try{
            return (BlockPos)getRandomPosWithin.invoke(null, world, chunk);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * {@link WorldEntitySpawner#spawnForChunk(ServerWorld, Chunk, WorldEntitySpawner.EntityDensityManager, boolean, boolean, boolean)}
     */
    public static void spawnEntitiesInChunk(ServerWorld world, Chunk chunk, WorldEntitySpawner.EntityDensityManager densityManager, boolean spawnPassives, boolean spawnHostiles, boolean spawnAnimals){
        world.getProfiler().push("spawner");

        for(EntityClassification classification : EntityClassification.values()){
            if(classification != EntityClassification.MISC &&
                (spawnPassives || !classification.isFriendly()) &&
                (spawnHostiles || classification.isFriendly()) &&
                (spawnAnimals || !classification.isPersistent()) &&
                canSpawnForCategory(densityManager, classification, world)){

                spawnCategoryForChunk(classification, world, chunk,
                    (type, pos, c) -> canSpawn(densityManager, type, pos, c),
                    (entity, c) -> afterSpawn(densityManager, entity, c));
            }
        }

        world.getProfiler().pop();
    }

    /**
     * {@link WorldEntitySpawner#spawnCategoryForChunk(EntityClassification, ServerWorld, Chunk, WorldEntitySpawner.IDensityCheck, WorldEntitySpawner.IOnSpawnDensityAdder)}
     */
    private static void spawnCategoryForChunk(EntityClassification classification, ServerWorld world, Chunk chunk, WorldEntitySpawner.IDensityCheck densityCheck, WorldEntitySpawner.IOnSpawnDensityAdder densityAdder){
        BlockPos blockpos = getRandomPosWithin(world, chunk);
        if(blockpos != null && blockpos.getY() >= 1){
            spawnCategoryForPosition(classification, world, chunk, blockpos, densityCheck, densityAdder);
        }
    }

    /**
     * {@link WorldEntitySpawner#spawnCategoryForPosition(EntityClassification, ServerWorld, IChunk, BlockPos, WorldEntitySpawner.IDensityCheck, WorldEntitySpawner.IOnSpawnDensityAdder)}
     */
    private static void spawnCategoryForPosition(EntityClassification classification, ServerWorld world, IChunk chunk, BlockPos pos, WorldEntitySpawner.IDensityCheck densityCheck, WorldEntitySpawner.IOnSpawnDensityAdder densityAdder){
        StructureManager structuremanager = world.structureFeatureManager();
        ChunkGenerator chunkgenerator = world.getChunkSource().getGenerator();
        int y = pos.getY();
        BlockState blockstate = chunk.getBlockState(pos);
        if(!blockstate.isRedstoneConductor(chunk, pos)){
            BlockPos.Mutable spawnPos = new BlockPos.Mutable();
            int entitiesSpawned = 0;

            // try spawning a group 3 times
            for(int k = 0; k < 3; ++k){
                int spawnX = pos.getX();
                int spawnZ = pos.getZ();
                MobSpawnInfo.Spawners spawner = null;
                ILivingEntityData ilivingentitydata = null;
                int groupSize = MathHelper.ceil(world.random.nextFloat() * 4.0F);
                int entitiesInGroup = 0;

                // try spawning entities in the group
                for(int i2 = 0; i2 < groupSize; ++i2){
                    spawnX += world.random.nextInt(6) - world.random.nextInt(6);
                    spawnZ += world.random.nextInt(6) - world.random.nextInt(6);
                    spawnPos.set(spawnX, y, spawnZ);
                    double spawnXCenter = (double)spawnX + 0.5D;
                    double spawnZCenter = (double)spawnZ + 0.5D;
                    if(spawner == null){
                        spawner = getRandomSpawnMobAt(world, structuremanager, chunkgenerator, classification, world.random, spawnPos);
                        if(spawner == null){
                            break;
                        }

                        groupSize = spawner.minCount + world.random.nextInt(1 + spawner.maxCount - spawner.minCount);
                    }

                    if(isValidSpawnPositionForType(world, classification, structuremanager, chunkgenerator, spawner, spawnPos) && densityCheck.test(spawner.type, spawnPos, chunk)){
                        MobEntity mobentity = getMobForSpawn(world, spawner.type);
                        if(mobentity == null){
                            return;
                        }

                        mobentity.moveTo(spawnXCenter, y, spawnZCenter, world.random.nextFloat() * 360.0F, 0.0F);
                        int canSpawn = net.minecraftforge.common.ForgeHooks.canEntitySpawn(mobentity, world, spawnXCenter, y, spawnZCenter, null, SpawnReason.NATURAL);
                        if(canSpawn != -1 && (canSpawn == 1 || (mobentity.checkSpawnRules(world, SpawnReason.NATURAL) && mobentity.checkSpawnObstruction(world)))){
                            if(!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(mobentity, world, (float)spawnXCenter, (float)y, (float)spawnZCenter, null, SpawnReason.NATURAL))
                                ilivingentitydata = mobentity.finalizeSpawn(world, world.getCurrentDifficultyAt(mobentity.blockPosition()), SpawnReason.NATURAL, ilivingentitydata, null);
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

    /**
     * {@link WorldEntitySpawner#isValidSpawnPostitionForType(ServerWorld, EntityClassification, StructureManager, ChunkGenerator, MobSpawnInfo.Spawners, BlockPos.Mutable, double)}
     */
    private static boolean isValidSpawnPositionForType(ServerWorld world, EntityClassification classification, StructureManager structureManager, ChunkGenerator chunkGenerator, MobSpawnInfo.Spawners spawners, BlockPos.Mutable pos){
        EntityType<?> entityType = spawners.type;
        if(entityType.getCategory() == EntityClassification.MISC)
            return false;

        // removed the player distance check here

        if(entityType.canSummon() && canSpawnMobAt(world, structureManager, chunkGenerator, classification, spawners, pos)){
            EntitySpawnPlacementRegistry.PlacementType placementType = EntitySpawnPlacementRegistry.getPlacementType(entityType);
            if(!WorldEntitySpawner.isSpawnPositionOk(placementType, world, pos, entityType) ||
                !EntitySpawnPlacementRegistry.checkSpawnRules(entityType, world, SpawnReason.NATURAL, pos, world.random))
                return false;

            return world.noCollision(entityType.getAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
        }

        return false;
    }

    /**
     * {@link WorldEntitySpawner.EntityDensityManager#canSpawnForCategory(EntityClassification)}
     */
    private static boolean canSpawnForCategory(WorldEntitySpawner.EntityDensityManager densityManager, EntityClassification classification, IWorld world){
        int i = classification.getMaxInstancesPerChunk() * Math.max(densityManager.getSpawnableChunkCount(), ScarecrowTracker.getNumberOfChunksToSpawnMobsIn(world)) / 17 * 17;
        return densityManager.getMobCategoryCounts().getInt(classification) < i;
    }

}
