package com.leon.saintsdragons.server.flight;

import net.minecraft.world.phys.Vec3;

public final class DragonFallRecovery {
    public static final float DEFAULT_FALL_ANIMATION_MIN_BLOCKS = 1.0F;
    public static final float DEFAULT_FALL_RECOVERY_MIN_BLOCKS = 1.0F;
    public static final double DEFAULT_FALL_ANIMATION_MIN_DESCENT = -0.12D;
    public static final double DEFAULT_FALL_RECOVERY_MIN_DESCENT = -0.08D;

    private DragonFallRecovery() {
    }

    public static boolean isFallingForAnimation(
            boolean isVehicle,
            boolean isFlying,
            boolean isTakeoff,
            boolean isLanding,
            boolean isHovering,
            boolean onGround,
            boolean inWaterOrBubble,
            boolean inLava,
            float fallDistance,
            Vec3 deltaMovement
    ) {
        if (!isVehicle || isFlying || isTakeoff || isLanding || isHovering) {
            return false;
        }
        if (onGround || inWaterOrBubble || inLava) {
            return false;
        }
        return fallDistance >= DEFAULT_FALL_ANIMATION_MIN_BLOCKS
                && deltaMovement.y <= DEFAULT_FALL_ANIMATION_MIN_DESCENT;
    }

    public static boolean canRecoverTakeoffFromFall(
            boolean isTame,
            boolean isVehicle,
            boolean isAlive,
            boolean isBaby,
            boolean isFlying,
            boolean isTakeoff,
            boolean isLanding,
            boolean isHovering,
            boolean onGround,
            boolean inWaterOrBubble,
            boolean inLava,
            boolean additionalBlocked,
            float fallDistance,
            Vec3 deltaMovement
    ) {
        if (isFlying || isTakeoff || isLanding || isHovering) {
            return false;
        }
        if (!isTame || !isVehicle || !isAlive || isBaby) {
            return false;
        }
        if (onGround || inWaterOrBubble || inLava || additionalBlocked) {
            return false;
        }
        return fallDistance >= DEFAULT_FALL_RECOVERY_MIN_BLOCKS
                || deltaMovement.y <= DEFAULT_FALL_RECOVERY_MIN_DESCENT;
    }
}