package com.leon.saintsdragons.server.ai.navigation.async;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class FlightLandingMotion {
    static final double TOUCHDOWN_HEIGHT = 3.0D;
    // Downward speeds in blocks per tick; these apply only to the final contact descent.
    static final double MIN_DESCENT_SPEED = 0.16D;
    static final double MAX_DESCENT_SPEED = 0.65D;
    static final double MIN_INITIAL_DESCENT_SPEED = 0.08D;

    private FlightLandingMotion() { }

    static boolean nearTouchdown(Vec3 position, Vec3 touchdown) {
        Vec3 offset = position.subtract(touchdown);
        return offset.y >= -0.1D && offset.y <= TOUCHDOWN_HEIGHT
                && offset.horizontalDistanceSqr() <= 1.0D;
    }

    static Vec3 velocity(Vec3 current, Vec3 offset, double speed) {
        Vec3 horizontal = offset.multiply(0.5D, 0.0D, 0.5D);
        double maximum = Math.min(speed, offset.horizontalDistance() * 0.6D);
        if (horizontal.horizontalDistance() > maximum) horizontal = horizontal.normalize().scale(maximum);
        double descent = Math.min(Math.max(MIN_DESCENT_SPEED, speed),
                Mth.clamp(-offset.y * 0.5D, MIN_DESCENT_SPEED, MAX_DESCENT_SPEED));
        // Remove upward momentum before enabling the landing pose.
        Vec3 result = new Vec3(Mth.lerp(0.5D, current.x, horizontal.x),
                Mth.clamp(Mth.lerp(0.6D, current.y, -descent), -MAX_DESCENT_SPEED, -MIN_INITIAL_DESCENT_SPEED),
                Mth.lerp(0.5D, current.z, horizontal.z));
        double horizontalSpeed = result.horizontalDistance();
        if (horizontalSpeed > maximum) {
            double scale = maximum / horizontalSpeed;
            result = new Vec3(result.x * scale, result.y, result.z * scale);
        }
        return result;
    }
}
