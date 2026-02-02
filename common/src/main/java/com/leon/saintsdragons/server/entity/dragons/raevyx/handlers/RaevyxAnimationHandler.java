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

    /** Dash forward right animation (movement animation like DODGE) */
    private static final RawAnimation DASH_FORWARD_RIGHT = RawAnimation.begin().thenPlay("animation.raevyx.dash_forward_right");

    /** Dash forward left animation (movement animation like DODGE) */
    private static final RawAnimation DASH_FORWARD_LEFT = RawAnimation.begin().thenPlay("animation.raevyx.dash_forward_left");

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

    /**
     * Triggers the dodge left animation
     */
    public void triggerDodgeLeftAnimation() {
        wyvern.triggerAnim("action", "dodge_left");
    }

    /**
     * Triggers the dodge right animation
     */
    public void triggerDodgeRightAnimation() {
        wyvern.triggerAnim("action", "dodge_right");
    }

    /**
     * Triggers the dodge backward animation
     */
    public void triggerDodgeBackwardAnimation() {
        wyvern.triggerAnim("action", "dash_backward");
    }


    // ===== MOVEMENT CONTROLLER =====
    public PlayState handleMovementAnimation(AnimationState<Raevyx> state) {
        state.getController().transitionLength(6);

        if (wyvern.areRiderControlsLocked()) {
            return PlayState.STOP;
        }

        if (wyvern.isTakeoff()) {
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
            state.getController().transitionLength(2);
            state.setAndContinue(DODGE);
            return PlayState.CONTINUE;
        }

        // Check for dashing (movement animation like dodge) - allows action animations to play alongside
        if (wyvern.isDashing()) {
            state.getController().transitionLength(2);
            // Alternate between left and right dash animations
            if (wyvern.wasLastDashRight()) {
                state.setAndContinue(DASH_FORWARD_LEFT);
            } else {
                state.setAndContinue(DASH_FORWARD_RIGHT);
            }
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

            // GLIDE_DOWN check - for both ridden and AI dragons
            if (wyvern.isVehicle()) {
                // Ridden: use player pitch
                float pitchDegrees = (float)Math.toDegrees(wyvern.getFlightPitchRadians(state.getPartialTick()));
                if (pitchDegrees < -10.0f && !wyvern.isRiderLandingBlendActive()) {
                    RawAnimation descend = GLIDE_DOWN;
                    if (currentFlightAnimation != descend) {
                        state.getController().transitionLength(6);
                        currentFlightAnimation = descend;
                    }
                    state.setAndContinue(descend);
                    return PlayState.CONTINUE;
                }
            } else {
                // AI: calculate pitch from velocity
                double horizontalSpeed = Math.sqrt(vNow.x * vNow.x + vNow.z * vNow.z);
                if (horizontalSpeed > 0.01) {
                    double pitchRadians = Math.atan2(-vNow.y, horizontalSpeed);
                    double pitchDegrees = Math.toDegrees(pitchRadians);

                    if (pitchDegrees > 10.0) {
                        RawAnimation descend = GLIDE_DOWN;
                        if (currentFlightAnimation != descend) {
                            state.getController().transitionLength(6);
                            currentFlightAnimation = descend;
                        }
                        state.setAndContinue(descend);
                        return PlayState.CONTINUE;
                    }
                }
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
                state.setAndContinue(FLY_GLIDE);
                return PlayState.CONTINUE;
            }

            // Fallback: should not reach here with proper sync, but default to glide
            state.getController().transitionLength(12);
            state.setAndContinue(FLY_GLIDE);
            return PlayState.CONTINUE;
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

        // Dodge animations
        actionController.triggerableAnim("dodge_left",
                RawAnimation.begin().thenPlay("animation.raevyx.dodge_left"));
        actionController.triggerableAnim("dodge_right",
                RawAnimation.begin().thenPlay("animation.raevyx.dodge_right"));
        actionController.triggerableAnim("dash_backward",
                RawAnimation.begin().thenPlay("animation.raevyx.dash_backward"));

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
        actionController.triggerableAnim("taunt",
                RawAnimation.begin().thenPlay("animation.raevyx.taunt"));
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
    }

    public PlayState instantActionPredicate(AnimationState<Raevyx> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }

    public void setupInstantActionController(AnimationController<Raevyx> controller) {
        controller.triggerableAnim("takeoff", TAKEOFF);
        controller.triggerableAnim("raevyx_hurt",
                RawAnimation.begin().thenPlay("animation.raevyx.hurt"));
        controller.triggerableAnim("die",
                RawAnimation.begin().thenPlay("animation.raevyx.die"));
    }
    
    /**
     * Registers vocal animation triggers
     */
    private void registerVocalTriggers(AnimationController<Raevyx> action) {
        // Only register sounds that actually have animations (skip sound-only vocals like excited)
        wyvern.getVocalEntries().forEach((key, entry) -> {
            if (!"action".equals(entry.controllerId())) {
                return;
            }
            if (entry.animationId() != null && !entry.animationId().isEmpty()) {
                action.triggerableAnim(key, RawAnimation.begin().thenPlay(entry.animationId()));
            }
        });
    }


    // Removed: resolveGlideAnimation() is no longer needed with synced flight modes

    // ===== ANIMATION PREDICATES =====

    /**
     * DEPRECATED: Banking is now fully procedural via model bone rotations
     * This controller is kept for compatibility but always returns STOP
     */
    public PlayState bankingPredicate(AnimationState<Raevyx> state) {
        // Banking is handled procedurally in RaevyxModel.applyBankingRoll()
        // No keyframed animations needed
        return PlayState.STOP;
    }

    /**
     * DEPRECATED: Pitching is now fully procedural via model bone rotations
     * This controller is kept for compatibility but always returns STOP
     */
    public PlayState pitchingPredicate(AnimationState<Raevyx> state) {
        // Pitching is handled procedurally in RaevyxModel.applyFlightPitch()
        // No keyframed animations needed
        return PlayState.STOP;
    }

}
