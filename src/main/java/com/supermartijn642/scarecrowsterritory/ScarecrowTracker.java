package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.ClientUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Created 1/13/2021 by SuperMartijn642
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ScarecrowTracker {

    private static final Map<IWorld,Set<BlockPos>> SCARECROWS_PER_WORLD = new HashMap<>();
    private static final Map<IWorld,Map<ChunkPos,Integer>> CHUNKS_TO_SPAWN_MOBS = new HashMap<>();

    public static int getScarecrowCount(World world){
        return SCARECROWS_PER_WORLD.getOrDefault(world, Collections.emptySet()).size();
    }

    @SubscribeEvent
    public static void onEntityDespawn(LivingSpawnEvent.AllowDespawn e){
        if(!STConfig.passiveMobSpawning.get() || e.getEntity().level.isClientSide)
            return;

        Entity entity = e.getEntity();
        double range = STConfig.passiveMobRange.get();
        if(isScarecrowInRange(e.getEntityLiving().level, entity.position(), range)){
            e.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent e){
        World world = e.world;
        if(world.isClientSide || !(world instanceof ServerWorld) || world.getGeneratorType() == WorldType.DEBUG_ALL_BLOCK_STATES)
            return;

        if(!world.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))
            return;

        Map<ChunkPos,Integer> chunks = CHUNKS_TO_SPAWN_MOBS.get(world);
        if(chunks != null){
            for(Map.Entry<ChunkPos,Integer> entry : chunks.entrySet()){
                if(entry.getValue() > 0 && world.getChunkSource().isEntityTickingChunk(entry.getKey())){
                    Chunk chunk = world.getChunkSource().getChunk(entry.getKey().x, entry.getKey().z, false);
                    if(chunk != null && !chunk.isEmpty() && world.getWorldBorder().isWithinBounds(entry.getKey()))
                        spawnEntitiesInChunk((ServerWorld)world, chunk);
                }
            }
        }
    }

    private static void spawnEntitiesInChunk(ServerWorld world, Chunk chunk){
        boolean spawnAnimals = world.getLevelData().getGameTime() % 400L == 0L;
        boolean spawnHostiles = world.getDifficulty() != Difficulty.PEACEFUL;
        MobSpawningUtil.spawnEntitiesInChunk(world, chunk, true, spawnHostiles, spawnAnimals);
    }

    private static void addScarecrow(IWorld world, BlockPos pos){
        SCARECROWS_PER_WORLD.putIfAbsent(world, new HashSet<>());
        SCARECROWS_PER_WORLD.computeIfPresent(world, (w, s) -> {
            s.add(pos);
            return s;
        });

        int range = (int)Math.ceil(STConfig.passiveMobRange.get());
        int minX = (pos.getX() - range) >> 4, maxX = (pos.getX() + range) >> 4;
        int minZ = (pos.getZ() - range) >> 4, maxZ = (pos.getZ() + range) >> 4;
        CHUNKS_TO_SPAWN_MOBS.putIfAbsent(world, new LinkedHashMap<>());
        CHUNKS_TO_SPAWN_MOBS.computeIfPresent(world, (w, s) -> {
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

    private static void removeScarecrow(IWorld world, BlockPos pos){
        SCARECROWS_PER_WORLD.computeIfPresent(world, (w, s) -> {
            s.remove(pos);
            return s;
        });

        int range = (int)Math.ceil(STConfig.passiveMobRange.get());
        int minX = (pos.getX() - range) >> 4, maxX = (pos.getX() + range) >> 4;
        int minZ = (pos.getZ() - range) >> 4, maxZ = (pos.getZ() + range) >> 4;
        CHUNKS_TO_SPAWN_MOBS.computeIfPresent(world, (w, s) -> {
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

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload e){
        SCARECROWS_PER_WORLD.remove(e.getWorld());
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load e){
        IChunk chunk = e.getChunk();

        for(BlockPos pos : chunk.getBlockEntitiesPos()){
            Runnable task = () -> {
                if(chunk.getBlockEntity(pos) instanceof ScarecrowTile)
                    addScarecrow(e.getWorld(), pos);
            };
            if(e.getWorld().isClientSide())
                ClientUtils.queueTask(task);
            else if(e.getWorld() instanceof World)
                ((World)e.getWorld()).getServer().tell(new TickDelayedTask(0, task));
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload e){
        IChunk chunk = e.getChunk();

        for(BlockPos pos : chunk.getBlockEntitiesPos()){
            if(chunk.getBlockEntity(pos) instanceof ScarecrowTile)
                removeScarecrow(e.getWorld(), pos);
        }
    }

    @SubscribeEvent
    public static void onBlockAdded(BlockEvent.EntityPlaceEvent e){
        if(e.getPlacedBlock().getBlock() instanceof ScarecrowBlock){
            addScarecrow(e.getWorld(), e.getPos());

            boolean bottom = e.getPlacedBlock().getValue(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? e.getPos().above() : e.getPos().below();
            BlockState state = e.getWorld().getBlockState(otherHalf);
            if(state.getBlock() instanceof ScarecrowBlock && state.getValue(ScarecrowBlock.BOTTOM) != bottom)
                addScarecrow(e.getWorld(), otherHalf);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent e){
        if(e.getState().getBlock() instanceof ScarecrowBlock){
            removeScarecrow(e.getWorld(), e.getPos());

            boolean bottom = e.getState().getValue(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? e.getPos().above() : e.getPos().below();
            BlockState state = e.getWorld().getBlockState(otherHalf);
            if(state.getBlock() instanceof ScarecrowBlock && state.getValue(ScarecrowBlock.BOTTOM) != bottom)
                removeScarecrow(e.getWorld(), otherHalf);
        }
    }

    public static boolean isScarecrowInRange(World world, Vec3d center, double range){
        double rangeSquared = range * range;
        Set<BlockPos> scarecrows = SCARECROWS_PER_WORLD.getOrDefault(world, Collections.emptySet());

        for(BlockPos scarecrow : scarecrows){
            if(center.distanceToSqr(scarecrow.getX() + 0.5, scarecrow.getY() + 0.5, scarecrow.getZ() + 0.5) < rangeSquared)
                return true;
        }

        return false;
    }
}
