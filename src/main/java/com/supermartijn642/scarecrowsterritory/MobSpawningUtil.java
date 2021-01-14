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

    private static final Method func_234991_a_;
    private static final Method func_234989_a_;
    private static final Method func_234990_a_;

    static{
        func_234991_a_ = ReflectionUtil.findMethod("func_234991_a_");
        func_234989_a_ = ReflectionUtil.findMethod("func_234989_a_");
        func_234990_a_ = ReflectionUtil.findMethod("func_234990_a_");
    }

    private static boolean func_234991_a_(WorldEntitySpawner.EntityDensityManager densityManager, EntityClassification classification){
        try{
            return (boolean)func_234991_a_.invoke(densityManager, classification);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return false;
        }
    }

    private static boolean func_234989_a_(WorldEntitySpawner.EntityDensityManager densityManager, EntityType<?> type, BlockPos pos, IChunk chunk){
        try{
            return (boolean)func_234989_a_.invoke(densityManager, type, pos, chunk);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            return false;
        }
    }

    private static void func_234990_a_(WorldEntitySpawner.EntityDensityManager densityManager, MobEntity entity, IChunk chunk){
        try{
            func_234990_a_.invoke(densityManager, entity, chunk);
        }catch(IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
        }
    }

    public static void spawnEntitiesInChunk(ServerWorld world, Chunk chunk, WorldEntitySpawner.EntityDensityManager densityManager, boolean spawnPassives, boolean spawnHostiles, boolean spawnAnimals){
        world.getProfiler().startSection("spawner");

        for(EntityClassification classification : EntityClassification.values()){
            if(classification != EntityClassification.MISC &&
                (spawnPassives || !classification.getPeacefulCreature()) &&
                (spawnHostiles || classification.getPeacefulCreature()) &&
                (spawnAnimals || !classification.getAnimal()) &&
                func_234991_a_(densityManager, classification)){

                func_234967_a_(classification, world, chunk,
                    (type, pos, c) -> func_234989_a_(densityManager, type, pos, c),
                    (entity, c) -> func_234990_a_(densityManager, entity, c));
            }
        }

        world.getProfiler().endSection();
    }

    private static void func_234967_a_(EntityClassification classification, ServerWorld world, Chunk chunk, WorldEntitySpawner.IDensityCheck densityCheck, WorldEntitySpawner.IOnSpawnDensityAdder densityAdder){
        BlockPos blockpos = getRandomHeight(world, chunk);
        if(blockpos.getY() >= 1){
            spawnEntitiesAtPos(classification, world, chunk, blockpos, densityCheck, densityAdder);
        }
    }

    private static BlockPos getRandomHeight(World world, Chunk chunk){
        ChunkPos chunkpos = chunk.getPos();
        int x = chunkpos.getXStart() + world.rand.nextInt(16);
        int z = chunkpos.getZStart() + world.rand.nextInt(16);
        int y = chunk.getTopBlockY(Heightmap.Type.WORLD_SURFACE, x, z) + 1;
        y = world.rand.nextInt(y + 1);
        return new BlockPos(x, y, z);
    }

    private static void spawnEntitiesAtPos(EntityClassification classification, ServerWorld world, IChunk chunk, BlockPos pos, WorldEntitySpawner.IDensityCheck densityCheck, WorldEntitySpawner.IOnSpawnDensityAdder densityAdder){
        StructureManager structuremanager = world.func_241112_a_();
        ChunkGenerator chunkgenerator = world.getChunkProvider().getChunkGenerator();
        int y = pos.getY();
        BlockState blockstate = chunk.getBlockState(pos);
        if(!blockstate.isNormalCube(chunk, pos)){
            BlockPos.Mutable spawnPos = new BlockPos.Mutable();
            int entitiesSpawned = 0;

            // try spawning a group 3 times
            for(int k = 0; k < 3; ++k){
                int spawnX = pos.getX();
                int spawnZ = pos.getZ();
                MobSpawnInfo.Spawners spawner = null;
                ILivingEntityData ilivingentitydata = null;
                int groupSize = MathHelper.ceil(world.rand.nextFloat() * 4.0F);
                int entitiesInGroup = 0;

                // try spawning entities in the group
                for(int i2 = 0; i2 < groupSize; ++i2){
                    spawnX += world.rand.nextInt(6) - world.rand.nextInt(6);
                    spawnZ += world.rand.nextInt(6) - world.rand.nextInt(6);
                    spawnPos.setPos(spawnX, y, spawnZ);
                    double spawnXCenter = (double)spawnX + 0.5D;
                    double spawnZCenter = (double)spawnZ + 0.5D;
                    PlayerEntity playerentity = world.getClosestPlayer(spawnXCenter, y, spawnZCenter, -1.0D, false);
                    if(playerentity != null){
                        double playerDistanceSq = playerentity.getDistanceSq(spawnXCenter, y, spawnZCenter);
                        if(isFarEnoughFromPlayer(world, chunk, spawnPos, playerDistanceSq)){
                            if(spawner == null){
                                spawner = getEntitySpawner(world, structuremanager, chunkgenerator, classification, world.rand, spawnPos);
                                if(spawner == null){
                                    break;
                                }

                                groupSize = spawner.minCount + world.rand.nextInt(1 + spawner.maxCount - spawner.minCount);
                            }

                            if(canEntitySpawnAt(world, classification, structuremanager, chunkgenerator, spawner, spawnPos, playerDistanceSq) && densityCheck.test(spawner.type, spawnPos, chunk)){
                                MobEntity mobentity = createEntity(world, spawner.type);
                                if(mobentity == null){
                                    return;
                                }

                                mobentity.setLocationAndAngles(spawnXCenter, y, spawnZCenter, world.rand.nextFloat() * 360.0F, 0.0F);
                                int canSpawn = net.minecraftforge.common.ForgeHooks.canEntitySpawn(mobentity, world, spawnXCenter, y, spawnZCenter, null, SpawnReason.NATURAL);
                                if(canSpawn != -1 && (canSpawn == 1 || (mobentity.canSpawn(world, SpawnReason.NATURAL) && mobentity.isNotColliding(world)))){
                                    if(!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(mobentity, world, (float)spawnXCenter, (float)y, (float)spawnZCenter, null, SpawnReason.NATURAL))
                                        ilivingentitydata = mobentity.onInitialSpawn(world, world.getDifficultyForLocation(mobentity.getPosition()), SpawnReason.NATURAL, ilivingentitydata, (CompoundNBT)null);
                                    entitiesSpawned++;
                                    entitiesInGroup++;
                                    world.func_242417_l(mobentity);
                                    densityAdder.run(mobentity, chunk);
                                    if(entitiesSpawned >= net.minecraftforge.event.ForgeEventFactory.getMaxSpawnPackSize(mobentity)){
                                        return;
                                    }

                                    if(mobentity.isMaxGroupSize(entitiesInGroup)){
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
        }else if(world.getSpawnPoint().withinDistance(pos, 24.0D)){
            return false;
        }else{
            ChunkPos chunkpos = new ChunkPos(pos);
            return Objects.equals(chunkpos, chunk.getPos()) || world.getChunkProvider().isChunkLoaded(chunkpos);
        }
    }

    private static boolean canEntitySpawnAt(ServerWorld world, EntityClassification classification, StructureManager structureManager, ChunkGenerator chunkGenerator, MobSpawnInfo.Spawners spawners, BlockPos.Mutable pos, double playerDistanceSq){
        EntityType<?> entityType = spawners.type;
        if(entityType.getClassification() == EntityClassification.MISC)
            return false;

        if(entityType.isSummonable() && isEntityInSpawnListAt(world, structureManager, chunkGenerator, classification, spawners, pos)){
            EntitySpawnPlacementRegistry.PlacementType placementType = EntitySpawnPlacementRegistry.getPlacementType(entityType);
            if(!WorldEntitySpawner.canCreatureTypeSpawnAtLocation(placementType, world, pos, entityType) ||
                !EntitySpawnPlacementRegistry.canSpawnEntity(entityType, world, SpawnReason.NATURAL, pos, world.rand))
                return false;

            return world.hasNoCollisions(entityType.getBoundingBoxWithSizeApplied(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
        }

        return false;
    }

    private static boolean isEntityInSpawnListAt(ServerWorld world, StructureManager structureManager, ChunkGenerator chunkGenerator, EntityClassification classification, MobSpawnInfo.Spawners spawners, BlockPos pos){
        Biome biome = world.getBiome(pos);
        return getSpawnList(world, structureManager, chunkGenerator, classification, pos, biome).contains(spawners);
    }

    private static List<MobSpawnInfo.Spawners> getSpawnList(ServerWorld world, StructureManager structureManager, ChunkGenerator chunkGenerator, EntityClassification classification, BlockPos pos, Biome biome){
        return classification == EntityClassification.MONSTER &&
            world.getBlockState(pos.down()).getBlock() == Blocks.NETHER_BRICKS &&
            structureManager.getStructureStart(pos, false, Structure.FORTRESS).isValid() ?
            Structure.FORTRESS.getSpawnList() : chunkGenerator.func_230353_a_(biome, structureManager, classification, pos);
    }

    @Nullable
    private static MobSpawnInfo.Spawners getEntitySpawner(ServerWorld world, StructureManager structureManager, ChunkGenerator chunkGenerator, EntityClassification classification, Random random, BlockPos pos){
        Biome biome = world.getBiome(pos);
        if(classification == EntityClassification.WATER_AMBIENT && biome.getCategory() == Biome.Category.RIVER && random.nextFloat() < 0.98F){
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
