package com.leon.saintsdragons.forge.mixin.client;

import com.leon.saintsdragons.client.camera.BloodTempestKatanaVisuals;
import com.leon.saintsdragons.client.camera.DragonFovHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GameRenderer.class, priority = 500)
public class EntityRendererMixin {

    @Unique
    private static double saint_sDragons$currentFOVMultiplier = 1.0;
    @Unique
    private static final double FOV_TRANSITION_SPEED = 0.05;

    @Inject(method = "getFov(Lnet/minecraft/client/Camera;FZ)D", at = @At("RETURN"), cancellable = true, require = 0)
    private void modifyFOV(Camera camera, float partialTicks, boolean useFOVSetting, CallbackInfoReturnable<Double> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() != null) {
            Entity vehicle = mc.player.getVehicle();
            if (!DragonFovHelper.shouldApply(vehicle)) {
                saint_sDragons$currentFOVMultiplier = 1.0;
            } else {
                double targetFOVMultiplier = DragonFovHelper.getTargetMultiplier(vehicle);

                double diff = targetFOVMultiplier - saint_sDragons$currentFOVMultiplier;
                if (Math.abs(diff) > 0.001) {
                    saint_sDragons$currentFOVMultiplier += diff * FOV_TRANSITION_SPEED;
                } else {
                    saint_sDragons$currentFOVMultiplier = targetFOVMultiplier;
                }

                cir.setReturnValue(cir.getReturnValue() * saint_sDragons$currentFOVMultiplier);
            }
        } else {
            saint_sDragons$currentFOVMultiplier = 1.0;
        }

        cir.setReturnValue(cir.getReturnValue()
                * BloodTempestKatanaVisuals.getFovMultiplier(partialTicks));
    }
}
