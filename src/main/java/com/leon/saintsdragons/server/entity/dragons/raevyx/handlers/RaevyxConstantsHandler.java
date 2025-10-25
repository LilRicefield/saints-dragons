package com.leon.saintsdragons.server.entity.dragons.raevyx.handlers;

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