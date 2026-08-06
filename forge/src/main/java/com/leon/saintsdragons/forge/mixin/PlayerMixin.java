package com.leon.saintsdragons.forge.mixin;

import com.leon.saintsdragons.common.item.tools.DragonMeleeHitContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void saintsdragons$beginDirectMeleeHit(Entity target, CallbackInfo callback) {
        DragonMeleeHitContext.begin((Player) (Object) this, target);
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void saintsdragons$endDirectMeleeHit(Entity target, CallbackInfo callback) {
        DragonMeleeHitContext.end((Player) (Object) this, target);
    }
}
