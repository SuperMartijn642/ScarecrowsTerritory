package com.supermartijn642.scarecrowsterritory;

import net.minecraft.block.Blocks;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpawnerTracker {

    private static final Map<IWorld,Set<BlockPos>> SPAWNERS_PER_WORLD = new LinkedHashMap<>();

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload e){
        SPAWNERS_PER_WORLD.remove(e.getWorld());
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load e){
        IChunk chunk = e.getChunk();

        for(BlockPos pos : chunk.getTileEntitiesPos()){
            if(chunk.getTileEntity(pos) instanceof MobSpawnerTileEntity){
                SPAWNERS_PER_WORLD.putIfAbsent(e.getWorld(), new HashSet<>());
                SPAWNERS_PER_WORLD.computeIfPresent(e.getWorld(), (w, s) -> {
                    s.add(pos); return s;
                });
            }
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload e){
        IChunk chunk = e.getChunk();

        for(BlockPos pos : chunk.getTileEntitiesPos()){
            if(chunk.getTileEntity(pos) instanceof MobSpawnerTileEntity){
                SPAWNERS_PER_WORLD.computeIfPresent(e.getWorld(), (w, s) -> {
                    s.remove(pos); return s;
                });
            }
        }
    }

    @SubscribeEvent
    public static void onBlockAdded(BlockEvent.EntityPlaceEvent e){
        if(e.getPlacedBlock().getBlock() == Blocks.SPAWNER){
            SPAWNERS_PER_WORLD.putIfAbsent(e.getWorld(), new HashSet<>());
            SPAWNERS_PER_WORLD.computeIfPresent(e.getWorld(), (w, s) -> {
                s.add(e.getPos()); return s;
            });
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent e){
        if(e.getState().getBlock() == Blocks.SPAWNER){
            SPAWNERS_PER_WORLD.computeIfPresent(e.getWorld(), (w, s) -> {
                s.remove(e.getPos()); return s;
            });
        }
    }

    public static Set<BlockPos> getSpawnersInRange(World world, BlockPos center, double range){
        double rangeSquared = range * range;
        return SPAWNERS_PER_WORLD.getOrDefault(world, Collections.emptySet())
            .stream().filter(pos -> center.distanceSq(pos) <= rangeSquared).collect(Collectors.toSet());
    }

}
