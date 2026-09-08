package com.leon.saintsdragons.server.ai.navigation;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public final class GroundPathGeometry {
    public static final double SUPPORT_EPSILON = 1.0E-5D;
    public static final double MAX_SUPPORT_GAP = 1.0D - SUPPORT_EPSILON;

    private GroundPathGeometry() {
    }

    public static double supportHeight(AABB body, AABB obstacle) {
        double gap = body.minY - obstacle.maxY;
        return gap >= -SUPPORT_EPSILON && gap <= MAX_SUPPORT_GAP
                && body.maxX > obstacle.minX + SUPPORT_EPSILON
                && body.minX < obstacle.maxX - SUPPORT_EPSILON
                && body.maxZ > obstacle.minZ + SUPPORT_EPSILON
                && body.minZ < obstacle.maxZ - SUPPORT_EPSILON
                ? obstacle.maxY : Double.NEGATIVE_INFINITY;
    }

    public static boolean canShortcut(AABB start, Vec3 movement,
                                      BiPredicate<AABB, Vec3> clear,
                                      Predicate<AABB> supportedAndSafe) {
        if (Math.abs(movement.y) > SUPPORT_EPSILON || !clear.test(start, movement)) {
            return false;
        }
        int samples = Math.max(1, (int) Math.ceil(movement.length() / 0.25D));
        for (int i = 0; i <= samples; i++) {
            if (!supportedAndSafe.test(start.move(movement.scale((double) i / samples)))) {
                return false;
            }
        }
        return true;
    }
}
