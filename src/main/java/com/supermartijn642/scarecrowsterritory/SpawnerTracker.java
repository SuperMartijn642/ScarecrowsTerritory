package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.ClientUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class SpawnerTracker {

    private static final Map<LevelAccessor,Set<BlockPos>> SPAWNERS_PER_WORLD = new LinkedHashMap<>();

    public static void registerListeners(){
        ServerWorldEvents.UNLOAD.register((server, level) -> onLevelUnload(level));
        ServerChunkEvents.CHUNK_LOAD.register(SpawnerTracker::onChunkLoad);
        ServerChunkEvents.CHUNK_UNLOAD.register(SpawnerTracker::onChunkUnload);
        ClientChunkEvents.CHUNK_LOAD.register(SpawnerTracker::onChunkLoad);
        ClientChunkEvents.CHUNK_UNLOAD.register(SpawnerTracker::onChunkUnload);
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> onBlockBreak(level, pos, state));
    }

    public static void onLevelUnload(Level level){
        SPAWNERS_PER_WORLD.remove(level);
    }

    public static void onChunkLoad(Level level, LevelChunk chunk){
        for(BlockPos pos : chunk.getBlockEntitiesPos()){
            Runnable task = () -> {
                if(chunk.getBlockEntity(pos) instanceof SpawnerBlockEntity){
                    SPAWNERS_PER_WORLD.putIfAbsent(level, new HashSet<>());
                    SPAWNERS_PER_WORLD.computeIfPresent(level, (w, s) -> {
                        s.add(pos);
                        return s;
                    });
                }
            };
            if(level.isClientSide())
                ClientUtils.queueTask(task);
            else
                level.getServer().tell(new TickTask(0, task));
        }
    }

    public static void onChunkUnload(Level level, LevelChunk chunk){
        for(BlockPos pos : chunk.getBlockEntitiesPos()){
            if(chunk.getBlockEntity(pos) instanceof SpawnerBlockEntity){
                SPAWNERS_PER_WORLD.computeIfPresent(level, (w, s) -> {
                    s.remove(pos);
                    return s;
                });
            }
        }
    }

    public static void onBlockAdded(Level level, BlockPos pos, BlockState state){
        if(state.getBlock() == Blocks.SPAWNER){
            SPAWNERS_PER_WORLD.putIfAbsent(level, new HashSet<>());
            SPAWNERS_PER_WORLD.computeIfPresent(level, (w, s) -> {
                s.add(pos);
                return s;
            });
        }
    }

    public static void onBlockBreak(Level level, BlockPos pos, BlockState state){
        if(state.getBlock() == Blocks.SPAWNER){
            SPAWNERS_PER_WORLD.computeIfPresent(level, (w, s) -> {
                s.remove(pos);
                return s;
            });
        }
    }

    public static Set<BlockPos> getSpawnersInRange(Level level, BlockPos center, double range){
        double rangeSquared = range * range;
        return SPAWNERS_PER_WORLD.getOrDefault(level, Collections.emptySet())
            .stream().filter(pos -> center.distSqr(pos) <= rangeSquared).collect(Collectors.toSet());
    }
}
