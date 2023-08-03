package com.supermartijn642.scarecrowsterritory.mixin;

import com.supermartijn642.scarecrowsterritory.ScarecrowTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 1/16/2021 by SuperMartijn642
 */
@Mixin(EntityLiving.class)
public abstract class EntityLivingMixin {

    @Inject(
        method = "despawnEntity()V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void despawnEntityHead(CallbackInfo ci){
        //noinspection DataFlowIssue
        EntityLiving entity = (EntityLiving)(Object)this;
        if(!entity.world.isRemote && !ScarecrowTracker.canDespawn(entity.world, entity.getPositionVector()))
            ci.cancel();
    }

    @Inject(
        method = "despawnEntity()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;getClosestPlayerToEntity(Lnet/minecraft/entity/Entity;D)Lnet/minecraft/entity/player/EntityPlayer;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void despawnEntityTail(CallbackInfo ci){
        //noinspection DataFlowIssue
        EntityLiving entity = (EntityLiving)(Object)this;
        if(!entity.world.isRemote && entity.getEntityData().getBoolean("spawnedByScarecrow")){
            Entity player = entity.world.getClosestPlayerToEntity(entity, -1);
            if(player == null){
                if(ScarecrowTracker.canDespawn(entity.world, entity.getPositionVector()))
                    entity.setDead();
            }
        }
    }
}
