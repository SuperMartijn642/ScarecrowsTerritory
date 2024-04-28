package com.supermartijn642.scarecrowsterritory.mixin;

import com.supermartijn642.scarecrowsterritory.ScarecrowTracker;
import com.supermartijn642.scarecrowsterritory.ScarecrowsTerritoryConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 28/04/2024 by SuperMartijn642
 */
@Mixin(BaseSpawner.class)
public class BaseSpawnerMixin {

    @Inject(
        method = "isNearPlayer",
        at = @At("RETURN"),
        cancellable = true
    )
    private void isNearPlayer(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> ci){
        if(!ci.getReturnValue() && ScarecrowTracker.isScarecrowInRange(level, Vec3.atCenterOf(pos), ScarecrowsTerritoryConfig.loadSpawnerRange.get()))
            ci.setReturnValue(true);
    }
}
