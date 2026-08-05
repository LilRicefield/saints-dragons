package com.leon.saintsdragons.client.camera;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

@Environment(EnvType.CLIENT)
public final class DragonFovEffects {
    private static final double BASELINE_FRAME_SECONDS = 1.0D / 60.0D;
    private static final double MAX_FRAME_SCALE = 4.0D;
    private static final double TRANSITION_SPEED = 0.05D;

    private static double currentDragonMultiplier = 1.0D;
    private static long lastUpdateNanos;

    private DragonFovEffects() {
    }

    public static double apply(double originalFov, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity vehicle = minecraft.player == null ? null : minecraft.player.getVehicle();

        if (DragonFovHelper.shouldApply(vehicle)) {
            double targetMultiplier = DragonFovHelper.getTargetMultiplier(vehicle);
            double difference = targetMultiplier - currentDragonMultiplier;
            if (Math.abs(difference) > 0.001D) {
                currentDragonMultiplier += difference * frameAdjustedSmoothing(
                        TRANSITION_SPEED,
                        consumeFrameScale()
                );
            } else {
                currentDragonMultiplier = targetMultiplier;
            }
        } else {
            resetDragonSmoothing();
        }

        return originalFov
                * currentDragonMultiplier
                * BloodTempestKatanaVisuals.getFovMultiplier(partialTick)
                * DragonlordFlightVisuals.getFovMultiplier(partialTick);
    }

    private static void resetDragonSmoothing() {
        currentDragonMultiplier = 1.0D;
        lastUpdateNanos = 0L;
    }

    private static double consumeFrameScale() {
        long now = System.nanoTime();
        if (lastUpdateNanos == 0L) {
            lastUpdateNanos = now;
            return 1.0D;
        }

        double elapsedSeconds = (now - lastUpdateNanos) / 1_000_000_000.0D;
        lastUpdateNanos = now;
        return Math.min(Math.max(elapsedSeconds / BASELINE_FRAME_SECONDS, 0.0D), MAX_FRAME_SCALE);
    }

    private static double frameAdjustedSmoothing(double smoothing, double frameScale) {
        return 1.0D - Math.pow(1.0D - smoothing, frameScale);
    }
}
