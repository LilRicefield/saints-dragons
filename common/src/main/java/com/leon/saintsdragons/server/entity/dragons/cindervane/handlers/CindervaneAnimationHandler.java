package com.leon.saintsdragons.server.entity.dragons.cindervane.handlers;

import com.leon.saintsdragons.common.network.DragonAnimTickets;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;


/**
 * Lightweight animation helper for the Amphithere.
 */
public class CindervaneAnimationHandler {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.cindervane.idle");
    private static final RawAnimation GLIDE = RawAnimation.begin().thenLoop("animation.cindervane.glide");
    private static final RawAnimation GLIDE_DOWN = RawAnimation.begin().thenLoop("animation.cindervane.glide_down");
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.cindervane.flap");
    private static final RawAnimation SPRINT_FLAP = RawAnimation.begin().thenLoop("animation.cindervane.sprint_flap");
    private static final RawAnimation FLY_IDLE = RawAnimation.begin().thenLoop("animation.cindervane.fly_idle");
    private static final RawAnimation TAKEOFF = RawAnimation.begin().thenPlay("animation.cindervane.takeoff");
    private static final RawAnimation LANDING = RawAnimation.begin().thenPlay("animation.cindervane.landing");
    private static final RawAnimation LANDED = RawAnimation.begin().thenPlay("animation.cindervane.landed");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.cindervane.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.cindervane.run");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.cindervane.sit");

    // Sleep sequence animations
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlay("animation.cindervane.down");
    private static final RawAnimation SIT_UP = RawAnimation.begin().thenPlay("animation.cindervane.up");
    private static final RawAnimation FALL_ASLEEP = RawAnimation.begin().thenPlay("animation.cindervane.fall_asleep");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.cindervane.sleep");
    private static final RawAnimation WAKE_UP = RawAnimation.begin().thenPlay("animation.cindervane.wake_up");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.cindervane.swim");


    private final Cindervane dragon;

    public CindervaneAnimationHandler(Cindervane dragon) {
        this.dragon = dragon;
    }

    public PlayState handleMovementAnimation(AnimationState<Cindervane> state) {
        state.getController().transitionLength(12); // Longer transitions for smoother animation

        // CLIENT-SIDE GRACE PERIOD: Prevent T-pose on world rejoin with shaders
        // Wait for entity data to sync from server before processing animations
        if (dragon.level().isClientSide && !dragon.isClientAnimationReady()) {
            state.setAndContinue(IDLE);
            return PlayState.CONTINUE;
        }

        if (dragon.isDying()) {
            return PlayState.STOP;
        }

        if (dragon.isTakeoff()) {
            return PlayState.STOP;
        }

        // Handle sleep: continuous animation for sleep loop, stop for transitions
        if (dragon.isSleeping() && !dragon.isSleepTransitioning()) {
            state.getController().transitionLength(6);
            state.setAndContinue(SLEEP);
            return PlayState.CONTINUE;
        } else if (dragon.isSleepTransitioning()) {
            // Transition animations are triggered, don't interfere
            return PlayState.STOP;
        }

        boolean inWater = dragon.isInWater() || dragon.isInWaterOrBubble();

        if (inWater) {
            state.getController().transitionLength(6);
            state.setAndContinue(SWIM);
            state.getController().setAnimationSpeed(1.0f);
            return PlayState.CONTINUE;
        }

        if (dragon.isVehicle()) {
            state.getController().transitionLength(4);
            if (dragon.isFlying()) {
                // Get synced flight mode from physics controller
                // 0 = glide, 1 = flap, 2 = hover, 3 = takeoff, 4 = sprint_flap, 5 = fly_idle, -1 = ground
                int syncedMode = dragon.getSyncedFlightMode();

                // Takeoff is handled by instant controller.
                if (syncedMode == 3) {
                    return PlayState.STOP;
                }
                // Check for landing blend (second highest priority)
                if (dragon.isRiderLandingBlendActive()) {
                    state.getController().transitionLength(4);
                    state.setAndContinue(LANDING);
                    return PlayState.CONTINUE;
                }

                // GLIDE_DOWN - third priority (diving past 10 degrees)
                // Note: pitch is negated, so looking down = negative pitch
                // Only applies to ridden dragons, prevents AI from always playing glide_down
                float pitchDegrees = (float)Math.toDegrees(dragon.getFlightPitchRadians(state.getPartialTick()));
                if (pitchDegrees < -10.0f && !dragon.isRiderLandingBlendActive()) {
                    state.getController().transitionLength(6);
                    state.setAndContinue(GLIDE_DOWN);
                    return PlayState.CONTINUE;
                }

                // Mode 5: FLY_IDLE - stationary rider hover (physics controller detects via position tracking)
                if (syncedMode == 5) {
                    state.getController().transitionLength(6);
                    state.setAndContinue(FLY_IDLE);
                    return PlayState.CONTINUE;
                }

                // Mode 4: SPRINT_FLAP - accelerating flight (detected by physics controller)
                if (syncedMode == 4) {
                    state.getController().transitionLength(3);
                    state.setAndContinue(SPRINT_FLAP);
                    return PlayState.CONTINUE;
                }

                // Mode 2: HOVER
                if (syncedMode == 2) {
                    state.getController().transitionLength(6);
                    state.setAndContinue(FLAP);
                    return PlayState.CONTINUE;
                }
                // Altitude-based animations (lowest priority)
                else {
                    // Altitude-based animation when being ridden
                    // IMPORTANT: Use the synced flight mode instead of recalculating client-side
                    int flightMode = dragon.getSyncedFlightMode();

                    if (flightMode == 0) {
                        // High altitude glide - long transition for smooth feel
                        state.getController().transitionLength(12);
                        state.setAndContinue(GLIDE);
                    } else if (flightMode == 1) {
                        // Low altitude flap - medium transition
                        state.getController().transitionLength(6);
                        state.setAndContinue(FLAP);
                    } else {
                        // Default to glide with long transition
                        state.getController().transitionLength(12);
                        state.setAndContinue(GLIDE);
                    }
                }
            } else {
                int groundState = dragon.getEffectiveGroundState();
                if (groundState == 2) {
                    state.setAndContinue(RUN);
                } else if (groundState == 1) {
                    state.setAndContinue(WALK);
                } else {
                    state.setAndContinue(IDLE);
                }
            }
            state.getController().setAnimationSpeed(1.0f);
            return PlayState.CONTINUE;
        }

        // Drive SIT from our custom progress system only to avoid desync
        // Only play SIT loop when FULLY sat down (sit_down transition finished)
        float sitProgress = dragon.getSitProgress();
        float maxSit = dragon.maxSitTicks();

        if (sitProgress >= maxSit) {
            // Fully sitting - play SIT loop
            state.setAndContinue(SIT);
            return PlayState.CONTINUE;
        } else if (sitProgress > 0f) {
            // In transition (either sitting down or standing up) - let action controller handle it
            return PlayState.STOP;
        }

        state.getController().setAnimationSpeed(1.0f);

        if (dragon.isFlying()) {
            int syncedMode = dragon.getSyncedFlightMode();

            // Takeoff is handled by instant controller.
            if (syncedMode == 3) {
                return PlayState.STOP;
            }

            // GLIDE_DOWN check for AI dragons (calculate pitch from velocity)
            if (!dragon.isVehicle()) {
                net.minecraft.world.phys.Vec3 velocity = dragon.getDeltaMovement();
                double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                if (horizontalSpeed > 0.01) { // Only check pitch if moving horizontally
                    // Calculate pitch angle from velocity (negative = diving down)
                    double pitchRadians = Math.atan2(-velocity.y, horizontalSpeed);
                    double pitchDegrees = Math.toDegrees(pitchRadians);

                    // If diving down steeply (pitch > 10 degrees downward)
                    if (pitchDegrees > 10.0) {
                        state.getController().transitionLength(6);
                        state.setAndContinue(GLIDE_DOWN);
                        return PlayState.CONTINUE;
                    }
                }
            }

            // Mode 5: FLY_IDLE (hovering still)
            if (syncedMode == 5) {
                state.getController().transitionLength(6);
                state.setAndContinue(FLY_IDLE);
                return PlayState.CONTINUE;
            }

            // Mode 4: SPRINT_FLAP
            if (syncedMode == 4) {
                state.getController().transitionLength(3);
                state.setAndContinue(SPRINT_FLAP);
                return PlayState.CONTINUE;
            }

            // Mode 2: Hover (AI hovering/landing)
            if (syncedMode == 2) {
                state.getController().transitionLength(6);
                state.setAndContinue(FLAP);
                return PlayState.CONTINUE;
            }

            // Mode 1: Forward flight (flapping)
            if (syncedMode == 1) {
                state.getController().transitionLength(6);
                state.setAndContinue(FLAP);
                return PlayState.CONTINUE;
            }

            // Mode 0: Glide
            if (syncedMode == 0) {
                state.getController().transitionLength(8);
                state.setAndContinue(GLIDE);
                return PlayState.CONTINUE;
            }

            // Fallback: default to flap
            state.getController().transitionLength(6);
            state.setAndContinue(FLAP);
            return PlayState.CONTINUE;
        }

        if (!dragon.isTakeoff() && !dragon.isLanding() && !dragon.isHovering()) {
            // Use the improved movement state detection - prioritize AI-set states for tamed dragons
            int groundState = dragon.getEffectiveGroundState(); // Use effective state for client-side consistency
            
            // Add hysteresis to prevent rapid animation changes
            if (groundState == 2) {
                // Running state
                state.setAndContinue(RUN);
            } else if (groundState == 1) {
                // Walking state
                state.setAndContinue(WALK);
            } else if (dragon.isRunning()) {
                // Fallback to running check
                state.setAndContinue(RUN);
            } else if (dragon.isWalking()) {
                // Fallback to walking check
                state.setAndContinue(WALK);
            } else {
                state.setAndContinue(IDLE);
            }
        } else {
            // During takeoff, landing, or hovering, play idle animation
            state.setAndContinue(IDLE);
        }

        return PlayState.CONTINUE;
    }

    /**
     * DEPRECATED: Banking is now fully procedural via model bone rotations
     * This controller is kept for compatibility but always returns STOP
     */
    public PlayState bankingPredicate(AnimationState<Cindervane> state) {
        // Banking is handled procedurally in CindervaneModel.applyBankingRoll()
        // No keyframed animations needed
        return PlayState.STOP;
    }

    /**
     * DEPRECATED: Pitching is now fully procedural via model bone rotations
     * This controller is kept for compatibility but always returns STOP
     */
    public PlayState pitchingPredicate(AnimationState<Cindervane> state) {
        // Pitching is handled procedurally in CindervaneModel.applyFlightPitch()
        // No keyframed animations needed
        return PlayState.STOP;
    }

    public void setupActionController(AnimationController<Cindervane> controller) {
        // Explicit animation triggers
        controller.triggerableAnim("bite",
                RawAnimation.begin().thenPlay("animation.cindervane.bite"));
        controller.triggerableAnim("bite_air",
                RawAnimation.begin().thenPlay("animation.cindervane.bite_air"));
        controller.triggerableAnim("roar_air",
                RawAnimation.begin().thenPlay("animation.cindervane.roar_air"));
        controller.triggerableAnim("magma_blast",
                RawAnimation.begin().thenPlay("animation.cindervane.magma_blast"));
        controller.triggerableAnim("eat",
                RawAnimation.begin().thenPlay("animation.cindervane.eat"));

        // Landed animation (plays after landing with rider)
        controller.triggerableAnim("landed", LANDED);

        // Sleep sequence animations
        controller.triggerableAnim("down", SIT_DOWN);
        controller.triggerableAnim("up", SIT_UP);
        controller.triggerableAnim("fall_asleep", FALL_ASLEEP);
        controller.triggerableAnim("sleep", SLEEP);
        controller.triggerableAnim("wake_up", WAKE_UP);

        // Vocal entries (only those bound to the actions controller)
        dragon.getVocalEntries().forEach((key, entry) -> {
            if (!"actions".equals(entry.controllerId())) {
                return;
            }
            if (entry.animationId() != null && !entry.animationId().isEmpty()) {
                controller.triggerableAnim(key, RawAnimation.begin().thenPlay(entry.animationId()));
            }
        });
    }

    public PlayState instantActionPredicate(AnimationState<Cindervane> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }

    public void setupInstantActionController(AnimationController<Cindervane> controller) {
        controller.triggerableAnim("takeoff", TAKEOFF);
        controller.triggerableAnim("cindervane_hurt",
                RawAnimation.begin().thenPlay("animation.cindervane.hurt"));
        controller.triggerableAnim("die",
                RawAnimation.begin().thenPlay("animation.cindervane.die"));
    }

    // Sleep animation trigger methods
    public void triggerSitDownAnimation() {
        dragon.triggerAnim("actions", "down");
    }

    public void triggerSitUpAnimation() {
        dragon.triggerAnim("actions", "up");
    }

    public void triggerFallAsleepAnimation() {
        dragon.triggerAnim("actions", "fall_asleep");
    }

    public void triggerSleepAnimation() {
        dragon.triggerAnim("actions", "sleep");
    }

    public void triggerWakeUpAnimation() {
        dragon.triggerAnim("actions", "wake_up");
    }

    public PlayState actionPredicate(AnimationState<Cindervane> state) {
        state.getController().transitionLength(5);
        return PlayState.STOP;
    }

}
