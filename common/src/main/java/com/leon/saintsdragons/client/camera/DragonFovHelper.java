package com.leon.saintsdragons.client.camera;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.RideableDragon;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class DragonFovHelper {
    public static final double GROUND_SPRINT_MULTIPLIER = 1.050;
    public static final double FLY_SWIM_SPRINT_MULTIPLIER = 1.075;
    private static final double DIVE_START_SPEED = 0.75D;
    private static final double DIVE_FULL_SPEED = 4.00D;
    private static final double DIVE_START_DOWNWARD_SPEED = 0.10D;
    private static final double DIVE_FULL_DOWNWARD_SPEED = 1.35D;
    private static final double DIVE_MAX_MULTIPLIER = 1.320D;

    private DragonFovHelper() {}

    public static boolean shouldApply(Entity vehicle) {
        return vehicle instanceof DragonEntity && vehicle instanceof RideableDragon;
    }

    public static double getTargetMultiplier(Entity vehicle) {
        if (!(vehicle instanceof DragonEntity dragon) || !(vehicle instanceof RideableDragon rideable)) {
            return 1.0;
        }
        double diveMultiplier = getDiveMultiplier(dragon);
        if (!rideable.isAccelerating()) {
            return diveMultiplier;
        }
        if (dragon.isFlying() || dragon.isInWaterOrBubble()) {
            return Math.max(FLY_SWIM_SPRINT_MULTIPLIER, diveMultiplier);
        }
        return GROUND_SPRINT_MULTIPLIER;
    }

    private static double getDiveMultiplier(DragonEntity dragon) {
        if (!dragon.isFlying() || dragon.isInWaterOrBubble()) {
            return 1.0D;
        }

        Vec3 velocity = dragon.getDeltaMovement();
        double downwardSpeed = -velocity.y;
        if (downwardSpeed <= DIVE_START_DOWNWARD_SPEED) {
            return 1.0D;
        }

        double speedFactor = normalize(velocity.length(), DIVE_START_SPEED, DIVE_FULL_SPEED);
        double downwardFactor = normalize(downwardSpeed, DIVE_START_DOWNWARD_SPEED, DIVE_FULL_DOWNWARD_SPEED);
        double diveFactor = speedFactor * downwardFactor;
        return Mth.lerp(diveFactor, 1.0D, DIVE_MAX_MULTIPLIER);
    }

    private static double normalize(double value, double start, double end) {
        return Mth.clamp((value - start) / (end - start), 0.0D, 1.0D);
    }
}
