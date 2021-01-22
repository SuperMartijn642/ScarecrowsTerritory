package com.supermartijn642.scarecrowsterritory;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.WorldEntitySpawner;
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

    @SubscribeEvent
    public static void onEntityDespawn(LivingSpawnEvent.AllowDespawn e){
        if(!STConfig.passiveMobSpawning.get() || e.getEntity().world.isRemote)
            return;

        Entity entity = e.getEntity();
        double range = STConfig.passiveMobRange.get();
        if(isScarecrowInRange(e.getEntityLiving().world, entity.getPositionVec(), range)){
            e.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent e){
        World world = e.world;
        if(!world.isRemote || !(world instanceof ServerWorld) || world.isDebug() || world.getDifficulty() == Difficulty.PEACEFUL)
            return;

        if(!world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING))
            return;

        Map<ChunkPos,Integer> chunks = CHUNKS_TO_SPAWN_MOBS.get(world);
        for(Map.Entry<ChunkPos,Integer> entry : chunks.entrySet()){
            if(entry.getValue() > 0 && world.getChunkProvider().isChunkLoaded(entry.getKey())){
                Chunk chunk = world.getChunkProvider().getChunk(entry.getKey().x, entry.getKey().z, false);
                if(chunk != null && !chunk.isEmpty() && world.getWorldBorder().contains(entry.getKey()))
                    spawnEntitiesInChunk((ServerWorld)world, chunk);
            }
        }
    }

    private static void spawnEntitiesInChunk(ServerWorld world, Chunk chunk){
        WorldEntitySpawner.EntityDensityManager entityDensityManager = world.getChunkProvider().func_241101_k_();
        if(entityDensityManager != null){
            boolean spawnAnimals = world.getWorldInfo().getGameTime() % 400L == 0L;
            boolean spawnHostiles = world.getDifficulty() != Difficulty.PEACEFUL;
            MobSpawningUtil.spawnEntitiesInChunk(world, chunk, entityDensityManager, true, spawnHostiles, spawnAnimals);
        }
    }

    private static void addScarecrow(IWorld world, BlockPos pos){
        SCARECROWS_PER_WORLD.putIfAbsent(world, new HashSet<>());
        SCARECROWS_PER_WORLD.computeIfPresent(world, (w, s) -> {
            s.add(pos);
            return s;
        });

        int range = (int)Math.ceil(STConfig.passiveMobRange.get());
        int minX = (pos.getX() - range) >> 4, maxX = (pos.getX() + range) >> 4;
        int minY = (pos.getY() - range) >> 4, maxY = (pos.getY() + range) >> 4;
        CHUNKS_TO_SPAWN_MOBS.putIfAbsent(world, new LinkedHashMap<>());
        CHUNKS_TO_SPAWN_MOBS.computeIfPresent(world, (w, s) -> {
            for(int x = minX; x <= maxX; x++){
                for(int y = minY; y <= maxY; y++){
                    ChunkPos chunk = new ChunkPos(x, y);
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
        int minY = (pos.getY() - range) >> 4, maxY = (pos.getY() + range) >> 4;
        CHUNKS_TO_SPAWN_MOBS.computeIfPresent(world, (w, s) -> {
            for(int x = minX; x <= maxX; x++){
                for(int y = minY; y <= maxY; y++){
                    ChunkPos chunk = new ChunkPos(x, y);
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

        for(BlockPos pos : chunk.getTileEntitiesPos()){
            Runnable task = () -> {
                if(chunk.getTileEntity(pos) instanceof ScarecrowTile)
                    addScarecrow(e.getWorld(), pos);
            };
            if(e.getWorld().isRemote())
                ClientProxy.enqueueTask(task);
            else if(e.getWorld() instanceof World)
                ((World)e.getWorld()).getServer().enqueue(new TickDelayedTask(0, task));
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload e){
        IChunk chunk = e.getChunk();

        for(BlockPos pos : chunk.getTileEntitiesPos()){
            if(chunk.getTileEntity(pos) instanceof ScarecrowTile)
                removeScarecrow(e.getWorld(), pos);
        }
    }

    @SubscribeEvent
    public static void onBlockAdded(BlockEvent.EntityPlaceEvent e){
        if(e.getPlacedBlock().getBlock() instanceof ScarecrowBlock){
            addScarecrow(e.getWorld(), e.getPos());

            boolean bottom = e.getPlacedBlock().get(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? e.getPos().up() : e.getPos().down();
            BlockState state = e.getWorld().getBlockState(otherHalf);
            if(state.getBlock() instanceof ScarecrowBlock && state.get(ScarecrowBlock.BOTTOM) != bottom)
                addScarecrow(e.getWorld(), otherHalf);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent e){
        if(e.getState().getBlock() instanceof ScarecrowBlock){
            removeScarecrow(e.getWorld(), e.getPos());

            boolean bottom = e.getState().get(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? e.getPos().up() : e.getPos().down();
            BlockState state = e.getWorld().getBlockState(otherHalf);
            if(state.getBlock() instanceof ScarecrowBlock && state.get(ScarecrowBlock.BOTTOM) != bottom)
                removeScarecrow(e.getWorld(), otherHalf);
        }
    }

    public static boolean isScarecrowInRange(World world, Vector3d center, double range){
        double rangeSquared = range * range;
        Set<BlockPos> scarecrows = SCARECROWS_PER_WORLD.getOrDefault(world, Collections.emptySet());

        for(BlockPos scarecrow : scarecrows){
            if(center.squareDistanceTo(scarecrow.getX() + 0.5, scarecrow.getY() + 0.5, scarecrow.getZ() + 0.5) < rangeSquared)
                return true;
        }

        return false;
    }
}
