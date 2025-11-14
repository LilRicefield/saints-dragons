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

    /**
     * Main animation predicate - handles idle, walk, run, fly, and sit animations
     */
    public PlayState handleMovementAnimation(AnimationState<Ignivorus> state) {
        // Reduced transition to prevent overlapping step sounds during animation changes
        state.getController().transitionLength(3);

        // CLIENT-SIDE GRACE PERIOD: Prevent T-pose on world rejoin with shaders
        // Wait for entity data to sync from server before processing animations
        if (dragon.level().isClientSide && !dragon.isClientAnimationReady()) {
            state.setAndContinue(IDLE);
            return PlayState.CONTINUE;
        }

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
            // Ground movement - use DATA_GROUND_MOVE_STATE as single source of truth
            // This value is set by:
            // - applyRiderMovementInput() when ridden
            // - setGroundMoveStateFromAI() when AI-controlled
            int groundState = dragon.getEntityData().get(dragon.DATA_GROUND_MOVE_STATE);

            switch (groundState) {
                case 2 -> state.setAndContinue(RUN);   // Running/sprinting
                case 1 -> state.setAndContinue(WALK);  // Walking
                default -> state.setAndContinue(IDLE); // Idle/stopped
            }
        }
        return PlayState.CONTINUE;
    }

    /**
     * Banking animation based on turn direction
     */
    public PlayState bankingPredicate(AnimationState<Ignivorus> state) {
        // CLIENT-SIDE GRACE PERIOD: Prevent T-pose on world rejoin with shaders
        if (dragon.level().isClientSide && !dragon.isClientAnimationReady()) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.ignivorus.banking_off"));
            return PlayState.CONTINUE;
        }

        // Simple version - can expand later
        state.setAndContinue(RawAnimation.begin().thenLoop("animation.ignivorus.banking_off"));
        return PlayState.CONTINUE;
    }

    /**
     * Pitching animation based on pitch direction
     */
    public PlayState pitchingPredicate(AnimationState<Ignivorus> state) {
        // CLIENT-SIDE GRACE PERIOD: Prevent T-pose on world rejoin with shaders
        if (dragon.level().isClientSide && !dragon.isClientAnimationReady()) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.ignivorus.pitching_off"));
            return PlayState.CONTINUE;
        }

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

    /**
     * Sets up all GeckoLib animation triggers for the action controller.
     * Follows the same pattern as Raevyx for consistent ability animation handling.
     */
    public void setupActionController(AnimationController<Ignivorus> actionController) {
        // Sit transition animations
        actionController.triggerableAnim("sit_down",
            RawAnimation.begin().thenPlay("animation.ignivorus.down"));
        actionController.triggerableAnim("sit_up",
            RawAnimation.begin().thenPlay("animation.ignivorus.up"));

        // Bite ability animation
        actionController.triggerableAnim("bite",
            RawAnimation.begin().thenPlay("animation.ignivorus.bite"));

        // Body slam ability animation
        actionController.triggerableAnim("body_slam",
            RawAnimation.begin().thenPlay("animation.ignivorus.body_slam"));

        // Fire breath ability animations
        // Start animation plays for ~75ms (4 ticks) before actual fire spawns
        actionController.triggerableAnim("fire_breath_start",
            RawAnimation.begin().thenPlay("animation.ignivorus.fire_breath_start"));
        // Loop animation for continuous fire breathing
        actionController.triggerableAnim("fire_breathing",
            RawAnimation.begin().thenLoop("animation.ignivorus.fire_breathing"));
        // Stop animation to cleanly exit the breathing loop
        actionController.triggerableAnim("fire_breath_stop",
            RawAnimation.begin().thenPlay("animation.ignivorus.fire_breath_end"));

        // Roar animation
        actionController.triggerableAnim("roar",
            RawAnimation.begin().thenPlay("animation.ignivorus.roar"));

        // Death animation
        actionController.triggerableAnim("die",
            RawAnimation.begin().thenPlay("animation.ignivorus.die"));

        // Ambient grumbles
        actionController.triggerableAnim("ignivorus_grumble1",
            RawAnimation.begin().thenPlay("animation.ignivorus.grumble1"));
        actionController.triggerableAnim("ignivorus_grumble2",
            RawAnimation.begin().thenPlay("animation.ignivorus.grumble2"));
        actionController.triggerableAnim("ignivorus_grumble3",
            RawAnimation.begin().thenPlay("animation.ignivorus.grumble3"));
    }
}
