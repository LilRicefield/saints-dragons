package com.leon.saintsdragons.server.entity.sleep;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/**
 * Manages the persistent rest/sleep state machine for dragons.
 * Handles state transitions, timing, and NBT serialization so dragons can
 * properly resume their rest cycle after world reload.
 *
 * This solves the problem where AI goals lose their internal state on reload,
 * causing dragons to get stuck sleeping or sitting forever.
 */
public class DragonRestManager {

    private final LivingEntity entity;

    private DragonRestState currentState = DragonRestState.IDLE;
    private int stateTimer = 0;
    private int restDuration = 0; // How long to sleep in SLEEPING state
    private int restingTicks = 0; // Tick counter during SLEEPING

    public DragonRestManager(LivingEntity entity) {
        this.entity = entity;
    }

    /**
     * Get the current rest state
     */
    public DragonRestState getCurrentState() {
        return currentState;
    }

    /**
     * Check if the dragon is currently in a rest cycle
     */
    public boolean isResting() {
        return currentState.isRestCycle();
    }

    /**
     * Check if the dragon is in the sleeping state
     */
    public boolean isSleepingState() {
        return currentState == DragonRestState.SLEEPING;
    }

    /**
     * Start a new rest cycle with the given sleep duration
     */
    public void startRest(int sleepDurationTicks) {
        if (currentState != DragonRestState.IDLE) {
            return; // Already resting
        }
        currentState = DragonRestState.SITTING_DOWN;
        stateTimer = 0;
        restDuration = sleepDurationTicks;
        restingTicks = 0;
    }

    /**
     * Force stop the rest cycle and return to idle
     */
    public void stopRest() {
        currentState = DragonRestState.IDLE;
        stateTimer = 0;
        restDuration = 0;
        restingTicks = 0;
    }

    /**
     * Tick the state machine forward
     * Returns true if a state transition occurred
     */
    public boolean tick() {
        if (currentState == DragonRestState.IDLE) {
            return false;
        }

        stateTimer++;
        return false; // State machine logic handled externally by entity
    }

    /**
     * Get the current state timer
     */
    public int getStateTimer() {
        return stateTimer;
    }

    /**
     * Reset the state timer (used when transitioning to a new state)
     */
    public void resetStateTimer() {
        stateTimer = 0;
    }

    /**
     * Advance to the next state in the rest cycle
     */
    public void advanceState() {
        switch (currentState) {
            case SITTING_DOWN -> currentState = DragonRestState.SITTING;
            case SITTING -> currentState = DragonRestState.FALLING_ASLEEP;
            case FALLING_ASLEEP -> currentState = DragonRestState.SLEEPING;
            case SLEEPING -> currentState = DragonRestState.WAKING_UP;
            case WAKING_UP -> currentState = DragonRestState.SITTING_AFTER;
            case SITTING_AFTER -> currentState = DragonRestState.STANDING_UP;
            case STANDING_UP -> currentState = DragonRestState.IDLE;
            default -> currentState = DragonRestState.IDLE;
        }
        stateTimer = 0;
    }

    /**
     * Get the resting tick counter (only counts during SLEEPING state)
     */
    public int getRestingTicks() {
        return restingTicks;
    }

    /**
     * Increment the resting tick counter
     */
    public void incrementRestingTicks() {
        restingTicks++;
    }

    /**
     * Get the rest duration
     */
    public int getRestDuration() {
        return restDuration;
    }

    /**
     * Check if the rest cycle is complete (reached IDLE again)
     */
    public boolean isRestCycleComplete() {
        return currentState == DragonRestState.IDLE;
    }

    /**
     * Save rest state to NBT
     */
    public void save(CompoundTag tag) {
        tag.putInt("RestState", currentState.getId());
        tag.putInt("RestStateTimer", stateTimer);
        tag.putInt("RestDuration", restDuration);
        tag.putInt("RestingTicks", restingTicks);
    }

    /**
     * Load rest state from NBT
     */
    public void load(CompoundTag tag) {
        currentState = DragonRestState.fromId(tag.getInt("RestState"));
        stateTimer = tag.getInt("RestStateTimer");
        restDuration = tag.getInt("RestDuration");
        restingTicks = tag.getInt("RestingTicks");
    }
}
