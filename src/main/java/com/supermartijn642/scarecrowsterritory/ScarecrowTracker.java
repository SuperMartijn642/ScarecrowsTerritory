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
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
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
    public static void onEntityDespawn(MobSpawnEvent.AllowDespawn e){
        if(!ScarecrowsTerritoryConfig.passiveMobSpawning.get() || e.getEntity().level().isClientSide)
            return;

        Entity entity = e.getEntity();
        double range = ScarecrowsTerritoryConfig.passiveMobRange.get();
        if(isScarecrowInRange(e.getEntity().level(), entity.position(), range)){
            e.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent e){
        Level level = e.level;
        if(!ScarecrowsTerritoryConfig.passiveMobSpawning.get() || level.isClientSide || !(level instanceof ServerLevel) || level.isDebug())
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

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload e){
        SCARECROWS_PER_WORLD.remove(e.getLevel());
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load e){
        ChunkAccess chunk = e.getChunk();

        for(BlockPos pos : chunk.getBlockEntitiesPos()){
            Runnable task = () -> {
                if(chunk.getBlockEntity(pos) instanceof ScarecrowBlockEntity)
                    addScarecrow(e.getLevel(), pos);
            };
            if(e.getLevel().isClientSide())
                ClientUtils.queueTask(task);
            else
                e.getLevel().getServer().tell(new TickTask(0, task));
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload e){
        ChunkAccess chunk = e.getChunk();

        for(BlockPos pos : chunk.getBlockEntitiesPos()){
            if(chunk.getBlockEntity(pos) instanceof ScarecrowBlockEntity)
                removeScarecrow(e.getLevel(), pos);
        }
    }

    @SubscribeEvent
    public static void onBlockAdded(BlockEvent.EntityPlaceEvent e){
        if(e.getPlacedBlock().getBlock() instanceof ScarecrowBlock){
            addScarecrow(e.getLevel(), e.getPos());

            boolean bottom = e.getPlacedBlock().getValue(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? e.getPos().above() : e.getPos().below();
            BlockState state = e.getLevel().getBlockState(otherHalf);
            if(state.getBlock() instanceof ScarecrowBlock && state.getValue(ScarecrowBlock.BOTTOM) != bottom)
                addScarecrow(e.getLevel(), otherHalf);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent e){
        if(e.getState().getBlock() instanceof ScarecrowBlock){
            removeScarecrow(e.getLevel(), e.getPos());

            boolean bottom = e.getState().getValue(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? e.getPos().above() : e.getPos().below();
            BlockState state = e.getLevel().getBlockState(otherHalf);
            if(state.getBlock() instanceof ScarecrowBlock && state.getValue(ScarecrowBlock.BOTTOM) != bottom)
                removeScarecrow(e.getLevel(), otherHalf);
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
