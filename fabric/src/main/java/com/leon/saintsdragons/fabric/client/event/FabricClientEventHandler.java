package com.leon.saintsdragons.fabric.client.event;

import com.leon.saintsdragons.client.camera.ClientCameraImpulse;
import com.leon.saintsdragons.client.camera.DragonRideCameraController;
import com.leon.saintsdragons.client.camera.DragonDiveCameraWobble;
import com.leon.saintsdragons.client.init.CommonClientLifecycleEvents;
import com.leon.saintsdragons.fabric.client.accessor.CameraAccessor;
import com.leon.saintsdragons.fabric.client.camera.DragonCameraState;
import com.leon.saintsdragons.fabric.config.FabricClientConfigAccess;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.entity.interfaces.ShakesScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

public class FabricClientEventHandler {
    private static final double[] randomTremorOffsets = new double[3];

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
        CommonClientLifecycleEvents.bootstrap();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CommonClientLifecycleEvents.onEndClientTick(client);
        });
    }

    /**
     * Applies rotation and mode-specific movement before the first-person seat
     * anchor is resolved.
     */
    public static void onComputeCameraBeforeSeatAnchor(Camera camera, float partialTicks) {
        DragonCameraState.clearDiveRoll();
        Entity player = Minecraft.getInstance().getCameraEntity();
        if (player == null) return;

        Entity vehicle = player.getVehicle();
        handleRaevyxBeamCamera(camera, vehicle);

        if (camera.isDetached()) {
            if (!applyDetachedDragonCamera(camera, vehicle, partialTicks)) {
                DragonRideCameraController.reset();
            }
        } else if (!player.isPassenger() || !(vehicle instanceof RideableDragonBase)) {
            DragonRideCameraController.reset();
        }

        applyDiveCameraWobble(camera, vehicle, partialTicks);
    }

    /**
     * Applies transient offsets after the seat anchor so they are not overwritten.
     */
    public static void onComputeCameraAfterSeatAnchor(Camera camera, float partialTicks) {
        Entity player = Minecraft.getInstance().getCameraEntity();
        if (player == null) return;

        applyScreenShake(camera, player, partialTicks);

        ClientCameraImpulse.Offset impulse = ClientCameraImpulse.sample(partialTicks);
        if (impulse.active()) {
            ((CameraAccessor) camera).saintsdragons$invokeMove(
                    impulse.forward(), impulse.vertical(), impulse.lateral());
        }
    }

    private static void applyDiveCameraWobble(Camera camera, Entity vehicle, float partialTicks) {
        if (!FabricClientConfigAccess.isDiveCameraWobbleEnabled()) {
            return;
        }
        if (vehicle instanceof Raevyx raevyx && raevyx.isBeaming()) {
            return;
        }

        DragonDiveCameraWobble.Output wobble = DragonDiveCameraWobble.get(vehicle, partialTicks);
        if (!wobble.active()) {
            return;
        }

        CameraAccessor cameraAccessor = (CameraAccessor) camera;
        float yaw = cameraAccessor.saintsdragons$invokeGetYRot() + wobble.yawDegrees();
        float pitch = Mth.clamp(cameraAccessor.saintsdragons$invokeGetXRot() + wobble.pitchDegrees(), -90.0F, 90.0F);
        cameraAccessor.saintsdragons$invokeSetRotation(yaw, pitch);
        DragonCameraState.setDiveRoll(wobble.rollDegrees());
    }

    private static void handleRaevyxBeamCamera(Camera camera, Entity vehicle) {
        if (!(vehicle instanceof Raevyx raevyx)) {
            wasBeaming = false;
            previousPerspective = null;
            beamCameraForward = 0.0f;
            beamCameraUp = 0.0f;
            return;
        }

        boolean isBeaming = raevyx.isBeaming();
        Minecraft mc = Minecraft.getInstance();
        if (isBeaming && !wasBeaming) {
            previousPerspective = mc.options.getCameraType();
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            wasBeaming = true;
        } else if (!isBeaming && wasBeaming) {
            if (previousPerspective != null) {
                mc.options.setCameraType(previousPerspective);
                previousPerspective = null;
            }
            wasBeaming = false;
            beamCameraForward = 0.0f;
            beamCameraUp = 0.0f;
        }

        if (!isBeaming) {
            return;
        }

        float targetForward = 7.5f;
        float targetUp = -2.0f;
        float blendRate = 0.2f;
        beamCameraForward += (targetForward - beamCameraForward) * blendRate;
        beamCameraUp += (targetUp - beamCameraUp) * blendRate;

        CameraAccessor cameraAccessor = (CameraAccessor) camera;
        cameraAccessor.saintsdragons$invokeMove(beamCameraForward, 0, 0);
        cameraAccessor.saintsdragons$invokeMove(0, -beamCameraUp, 0);
    }

    private static boolean applyDetachedDragonCamera(Camera camera, Entity vehicle, float partialTicks) {
        if (!DragonRideCameraController.supports(vehicle)) {
            return false;
        }

        if (vehicle instanceof Raevyx raevyx && raevyx.isBeaming()) {
            return false;
        }

        DragonRideCameraController.CameraOutput output = DragonRideCameraController.update(vehicle, partialTicks);
        CameraAccessor cameraAccessor = (CameraAccessor) camera;
        double maxZoom = cameraAccessor.saintsdragons$invokeGetMaxZoom(output.zoom());
        cameraAccessor.saintsdragons$invokeMove(-maxZoom, 0, 0);
        double lateralShift = FabricClientConfigAccess.isThirdPersonBankingCameraEnabled()
                ? output.lateralShift()
                : 0.0D;
        cameraAccessor.saintsdragons$invokeMove(0, output.verticalShift(), lateralShift);

        float currentYaw = cameraAccessor.saintsdragons$invokeGetYRot();
        float currentPitch = cameraAccessor.saintsdragons$invokeGetXRot();
        float clampedPitch = Mth.clamp(currentPitch + output.pitchOffset(), -90.0f, 90.0f);
        cameraAccessor.saintsdragons$invokeSetRotation(currentYaw, clampedPitch);
        return true;
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

