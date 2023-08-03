package com.supermartijn642.scarecrowsterritory;

import com.google.common.collect.Lists;
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
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
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
        BlockPos worldSpawnPoint = level.getSpawnPoint();

        for(EnumCreatureType classification : EnumCreatureType.values()){
            if((!classification.getPeacefulCreature() || spawnPassives) &&
                (classification.getPeacefulCreature() || spawnHostiles) &&
                (!classification.getAnimal() || spawnAnimals) &&
                canSpawnForCategory(classification, level)){

                ArrayList<ChunkPos> shuffled = Lists.newArrayList(chunks);
                Collections.shuffle(shuffled);
                BlockPos.MutableBlockPos spawnPos = new BlockPos.MutableBlockPos();

                label134:
                for(ChunkPos chunkPos : shuffled){
                    BlockPos randomChunkPos = getRandomChunkPosition(level, chunkPos.x, chunkPos.z);
                    int randomChunkX = randomChunkPos.getX();
                    int randomChunkY = randomChunkPos.getY();
                    int randomChunkZ = randomChunkPos.getZ();
                    IBlockState state = level.getBlockState(randomChunkPos);

                    if(!state.isNormalCube()){
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
                                spawnY += level.rand.nextInt(2) - level.rand.nextInt(2);
                                spawnZ += level.rand.nextInt(6) - level.rand.nextInt(6);
                                spawnPos.setPos(spawnX, spawnY, spawnZ);
                                float spawnXCenter = spawnX + 0.5F;
                                float spawnZCenter = spawnZ + 0.5F;

                                if(worldSpawnPoint.distanceSq(spawnXCenter, spawnY, spawnZCenter) >= 576.0D){
                                    if(!ScarecrowTracker.isScarecrowInRange(level, new Vec3d(spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5), ScarecrowsTerritoryConfig.passiveMobRange.get()))
                                        continue;

                                    if(spawnEntry == null){
                                        spawnEntry = level.getSpawnListEntryForTypeAt(classification, spawnPos);

                                        if(spawnEntry == null){
                                            break;
                                        }
                                    }

                                    if(level.canCreatureTypeSpawnHere(classification, spawnEntry, spawnPos) && WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntitySpawnPlacementRegistry.getPlacementForEntity(spawnEntry.entityClass), level, spawnPos)){
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
                                    }
                                }
                            }
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
