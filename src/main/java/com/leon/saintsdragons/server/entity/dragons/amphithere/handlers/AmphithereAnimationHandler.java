package com.leon.saintsdragons.server.entity.dragons.amphithere.handlers;

import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * Lightweight animation helper for the Amphithere.
 */
public class AmphithereAnimationHandler {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.amphithere.idle");
    private static final RawAnimation GLIDE = RawAnimation.begin().thenLoop("animation.amphithere.glide");
    private static final RawAnimation GLIDE_DOWN = RawAnimation.begin().thenLoop("animation.amphithere.glide_down");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.amphithere.walk");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.amphithere.sit");

    private static final RawAnimation BANK_LEFT = RawAnimation.begin().thenLoop("animation.amphithere.banking_left");
    private static final RawAnimation BANK_RIGHT = RawAnimation.begin().thenLoop("animation.amphithere.banking_right");
    private static final RawAnimation BANK_OFF = RawAnimation.begin().thenLoop("animation.amphithere.banking_off");

    private static final RawAnimation PITCH_UP = RawAnimation.begin().thenLoop("animation.amphithere.pitching_up");
    private static final RawAnimation PITCH_DOWN = RawAnimation.begin().thenLoop("animation.amphithere.pitching_down");
    private static final RawAnimation PITCH_OFF = RawAnimation.begin().thenLoop("animation.amphithere.pitching_off");

    private final AmphithereEntity dragon;

    public AmphithereAnimationHandler(AmphithereEntity dragon) {
        this.dragon = dragon;
    }

    public PlayState handleMovementAnimation(AnimationState<AmphithereEntity> state) {
        state.getController().transitionLength(12); // Longer transitions for smoother animation

        if (dragon.isVehicle()) {
            state.getController().transitionLength(4);
            if (dragon.isFlying()) {
                state.setAndContinue(GLIDE);
            } else {
                int groundState = dragon.getEffectiveGroundState();
                state.setAndContinue(groundState >= 1 ? WALK : IDLE);
            }
            state.getController().setAnimationSpeed(1.0f);
            return PlayState.CONTINUE;
        }

        // Drive SIT from our custom progress system only to avoid desync
        if (dragon.getSitProgress() > 0.5f) {
            state.setAndContinue(SIT);
            return PlayState.CONTINUE;
        }

        state.getController().setAnimationSpeed(1.0f);

        if (dragon.isFlying()) {
            boolean riderDescending = dragon.isVehicle() && dragon.getControllingPassenger() != null && dragon.isGoingDown();
            if (riderDescending) {
                state.getController().transitionLength(6);
                state.setAndContinue(GLIDE_DOWN);
            } else {
                state.setAndContinue(GLIDE);
            }
            return PlayState.CONTINUE;
        }

        if (!dragon.isTakeoff() && !dragon.isLanding() && !dragon.isHovering()) {
            // Use the improved movement state detection - prioritize AI-set states for tamed dragons
            int groundState = dragon.getEffectiveGroundState(); // Use effective state for client-side consistency
            
            // Add hysteresis to prevent rapid animation changes
            if (groundState == 2) {
                // Running state
                state.setAndContinue(WALK); // Fallback to walk for now (no run animation yet)
            } else if (groundState == 1) {
                // Walking state
                state.setAndContinue(WALK);
            } else if (dragon.isRunning()) {
                // Fallback to running check
                state.setAndContinue(WALK);
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

    public PlayState bankingPredicate(AnimationState<AmphithereEntity> state) {
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

    public PlayState pitchingPredicate(AnimationState<AmphithereEntity> state) {
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

    public void setupActionController(AnimationController<AmphithereEntity> controller) {
        controller.triggerableAnim("roar",
                RawAnimation.begin().thenPlay("animation.amphithere.roar"));
        controller.triggerableAnim("roar_ground",
                RawAnimation.begin().thenPlay("animation.amphithere.roar_ground"));
        controller.triggerableAnim("roar_air",
                RawAnimation.begin().thenPlay("animation.amphithere.roar_air"));
        controller.triggerableAnim("bite_ground",
                RawAnimation.begin().thenPlay("animation.amphithere.bite_ground"));
        controller.triggerableAnim("bite_air",
                RawAnimation.begin().thenPlay("animation.amphithere.bite_air"));
    }
    public PlayState actionPredicate(AnimationState<AmphithereEntity> state) {
        state.getController().transitionLength(5);
        return PlayState.STOP;
    }
}

