package com.supermartijn642.scarecrowsterritory.mixin.in_control;

import com.mojang.authlib.GameProfile;
import com.supermartijn642.scarecrowsterritory.ScarecrowBlockEntity;
import com.supermartijn642.scarecrowsterritory.ScarecrowTracker;
import com.supermartijn642.scarecrowsterritory.ScarecrowsTerritory;
import mcjty.incontrol.spawner.SpawnerConditions;
import mcjty.incontrol.spawner.SpawnerSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Created 03/11/2023 by SuperMartijn642
 */
@Mixin(value = SpawnerSystem.class, remap = false)
public class SpawnerSystemMixin {

    @Final
    @Shadow(remap = false)
    private static Random random;

    @Unique
    private static final ThreadLocal<Boolean> replacedWithScarecrow = ThreadLocal.withInitial(() -> false);
    @Unique
    private static final ThreadLocal<FakePlayer> fakePlayer = new ThreadLocal<>();

    @ModifyVariable(
        method = "getRandomPositionInBox",
        at = @At("STORE"),
        ordinal = 0,
        remap = false
    )
    private static Player replaceWithScarecrowInBox(Player player, Level level){
        return pickRandomScarecrow(player, level);
    }

    @ModifyVariable(
        method = "getRandomPositionOnGround",
        at = @At("STORE"),
        ordinal = 0,
        remap = false
    )
    private static Player replaceWithScarecrowOnGround(Player player, Level level){
        return pickRandomScarecrow(player, level);
    }

    @Unique
    private static Player pickRandomScarecrow(Player player, Level level){
        replacedWithScarecrow.set(false);
        if(!(level instanceof ServerLevel))
            return player;

        Set<BlockPos> scarecrows = ScarecrowTracker.getScarecrows(level);
        if(scarecrows.size() == 0)
            return player;

        int players = level.players().size();
        int pick = random.nextInt(players + scarecrows.size());
        if(pick < players)
            return player;

        pick -= players;
        int counter = 0;
        for(BlockPos scarecrow : scarecrows){
            if(counter == pick){
                FakePlayer scarecrowPlayer = fakePlayer.get();
                if(scarecrowPlayer == null){
                    scarecrowPlayer = new FakePlayer((ServerLevel)level, new GameProfile(UUID.randomUUID(), ""));
                    fakePlayer.set(scarecrowPlayer);
                }
                scarecrowPlayer.setServerLevel((ServerLevel)level);
                scarecrowPlayer.setPos(scarecrow.getCenter());
                replacedWithScarecrow.set(true);
                return scarecrowPlayer;
            }
            counter++;
        }

        // This *should* never be reached
        return player;
    }

    @Inject(
        method = "selectMob",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private static void selectMob(ServerLevel level, EntityType<?> mob, MobCategory category, SpawnerConditions conditions, BlockPos pos, CallbackInfoReturnable<EntityType<?>> ci){
        if(replacedWithScarecrow.get() && ScarecrowsTerritory.ENABLE_TROPHIES_INTEGRATION.get()){
            EntityType<?> type = ci.getReturnValue();
            if(type != null){
                BlockPos scarecrowPos = ScarecrowTracker.getClosestScarecrow(level, pos);
                BlockEntity entity;
                if(scarecrowPos == null || !((entity = level.getBlockEntity(scarecrowPos)) instanceof ScarecrowBlockEntity) || !((ScarecrowBlockEntity)entity).canTrophiesSpawn(type))
                    ci.setReturnValue(null);
            }
        }
    }

    @ModifyVariable(
        method = "executeRule(ILmcjty/incontrol/spawner/SpawnerRule;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/MobCategory;F)V",
        at = @At("STORE"),
        ordinal = 0
    )
    private static Entity markSpawnedEntity(Entity entity){
        if(entity != null && replacedWithScarecrow.get())
            entity.getPersistentData().putBoolean("scarecrowsterritory", true);
        return entity;
    }
}
