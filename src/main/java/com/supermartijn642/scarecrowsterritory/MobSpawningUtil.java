package com.supermartijn642.scarecrowsterritory;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.WorldEntitySpawner;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Created 1/14/2021 by SuperMartijn642
 */
public class MobSpawningUtil {

    public static void spawnEntitiesInChunk(ServerWorld world, Chunk chunk, boolean spawnPassives, boolean spawnHostiles, boolean spawnAnimals){
        world.getProfiler().startSection("spawner");

        Object2IntMap<EntityClassification> entityCounts = world.countEntities();
        for(EntityClassification entityclassification : EntityClassification.values()){
            if(entityclassification != EntityClassification.MISC && (!entityclassification.getPeacefulCreature() || spawnPassives) && (entityclassification.getPeacefulCreature() || spawnHostiles) && (!entityclassification.getAnimal() || spawnAnimals)){
                int i1 = entityclassification.getMaxNumberOfCreature() * ScarecrowTracker.getScarecrowCount(world) * 4 / (17 * 17);
                if(entityCounts.getInt(entityclassification) <= i1){
                    func_234967_a_(entityclassification, world, chunk, world.getSpawnPoint());
                }
            }
        }

        world.getProfiler().endSection();
    }

    private static void func_234967_a_(EntityClassification classification, ServerWorld world, Chunk chunk, BlockPos worldSpawnPos){
        BlockPos blockpos = getRandomHeight(world, chunk);
        if(blockpos.getY() >= 1){
            spawnEntitiesInChunk(classification, world, chunk, blockpos, worldSpawnPos);
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

    private static void spawnEntitiesInChunk(EntityClassification classification, ServerWorld world, Chunk chunk, BlockPos pos, BlockPos worldSpawnPos){
        ChunkGenerator<?> chunkGenerator = world.getChunkProvider().getChunkGenerator();
        int y = pos.getY();
        BlockState blockstate = chunk.getBlockState(pos);
        if(!blockstate.isNormalCube(chunk, pos)){
            BlockPos.MutableBlockPos spawnPos = new BlockPos.MutableBlockPos();
            int entitiesSpawned = 0;

            // try spawning a group 3 times
            for(int i1 = 0; i1 < 3; i1++){
                int spawnX = pos.getX();
                int spawnZ = pos.getZ();
                Biome.SpawnListEntry spawner = null;
                ILivingEntityData ilivingentitydata = null;
                int groupSize = MathHelper.ceil(Math.random() * 4.0F);
                int entitiesInGroup = 0;

                // try spawning entities in the group
                for(int i2 = 0; i2 < groupSize; ++i2){
                    spawnX += world.rand.nextInt(6) - world.rand.nextInt(6);
                    spawnZ += world.rand.nextInt(6) - world.rand.nextInt(6);
                    spawnPos.setPos(spawnX, y, spawnZ);
                    float spawnXCenter = spawnX + 0.5F;
                    float spawnZCenter = spawnZ + 0.5F;
                    PlayerEntity playerentity = world.getClosestPlayer(spawnXCenter, y, spawnZCenter, -1.0D, false);
                    if(playerentity != null){
                        double playerDistanceSq = playerentity.getDistanceSq(spawnXCenter, y, spawnZCenter);
                        if(isFarEnoughFromPlayer(world, chunk, spawnPos, playerDistanceSq)){
                            if(spawner == null){
                                spawner = getSpawnListEntry(chunkGenerator, classification, world.rand, spawnPos, world);
                                if(spawner == null){
                                    break;
                                }

                                groupSize = spawner.minGroupCount + world.rand.nextInt(1 + spawner.maxGroupCount - spawner.minGroupCount);
                            }

                            if(canEntitySpawnAt(world, spawner.entityType.getClassification(), chunkGenerator, spawner, spawnPos)){
                                MobEntity mobentity = createEntity(world, spawner.entityType);
                                if(mobentity == null){
                                    return;
                                }

                                mobentity.setLocationAndAngles(spawnXCenter, y, spawnZCenter, world.rand.nextFloat() * 360.0F, 0.0F);
                                int canSpawn = net.minecraftforge.common.ForgeHooks.canEntitySpawn(mobentity, world, spawnXCenter, y, spawnZCenter, null, SpawnReason.NATURAL);
                                if(canSpawn != -1 && (canSpawn == 1 || (mobentity.canSpawn(world, SpawnReason.NATURAL) && mobentity.isNotColliding(world)))){
                                    if(!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(mobentity, world, spawnXCenter, y, spawnZCenter, null, SpawnReason.NATURAL))
                                        ilivingentitydata = mobentity.onInitialSpawn(world, world.getDifficultyForLocation(mobentity.getPosition()), SpawnReason.NATURAL, ilivingentitydata, null);
                                    entitiesSpawned++;
                                    entitiesInGroup++;
                                    world.addEntity(mobentity);
                                    if(entitiesSpawned >= net.minecraftforge.event.ForgeEventFactory.getMaxSpawnPackSize(mobentity)){
                                        return;
                                    }

                                    if(mobentity.func_204209_c(entitiesInGroup)){
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

    private static boolean isFarEnoughFromPlayer(ServerWorld world, IChunk chunk, BlockPos.MutableBlockPos pos, double playerDistanceSq){
        if(playerDistanceSq <= 24 * 24){
            return false;
        }else if(world.getSpawnPoint().withinDistance(pos, 24.0D)){
            return false;
        }else{
            ChunkPos chunkpos = new ChunkPos(pos);
            return Objects.equals(chunkpos, chunk.getPos()) || world.getChunkProvider().isChunkLoaded(chunkpos);
        }
    }

    private static boolean canEntitySpawnAt(ServerWorld world, EntityClassification classification, ChunkGenerator<?> chunkGenerator, Biome.SpawnListEntry spawner, BlockPos.MutableBlockPos pos){
        EntityType<?> entityType = spawner.entityType;
        if(entityType.getClassification() == EntityClassification.MISC)
            return false;

        if(entityType.isSummonable() && isEntityInSpawnListAt(world, chunkGenerator, classification, spawner, pos)){
            EntitySpawnPlacementRegistry.PlacementType placementType = EntitySpawnPlacementRegistry.getPlacementType(entityType);
            if(!WorldEntitySpawner.canCreatureTypeSpawnAtLocation(placementType, world, pos, entityType) ||
                !EntitySpawnPlacementRegistry.func_223515_a(entityType, world, SpawnReason.NATURAL, pos, world.rand))
                return false;

            return world.areCollisionShapesEmpty(entityType.func_220328_a(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
        }

        return false;
    }

    private static boolean isEntityInSpawnListAt(ServerWorld world, ChunkGenerator<?> chunkGenerator, EntityClassification classification, Biome.SpawnListEntry spawners, BlockPos pos){
        return getSpawnList(chunkGenerator, classification, pos, world).contains(spawners);
    }

    @Nullable
    private static Biome.SpawnListEntry getSpawnListEntry(ChunkGenerator<?> chunkGenerator, EntityClassification classification, Random random, BlockPos pos, World world){
        List<Biome.SpawnListEntry> entries = getSpawnList(chunkGenerator, classification, pos, world);
        return entries.isEmpty() ? null : WeightedRandom.getRandomItem(random, entries);
    }

    @Nullable
    private static List<Biome.SpawnListEntry> getSpawnList(ChunkGenerator<?> chunkGenerator, EntityClassification classification, BlockPos pos, World world){
        List<Biome.SpawnListEntry> list = chunkGenerator.getPossibleCreatures(classification, pos);
        list = net.minecraftforge.event.ForgeEventFactory.getPotentialSpawns(world, classification, pos, list);
        return list;
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
