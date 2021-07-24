package com.supermartijn642.scarecrowsterritory;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.WorldEntitySpawner;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Created 1/14/2021 by SuperMartijn642
 */
public class MobSpawningUtil {

    private static final Method canSpawnForCategory;
    private static final Method canSpawn;
    private static final Method afterSpawn;

    static{
        canSpawnForCategory = ReflectionUtil.findMethod(WorldEntitySpawner.class, "canSpawnForCategory");
        canSpawn = ReflectionUtil.findMethod(WorldEntitySpawner.class, "canSpawn");
        afterSpawn = ReflectionUtil.findMethod(WorldEntitySpawner.class, "afterSpawn");
    }

    private static boolean canSpawnForCategory(WorldEntitySpawner.EntityDensityManager densityManager, EntityClassification classification){
        try{
            return (boolean)canSpawnForCategory.invoke(densityManager, classification);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return false;
        }
    }

    private static boolean canSpawn(WorldEntitySpawner.EntityDensityManager densityManager, EntityType<?> type, BlockPos pos, IChunk chunk){
        try{
            return (boolean)canSpawn.invoke(densityManager, type, pos, chunk);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return false;
        }
    }

    private static void afterSpawn(WorldEntitySpawner.EntityDensityManager densityManager, MobEntity entity, IChunk chunk){
        try{
            afterSpawn.invoke(densityManager, entity, chunk);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
        }
    }

    public static void spawnEntitiesInChunk(ServerWorld world, Chunk chunk, WorldEntitySpawner.EntityDensityManager densityManager, boolean spawnPassives, boolean spawnHostiles, boolean spawnAnimals){
        world.getProfiler().push("spawner");

        for(EntityClassification classification : EntityClassification.values()){
            if(classification != EntityClassification.MISC &&
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

    private static void spawnCategoryForChunk(EntityClassification classification, ServerWorld world, Chunk chunk, WorldEntitySpawner.IDensityCheck densityCheck, WorldEntitySpawner.IOnSpawnDensityAdder densityAdder){
        BlockPos blockpos = getRandomHeight(world, chunk);
        if(blockpos.getY() >= 1){
            spawnEntitiesAtPos(classification, world, chunk, blockpos, densityCheck, densityAdder);
        }
    }

    private static BlockPos getRandomHeight(World world, Chunk chunk){
        ChunkPos chunkpos = chunk.getPos();
        int x = chunkpos.getMinBlockX() + world.random.nextInt(16);
        int z = chunkpos.getMinBlockZ() + world.random.nextInt(16);
        int y = chunk.getHeight(Heightmap.Type.WORLD_SURFACE, x, z) + 1;
        y = world.random.nextInt(y + 1);
        return new BlockPos(x, y, z);
    }

    private static void spawnEntitiesAtPos(EntityClassification classification, ServerWorld world, IChunk chunk, BlockPos pos, WorldEntitySpawner.IDensityCheck densityCheck, WorldEntitySpawner.IOnSpawnDensityAdder densityAdder){
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
                    PlayerEntity playerentity = world.getNearestPlayer(spawnXCenter, y, spawnZCenter, -1.0D, false);
                    if(playerentity != null){
                        double playerDistanceSq = playerentity.distanceToSqr(spawnXCenter, y, spawnZCenter);
                        if(isFarEnoughFromPlayer(world, chunk, spawnPos, playerDistanceSq)){
                            if(spawner == null){
                                spawner = getEntitySpawner(world, structuremanager, chunkgenerator, classification, world.random, spawnPos);
                                if(spawner == null){
                                    break;
                                }

                                groupSize = spawner.minCount + world.random.nextInt(1 + spawner.maxCount - spawner.minCount);
                            }

                            if(canEntitySpawnAt(world, classification, structuremanager, chunkgenerator, spawner, spawnPos, playerDistanceSq) && densityCheck.test(spawner.type, spawnPos, chunk)){
                                MobEntity mobentity = createEntity(world, spawner.type);
                                if(mobentity == null){
                                    return;
                                }

                                mobentity.moveTo(spawnXCenter, y, spawnZCenter, world.random.nextFloat() * 360.0F, 0.0F);
                                int canSpawn = net.minecraftforge.common.ForgeHooks.canEntitySpawn(mobentity, world, spawnXCenter, y, spawnZCenter, null, SpawnReason.NATURAL);
                                if(canSpawn != -1 && (canSpawn == 1 || (mobentity.checkSpawnRules(world, SpawnReason.NATURAL) && mobentity.checkSpawnObstruction(world)))){
                                    if(!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(mobentity, world, (float)spawnXCenter, (float)y, (float)spawnZCenter, null, SpawnReason.NATURAL))
                                        ilivingentitydata = mobentity.finalizeSpawn(world, world.getCurrentDifficultyAt(mobentity.blockPosition()), SpawnReason.NATURAL, ilivingentitydata, (CompoundNBT)null);
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
    }

    private static boolean isFarEnoughFromPlayer(ServerWorld world, IChunk chunk, BlockPos.Mutable pos, double playerDistanceSq){
        if(playerDistanceSq <= 24 * 24){
            return false;
        }else if(world.getSharedSpawnPos().closerThan(pos, 24.0D)){
            return false;
        }else{
            ChunkPos chunkpos = new ChunkPos(pos);
            return Objects.equals(chunkpos, chunk.getPos()) || world.getChunkSource().isEntityTickingChunk(chunkpos);
        }
    }

    private static boolean canEntitySpawnAt(ServerWorld world, EntityClassification classification, StructureManager structureManager, ChunkGenerator chunkGenerator, MobSpawnInfo.Spawners spawners, BlockPos.Mutable pos, double playerDistanceSq){
        EntityType<?> entityType = spawners.type;
        if(entityType.getCategory() == EntityClassification.MISC)
            return false;

        if(entityType.canSummon() && isEntityInSpawnListAt(world, structureManager, chunkGenerator, classification, spawners, pos)){
            EntitySpawnPlacementRegistry.PlacementType placementType = EntitySpawnPlacementRegistry.getPlacementType(entityType);
            if(!WorldEntitySpawner.isSpawnPositionOk(placementType, world, pos, entityType) ||
                !EntitySpawnPlacementRegistry.checkSpawnRules(entityType, world, SpawnReason.NATURAL, pos, world.random))
                return false;

            return world.noCollision(entityType.getAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
        }

        return false;
    }

    private static boolean isEntityInSpawnListAt(ServerWorld world, StructureManager structureManager, ChunkGenerator chunkGenerator, EntityClassification classification, MobSpawnInfo.Spawners spawners, BlockPos pos){
        Biome biome = world.getBiome(pos);
        return getSpawnList(world, structureManager, chunkGenerator, classification, pos, biome).contains(spawners);
    }

    private static List<MobSpawnInfo.Spawners> getSpawnList(ServerWorld world, StructureManager structureManager, ChunkGenerator chunkGenerator, EntityClassification classification, BlockPos pos, Biome biome){
        return classification == EntityClassification.MONSTER &&
            world.getBlockState(pos.below()).getBlock() == Blocks.NETHER_BRICKS &&
            structureManager.getStructureAt(pos, false, Structure.NETHER_BRIDGE).isValid() ?
            Structure.NETHER_BRIDGE.getSpecialEnemies() : chunkGenerator.getMobsAt(biome, structureManager, classification, pos);
    }

    @Nullable
    private static MobSpawnInfo.Spawners getEntitySpawner(ServerWorld world, StructureManager structureManager, ChunkGenerator chunkGenerator, EntityClassification classification, Random random, BlockPos pos){
        Biome biome = world.getBiome(pos);
        if(classification == EntityClassification.WATER_AMBIENT && biome.getBiomeCategory() == Biome.Category.RIVER && random.nextFloat() < 0.98F){
            return null;
        }else{
            List<MobSpawnInfo.Spawners> list = getSpawnList(world, structureManager, chunkGenerator, classification, pos, biome);
            list = net.minecraftforge.event.ForgeEventFactory.getPotentialSpawns(world, classification, pos, list);
            return list.isEmpty() ? null : WeightedRandom.getRandomItem(random, list);
        }
    }

    @Nullable
    private static MobEntity createEntity(ServerWorld world, EntityType<?> entityType){
        try{
            Entity entity = entityType.create(world);
            if(!(entity instanceof MobEntity)){
                throw new IllegalStateException("Trying to spawn a non-mob: " + Registry.ENTITY_TYPE.getKey(entityType));
            }else{
                return (MobEntity)entity;
            }
        }catch(Exception exception){
            return null;
        }
    }

}
