package com.leon.saintsdragons.forge.mixin;

import com.leon.saintsdragons.common.item.DragonlordArmorSetBonus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
    private void saintsdragons$keepDragonlordFlightActive(CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof ServerPlayer player
                && DragonlordArmorSetBonus.isFlightActive(player)
                && player.isFallFlying()
                && !player.onGround()
                && !player.isPassenger()
                && !player.hasEffect(MobEffects.LEVITATION)) {
            callback.cancel();
        }
    }
}
