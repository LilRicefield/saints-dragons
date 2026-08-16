package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.client.camera.DragonFovEffects;
import com.leon.saintsdragons.client.renderer.RiderBullcrap;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, priority = 500)
public class EntityRendererMixin {
    @ModifyReturnValue(method = "getFov(Lnet/minecraft/client/Camera;FZ)D", at = @At("RETURN"), require = 0)
    private double modifyFOV(double incomingFov, Camera camera, float partialTicks, boolean useFOVSetting) {
        return DragonFovEffects.apply(incomingFov, partialTicks);
    }

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void saintsdragons$beginRiderRenderFrame(CallbackInfo ci) {
        RiderBullcrap.beginRenderFrame(Minecraft.getInstance().level);
    }

}
