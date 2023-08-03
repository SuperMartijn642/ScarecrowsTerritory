package com.supermartijn642.scarecrowsterritory.mixin;

import com.supermartijn642.scarecrowsterritory.ScarecrowTracker;
import com.supermartijn642.scarecrowsterritory.ScarecrowsTerritoryConfig;
import com.supermartijn642.scarecrowsterritory.extensions.ScarecrowMobExtension;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 25/03/2023 by SuperMartijn642
 */
@Mixin(Mob.class)
public class MobMixin implements ScarecrowMobExtension {

    @Unique
    private boolean scarecrowsterritory$spawnedByScarecrow = false;

    @Inject(
        method = "checkDespawn()V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void checkDespawnHead(CallbackInfo ci){
        //noinspection DataFlowIssue
        Mob mob = (Mob)(Object)this;
        if((mob.level().getDifficulty() != Difficulty.PEACEFUL || !mob.shouldDespawnInPeaceful())
            && !mob.isPersistenceRequired() && !mob.requiresCustomPersistence()){
            if(!ScarecrowTracker.shouldEntityDespawn(mob)){
                mob.setNoActionTime(0);
                ci.cancel();
            }
        }
    }

    @Inject(
        method = "checkDespawn()V",
        at = @At("TAIL"),
        cancellable = true
    )
    private void checkDespawnTail(CallbackInfo ci){
        //noinspection DataFlowIssue
        Mob mob = (Mob)(Object)this;
        if(!((ScarecrowMobExtension)mob).scarecrowsterritory$wasSpawnedByScarecrow())
            return;
        Player entity = mob.level().getNearestPlayer(mob, -1.0);
        if(entity == null && ScarecrowTracker.shouldEntityDespawn(mob)){
            if(ScarecrowTracker.shouldEntityDespawn(mob) && mob.removeWhenFarAway(ScarecrowsTerritoryConfig.passiveMobRange.get() * ScarecrowsTerritoryConfig.passiveMobRange.get())){
                mob.discard();
                ci.cancel();
            }else
                mob.setNoActionTime(0);
        }
    }

    @Override
    public void scarecrowsterritory$setSpawnedByScarecrow(){
        this.scarecrowsterritory$spawnedByScarecrow = true;
    }

    @Override
    public boolean scarecrowsterritory$wasSpawnedByScarecrow(){
        return this.scarecrowsterritory$spawnedByScarecrow;
    }
}
