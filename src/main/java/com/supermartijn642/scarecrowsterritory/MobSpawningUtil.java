package com.supermartijn642.scarecrowsterritory;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.lang.reflect.Method;
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
            chunks = chunks.stream().filter(pos -> level.isAreaLoaded(pos.getBlock(0, 0, 0), 0)).collect(Collectors.toSet());
            if(chunks.size() > 0)
                trySpawnEntitiesInChunks(level, chunks, spawnPassives, spawnHostiles, spawnAnimals);
        }
    }

    private static void trySpawnEntitiesInChunks(WorldServer level, Set<ChunkPos> chunks, boolean spawnPassives, boolean spawnHostiles, boolean spawnAnimals){
        int i = chunks.size() + Math.max(level.getChunkProvider().getLoadedChunkCount(), ScarecrowTracker.getNumberOfChunksToSpawnMobsIn(level));

        int entitiesSpawned = 0;
        BlockPos worldSpawnPoint = level.getSpawnPoint();

        for(EnumCreatureType creatureType : EnumCreatureType.values()){
            if((!creatureType.getPeacefulCreature() || spawnPassives) && (creatureType.getPeacefulCreature() || spawnHostiles) && (!creatureType.getAnimal() || spawnAnimals)){
                int k4 = level.countEntities(creatureType, true);
                int l4 = creatureType.getMaxNumberOfCreature() * i / (17 * 17);

                if(k4 <= l4){
                    java.util.ArrayList<ChunkPos> shuffled = com.google.common.collect.Lists.newArrayList(chunks);
                    java.util.Collections.shuffle(shuffled);
                    BlockPos.MutableBlockPos spawnPos = new BlockPos.MutableBlockPos();
                    label134:

                    for(ChunkPos chunkpos1 : shuffled){
                        BlockPos randomChunkPos = getRandomChunkPosition(level, chunkpos1.x, chunkpos1.z);
                        int randomChunkX = randomChunkPos.getX();
                        int randomChunkY = randomChunkPos.getY();
                        int randomChunkZ = randomChunkPos.getZ();
                        IBlockState iblockstate = level.getBlockState(randomChunkPos);

                        if(!iblockstate.isNormalCube()){
                            int entitiesInGroup = 0;

                            // try spawning entity groups 3 times
                            for(int k2 = 0; k2 < 3; ++k2){
                                int spawnX = randomChunkX;
                                int spawnY = randomChunkY;
                                int spawnZ = randomChunkZ;
                                Biome.SpawnListEntry spawnEntry = null;
                                IEntityLivingData entityData = null;
                                int groupSize = MathHelper.ceil(Math.random() * 4.0D);

                                // try spawning entities in the group
                                for(int i4 = 0; i4 < groupSize; ++i4){
                                    spawnX += level.rand.nextInt(6) - level.rand.nextInt(6);
                                    spawnY += level.rand.nextInt(1) - level.rand.nextInt(1);
                                    spawnZ += level.rand.nextInt(6) - level.rand.nextInt(6);
                                    spawnPos.setPos(spawnX, spawnY, spawnZ);
                                    float spawnXCenter = spawnX + 0.5F;
                                    float spawnZCenter = spawnZ + 0.5F;

                                    if(worldSpawnPoint.distanceSq(spawnXCenter, spawnY, spawnZCenter) >= 576.0D){
                                        if(spawnEntry == null){
                                            spawnEntry = level.getSpawnListEntryForTypeAt(creatureType, spawnPos);

                                            if(spawnEntry == null){
                                                break;
                                            }
                                        }

                                        if(level.canCreatureTypeSpawnHere(creatureType, spawnEntry, spawnPos) && WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntitySpawnPlacementRegistry.getPlacementForEntity(spawnEntry.entityClass), level, spawnPos)){
                                            EntityLiving entityLiving;

                                            try{
                                                entityLiving = spawnEntry.newInstance(level);
                                            }catch(Exception exception){
                                                exception.printStackTrace();
                                                return;
                                            }

                                            entityLiving.setLocationAndAngles(spawnXCenter, spawnY, spawnZCenter, level.rand.nextFloat() * 360.0F, 0.0F);

                                            Event.Result canSpawn = ForgeEventFactory.canEntitySpawn(entityLiving, level, spawnXCenter, spawnY, spawnZCenter, false);
                                            if(canSpawn == Event.Result.ALLOW || (canSpawn == Event.Result.DEFAULT && (entityLiving.getCanSpawnHere() && entityLiving.isNotColliding()))){
                                                if(!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(entityLiving, level, spawnXCenter, spawnY, spawnZCenter))
                                                    entityData = entityLiving.onInitialSpawn(level.getDifficultyForLocation(new BlockPos(entityLiving)), entityData);

                                                if(entityLiving.isNotColliding()){
                                                    ++entitiesInGroup;
                                                    level.spawnEntity(entityLiving);
                                                }else{
                                                    entityLiving.setDead();
                                                }

                                                if(entitiesInGroup >= net.minecraftforge.event.ForgeEventFactory.getMaxSpawnPackSize(entityLiving)){
                                                    continue label134;
                                                }
                                            }

                                            entitiesSpawned += entitiesInGroup;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
