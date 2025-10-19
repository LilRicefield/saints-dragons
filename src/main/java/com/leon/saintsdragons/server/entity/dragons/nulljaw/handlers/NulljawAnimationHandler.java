package com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers;

import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public record NulljawAnimationHandler(Nulljaw drake) {

    // Phase 1 animations
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.nulljaw.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.nulljaw.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.nulljaw.run");

    // Phase 2 animations
    private static final RawAnimation IDLE2 = RawAnimation.begin().thenLoop("animation.nulljaw.idle2");
    private static final RawAnimation WALK2 = RawAnimation.begin().thenLoop("animation.nulljaw.walk2");
    private static final RawAnimation RUN2 = RawAnimation.begin().thenLoop("animation.nulljaw.run2");

    // Swim animations (shared between phases)
    private static final RawAnimation SWIM_IDLE = RawAnimation.begin().thenLoop("animation.nulljaw.swim_idle");
    private static final RawAnimation SWIM_CRUISE = RawAnimation.begin().thenLoop("animation.nulljaw.swim_move");
    private static final RawAnimation SWIM_UP = RawAnimation.begin().thenLoop("animation.nulljaw.swimming_up");
    private static final RawAnimation SWIM_DOWN = RawAnimation.begin().thenLoop("animation.nulljaw.swimming_down");
    private static final RawAnimation SWIM_LEFT = RawAnimation.begin().thenLoop("animation.nulljaw.swimming_left");
    private static final RawAnimation SWIM_RIGHT = RawAnimation.begin().thenLoop("animation.nulljaw.swimming_right");
    private static final RawAnimation SWIM_NEUTRAL = RawAnimation.begin().thenLoop("animation.nulljaw.swimming_off");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.nulljaw.sit");

    private static final int MOVEMENT_TRANSITION_TICKS = 6;
    private static final int SWIM_TRANSITION_TICKS = 7;

    /**
     * Sets up all GeckoLib animation triggers for the action controller (combat abilities)
     */
    public void setupActionController(AnimationController<Nulljaw> actionController) {
        // Register phase transitions
        actionController.triggerableAnim("phase1",
                RawAnimation.begin().thenPlay("animation.nulljaw.phase1"));
        actionController.triggerableAnim("phase2",
                RawAnimation.begin().thenPlay("animation.nulljaw.phase2"));

        // Phase 1 melee attack (works both on ground and underwater)
        actionController.triggerableAnim("bite",
                RawAnimation.begin().thenPlay("animation.nulljaw.bite"));

        // Phase 2 rage mode bite - faster
        actionController.triggerableAnim("bite2",
                RawAnimation.begin().thenPlay("animation.nulljaw.bite2"));

        // Phase 2 claw attacks (bipedal melee)
        actionController.triggerableAnim("claw_left",
                RawAnimation.begin().thenPlay("animation.nulljaw.claw_left"));
        actionController.triggerableAnim("claw_right",
                RawAnimation.begin().thenPlay("animation.nulljaw.claw_right"));

        // Horn gore attack (works in both phases)
        actionController.triggerableAnim("horn_gore",
                RawAnimation.begin().thenPlay("animation.nulljaw.horn_gore"));

        // Roar animations (different for each phase) - player triggered
        actionController.triggerableAnim("roar",
                RawAnimation.begin().thenPlay("animation.nulljaw.roar"));
        actionController.triggerableAnim("roar2",
                RawAnimation.begin().thenPlay("animation.nulljaw.roar2"));
    }

    /**
     * Sets up all GeckoLib animation triggers for the ambient controller (AI-triggered vocal animations)
     */
    public void setupAmbientController(AnimationController<Nulljaw> ambientController) {
        // Ambient/idle sounds - triggered by random ticks and AI
        ambientController.triggerableAnim("grumble1",
                RawAnimation.begin().thenPlay("animation.nulljaw.grumble1"));
        ambientController.triggerableAnim("grumble2",
                RawAnimation.begin().thenPlay("animation.nulljaw.grumble2"));
        ambientController.triggerableAnim("grumble3",
                RawAnimation.begin().thenPlay("animation.nulljaw.grumble3"));
    }

    public PlayState movementPredicate(AnimationState<Nulljaw> state) {
        boolean isSwimming = drake.isSwimming();
        boolean isInWater = drake.isInWater();
        boolean isNavigating = drake.getNavigation().isInProgress() && drake.getNavigation().getPath() != null;
        double horizontalSpeedSq = drake.getDeltaMovement().horizontalDistanceSqr();
        double totalSpeedSq = drake.getDeltaMovement().lengthSqr();
        boolean isMovingLand = state.isMoving() || horizontalSpeedSq > 0.008D;
        var controller = state.getController();
        controller.setAnimationSpeed(1.0F);

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

    public PlayState swimDirectionPredicate(AnimationState<Nulljaw> state) {
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

    public PlayState actionPredicate(AnimationState<Nulljaw> state) {
        // Action controller handles one-shot combat animations triggered via triggerAnim()
        state.getController().transitionLength(5);
        // Return STOP so this controller doesn't compete with movement controller when idle
        return PlayState.STOP;
    }

    public PlayState ambientPredicate(AnimationState<Nulljaw> state) {
        // Ambient controller handles vocal/ambient animations triggered via triggerAnim()
        state.getController().transitionLength(3);
        // Return STOP so this controller doesn't compete with other controllers when idle
        return PlayState.STOP;
    }

    public void configureMovementBlend(AnimationController<Nulljaw> controller) {
        controller.transitionLength(MOVEMENT_TRANSITION_TICKS);
    }

    public void configureSwimBlend(AnimationController<Nulljaw> controller) {
        controller.transitionLength(SWIM_TRANSITION_TICKS);
    }
}
