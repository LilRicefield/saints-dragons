package com.leon.saintsdragons.server.entity.base;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

/**
 * Collective DATA fields and constants for all rideable dragons.
 * This class provides standardized EntityDataAccessor fields that all rideable dragons can use
 * to ensure consistent animation behavior and prevent thrashing issues.
 * 
 * Usage: Extend this class in your dragon's constants handler or use the fields directly.
 */
public class RideableDragonData {
    
    // ===== CORE FLIGHT & MOVEMENT DATA (Essential for all rideable dragons) =====
    
    /** Entity data accessor for flying state */
    public static final EntityDataAccessor<Boolean> DATA_FLYING = 
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    
    /** Entity data accessor for takeoff state */
    public static final EntityDataAccessor<Boolean> DATA_TAKEOFF = 
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    
    /** Entity data accessor for hovering state */
    public static final EntityDataAccessor<Boolean> DATA_HOVERING = 
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    
    /** Entity data accessor for landing state */
    public static final EntityDataAccessor<Boolean> DATA_LANDING = 
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    
    /** Entity data accessor for running state */
    public static final EntityDataAccessor<Boolean> DATA_RUNNING = 
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    
    /** Entity data accessor for ground move state (0=idle, 1=walk, 2=run) */
    public static final EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE = 
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.INT);
    
    /** Entity data accessor for flight mode (0=glide,1=forward,2=hover,3=takeoff,-1=ground) */
    public static final EntityDataAccessor<Integer> DATA_FLIGHT_MODE = 
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.INT);
    
    // ===== RIDER INPUT DATA (Critical for preventing animation thrashing) =====
    
    /** Entity data accessor for rider forward input */
    public static final EntityDataAccessor<Float> DATA_RIDER_FORWARD = 
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.FLOAT);
    
    /** Entity data accessor for rider strafe input */
    public static final EntityDataAccessor<Float> DATA_RIDER_STRAFE = 
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.FLOAT);
    
    /** Entity data accessor for going up state (rider control) */
    public static final EntityDataAccessor<Boolean> DATA_GOING_UP = 
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    
    /** Entity data accessor for going down state (rider control) */
    public static final EntityDataAccessor<Boolean> DATA_GOING_DOWN = 
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    
    /** Entity data accessor for accelerating state */
    public static final EntityDataAccessor<Boolean> DATA_ACCELERATING = 
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    
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
