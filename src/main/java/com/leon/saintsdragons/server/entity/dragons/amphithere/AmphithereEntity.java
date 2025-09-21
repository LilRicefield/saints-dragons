package com.leon.saintsdragons.server.entity.dragons.amphithere;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.ai.goals.amphithere.AmphithereFlightGoal;
import com.leon.saintsdragons.server.ai.goals.amphithere.AmphithereFollowOwnerGoal;
import com.leon.saintsdragons.server.ai.goals.amphithere.AmphithereGroundWanderGoal;
import com.leon.saintsdragons.server.ai.navigation.DragonFlightMoveHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.controller.amphithere.AmphithereRiderController;
import com.leon.saintsdragons.server.entity.dragons.amphithere.handlers.AmphithereAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.amphithere.handlers.AmphithereInteractionHandler;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nonnull;

public class AmphithereEntity extends DragonEntity implements FlyingAnimal, DragonFlightCapable {
    private static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TAKEOFF =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HOVERING =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LANDING =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GOING_UP =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GOING_DOWN =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RUNNING =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FLIGHT_MODE =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.INT);
    private static final int LANDING_SETTLE_TICKS = 4;


    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final AmphithereAnimationHandler animationHandler = new AmphithereAnimationHandler(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final AmphithereRiderController riderController;
    private final AmphithereInteractionHandler interactionHandler = new AmphithereInteractionHandler(this);

    private final GroundPathNavigation groundNav;
    private final FlyingPathNavigation airNav;
    private boolean usingAirNav;

    private Vec3 currentFlightTarget;
    private int targetCooldown;
    private int airTicks;
    private int groundTicks;
    private int landingTicks;
    private int riderTakeoffTicks;

    private float bankSmoothedYaw = 0f;
    private int bankDir = 0;
    private float bankTransitionProgress = 0f; // 0.0 to 1.0 for smooth transitions

    private float pitchSmoothedPitch = 0f;
    private int pitchHoldTicks = 0;
    private int pitchDir = 0;

    // ===== Client animation overrides (for robust observer sync) =====
    private int clientGroundOverride = Integer.MIN_VALUE;
    private int clientFlightOverride = Integer.MIN_VALUE;
    private int clientOverrideExpiry = 0;

    public AmphithereEntity(EntityType<? extends AmphithereEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.1F);

        this.groundNav = new GroundPathNavigation(this, level);
        this.airNav = new FlyingPathNavigation(this, level) {
            @Override
            public boolean isStableDestination(@Nonnull BlockPos pos) {
                BlockState below = this.level.getBlockState(pos.below());
                return !below.isAir();
            }
        };
        this.airNav.setCanOpenDoors(false);
        this.airNav.setCanFloat(false);
        this.airNav.setCanPassDoors(false);

        this.navigation = groundNav;
        this.moveControl = new MoveControl(this);
        this.usingAirNav = false;
        this.riderController = new AmphithereRiderController(this);

        this.setPathfindingMalus(BlockPathTypes.LEAVES, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
    }

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType spawnType,
                                                 @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnData, dataTag);

        if (!this.isTame()) {
            this.setOwnerUUID(null);
            this.setCommand(2);
            this.setOrderedToSit(false);
        }

        return data;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.FLYING_SPEED, 0.20D) // Slower for glider behavior
                .add(Attributes.ARMOR, 4.0D);
    }

    public static boolean canSpawnHere(EntityType<? extends AmphithereEntity> type, LevelAccessor level, net.minecraft.world.entity.MobSpawnType spawnType, BlockPos pos, RandomSource rng) {
        return net.minecraft.world.entity.animal.Animal.checkAnimalSpawnRules(type, level, spawnType, pos, rng);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FLYING, false);
        this.entityData.define(DATA_TAKEOFF, false);
        this.entityData.define(DATA_HOVERING, false);
        this.entityData.define(DATA_LANDING, false);
        this.entityData.define(DATA_GOING_UP, false);
        this.entityData.define(DATA_GOING_DOWN, false);
        this.entityData.define(DATA_RUNNING, false);
        this.entityData.define(DATA_GROUND_MOVE_STATE, 0);
        this.entityData.define(DATA_FLIGHT_MODE, -1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new AmphithereFlightGoal(this));
        this.goalSelector.addGoal(4, new AmphithereFollowOwnerGoal(this));
        this.goalSelector.addGoal(6, new AmphithereGroundWanderGoal(this, 0.6D, 160));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            boolean onGroundNow = this.onGround() || this.isInWater();

            if (isFlying()) {
                airTicks++;
                groundTicks = 0;
                this.fallDistance = 0.0F;

                if (isTakeoff() && !onGroundNow && airTicks > 5) {
                    setTakeoff(false);
                }

                if (onGroundNow && !isTakeoff()) {
                    if (!isLanding()) {
                        setLanding(true);
                    }
                    setFlying(false);
                } else if (isLanding() && !onGroundNow) {
                    setLanding(false);
                }
            } else {
                groundTicks++;
                airTicks = 0;
            }

            if (isLanding()) {
                // Hold landing state briefly so the landing animation can finish before ground loops resume
                if (onGroundNow) {
                    landingTicks++;
                    if (landingTicks >= LANDING_SETTLE_TICKS) {
                        markLandedNow();
                    }
                } else {
                    landingTicks = 0;
                }
            } else {
                landingTicks = 0;
            }

            // Update animation states
            tickAnimationStates();
        }

        this.setNoGravity(isFlying() || isHovering());
        if (!isFlying() && usingAirNav) {
            switchToGroundNavigation();
        }
    }

    public void tick() {
        super.tick();

        tickBankingLogic();
        tickPitchingLogic();
        tickRiderTakeoff();

        if (!level().isClientSide && targetCooldown > 0) {
            targetCooldown--;
        }
    }

    private void tickRiderTakeoff() {
        if (!level().isClientSide && riderTakeoffTicks > 0) {
            riderTakeoffTicks--;
            Vec3 velocity = this.getDeltaMovement();
            double boost = this.isFlying() ? 0.08D : 0.12D;
            if (velocity.y < boost) {
                this.setDeltaMovement(velocity.x, boost, velocity.z);
            }
            this.hasImpulse = true;
        }
    }

    private void tickBankingLogic() {
        // Reset banking when not flying or when controls are locked - INSTANT reset
        if (!isFlying() || isLanding() || isHovering() || isOrderedToSit()) {
            if (bankDir != 0) {
                bankDir = 0;
                bankSmoothedYaw = 0f;
            }
            return;
        }

        float yawChange = getYRot() - yRotO;
        bankSmoothedYaw = bankSmoothedYaw * 0.85f + yawChange * 0.15f;
        float enter = 0.9f;
        float exit = 3.5f;

        int desiredDir = bankDir;
        if (bankSmoothedYaw > enter) desiredDir = 1;
        else if (bankSmoothedYaw < -enter) desiredDir = -1;
        else if (Math.abs(bankSmoothedYaw) < exit) desiredDir = 0;

        if (desiredDir != bankDir) {
            bankDir = desiredDir;
            if (bankDir != 0) {
                bankTransitionProgress = Math.min(bankTransitionProgress, 0.25f);
            }
        }

        if (bankDir != 0) {
            bankTransitionProgress = Math.min(bankTransitionProgress + 0.18f, 1.0f);
        } else {
            bankTransitionProgress = Math.max(bankTransitionProgress - 0.20f, 0.0f);
        }
    }

    private void tickPitchingLogic() {
        // Reset pitching when not flying or when controls are locked - INSTANT reset
        if (!isFlying() || isLanding() || isHovering() || isOrderedToSit()) {
            if (pitchDir != 0) {
                pitchDir = 0;
                pitchSmoothedPitch = 0f;
                pitchHoldTicks = 0;
            }
            return;
        }
        
        int desiredDir = pitchDir;

        if (this.isVehicle() && this.getControllingPassenger() instanceof Player) {
            // Handle rider input for pitching
            if (isGoingUp()) {
                desiredDir = -1;  // Pitching up
            } else if (isGoingDown()) {
                desiredDir = 1;   // Pitching down
            } else {
                desiredDir = 0;   // No pitching
            }
        } else {
            // AI-controlled pitching based on movement - slower for glider
            float pitchChange = getXRot() - xRotO;
            pitchSmoothedPitch = pitchSmoothedPitch * 0.90f + pitchChange * 0.10f; // Slower smoothing

            // Hysteresis thresholds - less sensitive for smoother glider behavior
            float enter = 4.0f;  // Less sensitive for smoother pitching
            float exit = 6.0f;   // Larger exit threshold for stability

            if (pitchSmoothedPitch > enter) desiredDir = 1;
            else if (pitchSmoothedPitch < -enter) desiredDir = -1;
            else if (Math.abs(pitchSmoothedPitch) < exit) desiredDir = 0;  // pitching_off when flying straight
        }

        // Smoother transitions for glider behavior
        if (desiredDir != pitchDir) {
            // Longer hold times for smoother pitching transitions
            int holdTime = (desiredDir == 0) ? 4 : 6; // Longer hold for smoother pitching
            if (pitchHoldTicks >= holdTime) {
                pitchDir = desiredDir;
                pitchHoldTicks = 0;
            } else {
                pitchHoldTicks++;
            }
        } else {
            pitchHoldTicks = Math.min(pitchHoldTicks + 1, 15);  // Increased max for stability
        }
    }

    public int getBankDirection() {
        return bankDir;
    }
    
    public float getBankTransitionProgress() {
        return bankTransitionProgress;
    }
    
    public float getSmoothBankDirection() {
        // Return a smooth value between -1.0 and 1.0 for banking
        if (bankDir == 0) return 0f;
        return bankDir * bankTransitionProgress;
    }

    public int getPitchDirection() {
        return pitchDir;
    }

    // ===== Rider Control Methods =====
    public boolean isGoingUp() { 
        return this.entityData.get(DATA_GOING_UP); 
    }
    
    public void setGoingUp(boolean goingUp) { 
        this.entityData.set(DATA_GOING_UP, goingUp); 
    }
    
    public boolean isGoingDown() { 
        return this.entityData.get(DATA_GOING_DOWN); 
    }
    
    public void setGoingDown(boolean goingDown) { 
        this.entityData.set(DATA_GOING_DOWN, goingDown); 
    }

    // ===== Animation State Methods =====
    
    public boolean isRunning() { 
        if (level().isClientSide) {
            int s = getEffectiveGroundState();
            return s == 2; // running state
        }
        int s = this.entityData.get(DATA_GROUND_MOVE_STATE);
        return s == 2; // running state
    }
    
    public void setRunning(boolean running) { 
        this.entityData.set(DATA_RUNNING, running); 
    }
    
    public boolean isWalking() {
        if (level().isClientSide) {
            int s = getEffectiveGroundState();
            return s == 1; // walking state
        }
        int s = this.entityData.get(DATA_GROUND_MOVE_STATE);
        return s == 1; // walking state
    }
    
    public int getGroundMoveState() {
        return this.entityData.get(DATA_GROUND_MOVE_STATE);
    }

    public void setGroundMoveStateFromAI(int state) {
        if (!this.level().isClientSide) {
            int s = Mth.clamp(state, 0, 2);
            if (this.entityData.get(DATA_GROUND_MOVE_STATE) != s) {
                this.entityData.set(DATA_GROUND_MOVE_STATE, s);
            }
        }
    }

    public int getSyncedFlightMode() {
        return this.entityData.get(DATA_FLIGHT_MODE);
    }

    // ===== Client animation overrides (for robust observer sync) =====
    public void applyClientAnimState(int groundState, int flightMode) {
        this.clientGroundOverride = groundState;
        this.clientFlightOverride = flightMode;
        this.clientOverrideExpiry = this.tickCount + 40; // Expire after 2 seconds
    }
    
    public int getEffectiveGroundState() {
        if (level().isClientSide && clientGroundOverride != Integer.MIN_VALUE && tickCount < clientOverrideExpiry) {
            return clientGroundOverride;
        }
        // Clear expired overrides
        if (level().isClientSide && tickCount >= clientOverrideExpiry) {
            clientGroundOverride = Integer.MIN_VALUE;
        }
        if (level().isClientSide) {
            // Client-side calculation based on movement with refined thresholds
            double velSqr = this.getDeltaMovement().horizontalDistanceSqr();
            final double WALK_MIN = 0.0008;
            final double RUN_MIN = 0.0200;
            
            if (velSqr > RUN_MIN) return 2; // running
            if (velSqr > WALK_MIN) return 1; // walking
            return 0; // idle
        }
        return this.entityData.get(DATA_GROUND_MOVE_STATE);
    }
    
    private void tickAnimationStates() {
        // Update ground movement state with more sophisticated detection
        int moveState = 0; // idle
        
        if (!isFlying() && !isTakeoff() && !isLanding() && !isHovering()) {
            // Ground movement with refined thresholds
            double velSqr = this.getDeltaMovement().horizontalDistanceSqr();
            
            // Use similar thresholds to LightningDragonEntity for consistency
            final double WALK_MIN = 0.0008;
            final double RUN_MIN = 0.0200;
            
            if (velSqr > RUN_MIN) {
                moveState = 2; // running
            } else if (velSqr > WALK_MIN) {
                moveState = 1; // walking
            }
        }
        
        // Update flight mode - match Lightning Dragon's logic
        int flightMode = -1; // not flying (ground state)
        if (isFlying()) {
            if (isTakeoff()) {
                flightMode = 3; // takeoff
            } else if (isHovering()) {
                flightMode = 2; // hover
            } else if (isGoingDown()) {
                flightMode = 0; // glide
            } else if (isGoingUp()) {
                flightMode = 1; // flap
            } else {
                // Default to glide for natural descent
                flightMode = 0; // glide
            }
        }
        
        // Update entity data and sync to clients
        boolean groundStateChanged = this.entityData.get(DATA_GROUND_MOVE_STATE) != moveState;
        boolean flightModeChanged = this.entityData.get(DATA_FLIGHT_MODE) != flightMode;
        
        if (groundStateChanged) {
            this.entityData.set(DATA_GROUND_MOVE_STATE, moveState);
        }
        
        if (flightModeChanged) {
            this.entityData.set(DATA_FLIGHT_MODE, flightMode);
        }
        
        // Send animation state sync to clients when states change
        if (groundStateChanged || flightModeChanged) {
            com.leon.saintsdragons.common.network.NetworkHandler.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                    new com.leon.saintsdragons.common.network.MessageDragonAnimState(this.getId(), (byte) moveState, (byte) flightMode)
            );
        }
        
        // Stop running if not moving
        if (this.isRunning() && this.getDeltaMovement().horizontalDistanceSqr() < 0.01) {
            this.setRunning(false);
        }
    }

    // ===== Riding System Methods =====
    
    public void requestRiderTakeoff() {
        riderController.requestRiderTakeoff();
    }
    
    public double getPassengersRidingOffset() {
        return riderController.getPassengersRidingOffset();
    }
    
    @Override
    protected void positionRider(@Nonnull Entity passenger, @Nonnull Entity.MoveFunction moveFunction) {
        riderController.positionRider(passenger, moveFunction);
    }
    
    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@Nonnull LivingEntity passenger) {
        return riderController.getDismountLocationForPassenger(passenger);
    }
    
    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return riderController.getControllingPassenger();
    }
    
    @Override
    protected float getRiddenSpeed(@Nonnull Player rider) {
        return riderController.getRiddenSpeed(rider);
    }
    
    @Override
    protected void tickRidden(@Nonnull Player player, @Nonnull Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        riderController.tickRidden(player, travelVector);
    }
    
    @Override
    protected @NotNull Vec3 getRiddenInput(@Nonnull Player player, @Nonnull Vec3 deltaIn) {
        return riderController.getRiddenInput(player, deltaIn);
    }
    
    @Override
    public void travel(Vec3 motion) {
        // Riding logic
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            // Clear any AI navigation when being ridden
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }

            if (isFlying()) {
                // Delegate flying movement to rider controller for consistency
                this.riderController.handleRiderMovement(player, motion);
            } else {
                // Ground movement - use vanilla system which calls getRiddenInput()
                super.travel(motion);
            }
        } else {
            // Normal AI movement
            if (isFlying()) {
                // AI flight movement - use existing flight controller
                super.travel(motion);
            } else {
                // AI ground movement
                super.travel(motion);
            }
        }
    }
    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
    }

    public boolean needsNewFlightTarget() {
        return currentFlightTarget == null || targetCooldown <= 0;
    }

    public void assignFlightTarget(Vec3 target) {
        this.currentFlightTarget = target;
        this.targetCooldown = 50 + this.getRandom().nextInt(40);
    }

    @Nullable
    public Vec3 getFlightTarget() {
        return currentFlightTarget;
    }

    public int getAirTicks() {
        return airTicks;
    }

    public int getGroundTicks() {
        return groundTicks;
    }

    public void switchToAirNavigation() {
        if (!usingAirNav) {
            this.navigation = this.airNav;
            this.moveControl = new DragonFlightMoveHelper(this, getGliderFlightParameters());
            this.usingAirNav = true;
        }
    }

    // Amphithere-specific flight parameters for glider behavior
    private DragonFlightMoveHelper.FlightParameters getGliderFlightParameters() {
        return new DragonFlightMoveHelper.FlightParameters(
            3.0F,    // maxYawChange - smoother turns for gradual banking
            5.0F,    // maxPitchChange - slower pitching for glider
            0.3F,    // speedFactorMin - lower minimum speed
            2.0F,    // speedFactorMax - lower maximum speed for glider
            0.08F,   // speedTransitionRate - slower transitions for glider
            0.15D,   // accelerationCap - lower acceleration cap for glider
            0.10D    // velocityBlendRate - gentler blend for glider
        );
    }

    public void switchToGroundNavigation() {
        if (usingAirNav) {
            this.navigation = this.groundNav;
            this.moveControl = new MoveControl(this);
            this.usingAirNav = false;
        }
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@Nonnull Level level) {
        return new GroundPathNavigation(this, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<AmphithereEntity> movement = new AnimationController<>(this, "movement", 5, animationHandler::handleMovementAnimation);
        movement.setSoundKeyframeHandler(this::onAnimationSound);
        controllers.add(movement);

        AnimationController<AmphithereEntity> banking = new AnimationController<>(this, "banking", 10, animationHandler::bankingPredicate);
        controllers.add(banking);

        AnimationController<AmphithereEntity> pitching = new AnimationController<>(this, "pitching", 10, animationHandler::pitchingPredicate);
        controllers.add(pitching);

        AnimationController<AmphithereEntity> actions = new AnimationController<>(this, "actions", 10, animationHandler::actionPredicate);
        animationHandler.setupActionController(actions);
        actions.setSoundKeyframeHandler(this::onAnimationSound);
        controllers.add(actions);
    }
    private void onAnimationSound(SoundKeyframeEvent<AmphithereEntity> event) {
        soundHandler.handleAnimationSound(this, event.getKeyframeData(), event.getController());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        return null;
    }

    @Override
    public Vec3 getHeadPosition() {
        return this.getEyePosition();
    }

    @Override
    public Vec3 getMouthPosition() {
        return this.position().add(0.0D, this.getBbHeight() * 0.8D, 0.0D);
    }

    @Override
    public boolean isFood(@Nonnull ItemStack stack) {
        return stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH) || stack.is(Items.CHICKEN);
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Use interaction handler for all interactions
        InteractionResult handlerResult = interactionHandler.handleInteraction(player, hand);
        if (handlerResult != InteractionResult.PASS) {
            return handlerResult;
        }

        // Fall back to base implementation for any unhandled interactions
        return super.mobInteract(player, hand);
    }


    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@Nonnull ServerLevel level, @Nonnull AgeableMob partner) {
        return ModEntities.AMPHITHERE.get().create(level);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Flying", isFlying());
        tag.putBoolean("Hovering", isHovering());
        tag.putBoolean("Landing", isLanding());
        tag.putBoolean("Takeoff", isTakeoff());
        tag.putBoolean("GoingUp", isGoingUp());
        tag.putBoolean("GoingDown", isGoingDown());
        tag.putBoolean("Running", isRunning());
        tag.putInt("GroundMoveState", getGroundMoveState());
        tag.putInt("FlightMode", getSyncedFlightMode());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Flying")) setFlying(tag.getBoolean("Flying"));
        if (tag.contains("Hovering")) setHovering(tag.getBoolean("Hovering"));
        if (tag.contains("Landing")) setLanding(tag.getBoolean("Landing"));
        if (tag.contains("Takeoff")) setTakeoff(tag.getBoolean("Takeoff"));
        if (tag.contains("GoingUp")) setGoingUp(tag.getBoolean("GoingUp"));
        if (tag.contains("GoingDown")) setGoingDown(tag.getBoolean("GoingDown"));
        if (tag.contains("Running")) setRunning(tag.getBoolean("Running"));
        if (tag.contains("GroundMoveState")) this.entityData.set(DATA_GROUND_MOVE_STATE, tag.getInt("GroundMoveState"));
        if (tag.contains("FlightMode")) this.entityData.set(DATA_FLIGHT_MODE, tag.getInt("FlightMode"));
    }

    // ===== DragonFlightCapable =====
    @Override
    public boolean isFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    @Override
    public void setFlying(boolean flying) {
        if (flying == isFlying()) {
            return;
        }
        this.entityData.set(DATA_FLYING, flying);
        if (flying) {
            switchToAirNavigation();
            setLanding(false);
            setTakeoff(true);
            this.getNavigation().stop();
        } else {
            switchToGroundNavigation();
            setHovering(false);
            setTakeoff(false);
        }
    }

    @Override
    public boolean isTakeoff() {
        return this.entityData.get(DATA_TAKEOFF);
    }

    @Override
    public void setTakeoff(boolean takeoff) {
        this.entityData.set(DATA_TAKEOFF, takeoff);
    }

    @Override
    public boolean isHovering() {
        return this.entityData.get(DATA_HOVERING);
    }

    @Override
    public void setHovering(boolean hovering) {
        this.entityData.set(DATA_HOVERING, hovering);
    }

    @Override
    public boolean isLanding() {
        return this.entityData.get(DATA_LANDING);
    }

    @Override
    public void setLanding(boolean landing) {
        this.entityData.set(DATA_LANDING, landing);
        landingTicks = 0;
        if (landing) {
            setHovering(false);
            setTakeoff(false);
        }
    }

    @Override
    public float getFlightSpeed() {
        return 0.5F;
    }

    @Override
    public double getPreferredFlightAltitude() {
        double base = 24.0D + this.getRandom().nextDouble() * 10.0D;
        return Mth.clamp(base, 20.0D, 45.0D);
    }

    @Override
    public boolean canTakeoff() {
        return !this.isBaby() && !this.isOrderedToSit() && this.isAlive() && (this.onGround() || this.isInWater());
    }

    @Override
    public void markLandedNow() {
        setLanding(false);
        setTakeoff(false);
        this.riderTakeoffTicks = 0;
    }

    public int getRiderTakeoffTicks() {
        return riderTakeoffTicks;
    }

    public void setRiderTakeoffTicks(int ticks) {
        this.riderTakeoffTicks = Math.max(0, ticks);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float fallMultiplier, DamageSource source) {
        if (this.isFlying() || this.isTakeoff() || this.isLanding()) {
            return false;
        }
        return super.causeFallDamage(fallDistance, fallMultiplier, source);
    }

    // ===== FlyingAnimal =====
    @Override
    public boolean isFlapping() {
        return isFlying() && this.getDeltaMovement().y > -0.1D;
    }
}
