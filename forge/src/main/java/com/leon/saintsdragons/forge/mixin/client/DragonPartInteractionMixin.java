package com.leon.saintsdragons.forge.mixin.client;

import com.leon.saintsdragons.client.input.DragonPartInteractionTargeting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class DragonPartInteractionMixin {
    @Unique
    private @Nullable HitResult saintsdragons$originalUseTarget;
    @Unique
    private @Nullable HitResult saintsdragons$useTarget;

    @Inject(method = "startUseItem", at = {@At("HEAD"), @At("RETURN")})
    private void saintsdragons$clearUseTarget(CallbackInfo callback) {
        saintsdragons$originalUseTarget = null;
        saintsdragons$useTarget = null;
    }

    @Redirect(method = "startUseItem", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;hitResult:Lnet/minecraft/world/phys/HitResult;",
            opcode = Opcodes.GETFIELD))
    private @Nullable HitResult saintsdragons$interactionTarget(Minecraft minecraft) {
        if (minecraft.hitResult != saintsdragons$originalUseTarget) {
            saintsdragons$originalUseTarget = minecraft.hitResult;
            saintsdragons$useTarget = DragonPartInteractionTargeting.pick(minecraft);
        }
        return saintsdragons$useTarget;
    }
}
