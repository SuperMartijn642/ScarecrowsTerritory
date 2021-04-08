package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.core.ClientUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 1/13/2021 by SuperMartijn642
 */
@Mod.EventBusSubscriber
public class ScarecrowTracker {

    private static final Map<World,Set<BlockPos>> SCARECROWS_PER_WORLD = new HashMap<>();
    private static final Map<World,Map<ChunkPos,Integer>> CHUNKS_TO_SPAWN_MOBS = new HashMap<>();

    public static int getScarecrowCount(World world){
        return SCARECROWS_PER_WORLD.getOrDefault(world, Collections.emptySet()).size();
    }

    public static boolean canDespawn(World world, Vec3d pos){
        if(!STConfig.passiveMobSpawning.get())
            return true;

        double range = STConfig.passiveMobRange.get();
        return !isScarecrowInRange(world, pos, range);
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent e){
        World world = e.world;
        if(!world.isRemote || !(world instanceof WorldServer) || world.getWorldType() == WorldType.DEBUG_ALL_BLOCK_STATES || world.getDifficulty() == EnumDifficulty.PEACEFUL)
            return;

        if(!CHUNKS_TO_SPAWN_MOBS.containsKey(world) || !world.getGameRules().getBoolean("doMobSpawning"))
            return;

        spawnEntitiesInChunks((WorldServer)world);
    }

    private static void spawnEntitiesInChunks(WorldServer world){
        boolean spawnAnimals = world.getWorldInfo().getWorldTime() % 400L == 0L;
        boolean spawnHostiles = world.getDifficulty() != EnumDifficulty.PEACEFUL;
        Set<Map.Entry<ChunkPos,Integer>> entries = CHUNKS_TO_SPAWN_MOBS.getOrDefault(world, Collections.emptyMap()).entrySet();
        Set<ChunkPos> chunks = entries.stream().filter(entry -> entry.getValue() > 0).map(Map.Entry::getKey).collect(Collectors.toSet());
        MobSpawningUtil.spawnEntitiesInChunks(world, chunks, true, spawnHostiles, spawnAnimals);
    }

    private static void addScarecrow(World world, BlockPos pos){
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

    private static void removeScarecrow(World world, BlockPos pos){
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
        Chunk chunk = e.getChunk();

        for(Map.Entry<BlockPos,TileEntity> entry : chunk.getTileEntityMap().entrySet()){
            Runnable task = () -> {
                if(entry.getValue() instanceof ScarecrowTile)
                    addScarecrow(e.getWorld(), entry.getKey());
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
            if(entry.getValue() instanceof ScarecrowTile)
                removeScarecrow(e.getWorld(), entry.getKey());
        }
    }

    @SubscribeEvent
    public static void onBlockAdded(BlockEvent.EntityPlaceEvent e){
        if(e.getPlacedBlock().getBlock() instanceof ScarecrowBlock){
            addScarecrow(e.getWorld(), e.getPos());

            boolean bottom = e.getPlacedBlock().getValue(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? e.getPos().up() : e.getPos().down();
            IBlockState state = e.getWorld().getBlockState(otherHalf);
            if(state.getBlock() instanceof ScarecrowBlock && state.getValue(ScarecrowBlock.BOTTOM) != bottom)
                addScarecrow(e.getWorld(), otherHalf);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent e){
        if(e.getState().getBlock() instanceof ScarecrowBlock){
            removeScarecrow(e.getWorld(), e.getPos());

            boolean bottom = e.getState().getValue(ScarecrowBlock.BOTTOM);
            BlockPos otherHalf = bottom ? e.getPos().up() : e.getPos().down();
            IBlockState state = e.getWorld().getBlockState(otherHalf);
            if(state.getBlock() instanceof ScarecrowBlock && state.getValue(ScarecrowBlock.BOTTOM) != bottom)
                removeScarecrow(e.getWorld(), otherHalf);
        }
    }

    public static boolean isScarecrowInRange(World world, Vec3d center, double range){
        double rangeSquared = range * range;
        Set<BlockPos> scarecrows = SCARECROWS_PER_WORLD.getOrDefault(world, Collections.emptySet());

        for(BlockPos scarecrow : scarecrows){
            if(center.squareDistanceTo(scarecrow.getX() + 0.5, scarecrow.getY() + 0.5, scarecrow.getZ() + 0.5) < rangeSquared)
                return true;
        }

        return false;
    }
}
