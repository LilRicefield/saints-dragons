package com.leon.saintsdragons.server.ai.dragonbrain.perception;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public record DragonVisionProfile(double range, double nearDistance,
                                  double nearHorizontal, double farHorizontal,
                                  double nearVertical, double farVertical) {
    public static final int SCAN_INTERVAL = 4;
    public static final int LIVING_BUDGET = 8;
    public static final int PROJECTILE_BUDGET = 4;
    public static final double PROJECTILE_RANGE = 32;
    public static final double PROJECTILE_HORIZON = 12;
    public static final float SUSPICION = 0.2F;
    public static final float RECOGNITION = 1.0F;
    public static final float FORGET_RECOGNITION = 0.15F;
    public static final float DECAY_PER_TICK = 0.025F;
    public static final double RETENTION_MARGIN = 12;

    public static DragonVisionProfile standard(double range, double width) {
        return new DragonVisionProfile(Mth.clamp(range, 16, 128), Math.max(6, width * 1.5),
                110, 45, 80, 45);
    }

    private double blend(double distance) {
        double end = nearDistance / (nearDistance + range);
        return Mth.clamp((nearDistance / (nearDistance + distance) - end) / (1 - end), 0, 1);
    }

    public double horizontal(double distance, boolean retained) {
        return Math.min(150, Mth.lerp(blend(distance), farHorizontal, nearHorizontal)
                + (retained ? RETENTION_MARGIN : 0));
    }

    public double vertical(double distance, boolean retained) {
        return Math.min(89, Mth.lerp(blend(distance), farVertical, nearVertical)
                + (retained ? RETENTION_MARGIN : 0));
    }

    public static Vec3 right(Vec3 forward) {
        Vec3 right = new Vec3(forward.z, 0, -forward.x);
        return right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
    }

    public boolean contains(Vec3 origin, Vec3 forward, Vec3 point, boolean retained) {
        Vec3 offset = point.subtract(origin);
        double distance = offset.length();
        if (distance > range) return false;
        if (distance < 1.0E-6) return true;
        Vec3 right = right(forward);
        Vec3 up = forward.cross(right).normalize();
        double x = offset.dot(right), z = offset.dot(forward), y = offset.dot(up);
        return Math.abs(Math.toDegrees(Math.atan2(x, z))) <= horizontal(distance, retained)
                && Math.abs(Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z))))
                <= vertical(distance, retained);
    }

    public static Vec3 point(Vec3 origin, Vec3 forward, double distance, double yaw, double pitch) {
        Vec3 right = right(forward);
        Vec3 up = forward.cross(right).normalize();
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return origin.add(forward.scale(Math.cos(y) * Math.cos(p) * distance))
                .add(right.scale(Math.sin(y) * Math.cos(p) * distance))
                .add(up.scale(Math.sin(p) * distance));
    }
}
