package com.leon.saintsdragons.server.ai.dragonbrain.tactical;

public enum DragonTactic {
    NONE(0),
    GUARD(1),
    GROUND_PURSUIT(4),
    WATER_PURSUIT(5),
    AERIAL_PURSUIT(6),
    LANDING_APPROACH(7),
    INVESTIGATE(8),
    RETREAT(10);

    private final int tiePriority;

    DragonTactic(int tiePriority) {
        this.tiePriority = tiePriority;
    }

    public int tiePriority() {
        return tiePriority;
    }
}
