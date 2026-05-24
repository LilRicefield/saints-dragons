package com.leon.saintsdragons.util.animation;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public final class DragonInteractionAnimationHelper {
    public static final String CONTROLLER = "interaction";
    public static final String EAT = "eat";
    public static final String DIE = "die";

    private DragonInteractionAnimationHelper() {
    }

    public static PlayState idle(AnimationState<?> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }

    public static void triggerEat(DragonEntity dragon) {
        dragon.triggerAnim(CONTROLLER, EAT);
    }

    public static void register(AnimationController<?> controller, String trigger, RawAnimation animation) {
        controller.triggerableAnim(trigger, animation);
    }
}
