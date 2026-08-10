package com.leon.saintsdragons.fabric.mixin;

import com.leon.saintsdragons.common.registry.ModAttributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDefaultAttributesMixin {
    @Inject(method = "createAttributes", at = @At("RETURN"))
    private static void saintsdragons$addPlayerAttributes(
            CallbackInfoReturnable<AttributeSupplier.Builder> callback
    ) {
        callback.getReturnValue()
                .add(ModAttributes.DOUBLE_JUMP.get())
                .add(ModAttributes.FIRE_RESISTANCE.get())
                .add(ModAttributes.BLAST_RESISTANCE.get());
    }
}
