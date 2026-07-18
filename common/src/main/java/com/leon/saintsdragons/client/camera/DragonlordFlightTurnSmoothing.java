package com.leon.saintsdragons.client.camera;

import com.leon.saintsdragons.common.item.DragonlordArmorSetBonus;
import com.leon.saintsdragons.platform.Services;
import com.mojang.blaze3d.Blaze3D;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.SmoothDouble;

@Environment(EnvType.CLIENT)
public final class DragonlordFlightTurnSmoothing {
    private static final double DEFAULT_FRAME_SECONDS = 1.0D / 60.0D;
    private static final double MAX_FRAME_SECONDS = 1.0D / 20.0D;
    private static final double PITCH_SMOOTHING_SECONDS = 0.35D;
    private static final double YAW_SMOOTHING_SECONDS = 0.55D;
    private static final boolean DO_A_BARREL_ROLL_LOADED =
            Services.PLATFORM.isModLoaded("do_a_barrel_roll");

    private static final SmoothDouble PITCH_SMOOTHER = new SmoothDouble();
    private static final SmoothDouble YAW_SMOOTHER = new SmoothDouble();

    private static boolean active;
    private static double lastPitchUpdateTime;
    private static double lastYawUpdateTime;

    private DragonlordFlightTurnSmoothing() {
    }

    public static double smoothYaw(LocalPlayer player, double yawDelta) {
        if (!shouldSmooth(player)) {
            reset();
            return yawDelta;
        }

        active = true;
        return YAW_SMOOTHER.getNewDeltaValue(
                yawDelta,
                Math.min(1.0D, consumeYawFrameSeconds() / YAW_SMOOTHING_SECONDS)
        );
    }

    public static double smoothPitch(LocalPlayer player, double pitchDelta) {
        if (!shouldSmooth(player)) {
            reset();
            return pitchDelta;
        }

        active = true;
        return PITCH_SMOOTHER.getNewDeltaValue(
                pitchDelta,
                Math.min(1.0D, consumePitchFrameSeconds() / PITCH_SMOOTHING_SECONDS)
        );
    }

    private static boolean shouldSmooth(LocalPlayer player) {
        return !DO_A_BARREL_ROLL_LOADED
                && player != null
                && player.isAlive()
                && !player.isSpectator()
                && !player.isPassenger()
                && player.isFallFlying()
                && DragonlordArmorSetBonus.isWearingFullSet(player);
    }

    private static double consumeYawFrameSeconds() {
        double now = Blaze3D.getTime();
        double frameSeconds = lastYawUpdateTime == 0.0D
                ? DEFAULT_FRAME_SECONDS
                : Mth.clamp(now - lastYawUpdateTime, 0.0D, MAX_FRAME_SECONDS);
        lastYawUpdateTime = now;
        return frameSeconds;
    }

    private static double consumePitchFrameSeconds() {
        double now = Blaze3D.getTime();
        double frameSeconds = lastPitchUpdateTime == 0.0D
                ? DEFAULT_FRAME_SECONDS
                : Mth.clamp(now - lastPitchUpdateTime, 0.0D, MAX_FRAME_SECONDS);
        lastPitchUpdateTime = now;
        return frameSeconds;
    }

    private static void reset() {
        if (!active) {
            return;
        }
        active = false;
        lastPitchUpdateTime = 0.0D;
        lastYawUpdateTime = 0.0D;
        PITCH_SMOOTHER.reset();
        YAW_SMOOTHER.reset();
    }
}
