package com.leon.saintsdragons.server.entity.dragons.stegonaut.handlers;

import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.util.animation.DragonInteractionAnimationHelper;
import com.leon.saintsdragons.util.animation.MovementAnimationHelper;
import com.leon.saintsdragons.util.animation.DragonStateAnimationHelper;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;


public class StegonautAnimationHandler {
    public static final String FAST_ACTION_CONTROLLER = "stegonautFastAction";
    public static final String ACTION_CONTROLLER = "stegonautAction";

    private final Stegonaut drake;

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.walk");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.run");
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.swim");
    private static final RawAnimation SLEEP_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.sleep");
    private static final RawAnimation SIT_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.sit");
    private static final RawAnimation JUMP_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.jump");
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlay("animation.stegonaut.down");
    private static final RawAnimation SIT_UP = RawAnimation.begin().thenPlay("animation.stegonaut.up");
    private static final RawAnimation FALL_ASLEEP = RawAnimation.begin().thenPlay("animation.stegonaut.fall_asleep");
    private static final RawAnimation WAKE_UP = RawAnimation.begin().thenPlay("animation.stegonaut.wake_up");
    
    public StegonautAnimationHandler(Stegonaut drake) {
        this.drake = drake;
    }

    public void triggerSitDownAnimation() {
        drake.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.SIT_DOWN);
    }

    public void triggerSitUpAnimation() {
        drake.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.SIT_UP);
    }

    public void triggerFallAsleepAnimation() {
        drake.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.FALL_ASLEEP);
    }

    public void triggerSleepAnimation() {
        drake.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.SLEEP);
    }

    public void triggerWakeUpAnimation() {
        drake.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.WAKE_UP);
    }

    public PlayState handleMovementAnimation(AnimationState<Stegonaut> state) {
        state.getController().transitionLength(8);
        if (drake.isInWaterOrBubble()) {
            state.getController().transitionLength(6);
            state.setAndContinue(SWIM_ANIM);
            return PlayState.CONTINUE;
        } else if (drake.isRiddenGroundJumpAirborne()) {
            state.getController().transitionLength(1);
            state.setAndContinue(JUMP_ANIM);
            return PlayState.CONTINUE;
        }

        PlayState restPose = MovementAnimationHelper.tryHandleRestPose(state, drake, SLEEP_ANIM, SIT_ANIM, 6, 4);
        if (restPose != null) {
            return restPose;
        }

        return MovementAnimationHelper.handleGroundMovement(state, drake, IDLE_ANIM, WALK_ANIM, RUN_ANIM);
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

    public void setupStateController(AnimationController<Stegonaut> controller) {
        DragonStateAnimationHelper.register(controller, DragonStateAnimationHelper.SIT_DOWN, SIT_DOWN);
        DragonStateAnimationHelper.register(controller, DragonStateAnimationHelper.SIT_UP, SIT_UP);
        DragonStateAnimationHelper.register(controller, DragonStateAnimationHelper.FALL_ASLEEP, FALL_ASLEEP);
        DragonStateAnimationHelper.register(controller, DragonStateAnimationHelper.SLEEP, SLEEP_ANIM);
        DragonStateAnimationHelper.register(controller, DragonStateAnimationHelper.WAKE_UP, WAKE_UP);
    }

    public PlayState fastActionPredicate(AnimationState<Stegonaut> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }

    public void setupFastActionController(AnimationController<Stegonaut> controller) {
    }

    public void setupInteractionController(AnimationController<Stegonaut> controller) {
        controller.triggerableAnim("stegonaut_hurt",
                RawAnimation.begin().thenPlay("animation.stegonaut.hurt"));
        controller.triggerableAnim("hurt",
                RawAnimation.begin().thenPlay("animation.stegonaut.hurt"));
        controller.triggerableAnim(DragonInteractionAnimationHelper.DIE,
                RawAnimation.begin().thenPlay("animation.stegonaut.die"));
        controller.triggerableAnim(DragonInteractionAnimationHelper.EAT,
                RawAnimation.begin().thenPlay("animation.stegonaut.eat"));
    }

    public PlayState actionPredicate(AnimationState<Stegonaut> state) {
        state.getController().transitionLength(5);
        return PlayState.STOP;
    }
}
