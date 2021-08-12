package com.supermartijn642.scarecrowsterritory;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.spawner.WorldEntitySpawner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Random;

/**
 * Created 1/14/2021 by SuperMartijn642
 */
public class MobSpawningUtil {

    /**
     * Copied from {@link net.minecraft.world.server.ServerChunkProvider#MAGIC_NUMBER}
     */
    private static final int MAGIC_NUMBER = (int)Math.pow(17.0D, 2.0D);

    private static final Field distanceManager;

    private static final Method getRandomPosWithin;
    private static final Method getSpawnList;
    private static final Method getSpawnList2;

    static{
        distanceManager = ReflectionUtil.findField(ServerChunkProvider.class, "field_217240_d");

        getRandomPosWithin = ReflectionUtil.findMethod(WorldEntitySpawner.class, "func_222262_a", World.class, Chunk.class);
        getSpawnList = ReflectionUtil.findMethod(WorldEntitySpawner.class, "getSpawnList", ChunkGenerator.class, EntityClassification.class, Random.class, BlockPos.class, World.class);
        getSpawnList2 = ReflectionUtil.findMethod(WorldEntitySpawner.class, "getSpawnList", ChunkGenerator.class, EntityClassification.class, Biome.SpawnListEntry.class, BlockPos.class, World.class);
    }

    /**
     * {@link ServerChunkProvider#distanceManager}
     */
    private static TicketManager getDistanceManager(ServerChunkProvider serverChunkProvider){
        try{
            return (TicketManager)distanceManager.get(serverChunkProvider);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * {@link WorldEntitySpawner#getSpawnList(ChunkGenerator, EntityClassification, Random, BlockPos, World)}
     */
    private static Biome.SpawnListEntry getSpawnList(ChunkGenerator<?> chunkGenerator, EntityClassification classification, Random random, BlockPos pos, World world){
        try{
            return (Biome.SpawnListEntry)getSpawnList.invoke(null, chunkGenerator, classification, random, pos, world);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * {@link WorldEntitySpawner#getSpawnList(ChunkGenerator, EntityClassification, Biome.SpawnListEntry, BlockPos, World)}
     */
    private static boolean getSpawnList(ChunkGenerator<?> chunkGenerator, EntityClassification classification, Biome.SpawnListEntry spawnListEntry, BlockPos pos, World world){
        try{
            return (boolean)getSpawnList2.invoke(null, chunkGenerator, classification, spawnListEntry, pos, world);
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * {@link WorldEntitySpawner#getRandomPosWithin(World, Chunk)}
     */
    private static BlockPos getRandomPosWithin(World world, Chunk chunk){
        try{
            return (BlockPos)getRandomPosWithin.invoke(null, world, chunk);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * {@link WorldEntitySpawner#spawnForChunk(ServerWorld, Chunk, WorldEntitySpawner.EntityDensityManager, boolean, boolean, boolean)}
     */
    public static void spawnEntitiesInChunk(ServerWorld world, Chunk chunk, boolean spawnPassives, boolean spawnHostiles, boolean spawnAnimals){
        world.getProfiler().push("spawner");

        for(EntityClassification classification : EntityClassification.values()){
            if(classification != EntityClassification.MISC &&
                (spawnPassives || !classification.isFriendly()) &&
                (spawnHostiles || classification.isFriendly()) &&
                (spawnAnimals || !classification.isPersistent())){

                Object2IntMap<EntityClassification> categoryCounts = world.getMobCategoryCounts();
                int spawnChunkCount = getDistanceManager(world.getChunkSource()).getNaturalSpawnChunkCount();
                int categoryMobCap = classification.getMaxInstancesPerChunk() * spawnChunkCount / MAGIC_NUMBER;
                if(categoryCounts.getInt(classification) <= categoryMobCap){
                    spawnCategoryForChunk(classification, world, chunk);
                }
            }
        }

        world.getProfiler().pop();
    }

    /**
     * {@link WorldEntitySpawner#spawnCategoryForChunk(EntityClassification, ServerWorld, Chunk, BlockPos)}
     */
    private static void spawnCategoryForChunk(EntityClassification classification, ServerWorld world, Chunk chunk){
        BlockPos blockpos = getRandomPosWithin(world, chunk);
        if(blockpos != null && blockpos.getY() >= 1){
            spawnCategoryForPosition(classification, world, chunk, blockpos);
        }
    }

    /**
     * {@link WorldEntitySpawner#spawnCategoryForChunk(EntityClassification, ServerWorld, Chunk, BlockPos)}
     */
    private static void spawnCategoryForPosition(EntityClassification classification, ServerWorld world, Chunk chunk, BlockPos pos){
        ChunkGenerator<?> chunkgenerator = world.getChunkSource().getGenerator();
        int entitiesSpawned = 0;
        int y = pos.getY();
        BlockState blockstate = chunk.getBlockState(pos);
        if(!blockstate.isRedstoneConductor(chunk, pos)){
            BlockPos.MutableBlockPos spawnPos = new BlockPos.MutableBlockPos();
            int groupsSpawned = 0;

            // try spawning a group 3 times
            while(groupsSpawned < 3){
                int spawnX = pos.getX();
                int spawnZ = pos.getZ();
                Biome.SpawnListEntry spawnListEntry = null;
                ILivingEntityData ilivingentitydata = null;
                int groupSize = MathHelper.ceil(Math.random() * 4.0D);
                int entitiesInGroup = 0;

                // try spawning entities in the group
                for(int i2 = 0; i2 < groupSize; i2++){
                    spawnX += world.random.nextInt(6) - world.random.nextInt(6);
                    spawnZ += world.random.nextInt(6) - world.random.nextInt(6);
                    spawnPos.set(spawnX, y, spawnZ);
                    float spawnXCenter = (float)spawnX + 0.5F;
                    float spawnZCenter = (float)spawnZ + 0.5F;

                    if(world.getSharedSpawnPos().closerThan(new Vec3d(spawnXCenter, y, spawnZCenter), 24.0D))
                        continue;

                    ChunkPos chunkpos = new ChunkPos(spawnPos);
                    if(!Objects.equals(chunkpos, chunk.getPos()) && !world.getChunkSource().isEntityTickingChunk(chunkpos))
                        continue;

                    if(spawnListEntry == null){
                        spawnListEntry = getSpawnList(chunkgenerator, classification, world.random, spawnPos, world);
                        if(spawnListEntry == null){
                            break;
                        }

                        groupSize = spawnListEntry.minCount + world.random.nextInt(1 + spawnListEntry.maxCount - spawnListEntry.minCount);
                    }

                    if(spawnListEntry.type.getCategory() == EntityClassification.MISC)
                        continue;

                    EntityType<?> entityType = spawnListEntry.type;
                    if(!entityType.canSummon() || !getSpawnList(chunkgenerator, classification, spawnListEntry, spawnPos, world))
                        continue;

                    EntitySpawnPlacementRegistry.PlacementType placementType = EntitySpawnPlacementRegistry.getPlacementType(entityType);
                    if(!WorldEntitySpawner.isSpawnPositionOk(placementType, world, spawnPos, entityType) || !EntitySpawnPlacementRegistry.checkSpawnRules(entityType, world, SpawnReason.NATURAL, spawnPos, world.random) || !world.noCollision(entityType.getAABB((double)spawnXCenter, (double)y, (double)spawnZCenter))){
                        continue;
                    }

                    MobEntity mobentity;
                    try{
                        Entity entity = entityType.create(world);
                        if(!(entity instanceof MobEntity)){
                            throw new IllegalStateException("Trying to spawn a non-mob: " + Registry.ENTITY_TYPE.getKey(entityType));
                        }

                        mobentity = (MobEntity)entity;
                    }catch(Exception exception){
                        System.err.println("Failed to create mob");
                        exception.printStackTrace();
                        return;
                    }

                    mobentity.moveTo(spawnXCenter, y, spawnZCenter, world.random.nextFloat() * 360, 0);
                    int canSpawn = net.minecraftforge.common.ForgeHooks.canEntitySpawn(mobentity, world, spawnXCenter, y, spawnZCenter, null, SpawnReason.NATURAL);
                    if(canSpawn != -1 && (canSpawn == 0 || (!mobentity.checkSpawnRules(world, SpawnReason.NATURAL) && mobentity.checkSpawnObstruction(world)))){
                        if(!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(mobentity, world, spawnXCenter, y, spawnZCenter, null, SpawnReason.NATURAL))
                            ilivingentitydata = mobentity.finalizeSpawn(world, world.getCurrentDifficultyAt(mobentity.getCommandSenderBlockPosition()), SpawnReason.NATURAL, ilivingentitydata, (CompoundNBT)null);
                        entitiesSpawned++;
                        entitiesInGroup++;
                        world.addFreshEntity(mobentity);
                        if(entitiesSpawned >= net.minecraftforge.event.ForgeEventFactory.getMaxSpawnPackSize(mobentity)){
                            return;
                        }

                        if(mobentity.isMaxGroupSizeReached(entitiesInGroup)){
                            break;
                        }
                    }
                }
                groupsSpawned++;
            }
        }
    }

}
