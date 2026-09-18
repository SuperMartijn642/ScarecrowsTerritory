package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.ClientUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;

import java.util.*;
import java.util.function.Consumer;

/**
 * Created 1/13/2021 by SuperMartijn642
 */
public class ScarecrowTracker {

    private static final ScarecrowTracker SERVER = new ScarecrowTracker(), CLIENT = new ScarecrowTracker();

    public static ScarecrowTracker get(boolean client){
        return client ? CLIENT : SERVER;
    }

    public static ScarecrowTracker get(World level){
        return get(level.isClientSide());
    }

    public static void registerListeners(){
        MinecraftForge.EVENT_BUS.addListener((Consumer<LivingSpawnEvent.AllowDespawn>)e -> {
            Boolean result = get(e.getEntity().level).shouldEntityDespawn(e.getEntityLiving());
            if(result != null)
                e.setResult(result ? Event.Result.ALLOW : Event.Result.DENY);
        });
        MinecraftForge.EVENT_BUS.addListener((Consumer<TickEvent.WorldTickEvent>)e -> get(e.world).onWorldTick(e.world));
        MinecraftForge.EVENT_BUS.addListener((Consumer<WorldEvent.Unload>)e -> {
            if(e.getWorld() instanceof World)
                get((World)e.getWorld()).onWorldUnload(e.getWorld());
        });
        MinecraftForge.EVENT_BUS.addListener((Consumer<ChunkEvent.Load>)e -> {
            if(e.getWorld() instanceof World)
                get((World)e.getWorld()).onChunkLoad((World)e.getWorld(), e.getChunk());
        });
        MinecraftForge.EVENT_BUS.addListener((Consumer<ChunkEvent.Unload>)e -> {
            if(e.getWorld() instanceof World)
                get((World)e.getWorld()).onChunkUnload((World)e.getWorld(), e.getChunk());
        });
        MinecraftForge.EVENT_BUS.addListener((Consumer<BlockEvent.EntityPlaceEvent>)e -> {
            if(e.getWorld() instanceof World)
                get((World)e.getWorld()).onBlockAdded((World)e.getWorld(), e.getPos(), e.getPlacedBlock());
        });
        MinecraftForge.EVENT_BUS.addListener((Consumer<BlockEvent.BreakEvent>)e -> {
            if(e.getWorld() instanceof World)
                get((World)e.getWorld()).onBlockBreak((World)e.getWorld(), e.getPos(), e.getState());
        });
    }

    private final Map<IWorld,Set<BlockPos>> scarecrowsPerWorld = new HashMap<>();
    private final Map<IWorld,Map<ChunkPos,Integer>> chunksToSpawnMobs = new HashMap<>();

    public Boolean shouldEntityDespawn(LivingEntity entity){
        if(!ScarecrowsTerritoryConfig.passiveMobSpawning.get() || entity.level.isClientSide())
            return null;

        double range = Math.max(ScarecrowsTerritoryConfig.passiveMobRange.get(), ScarecrowsTerritoryConfig.loadSpawnerRange.get()) + ScarecrowsTerritoryConfig.noDespawnBuffer.get();
        if(this.isScarecrowInRange(entity.level, entity.position(), range))
            return false;
        if(entity.getPersistentData().getBoolean("spawnedByScarecrow"))
            return !(entity instanceof MobEntity) || ((MobEntity)entity).removeWhenFarAway(range * range);
        return null;
    }

    private void onWorldTick(World level){
        if(!ScarecrowsTerritoryConfig.passiveMobSpawning.get() || level.isClientSide()
            || !(level instanceof ServerWorld) || level.getGeneratorType() == WorldType.DEBUG_ALL_BLOCK_STATES)
            return;

        if(!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))
            return;

        Map<ChunkPos,Integer> chunks = this.chunksToSpawnMobs.get(level);
        if(chunks != null){
            for(Map.Entry<ChunkPos,Integer> entry : chunks.entrySet()){
                if(entry.getValue() > 0 && level.getChunkSource().isEntityTickingChunk(entry.getKey())){
                    Chunk chunk = level.getChunkSource().getChunk(entry.getKey().x, entry.getKey().z, false);
                    if(chunk != null && !chunk.isEmpty() && level.getWorldBorder().isWithinBounds(entry.getKey()))
                        this.spawnEntitiesInChunk((ServerWorld)level, chunk);
                }
            }
        }
    }

    private void spawnEntitiesInChunk(ServerWorld level, Chunk chunk){
        boolean spawnAnimals = level.getLevelData().getGameTime() % 400L == 0L;
        boolean spawnHostiles = level.getDifficulty() != Difficulty.PEACEFUL;
        MobSpawningUtil.spawnEntitiesInChunk(level, chunk, true, spawnHostiles, spawnAnimals);
    }

    private void addScarecrow(IWorld level, BlockPos pos){
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

    private void removeScarecrow(IWorld level, BlockPos pos){
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

    private void onWorldUnload(IWorld level){
        this.scarecrowsPerWorld.remove(level);
        this.chunksToSpawnMobs.remove(level);
    }

    private void onChunkLoad(World level, IChunk chunk){
        Runnable task = () -> {
            for(BlockPos pos : chunk.getBlockEntitiesPos()){
                if(chunk.getBlockEntity(pos) instanceof ScarecrowBlockEntity)
                    this.addScarecrow(level, pos);
            }
        };
        if(level.isClientSide())
            ClientUtils.queueTask(task);
        else
            level.getServer().tell(new TickDelayedTask(0, task));
    }

    private void onChunkUnload(World level, IChunk chunk){
        for(BlockPos pos : chunk.getBlockEntitiesPos()){
            if(chunk.getBlockEntity(pos) instanceof ScarecrowBlockEntity)
                this.removeScarecrow(level, pos);
        }
    }

    private void onBlockAdded(World level, BlockPos pos, BlockState placedState){
        if(placedState.getBlock() instanceof ScarecrowBlock){
            this.addScarecrow(level, pos);

            boolean bottom = placedState.getValue(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? pos.above() : pos.below();
            BlockState state = level.getBlockState(otherHalf);
            if(state.getBlock() instanceof ScarecrowBlock && state.getValue(ScarecrowBlock.BOTTOM) != bottom)
                this.addScarecrow(level, otherHalf);
        }
    }

    private void onBlockBreak(World level, BlockPos pos, BlockState removedState){
        if(removedState.getBlock() instanceof ScarecrowBlock){
            this.removeScarecrow(level, pos);

            boolean bottom = removedState.getValue(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? pos.above() : pos.below();
            this.removeScarecrow(level, otherHalf);
        }
    }

    public boolean isScarecrowInRange(World level, Vec3d pos, double range){
        Set<BlockPos> scarecrows = this.scarecrowsPerWorld.getOrDefault(level, Collections.emptySet());
        for(BlockPos scarecrow : scarecrows){ // TODO this is dumb, don't iterate all scarecrows
            Vec3d center = new Vec3d(scarecrow).add(0.5, 0.5, 0.5);
            if(Math.abs(center.x - pos.x) <= range && Math.abs(center.y - pos.y) <= range && Math.abs(center.z - pos.z) <= range)
                return true;
        }

        return false;
    }
}
