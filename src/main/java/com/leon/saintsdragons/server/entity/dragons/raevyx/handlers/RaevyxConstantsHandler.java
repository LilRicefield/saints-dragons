package com.leon.saintsdragons.server.entity.dragons.raevyx.handlers;

import com.leon.saintsdragons.server.entity.base.RideableDragonData;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import software.bernie.geckolib.core.animation.RawAnimation;

/**
 * Constants for the Lightning Dragon entity.
 * Centralizes all static final values used throughout the LightningDragonEntity class.
 */
public class RaevyxConstantsHandler {

    // ===== SOUND CONSTANTS =====
    
    /** Minimum delay between ambient sounds (in ticks) */
    public static final int MIN_AMBIENT_DELAY = 200;  // 10 seconds
    
    /** Maximum delay between ambient sounds (in ticks) */
    public static final int MAX_AMBIENT_DELAY = 600;  // 30 seconds

    // ===== ANIMATION CONSTANTS =====

    /** Ground idle animation */
    public static final RawAnimation GROUND_IDLE = RawAnimation.begin().thenLoop("animation.raevyx.idle");

    /** Ground walk animation */
    public static final RawAnimation GROUND_WALK = RawAnimation.begin().thenLoop("animation.raevyx.walk");

    /** Ground run animation */
    public static final RawAnimation GROUND_RUN = RawAnimation.begin().thenLoop("animation.raevyx.run");

    // Baby-specific animations
    /** Baby ground idle animation */
    public static final RawAnimation BABY_IDLE = RawAnimation.begin().thenLoop("animation.baby_raevyx.idle");

    /** Baby ground walk animation */
    public static final RawAnimation BABY_WALK = RawAnimation.begin().thenLoop("animation.baby_raevyx.walk");

    /** Baby ground run animation */
    public static final RawAnimation BABY_RUN = RawAnimation.begin().thenLoop("animation.baby_raevyx.run");
    
    /** Sitting animation (looping) */
    public static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.raevyx.sit");
    
    /** Takeoff animation */
    public static final RawAnimation TAKEOFF = RawAnimation.begin().thenPlay("animation.raevyx.takeoff");
    
    /** Flying glide animation */
    public static final RawAnimation FLY_GLIDE = RawAnimation.begin().thenLoop("animation.raevyx.fly_glide");
    
    /** Flying glide down animation (for tamed dragons pitching down) */
    public static final RawAnimation GLIDE_DOWN = RawAnimation.begin().thenLoop("animation.raevyx.glide_down");

    /** Wing flapping animation */
    public static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.raevyx.flap");
    
    /** Landing animation */
    public static final RawAnimation LANDING = RawAnimation.begin().thenPlay("animation.raevyx.landing");
    
    /** Dodge animation */
    public static final RawAnimation DODGE = RawAnimation.begin().thenPlay("animation.raevyx.dodge");

    // ===== MODEL CONSTANTS =====
    
    /** Scale factor for the wyvern model */
    public static final float MODEL_SCALE = 1.0f;

    // ===== SPEED CONSTANTS =====
    
    /** Walking speed */
    public static final double WALK_SPEED = 0.25D;
    
    /** Running speed */
    public static final double RUN_SPEED = 0.45D;

    // ===== ENTITY DATA ACCESSORS =====
    // Lightning Dragon specific entity data accessors using helper methods from RideableDragonData
    
    /** Entity data accessor for flying state */
    public static final EntityDataAccessor<Boolean> DATA_FLYING =
            RideableDragonData.createFlyingAccessor(Raevyx.class);
    
    /** Entity data accessor for takeoff state */
    public static final EntityDataAccessor<Boolean> DATA_TAKEOFF =
            RideableDragonData.createTakeoffAccessor(Raevyx.class);
    
    /** Entity data accessor for hovering state */
    public static final EntityDataAccessor<Boolean> DATA_HOVERING =
            RideableDragonData.createHoveringAccessor(Raevyx.class);
    
    /** Entity data accessor for landing state */
    public static final EntityDataAccessor<Boolean> DATA_LANDING =
            RideableDragonData.createLandingAccessor(Raevyx.class);
    
    /** Entity data accessor for running state */
    public static final EntityDataAccessor<Boolean> DATA_RUNNING =
            RideableDragonData.createRunningAccessor(Raevyx.class);
    
    /** Entity data accessor for ground move state (0=idle, 1=walk, 2=run) */
    public static final EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE =
            RideableDragonData.createGroundMoveStateAccessor(Raevyx.class);
    
    /** Entity data accessor for flight mode (0=glide,1=forward,2=hover,3=takeoff,-1=ground) */
    public static final EntityDataAccessor<Integer> DATA_FLIGHT_MODE =
            RideableDragonData.createFlightModeAccessor(Raevyx.class);
    
    /** Entity data accessor for rider forward input */
    public static final EntityDataAccessor<Float> DATA_RIDER_FORWARD =
            RideableDragonData.createRiderForwardAccessor(Raevyx.class);
    
    /** Entity data accessor for rider strafe input */
    public static final EntityDataAccessor<Float> DATA_RIDER_STRAFE =
            RideableDragonData.createRiderStrafeAccessor(Raevyx.class);
    
    /** Entity data accessor for attack kind */
    public static final EntityDataAccessor<Integer> DATA_ATTACK_KIND =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.INT);
    
    /** Entity data accessor for attack phase */
    public static final EntityDataAccessor<Integer> DATA_ATTACK_PHASE =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.INT);
    
    /** Entity data accessor for attack state (Cataclysm-style simple state system) */
    public static final EntityDataAccessor<Integer> DATA_ATTACK_STATE =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.INT);
    
    /** Entity data accessor for screen shake amount */
    public static final EntityDataAccessor<Float> DATA_SCREEN_SHAKE_AMOUNT =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    
    /** Entity data accessor for beaming state */
    public static final EntityDataAccessor<Boolean> DATA_BEAMING =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for rider landing blend active state */
    public static final EntityDataAccessor<Boolean> DATA_RIDER_LANDING_BLEND =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for rider controls locked state */
    public static final EntityDataAccessor<Boolean> DATA_RIDER_LOCKED =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for beam end position set flag */
    public static final EntityDataAccessor<Boolean> DATA_BEAM_END_SET =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);
    
    /** Entity data accessor for beam end X coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_END_X =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    
    /** Entity data accessor for beam end Y coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_END_Y =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    
    /** Entity data accessor for beam end Z coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_END_Z =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    
    /** Entity data accessor for beam start position set flag */
    public static final EntityDataAccessor<Boolean> DATA_BEAM_START_SET =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);
    
    /** Entity data accessor for beam start X coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_START_X =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    
    /** Entity data accessor for beam start Y coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_START_Y =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    
    /** Entity data accessor for beam start Z coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_START_Z =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    
    /** Entity data accessor for going up state */
    public static final EntityDataAccessor<Boolean> DATA_GOING_UP =
            RideableDragonData.createGoingUpAccessor(Raevyx.class);
    
    /** Entity data accessor for going down state */
    public static final EntityDataAccessor<Boolean> DATA_GOING_DOWN =
            RideableDragonData.createGoingDownAccessor(Raevyx.class);
    
    /** Entity data accessor for accelerating state */
    public static final EntityDataAccessor<Boolean> DATA_ACCELERATING =
            RideableDragonData.createAcceleratingAccessor(Raevyx.class);
    
    /** Entity data accessor for sleeping state */
    public static final EntityDataAccessor<Boolean> DATA_SLEEPING =
            RideableDragonData.createSleepingAccessor(Raevyx.class);

    /** Entity data accessor for sleep enter transition state */
    public static final EntityDataAccessor<Boolean> DATA_SLEEPING_ENTERING =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for sleep exit transition state */
    public static final EntityDataAccessor<Boolean> DATA_SLEEPING_EXITING =
            SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);

    // ===== AI CONSTANTS =====
    
    /** Time to live for aggression tracking (in ticks) */
    public static final int AGGRO_TTL_TICKS = 200; // ~10s
    
    // ===== ATTACK STATE CONSTANTS =====
    
    /** Attack state: Idle/moving */
    public static final int ATTACK_STATE_IDLE = 0;
    
    /** Attack state: Horn gore windup */
    public static final int ATTACK_STATE_HORN_WINDUP = 1;
    
    /** Attack state: Horn gore active */
    public static final int ATTACK_STATE_HORN_ACTIVE = 2;
    
    /** Attack state: Bite windup */
    public static final int ATTACK_STATE_BITE_WINDUP = 3;
    
    /** Attack state: Bite active */
    public static final int ATTACK_STATE_BITE_ACTIVE = 4;
    
    /** Attack state: Recovery cooldown */
    public static final int ATTACK_STATE_RECOVERY = 5;

    // Private constructor to prevent instantiation
    private RaevyxConstantsHandler() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
