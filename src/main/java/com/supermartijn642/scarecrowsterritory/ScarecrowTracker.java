package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.ClientUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

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

    public static ScarecrowTracker get(Level level){
        return get(level.isClientSide());
    }

    public static void registerListeners(){
        NeoForge.EVENT_BUS.addListener((Consumer<MobSpawnEvent.AllowDespawn>)e -> {
            Boolean result = get(e.getEntity().level()).shouldEntityDespawn(e.getEntity());
            if(result != null)
                e.setResult(result ? Event.Result.ALLOW : Event.Result.DENY);
        });
        NeoForge.EVENT_BUS.addListener((Consumer<TickEvent.LevelTickEvent>)e -> get(e.level).onWorldTick(e.level));
        NeoForge.EVENT_BUS.addListener((Consumer<LevelEvent.Unload>)e -> {
            if(e.getLevel() instanceof Level level)
                get(level).onWorldUnload(level);
        });
        NeoForge.EVENT_BUS.addListener((Consumer<ChunkEvent.Load>)e -> {
            if(e.getLevel() instanceof Level level)
                get(level).onChunkLoad(level, e.getChunk());
        });
        NeoForge.EVENT_BUS.addListener((Consumer<ChunkEvent.Unload>)e -> {
            if(e.getLevel() instanceof Level level)
                get(level).onChunkUnload(level, e.getChunk());
        });
        NeoForge.EVENT_BUS.addListener((Consumer<BlockEvent.EntityPlaceEvent>)e -> {
            if(e.getLevel() instanceof Level level)
                get(level).onBlockAdded(level, e.getPos(), e.getPlacedBlock());
        });
        NeoForge.EVENT_BUS.addListener((Consumer<BlockEvent.BreakEvent>)e -> {
            if(e.getLevel() instanceof Level level)
                get(level).onBlockBreak(level, e.getPos(), e.getState());
        });
    }

    private final Map<LevelAccessor,Set<BlockPos>> scarecrowsPerWorld = new HashMap<>();
    private final Map<LevelAccessor,Map<ChunkPos,Integer>> chunksToSpawnMobs = new HashMap<>();

    public Boolean shouldEntityDespawn(Mob mob){
        if(!ScarecrowsTerritoryConfig.passiveMobSpawning.get() || mob.level().isClientSide())
            return null;

        double range = Math.max(ScarecrowsTerritoryConfig.passiveMobRange.get(), ScarecrowsTerritoryConfig.loadSpawnerRange.get()) + ScarecrowsTerritoryConfig.noDespawnBuffer.get();
        if(this.isScarecrowInRange(mob.level(), mob.position(), range))
            return false;
        if(mob.getPersistentData().getBoolean("spawnedByScarecrow"))
            return mob.removeWhenFarAway(range * range);
        return null;
    }

    private void onWorldTick(Level level){
        if(!ScarecrowsTerritoryConfig.passiveMobSpawning.get() || level.isClientSide()
            || !(level instanceof ServerLevel) || level.isDebug())
            return;

        if(!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))
            return;

        Map<ChunkPos,Integer> chunks = this.chunksToSpawnMobs.get(level);
        if(chunks != null){
            for(Map.Entry<ChunkPos,Integer> entry : chunks.entrySet()){
                if(entry.getValue() > 0 && ((ServerLevel)level).getChunkSource().isPositionTicking(entry.getKey().toLong())){
                    LevelChunk chunk = level.getChunkSource().getChunk(entry.getKey().x, entry.getKey().z, false);
                    if(chunk != null && !chunk.isEmpty() && level.getWorldBorder().isWithinBounds(entry.getKey()))
                        this.spawnEntitiesInChunk((ServerLevel)level, chunk);
                }
            }
        }
    }

    private void spawnEntitiesInChunk(ServerLevel level, LevelChunk chunk){
        NaturalSpawner.SpawnState entityDensityManager = level.getChunkSource().getLastSpawnState();
        if(entityDensityManager != null){
            boolean spawnAnimals = level.getLevelData().getGameTime() % 400L == 0L;
            boolean spawnHostiles = level.getDifficulty() != Difficulty.PEACEFUL;
            MobSpawningUtil.spawnEntitiesInChunk(level, chunk, entityDensityManager, true, spawnHostiles, spawnAnimals);
        }
    }

    private void addScarecrow(LevelAccessor level, BlockPos pos){
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

    private void removeScarecrow(LevelAccessor level, BlockPos pos){
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

    private void onWorldUnload(LevelAccessor level){
        this.scarecrowsPerWorld.remove(level);
        this.chunksToSpawnMobs.remove(level);
    }

    private void onChunkLoad(Level level, ChunkAccess chunk){
        Runnable task = () -> {
            for(BlockPos pos : chunk.getBlockEntitiesPos()){
                if(chunk.getBlockEntity(pos) instanceof ScarecrowBlockEntity)
                    this.addScarecrow(level, pos);
            }
        };
        if(level.isClientSide())
            ClientUtils.queueTask(task);
        else
            level.getServer().tell(new TickTask(0, task));
    }

    private void onChunkUnload(Level level, ChunkAccess chunk){
        for(BlockPos pos : chunk.getBlockEntitiesPos()){
            if(chunk.getBlockEntity(pos) instanceof ScarecrowBlockEntity)
                this.removeScarecrow(level, pos);
        }
    }

    private void onBlockAdded(Level level, BlockPos pos, BlockState placedState){
        if(placedState.getBlock() instanceof ScarecrowBlock){
            this.addScarecrow(level, pos);

            boolean bottom = placedState.getValue(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? pos.above() : pos.below();
            BlockState state = level.getBlockState(otherHalf);
            if(state.getBlock() instanceof ScarecrowBlock && state.getValue(ScarecrowBlock.BOTTOM) != bottom)
                this.addScarecrow(level, otherHalf);
        }
    }

    private void onBlockBreak(Level level, BlockPos pos, BlockState removedState){
        if(removedState.getBlock() instanceof ScarecrowBlock){
            this.removeScarecrow(level, pos);

            boolean bottom = removedState.getValue(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? pos.above() : pos.below();
            this.removeScarecrow(level, otherHalf);
        }
    }

    public boolean isScarecrowInRange(Level level, Vec3 pos, double range){
        Set<BlockPos> scarecrows = this.scarecrowsPerWorld.getOrDefault(level, Collections.emptySet());
        for(BlockPos scarecrow : scarecrows){ // TODO this is dumb, don't iterate all scarecrows
            Vec3 center = Vec3.atCenterOf(scarecrow);
            if(Math.abs(center.x - pos.x) <= range && Math.abs(center.y - pos.y) <= range && Math.abs(center.z - pos.z) <= range)
                return true;
        }

        return false;
    }

    public BlockPos getClosestScarecrow(Level level, BlockPos pos){
        Set<BlockPos> scarecrows = this.scarecrowsPerWorld.getOrDefault(level, Collections.emptySet());
        BlockPos closestPos = null;
        double closest = Double.MAX_VALUE;
        for(BlockPos scarecrow : scarecrows){
            double distance = scarecrow.distSqr(pos);
            if(distance < closest || closestPos == null){
                closestPos = scarecrow;
                closest = distance;
            }
        }
        return closestPos;
    }

    public Set<BlockPos> getScarecrows(Level level){
        return this.scarecrowsPerWorld.getOrDefault(level, Collections.emptySet());
    }
}
