package com.leon.saintsdragons.server.entity.dragons.raevyx.handlers;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

/**
 * Handles all animation logic for Raevyx
 * Extracted from Raevyx to improve organization and maintainability
 */
public record RaevyxAnimationHandler(Raevyx wyvern) {
    // ===== ANIMATION CONSTANTS =====

    /** Ground idle animation */
    private static final RawAnimation GROUND_IDLE = RawAnimation.begin().thenLoop("animation.raevyx.idle");

    /** Ground walk animation */
    private static final RawAnimation GROUND_WALK = RawAnimation.begin().thenLoop("animation.raevyx.walk");

    /** Ground run animation */
    private static final RawAnimation GROUND_RUN = RawAnimation.begin().thenLoop("animation.raevyx.run");

    /** Sitting animation (looping) */
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.raevyx.sit");

    /** Takeoff animation */
    private static final RawAnimation TAKEOFF = RawAnimation.begin().thenPlay("animation.raevyx.takeoff");

    /** Flying glide animation */
    private static final RawAnimation FLY_GLIDE = RawAnimation.begin().thenLoop("animation.raevyx.fly_glide");

    /** Flying glide down animation (for tamed dragons pitching down) */
    private static final RawAnimation GLIDE_DOWN = RawAnimation.begin().thenLoop("animation.raevyx.glide_down");

    /** Wing flapping animation */
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.raevyx.flap");

    /** Rider hover/idle animation */
    private static final RawAnimation FLY_IDLE = RawAnimation.begin().thenLoop("animation.raevyx.fly_idle");

    /** Sprint flapping animation (rider only) */
    private static final RawAnimation SPRINT_FLAP = RawAnimation.begin().thenLoop("animation.raevyx.sprint_flap");

    /** Landing animation */
    private static final RawAnimation LANDING = RawAnimation.begin().thenPlay("animation.raevyx.landing");

    /** Landed animation (plays after landing completes) */
    private static final RawAnimation LANDED = RawAnimation.begin().thenPlay("animation.raevyx.landed");

    /** Dodge animation */
    private static final RawAnimation DODGE = RawAnimation.begin().thenPlay("animation.raevyx.dodge");

    /** Swim animation (overrides all others when in water) */
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.raevyx.swim");

    /** Taming stun loop (treated like alternate idle) */
    private static final RawAnimation STUNNED = RawAnimation.begin().thenLoop("animation.raevyx.stunned");

    /** Sleep loop animation (applied continuously when sleeping) */
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.raevyx.sleep");

    private static RawAnimation currentFlightAnimation = FLY_GLIDE;

    // ===== ANIMATION TRIGGERS =====

    /**
     * Triggers the sit down transition animation
     */
    public void triggerSitDownAnimation() {
        wyvern.triggerAnim("action", "sit_down");
    }

    /**
     * Triggers the stand up transition animation
     */
    public void triggerSitUpAnimation() {
        wyvern.triggerAnim("action", "sit_up");
    }

    /**
     * Triggers the fall asleep transition animation (sit → sleep)
     */
    public void triggerFallAsleepAnimation() {
        wyvern.triggerAnim("action", "fall_asleep");
    }

    /**
     * Triggers the sleep loop animation
     */
    public void triggerSleepAnimation() {
        wyvern.triggerAnim("action", "sleep");
    }

    /**
     * Triggers the wake up transition animation (sleep → sit)
     */
    public void triggerWakeUpAnimation() {
        wyvern.triggerAnim("action", "wake_up");
    }


    // ===== MOVEMENT CONTROLLER =====
    public PlayState handleMovementAnimation(AnimationState<Raevyx> state) {
        state.getController().transitionLength(6);

        if (wyvern.areRiderControlsLocked()) {
            return PlayState.STOP;
        }

        if (wyvern.level().isClientSide && !wyvern.isClientAnimationReady()) {
            state.setAndContinue(GROUND_IDLE);
            return PlayState.CONTINUE;
        }

        boolean inWater = wyvern.isInWater() || wyvern.isInWaterOrBubble();
        if (inWater) {
            state.getController().transitionLength(4);
            state.setAndContinue(SWIM);
            return PlayState.CONTINUE;
        }

        if (wyvern.isDying()) {
            return PlayState.STOP;
        }

        // Taming stunned - highest priority (should override sleep)
        if (wyvern.isTamingStunned()) {
            state.getController().transitionLength(4);
            state.setAndContinue(STUNNED);
            return PlayState.CONTINUE;
        }

        // Handle sleep: continuous animation for sleep loop, stop for transitions
        if (wyvern.isSleeping() && !wyvern.isSleepingEntering() && !wyvern.isSleepingExiting()) {
            state.getController().transitionLength(6);
            state.setAndContinue(SLEEP);
            return PlayState.CONTINUE;
        } else if (wyvern.isSleepingEntering() || wyvern.isSleepingExiting()) {
            // Transition animations are triggered, don't interfere
            return PlayState.STOP;
        }

        float maxSit = wyvern.maxSitTicks();
        float sitProgress = wyvern.getSitProgress();
        if (sitProgress >= maxSit) {
            state.setAndContinue(SIT);
            return PlayState.CONTINUE;
        } else if (sitProgress > 0f) {
            return PlayState.STOP;
        }

        if (wyvern.isBaby()) {
            if (wyvern.isActuallyRunning()) {
                state.getController().transitionLength(3);
                state.setAndContinue(GROUND_RUN);
            } else if (wyvern.isWalking()) {
                state.getController().transitionLength(3);
                state.setAndContinue(GROUND_WALK);
            } else {
                state.getController().transitionLength(4);
                state.setAndContinue(GROUND_IDLE);
            }
            return PlayState.CONTINUE;
        }

        if (wyvern.isDodging()) {
            state.setAndContinue(DODGE);
            return PlayState.CONTINUE;
        }

        if (wyvern.isLanding()) {
            state.setAndContinue(LANDING);
            return PlayState.CONTINUE;
        }

        if (wyvern.isFlying()) {
            int syncedMode = wyvern.getSyncedFlightMode();
            Vec3 vNow = wyvern.getDeltaMovement();

            // Mode 3: Takeoff
            if (syncedMode == 3) {
                state.getController().transitionLength(4);
                state.setAndContinue(TAKEOFF);
                return PlayState.CONTINUE;
            }

            // Rider landing blend (overrides flight mode)
            if (wyvern.isRiderLandingBlendActive()) {
                state.getController().transitionLength(4);
                currentFlightAnimation = LANDING;
                state.setAndContinue(LANDING);
                return PlayState.CONTINUE;
            }

            // Special case: GLIDE_DOWN for ridden dragons pitching down
            if (wyvern.isVehicle() && wyvern.isGoingDown() && !wyvern.isRiderLandingBlendActive()) {
                RawAnimation descend = GLIDE_DOWN;
                if (currentFlightAnimation != descend) {
                    state.getController().transitionLength(6);
                    currentFlightAnimation = descend;
                }
                state.setAndContinue(descend);
                return PlayState.CONTINUE;
            }

            // Mode 5: FLY_IDLE (stationary rider hover)
            if (syncedMode == 5) {
                RawAnimation hover = FLY_IDLE;
                if (currentFlightAnimation != hover) {
                    state.getController().transitionLength(6);
                    currentFlightAnimation = hover;
                }
                state.setAndContinue(hover);
                return PlayState.CONTINUE;
            }

            // Mode 4: SPRINT_FLAP
            if (syncedMode == 4) {
                RawAnimation sprint = SPRINT_FLAP;
                if (currentFlightAnimation != sprint) {
                    state.getController().transitionLength(3);
                    currentFlightAnimation = sprint;
                }
                state.setAndContinue(sprint);
                return PlayState.CONTINUE;
            }

            // Mode 2: Hover
            if (syncedMode == 2) {
                state.getController().transitionLength(6);
                state.setAndContinue(FLAP);
                return PlayState.CONTINUE;
            }

            // Mode 1: Forward flight (flap)
            if (syncedMode == 1) {
                state.getController().transitionLength(4);
                state.setAndContinue(FLAP);
                return PlayState.CONTINUE;
            }

            // Mode 0: Glide
            if (syncedMode == 0) {
                state.getController().transitionLength(12);
                state.setAndContinue(resolveGlideAnimation(vNow));
                return PlayState.CONTINUE;
            }

            float hoverWeight = wyvern.getHoveringFraction();
            float flapWeight = wyvern.getFlappingFraction();
            boolean descendingNow = vNow.y < -0.03;
            if (wyvern.isVehicle()) {
                descendingNow |= wyvern.isGoingDown();
            } else {
                descendingNow |= wyvern.getPitchDirection() > 0;
            }

            boolean shouldFlapBase = (currentFlightAnimation == FLAP)
                    ? (flapWeight > 0.55f || hoverWeight > 0.65f)
                    : (flapWeight > 0.22f || hoverWeight > 0.28f);

            if (hoverWeight > 0.45f) {
                state.getController().transitionLength(6);
                currentFlightAnimation = FLAP;
                state.setAndContinue(FLAP);
                return PlayState.CONTINUE;
            }

            boolean ascendingNow = wyvern.isGoingUp() || vNow.y > 0.02;
            if (ascendingNow) {
                if (currentFlightAnimation != FLAP) {
                    state.getController().transitionLength(4);
                    currentFlightAnimation = FLAP;
                }
                state.setAndContinue(FLAP);
                return PlayState.CONTINUE;
            } else if (shouldFlapBase) {
                if (currentFlightAnimation != FLAP) {
                    state.getController().transitionLength(4);
                    currentFlightAnimation = FLAP;
                }
                state.setAndContinue(FLAP);
                return PlayState.CONTINUE;
            } else {
                RawAnimation glideAnimation = resolveGlideAnimation(vNow);
                if (currentFlightAnimation != glideAnimation) {
                    state.getController().transitionLength(8);
                    currentFlightAnimation = glideAnimation;
                }
                state.setAndContinue(glideAnimation);
                return PlayState.CONTINUE;
            }
        }

        if (wyvern.isActuallyRunning()) {
            state.getController().transitionLength(3);
            state.setAndContinue(GROUND_RUN);
        } else if (wyvern.isWalking()) {
            state.getController().transitionLength(3);
            state.setAndContinue(GROUND_WALK);
        } else {
            state.getController().transitionLength(4);
            state.setAndContinue(GROUND_IDLE);
        }
        return PlayState.CONTINUE;
    }

    // ===== GECKOLIB SETUP =====
    /**
     * Sets up all GeckoLib animation triggers for the action controller.
     * ALL animations should use triggers for consistent behavior between player and AI control.
     */
    public void setupActionController(AnimationController<Raevyx> actionController) {
        // Register triggerable one-shots for server-side triggerAnim()
        registerVocalTriggers(actionController);

        // Combat abilities
        actionController.triggerableAnim("lightning_bite",
                RawAnimation.begin().thenPlay("animation.raevyx.lightning_bite"));
        actionController.triggerableAnim("horn_gore",
                RawAnimation.begin().thenPlay("animation.raevyx.horn_gore"));
        actionController.triggerableAnim("dodge",
                RawAnimation.begin().thenPlay("animation.raevyx.dodge"));

        // Lightning beam ability
        actionController.triggerableAnim("lightning_beam_start",
                RawAnimation.begin().thenPlay("animation.raevyx.lightning_beam_start"));
        actionController.triggerableAnim("lightning_beaming",
                RawAnimation.begin().thenLoop("animation.raevyx.lightning_beaming"));
        actionController.triggerableAnim("lightning_beam_stop",
                RawAnimation.begin().thenPlay("animation.raevyx.lightning_beam_stop"));

        // Other abilities
        actionController.triggerableAnim("eat",
                RawAnimation.begin().thenPlay("animation.raevyx.eat"));
        actionController.triggerableAnim("summon_storm_ground_start",
                RawAnimation.begin().thenPlay("animation.raevyx.summon_storm_ground_start"));
        actionController.triggerableAnim("summon_storm_ground",
                RawAnimation.begin().thenPlay("animation.raevyx.summon_storm_ground"));
        actionController.triggerableAnim("summon_storm_ground_end",
                RawAnimation.begin().thenPlay("animation.raevyx.summon_storm_ground_end"));
        actionController.triggerableAnim("summon_storm_air_start",
                RawAnimation.begin().thenPlay("animation.raevyx.summon_storm_air_start"));
        actionController.triggerableAnim("summon_storm_air",
                RawAnimation.begin().thenPlay("animation.raevyx.summon_storm_air"));
        actionController.triggerableAnim("summon_storm_air_end",
                RawAnimation.begin().thenPlay("animation.raevyx.summon_storm_air_end"));

        // Sit transition animations (player command)
        actionController.triggerableAnim("sit_down",
                RawAnimation.begin().thenPlay("animation.raevyx.down"));
        actionController.triggerableAnim("sit_up",
                RawAnimation.begin().thenPlay("animation.raevyx.up"));

        // Sleep animations (new system: sit → fall_asleep → sleep → wake_up → sit)
        actionController.triggerableAnim("fall_asleep",
                RawAnimation.begin().thenPlay("animation.raevyx.fall_asleep"));
        actionController.triggerableAnim("sleep",
                RawAnimation.begin().thenLoop("animation.raevyx.sleep"));
        actionController.triggerableAnim("wake_up",
                RawAnimation.begin().thenPlay("animation.raevyx.wake_up"));

        // Landed animation (plays after landing with rider)
        actionController.triggerableAnim("landed", LANDED);

        // Death animation
        actionController.triggerableAnim("die",
                RawAnimation.begin().thenPlay("animation.raevyx.die"));
    }
    
    /**
     * Registers vocal animation triggers
     */
    private void registerVocalTriggers(AnimationController<Raevyx> action) {
        // Only register sounds that actually have animations (skip sound-only vocals like excited, growl_warning)
        wyvern.getVocalEntries().forEach((key, entry) -> {
            if (entry.animationId() != null && !entry.animationId().isEmpty()) {
                action.triggerableAnim(key, RawAnimation.begin().thenPlay(entry.animationId()));
            }
        });
    }


    private RawAnimation resolveGlideAnimation(Vec3 velocity) {
        if (!wyvern.isTame()) {
            return FLY_GLIDE;
        }

        Vec3 motion = velocity == null ? Vec3.ZERO : velocity;
        double verticalSpeed = motion.y;
        double horizontalSpeedSqr = motion.horizontalDistanceSqr();

        boolean riderDescending = wyvern.isVehicle() && wyvern.isGoingDown();
        boolean pitchingDown = !wyvern.isVehicle() && wyvern.getPitchDirection() > 0;
        boolean fallingFast = verticalSpeed < -0.06;
        boolean moderateDescent = verticalSpeed < -0.025;
        boolean sustainedGlide = wyvern.getGlidingFraction() > 0.18f || wyvern.getFlappingFraction() < 0.35f;
        boolean hasForwardSpeed = horizontalSpeedSqr > 0.0009;

        if ((pitchingDown || riderDescending || fallingFast || moderateDescent) && sustainedGlide && hasForwardSpeed) {
            return GLIDE_DOWN;
        }
        return FLY_GLIDE;
    }

    // ===== ANIMATION PREDICATES =====

    /**
     * Handles banking animation based on bank direction
     * Disabled when in water - swimming has its own movement animations
     */
    public PlayState bankingPredicate(AnimationState<Raevyx> state) {
        // CLIENT-SIDE GRACE PERIOD: Prevent T-pose on world rejoin with shaders
        if (wyvern.level().isClientSide && !wyvern.isClientAnimationReady()) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.raevyx.banking_off"));
            return PlayState.CONTINUE;
        }

        // Stop banking during sleep transitions, taming stun, or when controls are locked
        if (wyvern.isSleeping() || wyvern.isSleepingEntering() || wyvern.isSleepingExiting()
                || wyvern.isTamingStunned() || wyvern.areRiderControlsLocked()) {
            return PlayState.STOP;
        }

        // Disable banking when in water
        boolean inWater = wyvern.isInWater() || wyvern.isInWaterOrBubble();
        if (inWater) {
            return PlayState.STOP;
        }

        state.setAndContinue(RawAnimation.begin().thenLoop("animation.raevyx.banking_off"));
        return PlayState.CONTINUE;
    }

    /**
     * Handles pitching animation based on pitch direction
     * Disabled when in water - swimming has its own movement animations
     */
    public PlayState pitchingPredicate(AnimationState<Raevyx> state) {
        // CLIENT-SIDE GRACE PERIOD: Prevent T-pose on world rejoin with shaders
        if (wyvern.level().isClientSide && !wyvern.isClientAnimationReady()) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.raevyx.pitching_off"));
            return PlayState.CONTINUE;
        }

        // Stop pitching during sleep transitions, taming stun, or when controls are locked
        if (wyvern.isSleeping() || wyvern.isSleepingEntering() || wyvern.isSleepingExiting()
                || wyvern.isTamingStunned() || wyvern.areRiderControlsLocked()) {
            return PlayState.STOP;
        }

        // Disable pitching when in water
        boolean inWater = wyvern.isInWater() || wyvern.isInWaterOrBubble();
        if (inWater) {
            return PlayState.STOP;
        }

        double pitchDir = wyvern.getPitchDirection();

        if (pitchDir > 0) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.raevyx.pitching_down"));
        } else if (pitchDir < 0) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.raevyx.pitching_up"));
        } else {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.raevyx.pitching_off"));
        }
        return PlayState.CONTINUE;
    }

}
