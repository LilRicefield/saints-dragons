package com.leon.saintsdragons.mixin.client;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
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

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void modifyFOV(Camera camera, float partialTicks, boolean useFOVSetting, CallbackInfoReturnable<Double> cir) {
        Minecraft mc = Minecraft.getInstance();
        double targetFOVMultiplier = 1.0;
        if (mc.player != null && (mc.player.getVehicle() instanceof Raevyx raevyx || mc.player.getVehicle() instanceof AmphithereEntity amphithere || mc.player.getVehicle() instanceof Nulljaw nulljaw)) {
            boolean isAccelerating = false;
            boolean isFlying = false;
            double currentSpeed = 0;
            double maxSpeed = 0;
            
            if (mc.player.getVehicle() instanceof Raevyx raevyx) {
                isAccelerating = raevyx.isAccelerating();
                isFlying = raevyx.isFlying();
                
                if (isAccelerating) {
                    if (isFlying) {
                        // Flying sprint - use flying speed attributes
                        currentSpeed = raevyx.getDeltaMovement().horizontalDistance();
                        maxSpeed = raevyx.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FLYING_SPEED) * 50.0; // SPRINT_MAX_MULT
                    } else {
                        // Ground sprint - use movement speed attributes
                        currentSpeed = raevyx.getDeltaMovement().horizontalDistance();
                        maxSpeed = raevyx.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 0.7; // Ground sprint multiplier
                    }
                }
            } else if (mc.player.getVehicle() instanceof AmphithereEntity amphithereDragon) {
                isAccelerating = amphithereDragon.isAccelerating();
                isFlying = amphithereDragon.isFlying();
                
                if (isAccelerating) {
                    if (isFlying) {
                        // Flying sprint - use flying speed attributes
                        currentSpeed = amphithereDragon.getDeltaMovement().horizontalDistance();
                        maxSpeed = amphithereDragon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FLYING_SPEED) * 20.0; // SPRINT_MAX_MULT
                    } else {
                        // Ground sprint - use movement speed attributes
                        currentSpeed = amphithereDragon.getDeltaMovement().horizontalDistance();
                        maxSpeed = amphithereDragon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 0.6; // Ground sprint multiplier
                    }
                }
            } else if (mc.player.getVehicle() instanceof Nulljaw nulljaw) {
                isAccelerating = nulljaw.isAccelerating();
                isFlying = false; // Rift Drake doesn't fly
                
                if (isAccelerating) {
                    // Ground sprint - use movement speed attributes
                    currentSpeed = nulljaw.getDeltaMovement().horizontalDistance();
                    maxSpeed = nulljaw.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 1.0; // Ground sprint multiplier
                }
            }
            
            if (isAccelerating && maxSpeed > 0) {
                double speedRatio = Math.min(1.0, currentSpeed / maxSpeed);
                
                // Different FOV multipliers for flying vs ground sprinting
                if (isFlying) {
                    // Flying sprint - dramatic but cinematic FOV effect
                    targetFOVMultiplier = 1.0 + (speedRatio); // Up to 100% wider FOV at max speed
                } else {
                    // Ground sprint - more subtle FOV effect (ground running is already fast enough!)
                    targetFOVMultiplier = 1.0 + (0.15 * speedRatio); // Up to 15% wider FOV at max speed
                }
            }
            
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