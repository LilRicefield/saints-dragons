package com.leon.saintsdragons.server.ai.navigation.async;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class FlightRoutePolicy {
    static final int MIN_REPLAN_INTERVAL_TICKS = 6;
    static final double MAX_ENDPOINT_EXTENSION = 24.0D;

    private FlightRoutePolicy() { }

    static boolean needsNewRoute(Vec3 position, @Nullable Vec3 plannedTarget, Vec3 target) {
        if (plannedTarget == null || plannedTarget.distanceToSqr(target) >= 16.0D
                || Math.abs(plannedTarget.y - target.y) >= 2.0D) return true;
        Vec3 previousDirection = plannedTarget.subtract(position).multiply(1.0D, 0.0D, 1.0D);
        Vec3 direction = target.subtract(position).multiply(1.0D, 0.0D, 1.0D);
        return previousDirection.lengthSqr() >= 1.0D && direction.lengthSqr() >= 1.0D
                && previousDirection.normalize().dot(direction.normalize()) < 0.75D;
    }

    static boolean isNewObjective(Vec3 position, @Nullable DragonFlightRequest previous, DragonFlightRequest next) {
        return previous == null || !samePurpose(previous.purpose(), next.purpose())
                || needsNewRoute(position, previous.target(), next.target());
    }

    static boolean samePurpose(DragonFlightRequest.Purpose first, DragonFlightRequest.Purpose second) {
        return first == second || (pursuit(first) && pursuit(second));
    }

    private static boolean pursuit(DragonFlightRequest.Purpose purpose) {
        return purpose == DragonFlightRequest.Purpose.TRACK || purpose == DragonFlightRequest.Purpose.DIVE;
    }

    static boolean resultStillUseful(Vec3 origin, Vec3 requested, Vec3 active, Vec3 endpoint, double width) {
        if (!needsNewRoute(origin, requested, active)) return true;
        return endpoint.distanceTo(active) + Math.max(1.0D, width * 0.25D) < origin.distanceTo(active);
    }

    static boolean mayRequest(long now, long lastRequestTick, boolean awaitingResult) {
        return !awaitingResult && (lastRequestTick == Long.MIN_VALUE || now < lastRequestTick
                || now - lastRequestTick >= MIN_REPLAN_INTERVAL_TICKS);
    }
}
