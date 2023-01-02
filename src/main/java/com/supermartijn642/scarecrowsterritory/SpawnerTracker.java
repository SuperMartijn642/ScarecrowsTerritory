package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.ClientUtils;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
@Mod.EventBusSubscriber
public class SpawnerTracker {

    private static final Map<World,Set<BlockPos>> SPAWNERS_PER_WORLD = new LinkedHashMap<>();

    @SubscribeEvent
    public static void onLevelUnload(WorldEvent.Unload e){
        SPAWNERS_PER_WORLD.remove(e.getWorld());
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load e){
        Chunk chunk = e.getChunk();

        for(Map.Entry<BlockPos,TileEntity> entry : chunk.getTileEntityMap().entrySet()){
            Runnable task = () -> {
                if(entry.getValue() instanceof TileEntityMobSpawner){
                    SPAWNERS_PER_WORLD.putIfAbsent(e.getWorld(), new HashSet<>());
                    SPAWNERS_PER_WORLD.computeIfPresent(e.getWorld(), (w, s) -> {
                        s.add(entry.getKey());
                        return s;
                    });
                }
            };
            if(e.getWorld().isRemote)
                ClientUtils.queueTask(task);
            else
                e.getWorld().getMinecraftServer().addScheduledTask(task);
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload e){
        Chunk chunk = e.getChunk();

        for(Map.Entry<BlockPos,TileEntity> entry : chunk.getTileEntityMap().entrySet()){
            if(entry.getValue() instanceof TileEntityMobSpawner){
                SPAWNERS_PER_WORLD.computeIfPresent(e.getWorld(), (w, s) -> {
                    s.remove(entry.getKey());
                    return s;
                });
            }
        }
    }

    @SubscribeEvent
    public static void onBlockAdded(BlockEvent.EntityPlaceEvent e){
        if(e.getPlacedBlock().getBlock() == Blocks.MOB_SPAWNER){
            SPAWNERS_PER_WORLD.putIfAbsent(e.getWorld(), new HashSet<>());
            SPAWNERS_PER_WORLD.computeIfPresent(e.getWorld(), (w, s) -> {
                s.add(e.getPos());
                return s;
            });
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent e){
        if(e.getState().getBlock() == Blocks.MOB_SPAWNER){
            SPAWNERS_PER_WORLD.computeIfPresent(e.getWorld(), (w, s) -> {
                s.remove(e.getPos());
                return s;
            });
        }
    }

    public static Set<BlockPos> getSpawnersInRange(World level, BlockPos center, double range){
        double rangeSquared = range * range;
        return SPAWNERS_PER_WORLD.getOrDefault(level, Collections.emptySet())
            .stream().filter(pos -> center.distanceSq(pos) <= rangeSquared).collect(Collectors.toSet());
    }
}
