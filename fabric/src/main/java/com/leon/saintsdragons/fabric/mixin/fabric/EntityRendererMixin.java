package com.leon.saintsdragons.fabric.mixin.fabric;

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

@Mixin(GameRenderer.class)
public class EntityRendererMixin {
    
    // Smooth FOV transition state
    @Unique
    private static double saint_sDragons$currentFOVMultiplier = 1.0;
    @Unique
    private static final double FOV_TRANSITION_SPEED = 0.05; // How fast FOV changes (0.01 = very slow, 0.1 = fast)

    @Inject(method = "getFov(Lnet/minecraft/client/Camera;FZ)D", at = @At("RETURN"), cancellable = true)
    private void modifyFOV(Camera camera, float partialTicks, boolean useFOVSetting, CallbackInfoReturnable<Double> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() != null) {
            Entity vehicle = mc.player.getVehicle();
            if (!DragonFovHelper.shouldApply(vehicle)) {
                saint_sDragons$currentFOVMultiplier = 1.0;
                return;
            }

            double targetFOVMultiplier = DragonFovHelper.getTargetMultiplier(vehicle);
            
            // Smooth interpolation between current and target FOV multiplier
            double diff = targetFOVMultiplier - saint_sDragons$currentFOVMultiplier;
            if (Math.abs(diff) > 0.001) { // Only interpolate if there's a meaningful difference
                saint_sDragons$currentFOVMultiplier += diff * FOV_TRANSITION_SPEED;
            } else {
                saint_sDragons$currentFOVMultiplier = targetFOVMultiplier; // Snap to target if very close
            }
            
            // Apply the smoothly interpolated FOV multiplier
            double baseFOV = cir.getReturnValue();
            double newFOV = baseFOV * saint_sDragons$currentFOVMultiplier;
            
            cir.setReturnValue(newFOV);
        } else {
            saint_sDragons$currentFOVMultiplier = 1.0;
        }
    }
}
