package com.leon.saintsdragons.mixin.client;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class EntityRendererMixin {
    
    // Smooth FOV transition state
    private static double currentFOVMultiplier = 1.0;
    private static double targetFOVMultiplier = 1.0;
    private static final double FOV_TRANSITION_SPEED = 0.05; // How fast FOV changes (0.01 = very slow, 0.1 = fast)

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void modifyFOV(Camera camera, float partialTicks, boolean useFOVSetting, CallbackInfoReturnable<Double> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof LightningDragonEntity dragon) {
            // Calculate target FOV multiplier based on dragon sprint state
            if (dragon.isAccelerating()) {
                double currentSpeed;
                double maxSpeed;
                
                if (dragon.isFlying()) {
                    // Flying sprint - use flying speed attributes
                    currentSpeed = dragon.getDeltaMovement().horizontalDistance();
                    maxSpeed = dragon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FLYING_SPEED) * 40.0; // SPRINT_MAX_MULT
                } else {
                    // Ground sprint - use movement speed attributes
                    currentSpeed = dragon.getDeltaMovement().horizontalDistance();
                    maxSpeed = dragon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 0.7; // Ground sprint multiplier
                }
                
                double speedRatio = Math.min(1.0, currentSpeed / maxSpeed);
                
                // Different FOV multipliers for flying vs ground sprinting
                if (dragon.isFlying()) {
                    // Flying sprint - dramatic but cinematic FOV effect
                    targetFOVMultiplier = 1.0 + (speedRatio); // Up to 100% wider FOV at max speed
                } else {
                    // Ground sprint - more subtle FOV effect (ground running is already fast enough!)
                    targetFOVMultiplier = 1.0 + (0.15 * speedRatio); // Up to 15% wider FOV at max speed
                }
            } else {
                // Not sprinting - return to normal FOV
                targetFOVMultiplier = 1.0;
            }
            
            // Smooth interpolation between current and target FOV multiplier
            double diff = targetFOVMultiplier - currentFOVMultiplier;
            if (Math.abs(diff) > 0.001) { // Only interpolate if there's a meaningful difference
                currentFOVMultiplier += diff * FOV_TRANSITION_SPEED;
            } else {
                currentFOVMultiplier = targetFOVMultiplier; // Snap to target if very close
            }
            
            // Apply the smoothly interpolated FOV multiplier
            double baseFOV = cir.getReturnValue();
            double newFOV = baseFOV * currentFOVMultiplier;
            
            cir.setReturnValue(newFOV);
        } else {
            // Not riding dragon - reset FOV to normal
            currentFOVMultiplier = 1.0;
            targetFOVMultiplier = 1.0;
        }
    }
}
