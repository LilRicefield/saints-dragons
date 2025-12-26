package com.leon.saintsdragons.forge.client.event;

import com.leon.saintsdragons.client.sound.ignivorus.IgnivorusFireBreathSoundController;
import com.leon.saintsdragons.client.sound.raevyx.RaevyxLightningBeamSoundController;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.interfaces.ShakesScreen;
import net.minecraft.client.Minecraft;
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
    private static float ignivorusCameraZoom = 10F; // Base zoom
    private static float ignivorusCameraZoomTarget = 15F;

    // Ignivorus camera shift smoothing (banking response)
    private static double ignivorusCameraShift = 0.0;

    // Shared vertical camera shift for ascending/descending (both dragons)
    private static double verticalCameraShift = 0.0;

    @SubscribeEvent
    public static void onComputeCamera(ViewportEvent.ComputeCameraAngles event) {
        Entity player = Minecraft.getInstance().getCameraEntity();
        if (player == null) return;


        // Dragon riding camera adjustments
        if (player.isPassenger() && player.getVehicle() instanceof Raevyx raevyx && event.getCamera().isDetached()) {
            // Determine target zoom based on flight state
            boolean isFlying = raevyx.isFlying();

            // Flying: zoom to 18F, grounded: 10F base
            raevyxCameraZoomTarget = isFlying ? 18F : 10F;

            // Smooth transition (slower blend rate for more gradual zoom)
            float blendRate = 0.05F; // Reduced from 0.15F for slower, smoother transitions
            raevyxCameraZoom += (raevyxCameraZoomTarget - raevyxCameraZoom) * blendRate;

            // Calculate camera shift based on banking (only when flying)
            double targetCameraShift = 0.0;
            if (isFlying) {
                // Get interpolated bank angle (-90 to +90 degrees)
                float bankAngle = raevyx.getBankAngleDegrees((float) event.getPartialTick());

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
            double targetVerticalShift = 0.0;
            if (isFlying) {
                if (raevyx.isGoingUp()) {
                    targetVerticalShift = 1.2; // Subtle upward shift when ascending
                } else if (raevyx.isGoingDown()) {
                    targetVerticalShift = -1.2; // Subtle downward shift when descending
                }
            }
            // Smooth vertical shift
            double verticalBlendRate = 0.12; // Slightly slower than lateral for smoother feel
            verticalCameraShift += (targetVerticalShift - verticalCameraShift) * verticalBlendRate;

            // Apply the smoothed zoom and lateral shift
            event.getCamera().move(-event.getCamera().getMaxZoom(raevyxCameraZoom), 0, 0);
            // Apply lateral and vertical shifts
            event.getCamera().move(0, verticalCameraShift, raevyxCameraShift);
        } else {
            // Reset zoom and shift when not riding Raevyx
            raevyxCameraZoom = 10F;
            raevyxCameraZoomTarget = 10F;
            raevyxCameraShift = 0.0;
            verticalCameraShift = 0.0;
        }

        // Cindervane camera zoom adjustments
        if (player.isPassenger() && player.getVehicle() instanceof Cindervane cindervane && event.getCamera().isDetached()) {
            // Determine target zoom based on flight state
            boolean isFlying = cindervane.isFlying();

            // Flying: zoom to 25F, grounded: 10F base
            cindervaneCameraZoomTarget = isFlying ? 30F : 15F;

            // Smooth transition (slower blend rate for more gradual zoom)
            float blendRate = 0.05F; // Reduced from 0.15F for slower, smoother transitions
            cindervaneCameraZoom += (cindervaneCameraZoomTarget - cindervaneCameraZoom) * blendRate;

            // Calculate camera shift based on banking (only when flying)
            double targetCameraShift = 0.0;
            if (isFlying) {
                // Get interpolated bank angle (-90 to +90 degrees)
                float bankAngle = cindervane.getBankAngleDegrees((float) event.getPartialTick());

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
            double targetVerticalShift = 0.0;
            if (isFlying) {
                if (cindervane.isGoingUp()) {
                    targetVerticalShift = 1.2; // Subtle upward shift when ascending
                } else if (cindervane.isGoingDown()) {
                    targetVerticalShift = -1.2; // Subtle downward shift when descending
                }
            }
            // Smooth vertical shift
            double verticalBlendRate = 0.12; // Slightly slower than lateral for smoother feel
            verticalCameraShift += (targetVerticalShift - verticalCameraShift) * verticalBlendRate;

            // Apply the smoothed zoom and lateral shift
            event.getCamera().move(-event.getCamera().getMaxZoom(cindervaneCameraZoom), 0, 0);
            // Apply lateral and vertical shifts
            event.getCamera().move(0, verticalCameraShift, cindervaneCameraShift);
        } else if (!(player.getVehicle() instanceof Cindervane)) {
            // Reset zoom and shift when not riding Cindervane
            cindervaneCameraZoom = 15F;
            cindervaneCameraZoomTarget = 15F;
            cindervaneCameraShift = 0.0;
            verticalCameraShift = 0.0;
        }

        // Ignivorus camera zoom adjustments
        if (player.isPassenger() && player.getVehicle() instanceof Ignivorus ignivorus && event.getCamera().isDetached()) {
            // Determine target zoom based on flight state
            boolean isFlying = ignivorus.isFlying();
            boolean isPhase2 = ignivorus.isPhase2Active();

            // Phase 2 only affects grounded camera zoom
            if (isFlying) {
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
                float bankAngle = ignivorus.getBankAngleDegrees((float) event.getPartialTick());

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
            double targetVerticalShift = 0.0;
            if (isFlying) {
                if (ignivorus.isGoingUp()) {
                    targetVerticalShift = 1.2; // Subtle upward shift when ascending
                } else if (ignivorus.isGoingDown()) {
                    targetVerticalShift = -1.2; // Subtle downward shift when descending
                }
            }
            // Smooth vertical shift
            double verticalBlendRate = 0.12;
            verticalCameraShift += (targetVerticalShift - verticalCameraShift) * verticalBlendRate;

            // Apply the smoothed zoom
            event.getCamera().move(-event.getCamera().getMaxZoom(ignivorusCameraZoom), 0, 0);
            // Apply lateral and vertical shifts
            event.getCamera().move(0, verticalCameraShift, ignivorusCameraShift);
        } else if (!(player.getVehicle() instanceof Ignivorus)) {
            // Reset zoom and shift when not riding Ignivorus
            ignivorusCameraZoom = 10F;
            ignivorusCameraZoomTarget = 15F;
            ignivorusCameraShift = 0.0;
            verticalCameraShift = 0.0;
        }

        if (player.isPassenger() && player.getVehicle() instanceof Nulljaw && event.getCamera().isDetached()) {
            event.getCamera().move(-event.getCamera().getMaxZoom(15F), 0, 0);
        }

        // Screen shake detection and application
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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        RaevyxLightningBeamSoundController.tick(minecraft);
        IgnivorusFireBreathSoundController.tick(minecraft);
    }
}
