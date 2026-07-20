package com.leon.saintsdragons.server.ai.dragonbrain.tactical;

import com.leon.saintsdragons.server.entity.base.DragonEntity;

public record DragonTacticalProfile(
        int evaluationIntervalTicks,
        int switchMargin,
        float retreatHealthRatio
) {
    private static final DragonTacticalProfile STANDARD = new DragonTacticalProfile(
            10,
            15,
            0.25F
    );

    public static DragonTacticalProfile forDragon(DragonEntity dragon) {
        return STANDARD;
    }

    public int minimumTicks(DragonTactic tactic) {
        return switch (tactic) {
            case NONE -> 20;
            case GUARD -> 60;
            case INVESTIGATE -> 40;
            case GROUND_PURSUIT, WATER_PURSUIT -> 60;
            case AERIAL_PURSUIT, LANDING_APPROACH -> 80;
            case RETREAT -> 100;
        };
    }

    public int maximumTicks(DragonTactic tactic) {
        return switch (tactic) {
            case NONE -> 100;
            case GUARD -> 200;
            case INVESTIGATE -> 160;
            case GROUND_PURSUIT, WATER_PURSUIT -> 200;
            case AERIAL_PURSUIT, LANDING_APPROACH -> 240;
            case RETREAT -> 160;
        };
    }
}
