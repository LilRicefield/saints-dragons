package com.leon.saintsdragons.server.entity.interfaces;

/**
 * Interface for dragons that can sleep.
 * Defines the minimum requirements for sleep behaviors.
 */
public interface DragonSleepCapable {
    
    /**
     * Check if the dragon is currently sleeping
     */
    boolean isSleeping();
    
    /**
     * Check if the dragon is transitioning to/from sleep
     */
    boolean isSleepTransitioning();
    
    /**
     * Check if sleep is suppressed (e.g., during combat)
     */
    boolean isSleepSuppressed();
    
    /**
     * Start the sleep enter animation/state
     */
    void startSleepEnter();
    
    /**
     * Start the sleep exit animation/state
     */
    void startSleepExit();
    
    /**
     * Get the dragon's preferred sleep conditions
     */
    SleepPreferences getSleepPreferences();
    
    /**
     * Check if the dragon can sleep in current conditions
     */
    boolean canSleepNow();
    
    /**
     * Sleep preferences for different dragon types
     */
    record SleepPreferences(
        boolean canSleepAtNight,      // Can sleep during night
        boolean canSleepDuringDay,    // Can sleep during day
        boolean requiresShelter,       // Needs shelter to sleep
        boolean avoidsThunderstorms,   // Won't sleep during storms
        boolean sleepsNearOwner        // Prefers sleeping near owner
    ) {}
}
