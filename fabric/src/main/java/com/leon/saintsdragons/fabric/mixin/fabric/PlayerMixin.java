package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.common.item.tools.BloodTempestKatanaAbility;
import com.leon.saintsdragons.common.item.tools.DragonheartSwordItem;
import com.leon.saintsdragons.common.item.tools.DragonWeaponDamage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
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
        Player attacker = (Player) (Object) this;
        boolean hurt = target.hurt(source,
                DragonWeaponDamage.applyDirectMeleeMultiplier(attacker, target, damage));
        if (hurt && (Object) this instanceof ServerPlayer player && target instanceof LivingEntity livingTarget) {
            BloodTempestKatanaAbility.onSuccessfulKatanaHit(player, livingTarget);
        }
        return hurt;
    }

    @ModifyConstant(method = "attack", constant = @Constant(floatValue = 1.5F))
    private float saintsdragons$applyWeaponCriticalDamage(float vanillaMultiplier) {
        Player player = (Player) (Object) this;
        if (player.getMainHandItem().getItem() instanceof DragonheartSwordItem sword) {
            return vanillaMultiplier + sword.getCriticalDamageBonus();
        }
        return vanillaMultiplier;
    }
}
