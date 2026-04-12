package com.leon.saintsdragons.server.entity.dragons.varasuchus.handlers;

import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public record VarasuchusAnimationHandler(Varasuchus drake) {

    // ===== ANIMATION TRIGGER HELPERS =====
    public void triggerSitDownAnimation() {
        boolean isPhaseTwo = drake.isPhaseTwoActive();
        drake.triggerAnim("action", isPhaseTwo ? "sit_down2" : "sit_down");
    }

    public void triggerSitUpAnimation() {
        boolean isPhaseTwo = drake.isPhaseTwoActive();
        drake.triggerAnim("action", isPhaseTwo ? "sit_up2" : "sit_up");
    }

    public void triggerFallAsleepAnimation() {
        drake.triggerAnim("action", "fall_asleep");
    }

    public void triggerSleepAnimation() {
        drake.triggerAnim("action", "sleep");
    }

    public void triggerWakeUpAnimation() {
        drake.triggerAnim("action", "wake_up");
    }

    // Phase 1 animations
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.varasuchus.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.varasuchus.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.varasuchus.run");
    private static final RawAnimation BUCKING = RawAnimation.begin().thenLoop("animation.varasuchus.bucking");
    private static final RawAnimation THRASHING_UNDERWATER = RawAnimation.begin().thenLoop("animation.varasuchus.thrashing_underwater");

    // Phase 2 animations
    private static final RawAnimation IDLE2 = RawAnimation.begin().thenLoop("animation.varasuchus.idle2");
    private static final RawAnimation WALK2 = RawAnimation.begin().thenLoop("animation.varasuchus.walk2");
    private static final RawAnimation RUN2 = RawAnimation.begin().thenLoop("animation.varasuchus.run2");

    // Swim animations (shared between phases)
    private static final RawAnimation SWIM_IDLE = RawAnimation.begin().thenLoop("animation.varasuchus.swim_idle");
    private static final RawAnimation SWIM_CRUISE = RawAnimation.begin().thenLoop("animation.varasuchus.swim_move");

    // Phase 1 sitting/sleeping animations
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.varasuchus.sit");
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlay("animation.varasuchus.down");
    private static final RawAnimation SIT_UP = RawAnimation.begin().thenPlay("animation.varasuchus.up");
    private static final RawAnimation FALL_ASLEEP = RawAnimation.begin().thenPlay("animation.varasuchus.fall_asleep");
    private static final RawAnimation SLEEP_LOOP = RawAnimation.begin().thenLoop("animation.varasuchus.sleep");
    private static final RawAnimation WAKE_UP = RawAnimation.begin().thenPlay("animation.varasuchus.wake_up");

    // Phase 2 sitting transitions
    private static final RawAnimation SIT_DOWN2 = RawAnimation.begin().thenPlay("animation.varasuchus.down2");
    private static final RawAnimation SIT_UP2 = RawAnimation.begin().thenPlay("animation.varasuchus.up2");

    private static final int MOVEMENT_TRANSITION_TICKS = 6;
    private static final int SWIM_TRANSITION_TICKS = 7;

    /**
     * Sets up all GeckoLib animation triggers for the action controller (combat abilities)
     */
    public void setupActionController(AnimationController<Varasuchus> actionController) {
        // Register phase transitions
        actionController.triggerableAnim("phase1",
                RawAnimation.begin().thenPlay("animation.varasuchus.phase1"));

        actionController.triggerableAnim("phase2",
                RawAnimation.begin().thenPlay("animation.varasuchus.phase2"));
        actionController.triggerableAnim("phase2_underwater",
                RawAnimation.begin().thenPlay("animation.varasuchus.phase2_underwater"));

        // Phase 1 revert animations (ground and underwater)
        actionController.triggerableAnim("phase1_underwater",
                RawAnimation.begin().thenPlay("animation.varasuchus.phase1_underwater"));

        // Phase 1 melee attack (works both on ground and underwater)
        actionController.triggerableAnim("bite",
                RawAnimation.begin().thenPlay("animation.varasuchus.bite"));

        // Phase 2 rage mode bite - faster
        actionController.triggerableAnim("bite2",
                RawAnimation.begin().thenPlay("animation.varasuchus.bite2"));

        // Horn gore attack (works in both phases)
        actionController.triggerableAnim("horn_gore",
                RawAnimation.begin().thenPlay("animation.varasuchus.horn_gore"));

        // Roar animations (different for each phase) - player triggered
        actionController.triggerableAnim("roar",
                RawAnimation.begin().thenPlay("animation.varasuchus.roar"));

        // Eat animation - triggered when feeding
        actionController.triggerableAnim("eat",
                RawAnimation.begin().thenPlay("animation.varasuchus.eat"));

        // Sit & sleep transitions (Phase 1)
        actionController.triggerableAnim("sit_down", SIT_DOWN);
        actionController.triggerableAnim("sit_up", SIT_UP);
        actionController.triggerableAnim("fall_asleep", FALL_ASLEEP);
        actionController.triggerableAnim("sleep", SLEEP_LOOP);
        actionController.triggerableAnim("wake_up", WAKE_UP);

        // Sit & sleep transitions (Phase 2)
        actionController.triggerableAnim("sit_down2", SIT_DOWN2);
        actionController.triggerableAnim("sit_up2", SIT_UP2);

        // Ambient vocal grumbles now run through the action controller so they can blend with look control
        actionController.triggerableAnim("grumble1",
                RawAnimation.begin().thenPlay("animation.varasuchus.grumble1"));
        actionController.triggerableAnim("grumble2",
                RawAnimation.begin().thenPlay("animation.varasuchus.grumble2"));
        actionController.triggerableAnim("grumble3",
                RawAnimation.begin().thenPlay("animation.varasuchus.grumble3"));
    }

    public PlayState movementPredicate(AnimationState<Varasuchus> state) {

        // Stop movement animations during actual death sequence, not just at low health
        if (drake.isDying()) {
            return PlayState.STOP;
        }

        var controller = state.getController();
        controller.setAnimationSpeed(1.0F);

        // CLIENT-SIDE GRACE PERIOD: Prevent T-pose on world rejoin with shaders
        // Wait for entity data to sync from server before processing animations
        if (drake.level().isClientSide && !drake.isClientAnimationReady()) {
            state.setAndContinue(IDLE);
            return PlayState.CONTINUE;
        }

        // Wild ride uses the same rider-lock path, but it should still show the bucking loop.
        if (drake.isWildRideAnimationActive()) {
            controller.transitionLength(2);
            state.setAndContinue(drake.isInWaterOrBubble() ? THRASHING_UNDERWATER : BUCKING);
            return PlayState.CONTINUE;
        }

        // CRITICAL: Stop movement controller when controls are locked (e.g., phase shift transition)
        // This prevents movement animations from competing with action controller animations
        if (drake.areRiderControlsLocked()) {
            return PlayState.STOP;
        }

        boolean isSwimming = drake.isSwimming();
        boolean isInWater = drake.isInWaterOrBubble();
        boolean isNavigating = drake.getNavigation().isInProgress() && drake.getNavigation().getPath() != null;
        double horizontalSpeedSq = drake.getDeltaMovement().horizontalDistanceSqr();
        double totalSpeedSq = drake.getDeltaMovement().lengthSqr();
        boolean isMovingLand = state.isMoving() || horizontalSpeedSq > 0.008D;

        // Check swimming state - use isInWater as a fallback if isSwimming isn't synced yet
        if (isSwimming || isInWater) {
            controller.transitionLength(SWIM_TRANSITION_TICKS);

            // Determine if swimming and moving
            boolean isSwimmingMoving;

            // When being ridden, use rider input values instead of entity zza/yya
            if (drake.isVehicle() && drake.getControllingPassenger() != null) {
                // Check rider forward/strafe inputs
                float riderFwd = Math.abs(drake.getLastRiderForward());
                float riderStr = Math.abs(drake.getLastRiderStrafe());
                boolean riderMoving = riderFwd > 0.05F || riderStr > 0.05F;
                // Also check velocity as fallback
                isSwimmingMoving = riderMoving || totalSpeedSq > 0.004D;
            } else {
                // AI-controlled: use navigation, velocity, and movement intention
                isSwimmingMoving = drake.isSwimmingMoving() || isNavigating ||
                        totalSpeedSq > 0.002D ||
                        Math.abs(drake.zza) > 0.01F ||
                        Math.abs(drake.yya) > 0.01F;
            }

            RawAnimation swimAnim = isSwimmingMoving ? SWIM_CRUISE : SWIM_IDLE;
            state.setAnimation(swimAnim);
        } else if (drake.isSleeping() && !drake.isSleepingEntering() && !drake.isSleepingExiting()) {
            // Continuously apply sleep loop animation (survives chunk reload)
            controller.transitionLength(6);
            state.setAndContinue(SLEEP_LOOP);
            return PlayState.CONTINUE;
        } else if (drake.isSleepingEntering() || drake.isSleepingExiting()) {
            // Transition animations are triggered, don't interfere
            return PlayState.STOP;
        } else if (drake.getSitProgress() > 0.5f) {
            // Drive SIT from our custom progress system only to avoid de-sync
            controller.transitionLength(4);
            state.setAnimation(SIT);
        } else {
            int groundState = drake.getEffectiveGroundState();
            boolean phaseTwo = drake.isPhaseTwoActive();
            boolean abilityActive = drake.getActiveAbility() != null;
            boolean riderControlled = drake.isVehicle() && drake.getControllingPassenger() instanceof Player player && drake.isOwnedBy(player);

            // Check if drake is in aggressive combat (running toward target)
            boolean isAggressive = drake.isAggressive() && isMovingLand;

            int baseTransition = MOVEMENT_TRANSITION_TICKS;
            if (riderControlled) {
                baseTransition = Math.max(3, baseTransition - 2);
            }
            if (abilityActive) {
                baseTransition = Math.max(2, baseTransition - 1);
            }

            if (groundState == 2 || isAggressive) {
                // Running state (either rider-controlled or AI aggressive combat)
                controller.transitionLength(baseTransition);
                state.setAnimation(phaseTwo ? RUN2 : RUN);
            } else if (groundState == 1 || isMovingLand) {
                // Walking state or moving
                controller.transitionLength(Math.max(3, baseTransition + 1));
                state.setAnimation(phaseTwo ? WALK2 : WALK);
            } else {
                // Idle state
                controller.transitionLength(Math.max(3, baseTransition + (abilityActive ? 2 : 0)));
                state.setAnimation(phaseTwo ? IDLE2 : IDLE);
            }
        }
        return PlayState.CONTINUE;
    }

    public PlayState actionPredicate(AnimationState<Varasuchus> state) {
        // Action controller handles one-shot combat animations triggered via triggerAnim()
        state.getController().transitionLength(5);
        // Return STOP so this controller doesn't compete with movement controller when idle
        return PlayState.STOP;
    }


    public PlayState instantActionPredicate(AnimationState<Varasuchus> state) {
        state.getController().transitionLength(2);
        return PlayState.STOP;
    }

    public void setupInstantActionController(AnimationController<Varasuchus> controller) {
        controller.triggerableAnim("tail_swipe_left",
                RawAnimation.begin().thenPlay("animation.varasuchus.tail_swipe_left"));
        controller.triggerableAnim("tail_attack_left",
                RawAnimation.begin().thenPlay("animation.varasuchus.tail_attack_left"));
        controller.triggerableAnim("tail_attack_right",
                RawAnimation.begin().thenPlay("animation.varasuchus.tail_attack_right"));
        controller.triggerableAnim("phase2_dash_left",
                RawAnimation.begin().thenPlay("animation.varasuchus.phase2_dash_left"));
        controller.triggerableAnim("phase2_dash_right",
                RawAnimation.begin().thenPlay("animation.varasuchus.phase2_dash_right"));
        controller.triggerableAnim("claw_left",
                RawAnimation.begin().thenPlay("animation.varasuchus.claw_left"));
        controller.triggerableAnim("claw_right",
                RawAnimation.begin().thenPlay("animation.varasuchus.claw_right"));
        controller.triggerableAnim("run_and_claw_left",
                RawAnimation.begin().thenPlay("animation.varasuchus.run_and_claw_left"));
        controller.triggerableAnim("run_and_claw_right",
                RawAnimation.begin().thenPlay("animation.varasuchus.run_and_claw_right"));
        controller.triggerableAnim("slash_barrage",
                RawAnimation.begin().thenPlay("animation.varasuchus.slash_barrage"));
        controller.triggerableAnim("varasuchus_hurt",
                RawAnimation.begin().thenPlay("animation.varasuchus.hurt"));
        controller.triggerableAnim("varasuchus_die",
                RawAnimation.begin().thenPlay("animation.varasuchus.die"));
    }
}
