package com.leon.saintsdragons.server.entity.controller.primitivedrake;

import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * Handles smooth animation transitions for the Primitive Drake
 * Based on Lightning Dragon's animation system for consistent behavior
 */
public class PrimitiveDrakeAnimationController {
    private final PrimitiveDrakeEntity drake;
    
    // Animation state tracking for smooth transitions
    private RawAnimation currentAnimation = null;
    private int animationTransitionTicks = 0;
    private static final int TRANSITION_COOLDOWN = 5; // Prevent rapid switching
    
    // Movement detection smoothing
    private boolean wasMoving = false;
    private int movementDetectionTicks = 0;
    private static final int MOVEMENT_DETECTION_DELAY = 3; // Ticks to confirm movement state
    
    // Animation constants
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.primitive_drake.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.primitive_drake.walk");
    
    public PrimitiveDrakeAnimationController(PrimitiveDrakeEntity drake) {
        this.drake = drake;
    }
    
    /**
     * Main animation handler with smooth transitions
     */
    public PlayState handleMovementAnimation(AnimationState<PrimitiveDrakeEntity> state) {
        // Set default transition length for smooth blending
        state.getController().transitionLength(8); // Smooth but not too slow
        
        // Update movement detection with smoothing
        boolean isMoving = detectMovement();
        
        // Determine desired animation
        RawAnimation desiredAnimation = isMoving ? WALK_ANIM : IDLE_ANIM;
        
        // Handle animation transitions with hysteresis
        if (currentAnimation != desiredAnimation) {
            if (animationTransitionTicks <= 0) {
                // Allow transition after cooldown
                currentAnimation = desiredAnimation;
                animationTransitionTicks = TRANSITION_COOLDOWN;
                
                // Adjust transition length based on animation type
                if (desiredAnimation == WALK_ANIM) {
                    state.getController().transitionLength(6); // Quicker into walk
                } else {
                    state.getController().transitionLength(10); // Slower into idle for smoothness
                }
            } else {
                animationTransitionTicks--;
            }
        } else {
            animationTransitionTicks = Math.max(0, animationTransitionTicks - 1);
        }
        
        // Set the animation
        state.setAndContinue(currentAnimation != null ? currentAnimation : IDLE_ANIM);
        
        return PlayState.CONTINUE;
    }
    
    /**
     * Enhanced movement detection with smoothing to prevent jittery animations
     */
    private boolean detectMovement() {
        // Multiple movement detection methods for reliability
        boolean velocityMoving = drake.getDeltaMovement().lengthSqr() > 0.005;
        boolean navigationMoving = drake.getNavigation().isInProgress();
        boolean pathfindingMoving = drake.getMoveControl().hasWanted();
        
        // Additional checks for more reliable movement detection
        boolean horizontalMoving = drake.getDeltaMovement().horizontalDistanceSqr() > 0.002;
        boolean hasPath = drake.getNavigation().getPath() != null && !drake.getNavigation().isDone();
        
        // Combine detection methods
        boolean currentlyMoving = velocityMoving || navigationMoving || pathfindingMoving || horizontalMoving || hasPath;
        
        // Apply smoothing to prevent rapid state changes
        if (currentlyMoving != wasMoving) {
            movementDetectionTicks++;
            if (movementDetectionTicks >= MOVEMENT_DETECTION_DELAY) {
                wasMoving = currentlyMoving;
                movementDetectionTicks = 0;
            }
        } else {
            movementDetectionTicks = 0;
        }
        
        return wasMoving;
    }
    
    /**
     * Initialize animation state on spawn
     */
    public void initializeAnimation() {
        currentAnimation = IDLE_ANIM;
        wasMoving = false;
        movementDetectionTicks = 0;
        animationTransitionTicks = 0;
    }
    
    /**
     * Reset animation state (useful for debugging or state changes)
     */
    public void resetAnimationState() {
        currentAnimation = null;
        animationTransitionTicks = 0;
    }
}
