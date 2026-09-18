package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.ClientUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 1/13/2021 by SuperMartijn642
 */
public class ScarecrowTracker {

    private static final ScarecrowTracker SERVER = new ScarecrowTracker(), CLIENT = new ScarecrowTracker();

    public static ScarecrowTracker get(boolean client){
        return client ? CLIENT : SERVER;
    }

    public static ScarecrowTracker get(World level){
        return get(level.isRemote);
    }

    public static void registerListeners(){
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void allowDespawn(LivingSpawnEvent.AllowDespawn e){
                Boolean result = get(e.getEntity().world).shouldEntityDespawn(e.getEntityLiving());
                if(result != null)
                    e.setResult(result ? Event.Result.ALLOW : Event.Result.DENY);
            }

            @SubscribeEvent
            public void worldTick(TickEvent.WorldTickEvent e){
                get(e.world).onWorldTick(e.world);
            }

            @SubscribeEvent
            public void worldUnload(WorldEvent.Unload e){
                get(e.getWorld()).onWorldUnload(e.getWorld());
            }

            @SubscribeEvent
            public void chunkLoad(ChunkEvent.Load e){
                get(e.getWorld()).onChunkLoad(e.getWorld(), e.getChunk());
            }

            @SubscribeEvent
            public void chunkUnload(ChunkEvent.Unload e){
                get(e.getWorld()).onChunkUnload(e.getWorld(), e.getChunk());
            }

            @SubscribeEvent
            public void placeBlock(BlockEvent.EntityPlaceEvent e){
                get(e.getWorld()).onBlockAdded(e.getWorld(), e.getPos(), e.getPlacedBlock());
            }

            @SubscribeEvent
            public void breakBlock(BlockEvent.BreakEvent e){
                get(e.getWorld()).onBlockBreak(e.getWorld(), e.getPos(), e.getState());
            }
        });
    }

    private final Map<World,Set<BlockPos>> scarecrowsPerWorld = new HashMap<>();
    private final Map<World,Map<ChunkPos,Integer>> chunksToSpawnMobs = new HashMap<>();

    public Boolean shouldEntityDespawn(EntityLivingBase entity){
        if(!ScarecrowsTerritoryConfig.passiveMobSpawning.get() || entity.world.isRemote)
            return true;

        double range = Math.max(ScarecrowsTerritoryConfig.passiveMobRange.get(), ScarecrowsTerritoryConfig.loadSpawnerRange.get()) + ScarecrowsTerritoryConfig.noDespawnBuffer.get();
        return !this.isScarecrowInRange(entity.world, entity.getPositionVector(), range);
    }

    private void onWorldTick(World level){
        if(!ScarecrowsTerritoryConfig.passiveMobSpawning.get() || level.isRemote
            || !(level instanceof WorldServer) || level.getWorldType() == WorldType.DEBUG_ALL_BLOCK_STATES)
            return;

        if(!this.chunksToSpawnMobs.containsKey(level) || !level.getGameRules().getBoolean("doMobSpawning"))
            return;

        this.spawnEntitiesInChunks((WorldServer)level);
    }

    private void spawnEntitiesInChunks(WorldServer level){
        boolean spawnAnimals = level.getWorldInfo().getWorldTime() % 400L == 0L;
        boolean spawnHostiles = level.getDifficulty() != EnumDifficulty.PEACEFUL;
        Set<Map.Entry<ChunkPos,Integer>> entries = this.chunksToSpawnMobs.getOrDefault(level, Collections.emptyMap()).entrySet();
        Set<ChunkPos> chunks = entries.stream().filter(entry -> entry.getValue() > 0).map(Map.Entry::getKey).collect(Collectors.toSet());
        MobSpawningUtil.spawnEntitiesInChunks(level, chunks, true, spawnHostiles, spawnAnimals);
    }

    private void addScarecrow(World level, BlockPos pos){
        this.scarecrowsPerWorld.putIfAbsent(level, new HashSet<>());
        this.scarecrowsPerWorld.computeIfPresent(level, (w, s) -> {
            s.add(pos);
            return s;
        });

        int range = (int)Math.ceil(ScarecrowsTerritoryConfig.passiveMobRange.get());
        int minX = (pos.getX() - range) >> 4, maxX = (pos.getX() + range) >> 4;
        int minZ = (pos.getZ() - range) >> 4, maxZ = (pos.getZ() + range) >> 4;
        this.chunksToSpawnMobs.putIfAbsent(level, new LinkedHashMap<>());
        this.chunksToSpawnMobs.computeIfPresent(level, (w, s) -> {
            for(int x = minX; x <= maxX; x++){
                for(int z = minZ; z <= maxZ; z++){
                    ChunkPos chunk = new ChunkPos(x, z);
                    s.putIfAbsent(chunk, 0);
                    s.computeIfPresent(chunk, (c, i) -> i + 1);
                }
            }
            return s;
        });
    }

    private void removeScarecrow(World level, BlockPos pos){
        this.scarecrowsPerWorld.computeIfPresent(level, (w, s) -> {
            s.remove(pos);
            return s;
        });

        int range = (int)Math.ceil(ScarecrowsTerritoryConfig.passiveMobRange.get());
        int minX = (pos.getX() - range) >> 4, maxX = (pos.getX() + range) >> 4;
        int minZ = (pos.getZ() - range) >> 4, maxZ = (pos.getZ() + range) >> 4;
        this.chunksToSpawnMobs.computeIfPresent(level, (w, s) -> {
            for(int x = minX; x <= maxX; x++){
                for(int z = minZ; z <= maxZ; z++){
                    ChunkPos chunk = new ChunkPos(x, z);
                    if(s.containsKey(chunk) && s.get(chunk) == 1)
                        s.remove(chunk);
                    else
                        s.computeIfPresent(chunk, (c, i) -> Math.max(i - 1, 0));
                }
            }
            return s;
        });
    }

    private void onWorldUnload(World level){
        this.scarecrowsPerWorld.remove(level);
        this.chunksToSpawnMobs.remove(level);
    }

    private void onChunkLoad(World level, Chunk chunk){
        for(Map.Entry<BlockPos,TileEntity> entry : chunk.getTileEntityMap().entrySet()){
            Runnable task = () -> {
                if(entry.getValue() instanceof ScarecrowBlockEntity)
                    this.addScarecrow(level, entry.getKey());
            };
            if(level.isRemote)
                ClientUtils.queueTask(task);
            else
                level.getMinecraftServer().addScheduledTask(task);
        }
    }

    private void onChunkUnload(World level, Chunk chunk){
        for(Map.Entry<BlockPos,TileEntity> entry : chunk.getTileEntityMap().entrySet()){
            if(entry.getValue() instanceof ScarecrowBlockEntity)
                this.removeScarecrow(level, entry.getKey());
        }
    }

    private void onBlockAdded(World level, BlockPos pos, IBlockState placedState){
        if(placedState.getBlock() instanceof ScarecrowBlock){
            this.addScarecrow(level, pos);

            boolean bottom = placedState.getValue(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? pos.up() : pos.down();
            IBlockState state = level.getBlockState(otherHalf);
            if(state.getBlock() instanceof ScarecrowBlock && state.getValue(ScarecrowBlock.BOTTOM) != bottom)
                this.addScarecrow(level, otherHalf);
        }
    }

    private void onBlockBreak(World level, BlockPos pos, IBlockState removedState){
        if(removedState.getBlock() instanceof ScarecrowBlock){
            this.removeScarecrow(level, pos);

            boolean bottom = removedState.getValue(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? pos.up() : pos.down();
            this.removeScarecrow(level, otherHalf);
        }
    }

    public boolean isScarecrowInRange(World level, Vec3d pos, double range){
        Set<BlockPos> scarecrows = this.scarecrowsPerWorld.getOrDefault(level, Collections.emptySet());
        for(BlockPos scarecrow : scarecrows){ // TODO this is dumb, don't iterate all scarecrows
            Vec3d center = new Vec3d(scarecrow).addVector(0.5, 0.5, 0.5);
            if(Math.abs(center.x - pos.x) <= range && Math.abs(center.y - pos.y) <= range && Math.abs(center.z - pos.z) <= range)
                return true;
        }

        return false;
    }
}
