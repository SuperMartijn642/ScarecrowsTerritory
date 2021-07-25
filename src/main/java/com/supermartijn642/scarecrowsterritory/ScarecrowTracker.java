package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.ClientUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
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

    private static final Map<LevelAccessor,Set<BlockPos>> SCARECROWS_PER_WORLD = new HashMap<>();
    private static final Map<LevelAccessor,Map<ChunkPos,Integer>> CHUNKS_TO_SPAWN_MOBS = new HashMap<>();

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
        Level world = e.world;
        if(!world.isClientSide || !(world instanceof ServerLevel) || world.isDebug() || world.getDifficulty() == Difficulty.PEACEFUL)
            return;

        if(!world.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))
            return;

        Map<ChunkPos,Integer> chunks = CHUNKS_TO_SPAWN_MOBS.get(world);
        for(Map.Entry<ChunkPos,Integer> entry : chunks.entrySet()){
            if(entry.getValue() > 0 && ((ServerLevel)world).getChunkSource().isPositionTicking(entry.getKey().toLong())){
                LevelChunk chunk = world.getChunkSource().getChunk(entry.getKey().x, entry.getKey().z, false);
                if(chunk != null && !chunk.isEmpty() && world.getWorldBorder().isWithinBounds(entry.getKey()))
                    spawnEntitiesInChunk((ServerLevel)world, chunk);
            }
        }
    }

    private static void spawnEntitiesInChunk(ServerLevel world, LevelChunk chunk){
        NaturalSpawner.SpawnState entityDensityManager = world.getChunkSource().getLastSpawnState();
        if(entityDensityManager != null){
            boolean spawnAnimals = world.getLevelData().getGameTime() % 400L == 0L;
            boolean spawnHostiles = world.getDifficulty() != Difficulty.PEACEFUL;
            MobSpawningUtil.spawnEntitiesInChunk(world, chunk, entityDensityManager, true, spawnHostiles, spawnAnimals);
        }
    }

    private static void addScarecrow(LevelAccessor world, BlockPos pos){
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

    private static void removeScarecrow(LevelAccessor world, BlockPos pos){
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
        ChunkAccess chunk = e.getChunk();

        for(BlockPos pos : chunk.getBlockEntitiesPos()){
            Runnable task = () -> {
                if(chunk.getBlockEntity(pos) instanceof ScarecrowTile)
                    addScarecrow(e.getWorld(), pos);
            };
            if(e.getWorld().isClientSide())
                ClientUtils.queueTask(task);
            else if(e.getWorld() instanceof Level)
                ((Level)e.getWorld()).getServer().tell(new TickTask(0, task));
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload e){
        ChunkAccess chunk = e.getChunk();

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

    public static boolean isScarecrowInRange(Level world, Vec3 center, double range){
        double rangeSquared = range * range;
        Set<BlockPos> scarecrows = SCARECROWS_PER_WORLD.getOrDefault(world, Collections.emptySet());

        for(BlockPos scarecrow : scarecrows){
            if(center.distanceToSqr(scarecrow.getX() + 0.5, scarecrow.getY() + 0.5, scarecrow.getZ() + 0.5) < rangeSquared)
                return true;
        }

        return false;
    }
}
