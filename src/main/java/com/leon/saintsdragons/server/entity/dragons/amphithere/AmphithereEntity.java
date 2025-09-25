package com.leon.saintsdragons.server.entity.dragons.amphithere;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.common.registry.amphithere.AmphithereAbilities;
import com.leon.saintsdragons.server.ai.goals.amphithere.AmphithereFlightGoal;
import com.leon.saintsdragons.server.ai.goals.amphithere.AmphithereFollowOwnerGoal;
import com.leon.saintsdragons.server.ai.goals.amphithere.AmphithereGroundWanderGoal;
import com.leon.saintsdragons.server.ai.navigation.DragonFlightMoveHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.controller.amphithere.AmphithereRiderController;
import com.leon.saintsdragons.server.entity.dragons.amphithere.handlers.AmphithereAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.amphithere.handlers.AmphithereInteractionHandler;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.common.network.DragonAnimTickets;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.particles.ParticleTypes;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
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
    private static final EntityDataAccessor<Boolean> DATA_ACCELERATING =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_RIDER_FORWARD =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIDER_STRAFE =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_FIRE_BREATHING =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.BOOLEAN);
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
    private boolean wasVehicleLastTick;
    private boolean fireBodyCrashArmed;

    private float bankSmoothedYaw = 0f;
    private int bankDir = 0;
    private float bankTransitionProgress = 0f; // 0.0 to 1.0 for smooth transitions

    private float pitchSmoothedPitch = 0f;
    private int pitchHoldTicks = 0;
    private int pitchDir = 0;

    // ===== Client animation overrides (for robust observer sync) =====

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
                .add(Attributes.FLYING_SPEED, 0.60D) // Slower for glider behavior
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
        this.entityData.define(DATA_ACCELERATING, false);
        this.entityData.define(DATA_RIDER_FORWARD, 0f);
        this.entityData.define(DATA_RIDER_STRAFE, 0f);
        this.entityData.define(DATA_FIRE_BREATHING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new AmphithereFlightGoal(this));
        this.goalSelector.addGoal(4, new AmphithereFollowOwnerGoal(this));
        this.goalSelector.addGoal(6, new AmphithereGroundWanderGoal(this, 0.6D, 160));

        this.targetSelector.addGoal(1, new com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtTargetGoal(this));
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

        combatManager.tick();

        tickBankingLogic();
        tickPitchingLogic();
        tickRiderTakeoff();
        tickMountedState();

        if (!level().isClientSide) {
            if (targetCooldown > 0) {
                targetCooldown--;
            }
            handleFireBodyCrash();
        }

        // Initialize animation state on first tick after loading to prevent thrashing
        if (!level().isClientSide && this.tickCount == 1) {
            initializeAnimationState();
        }
    }


    private void tickMountedState() {
        boolean mounted = this.isVehicle();

        if (level().isClientSide) {
            wasVehicleLastTick = mounted;
            return;
        }

        if (mounted) {
            if (!wasVehicleLastTick) {
                clearStatesWhenMounted();
            }

            if (this.isOrderedToSit()) {
                this.setOrderedToSit(false);
                if (this.getCommand() == 1) {
                    this.setCommand(0);
                }
            }
        } else if (wasVehicleLastTick) {
            this.setBreathingFire(false);
            combatManager.forceEndActiveAbility();
        }

        wasVehicleLastTick = mounted;
    }

    private void clearStatesWhenMounted() {
        if (level().isClientSide || !this.isVehicle()) {
            return;
        }

        setRunning(false);
        setGoingUp(false);
        setGoingDown(false);
        setHovering(false);
        setLanding(false);
        setTakeoff(false);
        setAccelerating(false);
        this.riderTakeoffTicks = 0;

        if (!isFlying()) {
            airTicks = 0;
        }

        if (this.getNavigation().getPath() != null) {
            this.getNavigation().stop();
        }

        if (!isFlying() && usingAirNav) {
            switchToGroundNavigation();
        }

        this.entityData.set(DATA_GROUND_MOVE_STATE, 0);
        this.entityData.set(DATA_FLIGHT_MODE, -1);
        this.entityData.set(DATA_RIDER_FORWARD, 0f);
        this.entityData.set(DATA_RIDER_STRAFE, 0f);
        this.syncAnimState(0, -1);

        this.setTarget(null);
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
                this.syncAnimState(s, getSyncedFlightMode());
            }
        }
    }

    public int getSyncedFlightMode() {
        return this.entityData.get(DATA_FLIGHT_MODE);
    }

    public boolean isAccelerating() { return this.entityData.get(DATA_ACCELERATING); }
    public void setAccelerating(boolean accelerating) { this.entityData.set(DATA_ACCELERATING, accelerating); }
    
    // Rider input snapshots for server-side animation sync
    public void setLastRiderForward(float forward) { this.entityData.set(DATA_RIDER_FORWARD, forward); }
    public void setLastRiderStrafe(float strafe) { this.entityData.set(DATA_RIDER_STRAFE, strafe); }
    
    /**
     * Initialize animation state after entity loading to prevent thrashing
     */
    public void initializeAnimationState() {
        if (!level().isClientSide) {
            // Reset all tick counters to ensure clean state
            groundTicks = 0;
            airTicks = 0;
            landingTicks = 0;
            
            // Set initial state based on current entity state
            int initialGroundState = 0; // Default to idle
            int initialFlightMode = -1; // Default to ground state
            
            if (!isFlying() && !isTakeoff() && !isLanding() && !isHovering()) {
                // Check if entity is actually moving
                double velSqr = this.getDeltaMovement().horizontalDistanceSqr();
                final double WALK_MIN = 0.0008;
                final double RUN_MIN = 0.0200;
                
                if (velSqr > RUN_MIN) {
                    initialGroundState = 2; // running
                } else if (velSqr > WALK_MIN) {
                    initialGroundState = 1; // walking
                }
            } else if (isFlying()) {
                initialFlightMode = getFlightMode();
            }
            
            // Set the initial state without triggering sync (to avoid thrashing)
            this.entityData.set(DATA_GROUND_MOVE_STATE, initialGroundState);
            this.entityData.set(DATA_FLIGHT_MODE, initialFlightMode);
        }
    }
    
    /**
     * Force reset animation state to prevent thrashing issues
     */
    public void resetAnimationState() {
        if (!level().isClientSide) {
            // Recalculate current state based on actual entity state
            int currentGroundState = 0; // Default to idle
            
            if (!isFlying() && !isTakeoff() && !isLanding() && !isHovering()) {
                // Recalculate ground movement state based on current velocity
                double velSqr = this.getDeltaMovement().horizontalDistanceSqr();
                final double WALK_MIN = 0.0008;
                final double RUN_MIN = 0.0200;
                
                if (velSqr > RUN_MIN) {
                    currentGroundState = 2; // running
                } else if (velSqr > WALK_MIN) {
                    currentGroundState = 1; // walking
                }
            }
            
            int currentFlightMode = getFlightMode();
            
            // Update entity data to match calculated state
            this.entityData.set(DATA_GROUND_MOVE_STATE, currentGroundState);
            this.entityData.set(DATA_FLIGHT_MODE, currentFlightMode);
            
            // Force sync current state
            this.syncAnimState(currentGroundState, currentFlightMode);
        }
    }

    // ===== Client animation overrides (for robust observer sync) =====
    
    public int getEffectiveGroundState() {
        Integer state = this.getAnimData(DragonAnimTickets.GROUND_STATE);
        if (state != null) {
            return state;
        }
        return this.entityData.get(DATA_GROUND_MOVE_STATE);
    }
    
    private void tickAnimationStates() {
        // Update ground movement state with more sophisticated detection
        int moveState = 0; // idle
        
        if (!isFlying() && !isTakeoff() && !isLanding() && !isHovering()) {
            // If being ridden, prefer rider inputs for robust state selection
            if (getControllingPassenger() != null) {
                float fwd = this.entityData.get(DATA_RIDER_FORWARD);
                float str = this.entityData.get(DATA_RIDER_STRAFE);
                float mag = Math.abs(fwd) + Math.abs(str);
                if (mag > 0.05f) {
                    moveState = this.isAccelerating() ? 2 : 1;
                } else {
                    // Fallback while ridden: use actual velocity so observers still see walk/run
                    double speedSqr = getDeltaMovement().horizontalDistanceSqr();
                    if (speedSqr > 0.08) {
                        moveState = 2; // running-level velocity
                    } else if (speedSqr > 0.005) {
                        moveState = 1; // walking-level velocity
                    }
                }
            } else {
                // Use horizontal velocity (matches HUD vel2) for AI classification to avoid position delta spikes
                double velSqr = this.getDeltaMovement().horizontalDistanceSqr();
                
                // Thresholds tuned so typical AI follow (≈0.0054) is walk, not run
                final double WALK_MIN = 0.0008;
                final double RUN_MIN = 0.0200;
                
                if (velSqr > RUN_MIN) {
                    moveState = 2; // running
                } else if (velSqr > WALK_MIN) {
                    moveState = 1; // walking
                }
            }
        }
        
        // Update flight mode - match Lightning Dragon's logic
        int flightMode = getFlightMode();

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
            this.syncAnimState(moveState, flightMode);
        }
        
        // Decay rider inputs slightly each tick to avoid sticking when packets drop
        if (this.entityData.get(DATA_RIDER_FORWARD) != 0f || this.entityData.get(DATA_RIDER_STRAFE) != 0f) {
            float nf = this.entityData.get(DATA_RIDER_FORWARD) * 0.8f;
            float ns = this.entityData.get(DATA_RIDER_STRAFE) * 0.8f;
            if (Math.abs(nf) < 0.01f) nf = 0f;
            if (Math.abs(ns) < 0.01f) ns = 0f;
            this.entityData.set(DATA_RIDER_FORWARD, nf);
            this.entityData.set(DATA_RIDER_STRAFE, ns);
        }
        
        // Stop running if not moving
        if (this.isRunning() && this.getDeltaMovement().horizontalDistanceSqr() < 0.01) {
            this.setRunning(false);
        }
    }

    private int getFlightMode() {
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
        return flightMode;
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
        Vec3 input = riderController.getRiddenInput(player, deltaIn);
        
        // Capture rider inputs for animation state (like Lightning Dragon)
        if (!level().isClientSide && !isFlying()) {
            float fwd = (float) Mth.clamp(input.z, -1.0, 1.0);
            float str = (float) Mth.clamp(input.x, -1.0, 1.0);
            this.entityData.set(DATA_RIDER_FORWARD, Math.abs(fwd) > 0.02f ? fwd : 0f);
            this.entityData.set(DATA_RIDER_STRAFE, Math.abs(str) > 0.02f ? str : 0f);
        }
        
        return input;
    }
    
    @Override
    public void travel(@NotNull Vec3 motion) {
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
            super.travel(motion);
        }
    }
    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
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

    private void handleFireBodyCrash() {
        boolean fireActive = this.isBreathingFire();
        boolean airborne = !this.onGround();
        LivingEntity rider = this.getControllingPassenger();
        if (fireActive && rider != null) {
            rider.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 10, 0, true, false, false));
        }
        if (fireActive && airborne) {
            fireBodyCrashArmed = true;
        }
        if (fireBodyCrashArmed && !airborne && fireActive) {
            triggerFireBodyCrash();
            fireBodyCrashArmed = false;
        }
        if (!fireActive && !airborne) {
            fireBodyCrashArmed = false;
        }
    }

    private void triggerFireBodyCrash() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        List<Entity> immune = new ArrayList<>(this.getPassengers());
        
        // Give passengers explosion resistance before the explosion
        for (Entity passenger : immune) {
            if (passenger instanceof LivingEntity livingPassenger) {
                livingPassenger.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 4, true, false, false));
            }
        }
        
        Explosion explosion = new Explosion(server, this, server.damageSources().explosion(this, this), null,
                x, y + 0.2D, z, 6.0F, true, Explosion.BlockInteraction.DESTROY);
        explosion.explode();
        explosion.finalizeExplosion(true);

        server.sendParticles(ParticleTypes.FLAME, x, y + 0.8D, z, 150, 2.0D, 1.0D, 2.0D, 0.2D);
        server.sendParticles(ParticleTypes.SMALL_FLAME, x, y + 0.5D, z, 120, 1.8D, 0.8D, 1.8D, 0.15D);
        server.sendParticles(ParticleTypes.LAVA, x, y + 0.5D, z, 40, 1.3D, 0.6D, 1.3D, 0.12D);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.5D, z, 80, 2.2D, 0.7D, 2.2D, 0.05D);

        BlockPos.MutableBlockPos flamePos = new BlockPos.MutableBlockPos();
        int baseY = this.getBlockY();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (this.getRandom().nextFloat() > 0.45F) {
                    continue;
                }
                flamePos.set(x + dx, baseY, z + dz);
                if (!server.isLoaded(flamePos)) {
                    continue;
                }
                if (!server.getBlockState(flamePos).isAir()) {
                    continue;
                }
                BlockState belowState = server.getBlockState(flamePos.below());
                if (!belowState.isAir() && Blocks.FIRE.defaultBlockState().canSurvive(server, flamePos)) {
                    server.setBlock(flamePos, Blocks.FIRE.defaultBlockState(), 11);
                }
            }
        }
        this.forceEndActiveAbility();
    }
    public void switchToGroundNavigation() {
        if (usingAirNav) {
            this.navigation = this.groundNav;
            this.moveControl = new MoveControl(this);
            this.usingAirNav = false;
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    public boolean isBreathingFire() {
        return this.entityData.get(DATA_FIRE_BREATHING);
    }

    public void setBreathingFire(boolean breathing) {
        this.entityData.set(DATA_FIRE_BREATHING, breathing);
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
        return AmphithereAbilities.FIRE_BODY;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends DragonEntity> DragonAbility<T> getActiveAbility() {
        return (DragonAbility<T>) combatManager.getActiveAbility();
    }

    public void useRidingAbility(String abilityName) {
        if (abilityName == null || abilityName.isEmpty()) {
            return;
        }
        Entity rider = this.getControllingPassenger();
        if (!(rider instanceof LivingEntity)) {
            return;
        }
        if (this.isTame() && rider instanceof Player player && !this.isOwnedBy(player)) {
            return;
        }
        DragonAbilityType<?, ?> type = AbilityRegistry.get(abilityName);
        if (type == AmphithereAbilities.FIRE_BODY) {
            combatManager.tryUseAbility(AmphithereAbilities.FIRE_BODY);
        }
    }

    public void forceEndActiveAbility() {
        combatManager.forceEndActiveAbility();
        fireBodyCrashArmed = false;
        this.setBreathingFire(false);
    }


    @Override
    public Vec3 getHeadPosition() {
        return this.getEyePosition();
    }

    @Override
    public Vec3 getMouthPosition() {
        Vec3 eye = this.getEyePosition();
        Vec3 forward = Vec3.directionFromRotation(this.getXRot(), this.getYHeadRot()).normalize();
        return eye.add(forward.scale(0.9D));
    }

    @Override
    public boolean isFood(@Nonnull ItemStack stack) {
        return stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH) || stack.is(Items.CHICKEN);
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
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
        tag.putBoolean("Accelerating", isAccelerating());
        tag.putInt("GroundMoveState", getGroundMoveState());
        tag.putInt("FlightMode", getSyncedFlightMode());
        tag.putFloat("RiderForward", this.entityData.get(DATA_RIDER_FORWARD));
        tag.putFloat("RiderStrafe", this.entityData.get(DATA_RIDER_STRAFE));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        boolean savedFlying = tag.getBoolean("Flying");
        boolean savedLanding = tag.getBoolean("Landing");
        boolean savedTakeoff = tag.getBoolean("Takeoff");
        boolean savedHovering = tag.getBoolean("Hovering");
        boolean savedGoingUp = tag.getBoolean("GoingUp");
        boolean savedGoingDown = tag.getBoolean("GoingDown");
        boolean savedRunning = tag.getBoolean("Running");
        boolean savedAccelerating = tag.getBoolean("Accelerating");
        int savedGroundState = tag.contains("GroundMoveState") ? tag.getInt("GroundMoveState") : 0;
        int savedFlightMode = tag.contains("FlightMode") ? tag.getInt("FlightMode") : -1;
        float savedRiderForward = tag.contains("RiderForward") ? tag.getFloat("RiderForward") : 0f;
        float savedRiderStrafe = tag.contains("RiderStrafe") ? tag.getFloat("RiderStrafe") : 0f;

        setFlying(savedFlying);
        setLanding(savedLanding);
        setTakeoff(savedFlying && savedTakeoff && !savedLanding);
        setHovering(savedFlying && savedHovering && !savedLanding);
        setGoingUp(savedFlying && savedGoingUp);
        setGoingDown(savedFlying && savedGoingDown);
        setRunning(savedRunning);
        setAccelerating(savedAccelerating);

        this.entityData.set(DATA_GROUND_MOVE_STATE, Mth.clamp(savedGroundState, 0, 2));
        this.entityData.set(DATA_FLIGHT_MODE, savedFlying ? Mth.clamp(savedFlightMode, -1, 3) : -1);
        this.entityData.set(DATA_RIDER_FORWARD, savedRiderForward);
        this.entityData.set(DATA_RIDER_STRAFE, savedRiderStrafe);

        // Reset all tick counters to prevent state inconsistencies
        if (!savedFlying) {
            landingTicks = 0;
            airTicks = 0;
            groundTicks = 0; // Reset ground ticks on load
        } else {
            airTicks = Math.max(airTicks, 1);
            groundTicks = 0; // Reset ground ticks when flying
        }

        this.setNoGravity(isFlying() || isHovering());
        
        // Force animation state sync after loading to prevent thrashing
        if (!level().isClientSide) {
            // Delay the sync slightly to ensure all systems are initialized
            this.tickCount = 0; // Reset tick counter to ensure proper initialization
        }
    }

    // ===== DragonFlightCapable =====
    @Override
    public final boolean isFlying() {
        return isDragonFlying();
    }

    protected boolean isDragonFlying() {
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

    public void setRiderTakeoffTicks(int ticks) {
        this.riderTakeoffTicks = Math.max(0, ticks);
    }

    @Override
    protected void removePassenger(@Nonnull Entity passenger) {
        super.removePassenger(passenger);
        // Reset rider-driven movement states immediately on dismount
        if (!this.level().isClientSide) {
            this.setAccelerating(false);
            this.setRunning(false);
            this.entityData.set(DATA_RIDER_FORWARD, 0f);
            this.entityData.set(DATA_RIDER_STRAFE, 0f);
            this.entityData.set(DATA_GROUND_MOVE_STATE, 0);
            // Nudge observers so animation stops if we dismounted mid-run/walk
            this.syncAnimState(0, getSyncedFlightMode());
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float fallMultiplier, @NotNull DamageSource source) {
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
    
    /**
     * Check if this amphithere can be bound (not flying, not dying, etc.)
     */
    public boolean canBeBound() {
        return !isFlying() && !isDying() && !isAccelerating();
    }
}
