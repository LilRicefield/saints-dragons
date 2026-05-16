package com.leon.saintsdragons.server.entity.dragons.handlers;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public final class DragonFlightAnimationHelper {
    public static final String CONTROLLER = "flight";
    public static final int DEFAULT_TRANSITION_TICKS = 1;
    public static final String TAKEOFF = "takeoff";
    public static final String RIDER_TAKEOFF = "rider_takeoff";
    public static final String LANDED = "landed";
    public static final String PHASE2_TAKEOFF = "phase2_takeoff";
    public static final String PHASE2_LANDED = "phase2_landed";

    private DragonFlightAnimationHelper() {
    }

    public static <T extends DragonEntity> AnimationController<T> createController(T dragon, int transitionTicks) {
        int safeTransitionTicks = Math.max(0, transitionTicks);
        return new AnimationController<>(dragon, CONTROLLER, safeTransitionTicks,
                state -> idle(state, safeTransitionTicks));
    }

    public static <T extends DragonEntity> PlayState idle(AnimationState<T> state) {
        return idle(state, DEFAULT_TRANSITION_TICKS);
    }

    public static <T extends DragonEntity> PlayState idle(AnimationState<T> state, int transitionTicks) {
        state.getController().transitionLength(Math.max(0, transitionTicks));
        return PlayState.STOP;
    }

    public static <T extends DragonEntity> void registerStandard(AnimationController<T> controller,
                                                                 RawAnimation takeoff,
                                                                 RawAnimation riderTakeoff,
                                                                 RawAnimation landed) {
        register(controller, TAKEOFF, takeoff);
        register(controller, RIDER_TAKEOFF, riderTakeoff);
        register(controller, LANDED, landed);
    }

    public static <T extends DragonEntity> void register(AnimationController<T> controller,
                                                        String trigger,
                                                        RawAnimation animation) {
        if (trigger != null && !trigger.isEmpty() && animation != null) {
            controller.triggerableAnim(trigger, animation);
        }
    }
}
