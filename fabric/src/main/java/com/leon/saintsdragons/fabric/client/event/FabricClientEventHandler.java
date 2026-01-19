package com.leon.saintsdragons.fabric.client.event;

import com.leon.saintsdragons.client.sound.ignivorus.IgnivorusFireBreathSoundController;
import com.leon.saintsdragons.client.sound.raevyx.RaevyxLightningBeamSoundController;
import com.leon.saintsdragons.fabric.client.accessor.CameraAccessor;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.interfaces.ShakesScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/**
 * Fabric client event handler for camera adjustments and screen shake effects.
 * Equivalent to the Forge ClientEventHandler.
 */
public class FabricClientEventHandler {
    private static final double[] randomTremorOffsets = new double[3];

    // Raevyx takeoff camera zoom transition
    private static float raevyxCameraZoom = 15F; // Base zoom
    private static float raevyxCameraZoomTarget = 15F;

    // Raevyx camera shift smoothing (banking response)
    private static double raevyxCameraShift = 0.0;

    // Cindervane takeoff camera zoom transition
    private static float cindervaneCameraZoom = 5F; // Base zoom
    private static float cindervaneCameraZoomTarget = 15F;

    // Cindervane camera shift smoothing (banking response)
    private static double cindervaneCameraShift = 0.0;

    // Ignivorus camera zoom transition
    private static float ignivorusCameraZoom = 15F; // Base zoom
    private static float ignivorusCameraZoomTarget = 15F;

    // Ignivorus camera shift smoothing (banking response)
    private static double ignivorusCameraShift = 0.0;

    // Shared vertical camera shift for ascending/descending (both dragons)
    private static double verticalCameraShift = 0.0;
    // Camera pitch smoothing (per dragon)
    private static float raevyxCameraPitch = 0.0f;
    private static float cindervaneCameraPitch = 0.0f;
    private static float ignivorusCameraPitch = 0.0f;
    private static float nulljawCameraPitch = 0.0f;

    // Raevyx beam camera state
    private static boolean wasBeaming = false;
    private static net.minecraft.client.CameraType previousPerspective = null;
    private static float beamCameraForward = 0.0f;
    private static float beamCameraUp = 0.0f;

    /**
     * Initialize the client event handler.
     * Call this from your client mod initializer.
     */
    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            onClientTick(client);
        });
    }

    private static void onClientTick(Minecraft minecraft) {
        RaevyxLightningBeamSoundController.tick(minecraft);
        IgnivorusFireBreathSoundController.tick(minecraft);
        // Camera adjustments happen during render, not tick
        // We'll handle this in the render phase via mixin
    }

    /**
     * Called from CameraMixin to apply camera adjustments.
     * This is the Fabric equivalent of Forge's ViewportEvent.ComputeCameraAngles.
     */
    public static void onComputeCamera(Camera camera, float partialTicks) {
        Entity player = Minecraft.getInstance().getCameraEntity();
        if (player == null) return;

        // Dragon riding camera adjustments - Raevyx
        if (player.isPassenger() && player.getVehicle() instanceof Raevyx raevyx) {
            boolean isBeaming = raevyx.isBeaming();
            Minecraft mc = Minecraft.getInstance();

            // Force first person when beaming starts
            if (isBeaming && !wasBeaming) {
                previousPerspective = mc.options.getCameraType();
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                wasBeaming = true;
            }
            // Restore previous perspective when beaming ends
            else if (!isBeaming && wasBeaming) {
                if (previousPerspective != null) {
                    mc.options.setCameraType(previousPerspective);
                    previousPerspective = null;
                }
                wasBeaming = false;
                beamCameraForward = 0.0f;
                beamCameraUp = 0.0f;
            }

            // Special beam camera (first person, moved forward to snout)
            if (isBeaming) {
                // Smoothly move camera forward and up to snout position
                float targetForward = 7.5f; // ~7.5 blocks forward to near the snout
                float targetUp = -2.0f; // ~-2 blocks for better view angle
                float blendRate = 0.2f;
                beamCameraForward += (targetForward - beamCameraForward) * blendRate;
                beamCameraUp += (targetUp - beamCameraUp) * blendRate;

                // Move camera forward and up using accessor
                CameraAccessor cameraAccessor = (CameraAccessor) camera;
                cameraAccessor.saintsdragons$invokeMove(beamCameraForward, 0, 0);
                cameraAccessor.saintsdragons$invokeMove(0, -beamCameraUp, 0); // Negative Y = up in camera space
            }
            // Normal third person camera
            else if (camera.isDetached()) {
                // Determine target zoom based on flight state
                boolean isFlying = raevyx.isFlying();

                // Flying: zoom to 18F, grounded: 18F base
                raevyxCameraZoomTarget = isFlying ? 13F : 15F;

                // Smooth transition (slower blend rate for more gradual zoom)
                float blendRate = 0.05F;
                raevyxCameraZoom += (raevyxCameraZoomTarget - raevyxCameraZoom) * blendRate;

                // Calculate camera shift based on banking (only when flying)
                double targetCameraShift = 0.0;
                if (isFlying) {
                    // Get interpolated bank angle (-90 to +90 degrees)
                    float bankAngle = raevyx.getBankAngleDegrees(partialTicks);

                    // Calculate lateral shift magnitude based on bank angle and velocity
                    double velocity = raevyx.getDeltaMovement().horizontalDistance();
                    double velocityFactor = Math.min(velocity * 2.0, 1.5); // Cap at 1.5x

                    // Convert bank angle to shift
                    // Scale: at 45° bank with full velocity, shift ~5.5 blocks (more aggressive than Cindervane)
                    targetCameraShift = -(bankAngle / 45.0) * 5.5 * velocityFactor;
                }

                // Smooth the camera shift for gradual, natural movement
                double shiftBlendRate = 0.15;
                raevyxCameraShift += (targetCameraShift - raevyxCameraShift) * shiftBlendRate;

                // Calculate vertical camera shift based on ascending/descending
                double targetVerticalShift = isFlying ? 50.0 : 0.0;
                // Smooth vertical shift
                double verticalBlendRate = 0.12; // Slightly slower than lateral for smoother feel
                verticalCameraShift += (targetVerticalShift - verticalCameraShift) * verticalBlendRate;

                // Apply the smoothed zoom and lateral shift using the mixin accessor
                CameraAccessor cameraAccessor = (CameraAccessor) camera;
                double maxZoom = cameraAccessor.saintsdragons$invokeGetMaxZoom(raevyxCameraZoom);
                cameraAccessor.saintsdragons$invokeMove(-maxZoom, 0, 0);
                // Apply lateral and vertical shifts
                cameraAccessor.saintsdragons$invokeMove(0, verticalCameraShift, raevyxCameraShift);
                // Slight downward tilt for better forward visibility
                float raevyxTargetPitch = isFlying ? 6.0f : 0.0f;
                float raevyxPitchBlendRate = 0.15f;
                raevyxCameraPitch += (raevyxTargetPitch - raevyxCameraPitch) * raevyxPitchBlendRate;
                float currentYaw = cameraAccessor.saintsdragons$invokeGetYRot();
                float currentPitch = cameraAccessor.saintsdragons$invokeGetXRot();
                float clampedPitch = Mth.clamp(currentPitch + raevyxCameraPitch, -90.0f, 90.0f);
                cameraAccessor.saintsdragons$invokeSetRotation(currentYaw, clampedPitch);
            }
        } else {
            // Reset zoom and shift when not riding Raevyx
            raevyxCameraZoom = 15F;
            raevyxCameraZoomTarget = 15F;
            raevyxCameraShift = 0.0;
            verticalCameraShift = 0.0;
            raevyxCameraPitch = 0.0f;
            wasBeaming = false;
            previousPerspective = null;
            beamCameraForward = 0.0f;
            beamCameraUp = 0.0f;
        }

        // Dragon riding camera adjustments - Cindervane
        if (player.isPassenger() && player.getVehicle() instanceof Cindervane cindervane && camera.isDetached()) {
            // Determine target zoom based on flight state
            boolean isFlying = cindervane.isFlying();

            // Flying: zoom to 15F, grounded: 5F base
            cindervaneCameraZoomTarget = isFlying ? 15F : 5F;

            // Smooth transition
            float blendRate = 0.05F;
            cindervaneCameraZoom += (cindervaneCameraZoomTarget - cindervaneCameraZoom) * blendRate;

            // Calculate camera shift based on banking (only when flying)
            double targetCameraShift = 0.0;
            if (isFlying) {
                // Get interpolated bank angle (-90 to +90 degrees)
                float bankAngle = cindervane.getBankAngleDegrees(partialTicks);

                // Calculate lateral shift magnitude based on bank angle and velocity
                // More banking = more shift. Scale by velocity for dynamic feel.
                double velocity = cindervane.getDeltaMovement().horizontalDistance();
                double velocityFactor = Math.min(velocity * 2.0, 1.5); // Cap at 1.5x

                targetCameraShift = -(bankAngle / 45.0) * 5.5 * velocityFactor;
            }

            // Smooth the camera shift for gradual, natural movement
            double shiftBlendRate = 0.15; // Faster response than zoom for snappy feel
            cindervaneCameraShift += (targetCameraShift - cindervaneCameraShift) * shiftBlendRate;

            // Calculate vertical camera shift based on ascending/descending
            double targetVerticalShift = isFlying ? 50.0 : 0.0;
            // Smooth vertical shift
            double verticalBlendRate = 0.12; // Slightly slower than lateral for smoother feel
            verticalCameraShift += (targetVerticalShift - verticalCameraShift) * verticalBlendRate;

            // Apply the smoothed zoom and lateral shift using the mixin accessor
            CameraAccessor cameraAccessor = (CameraAccessor) camera;
            double maxZoom = cameraAccessor.saintsdragons$invokeGetMaxZoom(cindervaneCameraZoom);

            // Move camera: back (zoom), no vertical, lateral shift based on banking
            cameraAccessor.saintsdragons$invokeMove(-maxZoom, 0, 0);
            // Apply lateral and vertical shifts
            cameraAccessor.saintsdragons$invokeMove(0, verticalCameraShift, cindervaneCameraShift);
            // Slight downward tilt for better forward visibility
            float cindervaneTargetPitch = isFlying ? 10.0f : 0.0f;
            float cindervanePitchBlendRate = 0.15f;
            cindervaneCameraPitch += (cindervaneTargetPitch - cindervaneCameraPitch) * cindervanePitchBlendRate;
            float currentYaw = cameraAccessor.saintsdragons$invokeGetYRot();
            float currentPitch = cameraAccessor.saintsdragons$invokeGetXRot();
            float clampedPitch = Mth.clamp(currentPitch + cindervaneCameraPitch, -90.0f, 90.0f);
            cameraAccessor.saintsdragons$invokeSetRotation(currentYaw, clampedPitch);
        } else if (!(player.getVehicle() instanceof Cindervane)) {
            // Reset zoom and shift when not riding Cindervane
            cindervaneCameraZoom = 5F;
            cindervaneCameraZoomTarget = 5F;
            cindervaneCameraShift = 0.0;
            verticalCameraShift = 0.0;
            cindervaneCameraPitch = 0.0f;
        }

        // Dragon riding camera adjustments - Ignivorus
        if (player.isPassenger() && player.getVehicle() instanceof Ignivorus ignivorus && camera.isDetached()) {
            // Determine target zoom based on flight state
            boolean isFlying = ignivorus.isFlying();
            boolean isPhase2 = ignivorus.isPhase2Active();
            boolean isBaby = ignivorus.isBaby();

            // Phase 2 only affects grounded camera zoom
            if (isBaby) {
                ignivorusCameraZoomTarget = 9F;
            } else if (isFlying) {
                ignivorusCameraZoomTarget = 30F;
            } else if (isPhase2) {
                ignivorusCameraZoomTarget = 25F; // Phase 2 grounded = zoom out more
            } else {
                ignivorusCameraZoomTarget = 15F; // Normal grounded
            }

            // Smooth transition
            float blendRate = 0.05F;
            ignivorusCameraZoom += (ignivorusCameraZoomTarget - ignivorusCameraZoom) * blendRate;

            // Calculate camera shift based on banking (only when flying)
            double targetCameraShift = 0.0;
            if (isFlying) {
                // Get interpolated bank angle (-90 to +90 degrees)
                float bankAngle = ignivorus.getBankAngleDegrees(partialTicks);

                // Calculate lateral shift magnitude based on bank angle and velocity
                double velocity = ignivorus.getDeltaMovement().horizontalDistance();
                double velocityFactor = Math.min(velocity * 2.0, 1.5); // Cap at 1.5x

                // Convert bank angle to shift
                // Scale: at 45° bank with full velocity, shift ~4.5 blocks (between Cindervane and Raevyx)
                targetCameraShift = -(bankAngle / 45.0) * 6.5 * velocityFactor;
            }

            // Smooth the camera shift for gradual, natural movement
            double shiftBlendRate = 0.15;
            ignivorusCameraShift += (targetCameraShift - ignivorusCameraShift) * shiftBlendRate;

            // Calculate vertical camera shift based on ascending/descending
            double targetVerticalShift = isFlying ?50.0 : 0.0;
            // Smooth vertical shift for Ignivorus
            double verticalBlendRate = 0.12;
            verticalCameraShift += (targetVerticalShift - verticalCameraShift) * verticalBlendRate;

            // Apply the smoothed zoom using the mixin accessor
            CameraAccessor cameraAccessor = (CameraAccessor) camera;
            double maxZoom = cameraAccessor.saintsdragons$invokeGetMaxZoom(ignivorusCameraZoom);
            cameraAccessor.saintsdragons$invokeMove(-maxZoom, 0, 0);
            // Apply lateral and vertical shifts
            cameraAccessor.saintsdragons$invokeMove(0, verticalCameraShift, ignivorusCameraShift);
            // Slight downward tilt for better forward visibility
            float ignivorusTargetPitch = isFlying ? 10.0f : 0.0f;
            float ignivorusPitchBlendRate = 0.15f;
            ignivorusCameraPitch += (ignivorusTargetPitch - ignivorusCameraPitch) * ignivorusPitchBlendRate;
            float currentYaw = cameraAccessor.saintsdragons$invokeGetYRot();
            float currentPitch = cameraAccessor.saintsdragons$invokeGetXRot();
            float clampedPitch = Mth.clamp(currentPitch + ignivorusCameraPitch, -90.0f, 90.0f);
            cameraAccessor.saintsdragons$invokeSetRotation(currentYaw, clampedPitch);
        } else if (!(player.getVehicle() instanceof Ignivorus)) {
            // Reset zoom and shift when not riding Ignivorus
            ignivorusCameraZoom = 15F;
            ignivorusCameraZoomTarget = 15F;
            ignivorusCameraShift = 0.0;
            verticalCameraShift = 0.0;
            ignivorusCameraPitch = 0.0f;
        }

        // Nulljaw camera zoom
        if (player.isPassenger() && player.getVehicle() instanceof Nulljaw nulljaw && camera.isDetached()) {
            CameraAccessor cameraAccessor = (CameraAccessor) camera;
            boolean isSwimming = nulljaw.isInWaterOrBubble();
            if (isSwimming) {
                float blendRate = 0.05F;
                raevyxCameraZoomTarget = 18F;
                raevyxCameraZoom += (raevyxCameraZoomTarget - raevyxCameraZoom) * blendRate;

                double targetCameraShift = 0.0;
                float bankAngle = nulljaw.getSwimRollAngleDegrees(partialTicks);
                double velocity = nulljaw.getDeltaMovement().horizontalDistance();
                double velocityFactor = Math.min(velocity * 2.0, 1.5);
                targetCameraShift = -(bankAngle / 45.0) * 5.5 * velocityFactor;

                double shiftBlendRate = 0.15;
                raevyxCameraShift += (targetCameraShift - raevyxCameraShift) * shiftBlendRate;

                double targetVerticalShift = 50.0;
                double verticalBlendRate = 0.12;
                verticalCameraShift += (targetVerticalShift - verticalCameraShift) * verticalBlendRate;

                double maxZoom = cameraAccessor.saintsdragons$invokeGetMaxZoom(raevyxCameraZoom);
                cameraAccessor.saintsdragons$invokeMove(-maxZoom, 0, 0);
                cameraAccessor.saintsdragons$invokeMove(0, verticalCameraShift, raevyxCameraShift);

                float nulljawTargetPitch = 6.0f;
                float pitchBlendRate = 0.15f;
                nulljawCameraPitch += (nulljawTargetPitch - nulljawCameraPitch) * pitchBlendRate;
                float currentYaw = cameraAccessor.saintsdragons$invokeGetYRot();
                float currentPitch = cameraAccessor.saintsdragons$invokeGetXRot();
                float clampedPitch = Mth.clamp(currentPitch + nulljawCameraPitch, -90.0f, 90.0f);
                cameraAccessor.saintsdragons$invokeSetRotation(currentYaw, clampedPitch);
            } else {
                double maxZoom = cameraAccessor.saintsdragons$invokeGetMaxZoom(15F);
                cameraAccessor.saintsdragons$invokeMove(-maxZoom, 0, 0);
                raevyxCameraShift = 0.0;
                verticalCameraShift = 0.0;
                nulljawCameraPitch = 0.0f;
            }
        }

        // Screen shake detection and application
        applyScreenShake(camera, player, partialTicks);
    }

    private static void applyScreenShake(Camera camera, Entity player, float partialTicks) {
        double shakeDistanceScale = 64.0;
        double distance = Double.MAX_VALUE;
        float tremorAmount = 0.0F; // Reset tremor amount each frame

        AABB aabb = player.getBoundingBox().inflate(shakeDistanceScale);
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        for (Mob screenShaker : level.getEntitiesOfClass(Mob.class, aabb, (mob -> mob instanceof ShakesScreen))) {
            ShakesScreen shakesScreen = (ShakesScreen) screenShaker;
            if (shakesScreen.canFeelShake(player) && screenShaker.distanceTo(player) < distance) {
                distance = screenShaker.distanceTo(player);
                float shakeAmount = shakesScreen.getScreenShakeAmount(partialTicks);
                tremorAmount = Math.min((1F - (float) Math.min(1, distance / shakesScreen.getShakeDistance())) * Math.max(shakeAmount, 0F), 2.0F);
            }
        }

        if (tremorAmount > 0) {
            // Generate random offsets for camera movement
            double intensity = tremorAmount * Minecraft.getInstance().options.screenEffectScale().get();

            CameraAccessor cameraAccessor = (CameraAccessor) camera;
            cameraAccessor.saintsdragons$invokeMove(
                randomTremorOffsets[0] * 0.2F * intensity,
                randomTremorOffsets[1] * 0.2F * intensity,
                randomTremorOffsets[2] * 0.5F * intensity
            );

            // Update random offsets for next frame
            randomTremorOffsets[0] = (Math.random() - 0.5) * 2.0;
            randomTremorOffsets[1] = (Math.random() - 0.5) * 2.0;
            randomTremorOffsets[2] = (Math.random() - 0.5) * 2.0;
        }
    }
}
