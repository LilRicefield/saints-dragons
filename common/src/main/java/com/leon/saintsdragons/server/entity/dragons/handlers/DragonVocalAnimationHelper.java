package com.leon.saintsdragons.server.entity.dragons.handlers;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public final class DragonVocalAnimationHelper {
    public static final String CONTROLLER = "vocal";

    private DragonVocalAnimationHelper() {
    }

    public static <T extends DragonEntity> PlayState idle(AnimationState<T> state) {
        state.getController().transitionLength(2);
        return PlayState.STOP;
    }

    public static <T extends DragonEntity> void registerGrumbles(AnimationController<T> controller, T dragon) {
        dragon.getVocalEntries().forEach((key, entry) -> {
            if (!isGrumbleKey(key) || entry.animationId() == null || entry.animationId().isEmpty()) {
                return;
            }
            controller.triggerableAnim(key, RawAnimation.begin().thenPlay(entry.animationId()));
        });
    }

    private static boolean isGrumbleKey(String key) {
        return key != null && key.contains("grumble");
    }
}
