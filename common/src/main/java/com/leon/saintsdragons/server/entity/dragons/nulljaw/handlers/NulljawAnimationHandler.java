package com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers;

import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public final class NulljawAnimationHandler {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.nulljaw.idle");
    private static final RawAnimation HOVER = RawAnimation.begin().thenLoop("animation.nulljaw.hover");
    private static final RawAnimation HAND_HOLDING = RawAnimation.begin().thenLoop("animation.nulljaw.hand_holding");
    private static final RawAnimation GRUMBLE_1 = RawAnimation.begin().thenPlay("animation.nulljaw.grumble1");
    private static final RawAnimation GRUMBLE_2 = RawAnimation.begin().thenPlay("animation.nulljaw.grumble2");
    private static final RawAnimation GRUMBLE_3 = RawAnimation.begin().thenPlay("animation.nulljaw.grumble3");
    private static final RawAnimation EAT = RawAnimation.begin().thenPlay("animation.nulljaw.eat");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.nulljaw.hurt");
    private static final RawAnimation DIE = RawAnimation.begin().thenPlay("animation.nulljaw.die");

    private final Nulljaw dragon;

    public NulljawAnimationHandler(Nulljaw dragon) {
        this.dragon = dragon;
    }

    public PlayState movementPredicate(AnimationState<Nulljaw> state) {
        if (dragon.isDeadOrDying()) {
            return PlayState.STOP;
        }

        state.getController().transitionLength(6);
        if (dragon.shouldUseHoverAnimation()) {
            state.setAndContinue(HOVER);
        } else {
            state.setAndContinue(IDLE);
        }
        return PlayState.CONTINUE;
    }

    public PlayState actionPredicate(AnimationState<Nulljaw> state) {
        state.getController().transitionLength(2);
        return PlayState.STOP;
    }

    public PlayState instantPredicate(AnimationState<Nulljaw> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }

    public PlayState mountedPredicate(AnimationState<Nulljaw> state) {
        state.getController().transitionLength(2);
        if (!dragon.isVehicle()) {
            return PlayState.STOP;
        }

        state.setAndContinue(HAND_HOLDING);
        return PlayState.CONTINUE;
    }

    public void setupActionController(AnimationController<Nulljaw> controller) {
        controller.triggerableAnim("grumble1", GRUMBLE_1);
        controller.triggerableAnim("grumble2", GRUMBLE_2);
        controller.triggerableAnim("grumble3", GRUMBLE_3);
        controller.triggerableAnim("eat", EAT);
    }

    public void setupInstantController(AnimationController<Nulljaw> controller) {
        controller.triggerableAnim("nulljaw_hurt", HURT);
        controller.triggerableAnim("nulljaw_die", DIE);
    }
}
