package com.leon.saintsdragons.server.entity.component;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class DragonMotionMath {
    private DragonMotionMath() {
    }

    public static double speedForIntegratedDistance(double distance, double horizontalDrag, int durationTicks) {
        double dragScale = 1.0D - Math.pow(horizontalDrag, Math.max(1, durationTicks));
        if (dragScale <= 1.0E-6D) {
            return 0.0D;
        }
        return distance * (1.0D - horizontalDrag) / dragScale;
    }

    public static Vec3 horizontalForward(float yawDegrees) {
        float yawRadians = yawDegrees * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yawRadians), 0.0D, Mth.cos(yawRadians)).normalize();
    }

    public static Vec3 horizontalRight(float yawDegrees) {
        float yawRadians = yawDegrees * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(yawRadians), 0.0D, Mth.sin(yawRadians)).normalize();
    }

    public static Vec3 horizontalRelative(float yawDegrees, float offsetDegrees) {
        return horizontalForward(yawDegrees + offsetDegrees);
    }
}
