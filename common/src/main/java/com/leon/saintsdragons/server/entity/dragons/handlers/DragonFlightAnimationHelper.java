package com.leon.saintsdragons.server.entity.dragons.handlers;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.function.Function;

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

    public static <T extends DragonEntity> AnimationController<T> createController(T dragon,
                                                                                   int transitionTicks,
                                                                                   Function<AnimationState<T>, PlayState> predicate) {
        int safeTransitionTicks = Math.max(0, transitionTicks);
        return new AnimationController<>(dragon, CONTROLLER, safeTransitionTicks, state -> {
            state.getController().transitionLength(safeTransitionTicks);
            return predicate.apply(state);
        });
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

    public static <T extends DragonEntity> PlayState handleState(AnimationState<T> state,
                                                                 DragonFlightStateEvaluator.VisualState visualState,
                                                                 Animations animations,
        Transitions transitions) {
        if (visualState == null) {
            return PlayState.STOP;
        }

        RawAnimation animation = switch (visualState) {
            case TAKEOFF -> animations.takeoff();
            case GLIDE_DOWN, LANDING -> animations.glideDown();
            case FLY_IDLE -> animations.flyIdle();
            case SPRINT_FLAP -> animations.sprintFlap();
            case FLAP -> animations.flap();
            case GLIDE -> animations.flyGlide();
            default -> null;
        };

        if (animation == null) {
            return PlayState.STOP;
        }

        int transitionTicks = getTransitionTicks(visualState, transitions);
        state.getController().transitionLength(transitionTicks);
        state.setAndContinue(animation);
        return PlayState.CONTINUE;
    }

    public static <T extends DragonEntity> PlayState handleTakeoff(AnimationState<T> state,
                                                                   boolean riderTakeoff,
                                                                   Animations animations,
                                                                   Transitions transitions) {
        RawAnimation animation = riderTakeoff && animations.riderTakeoff() != null
                ? animations.riderTakeoff()
                : animations.takeoff();
        if (animation == null) {
            return PlayState.STOP;
        }
        state.getController().transitionLength(transitions.takeoff());
        state.setAndContinue(animation);
        return PlayState.CONTINUE;
    }

    public static <T extends DragonEntity> PlayState handleLanded(AnimationState<T> state,
                                                                  Animations animations,
                                                                  Transitions transitions) {
        if (animations.landed() == null) {
            return PlayState.STOP;
        }
        state.getController().transitionLength(transitions.landed());
        state.setAndContinue(animations.landed());
        return PlayState.CONTINUE;
    }

    private static int getTransitionTicks(DragonFlightStateEvaluator.VisualState visualState, Transitions transitions) {
        return switch (visualState) {
            case TAKEOFF -> transitions.takeoff();
            case LANDING -> transitions.landing();
            case GLIDE_DOWN -> transitions.glideDown();
            case FLY_IDLE -> transitions.flyIdle();
            case SPRINT_FLAP -> transitions.sprintFlap();
            case FLAP -> transitions.flap();
            case GLIDE -> transitions.flyGlide();
            default -> 0;
        };
    }

    public static <T extends DragonEntity> void register(AnimationController<T> controller,
                                                        String trigger,
                                                        RawAnimation animation) {
        if (trigger != null && !trigger.isEmpty() && animation != null) {
            controller.triggerableAnim(trigger, animation);
        }
    }

    public record Animations(
            RawAnimation takeoff,
            RawAnimation riderTakeoff,
            RawAnimation landed,
            RawAnimation flyGlide,
            RawAnimation glideDown,
            RawAnimation flyIdle,
            RawAnimation flap,
            RawAnimation sprintFlap
    ) {
    }

    public record Transitions(
            int takeoff,
            int flyGlide,
            int glideDown,
            int landing,
            int flyIdle,
            int flap,
            int sprintFlap,
            int landed
    ) {
    }
}
