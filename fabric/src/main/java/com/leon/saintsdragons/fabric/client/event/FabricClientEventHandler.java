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

    // Cindervane takeoff camera zoom transition
    private static float cindervaneCameraZoom = 15F; // Base zoom
    private static float cindervaneCameraZoomTarget = 15F;

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

            // Apply the smoothed zoom using the mixin accessor
            CameraAccessor cameraAccessor = (CameraAccessor) camera;
            double maxZoom = cameraAccessor.saintsdragons$invokeGetMaxZoom(raevyxCameraZoom);
            cameraAccessor.saintsdragons$invokeMove(-maxZoom, 0, 0);
        } else {
            // Reset zoom when not riding Raevyx
            raevyxCameraZoom = 10F;
            raevyxCameraZoomTarget = 10F;
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

            // Apply the smoothed zoom using the mixin accessor
            CameraAccessor cameraAccessor = (CameraAccessor) camera;
            double maxZoom = cameraAccessor.saintsdragons$invokeGetMaxZoom(cindervaneCameraZoom);
            cameraAccessor.saintsdragons$invokeMove(-maxZoom, 0, 0);
        } else if (!(player.getVehicle() instanceof Cindervane)) {
            // Reset zoom when not riding Cindervane
            cindervaneCameraZoom = 15F;
            cindervaneCameraZoomTarget = 15F;
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
