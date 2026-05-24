package com.leon.saintsdragons.server.entity.dragons.cindervane.handlers;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.util.animation.DragonFlightAnimationHelper;
import com.leon.saintsdragons.util.animation.DragonInteractionAnimationHelper;
import com.leon.saintsdragons.util.animation.MovementAnimationHelper;
import com.leon.saintsdragons.util.animation.DragonStateAnimationHelper;
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
    private static final DragonFlightAnimationHelper.Animations FLIGHT_ANIMATIONS =
            new DragonFlightAnimationHelper.Animations(TAKEOFF, null, LANDED, GLIDE, GLIDE_DOWN, FLY_IDLE, FLAP, SPRINT_FLAP);
    private static final DragonFlightAnimationHelper.Transitions FLIGHT_TRANSITIONS =
            new DragonFlightAnimationHelper.Transitions(1, 8, 6, 3, 6, 6, 3, 1);

    public CindervaneAnimationHandler(Cindervane dragon) {
        this.amphithere = dragon;
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

    public void setupFastActionController(AnimationController<Cindervane> controller) {
    }

    public void setupFlightController(AnimationController<Cindervane> controller) {
        DragonFlightAnimationHelper.registerStandard(controller, TAKEOFF, null, LANDED);
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

    public PlayState movementPredicate(AnimationState<Cindervane> state) {
        state.getController().transitionLength(10);
        boolean aerialState = amphithere.isFlying() || amphithere.isTakeoff() || amphithere.isLanding() || amphithere.isHovering();

        if (amphithere.isDying()) {
            return PlayState.STOP;
        }

        PlayState sleepPose = MovementAnimationHelper.tryHandleRestPose(state, amphithere, SLEEP, SIT, 6, 0, false);
        if (sleepPose != null) {
            return sleepPose;
        }

        boolean inWater = amphithere.isInWater() || amphithere.isInWaterOrBubble();

        if (!aerialState && inWater) {
            state.getController().transitionLength(6);
            state.setAndContinue(SWIM);
            state.getController().setAnimationSpeed(1.0f);
            return PlayState.CONTINUE;
        }

        if (!aerialState && amphithere.isFallingForAnimation()) {
            state.getController().transitionLength(4);
            state.setAndContinue(FALLING);
            state.getController().setAnimationSpeed(1.0f);
            return PlayState.CONTINUE;
        }

        if (aerialState) {
            return PlayState.STOP;
        }

        if (amphithere.isVehicle()) {
            state.getController().transitionLength(4);
            int groundState = amphithere.getEffectiveGroundState();
            if (groundState == 2) {
                state.setAndContinue(RUN);
            } else if (groundState == 1) {
                state.setAndContinue(WALK);
            } else {
                state.setAndContinue(IDLE);
            }
            state.getController().setAnimationSpeed(1.0f);
            return PlayState.CONTINUE;
        }
        PlayState sitPose = MovementAnimationHelper.tryHandleRestPose(state, amphithere, null, SIT, 0, 0);
        if (sitPose != null) {
            return sitPose;
        }

        state.getController().setAnimationSpeed(1.0f);

        return MovementAnimationHelper.handleGroundMovement(state, amphithere, IDLE, WALK, RUN);
    }

    public PlayState flightPredicate(AnimationState<Cindervane> state) {
        if (amphithere.isDying()) {
            return PlayState.STOP;
        }
        boolean aerialState = amphithere.isFlying() || amphithere.isTakeoff() || amphithere.isLanding() || amphithere.isHovering();
        if (!aerialState) {
            return PlayState.STOP;
        }
        if (amphithere.isTakeoff()) {
            return DragonFlightAnimationHelper.handleTakeoff(state, false, FLIGHT_ANIMATIONS, FLIGHT_TRANSITIONS);
        }
        return DragonFlightAnimationHelper.handleState(
                state,
                amphithere.getVisualFlightState(state.getPartialTick()),
                FLIGHT_ANIMATIONS,
                FLIGHT_TRANSITIONS
        );
    }
    public PlayState actionPredicate(AnimationState<Cindervane> state) {
        state.getController().transitionLength(4);
        return PlayState.STOP;
    }
    public PlayState fastActionPredicate(AnimationState<Cindervane> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }
}