package com.leon.saintsdragons.server.ai.navigation.async;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class FlightMotionPolicy {
    static final double MAX_ACCELERATION = 0.12D;
    static final double TAKEOFF_MAX_SPEED = 1.0D;
    static final double TAKEOFF_MAX_VERTICAL_SPEED = 0.8D;
    static final double TAKEOFF_MIN_LIFT = 0.12D;
    static final double DIVE_VERTICAL_ACCELERATION = 0.35D;
    static final double DIVE_VERTICAL_LERP = 0.35D;
    static final double DIVE_SPEED_OVERDRIVE = 1.8D;
    private static final double DIVE_FULL_RATIO = Math.sin(Math.toRadians(55.0D));

    private FlightMotionPolicy() { }

    static double diveSpeed(double speed, double descentComponent) {
        double ratio = Mth.clamp(-descentComponent / DIVE_FULL_RATIO, 0.0D, 1.0D);
        return speed * Mth.lerp(ratio, 1.0D, DIVE_SPEED_OVERDRIVE);
    }

    static double requestedSpeed(double flightSpeed, double modifier) {
        return Double.isFinite(flightSpeed) && Double.isFinite(modifier)
                ? Math.max(0.0D, flightSpeed) * Math.max(0.0D, modifier) : 0.0D;
    }

    static double takeoffVerticalSpeed(double requested, double heightRemaining) {
        double brakingSpeed = Math.sqrt(2.0D * MAX_ACCELERATION * Math.max(0.0D, heightRemaining - 0.75D));
        return Mth.clamp(Math.min(requested, brakingSpeed), TAKEOFF_MIN_LIFT, TAKEOFF_MAX_VERTICAL_SPEED);
    }

    static double arrivalSpeed(double speed, double distance, double arrivalDistance, DragonFlightRequest.Arrival arrival) {
        if (arrival == DragonFlightRequest.Arrival.PASS_THROUGH) return speed;
        double exitSpeed = speed * 0.3D;
        double brakingDistance = Math.max(0.0D, distance - arrivalDistance);
        return Math.min(speed, Math.sqrt(exitSpeed * exitSpeed + 2.0D * MAX_ACCELERATION * brakingDistance));
    }

    static double turnSpeed(double speed, double alignment, double distance) {
        speed *= Mth.clamp((alignment + 1.0D) * 0.5D, 0.35D, 1.0D);
        return alignment < 0.7D ? Math.min(speed, Math.max(0.12D, distance * 0.08D)) : speed;
    }

    static double sprintAcceleration(double speed) {
        // Fast combat flight needs enough thrust to overcome travel drag.
        return Mth.clamp(speed * 0.10D, MAX_ACCELERATION, 0.36D);
    }

    static Vec3 limitAcceleration(Vec3 current, Vec3 desired) {
        return limitAcceleration(current, desired, MAX_ACCELERATION);
    }

    static Vec3 limitAcceleration(Vec3 current, Vec3 desired, double acceleration) {
        Vec3 change = desired.subtract(current);
        return change.lengthSqr() > acceleration * acceleration
                ? current.add(change.normalize().scale(acceleration)) : desired;
    }

    static Vec3 limitDiveAcceleration(Vec3 current, Vec3 desired) {
        return limitDiveAcceleration(current, desired, MAX_ACCELERATION);
    }

    static Vec3 limitDiveAcceleration(Vec3 current, Vec3 desired, double horizontalAcceleration) {
        double dx = desired.x - current.x;
        double dz = desired.z - current.z;
        double horizontalChangeSq = dx * dx + dz * dz;
        double limitedX = desired.x;
        double limitedZ = desired.z;
        if (horizontalChangeSq > horizontalAcceleration * horizontalAcceleration) {
            double scale = horizontalAcceleration / Math.sqrt(horizontalChangeSq);
            limitedX = current.x + dx * scale;
            limitedZ = current.z + dz * scale;
        }
        double dy = desired.y - current.y;
        double limitedY = Math.abs(dy) > DIVE_VERTICAL_ACCELERATION
                ? current.y + Math.copySign(DIVE_VERTICAL_ACCELERATION, dy)
                : desired.y;
        return new Vec3(limitedX, limitedY, limitedZ);
    }
}
