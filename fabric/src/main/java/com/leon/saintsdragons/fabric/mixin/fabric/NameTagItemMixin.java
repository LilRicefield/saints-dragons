package com.leon.saintsdragons.fabric.mixin.fabric;

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
    private void saintsdragons$refuseIvyRename(ItemStack itemStack, Player player, LivingEntity livingEntity, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(livingEntity instanceof IvyTheDragonMerchant ivy) || !itemStack.hasCustomHoverName()) {
            return;
        }

        if (!player.level().isClientSide) {
            ivy.refuseRenameAttempt();
        }
        cir.setReturnValue(InteractionResult.sidedSuccess(player.level().isClientSide));
    }
}
