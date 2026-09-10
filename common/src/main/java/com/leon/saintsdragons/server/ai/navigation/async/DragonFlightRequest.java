package com.leon.saintsdragons.server.ai.navigation.async;

import java.util.Objects;
import net.minecraft.world.phys.Vec3;

public record DragonFlightRequest(Vec3 target, double speedModifier, Purpose purpose,
                                  double arrivalTolerance, Arrival arrival) {
    public DragonFlightRequest {
        Objects.requireNonNull(target);
        Objects.requireNonNull(purpose);
        Objects.requireNonNull(arrival);
        if (!Double.isFinite(target.x) || !Double.isFinite(target.y) || !Double.isFinite(target.z)
                || !Double.isFinite(speedModifier) || speedModifier < 0.0D
                || !Double.isFinite(arrivalTolerance) || arrivalTolerance < 0.0D) {
            throw new IllegalArgumentException("Flight requests require finite positions and nonnegative speed/tolerance");
        }
    }

    public static DragonFlightRequest cruise(Vec3 target, double speed) {
        return new DragonFlightRequest(target, speed, Purpose.CRUISE, 0.0D, Arrival.BRAKE);
    }

    public static DragonFlightRequest track(Vec3 target, double speed) {
        return track(target, speed, 0.0D);
    }

    public static DragonFlightRequest track(Vec3 target, double speed, double tolerance) {
        return new DragonFlightRequest(target, speed, Purpose.TRACK, tolerance, Arrival.BRAKE);
    }

    public static DragonFlightRequest chase(Vec3 target, double speed) {
        return new DragonFlightRequest(target, speed, Purpose.TRACK, 0.0D, Arrival.PASS_THROUGH);
    }

    public static DragonFlightRequest dive(Vec3 target, double speed) {
        return new DragonFlightRequest(target, speed, Purpose.DIVE, 0.0D, Arrival.PASS_THROUGH);
    }

    public static DragonFlightRequest maneuver(Vec3 target, double speed, double tolerance, Arrival arrival) {
        return new DragonFlightRequest(target, speed, Purpose.MANEUVER, tolerance, arrival);
    }

    public DragonFlightRequest withTarget(Vec3 target) {
        return new DragonFlightRequest(target, speedModifier, purpose, arrivalTolerance, arrival);
    }

    public boolean requestsSprint() {
        return speedModifier > 0.0D && purpose != Purpose.CRUISE && arrival == Arrival.PASS_THROUGH;
    }

    public double arrivalDistance(double width) {
        return arrivalTolerance > 0.0D ? arrivalTolerance : Math.max(0.75D, 1.5D * Math.max(1.0D, Math.sqrt(width * 2.0D)));
    }

    public enum Purpose { CRUISE, TRACK, MANEUVER, DIVE }
    public enum Arrival { BRAKE, PASS_THROUGH }
}
