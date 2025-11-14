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
    private static float raevyxCameraZoom = 10F; // Base zoom
    private static float raevyxCameraZoomTarget = 10F;

    // Raevyx camera shift smoothing (banking response)
    private static double raevyxCameraShift = 0.0;

    // Cindervane takeoff camera zoom transition
    private static float cindervaneCameraZoom = 15F; // Base zoom
    private static float cindervaneCameraZoomTarget = 15F;

    // Cindervane camera shift smoothing (banking response)
    private static double cindervaneCameraShift = 0.0;

    // Ignivorus camera zoom transition
    private static float ignivorusCameraZoom = 15F; // Base zoom
    private static float ignivorusCameraZoomTarget = 15F;

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
        if (player.isPassenger() && player.getVehicle() instanceof Raevyx raevyx && camera.isDetached()) {
            // Determine target zoom based on flight state
            boolean isFlying = raevyx.isFlying();

            // Flying: zoom to 18F, grounded: 10F base
            raevyxCameraZoomTarget = isFlying ? 18F : 10F;

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

            // Apply the smoothed zoom and lateral shift using the mixin accessor
            CameraAccessor cameraAccessor = (CameraAccessor) camera;
            double maxZoom = cameraAccessor.saintsdragons$invokeGetMaxZoom(raevyxCameraZoom);
            cameraAccessor.saintsdragons$invokeMove(-maxZoom, 0, 0);
            // Apply lateral shift
            cameraAccessor.saintsdragons$invokeMove(0, 0, raevyxCameraShift);
        } else {
            // Reset zoom and shift when not riding Raevyx
            raevyxCameraZoom = 10F;
            raevyxCameraZoomTarget = 10F;
            raevyxCameraShift = 0.0;
        }

        // Dragon riding camera adjustments - Cindervane
        if (player.isPassenger() && player.getVehicle() instanceof Cindervane cindervane && camera.isDetached()) {
            // Determine target zoom based on flight state
            boolean isFlying = cindervane.isFlying();

            // Flying: zoom to 30F, grounded: 15F base
            cindervaneCameraZoomTarget = isFlying ? 30F : 15F;

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

                // Convert bank angle to shift: positive bank (right) = shift right
                // Scale: at 45° bank with full velocity, shift ~3.5 blocks
                // Negative sign to match banking direction properly
                targetCameraShift = -(bankAngle / 45.0) * 3.5 * velocityFactor;
            }

            // Smooth the camera shift for gradual, natural movement
            double shiftBlendRate = 0.15; // Faster response than zoom for snappy feel
            cindervaneCameraShift += (targetCameraShift - cindervaneCameraShift) * shiftBlendRate;

            // Apply the smoothed zoom and lateral shift using the mixin accessor
            CameraAccessor cameraAccessor = (CameraAccessor) camera;
            double maxZoom = cameraAccessor.saintsdragons$invokeGetMaxZoom(cindervaneCameraZoom);

            // Move camera: back (zoom), no vertical, lateral shift based on banking
            cameraAccessor.saintsdragons$invokeMove(-maxZoom, 0, 0);
            // Apply lateral shift (strafe direction perpendicular to view)
            cameraAccessor.saintsdragons$invokeMove(0, 0, cindervaneCameraShift);
        } else if (!(player.getVehicle() instanceof Cindervane)) {
            // Reset zoom and shift when not riding Cindervane
            cindervaneCameraZoom = 15F;
            cindervaneCameraZoomTarget = 15F;
            cindervaneCameraShift = 0.0;
        }

        // Dragon riding camera adjustments - Ignivorus
        if (player.isPassenger() && player.getVehicle() instanceof Ignivorus ignivorus && camera.isDetached()) {
            // Determine target zoom based on flight state
            boolean isFlying = ignivorus.isFlying();


            ignivorusCameraZoomTarget = isFlying ? 30F : 15F;

            // Smooth transition
            float blendRate = 0.05F;
            ignivorusCameraZoom += (ignivorusCameraZoomTarget - ignivorusCameraZoom) * blendRate;

            // Apply the smoothed zoom using the mixin accessor
            CameraAccessor cameraAccessor = (CameraAccessor) camera;
            double maxZoom = cameraAccessor.saintsdragons$invokeGetMaxZoom(ignivorusCameraZoom);
            cameraAccessor.saintsdragons$invokeMove(-maxZoom, 0, 0);
        } else if (!(player.getVehicle() instanceof Ignivorus)) {
            // Reset zoom when not riding Ignivorus
            ignivorusCameraZoom = 15F;
            ignivorusCameraZoomTarget = 15F;
        }

        // Nulljaw camera zoom
        if (player.isPassenger() && player.getVehicle() instanceof Nulljaw && camera.isDetached()) {
            CameraAccessor cameraAccessor = (CameraAccessor) camera;
            double maxZoom = cameraAccessor.saintsdragons$invokeGetMaxZoom(15F);
            cameraAccessor.saintsdragons$invokeMove(-maxZoom, 0, 0);
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
