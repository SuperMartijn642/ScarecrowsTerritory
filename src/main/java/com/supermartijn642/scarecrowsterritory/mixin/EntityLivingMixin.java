package com.supermartijn642.scarecrowsterritory.mixin;

import com.supermartijn642.scarecrowsterritory.ScarecrowTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 1/16/2021 by SuperMartijn642
 */
@Mixin(EntityLiving.class)
public abstract class EntityLivingMixin extends Entity {

    public EntityLivingMixin(World level){
        super(level);
    }

    @Inject(at = @At("HEAD"), method = "despawnEntity()V", cancellable = true)
    public void despawnEntity(CallbackInfo ci){
        if(!this.world.isRemote && !ScarecrowTracker.canDespawn(this.world, this.getPositionVector()))
            ci.cancel();
    }
}
