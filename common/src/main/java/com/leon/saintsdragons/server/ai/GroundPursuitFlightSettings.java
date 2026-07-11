package com.leon.saintsdragons.server.ai;

public record GroundPursuitFlightSettings(int stallTicks,
                                          double progressEpsilon,
                                          double highGroundMinVerticalSeparation,
                                          double highGroundMaxHorizontalDistance,
                                          double minPursuitDistance,
                                          int minAirPursuitTicks,
                                          int landingSearchIntervalTicks,
                                          int landingSearchRadius,
                                          double landingMaxVerticalDelta,
                                          int landingFailureTimeoutTicks) {
    public static GroundPursuitFlightSettings standard() {
        return new GroundPursuitFlightSettings(
                60,
                0.08D,
                4.5D,
                12.0D,
                8.0D,
                40,
                20,
                24,
                3.5D,
                40
        );
    }
}
