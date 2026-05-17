package com.leon.saintsdragons.server.entity.dragons.raevyx.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonFlightAnimationHelper;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonInteractionAnimationHelper;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonMovementAnimationHelper;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonStateAnimationHelper;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

public record RaevyxAnimationHandler(Raevyx wyvern) {
    public static final String FAST_ACTION_CONTROLLER = "raevyxFastAction";
    public static final String ACTION_CONTROLLER = "raevyxAction";

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
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.raevyx.swim");
    private static final RawAnimation STUNNED = RawAnimation.begin().thenLoop("animation.raevyx.stunned");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.raevyx.sleep");

    private static final DragonFlightAnimationHelper.Animations FLIGHT_ANIMATIONS =
            new DragonFlightAnimationHelper.Animations(TAKEOFF, RIDER_TAKEOFF, LANDED, FLY_GLIDE, GLIDE_DOWN, FLY_IDLE, FLAP, SPRINT_FLAP);
    private static final DragonFlightAnimationHelper.Transitions FLIGHT_TRANSITIONS =
            new DragonFlightAnimationHelper.Transitions(1, 12, 6, 3, 6, 4, 3, 1);

    public void triggerSitDownAnimation() {
        wyvern.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.SIT_DOWN);
    }

    public void triggerSitUpAnimation() {
        wyvern.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.SIT_UP);
    }

    public void triggerFallAsleepAnimation() {
        wyvern.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.FALL_ASLEEP);
    }

    public void triggerSleepAnimation() {
        wyvern.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.SLEEP);
    }

    public void triggerWakeUpAnimation() {
        wyvern.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.WAKE_UP);
    }

    public void triggerDodgeLeftAnimation() {
        wyvern.triggerAnim(ACTION_CONTROLLER, "dodge_left");
        if (!wyvern.level().isClientSide) {
            wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_DODGE.get(), 1.6f, 1.0f, 35);
        }
    }

    public void triggerDodgeRightAnimation() {
        wyvern.triggerAnim(ACTION_CONTROLLER, "dodge_right");
        if (!wyvern.level().isClientSide) {
            wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_DODGE.get(), 1.6f, 1.0f, 35);
        }
    }

    public void triggerDodgeBackwardAnimation() {
        wyvern.triggerAnim(ACTION_CONTROLLER, "dash_backward");
        if (!wyvern.level().isClientSide) {
            wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_DODGE.get(), 1.6f, 1.0f, 35);
        }
    }

    public void triggerDodgeAirLeftAnimation() {
        wyvern.triggerAnim(ACTION_CONTROLLER, "dodge_air_left");
        if (!wyvern.level().isClientSide) {
            wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_DODGE.get(), 1.6f, 1.0f, 35);
        }
    }

    public void triggerDodgeAirRightAnimation() {
        wyvern.triggerAnim(ACTION_CONTROLLER, "dodge_air_right");
        if (!wyvern.level().isClientSide) {
            wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_DODGE.get(), 1.6f, 1.0f, 35);
        }
    }

    public PlayState movementPredicate(AnimationState<Raevyx> state) {
        state.getController().transitionLength(6);

        if (wyvern.areRiderControlsLocked()) {
            return PlayState.STOP;
        }

        if (wyvern.isTakeoff()) {
            return PlayState.STOP;
        }

        boolean inWater = wyvern.isInWater() || wyvern.isInWaterOrBubble();
        if (inWater) {
            state.getController().transitionLength(4);
            state.setAndContinue(SWIM);
            return PlayState.CONTINUE;
        }

        if (wyvern.isDying()) {
            return PlayState.STOP;
        }

        if (wyvern.isTamingStunned()) {
            state.getController().transitionLength(4);
            state.setAndContinue(STUNNED);
            return PlayState.CONTINUE;
        }

        PlayState restPose = DragonMovementAnimationHelper.tryHandleRestPose(state, wyvern, SLEEP, SIT, 6, 0);
        if (restPose != null) {
            return restPose;
        }

        if (wyvern.isBaby()) {
            return DragonMovementAnimationHelper.handleGroundMovement(state, wyvern, GROUND_IDLE, GROUND_WALK, GROUND_RUN, 3, 4);
        }

        if (wyvern.isDodging()) {
            state.getController().transitionLength(2);
            state.setAndContinue(DODGE);
            return PlayState.CONTINUE;
        }

        if (wyvern.isGroundRending()) {
            return PlayState.STOP;
        }
        if (wyvern.isDashing()) {
            state.getController().transitionLength(2);
            if (wyvern.wasLastDashRight()) {
                state.setAndContinue(DASH_FORWARD_LEFT);
            } else {
                state.setAndContinue(DASH_FORWARD_RIGHT);
            }
            return PlayState.CONTINUE;
        }

        if (wyvern.isFallingForAnimation()) {
            state.getController().transitionLength(4);
            state.setAndContinue(FALLING);
            return PlayState.CONTINUE;
        }

        if (wyvern.isLanding() || wyvern.isFlying()) {
            return PlayState.STOP;
        }

        return DragonMovementAnimationHelper.handleGroundMovement(state, wyvern, GROUND_IDLE, GROUND_WALK, GROUND_RUN, 3, 4);
    }

    public PlayState flightPredicate(AnimationState<Raevyx> state) {
        if (wyvern.isDying() || wyvern.isTamingStunned()) {
            return PlayState.STOP;
        }
        if (!wyvern.isTakeoff() && !wyvern.isLanding() && !wyvern.isFlying()) {
            return PlayState.STOP;
        }
        if (wyvern.isTakeoff()) {
            return DragonFlightAnimationHelper.handleTakeoff(
                    state,
                    wyvern.getControllingPassenger() != null,
                    FLIGHT_ANIMATIONS,
                    FLIGHT_TRANSITIONS
            );
        }
        DragonFlightStateEvaluator.VisualState visualState = isInvertedGlideWindow(state.getPartialTick())
                ? DragonFlightStateEvaluator.VisualState.GLIDE
                : wyvern.getVisualFlightState(state.getPartialTick());
        return DragonFlightAnimationHelper.handleState(state, visualState, FLIGHT_ANIMATIONS, FLIGHT_TRANSITIONS);
    }

    private boolean isInvertedGlideWindow(float partialTick) {
        float roll = wyvern.getSmoothedRoll(partialTick);
        float nearestInvertedRoll = Math.round((roll - net.minecraft.util.Mth.PI) / net.minecraft.util.Mth.TWO_PI)
                * net.minecraft.util.Mth.TWO_PI + net.minecraft.util.Mth.PI;
        float offsetDegrees = Math.abs((roll - nearestInvertedRoll) * net.minecraft.util.Mth.RAD_TO_DEG);
        return offsetDegrees <= INVERTED_GLIDE_ROLL_WINDOW_DEGREES;
    }

    public void setupStateController(AnimationController<Raevyx> controller) {
        DragonStateAnimationHelper.registerStandard(controller, "raevyx");
    }

    public void setupActionController(AnimationController<Raevyx> controller) {
        registerVocalTriggers(controller, ACTION_CONTROLLER);
        controller.triggerableAnim("dodge_left",
                RawAnimation.begin().thenPlay("animation.raevyx.dodge_left"));
        controller.triggerableAnim("dodge_right",
                RawAnimation.begin().thenPlay("animation.raevyx.dodge_right"));
        controller.triggerableAnim("dodge_air_left",
                RawAnimation.begin().thenPlay("animation.raevyx.dodge_air_left"));
        controller.triggerableAnim("dodge_air_right",
                RawAnimation.begin().thenPlay("animation.raevyx.dodge_air_right"));
        controller.triggerableAnim("ground_rend",
                RawAnimation.begin().thenPlay("animation.raevyx.ground_rend"));
        controller.triggerableAnim("dash_backward",
                RawAnimation.begin().thenPlay("animation.raevyx.dash_backward"));
        controller.triggerableAnim("summon_storm",
                RawAnimation.begin().thenPlay("animation.raevyx.summon_storm"));
        controller.triggerableAnim("summon_storm_air",
                RawAnimation.begin().thenPlay("animation.raevyx.summon_storm_air"));
    }

    public PlayState raevyxActionPredicate(AnimationState<Raevyx> state) {
        state.getController().transitionLength(3);
        return PlayState.STOP;
    }

    public PlayState raevyxFastActionPredicate(AnimationState<Raevyx> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }

    public void setupFlightController(AnimationController<Raevyx> controller) {
        DragonFlightAnimationHelper.registerStandard(controller, TAKEOFF, RIDER_TAKEOFF, LANDED);
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
        controller.triggerableAnim(DragonInteractionAnimationHelper.EAT,
                RawAnimation.begin().thenPlay("animation.raevyx.eat"));
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
