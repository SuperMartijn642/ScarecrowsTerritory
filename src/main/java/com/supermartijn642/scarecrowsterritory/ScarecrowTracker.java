package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.ClientUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 1/13/2021 by SuperMartijn642
 */
public class ScarecrowTracker {

    private static final ScarecrowTracker SERVER = new ScarecrowTracker(), CLIENT = new ScarecrowTracker();

    public static ScarecrowTracker get(boolean client){
        return client ? CLIENT : SERVER;
    }

    public static ScarecrowTracker get(Level level){
        return get(level.isClientSide());
    }

    public static void registerListeners(){
        ServerTickEvents.END_WORLD_TICK.register(SERVER::onWorldTick);
        ServerWorldEvents.UNLOAD.register((server, level) -> SERVER.onWorldUnload(level));
        ServerChunkEvents.CHUNK_LOAD.register(SERVER::onChunkLoad);
        ServerChunkEvents.CHUNK_UNLOAD.register(SERVER::onChunkUnload);
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> get(level).onBlockBreak(level, pos, state));
    }

    private final Map<LevelAccessor,Set<BlockPos>> scarecrowsPerWorld = new HashMap<>();
    private final Map<LevelAccessor,Map<ChunkPos,Integer>> chunksToSpawnMobs = new HashMap<>();

    public boolean shouldEntityDespawn(Mob mob){
        if(!ScarecrowsTerritoryConfig.passiveMobSpawning.get() || mob.level().isClientSide())
            return true;

        double range = Math.max(ScarecrowsTerritoryConfig.passiveMobRange.get(), ScarecrowsTerritoryConfig.loadSpawnerRange.get()) + ScarecrowsTerritoryConfig.noDespawnBuffer.get();
        return !this.isScarecrowInRange(mob.level(), mob.position(), range);
    }

    private void onWorldTick(Level level){
        if(!ScarecrowsTerritoryConfig.passiveMobSpawning.get() || level.isClientSide()
            || !(level instanceof ServerLevel) || level.isDebug())
            return;

        if(!((ServerLevel)level).getGameRules().get(GameRules.SPAWN_MOBS))
            return;

        Map<ChunkPos,Integer> chunks = this.chunksToSpawnMobs.get(level);
        if(chunks != null){
            for(Map.Entry<ChunkPos,Integer> entry : chunks.entrySet()){
                if(entry.getValue() > 0 && ((ServerLevel)level).getChunkSource().isPositionTicking(entry.getKey().toLong())){
                    LevelChunk chunk = level.getChunkSource().getChunk(entry.getKey().x, entry.getKey().z, false);
                    if(chunk != null && !chunk.isEmpty() && level.getWorldBorder().isWithinBounds(entry.getKey()))
                        this.spawnEntitiesInChunk((ServerLevel)level, chunk);
                }
            }
        }
    }

    private void spawnEntitiesInChunk(ServerLevel level, LevelChunk chunk){
        NaturalSpawner.SpawnState entityDensityManager = level.getChunkSource().getLastSpawnState();
        if(entityDensityManager != null){
            boolean spawnAnimals = level.getLevelData().getGameTime() % 400L == 0L;
            boolean spawnHostiles = level.isSpawningMonsters();
            List<MobCategory> categories = MobSpawningUtil.getFilteredSpawningCategories(entityDensityManager, true, spawnHostiles, spawnAnimals);
            MobSpawningUtil.spawnEntitiesInChunk(level, chunk, entityDensityManager, categories);
        }
    }

    private void addScarecrow(LevelAccessor level, BlockPos pos){
        this.scarecrowsPerWorld.putIfAbsent(level, new HashSet<>());
        this.scarecrowsPerWorld.computeIfPresent(level, (w, s) -> {
            s.add(pos);
            return s;
        });

        int range = (int)Math.ceil(ScarecrowsTerritoryConfig.passiveMobRange.get());
        int minX = (pos.getX() - range) >> 4, maxX = (pos.getX() + range) >> 4;
        int minZ = (pos.getZ() - range) >> 4, maxZ = (pos.getZ() + range) >> 4;
        this.chunksToSpawnMobs.putIfAbsent(level, new LinkedHashMap<>());
        this.chunksToSpawnMobs.computeIfPresent(level, (w, s) -> {
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

    private void removeScarecrow(LevelAccessor level, BlockPos pos){
        this.scarecrowsPerWorld.computeIfPresent(level, (w, s) -> {
            s.remove(pos);
            return s;
        });

        int range = (int)Math.ceil(ScarecrowsTerritoryConfig.passiveMobRange.get());
        int minX = (pos.getX() - range) >> 4, maxX = (pos.getX() + range) >> 4;
        int minZ = (pos.getZ() - range) >> 4, maxZ = (pos.getZ() + range) >> 4;
        this.chunksToSpawnMobs.computeIfPresent(level, (w, s) -> {
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

    private void onWorldUnload(LevelAccessor level){
        this.scarecrowsPerWorld.remove(level);
        this.chunksToSpawnMobs.remove(level);
    }

    void onClientLevelChange(@Nullable Level newLevel){
        this.scarecrowsPerWorld.clear();
        this.chunksToSpawnMobs.clear();
    }

    void onChunkLoad(Level level, LevelChunk chunk){
        Runnable task = () -> {
            for(BlockPos pos : chunk.getBlockEntitiesPos()){
                if(chunk.getBlockEntity(pos) instanceof ScarecrowBlockEntity)
                    this.addScarecrow(level, pos);
            }
        };
        if(level.isClientSide())
            ClientUtils.queueTask(task);
        else
            level.getServer().schedule(new TickTask(0, task));
    }

    void onChunkUnload(Level level, LevelChunk chunk){
        for(BlockPos pos : chunk.getBlockEntitiesPos()){
            if(chunk.getBlockEntity(pos) instanceof ScarecrowBlockEntity)
                this.removeScarecrow(level, pos);
        }
    }

    public void onBlockAdded(Level level, BlockPos pos, BlockState placedState){
        if(placedState.getBlock() instanceof ScarecrowBlock){
            this.addScarecrow(level, pos);

            boolean bottom = placedState.getValue(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? pos.above() : pos.below();
            BlockState state = level.getBlockState(otherHalf);
            if(state.getBlock() instanceof ScarecrowBlock && state.getValue(ScarecrowBlock.BOTTOM) != bottom)
                this.addScarecrow(level, otherHalf);
        }
    }

    private void onBlockBreak(Level level, BlockPos pos, BlockState removedState){
        if(removedState.getBlock() instanceof ScarecrowBlock){
            this.removeScarecrow(level, pos);

            boolean bottom = removedState.getValue(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? pos.above() : pos.below();
            this.removeScarecrow(level, otherHalf);
        }
    }

    public boolean isScarecrowInRange(Level level, Vec3 pos, double range){
        Set<BlockPos> scarecrows = this.scarecrowsPerWorld.getOrDefault(level, Collections.emptySet());
        for(BlockPos scarecrow : scarecrows){ // TODO this is dumb, don't iterate all scarecrows
            Vec3 center = Vec3.atCenterOf(scarecrow);
            if(Math.abs(center.x - pos.x) <= range && Math.abs(center.y - pos.y) <= range && Math.abs(center.z - pos.z) <= range)
                return true;
        }

        return false;
    }
}
