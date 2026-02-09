package com.leon.saintsdragons.server.ai.goals.base;

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
                60,
                64.0,
                400,
                10,
                10,
                10,
                30,
                30,
                30,
                3000,
                3000,
                3000
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
                8,
                2,
                80,
                8,
                4,
                200,
                1800,
                3000
        );
    }
}
