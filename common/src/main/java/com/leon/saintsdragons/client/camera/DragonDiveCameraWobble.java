package com.leon.saintsdragons.client.camera;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public final class DragonDiveCameraWobble {
    private static final double DIVE_START_SPEED = 1.55D;
    private static final double DIVE_FULL_SPEED = 4.00D;
    private static final double DIVE_START_DOWNWARD_SPEED = 0.45D;
    private static final double DIVE_FULL_DOWNWARD_SPEED = 1.35D;
    private static final float MAX_YAW_DEGREES = 0.55F;
    private static final float MAX_PITCH_DEGREES = 0.38F;
    private static final float MAX_ROLL_DEGREES = 0.85F;

    private DragonDiveCameraWobble() {
    }

    public static Output get(Entity vehicle, float partialTick) {
        if (!(vehicle instanceof RideableDragonBase dragon)) {
            return Output.NONE;
        }

        float intensity = getDiveIntensity(dragon);
        if (intensity <= 0.0F) {
            return Output.NONE;
        }

        double screenScale = Minecraft.getInstance().options.screenEffectScale().get();
        float scaledIntensity = (float) (intensity * screenScale);
        if (scaledIntensity <= 0.0F) {
            return Output.NONE;
        }

        float time = dragon.tickCount + partialTick;
        float fast = time * 2.85F;
        float flutter = time * 5.40F;
        float yaw = (Mth.sin(fast) * 0.70F + Mth.sin(flutter + 1.4F) * 0.30F) * MAX_YAW_DEGREES * scaledIntensity;
        float pitch = (Mth.sin(fast + 2.1F) * 0.65F + Mth.sin(flutter + 0.3F) * 0.35F) * MAX_PITCH_DEGREES * scaledIntensity;
        float roll = Mth.sin(fast + 0.8F) * MAX_ROLL_DEGREES * scaledIntensity;
        return new Output(yaw, pitch, roll);
    }

    public static float getDiveIntensity(Entity entity) {
        if (!(entity instanceof RideableDragonBase dragon)) {
            return 0.0F;
        }

        if (!dragon.isFlying() || dragon.isInWaterOrBubble()) {
            return 0.0F;
        }

        Vec3 velocity = dragon.getDeltaMovement();
        double downwardSpeed = -velocity.y;
        if (downwardSpeed <= DIVE_START_DOWNWARD_SPEED) {
            return 0.0F;
        }

        double speedFactor = normalize(velocity.length(), DIVE_START_SPEED, DIVE_FULL_SPEED);
        double downwardFactor = normalize(downwardSpeed, DIVE_START_DOWNWARD_SPEED, DIVE_FULL_DOWNWARD_SPEED);
        return (float) (speedFactor * downwardFactor);
    }

    private static double normalize(double value, double start, double end) {
        return Mth.clamp((value - start) / (end - start), 0.0D, 1.0D);
    }

    public record Output(float yawDegrees, float pitchDegrees, float rollDegrees) {
        private static final Output NONE = new Output(0.0F, 0.0F, 0.0F);

        public boolean active() {
            return yawDegrees != 0.0F || pitchDegrees != 0.0F || rollDegrees != 0.0F;
        }
    }
}
