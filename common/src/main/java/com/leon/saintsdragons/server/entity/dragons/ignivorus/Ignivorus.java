package com.leon.saintsdragons.server.entity.dragons.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.network.MessageDragonMeleeMode;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.ignivorus.IgnivorusAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtByTargetGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtTargetGoal;
import com.leon.saintsdragons.server.ai.goals.ignivorus.IgnivorusAirCombatGoal;
import com.leon.saintsdragons.server.ai.goals.ignivorus.IgnivorusGroundCombatGoal;
import com.leon.saintsdragons.server.ai.navigation.DragonFlightMoveHelper;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.controller.ignivorus.IgnivorusPhysicsController;
import com.leon.saintsdragons.server.entity.controller.ignivorus.IgnivorusRiderController;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusSoundProfile;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusTamingHandler;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.interfaces.DragonSleepCapable;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.interfaces.ShakesScreen;
import com.leon.saintsdragons.server.entity.interfaces.SoundHandledDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import javax.annotation.Nonnull;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MoverType;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Map;

public class Ignivorus extends RideableDragonBase implements DragonFlightCapable, SoundHandledDragon, ShakesScreen, DragonSleepCapable {

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
    public static final EntityDataAccessor<Boolean> DATA_RIDER_LOCKED =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> DATA_FEEDING_COOLDOWN =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.INT);
    /** Tracks whether the dragon is stunned during a taming attempt */
    public static final EntityDataAccessor<Boolean> DATA_TAMING_STUNNED =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> DATA_FIRE_BREATHING =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_FIRE_BREATH_PROGRESS =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FIRE_START_SET =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_FIRE_START_X =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FIRE_START_Y =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FIRE_START_Z =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_FIRE_END_SET =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_FIRE_END_X =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FIRE_END_Y =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FIRE_END_Z =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> DATA_SCREEN_SHAKE_AMOUNT =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> DATA_CINEMATIC_ZOOM_ACTIVE =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING_ENTERING =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING_EXITING =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);

    private static final double MODEL_SCALE = 1.0D;

    public static final double RIDER_GLIDE_ALTITUDE_THRESHOLD = 40.0D;
    public static final double RIDER_GLIDE_ALTITUDE_EXIT = 30.0D;
    public static final double RIDER_LOW_ALTITUDE_GLIDE_THRESHOLD = 6.0D;
    public static final double RIDER_WATER_SURFACE_LEVEL = 62.0D;
    public static final double RIDER_WATER_SURFACE_TOLERANCE = 2.0D;
    public static final int RIDER_WATER_SCAN_RADIUS = 2;
    public static final int RIDER_WATER_SCAN_DEPTH = 8;
    private static final double WATER_EFFECT_MAX_HEIGHT = 8.0D;
    private static final double WATER_EFFECT_INTENSITY = 1.15D;
    private static final double LANDING_TRIGGER_ALTITUDE = 6.0D;
    private static final double LANDING_RELEASE_ALTITUDE = 8.5D;
    private static final double LANDING_DESCENT_SPEED = -0.02D;
    private static final int LANDING_HYSTERESIS_TICKS = 6;

    // Vocal entries (placeholder - sounds to be added later)
    private static final Map<String, VocalEntry> VOCAL_ENTRIES =
            new DragonEntity.VocalEntryBuilder()
                    .add("ignivorus_roar", "action", "animation.ignivorus.roar",
                            ModSounds.IGNIVORUS_ROAR, 1.8f, 0.85f, 0.15f,
                            false, false, false)
                    .add("ignivorus_grumble1", "action", "animation.ignivorus.grumble1",
                            ModSounds.IGNIVORUS_GRUMBLE_1, 1.1f, 0.95f, 0.08f,
                            true, false, true)
                    .add("ignivorus_grumble2", "action", "animation.ignivorus.grumble2",
                            ModSounds.IGNIVORUS_GRUMBLE_2, 1.15f, 1.0f, 0.08f,
                            true, false, true)
                    .add("ignivorus_grumble3", "action", "animation.ignivorus.grumble3",
                            ModSounds.IGNIVORUS_GRUMBLE_3, 1.2f, 0.9f, 0.08f,
                            true, false, true)
                    .add("ignivorus_hurt", "hurt", "animation.ignivorus.hurt",
                            ModSounds.IGNIVORUS_HURT, 1.6f, 0.95f, 0.1f,
                            true, true, true)
                    .add("ignivorus_die", "action", "animation.ignivorus.die",
                            ModSounds.IGNIVORUS_DIE, 1.8f, 0.9f, 0.05f,
                            false, true, true)
                    .build();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final IgnivorusAnimationHandler animationHandler = new IgnivorusAnimationHandler(this);
    private final IgnivorusPhysicsController physicsController = new IgnivorusPhysicsController(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final IgnivorusRiderController riderController;
    private final IgnivorusInteractionHandler interactionHandler = new IgnivorusInteractionHandler(this);
    private final IgnivorusTamingHandler tamingController = new IgnivorusTamingHandler(this);

    private final DragonPathNavigateGround groundNav;
    private final FlyingPathNavigation airNav;
    private boolean usingAirNav;

    public int timeFlying = 0;
    private int airTicks;
    public int groundTicks;
    private int riderControlLockTicks;
    private int landingApproachTicks;

    private static final float MAX_FIRE_YAW_DEG = 70.0F;
    private static final float MAX_FIRE_PITCH_DEG = 45.0F;
    private Vec3 fireAimDir;

    // Sleep system state (one-shot transitions: down -> fall_asleep -> wake_up -> up)
    private boolean sleeping = false;
    private boolean sleepingEntering = false;
    private boolean sleepingExiting = false;
    private boolean sleepTransitioning = false;
    private int sleepTransitionTicks = 0;
    private boolean sleepFallAsleepTriggered = false;
    private boolean sleepSitUpTriggered = false;
    private boolean sleepLocked = false;
    private int sleepCommandSnapshot = -1;
    private int sleepSuppressionTicks = 0;

    // Fire breath targeting (AI-driven smart aiming)
    private int fireTime = 0; // Tracks how long fire breath has been active for accuracy ramping
    private Vec3 fireServerTarget = null; // Server-side smooth target position with wobble

    // Banking animation state
    private float bankSmoothedYaw = 0f;
    private int bankHoldTicks = 0;
    private int bankDir = 0;
    private float bankAngle = 0f;
    private float prevBankAngle = 0f;

    // Screen shake system
    private static final float SHAKE_DECAY_PER_TICK = 0.025F;
    private float prevScreenShakeAmount = 0.0F;
    private float screenShakeAmount = 0.0F;

    private float cinematicZoomProgress = 0.0F;
    private float prevCinematicZoomProgress = 0.0F;

    // Ambient vocals
    private int ambientSoundTimer;
    private int nextAmbientSoundDelay;
    private static final int MIN_AMBIENT_DELAY = 180;
    private static final int MAX_AMBIENT_DELAY = 520;

    // Pitching animation state
    private float pitchSmoothedPitch = 0f;
    private int pitchHoldTicks = 0;
    private int pitchDir = 0;

    // Client-side animation initialization grace period (fixes T-pose on world rejoin with shaders)
    private int clientAnimInitTicks = 0;
    private static final int ANIM_INIT_GRACE_PERIOD = 5; // Wait 5 ticks for entity data sync

    // Sitting transition state (1.88 seconds = 38 ticks for both down and up animations)
    private int sitTransitionTicks = 0;
    private boolean isSittingDown = false;
    private boolean isStandingUp = false;

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
        this.moveControl = new net.minecraft.world.entity.ai.control.MoveControl(this); // Start with ground control
        this.usingAirNav = false;

        this.riderController = new IgnivorusRiderController(this);
        resetAmbientSoundTimer();
        if (!level.isClientSide) {
            applyConfiguredAttributes();
        }
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
        this.entityData.define(DATA_RIDER_LOCKED, false);
        this.entityData.define(DATA_FIRE_BREATHING, false);
        this.entityData.define(DATA_FIRE_BREATH_PROGRESS, 0);
        this.entityData.define(DATA_FIRE_START_SET, false);
        this.entityData.define(DATA_FIRE_START_X, 0F);
        this.entityData.define(DATA_FIRE_START_Y, 0F);
        this.entityData.define(DATA_FIRE_START_Z, 0F);
        this.entityData.define(DATA_FIRE_END_SET, false);
        this.entityData.define(DATA_FIRE_END_X, 0F);
        this.entityData.define(DATA_FIRE_END_Y, 0F);
        this.entityData.define(DATA_FIRE_END_Z, 0F);
        this.entityData.define(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        this.entityData.define(DATA_CINEMATIC_ZOOM_ACTIVE, false);
        this.entityData.define(DATA_FEEDING_COOLDOWN, 0);
        this.entityData.define(DATA_TAMING_STUNNED, false);
        this.entityData.define(DATA_SLEEPING, false);
        this.entityData.define(DATA_SLEEPING_ENTERING, false);
        this.entityData.define(DATA_SLEEPING_EXITING, false);
    }

    @Override
    protected void defineRideableDragonData() {
        // Additional rideable dragon data if needed
    }

    public static AttributeSupplier.Builder createAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        double attackDamage = config.extraDouble("attack_damage", 15.0D);
        return createMobAttributes()
            .add(Attributes.MAX_HEALTH, config.maxHealth())
            .add(Attributes.MOVEMENT_SPEED, config.movementSpeed())
            .add(Attributes.FLYING_SPEED, config.flyingSpeed())
            .add(Attributes.ATTACK_DAMAGE, attackDamage)
            .add(Attributes.FOLLOW_RANGE, 128.0D) // Long range to support fire breath at distance
            .add(Attributes.ARMOR, config.armor())
            .add(Attributes.KNOCKBACK_RESISTANCE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        // Priority 0: Sleep (night/owner sleep) - matches Stegonaut state machine with Ignivorus timings
        this.goalSelector.addGoal(0, new com.leon.saintsdragons.server.ai.goals.ignivorus.IgnivorusSleepGoal(this));

        // Priority 1: Combat - highest priority when aggressive
        // Air combat takes precedence when target is airborne and dragon is flying
        this.goalSelector.addGoal(1, new IgnivorusAirCombatGoal(this));
        this.goalSelector.addGoal(1, new IgnivorusGroundCombatGoal(this));

        // Priority 3: Follow owner
        this.goalSelector.addGoal(3, new com.leon.saintsdragons.server.ai.goals.ignivorus.IgnivorusFollowOwnerGoal(this));

        // Priority 4: Flight patrol
        this.goalSelector.addGoal(4, new com.leon.saintsdragons.server.ai.goals.ignivorus.IgnivorusSmartFlightGoal(this));

        // Priority 5: Ground wandering
        this.goalSelector.addGoal(5, new com.leon.saintsdragons.server.ai.goals.ignivorus.IgnivorusGroundWanderGoal(this, 1.0, 120));

        // Priority 12: Look around when idle
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new DragonOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new DragonOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));

    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level,
                                                  @NotNull DifficultyInstance difficulty,
                                                  @NotNull MobSpawnType spawnType,
                                                  @Nullable SpawnGroupData spawnData,
                                                  @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnData, dataTag);
        applyConfiguredAttributes();
        return data;
    }

    @Override
    public void tick() {
        super.tick();
        soundHandler.tick();
        tickRiderControlLock();
        physicsController.tick();
        tickScreenShake();
        tickCinematicZoom();

        // Update client-side sit progress
        if (level().isClientSide) {
            // Increment animation initialization counter (prevents T-pose on rejoin with shaders)
            if (clientAnimInitTicks < ANIM_INIT_GRACE_PERIOD) {
                clientAnimInitTicks++;
            }
            prevSitProgress = sitProgress;
            sitProgress = this.entityData.get(DATA_SIT_PROGRESS);
        }

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
        tickLandingLogic();

        if (!level().isClientSide) {
            if (tamingAbortCalmTicks > 0) {
                tamingAbortCalmTicks--;
            }
            tamingController.tickServer();
            tickTerrainClearing();
            handleAmbientSounds();
            if (tickCount % 2 == 0) {
                tickWaterDisturbance();
            }

            int cooldown = this.entityData.get(DATA_FEEDING_COOLDOWN);
            if (cooldown > 0) {
                this.entityData.set(DATA_FEEDING_COOLDOWN, cooldown - 1);
            }
        }

        // Update sitting progress
        updateSittingProgress();
        // Server-side sleep transition driver
        if (!level().isClientSide) {
            if (sleepSuppressionTicks > 0) {
                sleepSuppressionTicks--;
            }
            tickSleepTransitions();
        }
    }

    @Override
    public boolean hurt(@Nonnull DamageSource damageSource, float amount) {
        // During dying sequence, ignore all damage (entity is already dead, playing death animation)
        if (isDying()) {
            return false;
        }
        // Immune to fire damage
        if (damageSource.is(DamageTypes.IN_FIRE) || damageSource.is(DamageTypes.ON_FIRE) || damageSource.is(DamageTypes.LAVA)) {
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
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        return groundNav;
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        Vec3 input = riderController.getRiddenInput(player, deltaIn);
        return super.getRiddenInput(player, input);
    }

    private void tickRiderControlLock() {
        if (riderControlLockTicks > 0) {
            riderControlLockTicks--;
            if (riderControlLockTicks <= 0) {
                this.entityData.set(DATA_RIDER_LOCKED, false);
            }
        }
    }

    private void handleAmbientSounds() {
        if (isBaby() || isDying() || isSleeping() || isSleepTransitioning()) {
            return;
        }
        if (getTarget() != null || getActiveAbility() != null || isBreathingFire()) {
            return;
        }
        if (isOrderedToSit() || this.isStayOrSitMuted()) {
            return;
        }

        if (ambientSoundTimer < nextAmbientSoundDelay) {
            ambientSoundTimer++;
            return;
        }

        playAmbientGrumble();
        resetAmbientSoundTimer();
    }

    private void playAmbientGrumble() {
        float roll = this.getRandom().nextFloat();
        String vocalKey = roll < 0.34f ? "ignivorus_grumble1"
                : (roll < 0.67f ? "ignivorus_grumble2" : "ignivorus_grumble3");
        this.getSoundHandler().playVocal(vocalKey);
    }

    private void resetAmbientSoundTimer() {
        RandomSource random = getRandom();
        ambientSoundTimer = 0;
        int range = Math.max(1, MAX_AMBIENT_DELAY - MIN_AMBIENT_DELAY);
        nextAmbientSoundDelay = MIN_AMBIENT_DELAY + random.nextInt(range);
    }

    public boolean areRiderControlsLocked() {
        return level().isClientSide ? this.entityData.get(DATA_RIDER_LOCKED) : riderControlLockTicks > 0;
    }

    public boolean canFeed() {
        return this.entityData.get(DATA_FEEDING_COOLDOWN) <= 0;
    }

    public void setFeedingCooldown(int ticks) {
        this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, ticks));
    }

    // ===== TAMING SYSTEM =====
    private static final float TAMING_HEALTH_RATIO = 1.0F / 3.0F;
    private int tamingAbortCalmTicks = 0;

    public boolean isTamingStunned() {
        return this.entityData.get(DATA_TAMING_STUNNED);
    }

    public void enterTamingStun() {
        tamingController.enterStun();
    }

    public void enterTamingHoldState() {
        tamingController.enterHoldState();
    }

    public void setTamingRecoveryTarget(float targetHealth) {
        tamingController.setRecoveryTarget(targetHealth);
    }

    public void clearTamingRecovery() {
        tamingController.clearRecovery();
    }

    public void incrementTamingFailures() {
        tamingController.incrementFailures();
    }

    public void resetTamingFailures() {
        tamingController.resetFailures();
    }

    public int getTamingFailureCounter() {
        return tamingController.getFailureCounter();
    }

    public boolean isAwaitingTamingFeed() {
        return tamingController.isAwaitingFeed();
    }

    public void abortTamingAttempt() {
        clearTamingRecovery();
        tamingAbortCalmTicks = Math.max(tamingAbortCalmTicks, 100);
    }

    public boolean isBelowTamingThreshold() {
        return this.getHealth() <= getTamingThreshold();
    }

    public float getTamingThreshold() {
        return this.getMaxHealth() * TAMING_HEALTH_RATIO;
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

    public void clearRiderControlLock() {
        riderControlLockTicks = 0;
        this.entityData.set(DATA_RIDER_LOCKED, false);
    }

    private void tickCinematicZoom() {
        prevCinematicZoomProgress = cinematicZoomProgress;
        boolean active = this.entityData.get(DATA_CINEMATIC_ZOOM_ACTIVE);
        float target = active ? 1.0F : 0.0F;
        cinematicZoomProgress = Mth.lerp(0.12F, cinematicZoomProgress, target);
        if (Math.abs(cinematicZoomProgress - target) < 0.01F) {
            cinematicZoomProgress = target;
        }
    }

    public void setUltimateCameraZoomActive(boolean active) {
        this.entityData.set(DATA_CINEMATIC_ZOOM_ACTIVE, active);
    }

    public float getUltimateCameraZoom(float partialTicks) {
        return Mth.lerp(partialTicks, prevCinematicZoomProgress, cinematicZoomProgress);
    }

    @Override
    protected boolean isRiderInputLocked(Player player) {
        return areRiderControlsLocked();
    }

    @Override
    protected float getRiderLockPitchMin() {
        return -70.0F;
    }

    @Override
    protected float getRiderLockPitchMax() {
        return 45.0F;
    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        riderController.tickRidden(player, travelVector);
        if (isBreathingFire()) {
                Vec3 start = getFireBreathStartAnchor(1.0f);
                if (start == null) {
                    start = getEyePosition();
                }
                Vec3 aim = refreshFireAimDirection(start, true);
                if (aim != null) {
                    applyFireLook(aim);
                } else {
                    copyRiderLook(player);
                }
        } else {
            resetFireAimDirection();
        }
    }

    @Override
    public void travel(@NotNull Vec3 travelVec) {
        // Block ALL movement when controls are locked (e.g., during ultimate ability)
        if (areRiderControlsLocked()) {
            super.travel(Vec3.ZERO);
            return;
        }

        boolean inWater = this.isInWater() || this.isInWaterOrBubble();

        if (this.isVehicle() && riderController.getRidingPlayer() != null) {
            Player rider = riderController.getRidingPlayer();
            if (inWater) {
                handleWaterSwimming(travelVec);
            } else if (isFlying()) {
                riderController.handleRiderMovement(rider, travelVec);
            } else {
                this.setSpeed(riderController.getRiddenSpeed(rider));
                super.travel(travelVec);
            }
            return;
        }

        if (inWater) {
            handleWaterSwimming(travelVec);
            return;
        }

        super.travel(travelVec);
    }

    @Override
    public float getRiddenSpeed(@NotNull Player rider) {
        return riderController.getRiddenSpeed(rider);
    }

    private void handleWaterSwimming(Vec3 input) {
        Vec3 velocity = this.getDeltaMovement();

        double swimSpeed = 0.4D;
        if (isAccelerating()) {
            swimSpeed *= 1.3D;
        }

        Vec3 desired = getSwimVec3(input, swimSpeed);
        Vec3 blended = velocity.add(desired.subtract(velocity).scale(0.15D));

        double dragFactor = 0.88D;
        blended = blended.multiply(dragFactor, 0.92D, dragFactor);

        double dy = blended.y;
        if (isGoingUp()) {
            dy = Math.min(swimSpeed * 0.6D, dy + 0.08D);
        } else if (isGoingDown()) {
            dy = Math.max(-swimSpeed * 0.8D, dy - 0.12D);
        } else {
            dy -= 0.03D;
        }

        blended = new Vec3(blended.x, dy, blended.z);

        this.setDeltaMovement(blended);
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    private Vec3 getSwimVec3(Vec3 wishDir, double swimSpeed) {
        double strafe = wishDir.x;
        double forward = wishDir.z;
        float yawRad = this.getYRot() * ((float) Math.PI / 180F);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double worldX = strafe * cos - forward * sin;
        double worldZ = forward * cos + strafe * sin;

        double dx = worldX * 0.6D * swimSpeed;
        double dz = worldZ * 0.6D * swimSpeed;

        return new Vec3(dx, 0.0D, dz);
    }

    @Override
    protected void applyRiderMovementInput(Player player, float forward, float strafe, float yaw, boolean locked) {
        // Apply deadzone and store input (locked = 0)
        float fwd = locked ? 0f : applyInputDeadzone(forward);
        float str = locked ? 0f : applyInputDeadzone(strafe);
        setLastRiderForward(fwd);
        setLastRiderStrafe(str);

        // Update ground move state for multiplayer sync
        if (!isFlying()) {
            int moveState = 0; // idle
            float magnitude = Math.abs(fwd) + Math.abs(str);
            if (magnitude > 0.05f) {
                moveState = this.isAccelerating() ? 2 : 1; // run : walk
            }
            // Only update if changed (avoid unnecessary network packets)
            if (this.getEntityData().get(DATA_GROUND_MOVE_STATE) != moveState) {
                this.getEntityData().set(DATA_GROUND_MOVE_STATE, moveState);
            }
        }
    }

    @Override
    protected void handleRiderAction(ServerPlayer player, DragonRiderAction action, String abilityName, boolean locked) {
        if (action == null) {
            return;
        }
        switch (action) {
            case TAKEOFF_REQUEST -> {
                if (!locked) {
                    requestRiderTakeoff();
                }
            }
            case ACCELERATE -> {
                if (!locked) {
                    setAccelerating(true);
                }
            }
            case STOP_ACCELERATE -> setAccelerating(false);
            case TOGGLE_MELEE -> {
                if (!locked) {
                    onRiderToggleMelee(player);
                }
            }
            case ABILITY_USE -> {
                if (!locked && abilityName != null && !abilityName.isEmpty()) {
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

    // Animation initialization system (fixes T-pose on world rejoin with shaders)
    public boolean isClientAnimationReady() {
        return clientAnimInitTicks >= ANIM_INIT_GRACE_PERIOD;
    }

    /**
     * Allows AI goals to explicitly set ground move state for multiplayer sync.
     * Called by AI goals when the dragon is walking, running, or stopping.
     */
    public void setGroundMoveStateFromAI(int state) {
        if (!this.level().isClientSide) {
            int s = Mth.clamp(state, 0, 2); // 0=idle, 1=walk, 2=run
            if (this.entityData.get(DATA_GROUND_MOVE_STATE) != s) {
                this.entityData.set(DATA_GROUND_MOVE_STATE, s);
            }
        }
    }

    public void setGroundMoveStateFromRider(int state) {
        int s = Mth.clamp(state, 0, 2);
        if (this.entityData.get(DATA_GROUND_MOVE_STATE) != s) {
            this.entityData.set(DATA_GROUND_MOVE_STATE, s);
        }
    }

    // ===== SLEEP SYSTEM =====
    @Override
    public boolean isSleeping() {
        return level().isClientSide ? this.entityData.get(DATA_SLEEPING) : sleeping;
    }

    @Override
    public boolean isSleepTransitioning() {
        if (level().isClientSide) {
            return this.entityData.get(DATA_SLEEPING_ENTERING) || this.entityData.get(DATA_SLEEPING_EXITING);
        }
        return sleepTransitioning || sleepingEntering || sleepingExiting;
    }

    public boolean isSleepingEntering() {
        return level().isClientSide ? this.entityData.get(DATA_SLEEPING_ENTERING) : sleepingEntering;
    }

    public boolean isSleepingExiting() {
        return level().isClientSide ? this.entityData.get(DATA_SLEEPING_EXITING) : sleepingExiting;
    }

    public boolean isSleepLocked() {
        return sleeping || sleepingEntering || sleepingExiting || sleepTransitioning || sleepLocked;
    }

    @Override
    public void startSleepEnter() {
        if (sleeping || sleepingEntering || sleepingExiting || sleepTransitioning) {
            return;
        }
        sleepTransitioning = true;
        sleepingEntering = true;
        sleepingExiting = false;
        sleeping = false;
        sleepFallAsleepTriggered = false;
        sleepSitUpTriggered = false;
        sleepLocked = true;
        sleepCommandSnapshot = this.getCommand();

        this.entityData.set(DATA_SLEEPING_ENTERING, true);
        this.entityData.set(DATA_SLEEPING_EXITING, false);
        this.entityData.set(DATA_SLEEPING, false);

        setGroundMoveStateFromAI(0);
        setFlying(false);
        setHovering(false);
        setTakeoff(false);
        setLanding(false);
        // Only issue sit_down if not already fully seated before the chain begins
        boolean alreadySeated = this.getSitProgress() >= this.maxSitTicks();
        if (!alreadySeated) {
            this.setOrderedToSit(false);
        }

        if (isOrderedToSit() || this.getSitProgress() >= this.maxSitTicks()) {
            sleepTransitionTicks = 1;
        } else {
            sleepTransitionTicks = getSleepSitDownDuration();
            animationHandler.triggerSitDownAnimation();
        }
    }

    @Override
    public void startSleepExit() {
        if ((!sleeping && !sleepingEntering) || sleepingExiting) {
            return;
        }
        sleeping = false;
        sleepingEntering = false;
        sleepingExiting = true;
        sleepTransitioning = true;
        sleepSitUpTriggered = false;
        sleepTransitionTicks = getSleepWakeUpDuration();

        this.entityData.set(DATA_SLEEPING, false);
        this.entityData.set(DATA_SLEEPING_ENTERING, false);
        this.entityData.set(DATA_SLEEPING_EXITING, true);

        setGroundMoveStateFromAI(0);
        setOrderedToSit(true);
        animationHandler.triggerWakeUpAnimation();
        suppressSleep(40);
    }

    public void wakeUpImmediately() {
        suppressSleep(40);
        sleepTransitionTicks = 0;
        sleepTransitioning = false;
        sleepFallAsleepTriggered = false;
        sleepSitUpTriggered = false;
        sleeping = false;
        sleepingEntering = false;
        sleepingExiting = false;
        sleepLocked = false;
        sleepCommandSnapshot = -1;
        this.entityData.set(DATA_SLEEPING, false);
        this.entityData.set(DATA_SLEEPING_ENTERING, false);
        this.entityData.set(DATA_SLEEPING_EXITING, false);
        setOrderedToSit(false);
        setGroundMoveStateFromAI(0);
    }

    public void suppressSleep(int ticks) {
        sleepSuppressionTicks = Math.max(sleepSuppressionTicks, ticks);
    }

    @Override
    public boolean isSleepSuppressed() {
        return sleepSuppressionTicks > 0 || getTarget() != null || isFlying() || isInWaterOrBubble() || isVehicle();
    }

    @Override
    public SleepPreferences getSleepPreferences() {
        return new SleepPreferences(
                true,   // canSleepAtNight
                false,  // canSleepDuringDay
                false,  // requiresShelter
                false,  // avoidsThunderstorms
                true    // sleepsNearOwner
        );
    }

    @Override
    public boolean canSleepNow() {
        boolean ownerSleeping = false;
        if (isTame()) {
            var owner = getOwner();
            ownerSleeping = owner instanceof Player player && player.isSleeping();
        }
        return !level().isDay() || ownerSleeping;
    }

    public int getSleepSitDownDuration() {
        return 38; // animation "down" length
    }

    public int getSleepSitUpDuration() {
        return 38; // animation "up" length
    }

    public int getSleepFallAsleepDuration() {
        return 38; // animation "fall_asleep" length
    }

    public int getSleepWakeUpDuration() {
        return 38; // animation "wake_up" length
    }

    private void tickSleepTransitions() {
        if (!(sleeping || sleepingEntering || sleepingExiting || sleepTransitioning)) {
            return;
        }

        freezeDuringSleepChain();

        if (sleepingEntering) {
            if (!sleepFallAsleepTriggered) {
                if (sleepTransitionTicks > 0) {
                    sleepTransitionTicks--;
                    if (sleepTransitionTicks > 0) {
                        return;
                    }
                }
                boolean seatedEnough = isOrderedToSit() || getSitProgress() >= maxSitTicks();
                if (seatedEnough) {
                    sleepFallAsleepTriggered = true;
                    sleepTransitionTicks = getSleepFallAsleepDuration();
                    animationHandler.triggerFallAsleepAnimation();
                    return;
                }
                // Trigger sit_down and wait for it to complete
                sleepTransitionTicks = getSleepSitDownDuration();
                animationHandler.triggerSitDownAnimation();
                setOrderedToSit(true);
                return;
            }

            if (sleepTransitionTicks > 0) {
                sleepTransitionTicks--;
                if (sleepTransitionTicks > 0) {
                    return;
                }
            }

            sleeping = true;
            sleepingEntering = false;
            sleepTransitioning = false;
            sleepFallAsleepTriggered = false;
            this.entityData.set(DATA_SLEEPING_ENTERING, false);
            this.entityData.set(DATA_SLEEPING, true);
            animationHandler.triggerSleepAnimation();
            setOrderedToSit(true);
            setGroundMoveStateFromAI(0);
            return;
        }

        if (sleepingExiting) {
            if (sleepTransitionTicks > 0) {
                sleepTransitionTicks--;
                if (sleepTransitionTicks > 0) {
                    return;
                }
            }

            sleepingExiting = false;
            sleepTransitioning = false;
            sleepSitUpTriggered = false;
            this.entityData.set(DATA_SLEEPING_EXITING, false);
            this.entityData.set(DATA_SLEEPING, false);

            sleepLocked = false;
            int desired = sleepCommandSnapshot;
            sleepCommandSnapshot = -1;
            boolean ownerWantsSit = desired == 1;
            setOrderedToSit(ownerWantsSit);
            setGroundMoveStateFromAI(0);
        }
    }

    private void freezeDuringSleepChain() {
        this.getNavigation().stop();
        this.setDeltaMovement(0, 0, 0);
        this.setRunning(false);
        this.setGroundMoveStateFromAI(0);
        this.setFlying(false);
        this.setHovering(false);
        this.setTakeoff(false);
        this.setLanding(false);
        this.setOrderedToSit(true);
    }

    public void useRidingAbility(String abilityName) {
        if (abilityName == null || abilityName.isEmpty()) {
            return;
        }
        Entity controller = this.getControllingPassenger();
        if (!(controller instanceof LivingEntity living)) {
            return;
        }
        if (this.isTame() && controller instanceof Player player && !this.isOwnedBy(player)) {
            return;
        }
        var type = AbilityRegistry.get(abilityName);
        if (type != null) {
            this.combatManager.tryUseAbility(type);
        }
    }

    public void forceEndActiveAbility() {
        this.combatManager.forceEndActiveAbility();
        clearFireBreathPath();
        setBreathingFire(false);
    }

    public void forceEndAbility(DragonAbilityType<?, ?> abilityType) {
        combatManager.forceEndAbility(abilityType);
        // Clear fire breath state if fire breath was cancelled
        if (abilityType == IgnivorusAbilities.IGNIVORUS_FIRE_BREATH) {
            clearFireBreathPath();
            setBreathingFire(false);
        }
    }

    public boolean isAbilityActive(DragonAbilityType<?, ?> abilityType) {
        return combatManager.isAbilityActive(abilityType);
    }

    @Override
    public RiderAbilityBinding getPrimaryRiderAbility() {
        return new RiderAbilityBinding(IgnivorusAbilities.IGNIVORUS_ROAR_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getSecondaryRiderAbility() {
        return new RiderAbilityBinding(IgnivorusAbilities.IGNIVORUS_ULTIMATE_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getTertiaryRiderAbility() {
        return new RiderAbilityBinding(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH_ID, RiderAbilityBinding.Activation.HOLD);
    }

    @Override
    public RiderAbilityBinding getAttackRiderAbility() {
        String abilityId = getMeleeMode() == 1
                ? IgnivorusAbilities.IGNIVORUS_BODY_SLAM_ID
                : IgnivorusAbilities.IGNIVORUS_BITE_ID;
        return new RiderAbilityBinding(abilityId, RiderAbilityBinding.Activation.PRESS);
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
        return stack.is(Items.SALMON) ||
               stack.is(Items.COD) ||
               stack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
    }

    public void setCommandManual(int command) {
        this.setCommand(command);
    }

    // ===== RIDER INPUT HANDLERS =====

    @Override
    protected void onRiderToggleMelee(Player player) {
        if ((isFlying() || isTakeoff() || isLanding() || isHovering()) && player instanceof ServerPlayer serverPlayer && !level().isClientSide) {
            serverPlayer.displayClientMessage(
                    Component.translatable("saintsdragons.message.ignivorus_secondary_ground_only"),
                    true
            );
            syncMeleeMode(serverPlayer);
            return;
        }
        super.onRiderToggleMelee(player);
        if (!level().isClientSide) {
            syncMeleeMode(player);
        }
    }

    @Override
    protected void onRiderTakeoffRequest(Player player) {
        if (!isFlying() && onGround()) {
            enforcePrimaryMeleeForFlight(player);
            riderController.requestRiderTakeoff();
        }
    }

    public void requestRiderTakeoff() {
        riderController.requestRiderTakeoff();
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
            this.moveControl = new DragonFlightMoveHelper(this);
            this.usingAirNav = true;
        } else if (!flying && usingAirNav) {
            this.navigation = this.groundNav;
            this.moveControl = new net.minecraft.world.entity.ai.control.MoveControl(this);
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

    /**
     * Whether this Ignivorus can be stored inside a binder.
     */
    public boolean canBeBound() {
        return !isFlying()
                && !isDying()
                && !isBreathingFire()
                && !areRiderControlsLocked()
                && getActiveAbility() == null;
    }

    private void applyConfiguredAttributes() {
        if (this.level().isClientSide) {
            return;
        }
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        double attackDamage = config.extraDouble("attack_damage", 15.0D);

        setAttributeBase(Attributes.MAX_HEALTH, config.maxHealth());
        setAttributeBase(Attributes.MOVEMENT_SPEED, config.movementSpeed());
        setAttributeBase(Attributes.FLYING_SPEED, config.flyingSpeed());
        setAttributeBase(Attributes.ARMOR, config.armor());
        setAttributeBase(Attributes.ATTACK_DAMAGE, attackDamage);

        double maxHealth = config.maxHealth();
        if (this.getHealth() > maxHealth) {
            this.setHealth((float) maxHealth);
        }
    }

    private void setAttributeBase(Attribute attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
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
                enforcePrimaryMeleeForFlight(getControllingPassenger() instanceof Player p ? p : null);
                switchNavigation(true);
                setRunning(false);
            } else {
                switchNavigation(false);
            }
        }
    }

    public void setTakeoff(boolean takeoff) {
        boolean wasTakeoff = isTakeoff();
        this.entityData.set(DATA_TAKEOFF, takeoff);
        if (takeoff && !wasTakeoff && !level().isClientSide) {
            float pitch = 0.9f + this.getRandom().nextFloat() * 0.15f;
            this.playSound(ModSounds.IGNIVORUS_TAKEOFF.get(), 1.4f, pitch);
        }
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

    public boolean isBreathingFire() {
        return this.entityData.get(DATA_FIRE_BREATHING);
    }

    public void setBreathingFire(boolean breathing) {
        boolean wasBreathing = this.entityData.get(DATA_FIRE_BREATHING);
        this.entityData.set(DATA_FIRE_BREATHING, breathing);
        if (!breathing) {
            resetFireAimDirection();
            setFireBreathProgress(0);
            fireTime = 0;
            fireServerTarget = null;
        }
        if (breathing && !wasBreathing) {
            // Just started breathing - initialize targeting
            fireTime = 0;
            fireServerTarget = createInitialFireTarget();
        }
    }

    /**
     * Gets the fire breath stream progress (0-40).
     * Used for animating the stream extending over time.
     */
    public int getFireBreathProgress() {
        return this.entityData.get(DATA_FIRE_BREATH_PROGRESS);
    }

    /**
     * Sets the fire breath stream progress (0-40).
     * 0 = no stream, 40 = full range stream.
     */
    public void setFireBreathProgress(int progress) {
        this.entityData.set(DATA_FIRE_BREATH_PROGRESS, Mth.clamp(progress, 0, 40));
    }

    public void syncFireBreathPath(@Nullable Vec3 start, @Nullable Vec3 end) {
        setFireBreathStart(start);
        setFireBreathTarget(end);
    }

    public void clearFireBreathPath() {
        setFireBreathStart(null);
        setFireBreathTarget(null);
    }

    @Nullable
    public Vec3 getFireBreathStart() {
        if (!this.entityData.get(DATA_FIRE_START_SET)) {
            return null;
        }
        return new Vec3(
            this.entityData.get(DATA_FIRE_START_X),
            this.entityData.get(DATA_FIRE_START_Y),
            this.entityData.get(DATA_FIRE_START_Z)
        );
    }

    @Nullable
    public Vec3 getFireBreathTarget() {
        if (!this.entityData.get(DATA_FIRE_END_SET)) {
            return null;
        }
        return new Vec3(
            this.entityData.get(DATA_FIRE_END_X),
            this.entityData.get(DATA_FIRE_END_Y),
            this.entityData.get(DATA_FIRE_END_Z)
        );
    }

    public Vec3 getFireBreathStartAnchor(float partialTicks) {
        Vec3 clientBone = getClientLocatorPosition("fireBoneOrigin");
        if (clientBone != null) {
            return clientBone;
        }
        return computeFireBoneFallback(partialTicks);
    }

    private Vec3 computeFireBoneFallback(float partialTicks) {
        double x = Mth.lerp(partialTicks, this.xo, this.getX());
        double y = Mth.lerp(partialTicks, this.yo, this.getY());
        double z = Mth.lerp(partialTicks, this.zo, this.getZ());

        float yawDeg = Mth.lerp(partialTicks, this.yHeadRotO, this.yHeadRot);
        float pitchDeg = Mth.lerp(partialTicks, this.xRotO, this.getXRot());

        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);

        // FireBone position in model: [-0.06603, 59.45, -245.05] pixels
        // Converted to blocks (÷16) and applied to local coordinate system
        double localRight = (-0.06603D / 16.0D) * MODEL_SCALE;  // ≈0 (centered)
        double localUp = (59.45D / 16.0D) * MODEL_SCALE;        // 3.716 blocks up
        double localForward = (245.05D / 16.0D) * MODEL_SCALE;  // 15.316 blocks forward

        double cp = Math.cos(pitch);
        double sp = Math.sin(pitch);
        double pitchedUp = localUp * cp - localForward * sp;
        double pitchedForward = localUp * sp + localForward * cp;

        double cy = Math.cos(yaw);
        double sy = Math.sin(yaw);
        double offX = localRight * cy - pitchedForward * sy;
        double offZ = localRight * sy + pitchedForward * cy;

        return new Vec3(x + offX, y + pitchedUp, z + offZ);
    }

    public Vec3 refreshFireAimDirection(Vec3 start, boolean smooth) {
        Vec3 desired = computeRawFireAimDirection(start);
        if (desired == null) {
            resetFireAimDirection();
            return null;
        }
        Vec3 clamped = clampFireDirection(desired);
        if (clamped == null) {
            resetFireAimDirection();
            return null;
        }

        if (fireAimDir == null) {
            fireAimDir = clamped;
        } else if (smooth) {
            double blend = 0.35D;
            fireAimDir = fireAimDir.add(clamped.subtract(fireAimDir).scale(blend));
            double len = fireAimDir.length();
            if (len > 1.0E-6) {
                fireAimDir = fireAimDir.scale(1.0 / len);
            } else {
                fireAimDir = clamped;
            }
        } else {
            fireAimDir = clamped;
        }
        return fireAimDir;
    }

    private Vec3 computeRawFireAimDirection(Vec3 start) {
        // Rider always has perfect control
        Entity controller = this.getControllingPassenger();
        if (controller instanceof LivingEntity rider) {
            Vec3 look = rider.getLookAngle();
            if (look.lengthSqr() > 1.0E-6) {
                return look.normalize();
            }
        }

        // AI targeting with smart tracking
        if (!level().isClientSide) {
            tickFireTargeting(start);
        }

        if (fireServerTarget != null) {
            Vec3 towardTarget = fireServerTarget.subtract(start);
            if (towardTarget.lengthSqr() > 1.0E-6) {
                return towardTarget.normalize();
            }
        }

        // Fallback to looking direction
        Vec3 fallback = Vec3.directionFromRotation(this.getXRot(), this.yHeadRot);
        return fallback.lengthSqr() > 1.0E-6 ? fallback.normalize() : null;
    }

    private Vec3 clampFireDirection(Vec3 desiredDir) {
        if (desiredDir == null || desiredDir.lengthSqr() < 1.0E-6) {
            return null;
        }
        Vec3 dir = desiredDir.normalize();
        float desiredYaw = (float) (Math.atan2(-dir.x, dir.z) * (180.0F / Math.PI));
        float desiredPitch = (float) (-Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * (180.0F / Math.PI));

        float headYaw = this.yHeadRot;
        float headPitch = this.getXRot();

        float yawErr = Mth.degreesDifference(headYaw, desiredYaw);
        float pitchErr = desiredPitch - headPitch;

        float finalYaw = headYaw + Mth.clamp(yawErr, -MAX_FIRE_YAW_DEG, MAX_FIRE_YAW_DEG);
        float finalPitch = headPitch + Mth.clamp(pitchErr, -MAX_FIRE_PITCH_DEG, MAX_FIRE_PITCH_DEG);

        Vec3 finalDir = Vec3.directionFromRotation(finalPitch, finalYaw);
        return finalDir.lengthSqr() > 1.0E-6 ? finalDir.normalize() : null;
    }

    private void applyFireLook(Vec3 aimDir) {
        if (aimDir == null) {
            return;
        }
        float desiredYaw = (float) (Math.atan2(-aimDir.x, aimDir.z) * (180.0 / Math.PI));
        float desiredPitch = (float) (-Math.atan2(aimDir.y, Math.sqrt(aimDir.x * aimDir.x + aimDir.z * aimDir.z)) * (180.0 / Math.PI));

        float headYawSpeed = 12.0F;
        float headPitchSpeed = 9.0F;

        this.yHeadRot = Mth.approachDegrees(this.yHeadRot, desiredYaw, headYawSpeed);

        float currentPitch = this.getXRot();
        float pitchDelta = desiredPitch - currentPitch;
        float pitchChange = Mth.clamp(pitchDelta, -headPitchSpeed, headPitchSpeed);
        this.setXRot(currentPitch + pitchChange);

        float yawDiff = Mth.degreesDifferenceAbs(desiredYaw, Mth.wrapDegrees(this.yBodyRot));
        if (yawDiff > MAX_FIRE_YAW_DEG * 0.65F) {
            float bodySpeed = 6.0F;
            this.setYRot(Mth.approachDegrees(this.getYRot(), desiredYaw, bodySpeed));
            this.yBodyRot = Mth.approachDegrees(this.yBodyRot, desiredYaw, bodySpeed);
        }
    }

    private void resetFireAimDirection() {
        fireAimDir = null;
    }

    /**
     * Creates initial fire breath target position with wild random offset.
     * Makes the breath start very inaccurate.
     */
    private Vec3 createInitialFireTarget() {
        LivingEntity target = getTarget();
        Vec3 shootFrom = getFireBreathStartAnchor(1.0f);
        if (shootFrom == null) {
            shootFrom = position().add(0, getBbHeight() * 0.5, 0);
        }

        if (target != null && target.isAlive()) {
            // Start with huge random offset around target
            Vec3 randomOffset = new Vec3(
                -50 + random.nextFloat() * 100F,  // ±50 blocks X
                -20 + random.nextFloat() * 40F,    // ±20 blocks Y
                -50 + random.nextFloat() * 100F    // ±50 blocks Z
            );
            return target.position().add(randomOffset);
        } else {
            // No target - aim forward with random spread
            Vec3 forward = new Vec3(0, random.nextBoolean() ? 50 : 10, 30)
                .yRot((float) Math.toRadians(-this.yBodyRot));
            return shootFrom.add(forward);
        }
    }

    /**
     * Updates fire breath targeting with accuracy ramping and dynamic wobble.
     * Called each tick while breathing to smoothly track targets.
     */
    private void tickFireTargeting(Vec3 shootFrom) {
        fireTime++;

        LivingEntity target = getTarget();
        Vec3 currentTarget = fireServerTarget != null ? fireServerTarget : shootFrom;

        if (target != null && target.isAlive()) {
            // Calculate accuracy: starts at 100% inaccurate, converges to 0% over 60 ticks
            float maxFireTime = 60.0F;
            float time = (float) fireTime / maxFireTime;
            float accuracy = 1.0F - (Math.min(0.75F, time) / 0.75F);

            // Create dynamic wobble pattern that scales with inaccuracy
            Vec3 wobbleOffset = new Vec3(
                Math.sin(tickCount * 0.2F) * 4.0,
                Math.sin(tickCount * 0.15F) * 2.0,
                Math.cos(tickCount * 0.2F) * -4.0
            ).yRot((float) Math.toRadians(-this.yBodyRot)).scale(accuracy);

            // Aim point with wobble
            Vec3 targetPoint = target.getEyePosition().add(0, -0.2, 0).add(wobbleOffset);

            // Smooth approach: only move 10% toward desired position each tick
            Vec3 approach = targetPoint.subtract(currentTarget).scale(0.1F).add(currentTarget);
            fireServerTarget = approach;
        } else {
            // No target - slowly sweep the breath forward
            Vec3 sweepOffset = new Vec3(
                Math.sin(tickCount * 0.1F) * 10,
                0,
                6
            ).yRot((float) Math.toRadians(-this.yBodyRot));
            Vec3 sweepTarget = shootFrom.add(sweepOffset);
            Vec3 approach = sweepTarget.subtract(currentTarget).scale(0.1F).add(currentTarget);
            fireServerTarget = approach;
        }
    }


    private void setFireBreathStart(@Nullable Vec3 pos) {
        if (pos == null) {
            this.entityData.set(DATA_FIRE_START_SET, false);
            return;
        }
        this.entityData.set(DATA_FIRE_START_SET, true);
        this.entityData.set(DATA_FIRE_START_X, (float) pos.x);
        this.entityData.set(DATA_FIRE_START_Y, (float) pos.y);
        this.entityData.set(DATA_FIRE_START_Z, (float) pos.z);
    }

    private void setFireBreathTarget(@Nullable Vec3 pos) {
        if (pos == null) {
            this.entityData.set(DATA_FIRE_END_SET, false);
            return;
        }
        this.entityData.set(DATA_FIRE_END_SET, true);
        this.entityData.set(DATA_FIRE_END_X, (float) pos.x);
        this.entityData.set(DATA_FIRE_END_Y, (float) pos.y);
        this.entityData.set(DATA_FIRE_END_Z, (float) pos.z);
    }

    // ===== UTILITY METHODS =====

    @Override
    public Vec3 getMouthPosition() {
        // Use the mouth_origin locator from the .geo file
        Vec3 mouthLocator = getClientLocatorPosition("mouth_origin");
        if (mouthLocator != null) {
            return mouthLocator;
        }
        // Fallback calculation using mouth_origin position from model
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

        // mouth_origin position in model - use same as fireBone for now
        // (You can adjust these coordinates if mouth_origin is in a different position)
        double localRight = (-0.06603D / 16.0D) * MODEL_SCALE;
        double localUp = (59.45D / 16.0D) * MODEL_SCALE;
        double localForward = (245.05D / 16.0D) * MODEL_SCALE;

        double cp = Math.cos(pitch);
        double sp = Math.sin(pitch);
        double pitchedUp = localUp * cp - localForward * sp;
        double pitchedForward = localUp * sp + localForward * cp;

        double cy = Math.cos(yaw);
        double sy = Math.sin(yaw);
        double offX = localRight * cy - pitchedForward * sy;
        double offZ = localRight * sy + pitchedForward * cy;

        return new Vec3(x + offX, y + pitchedUp, z + offZ);
    }

    @Override
    public Vec3 getHeadPosition() {
        // Simple fallback: return eye position
        return this.getEyePosition();
    }

    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        return getMeleeMode() == 1 ? IgnivorusAbilities.IGNIVORUS_BODY_SLAM : IgnivorusAbilities.IGNIVORUS_BITE;
    }

    private void enforcePrimaryMeleeForFlight(@Nullable Player rider) {
        if (level().isClientSide || getMeleeMode() == 0) {
            return;
        }
        setMeleeMode(0);
        syncMeleeMode(rider);
    }

    private void syncMeleeMode(@Nullable Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendToPlayer(serverPlayer, new MessageDragonMeleeMode(getMeleeMode()));
        }
    }


    @Override
    public DragonAbilityType<?, ?> getRoaringAbility() {
        return IgnivorusAbilities.IGNIVORUS_ROAR;
    }

    @Override
    public DragonAbilityType<?, ?> getChannelingAbility() {
        return IgnivorusAbilities.IGNIVORUS_FIRE_BREATH;
    }

    @Override
    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return IgnivorusAbilities.IGNIVORUS_HURT;
    }

    @Override
    protected DragonAbilityType<?, ?> getDeathAbilityType() {
        return IgnivorusAbilities.IGNIVORUS_DIE;
    }

    @Override
    public int getDeathAnimationDurationTicks() {
        return 80; // 4 seconds to match the die animation length
    }

    @Override
    public void onDeathAbilityStarted() {
        setBreathingFire(false);
        clearFireBreathPath();
        super.onDeathAbilityStarted();
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

    private void tickLandingLogic() {
        if (!isFlying()) {
            if (isLanding()) {
                setLanding(false);
            }
            landingApproachTicks = 0;
            return;
        }

        double altitude = getAltitudeAboveTerrain();
        Vec3 motion = getDeltaMovement();
        boolean descending = motion.y <= LANDING_DESCENT_SPEED;
        boolean nearGround = altitude != Double.POSITIVE_INFINITY && altitude <= LANDING_TRIGGER_ALTITUDE;

        if (nearGround && descending && !isVehicle()) {
            if (landingApproachTicks < LANDING_HYSTERESIS_TICKS) {
                landingApproachTicks++;
            }
            if (landingApproachTicks >= LANDING_HYSTERESIS_TICKS && !isLanding()) {
                setLanding(true);
            }
        } else {
            landingApproachTicks = 0;
            if (isLanding()) {
                boolean tooHigh = altitude == Double.POSITIVE_INFINITY || altitude > LANDING_RELEASE_ALTITUDE;
                boolean ascending = motion.y > 0.05D;
                if (tooHigh || ascending || onGround()) {
                    setLanding(false);
                }
            }
        }

        if (isLanding() && onGround()) {
            setLanding(false);
        }
    }

    private double getAltitudeAboveTerrain() {
        BlockPos pos = this.blockPosition();
        if (!level().hasChunkAt(pos)) {
            return Double.POSITIVE_INFINITY;
        }

        int groundY = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        int dragonY = Mth.floor(this.getY());
        int scanBottom = Math.min(groundY, dragonY - 12);

        for (int y = dragonY; y >= scanBottom; y--) {
            BlockPos check = new BlockPos(pos.getX(), y, pos.getZ());
            if (!this.level().getFluidState(check).isEmpty()) {
                return Double.POSITIVE_INFINITY;
            }
        }

        return this.getY() - groundY;
    }

    private void tickWaterDisturbance() {
        if (level().isClientSide || !isFlying()) {
            return;
        }

        Vec3 pos = position();
        AABB box = getBoundingBox();
        ServerLevel serverLevel = (ServerLevel) level();

        for (int checkDown = 0; checkDown < WATER_EFFECT_MAX_HEIGHT; checkDown++) {
            BlockPos checkPos = new BlockPos(
                    Mth.floor(pos.x),
                    Mth.floor(pos.y) - checkDown,
                    Mth.floor(pos.z)
            );

            BlockState state = level().getBlockState(checkPos);
            if (!state.getFluidState().isEmpty()) {
                double waterY = checkPos.getY() + 1.0;
                double boxWidth = box.getXsize();
                double boxLength = box.getZsize();
                int particleCount = (int) Math.ceil((boxWidth + boxLength) / 2.0 * WATER_EFFECT_INTENSITY * 8.0);
                particleCount = Math.min(particleCount, 40);

                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * boxWidth;
                    double offsetZ = (random.nextDouble() - 0.5) * boxLength;
                    double particleX = pos.x + offsetX;
                    double particleZ = pos.z + offsetZ;

                    serverLevel.sendParticles(
                            ParticleTypes.SPLASH,
                            particleX, waterY, particleZ,
                            1,
                            offsetX * 0.2, 0.1, offsetZ * 0.2,
                            0.1
                    );

                    if (random.nextFloat() < 0.25f) {
                        serverLevel.sendParticles(
                                ParticleTypes.BUBBLE_POP,
                                particleX, waterY, particleZ,
                                1,
                                0.0, 0.0, 0.0,
                                0.0
                        );
                    }
                }

                break;
            }
        }
    }

    private void tickTerrainClearing() {
        if (level().isClientSide || this.isBaby() || !this.isAlive()) {
            return;
        }
        if (!level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }

        // Only break blocks when moving OR being ridden (prevent stationary destruction when wild)
        boolean isBeingRidden = this.isVehicle();
        Vec3 velocity = this.getDeltaMovement();
        double speed = velocity.horizontalDistanceSqr();

        // If not being ridden, require movement. If being ridden, always break blocks (rider controls movement)
        if (!isBeingRidden && speed < 0.01) {
            return; // Not moving enough to break blocks
        }

        int tickInterval = isBeingRidden ? 1 : 3;
        if (this.tickCount % tickInterval != 0) {
            return;
        }

        AABB rawBounds = this.getBoundingBox();
        AABB bounds = rawBounds.inflate(0.1); // include the collision skin so collisions still get cleared
        if (isBeingRidden) {
            Vec3 planarVelocity = new Vec3(velocity.x, 0.0, velocity.z);
            if (planarVelocity.lengthSqr() > 0.0004) {
                double reach = isBeingRidden ? 1.1 : 0.6;
                Vec3 forwardProbe = planarVelocity.normalize().scale(reach); // push ahead further when ridden for instant clearing
                bounds = bounds.expandTowards(forwardProbe.x, 0.0, forwardProbe.z);
            }
        }
        int minX = Mth.floor(bounds.minX);
        int maxX = Mth.floor(bounds.maxX);
        int minZ = Mth.floor(bounds.minZ);
        int maxZ = Mth.floor(bounds.maxZ);
        int baseY = Mth.floor(rawBounds.minY);
        int minBreakY = baseY + 1;  // Start above the feet
        int maxY = Mth.floor(bounds.maxY);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int brokenThisTick = 0;
        int maxBreakPerTick = isBeingRidden ? 24 : 8; // mounted dragons chew through more blocks instantly

        for (int x = minX; x <= maxX; x++) {
            for (int y = minBreakY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (brokenThisTick >= maxBreakPerTick) {
                        return; // Hit the limit for this tick
                    }

                    cursor.set(x, y, z);
                    if (!level().hasChunkAt(cursor)) {
                        continue;
                    }

                    BlockState state = level().getBlockState(cursor);
                    if (state.isAir() || !state.getFluidState().isEmpty()) {
                        continue;
                    }

                    // Skip indestructible blocks and block entities
                    float hardness = state.getDestroySpeed(level(), cursor);
                    if (hardness < 0 || hardness > 5.0F || state.hasBlockEntity()) {
                        continue; // Skip bedrock, obsidian-like blocks, and tile entities
                    }

                    level().destroyBlock(cursor, true, this);
                    brokenThisTick++;
                }
            }
        }
    }

    private void updateSittingProgress() {
        if (level().isClientSide) {
            return;
        }

        // Tick down sit transition animations
        if (sitTransitionTicks > 0) {
            sitTransitionTicks--;
            if (sitTransitionTicks == 0) {
                // Transition animation finished
                isSittingDown = false;
                isStandingUp = false;
            }
        }

        if (this.isOrderedToSit()) {
            // Trigger sit down animation when starting from standing OR interrupting stand-up
            if ((sitProgress == 0f || isStandingUp) && !isSittingDown) {
                animationHandler.triggerSitDownAnimation();
                isSittingDown = true;
                isStandingUp = false;
                sitTransitionTicks = getSitDownAnimationTicks();
            }

            // Increment sitProgress smoothly
            if (sitProgress < maxSitTicks()) {
                sitProgress++;
                this.entityData.set(DATA_SIT_PROGRESS, sitProgress);
            }
        } else {
            // Not ordered to sit - handle standing up
            if (this.isVehicle()) {
                // Instantly reset when ridden
                if (sitProgress != 0f) {
                    sitProgress = 0f;
                    prevSitProgress = 0f;
                    this.entityData.set(DATA_SIT_PROGRESS, 0f);
                    isSittingDown = false;
                    isStandingUp = false;
                    sitTransitionTicks = 0;
                }
            } else if (sitProgress > 0f) {
                // Trigger sit up animation when at max sitting OR interrupting sit-down
                if ((sitProgress == maxSitTicks() || isSittingDown) && !isStandingUp) {
                    animationHandler.triggerSitUpAnimation();
                    isStandingUp = true;
                    isSittingDown = false;
                    sitTransitionTicks = getSitUpAnimationTicks();
                }

                // Decrement sitProgress (same speed as increment for Ignivorus)
                sitProgress--;
                if (sitProgress < 0f) {
                    sitProgress = 0f;
                }
                this.entityData.set(DATA_SIT_PROGRESS, sitProgress);
            }
        }
    }

    /**
     * Get the duration of the sit down animation in ticks.
     * Ignivorus sit down animation is 1.88 seconds (38 ticks at 20 TPS)
     */
    public int getSitDownAnimationTicks() {
        return 38;
    }

    /**
     * Get the duration of the sit up animation in ticks.
     * Ignivorus sit up animation is 1.88 seconds (38 ticks at 20 TPS)
     */
    public int getSitUpAnimationTicks() {
        return 38;
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
        // Movement controller handles idle/walk/run/flight/sit animations
        AnimationController<Ignivorus> movementController =
            new AnimationController<>(this, "movement", 5, animationHandler::handleMovementAnimation);
        movementController.setSoundKeyframeHandler(this::onAnimationSound);

        // Banking and pitching controllers for flight dynamics
        AnimationController<Ignivorus> bankingController =
            new AnimationController<>(this, "banking", 8, animationHandler::bankingPredicate);

        AnimationController<Ignivorus> pitchingController =
            new AnimationController<>(this, "pitching", 6, animationHandler::pitchingPredicate);

        // Action controller for triggerable animations (sit transitions, fire breath, etc.)
        AnimationController<Ignivorus> actionController =
            new AnimationController<>(this, "action", 5, state -> software.bernie.geckolib.core.object.PlayState.STOP);

        AnimationController<Ignivorus> hurtController =
            new AnimationController<>(this, "hurt", 3, state -> software.bernie.geckolib.core.object.PlayState.STOP);
        hurtController.triggerableAnim("ignivorus_hurt",
            software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlay("animation.ignivorus.hurt"));
        hurtController.setSoundKeyframeHandler(this::onAnimationSound);

        // Register all action animations via handler
        animationHandler.setupActionController(actionController);
        actionController.setSoundKeyframeHandler(this::onAnimationSound);

        controllers.add(movementController, bankingController, pitchingController, hurtController, actionController);
    }

    private void onAnimationSound(SoundKeyframeEvent<Ignivorus> event) {
        if (!level().isClientSide) {
            return;
        }

        Object data = event.getKeyframeData();
        String key = extractSoundKey(data);
        String controllerName = event.getController() != null ? event.getController().getName() : "unknown";

        // Ignore step sounds from non-movement controllers to prevent duplicates
        if (key != null && (key.contains("ignivorus_walk") || key.contains("ignivorus_run")) && !"movement".equals(controllerName)) {
            return;
        }

        soundHandler.handleAnimationSound(this, data, event.getController());
    }

    private String extractSoundKey(Object data) {
        if (data == null) {
            return null;
        }
        try {
            Object value = data.getClass().getMethod("getSound").invoke(data);
            return value instanceof String ? ((String) value).toLowerCase(java.util.Locale.ROOT) : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private String extractLocator(Object data) {
        if (data == null) {
            return null;
        }
        try {
            Object value = data.getClass().getMethod("getLocator").invoke(data);
            return value instanceof String ? (String) value : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
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
    public DragonSoundProfile getSoundProfile() {
        return IgnivorusSoundProfile.INSTANCE;
    }

    @Override
    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        // GeckoLib handles walking/running audio via keyframes.
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
        saveRideableData(tag);  // Save flight state (flying, hovering, takeoff, etc.)
        tag.putInt("TimeFlying", timeFlying);  // Save flying duration
        this.combatManager.saveToNBT(tag);
        this.physicsController.writeToNBT(tag);  // Save physics envelope state
        tag.putInt("FeedingCooldownTicks", Math.max(0, this.entityData.get(DATA_FEEDING_COOLDOWN)));
        tamingController.save(tag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadRideableData(tag);  // Restore flight state
        this.timeFlying = tag.getInt("TimeFlying");  // Restore flying duration
        this.combatManager.loadFromNBT(tag);
        this.physicsController.readFromNBT(tag);  // Restore physics envelope state
        if (tag.contains("FeedingCooldownTicks")) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, tag.getInt("FeedingCooldownTicks")));
        }
        tamingController.load(tag);
        applyConfiguredAttributes();
    }

    @Override
    protected void applyLoadedFlightState(boolean flying, boolean takeoff, boolean hovering, boolean landing) {
        // Apply loaded flight state to entity data accessors
        setFlying(flying);
        setTakeoff(takeoff);
        setHovering(hovering);
        setLanding(landing);
    }

    // ===== TAMING DAMAGE HANDLING =====

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if ((isTamingStunned() || tamingAbortCalmTicks > 0) && target != null) {
            return;
        }
        super.setTarget(target);
    }

    // ===== FALL DAMAGE IMMUNITY =====

    @Override
    public boolean causeFallDamage(float fallDistance, float fallMultiplier, @NotNull net.minecraft.world.damagesource.DamageSource source) {
        // Ignivorus is completely immune to fall damage
        return false;
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

    // ===== SCREEN SHAKE SYSTEM =====

    private void tickScreenShake() {
        // Client side: just read the synced value from entity data
        if (level().isClientSide) {
            prevScreenShakeAmount = screenShakeAmount;
            screenShakeAmount = this.entityData.get(DATA_SCREEN_SHAKE_AMOUNT);
            return;
        }

        // Server side: decay and update entity data
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
    public float getScreenShakeAmount(float partialTicks) {
        float currentAmount = this.entityData.get(DATA_SCREEN_SHAKE_AMOUNT);
        return prevScreenShakeAmount + (currentAmount - prevScreenShakeAmount) * partialTicks;
    }

    @Override
    public double getShakeDistance() {
        return 30.0; // Larger shake radius for ground-pounding roar
    }

    @Override
    public boolean canFeelShake(Entity player) {
        // Allow screen shake regardless of whether player is on ground
        // This is important for dragon riding scenarios
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
}
