package com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers;

import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class RiftDrakeAnimationHandler {

    // Phase 1 animations
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.rift_drake.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.rift_drake.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.rift_drake.run");

    // Phase 2 animations
    private static final RawAnimation IDLE2 = RawAnimation.begin().thenLoop("animation.rift_drake.idle2");
    private static final RawAnimation WALK2 = RawAnimation.begin().thenLoop("animation.rift_drake.walk2");
    private static final RawAnimation RUN2 = RawAnimation.begin().thenLoop("animation.rift_drake.run2");

    // Swim animations (shared between phases)
    private static final RawAnimation SWIM_IDLE = RawAnimation.begin().thenLoop("animation.rift_drake.swim_idle");
    private static final RawAnimation SWIM_CRUISE = RawAnimation.begin().thenLoop("animation.rift_drake.swim_move");
    private static final RawAnimation SWIM_UP = RawAnimation.begin().thenLoop("animation.rift_drake.swimming_up");
    private static final RawAnimation SWIM_DOWN = RawAnimation.begin().thenLoop("animation.rift_drake.swimming_down");
    private static final RawAnimation SWIM_LEFT = RawAnimation.begin().thenLoop("animation.rift_drake.swimming_left");
    private static final RawAnimation SWIM_RIGHT = RawAnimation.begin().thenLoop("animation.rift_drake.swimming_right");
    private static final RawAnimation SWIM_NEUTRAL = RawAnimation.begin().thenLoop("animation.rift_drake.swimming_off");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.rift_drake.sit");

    private final RiftDrakeEntity drake;

    private static final int MOVEMENT_TRANSITION_TICKS = 6;
    private static final int SWIM_TRANSITION_TICKS = 7;

    public RiftDrakeAnimationHandler(RiftDrakeEntity drake) {
        this.drake = drake;
    }

    /**
     * Sets up all GeckoLib animation triggers for the action controller
     */
    public void setupActionController(AnimationController<RiftDrakeEntity> actionController) {
        // Register phase transitions
        actionController.triggerableAnim("phase1",
                RawAnimation.begin().thenPlay("animation.rift_drake.phase1"));
        actionController.triggerableAnim("phase2",
                RawAnimation.begin().thenPlay("animation.rift_drake.phase2"));

        // Phase 1 melee attack (works both on ground and underwater)
        actionController.triggerableAnim("bite",
                RawAnimation.begin().thenPlay("animation.rift_drake.bite"));

        // Phase 2 rage mode bite - faster
        actionController.triggerableAnim("bite2",
                RawAnimation.begin().thenPlay("animation.rift_drake.bite2"));

        // Phase 2 claw attacks (bipedal melee)
        actionController.triggerableAnim("claw_left",
                RawAnimation.begin().thenPlay("animation.rift_drake.claw_left"));
        actionController.triggerableAnim("claw_right",
                RawAnimation.begin().thenPlay("animation.rift_drake.claw_right"));

        // Horn gore attack (works in both phases)
        actionController.triggerableAnim("horn_gore",
                RawAnimation.begin().thenPlay("animation.rift_drake.horn_gore"));

        // Roar animations (different for each phase)
        actionController.triggerableAnim("roar",
                RawAnimation.begin().thenPlay("animation.rift_drake.roar"));
        actionController.triggerableAnim("roar2",
                RawAnimation.begin().thenPlay("animation.rift_drake.roar2"));
    }

    public PlayState movementPredicate(AnimationState<RiftDrakeEntity> state) {
        boolean isSwimming = drake.isSwimming();
        boolean isNavigating = drake.getNavigation().isInProgress() && drake.getNavigation().getPath() != null;
        double horizontalSpeedSq = drake.getDeltaMovement().horizontalDistanceSqr();
        boolean isMovingLand = state.isMoving() || horizontalSpeedSq > 0.008D;
        var controller = state.getController();
        controller.setAnimationSpeed(1.0F);

        if (isSwimming) {
            controller.transitionLength(SWIM_TRANSITION_TICKS);
            RawAnimation swimAnim = (drake.isSwimmingMoving() || isNavigating) ? SWIM_CRUISE : SWIM_IDLE;
            state.setAnimation(swimAnim);
        } else if (drake.getSitProgress() > 0.5f) {
            // Drive SIT from our custom progress system only to avoid de-sync
            controller.transitionLength(4);
            state.setAnimation(SIT);
        } else {
            int groundState = drake.getEffectiveGroundState();
            boolean phaseTwo = drake.isPhaseTwoActive();
            boolean abilityActive = drake.getActiveAbility() != null;
            boolean riderControlled = drake.isVehicle() && drake.getControllingPassenger() instanceof Player player && drake.isOwnedBy(player);

            int baseTransition = MOVEMENT_TRANSITION_TICKS;
            if (riderControlled) {
                baseTransition = Math.max(3, baseTransition - 2);
            }
            if (abilityActive) {
                baseTransition = Math.max(2, baseTransition - 1);
            }

            if (groundState == 2) {
                // Running state
                controller.transitionLength(Math.max(2, baseTransition));
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

    public PlayState swimDirectionPredicate(AnimationState<RiftDrakeEntity> state) {
        var controller = state.getController();
        controller.transitionLength(SWIM_TRANSITION_TICKS);

        if (!drake.isSwimming()) {
            state.setAndContinue(SWIM_NEUTRAL);
            return PlayState.CONTINUE;
        }

        if (drake.isSwimmingUp()) {
            state.setAndContinue(SWIM_UP);
            return PlayState.CONTINUE;
        }
        if (drake.isSwimmingDown()) {
            state.setAndContinue(SWIM_DOWN);
            return PlayState.CONTINUE;
        }

        int yawDir = drake.getSwimTurnDirection();
        if (yawDir < 0) {
            state.setAndContinue(SWIM_LEFT);
        } else if (yawDir > 0) {
            state.setAndContinue(SWIM_RIGHT);
        } else {
            state.setAndContinue(SWIM_NEUTRAL);
        }
        return PlayState.CONTINUE;
    }

    public PlayState actionPredicate(AnimationState<RiftDrakeEntity> state) {
        // Action controller handles one-shot animations triggered via triggerAnim()
        state.getController().transitionLength(5);
        // Return STOP so this controller doesn't compete with movement controller when idle
        return PlayState.STOP;
    }

    public void configureMovementBlend(AnimationController<RiftDrakeEntity> controller) {
        controller.transitionLength(MOVEMENT_TRANSITION_TICKS);
    }

    public void configureSwimBlend(AnimationController<RiftDrakeEntity> controller) {
        controller.transitionLength(SWIM_TRANSITION_TICKS);
    }
}
