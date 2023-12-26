package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.ClientUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpawnerTracker {

    private static final Map<LevelAccessor,Set<BlockPos>> SPAWNERS_PER_WORLD = new LinkedHashMap<>();

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload e){
        SPAWNERS_PER_WORLD.remove(e.getLevel());
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load e){
        ChunkAccess chunk = e.getChunk();

        for(BlockPos pos : chunk.getBlockEntitiesPos()){
            Runnable task = () -> {
                if(chunk.getBlockEntity(pos) instanceof SpawnerBlockEntity){
                    SPAWNERS_PER_WORLD.putIfAbsent(e.getLevel(), new HashSet<>());
                    SPAWNERS_PER_WORLD.computeIfPresent(e.getLevel(), (w, s) -> {
                        s.add(pos);
                        return s;
                    });
                }
            };
            if(e.getLevel().isClientSide())
                ClientUtils.queueTask(task);
            else if(e.getLevel() instanceof Level)
                e.getLevel().getServer().tell(new TickTask(0, task));
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload e){
        ChunkAccess chunk = e.getChunk();

        for(BlockPos pos : chunk.getBlockEntitiesPos()){
            if(chunk.getBlockEntity(pos) instanceof SpawnerBlockEntity){
                SPAWNERS_PER_WORLD.computeIfPresent(e.getLevel(), (w, s) -> {
                    s.remove(pos);
                    return s;
                });
            }
        }
    }

    @SubscribeEvent
    public static void onBlockAdded(BlockEvent.EntityPlaceEvent e){
        if(e.getPlacedBlock().getBlock() == Blocks.SPAWNER){
            SPAWNERS_PER_WORLD.putIfAbsent(e.getLevel(), new HashSet<>());
            SPAWNERS_PER_WORLD.computeIfPresent(e.getLevel(), (w, s) -> {
                s.add(e.getPos());
                return s;
            });
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent e){
        if(e.getState().getBlock() == Blocks.SPAWNER){
            SPAWNERS_PER_WORLD.computeIfPresent(e.getLevel(), (w, s) -> {
                s.remove(e.getPos());
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
