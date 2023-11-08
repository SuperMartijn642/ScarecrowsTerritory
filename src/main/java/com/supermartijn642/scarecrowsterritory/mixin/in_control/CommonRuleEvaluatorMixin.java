package com.supermartijn642.scarecrowsterritory.mixin.in_control;

import com.supermartijn642.scarecrowsterritory.ScarecrowsTerritoryConfig;
import mcjty.incontrol.tools.rules.CommonRuleEvaluator;
import mcjty.incontrol.tools.rules.IEventQuery;
import net.minecraft.world.entity.Entity;
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object scarecrowGameStageCheck(Object o){
        if(!ScarecrowsTerritoryConfig.byPassGameStageCheck.get())
            return o;

        BiFunction<Object,IEventQuery,Boolean> check = (BiFunction<Object,IEventQuery,Boolean>)o;
        return (BiFunction<Object,IEventQuery,Boolean>)(event, query) -> {
            Entity entity = query.getEntity(event);
            if(entity != null && entity.getPersistentData().getBoolean("scarecrowsterritory"))
                return true;
            return check.apply(event, query);
        };
    }
}
