package com.leon.saintsdragons.server.ai;

public record DragonFlightBehaviorProfile(
        int landingCooldownTicks,
        double targetReachedDistanceSq,
        int maxTargetAgeTicks,
        int decisionIntervalClear,
        int decisionIntervalRain,
        int decisionIntervalThunder,
        int takeoffRollClear,
        int takeoffRollRain,
        int takeoffRollThunder,
        int keepFlyingRollClear,
        int keepFlyingRollRain,
        int keepFlyingRollThunder
) {
    public static DragonFlightBehaviorProfile ignivorus() {
        return new DragonFlightBehaviorProfile(
                600,
                25.0,
                400,
                20,
                20,
                20,
                180,
                180,
                180,
                300,
                300,
                300
        );
    }

    public static DragonFlightBehaviorProfile cindervane() {
        return new DragonFlightBehaviorProfile(
                40,
                100.0,
                300,
                8,
                5,
                2,
                40,
                100,
                200,
                3600,
                400,
                200
        );
    }

    public static DragonFlightBehaviorProfile raevyx() {
        return new DragonFlightBehaviorProfile(
                100,
                64.0,
                300,
                25,
                25,
                2,
                80,
                80,
                4,
                200,
                200,
                3000
        );
    }

    public static DragonFlightBehaviorProfile volitans() {
        return new DragonFlightBehaviorProfile(
                600,
                64.0,
                240,
                18,
                18,
                18,
                140,
                140,
                140,
                400,
                400,
                400
        );
    }
}
