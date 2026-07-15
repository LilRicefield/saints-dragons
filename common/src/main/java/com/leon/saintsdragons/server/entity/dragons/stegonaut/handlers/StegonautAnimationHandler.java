package com.leon.saintsdragons.server.entity.dragons.stegonaut.handlers;

import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;


public class StegonautAnimationHandler {
    public static final String MOVEMENT_CONTROLLER = AnimationHelper.MOVEMENT_CONTROLLER;
    public static final String FAST_ACTION_CONTROLLER = "stegonautFastAction";
    public static final String ACTION_CONTROLLER = "stegonautAction";

    private final Stegonaut drake;

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.walk");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.run");
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.swim");
    private static final RawAnimation SLEEP_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.sleep");
    private static final RawAnimation SIT_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.sit");
    private static final RawAnimation JUMP_ANIM = RawAnimation.begin().thenPlay("animation.stegonaut.jump");
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlay("animation.stegonaut.down");
    private static final RawAnimation SIT_UP = RawAnimation.begin().thenPlay("animation.stegonaut.up");
    private static final RawAnimation FALL_ASLEEP = RawAnimation.begin().thenPlay("animation.stegonaut.fall_asleep");
    private static final RawAnimation WAKE_UP = RawAnimation.begin().thenPlay("animation.stegonaut.wake_up");
    private static final RawAnimation GROUND_SLAM = RawAnimation.begin().thenPlay("animation.stegonaut.ground_slam");
    private static final RawAnimation GROUND_SLAM2 = RawAnimation.begin().thenPlay("animation.stegonaut.ground_slam2");
    private static final AnimationHelper.Animations GROUND_ANIMATIONS =
            new AnimationHelper.Animations(IDLE_ANIM, WALK_ANIM, RUN_ANIM, SIT_ANIM, SIT_DOWN, SIT_UP, FALL_ASLEEP, SLEEP_ANIM, WAKE_UP, SWIM_ANIM, null, JUMP_ANIM);
    private static final AnimationHelper.Transitions GROUND_TRANSITIONS =
            new AnimationHelper.Transitions(4, 4, 4, 4, 4, 4, 4, 4);
    private static final int FAST_ACTION_TRANSITION_TICKS = 1;
    private static final int ACTION_TRANSITION_TICKS = 5;
    
    public StegonautAnimationHandler(Stegonaut drake) {
        this.drake = drake;
    }

    public void triggerSitDownAnimation() {
        AnimationHelper.triggerRestAnimation(drake, AnimationHelper.SIT_DOWN);
    }

    public void triggerSitUpAnimation() {
        AnimationHelper.triggerRestAnimation(drake, AnimationHelper.SIT_UP);
    }

    public void triggerFallAsleepAnimation() {
        AnimationHelper.triggerRestAnimation(drake, AnimationHelper.FALL_ASLEEP);
    }

    public void triggerWakeUpAnimation() {
        AnimationHelper.triggerRestAnimation(drake, AnimationHelper.WAKE_UP);
    }

    public PlayState handleMovementAnimation(AnimationState<Stegonaut> state) {
        if (drake.isInWaterOrBubble()) {
            state.getController().transitionLength(GROUND_TRANSITIONS.water());
            state.setAndContinue(SWIM_ANIM);
            return PlayState.CONTINUE;
        }

        PlayState restPose = AnimationHelper.tryHandleRestPose(
                state, drake, SLEEP_ANIM, SIT_ANIM, GROUND_TRANSITIONS.sleep(), GROUND_TRANSITIONS.sit()
        );
        if (restPose != null) {
            return restPose;
        }

        PlayState dance = AnimationHelper.tryHandleDance(state, drake, GROUND_TRANSITIONS.idle());
        if (dance != null) {
            return dance;
        }

        return AnimationHelper.handleGroundMovement(
                state, drake, IDLE_ANIM, WALK_ANIM, RUN_ANIM,
                GROUND_TRANSITIONS.moving(), GROUND_TRANSITIONS.idle()
        );
    }

    public void setupActionController(AnimationController<Stegonaut> actionController) {
        actionController.triggerableAnim("bite",
                RawAnimation.begin().thenPlay("animation.stegonaut.bite"));
        actionController.triggerableAnim("chin_slam",
                RawAnimation.begin().thenPlay("animation.stegonaut.chin_slam"));
        actionController.triggerableAnim("ground_eating",
                RawAnimation.begin().thenPlay("animation.stegonaut.ground_eating"));
        actionController.triggerableAnim("ground_eating_hold",
                RawAnimation.begin().thenLoop("animation.stegonaut.ground_eating_hold"));
        actionController.triggerableAnim("ground_eating_shoot",
                RawAnimation.begin().thenPlay("animation.stegonaut.ground_eating_shoot"));
        actionController.triggerableAnim("ground_eating_cancel",
                RawAnimation.begin().thenPlay("animation.stegonaut.ground_eating_cancel"));

    }

    public void setupMovementController(AnimationController<Stegonaut> controller) {
        AnimationHelper.registerRestAnimations(controller, GROUND_ANIMATIONS);
        AnimationHelper.register(controller, "ground_slam", GROUND_SLAM);
        AnimationHelper.register(controller, "ground_slam2", GROUND_SLAM2);
    }

    public PlayState fastActionPredicate(AnimationState<Stegonaut> state) {
        state.getController().transitionLength(FAST_ACTION_TRANSITION_TICKS);
        return PlayState.STOP;
    }

    public void setupFastActionController(AnimationController<Stegonaut> controller) {
        AnimationHelper.register(controller, "jump", JUMP_ANIM);
    }

    public void triggerRiderJumpAnimation() {
        drake.triggerAnim(FAST_ACTION_CONTROLLER, "jump");
    }

    public void setupInteractionController(AnimationController<Stegonaut> controller) {
        controller.triggerableAnim("stegonaut_hurt",
                RawAnimation.begin().thenPlay("animation.stegonaut.hurt"));
        controller.triggerableAnim("hurt",
                RawAnimation.begin().thenPlay("animation.stegonaut.hurt"));
        controller.triggerableAnim(AnimationHelper.DIE,
                RawAnimation.begin().thenPlay("animation.stegonaut.die"));
        controller.triggerableAnim(AnimationHelper.EAT,
                RawAnimation.begin().thenPlay("animation.stegonaut.eat"));
    }

    public PlayState actionPredicate(AnimationState<Stegonaut> state) {
        state.getController().transitionLength(ACTION_TRANSITION_TICKS);
        return PlayState.STOP;
    }
}
