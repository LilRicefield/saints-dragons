package com.leon.saintsdragons.server.entity.base;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class RideableDragonBase extends DragonEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger(RideableDragonBase.class);
    private static final int MAX_PERSISTED_FLIGHT_MODE = 5;
    private final Set<String> warnedMissingActions = new HashSet<>();
    protected final Map<String, Vec3> clientLocatorCache = new ConcurrentHashMap<>();
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
    public static final EntityDataAccessor<Boolean> DATA_LANDING =
            SynchedEntityData.defineId(RideableDragonBase.class, EntityDataSerializers.BOOLEAN);
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
    private int riderFlexCooldownTicks = 0;
    private boolean wasVehicleLastTick = false;
    private boolean riderWasAirborneForLanding = false;
    private int riderAirborneTicksForLanding = 0;
    private double riderFlightThrottle = 0.0D;

    protected RideableDragonBase(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MELEE_MODE, 0);
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

    protected void defineRideableDragonData() {
    }

    public void setClientLocatorPosition(String name, Vec3 pos) {
        if (name == null || pos == null) {
            return;
        }
        this.clientLocatorCache.put(name, pos);
    }

    @Override
    public Vec3 getClientLocatorPosition(String name) {
        return name == null ? null : this.clientLocatorCache.get(name);
    }

    public boolean canBeControlledBy(Player player) {
        if (player == null) {
            return false;
        }
        if (this.isTame()) {
            return this.isOwnedBy(player);
        }
        return player.isCreative() || player.isSpectator();
    }

    public boolean isRiddenByOwner() {
        if (!isTame() || !isVehicle()) {
            return false;
        }
        if (!(getControllingPassenger() instanceof Player player)) {
            return false;
        }
        return isOwnedBy(player);
    }

    public void handleRiderNetworkInput(ServerPlayer player, MessageDragonRideInput msg) {
        boolean locked = isRiderInputLocked(player);
        applyRiderVerticalInput(player, msg.goingUp(), msg.goingDown(), locked);
        handleRiderAction(player, msg.action(), msg.abilityName(), locked);
        applyRiderMovementInput(player, msg.forward(), msg.strafe(), locked);
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

    protected void applyRiderMovementInput(Player player, float forward, float strafe, boolean locked) {
        float clampedForward = locked ? 0f : applyInputDeadzone(forward);
        float clampedStrafe = locked ? 0f : applyInputDeadzone(strafe);
        setLastRiderForward(clampedForward);
        setLastRiderStrafe(clampedStrafe);
        updateRiderGroundMoveState(clampedForward, clampedStrafe);
    }

    protected void updateRiderGroundMoveState(float forward, float strafe) {
        if (!shouldUpdateRiderGroundMoveState()) {
            return;
        }
        int moveState = getRiderGroundMoveState(forward, strafe);
        setGroundMoveStateFromRider(moveState);
        setRunning(moveState == 2);
    }

    protected boolean shouldUpdateRiderGroundMoveState() {
        return !isAerial();
    }

    protected int getRiderGroundMoveState(float forward, float strafe) {
        if (Math.abs(forward) + Math.abs(strafe) <= 0.05F) {
            return 0;
        }
        return isAccelerating() && canRiderGroundRun() ? 2 : 1;
    }

    protected boolean canRiderGroundRun() {
        return true;
    }

    public void setGroundMoveStateFromRider(int state) {
        setGroundMoveStateFromAI(state);
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
            case FLEX -> { if (!locked) onRiderFlex(player); }
            case START_PITCH_MODE -> { if (!locked) setRiderPitchKeyMode(true); }
            case STOP_PITCH_MODE -> setRiderPitchKeyMode(false);
            case DOUBLE_TAP_A -> { if (!locked) onRiderDodge(player, true); }
            case DOUBLE_TAP_D -> { if (!locked) onRiderDodge(player, false); }
            case OPEN_INVENTORY -> onRiderOpenInventory(player);
            default -> { }
        }
    }

    protected boolean handleCustomRiderAction(ServerPlayer player, DragonRiderAction action,
                                              String abilityName, boolean locked) {
        return false;
    }

    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case TAKEOFF_REQUEST, ACCELERATE, STOP_ACCELERATE, TOGGLE_MELEE, START_PITCH_MODE, STOP_PITCH_MODE -> true;
            case ABILITY_USE, ABILITY_STOP,
                 DOUBLE_TAP_A, DOUBLE_TAP_D, DOUBLE_TAP_W, DOUBLE_TAP_S,
                 OPEN_INVENTORY -> false;
            case FLEX -> hasRiderFlex();
            default -> true;
        };
    }

    protected record RiderFlexSpec(int controlLockTicks, int cooldownTicks) {
        public RiderFlexSpec {
        }
    }

    protected boolean hasRiderFlex() {
        return getRiderFlexSpec() != null;
    }

    @Nullable
    protected RiderFlexSpec getRiderFlexSpec() {
        return null;
    }

    protected boolean canRiderFlex(ServerPlayer player, RiderFlexSpec spec) {
        return spec != null && riderFlexCooldownTicks <= 0;
    }

    protected void onRiderFlex(ServerPlayer player) {
        RiderFlexSpec spec = getRiderFlexSpec();
        if (!canRiderFlex(player, spec)) {
            return;
        }
        if (spec.controlLockTicks() > 0) {
            lockRiderControls(spec.controlLockTicks());
        }
        riderFlexCooldownTicks = Math.max(0, spec.cooldownTicks());
        playRiderFlex(player, spec);
    }

    protected void playRiderFlex(ServerPlayer player, RiderFlexSpec spec) {
    }

    public void setRiderPitchKeyMode(boolean enabled) {
    }

    public boolean isRiderPitchKeyMode() {
        return false;
    }

    private void warnMissingAction(String action) {
        if (warnedMissingActions.add(action)) {
            LOGGER.warn("{} does not support rider action '{}'", this, action);
        }
    }

    protected void onRiderDodge(Player player, boolean isLeft) {
        if (!supportsRiderAction(DragonRiderAction.DOUBLE_TAP_A)
                && !supportsRiderAction(DragonRiderAction.DOUBLE_TAP_D)) {
            warnMissingAction("double_tap_a/d");
        }
    }

    protected void onRiderBulldoze(Player player) {
        warnMissingAction("bulldoze");
    }

    protected void onRiderDash(Player player) {
        if (!supportsRiderAction(DragonRiderAction.DOUBLE_TAP_W)) {
            warnMissingAction("double_tap_w");
        }
    }
    protected void onRiderBackwardDodge(Player player) {
        if (!supportsRiderAction(DragonRiderAction.DOUBLE_TAP_S)) {
            warnMissingAction("double_tap_s");
        }
    }
    protected void onRiderToggleMelee(Player player) {

        if (!hasSecondaryMelee()) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("saintsdragons.message.no_secondary_melee"),
                    true
                );
            }
            return;
        }

        toggleMeleeMode();
    }

    public boolean hasSecondaryMelee() {
        return true;
    }

    public boolean canTakeoff() {
        return false;
    }

    protected void onRiderTakeoffRequest(Player player) {
        if (!supportsRiderAction(DragonRiderAction.TAKEOFF_REQUEST)) {
            warnMissingAction("takeoff_request");
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
        if (abilityName != null && !abilityName.isEmpty()) {
            useRidingAbility(abilityName);
        }
    }

    protected void onRiderAbilityStop(Player player, String abilityName) {
        if (!supportsRiderAction(DragonRiderAction.ABILITY_STOP)) {
            warnMissingAction("ability_stop");
            return;
        }
        if (abilityName != null && !abilityName.isEmpty()) {
            if (tryReleaseHeldRidingAbility(abilityName)) {
                return;
            }
            forceEndActiveAbility();
        }
    }

    protected boolean tryReleaseHeldRidingAbility(String abilityName) {
        return false;
    }

    public void useRidingAbility(String abilityName) {
        DragonAbilityType<?, ?> abilityType = resolveRidingAbilityType(abilityName);
        if (abilityType == null || !canUseResolvedRidingAbility(abilityType)) {
            return;
        }
        combatManager.tryUseAbility(abilityType);
    }

    @Nullable
    protected DragonAbilityType<?, ?> resolveRidingAbilityType(String abilityName) {
        if (abilityName == null || abilityName.isEmpty()) {
            return null;
        }
        return AbilityRegistry.get(abilityName);
    }

    protected boolean canUseResolvedRidingAbility(DragonAbilityType<?, ?> abilityType) {
        Entity rider = this.getControllingPassenger();
        if (!(rider instanceof LivingEntity)) {
            return false;
        }
        if (this.isTame() && rider instanceof Player player && !this.isOwnedBy(player)) {
            return false;
        }
        return isRidingAbilityAllowed(abilityType);
    }

    protected boolean isRidingAbilityAllowed(DragonAbilityType<?, ?> abilityType) {
        return true;
    }

    protected void onRiderOpenInventory(Player player) {
        if (!supportsRiderAction(DragonRiderAction.OPEN_INVENTORY)) {
            warnMissingAction("open_inventory");
        }
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

    public int getMeleeMode() {
        return this.entityData.get(DATA_MELEE_MODE);
    }

    public void setMeleeMode(int mode) {
        this.entityData.set(DATA_MELEE_MODE, Mth.clamp(mode, 0, 1));
    }

    public void toggleMeleeMode() {
        setMeleeMode(getMeleeMode() == 0 ? 1 : 0);
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

    public record RiderAbilityBinding(String abilityId, Activation activation) {
        public enum Activation {
            PRESS,
            HOLD
        }
    }

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

    public void setLastRiderForward(float forward) {
        this.entityData.set(getRiderForwardAccessor(), forward);
    }

    public void setLastRiderStrafe(float strafe) {
        this.entityData.set(getRiderStrafeAccessor(), strafe);
    }

    public float getLastRiderForward() {
        return this.entityData.get(getRiderForwardAccessor());
    }

    public float getLastRiderStrafe() {
        return this.entityData.get(getRiderStrafeAccessor());
    }

    public int getGroundMoveState() {
        return this.entityData.get(getGroundMoveStateAccessor());
    }

    public void setGroundMoveStateFromAI(int state) {
        int clampedState = Mth.clamp(state, 0, 2);
        if (this.entityData.get(getGroundMoveStateAccessor()) != clampedState) {
            this.entityData.set(getGroundMoveStateAccessor(), clampedState);
            this.syncAnimState(clampedState, getFlightMode());
        }
    }

    public int getSyncedFlightMode() {
        return this.entityData.get(getFlightModeAccessor());
    }

    public int getEffectiveGroundState() {
        return this.entityData.get(getGroundMoveStateAccessor());
    }

    public boolean shouldUseRunAnimation() {
        LivingEntity target = getTarget();
        return target != null && target.isAlive();
    }

    public int getMovementState() {
        int groundState = getGroundMoveState();
        if (isAerial()) {
            return 0;
        }
        if (isVehicle() || groundState > 0) {
            return groundState;
        }
        if (getNavigation().isInProgress() && getNavigation().getPath() != null) {
            return shouldUseRunAnimation() ? 2 : 1;
        }
        return 0;
    }

    public boolean isInSitTransition() {
        return false;
    }

    public boolean isSittingDownAnimation() {
        return false;
    }

    public boolean isStandingUpAnimation() {
        return false;
    }

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

    public boolean isAccelerating() {
        return this.entityData.get(getAcceleratingAccessor());
    }

    public void setAccelerating(boolean accelerating) {
        this.entityData.set(getAcceleratingAccessor(), accelerating);
    }

    public double getRiderFlightThrottle() {
        return riderFlightThrottle;
    }

    public void setRiderFlightThrottle(double throttle) {
        riderFlightThrottle = Math.max(0.0D, throttle);
    }

    public void resetRiderFlightThrottle() {
        riderFlightThrottle = 0.0D;
    }

    protected void copyRiderYaw(Player player) {
        if (player == null) {
            return;
        }

        float currentYaw = this.getYRot();
        float yawDelta = Mth.wrapDegrees(player.getYRot() - currentYaw);
        float blendedYaw = currentYaw + yawDelta * 0.18F;

        this.setYRot(blendedYaw);
        this.yBodyRotO = this.yBodyRot;
        this.yBodyRot = blendedYaw;
        this.yHeadRotO = this.yHeadRot;
        this.setYHeadRot(blendedYaw);
    }

    @Override
    public void syncAnimState(int groundState, int flightMode) {
    }

    public void initializeAnimationState() {
        if (!level().isClientSide) {
            int initialGroundState = 0;
            int initialFlightMode = -1;
            if (isFlying()) {
                initialFlightMode = getFlightMode();
            }
            this.entityData.set(getGroundMoveStateAccessor(), initialGroundState);
            this.entityData.set(getFlightModeAccessor(), initialFlightMode);
            afterInitializeAnimationState();
        }
    }

    protected void afterInitializeAnimationState() {
    }

    public void resetAnimationState() {
        if (!level().isClientSide) {
            int currentGroundState = 0;
            int currentFlightMode = getFlightMode();
            this.entityData.set(getGroundMoveStateAccessor(), currentGroundState);
            this.entityData.set(getFlightModeAccessor(), currentFlightMode);
            this.syncAnimState(currentGroundState, currentFlightMode);
        }
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        Vec3 input = super.getRiddenInput(player, deltaIn);
        if (!level().isClientSide && !isFlying()) {
            float fwd = (float) Mth.clamp(input.z, -1.0, 1.0);
            float str = (float) Mth.clamp(input.x, -1.0, 1.0);
            this.setLastRiderForward(Math.abs(fwd) > 0.02f ? fwd : 0f);
            this.setLastRiderStrafe(Math.abs(str) > 0.02f ? str : 0f);
        }

        return input;
    }

    @Override
    public void removePassenger(@NotNull Entity passenger) {
        if (passenger == getControllingPassenger()) {
            clearRiderControlLock();
            setRiderPitchKeyMode(false);
        }
        super.removePassenger(passenger);
        if (!this.level().isClientSide) {
            this.setAccelerating(false);
            this.setRunning(false);
            this.setLastRiderForward(0f);
            this.setLastRiderStrafe(0f);
            this.entityData.set(getGroundMoveStateAccessor(), 0);
            this.syncAnimState(0, getSyncedFlightMode());
        }
    }


    public void tickAnimationStates() {
        int moveState = this.entityData.get(getGroundMoveStateAccessor());
        if (!isAerial() && getControllingPassenger() != null) {
            float fwd = this.entityData.get(getRiderForwardAccessor());
            float str = this.entityData.get(getRiderStrafeAccessor());
            if (Math.abs(fwd) + Math.abs(str) > 0.05f) {
                moveState = this.isAccelerating() ? 2 : 1;
            } else {
                moveState = 0;
            }
        }
        int flightMode = getFlightMode();
        boolean groundStateChanged = this.entityData.get(getGroundMoveStateAccessor()) != moveState;
        boolean flightModeChanged = this.entityData.get(getFlightModeAccessor()) != flightMode;

        if (groundStateChanged) {
            this.entityData.set(getGroundMoveStateAccessor(), moveState);
        }

        if (flightModeChanged) {
            this.entityData.set(getFlightModeAccessor(), flightMode);
        }
        if (groundStateChanged || flightModeChanged) {
            this.syncAnimState(moveState, flightMode);
        }
    }


    protected int getFlightMode() {
        return -1;
    }

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

    public boolean isNearLandingTerrain(double maxAltitude) {
        double altitude = getAltitudeAboveCollisionTerrain(24, true);
        return altitude != Double.POSITIVE_INFINITY && altitude >= -0.25D && altitude <= maxAltitude;
    }
    @Override
    public final boolean isFlying() {
        return isDragonFlying();
    }

    protected boolean isDragonFlying() {
        return false;
    }

    public boolean isTakeoff() {
        return false;
    }

    public boolean isLanding() {
        return false;
    }

    public boolean isHovering() {
        return false;
    }

    @Override
    public boolean isWalking() {
        return !isFlying() && getEffectiveGroundState() == 1;
    }

    @Override
    public boolean isRunning() {
        return !isFlying() && getEffectiveGroundState() == 2;
    }

    public void setRunning(boolean running) {
    }

    @Override
    protected void onSleepFreezeTick() {
        super.onSleepFreezeTick();
        this.setRunning(false);
        this.setGroundMoveStateFromAI(0);
    }

    public void prepareForMounting() {
        if (level().isClientSide) {
            return;
        }

        clearSittingForMounting();
        if (getCommand() == 1) {
            setCommand(0);
        }
        clearSitProgress();
        setTarget(null);
        if (getNavigation().getPath() != null) {
            getNavigation().stop();
        }
        afterPrepareForMounting();
    }

    protected void clearSittingForMounting() {
        setOrderedToSit(false);
    }

    protected void afterPrepareForMounting() {
    }

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

    protected void loadRideableData(CompoundTag tag) {
        boolean savedFlying = tag.getBoolean("Flying");
        boolean savedTakeoff = tag.getBoolean("Takeoff");
        boolean savedHovering = tag.getBoolean("Hovering");
        boolean savedLanding = tag.getBoolean("Landing");
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
        tickMountedState();
        tickRiderFlexCooldown();
    }

    public void restoreMountedAnimationStateAfterLogin() {
        if (level().isClientSide) {
            return;
        }
        setPersistenceRequired();
        initializeAnimationState();
        resetAnimationState();
        tickAnimationStates();
        syncAnimState(getGroundMoveState(), getSyncedFlightMode());
        setAccelerating(false);
        setLastRiderForward(0f);
        setLastRiderStrafe(0f);

        if (isOrderedToSit()) {
            forceSitProgress(maxSitTicks());
        }
    }

    private void tickRiderFlexCooldown() {
        if (!level().isClientSide && riderFlexCooldownTicks > 0) {
            riderFlexCooldownTicks--;
        }
    }

    private void tickMountedState() {
        if (level().isClientSide) {
            return;
        }

        boolean mounted = isVehicle();
        if (mounted && !wasVehicleLastTick) {
            onMountedStateStarted();
        } else if (!mounted && wasVehicleLastTick) {
            onMountedStateStopped();
        }
        wasVehicleLastTick = mounted;
    }

    protected void onMountedStateStarted() {
        clearStateForMountedRider();
        clearLocalSitTransitionForMount();
        if (isSleeping() || isSleepingEntering() || isSleepingExiting()) {
            wakeUpImmediately();
            suppressSleep(300);
        }
    }

    protected void onMountedStateStopped() {
        this.entityData.set(getRiderForwardAccessor(), 0f);
        this.entityData.set(getRiderStrafeAccessor(), 0f);
        this.entityData.set(getGroundMoveStateAccessor(), 0);
        resetRiderFlightThrottle();
        syncAnimState(0, getFlightMode());
        clearLocalSitTransitionForMount();
    }

    protected void clearLocalSitTransitionForMount() {
    }

    protected void clearStateForMountedRider() {
        clearSittingForMounting();
        if (getCommand() == 1) {
            setCommand(0);
        }
        clearSitProgress();
        setInSittingPose(false);
        clearRiderControlLock();
        setRunning(false);
        setAccelerating(false);
        resetRiderFlightThrottle();
        setGoingUp(false);
        setGoingDown(false);
        setTarget(null);
        if (getNavigation().getPath() != null) {
            getNavigation().stop();
        }
    }


    protected void applyLoadedFlightState(boolean flying, boolean takeoff, boolean hovering, boolean landing) {
    }

    public void lockRiderControls(int ticks) {
        riderControlLockTicks = Math.max(riderControlLockTicks, Math.max(0, ticks));
        this.entityData.set(DATA_RIDER_LOCKED, true);
    }

    public boolean areRiderControlsLocked() {
        return level().isClientSide ? this.entityData.get(DATA_RIDER_LOCKED) : riderControlLockTicks > 0;
    }

    public void clearRiderControlLock() {
        if (riderControlLockTicks > 0 || this.entityData.get(DATA_RIDER_LOCKED)) {
            riderControlLockTicks = 0;
            this.entityData.set(DATA_RIDER_LOCKED, false);
        }
    }

    protected void tickRiderControlLock() {
        if (!level().isClientSide && riderControlLockTicks > 0) {
            riderControlLockTicks--;
            if (riderControlLockTicks == 0) {
                this.entityData.set(DATA_RIDER_LOCKED, false);
            }
        }
    }
}
