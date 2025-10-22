package com.leon.saintsdragons.server.entity.dragons.cindervane;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.cindervane.CindervaneAbilities;
import com.leon.saintsdragons.server.ai.goals.cindervane.CindervaneCombatGoal;
import com.leon.saintsdragons.server.ai.goals.cindervane.CindervaneFlightGoal;
import com.leon.saintsdragons.server.ai.goals.cindervane.CindervaneFollowOwnerGoal;
import com.leon.saintsdragons.server.ai.goals.cindervane.CindervaneGroundWanderGoal;
import com.leon.saintsdragons.server.ai.navigation.DragonFlightMoveHelper;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.controller.cindervane.CindervaneRiderController;
import com.leon.saintsdragons.server.entity.dragons.cindervane.handlers.CinderAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.cindervane.handlers.CinderInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.cindervane.handlers.CinderSoundProfile;
import java.util.Map;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.base.RideableDragonData;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.interfaces.SoundHandledDragon;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.particles.ParticleTypes;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Optional;
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
import net.minecraft.world.damagesource.DamageTypes;
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
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.util.GeckoLibUtil;
import javax.annotation.Nonnull;

public class Cindervane extends RideableDragonBase implements DragonFlightCapable, SoundHandledDragon {
    // Note: DATA_FIRE_BREATHING will be defined in defineSynchedData() using a unique ID
    private static final int LANDING_SETTLE_TICKS = 4;
    private static final double FIRE_BODY_CRASH_MIN_DROP = 7.0D;
    private static final float FIRE_BODY_EXPLOSION_RADIUS = 15.0F;
    private static final double FIRE_BODY_IMPRINT_RADIUS = 9.0D;
    private static final double FIRE_BODY_IMPRINT_DEPTH_FACTOR = 0.6D;
    private static final float FIRE_BODY_EXPLOSION_DAMAGE = 200.0F;


    private static final Map<String, VocalEntry> VOCAL_ENTRIES =
        new VocalEntryBuilder()
            .add("roar", "actions", "animation.amphithere.roar", ModSounds.AMPHITHERE_ROAR, 1.5f, 0.95f, 0.1f, false, false, false)
            .add("roar_ground", "actions", "animation.amphithere.roar_ground", ModSounds.AMPHITHERE_ROAR, 1.5f, 0.9f, 0.05f, false, false, false)
            .add("roar_air", "actions", "animation.amphithere.roar_air", ModSounds.AMPHITHERE_ROAR, 1.5f, 1.05f, 0.05f, false, false, false)
            .add("amphithere_hurt", "actions", "animation.amphithere.hurt", ModSounds.AMPHITHERE_HURT, 1.2f, 0.95f, 0.1f, false, true, true)
            .add("amphithere_die", "actions", "animation.amphithere.die", ModSounds.AMPHITHERE_DIE, 1.5f, 1.0f, 0.0f, false, true, true)
            .build();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final CinderAnimationHandler animationHandler = new CinderAnimationHandler(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final CinderInteractionHandler interactionHandler = new CinderInteractionHandler(this);
    private final CindervaneRiderController riderController;

    private final DragonPathNavigateGround groundNav;
    private final FlyingPathNavigation airNav;
    private boolean usingAirNav;

    private int targetCooldown;
    private int airTicks;
    public int groundTicks;
    private int landingTicks;
    private int riderTakeoffTicks;
    private boolean wasVehicleLastTick;
    private boolean fireBodyCrashArmed;
    private double fireBodyCrashMaxHeight;

    // Banking smoothing state (mouse-sensitivity based, like Raevyx)
    private float bankSmoothedYaw = 0f;
    private int bankHoldTicks = 0;
    private int bankDir = 0; // -1 left, 0 none, 1 right
    private float bankAngle = 0f;
    private float prevBankAngle = 0f;

    private float pitchSmoothedPitch = 0f;
    private int pitchHoldTicks = 0;
    private int pitchDir = 0;

    private static final double MODEL_SCALE = 1.0D;

    // ===== ALTITUDE-BASED FLYING SYSTEM (like Raevyx) =====
    private static final double RIDER_GLIDE_ALTITUDE_THRESHOLD = 40.0D;
    private static final double RIDER_GLIDE_ALTITUDE_EXIT = 30.0D; // Hysteresis: exit at lower altitude
    private boolean inHighAltitudeGlide = false; // Track glide state for smooth transitions

    // ===== CLIENT LOCATOR CACHE (client-side only) =====
    private final Map<String, Vec3> clientLocatorCache = new java.util.concurrent.ConcurrentHashMap<>();

    // ===== Client animation overrides (for robust observer sync) =====

    public Cindervane(EntityType<? extends Cindervane> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.1F);

        this.groundNav = new DragonPathNavigateGround(this, level);
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
        this.riderController = new CindervaneRiderController(this);

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

    public static boolean canSpawnHere(EntityType<? extends Cindervane> type,
                                       LevelAccessor level,
                                       MobSpawnType spawnType,
                                       BlockPos pos,
                                       RandomSource random) {
        if (!Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }

        // Reject waterlogged blocks or fluid directly around the spawn
        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }
        if (!level.getFluidState(pos.below()).isEmpty()) {
            return false;
        }

        // Optional: enforce a sturdy block underneath
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    // Amphithere-specific entity data accessors
    private static final EntityDataAccessor<Boolean> DATA_FIRE_BREATHING =
            SynchedEntityData.defineId(Cindervane.class, EntityDataSerializers.BOOLEAN);
    
    // Rideable dragon data accessors specific to AmphithereEntity
    private static final EntityDataAccessor<Boolean> DATA_FLYING = 
            RideableDragonData.createFlyingAccessor(Cindervane.class);
    private static final EntityDataAccessor<Boolean> DATA_TAKEOFF = 
            RideableDragonData.createTakeoffAccessor(Cindervane.class);
    private static final EntityDataAccessor<Boolean> DATA_HOVERING = 
            RideableDragonData.createHoveringAccessor(Cindervane.class);
    private static final EntityDataAccessor<Boolean> DATA_LANDING = 
            RideableDragonData.createLandingAccessor(Cindervane.class);
    private static final EntityDataAccessor<Boolean> DATA_RUNNING = 
            RideableDragonData.createRunningAccessor(Cindervane.class);
    private static final EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE = 
            RideableDragonData.createGroundMoveStateAccessor(Cindervane.class);
    private static final EntityDataAccessor<Integer> DATA_FLIGHT_MODE = 
            RideableDragonData.createFlightModeAccessor(Cindervane.class);
    private static final EntityDataAccessor<Float> DATA_RIDER_FORWARD = 
            RideableDragonData.createRiderForwardAccessor(Cindervane.class);
    private static final EntityDataAccessor<Float> DATA_RIDER_STRAFE = 
            RideableDragonData.createRiderStrafeAccessor(Cindervane.class);
    private static final EntityDataAccessor<Boolean> DATA_GOING_UP = 
            RideableDragonData.createGoingUpAccessor(Cindervane.class);
    private static final EntityDataAccessor<Boolean> DATA_GOING_DOWN = 
            RideableDragonData.createGoingDownAccessor(Cindervane.class);
    private static final EntityDataAccessor<Boolean> DATA_ACCELERATING = 
            RideableDragonData.createAcceleratingAccessor(Cindervane.class);
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        defineRideableDragonData();
        // Define Amphithere-specific data
        this.entityData.define(DATA_FIRE_BREATHING, false);
    }

    @Override
    protected void defineRideableDragonData() {
        // Define all rideable dragon data keys for AmphithereEntity
        this.entityData.define(DATA_FLYING, false);
        this.entityData.define(DATA_TAKEOFF, false);
        this.entityData.define(DATA_HOVERING, false);
        this.entityData.define(DATA_LANDING, false);
        this.entityData.define(DATA_RUNNING, false);
        this.entityData.define(DATA_GROUND_MOVE_STATE, 0);
        this.entityData.define(DATA_FLIGHT_MODE, -1);
        this.entityData.define(DATA_RIDER_FORWARD, 0f);
        this.entityData.define(DATA_RIDER_STRAFE, 0f);
        this.entityData.define(DATA_GOING_UP, false);
        this.entityData.define(DATA_GOING_DOWN, false);
        this.entityData.define(DATA_ACCELERATING, false);
    }

    // Implementation of abstract accessor methods
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
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new CindervaneFlightGoal(this));
        this.goalSelector.addGoal(3, new CindervaneCombatGoal(this));
        this.goalSelector.addGoal(4, new CindervaneFollowOwnerGoal(this));
        this.goalSelector.addGoal(6, new CindervaneGroundWanderGoal(this, 0.6D, 160));

        this.targetSelector.addGoal(1, new com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            if (!this.isOrderedToSit() && this.entityData.get(DATA_SIT_PROGRESS) != 0f) {
                this.entityData.set(DATA_SIT_PROGRESS, 0f);
            }
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

        tickSittingState();
        tickBankingLogic();
        tickPitchingLogic();
        tickRiderTakeoff();
        tickMountedState();
        updateSittingProgress();

        if (!level().isClientSide) {
            // Ensure sit animation is cleared for riders even if packets arrive late
            if (isVehicle() && this.entityData.get(DATA_SIT_PROGRESS) != 0f) {
                this.entityData.set(DATA_SIT_PROGRESS, 0f);
            }
            if (targetCooldown > 0) {
                targetCooldown--;
            }
            handleFireBodyCrash();
        }

        // Initialize animation state on first tick after loading to prevent thrashing
        if (!level().isClientSide && this.tickCount == 1) {
            initializeAnimationState();
        }

        tickClientSideUpdates();
    }

    private void tickSittingState() {
        // Clear sitting state if the dragon is being ridden
        if (!this.level().isClientSide && this.isVehicle() && this.isOrderedToSit()) {
            this.setOrderedToSit(false);
        }
    }

    private void tickMountedState() {
        boolean mounted = this.isVehicle();

        if (mounted && !wasVehicleLastTick) {
            this.sitProgress = 0f;
            this.prevSitProgress = 0f;
            this.entityData.set(DATA_SIT_PROGRESS, 0f);
            clearStatesWhenMounted();

            if (this.isOrderedToSit()) {
                this.setOrderedToSit(false);
                if (this.getCommand() == 1) {
                    this.setCommand(0);
                }
            }
        }

        if (!mounted && wasVehicleLastTick) {
            this.setBreathingFire(false);
            combatManager.forceEndActiveAbility();
            this.sitProgress = 0f;
            this.prevSitProgress = 0f;
            this.entityData.set(DATA_SIT_PROGRESS, 0f);
            this.entityData.set(DATA_GROUND_MOVE_STATE, 0);
            this.entityData.set(DATA_FLIGHT_MODE, -1);
            this.entityData.set(DATA_RIDER_FORWARD, 0f);
            this.entityData.set(DATA_RIDER_STRAFE, 0f);
            this.syncAnimState(0, -1);
        }

        wasVehicleLastTick = mounted;
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
        this.setOrderedToSit(false);
        this.setNoGravity(isFlying() || isHovering());
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
        prevBankAngle = bankAngle;

        // Only apply mouse-sensitivity banking when being ridden
        // Wild Cindervanes don't need banking animations
        boolean shouldBank = isFlying() && !isLanding() && !isHovering() && !isOrderedToSit() && isVehicle();

        // Reset banking when not flying, or not being ridden - instant snap back
        if (!shouldBank) {
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
        bankSmoothedYaw = bankSmoothedYaw * 0.75f + yawChange * 0.25f;

        // Convert smoothed yaw delta into a banking roll. Multiplying gives us headroom for aggressive turns.
        // More aggressive multiplier for glider-style banking (6.0f vs Raevyx's 5.0f)
        float targetAngle = Mth.clamp(bankSmoothedYaw * 6.0f, -90f, 90f);
        // Ease toward the new target so long sweeping turns feel weighty but responsive.
        bankAngle = Mth.lerp(0.30f, bankAngle, targetAngle);
        if (Math.abs(bankAngle) < 0.01f) {
            bankAngle = 0f;
        }

        // Update coarse direction for animation fallbacks
        float enter = 10.0f;
        float exit = 4.0f;

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

    /**
     * Gets the current bank direction for animation purposes
     * @return -1 for left, 0 for none, 1 for right
     */
    public int getBankDirection() {
        return bankDir;
    }

    /**
     * Gets the current bank angle in degrees. Positive values bank right, negative bank left.
     * This is based on mouse drag sensitivity - faster mouse movement = harder banking.
     */
    public float getBankAngleDegrees() {
        return bankAngle;
    }

    /**
     * Interpolated bank angle for smooth client-side rendering.
     */
    public float getBankAngleDegrees(float partialTick) {
        return Mth.lerp(partialTick, prevBankAngle, bankAngle);
    }

    /**
     * Legacy method - returns smooth bank direction for animation
     * Returns a smooth value based on actual bank angle
     */
    public float getSmoothBankDirection() {
        // Normalize bank angle to -1.0 to 1.0 range for animation
        return Mth.clamp(bankAngle / 45.0f, -1.0f, 1.0f);
    }

    public int getPitchDirection() {
        return pitchDir;
    }

    /**
     * Gets the number of ticks the dragon has been airborne.
     * Used for takeoff animation timing.
     */
    public int getAirTicks() {
        return airTicks;
    }

    // ===== Rider Control Methods =====
    @Override
    public boolean isGoingUp() {
        return this.entityData.get(DATA_GOING_UP); 
    }

    // ===== Animation State Methods =====
    
    @Override
    public boolean isRunning() {
        return this.entityData.get(DATA_RUNNING);
    }

    @Override
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
    

    public void setGroundMoveStateFromAI(int state) {
        if (!this.level().isClientSide) {
            int s = Mth.clamp(state, 0, 2);
            if (this.entityData.get(DATA_GROUND_MOVE_STATE) != s) {
                this.entityData.set(DATA_GROUND_MOVE_STATE, s);
                this.syncAnimState(s, getSyncedFlightMode());
            }
        }
    }


    
    // Rider input snapshots for server-side animation sync
    
    /**
     * Initialize animation state after entity loading to prevent thrashing.
     */
    @Override
    public void initializeAnimationState() {
        super.initializeAnimationState();
        if (!level().isClientSide) {
            // Reset all tick counters to ensure clean state
            groundTicks = 0;
            airTicks = 0;
            landingTicks = 0;
        }
    }
    

    // ===== Client animation overrides (for robust observer sync) =====
    
    

    @Override
    public int getFlightMode() {
        int flightMode = -1; // not flying (ground state)
        if (isFlying()) {
            // Reset altitude glide flag when not flying
            inHighAltitudeGlide = false;

            if (isTakeoff()) {
                flightMode = 3; // takeoff
            } else if (isHovering()) {
                flightMode = 2; // hover
            } else if (isGoingDown()) {
                flightMode = 0; // glide (descending always plays GLIDE_DOWN via animation handler)
            } else if (isGoingUp()) {
                flightMode = 1; // flap (ascending)
            } else {
                // Altitude-based flight mode for ridden dragons
                if (this.isTame() && this.isVehicle()) {
                    Entity rider = this.getControllingPassenger();
                    if (rider != null) {
                        int groundY = this.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                Mth.floor(this.getX()), Mth.floor(this.getZ()));
                        double altitudeAboveTerrain = this.getY() - groundY;

                        // Hysteresis: Enter glide at 40 blocks, exit at 30 blocks
                        if (inHighAltitudeGlide) {
                            // Already gliding - stay in glide until we drop below exit threshold
                            if (altitudeAboveTerrain > RIDER_GLIDE_ALTITUDE_EXIT) {
                                flightMode = 0; // High-altitude glide
                            } else {
                                inHighAltitudeGlide = false;
                                flightMode = 1; // Low altitude - flap
                            }
                        } else {
                            // Not gliding yet - enter glide if above entry threshold
                            if (altitudeAboveTerrain > RIDER_GLIDE_ALTITUDE_THRESHOLD) {
                                inHighAltitudeGlide = true;
                                flightMode = 0; // High-altitude glide
                            } else {
                                flightMode = 1; // Low altitude - flap
                            }
                        }
                    } else {
                        // Default to glide for natural descent
                        flightMode = 0; // glide
                    }
                } else {
                    // Not being ridden - reset flag and default to glide
                    inHighAltitudeGlide = false;
                    flightMode = 0; // glide
                }
            }
        } else {
            // Not flying - reset flag
            inHighAltitudeGlide = false;
        }
        return flightMode;
    }

    @Override
    protected void applyRiderVerticalInput(Player player, boolean goingUp, boolean goingDown, boolean locked) {
        if (this.isFlying()) {
            setGoingUp(goingUp);
            setGoingDown(goingDown);
        } else {
            setGoingUp(false);
            setGoingDown(false);
        }
    }

    @Override
    protected void applyRiderMovementInput(Player player, float forward, float strafe, float yaw, boolean locked) {
        float fwd = applyInputDeadzone(forward);
        float str = applyInputDeadzone(strafe);
        setLastRiderForward(fwd);
        setLastRiderStrafe(str);
        if (!isFlying()) {
            int moveState = 0;
            float magnitude = Math.abs(fwd) + Math.abs(str);
            if (magnitude > 0.05f) {
                moveState = isAccelerating() ? 2 : 1;
            }
            setGroundMoveStateFromAI(moveState);
            setRunning(moveState == 2);
        }
    }

    @Override
    protected void handleRiderAction(ServerPlayer player, DragonRiderAction action, String abilityName, boolean locked) {
        if (action == null) {
            return;
        }
        switch (action) {
            case TAKEOFF_REQUEST -> requestRiderTakeoff();
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

    @Override
    public RiderAbilityBinding getTertiaryRiderAbility() {
        return new RiderAbilityBinding(CindervaneAbilities.FIRE_BODY_ID, RiderAbilityBinding.Activation.HOLD);
    }

    @Override
    public RiderAbilityBinding getPrimaryRiderAbility() {
        return new RiderAbilityBinding(CindervaneAbilities.ROAR_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getSecondaryRiderAbility() {
        return new RiderAbilityBinding(CindervaneAbilities.FIRE_BREATH_VOLLEY_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getAttackRiderAbility() {
        return new RiderAbilityBinding(CindervaneAbilities.BITE_ID, RiderAbilityBinding.Activation.PRESS);
    }


    // ===== Riding System Methods =====

    protected int getMaxPassengers() {
        // Two-seater: Seat 0 (driver/owner) + Seat 1 (passenger)
        return 2;
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity passenger) {
        // Allow up to 2 passengers
        return this.getPassengers().size() < getMaxPassengers();
    }

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
        if (areRiderControlsLocked()) {
            copyRiderLook(player);
        }
    }
    
    @Override
    public @NotNull Vec3 getRiddenInput(@Nonnull Player player, @Nonnull Vec3 deltaIn) {
        Vec3 input = riderController.getRiddenInput(player, deltaIn);
        
        // Capture rider inputs for animation state (like Lightning Dragon)
        if (!level().isClientSide && !isFlying()) {
            float fwd = (float) Mth.clamp(input.z, -1.0, 1.0);
            float str = (float) Mth.clamp(input.x, -1.0, 1.0);
            this.entityData.set(DATA_RIDER_FORWARD, RideableDragonData.applyInputThreshold(fwd));
            this.entityData.set(DATA_RIDER_STRAFE, RideableDragonData.applyInputThreshold(str));
        }
        
        return input;
    }

    @Override
    public void setOrderedToSit(boolean sitting) {
        super.setOrderedToSit(sitting);

        if (sitting) {
            if (isFlying()) {
                this.setLanding(true);
            }
            this.setRunning(false);
            this.getNavigation().stop();
            if (!level().isClientSide) {
                this.entityData.set(DATA_SIT_PROGRESS, this.sitProgress);
            }
        } else if (!level().isClientSide) {
            this.sitProgress = 0f;
            this.prevSitProgress = 0f;
            this.entityData.set(DATA_SIT_PROGRESS, 0f);
        }
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
            if (!fireBodyCrashArmed) {
                fireBodyCrashMaxHeight = this.getY();
            } else if (this.getY() > fireBodyCrashMaxHeight) {
                fireBodyCrashMaxHeight = this.getY();
            }
            fireBodyCrashArmed = true;
        }
        if (fireBodyCrashArmed && !airborne && fireActive) {
            double dropDistance = fireBodyCrashMaxHeight - this.getY();
            if (dropDistance >= FIRE_BODY_CRASH_MIN_DROP) {
                triggerFireBodyCrash();
            }
            fireBodyCrashArmed = false;
            fireBodyCrashMaxHeight = 0.0D;
        }
        if (!fireActive && !airborne) {
            fireBodyCrashArmed = false;
            fireBodyCrashMaxHeight = 0.0D;
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
        
        ExplosionDamageCalculator calculator = new ExplosionDamageCalculator() {
            @Override
            public @NotNull Optional<Float> getBlockExplosionResistance(@NotNull Explosion explosion, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull FluidState fluid) {
                if (isFireBodyImmuneBlock(state)) {
                    return Optional.of(Float.MAX_VALUE);
                }
                return Optional.of(0.0F);
            }

            @Override
            public boolean shouldBlockExplode(@NotNull Explosion explosion, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull BlockState state, float exposure) {
                return !isFireBodyImmuneBlock(state);
            }
        };

        Explosion explosion = new Explosion(server, this, server.damageSources().explosion(this, this), calculator,
                x, y + 0.2D, z, FIRE_BODY_EXPLOSION_RADIUS, true, Explosion.BlockInteraction.DESTROY);

        List<LivingEntity> allies = grantAlliesExplosionImmunity(server, x, y, z);

        // Make allies temporarily invulnerable to prevent armor damage
        for (LivingEntity ally : allies) {
            ally.setInvulnerable(true);
        }

        explosion.explode();
        explosion.finalizeExplosion(true);

        // Restore vulnerability
        for (LivingEntity ally : allies) {
            ally.setInvulnerable(false);
        }

        applyFireBodyBlastDamage(server, x, y, z, immune);

        carveFireBodyImprint(server, BlockPos.containing(x, y, z));

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

    private boolean isFireBodyImmuneBlock(BlockState state) {
        return state.is(Blocks.BEDROCK) || state.is(Blocks.OBSIDIAN);
    }

    private List<LivingEntity> grantAlliesExplosionImmunity(ServerLevel server, double x, double y, double z) {
        double radius = FIRE_BODY_EXPLOSION_RADIUS + 4.0D;
        AABB area = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        List<LivingEntity> allies = server.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity != this && this.isAlly(entity));

        if (allies.isEmpty()) {
            return allies;
        }

        for (LivingEntity ally : allies) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4, true, false, false));
            ally.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0, true, false, false));
            ally.setRemainingFireTicks(0);
        }

        return allies;
    }

    private void applyFireBodyBlastDamage(ServerLevel server, double x, double y, double z, List<Entity> immune) {
        double radius = FIRE_BODY_EXPLOSION_RADIUS + 2.5D;
        AABB area = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);

        Set<Integer> immuneIds = new HashSet<>();
        for (Entity entity : immune) {
            immuneIds.add(entity.getId());
        }
        immuneIds.add(this.getId());

        List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, area,
                living -> living.isAlive() && !immuneIds.contains(living.getId()) && !this.isAlly(living));

        if (targets.isEmpty()) {
            return;
        }

        for (LivingEntity target : targets) {
            if (target.hurt(server.damageSources().explosion(this, this), FIRE_BODY_EXPLOSION_DAMAGE)) {
                target.setSecondsOnFire(8);
            }
        }
    }

    private void carveFireBodyImprint(ServerLevel server, BlockPos center) {
        int radius = Mth.ceil(FIRE_BODY_IMPRINT_RADIUS);
        double radiusSq = FIRE_BODY_IMPRINT_RADIUS * FIRE_BODY_IMPRINT_RADIUS;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double horizontalSq = dx * dx + dz * dz;
                if (horizontalSq > radiusSq) {
                    continue;
                }

                double normalized = Math.sqrt(horizontalSq) / FIRE_BODY_IMPRINT_RADIUS;
                double depth = (1.0D - normalized * normalized) * (FIRE_BODY_IMPRINT_RADIUS * FIRE_BODY_IMPRINT_DEPTH_FACTOR);
                int maxDepth = Mth.floor(depth);

                for (int dy = 0; dy <= maxDepth; dy++) {
                    cursor.set(center.getX() + dx, center.getY() - dy, center.getZ() + dz);
                    if (!server.isLoaded(cursor)) {
                        continue;
                    }

                    BlockState state = server.getBlockState(cursor);
                    if (state.isAir() || isFireBodyImmuneBlock(state)) {
                        continue;
                    }

                    server.destroyBlock(cursor, true, this);
                }
            }
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
        return new DragonPathNavigateGround(this, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Cindervane> movement = new AnimationController<>(this, "movement", 5, animationHandler::handleMovementAnimation);
        movement.setSoundKeyframeHandler(this::onAnimationSound);
        controllers.add(movement);

        AnimationController<Cindervane> banking = new AnimationController<>(this, "banking", 10, animationHandler::bankingPredicate);
        controllers.add(banking);

        AnimationController<Cindervane> pitching = new AnimationController<>(this, "pitching", 10, animationHandler::pitchingPredicate);
        controllers.add(pitching);

        AnimationController<Cindervane> actions = new AnimationController<>(this, "actions", 10, animationHandler::actionPredicate);
        animationHandler.setupActionController(actions);
        actions.setSoundKeyframeHandler(this::onAnimationSound);
        controllers.add(actions);
    }
    private void onAnimationSound(SoundKeyframeEvent<Cindervane> event) {
        soundHandler.handleAnimationSound(this, event.getKeyframeData(), event.getController());
    }

    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public Map<String, VocalEntry> getVocalEntries() {
        return VOCAL_ENTRIES;
    }

    @Override
    public DragonSoundProfile getSoundProfile() {
        return CinderSoundProfile.INSTANCE;
    }

    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        return CindervaneAbilities.BITE;
    }

    @Override
    public DragonAbilityType<?, ?> getRoaringAbility() {
        return CindervaneAbilities.ROAR;
    }

    @Override
    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return CindervaneAbilities.HURT;
    }

    @Override
    public int getDeathAnimationDurationTicks() {
        return 95; // 4.75s - matches amphithere death animation length
    }

    // Death handling now uses base class helpers

    @Override
    public boolean hurt(@Nonnull DamageSource source, float amount) {
        // During dying sequence, ignore all damage except the final generic kill used by DieAbility
        if (isDying()) {
            if (source.is(DamageTypes.GENERIC_KILL)) {
                return super.hurt(source, amount);
            }
            return false;
        }

        // Intercept lethal damage to play custom death ability first
        if (handleLethalDamage(source, amount, CindervaneAbilities.DIE)) {
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void tickDeath() {
        // Override vanilla death animation to prevent rotation/flop
        // Just increment death time without the rotation animation
        ++this.deathTime;
        if (this.deathTime >= 20) {
            this.remove(RemovalReason.KILLED);
            this.dropExperience();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends DragonEntity> DragonAbility<T> getActiveAbility() {
        return (DragonAbility<T>) combatManager.getActiveAbility();
    }

    public boolean isAbilityActive(DragonAbilityType<?, ?> abilityType) {
        return combatManager.isAbilityActive(abilityType);
    }

    public void forceEndAbility(DragonAbilityType<?, ?> abilityType) {
        combatManager.forceEndAbility(abilityType);
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
        if (type == CindervaneAbilities.BITE || type == CindervaneAbilities.FIRE_BODY || type == CindervaneAbilities.ROAR || type == CindervaneAbilities.FIRE_BREATH_VOLLEY) {
            combatManager.tryUseAbility(type);
        }
    }

    public void forceEndActiveAbility() {
        combatManager.forceEndActiveAbility();
        fireBodyCrashArmed = false;
        this.setBreathingFire(false);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.@NotNull Entity target) {
        // Use bite ability for melee attacks
        if (!this.isVehicle() && !this.isOrderedToSit()) {
            combatManager.tryUseAbility(CindervaneAbilities.BITE);
        }
        // Return true to indicate we handled the attack
        return true;
    }

    @Override
    public Vec3 getHeadPosition() {
        return this.getEyePosition();
    }

    @Override
    public Vec3 getMouthPosition() {
        return computeMouthOrigin(1.0f);
    }

    public Vec3 computeMouthOrigin(float partialTicks) {
        double x = Mth.lerp(partialTicks, this.xo, this.getX());
        double y = Mth.lerp(partialTicks, this.yo, this.getY());
        double z = Mth.lerp(partialTicks, this.zo, this.getZ());

        float yawDeg = Mth.lerp(partialTicks, this.yHeadRotO, this.yHeadRot);
        float pitchDeg = Mth.lerp(partialTicks, this.xRotO, this.getXRot());

        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);

        double R = (-0.4 / 16.0) * MODEL_SCALE;
        double U = (5.2 / 16.0) * MODEL_SCALE;
        double F = (12.5 / 16.0) * MODEL_SCALE;

        double cp = Math.cos(pitch), sp = Math.sin(pitch);
        double up = U * cp - F * sp;
        double fwd = U * sp + F * cp;

        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        double offX = R * cy - fwd * sy;
        double offZ = R * sy + fwd * cy;

        return new Vec3(x + offX, y + up, z + offZ);
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
        return ModEntities.CINDERVANE.get().create(level);
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
        tag.putBoolean("IsSitting", this.isOrderedToSit());
        tag.putFloat("SitProgress", this.sitProgress);
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
        boolean savedSitting = tag.getBoolean("IsSitting");
        float savedSitProgress = tag.contains("SitProgress") ? tag.getFloat("SitProgress") : (savedSitting ? this.maxSitTicks() : 0f);

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
        this.sitProgress = Mth.clamp(savedSitProgress, 0f, this.maxSitTicks());
        this.prevSitProgress = this.sitProgress;
        this.entityData.set(DATA_SIT_PROGRESS, this.sitProgress);
        this.setOrderedToSit(savedSitting);

        // Reset all tick counters to prevent state inconsistencies
        // Reset ground ticks when flying
        if (!savedFlying) {
            landingTicks = 0;
            airTicks = 0;
        } else {
            airTicks = Math.max(airTicks, 1);
        }
        groundTicks = 0; // Reset ground ticks on load

        this.setNoGravity(isFlying() || isHovering());
        
        // Force animation state sync after loading to prevent thrashing
        if (!level().isClientSide) {
            // Delay the sync slightly to ensure all systems are initialized
            this.tickCount = 0; // Reset tick counter to ensure proper initialization
        }
    }

    public void prepareForMounting() {
        if (level().isClientSide) {
            return;
        }

        this.setOrderedToSit(false);
        if (this.getCommand() == 1) {
            this.setCommand(0);
        }
        this.sitProgress = 0f;
        this.prevSitProgress = 0f;
        this.entityData.set(DATA_SIT_PROGRESS, 0f);
        this.setTarget(null);

        if (this.getNavigation().getPath() != null) {
            this.getNavigation().stop();
        }

        clearStatesWhenMounted();
    }

    // ===== DragonFlightCapable =====

    @Override
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

    @Override
    public DragonAbilityType<?, ?> getChannelingAbility() {
        return CindervaneAbilities.FIRE_BREATH_VOLLEY;
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

    // ===== CLIENT LOCATOR CACHE METHODS =====

    /**
     * Store a client-side locator position (used by renderer to cache bone positions)
     */
    public void setClientLocatorPosition(String name, Vec3 pos) {
        if (name == null || pos == null) return;
        this.clientLocatorCache.put(name, pos);
    }

    /**
     * Get a client-side locator position (used by rider controller to position passengers)
     */
    @Override
    public Vec3 getClientLocatorPosition(String name) {
        if (name == null) return null;
        return this.clientLocatorCache.get(name);
    }

}

