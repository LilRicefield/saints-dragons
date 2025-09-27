package com.leon.saintsdragons.server.entity.base;

import com.leon.saintsdragons.server.entity.interfaces.RideableDragon;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Base implementation for rideable dragons.
 * Provides common functionality for animation state management and rider input handling.
 *
 * Usage: Extend this class in your dragon entity to get the standard rideable dragon behavior.
 */
public abstract class RideableDragonBase extends DragonEntity implements RideableDragon, FlyingAnimal {

    protected RideableDragonBase(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    // This method should be overridden by subclasses to define their own entity data keys
    protected abstract void defineRideableDragonData();

    // ===== RIDER INPUT IMPLEMENTATION =====

    // Abstract methods for entity-specific data accessors
    protected abstract EntityDataAccessor<Float> getRiderForwardAccessor();
    protected abstract EntityDataAccessor<Float> getRiderStrafeAccessor();
    protected abstract EntityDataAccessor<Integer> getGroundMoveStateAccessor();
    protected abstract EntityDataAccessor<Integer> getFlightModeAccessor();
    protected abstract EntityDataAccessor<Boolean> getGoingUpAccessor();
    protected abstract EntityDataAccessor<Boolean> getGoingDownAccessor();
    protected abstract EntityDataAccessor<Boolean> getAcceleratingAccessor();

    @Override
    public void setLastRiderForward(float forward) {
        this.entityData.set(getRiderForwardAccessor(), forward);
    }

    @Override
    public void setLastRiderStrafe(float strafe) {
        this.entityData.set(getRiderStrafeAccessor(), strafe);
    }

    // ===== MOVEMENT STATE IMPLEMENTATION =====

    @Override
    public int getGroundMoveState() {
        return this.entityData.get(getGroundMoveStateAccessor());
    }

    @Override
    public int getSyncedFlightMode() {
        return this.entityData.get(getFlightModeAccessor());
    }

    @Override
    public int getEffectiveGroundState() {
        Integer state = this.getAnimData(com.leon.saintsdragons.common.network.DragonAnimTickets.GROUND_STATE);
        if (state != null) {
            return state;
        }
        return this.entityData.get(getGroundMoveStateAccessor());
    }

    // ===== RIDER CONTROL IMPLEMENTATION =====

    @Override
    public boolean isGoingUp() {
        return this.entityData.get(getGoingUpAccessor());
    }

    @Override
    public void setGoingUp(boolean goingUp) {
        this.entityData.set(getGoingUpAccessor(), goingUp);
    }

    @Override
    public boolean isGoingDown() {
        return this.entityData.get(getGoingDownAccessor());
    }

    @Override
    public void setGoingDown(boolean goingDown) {
        this.entityData.set(getGoingDownAccessor(), goingDown);
    }

    @Override
    public boolean isAccelerating() {
        return this.entityData.get(getAcceleratingAccessor());
    }

    @Override
    public void setAccelerating(boolean accelerating) {
        this.entityData.set(getAcceleratingAccessor(), accelerating);
    }

    // ===== ANIMATION SYNC IMPLEMENTATION =====

    @Override
    public void syncAnimState(int groundState, int flightMode) {
        if (level().isClientSide) {
            return;
        }
        this.setAnimData(com.leon.saintsdragons.common.network.DragonAnimTickets.GROUND_STATE, groundState);
        this.setAnimData(com.leon.saintsdragons.common.network.DragonAnimTickets.FLIGHT_MODE, flightMode);
    }

    @Override
    public void initializeAnimationState() {
        if (!level().isClientSide) {
            // Set initial state based on current entity state
            int initialGroundState = 0; // Default to idle
            int initialFlightMode = -1; // Default to ground state

            if (!isFlying() && !isTakeoff() && !isLanding() && !isHovering()) {
                // Check if entity is actually moving
                double velSqr = this.getDeltaMovement().horizontalDistanceSqr();
                initialGroundState = RideableDragonData.getGroundStateFromVelocity(velSqr);
            } else if (isFlying()) {
                initialFlightMode = getFlightMode();
            }

            // Set the initial state without triggering sync (to avoid thrashing)
            this.entityData.set(getGroundMoveStateAccessor(), initialGroundState);
            this.entityData.set(getFlightModeAccessor(), initialFlightMode);
        }
    }

    @Override
    public void resetAnimationState() {
        if (!level().isClientSide) {
            // Recalculate current state based on actual entity state
            int currentGroundState = 0; // Default to idle

            if (!isFlying() && !isTakeoff() && !isLanding() && !isHovering()) {
                // Recalculate ground movement state based on current velocity
                double velSqr = this.getDeltaMovement().horizontalDistanceSqr();
                currentGroundState = RideableDragonData.getGroundStateFromVelocity(velSqr);
            }

            int currentFlightMode = getFlightMode();

            // Update entity data to match calculated state
            this.entityData.set(getGroundMoveStateAccessor(), currentGroundState);
            this.entityData.set(getFlightModeAccessor(), currentFlightMode);

            // Force sync current state
            this.syncAnimState(currentGroundState, currentFlightMode);
        }
    }

    // ===== REQUIRED OVERRIDES IMPLEMENTATION =====

    @Override
    public @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        Vec3 input = super.getRiddenInput(player, deltaIn);

        // Capture rider inputs for animation state (like Lightning Dragon)
        if (!level().isClientSide && !isFlying()) {
            float fwd = (float) Mth.clamp(input.z, -1.0, 1.0);
            float str = (float) Mth.clamp(input.x, -1.0, 1.0);
            this.setLastRiderForward(RideableDragonData.applyInputThreshold(fwd));
            this.setLastRiderStrafe(RideableDragonData.applyInputThreshold(str));
        }

        return input;
    }

    @Override
    public void removePassenger(@NotNull Entity passenger) {
        super.removePassenger(passenger);
        // Reset rider-driven movement states immediately on dismount
        if (!this.level().isClientSide) {
            this.setAccelerating(false);
            this.setRunning(false);
            this.setLastRiderForward(0f);
            this.setLastRiderStrafe(0f);
            this.entityData.set(getGroundMoveStateAccessor(), 0);
            // Nudge observers so animation stops if we dismounted mid-run/walk
            this.syncAnimState(0, getSyncedFlightMode());
        }
    }

    // ===== ANIMATION STATE TICKING IMPLEMENTATION =====

    @Override
    public void tickAnimationStates() {
        // Update ground movement state with more sophisticated detection
        int moveState = 0; // idle

        if (!isFlying() && !isTakeoff() && !isLanding() && !isHovering()) {
            // If being ridden, prefer rider inputs for robust state selection
            if (getControllingPassenger() != null) {
                float fwd = this.entityData.get(getRiderForwardAccessor());
                float str = this.entityData.get(getRiderStrafeAccessor());

                if (RideableDragonData.isSignificantRiderInput(fwd, str)) {
                    moveState = this.isAccelerating() ? 2 : 1;
                } else {
                    // Fallback while ridden: use actual velocity so observers still see walk/run
                    double speedSqr = getDeltaMovement().horizontalDistanceSqr();
                    moveState = RideableDragonData.getRiddenGroundStateFromVelocity(speedSqr);
                }
            } else {
                // Use horizontal velocity for AI classification
                double velSqr = this.getDeltaMovement().horizontalDistanceSqr();
                moveState = RideableDragonData.getGroundStateFromVelocity(velSqr);
            }
        }

        // Update flight mode
        int flightMode = getFlightMode();

        // Update entity data and sync to clients
        boolean groundStateChanged = this.entityData.get(getGroundMoveStateAccessor()) != moveState;
        boolean flightModeChanged = this.entityData.get(getFlightModeAccessor()) != flightMode;

        if (groundStateChanged) {
            this.entityData.set(getGroundMoveStateAccessor(), moveState);
        }

        if (flightModeChanged) {
            this.entityData.set(getFlightModeAccessor(), flightMode);
        }

        // Send animation state sync to clients when states change
        if (groundStateChanged || flightModeChanged) {
            this.syncAnimState(moveState, flightMode);
        }

        // Decay rider inputs slightly each tick to avoid sticking when packets drop
        if (this.entityData.get(getRiderForwardAccessor()) != 0f ||
                this.entityData.get(getRiderStrafeAccessor()) != 0f) {

            float nf = RideableDragonData.decayRiderInput(this.entityData.get(getRiderForwardAccessor()));
            float ns = RideableDragonData.decayRiderInput(this.entityData.get(getRiderStrafeAccessor()));

            this.entityData.set(getRiderForwardAccessor(), nf);
            this.entityData.set(getRiderStrafeAccessor(), ns);
        }

        // Stop running if not moving
        if (this.isRunning() && this.getDeltaMovement().horizontalDistanceSqr() < 0.01) {
            this.setRunning(false);
        }
    }

    // ===== ABSTRACT METHODS TO IMPLEMENT =====

    /**
     * Get the current flight mode. Must be implemented by subclasses.
     *
     * @return flight mode (-1=ground, 0=glide, 1=forward, 2=hover, 3=takeoff)
     */
    protected abstract int getFlightMode();

    /**
     * Check if the dragon is flying. Final bridge delegates to subclass hook to avoid obfuscation mismatches.
     */
    @Override
    public final boolean isFlying() {
        return isDragonFlying();
    }

    /**
     * Subclass hook to report actual flying state.
     */
    protected abstract boolean isDragonFlying();

    /**
     * Check if the dragon is taking off. Must be implemented by subclasses.
     */
    public abstract boolean isTakeoff();

    /**
     * Check if the dragon is landing. Must be implemented by subclasses.
     */
    public abstract boolean isLanding();

    /**
     * Check if the dragon is hovering. Must be implemented by subclasses.
     */
    public abstract boolean isHovering();

    /**
     * Check if the dragon is running. Must be implemented by subclasses.
     */
    public abstract boolean isRunning();

    /**
     * Set if the dragon is running. Must be implemented by subclasses.
     */
    public abstract void setRunning(boolean running);
}
