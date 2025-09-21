package com.leon.saintsdragons.server.entity.dragons.amphithere;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.ai.goals.amphithere.AmphithereFlightGoal;
import com.leon.saintsdragons.server.ai.goals.amphithere.AmphithereGroundWanderGoal;
import com.leon.saintsdragons.server.ai.navigation.DragonFlightMoveHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.controller.amphithere.AmphithereRiderController;
import com.leon.saintsdragons.server.entity.dragons.amphithere.handlers.AmphithereAnimationHandler;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
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

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final AmphithereAnimationHandler animationHandler = new AmphithereAnimationHandler(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final AmphithereRiderController riderController;

    private final GroundPathNavigation groundNav;
    private final FlyingPathNavigation airNav;
    private boolean usingAirNav;

    private Vec3 currentFlightTarget;
    private int targetCooldown;
    private int airTicks;
    private int groundTicks;

    private float bankSmoothedYaw = 0f;
    private int bankHoldTicks = 0;
    private int bankDir = 0;

    private float pitchSmoothedPitch = 0f;
    private int pitchHoldTicks = 0;
    private int pitchDir = 0;

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

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.FLYING_SPEED, 0.35D)
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
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1D, 10.0F, 3.0F, false));
        this.goalSelector.addGoal(6, new AmphithereGroundWanderGoal(this, 0.6D, 160));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            if (isFlying()) {
                airTicks++;
                groundTicks = 0;
                this.fallDistance = 0.0F;
            } else {
                groundTicks++;
                airTicks = 0;
            }

            if (isFlying() && this.onGround()) {
                setLanding(true);
                setFlying(false);
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

        if (!level().isClientSide && targetCooldown > 0) {
            targetCooldown--;
        }
    }

    private void tickBankingLogic() {
        // Reset banking when not flying or when controls are locked - INSTANT reset
        if (!isFlying() || isLanding() || isHovering() || isOrderedToSit()) {
            if (bankDir != 0) {
                bankDir = 0;
                bankSmoothedYaw = 0f;
                bankHoldTicks = 0;
            }
            return;
        }
        
        // Exponential smoothing to avoid jitter
        float yawChange = getYRot() - yRotO;
        bankSmoothedYaw = bankSmoothedYaw * 0.85f + yawChange * 0.15f;
        float enter = 1.0f;  // Less sensitive than before for smoother banking
        float exit = 5.0f;   // Larger exit threshold for more stable straight flight

        int desiredDir = bankDir;
        if (bankSmoothedYaw > enter) desiredDir = 1;
        else if (bankSmoothedYaw < -enter) desiredDir = -1;
        else if (Math.abs(bankSmoothedYaw) < exit) desiredDir = 0;  // banking_off when flying straight
        
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
            bankHoldTicks = Math.min(bankHoldTicks + 1, 10);  // Reduced max from 20 to 10
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
            // AI-controlled pitching based on movement
            float pitchChange = getXRot() - xRotO;
            pitchSmoothedPitch = pitchSmoothedPitch * 0.85f + pitchChange * 0.15f;

            // Hysteresis thresholds - tighter for more responsive straight flight
            float enter = 2.0f;  // More sensitive than Lightning Dragon for glider behavior
            float exit = 2.0f;

            if (pitchSmoothedPitch > enter) desiredDir = 1;
            else if (pitchSmoothedPitch < -enter) desiredDir = -1;
            else if (Math.abs(pitchSmoothedPitch) < exit) desiredDir = 0;  // pitching_off when flying straight
        }

        // Faster reset to off state (reduced hold time)
        if (desiredDir != pitchDir) {
            // If transitioning to "off" (0), use shorter hold time for faster reset
            int holdTime = (desiredDir == 0) ? 1 : 2;
            if (pitchHoldTicks >= holdTime) {
                pitchDir = desiredDir;
                pitchHoldTicks = 0;
            } else {
                pitchHoldTicks++;
            }
        } else {
            pitchHoldTicks = Math.min(pitchHoldTicks + 1, 10);  // Reduced max from 20 to 10
        }
    }

    public int getBankDirection() {
        return bankDir;
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
    
    public int getSyncedFlightMode() { 
        return this.entityData.get(DATA_FLIGHT_MODE); 
    }
    
    public int getEffectiveGroundState() {
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
        
        // Update flight mode
        int flightMode = -1; // not flying
        if (isFlying()) {
            if (isGoingDown()) {
                flightMode = 0; // glide
            } else if (isGoingUp()) {
                flightMode = 1; // flap
            } else {
                flightMode = 0; // glide
            }
        }
        
        // Update entity data
        if (this.entityData.get(DATA_GROUND_MOVE_STATE) != moveState) {
            this.entityData.set(DATA_GROUND_MOVE_STATE, moveState);
        }
        
        if (this.entityData.get(DATA_FLIGHT_MODE) != flightMode) {
            this.entityData.set(DATA_FLIGHT_MODE, flightMode);
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
            this.moveControl = new DragonFlightMoveHelper(this);
            this.usingAirNav = true;
        }
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
    }

    // ===== FlyingAnimal =====
    @Override
    public boolean isFlapping() {
        return isFlying() && this.getDeltaMovement().y > -0.1D;
    }
}