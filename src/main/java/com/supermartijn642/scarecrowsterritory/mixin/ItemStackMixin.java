package com.supermartijn642.scarecrowsterritory.mixin;

import com.supermartijn642.scarecrowsterritory.SpawnerTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 25/03/2023 by SuperMartijn642
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(
        method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
            shift = At.Shift.AFTER
        )
    )
    private void useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> ci){
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        BlockState state = context.getLevel().getBlockState(pos);
        SpawnerTracker.onBlockAdded(context.getLevel(), pos, state);
    }
}
