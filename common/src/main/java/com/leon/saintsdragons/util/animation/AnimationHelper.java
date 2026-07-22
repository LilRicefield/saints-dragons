package com.leon.saintsdragons.util.animation;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.DancingEntity;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.function.Function;

public final class AnimationHelper {
    public static final String MOVEMENT_CONTROLLER = "movement";
    public static final String FLIGHT_CONTROLLER = "flight";
    public static final String INTERACTION_CONTROLLER = "interaction";
    public static final String VOCAL_CONTROLLER = "vocal";
    public static final String SIT_DOWN = "sit_down";
    public static final String SIT_UP = "sit_up";
    public static final String FALL_ASLEEP = "fall_asleep";
    public static final String SLEEP = "sleep";
    public static final String WAKE_UP = "wake_up";
    public static final String TAKEOFF = "takeoff";
    public static final String RIDER_TAKEOFF = "rider_takeoff";
    public static final String LANDED = "landed";
    public static final String PHASE2_TAKEOFF = "phase2_takeoff";
    public static final String PHASE2_LANDED = "phase2_landed";
    public static final String EAT = "eat";
    public static final String DIE = "die";
    public static final String DANCE = "dance";

    private AnimationHelper() {
    }

    @SafeVarargs
    public static <T extends DragonEntity> void registerSoundKeyframes(T dragon, AnimationController<T>... controllers) {
        for (AnimationController<T> controller : controllers) {
            controller.setSoundKeyframeHandler(event -> dragon.playAnimationKeyframeSound(event.getKeyframeData().getSound()));
        }
    }

    @SafeVarargs
    public static <T extends GeoAnimatable> void registerStepKeyframes(Entity entity, AnimationController<T>... controllers) {
        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityTypeId == null) {
            return;
        }

        for (AnimationController<T> controller : controllers) {
            controller.setSoundKeyframeHandler(event -> {
                String soundKey = event.getKeyframeData().getSound();
                ResourceLocation soundId = "step".equals(soundKey)
                        ? new ResourceLocation(entityTypeId.getNamespace(), entityTypeId.getPath() + "_step")
                        : soundKey != null && soundKey.endsWith("_step")
                                ? new ResourceLocation(entityTypeId.getNamespace(), soundKey)
                                : null;
                SoundEvent sound = soundId != null ? BuiltInRegistries.SOUND_EVENT.get(soundId) : null;
                if (sound != null) {
                    entity.level().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), sound,
                            SoundSource.NEUTRAL, 1.0f, 1.0f, false);
                }
            });
        }
    }

    public static <T extends RideableDragonBase> void registerRestAnimations(AnimationController<T> controller, Animations animations) {
        register(controller, SLEEP, animations.sleep());
        register(controller, SIT_DOWN, animations.sitDown());
        register(controller, SIT_UP, animations.sitUp());
        register(controller, FALL_ASLEEP, animations.fallAsleep());
        register(controller, WAKE_UP, animations.wakeUp());
    }

    public static void triggerRestAnimation(RideableDragonBase dragon, String animation) {
        dragon.triggerAnim(MOVEMENT_CONTROLLER, animation);
    }

    public static PlayState idle(AnimationState<?> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }

    public static <T extends DragonEntity> PlayState vocalIdle(AnimationState<T> state) {
        state.getController().transitionLength(2);
        return PlayState.STOP;
    }

    public static PlayState interactionIdle(AnimationState<?> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }


    public static <T extends RideableDragonBase> PlayState handleGrounded(AnimationState<T> state,
                                                                          T dragon,
                                                                          Animations animations,
                                                                          Transitions transitions,
                                                                          SpecialStates<T> specialStates) {
        var controller = state.getController();
        boolean aerialState = dragon.isFlying() || dragon.isTakeoff() || dragon.isLanding() || dragon.isHovering();

        if (dragon.isDying()) {
            return PlayState.STOP;
        }

        if (specialStates.riderControlsLocked(dragon)) {
            return PlayState.STOP;
        }

        if (aerialState) {
            return PlayState.STOP;
        }

        if (specialStates.inWater(dragon)) {
            controller.transitionLength(transitions.water());
            setAndContinue(state, animations.swim());
            return PlayState.CONTINUE;
        }

        if (dragon.isSleepTransitioning()) {
            return PlayState.STOP;
        }

        if (dragon.isSleeping()) {
            controller.transitionLength(transitions.sleep());
            setAndContinue(state, animations.sleep());
            return PlayState.CONTINUE;
        }

        float sitProgress = dragon.getSitProgress();
        if (sitProgress >= dragon.maxSitTicks()) {
            controller.transitionLength(transitions.sit());
            setAndContinue(state, animations.sit());
            return PlayState.CONTINUE;
        }
        if (sitProgress > 0f) {
            return PlayState.STOP;
        }

        PlayState dance = tryHandleDance(state, dragon, transitions.idle());
        if (dance != null) {
            return dance;
        }

        PlayState special = specialStates.handle(state, dragon, animations, transitions);
        if (special != null) {
            return special;
        }

        if (specialStates.falling(dragon)) {
            controller.transitionLength(transitions.falling());
            setAndContinue(state, animations.falling());
            return PlayState.CONTINUE;
        }

        if (specialStates.tamingStunned(dragon)) {
            controller.transitionLength(transitions.idle());
            setAndContinue(state, animations.stunned());
            return PlayState.CONTINUE;
        }

        return handleGroundMovement(
                state,
                dragon,
                animations.idle(),
                animations.walk(),
                animations.run(),
                transitions.moving(),
                transitions.idle()
        );
    }

    public static <T extends RideableDragonBase> void register(AnimationController<T> controller, String trigger, RawAnimation animation) {
        if (animation != null) {
            controller.triggerableAnim(trigger, animation);
        }
    }

    public static <T extends DragonEntity> void registerFlight(AnimationController<T> controller,
                                                              String trigger,
                                                              RawAnimation animation) {
        if (trigger != null && !trigger.isEmpty() && animation != null) {
            controller.triggerableAnim(trigger, animation);
        }
    }

    public static <T extends DragonEntity> void registerFlightStandard(AnimationController<T> controller,
                                                                       RawAnimation takeoff,
                                                                       RawAnimation riderTakeoff,
                                                                       RawAnimation landed) {
        registerFlight(controller, TAKEOFF, takeoff);
        registerFlight(controller, RIDER_TAKEOFF, riderTakeoff);
        registerFlight(controller, LANDED, landed);
    }


    public static <T extends DragonEntity> AnimationController<T> createFlightController(T dragon,
                                                                                        int transitionTicks,
                                                                                        Function<AnimationState<T>, PlayState> predicate) {
        int safeTransitionTicks = Math.max(0, transitionTicks);
        return new AnimationController<>(dragon, FLIGHT_CONTROLLER, safeTransitionTicks, state -> {
            state.getController().transitionLength(safeTransitionTicks);
            return predicate.apply(state);
        });
    }


    public static <T extends DragonEntity> PlayState handleFlightState(AnimationState<T> state,
                                                                       DragonFlightStateEvaluator.VisualState visualState,
                                                                       FlightAnimations animations,
                                                                       FlightTransitions transitions) {
        if (visualState == null) {
            return PlayState.STOP;
        }

        RawAnimation animation = switch (visualState) {
            case TAKEOFF -> animations.takeoff();
            case LANDING -> animations.landing();
            case GLIDE_DOWN -> animations.glideDown();
            case FLY_IDLE -> animations.flyIdle();
            case SPRINT_FLAP -> animations.sprintFlap();
            case FLAP -> animations.flap();
            case GLIDE -> animations.flyGlide();
            default -> null;
        };

        if (animation == null) {
            return PlayState.STOP;
        }

        state.getController().transitionLength(getFlightTransitionTicks(visualState, transitions));
        setAndContinue(state, animation);
        return PlayState.CONTINUE;
    }

    public static <T extends DragonEntity> PlayState handleTakeoff(AnimationState<T> state,
                                                                   boolean riderTakeoff,
                                                                   FlightAnimations animations,
                                                                   FlightTransitions transitions) {
        RawAnimation animation = riderTakeoff && animations.riderTakeoff() != null
                ? animations.riderTakeoff()
                : animations.takeoff();
        if (animation == null) {
            return PlayState.STOP;
        }
        state.getController().transitionLength(transitions.takeoff());
        setAndContinue(state, animation);
        return PlayState.CONTINUE;
    }



    private static int getFlightTransitionTicks(DragonFlightStateEvaluator.VisualState visualState, FlightTransitions transitions) {
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

    public static void setAndContinue(AnimationState<?> state, RawAnimation animation) {
        state.setAnimation(animation);
        if (animation != null
                && state.getController().getCurrentAnimation() == null
                && state.isCurrentAnimation(animation)) {
            state.resetCurrentAnimation();
            state.setAnimation(animation);
        }
    }

    public static PlayState tryHandleRestPose(AnimationState<?> state,
                                              DragonEntity dragon,
                                              RawAnimation sleepAnimation,
                                              RawAnimation sitAnimation,
                                              int sleepTransitionTicks,
                                              int sitTransitionTicks) {
        return tryHandleRestPose(state, dragon, sleepAnimation, sitAnimation, sleepTransitionTicks, sitTransitionTicks, true);
    }

    public static PlayState tryHandleRestPose(AnimationState<?> state,
                                              DragonEntity dragon,
                                              RawAnimation sleepAnimation,
                                              RawAnimation sitAnimation,
                                              int sleepTransitionTicks,
                                              int sitTransitionTicks,
                                              boolean allowSitPose) {
        if (dragon.isSleeping() && !dragon.isSleepTransitioning()) {
            if (sleepAnimation == null) {
                return PlayState.STOP;
            }
            state.getController().transitionLength(sleepTransitionTicks);
            setAndContinue(state, sleepAnimation);
            return PlayState.CONTINUE;
        }
        if (dragon.isSleepTransitioning()) {
            return PlayState.STOP;
        }

        float sitProgress = dragon.getSitProgress();
        if (!allowSitPose && sitProgress > 0f) {
            return null;
        }
        if (sitProgress >= dragon.maxSitTicks()) {
            if (sitAnimation == null) {
                return PlayState.STOP;
            }
            state.getController().transitionLength(sitTransitionTicks);
            setAndContinue(state, sitAnimation);
            return PlayState.CONTINUE;
        }
        if (sitProgress > 0f) {
            return PlayState.STOP;
        }

        return null;
    }

    public static PlayState tryHandleDance(AnimationState<?> state, DancingEntity dancer, int transitionTicks) {
        if (!dancer.isDancing() || !dancer.canDance()) {
            return null;
        }
        RawAnimation animation = dancer.getDanceAnimation();
        if (animation == null) {
            return PlayState.STOP;
        }
        state.getController().transitionLength(transitionTicks);
        setAndContinue(state, animation);
        return PlayState.CONTINUE;
    }

    public static PlayState handleGroundMovement(AnimationState<?> state,
                                                 RideableDragonBase dragon,
                                                 RawAnimation idleAnimation,
                                                 RawAnimation walkAnimation,
                                                 RawAnimation runAnimation) {
        return handleGroundMovement(state, dragon, idleAnimation, walkAnimation, runAnimation, false);
    }

    public static PlayState handleGroundMovement(AnimationState<?> state,
                                                 RideableDragonBase dragon,
                                                 RawAnimation idleAnimation,
                                                 RawAnimation walkAnimation,
                                                 RawAnimation runAnimation,
                                                 boolean treatAnimationStateMovingAsWalk) {
        int groundState = dragon.getEffectiveGroundState();
        if (dragon.isVehicle()) {
            if (groundState == 2 || dragon.isRunning()) {
                setAndContinue(state, runAnimation);
            } else if (groundState == 1 || dragon.isWalking() || (treatAnimationStateMovingAsWalk && state.isMoving())) {
                setAndContinue(state, walkAnimation);
            } else {
                setAndContinue(state, idleAnimation);
            }
            return PlayState.CONTINUE;
        }

        if (groundState == 2 || (state.isMoving() && dragon.shouldUseRunAnimation())) {
            setAndContinue(state, runAnimation);
        } else if (groundState == 1 || state.isMoving()) {
            setAndContinue(state, walkAnimation);
        } else {
            setAndContinue(state, idleAnimation);
        }
        return PlayState.CONTINUE;
    }

    public static PlayState handleGroundMovement(AnimationState<?> state,
                                                 RideableDragonBase dragon,
                                                 RawAnimation idleAnimation,
                                                 RawAnimation walkAnimation,
                                                 RawAnimation runAnimation,
                                                 int movingTransitionTicks,
                                                 int idleTransitionTicks) {
        return handleGroundMovement(state, dragon, idleAnimation, walkAnimation, runAnimation,
                movingTransitionTicks, idleTransitionTicks, false);
    }

    public static PlayState handleGroundMovement(AnimationState<?> state,
                                                 RideableDragonBase dragon,
                                                 RawAnimation idleAnimation,
                                                 RawAnimation walkAnimation,
                                                 RawAnimation runAnimation,
                                                 int movingTransitionTicks,
                                                 int idleTransitionTicks,
                                                 boolean treatAnimationStateMovingAsWalk) {
        int groundState = dragon.getEffectiveGroundState();
        if (dragon.isVehicle()) {
            if (groundState == 2 || dragon.isRunning()) {
                state.getController().transitionLength(movingTransitionTicks);
                setAndContinue(state, runAnimation);
            } else if (groundState == 1 || dragon.isWalking() || (treatAnimationStateMovingAsWalk && state.isMoving())) {
                state.getController().transitionLength(movingTransitionTicks);
                setAndContinue(state, walkAnimation);
            } else {
                state.getController().transitionLength(idleTransitionTicks);
                setAndContinue(state, idleAnimation);
            }
            return PlayState.CONTINUE;
        }

        if (groundState == 2 || (state.isMoving() && dragon.shouldUseRunAnimation())) {
            state.getController().transitionLength(movingTransitionTicks);
            setAndContinue(state, runAnimation);
        } else if (groundState == 1 || state.isMoving()) {
            state.getController().transitionLength(movingTransitionTicks);
            setAndContinue(state, walkAnimation);
        } else {
            state.getController().transitionLength(idleTransitionTicks);
            setAndContinue(state, idleAnimation);
        }
        return PlayState.CONTINUE;
    }


    public static RawAnimation loop(String dragonName, String animationName) {
        return RawAnimation.begin().thenLoop(path(dragonName, animationName));
    }

    private static String path(String dragonName, String animationName) {
        return "animation." + dragonName + "." + animationName;
    }

    public record Animations(
            RawAnimation idle,
            RawAnimation walk,
            RawAnimation run,
            RawAnimation sit,
            RawAnimation sitDown,
            RawAnimation sitUp,
            RawAnimation fallAsleep,
            RawAnimation sleep,
            RawAnimation wakeUp,
            RawAnimation swim,
            RawAnimation stunned,
            RawAnimation falling
    ) {
    }

    public record Transitions(
            int idle,
            int moving,
            int sit,
            int sleep,
            int bodyTransition,
            int water,
            int stunned,
            int falling
    ) {
    }

    public record FlightAnimations(
            RawAnimation takeoff,
            RawAnimation riderTakeoff,
            RawAnimation landing,
            RawAnimation flyGlide,
            RawAnimation glideDown,
            RawAnimation flyIdle,
            RawAnimation flap,
            RawAnimation sprintFlap
    ) {
    }

    public record FlightTransitions(
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

    public interface SpecialStates<T extends RideableDragonBase> {
        default boolean riderControlsLocked(T dragon) {
            return dragon.areRiderControlsLocked();
        }

        default boolean tamingStunned(T dragon) {
            return false;
        }

        default boolean inWater(T dragon) {
            return dragon.isInWater() || dragon.isInWaterOrBubble();
        }

        default boolean falling(T dragon) {
            return false;
        }

        default PlayState handle(AnimationState<T> state, T dragon, Animations animations, Transitions transitions) {
            return null;
        }
    }
}
