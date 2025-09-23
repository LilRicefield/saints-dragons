package com.leon.saintsdragons.server.entity.interfaces;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for dragons that can be ridden.
 * Defines the minimum requirements for rideable dragon behavior and animation state management.
 * 
 * All rideable dragons should implement this interface to ensure consistent behavior.
 */
public interface RideableDragon {
    
    // ===== RIDER INPUT METHODS =====
    
    /**
     * Set the last rider forward input for animation state tracking
     */
    void setLastRiderForward(float forward);
    
    /**
     * Set the last rider strafe input for animation state tracking
     */
    void setLastRiderStrafe(float strafe);
    
    // ===== MOVEMENT STATE METHODS =====
    
    /**
     * Get the current ground movement state (0=idle, 1=walk, 2=run)
     */
    int getGroundMoveState();
    
    /**
     * Get the current flight mode (-1=ground, 0=glide, 1=forward, 2=hover, 3=takeoff)
     */
    int getSyncedFlightMode();
    
    /**
     * Get the effective ground state for client-side animation consistency
     */
    int getEffectiveGroundState();
    
    // ===== RIDER CONTROL METHODS =====
    
    /**
     * Check if the dragon is going up (rider control)
     */
    boolean isGoingUp();
    
    /**
     * Set if the dragon is going up (rider control)
     */
    void setGoingUp(boolean goingUp);
    
    /**
     * Check if the dragon is going down (rider control)
     */
    boolean isGoingDown();
    
    /**
     * Set if the dragon is going down (rider control)
     */
    void setGoingDown(boolean goingDown);
    
    /**
     * Check if the dragon is accelerating
     */
    boolean isAccelerating();
    
    /**
     * Set if the dragon is accelerating
     */
    void setAccelerating(boolean accelerating);
    
    // ===== ANIMATION SYNC METHODS =====
    
    /**
     * Sync animation state to clients
     */
    void syncAnimState(int groundState, int flightMode);
    
    /**
     * Initialize animation state after entity loading
     */
    void initializeAnimationState();
    
    /**
     * Reset animation state to prevent thrashing
     */
    void resetAnimationState();
    
    // ===== REQUIRED OVERRIDES =====
    
    /**
     * Override getRiddenInput to capture rider inputs for animation state
     * 
     * Example implementation:
     * <pre>
     * {@code
     * @Override
     * protected @NotNull Vec3 getRiddenInput(@Nonnull Player player, @Nonnull Vec3 deltaIn) {
     *     Vec3 input = super.getRiddenInput(player, deltaIn);
     *     
     *     // Capture rider inputs for animation state
     *     if (!level().isClientSide && !isFlying()) {
     *         float fwd = (float) Mth.clamp(input.z, -1.0, 1.0);
     *         float str = (float) Mth.clamp(input.x, -1.0, 1.0);
     *         this.setLastRiderForward(RideableDragonData.applyInputThreshold(fwd));
     *         this.setLastRiderStrafe(RideableDragonData.applyInputThreshold(str));
     *     }
     *     
     *     return input;
     * }
     * }
     * </pre>
     */
    @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn);
    
    /**
     * Override removePassenger to clean up rider states on dismount
     * 
     * Example implementation:
     * <pre>
     * {@code
     * @Override
     * protected void removePassenger(@Nonnull Entity passenger) {
     *     super.removePassenger(passenger);
     *     // Reset rider-driven movement states immediately on dismount
     *     if (!this.level().isClientSide) {
     *         this.setAccelerating(false);
     *         this.setRunning(false);
     *         this.setLastRiderForward(0f);
     *         this.setLastRiderStrafe(0f);
     *         this.entityData.set(RideableDragonData.DATA_GROUND_MOVE_STATE, 0);
     *         this.syncAnimState(0, getSyncedFlightMode());
     *     }
     * }
     * }
     * </pre>
     */
    void removePassenger(@NotNull net.minecraft.world.entity.Entity passenger);
    
    // ===== ANIMATION STATE TICKING =====
    
    /**
     * Tick animation states - should be called every tick
     * 
     * This method should implement the core animation state logic:
     * 1. Check if being ridden and use rider inputs for state
     * 2. Fall back to velocity-based detection for observers
     * 3. Decay rider inputs over time
     * 4. Sync state changes to clients
     */
    void tickAnimationStates();
}
