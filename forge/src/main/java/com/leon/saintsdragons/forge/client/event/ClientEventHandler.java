package com.leon.saintsdragons.forge.client.event;

import com.leon.saintsdragons.client.camera.DragonRideCameraTuning;
import com.leon.saintsdragons.client.camera.DragonRideCameraController;
import com.leon.saintsdragons.client.camera.DragonDiveCameraWobble;
import com.leon.saintsdragons.client.renderer.DragonRiderCameraSync;
import com.leon.saintsdragons.client.renderer.RiderTuning;
import com.leon.saintsdragons.client.sound.DragonDiveSoundController;
import com.leon.saintsdragons.client.sound.ignivorus.IgnivorusFireBreathSoundController;
import com.leon.saintsdragons.client.sound.raevyx.RaevyxLightningBeamSoundController;
import com.leon.saintsdragons.client.sound.volitans.VolitansBreathSoundController;
import com.leon.saintsdragons.client.sound.volitans.VolitansBurrowSoundController;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.forge.client.accessor.CameraAccessor;
import com.leon.saintsdragons.forge.client.camera.CameraLeanData;
import com.leon.saintsdragons.forge.client.camera.DragonCameraState;
import com.leon.saintsdragons.forge.client.camera.NulljawFirstPersonCamera;
import com.leon.saintsdragons.forge.platform.ForgeClientConfig;
import com.leon.saintsdragons.sound.client.DragonSoundRuntime;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.entity.interfaces.ShakesScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SaintsDragonsCommon.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEventHandler {
    private static final double[] randomTremorOffsets = new double[3];

    // Raevyx beam camera state
    private static boolean wasBeaming = false;
    private static net.minecraft.client.CameraType previousPerspective = null;
    private static float beamCameraForward = 0.0f;
    private static float beamCameraUp = 0.0f;

    @SubscribeEvent
    public static void onComputeCamera(ViewportEvent.ComputeCameraAngles event) {
        Entity player = Minecraft.getInstance().getCameraEntity();
        if (player == null) return;
        if (!applyFirstPersonDragonCamera(player, event)) {
            CameraLeanData.reset();
            DragonCameraState.clearRoll();
        }
        Entity vehicle = player.getVehicle();
        handleRaevyxBeamCamera(event, vehicle);

        if (event.getCamera().isDetached()) {
            if (!applyDetachedDragonCamera(event, vehicle)) {
                DragonRideCameraController.reset();
            }
        } else if (vehicle instanceof Stegonaut stegonaut) {
            DragonRiderCameraSync.applyFirstPersonBoneAnchor(
                    stegonaut,
                    0,
                    (float) event.getPartialTick(),
                    0.0f,
                    ((CameraAccessor) event.getCamera())::saintsdragons$invokeSetPosition
            );
        } else if (vehicle instanceof Nulljaw) {
            event.getCamera().move(0.0D, NulljawFirstPersonCamera.Y_OFFSET, 0.0D);
        } else if (!player.isPassenger() || !DragonRideCameraController.supports(vehicle)) {
            DragonRideCameraController.reset();
        }

        applyDiveCameraWobble(event, vehicle);

        double shakeDistanceScale = 64.0;
        double distance = Double.MAX_VALUE;
        // Screen shake system
        float tremorAmount = 0.0F; // Reset tremor amount each frame

        AABB aabb = player.getBoundingBox().inflate(shakeDistanceScale);
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        for (Mob screenShaker : level.getEntitiesOfClass(Mob.class, aabb, (mob -> mob instanceof ShakesScreen))) {
            ShakesScreen shakesScreen = (ShakesScreen) screenShaker;
            if (shakesScreen.canFeelShake(player) && screenShaker.distanceTo(player) < distance) {
                distance = screenShaker.distanceTo(player);
                float shakeAmount = shakesScreen.getScreenShakeAmount((float) event.getPartialTick());
                tremorAmount = Math.min((1F - (float) Math.min(1, distance / shakesScreen.getShakeDistance())) * Math.max(shakeAmount, 0F), 2.0F);
            }
        }

        if (tremorAmount > 0) {
            // Generate random offsets for camera movement
            double intensity = tremorAmount * Minecraft.getInstance().options.screenEffectScale().get();
            event.getCamera().move(randomTremorOffsets[0] * 0.2F * intensity,
                    randomTremorOffsets[1] * 0.2F * intensity,
                    randomTremorOffsets[2] * 0.5F * intensity);

            // Update random offsets for next frame
            randomTremorOffsets[0] = (Math.random() - 0.5) * 2.0;
            randomTremorOffsets[1] = (Math.random() - 0.5) * 2.0;
            randomTremorOffsets[2] = (Math.random() - 0.5) * 2.0;
        }
    }

    private static void applyDiveCameraWobble(ViewportEvent.ComputeCameraAngles event, Entity vehicle) {
        if (!ForgeClientConfig.DIVE_CAMERA_WOBBLE_ENABLED.get()) {
            return;
        }

        DragonDiveCameraWobble.Output wobble = DragonDiveCameraWobble.get(vehicle, (float) event.getPartialTick());
        if (!wobble.active()) {
            return;
        }

        event.setYaw(event.getYaw() + wobble.yawDegrees());
        event.setPitch(Mth.clamp(event.getPitch() + wobble.pitchDegrees(), -90.0F, 90.0F));
        event.setRoll(event.getRoll() + wobble.rollDegrees());
    }

    private static void handleRaevyxBeamCamera(ViewportEvent.ComputeCameraAngles event, Entity vehicle) {
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
        event.getCamera().move(beamCameraForward, 0, 0);
        event.getCamera().move(0, -beamCameraUp, 0);
    }

    private static boolean applyDetachedDragonCamera(ViewportEvent.ComputeCameraAngles event, Entity vehicle) {
        if (!DragonRideCameraController.supports(vehicle)) {
            return false;
        }

        if (vehicle instanceof Raevyx raevyx && raevyx.isBeaming()) {
            return false;
        }

        DragonRideCameraController.CameraOutput output =
                DragonRideCameraController.update(vehicle, (float) event.getPartialTick());
        event.getCamera().move(-event.getCamera().getMaxZoom(output.zoom()), 0, 0);
        event.getCamera().move(0, output.verticalShift(), output.lateralShift());
        event.setPitch(Mth.clamp(event.getPitch() + output.pitchOffset(), -90.0f, 90.0f));
        return true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        DragonRideCameraTuning.bootstrap();
        RiderTuning.bootstrap();
        Minecraft minecraft = Minecraft.getInstance();
        DragonSoundRuntime.tick(minecraft);
        DragonDiveSoundController.tick(minecraft);
        RaevyxLightningBeamSoundController.tick(minecraft);
        IgnivorusFireBreathSoundController.tick(minecraft);
        VolitansBreathSoundController.tick(minecraft);
        VolitansBurrowSoundController.tick(minecraft);
    }

    private static boolean applyFirstPersonDragonCamera(Entity player, ViewportEvent.ComputeCameraAngles event) {
        if (event.getCamera().isDetached()) {
            return false;
        }

        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof RideableDragonBase dragon) || !usesFirstPersonDragonCamera(dragon)) {
            return false;
        }

        if (!isFirstPersonBankingCameraEnabled()) {
            CameraLeanData.reset();
            DragonCameraState.clearRoll();
            return true;
        }

        if (dragon instanceof Raevyx raevyx && raevyx.isBeaming()) {
            CameraLeanData.reset();
            DragonCameraState.clearRoll();
            return true;
        }

        DragonCameraState.clearRoll();

        float partialTick = (float) event.getPartialTick();
        float rollDegrees = getBodyRollDegrees(dragon, partialTick);
        float pitchDegrees = Mth.lerp(partialTick, dragon.xRotO, dragon.getXRot());
        float yawSpeed = Mth.wrapDegrees(dragon.yBodyRot - dragon.yBodyRotO);
        float yawLeanScale = 1.0f;

        CameraLeanData.updateTarget(rollDegrees, pitchDegrees, yawSpeed, yawLeanScale);
        CameraLeanData.update();

        float cameraTilt = (float) CameraLeanData.getCameraTilt();
        event.setRoll(event.getRoll() - rollDegrees + cameraTilt);

        double leanX = CameraLeanData.getLeanX();
        double leanY = CameraLeanData.getLeanY();
        double leanZ = CameraLeanData.getLeanZ();
        if (Math.abs(leanX) > 0.001 || Math.abs(leanY) > 0.001 || Math.abs(leanZ) > 0.001) {
            event.getCamera().move(leanZ, leanY, leanX);
        }
        return true;
    }

    private static boolean usesFirstPersonDragonCamera(RideableDragonBase dragon) {
        return dragon instanceof Raevyx
                || dragon instanceof Cindervane
                || dragon instanceof Ignivorus
                || dragon instanceof Volitans;
    }

    private static float getBodyRollDegrees(RideableDragonBase dragon, float partialTick) {
        if (dragon instanceof Raevyx raevyx) {
            return raevyx.getBankAngleDegrees(partialTick) + raevyx.getSmoothedRoll(partialTick) * Mth.RAD_TO_DEG;
        }
        if (dragon instanceof Cindervane cindervane) {
            return cindervane.getBankAngleDegrees(partialTick) + cindervane.getSmoothedRoll(partialTick) * Mth.RAD_TO_DEG;
        }
        if (dragon instanceof Ignivorus ignivorus) {
            return ignivorus.getBankAngleDegrees(partialTick) + ignivorus.getSmoothedRoll(partialTick) * Mth.RAD_TO_DEG;
        }
        if (dragon instanceof Volitans volitans) {
            return volitans.getBankAngleDegrees(partialTick) + volitans.getSmoothedRoll(partialTick) * Mth.RAD_TO_DEG;
        }
        return 0.0f;
    }

    private static boolean isFirstPersonBankingCameraEnabled() {
        return ForgeClientConfig.FIRST_PERSON_BANKING_CAMERA_ENABLED == null
                || ForgeClientConfig.FIRST_PERSON_BANKING_CAMERA_ENABLED.get();
    }
}

