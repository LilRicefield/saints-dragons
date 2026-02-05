package com.leon.saintsdragons.server.entity.dragons.stegonaut.handlers;

import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * Handles smooth animation transitions for the Primitive Drake
 * Based on Lightning Dragon's animation system for consistent behavior
 */
public class StegonautAnimationHandler {
    private final Stegonaut drake;
    
    
    // Animation constants
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.walk");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.run");
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.swim");
    private static final RawAnimation SLEEP_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.sleep");
    private static final RawAnimation SIT_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.sit");

    // Rest transition animations (one-shot)
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlay("animation.stegonaut.down");
    private static final RawAnimation SIT_UP = RawAnimation.begin().thenPlay("animation.stegonaut.up");
    private static final RawAnimation FALL_ASLEEP = RawAnimation.begin().thenPlay("animation.stegonaut.fall_asleep");
    private static final RawAnimation WAKE_UP = RawAnimation.begin().thenPlay("animation.stegonaut.wake_up");
    
    public StegonautAnimationHandler(Stegonaut drake) {
        this.drake = drake;
    }

    // ===== ANIMATION TRIGGER HELPERS =====

    /**
     * Triggers the sit down transition animation
     */
    public void triggerSitDownAnimation() {
        drake.triggerAnim("action", "sit_down");
    }

    /**
     * Triggers the stand up transition animation
     */
    public void triggerSitUpAnimation() {
        drake.triggerAnim("action", "sit_up");
    }

    /**
     * Triggers the fall asleep transition animation (sit → sleep)
     */
    public void triggerFallAsleepAnimation() {
        drake.triggerAnim("action", "fall_asleep");
    }

    /**
     * Triggers the sleep loop animation
     */
    public void triggerSleepAnimation() {
        drake.triggerAnim("action", "sleep");
    }

    /**
     * Triggers the wake up transition animation (sleep → sit)
     */
    public void triggerWakeUpAnimation() {
        drake.triggerAnim("action", "wake_up");
    }
    
    /**
     * Main animation handler with smooth transitions
     */
    public PlayState handleMovementAnimation(AnimationState<Stegonaut> state) {
        // Set default transition length for smooth blending
        state.getController().transitionLength(8); // Smooth but not too slow

        // CLIENT-SIDE GRACE PERIOD: Prevent T-pose on world rejoin with shaders
        // Wait for entity data to sync from server before processing animations
        if (drake.level().isClientSide && !drake.isClientAnimationReady()) {
            state.setAndContinue(IDLE_ANIM);
            return PlayState.CONTINUE;
        }

        // Swimming has higher priority than ground loops
        if (drake.isInWaterOrBubble()) {
            state.getController().transitionLength(6);
            state.setAndContinue(SWIM_ANIM);
            return PlayState.CONTINUE;
        } else if (drake.isSleeping() && !drake.isSleepingEntering() && !drake.isSleepingExiting()) {
            // Continuously apply sleep loop animation (survives chunk reload)
            state.getController().transitionLength(6);
            state.setAndContinue(SLEEP_ANIM);
            return PlayState.CONTINUE;
        } else if (drake.isSleepingEntering() || drake.isSleepingExiting()) {
            // Transition animations are triggered, don't interfere
            return PlayState.STOP;
        } else if (drake.getSitProgress() > 0.5f) {
            // Drive SIT from our custom progress system only to avoid de-sync
            state.getController().transitionLength(4);
            state.setAndContinue(SIT_ANIM);
            return PlayState.CONTINUE;
        }

        // Use the improved movement state detection
        int groundState = drake.getEffectiveGroundState();
        if (groundState == 2 || drake.isRunning()) {
            // Running state
            state.setAndContinue(RUN_ANIM);
        } else if (groundState == 1 || drake.isWalking()) {
            // Walking state
            state.setAndContinue(WALK_ANIM);
        } else {
            state.setAndContinue(IDLE_ANIM);
        }

        return PlayState.CONTINUE;
    }

    
    
    /**
     * Initialize animation state on spawn
     */
    public void initializeAnimation() {
        // Animation state is now handled by the entity's movement state tracking
    }
    
    /**
     * Reset animation state (useful for debugging or state changes)
     */
    public void resetAnimationState() {
        // Animation state is now handled by the entity's movement state tracking
    }
    
    // ===== ACTION CONTROLLER SETUP =====
    
    /**
     * Sets up all GeckoLib animation triggers for the action controller
     */
    public void setupActionController(AnimationController<Stegonaut> actionController) {
        // Register grumble animations
        actionController.triggerableAnim("grumble1",
                RawAnimation.begin().thenPlay("animation.stegonaut.grumble1"));
        actionController.triggerableAnim("grumble2",
                RawAnimation.begin().thenPlay("animation.stegonaut.grumble2"));
        actionController.triggerableAnim("grumble3",
                RawAnimation.begin().thenPlay("animation.stegonaut.grumble3"));

        // Eat animation - triggered when feeding
        actionController.triggerableAnim("eat",
                RawAnimation.begin().thenPlay("animation.stegonaut.eat"));
        actionController.triggerableAnim("bite",
                RawAnimation.begin().thenPlay("animation.stegonaut.bite"));
        actionController.triggerableAnim("chin_slam",
                RawAnimation.begin().thenPlay("animation.stegonaut.chin_slam"));
        actionController.triggerableAnim("ground_eating",
                RawAnimation.begin().thenPlay("animation.stegonaut.ground_eating"));
        actionController.triggerableAnim("ground_eating_hold",
                RawAnimation.begin().thenLoop("animation.stegonaut.ground_eating_hold"));
        actionController.triggerableAnim("ground_eating_shoot",
                RawAnimation.begin().thenPlay("animation.stegonaut.ground_eating_shoot"));
        actionController.triggerableAnim("ground_eating_cancel",
                RawAnimation.begin().thenPlay("animation.stegonaut.ground_eating_cancel"));

        // Rest transition animations (sit ↔ idle, sit ↔ sleep)
        actionController.triggerableAnim("sit_down", SIT_DOWN);
        actionController.triggerableAnim("sit_up", SIT_UP);
        actionController.triggerableAnim("fall_asleep", FALL_ASLEEP);
        actionController.triggerableAnim("sleep", SLEEP_ANIM);
        actionController.triggerableAnim("wake_up", WAKE_UP);
    }

    public PlayState instantActionPredicate(AnimationState<Stegonaut> state) {
        state.getController().transitionLength(1);
        return PlayState.STOP;
    }

    public void setupInstantActionController(AnimationController<Stegonaut> controller) {
        controller.triggerableAnim("stegonaut_hurt",
                RawAnimation.begin().thenPlay("animation.stegonaut.hurt"));
        controller.triggerableAnim("hurt",
                RawAnimation.begin().thenPlay("animation.stegonaut.hurt"));
        controller.triggerableAnim("die",
                RawAnimation.begin().thenPlay("animation.stegonaut.die"));
    }
    
    /**
     * Handles action animations (grumbles, etc.)
     */
    public PlayState actionPredicate(AnimationState<Stegonaut> state) {
        // Native GeckoLib: controller idles until triggerAnim is fired
        state.getController().transitionLength(5);
        
        // For now, just return STOP - the action animations will be triggered via triggerAnim()
        return PlayState.STOP;
    }
}
