package com.leon.saintsdragons.server.flight;

public record DragonRiderFlightSettings(
        double baseSpeed,
        double sprintSpeed,
        double flightAcceleration,
        double diveSpeedMultiplier,
        double diveAcceleration,
        double strafePower,
        double noInputDrag,
        double ascendThrust,
        double descendThrust,
        double verticalSpeedLimit,
        double takeoffBoost
) {
}
