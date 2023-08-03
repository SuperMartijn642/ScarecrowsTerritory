package com.supermartijn642.scarecrowsterritory;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created 1/14/2021 by SuperMartijn642
 */
public class MobSpawningUtil {

    private static final Method getRandomChunkPosition;

    static{
        getRandomChunkPosition = ReflectionUtil.findMethod(WorldEntitySpawner.class, "func_180621_a", BlockPos.class, World.class, int.class, int.class);
    }

    /**
     * {@link WorldEntitySpawner#getRandomChunkPosition(World, int, int)}
     */
    private static BlockPos getRandomChunkPosition(World level, int x, int z){
        try{
            return (BlockPos)getRandomChunkPosition.invoke(null, level, x, z);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static void spawnEntitiesInChunks(WorldServer level, Set<ChunkPos> chunks, boolean spawnPassives, boolean spawnHostiles, boolean spawnAnimals){
        if(spawnHostiles || spawnPassives || spawnAnimals){
            List<ChunkPos> loadedChunks = chunks.stream().filter(pos -> level.isAreaLoaded(pos.getBlock(0, 0, 0), 0)).collect(Collectors.toList());
            Collections.shuffle(loadedChunks);
            for(ChunkPos chunkPos : loadedChunks){
                Chunk chunk = level.getChunkFromChunkCoords(chunkPos.x, chunkPos.z);
                spawnEntitiesInChunk(level, chunk, spawnPassives, spawnHostiles, spawnAnimals);
            }
        }
    }

    public static void spawnEntitiesInChunk(WorldServer level, Chunk chunk, boolean spawnPassives, boolean spawnHostiles, boolean spawnAnimals){
        for(EnumCreatureType classification : EnumCreatureType.values()){
            if((spawnPassives || !classification.getPeacefulCreature()) &&
                (spawnHostiles || classification.getPeacefulCreature()) &&
                (spawnAnimals || !classification.getAnimal()) &&
                canSpawnForCategory(classification, level)){

                spawnCategoryForChunk(classification, level, chunk);
            }
        }
    }

    private static void spawnCategoryForChunk(EnumCreatureType classification, WorldServer level, Chunk chunk){
        BlockPos blockpos = getRandomChunkPosition(level, chunk.x, chunk.z);
        if(blockpos != null && blockpos.getY() >= 1){
            spawnCategoryForPosition(classification, level, chunk, blockpos);
        }
    }

    private static void spawnCategoryForPosition(EnumCreatureType classification, WorldServer level, Chunk chunk, BlockPos pos){
        int y = pos.getY();
        IBlockState blockstate = chunk.getBlockState(pos);
        if(!blockstate.isNormalCube()){
            BlockPos.MutableBlockPos spawnPos = new BlockPos.MutableBlockPos();
            int entitiesSpawned = 0;

            // try spawning a group 3 times
            for(int k = 0; k < 3; ++k){
                int spawnX = pos.getX();
                int spawnZ = pos.getZ();
                Biome.SpawnListEntry spawner = null;
                IEntityLivingData entityData = null;
                int groupSize = MathHelper.ceil(Math.random() * 4.0D);

                // try spawning entities in the group
                for(int i2 = 0; i2 < groupSize; ++i2){
                    spawnX += level.rand.nextInt(6) - level.rand.nextInt(6);
                    spawnZ += level.rand.nextInt(6) - level.rand.nextInt(6);
                    spawnPos.setPos(spawnX, y, spawnZ);
                    double spawnXCenter = (double)spawnX + 0.5D;
                    double spawnZCenter = (double)spawnZ + 0.5D;

                    if(level.getSpawnPoint().distanceSq(spawnXCenter, y, spawnZCenter) < 576.0D)
                        continue;

                    if(!ScarecrowTracker.isScarecrowInRange(level, new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), ScarecrowsTerritoryConfig.passiveMobRange.get()))
                        continue;

                    if(spawner == null){
                        spawner = level.getSpawnListEntryForTypeAt(classification, spawnPos);
                        if(spawner == null)
                            break;
                    }

                    if(level.canCreatureTypeSpawnHere(classification, spawner, spawnPos) && WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntitySpawnPlacementRegistry.getPlacementForEntity(spawner.entityClass), level, spawnPos)){
                        EntityLiving entity;

                        try{
                            entity = spawner.newInstance(level);
                        }catch(Exception exception){
                            exception.printStackTrace();
                            return;
                        }

                        entity.getEntityData().setBoolean("spawnedByScarecrow", true);
                        entity.setLocationAndAngles(spawnXCenter, y, spawnZCenter, level.rand.nextFloat() * 360.0F, 0.0F);
                        Event.Result canSpawn = ForgeEventFactory.canEntitySpawn(entity, level, (float)spawnXCenter, (float)y, (float)spawnZCenter, false);
                        if(canSpawn == Event.Result.ALLOW || (canSpawn == Event.Result.DEFAULT && (entity.getCanSpawnHere() && entity.isNotColliding()))){
                            if(!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(entity, level, (float)spawnXCenter, (float)y, (float)spawnZCenter))
                                entityData = entity.onInitialSpawn(level.getDifficultyForLocation(new BlockPos(entity)), entityData);

                            if(entity.isNotColliding()){
                                entitiesSpawned++;
                                level.spawnEntity(entity);
                            }else
                                entity.setDead();

                            if(entitiesSpawned >= net.minecraftforge.event.ForgeEventFactory.getMaxSpawnPackSize(entity))
                                break;
                        }
                    }
                }
            }
        }
    }

    private static boolean canSpawnForCategory(EnumCreatureType classification, WorldServer level){
        int spawnableChunks = Math.max(1, level.getChunkProvider().getLoadedChunkCount() / (17 * 17));
        return level.countEntities(classification, true) < classification.getMaxNumberOfCreature() * spawnableChunks;
    }
}
