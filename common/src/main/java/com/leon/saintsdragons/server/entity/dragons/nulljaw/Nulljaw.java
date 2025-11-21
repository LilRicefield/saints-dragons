package com.leon.saintsdragons.server.entity.dragons.nulljaw;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.nulljaw.NulljawAbilities;
import com.leon.saintsdragons.server.ai.goals.nulljaw.*;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.ai.navigation.DragonAmphibiousNavigation;
import com.leon.saintsdragons.server.ai.navigation.DragonSwimMoveControl;
import com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtByTargetGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtTargetGoal;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers.*;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.*;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import com.leon.saintsdragons.server.entity.controller.nulljaw.NulljawRiderController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreathAirGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.damagesource.DamageSource;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Nulljaw extends RideableDragonBase implements AquaticDragon, ShakesScreen, SoundHandledDragon, DragonSleepCapable {

    // Force-load abilities registry when this class is loaded
    static {
        // This triggers the static initializers in NulljawAbilities, registering all abilities
        try {
            Class.forName("com.leon.saintsdragons.common.registry.nulljaw.NulljawAbilities");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load Nulljaw abilities!", e);
        }
    }

    // ===== VOCAL ENTRIES =====
    // IMPORTANT: Keys MUST match animation trigger names registered in NulljawAnimationHandler
    private static final Map<String, VocalEntry> VOCAL_ENTRIES = new VocalEntryBuilder()
            .add("grumble1", "action", "animation.nulljaw.grumble1", ModSounds.NULLJAW_GRUMBLE_1, 0.8f, 0.95f, 0.1f, false, false, true)
            .add("grumble2", "action", "animation.nulljaw.grumble2", ModSounds.NULLJAW_GRUMBLE_2, 0.8f, 0.95f, 0.1f, false, false, true)
            .add("grumble3", "action", "animation.nulljaw.grumble3", ModSounds.NULLJAW_GRUMBLE_3, 0.8f, 0.95f, 0.1f, false, false, true)
            .add("nulljaw_hurt", "hurt", "animation.nulljaw.hurt", ModSounds.NULLJAW_HURT, 1.1f, 0.95f, 0.1f, false, true, true)
            .add("nulljaw_die", "hurt", "animation.nulljaw.die", ModSounds.NULLJAW_DIE, 1.35f, 0.9f, 0.05f, false, true, true)
            .build();

    // ===== AMBIENT SOUND SYSTEM =====
    private int ambientSoundTimer;
    private int nextAmbientSoundDelay;
    private static final int MIN_AMBIENT_DELAY = 200;  // 10 seconds
    private static final int MAX_AMBIENT_DELAY = 600;  // 30 seconds

    private static final EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RIDER_FORWARD = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIDER_STRAFE = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_ACCELERATING = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SWIMMING = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SWIM_TURN = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SWIM_PITCH = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PHASE_TWO = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RIDER_LOCKED = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_SCREEN_SHAKE_AMOUNT = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.FLOAT);

    // Flight mode data accessor (not used for ground drake but required by interface)
    private static final EntityDataAccessor<Integer> DATA_FLIGHT_MODE = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_GOING_UP = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GOING_DOWN = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING =
            SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING_ENTERING =
            SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING_EXITING =
            SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for feeding cooldown ticks */
    private static final EntityDataAccessor<Integer> DATA_FEEDING_COOLDOWN =
            SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final NulljawAnimationHandler animationHandler = new NulljawAnimationHandler(this);
    private final NulljawInteractionHandler interactionHandler = new NulljawInteractionHandler(this);
    private final NulljawRiderController riderController;
    private double configuredWalkSpeed = 0.14D;
    private double configuredRunSpeed = 0.28D;
    private double configuredSwimSpeed = 1.45D;
    private final PathNavigation groundNavigation;
    private final DragonAmphibiousNavigation waterNavigation;
    private final MoveControl landMoveControl;
    private final DragonSwimMoveControl swimMoveControl;
    private final RiftDrakeLookController landLookControl;
    private int riderControlLockTicks = 0;
    private NulljawSwimGoal waterSwimGoal;
    private NulljawGroundWanderGoal groundWanderGoal;
    private boolean swimming;
    private int swimTicks;
    private int ticksInWater;
    private int ticksOutOfWater;
    private float swimTurnSmoothedYaw;
    private int swimTurnState;
    private int swimPitchStateTicks;
    // Continuous swim roll angle for smooth banking (like Raevyx's flight banking)
    private float swimRollAngle = 0f;
    private float prevSwimRollAngle = 0f;
    private boolean useLeftClawNext = true; // Toggles between left/right claw attacks
    // ===== SCREEN SHAKE SYSTEM =====
    private static final float SHAKE_DECAY_PER_TICK = 0.02F;
    private float prevScreenShakeAmount = 0.0F;
    private float screenShakeAmount = 0.0F;
    // ===== SIT / SLEEP STATE =====
    private boolean isSittingDown = false;
    private boolean isStandingUp = false;
    private int sitTransitionTicks = 0;
    private int sleepTransitionTicks = 0;
    private int sleepAmbientCooldownTicks = 0;
    private int sleepReentryCooldownTicks = 0;
    private int sleepCancelTicks = 0;
    private boolean sleepLocked = false;
    private int sleepCommandSnapshot = -1;
    private boolean wasVehicleLastTick = false;

    // ===== UNTAMED RIDE / TAMING STATE =====
    private static final int MIN_WILD_TAME_TICKS = 60;
    private static final int MAX_TAMING_PROGRESS = 400;
    private static final int BUCK_INTERVAL_MIN = 60;
    private static final int BUCK_INTERVAL_MAX = 110;
    private boolean wildRideActive = false;
    private int wildRideTicks = 0;
    private int nextBuckAttemptTick = 0;
    private int cumulativeWildRideProgress = 0;

    // Client-side animation initialization grace period (fixes T-pose on world rejoin with shaders)
    private int clientAnimInitTicks = 0;
    private static final int ANIM_INIT_GRACE_PERIOD = 5; // Wait 5 ticks for entity data sync

    // Derived from mouth_origin locator in rift_drake.geo (Z negative means forward in model space)
    private static final double MODEL_SCALE = 1.0D;
    private static final double MOUTH_OFFSET_RIGHT = (0.0D / 16.0D) * MODEL_SCALE;
    private static final double MOUTH_OFFSET_UP = (10.25D / 16.0D) * MODEL_SCALE;
    private static final double MOUTH_OFFSET_FORWARD = (24.0D / 16.0D) * MODEL_SCALE;

    // Feeding cooldown synced via DATA_FEEDING_COOLDOWN entity data accessor

    public boolean canFeed() {
        int cooldownTicks = this.entityData.get(DATA_FEEDING_COOLDOWN);
        return cooldownTicks <= 0;
    }

    public void setFeedingCooldown(int ticks) {
        this.entityData.set(DATA_FEEDING_COOLDOWN, ticks);
    }

    public Nulljaw(EntityType<? extends Nulljaw> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
        this.setMaxUpStep(1.4F);
        this.groundNavigation = new DragonPathNavigateGround(this, level);
        this.waterNavigation = new DragonAmphibiousNavigation(this, level);
        this.landMoveControl = new RiftDrakeMoveControl(this);
        this.swimMoveControl = new DragonSwimMoveControl(this, 6.0F, 0.08D, 0.12D);
        this.landLookControl = new RiftDrakeLookController(this);
        this.navigation = this.groundNavigation;
        this.moveControl = this.landMoveControl;
        this.lookControl = this.landLookControl;
        this.riderController = new NulljawRiderController(this);
        this.setRideable();

        // Initialize ambient sound system with random offset
        RandomSource rng = this.getRandom();
        this.ambientSoundTimer = rng.nextInt(80);
        this.nextAmbientSoundDelay = MIN_AMBIENT_DELAY + rng.nextInt(MAX_AMBIENT_DELAY - MIN_AMBIENT_DELAY);
        if (!level.isClientSide) {
            applyConfiguredAttributes();
        }
    }

    private void tickRiderControlLock() {
        if (riderControlLockTicks > 0) {
            riderControlLockTicks--;
            if (riderControlLockTicks <= 0) {
                this.entityData.set(DATA_RIDER_LOCKED, false);
            }
        }
    }

    @Override
    protected float getRiderLockYawBlend() {
        return this.isPhaseTwoActive() ? 0.25F : 0.18F;
    }

    @Override
    protected float getRiderLockPitchBlend() {
        return this.isPhaseTwoActive() ? 0.25F : 0.18F;
    }

    public boolean areRiderControlsLocked() {
        boolean locked = level().isClientSide
                ? this.entityData.get(DATA_RIDER_LOCKED)
                : riderControlLockTicks > 0;
        return locked || isWildRideActive();
    }

    // Animation initialization system (fixes T-pose on world rejoin with shaders)
    public boolean isClientAnimationReady() {
        return clientAnimInitTicks >= ANIM_INIT_GRACE_PERIOD;
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
    public boolean canTakeoff() {
        return false; // Nulljaw is a ground/aquatic dragon, cannot fly
    }

    @Override
    protected void applyRiderVerticalInput(Player player, boolean goingUp, boolean goingDown, boolean locked) {
        if (locked) {
            setGoingUp(false);
            setGoingDown(false);
            return;
        }
        boolean inWater = this.isSwimming() || this.isInWaterOrBubble();

        // In water: allow vertical movement
        if (inWater) {
            setGoingUp(goingUp);
            setGoingDown(goingDown);
        } else {
            // On land: trigger jump when space is pressed
            setGoingUp(false);
            setGoingDown(false);
            if (goingUp && this.onGround()) {
                handleJumpRequest();
            }
        }
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
            case TOGGLE_MELEE -> {
                if (!locked) {
                    toggleMeleeMode();
                }
            }
            default -> { }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.NULLJAW_ID);
        return TamableAnimal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, config.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, config.movementSpeed())
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SWIMMING, false);
        this.entityData.define(DATA_SWIM_TURN, 0);
        this.entityData.define(DATA_SWIM_PITCH, 0);
        this.entityData.define(DATA_PHASE_TWO, false);
        this.entityData.define(DATA_RIDER_LOCKED, false);
        this.entityData.define(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        this.entityData.define(DATA_SLEEPING, false);
        this.entityData.define(DATA_SLEEPING_ENTERING, false);
        this.entityData.define(DATA_SLEEPING_EXITING, false);
        this.entityData.define(DATA_FEEDING_COOLDOWN, 0);
    }
    
    @Override
    protected void defineRideableDragonData() {
        this.entityData.define(DATA_GROUND_MOVE_STATE, 0);
        this.entityData.define(DATA_RIDER_FORWARD, 0.0F);
        this.entityData.define(DATA_RIDER_STRAFE, 0.0F);
        this.entityData.define(DATA_ACCELERATING, false);
        this.entityData.define(DATA_FLIGHT_MODE, -1);
        this.entityData.define(DATA_GOING_UP, false);
        this.entityData.define(DATA_GOING_DOWN, false);
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
        // Priority 0: Critical survival - air management
        this.goalSelector.addGoal(0, new BreathAirGoal(this));
        // Priority 1: Sleep system (sleeps at night, or when owner is asleep)
        this.goalSelector.addGoal(1, new NulljawSleepGoal(this));

        // Priority 3: Combat abilities (CombatGoal handles both movement and attacks)
        this.goalSelector.addGoal(3, new NulljawCombatGoal(this));

        // Priority 6-7: Amphibious behavior (semi-aquatic patrol pattern)
        this.goalSelector.addGoal(6, new NulljawLeaveWaterGoal(this));
        this.goalSelector.addGoal(7, new NulljawFindWaterGoal(this));

        // Priority 8: Social behavior (follow owner)
        this.goalSelector.addGoal(8, new NulljawFollowOwnerGoal(this));

        // Priority 9: Idle swimming
        this.waterSwimGoal = new NulljawSwimGoal(this, 1.2D, 30);
        this.goalSelector.addGoal(9, waterSwimGoal);

        // Priority 10: Idle roaming on land
        this.groundWanderGoal = new NulljawGroundWanderGoal(this, 1.0D, 100);
        this.goalSelector.addGoal(10, groundWanderGoal);

        // Priority 11: Ambient behaviors
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));

        // Target selectors (threat detection)
        this.targetSelector.addGoal(1, new DragonOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new DragonOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Nulljaw> movementController =
                new AnimationController<>(this, "movement", 5, animationHandler::movementPredicate);
        AnimationController<Nulljaw> swimController =
                new AnimationController<>(this, "swim_direction", 4, animationHandler::swimDirectionPredicate);
        AnimationController<Nulljaw> actions =
                new AnimationController<>(this, "action", 10, animationHandler::actionPredicate);
        AnimationController<Nulljaw> hurtController =
                new AnimationController<>(this, "hurt", 1, animationHandler::hurtPredicate);

        animationHandler.configureMovementBlend(movementController);
        animationHandler.configureSwimBlend(swimController);
        animationHandler.setupHurtController(hurtController);

        // Sound keyframes
        movementController.setSoundKeyframeHandler(this::onAnimationSound);
        swimController.setSoundKeyframeHandler(this::onAnimationSound);
        actions.setSoundKeyframeHandler(this::onAnimationSound);
        hurtController.setSoundKeyframeHandler(this::onAnimationSound);

        // Setup animation triggers
        animationHandler.setupActionController(actions);

        controllers.add(movementController);
        controllers.add(swimController);
        controllers.add(actions);
        controllers.add(hurtController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
    }

    @Override
    public DragonSoundProfile getSoundProfile() {
        return NulljawSoundProfile.INSTANCE;
    }

    @Override
    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return NulljawAbilities.HURT;
    }

    @Override
    protected DragonAbilityType<?, ?> getDeathAbilityType() {
        return NulljawAbilities.DIE;
    }

    @Override
    public int getDeathAnimationDurationTicks() {
        return 56; // 2.7917 seconds
    }

    @Override
    public Map<String, VocalEntry> getVocalEntries() {
        return VOCAL_ENTRIES;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        // Mute vanilla footsteps; custom sounds handled via GeckoLib keyframes
    }

    // ===== AMBIENT SOUND METHODS =====

    /**
     * Plays appropriate ambient sound based on drake's current mood and state
     */
    private void playCustomAmbientSound() {
        RandomSource random = getRandom();

        // Don't make ambient sounds if dying, in combat, or using abilities
        if (isDying() || getTarget() != null || getActiveAbility() != null) {
            return;
        }

        String vocalKey = null;

        // Simple grumbles for the amphibious drake
        float moodRoll = random.nextFloat();
        if (moodRoll < 0.4f) {
            vocalKey = "grumble1";
        } else if (moodRoll < 0.7f) {
            vocalKey = "grumble2";
        } else {
            vocalKey = "grumble3";
        }

        // Play/animate if we chose one
        if (vocalKey != null) {
            this.getSoundHandler().playVocal(vocalKey);
        }
    }

    /**
     * Handles all the ambient grumbling sounds
     */
    private void handleAmbientSounds() {
        // Suppress ambient sounds while transitioning or resting to prevent animation snapping
        if (isBaby() || isDying() || isOrderedToSit() || isSleeping() || isSleepTransitioning() || isInSitTransition() || sleepAmbientCooldownTicks > 0) {
            return;
        }

        ambientSoundTimer++;

        // Time to make some noise?
        if (ambientSoundTimer >= nextAmbientSoundDelay) {
            playCustomAmbientSound();
            resetAmbientSoundTimer();
        }
    }

    /**
     * Resets the ambient sound timer with some randomness
     */
    private void resetAmbientSoundTimer() {
        RandomSource random = getRandom();
        ambientSoundTimer = 0;
        nextAmbientSoundDelay = MIN_AMBIENT_DELAY + random.nextInt(MAX_AMBIENT_DELAY - MIN_AMBIENT_DELAY);
    }

    @Override
    public void tick() {
        super.tick();
        soundHandler.tick();

        // Increment animation initialization counter on client (prevents T-pose on rejoin with shaders)
        if (level().isClientSide && clientAnimInitTicks < ANIM_INIT_GRACE_PERIOD) {
            clientAnimInitTicks++;
        }

        tickSittingState();
        tickMountedState();
        tickScreenShake();
        updateSittingProgress();
        tickSleepTransition();
        tickSleepCooldowns();
        tickFeedingCooldown();

        // Handle ambient sounds (server-side only)
        if (!level().isClientSide) {
            handleAmbientSounds();
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

            if ((isSleeping() || isSleepingEntering() || isSleepingExiting())
                    && (this.getTarget() != null || this.isAggressive() || this.isInWaterOrBubble())) {
                wakeUpImmediately();
                suppressSleep(200);
            }

            this.tickAnimationStates();
            this.updateSwimOrientationState();
            this.tickWildRideState();
        }

        tickClientSideUpdates();
    }

    @Override
    public void travel(@NotNull Vec3 motion) {
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player && !isWildRideActive()) {
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
            super.travel(motion);
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
    public double getSwimSpeed() {
        double baseSpeed = configuredSwimSpeed;
        if (this.isVehicle()) {
            baseSpeed += 0.2D; // Give riders a bit more responsiveness when mounted
        }
        return baseSpeed;
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

    public boolean beginUntamedRide(Player player) {
        if (this.isTame() || this.isBaby() || this.isVehicle() || player.isSecondaryUseActive()) {
            return false;
        }
        if (this.level().isClientSide) {
            return true;
        }
        player.startRiding(this);
        startWildRideSequence();
        return true;
    }

    public void awardTamingAdvancement(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var advancement = serverPlayer.server.getAdvancements()
                    .getAdvancement(SaintsDragonsCommon.rl("tame_nulljaw"));
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "tame_nulljaw");
            }
        }
    }

    private void applyConfiguredAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.NULLJAW_ID);
        setAttributeBase(Attributes.MAX_HEALTH, config.maxHealth());
        setAttributeBase(Attributes.MOVEMENT_SPEED, config.movementSpeed());
        setAttributeBase(Attributes.ARMOR, config.armor());
        configuredRunSpeed = Math.max(0.01D, config.movementSpeed());
        configuredWalkSpeed = config.extraDouble("walk_speed", configuredRunSpeed * 0.5D);
        configuredSwimSpeed = config.extraDouble("swim_speed", 1.45D);
        if (this.getHealth() > config.maxHealth()) {
            this.setHealth((float) config.maxHealth());
        }
    }

    private void setAttributeBase(Attribute attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    public double getConfiguredWalkSpeed() {
        return configuredWalkSpeed;
    }

    public double getConfiguredRunSpeed() {
        return configuredRunSpeed;
    }

    public double getConfiguredSwimSpeed() {
        return configuredSwimSpeed;
    }
    
    @Override
    public boolean isFood(@Nonnull net.minecraft.world.item.ItemStack stack) {
        // Rift Drakes prefer fish like other dragons
        return stack.is(net.minecraft.world.item.Items.COD) ||
               stack.is(net.minecraft.world.item.Items.SALMON) ||
               stack.is(net.minecraft.world.item.Items.TROPICAL_FISH) ||
               stack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
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

    /**
     * Allow AI goals to set ground move state explicitly
     */
    public void setGroundMoveStateFromAI(int state) {
        if (!this.level().isClientSide) {
            int s = Mth.clamp(state, 0, 2);
            if (this.entityData.get(DATA_GROUND_MOVE_STATE) != s) {
                this.entityData.set(DATA_GROUND_MOVE_STATE, s);
                this.syncAnimState(s, getSyncedFlightMode());
            }
        }
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
        if (isWildRideActive()) {
            handleUntamedRideWhileMounted(player);
            return;
        }
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
        if (!this.level().isClientSide && !this.isTame() && wildRideActive) {
            endWildRide(false);
        }
        if (!this.level().isClientSide) {
            this.setAccelerating(false);
            this.setLastRiderForward(0.0F);
            this.setLastRiderStrafe(0.0F);
            this.setGroundMoveStateFromRider(0);
        }
    }

    // Let RideableDragonBase handle tickAnimationStates() for proper networking

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        if (fallDistance <= 16.0F) {
            return false;
        }
        return super.causeFallDamage(fallDistance - 16.0F, damageMultiplier, source);
    }

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
        Vec3 locator = getClientLocatorPosition("mouth_origin");
        if (locator != null) {
            return locator;
        }
        return computeMouthPositionFallback();
    }

    private Vec3 computeMouthPositionFallback() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        float yawDeg = this.yHeadRot;
        float pitchDeg = this.getXRot();

        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);

        double cp = Math.cos(pitch);
        double sp = Math.sin(pitch);
        double pitchedUp = MOUTH_OFFSET_UP * cp - MOUTH_OFFSET_FORWARD * sp;
        double pitchedForward = MOUTH_OFFSET_UP * sp + MOUTH_OFFSET_FORWARD * cp;

        double cy = Math.cos(yaw);
        double sy = Math.sin(yaw);
        double offX = MOUTH_OFFSET_RIGHT * cy - pitchedForward * sy;
        double offZ = MOUTH_OFFSET_RIGHT * sy + pitchedForward * cy;

        return new Vec3(x + offX, y + pitchedUp, z + offZ);
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
        } else {
            // Debug: log if ability type not found
            System.out.println("[Nulljaw Debug] Ability '" + abilityName + "' not found in registry!");
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
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        return riderController.getDismountLocationForPassenger(passenger);
    }

    @Override
    public net.minecraft.world.entity.AgeableMob getBreedOffspring(@Nonnull net.minecraft.server.level.ServerLevel level, @Nonnull net.minecraft.world.entity.AgeableMob other) {
        return null;
    }

    @Override
    public com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        // Use melee mode to switch between bite and horn gore
        // Mode 0 = bite (primary), Mode 1 = horn gore (secondary)
        boolean useHornGore = getMeleeMode() == 1;

        // Phase 2 uses bite2 (faster bite) instead of normal bite
        if (isPhaseTwoActive()) {
            return useHornGore ? NulljawAbilities.NULLJAW_HORN_GORE : NulljawAbilities.NULLJAW_BITE2;
        }
        return useHornGore ? NulljawAbilities.NULLJAW_HORN_GORE : NulljawAbilities.NULLJAW_BITE;
    }

    public boolean isAbilityActive(DragonAbilityType<?, ?> abilityType) {
        return combatManager.isAbilityActive(abilityType);
    }

    public void forceEndAbility(DragonAbilityType<?, ?> abilityType) {
        combatManager.forceEndAbility(abilityType);
    }

    public static boolean canSpawnHere(EntityType<? extends Nulljaw> type,
                                       LevelAccessor level,
                                       MobSpawnType spawnType,
                                       BlockPos pos,
                                       RandomSource random) {
        if (!Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }

        // Allow amphibious spawning: valid on solid ground OR in water
        boolean fluidHere = !level.getFluidState(pos).isEmpty();

        if (!fluidHere) {
            // Land: require sturdy ground and free feet/head
            BlockPos below = pos.below();
            boolean solidGround = level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
            boolean feetFree = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
            boolean headFree = level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
            return solidGround && feetFree && headFree;
        } else {
            // Water: require water at feet and head positions for clearance
            FluidState feet = level.getFluidState(pos);
            FluidState head = level.getFluidState(pos.above());
            boolean feetWater = !feet.isEmpty() && feet.isSource();
            boolean headWater = !head.isEmpty();
            return feetWater && headWater;
        }
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
        if (groundWanderGoal != null) {
            groundWanderGoal.forceTrigger();
        }
        this.entityData.set(DATA_SWIMMING, false);
        this.entityData.set(DATA_SWIM_TURN, 0);
        this.entityData.set(DATA_SWIM_PITCH, 0);
        this.swimTurnSmoothedYaw = 0.0F;
        this.swimTurnState = 0;
        this.swimPitchStateTicks = 0;
        this.swimRollAngle = 0f;
        this.prevSwimRollAngle = 0f;
    }

    private void updateSwimOrientationState() {
        // Server-only: Update synced swim turn and pitch states
        if (!level().isClientSide) {
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

        // Client + Server: Calculate continuous swim roll angle (like Raevyx's banking)
        prevSwimRollAngle = swimRollAngle;

        // Reset when not swimming or controls locked
        if (!isSwimming() || areRiderControlsLocked()) {
            swimRollAngle = 0f;
            prevSwimRollAngle = 0f;
            swimTurnSmoothedYaw = 0f;
            return;
        }

        // Calculate yaw velocity for banking (works on both client and server)
        float yawDelta = Mth.wrapDegrees(this.getYRot() - this.yRotO);
        swimTurnSmoothedYaw = swimTurnSmoothedYaw * 0.6F + yawDelta * 0.4F;

        // Convert smoothed yaw delta into a roll angle
        float targetAngle = Mth.clamp(swimTurnSmoothedYaw * 40.0f, -60f, 60f); // More roll than Raevyx
        // Ease toward the new target (more responsive than before)
        swimRollAngle = Mth.lerp(0.40f, swimRollAngle, targetAngle); // Increased from 0.25f to 0.40f
        if (Math.abs(swimRollAngle) < 0.01f) {
            swimRollAngle = 0f;
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

    /**
     * Get the swim roll angle in degrees (like Raevyx's banking)
     */
    public float getSwimRollAngleDegrees() {
        return swimRollAngle;
    }

    /**
     * Get interpolated swim roll angle for smooth rendering
     */
    public float getSwimRollAngleDegrees(float partialTick) {
        return Mth.lerp(partialTick, prevSwimRollAngle, swimRollAngle);
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


    @Override
    public RiderAbilityBinding getAttackRiderAbility() {
        // Use melee mode to switch between horn gore and bite/bite2
        // Mode 0 = bite (primary), Mode 1 = horn gore (secondary)
        if (getMeleeMode() == 1) {
            return new RiderAbilityBinding(NulljawAbilities.NULLJAW_HORN_GORE_ID, RiderAbilityBinding.Activation.PRESS);
        } else {
            // Phase 2 uses bite2, Phase 1 uses bite
            try {
                if (isPhaseTwoActive()) {
                    return new RiderAbilityBinding(NulljawAbilities.NULLJAW_BITE2_ID, RiderAbilityBinding.Activation.PRESS);
                }
            } catch (Exception e) {
                // Fallback to phase 1 bite if entity data isn't ready
            }
            return new RiderAbilityBinding(NulljawAbilities.NULLJAW_BITE_ID, RiderAbilityBinding.Activation.PRESS);
        }
    }

    @Override
    public RiderAbilityBinding getPrimaryRiderAbility() {
        // R key: Universal roar ability for all dragons (PRESS)
        return new RiderAbilityBinding(NulljawAbilities.NULLJAW_ROAR_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getSecondaryRiderAbility() {
        return new RiderAbilityBinding(NulljawAbilities.NULLJAW_PHASE_SHIFT_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getTertiaryRiderAbility() {
        // G key: Phase 2 gets claw attacks (PRESS, not HOLD like other dragons)
        if (isPhaseTwoActive()) {
            return new RiderAbilityBinding(NulljawAbilities.NULLJAW_CLAW_ID, RiderAbilityBinding.Activation.PRESS);
        }
        return null; // G key - not used in phase 1
    }

    @Override
    public DragonAbilityType<?, ?> getChannelingAbility() {
        return NulljawAbilities.NULLJAW_PHASE_SHIFT;
    }

    public void onAnimationSound(SoundKeyframeEvent<Nulljaw> event) {
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
        blended = blended.multiply(dragFactor, 0.96D, dragFactor);

        if (!isGoingUp() && !isGoingDown() && getControllingPassenger() != null) {
            blended = blended.multiply(1.0D, 0.0D, 1.0D);
        } else if (!isGoingUp() && !isGoingDown() && getTarget() == null) {
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
     * Check if the drake is dying (health below 10%)
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

        public RiftDrakeMoveControl(Nulljaw drake) {
            super(drake);
        }

        @Override
        public void tick() {
            super.tick();
        }
    }

    // ===== LOOK CONTROLLER =====
    public static class RiftDrakeLookController extends LookControl {
        private final Nulljaw dragon;

        public RiftDrakeLookController(Nulljaw dragon) {
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

                    boolean allowRiderPitch = !(this.dragon.isPhaseTwoActive() && this.dragon.areRiderControlsLocked());
                    if (allowRiderPitch) {
                        float pitch = Mth.clamp(rider.getXRot(), -45.0F, 45.0F);
                        this.dragon.setXRot(pitch);
                        this.dragon.xRotO = pitch;
                    } else {
                        float eased = Mth.approachDegrees(this.dragon.getXRot(), 0.0F, 6.0F);
                        this.dragon.setXRot(eased);
                        this.dragon.xRotO = eased;
                    }
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
        // Clear sitting state if the drake is being ridden
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
            isSittingDown = false;
            isStandingUp = false;
            sitTransitionTicks = 0;

            if (this.isOrderedToSit()) {
                this.setOrderedToSit(false);
                if (this.getCommand() == 1) {
                    this.setCommand(0);
                }
            }

            if (isSleeping() || isSleepingEntering() || isSleepingExiting()) {
                wakeUpImmediately();
                suppressSleep(300);
            }
        }

        if (!mounted && wasVehicleLastTick) {
            this.sitProgress = 0f;
            this.prevSitProgress = 0f;
            this.entityData.set(DATA_SIT_PROGRESS, 0f);
            isSittingDown = false;
            isStandingUp = false;
            sitTransitionTicks = 0;
        }

        wasVehicleLastTick = mounted;
    }

    private void startWildRideSequence() {
        wildRideActive = true;
        wildRideTicks = 0;
        nextBuckAttemptTick = nextBuckDelay();
        this.getNavigation().stop();
        this.setTarget(null);
    }

    private void tickWildRideState() {
        if (!wildRideActive || this.isTame()) {
            wildRideActive = false;
            return;
        }

        Player rider = getWildRideRider();
        if (rider == null) {
            endWildRide(false);
            return;
        }

        wildRideTicks++;
        cumulativeWildRideProgress = Math.min(cumulativeWildRideProgress + 1, MAX_TAMING_PROGRESS);
        applyWildRideMotion();

        if (wildRideTicks >= nextBuckAttemptTick) {
            buckWildRider(rider);
            endWildRide(true);
            return;
        }

        if (wildRideTicks >= MIN_WILD_TAME_TICKS) {
            float progressFactor = (float) cumulativeWildRideProgress / (float) MAX_TAMING_PROGRESS;
            float successChance = 0.01F + progressFactor * 0.03F; // 1% base up to ~4%
            if (this.getRandom().nextFloat() < successChance) {
                this.tame(rider);
                this.setOrderedToSit(false);
                this.setCommand(0);
                this.level().broadcastEntityEvent(this, (byte) 7);
                awardTamingAdvancement(rider);
                endWildRide(false);
            }
        }
    }

    private void handleUntamedRideWhileMounted(Player rider) {
        rider.fallDistance = 0.0F;
        this.fallDistance = 0.0F;
    }

    private void applyWildRideMotion() {
        if (this.getNavigation().isDone() || this.getRandom().nextInt(40) == 0) {
            double targetX = this.getX() + (this.getRandom().nextDouble() - 0.5D) * 18.0D;
            double targetZ = this.getZ() + (this.getRandom().nextDouble() - 0.5D) * 18.0D;
            double targetY = this.getY();
            this.getNavigation().moveTo(targetX, targetY, targetZ, 1.45D);
        }

        if (this.onGround()) {
            if (this.tickCount % 7 == 0) {
                this.jumpFromGround();
                Vec3 impulse = new Vec3(
                        (this.getRandom().nextDouble() - 0.5D) * 1.0D,
                        0.5D + this.getRandom().nextDouble() * 0.4D,
                        (this.getRandom().nextDouble() - 0.5D) * 1.0D
                );
                this.setDeltaMovement(this.getDeltaMovement().add(impulse));
                this.hasImpulse = true;
            } else if (this.tickCount % 4 == 0) {
                Vec3 lateral = new Vec3(
                        (this.getRandom().nextDouble() - 0.5D) * 0.5D,
                        0.0D,
                        (this.getRandom().nextDouble() - 0.5D) * 0.5D
                );
                this.setDeltaMovement(this.getDeltaMovement().add(lateral));
                this.hasImpulse = true;
            }
        }
    }

    private Player getWildRideRider() {
        if (!this.isVehicle()) {
            return null;
        }
        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof Player player && !this.isTame()) {
            return player;
        }
        return null;
    }

    private void buckWildRider(Player rider) {
        rider.stopRiding();
        Vec3 launch = new Vec3(
                (this.getRandom().nextDouble() - 0.5D) * 0.9D,
                0.8D + this.getRandom().nextDouble() * 0.4D,
                (this.getRandom().nextDouble() - 0.5D) * 0.9D
        );
        rider.push(launch.x, launch.y, launch.z);
        rider.hurtMarked = true;
        this.level().broadcastEntityEvent(this, (byte) 6);
    }

    private void endWildRide(boolean bucked) {
        if (bucked) {
            cumulativeWildRideProgress = Math.max(0, cumulativeWildRideProgress - 40);
        } else {
            cumulativeWildRideProgress = Math.max(0, cumulativeWildRideProgress - 10);
        }
        wildRideActive = false;
        wildRideTicks = 0;
        nextBuckAttemptTick = 0;
    }

    private int nextBuckDelay() {
        return Mth.nextInt(this.getRandom(), BUCK_INTERVAL_MIN, BUCK_INTERVAL_MAX);
    }

    private boolean isWildRideActive() {
        return wildRideActive && !this.isTame();
    }

    private void updateSittingProgress() {
        if (level().isClientSide) {
            return;
        }

        if (sitTransitionTicks > 0) {
            sitTransitionTicks--;
            if (sitTransitionTicks == 0) {
                isSittingDown = false;
                isStandingUp = false;
            }
        }

        if (this.isOrderedToSit()) {
            if ((sitProgress == 0f || isStandingUp) && !isSittingDown) {
                animationHandler.triggerSitDownAnimation();
                isSittingDown = true;
                isStandingUp = false;
                sitTransitionTicks = getSitDownAnimationTicks();
            }

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
                    isSittingDown = false;
                    isStandingUp = false;
                    sitTransitionTicks = 0;
                }
            } else if (sitProgress > 0f) {
                if ((sitProgress >= maxSitTicks() || isSittingDown) && !isStandingUp) {
                    animationHandler.triggerSitUpAnimation();
                    isStandingUp = true;
                    isSittingDown = false;
                    sitTransitionTicks = getSitUpAnimationTicks();
                }

                float decrementRate = maxSitTicks() / (float) getSitUpAnimationTicks();
                sitProgress -= decrementRate;
                if (sitProgress < 0f) {
                    sitProgress = 0f;
                }
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

    private void tickSleepTransition() {
        // Handle sleep enter transition: sit_down → fall_asleep → sleep loop
        if (isSleepingEntering() && !level().isClientSide) {
            // Check if sit_down animation is complete (sitProgress reached max)
            if (getSitProgress() >= maxSitTicks()) {
                // Sit down complete, now trigger fall_asleep if we haven't started the transition timer yet
                if (sleepTransitionTicks == getFallAsleepAnimationTicks()) {
                    // Just reached sitting position, trigger fall_asleep
                    animationHandler.triggerFallAsleepAnimation();
                }
            }
        }

        if (sleepTransitionTicks > 0) {
            sleepTransitionTicks--;

            // Trigger sleep animation 3 ticks BEFORE fall_asleep finishes for smooth blend
            if (sleepTransitionTicks == 3 && isSleepingEntering() && !level().isClientSide) {
                animationHandler.triggerSleepAnimation();
            }

            if (sleepTransitionTicks == 0) {
                if (isSleepingEntering()) {
                    // fall_asleep animation finishing - mark as sleeping (animation already triggered)
                    setSleeping(true);
                    setSleepingEntering(false);
                } else if (isSleepingExiting()) {
                    // wake_up finished: dragon is now sitting, will stand up via normal sit system
                    setSleepingExiting(false);
                    // Start small ambient cooldown buffer (~0.5s)
                    sleepAmbientCooldownTicks = Math.max(sleepAmbientCooldownTicks, 10);
                }
            }
        }
    }

    private void tickSleepCooldowns() {
        if (sleepAmbientCooldownTicks > 0) sleepAmbientCooldownTicks--;
        if (sleepReentryCooldownTicks > 0) sleepReentryCooldownTicks--;
        if (sleepCancelTicks > 0) sleepCancelTicks--;
    }

    private void tickFeedingCooldown() {
        int cooldownTicks = this.entityData.get(DATA_FEEDING_COOLDOWN);
        if (cooldownTicks > 0) {
            cooldownTicks--;
            this.entityData.set(DATA_FEEDING_COOLDOWN, cooldownTicks);
        }
    }

    // ===== SIT TRANSITION HELPERS =====

    public boolean isInSitTransition() {
        return isSittingDown || isStandingUp;
    }
    public boolean isSittingDownAnimation() {
        return isSittingDown;
    }
    public boolean isStandingUpAnimation() {
        return isStandingUp;
    }

    @Override
    public float maxSitTicks() {
        return 38.0F; // down animation is 1.88s = 38 ticks
    }

    public int getSitDownAnimationTicks() {
        return 38; // down animation is 1.88s = 38 ticks
    }

    public int getSitUpAnimationTicks() {
        return 38; // up animation is 1.88s = 38 ticks
    }

    public int getFallAsleepAnimationTicks() {
        return 38; // fall_asleep animation is 1.88s = 38 ticks
    }

    public int getWakeUpAnimationTicks() {
        return 38; // up animation is 1.88s = 38 ticks
    }


    // ===== SLEEP SYSTEM =====

    @Override
    public boolean isSleeping() {
        return this.entityData.get(DATA_SLEEPING);
    }

    public void setSleeping(boolean sleeping) {
        this.entityData.set(DATA_SLEEPING, sleeping);
    }

    @Override
    public boolean isSleepTransitioning() {
        return isSleepingEntering() || isSleepingExiting();
    }

    public boolean isSleepingEntering() {
        return this.entityData.get(DATA_SLEEPING_ENTERING);
    }

    public void setSleepingEntering(boolean entering) {
        this.entityData.set(DATA_SLEEPING_ENTERING, entering);
    }

    public boolean isSleepingExiting() {
        return this.entityData.get(DATA_SLEEPING_EXITING);
    }

    public void setSleepingExiting(boolean exiting) {
        this.entityData.set(DATA_SLEEPING_EXITING, exiting);
    }

    public boolean isSleepLocked() {
        return sleepLocked || isSleeping() || isSleepingEntering() || isSleepingExiting();
    }

    private void enterSleepLock() {
        if (level().isClientSide) {
            return;
        }
        if (!sleepLocked) {
            sleepLocked = true;
            sleepCommandSnapshot = this.getCommand();
        }
        this.setOrderedToSit(true);
        this.getNavigation().stop();
        this.setTarget(null);
        this.setDeltaMovement(Vec3.ZERO);
        if (this.getCommand() != 1) {
            this.setCommand(1);
        }
    }

    private void releaseSleepLock() {
        if (level().isClientSide) {
            return;
        }
        if (sleepLocked) {
            int desired = sleepCommandSnapshot;
            sleepCommandSnapshot = -1;
            sleepLocked = false;
            if (desired >= 0 && desired != this.getCommand()) {
                this.setCommand(desired);
                this.setOrderedToSit(desired == 1);
            }
        }
    }

    @Override
    public void startSleepEnter() {
        if (isSleeping() || isSleepingEntering() || isSleepingExiting()) return;
        setSleepingEntering(true);
        // Sleep enter: sit_down (uses sitProgress) → fall_asleep → sleep loop
        boolean alreadySitting = isOrderedToSit() || getSitProgress() >= maxSitTicks();
        if (alreadySitting) {
            // Already sitting - trigger fall_asleep immediately
            sleepTransitionTicks = getFallAsleepAnimationTicks();
            animationHandler.triggerFallAsleepAnimation();
            this.setOrderedToSit(true);
            if (!level().isClientSide) {
                enterSleepLock();
            }
        } else {
            // Not sitting - trigger sit_down first
            sleepTransitionTicks = getFallAsleepAnimationTicks();
            animationHandler.triggerSitDownAnimation();
            if (!level().isClientSide) {
                enterSleepLock();
            }
        }
    }

    @Override
    public void startSleepExit() {
        if ((!isSleeping() && !isSleepingEntering()) || isSleepingExiting()) return;
        this.entityData.set(DATA_SLEEPING, false);
        setSleepingEntering(false);
        setSleepingExiting(true);
        sleepTransitionTicks = getWakeUpAnimationTicks();
        animationHandler.triggerWakeUpAnimation();
        if (!level().isClientSide) {
            suppressSleep(40);
            releaseSleepLock();
        }
    }

    public void wakeUpImmediately() {sleepAmbientCooldownTicks = Math.max(sleepAmbientCooldownTicks, 10);
        this.entityData.set(DATA_SLEEPING, false);
        setSleepingEntering(false);
        setSleepingExiting(false);
        sleepTransitionTicks = 0;
        sleepCancelTicks = 2;
        if (!level().isClientSide) {
            suppressSleep(40);
            releaseSleepLock();
        }
    }

    public void suppressSleep(int ticks) {
        sleepReentryCooldownTicks = Math.max(sleepReentryCooldownTicks, ticks);
    }

    @Override
    public boolean isSleepSuppressed() {
        return sleepReentryCooldownTicks > 0;
    }

    @Override
    public SleepPreferences getSleepPreferences() {
        return new SleepPreferences(
                true,   // canSleepAtNight
                false,  // canSleepDuringDay
                false,  // requiresShelter
                true,   // avoidsThunderstorms
                true    // sleepsNearOwner
        );
    }

    @Override
    public boolean canSleepNow() {
        return !isVehicle() && !this.isInWaterOrBubble() && getActiveAbility() == null && !isPhaseTwoActive();
    }

    private void tickScreenShake() {
        if (level().isClientSide) {
            prevScreenShakeAmount = screenShakeAmount;
            screenShakeAmount = this.entityData.get(DATA_SCREEN_SHAKE_AMOUNT);
            return;
        }

        prevScreenShakeAmount = screenShakeAmount;
        if (screenShakeAmount > 0.0F) {
            float newAmount = Math.max(0.0F, screenShakeAmount - SHAKE_DECAY_PER_TICK);
            screenShakeAmount = newAmount;
            this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, newAmount);
        } else if (this.entityData.get(DATA_SCREEN_SHAKE_AMOUNT) != 0.0F) {
            this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        }
    }

    @Override
    public boolean hurt(@javax.annotation.Nonnull net.minecraft.world.damagesource.DamageSource damageSource, float amount) {
        // During dying sequence, ignore all damage (entity is already dead, playing death animation)
        if (isDying()) {
            return false;
        }
        // Wake if sleeping and suppress re-entry on damage
        if (isSleeping() || isSleepingEntering() || isSleepingExiting()) {
            wakeUpImmediately();
            suppressSleep(200);
        }
        return super.hurt(damageSource, amount);
    }

    @Override
    public void addAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        saveRideableData(tag);
        tag.putBoolean("PhaseTwo", isPhaseTwoActive());
        tag.putBoolean("Sleeping", isSleeping());
        tag.putBoolean("SleepingEntering", isSleepingEntering());
        tag.putBoolean("SleepingExiting", isSleepingExiting());
        tag.putBoolean("SleepLocked", sleepLocked);
        tag.putInt("SleepTransitionTicks", sleepTransitionTicks);
        tag.putInt("SleepAmbientCooldown", sleepAmbientCooldownTicks);
        tag.putInt("SleepReentryCooldown", sleepReentryCooldownTicks);

        // Sleep state is ephemeral - not persisted (sleep goal re-evaluates on load)
        tag.putInt("SleepCancelTicks", sleepCancelTicks);
        tag.putInt("SleepCommandSnapshot", sleepCommandSnapshot);

        // Persist feeding cooldown (synced via entity data but saved for redundancy)
        tag.putInt("FeedingCooldownTicks", Math.max(0, this.entityData.get(DATA_FEEDING_COOLDOWN)));
    }

    @Override
    public void readAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadRideableData(tag);
        setSleeping(tag.getBoolean("Sleeping"));
        setSleepingEntering(tag.getBoolean("SleepingEntering"));
        setSleepingExiting(tag.getBoolean("SleepingExiting"));
        sleepLocked = tag.getBoolean("SleepLocked");
        sleepTransitionTicks = tag.getInt("SleepTransitionTicks");
        sleepAmbientCooldownTicks = tag.getInt("SleepAmbientCooldown");
        sleepReentryCooldownTicks = tag.getInt("SleepReentryCooldown");

        // Sleep state is ephemeral - not loaded (cleaned up below, sleep goal re-evaluates)

        // Restore feeding cooldown (synced via entity data but loaded for redundancy)
        if (tag.contains("FeedingCooldownTicks")) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, tag.getInt("FeedingCooldownTicks")));
        }

        sleepCancelTicks = tag.getInt("SleepCancelTicks");
        sleepCommandSnapshot = tag.contains("SleepCommandSnapshot") ? tag.getInt("SleepCommandSnapshot") : -1;

        // Clear all sleep state on world load (sleep is ephemeral, not persisted)
        // Sleep goal will re-evaluate conditions and put dragon back to sleep if appropriate
        if (!level().isClientSide) {
            if (sleepLocked || isSleepingEntering() || isSleepingExiting() || isSleeping()) {
                releaseSleepLock();
                wakeUpImmediately();
                suppressSleep(200);
            }
            setSleepingEntering(false);
            setSleepingExiting(false);
            sleepTransitionTicks = 0;
            setSleeping(false);
        }
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

    //LOCATOR
    private final Map<String, Vec3> clientLocatorCache = new ConcurrentHashMap<>();

    public void setClientLocatorPosition(String name, Vec3 pos) {
        if (name == null || pos == null) return;
        this.clientLocatorCache.put(name, pos);
    }

    public Vec3 getClientLocatorPosition(String name) {
        if (name == null) return null;
        return this.clientLocatorCache.get(name);
    }

    @Override
    public float getScreenShakeAmount(float partialTicks) {
        float currentAmount = this.entityData.get(DATA_SCREEN_SHAKE_AMOUNT);
        return prevScreenShakeAmount + (currentAmount - prevScreenShakeAmount) * partialTicks;
    }

    @Override
    public double getShakeDistance() {
        return 18.0;
    }

    @Override
    public boolean canFeelShake(Entity player) {
        return true;
    }

    public void triggerScreenShake(float intensity) {
        float clamped = Math.max(0.0F, intensity);
        if (clamped <= 0.0F) {
            return;
        }
        if (level().isClientSide) {
            return;
        }
        screenShakeAmount = Math.max(screenShakeAmount, clamped);
        this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, screenShakeAmount);
    }

    public boolean canBeBound() {
        return !isDying()
                && !isAccelerating()
                && !areRiderControlsLocked()
                && !isAbilityActive(NulljawAbilities.NULLJAW_PHASE_SHIFT);
    }

    // ===== AMPHIBIOUS BEHAVIOR =====

    @Override
    public boolean shouldEnterWater() {
        // Don't enter water if sitting or being ridden
        if (this.isOrderedToSit() || this.isVehicle()) {
            return false;
        }

        // Emergency: Seek water when on fire or low health with a target
        if (this.isOnFire()) {
            return true;
        }
        if (this.getHealth() < this.getMaxHealth() * 0.5F && this.getTarget() != null) {
            return true;
        }

        // Natural patrol behavior - semi-aquatic predator rotates between environments
        // After ~50 seconds on land (1000 ticks), has 8% chance per check to enter water
        return ticksOutOfWater > 1000 && this.getRandom().nextFloat() < 0.08F;
    }

    @Override
    public boolean shouldLeaveWater() {
        // Don't leave water if sitting
        if (this.isOrderedToSit()) {
            return false;
        }

        // Follow tamed owner onto land if they're far away
        if (this.isTame() && this.getOwner() != null) {
            LivingEntity owner = this.getOwner();
            if (!owner.isInWater() && this.distanceToSqr(owner) > 100.0D) { // 10 blocks
                return true;
            }
        }

        // Natural patrol behavior - after ~60 seconds in water (1200 ticks),
        // has 8% chance per check to leave for land patrol
        return ticksInWater > 1200 && this.getRandom().nextFloat() < 0.08F;
    }
}
