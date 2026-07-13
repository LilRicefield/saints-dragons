package com.leon.saintsdragons.forge.mixin;

import com.leon.saintsdragons.common.item.tools.BloodTempestKatanaAbility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Redirect(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean saintsdragons$chainBloodTempestHit(Entity target, DamageSource source, float damage) {
        boolean hurt = target.hurt(source, damage);
        if (hurt && (Object) this instanceof ServerPlayer player && target instanceof LivingEntity livingTarget) {
            BloodTempestKatanaAbility.onSuccessfulKatanaHit(player, livingTarget);
        }
        return hurt;
    }
}
