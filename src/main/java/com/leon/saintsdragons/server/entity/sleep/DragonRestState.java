package com.leon.saintsdragons.server.entity.sleep;

/**
 * Enum representing the rest/sleep state machine for dragons.
 * This is saved to NBT so dragons can resume their rest cycle after world reload.
 *
 * Animation sequence: IDLE → SITTING_DOWN → SITTING → FALLING_ASLEEP → SLEEPING →
 *                     WAKING_UP → SITTING_AFTER → STANDING_UP → IDLE
 */
public enum DragonRestState {
    /**
     * Default state - dragon is not resting
     */
    IDLE(0),

    /**
     * Playing down → sit animation
     */
    SITTING_DOWN(1),

    /**
     * In sit idle pose, may transition to sleep
     */
    SITTING(2),

    /**
     * Transitioning from sit to sleep (fall_asleep animation)
     */
    FALLING_ASLEEP(3),

    /**
     * In sleep loop animation
     */
    SLEEPING(4),

    /**
     * Transitioning from sleep to sit (wake_up animation)
     */
    WAKING_UP(5),

    /**
     * Back in sit pose after waking, before standing
     */
    SITTING_AFTER(6),

    /**
     * Playing up → idle animation
     */
    STANDING_UP(7);

    private final int id;

    DragonRestState(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /**
     * Get rest state from NBT ID
     */
    public static DragonRestState fromId(int id) {
        for (DragonRestState state : values()) {
            if (state.id == id) {
                return state;
            }
        }
        return IDLE;
    }

    /**
     * Check if this state is part of the sleep cycle (not idle/sitting for other reasons)
     */
    public boolean isRestCycle() {
        return this != IDLE;
    }

    /**
     * Check if this state should keep the dragon immobile
     */
    public boolean shouldStayStill() {
        return this != IDLE;
    }
}
