package com.supermartijn642.scarecrowsterritory.mixin.in_control;

import com.supermartijn642.scarecrowsterritory.ScarecrowTracker;
import com.supermartijn642.scarecrowsterritory.ScarecrowsTerritoryConfig;
import mcjty.incontrol.tools.rules.CommonRuleEvaluator;
import mcjty.incontrol.tools.rules.IEventQuery;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 03/11/2023 by SuperMartijn642
 */
@Mixin(value = CommonRuleEvaluator.class, remap = false)
public class CommonRuleEvaluatorMixin {

    @Inject(
        method = "lambda$addGameStageCheck$106(Ljava/lang/String;Ljava/lang/Object;Lmcjty/incontrol/tools/rules/IEventQuery;)Ljava/lang/Boolean",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void scarecrowGameStageCheck(String stage, Object event, IEventQuery<Object> query, CallbackInfoReturnable<Boolean> ci){
        LevelAccessor level = query.getWorld(event);
        BlockPos pos = query.getPos(event);
        if(level != null && pos != null && ScarecrowTracker.isScarecrowInRange(level, pos.getCenter(), ScarecrowsTerritoryConfig.passiveMobRange.get()))
            ci.setReturnValue(true);
    }
}
