package com.supermartijn642.scarecrowsterritory.mixin;

import com.supermartijn642.scarecrowsterritory.ScarecrowTracker;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 25/03/2023 by SuperMartijn642
 */
@Mixin(Mob.class)
public class MobMixin {

    @Inject(
        method = "checkDespawn()V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void checkDespawn(CallbackInfo ci){
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
}
