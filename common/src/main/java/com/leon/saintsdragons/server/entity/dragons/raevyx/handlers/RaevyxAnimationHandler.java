package com.leon.saintsdragons.server.entity.dragons.raevyx.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

public record RaevyxAnimationHandler(Raevyx wyvern) {
    public static final String MOVEMENT_CONTROLLER = AnimationHelper.MOVEMENT_CONTROLLER;
    public static final String FAST_ACTION_CONTROLLER = "raevyxFastAction";
    public static final String ACTION_CONTROLLER = "raevyxAction";
    private static final String DODGE_AIR_LEFT = "dodge_air_left";
    private static final String DODGE_AIR_RIGHT = "dodge_air_right";

    private static final float INVERTED_GLIDE_ROLL_WINDOW_DEGREES = 45.0f;
    private static final RawAnimation GROUND_IDLE = RawAnimation.begin().thenLoop("animation.raevyx.idle");
    private static final RawAnimation GROUND_WALK = RawAnimation.begin().thenLoop("animation.raevyx.walk");
    private static final RawAnimation GROUND_RUN = RawAnimation.begin().thenLoop("animation.raevyx.run");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.raevyx.sit");
    private static final RawAnimation TAKEOFF = RawAnimation.begin().thenPlay("animation.raevyx.takeoff");
    private static final RawAnimation RIDER_TAKEOFF = RawAnimation.begin().thenPlay("animation.raevyx.rider_takeoff");
    private static final RawAnimation FLY_GLIDE = RawAnimation.begin().thenLoop("animation.raevyx.fly_glide");
    private static final RawAnimation FALLING = RawAnimation.begin().thenLoop("animation.raevyx.falling");
    private static final RawAnimation GLIDE_DOWN = RawAnimation.begin().thenLoop("animation.raevyx.glide_down");
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.raevyx.flap");
    private static final RawAnimation FLY_IDLE = RawAnimation.begin().thenLoop("animation.raevyx.fly_idle");
    private static final RawAnimation SPRINT_FLAP = RawAnimation.begin().thenLoop("animation.raevyx.sprint_flap");
    private static final RawAnimation LANDING = RawAnimation.begin().thenPlay("animation.raevyx.landing");
    private static final RawAnimation LANDED = RawAnimation.begin().thenPlay("animation.raevyx.landed");
    private static final RawAnimation DODGE = RawAnimation.begin().thenPlay("animation.raevyx.dodge");
    private static final RawAnimation DASH_FORWARD_RIGHT = RawAnimation.begin().thenPlay("animation.raevyx.dash_forward_right");
    private static final RawAnimation DASH_FORWARD_LEFT = RawAnimation.begin().thenPlay("animation.raevyx.dash_forward_left");
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlay("animation.raevyx.down");
    private static final RawAnimation SIT_UP = RawAnimation.begin().thenPlay("animation.raevyx.up");
    private static final RawAnimation FALL_ASLEEP = RawAnimation.begin().thenPlay("animation.raevyx.fall_asleep");
    private static final RawAnimation WAKE_UP = RawAnimation.begin().thenPlay("animation.raevyx.wake_up");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.raevyx.swim");
    private static final RawAnimation STUNNED = RawAnimation.begin().thenLoop("animation.raevyx.stunned");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.raevyx.sleep");
    private static final AnimationHelper.Animations GROUND_ANIMATIONS =
            new AnimationHelper.Animations(
                    GROUND_IDLE,
                    GROUND_WALK,
                    GROUND_RUN,
                    SIT,
                    SIT_DOWN,
                    SIT_UP,
                    FALL_ASLEEP,
                    SLEEP,
                    WAKE_UP,
                    SWIM,
                    STUNNED,
                    FALLING
            );
    private static final AnimationHelper.Transitions GROUND_TRANSITIONS =
            new AnimationHelper.Transitions(4, 2, 4, 4, 4, 4, 4, 4);
    private static final AnimationHelper.FlightAnimations FLIGHT_ANIMATIONS =
            new AnimationHelper.FlightAnimations(TAKEOFF, RIDER_TAKEOFF, null, FLY_GLIDE, GLIDE_DOWN, FLY_IDLE, FLAP, SPRINT_FLAP);
    private static final AnimationHelper.FlightTransitions FLIGHT_TRANSITIONS =
            new AnimationHelper.FlightTransitions(1, 12, 6, 3, 6, 4, 3, 1);
    private static final int ACTION_TRANSITION_TICKS = 3;
    private static final int FAST_ACTION_TRANSITION_TICKS = 1;

    public void triggerSitDownAnimation() {
        wyvern.triggerAnim(AnimationHelper.TRANSITION_CONTROLLER, AnimationHelper.SIT_DOWN);
    }

    public void triggerSitUpAnimation() {
        wyvern.triggerAnim(AnimationHelper.TRANSITION_CONTROLLER, AnimationHelper.SIT_UP);
    }

    public void triggerFallAsleepAnimation() {
        wyvern.triggerAnim(AnimationHelper.TRANSITION_CONTROLLER, AnimationHelper.FALL_ASLEEP);
    }

    public void triggerWakeUpAnimation() {
        wyvern.triggerAnim(AnimationHelper.TRANSITION_CONTROLLER, AnimationHelper.WAKE_UP);
    }

    public void triggerDodgeLeftAnimation() {
        wyvern.triggerAnim(MOVEMENT_CONTROLLER, "dodge_left");
        if (!wyvern.level().isClientSide) {
            wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_DODGE.get(), 1.6f, 1.0f, 35);
        }
    }

    public void triggerDodgeRightAnimation() {
        wyvern.triggerAnim(MOVEMENT_CONTROLLER, "dodge_right");
        if (!wyvern.level().isClientSide) {
            wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_DODGE.get(), 1.6f, 1.0f, 35);
        }
    }

    public void triggerDodgeBackwardAnimation() {
        wyvern.triggerAnim(MOVEMENT_CONTROLLER, "dash_backward");
        if (!wyvern.level().isClientSide) {
            wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_DODGE.get(), 1.6f, 1.0f, 35);
        }
    }

    public void triggerDodgeAirLeftAnimation() {
        wyvern.triggerAnim(AnimationHelper.FLIGHT_CONTROLLER, DODGE_AIR_LEFT);
        if (!wyvern.level().isClientSide) {
            wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_DODGE.get(), 1.6f, 1.0f, 35);
        }
    }

    public void triggerDodgeAirRightAnimation() {
        wyvern.triggerAnim(AnimationHelper.FLIGHT_CONTROLLER, DODGE_AIR_RIGHT);
        if (!wyvern.level().isClientSide) {
            wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_DODGE.get(), 1.6f, 1.0f, 35);
        }
    }

    public void setupFlightController(AnimationController<Raevyx> controller) {
        AnimationHelper.registerFlightStandard(controller, TAKEOFF, RIDER_TAKEOFF, null);
        AnimationHelper.registerFlight(controller, DODGE_AIR_LEFT,
                RawAnimation.begin().thenPlay("animation.raevyx.dodge_air_left"));
        AnimationHelper.registerFlight(controller, DODGE_AIR_RIGHT,
                RawAnimation.begin().thenPlay("animation.raevyx.dodge_air_right"));
        AnimationHelper.registerFlight(controller, "summon_storm_air",
                RawAnimation.begin().thenPlay("animation.raevyx.summon_storm_air"));
    }

    public void setupMovementController(AnimationController<Raevyx> controller) {
        AnimationHelper.register(controller, GROUND_ANIMATIONS);
        AnimationHelper.register(controller, AnimationHelper.LANDED, LANDED);
        controller.triggerableAnim("dodge_left",
                RawAnimation.begin().thenPlay("animation.raevyx.dodge_left"));
        controller.triggerableAnim("dodge_right",
                RawAnimation.begin().thenPlay("animation.raevyx.dodge_right"));
        controller.triggerableAnim("dash_backward",
                RawAnimation.begin().thenPlay("animation.raevyx.dash_backward"));
        controller.triggerableAnim("ground_rend",
                RawAnimation.begin().thenPlay("animation.raevyx.ground_rend"));
        registerVocalTriggers(controller, MOVEMENT_CONTROLLER);
    }

    public void setupActionController(AnimationController<Raevyx> controller) {
        controller.triggerableAnim("summon_storm",
                RawAnimation.begin().thenPlay("animation.raevyx.summon_storm"));
    }

    public void setupFastActionController(AnimationController<Raevyx> controller) {
        registerVocalTriggers(controller, FAST_ACTION_CONTROLLER);
        controller.triggerableAnim("lightning_bite",
                RawAnimation.begin().thenPlay("animation.raevyx.lightning_bite"));
        controller.triggerableAnim("horn_gore",
                RawAnimation.begin().thenPlay("animation.raevyx.horn_gore"));
        controller.triggerableAnim("lightning_beam_start",
                RawAnimation.begin().thenPlay("animation.raevyx.lightning_beam_start"));
        controller.triggerableAnim("lightning_beaming",
                RawAnimation.begin().thenLoop("animation.raevyx.lightning_beaming"));
        controller.triggerableAnim("lightning_beam_stop",
                RawAnimation.begin().thenPlay("animation.raevyx.lightning_beam_stop"));
    }

    public void setupInteractionController(AnimationController<Raevyx> controller) {
        controller.triggerableAnim("raevyx_hurt",
                RawAnimation.begin().thenPlay("animation.raevyx.hurt"));
        controller.triggerableAnim("die",
                RawAnimation.begin().thenPlay("animation.raevyx.die"));
        controller.triggerableAnim(AnimationHelper.EAT,
                RawAnimation.begin().thenPlay("animation.raevyx.eat"));
    }

    public PlayState movementPredicate(AnimationState<Raevyx> state) {
        return AnimationHelper.handleGrounded(state, wyvern, GROUND_ANIMATIONS, GROUND_TRANSITIONS, RaevyxGroundStates.INSTANCE);
    }

    private enum RaevyxGroundStates implements AnimationHelper.SpecialStates<Raevyx> {
        INSTANCE;

        @Override
        public boolean tamingStunned(Raevyx dragon) {
            return dragon.isTamingStunned();
        }

        @Override
        public boolean sittingDown(Raevyx dragon) {
            return dragon.isSittingDownAnimation();
        }

        @Override
        public boolean standingUp(Raevyx dragon) {
            return dragon.isStandingUpAnimation();
        }

        @Override
        public boolean falling(Raevyx dragon) {
            return dragon.isFallingForAnimation();
        }

        @Override
        public PlayState handle(AnimationState<Raevyx> state,
                                Raevyx dragon,
                                AnimationHelper.Animations animations,
                                AnimationHelper.Transitions transitions) {
            if (dragon.isBaby()) {
                return null;
            }
            if (dragon.isDodging()) {
                state.getController().transitionLength(transitions.moving());
                state.setAndContinue(DODGE);
                return PlayState.CONTINUE;
            }
            if (dragon.isGroundRending()) {
                return PlayState.STOP;
            }
            if (dragon.isDashing()) {
                state.getController().transitionLength(transitions.moving());
                state.setAndContinue(dragon.wasLastDashRight() ? DASH_FORWARD_LEFT : DASH_FORWARD_RIGHT);
                return PlayState.CONTINUE;
            }
            return null;
        }
    }

    public PlayState flightPredicate(AnimationState<Raevyx> state) {
        if (wyvern.isDying() || wyvern.isTamingStunned()) {
            return PlayState.STOP;
        }
        boolean aerialState = wyvern.isFlying() || wyvern.isTakeoff() || wyvern.isLanding() || wyvern.isHovering();
        if (!aerialState) {
            return PlayState.STOP;
        }
        if (wyvern.isTakeoff()) {
            return AnimationHelper.handleTakeoff(
                    state,
                    wyvern.getControllingPassenger() != null,
                    FLIGHT_ANIMATIONS,
                    FLIGHT_TRANSITIONS
            );
        }
        DragonFlightStateEvaluator.VisualState visualState = isInvertedGlideWindow(state.getPartialTick())
                ? DragonFlightStateEvaluator.VisualState.GLIDE
                : wyvern.getVisualFlightState(state.getPartialTick());
        return AnimationHelper.handleFlightState(state, visualState, FLIGHT_ANIMATIONS, FLIGHT_TRANSITIONS);
    }

    private boolean isInvertedGlideWindow(float partialTick) {
        float roll = wyvern.getSmoothedRoll(partialTick);
        float nearestInvertedRoll = Math.round((roll - net.minecraft.util.Mth.PI) / net.minecraft.util.Mth.TWO_PI)
                * net.minecraft.util.Mth.TWO_PI + net.minecraft.util.Mth.PI;
        float offsetDegrees = Math.abs((roll - nearestInvertedRoll) * net.minecraft.util.Mth.RAD_TO_DEG);
        return offsetDegrees <= INVERTED_GLIDE_ROLL_WINDOW_DEGREES;
    }

    public PlayState raevyxActionPredicate(AnimationState<Raevyx> state) {
        state.getController().transitionLength(ACTION_TRANSITION_TICKS);
        return PlayState.STOP;
    }

    public void setupTransitionController(AnimationController<Raevyx> controller) {
        AnimationHelper.registerTransitions(controller, GROUND_ANIMATIONS);
    }

    public PlayState raevyxFastActionPredicate(AnimationState<Raevyx> state) {
        state.getController().transitionLength(FAST_ACTION_TRANSITION_TICKS);
        return PlayState.STOP;
    }

    private void registerVocalTriggers(AnimationController<Raevyx> controller, String controllerName) {
        wyvern.getVocalEntries().forEach((key, entry) -> {
            if (!controllerName.equals(entry.controllerId())) {
                return;
            }
            if (entry.animationId() != null && !entry.animationId().isEmpty()) {
                controller.triggerableAnim(key, RawAnimation.begin().thenPlay(entry.animationId()));
            }
        });
    }
}
