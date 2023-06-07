package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.ClientUtils;
import com.supermartijn642.core.CommonUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Created 1/13/2021 by SuperMartijn642
 */
public class ScarecrowTracker {

    private static final Map<LevelAccessor,Set<BlockPos>> SCARECROWS_PER_WORLD = new HashMap<>();
    private static final Map<LevelAccessor,Map<ChunkPos,Integer>> CHUNKS_TO_SPAWN_MOBS = new HashMap<>();

    public static void registerListeners(){
        ServerTickEvents.END_WORLD_TICK.register(ScarecrowTracker::onWorldTick);
        ServerWorldEvents.UNLOAD.register((server, level) -> onWorldUnload(level));
        ServerChunkEvents.CHUNK_LOAD.register(ScarecrowTracker::onChunkLoad);
        ServerChunkEvents.CHUNK_UNLOAD.register(ScarecrowTracker::onChunkUnload);
        if(CommonUtils.getEnvironmentSide().isClient()){
            ClientChunkEvents.CHUNK_LOAD.register(ScarecrowTracker::onChunkLoad);
            ClientChunkEvents.CHUNK_UNLOAD.register(ScarecrowTracker::onChunkUnload);
        }
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> onBlockBreak(level, pos, state));
    }

    public static boolean shouldEntityDespawn(Mob mob){
        if(!ScarecrowsTerritoryConfig.passiveMobSpawning.get() || mob.level().isClientSide)
            return true;

        double range = ScarecrowsTerritoryConfig.passiveMobRange.get();
        return !isScarecrowInRange(mob.level(), mob.position(), range);
    }

    private static void onWorldTick(Level level){
        if(!ScarecrowsTerritoryConfig.passiveMobSpawning.get() || level.isClientSide
            || !(level instanceof ServerLevel) || level.isDebug())
            return;

        if(!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))
            return;

        Map<ChunkPos,Integer> chunks = CHUNKS_TO_SPAWN_MOBS.get(level);
        if(chunks != null){
            for(Map.Entry<ChunkPos,Integer> entry : chunks.entrySet()){
                if(entry.getValue() > 0 && ((ServerLevel)level).getChunkSource().isPositionTicking(entry.getKey().toLong())){
                    LevelChunk chunk = level.getChunkSource().getChunk(entry.getKey().x, entry.getKey().z, false);
                    if(chunk != null && !chunk.isEmpty() && level.getWorldBorder().isWithinBounds(entry.getKey()))
                        spawnEntitiesInChunk((ServerLevel)level, chunk);
                }
            }
        }
    }

    private static void spawnEntitiesInChunk(ServerLevel level, LevelChunk chunk){
        NaturalSpawner.SpawnState entityDensityManager = level.getChunkSource().getLastSpawnState();
        if(entityDensityManager != null){
            boolean spawnAnimals = level.getLevelData().getGameTime() % 400L == 0L;
            boolean spawnHostiles = level.getDifficulty() != Difficulty.PEACEFUL;
            MobSpawningUtil.spawnEntitiesInChunk(level, chunk, entityDensityManager, true, spawnHostiles, spawnAnimals);
        }
    }

    private static void addScarecrow(LevelAccessor level, BlockPos pos){
        SCARECROWS_PER_WORLD.putIfAbsent(level, new HashSet<>());
        SCARECROWS_PER_WORLD.computeIfPresent(level, (w, s) -> {
            s.add(pos);
            return s;
        });

        int range = (int)Math.ceil(ScarecrowsTerritoryConfig.passiveMobRange.get());
        int minX = (pos.getX() - range) >> 4, maxX = (pos.getX() + range) >> 4;
        int minZ = (pos.getZ() - range) >> 4, maxZ = (pos.getZ() + range) >> 4;
        CHUNKS_TO_SPAWN_MOBS.putIfAbsent(level, new LinkedHashMap<>());
        CHUNKS_TO_SPAWN_MOBS.computeIfPresent(level, (w, s) -> {
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

    private static void removeScarecrow(LevelAccessor level, BlockPos pos){
        SCARECROWS_PER_WORLD.computeIfPresent(level, (w, s) -> {
            s.remove(pos);
            return s;
        });

        int range = (int)Math.ceil(ScarecrowsTerritoryConfig.passiveMobRange.get());
        int minX = (pos.getX() - range) >> 4, maxX = (pos.getX() + range) >> 4;
        int minZ = (pos.getZ() - range) >> 4, maxZ = (pos.getZ() + range) >> 4;
        CHUNKS_TO_SPAWN_MOBS.computeIfPresent(level, (w, s) -> {
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

    private static void onWorldUnload(Level level){
        SCARECROWS_PER_WORLD.remove(level);
    }

    private static void onChunkLoad(Level level, LevelChunk chunk){
        for(BlockPos pos : chunk.getBlockEntitiesPos()){
            Runnable task = () -> {
                if(chunk.getBlockEntity(pos) instanceof ScarecrowBlockEntity)
                    addScarecrow(level, pos);
            };
            if(level.isClientSide())
                ClientUtils.queueTask(task);
            else
                level.getServer().tell(new TickTask(0, task));
        }
    }

    private static void onChunkUnload(Level level, LevelChunk chunk){
        for(BlockPos pos : chunk.getBlockEntitiesPos()){
            if(chunk.getBlockEntity(pos) instanceof ScarecrowBlockEntity)
                removeScarecrow(level, pos);
        }
    }

    public static void onBlockAdded(Level level, BlockPos pos, BlockState placedState){
        if(placedState.getBlock() instanceof ScarecrowBlock){
            addScarecrow(level, pos);

            boolean bottom = placedState.getValue(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? pos.above() : pos.below();
            BlockState state = level.getBlockState(otherHalf);
            if(state.getBlock() instanceof ScarecrowBlock && state.getValue(ScarecrowBlock.BOTTOM) != bottom)
                addScarecrow(level, otherHalf);
        }
    }

    private static void onBlockBreak(Level level, BlockPos pos, BlockState removedState){
        if(removedState.getBlock() instanceof ScarecrowBlock){
            removeScarecrow(level, pos);

            boolean bottom = removedState.getValue(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? pos.above() : pos.below();
            removeScarecrow(level, otherHalf);
        }
    }

    public static boolean isScarecrowInRange(Level level, Vec3 center, double range){
        double rangeSquared = range * range;
        Set<BlockPos> scarecrows = SCARECROWS_PER_WORLD.getOrDefault(level, Collections.emptySet());

        for(BlockPos scarecrow : scarecrows){
            if(center.distanceToSqr(scarecrow.getX() + 0.5, scarecrow.getY() + 0.5, scarecrow.getZ() + 0.5) < rangeSquared)
                return true;
        }

        return false;
    }
}
