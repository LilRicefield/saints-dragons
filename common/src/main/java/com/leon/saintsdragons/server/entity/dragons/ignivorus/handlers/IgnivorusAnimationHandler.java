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
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.ignivorus.flap");

    /**
     * Main animation predicate - handles idle, walk, run, fly animations
     */
    public PlayState handleMovementAnimation(AnimationState<Ignivorus> state) {
        state.getController().transitionLength(8); // Smooth transitions

        if (dragon.isFlying()) {
            // Check for takeoff animation (30 ticks = 1.5s to match animation length)
            if (dragon.isTakeoff() || dragon.timeFlying < 30) {
                state.setAndContinue(TAKEOFF);
                return PlayState.CONTINUE;
            }

            // Flight mode determines animation: 0=glide, 1=flap, 2=hover, 3=takeoff
            int flightMode = dragon.getFlightMode();
            if (flightMode == 0) {
                // High altitude gliding
                state.getController().transitionLength(12);
                state.setAndContinue(GLIDE);
            } else {
                // Active flapping (low altitude or accelerating)
                state.setAndContinue(FLAP);
            }
        } else if (state.isMoving()) {
            // Ground movement - check if running/sprinting
            if (dragon.isAccelerating() || dragon.isRunning()) {
                state.setAndContinue(RUN);
            } else {
                state.setAndContinue(WALK);
            }
        } else {
            // Standing still
            state.setAndContinue(IDLE);
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
}
