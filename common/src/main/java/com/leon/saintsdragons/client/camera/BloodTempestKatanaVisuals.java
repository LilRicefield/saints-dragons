package com.leon.saintsdragons.client.camera;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public final class BloodTempestKatanaVisuals {
    private static final int ZIP_TICKS = 12;
    private static final double ZIP_FOV_BOOST = 0.20D;

    private static int zipTicks;

    private BloodTempestKatanaVisuals() {
    }

    public static void startZip() {
        zipTicks = ZIP_TICKS;
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        if (zipTicks > 0) {
            zipTicks--;
        }
    }

    public static double getFovMultiplier(float partialTick) {
        double effectScale = Minecraft.getInstance().options.fovEffectScale().get();
        float life = getZipIntensity(partialTick);
        return 1.0D + ZIP_FOV_BOOST * life * effectScale;
    }

    public static float getSpeedLineIntensity(float partialTick) {
        return getZipIntensity(partialTick);
    }

    private static float getZipIntensity(float partialTick) {
        if (zipTicks <= 0) {
            return 0.0F;
        }
        float life = Mth.clamp((zipTicks - partialTick) / ZIP_TICKS, 0.0F, 1.0F);
        return Mth.sin(life * Mth.HALF_PI);
    }

    private static void clear() {
        zipTicks = 0;
    }
}
