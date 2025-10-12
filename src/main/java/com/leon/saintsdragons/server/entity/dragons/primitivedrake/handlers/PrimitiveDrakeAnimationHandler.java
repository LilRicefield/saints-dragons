package com.leon.saintsdragons.server.entity.dragons.primitivedrake.handlers;

import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * Handles smooth animation transitions for the Primitive Drake
 * Based on Lightning Dragon's animation system for consistent behavior
 */
public class PrimitiveDrakeAnimationHandler {
    private final PrimitiveDrakeEntity drake;
    
    
    // Animation constants
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.primitive_drake.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.primitive_drake.walk");
    private static final RawAnimation SLEEP_ANIM = RawAnimation.begin().thenLoop("animation.primitive_drake.sleep");
    private static final RawAnimation SIT_ANIM = RawAnimation.begin().thenLoop("animation.primitive_drake.sit");
    
    public PrimitiveDrakeAnimationHandler(PrimitiveDrakeEntity drake) {
        this.drake = drake;
    }
    
    /**
     * Main animation handler with smooth transitions
     */
    public PlayState handleMovementAnimation(AnimationState<PrimitiveDrakeEntity> state) {
        // Set default transition length for smooth blending
        state.getController().transitionLength(8); // Smooth but not too slow

        // Check if playing dead first - play dead animation takes highest priority
        if (drake.isPlayingDead()) {
            return handlePlayDeadAnimation(state);
        }

        // Check if sleeping - sleep animation takes priority
        if (drake.isSleeping()) {
            return handleSleepAnimation(state);
        }

        // Check for sitting - use sit progress system to avoid desync
        if (drake.isOrderedToSit() || drake.getSitProgress() > 0.5f) {
            state.setAndContinue(SIT_ANIM);
            return PlayState.CONTINUE;
        }

        // Use the improved movement state detection
        int groundState = drake.getEffectiveGroundState();
        if (groundState == 2 || drake.isRunning()) {
            state.setAndContinue(WALK_ANIM); // Fallback to walk for now
        } else if (groundState == 1 || drake.isWalking()) {
            state.setAndContinue(WALK_ANIM);
        } else {
            state.setAndContinue(IDLE_ANIM);
        }

        return PlayState.CONTINUE;
    }
    
    /**
     * Handle play dead animation - takes priority over all other animations
     */
    private PlayState handlePlayDeadAnimation(AnimationState<PrimitiveDrakeEntity> state) {
        // Set smooth transition to play dead animation
        state.getController().transitionLength(15); // Slower transition for dramatic effect
        
        // While playing dead, the movement controller should NOT play any movement animations
        // The fake_death animation is handled by the action controller
        // Just return STOP to prevent any movement animations from playing
        return PlayState.STOP;
    }
    
    /**
     * Handle sleep animation - simple and smooth
     */
    private PlayState handleSleepAnimation(AnimationState<PrimitiveDrakeEntity> state) {
        // Set smooth transition to sleep animation
        state.getController().transitionLength(12); // Slower transition for sleep
        
        // Always use sleep animation when sleeping
        state.setAndContinue(SLEEP_ANIM);
        
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
    public void setupActionController(AnimationController<PrimitiveDrakeEntity> actionController) {
        // Register grumble animations
        actionController.triggerableAnim("grumble1",
                RawAnimation.begin().thenPlay("animation.primitive_drake.grumble1"));
        actionController.triggerableAnim("grumble2",
                RawAnimation.begin().thenPlay("animation.primitive_drake.grumble2"));
        actionController.triggerableAnim("grumble3",
                RawAnimation.begin().thenPlay("animation.primitive_drake.grumble3"));

        // Register hurt and die animations
        actionController.triggerableAnim("hurt",
                RawAnimation.begin().thenPlay("animation.primitive_drake.hurt"));
        actionController.triggerableAnim("die",
                RawAnimation.begin().thenPlay("animation.primitive_drake.die"));

        // Register fake death animation for lightning wyvern interaction
        actionController.triggerableAnim("fake_death",
                RawAnimation.begin().thenLoop("animation.primitive_drake.fake_death"));
        actionController.triggerableAnim("clear_fake_death",
                RawAnimation.begin().thenWait(1));
    }
    
    /**
     * Handles action animations (grumbles, etc.)
     */
    public PlayState actionPredicate(AnimationState<PrimitiveDrakeEntity> state) {
        // Native GeckoLib: controller idles until triggerAnim is fired
        state.getController().transitionLength(5);
        
        // For now, just return STOP - the action animations will be triggered via triggerAnim()
        return PlayState.STOP;
    }
}

