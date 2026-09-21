package com.leon.saintsdragons.server.ai.dragonbrain;

public enum DragonBehaviourInterruption {
    NONE(0, "routine"),
    RETURN_HOME(1, "return-home"),
    OWNER_FOLLOW(2, "owner-follow"),
    WATER_ESCAPE(3, "water-escape");

    private final int priority;
    private final String reason;

    DragonBehaviourInterruption(int priority, String reason) {
        this.priority = priority;
        this.reason = reason;
    }

    public boolean outranks(DragonBehaviourInterruption other) {
        return priority > other.priority;
    }

    public String reason() {
        return reason;
    }
}
