package com.leon.saintsdragons.server.entity.base;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;

/**
 * Collective DATA fields and constants for all rideable dragons.
 * This class provides standardized EntityDataAccessor field definitions that each rideable dragon
 * can use to ensure consistent animation behavior and prevent thrashing issues.
 * 
 * Usage: Each entity should define its own EntityDataAccessor instances using these patterns.
 */
public class RideableDragonData {
    
    // ===== CORE FLIGHT & MOVEMENT DATA PATTERNS (Essential for all rideable dragons) =====
    
    /** Helper method to create flying state accessor for a specific entity class */
    public static <T extends Entity> EntityDataAccessor<Boolean> createFlyingAccessor(Class<T> entityClass) {
        return SynchedEntityData.defineId(entityClass, EntityDataSerializers.BOOLEAN);
    }
    
    /** Helper method to create takeoff state accessor for a specific entity class */
    public static <T extends Entity> EntityDataAccessor<Boolean> createTakeoffAccessor(Class<T> entityClass) {
        return SynchedEntityData.defineId(entityClass, EntityDataSerializers.BOOLEAN);
    }
    
    /** Helper method to create hovering state accessor for a specific entity class */
    public static <T extends Entity> EntityDataAccessor<Boolean> createHoveringAccessor(Class<T> entityClass) {
        return SynchedEntityData.defineId(entityClass, EntityDataSerializers.BOOLEAN);
    }
    
    /** Helper method to create landing state accessor for a specific entity class */
    public static <T extends Entity> EntityDataAccessor<Boolean> createLandingAccessor(Class<T> entityClass) {
        return SynchedEntityData.defineId(entityClass, EntityDataSerializers.BOOLEAN);
    }
    
    /** Helper method to create running state accessor for a specific entity class */
    public static <T extends Entity> EntityDataAccessor<Boolean> createRunningAccessor(Class<T> entityClass) {
        return SynchedEntityData.defineId(entityClass, EntityDataSerializers.BOOLEAN);
    }
    
    /** Helper method to create ground move state accessor for a specific entity class */
    public static <T extends Entity> EntityDataAccessor<Integer> createGroundMoveStateAccessor(Class<T> entityClass) {
        return SynchedEntityData.defineId(entityClass, EntityDataSerializers.INT);
    }
    
    /** Helper method to create flight mode accessor for a specific entity class */
    public static <T extends Entity> EntityDataAccessor<Integer> createFlightModeAccessor(Class<T> entityClass) {
        return SynchedEntityData.defineId(entityClass, EntityDataSerializers.INT);
    }
    
    // ===== RIDER INPUT DATA PATTERNS (Critical for preventing animation thrashing) =====
    
    /** Helper method to create rider forward input accessor for a specific entity class */
    public static <T extends Entity> EntityDataAccessor<Float> createRiderForwardAccessor(Class<T> entityClass) {
        return SynchedEntityData.defineId(entityClass, EntityDataSerializers.FLOAT);
    }
    
    /** Helper method to create rider strafe input accessor for a specific entity class */
    public static <T extends Entity> EntityDataAccessor<Float> createRiderStrafeAccessor(Class<T> entityClass) {
        return SynchedEntityData.defineId(entityClass, EntityDataSerializers.FLOAT);
    }
    
    /** Helper method to create going up state accessor for a specific entity class */
    public static <T extends Entity> EntityDataAccessor<Boolean> createGoingUpAccessor(Class<T> entityClass) {
        return SynchedEntityData.defineId(entityClass, EntityDataSerializers.BOOLEAN);
    }
    
    /** Helper method to create going down state accessor for a specific entity class */
    public static <T extends Entity> EntityDataAccessor<Boolean> createGoingDownAccessor(Class<T> entityClass) {
        return SynchedEntityData.defineId(entityClass, EntityDataSerializers.BOOLEAN);
    }
    
    /** Helper method to create accelerating state accessor for a specific entity class */
    public static <T extends Entity> EntityDataAccessor<Boolean> createAcceleratingAccessor(Class<T> entityClass) {
        return SynchedEntityData.defineId(entityClass, EntityDataSerializers.BOOLEAN);
    }

    /** Helper method to create sleeping state accessor for a specific entity class */
    public static <T extends Entity> EntityDataAccessor<Boolean> createSleepingAccessor(Class<T> entityClass) {
        return SynchedEntityData.defineId(entityClass, EntityDataSerializers.BOOLEAN);
    }
    
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
