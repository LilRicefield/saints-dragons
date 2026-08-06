package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.common.item.DragonlordArmorSetBonus;
import com.leon.saintsdragons.common.item.tools.DragonMeleeHitContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float saintsdragons$modifyDirectMeleeDamage(float damage, DamageSource source) {
        return DragonMeleeHitContext.modifyDamage((LivingEntity) (Object) this, source, damage);
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void saintsdragons$observeDirectMeleeResult(DamageSource source, float damage,
                                                        CallbackInfoReturnable<Boolean> callback) {
        DragonMeleeHitContext.observeResult((LivingEntity) (Object) this, source, callback.getReturnValue());
    }

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
