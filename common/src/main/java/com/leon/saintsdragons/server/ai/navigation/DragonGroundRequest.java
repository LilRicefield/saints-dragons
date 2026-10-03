package com.leon.saintsdragons.server.ai.navigation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public record DragonGroundRequest(Vec3 target, double speed, boolean running, Arrival arrival, Route route) {
    public DragonGroundRequest {
        Objects.requireNonNull(target);
        Objects.requireNonNull(arrival);
        Objects.requireNonNull(route);
        if (!Double.isFinite(target.x) || !Double.isFinite(target.y) || !Double.isFinite(target.z)
                || !Double.isFinite(speed) || speed <= 0.0D) {
            throw new IllegalArgumentException("Invalid ground movement request");
        }
    }

    public static DragonGroundRequest travel(Vec3 target, double speed, boolean running, Arrival arrival) {
        return new DragonGroundRequest(target, speed, running, arrival, Route.PROGRESSIVE);
    }

    public static DragonGroundRequest complete(Vec3 target, double speed, boolean running, Arrival arrival) {
        return new DragonGroundRequest(target, speed, running, arrival, Route.COMPLETE);
    }

    public enum Route {
        PROGRESSIVE,
        COMPLETE
    }

    public record Arrival(double radius, boolean groundPosition) {
        public Arrival {
            if (!Double.isFinite(radius) || radius < 0.0D) {
                throw new IllegalArgumentException("Invalid ground arrival radius");
            }
            // Avoid requiring exact floating-point equality for a zero-distance walk target.
            radius = Math.max(0.05D, radius);
        }

        // Centre-to-centre distance, also used by behaviours following an entity.
        public static Arrival within(double radius) {
            return new Arrival(radius, false);
        }

        //Horizontal precision with room for the actual floor height (including slabs).
        public static Arrival atPosition(double radius) {
            return new Arrival(radius, true);
        }

        public static Arrival near(Entity dragon) {
            return within(Math.max(1.5D, dragon.getBbWidth() * 0.75D));
        }

        public boolean reached(Vec3 position, Vec3 target) {
            if (!groundPosition) {
                return position.distanceToSqr(target) <= radius * radius;
            }
            double dx = position.x - target.x;
            double dz = position.z - target.z;
            double dy = position.y - target.y;
            return dx * dx + dz * dz <= radius * radius && dy > -1.0D && dy <= 1.5D;
        }
    }
}
