package com.leon.saintsdragons.server.entity.base;

import com.leon.saintsdragons.server.entity.interfaces.RideableDragon;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
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
public abstract class RideableDragonBase extends DragonEntity implements RideableDragon {

    protected RideableDragonBase(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    // ===== RIDER INPUT IMPLEMENTATION =====

    @Override
    public void setLastRiderForward(float forward) {
        this.entityData.set(RideableDragonData.DATA_RIDER_FORWARD, forward);
    }

    @Override
    public void setLastRiderStrafe(float strafe) {
        this.entityData.set(RideableDragonData.DATA_RIDER_STRAFE, strafe);
    }

    // ===== MOVEMENT STATE IMPLEMENTATION =====

    @Override
    public int getGroundMoveState() {
        return this.entityData.get(RideableDragonData.DATA_GROUND_MOVE_STATE);
    }

    @Override
    public int getSyncedFlightMode() {
        return this.entityData.get(RideableDragonData.DATA_FLIGHT_MODE);
    }

    @Override
    public int getEffectiveGroundState() {
        Integer state = this.getAnimData(com.leon.saintsdragons.common.network.DragonAnimTickets.GROUND_STATE);
        if (state != null) {
            return state;
        }
        return this.entityData.get(RideableDragonData.DATA_GROUND_MOVE_STATE);
    }

    // ===== RIDER CONTROL IMPLEMENTATION =====

    @Override
    public boolean isGoingUp() {
        return this.entityData.get(RideableDragonData.DATA_GOING_UP);
    }

    @Override
    public void setGoingUp(boolean goingUp) {
        this.entityData.set(RideableDragonData.DATA_GOING_UP, goingUp);
    }

    @Override
    public boolean isGoingDown() {
        return this.entityData.get(RideableDragonData.DATA_GOING_DOWN);
    }

    @Override
    public void setGoingDown(boolean goingDown) {
        this.entityData.set(RideableDragonData.DATA_GOING_DOWN, goingDown);
    }

    @Override
    public boolean isAccelerating() {
        return this.entityData.get(RideableDragonData.DATA_ACCELERATING);
    }

    @Override
    public void setAccelerating(boolean accelerating) {
        this.entityData.set(RideableDragonData.DATA_ACCELERATING, accelerating);
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
            this.entityData.set(RideableDragonData.DATA_GROUND_MOVE_STATE, initialGroundState);
            this.entityData.set(RideableDragonData.DATA_FLIGHT_MODE, initialFlightMode);
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
            this.entityData.set(RideableDragonData.DATA_GROUND_MOVE_STATE, currentGroundState);
            this.entityData.set(RideableDragonData.DATA_FLIGHT_MODE, currentFlightMode);

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
            this.entityData.set(RideableDragonData.DATA_GROUND_MOVE_STATE, 0);
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
                float fwd = this.entityData.get(RideableDragonData.DATA_RIDER_FORWARD);
                float str = this.entityData.get(RideableDragonData.DATA_RIDER_STRAFE);

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
        boolean groundStateChanged = this.entityData.get(RideableDragonData.DATA_GROUND_MOVE_STATE) != moveState;
        boolean flightModeChanged = this.entityData.get(RideableDragonData.DATA_FLIGHT_MODE) != flightMode;

        if (groundStateChanged) {
            this.entityData.set(RideableDragonData.DATA_GROUND_MOVE_STATE, moveState);
        }

        if (flightModeChanged) {
            this.entityData.set(RideableDragonData.DATA_FLIGHT_MODE, flightMode);
        }

        // Send animation state sync to clients when states change
        if (groundStateChanged || flightModeChanged) {
            this.syncAnimState(moveState, flightMode);
        }

        // Decay rider inputs slightly each tick to avoid sticking when packets drop
        if (this.entityData.get(RideableDragonData.DATA_RIDER_FORWARD) != 0f ||
                this.entityData.get(RideableDragonData.DATA_RIDER_STRAFE) != 0f) {

            float nf = RideableDragonData.decayRiderInput(this.entityData.get(RideableDragonData.DATA_RIDER_FORWARD));
            float ns = RideableDragonData.decayRiderInput(this.entityData.get(RideableDragonData.DATA_RIDER_STRAFE));

            this.entityData.set(RideableDragonData.DATA_RIDER_FORWARD, nf);
            this.entityData.set(RideableDragonData.DATA_RIDER_STRAFE, ns);
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
     * Check if the dragon is flying. Must be implemented by subclasses.
     */
    public abstract boolean isFlying();

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
