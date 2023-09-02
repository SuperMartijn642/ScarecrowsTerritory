package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.ClientUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
        if(!ScarecrowsTerritoryConfig.passiveMobSpawning.get() || e.getEntity().level.isClientSide)
            return;

        LivingEntity mob = e.getEntityLiving();
        double range = Math.max(ScarecrowsTerritoryConfig.passiveMobRange.get(), ScarecrowsTerritoryConfig.loadSpawnerRange.get()) + ScarecrowsTerritoryConfig.noDespawnBuffer.get();
        if(isScarecrowInRange(mob.level, mob.position(), range))
            e.setResult(Event.Result.DENY);
        else if(mob.getPersistentData().getBoolean("spawnedByScarecrow") && mob instanceof Mob){
            Entity entity = mob.level.getNearestPlayer(mob, -1);
            if(entity == null){
                if(((Mob)mob).removeWhenFarAway(range * range))
                    e.setResult(Event.Result.ALLOW);
                else
                    mob.setNoActionTime(0);
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent e){
        Level level = e.world;
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

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload e){
        SCARECROWS_PER_WORLD.remove(e.getWorld());
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load e){
        ChunkAccess chunk = e.getChunk();

        for(BlockPos pos : chunk.getBlockEntitiesPos()){
            Runnable task = () -> {
                if(chunk.getBlockEntity(pos) instanceof ScarecrowBlockEntity)
                    addScarecrow(e.getWorld(), pos);
            };
            if(e.getWorld().isClientSide())
                ClientUtils.queueTask(task);
            else
                e.getWorld().getServer().tell(new TickTask(0, task));
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload e){
        ChunkAccess chunk = e.getChunk();

        for(BlockPos pos : chunk.getBlockEntitiesPos()){
            if(chunk.getBlockEntity(pos) instanceof ScarecrowBlockEntity)
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

    public static boolean isScarecrowInRange(Level level, Vec3 pos, double range){
        Set<BlockPos> scarecrows = SCARECROWS_PER_WORLD.getOrDefault(level, Collections.emptySet());
        for(BlockPos scarecrow : scarecrows){
            Vec3 center = Vec3.atCenterOf(scarecrow);
            if(Math.abs(center.x - pos.x) <= range && Math.abs(center.y - pos.y) <= range && Math.abs(center.z - pos.z) <= range)
                return true;
        }

        return false;
    }
}
