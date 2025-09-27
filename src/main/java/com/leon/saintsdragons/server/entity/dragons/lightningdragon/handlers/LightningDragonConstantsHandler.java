package com.leon.saintsdragons.server.entity.dragons.lightningdragon.handlers;

import com.leon.saintsdragons.server.entity.base.RideableDragonData;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import software.bernie.geckolib.core.animation.RawAnimation;

/**
 * Constants for the Lightning Dragon entity.
 * Centralizes all static final values used throughout the LightningDragonEntity class.
 */
public class LightningDragonConstantsHandler {

    // ===== SOUND CONSTANTS =====
    
    /** Minimum delay between ambient sounds (in ticks) */
    public static final int MIN_AMBIENT_DELAY = 200;  // 10 seconds
    
    /** Maximum delay between ambient sounds (in ticks) */
    public static final int MAX_AMBIENT_DELAY = 600;  // 30 seconds

    // ===== ANIMATION CONSTANTS =====
    
    /** Ground idle animation */
    public static final RawAnimation GROUND_IDLE = RawAnimation.begin().thenLoop("animation.lightning_dragon.ground_idle");
    
    /** Ground walk animation */
    public static final RawAnimation GROUND_WALK = RawAnimation.begin().thenLoop("animation.lightning_dragon.walk");
    
    /** Ground run animation */
    public static final RawAnimation GROUND_RUN = RawAnimation.begin().thenLoop("animation.lightning_dragon.run");
    
    /** Sitting animation */
    public static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.lightning_dragon.sit");
    
    /** Takeoff animation */
    public static final RawAnimation TAKEOFF = RawAnimation.begin().thenPlay("animation.lightning_dragon.takeoff");
    
    /** Flying glide animation */
    public static final RawAnimation FLY_GLIDE = RawAnimation.begin().thenLoop("animation.lightning_dragon.fly_gliding");
    
    /** Flying glide down animation (for tamed dragons pitching down) */
    public static final RawAnimation GLIDE_DOWN = RawAnimation.begin().thenLoop("animation.lightning_dragon.glide_down");
    
    /** Flying forward animation */
    public static final RawAnimation FLY_FORWARD = RawAnimation.begin().thenLoop("animation.lightning_dragon.fly_forward");
    
    /** Wing flapping animation */
    public static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.lightning_dragon.flap");
    
    /** Landing animation */
    public static final RawAnimation LANDING = RawAnimation.begin().thenPlay("animation.lightning_dragon.landing");
    
    /** Dodge animation */
    public static final RawAnimation DODGE = RawAnimation.begin().thenPlay("animation.lightning_dragon.dodge");

    // ===== MODEL CONSTANTS =====
    
    /** Scale factor for the dragon model */
    public static final float MODEL_SCALE = 4.5f;

    // ===== SPEED CONSTANTS =====
    
    /** Walking speed */
    public static final double WALK_SPEED = 0.25D;
    
    /** Running speed */
    public static final double RUN_SPEED = 0.45D;

    // ===== ENTITY DATA ACCESSORS =====
    // Lightning Dragon specific entity data accessors using helper methods from RideableDragonData
    
    /** Entity data accessor for flying state */
    public static final EntityDataAccessor<Boolean> DATA_FLYING =
            RideableDragonData.createFlyingAccessor(LightningDragonEntity.class);
    
    /** Entity data accessor for takeoff state */
    public static final EntityDataAccessor<Boolean> DATA_TAKEOFF =
            RideableDragonData.createTakeoffAccessor(LightningDragonEntity.class);
    
    /** Entity data accessor for hovering state */
    public static final EntityDataAccessor<Boolean> DATA_HOVERING =
            RideableDragonData.createHoveringAccessor(LightningDragonEntity.class);
    
    /** Entity data accessor for landing state */
    public static final EntityDataAccessor<Boolean> DATA_LANDING =
            RideableDragonData.createLandingAccessor(LightningDragonEntity.class);
    
    /** Entity data accessor for running state */
    public static final EntityDataAccessor<Boolean> DATA_RUNNING =
            RideableDragonData.createRunningAccessor(LightningDragonEntity.class);
    
    /** Entity data accessor for ground move state (0=idle, 1=walk, 2=run) */
    public static final EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE =
            RideableDragonData.createGroundMoveStateAccessor(LightningDragonEntity.class);
    
    /** Entity data accessor for flight mode (0=glide,1=forward,2=hover,3=takeoff,-1=ground) */
    public static final EntityDataAccessor<Integer> DATA_FLIGHT_MODE =
            RideableDragonData.createFlightModeAccessor(LightningDragonEntity.class);
    
    /** Entity data accessor for rider forward input */
    public static final EntityDataAccessor<Float> DATA_RIDER_FORWARD =
            RideableDragonData.createRiderForwardAccessor(LightningDragonEntity.class);
    
    /** Entity data accessor for rider strafe input */
    public static final EntityDataAccessor<Float> DATA_RIDER_STRAFE =
            RideableDragonData.createRiderStrafeAccessor(LightningDragonEntity.class);
    
    /** Entity data accessor for attack kind */
    public static final EntityDataAccessor<Integer> DATA_ATTACK_KIND =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.INT);
    
    /** Entity data accessor for attack phase */
    public static final EntityDataAccessor<Integer> DATA_ATTACK_PHASE =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.INT);
    
    /** Entity data accessor for attack state (Cataclysm-style simple state system) */
    public static final EntityDataAccessor<Integer> DATA_ATTACK_STATE =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.INT);
    
    /** Entity data accessor for screen shake amount */
    public static final EntityDataAccessor<Float> DATA_SCREEN_SHAKE_AMOUNT =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.FLOAT);
    
    /** Entity data accessor for beaming state */
    public static final EntityDataAccessor<Boolean> DATA_BEAMING =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.BOOLEAN);
    
    /** Entity data accessor for beam end position set flag */
    public static final EntityDataAccessor<Boolean> DATA_BEAM_END_SET =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.BOOLEAN);
    
    /** Entity data accessor for beam end X coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_END_X =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.FLOAT);
    
    /** Entity data accessor for beam end Y coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_END_Y =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.FLOAT);
    
    /** Entity data accessor for beam end Z coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_END_Z =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.FLOAT);
    
    /** Entity data accessor for beam start position set flag */
    public static final EntityDataAccessor<Boolean> DATA_BEAM_START_SET =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.BOOLEAN);
    
    /** Entity data accessor for beam start X coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_START_X =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.FLOAT);
    
    /** Entity data accessor for beam start Y coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_START_Y =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.FLOAT);
    
    /** Entity data accessor for beam start Z coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_START_Z =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.FLOAT);
    
    /** Entity data accessor for going up state */
    public static final EntityDataAccessor<Boolean> DATA_GOING_UP =
            RideableDragonData.createGoingUpAccessor(LightningDragonEntity.class);
    
    /** Entity data accessor for going down state */
    public static final EntityDataAccessor<Boolean> DATA_GOING_DOWN =
            RideableDragonData.createGoingDownAccessor(LightningDragonEntity.class);
    
    /** Entity data accessor for accelerating state */
    public static final EntityDataAccessor<Boolean> DATA_ACCELERATING =
            RideableDragonData.createAcceleratingAccessor(LightningDragonEntity.class);
    
    /** Entity data accessor for sleeping state */
    public static final EntityDataAccessor<Boolean> DATA_SLEEPING =
            RideableDragonData.createSleepingAccessor(LightningDragonEntity.class);

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

    /** Attack state: Summon Storm windup */
    public static final int ATTACK_STATE_SUMMON_STORM_WINDUP = 6;

    /** Attack state: Summon Storm active */
    public static final int ATTACK_STATE_SUMMON_STORM_ACTIVE = 7;

    /** Attack state: Summon Storm recovery */
    public static final int ATTACK_STATE_SUMMON_STORM_RECOVERY = 8;

    // Private constructor to prevent instantiation
    private LightningDragonConstantsHandler() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
