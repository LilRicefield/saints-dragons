package com.leon.saintsdragons.server.entity.base;

import com.leon.saintsdragons.server.entity.interfaces.RideableDragon;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.network.MessageDragonRideInput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
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
import org.jetbrains.annotations.Nullable;

/**
 * Base implementation for rideable dragons.
 * Provides common functionality for animation state management and rider input handling.
 * Usage: Extend this class in your dragon entity to get the standard rideable dragon behavior.
 */
public abstract class RideableDragonBase extends DragonEntity implements RideableDragon, FlyingAnimal {

    /** Entity data accessor for melee mode (0=primary melee, 1=secondary melee) */
    private static final EntityDataAccessor<Integer> DATA_MELEE_MODE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(RideableDragonBase.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    protected RideableDragonBase(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MELEE_MODE, 0); // Default to primary melee (mode 0)
        defineRideableDragonData();
    }

    // This method should be overridden by subclasses to define their own entity data keys
    protected abstract void defineRideableDragonData();


    public boolean canBeControlledBy(Player player) {
        if (player == null) {
            return false;
        }
        if (this.isTame()) {
            return this.isOwnedBy(player);
        }
        return player.isCreative() || player.isSpectator();
    }

    public void handleRiderNetworkInput(ServerPlayer player, MessageDragonRideInput msg) {
        boolean locked = isRiderInputLocked(player);
        applyRiderVerticalInput(player, msg.goingUp(), msg.goingDown(), locked);
        applyRiderMovementInput(player, msg.forward(), msg.strafe(), msg.yaw(), locked);
        handleRiderAction(player, msg.action(), msg.abilityName(), locked);
    }

    protected boolean isRiderInputLocked(Player player) {
        return false;
    }

    protected void applyRiderVerticalInput(Player player, boolean goingUp, boolean goingDown, boolean locked) {
        if (locked) {
            setGoingUp(false);
            setGoingDown(false);
            return;
        }
        setGoingUp(goingUp);
        setGoingDown(goingDown);
    }

    protected float applyInputDeadzone(float value) {
        return Math.abs(value) > 0.02f ? value : 0f;
    }

    protected void applyRiderMovementInput(Player player, float forward, float strafe, float yaw, boolean locked) {
        float clampedForward = locked ? 0f : applyInputDeadzone(forward);
        float clampedStrafe = locked ? 0f : applyInputDeadzone(strafe);
        setLastRiderForward(clampedForward);
        setLastRiderStrafe(clampedStrafe);
    }

    protected void handleRiderAction(ServerPlayer player, DragonRiderAction action, String abilityName, boolean locked) {
        if (action == null) {
            return;
        }
        switch (action) {
            case TAKEOFF_REQUEST -> { if (!locked) onRiderTakeoffRequest(player); }
            case ACCELERATE -> { if (!locked) onRiderAccelerationStart(player); }
            case STOP_ACCELERATE -> onRiderAccelerationStop(player);
            case ABILITY_USE -> { if (!locked) onRiderAbilityUse(player, abilityName); }
            case ABILITY_STOP -> { if (!locked) onRiderAbilityStop(player, abilityName); }
            case TOGGLE_MELEE -> { if (!locked) onRiderToggleMelee(player); }
            default -> { }
        }
    }

    protected void onRiderToggleMelee(Player player) {
        toggleMeleeMode();
    }

    protected void onRiderTakeoffRequest(Player player) {
    }

    protected void onRiderAccelerationStart(Player player) {
        setAccelerating(true);
    }

    protected void onRiderAccelerationStop(Player player) {
        setAccelerating(false);
    }

    protected void onRiderAbilityUse(Player player, String abilityName) {
    }

    protected void onRiderAbilityStop(Player player, String abilityName) {
    }

    protected float getRiderLockYawBlend() {
        return 0.18F;
    }

    protected float getRiderLockPitchBlend() {
        return 0.18F;
    }

    protected float getRiderLockPitchMin() {
        return -45.0F;
    }

    protected float getRiderLockPitchMax() {
        return 45.0F;
    }


    @Nullable
    public RiderAbilityBinding getPrimaryRiderAbility() {
        return null;
    }

    @Nullable
    public RiderAbilityBinding getSecondaryRiderAbility() {
        return null;
    }

    @Nullable
    public RiderAbilityBinding getTertiaryRiderAbility() {
        return null;
    }

    @Nullable
    public RiderAbilityBinding getAttackRiderAbility() {
        return null;
    }

    /**
     * Get the current melee mode (0=primary, 1=secondary)
     */
    public int getMeleeMode() {
        return this.entityData.get(DATA_MELEE_MODE);
    }

    /**
     * Set the melee mode (0=primary, 1=secondary)
     */
    public void setMeleeMode(int mode) {
        this.entityData.set(DATA_MELEE_MODE, Mth.clamp(mode, 0, 1));
    }

    /**
     * Toggle between primary and secondary melee mode
     */
    public void toggleMeleeMode() {
        setMeleeMode(getMeleeMode() == 0 ? 1 : 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MeleeMode", getMeleeMode());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("MeleeMode")) {
            setMeleeMode(tag.getInt("MeleeMode"));
        }
    }

    public byte buildClientControlState(boolean ascendDown, boolean descendDown, boolean attackDown, boolean primaryDown, boolean secondaryDown, boolean sneakDown) {
        return (byte) -1;
    }

    public record RiderAbilityBinding(String abilityId, Activation activation) {
        public enum Activation {
            PRESS,
            HOLD
        }
    }

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

    protected void copyRiderLook(Player player) {
        if (player == null) {
            return;
        }

        float currentYaw = this.getYRot();
        float targetYaw = player.getYRot();
        float yawDelta = Mth.wrapDegrees(targetYaw - currentYaw);
        float yawBlend = getRiderLockYawBlend();
        float blendedYaw = currentYaw + yawDelta * yawBlend;

        this.setYRot(blendedYaw);
        this.yBodyRotO = this.yBodyRot;
        this.yBodyRot = blendedYaw;
        this.yHeadRotO = this.yHeadRot;
        this.setYHeadRot(blendedYaw);

        float targetPitch = Mth.clamp(player.getXRot(), getRiderLockPitchMin(), getRiderLockPitchMax());
        float blendedPitch = Mth.lerp(getRiderLockPitchBlend(), this.getXRot(), targetPitch);
        this.xRotO = this.getXRot();
        this.setXRot(blendedPitch);
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

    /**
     * Persist the common rideable dragon state to NBT so every dragon saves the same baseline data.
     */
    protected void saveRideableData(CompoundTag tag) {
        tag.putBoolean("Flying", isFlying());
        tag.putBoolean("Takeoff", isTakeoff());
        tag.putBoolean("Hovering", isHovering());
        tag.putBoolean("Landing", isLanding());
        tag.putBoolean("Running", isRunning());
        tag.putBoolean("Accelerating", this.entityData.get(getAcceleratingAccessor()));
        tag.putBoolean("GoingUp", isGoingUp());
        tag.putBoolean("GoingDown", isGoingDown());
        tag.putInt("GroundMoveState", this.entityData.get(getGroundMoveStateAccessor()));
        tag.putInt("FlightMode", this.entityData.get(getFlightModeAccessor()));
        tag.putFloat("RiderForward", this.entityData.get(getRiderForwardAccessor()));
        tag.putFloat("RiderStrafe", this.entityData.get(getRiderStrafeAccessor()));
        tag.putBoolean("IsSitting", this.isOrderedToSit());
        tag.putFloat("SitProgress", this.sitProgress);
    }

    /**
     * Load the common rideable state from NBT. Subclasses can override {@link #applyLoadedFlightState}
     * if they need to push the booleans into custom accessors.
     */
    protected void loadRideableData(CompoundTag tag) {
        boolean savedFlying = tag.getBoolean("Flying");
        boolean savedTakeoff = tag.getBoolean("Takeoff");
        boolean savedHovering = tag.getBoolean("Hovering");
        boolean savedLanding = tag.getBoolean("Landing");
        applyLoadedFlightState(savedFlying, savedTakeoff, savedHovering, savedLanding);

        this.setRunning(tag.getBoolean("Running"));
        this.setAccelerating(tag.getBoolean("Accelerating"));
        this.setGoingUp(savedFlying && tag.getBoolean("GoingUp"));
        this.setGoingDown(savedFlying && tag.getBoolean("GoingDown"));

        int groundState = tag.contains("GroundMoveState") ? tag.getInt("GroundMoveState") : 0;
        this.entityData.set(getGroundMoveStateAccessor(), Mth.clamp(groundState, 0, 2));

        int flightMode = tag.contains("FlightMode") ? tag.getInt("FlightMode") : -1;
        this.entityData.set(getFlightModeAccessor(), savedFlying ? Mth.clamp(flightMode, -1, 3) : -1);

        float riderForward = tag.contains("RiderForward") ? tag.getFloat("RiderForward") : 0f;
        float riderStrafe = tag.contains("RiderStrafe") ? tag.getFloat("RiderStrafe") : 0f;
        this.entityData.set(getRiderForwardAccessor(), riderForward);
        this.entityData.set(getRiderStrafeAccessor(), riderStrafe);

        boolean savedSitting = tag.getBoolean("IsSitting");
        this.setOrderedToSit(savedSitting);
        float savedSitProgress = tag.contains("SitProgress")
                ? tag.getFloat("SitProgress")
                : (savedSitting ? this.maxSitTicks() : 0f);
        this.sitProgress = Mth.clamp(savedSitProgress, 0f, this.maxSitTicks());
        this.prevSitProgress = this.sitProgress;
        this.entityData.set(DATA_SIT_PROGRESS, this.sitProgress);

        if (!level().isClientSide) {
            this.syncAnimState(this.entityData.get(getGroundMoveStateAccessor()),
                    this.entityData.get(getFlightModeAccessor()));
        }
    }

    /**
     * Hook for subclasses to push saved flight booleans into their own accessors.
     * Ground-only dragons can ignore it.
     */
    protected void applyLoadedFlightState(boolean flying, boolean takeoff, boolean hovering, boolean landing) {
        // Default no-op; dragons with dedicated flight data should override.
    }
}
