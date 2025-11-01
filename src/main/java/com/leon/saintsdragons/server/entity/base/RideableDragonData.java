package com.leon.saintsdragons.server.entity.base;

/**
 * Collective DATA fields and constants for all rideable dragons.
 * This class provides standardized EntityDataAccessor field definitions that each rideable dragon
 * can use to ensure consistent animation behavior and prevent thrashing issues.
 * Usage: Each entity should define its own EntityDataAccessor instances using these patterns.
 */
public class RideableDragonData {
    
    // ===== ANIMATION THRESHOLDS (Standardized for consistent behavior) =====
    
    /** Minimum velocity threshold for walking animation */
    public static final double WALK_MIN_VELOCITY = 0.0008;
    
    /** Minimum velocity threshold for running animation */
    public static final double RUN_MIN_VELOCITY = 0.0200;
    
    /** Minimum rider input magnitude to trigger movement */
    public static final float RIDER_INPUT_THRESHOLD = 0.05f;
    
    /** Minimum rider input to register (prevents noise) */
    public static final float RIDER_INPUT_MIN = 0.02f;
    
    /** Rider input decay rate per tick */
    public static final float RIDER_INPUT_DECAY = 0.8f;
    
    /** Minimum rider input before zeroing */
    public static final float RIDER_INPUT_ZERO_THRESHOLD = 0.01f;
    
    // ===== VELOCITY THRESHOLDS FOR RIDDEN FALLBACK =====
    
    /** High velocity threshold for running while ridden */
    public static final double RIDDEN_RUN_VELOCITY = 0.08;
    
    /** Low velocity threshold for walking while ridden */
    public static final double RIDDEN_WALK_VELOCITY = 0.005;
    
    // ===== HELPER METHODS =====
    
    /**
     * Check if rider input magnitude is significant enough to trigger movement
     */
    public static boolean isSignificantRiderInput(float forward, float strafe) {
        return Math.abs(forward) + Math.abs(strafe) > RIDER_INPUT_THRESHOLD;
    }
    
    /**
     * Apply input threshold to prevent noise
     */
    public static float applyInputThreshold(float input) {
        return Math.abs(input) > RIDER_INPUT_MIN ? input : 0f;
    }
    
    /**
     * Decay rider input over time
     */
    public static float decayRiderInput(float input) {
        float decayed = input * RIDER_INPUT_DECAY;
        return Math.abs(decayed) < RIDER_INPUT_ZERO_THRESHOLD ? 0f : decayed;
    }
    
    /**
     * Determine ground movement state based on velocity
     */
    public static int getGroundStateFromVelocity(double velocitySqr) {
        if (velocitySqr > RUN_MIN_VELOCITY) return 2; // running
        if (velocitySqr > WALK_MIN_VELOCITY) return 1; // walking
        return 0; // idle
    }
    
    /**
     * Determine ground movement state while ridden based on velocity
     */
    public static int getRiddenGroundStateFromVelocity(double velocitySqr) {
        if (velocitySqr > RIDDEN_RUN_VELOCITY) return 2; // running
        if (velocitySqr > RIDDEN_WALK_VELOCITY) return 1; // walking
        return 0; // idle
    }
    
    // Private constructor to prevent instantiation
    private RideableDragonData() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
