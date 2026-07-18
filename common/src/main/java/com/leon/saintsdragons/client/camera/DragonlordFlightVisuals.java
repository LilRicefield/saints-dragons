package com.leon.saintsdragons.client.camera;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public final class DragonlordFlightVisuals {
    private static final int BOOST_TICKS = 80;
    private static final int FADE_IN_TICKS = 10;
    private static final int FADE_OUT_TICKS = 20;
    private static final double FOV_BOOST = 0.22D;

    private static int boostTicks;

    private DragonlordFlightVisuals() {
    }

    public static void startBoost() {
        boostTicks = BOOST_TICKS;
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            boostTicks = 0;
            return;
        }
        if (!minecraft.isPaused()) {
            if (boostTicks > 0) {
                boostTicks--;
            }
        }
    }

    public static double getFovMultiplier(float partialTick) {
        double effectScale = Minecraft.getInstance().options.fovEffectScale().get();
        return 1.0D + FOV_BOOST * getIntensity(partialTick) * effectScale;
    }

    public static float getSpeedLineIntensity(float partialTick) {
        return getIntensity(partialTick);
    }

    private static float getIntensity(float partialTick) {
        return getIntensity(boostTicks, partialTick);
    }

    private static float getIntensity(int remainingTicks, float partialTick) {
        if (remainingTicks <= 0) {
            return 0.0F;
        }
        float remaining = Mth.clamp(remainingTicks - partialTick, 0.0F, BOOST_TICKS);
        float elapsed = BOOST_TICKS - remaining;
        float fadeIn = Mth.clamp(elapsed / FADE_IN_TICKS, 0.0F, 1.0F);
        float fadeOut = Mth.clamp(remaining / FADE_OUT_TICKS, 0.0F, 1.0F);
        float blend = Math.min(fadeIn, fadeOut);
        return blend * blend * (3.0F - 2.0F * blend);
    }
}
