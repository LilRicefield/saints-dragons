package com.leon.saintsdragons.forge.mixin.client;

import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
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

    @Unique
    private static double saint_sDragons$currentFOVMultiplier = 1.0;
    @Unique
    private static final double FOV_TRANSITION_SPEED = 0.05;

    @Inject(method = "getFov(Lnet/minecraft/client/Camera;FZ)D", at = @At("RETURN"), cancellable = true)
    private void modifyFOV(Camera camera, float partialTicks, boolean useFOVSetting, CallbackInfoReturnable<Double> cir) {
        Minecraft mc = Minecraft.getInstance();
        double targetFOVMultiplier = 1.0;
        if (mc.player != null && mc.player.getVehicle() != null) {
            Ignivorus ignivorusVehicle = mc.player.getVehicle() instanceof Ignivorus iv ? iv : null;
            boolean isAccelerating = false;
            boolean isFlying = false;
            double currentSpeed = 0;
            double maxSpeed = 0;

            if (mc.player.getVehicle() instanceof Raevyx raevyx) {
                isAccelerating = raevyx.isAccelerating();
                isFlying = raevyx.isFlying();

                if (isAccelerating) {
                    if (isFlying) {
                        currentSpeed = raevyx.getDeltaMovement().horizontalDistance();
                        maxSpeed = raevyx.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FLYING_SPEED) * 50.0;
                    } else {
                        currentSpeed = raevyx.getDeltaMovement().horizontalDistance();
                        maxSpeed = raevyx.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 0.7;
                    }
                }
            } else if (mc.player.getVehicle() instanceof Cindervane cindervane) {
                isAccelerating = cindervane.isAccelerating();
                isFlying = cindervane.isFlying();

                if (isAccelerating) {
                    if (isFlying) {
                        currentSpeed = cindervane.getDeltaMovement().horizontalDistance();
                        maxSpeed = cindervane.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FLYING_SPEED) * 20.0;
                    } else {
                        currentSpeed = cindervane.getDeltaMovement().horizontalDistance();
                        maxSpeed = cindervane.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 0.6;
                    }
                }
            } else if (mc.player.getVehicle() instanceof Nulljaw nulljaw) {
                isAccelerating = nulljaw.isAccelerating();
                isFlying = false;

                if (isAccelerating) {
                    currentSpeed = nulljaw.getDeltaMovement().horizontalDistance();
                    maxSpeed = nulljaw.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 1.0;
                }
            } else if (mc.player.getVehicle() instanceof Ignivorus ignivorus) {
                isAccelerating = ignivorus.isAccelerating();
                isFlying = ignivorus.isFlying();

                if (isAccelerating) {
                    if (isFlying) {
                        currentSpeed = ignivorus.getDeltaMovement().horizontalDistance();
                        maxSpeed = ignivorus.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FLYING_SPEED) * 30.5;
                    } else {
                        currentSpeed = ignivorus.getDeltaMovement().horizontalDistance();
                        maxSpeed = ignivorus.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 2.2;
                    }
                }
            } else {
                saint_sDragons$currentFOVMultiplier = 1.0;
                return;
            }

            if (ignivorusVehicle != null) {
                float zoom = ignivorusVehicle.getUltimateCameraZoom(partialTicks);
                if (zoom > 0.001F) {
                    double cinematicMultiplier = 1.0 + (zoom * 0.35);
                    targetFOVMultiplier = Math.max(targetFOVMultiplier, cinematicMultiplier);
                }
            }

            if (isAccelerating && maxSpeed > 0) {
                double speedRatio = Math.min(1.0, currentSpeed / maxSpeed);
                if (isFlying) {
                    targetFOVMultiplier = 1.0 + (speedRatio);
                } else {
                    targetFOVMultiplier = 1.0 + (0.15 * speedRatio);
                }
            }

            double diff = targetFOVMultiplier - saint_sDragons$currentFOVMultiplier;
            if (Math.abs(diff) > 0.001) {
                saint_sDragons$currentFOVMultiplier += diff * FOV_TRANSITION_SPEED;
            } else {
                saint_sDragons$currentFOVMultiplier = targetFOVMultiplier;
            }

            double baseFOV = cir.getReturnValue();
            double newFOV = baseFOV * saint_sDragons$currentFOVMultiplier;
            cir.setReturnValue(newFOV);
        } else {
            saint_sDragons$currentFOVMultiplier = 1.0;
        }
    }
}
