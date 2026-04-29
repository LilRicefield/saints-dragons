package com.leon.saintsdragons.server.entity.dragons.raevyx.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

public record RaevyxAnimationHandler(Raevyx wyvern) {
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

    private static RawAnimation currentFlightAnimation = FLY_GLIDE;

    public void triggerSitDownAnimation() {
        wyvern.triggerAnim("action", "sit_down");
    }

    public void triggerSitUpAnimation() {
        wyvern.triggerAnim("action", "sit_up");
    }

    public void triggerFallAsleepAnimation() {
        wyvern.triggerAnim("action", "fall_asleep");
    }

    public void triggerSleepAnimation() {
        wyvern.triggerAnim("action", "sleep");
    }

    public void triggerWakeUpAnimation() {
        wyvern.triggerAnim("action", "wake_up");
    }

    public void triggerDodgeLeftAnimation() {
        wyvern.triggerAnim("action", "dodge_left");
        if (!wyvern.level().isClientSide) {
            wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_DODGE.get(), 1.6f, 1.0f, 35);
        }
    }

    public void triggerDodgeRightAnimation() {
        wyvern.triggerAnim("action", "dodge_right");
        if (!wyvern.level().isClientSide) {
            wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_DODGE.get(), 1.6f, 1.0f, 35);
        }
    }

    public void triggerDodgeBackwardAnimation() {
        wyvern.triggerAnim("action", "dash_backward");
        if (!wyvern.level().isClientSide) {
            wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_DODGE.get(), 1.6f, 1.0f, 35);
        }
    }

    public void triggerDodgeAirLeftAnimation() {
        wyvern.triggerAnim("action", "dodge_air_left");
        if (!wyvern.level().isClientSide) {
            wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_DODGE.get(), 1.6f, 1.0f, 35);
        }
    }

    public void triggerDodgeAirRightAnimation() {
        wyvern.triggerAnim("action", "dodge_air_right");
        if (!wyvern.level().isClientSide) {
            wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_DODGE.get(), 1.6f, 1.0f, 35);
        }
    }

    public PlayState handleMovementAnimation(AnimationState<Raevyx> state) {
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

        if (wyvern.isSleeping() && !wyvern.isSleepingEntering() && !wyvern.isSleepingExiting()) {
            state.getController().transitionLength(6);
            state.setAndContinue(SLEEP);
            return PlayState.CONTINUE;
        } else if (wyvern.isSleepingEntering() || wyvern.isSleepingExiting()) {
            return PlayState.STOP;
        }

        float maxSit = wyvern.maxSitTicks();
        float sitProgress = wyvern.getSitProgress();
        if (sitProgress >= maxSit) {
            state.setAndContinue(SIT);
            return PlayState.CONTINUE;
        } else if (sitProgress > 0f) {
            return PlayState.STOP;
        }

        if (wyvern.isBaby()) {
            if (wyvern.isActuallyRunning()) {
                state.getController().transitionLength(3);
                state.setAndContinue(GROUND_RUN);
            } else if (wyvern.isWalking()) {
                state.getController().transitionLength(3);
                state.setAndContinue(GROUND_WALK);
            } else {
                state.getController().transitionLength(4);
                state.setAndContinue(GROUND_IDLE);
            }
            return PlayState.CONTINUE;
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

        DragonFlightStateEvaluator.VisualState visualState =
                wyvern.getVisualFlightState(state.getPartialTick());

        if (wyvern.isLanding()) {
            RawAnimation landingAnimation = visualState == DragonFlightStateEvaluator.VisualState.LANDING
                    ? LANDING
                    : GLIDE_DOWN;
            state.getController().transitionLength(landingAnimation == LANDING ? 4 : 6);
            currentFlightAnimation = landingAnimation;
            state.setAndContinue(landingAnimation);
            return PlayState.CONTINUE;
        }

        if (wyvern.isFlying()) {
            if (isInvertedGlideWindow(state.getPartialTick())) {
                state.getController().transitionLength(5);
                currentFlightAnimation = FLY_GLIDE;
                state.setAndContinue(FLY_GLIDE);
                return PlayState.CONTINUE;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.LANDING) {
                state.getController().transitionLength(4);
                currentFlightAnimation = LANDING;
                state.setAndContinue(LANDING);
                return PlayState.CONTINUE;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.TAKEOFF) {
                state.getController().transitionLength(4);
                state.setAndContinue(TAKEOFF);
                return PlayState.CONTINUE;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.GLIDE_DOWN) {
                RawAnimation descend = GLIDE_DOWN;
                if (currentFlightAnimation != descend) {
                    state.getController().transitionLength(6);
                    currentFlightAnimation = descend;
                }
                state.setAndContinue(descend);
                return PlayState.CONTINUE;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.FLY_IDLE) {
                RawAnimation hover = FLY_IDLE;
                if (currentFlightAnimation != hover) {
                    state.getController().transitionLength(6);
                    currentFlightAnimation = hover;
                }
                state.setAndContinue(hover);
                return PlayState.CONTINUE;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.SPRINT_FLAP) {
                RawAnimation sprint = SPRINT_FLAP;
                if (currentFlightAnimation != sprint) {
                    state.getController().transitionLength(3);
                    currentFlightAnimation = sprint;
                }
                state.setAndContinue(sprint);
                return PlayState.CONTINUE;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.FLAP) {
                state.getController().transitionLength(4);
                state.setAndContinue(FLAP);
                return PlayState.CONTINUE;
            }

            state.getController().transitionLength(12);
            state.setAndContinue(FLY_GLIDE);
            return PlayState.CONTINUE;
        }

        if (wyvern.isActuallyRunning()) {
            state.getController().transitionLength(3);
            state.setAndContinue(GROUND_RUN);
        } else if (wyvern.isWalking()) {
            state.getController().transitionLength(3);
            state.setAndContinue(GROUND_WALK);
        } else {
            state.getController().transitionLength(4);
            state.setAndContinue(GROUND_IDLE);
        }
        return PlayState.CONTINUE;
    }

    private boolean isInvertedGlideWindow(float partialTick) {
        float roll = wyvern.getSmoothedRoll(partialTick);
        float nearestInvertedRoll = Math.round((roll - net.minecraft.util.Mth.PI) / net.minecraft.util.Mth.TWO_PI)
                * net.minecraft.util.Mth.TWO_PI + net.minecraft.util.Mth.PI;
        float offsetDegrees = Math.abs((roll - nearestInvertedRoll) * net.minecraft.util.Mth.RAD_TO_DEG);
        return offsetDegrees <= INVERTED_GLIDE_ROLL_WINDOW_DEGREES;
    }

    public void setupActionController(AnimationController<Raevyx> actionController) {
        registerVocalTriggers(actionController);
        actionController.triggerableAnim("lightning_bite",
                RawAnimation.begin().thenPlay("animation.raevyx.lightning_bite"));
        actionController.triggerableAnim("horn_gore",
                RawAnimation.begin().thenPlay("animation.raevyx.horn_gore"));
        actionController.triggerableAnim("dodge_left",
                RawAnimation.begin().thenPlay("animation.raevyx.dodge_left"));
        actionController.triggerableAnim("dodge_right",
                RawAnimation.begin().thenPlay("animation.raevyx.dodge_right"));
        actionController.triggerableAnim("dodge_air_left",
                RawAnimation.begin().thenPlay("animation.raevyx.dodge_air_left"));
        actionController.triggerableAnim("dodge_air_right",
                RawAnimation.begin().thenPlay("animation.raevyx.dodge_air_right"));
        actionController.triggerableAnim("dash_backward",
                RawAnimation.begin().thenPlay("animation.raevyx.dash_backward"));
        actionController.triggerableAnim("lightning_beam_start",
                RawAnimation.begin().thenPlay("animation.raevyx.lightning_beam_start"));
        actionController.triggerableAnim("lightning_beaming",
                RawAnimation.begin().thenLoop("animation.raevyx.lightning_beaming"));
        actionController.triggerableAnim("lightning_beam_stop",
                RawAnimation.begin().thenPlay("animation.raevyx.lightning_beam_stop"));
        actionController.triggerableAnim("eat",
                RawAnimation.begin().thenPlay("animation.raevyx.eat"));
        actionController.triggerableAnim("taunt",
                RawAnimation.begin().thenPlay("animation.raevyx.taunt"));
        actionController.triggerableAnim("ground_rend",
                RawAnimation.begin().thenPlay("animation.raevyx.ground_rend"));
        actionController.triggerableAnim("summon_storm",
                RawAnimation.begin().thenPlay("animation.raevyx.summon_storm"));
        actionController.triggerableAnim("summon_storm_air",
                RawAnimation.begin().thenPlay("animation.raevyx.summon_storm_air"));
        actionController.triggerableAnim("sit_down",
                RawAnimation.begin().thenPlay("animation.raevyx.down"));
        actionController.triggerableAnim("sit_up",
                RawAnimation.begin().thenPlay("animation.raevyx.up"));
        actionController.triggerableAnim("fall_asleep",
                RawAnimation.begin().thenPlay("animation.raevyx.fall_asleep"));
        actionController.triggerableAnim("sleep",
                RawAnimation.begin().thenLoop("animation.raevyx.sleep"));
        actionController.triggerableAnim("wake_up",
                RawAnimation.begin().thenPlay("animation.raevyx.wake_up"));
        actionController.triggerableAnim("landed", LANDED);
    }

    public PlayState instantActionPredicate(AnimationState<Raevyx> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }

    public void setupInstantActionController(AnimationController<Raevyx> controller) {
        controller.triggerableAnim("takeoff", TAKEOFF);
        controller.triggerableAnim("rider_takeoff", RIDER_TAKEOFF);
        controller.triggerableAnim("raevyx_hurt",
                RawAnimation.begin().thenPlay("animation.raevyx.hurt"));
        controller.triggerableAnim("die",
                RawAnimation.begin().thenPlay("animation.raevyx.die"));
    }

    private void registerVocalTriggers(AnimationController<Raevyx> action) {
        wyvern.getVocalEntries().forEach((key, entry) -> {
            if (!"action".equals(entry.controllerId())) {
                return;
            }
            if (entry.animationId() != null && !entry.animationId().isEmpty()) {
                action.triggerableAnim(key, RawAnimation.begin().thenPlay(entry.animationId()));
            }
        });
    }
}