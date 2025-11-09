package com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

/**
 * Handles all animation logic for Ignivorus
 */
public record IgnivorusAnimationHandler(Ignivorus dragon) {

    // Animation constants
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ignivorus.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.ignivorus.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.ignivorus.run");
    private static final RawAnimation TAKEOFF = RawAnimation.begin().thenPlay("animation.ignivorus.take_off");
    private static final RawAnimation GLIDE = RawAnimation.begin().thenLoop("animation.ignivorus.glide");
    private static final RawAnimation GLIDE_DOWN = RawAnimation.begin().thenLoop("animation.ignivorus.glide_down");
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.ignivorus.flap");
    private static final RawAnimation SPRINT_FLAP = RawAnimation.begin().thenLoop("animation.ignivorus.sprint_flap");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.ignivorus.sit");
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlay("animation.ignivorus.down");
    private static final RawAnimation SIT_UP = RawAnimation.begin().thenPlay("animation.ignivorus.up");

    /**
     * Main animation predicate - handles idle, walk, run, fly, and sit animations
     */
    public PlayState handleMovementAnimation(AnimationState<Ignivorus> state) {
        state.getController().transitionLength(8); // Smooth transitions

        // Check for sitting - highest priority after flying
        if (!dragon.isFlying() && (dragon.isOrderedToSit() || dragon.getSitProgress() > 0.5f)) {
            state.setAndContinue(SIT);
            return PlayState.CONTINUE;
        }

        if (dragon.isFlying()) {
            // Check for takeoff animation (highest priority)
            if (dragon.isTakeoff() || dragon.timeFlying < 30) {
                state.setAndContinue(TAKEOFF);
                return PlayState.CONTINUE;
            }

            // Check velocity for movement detection
            var vel = dragon.getDeltaMovement();
            boolean isMovingHorizontally = vel.horizontalDistanceSqr() > 0.01;

            // GLIDE_DOWN - second priority (diving/descending)
            if (dragon.isGoingDown()) {
                state.getController().transitionLength(6);
                state.setAndContinue(GLIDE_DOWN);
                return PlayState.CONTINUE;
            }

            // SPRINT_FLAP - third priority (accelerating flight)
            if (dragon.isAccelerating() && isMovingHorizontally) {
                state.getController().transitionLength(3);
                state.setAndContinue(SPRINT_FLAP);
                return PlayState.CONTINUE;
            }

            // Altitude-based animations (lowest priority)
            // Flight mode: 0=glide, 1=flap
            int flightMode = dragon.getFlightMode();
            if (flightMode == 0) {
                // High altitude gliding - long smooth transition
                state.getController().transitionLength(12);
                state.setAndContinue(GLIDE);
            } else {
                // Low altitude flapping - normal transition
                state.getController().transitionLength(6);
                state.setAndContinue(FLAP);
            }
        } else {
            // Ground movement - check velocity directly for multiplayer sync
            var vel = dragon.getDeltaMovement();
            boolean isMovingOnGround = vel.horizontalDistanceSqr() > 0.001;

            if (isMovingOnGround) {
                // Check if running/sprinting
                if (dragon.isAccelerating() || dragon.isRunning()) {
                    state.setAndContinue(RUN);
                } else {
                    state.setAndContinue(WALK);
                }
            } else {
                // Standing still
                state.setAndContinue(IDLE);
            }
        }
        return PlayState.CONTINUE;
    }

    /**
     * Banking animation based on turn direction
     */
    public PlayState bankingPredicate(AnimationState<Ignivorus> state) {
        // Simple version - can expand later
        state.setAndContinue(RawAnimation.begin().thenLoop("animation.ignivorus.banking_off"));
        return PlayState.CONTINUE;
    }

    /**
     * Pitching animation based on pitch direction
     */
    public PlayState pitchingPredicate(AnimationState<Ignivorus> state) {
        double pitchDir = dragon.getPitchDirection();

        if (pitchDir > 0) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.ignivorus.pitching_down"));
        } else if (pitchDir < 0) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.ignivorus.pitching_up"));
        } else {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.ignivorus.pitching_off"));
        }
        return PlayState.CONTINUE;
    }

    /**
     * Trigger the sit down animation (idle → sit transition)
     * Animation: "down" (38 ticks / 1.88 seconds)
     */
    public void triggerSitDownAnimation() {
        dragon.triggerAnim("action", "sit_down");
    }

    /**
     * Trigger the sit up animation (sit → idle transition)
     * Animation: "up" (38 ticks / 1.88 seconds)
     */
    public void triggerSitUpAnimation() {
        dragon.triggerAnim("action", "sit_up");
    }
}
