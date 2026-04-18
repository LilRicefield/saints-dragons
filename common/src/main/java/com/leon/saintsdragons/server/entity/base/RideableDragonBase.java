package com.leon.saintsdragons.server.entity.base;

import com.leon.saintsdragons.server.entity.interfaces.RideableDragon;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.network.MessageDragonRideInput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashSet;
import java.util.Set;

public abstract class RideableDragonBase extends DragonEntity implements RideableDragon, FlyingAnimal {
    private static final Logger LOGGER = LoggerFactory.getLogger(RideableDragonBase.class);
    private static final int MAX_PERSISTED_FLIGHT_MODE = 5;
    private final Set<String> warnedMissingActions = new HashSet<>();

    /**
     * Compatibility padding for modpacks that inject an extra tracked field into an upstream mob class.
     * Keep this unregistered; it reserves one shared rideable tracker id so the real fields below do not
     * collide with that injected parent slot in heavily-modded Fabric packs. PROMINENCE II?? Bomboclaat, i'm too lazy to track, so here we are.
     */
    private static final EntityDataAccessor<Byte> DATA_TRACKER_COMPAT_PADDING =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.BYTE);

    private static final EntityDataAccessor<Integer> DATA_MELEE_MODE =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_RIDER_LOCKED =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_TAKEOFF =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_HOVERING =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.BOOLEAN);

    /**
     * Prominence II injects a parent tracker that lands exactly where our landing flag would normally sit.
     * Keep this unregistered so that slot is skipped before we register DATA_LANDING.
     */
    private static final EntityDataAccessor<Byte> DATA_LANDING_TRACKER_COMPAT_PADDING =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.BYTE);

    public static final EntityDataAccessor<Boolean> DATA_LANDING =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.BOOLEAN);

    /**
     * Second compatibility gap for packs that occupy tracker id 41 in a parent class without reserving it
     * through normal defineId sequencing. This keeps the shared movement-state block above that slot. TWO. JUST CAUSE.
     */
    private static final EntityDataAccessor<Byte> DATA_MOVEMENT_TRACKER_COMPAT_PADDING =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.BYTE);
    public static final EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_FLIGHT_MODE =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> DATA_RIDER_FORWARD =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_RIDER_STRAFE =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> DATA_GOING_UP =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_GOING_DOWN =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_ACCELERATING =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.BOOLEAN);

    private int riderControlLockTicks = 0;
    private boolean riderWasAirborneForLanding = false;
    private int riderAirborneTicksForLanding = 0;

    protected RideableDragonBase(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MELEE_MODE, 0); // Default to primary melee (mode 0)
        this.entityData.define(DATA_RIDER_LOCKED, false);
        this.entityData.define(DATA_FLYING, false);
        this.entityData.define(DATA_TAKEOFF, false);
        this.entityData.define(DATA_HOVERING, false);
        this.entityData.define(DATA_LANDING, false);
        this.entityData.define(DATA_GROUND_MOVE_STATE, 0);
        this.entityData.define(DATA_FLIGHT_MODE, -1);
        this.entityData.define(DATA_RIDER_FORWARD, 0.0F);
        this.entityData.define(DATA_RIDER_STRAFE, 0.0F);
        this.entityData.define(DATA_GOING_UP, false);
        this.entityData.define(DATA_GOING_DOWN, false);
        this.entityData.define(DATA_ACCELERATING, false);
        defineRideableDragonData();
    }

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
        handleRiderAction(player, msg.action(), msg.abilityName(), locked);
        applyRiderMovementInput(player, msg.forward(), msg.strafe(), msg.yaw(), locked);
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

        if (handleCustomRiderAction(player, action, abilityName, locked)) {
            return;
        }

        if (!supportsRiderAction(action)) {
            warnMissingAction(action.name().toLowerCase());
            return;
        }
        switch (action) {
            case TAKEOFF_REQUEST -> { if (!locked) onRiderTakeoffRequest(player); }
            case ACCELERATE -> { if (!locked) onRiderAccelerationStart(player); }
            case STOP_ACCELERATE -> onRiderAccelerationStop(player);
            case ABILITY_USE -> { if (!locked) onRiderAbilityUse(player, abilityName); }
            case ABILITY_STOP -> { if (!locked) onRiderAbilityStop(player, abilityName); }
            case TOGGLE_MELEE -> { if (!locked) onRiderToggleMelee(player); }
            case DOUBLE_TAP_A -> { if (!locked) onRiderDodge(player, true); }
            case DOUBLE_TAP_D -> { if (!locked) onRiderDodge(player, false); }
            case OPEN_INVENTORY -> { if (!locked) onRiderOpenInventory(player); }
            default -> { }
        }
    }

    /**
     * Override this to handle dragon-specific rider actions.
     * Base actions (takeoff, accelerate, melee toggle, dodge) are handled automatically.
     *
     * @param player The riding player
     * @param action The action to handle
     * @param abilityName The ability name (for ABILITY_USE/STOP actions)
     * @param locked Whether rider controls are currently locked
     * @return true if the action was handled, false to let base handler try
     */
    protected boolean handleCustomRiderAction(ServerPlayer player, DragonRiderAction action,
                                              String abilityName, boolean locked) {
        return false; // Default: no custom actions
    }

    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case TAKEOFF_REQUEST, ACCELERATE, STOP_ACCELERATE, TOGGLE_MELEE -> true;
            case ABILITY_USE, ABILITY_STOP,
                 DOUBLE_TAP_A, DOUBLE_TAP_D, DOUBLE_TAP_W, DOUBLE_TAP_S,
                 TAUNT, TOGGLE_PITCH_MODE, OPEN_INVENTORY -> false;
            default -> true;
        };
    }

    private void warnMissingAction(String action) {
        if (warnedMissingActions.add(action)) {
            LOGGER.warn("{} does not support rider action '{}'", this, action);
        }
    }

    /**
     * Called when rider requests a dodge. Override in subclasses to implement dodge mechanics.
     * @param player The player riding
     * @param isLeft True if dodging left, false if dodging right
     */
    protected void onRiderDodge(Player player, boolean isLeft) {
        if (!supportsRiderAction(DragonRiderAction.DOUBLE_TAP_A)
                && !supportsRiderAction(DragonRiderAction.DOUBLE_TAP_D)) {
            warnMissingAction("double_tap_a/d");
            return;
        }
    }

    /**
     * Called when rider requests a bulldoze toggle. Override in subclasses to implement bulldoze mechanics.
     * @param player The player riding
     */
    protected void onRiderBulldoze(Player player) {
        warnMissingAction("bulldoze");
        return;
    }

    /**
     * Called when rider requests a dash forward. Override in subclasses to implement dash mechanics.
     * @param player The player riding
     */
    protected void onRiderDash(Player player) {
        if (!supportsRiderAction(DragonRiderAction.DOUBLE_TAP_W)) {
            warnMissingAction("double_tap_w");
            return;
        }
    }

    /**
     * Called when rider requests a backward dodge. Override in subclasses to implement backward dodge mechanics.
     * @param player The player riding
     */
    protected void onRiderBackwardDodge(Player player) {
        if (!supportsRiderAction(DragonRiderAction.DOUBLE_TAP_S)) {
            warnMissingAction("double_tap_s");
            return;
        }
    }

    protected void onRiderToggleMelee(Player player) {
        toggleMeleeMode();
    }

    /**
     * Override this to specify if the dragon has a secondary melee attack.
     * Defaults to true - dragons without secondary melee should override and return false.
     */
    public boolean hasSecondaryMelee() {
        return true;
    }

    /**
     * Override this to specify if the dragon can take off (i.e., is flight-capable).
     * Defaults to true - ground-only/aquatic dragons should override and return false.
     */
    public boolean canTakeoff() {
        return !this.isBaby();
    }

    protected void onRiderTakeoffRequest(Player player) {
        if (!supportsRiderAction(DragonRiderAction.TAKEOFF_REQUEST)) {
            warnMissingAction("takeoff_request");
            return;
        }
    }

    protected void onRiderAccelerationStart(Player player) {
        setAccelerating(true);
    }

    protected void onRiderAccelerationStop(Player player) {
        setAccelerating(false);
    }

    protected void onRiderAbilityUse(Player player, String abilityName) {
        if (!supportsRiderAction(DragonRiderAction.ABILITY_USE)) {
            warnMissingAction("ability_use");
            return;
        }
    }

    protected void onRiderAbilityStop(Player player, String abilityName) {
        if (!supportsRiderAction(DragonRiderAction.ABILITY_STOP)) {
            warnMissingAction("ability_stop");
            return;
        }
    }

    protected void onRiderOpenInventory(Player player) {
        if (!supportsRiderAction(DragonRiderAction.OPEN_INVENTORY)) {
            warnMissingAction("open_inventory");
        }
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

    public static final int MELEE_MODE_PRIMARY = 0;
    public static final int MELEE_MODE_SECONDARY = 1;
    public static final int MELEE_MODE_MINING = 2;

    /**
     * Get the current rider melee mode (0=primary, 1=secondary, 2=mining)
     */
    public int getMeleeMode() {
        return this.entityData.get(DATA_MELEE_MODE);
    }

    /**
     * Set the rider melee mode (0=primary, 1=secondary, 2=mining)
     */
    public void setMeleeMode(int mode) {
        this.entityData.set(DATA_MELEE_MODE, Mth.clamp(mode, MELEE_MODE_PRIMARY, MELEE_MODE_MINING));
    }

    /**
     * Returns the effective combat melee mode. Mining mode falls back to primary so
     * rider utility state never leaks into AI or generic combat selection.
     */
    public int getCombatMeleeMode() {
        return getMeleeMode() == MELEE_MODE_SECONDARY ? MELEE_MODE_SECONDARY : MELEE_MODE_PRIMARY;
    }

    public boolean isMiningMode() {
        return getMeleeMode() == MELEE_MODE_MINING;
    }

    public int getNextMeleeMode() {
        if (hasSecondaryMelee()) {
            return (getMeleeMode() + 1) % 3;
        }
        return getMeleeMode() == MELEE_MODE_PRIMARY ? MELEE_MODE_MINING : MELEE_MODE_PRIMARY;
    }

    /**
     * Toggle between available rider modes.
     */
    public void toggleMeleeMode() {
        setMeleeMode(getNextMeleeMode());
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MeleeMode", getMeleeMode());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
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

    protected EntityDataAccessor<Float> getRiderForwardAccessor() {
        return DATA_RIDER_FORWARD;
    }

    protected EntityDataAccessor<Float> getRiderStrafeAccessor() {
        return DATA_RIDER_STRAFE;
    }

    protected EntityDataAccessor<Integer> getGroundMoveStateAccessor() {
        return DATA_GROUND_MOVE_STATE;
    }

    protected EntityDataAccessor<Integer> getFlightModeAccessor() {
        return DATA_FLIGHT_MODE;
    }

    protected EntityDataAccessor<Boolean> getGoingUpAccessor() {
        return DATA_GOING_UP;
    }

    protected EntityDataAccessor<Boolean> getGoingDownAccessor() {
        return DATA_GOING_DOWN;
    }

    protected EntityDataAccessor<Boolean> getAcceleratingAccessor() {
        return DATA_ACCELERATING;
    }

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

    /**
     * Set ground move state from AI goals (0=idle, 1=walking, 2=running).
     * This bypasses velocity-based detection and directly sets the animation state.
     */
    public void setGroundMoveStateFromAI(int state) {
        int clampedState = Mth.clamp(state, 0, 2);
        if (this.entityData.get(getGroundMoveStateAccessor()) != clampedState) {
            this.entityData.set(getGroundMoveStateAccessor(), clampedState);
            this.syncAnimState(clampedState, getFlightMode());
        }
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

    // ===== SIT TRANSITION INTERFACE =====

    /**
     * Check if dragon is currently in a sit transition animation (sitting down or standing up).
     * Override this in dragons that have sit animations with specific transition timing.
     * Goals should avoid starting while transitions are active.
     */
    public boolean isInSitTransition() {
        return false; // Default: no transitions
    }

    /**
     * Check if dragon is currently playing the sit-down animation.
     * Override this in dragons that have sit-down animations.
     */
    public boolean isSittingDownAnimation() {
        return false; // Default: no sit-down animation
    }

    /**
     * Check if dragon is currently playing the stand-up animation.
     * Override this in dragons that have stand-up animations.
     */
    public boolean isStandingUpAnimation() {
        return false; // Default: no stand-up animation
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

    protected void copyRiderYaw(Player player) {
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
                // 0=idle, 1=walk, 2=run (GeckoLib threshold at 0.000001 handles actual animation)
                if (velSqr > 0.02) initialGroundState = 2;
                else if (velSqr > 0.0008) initialGroundState = 1;
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
                if (velSqr > 0.02) currentGroundState = 2;
                else if (velSqr > 0.0008) currentGroundState = 1;
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
            // Apply simple threshold to filter noise
            this.setLastRiderForward(Math.abs(fwd) > 0.02f ? fwd : 0f);
            this.setLastRiderStrafe(Math.abs(str) > 0.02f ? str : 0f);
        }

        return input;
    }

    @Override
    public void removePassenger(@NotNull Entity passenger) {
        // CRITICAL: Always clear control lock when passenger is removed to prevent stuck state
        if (passenger == getControllingPassenger()) {
            clearRiderControlLock();
        }
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

                // Check if rider has significant input (threshold 0.05)
                if (Math.abs(fwd) + Math.abs(str) > 0.05f) {
                    moveState = this.isAccelerating() ? 2 : 1;
                } else {
                    // Fallback while ridden: use actual velocity so observers still see walk/run
                    double speedSqr = getDeltaMovement().horizontalDistanceSqr();
                    if (speedSqr > 0.08) moveState = 2;
                    else if (speedSqr > 0.005) moveState = 1;
                }
            } else {
                // Use horizontal velocity for AI classification
                double velSqr = this.getDeltaMovement().horizontalDistanceSqr();
                if (velSqr > 0.02) moveState = 2;
                else if (velSqr > 0.0008) moveState = 1;
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
     * Track whether a ridden dragon has been airborne since the last grounded/reset state.
     * Dragons can call this once per tick before touchdown checks.
     */
    protected void trackRiderAirborneForLanding() {
        boolean airborneWhileInFlightState = isVehicle()
                && !onGround()
                && (isFlying() || isLanding() || isHovering() || isTakeoff());

        if (airborneWhileInFlightState) {
            riderWasAirborneForLanding = true;
            riderAirborneTicksForLanding++;
            return;
        }
        if (!isVehicle()) {
            riderWasAirborneForLanding = false;
            riderAirborneTicksForLanding = 0;
        }
    }

    /**
     * Consume a ridden air-to-ground touchdown event.
     * Returns true exactly when the dragon transitions from airborne to grounded while ridden.
     */
    protected boolean consumeRiderTouchdownFromAir(double maxUpwardVelocity) {
        boolean touchdown = onGround()
                && isVehicle()
                && riderWasAirborneForLanding
                && riderAirborneTicksForLanding >= 2
                && !isTakeoff()
                && getDeltaMovement().y <= maxUpwardVelocity;

        if (touchdown || onGround() || !isVehicle()) {
            riderWasAirborneForLanding = false;
            riderAirborneTicksForLanding = 0;
        }
        return touchdown;
    }

    /**
     * Shared collision-based altitude probe for ridden landing blend logic.
     * This is opt-in; only dragons that call it will use it.
     *
     * @param maxDropBlocks how far below the dragon's feet to scan
     * @param blockOnFluids when true, returns POSITIVE_INFINITY if fluid is found in sampled columns
     * @return altitude from dragon feet to nearest collision top, or POSITIVE_INFINITY if no usable terrain
     */
    protected double getAltitudeAboveCollisionTerrain(int maxDropBlocks, boolean blockOnFluids) {
        BlockPos origin = this.blockPosition();
        if (!level().hasChunkAt(origin)) {
            return Double.POSITIVE_INFINITY;
        }

        final AABB box = this.getBoundingBox();
        final int minBuildY = level().getMinBuildHeight();
        final double[] sampleX = {this.getX(), box.minX + 0.25D, box.maxX - 0.25D};
        final double[] sampleZ = {this.getZ(), box.minZ + 0.25D, box.maxZ - 0.25D};

        double bestAltitude = Double.POSITIVE_INFINITY;
        boolean foundGround = false;

        for (double sx : sampleX) {
            for (double sz : sampleZ) {
                int x = Mth.floor(sx);
                int z = Mth.floor(sz);
                int startY = Mth.floor(box.minY);
                int stopY = Math.max(minBuildY, startY - Math.max(1, maxDropBlocks));

                for (int y = startY; y >= stopY; y--) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    if (!level().hasChunkAt(checkPos)) {
                        continue;
                    }

                    BlockState state = level().getBlockState(checkPos);
                    if (blockOnFluids && !state.getFluidState().isEmpty()) {
                        return Double.POSITIVE_INFINITY;
                    }

                    VoxelShape shape = state.getCollisionShape(level(), checkPos);
                    if (shape.isEmpty()) {
                        continue;
                    }

                    double topY = y + shape.max(Direction.Axis.Y);
                    double altitude = box.minY - topY;
                    if (altitude < bestAltitude) {
                        bestAltitude = altitude;
                    }
                    foundGround = true;
                    break;
                }
            }
        }

        if (!foundGround) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.max(0.0D, bestAltitude);
    }

    /**
     * Returns true when collision terrain is close enough for a landing animation handoff.
     */
    public boolean isNearLandingTerrain(double maxAltitude) {
        double altitude = getAltitudeAboveCollisionTerrain(24, true);
        return altitude != Double.POSITIVE_INFINITY && altitude >= -0.25D && altitude <= maxAltitude;
    }

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
     * Force the sit progress to a specific value and sync it. Intended for platform handlers that
     * need to restore animation state (e.g., after reconnect).
     */
    public void forceSitProgress(float value) {
        super.forceSitProgress(value);
    }

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
        saveSitProgress(tag);
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

        // Trigger-only transient states do not replay cleanly across save/load.
        // Persist the broader flying/hovering posture, but drop mid-transition flags.
        if (savedTakeoff || savedLanding) {
            savedTakeoff = false;
            savedLanding = false;
        }
        applyLoadedFlightState(savedFlying, savedTakeoff, savedHovering, savedLanding);

        this.setRunning(tag.getBoolean("Running"));
        this.setAccelerating(tag.getBoolean("Accelerating"));
        this.setGoingUp(savedFlying && tag.getBoolean("GoingUp"));
        this.setGoingDown(savedFlying && tag.getBoolean("GoingDown"));

        int groundState = tag.contains("GroundMoveState") ? tag.getInt("GroundMoveState") : 0;
        this.entityData.set(getGroundMoveStateAccessor(), Mth.clamp(groundState, 0, 2));

        int flightMode = tag.contains("FlightMode") ? tag.getInt("FlightMode") : -1;
        this.entityData.set(getFlightModeAccessor(), savedFlying ? Mth.clamp(flightMode, -1, MAX_PERSISTED_FLIGHT_MODE) : -1);

        float riderForward = tag.contains("RiderForward") ? tag.getFloat("RiderForward") : 0f;
        float riderStrafe = tag.contains("RiderStrafe") ? tag.getFloat("RiderStrafe") : 0f;
        this.entityData.set(getRiderForwardAccessor(), riderForward);
        this.entityData.set(getRiderStrafeAccessor(), riderStrafe);

        boolean savedSitting = tag.getBoolean("IsSitting");
        this.setOrderedToSit(savedSitting);
        loadSitProgress(tag, savedSitting);

        if (!level().isClientSide) {
            this.syncAnimState(this.entityData.get(getGroundMoveStateAccessor()),
                    this.entityData.get(getFlightModeAccessor()));
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide && this.tickCount == 1) {
            initializeAnimationState();
            resetAnimationState();
        }
    }

    /**
     * Hook for subclasses to push saved flight booleans into their own accessors.
     * Ground-only dragons can ignore it.
     */
    protected void applyLoadedFlightState(boolean flying, boolean takeoff, boolean hovering, boolean landing) {
        // Default no-op; dragons with dedicated flight data should override.
    }

    // ===== RIDER CONTROL LOCK SYSTEM =====

    /**
     * Locks rider controls for the specified number of ticks.
     * Used during animations like landed, ultimate abilities, etc.
     * @param ticks Number of ticks to lock controls (20 ticks = 1 second)
     */
    public void lockRiderControls(int ticks) {
        riderControlLockTicks = Math.max(riderControlLockTicks, Math.max(0, ticks));
        this.entityData.set(DATA_RIDER_LOCKED, true);
    }

    /**
     * Checks if rider controls are currently locked.
     * Client-side checks synced entity data, server-side checks tick counter.
     * @return true if controls are locked
     */
    public boolean areRiderControlsLocked() {
        return level().isClientSide ? this.entityData.get(DATA_RIDER_LOCKED) : riderControlLockTicks > 0;
    }

    /**
     * Clears the rider control lock immediately.
     * Called when passenger dismounts to prevent stuck state.
     */
    public void clearRiderControlLock() {
        if (riderControlLockTicks > 0 || this.entityData.get(DATA_RIDER_LOCKED)) {
            riderControlLockTicks = 0;
            this.entityData.set(DATA_RIDER_LOCKED, false);
        }
    }

    /**
     * Ticks down the rider control lock timer.
     * Must be called in the dragon's tick/aiStep method.
     */
    protected void tickRiderControlLock() {
        if (!level().isClientSide && riderControlLockTicks > 0) {
            riderControlLockTicks--;
            if (riderControlLockTicks == 0) {
                this.entityData.set(DATA_RIDER_LOCKED, false);
            }
        }
    }

}
