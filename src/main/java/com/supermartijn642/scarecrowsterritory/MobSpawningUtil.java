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
    private static BlockPos getRandomChunkPosition(World world, int x, int z){
        try{
            return (BlockPos)getRandomChunkPosition.invoke(null, world, x, z);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static void spawnEntitiesInChunks(WorldServer world, Set<ChunkPos> chunks, boolean spawnPassives, boolean spawnHostiles, boolean spawnAnimals){
        if(spawnHostiles || spawnPassives || spawnAnimals){
            chunks = chunks.stream().filter(pos -> world.isAreaLoaded(pos.getBlock(0, 0, 0), 0)).collect(Collectors.toSet());
            if(chunks.size() > 0)
                trySpawnEntitiesInChunks(world, chunks, spawnPassives, spawnHostiles, spawnAnimals);
        }
    }

    private static void trySpawnEntitiesInChunks(WorldServer worldServerIn, Set<ChunkPos> chunks, boolean spawnPassives, boolean spawnHostiles, boolean spawnAnimals){
        int i = chunks.size() + worldServerIn.getChunkProvider().getLoadedChunkCount();

        int entitiesSpawned = 0;
        BlockPos worldSpawnPoint = worldServerIn.getSpawnPoint();

        for(EnumCreatureType enumcreaturetype : EnumCreatureType.values()){
            if((!enumcreaturetype.getPeacefulCreature() || spawnPassives) && (enumcreaturetype.getPeacefulCreature() || spawnHostiles) && (!enumcreaturetype.getAnimal() || spawnAnimals)){
                int k4 = worldServerIn.countEntities(enumcreaturetype, true);
                int l4 = enumcreaturetype.getMaxNumberOfCreature() * i / (17 * 17);

                if(k4 <= l4){
                    java.util.ArrayList<ChunkPos> shuffled = com.google.common.collect.Lists.newArrayList(chunks);
                    java.util.Collections.shuffle(shuffled);
                    BlockPos.MutableBlockPos spawnPos = new BlockPos.MutableBlockPos();
                    label134:

                    for(ChunkPos chunkpos1 : shuffled){
                        BlockPos randomChunkPos = getRandomChunkPosition(worldServerIn, chunkpos1.x, chunkpos1.z);
                        int randomChunkX = randomChunkPos.getX();
                        int randomChunkY = randomChunkPos.getY();
                        int randomChunkZ = randomChunkPos.getZ();
                        IBlockState iblockstate = worldServerIn.getBlockState(randomChunkPos);

                        if(!iblockstate.isNormalCube()){
                            int entitiesInGroup = 0;

                            // try spawning entity groups 3 times
                            for(int k2 = 0; k2 < 3; ++k2){
                                int spawnX = randomChunkX;
                                int spawnY = randomChunkY;
                                int spawnZ = randomChunkZ;
                                Biome.SpawnListEntry spawnEntry = null;
                                IEntityLivingData ientitylivingdata = null;
                                int groupSize = MathHelper.ceil(Math.random() * 4.0D);

                                // try spawning entities in the group
                                for(int i4 = 0; i4 < groupSize; ++i4){
                                    spawnX += worldServerIn.rand.nextInt(6) - worldServerIn.rand.nextInt(6);
                                    spawnY += worldServerIn.rand.nextInt(1) - worldServerIn.rand.nextInt(1);
                                    spawnZ += worldServerIn.rand.nextInt(6) - worldServerIn.rand.nextInt(6);
                                    spawnPos.setPos(spawnX, spawnY, spawnZ);
                                    float spawnXCenter = (float)spawnX + 0.5F;
                                    float spawnZCenter = (float)spawnZ + 0.5F;

                                    if(worldSpawnPoint.distanceSq(spawnXCenter, spawnY, spawnZCenter) >= 576.0D){
                                        if(spawnEntry == null){
                                            spawnEntry = worldServerIn.getSpawnListEntryForTypeAt(enumcreaturetype, spawnPos);

                                            if(spawnEntry == null){
                                                break;
                                            }
                                        }

                                        if(worldServerIn.canCreatureTypeSpawnHere(enumcreaturetype, spawnEntry, spawnPos) && WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntitySpawnPlacementRegistry.getPlacementForEntity(spawnEntry.entityClass), worldServerIn, spawnPos)){
                                            EntityLiving entityliving;

                                            try{
                                                entityliving = spawnEntry.newInstance(worldServerIn);
                                            }catch(Exception exception){
                                                exception.printStackTrace();
                                                return;
                                            }

                                            entityliving.setLocationAndAngles(spawnXCenter, spawnY, spawnZCenter, worldServerIn.rand.nextFloat() * 360.0F, 0.0F);

                                            net.minecraftforge.fml.common.eventhandler.Event.Result canSpawn = net.minecraftforge.event.ForgeEventFactory.canEntitySpawn(entityliving, worldServerIn, spawnXCenter, spawnY, spawnZCenter, false);
                                            if(canSpawn == net.minecraftforge.fml.common.eventhandler.Event.Result.ALLOW || (canSpawn == net.minecraftforge.fml.common.eventhandler.Event.Result.DEFAULT && (entityliving.getCanSpawnHere() && entityliving.isNotColliding()))){
                                                if(!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(entityliving, worldServerIn, spawnXCenter, spawnY, spawnZCenter))
                                                    ientitylivingdata = entityliving.onInitialSpawn(worldServerIn.getDifficultyForLocation(new BlockPos(entityliving)), ientitylivingdata);

                                                if(entityliving.isNotColliding()){
                                                    ++entitiesInGroup;
                                                    worldServerIn.spawnEntity(entityliving);
                                                }else{
                                                    entityliving.setDead();
                                                }

                                                if(entitiesInGroup >= net.minecraftforge.event.ForgeEventFactory.getMaxSpawnPackSize(entityliving)){
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
