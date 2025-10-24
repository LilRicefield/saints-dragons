package com.leon.saintsdragons.server.entity.dragons.cindervane.handlers;

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
    private static final RawAnimation TAKEOFF = RawAnimation.begin().thenPlay("animation.cindervane.takeoff");
    private static final RawAnimation LANDING = RawAnimation.begin().thenPlay("animation.cindervane.landing");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.cindervane.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.cindervane.run");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.cindervane.sit");

    // Sleep sequence animations
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlay("animation.cindervane.down");
    private static final RawAnimation SIT_UP = RawAnimation.begin().thenPlay("animation.cindervane.up");
    private static final RawAnimation FALL_ASLEEP = RawAnimation.begin().thenPlay("animation.cindervane.fall_asleep");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.cindervane.sleep");
    private static final RawAnimation WAKE_UP = RawAnimation.begin().thenPlay("animation.cindervane.wake_up");

    private static final RawAnimation BANK_LEFT = RawAnimation.begin().thenLoop("animation.cindervane.banking_left");
    private static final RawAnimation BANK_RIGHT = RawAnimation.begin().thenLoop("animation.cindervane.banking_right");
    private static final RawAnimation BANK_OFF = RawAnimation.begin().thenLoop("animation.cindervane.banking_off");

    private static final RawAnimation PITCH_UP = RawAnimation.begin().thenLoop("animation.cindervane.pitching_up");
    private static final RawAnimation PITCH_DOWN = RawAnimation.begin().thenLoop("animation.cindervane.pitching_down");
    private static final RawAnimation PITCH_OFF = RawAnimation.begin().thenLoop("animation.cindervane.pitching_off");

    private static final int TAKEOFF_ANIM_MIN_TICKS = 16;
    private static final int TAKEOFF_ANIM_MAX_TICKS = 120;
    private static final double TAKEOFF_ASCENT_THRESHOLD = 0.05D;

    private final Cindervane dragon;
    private boolean takeoffAnimationActive;
    private int takeoffAnimationTicks;
    private boolean wasFlying;

    public CindervaneAnimationHandler(Cindervane dragon) {
        this.dragon = dragon;
    }

    public PlayState handleMovementAnimation(AnimationState<Cindervane> state) {
        boolean flyingNow = dragon.isFlying();
        if (flyingNow && !wasFlying) {
            takeoffAnimationActive = true;
            takeoffAnimationTicks = 0;
        } else if (!flyingNow) {
            takeoffAnimationActive = false;
            takeoffAnimationTicks = 0;
        }
        wasFlying = flyingNow;

        state.getController().transitionLength(12); // Longer transitions for smoother animation

        // While dying or sleeping (including transitions), suppress movement animations entirely; action controller plays die/sleep clips
        if (dragon.isDying() || dragon.isSleeping() || dragon.isSleepTransitioning()) {
            return PlayState.STOP;
        }

        if (dragon.isVehicle()) {
            state.getController().transitionLength(4);
            if (dragon.isFlying()) {
                // Check for takeoff first (highest priority)
                if (shouldPlayTakeoff()) {
                    state.setAndContinue(TAKEOFF);
                    return PlayState.CONTINUE;
                }
                // Check for landing blend (second highest priority)
                if (dragon.isRiderLandingBlendActive()) {
                    state.getController().transitionLength(4);
                    state.setAndContinue(LANDING);
                    return PlayState.CONTINUE;
                }
                // Check if descending (takes priority over altitude-based, but not over landing blend)
                else if (dragon.isGoingDown() && !dragon.isRiderLandingBlendActive()) {
                    state.getController().transitionLength(6);
                    state.setAndContinue(GLIDE_DOWN);
                } else {
                    // Altitude-based animation when being ridden
                    int flightMode = dragon.getFlightMode();

                    if (flightMode == 0) {
                        // High altitude glide
                        state.setAndContinue(GLIDE);
                    } else if (flightMode == 1) {
                        // Low altitude flap
                        state.setAndContinue(FLAP);
                    } else {
                        // Default to glide
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
            // Check for takeoff first (highest priority)
            if (shouldPlayTakeoff()) {
                state.getController().transitionLength(4);
                state.setAndContinue(TAKEOFF);
                return PlayState.CONTINUE;
            }

            // Check if descending when being ridden (for GLIDE_DOWN animation)
            boolean riderDescending = dragon.isVehicle() && dragon.getControllingPassenger() != null && dragon.isGoingDown();
            if (riderDescending) {
                state.getController().transitionLength(6);
                state.setAndContinue(GLIDE_DOWN);
            } else {
                // Wild dragons alternate between FLAP and GLIDE for natural flight
                // Use vertical velocity to determine which animation to play
                double verticalVelocity = dragon.getDeltaMovement().y;

                // Ascending or low-speed flight: flap wings
                // Gliding: high-speed level flight or descending slowly
                if (verticalVelocity > 0.02 || dragon.getDeltaMovement().horizontalDistanceSqr() < 0.1) {
                    state.getController().transitionLength(6);
                    state.setAndContinue(FLAP);
                } else {
                    state.getController().transitionLength(8);
                    state.setAndContinue(GLIDE);
                }
            }
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

    public PlayState bankingPredicate(AnimationState<Cindervane> state) {
        state.getController().transitionLength(8); // Longer transitions for smoother banking
        float smoothDir = dragon.getSmoothBankDirection();

        if (smoothDir > 0.2f) {
            state.setAndContinue(BANK_RIGHT);
        } else if (smoothDir < -0.2f) {
            state.setAndContinue(BANK_LEFT);
        } else {
            state.setAndContinue(BANK_OFF);
        }
        return PlayState.CONTINUE;
    }

    public PlayState pitchingPredicate(AnimationState<Cindervane> state) {
        state.getController().transitionLength(4);
        int dir = dragon.getPitchDirection();

        if (dir > 0) {
            state.setAndContinue(PITCH_DOWN);
        } else if (dir < 0) {
            state.setAndContinue(PITCH_UP);
        } else {
            state.setAndContinue(PITCH_OFF);
        }
        return PlayState.CONTINUE;
    }

    public void setupActionController(AnimationController<Cindervane> controller) {
        // Explicit animation triggers
        controller.triggerableAnim("bite_ground",
                RawAnimation.begin().thenPlay("animation.cindervane.bite_ground"));
        controller.triggerableAnim("bite_air",
                RawAnimation.begin().thenPlay("animation.cindervane.bite_air"));
        controller.triggerableAnim("magma_blast",
                RawAnimation.begin().thenPlay("animation.cindervane.magma_blast"));
        controller.triggerableAnim("eat",
                RawAnimation.begin().thenPlay("animation.cindervane.eat"));

        // Sleep sequence animations
        controller.triggerableAnim("down", SIT_DOWN);
        controller.triggerableAnim("up", SIT_UP);
        controller.triggerableAnim("fall_asleep", FALL_ASLEEP);
        controller.triggerableAnim("sleep", SLEEP);
        controller.triggerableAnim("wake_up", WAKE_UP);

        // Vocal entries (automatically registers roar, hurt, die animations with sounds)
        dragon.getVocalEntries().forEach((key, entry) ->
                controller.triggerableAnim(key, RawAnimation.begin().thenPlay(entry.animationId())));
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

    /**
     * Determines if the takeoff animation should play.
     * Plays during initial launch AND while ascending, like Raevyx.
     */
    private boolean shouldPlayTakeoff() {
        if (!dragon.isFlying()) {
            takeoffAnimationActive = false;
            takeoffAnimationTicks = 0;
            return false;
        }

        boolean flaggedTakeoff = dragon.isTakeoff() || dragon.getSyncedFlightMode() == 3;

        if (flaggedTakeoff) {
            if (!takeoffAnimationActive) {
                takeoffAnimationActive = true;
                takeoffAnimationTicks = 0;
            } else {
                takeoffAnimationTicks++;
            }
            return true;
        }

        if (!takeoffAnimationActive) {
            return false;
        }

        takeoffAnimationTicks++;

        int elapsedTicks = takeoffAnimationTicks;
        if (!dragon.level().isClientSide()) {
            elapsedTicks = Math.max(elapsedTicks, dragon.getAirTicks());
        }

        if (elapsedTicks < TAKEOFF_ANIM_MIN_TICKS) {
            return true;
        }

        boolean ascending = dragon.getDeltaMovement().y > TAKEOFF_ASCENT_THRESHOLD;
        boolean airborne = !dragon.onGround();
        if (dragon.getAirTicks() > 0) {
            airborne = true;
        }
        if (elapsedTicks < TAKEOFF_ANIM_MAX_TICKS && (ascending || airborne)) {
            return true;
        }

        takeoffAnimationActive = false;
        takeoffAnimationTicks = 0;
        return false;
    }
}
