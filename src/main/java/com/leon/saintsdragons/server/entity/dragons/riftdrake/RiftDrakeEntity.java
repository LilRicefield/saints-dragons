package com.leon.saintsdragons.server.entity.dragons.riftdrake;

import com.leon.saintsdragons.common.registry.riftdrake.RiftDrakeAbilities;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.ai.navigation.DragonSwimMoveControl;
import com.leon.saintsdragons.server.ai.navigation.DragonSwimNavigate;
import com.leon.saintsdragons.server.ai.goals.riftdrake.RiftDrakeFindWaterGoal;
import com.leon.saintsdragons.server.ai.goals.riftdrake.RiftDrakeFollowOwnerGoal;
import com.leon.saintsdragons.server.ai.goals.riftdrake.RiftDrakeLeaveWaterGoal;
import com.leon.saintsdragons.server.ai.goals.riftdrake.RiftDrakeRandomSwimGoal;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers.*;
import com.leon.saintsdragons.server.entity.handler.DragonKeybindHandler;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.AquaticDragon;
import com.leon.saintsdragons.server.entity.interfaces.DragonControlStateHolder;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import net.minecraft.server.level.ServerPlayer;
import com.leon.saintsdragons.server.entity.controller.riftdrake.RiftDrakeRiderController;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreathAirGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.annotation.Nonnull;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.util.GeckoLibUtil;
import com.leon.saintsdragons.server.entity.base.RideableDragonData;
import net.minecraft.world.entity.LivingEntity;

public class RiftDrakeEntity extends RideableDragonBase implements AquaticDragon, DragonControlStateHolder {

    private static final EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RIDER_FORWARD = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIDER_STRAFE = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_ACCELERATING = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SWIMMING = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SWIM_TURN = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SWIM_PITCH = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PHASE_TWO = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RIDER_LOCKED = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.BOOLEAN);

    // Flight mode data accessor (not used for ground dragon but required by interface)
    private static final EntityDataAccessor<Integer> DATA_FLIGHT_MODE = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_GOING_UP = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GOING_DOWN = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final DragonKeybindHandler keybindHandler = new DragonKeybindHandler(this);
    private final RiftDrakeAnimationHandler animationHandler = new RiftDrakeAnimationHandler(this);
    private final RiftDrakeInteractionHandler interactionHandler = new RiftDrakeInteractionHandler(this);
    private final RiftDrakeRiderController riderController;
    private final PathNavigation groundNavigation;
    private final DragonSwimNavigate waterNavigation;
    private final MoveControl landMoveControl;
    private final DragonSwimMoveControl swimMoveControl;
    private final RiftDrakeLookController landLookControl;
    private int riderControlLockTicks = 0;
    private RiftDrakeRandomSwimGoal waterSwimGoal;
    private boolean swimming;
    private int swimTicks;
    private int ticksInWater;
    private int ticksOutOfWater;
    private float swimTurnSmoothedYaw;
    private int swimTurnState;
    private int swimPitchStateTicks;
    private byte controlState = 0;
    private boolean useLeftClawNext = true; // Toggles between left/right claw attacks

    public RiftDrakeEntity(EntityType<? extends RiftDrakeEntity> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
        this.groundNavigation = new DragonPathNavigateGround(this, level);
        this.waterNavigation = new DragonSwimNavigate(this, level);
        this.landMoveControl = new RiftDrakeMoveControl(this);
        this.swimMoveControl = new DragonSwimMoveControl(this, 6.0F, 0.08D, 0.12D);
        this.landLookControl = new RiftDrakeLookController(this);
        this.navigation = this.groundNavigation;
        this.moveControl = this.landMoveControl;
        this.lookControl = this.landLookControl;
        this.riderController = new RiftDrakeRiderController(this);
        this.setRideable();
    }

    private void tickRiderControlLock() {
        if (riderControlLockTicks > 0) {
            riderControlLockTicks--;
            if (riderControlLockTicks <= 0) {
                this.entityData.set(DATA_RIDER_LOCKED, false);
            }
        }
    }

    private void copyRiderLook(Player player) {
        if (player == null) {
            return;
        }
        float bodyYaw = player.getYRot();
        this.setYRot(bodyYaw);
        this.setYHeadRot(bodyYaw);
        this.yHeadRotO = bodyYaw;
        this.yBodyRot = bodyYaw;
        this.yBodyRotO = bodyYaw;

        float pitch = Mth.clamp(player.getXRot(), -45.0F, 45.0F);
        this.setXRot(pitch);
        this.xRotO = pitch;
    }

    public boolean areRiderControlsLocked() {
        return level().isClientSide ? this.entityData.get(DATA_RIDER_LOCKED) : riderControlLockTicks > 0;
    }

    public void lockRiderControls(int ticks) {
        riderControlLockTicks = Math.max(riderControlLockTicks, Math.max(0, ticks));
        this.entityData.set(DATA_RIDER_LOCKED, true);
        this.setAccelerating(false);
        this.setLastRiderForward(0.0F);
        this.setLastRiderStrafe(0.0F);
        this.setGroundMoveStateFromRider(0);
        this.setGoingUp(false);
        this.setGoingDown(false);
        this.setDeltaMovement(Vec3.ZERO);
        if (!this.level().isClientSide) {
            this.getNavigation().stop();
            this.setTarget(null);
        }
    }

    public void lockAbilities(int ticks) {
        combatManager.lockGlobalCooldown(ticks);
    }

    @Override
    protected boolean isRiderInputLocked(Player player) {
        return areRiderControlsLocked();
    }

    @Override
    protected void applyRiderVerticalInput(Player player, boolean goingUp, boolean goingDown, boolean locked) {
        if (locked) {
            setGoingUp(false);
            setGoingDown(false);
            return;
        }
        boolean inWater = this.isSwimming() || this.isInWaterOrBubble();
        setGoingUp(inWater && goingUp);
        setGoingDown(inWater && goingDown);
    }

    @Override
    protected void applyRiderMovementInput(Player player, float forward, float strafe, float yaw, boolean locked) {
        float fwd = locked ? 0f : applyInputDeadzone(forward);
        float str = locked ? 0f : applyInputDeadzone(strafe);
        setLastRiderForward(fwd);
        setLastRiderStrafe(str);
        int moveState = 0;
        float magnitude = Math.abs(fwd) + Math.abs(str);
        if (magnitude > 0.05f) {
            moveState = isAccelerating() ? 2 : 1;
        }
        setGroundMoveStateFromRider(moveState);
    }

    @Override
    protected void handleRiderAction(ServerPlayer player, DragonRiderAction action, String abilityName, boolean locked) {
        if (locked || action == null) {
            return;
        }
        switch (action) {
            case TAKEOFF_REQUEST -> handleJumpRequest();
            case ACCELERATE -> setAccelerating(true);
            case STOP_ACCELERATE -> setAccelerating(false);
            case ABILITY_USE -> {
                if (abilityName != null && !abilityName.isEmpty()) {
                    useRidingAbility(abilityName);
                }
            }
            case ABILITY_STOP -> {
                if (abilityName != null && !abilityName.isEmpty()) {
                    forceEndActiveAbility();
                }
            }
            default -> { }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 140.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_GROUND_MOVE_STATE, 0);
        this.entityData.define(DATA_RIDER_FORWARD, 0.0F);
        this.entityData.define(DATA_RIDER_STRAFE, 0.0F);
        this.entityData.define(DATA_ACCELERATING, false);
        this.entityData.define(DATA_SWIMMING, false);
        this.entityData.define(DATA_SWIM_TURN, 0);
        this.entityData.define(DATA_SWIM_PITCH, 0);
        this.entityData.define(DATA_PHASE_TWO, false);
        this.entityData.define(DATA_FLIGHT_MODE, -1);
        this.entityData.define(DATA_GOING_UP, false);
        this.entityData.define(DATA_GOING_DOWN, false);
        this.entityData.define(DATA_RIDER_LOCKED, false);
    }
    
    @Override
    protected void defineRideableDragonData() {
        // Data accessors are already defined in defineSynchedData()
    }
    
    // ===== REQUIRED ABSTRACT METHODS FROM RIDEABLEDRAGONBASE =====
    
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

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new BreathAirGoal(this));
        this.goalSelector.addGoal(2, new RiftDrakeLeaveWaterGoal(this));
        this.goalSelector.addGoal(3, new RiftDrakeFindWaterGoal(this));
        this.goalSelector.addGoal(4, new RiftDrakeFollowOwnerGoal(this));
        this.waterSwimGoal = new RiftDrakeRandomSwimGoal(this, 1.0D, 30);
        this.goalSelector.addGoal(5, waterSwimGoal);
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<RiftDrakeEntity> movementController =
                new AnimationController<>(this, "movement", 5, animationHandler::movementPredicate);
        AnimationController<RiftDrakeEntity> swimController =
                new AnimationController<>(this, "swim_direction", 4, animationHandler::swimDirectionPredicate);
        AnimationController<RiftDrakeEntity> actions =
                new AnimationController<>(this, "action", 10, animationHandler::actionPredicate);

        // Sound keyframes
        movementController.setSoundKeyframeHandler(this::onAnimationSound);
        swimController.setSoundKeyframeHandler(this::onAnimationSound);
        actions.setSoundKeyframeHandler(this::onAnimationSound);

        // Setup animation triggers
        animationHandler.setupActionController(actions);

        controllers.add(movementController);
        controllers.add(swimController);
        controllers.add(actions);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    @Override
    public void initializeAnimationState() {
        // TODO: derive initial animation state for Rift Drake
    }

    @Override
    public void resetAnimationState() {
        // TODO: recalculate animation state when needed
    }

    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
    }

    public DragonKeybindHandler getKeybindHandler() {
        return keybindHandler;
    }

    @Override
    public void tick() {
        super.tick();
        tickSittingState();
        updateSittingProgress();
        tickClientSideUpdates();
        
        if (!level().isClientSide) {
            tickRiderControlLock();
            boolean inWater = this.isInWater();
            if (inWater) {
                this.setAirSupply(this.getMaxAirSupply());
                swimTicks = Math.min(swimTicks + 1, 200);
                ticksInWater = Math.min(ticksInWater + 1, 1200);
                ticksOutOfWater = 0;
            } else {
                swimTicks = Math.max(swimTicks - 1, 0);
                ticksOutOfWater = Math.min(ticksOutOfWater + 1, 1200);
                ticksInWater = 0;
            }

            if (inWater && !swimming) {
                enterSwimState();
            } else if (!inWater && swimming) {
                exitSwimState();
            }

            // Ensure proper look control when being ridden
            if (this.getControllingPassenger() != null && this.lookControl != landLookControl) {
                this.lookControl = landLookControl;
            }

            this.tickAnimationStates();
            this.updateSwimOrientationState();
        }
    }

    @Override
    public void travel(@NotNull Vec3 motion) {
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            if (areRiderControlsLocked()) {
                this.setDeltaMovement(Vec3.ZERO);
                return;
            }
            // Clear any AI navigation when being ridden
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }

            if (this.isInWaterOrBubble()) {
                handleRiddenSwimming(motion);
            } else {
                setGoingUp(false);
                setGoingDown(false);
                // Use vanilla movement system for proper camera-relative movement
                // This will call getRiddenInput() and getRiddenSpeed() properly
                super.travel(motion);
            }
        } else {
            // Normal AI movement
            if (this.isInWater()) {
                handleRiddenSwimming(motion);
            } else {
                super.travel(motion);
            }
        }
    }

    @Override
    public @NotNull PathNavigation getNavigation() {
        return swimming ? waterNavigation : groundNavigation;
    }

    @Override
    public PathNavigation getAquaticNavigation() {
        return waterNavigation;
    }

    @Override
    public void onEnterWater() {
        this.setDeltaMovement(this.getDeltaMovement());
        this.tickAnimationStates();
    }

    @Override
    public void onExitWater() {
        this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
        this.tickAnimationStates();
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        InteractionResult handlerResult = interactionHandler.handleInteraction(player, hand);
        if (handlerResult != InteractionResult.PASS) {
            return handlerResult;
        }

        // Fall back to base implementation for any unhandled interactions
        return super.mobInteract(player, hand);
    }
    
    /**
     * Allow interaction handler to call super.mobInteract
     */
    public InteractionResult superMobInteract(Player player, InteractionHand hand) {
        return super.mobInteract(player, hand);
    }
    
    @Override
    public boolean isFood(@Nonnull net.minecraft.world.item.ItemStack stack) {
        // Rift Drakes prefer fish like other dragons
        return stack.is(net.minecraft.world.item.Items.COD) || 
               stack.is(net.minecraft.world.item.Items.SALMON) || 
               stack.is(net.minecraft.world.item.Items.TROPICAL_FISH);
    }

    @Override
    public void setLastRiderForward(float forward) {
        this.entityData.set(DATA_RIDER_FORWARD, forward);
    }

    @Override
    public void setLastRiderStrafe(float strafe) {
        this.entityData.set(DATA_RIDER_STRAFE, strafe);
    }

    public float getLastRiderForward() {
        return this.entityData.get(DATA_RIDER_FORWARD);
    }

    public float getLastRiderStrafe() {
        return this.entityData.get(DATA_RIDER_STRAFE);
    }

    @Override
    public boolean isAccelerating() {
        return this.entityData.get(DATA_ACCELERATING);
    }

    @Override
    public void setAccelerating(boolean accelerating) {
        this.entityData.set(DATA_ACCELERATING, accelerating);
    }

    public void setGroundMoveStateFromRider(int state) {
        int s = Mth.clamp(state, 0, 2);
        if (this.entityData.get(DATA_GROUND_MOVE_STATE) != s) {
            this.entityData.set(DATA_GROUND_MOVE_STATE, s);
        }
        this.syncAnimState(s, getSyncedFlightMode());
    }
    
    // ===== REQUIRED METHODS FROM RIDEABLEDRAGONBASE =====
    
    @Override
    public boolean isRunning() {
        return false; // Rift Drake doesn't have running state
    }
    
    @Override
    public void setRunning(boolean running) {
        // Rift Drake doesn't have running state
    }
    
    @Override
    public boolean isDragonFlying() {
        return false; // Rift Drake doesn't fly
    }
    
    @Override
    public boolean isHovering() {
        return false; // Rift Drake doesn't hover
    }
    
    @Override
    public boolean isTakeoff() {
        return false; // Rift Drake doesn't take off
    }
    
    @Override
    public boolean isLanding() {
        return false; // Rift Drake doesn't land
    }
    
    @Override
    public int getFlightMode() {
        return -1; // Ground mode
    }


    public void handleJumpRequest() {
        if (areRiderControlsLocked()) {
            return;
        }
        if (this.isInWater()) {
            // Enhanced water jump - more powerful for aquatic creature
            Vec3 jump = new Vec3(0.0D, 0.6D, 0.0D);
            this.setDeltaMovement(this.getDeltaMovement().add(jump));
            this.hasImpulse = true;
        } else if (this.onGround()) {
            // Ground jump - standard jump height
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x, 0.42D, movement.z);
            this.hasImpulse = true;
        }
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        if (areRiderControlsLocked()) {
            return Vec3.ZERO;
        }
        Vec3 input = riderController.getRiddenInput(player, deltaIn);

        // Capture rider inputs for animation state
        if (!level().isClientSide) {
            float fwd = (float) Mth.clamp(input.z, -1.0D, 1.0D);
            float str = (float) Mth.clamp(input.x, -1.0D, 1.0D);
            setLastRiderForward(RideableDragonData.applyInputThreshold(fwd));
            setLastRiderStrafe(RideableDragonData.applyInputThreshold(str));
        }
        return input;
    }

    @Override
    protected float getRiddenSpeed(@Nonnull @NotNull Player rider) {
        if (areRiderControlsLocked()) {
            return 0.0F;
        }
        return riderController.getRiddenSpeed(rider);
    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        if (areRiderControlsLocked()) {
            player.fallDistance = 0.0F;
            this.fallDistance = 0.0F;
            this.setTarget(null);
            copyRiderLook(player);
            this.setAccelerating(false);
            this.setGoingUp(false);
            this.setGoingDown(false);
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        riderController.tickRidden(player, travelVector);
    }

    @Override
    public void removePassenger(@NotNull Entity passenger) {
        super.removePassenger(passenger);
        if (!this.level().isClientSide) {
            this.setAccelerating(false);
            this.setLastRiderForward(0.0F);
            this.setLastRiderStrafe(0.0F);
            this.setGroundMoveStateFromRider(0);
        }
    }

    // Let RideableDragonBase handle tickAnimationStates() for proper networking

    @Override
    public void jumpFromGround() {
        super.jumpFromGround();
        this.setGroundMoveStateFromRider(1);
    }

    public void initializeRiderState() {
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_GROUND_MOVE_STATE, 0);
            this.entityData.set(DATA_RIDER_FORWARD, 0.0F);
            this.entityData.set(DATA_RIDER_STRAFE, 0.0F);
            this.setAccelerating(false);
        }
    }

    @Override
    public Vec3 getHeadPosition() {
        return this.getEyePosition();
    }

    @Override
    public Vec3 getMouthPosition() {
        return this.position().add(0, this.getBbHeight() * 0.8, 0);
    }

    public void useRidingAbility(String abilityName) {
        if (abilityName == null || abilityName.isEmpty()) {
            return;
        }
        if (areRiderControlsLocked()) {
            return;
        }
        Entity rider = this.getControllingPassenger();
        if (!(rider instanceof LivingEntity)) {
            return;
        }
        if (this.isTame() && rider instanceof Player player && !this.isOwnedBy(player)) {
            return;
        }

        DragonAbilityType<?, ?> type = com.leon.saintsdragons.common.registry.AbilityRegistry.get(abilityName);
        if (type != null) {
            combatManager.tryUseAbility(type);
        }
    }

    public void forceEndActiveAbility() {
        combatManager.forceEndActiveAbility();
    }

    // ===== RIDING METHODS =====
    
    @Override
    public double getPassengersRidingOffset() {
        return riderController.getPassengersRidingOffset();
    }

    @Override
    protected void positionRider(@Nonnull @NotNull Entity passenger, @Nonnull @NotNull Entity.MoveFunction moveFunction) {
        riderController.positionRider(passenger, moveFunction);
    }

    @Override
    public @Nullable net.minecraft.world.entity.LivingEntity getControllingPassenger() {
        return riderController.getControllingPassenger();
    }


    @Override
    public net.minecraft.world.entity.AgeableMob getBreedOffspring(@Nonnull net.minecraft.server.level.ServerLevel level, @Nonnull net.minecraft.world.entity.AgeableMob other) {
        return null;
    }

    @Override
    public com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        return com.leon.saintsdragons.common.registry.riftdrake.RiftDrakeAbilities.BITE;
    }

    public static boolean canSpawn(EntityType<RiftDrakeEntity> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, net.minecraft.util.RandomSource random) {
        return level.getFluidState(pos).isSource() || level.getFluidState(pos.below()).isSource();
    }

    private void enterSwimState() {
        swimming = true;
        this.navigation = waterNavigation;
        this.moveControl = swimMoveControl;
        
        this.entityData.set(DATA_SWIMMING, true);
        if (waterSwimGoal != null) {
            waterSwimGoal.forceTrigger();
        }
    }

    private void exitSwimState() {
        swimming = false;
        this.navigation = groundNavigation;
        this.moveControl = landMoveControl;
        this.lookControl = landLookControl; // Always use land look control when exiting water
        this.waterNavigation.stop();
        Vec3 delta = this.getDeltaMovement();
        this.setDeltaMovement(new Vec3(delta.x, 0.0D, delta.z));
        this.entityData.set(DATA_SWIMMING, false);
        this.entityData.set(DATA_SWIM_TURN, 0);
        this.entityData.set(DATA_SWIM_PITCH, 0);
        this.swimTurnSmoothedYaw = 0.0F;
        this.swimTurnState = 0;
        this.swimPitchStateTicks = 0;
    }

    private void updateSwimOrientationState() {
        if (level().isClientSide) {
            return;
        }

        int desiredTurn = this.entityData.get(DATA_SWIM_TURN);
        int desiredPitchState = this.entityData.get(DATA_SWIM_PITCH);

        boolean riderControlled = this.isTame()
                && this.isVehicle()
                && this.getControllingPassenger() instanceof Player player
                && this.isOwnedBy(player)
                && this.isInWaterOrBubble();

        if (riderControlled) {
            float yawDelta = Mth.wrapDegrees(this.getYRot() - this.yRotO);
            swimTurnSmoothedYaw = swimTurnSmoothedYaw * 0.6F + yawDelta * 0.4F;
            float enter = 0.35F;
            float exit = 0.12F;

            int targetState = swimTurnState;
            if (swimTurnSmoothedYaw > enter) {
                targetState = -1;
            } else if (swimTurnSmoothedYaw < -enter) {
                targetState = 1;
            } else if (Math.abs(swimTurnSmoothedYaw) < exit) {
                targetState = 0;
            }

            swimTurnState = targetState;
            desiredTurn = swimTurnState;

            if (this.isGoingUp()) {
                desiredPitchState = -1;
                swimPitchStateTicks = 6;
            } else if (this.isGoingDown()) {
                desiredPitchState = 1;
                swimPitchStateTicks = 6;
            } else if (desiredPitchState != 0) {
                if (swimPitchStateTicks > 0) {
                    swimPitchStateTicks--;
                } else {
                    desiredPitchState = 0;
                }
            }
        } else {
            swimTurnSmoothedYaw *= 0.5F;
            if (Math.abs(swimTurnSmoothedYaw) < 0.05F) {
                swimTurnState = 0;
            }
            desiredTurn = swimTurnState;

            if (desiredPitchState != 0) {
                if (++swimPitchStateTicks > 4) {
                    desiredPitchState = 0;
                    swimPitchStateTicks = 0;
                }
            } else {
                swimPitchStateTicks = 0;
            }
        }

        if (this.entityData.get(DATA_SWIM_TURN) != desiredTurn) {
            this.entityData.set(DATA_SWIM_TURN, desiredTurn);
        }

        if (this.entityData.get(DATA_SWIM_PITCH) != desiredPitchState) {
            this.entityData.set(DATA_SWIM_PITCH, desiredPitchState);
        }
    }

    public boolean isSwimming() {
        if (level().isClientSide) {
            return this.entityData.get(DATA_SWIMMING);
        }
        return swimming;
    }

    public int getSwimTurnDirection() {
        return Mth.clamp(this.entityData.get(DATA_SWIM_TURN), -1, 1);
    }

    public boolean isSwimmingDown() {
        return this.isSwimming() && this.entityData.get(DATA_SWIM_PITCH) > 0;
    }

    public boolean isSwimmingUp() {
        return this.isSwimming() && this.entityData.get(DATA_SWIM_PITCH) < 0;
    }

    public boolean isSwimmingMoving() {
        if (!isSwimming()) {
            return false;
        }

        if (this.getNavigation().isInProgress() && this.getNavigation().getPath() != null) {
            return true;
        }

        if (this.isVehicle()) {
            float fwd = Math.abs(this.entityData.get(DATA_RIDER_FORWARD));
            float str = Math.abs(this.entityData.get(DATA_RIDER_STRAFE));
            if (fwd > 0.03F || str > 0.03F) {
                return true;
            }
        }

        return this.getDeltaMovement().horizontalDistanceSqr() > 0.0025D;
    }

    public boolean isPhaseTwoActive() {
        return this.entityData.get(DATA_PHASE_TWO);
    }

    public void setPhaseTwoActive(boolean active, boolean syncAnim) {
        this.entityData.set(DATA_PHASE_TWO, active);
        if (syncAnim) {
            this.syncAnimState(this.entityData.get(DATA_GROUND_MOVE_STATE), this.getSyncedFlightMode());
        }
    }

    // ===== CLAW ALTERNATION SYSTEM =====

    public boolean shouldUseLeftClaw() {
        return useLeftClawNext;
    }

    public void toggleClawSide() {
        useLeftClawNext = !useLeftClawNext;
    }

    // ===== CONTROL STATE SYSTEM =====

    @Override
    public byte getControlState() {
        return controlState;
    }

    @Override
    public void setControlState(byte controlState) {
        this.controlState = controlState;
        if (!level().isClientSide) {
            keybindHandler.setControlState(controlState);
        }
    }

    @Override
    public boolean canPlayerModifyControlState(Player player) {
        return player != null && this.isOwnedBy(player);
    }

    @Override
    public byte buildClientControlState(boolean ascendDown, boolean descendDown, boolean attackDown, boolean primaryDown, boolean secondaryDown, boolean sneakDown) {
        byte state = 0;
        if (ascendDown) state |= 1;
        if (descendDown) state |= 2;
        if (attackDown) state |= 4;
        if (primaryDown) state |= 8;
        if (secondaryDown) state |= 16;
        if (sneakDown) state |= 32;
        return state;
    }

    @Override
    public RiderAbilityBinding getAttackRiderAbility() {
        // Phase 2 uses fast bite2, Phase 1 uses normal bite
        if (isPhaseTwoActive()) {
            return new RiderAbilityBinding(RiftDrakeAbilities.BITE2_ID, RiderAbilityBinding.Activation.PRESS);
        }
        return new RiderAbilityBinding(RiftDrakeAbilities.BITE_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getPrimaryRiderAbility() {
        // R key: Universal roar ability for all dragons (PRESS)
        return new RiderAbilityBinding(RiftDrakeAbilities.ROAR_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getSecondaryRiderAbility() {
        return new RiderAbilityBinding(RiftDrakeAbilities.PHASE_SHIFT_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getTertiaryRiderAbility() {
        // G key: Phase 2 gets claw attacks (PRESS, not HOLD like other dragons)
        if (isPhaseTwoActive()) {
            return new RiderAbilityBinding(RiftDrakeAbilities.CLAW_ID, RiderAbilityBinding.Activation.PRESS);
        }
        return null; // G key - not used in phase 1
    }

    @Override
    public DragonAbilityType<?, ?> getChannelingAbility() {
        return RiftDrakeAbilities.PHASE_SHIFT;
    }

    public void onAnimationSound(SoundKeyframeEvent<RiftDrakeEntity> event) {
        // Delegate all keyframed sounds to the sound handler
        this.getSoundHandler().handleAnimationSound(this, event.getKeyframeData(), event.getController());
    }

    private void handleRiddenSwimming(Vec3 input) {
        Vec3 velocity = this.getDeltaMovement();

        double swimSpeed = getSwimSpeed();
        if (isAccelerating()) {
            swimSpeed *= 1.6D;
        }

        Vec3 desired = getVec3(input, swimSpeed, velocity);
        Vec3 blended = velocity.add(desired.subtract(velocity).scale(0.28D));

        double dragFactor = this.isControlledByLocalInstance() ? 0.92D : 0.94D;
        blended = blended.multiply(dragFactor, 0.92D, dragFactor);

        if (!isGoingUp() && !isGoingDown() && getTarget() == null) {
            blended = blended.add(0.0D, -0.01D, 0.0D);
        }

        this.setDeltaMovement(blended);
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    private @NotNull Vec3 getVec3(Vec3 wishDir, double swimSpeed, Vec3 velocity) {
        double strafe = wishDir.x;
        double forward = wishDir.z;
        float yawRad = this.getYRot() * ((float)Math.PI / 180F);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double worldX = strafe * cos - forward * sin;
        double worldZ = forward * cos + strafe * sin;

        double dx = worldX * 0.85D * swimSpeed;
        double dz = worldZ * 0.85D * swimSpeed;

        double dy = velocity.y;
        if (isGoingUp()) {
            dy = Math.min(swimSpeed, dy + 0.12D * swimSpeed);
        } else if (isGoingDown()) {
            dy = Math.max(-swimSpeed, dy - 0.12D * swimSpeed);
        } else {
            dy *= 0.90D;
        }

        Vec3 desired = new Vec3(dx, dy, dz);
        return desired;
    }

    /**
     * Check if the dragon is dying (health below 10%)
     */
    public boolean isDying() {
        return this.getHealth() < this.getMaxHealth() * 0.1f;
    }
    
    // Required methods for RideableDragon interface
    @Override
    public boolean isGoingUp() {
        return this.entityData.get(DATA_GOING_UP);
    }
    
    @Override
    public void setGoingUp(boolean goingUp) {
        this.entityData.set(DATA_GOING_UP, goingUp);
    }
    
    @Override
    public boolean isGoingDown() {
        return this.entityData.get(DATA_GOING_DOWN);
    }
    
    @Override
    public void setGoingDown(boolean goingDown) {
        this.entityData.set(DATA_GOING_DOWN, goingDown);
    }

    private static class RiftDrakeMoveControl extends MoveControl {

        public RiftDrakeMoveControl(RiftDrakeEntity drake) {
            super(drake);
        }

        @Override
        public void tick() {
            super.tick();
        }
    }

    // ===== LOOK CONTROLLER =====
    public static class RiftDrakeLookController extends LookControl {
        private final RiftDrakeEntity dragon;

        public RiftDrakeLookController(RiftDrakeEntity dragon) {
            super(dragon);
            this.dragon = dragon;
        }

        @Override
        public void tick() {
            if (!this.dragon.isAlive()) {
                return;
            }

            LivingEntity rider = this.dragon.getControllingPassenger();
            if (this.dragon.isVehicle() && rider != null) {
                if (this.dragon.isControlledByLocalInstance()) {
                    float bodyYaw = rider.getYRot();
                    this.dragon.setYRot(bodyYaw);
                    this.dragon.setYHeadRot(bodyYaw);
                    this.dragon.yHeadRotO = bodyYaw;
                    this.dragon.yBodyRot = bodyYaw;
                    this.dragon.yBodyRotO = bodyYaw;

                    float pitch = Mth.clamp(rider.getXRot(), -45.0F, 45.0F);
                    this.dragon.setXRot(pitch);
                    this.dragon.xRotO = pitch;
                } else {
                    float serverPitch = this.dragon.getXRot();
                    this.dragon.xRotO = serverPitch;
                }
                return;
            }

            super.tick();
        }
    }

    private void tickSittingState() {
        // Clear sitting state if the dragon is being ridden
        if (!this.level().isClientSide && this.isVehicle() && this.isOrderedToSit()) {
            this.setOrderedToSit(false);
        }
    }

    private void updateSittingProgress() {
        if (level().isClientSide) {
            return;
        }

        if (this.isOrderedToSit()) {
            if (sitProgress < maxSitTicks()) {
                sitProgress++;
                this.entityData.set(DATA_SIT_PROGRESS, sitProgress);
            }
        } else {
            if (isVehicle()) {
                if (sitProgress != 0f) {
                    sitProgress = 0f;
                    prevSitProgress = 0f;
                    this.entityData.set(DATA_SIT_PROGRESS, 0f);
                }
            } else if (sitProgress > 0f) {
                sitProgress--;
                if (sitProgress < 0f) sitProgress = 0f;
                this.entityData.set(DATA_SIT_PROGRESS, sitProgress);
            }
        }
    }

    private void tickClientSideUpdates() {
        // Update client-side sit progress from synchronized data
        if (level().isClientSide) {
            prevSitProgress = sitProgress;
            sitProgress = this.entityData.get(DATA_SIT_PROGRESS);
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("PhaseTwo", isPhaseTwoActive());
    }

    @Override
    public void readAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("PhaseTwo")) {
            setPhaseTwoActive(tag.getBoolean("PhaseTwo"), false);
        }
    }

    @Override
    public void handleEntityEvent(byte eventId) {
        if (eventId == 6) {
            // Failed taming - show smoke particles ONLY, no sitting behavior at all
            if (level().isClientSide) {
                // Show smoke particles for failed taming
                for (int i = 0; i < 7; ++i) {
                    double d0 = this.random.nextGaussian() * 0.02D;
                    double d1 = this.random.nextGaussian() * 0.02D;
                    double d2 = this.random.nextGaussian() * 0.02D;
                    this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                            this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d0, d1, d2);
                }
            }
            // IMPORTANT: Don't call super for event 6 - it might trigger sitting behavior
        } else if (eventId == 7) {
            // Successful taming - show hearts only, sitting is handled separately
            if (level().isClientSide) {
                // Show heart particles for successful taming
                for (int i = 0; i < 7; ++i) {
                    double d0 = this.random.nextGaussian() * 0.02D;
                    double d1 = this.random.nextGaussian() * 0.02D;
                    double d2 = this.random.nextGaussian() * 0.02D;
                    this.level().addParticle(net.minecraft.core.particles.ParticleTypes.HEART,
                            this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d0, d1, d2);
                }
            }
            // IMPORTANT: Don't call super for event 7 either - sitting is explicitly handled in mobInteract
        } else {
            // Call super for all other entity events (NOT 6 or 7)
            super.handleEntityEvent(eventId);
        }
    }
}
