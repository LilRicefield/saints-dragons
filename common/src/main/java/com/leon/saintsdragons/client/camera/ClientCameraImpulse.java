package com.leon.saintsdragons.client.camera;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public final class ClientCameraImpulse {
    private static final double FIRST_PERSON_INTENSITY_MULTIPLIER = 1.35D;

    private static int remainingTicks;
    private static int totalTicks;
    private static float intensity;

    private ClientCameraImpulse() {
    }

    public static void trigger(float requestedIntensity, int durationTicks) {
        if (durationTicks <= 0
                || requestedIntensity <= 0.0F
                || SaintsDragonsConfig.SCREEN_SHAKE_ENABLED == null
                || !SaintsDragonsConfig.SCREEN_SHAKE_ENABLED.get()) {
            return;
        }

        intensity = Math.max(intensity, requestedIntensity);
        totalTicks = Math.max(totalTicks, durationTicks);
        remainingTicks = Math.max(remainingTicks, durationTicks);
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            clear();
            return;
        }
        if (!minecraft.isPaused() && remainingTicks > 0) {
            remainingTicks--;
            if (remainingTicks == 0) {
                clear();
            }
        }
    }

    public static Offset sample(float partialTick) {
        if (remainingTicks <= 0 || totalTicks <= 0) {
            return Offset.NONE;
        }

        float remaining = Mth.clamp(remainingTicks - partialTick, 0.0F, totalTicks);
        float life = remaining / totalTicks;
        float elapsed = totalTicks - remaining;
        Minecraft minecraft = Minecraft.getInstance();
        double perspectiveMultiplier = minecraft.options.getCameraType() == CameraType.FIRST_PERSON
                ? FIRST_PERSON_INTENSITY_MULTIPLIER
                : 1.0D;
        double scale = intensity
                * life * life
                * minecraft.options.screenEffectScale().get()
                * perspectiveMultiplier;

        double forward = 0.0D;
        double vertical = Math.sin(elapsed * 4.10D + 0.65D) * 0.07D * scale;
        double lateral = Math.sin(elapsed * 3.35D + 1.20D) * 0.11D * scale;
        return new Offset(forward, vertical, lateral);
    }

    private static void clear() {
        remainingTicks = 0;
        totalTicks = 0;
        intensity = 0.0F;
    }

    public record Offset(double forward, double vertical, double lateral) {
        private static final Offset NONE = new Offset(0.0D, 0.0D, 0.0D);

        public boolean active() {
            return this != NONE;
        }
    }
}
