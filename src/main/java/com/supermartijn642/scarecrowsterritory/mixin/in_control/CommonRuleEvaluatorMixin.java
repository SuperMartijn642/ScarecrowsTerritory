package com.supermartijn642.scarecrowsterritory.mixin.in_control;

import com.supermartijn642.scarecrowsterritory.ScarecrowTracker;
import com.supermartijn642.scarecrowsterritory.ScarecrowsTerritoryConfig;
import mcjty.incontrol.tools.rules.CommonRuleEvaluator;
import mcjty.incontrol.tools.rules.IEventQuery;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.BiFunction;

/**
 * Created 03/11/2023 by SuperMartijn642
 */
@Mixin(value = CommonRuleEvaluator.class, remap = false)
public class CommonRuleEvaluatorMixin {

    @ModifyArg(
        method = "addGameStageCheck",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z"
        ),
        index = 0,
        remap = false
    )
    private Object scarecrowGameStageCheck(Object o){
        //noinspection unchecked,rawtypes
        BiFunction<Object,IEventQuery,Boolean> check = (BiFunction<Object,IEventQuery,Boolean>)o;
        return (BiFunction<Object,IEventQuery,Boolean>)(event, query) -> {
            LevelAccessor level = query.getWorld(event);
            BlockPos pos = query.getPos(event);
            if(level != null && pos != null && ScarecrowTracker.isScarecrowInRange(level, pos.getCenter(), ScarecrowsTerritoryConfig.passiveMobRange.get()))
                return true;
            return check.apply(event, query);
        };
    }
}
