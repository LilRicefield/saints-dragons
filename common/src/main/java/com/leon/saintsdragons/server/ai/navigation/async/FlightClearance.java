package com.leon.saintsdragons.server.ai.navigation.async;

import net.minecraft.world.phys.AABB;

public record FlightClearance(double floor, double ceiling, boolean floorKnown,
                              boolean ceilingKnown, boolean clear) {
    public static FlightClearance measure(AABB body, double lowerLimit, double upperLimit,
                                          Iterable<AABB> obstacles) {
        double floor = lowerLimit;
        double ceiling = upperLimit;
        boolean floorKnown = false;
        boolean ceilingKnown = false;
        boolean clear = true;
        for (AABB obstacle : obstacles) {
            if (obstacle.maxX <= body.minX || obstacle.minX >= body.maxX
                    || obstacle.maxZ <= body.minZ || obstacle.minZ >= body.maxZ) continue;
            if (obstacle.maxY <= body.minY + 1.0E-4D) {
                if (obstacle.maxY >= floor) {
                    floor = obstacle.maxY;
                    floorKnown = true;
                }
            } else if (obstacle.minY >= body.maxY - 1.0E-4D) {
                if (obstacle.minY <= ceiling) {
                    ceiling = obstacle.minY;
                    ceilingKnown = true;
                }
            } else {
                clear = false;
            }
        }
        return new FlightClearance(floor, ceiling, floorKnown, ceilingKnown, clear);
    }

    public double fitHeight(double desiredFeetY, double bodyHeight, double floorMargin, double ceilingMargin) {
        double minimum = floor + floorMargin;
        double maximum = ceiling - bodyHeight - ceilingMargin;
        return !clear || minimum > maximum ? Double.NaN : Math.max(minimum, Math.min(maximum, desiredFeetY));
    }
}
