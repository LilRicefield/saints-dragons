package com.leon.saintsdragons.server.entity.dragons.ignivorus;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.ai.navigation.DragonFlightMoveHelper;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.controller.ignivorus.IgnivorusRiderController;
import com.leon.saintsdragons.server.entity.controller.ignivorus.IgnivorusPhysicsController;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusInteractionHandler;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.interfaces.SoundHandledDragon;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Map;

public class Ignivorus extends RideableDragonBase implements DragonFlightCapable, SoundHandledDragon {

    // ===== ENTITY DATA ACCESSORS =====

    public static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Boolean> DATA_TAKEOFF =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Boolean> DATA_HOVERING =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Boolean> DATA_LANDING =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Boolean> DATA_RUNNING =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Integer> DATA_FLIGHT_MODE =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Float> DATA_RIDER_FORWARD =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Float> DATA_RIDER_STRAFE =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Boolean> DATA_GOING_UP =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Boolean> DATA_GOING_DOWN =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Boolean> DATA_ACCELERATING =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);

    private static final double MODEL_SCALE = 1.0D;

    // Vocal entries (placeholder - sounds to be added later)
    private static final Map<String, VocalEntry> VOCAL_ENTRIES = Map.of();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final IgnivorusAnimationHandler animationHandler = new IgnivorusAnimationHandler(this);
    private final IgnivorusPhysicsController physicsController = new IgnivorusPhysicsController(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final IgnivorusRiderController riderController;
    private final IgnivorusInteractionHandler interactionHandler = new IgnivorusInteractionHandler(this);

    private final DragonPathNavigateGround groundNav;
    private final FlyingPathNavigation airNav;
    private boolean usingAirNav;

    public int timeFlying = 0;
    private int airTicks;
    public int groundTicks;

    // Banking animation state
    private float bankSmoothedYaw = 0f;
    private int bankHoldTicks = 0;
    private int bankDir = 0;
    private float bankAngle = 0f;
    private float prevBankAngle = 0f;

    // Pitching animation state
    private float pitchSmoothedPitch = 0f;
    private int pitchHoldTicks = 0;
    private int pitchDir = 0;

    // Client locator cache
    private final Map<String, Vec3> clientLocatorCache = new java.util.concurrent.ConcurrentHashMap<>();

    public Ignivorus(EntityType<? extends Ignivorus> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.1F);

        this.groundNav = new DragonPathNavigateGround(this, level);
        this.airNav = new FlyingPathNavigation(this, level) {
            @Override
            public boolean isStableDestination(@NotNull net.minecraft.core.BlockPos pos) {
                return !this.level.getBlockState(pos.below()).isAir();
            }
        };
        this.navigation = this.groundNav;
        this.moveControl = new DragonFlightMoveHelper(this);
        this.usingAirNav = false;

        this.riderController = new IgnivorusRiderController(this);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FLYING, false);
        this.entityData.define(DATA_TAKEOFF, false);
        this.entityData.define(DATA_HOVERING, false);
        this.entityData.define(DATA_LANDING, false);
        this.entityData.define(DATA_RUNNING, false);
        this.entityData.define(DATA_FLIGHT_MODE, -1);
        this.entityData.define(DATA_RIDER_FORWARD, 0F);
        this.entityData.define(DATA_RIDER_STRAFE, 0F);
        this.entityData.define(DATA_GROUND_MOVE_STATE, 0);
        this.entityData.define(DATA_GOING_UP, false);
        this.entityData.define(DATA_GOING_DOWN, false);
        this.entityData.define(DATA_ACCELERATING, false);
    }

    @Override
    protected void defineRideableDragonData() {
        // Additional rideable dragon data if needed
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
            .add(Attributes.MAX_HEALTH, 100.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.25D)
            .add(Attributes.FLYING_SPEED, 0.4D)
            .add(Attributes.ATTACK_DAMAGE, 15.0D)
            .add(Attributes.FOLLOW_RANGE, 48.0D)
            .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void registerGoals() {
        // No AI goals for now - just testing animations and flight
    }

    @Override
    public void tick() {
        super.tick();
        physicsController.tick();

        // Update air/ground time
        if (isFlying()) {
            airTicks++;
            groundTicks = 0;
            timeFlying++;

            // Clear takeoff flag after animation completes (30 ticks = 1.5s)
            if (isTakeoff() && timeFlying > 30) {
                setTakeoff(false);
            }
        } else {
            groundTicks++;
            airTicks = 0;
            timeFlying = 0;
        }

        // Update flight mode for animation system (server side only)
        if (!level().isClientSide && isFlying()) {
            int newFlightMode = physicsController.computeFlightModeForSync();
            setFlightMode(newFlightMode);
        }

        // Update banking and pitching for animations
        tickBankingLogic();
        tickPitchingLogic();
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        return groundNav;
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        Vec3 input = riderController.getRiddenInput(player, deltaIn);
        return super.getRiddenInput(player, input);
    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        riderController.tickRidden(player, travelVector);
    }

    @Override
    public void travel(@NotNull Vec3 travelVec) {
        if (this.isVehicle() && riderController.getRidingPlayer() != null) {
            Player rider = riderController.getRidingPlayer();
            if (isFlying()) {
                // Custom flight movement
                riderController.handleRiderMovement(rider, travelVec);
            } else {
                // Ground movement - use vanilla system
                this.setSpeed(riderController.getRiddenSpeed(rider));
                super.travel(travelVec);
            }
        } else {
            // AI movement
            super.travel(travelVec);
        }
    }

    @Override
    public float getRiddenSpeed(@NotNull Player rider) {
        return riderController.getRiddenSpeed(rider);
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        return riderController.getDismountLocationForPassenger(passenger);
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        return interactionHandler.handleInteraction(player, hand);
    }

    @Override
    public boolean isFood(@NotNull ItemStack stack) {
        // Fire dragon likes cooked meat
        return stack.is(Items.COOKED_BEEF) || stack.is(Items.COOKED_PORKCHOP);
    }

    public void setCommandManual(int command) {
        this.setCommand(command);
    }

    // ===== RIDER INPUT HANDLERS =====

    @Override
    protected void onRiderTakeoffRequest(Player player) {
        if (!isFlying() && onGround()) {
            riderController.requestRiderTakeoff();
        }
    }

    @Override
    public void positionRider(@NotNull Entity passenger, @NotNull Entity.MoveFunction moveFunction) {
        riderController.positionRider(passenger, moveFunction);
    }

    @Override
    public double getPassengersRidingOffset() {
        return riderController.getPassengersRidingOffset();
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return riderController.getControllingPassenger();
    }

    // ===== FLIGHT SYSTEM =====

    public void switchNavigation(boolean flying) {
        if (flying && !usingAirNav) {
            this.navigation = this.airNav;
            this.usingAirNav = true;
        } else if (!flying && usingAirNav) {
            this.navigation = this.groundNav;
            this.usingAirNav = false;
        }
    }

    @Override
    public void markLandedNow() {
        setFlying(false);
        setLanding(false);
        setTakeoff(false);
        timeFlying = 0;
    }

    @Override
    public float getFlightSpeed() {
        return (float) this.getAttributeValue(Attributes.FLYING_SPEED);
    }

    @Override
    public double getPreferredFlightAltitude() {
        return 20.0D; // Default altitude for flight AI
    }

    @Override
    public boolean canTakeoff() {
        return !isFlying() && onGround();
    }

    // ===== STATE MANAGEMENT =====

    public void setFlying(boolean flying) {
        boolean wasFlying = isFlying();
        this.entityData.set(DATA_FLYING, flying);

        if (wasFlying != flying) {
            this.setAccelerating(false);
            if (flying) {
                switchNavigation(true);
                setRunning(false);
            } else {
                switchNavigation(false);
            }
        }
    }

    public void setTakeoff(boolean takeoff) {
        this.entityData.set(DATA_TAKEOFF, takeoff);
    }

    public void setHovering(boolean hovering) {
        this.entityData.set(DATA_HOVERING, hovering);
    }

    public void setLanding(boolean landing) {
        if (landing && isVehicle()) {
            return;
        }
        this.entityData.set(DATA_LANDING, landing);
    }

    @Override
    public void setRunning(boolean running) {
        this.entityData.set(DATA_RUNNING, running);
    }

    @Override
    public boolean isRunning() {
        return this.entityData.get(DATA_RUNNING);
    }

    @Override
    protected boolean isDragonFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    @Override
    public boolean isTakeoff() {
        return this.entityData.get(DATA_TAKEOFF);
    }

    @Override
    public boolean isHovering() {
        return this.entityData.get(DATA_HOVERING);
    }

    @Override
    public boolean isLanding() {
        return this.entityData.get(DATA_LANDING);
    }

    @Override
    public int getFlightMode() {
        return this.entityData.get(DATA_FLIGHT_MODE);
    }

    public void setFlightMode(int mode) {
        this.entityData.set(DATA_FLIGHT_MODE, mode);
    }

    // ===== UTILITY METHODS =====

    @Override
    public Vec3 getMouthPosition() {
        // Simple fallback: return eye position
        return this.getEyePosition();
    }

    @Override
    public Vec3 getHeadPosition() {
        // Simple fallback: return eye position
        return this.getEyePosition();
    }

    @Override
    public com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        // No abilities yet - just testing flight and animations
        return null;
    }

    // ===== ENTITY DATA ACCESSOR GETTERS =====

    @Override
    protected EntityDataAccessor<Float> getRiderForwardAccessor() {
        return DATA_RIDER_FORWARD;
    }

    @Override
    protected EntityDataAccessor<Float> getRiderStrafeAccessor() {
        return DATA_RIDER_STRAFE;
    }

    @Override
    protected EntityDataAccessor<Integer> getGroundMoveStateAccessor() {
        return DATA_GROUND_MOVE_STATE;
    }

    @Override
    protected EntityDataAccessor<Integer> getFlightModeAccessor() {
        return DATA_FLIGHT_MODE;
    }

    @Override
    protected EntityDataAccessor<Boolean> getGoingUpAccessor() {
        return DATA_GOING_UP;
    }

    @Override
    protected EntityDataAccessor<Boolean> getGoingDownAccessor() {
        return DATA_GOING_DOWN;
    }

    @Override
    protected EntityDataAccessor<Boolean> getAcceleratingAccessor() {
        return DATA_ACCELERATING;
    }

    // ===== BANKING & PITCHING ANIMATIONS =====

    private void tickBankingLogic() {
        prevBankAngle = bankAngle;

        // Reset banking when not flying - instant snap back
        if (!isFlying()) {
            if (bankDir != 0 || bankAngle != 0f || bankSmoothedYaw != 0f) {
                bankDir = 0;
                bankSmoothedYaw = 0f;
                bankHoldTicks = 0;
                bankAngle = 0f;
                prevBankAngle = 0f;
            }
            return;
        }

        // Exponential smoothing on yaw delta to avoid jitter, wrap to account for crossing 360 -> 0
        float yawChange = Mth.wrapDegrees(getYRot() - yRotO);
        bankSmoothedYaw = bankSmoothedYaw * 0.70f + yawChange * 0.30f; // More reactive than before, less than Raevyx

        // Convert smoothed yaw delta into a banking roll
        float targetAngle = Mth.clamp(bankSmoothedYaw * 4.5f, -45f, 45f); // More dramatic than before, less than Raevyx

        // Ease toward the new target
        bankAngle = Mth.lerp(0.28f, bankAngle, targetAngle); // Snappier than before, less snappy than Raevyx
        if (Math.abs(bankAngle) < 0.01f) {
            bankAngle = 0f;
        }

        // Update coarse direction for animation fallbacks
        float enter = 12.0f; // Higher threshold than Raevyx (less sensitive)
        float exit = 5.0f;   // Higher threshold than Raevyx (less sensitive)

        int desiredDir = bankDir;
        if (bankAngle > enter) desiredDir = 1;
        else if (bankAngle < -enter) desiredDir = -1;
        else if (Math.abs(bankAngle) < exit) desiredDir = 0;  // banking_off when flying straight

        if (desiredDir != bankDir) {
            // If transitioning to "off" (0), use very short hold time for instant reset
            int holdTime = (desiredDir == 0) ? 1 : 2;
            if (bankHoldTicks >= holdTime) {
                bankDir = desiredDir;
                bankHoldTicks = 0;
            } else {
                bankHoldTicks++;
            }
        } else {
            bankHoldTicks = Math.min(bankHoldTicks + 1, 10);
        }
    }

    private void tickPitchingLogic() {
        // Reset pitching when not flying
        if (!isFlying()) {
            if (pitchDir != 0) {
                pitchDir = 0;
                pitchSmoothedPitch = 0f;
                pitchHoldTicks = 0;
            }
            return;
        }

        int desiredDir = pitchDir;

        // When ridden, use Space/L-Alt input directly (NOT entity pitch)
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player) {
            if (isGoingUp()) {
                desiredDir = -1;  // Pitching up
            } else if (isGoingDown()) {
                desiredDir = 1;   // Pitching down
            } else {
                desiredDir = 0;   // Level flight (pitching_off)
            }
        } else {
            // AI flight: use actual pitch change for animation
            float pitchChange = getXRot() - xRotO;
            pitchSmoothedPitch = pitchSmoothedPitch * 0.85f + pitchChange * 0.15f;

            // Hysteresis thresholds
            float enter = 3.0f;
            float exit = 3.0f;

            if (pitchSmoothedPitch > enter) desiredDir = 1;
            else if (pitchSmoothedPitch < -enter) desiredDir = -1;
            else if (Math.abs(pitchSmoothedPitch) < exit) desiredDir = 0;
        }

        // Hysteresis system to prevent rapid switching
        if (desiredDir != pitchDir) {
            // Faster transition to "off" state
            int holdTime = (desiredDir == 0) ? 1 : 2;
            if (pitchHoldTicks >= holdTime) {
                pitchDir = desiredDir;
                pitchHoldTicks = 0;
            } else {
                pitchHoldTicks++;
            }
        } else {
            pitchHoldTicks = Math.min(pitchHoldTicks + 1, 20);
        }
    }

    public float getBankAngleDegrees(float partialTick) {
        return Mth.lerp(partialTick, prevBankAngle, bankAngle);
    }

    public double getPitchDirection() {
        return pitchDir;
    }

    // ===== GECKOLIB ANIMATION =====

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Movement controller handles idle/walk/run/flight animations
        AnimationController<Ignivorus> movementController =
            new AnimationController<>(this, "movement", 5, animationHandler::handleMovementAnimation);

        // Banking and pitching controllers for flight dynamics
        AnimationController<Ignivorus> bankingController =
            new AnimationController<>(this, "banking", 8, animationHandler::bankingPredicate);

        AnimationController<Ignivorus> pitchingController =
            new AnimationController<>(this, "pitching", 6, animationHandler::pitchingPredicate);

        controllers.add(movementController, bankingController, pitchingController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ===== SOUND SYSTEM =====

    @Override
    public Map<String, VocalEntry> getVocalEntries() {
        return VOCAL_ENTRIES;
    }

    @Override
    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
    }

    // ===== CLIENT LOCATOR CACHE =====

    public void setClientLocatorPosition(String name, Vec3 pos) {
        if (name == null || pos == null) return;
        this.clientLocatorCache.put(name, pos);
    }

    @Override
    public Vec3 getClientLocatorPosition(String name) {
        if (name == null) return null;
        return this.clientLocatorCache.get(name);
    }

    // ===== BREEDING (placeholder) =====

    @Override
    public @Nullable AgeableMob getBreedOffspring(@NotNull net.minecraft.server.level.ServerLevel level, @NotNull AgeableMob otherParent) {
        return null; // No breeding for now
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    // ===== SPAWN PLACEMENT =====

    public static boolean canSpawnHere(EntityType<Ignivorus> type,
                                       net.minecraft.world.level.LevelAccessor level,
                                       MobSpawnType reason,
                                       BlockPos pos,
                                       net.minecraft.util.RandomSource random) {
        BlockPos below = pos.below();
        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }
        if (!level.getFluidState(below).isEmpty()) {
            return false;
        }
        boolean solidGround = level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP);
        boolean feetFree = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
        boolean headFree = level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
        return solidGround && feetFree && headFree;
    }
}
