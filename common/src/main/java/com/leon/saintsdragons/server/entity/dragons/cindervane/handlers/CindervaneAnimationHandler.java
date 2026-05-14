package com.leon.saintsdragons.server.entity.dragons.cindervane.handlers;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonInteractionAnimationHelper;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonMovementAnimationHelper;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonStateAnimationHelper;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;


public class CindervaneAnimationHandler {
    public static final String FAST_ACTION_CONTROLLER = "cindervaneFastAction";
    public static final String ACTION_CONTROLLER = "cindervaneAction";

    private final Cindervane amphithere;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.cindervane.idle");
    private static final RawAnimation GLIDE = RawAnimation.begin().thenLoop("animation.cindervane.glide");
    private static final RawAnimation GLIDE_DOWN = RawAnimation.begin().thenLoop("animation.cindervane.glide_down");
    private static final RawAnimation FALLING = RawAnimation.begin().thenLoop("animation.cindervane.falling");
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.cindervane.flap");
    private static final RawAnimation SPRINT_FLAP = RawAnimation.begin().thenLoop("animation.cindervane.sprint_flap");
    private static final RawAnimation FLY_IDLE = RawAnimation.begin().thenLoop("animation.cindervane.fly_idle");
    private static final RawAnimation TAKEOFF = RawAnimation.begin().thenPlay("animation.cindervane.takeoff");
    private static final RawAnimation LANDING = RawAnimation.begin().thenPlay("animation.cindervane.landing");
    private static final RawAnimation LANDED = RawAnimation.begin().thenPlay("animation.cindervane.landed");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.cindervane.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.cindervane.run");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.cindervane.sit");
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlay("animation.cindervane.down");
    private static final RawAnimation SIT_UP = RawAnimation.begin().thenPlay("animation.cindervane.up");
    private static final RawAnimation FALL_ASLEEP = RawAnimation.begin().thenPlay("animation.cindervane.fall_asleep");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.cindervane.sleep");
    private static final RawAnimation WAKE_UP = RawAnimation.begin().thenPlay("animation.cindervane.wake_up");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.cindervane.swim");

    public CindervaneAnimationHandler(Cindervane dragon) {
        this.amphithere = dragon;
    }

    public PlayState handleMovementAnimation(AnimationState<Cindervane> state) {
        state.getController().transitionLength(12); // Longer transitions for smoother animation
        boolean aerialState = amphithere.isFlying() || amphithere.isTakeoff() || amphithere.isLanding() || amphithere.isHovering();

        if (amphithere.isDying()) {
            return PlayState.STOP;
        }

        if (amphithere.isTakeoff()) {
            return PlayState.STOP;
        }
        PlayState sleepPose = DragonMovementAnimationHelper.tryHandleRestPose(state, amphithere, SLEEP, SIT, 6, 0, false);
        if (sleepPose != null) {
            return sleepPose;
        }

        boolean inWater = amphithere.isInWater() || amphithere.isInWaterOrBubble();

        if (inWater) {
            state.getController().transitionLength(6);
            state.setAndContinue(SWIM);
            state.getController().setAnimationSpeed(1.0f);
            return PlayState.CONTINUE;
        }

        if (amphithere.isFallingForAnimation()) {
            state.getController().transitionLength(4);
            state.setAndContinue(FALLING);
            state.getController().setAnimationSpeed(1.0f);
            return PlayState.CONTINUE;
        }

        if (amphithere.isVehicle()) {
            state.getController().transitionLength(4);
            if (aerialState) {
                DragonFlightStateEvaluator.VisualState visualState = amphithere.getVisualFlightState(state.getPartialTick());

                if (visualState == DragonFlightStateEvaluator.VisualState.TAKEOFF) {
                    return PlayState.STOP;
                }

                if (visualState == DragonFlightStateEvaluator.VisualState.LANDING) {
                    state.getController().transitionLength(4);
                    state.setAndContinue(LANDING);
                    return PlayState.CONTINUE;
                }

                if (visualState == DragonFlightStateEvaluator.VisualState.GLIDE_DOWN) {
                    state.getController().transitionLength(6);
                    state.setAndContinue(GLIDE_DOWN);
                    return PlayState.CONTINUE;
                }

                if (visualState == DragonFlightStateEvaluator.VisualState.FLY_IDLE) {
                    state.getController().transitionLength(6);
                    state.setAndContinue(FLY_IDLE);
                    return PlayState.CONTINUE;
                }

                if (visualState == DragonFlightStateEvaluator.VisualState.SPRINT_FLAP) {
                    state.getController().transitionLength(3);
                    state.setAndContinue(SPRINT_FLAP);
                    return PlayState.CONTINUE;
                }

                if (visualState == DragonFlightStateEvaluator.VisualState.FLAP) {
                    state.getController().transitionLength(6);
                    state.setAndContinue(FLAP);
                    return PlayState.CONTINUE;
                }

                state.getController().transitionLength(12);
                state.setAndContinue(GLIDE);
            } else {
                int groundState = amphithere.getEffectiveGroundState();
                if (groundState == 2) {
                    state.setAndContinue(RUN);
                } else if (groundState == 1) {
                    state.setAndContinue(WALK);
                } else {
                    state.setAndContinue(IDLE);
                }
            }
            state.getController().setAnimationSpeed(1.0f);
            return PlayState.CONTINUE;
        }
        PlayState sitPose = DragonMovementAnimationHelper.tryHandleRestPose(state, amphithere, null, SIT, 0, 0);
        if (sitPose != null) {
            return sitPose;
        }

        state.getController().setAnimationSpeed(1.0f);

        if (aerialState) {
            DragonFlightStateEvaluator.VisualState visualState = amphithere.getVisualFlightState(state.getPartialTick());

            if (visualState == DragonFlightStateEvaluator.VisualState.TAKEOFF) {
                return PlayState.STOP;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.LANDING) {
                state.getController().transitionLength(4);
                state.setAndContinue(LANDING);
                return PlayState.CONTINUE;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.GLIDE_DOWN) {
                state.getController().transitionLength(6);
                state.setAndContinue(GLIDE_DOWN);
                return PlayState.CONTINUE;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.FLY_IDLE) {
                state.getController().transitionLength(6);
                state.setAndContinue(FLY_IDLE);
                return PlayState.CONTINUE;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.SPRINT_FLAP) {
                state.getController().transitionLength(3);
                state.setAndContinue(SPRINT_FLAP);
                return PlayState.CONTINUE;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.FLAP) {
                state.getController().transitionLength(6);
                state.setAndContinue(FLAP);
                return PlayState.CONTINUE;
            }

            if (visualState == DragonFlightStateEvaluator.VisualState.GLIDE) {
                state.getController().transitionLength(8);
                state.setAndContinue(GLIDE);
                return PlayState.CONTINUE;
            }

            state.getController().transitionLength(6);
            state.setAndContinue(FLAP);
            return PlayState.CONTINUE;
        }

        if (!amphithere.isTakeoff() && !amphithere.isLanding() && !amphithere.isHovering()) {
            return DragonMovementAnimationHelper.handleGroundMovement(state, amphithere, IDLE, WALK, RUN);
        } else {
            state.setAndContinue(IDLE);
        }

        return PlayState.CONTINUE;
    }

    public void setupActionController(AnimationController<Cindervane> controller) {
        controller.triggerableAnim("bite",
                RawAnimation.begin().thenPlay("animation.cindervane.bite"));
        controller.triggerableAnim("bite_air",
                RawAnimation.begin().thenPlay("animation.cindervane.bite_air"));
        controller.triggerableAnim("roar_air",
                RawAnimation.begin().thenPlay("animation.cindervane.roar_air"));
        controller.triggerableAnim("magma_blast",
                RawAnimation.begin().thenPlay("animation.cindervane.magma_blast"));
        controller.triggerableAnim("slash_left",
                RawAnimation.begin().thenPlay("animation.cindervane.cindervane_slash_left"));
        controller.triggerableAnim("landed", LANDED);
        amphithere.getVocalEntries().forEach((key, entry) -> {
            if (!ACTION_CONTROLLER.equals(entry.controllerId())) {
                return;
            }
            if (entry.animationId() != null && !entry.animationId().isEmpty()) {
                controller.triggerableAnim(key, RawAnimation.begin().thenPlay(entry.animationId()));
            }
        });
    }

    public void setupStateController(AnimationController<Cindervane> controller) {
        DragonStateAnimationHelper.register(controller, DragonStateAnimationHelper.SIT_DOWN, SIT_DOWN);
        DragonStateAnimationHelper.register(controller, DragonStateAnimationHelper.SIT_UP, SIT_UP);
        DragonStateAnimationHelper.register(controller, DragonStateAnimationHelper.FALL_ASLEEP, FALL_ASLEEP);
        DragonStateAnimationHelper.register(controller, DragonStateAnimationHelper.SLEEP, SLEEP);
        DragonStateAnimationHelper.register(controller, DragonStateAnimationHelper.WAKE_UP, WAKE_UP);
    }

    public PlayState fastActionPredicate(AnimationState<Cindervane> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }

    public void setupFastActionController(AnimationController<Cindervane> controller) {
        controller.triggerableAnim("takeoff", TAKEOFF);
    }

    public void setupInteractionController(AnimationController<Cindervane> controller) {
        controller.triggerableAnim(DragonInteractionAnimationHelper.EAT,
                RawAnimation.begin().thenPlay("animation.cindervane.eat"));
        controller.triggerableAnim("cindervane_hurt",
                RawAnimation.begin().thenPlay("animation.cindervane.hurt"));
        controller.triggerableAnim(DragonInteractionAnimationHelper.DIE,
                RawAnimation.begin().thenPlay("animation.cindervane.die"));
    }

    public void triggerSitDownAnimation() {
        amphithere.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.SIT_DOWN);
    }

    public void triggerSitUpAnimation() {
        amphithere.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.SIT_UP);
    }

    public void triggerFallAsleepAnimation() {
        amphithere.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.FALL_ASLEEP);
    }

    public void triggerSleepAnimation() {
        amphithere.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.SLEEP);
    }

    public void triggerWakeUpAnimation() {
        amphithere.triggerAnim(DragonStateAnimationHelper.CONTROLLER, DragonStateAnimationHelper.WAKE_UP);
    }

    public PlayState actionPredicate(AnimationState<Cindervane> state) {
        state.getController().transitionLength(5);
        return PlayState.STOP;
    }

}
