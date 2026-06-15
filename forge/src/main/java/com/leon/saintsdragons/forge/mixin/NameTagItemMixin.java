package com.leon.saintsdragons.forge.mixin;

import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NameTagItem.class)
public abstract class NameTagItemMixin {
    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
    private void saintsdragons$refuseIvyRename(ItemStack arg, Player arg2, LivingEntity arg3, InteractionHand arg4, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(arg3 instanceof IvyTheDragonMerchant ivy) || !arg.hasCustomHoverName()) {
            return;
        }

        if (!arg2.level().isClientSide) {
            ivy.refuseRenameAttempt();
        }
        cir.setReturnValue(InteractionResult.sidedSuccess(arg2.level().isClientSide));
    }
}
