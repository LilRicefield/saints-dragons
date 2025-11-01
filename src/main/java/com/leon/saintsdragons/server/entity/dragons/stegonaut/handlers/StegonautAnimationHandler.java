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
    private static final RawAnimation SLEEP_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.sleep");
    private static final RawAnimation SIT_ANIM = RawAnimation.begin().thenLoop("animation.stegonaut.sit");
    
    public StegonautAnimationHandler(Stegonaut drake) {
        this.drake = drake;
    }
    
    /**
     * Main animation handler with smooth transitions
     */
    public PlayState handleMovementAnimation(AnimationState<Stegonaut> state) {
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
        // Stegonaut only walks (no running - it's a slow, heavy drake)
        int groundState = drake.getEffectiveGroundState();
        if (groundState >= 1 || drake.isWalking()) {
            state.setAndContinue(WALK_ANIM);
        } else {
            state.setAndContinue(IDLE_ANIM);
        }

        return PlayState.CONTINUE;
    }
    
    /**
     * Handle play dead animation - the actual fake_death animation plays on the action controller
     * Movement controller should play the idle pose (not stop) to maintain base pose
     */
    private PlayState handlePlayDeadAnimation(AnimationState<Stegonaut> state) {
        // While playing dead, maintain idle animation as base pose
        // The fake_death looping animation on action controller will override this
        state.getController().transitionLength(10);
        state.setAndContinue(IDLE_ANIM);
        return PlayState.CONTINUE;
    }
    
    /**
     * Handle sleep animation - simple and smooth
     */
    private PlayState handleSleepAnimation(AnimationState<Stegonaut> state) {
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

        // Register hurt and die animations
        actionController.triggerableAnim("hurt",
                RawAnimation.begin().thenPlay("animation.stegonaut.hurt"));
        actionController.triggerableAnim("die",
                RawAnimation.begin().thenPlay("animation.stegonaut.die"));

        // Register fake death animation for lightning wyvern interaction
        // fake_death loops while playing dead, clear_fake_death stops the loop
        actionController.triggerableAnim("fake_death",
                RawAnimation.begin().thenLoop("animation.stegonaut.fake_death"));
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

