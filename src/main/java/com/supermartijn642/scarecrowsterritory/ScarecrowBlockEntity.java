package com.supermartijn642.scarecrowsterritory;

import com.gizmo.trophies.block.TrophyBlockEntity;
import com.gizmo.trophies.trophy.Trophy;
import com.supermartijn642.core.TextComponents;
import com.supermartijn642.core.block.BaseBlockEntity;
import com.supermartijn642.core.block.TickableBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.*;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class ScarecrowBlockEntity extends BaseBlockEntity implements TickableBlockEntity {

    private final ScarecrowType type;
    // Trophy integration stuff
    private final HashMap<BlockPos,EntityType<?>> trophyToEntity = ScarecrowsTerritory.ENABLE_TROPHIES_INTEGRATION.get() ? new HashMap<>() : null;
    private final HashMap<EntityType<?>,Set<BlockPos>> entityToTrophies = ScarecrowsTerritory.ENABLE_TROPHIES_INTEGRATION.get() ? new HashMap<>() : null;
    private final Map<MobCategory,List<EntityType<?>>> categoryToEntities = ScarecrowsTerritory.ENABLE_TROPHIES_INTEGRATION.get() ? new EnumMap<>(MobCategory.class) : null;
    private final BlockPos.MutableBlockPos trophyScanPos = ScarecrowsTerritory.ENABLE_TROPHIES_INTEGRATION.get() ? new BlockPos.MutableBlockPos() : null;
    private int trophyScanIndex = 0;

    public ScarecrowBlockEntity(ScarecrowType type, BlockPos pos, BlockState state){
        super(type.blockEntityType, pos, state);
        this.type = type;
    }

    public boolean rightClick(Player player, InteractionHand hand){
        ItemStack stack = player.getItemInHand(hand);
        if(!stack.isEmpty() && stack.getItem() instanceof DyeItem){
            DyeColor color = ((DyeItem)stack.getItem()).getDyeColor();
            BlockState state = this.level.getBlockState(this.worldPosition);
            if(state.getBlock() instanceof ScarecrowBlock){
                this.level.setBlockAndUpdate(this.worldPosition,
                    this.type.blocks.get(color).defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, state.getValue(HorizontalDirectionalBlock.FACING))
                        .setValue(ScarecrowBlock.BOTTOM, state.getValue(ScarecrowBlock.BOTTOM))
                        .setValue(ScarecrowBlock.WATERLOGGED, state.getValue(ScarecrowBlock.WATERLOGGED))
                );
                // other half
                BlockPos pos2 = state.getValue(ScarecrowBlock.BOTTOM) ? this.worldPosition.above() : this.worldPosition.below();
                BlockState state2 = this.level.getBlockState(pos2);
                if(state2.getBlock() instanceof ScarecrowBlock || state2.isAir() || state2.getFluidState().getType().isSame(Fluids.WATER)){
                    this.level.setBlockAndUpdate(pos2,
                        this.type.blocks.get(color).defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, state.getValue(HorizontalDirectionalBlock.FACING))
                            .setValue(ScarecrowBlock.BOTTOM, !state.getValue(ScarecrowBlock.BOTTOM))
                            .setValue(ScarecrowBlock.WATERLOGGED, state2.getFluidState().getType().isSame(Fluids.WATER))
                    );
                }
            }
            return true;
        }else if(ScarecrowsTerritory.ENABLE_TROPHIES_INTEGRATION.get() && this.level.isClientSide){
            if(this.satisfiesTrophyConditions()){
                if(this.entityToTrophies.size() == 1){
                    EntityType<?> type = this.entityToTrophies.keySet().stream().findAny().get();
                    Component name = TextComponents.fromTextComponent(type.getDescription()).color(ChatFormatting.GOLD).get();
                    player.displayClientMessage(TextComponents.translation("scarecrowsterritory.primitive_scarecrow.trophy.success.single", name).color(ChatFormatting.GRAY).get(), true);
                }else{
                    Component count = TextComponents.number(this.trophyToEntity.size()).color(ChatFormatting.GOLD).get();
                    player.displayClientMessage(TextComponents.translation("scarecrowsterritory.primitive_scarecrow.trophy.success.multiple", count).color(ChatFormatting.GRAY).get(), true);
                }
            }else{
                if(this.entityToTrophies.size() == 0)
                    player.displayClientMessage(TextComponents.translation("scarecrowsterritory.primitive_scarecrow.trophy.missing").color(ChatFormatting.RED).get(), true);
                else
                    player.displayClientMessage(TextComponents.translation("scarecrowsterritory.primitive_scarecrow.trophy.too_many").color(ChatFormatting.RED).get(), true);
            }
            return true;
        }
        return false;
    }

    @Override
    public void update(){
        if(ScarecrowsTerritoryConfig.loadSpawners.get()){
            Set<BlockPos> spawners = SpawnerTracker.getSpawnersInRange(this.level, this.worldPosition, ScarecrowsTerritoryConfig.loadSpawnerRange.get());
            for(BlockPos spawnerPos : spawners){
                BlockEntity entity = this.level.getBlockEntity(spawnerPos);
                if(entity instanceof SpawnerBlockEntity)
                    AbstractSpawnerUtil.tickAbstractSpawner(((SpawnerBlockEntity)entity).getSpawner(), this.level, spawnerPos);
            }
        }

        if(ScarecrowsTerritory.ENABLE_TROPHIES_INTEGRATION.get()){
            // Increase the scan index
            int range = ScarecrowsTerritoryConfig.trophyCheckRange.get();
            this.trophyScanIndex++;
            if(this.trophyScanIndex >= Math.pow(range * 2 + 1, 3))
                this.trophyScanIndex = 0;

            // Determine the position to scan
            BlockPos pos = this.getBlockPos();
            if(!this.getBlockState().getValue(ScarecrowBlock.BOTTOM))
                pos = pos.below();
            int x = this.trophyScanIndex % 3;
            int y = this.trophyScanIndex / 3 % 3;
            int z = this.trophyScanIndex / 9 % 3;
            this.trophyScanPos.set(pos.getX() - range + x, pos.getY() - range + y, pos.getZ() - range + z);
            if(pos.equals(this.trophyScanPos))
                return;

            // Check if the position contains a trophy
            BlockEntity entity = this.level.getBlockEntity(this.trophyScanPos);
            boolean hasEntity = false;
            if(entity instanceof TrophyBlockEntity){
                Trophy trophy = ((TrophyBlockEntity)entity).getTrophy();
                EntityType<?> entityType = trophy == null ? null : trophy.type();
                if(entityType != null){
                    // Add the trophy to the cache
                    hasEntity = true;
                    if(this.trophyToEntity.get(this.trophyScanPos) != entityType){
                        BlockPos trophyPos = this.trophyScanPos.immutable();
                        EntityType<?> previousType = this.trophyToEntity.put(trophyPos, entityType);
                        this.entityToTrophies.computeIfAbsent(entityType, t -> new HashSet<>()).add(trophyPos);
                        this.categoryToEntities.computeIfAbsent(entityType.getCategory(), c -> new ArrayList<>()).add(entityType);
                        if(previousType != null && previousType != entityType && this.entityToTrophies.containsKey(previousType)){
                            Set<BlockPos> trophies = this.entityToTrophies.get(previousType);
                            trophies.remove(trophyPos);
                            if(trophies.isEmpty()){
                                this.entityToTrophies.remove(previousType);
                                if(this.categoryToEntities.containsKey(previousType.getCategory()))
                                    this.categoryToEntities.get(previousType.getCategory()).remove(previousType);
                            }
                        }
                    }
                }
            }

            // If no trophy, remove the previous value
            if(!hasEntity){
                EntityType<?> previousType = this.trophyToEntity.remove(this.trophyScanPos);
                if(previousType != null && this.entityToTrophies.containsKey(previousType)){
                    Set<BlockPos> trophies = this.entityToTrophies.get(previousType);
                    trophies.remove(this.trophyScanPos);
                    if(trophies.isEmpty()){
                        this.entityToTrophies.remove(previousType);
                        if(this.categoryToEntities.containsKey(previousType.getCategory()))
                            this.categoryToEntities.get(previousType.getCategory()).remove(previousType);
                    }
                }
            }
        }
    }

    public List<EntityType<?>> getTrophySpawnableEntities(MobCategory category){
        return this.satisfiesTrophyConditions() ? this.categoryToEntities.getOrDefault(category, Collections.emptyList()) : Collections.emptyList();
    }

    public boolean satisfiesTrophyConditions(){
        int entities = this.entityToTrophies.size();
        return entities > 0 && entities < ScarecrowsTerritoryConfig.maxTrophies.get();
    }

    public Collection<BlockPos> getTrophyPositions(){
        return this.trophyToEntity.keySet();
    }

    @Override
    protected CompoundTag writeData(){
        return null;
    }

    @Override
    protected void readData(CompoundTag compound){
    }
}
