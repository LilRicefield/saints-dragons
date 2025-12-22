package com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers;

import com.leon.saintsdragons.common.network.DragonAnimTickets;
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
    private static final RawAnimation LANDING = RawAnimation.begin().thenPlay("animation.ignivorus.landing");
    private static final RawAnimation LANDED = RawAnimation.begin().thenPlay("animation.ignivorus.landed");
    private static final RawAnimation GLIDE = RawAnimation.begin().thenLoop("animation.ignivorus.glide");
    private static final RawAnimation GLIDE_DOWN = RawAnimation.begin().thenLoop("animation.ignivorus.glide_down");
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.ignivorus.flap");
    private static final RawAnimation SPRINT_FLAP = RawAnimation.begin().thenLoop("animation.ignivorus.sprint_flap");
    private static final RawAnimation FLY_IDLE = RawAnimation.begin().thenLoop("animation.ignivorus.fly_idle");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.ignivorus.sit");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.ignivorus.swim");
    private static final RawAnimation STUNNED = RawAnimation.begin().thenLoop("animation.ignivorus.stunned");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.ignivorus.sleep");
    private static final RawAnimation BULLDOZER_IDLE = RawAnimation.begin().thenLoop("animation.ignivorus.bulldozer_idle");
    private static final RawAnimation BULLDOZING = RawAnimation.begin().thenLoop("animation.ignivorus.bulldozing");
    private static final RawAnimation PHASE2_IDLE = RawAnimation.begin().thenLoop("animation.ignivorus.phase2_idle");
    private static final RawAnimation PHASE2_WALK = RawAnimation.begin().thenLoop("animation.ignivorus.phase2_walk");
    private static final RawAnimation PHASE2_RUN = RawAnimation.begin().thenLoop("animation.ignivorus.phase2_run");

    /**
     * Main animation predicate - handles idle, walk, run, fly, and sit animations
     */
    public PlayState handleMovementAnimation(AnimationState<Ignivorus> state) {
        // Reduced transition to prevent overlapping step sounds during animation changes
        state.getController().transitionLength(6);

        // CLIENT-SIDE GRACE PERIOD: Prevent T-pose on world rejoin with shaders
        // Wait for entity data to sync from server before processing animations
        if (dragon.level().isClientSide && !dragon.isClientAnimationReady()) {
            state.setAndContinue(IDLE);
            return PlayState.CONTINUE;
        }

        // CRITICAL: Stop movement controller when controls are locked (e.g., during ultimate)
        // This prevents idle/walk animations from competing with action controller animations
        if (dragon.areRiderControlsLocked()) {
            return PlayState.STOP;
        }

        if (dragon.isDying()) {
            return PlayState.STOP;
        }

        // Taming stunned - highest priority on ground (plays exhausted/downed animation, should override sleep)
        if (dragon.isTamingStunned()) {
            state.getController().transitionLength(4);
            state.setAndContinue(STUNNED);
            return PlayState.CONTINUE;
        }

        // Handle sleep: continuous animation for sleep loop, stop for transitions
        if (dragon.isSleeping() && !dragon.isSleepingEntering() && !dragon.isSleepingExiting()) {
            state.getController().transitionLength(6);
            state.setAndContinue(SLEEP);
            return PlayState.CONTINUE;
        } else if (dragon.isSleepingEntering() || dragon.isSleepingExiting()) {
            // Transition animations are triggered, don't interfere
            return PlayState.STOP;
        }

        // Check for bulldozing - highest priority for ground movement
        if (!dragon.isFlying() && dragon.getEntityData().get(Ignivorus.DATA_BULLDOZING)) {
            // Check if moving (use synced rider input instead of velocity for proper client-side sync)
            float riderForward = dragon.getEntityData().get(Ignivorus.DATA_RIDER_FORWARD);
            float riderStrafe = dragon.getEntityData().get(Ignivorus.DATA_RIDER_STRAFE);
            boolean isMoving = Math.abs(riderForward) > 0.01f || Math.abs(riderStrafe) > 0.01f;
            if (isMoving) {
                state.setAndContinue(BULLDOZING);
            } else {
                state.setAndContinue(BULLDOZER_IDLE);
            }
            return PlayState.CONTINUE;
        }

        // Check for Phase 2 - second priority for ground movement
        if (!dragon.isFlying() && dragon.getEntityData().get(Ignivorus.DATA_PHASE2)) {
            // Check if moving (use synced rider input instead of velocity for proper client-side sync)
            float riderForward = dragon.getEntityData().get(Ignivorus.DATA_RIDER_FORWARD);
            float riderStrafe = dragon.getEntityData().get(Ignivorus.DATA_RIDER_STRAFE);
            boolean isMoving = Math.abs(riderForward) > 0.01f || Math.abs(riderStrafe) > 0.01f;

            if (isMoving) {
                // Check if running (sprinting) - use DATA_ACCELERATING which is properly synced
                boolean isRunning = dragon.getEntityData().get(Ignivorus.DATA_ACCELERATING);
                state.setAndContinue(isRunning ? PHASE2_RUN : PHASE2_WALK);
            } else {
                state.setAndContinue(PHASE2_IDLE);
            }
            return PlayState.CONTINUE;
        }

        // Check for sitting - highest priority after flying
        if (!dragon.isFlying() && (dragon.isOrderedToSit() || dragon.getSitProgress() > 0.5f)) {
            state.setAndContinue(SIT);
            return PlayState.CONTINUE;
        }

        if (!dragon.isFlying() && dragon.isInWaterOrBubble()) {
            state.setAndContinue(SWIM);
            return PlayState.CONTINUE;
        }

        if (dragon.isFlying()) {
            // Get synced flight mode from physics controller
            // 0 = glide, 1 = flap, 2 = hover, 3 = takeoff, 4 = sprint_flap, 5 = fly_idle, -1 = ground
            int syncedMode = dragon.getSyncedFlightMode();
            var vel = dragon.getDeltaMovement();
            boolean sprinting = dragon.isAccelerating();

            // Mode 3: Takeoff (highest priority)
            if (syncedMode == 3 || dragon.isTakeoff() || dragon.timeFlying < 30) {
                state.getController().transitionLength(4);
                state.setAndContinue(TAKEOFF);
                return PlayState.CONTINUE;
            }

            // Landing animation (second priority) - use rider landing blend for ridden dragons
            if (dragon.isRiderLandingBlendActive() || dragon.isLanding()) {
                state.getController().transitionLength(4);
                state.setAndContinue(LANDING);
                return PlayState.CONTINUE;
            }

            // GLIDE_DOWN - only for RIDER diving (not AI flight)
            // This prevents AI dragons from always playing glide_down
            // Also prevent glide_down when landing blend is active (rider is landing)
            if (dragon.isVehicle() && dragon.isGoingDown() && !dragon.isRiderLandingBlendActive()) {
                state.getController().transitionLength(6);
                state.setAndContinue(GLIDE_DOWN);
                return PlayState.CONTINUE;
            }

            // Mode 5: FLY_IDLE - stationary rider hover
            if (syncedMode == 5) {
                state.getController().transitionLength(6);
                state.setAndContinue(FLY_IDLE);
                return PlayState.CONTINUE;
            }

            // Mode 4: SPRINT_FLAP - accelerating flight
            if (syncedMode == 4) {
                state.getController().transitionLength(3);
                state.setAndContinue(SPRINT_FLAP);
                return PlayState.CONTINUE;
            }

            // ASCENDING - always flap when going up (rider or AI)
            if (dragon.isGoingUp() || vel.y > 0.02) {
                state.getController().transitionLength(4);
                state.setAndContinue(FLAP);
                return PlayState.CONTINUE;
            }

            // HOVER - stationary in air
            if (syncedMode == 2 || dragon.isHovering()) {
                state.getController().transitionLength(6);
                state.setAndContinue(sprinting ? SPRINT_FLAP : FLAP);
                return PlayState.CONTINUE;
            }

            // FLAP vs GLIDE based on physics controller
            // Mode 1 = FLAP (low altitude or needs lift)
            // Mode 0 = GLIDE (high altitude, can glide)
            if (syncedMode == 1) {
                state.getController().transitionLength(4);
                state.setAndContinue(sprinting ? SPRINT_FLAP : FLAP);
            } else {
                state.getController().transitionLength(12);
                state.setAndContinue(GLIDE);
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

        if (dragon.isDying() || dragon.isSleeping() || dragon.isSleepingEntering() || dragon.isSleepingExiting()) {
            return PlayState.STOP;
        }

        // Stop banking when taming stunned, controls are locked, or landing blend active
        if (dragon.isTamingStunned() || dragon.areRiderControlsLocked() || dragon.isRiderLandingBlendActive()) {
            return PlayState.STOP;
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

        if (dragon.isDying() || dragon.isSleeping() || dragon.isSleepingEntering() || dragon.isSleepingExiting()) {
            return PlayState.STOP;
        }

        // Stop pitching when taming stunned, controls are locked, or landing blend active
        if (dragon.isTamingStunned() || dragon.areRiderControlsLocked() || dragon.isRiderLandingBlendActive()) {
            return PlayState.STOP;
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

    public void triggerFallAsleepAnimation() {
        dragon.triggerAnim("action", "fall_asleep");
    }

    public void triggerSleepAnimation() {
        dragon.triggerAnim("action", "sleep");
    }

    public void triggerWakeUpAnimation() {
        dragon.triggerAnim("action", "wake_up");
    }

    public void triggerBulldozeEnterAnimation() {
        dragon.triggerAnim("action", "bulldozer_enter");
    }

    public void triggerBulldozeExitAnimation() {
        dragon.triggerAnim("action", "bulldozer_exit");
    }

    public void triggerPhase2EnterAnimation() {
        dragon.triggerAnim("action", "phase2_enter");
    }

    public void triggerPhase2ExitAnimation() {
        dragon.triggerAnim("action", "phase2_exit");
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
        actionController.triggerableAnim("fall_asleep",
            RawAnimation.begin().thenPlay("animation.ignivorus.fall_asleep"));
        actionController.triggerableAnim("wake_up",
            RawAnimation.begin().thenPlay("animation.ignivorus.wake_up"));
        actionController.triggerableAnim("sleep",
            RawAnimation.begin().thenLoop("animation.ignivorus.sleep"));

        // Bite ability animation
        actionController.triggerableAnim("bite",
            RawAnimation.begin().thenPlay("animation.ignivorus.bite"));
        actionController.triggerableAnim("eat",
            RawAnimation.begin().thenPlay("animation.ignivorus.eat"));

        // Wing swipe animations (Phase 2 melee attacks)
        actionController.triggerableAnim("wing_swipe_left",
            RawAnimation.begin().thenPlay("animation.ignivorus.wing_swipe_left"));
        actionController.triggerableAnim("wing_swipe_right",
            RawAnimation.begin().thenPlay("animation.ignivorus.wing_swipe_right"));

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

        // Ultimate ability animations (triggered separately in sequence, like Raevyx sleep)
        actionController.triggerableAnim("ultimate_start",
            RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_start"));
        actionController.triggerableAnim("ultimate",
            RawAnimation.begin().thenPlay("animation.ignivorus.ultimate"));
        actionController.triggerableAnim("ultimate_end",
            RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_end"));

        // Ultimate ability animations (air variants)
        actionController.triggerableAnim("ultimate_start_air",
            RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_start_air"));
        actionController.triggerableAnim("ultimate_air",
            RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_air"));
        actionController.triggerableAnim("ultimate_end_air",
            RawAnimation.begin().thenPlay("animation.ignivorus.ultimate_end_air"));

        // Death animation
        actionController.triggerableAnim("die",
            RawAnimation.begin().thenPlay("animation.ignivorus.die"));

        // Landed animation (plays after landing with rider)
        actionController.triggerableAnim("landed", LANDED);

        // Bulldoze animations
        actionController.triggerableAnim("bulldozer_enter",
            RawAnimation.begin().thenPlay("animation.ignivorus.bulldozer_enter"));
        actionController.triggerableAnim("bulldozer_exit",
            RawAnimation.begin().thenPlay("animation.ignivorus.bulldozer_exit"));

        // Phase 2 animations
        actionController.triggerableAnim("phase2_enter",
            RawAnimation.begin().thenPlay("animation.ignivorus.phase2_enter"));
        actionController.triggerableAnim("phase2_exit",
            RawAnimation.begin().thenPlay("animation.ignivorus.phase2_exit"));

        // Ambient grumbles
        actionController.triggerableAnim("ignivorus_grumble1",
            RawAnimation.begin().thenPlay("animation.ignivorus.grumble1"));
        actionController.triggerableAnim("ignivorus_grumble2",
            RawAnimation.begin().thenPlay("animation.ignivorus.grumble2"));
        actionController.triggerableAnim("ignivorus_grumble3",
            RawAnimation.begin().thenPlay("animation.ignivorus.grumble3"));
    }
}
