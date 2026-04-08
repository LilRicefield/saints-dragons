package com.leon.saintsdragons.server.entity.dragons.ignivorus;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.network.MessageDragonMeleeMode;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.common.registry.ModBlocks;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.ignivorus.IgnivorusAbilities;
import com.leon.saintsdragons.common.block.IgnivorusEggBlockEntity;
import com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtByTargetGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtTargetGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonFollowParentGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonProtectBabiesGoal;
import com.leon.saintsdragons.server.ai.goals.ignivorus.IgnivorusAirCombatGoal;
import com.leon.saintsdragons.server.ai.goals.ignivorus.IgnivorusGroundCombatGoal;
import com.leon.saintsdragons.server.ai.navigation.DragonNavigationModeController;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncFlightController;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncFlightMoveControl;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncFlyingPathNavigation;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.controller.ignivorus.IgnivorusRiderController;
import com.leon.saintsdragons.server.flight.DragonBarrelRollHelper;
import com.leon.saintsdragons.server.flight.DragonFlightOrientationHelper;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import com.leon.saintsdragons.server.flight.DragonFlightVisuals;
import com.leon.saintsdragons.server.flight.DragonRiderFallRecovery;
import com.leon.saintsdragons.server.flight.DragonRiderFlight;
import com.leon.saintsdragons.server.flight.DragonTakeoff;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireballAbility;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusSoundProfile;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusTamingHandler;
import com.leon.saintsdragons.server.entity.dragons.util.DragonGriefingRules;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.interfaces.ShakesScreen;
import com.leon.saintsdragons.server.entity.interfaces.SoundHandledDragon;
import com.leon.saintsdragons.server.entity.util.ClientAnimationInitHelper;
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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class Ignivorus extends RideableDragonBase implements DragonFlightCapable, SoundHandledDragon, ShakesScreen {
    public static final int TAKEOFF_ANIMATION_TICKS = 30;
    private final DragonTakeoff takeoffComponent;

    // ===== ENTITY DATA ACCESSORS =====

    public static final EntityDataAccessor<Boolean> DATA_RIDER_LANDING_BLEND =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Boolean> DATA_BULLDOZING =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Boolean> DATA_PHASE2 =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Boolean> DATA_LEAPING =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Integer> DATA_LEAP_ANIM_STATE =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Integer> DATA_FEEDING_COOLDOWN =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.INT);
    /** Tracks whether the dragon is stunned during a taming attempt */
    public static final EntityDataAccessor<Boolean> DATA_TAMING_STUNNED =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
    /** Entity data accessor for flight pitch (radians) */
    public static final EntityDataAccessor<Float> DATA_FLIGHT_PITCH =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_ACCUMULATED_ROLL =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);
    /** Entity data accessor for rider pitch key mode */
    public static final EntityDataAccessor<Boolean> DATA_PITCH_KEY_MODE =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);

    /** Tracks the fireball charge level for UI display (0 = not charging, 1-3 = charge level) */
    public static final EntityDataAccessor<Integer> DATA_FIREBALL_CHARGE =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> DATA_FIRE_BREATHING =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_FIRE_BREATH_PROGRESS =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_FIRE_BREATH_ENERGY =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_FIRE_BREATH_DEPLETED =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
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

    private static final double MODEL_SCALE = 1.0D;
    private static final float FIRE_BREATH_ENERGY_REGEN = 0.0025f;
    private static final float FIRE_BREATH_DEPLETED_THRESHOLD = 0.01f;
    private static final float FIRE_BREATH_REARM_THRESHOLD = 0.20f;

    public static final double RIDER_GLIDE_ALTITUDE_THRESHOLD = 40.0D;
    public static final double RIDER_GLIDE_ALTITUDE_EXIT = 30.0D;
    public static final double RIDER_LOW_ALTITUDE_GLIDE_THRESHOLD = 6.0D;
    public static final double RIDER_WATER_SURFACE_LEVEL = 62.0D;
    public static final double RIDER_WATER_SURFACE_TOLERANCE = 2.0D;
    public static final int RIDER_WATER_SCAN_RADIUS = 2;
    public static final int RIDER_WATER_SCAN_DEPTH = 8;
    private static final double WATER_EFFECT_MAX_HEIGHT = 8.0D;
    private static final double WATER_EFFECT_INTENSITY = 0.6D;
    public static final double LANDING_BLEND_ALTITUDE = 8.0D;
    private static final float AIR_AUTO_ALIGN_DECAY = 0.88f;
    private static final float LANDING_AUTO_ALIGN_STEP = 0.30f;
    private static final float INVERTED_PITCH_TRIGGER_RAD = Mth.HALF_PI;
    private static final float BARREL_ROLL_INPUT_SPEED = 0.235f;
    private static final DragonBarrelRollHelper.Config BARREL_ROLL_CONFIG =
            new DragonBarrelRollHelper.Config(
                    AIR_AUTO_ALIGN_DECAY,
                    LANDING_AUTO_ALIGN_STEP,
                    0.04f,
                    0.005f,
                    Mth.HALF_PI
            );
    private static final int RIDER_LANDING_BLEND_DURATION = 5;
    public static final double BREED_PARTNER_RANGE = 20.0D;
    public static final double BREED_DISTANCE_SQR = 2500.0D;

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
                    .add("ignivorus_hurt", "instant", "animation.ignivorus.hurt",
                            ModSounds.IGNIVORUS_HURT, 1.6f, 0.95f, 0.1f,
                            true, true, true)
                    .add("ignivorus_die", "instant", "animation.ignivorus.die",
                            ModSounds.IGNIVORUS_DIE, 1.8f, 0.9f, 0.05f,
                            false, true, true)
                    .build();

    public AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);
    private final IgnivorusAnimationHandler animationHandler = new IgnivorusAnimationHandler(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);

    private final IgnivorusRiderController riderController;
    private final DragonRiderFlight riderFlightComponent;
    private final IgnivorusInteractionHandler interactionHandler = new IgnivorusInteractionHandler(this);
    private final IgnivorusTamingHandler tamingController = new IgnivorusTamingHandler(this);

    private final DragonPathNavigateGround groundNav;
    private final AsyncFlightController asyncAirController;
    private final AsyncFlightMoveControl asyncAirMoveControl;
    private final MoveControl groundMoveControl;
    private final FlyingPathNavigation airNav;
    private final DragonNavigationModeController navigationModeController;

    public int timeFlying = 0;
    private int airTicks;
    public int groundTicks;
    private int riderLandingBlendTicks = 0;
    private float prevSmoothedRoll = 0.0f;
    private float smoothedRoll = 0.0f;

    // ===== HARDCODED GROUND SPEEDS =====
    public static final double RIDER_WALK_SPEED = 0.225D;
    public static final double RIDER_RUN_SPEED = 0.4D;
    public static final float RIDER_KEY_PITCH_DEG = 25.0f;

    // Phase 2 speeds (slower for dramatic effect)
    public static final double RIDER_PHASE2_WALK_SPEED = 0.15D;
    public static final double RIDER_PHASE2_RUN_SPEED = 0.32D;

    private static final float MAX_FIRE_YAW_DEG = 70.0F;
    private static final float MAX_FIRE_PITCH_DEG = 55.0F;  // Matches neck pitch limit
    private Vec3 fireAimDir;


    // Fire breath targeting (AI-driven smart aiming)
    private int fireTime = 0; // Tracks how long fire breath has been active for accuracy ramping
    private Vec3 fireServerTarget = null; // Server-side smooth target position with wobble

    private final DragonFlightStateEvaluator.State flightModeState = new DragonFlightStateEvaluator.State();
    private final DragonFlightVisuals.State flightVisualState = new DragonFlightVisuals.State();

    // Bulldoze toggle state
    private boolean bulldozing = false;
    private int bulldozeCooldownTicks = 0;
    private final java.util.Map<Integer, Integer> bulldozeHitCooldowns = new java.util.HashMap<>(); // entityId -> cooldown ticks
    private boolean bulldozeWasVehicle = false;

    // Phase 2 toggle state
    private boolean phase2Active = false;
    private int phase2CooldownTicks = 0;
    private boolean useRightWingSwipe = true; // Alternates between left and right
    private boolean phase2WasVehicle = false;
    private int aiPhase2LockTicks = 0;
    private int phase2InvalidTargetTicks = 0;
    private boolean aiSpecialCombatActive = false;

    // Leaping body slam system (Phase 2 replacement for bulldoze)
    private static final double LEAP_HORIZONTAL_SPEED = 2.75D; // Horizontal speed per tick - POWERFUL leap forward!
    private static final double LEAP_VERTICAL_BOOST = 1.15D; // Initial upward velocity - MASSIVE jump!
    private static final double LEAP_HORIZONTAL_DRAG = 0.94D; // Air resistance (less drag = more distance)
    private static final double LEAP_GRAVITY = 0.06D; // Gravity applied during leap (lower = floatier arc)
    private static final float LEAP_SLAM_DAMAGE = 50.0F; // Damage on landing
    private static final float DEFAULT_BULLDOZE_DAMAGE = 10.0F;
    private static final double LEAP_SLAM_RADIUS = 20.0D; // AoE damage radius on landing
    private static final double LEAP_KNOCKBACK = 5.5D; // Knockback strength
    private static final double LEAP_LIFT = 0.8D; // Upward launch on hit
    private static final double LEAP_IMPACT_TRIGGER_HEIGHT = 7.0D; // Trigger impact anim just before landing
    private static final int LEAP_GROUNDED_FAILSAFE_TICKS = 6; // Abort if we never leave the ground
    private static final int LEAP_COOLDOWN_TICKS = 140; // 7 seconds at 20 TPS

    // Leap animation states
    private static final int LEAP_STATE_NONE = 0;
    private static final int LEAP_STATE_TAKEOFF = 1;  // Single leap sequence (jump -> slam in one clip)

    private boolean leaping = false;
    private boolean leapWasVehicle = false;
    private int leapAnimState = LEAP_STATE_NONE;
    private Vec3 leapVelocity = Vec3.ZERO;
    private int leapCooldownTicks = 0;
    private int leapImpactRecoveryTicks = 0; // Blocks ambient sounds during impact animation
    private boolean leapImpactTriggered = false;
    private boolean wasAirborneBeforeLanding = false; // Track if we were in the air before landing
    private int leapGroundedTicks = 0; // Failsafe for rare cases where we never leave the ground
    private long lastAiLandedAnimTick = -40L;

    // Animation timing constants (in ticks, 20 ticks = 1 second)
    private static final int LEAP_IMPACT_RECOVERY_DURATION = 20;

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
    private int groundStepSoundCooldownTicks = 0;
    private int teethChipDropCooldownTicks = 0;
    private static final double BABY_MAX_HEALTH = 90.0D;
    private static final double BABY_ARMOR = 0.0D;
    private static final float BABY_HITBOX_SCALE = 0.55F;

    // Client-side animation initialization grace period (fixes T-pose on world rejoin with shaders)
    private int clientAnimInitTicks = 0;

    // Sitting transition state (1.88 seconds = 38 ticks for both down and up animations)
    private int sitTransitionTicks = 0;
    private boolean isSittingDown = false;
    private boolean isStandingUp = false;

    // Client locator cache
    private final Map<String, Vec3> clientLocatorCache = new java.util.concurrent.ConcurrentHashMap<>();

    public Ignivorus(EntityType<? extends Ignivorus> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.1F);

        this.asyncAirController = new AsyncFlightController(this);
        this.asyncAirMoveControl = new AsyncFlightMoveControl(this, this.asyncAirController);
        this.groundNav = new DragonPathNavigateGround(this, level);
        this.groundMoveControl = new MoveControl(this);
        this.airNav = new AsyncFlyingPathNavigation(this, level, this.asyncAirController) {
            @Override
            public boolean isStableDestination(@NotNull net.minecraft.core.BlockPos pos) {
                return !this.level.getBlockState(pos.below()).isAir();
            }
        };
        this.airNav.setCanOpenDoors(false);
        this.airNav.setCanFloat(false);
        this.airNav.setCanPassDoors(false);
        this.navigationModeController = new DragonNavigationModeController(
                new DragonNavigationModeController.Host() {
                    @Override
                    public void setActiveNavigation(PathNavigation navigation) {
                        Ignivorus.this.navigation = navigation;
                    }

                    @Override
                    public void setActiveMoveControl(MoveControl moveControl) {
                        Ignivorus.this.moveControl = moveControl;
                    }

                    @Override
                    public void afterSwitchToGround() {
                        if (Ignivorus.this.onGround()) {
                            Ignivorus.this.setDeltaMovement(Vec3.ZERO);
                            Ignivorus.this.hasImpulse = false;
                        } else {
                            Vec3 motion = Ignivorus.this.getDeltaMovement();
                            Ignivorus.this.setDeltaMovement(motion.x * 0.25D, motion.y, motion.z * 0.25D);
                        }
                    }
                },
                this.groundNav,
                this.airNav,
                this.groundMoveControl,
                this.asyncAirMoveControl
        );
        this.navigation = this.groundNav;
        this.moveControl = this.groundMoveControl;
        // Fire dragon: don't treat fire as a hazard for pathfinding.
        // This prevents repeated repath spikes when long fire lines are present.
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);

        this.riderController = new IgnivorusRiderController(this);
        this.takeoffComponent = createTakeoffComponent();
        this.riderFlightComponent = createRiderFlightComponent();
        resetAmbientSoundTimer();
        if (!level.isClientSide) {
            applyConfiguredAttributes();
        }
    }

    private DragonRiderFlight createRiderFlightComponent() {
        return new DragonRiderFlight(new DragonRiderFlight.Host() {
            @Override
            public Entity asEntity() {
                return Ignivorus.this;
            }

            @Override
            public Level level() {
                return Ignivorus.this.level();
            }

            @Override
            public AABB getBoundingBox() {
                return Ignivorus.this.getBoundingBox();
            }

            @Override
            public boolean isVehicle() {
                return Ignivorus.this.isVehicle();
            }

            @Override
            public boolean isFlying() {
                return Ignivorus.this.isFlying();
            }

            @Override
            public boolean isTakeoff() {
                return Ignivorus.this.isTakeoff();
            }

            @Override
            public boolean isGoingUp() {
                return Ignivorus.this.isGoingUp();
            }

            @Override
            public boolean isUnderWater() {
                return Ignivorus.this.isUnderWater();
            }

            @Override
            public boolean isInWaterOrBubble() {
                return Ignivorus.this.isInWaterOrBubble();
            }

            @Override
            public boolean isTame() {
                return Ignivorus.this.isTame();
            }

            @Override
            public boolean hasControllingRider() {
                return riderController.getRidingPlayer() != null;
            }

            @Override
            public boolean canTakeoff() {
                return Ignivorus.this.canTakeoff();
            }

            @Override
            public void setFlying(boolean value) {
                Ignivorus.this.setFlying(value);
            }

            @Override
            public void setHovering(boolean value) {
                Ignivorus.this.setHovering(value);
            }

            @Override
            public void setLanding(boolean value) {
                Ignivorus.this.setLanding(value);
            }

            @Override
            public void switchToAirNavigation() {
                Ignivorus.this.switchToAirNavigation();
            }

            @Override
            public void setGoingUp(boolean value) {
                Ignivorus.this.setGoingUp(value);
            }

            @Override
            public void setGoingDown(boolean value) {
                Ignivorus.this.setGoingDown(value);
            }

            @Override
            public void stopNavigation() {
                Ignivorus.this.getNavigation().stop();
            }

            @Override
            public void startTakeoffSequence(double minUpwardVelocity, int animationTicks) {
                Ignivorus.this.startTakeoffSequence(minUpwardVelocity, animationTicks);
            }

            @Override
            public Vec3 getDeltaMovement() {
                return Ignivorus.this.getDeltaMovement();
            }

            @Override
            public void setDeltaMovement(Vec3 movement) {
                Ignivorus.this.setDeltaMovement(movement);
            }

            @Override
            public void markImpulse() {
                Ignivorus.this.hasImpulse = true;
            }

            @Override
            public long getGameTime() {
                return Ignivorus.this.level().getGameTime();
            }

            @Override
            public void onManualTakeoffStart() {
                Ignivorus.this.timeFlying = 0;
            }
        }, new DragonRiderFlight.Config(
                true,
                0,
                0.25D,
                0,
                0.45D,
                0
        ));
    }

    private DragonTakeoff createTakeoffComponent() {
        return new DragonTakeoff(new DragonTakeoff.Host() {
            @Override
            public Level level() { return Ignivorus.this.level(); }

            @Override
            public boolean isFlying() { return Ignivorus.this.isFlying(); }

            @Override
            public void setFlying(boolean value) { Ignivorus.this.setFlying(value); }

            @Override
            public void setTakeoff(boolean value) { Ignivorus.this.setTakeoff(value); }

            @Override
            public void setHovering(boolean value) { Ignivorus.this.setHovering(value); }

            @Override
            public void setLanding(boolean value) { Ignivorus.this.setLanding(value); }

            @Override
            public void switchToAirNavigation() { Ignivorus.this.switchToAirNavigation(); }

            @Override
            public Vec3 getDeltaMovement() { return Ignivorus.this.getDeltaMovement(); }

            @Override
            public void setDeltaMovement(Vec3 movement) { Ignivorus.this.setDeltaMovement(movement); }

            @Override
            public void markImpulse() { Ignivorus.this.hasImpulse = true; }

            @Override
            public void onTakeoffStarted() { Ignivorus.this.timeFlying = 0; }
        });
    }

    public void startTakeoffSequence(double minUpwardVelocity, int animationTicks) {
        takeoffComponent.startTakeoff(animationTicks, minUpwardVelocity);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_RIDER_LANDING_BLEND, false);
        this.entityData.define(DATA_BULLDOZING, false);
        this.entityData.define(DATA_PHASE2, false);
        this.entityData.define(DATA_LEAPING, false);
        this.entityData.define(DATA_LEAP_ANIM_STATE, 0);
        this.entityData.define(DATA_FIRE_BREATHING, false);
        this.entityData.define(DATA_FIRE_BREATH_PROGRESS, 0);
        this.entityData.define(DATA_FIRE_BREATH_ENERGY, 1.0F);
        this.entityData.define(DATA_FIRE_BREATH_DEPLETED, false);
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
        this.entityData.define(DATA_FLIGHT_PITCH, 0f);
        this.entityData.define(DATA_ACCUMULATED_ROLL, 0f);
        this.entityData.define(DATA_PITCH_KEY_MODE, false);
        this.entityData.define(DATA_FIREBALL_CHARGE, 0);
    }

    @Override
    protected void defineRideableDragonData() {
        // Additional rideable dragon data if needed
    }

    public static AttributeSupplier.Builder createAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        double attackDamage = config.abilityDamage("bite", 15.0D);
        return createMobAttributes()
            .add(Attributes.MAX_HEALTH, config.maxHealth())
            .add(Attributes.MOVEMENT_SPEED, 0.3D) // Hardcoded AI pathfinding speed
            .add(Attributes.FLYING_SPEED, config.flyingSpeed())
            .add(Attributes.ATTACK_DAMAGE, attackDamage)
            .add(Attributes.FOLLOW_RANGE, 128.0D) // Long range to support fire breath at distance
            .add(Attributes.ARMOR, config.armor())
            .add(Attributes.KNOCKBACK_RESISTANCE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        // Large body needs stronger buoyancy to keep shoreline exits reliable.
        this.goalSelector.addGoal(0, new com.leon.saintsdragons.server.ai.goals.base.DragonFloatGoal(this, 0.018D, -0.02D, 0.95F));
        if (!this.isBaby()) {
            this.goalSelector.addGoal(2, new com.leon.saintsdragons.server.ai.goals.ignivorus.IgnivorusFlightGoal(this));
            this.goalSelector.addGoal(3, new IgnivorusAirCombatGoal(this));
            this.goalSelector.addGoal(3, new IgnivorusGroundCombatGoal(this));
        }
        this.goalSelector.addGoal(5, new com.leon.saintsdragons.server.ai.goals.base.DragonFollowOwnerGoal<>(this, com.leon.saintsdragons.server.ai.goals.base.DragonFollowOwnerGoal.FollowConfig.forIgnivorus()) {
            @Override
            protected void startFollowTakeoff() {
                if (Ignivorus.this.isFlying() || Ignivorus.this.isTakeoff()) {
                    return;
                }
                Ignivorus.this.startTakeoffSequence(0.12D, TAKEOFF_ANIMATION_TICKS);
            }
        });
        this.goalSelector.addGoal(6, new DragonFollowParentGoal<>(this, Ignivorus.class, 1.1D));
        if (!this.isBaby()) {
            this.goalSelector.addGoal(7, new com.leon.saintsdragons.server.ai.goals.base.DragonBreedGoal<>(this, 1.0D, Ignivorus.class, BREED_PARTNER_RANGE, BREED_DISTANCE_SQR));
        }
        this.goalSelector.addGoal(8, new com.leon.saintsdragons.server.ai.goals.base.DragonGroundWanderGoal<>(this, 1.0, 120));
        // Idle water behavior when no target: swim and prefer reaching nearby shore.
        this.goalSelector.addGoal(9, new com.leon.saintsdragons.server.ai.goals.base.DirectSwimWanderGoal(this, 8.0F, 0.12D, 1, true));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        if (!this.isBaby()) {
            this.targetSelector.addGoal(1, new DragonOwnerHurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new DragonOwnerHurtTargetGoal(this));
            this.targetSelector.addGoal(3, new DragonProtectBabiesGoal<>(this, Ignivorus.class));
            this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
            this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                    target -> shouldAggroOnSight()));
        }

    }

    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case DOUBLE_TAP_A, DOUBLE_TAP_D, DOUBLE_TAP_W, DOUBLE_TAP_S,
                 TOGGLE_PITCH_MODE, ABILITY_USE, ABILITY_STOP -> true;
            default -> super.supportsRiderAction(action);
        };
    }

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level,
                                                 @NotNull DifficultyInstance difficulty,
                                                 @NotNull MobSpawnType spawnType,
                                                 @Nullable SpawnGroupData spawnData,
                                                 @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnData, dataTag);
        applyConfiguredAttributes();
        this.setHealth(this.getMaxHealth());

        return data;
    }

    @Override
    public void tick() {
        super.tick();
        soundHandler.tick();
        tickRiderControlLock();
        tickBulldozeState();
        tickPhase2State();
        tickLeapState();
        tickScreenShake();
        tickCinematicZoom();

        // Update client-side sit progress
        if (level().isClientSide) {
            // Grace period avoids shader-time race conditions that can cause brief T-poses on rejoin.
            clientAnimInitTicks = ClientAnimationInitHelper.tickClientCounter(true, clientAnimInitTicks);
        }

        takeoffComponent.tick();

        // Update air/ground time
        if (isFlying()) {
            airTicks++;
            groundTicks = 0;
            timeFlying++;

        } else {
            groundTicks++;
            airTicks = 0;
            timeFlying = 0;
        }

        // Sync flight mode to entity data for animation system during any aerial state.
        if (!level().isClientSide && (isFlying() || isTakeoff() || isLanding() || isHovering())) {
            this.entityData.set(DATA_FLIGHT_MODE, getFlightMode());
        }

        // Auto-complete landing once we're actually on ground to avoid hover-stuck states
        if (!level().isClientSide && isLanding() && onGround()) {
            handleAiLandingComplete();
        }
        // Safety: clear hover flag if grounded to re-enable gravity/takeoff transitions
        if (!level().isClientSide && !isFlying() && isHovering() && onGround()) {
            setHovering(false);
        }

        // Keep takeoff/landing from fighting gravity during state handoff.
        this.setNoGravity(isFlying() || isTakeoff() || isHovering() || isLanding());

        if (!level().isClientSide && this.navigationModeController.isUsingAirNavigation()
                && (this.isFlying() || this.isTakeoff() || this.isLanding())
                && !this.isVehicle()
                && !isDirectAirCombatActive()) {
            this.asyncAirController.serverTick();
        }

        // Update banking and pitching for animations
        tickBankingLogic();
        tickBarrelRollLogic();
        tickPitchingLogic();

        if (!level().isClientSide) {
            if (isBaby()) {
                if (isFlying() || isHovering() || isTakeoff() || isLanding()) {
                    setFlying(false);
                    setHovering(false);
                    setTakeoff(false);
                    setLanding(false);
                }
                if (getTarget() != null) {
                    setTarget(null);
                }
                if (getActiveAbility() != null) {
                    combatManager.forceEndActiveAbility();
                }
                setAggressive(false);
            }
            if (tamingAbortCalmTicks > 0) {
                tamingAbortCalmTicks--;
            }
            tamingController.tickServer();
            if (isTamingStunned()) {
                tamingController.enforceGroundingTick();
            }
            tickFireBreathEnergy();
            tickTerrainClearing();
            tickGroundStepAudio();
            handleAmbientSounds();
            if (isFlying() && tickCount % 2 == 0) {
                tickWaterDisturbance();
            }

            int cooldown = this.entityData.get(DATA_FEEDING_COOLDOWN);
            if (cooldown > 0) {
                this.entityData.set(DATA_FEEDING_COOLDOWN, cooldown - 1);
            }
            if (teethChipDropCooldownTicks > 0) {
                teethChipDropCooldownTicks--;
            }
        }

        // Update sitting progress
        updateSittingProgress();
    }

    private boolean isDirectAirCombatActive() {
        LivingEntity target = this.getTarget();
        return !this.isLanding()
                && this.isAggressive()
                && target != null
                && this.isTargetValid(target);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        // Hook for Fabric mixin to clean up multipart entities
    }

    @Override
    public boolean hurt(@Nonnull DamageSource damageSource, float amount) {
        // During dying sequence, ignore all damage (entity is already dead, playing death animation)
        if (isDying()) {
            return false;
        }
        // Immune to vanilla environmental fire only (mundane fire can't harm fire dragons)
        // Magical fire from mods (e.g., Iron's Spells) is NOT blocked - magic fire is different!
        if (damageSource.is(DamageTypes.IN_FIRE) ||
            damageSource.is(DamageTypes.ON_FIRE) ||
            damageSource.is(DamageTypes.LAVA) ||
            damageSource.is(DamageTypes.HOT_FLOOR)) {
            if (this.isOnFire() || this.getRemainingFireTicks() > 0) {
                this.clearFire();
                this.setRemainingFireTicks(0);
            }
            return false;
        }
        // Wake if sleeping and suppress re-entry on damage
        if (isSleeping() || isSleepingEntering() || isSleepingExiting()) {
            wakeUpImmediately();
            suppressSleep(200);
        }
        boolean hurt = super.hurt(damageSource, amount);
        if (hurt
                && !level().isClientSide
                && amount > 0.0F
                && damageSource.getEntity() != null
                && teethChipDropCooldownTicks <= 0
                && this.random.nextFloat() < 0.12F) {
            this.spawnAtLocation(ModItems.IGNIVORUS_TOOTH.get());
            teethChipDropCooldownTicks = 30;
        }
        return hurt;
    }

    /**
     * Returns a larger bounding box for frustum culling to prevent the model from
     * disappearing when the entity's collision box is off-screen but the visual model
     * (wings, tail, etc.) should still be visible.
     */
    @Override
    public AABB getBoundingBoxForCulling() {
        // Expand the culling box significantly to account for wings, tail, and neck
        return super.getBoundingBoxForCulling().inflate(8.0, 4.0, 8.0);
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        EntityDimensions baseDimensions = super.getDimensions(pose);
        if (isBaby()) {
            return baseDimensions.scale(BABY_HITBOX_SCALE);
        }
        return baseDimensions;
    }

    @Override
    public void ageBoundaryReached() {
        super.ageBoundaryReached();
        applyConfiguredAttributes();
        refreshDimensions();
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        return groundNav;
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        Vec3 input = riderController.getRiddenInput(player, deltaIn);
        if (!level().isClientSide && isFlying()) {
            float fwd = (float) Mth.clamp(input.z, -1.0, 1.0);
            float str = (float) Mth.clamp(input.x, -1.0, 1.0);
            this.setLastRiderForward(Math.abs(fwd) > 0.02f ? fwd : 0f);
            this.setLastRiderStrafe(Math.abs(str) > 0.02f ? str : 0f);
        }
        return super.getRiddenInput(player, input);
    }


    private void handleAmbientSounds() {
        if (isBaby() || isDying() || isSleeping() || isSleepTransitioning() || areRiderControlsLocked()) {
            return;
        }
        if (getTarget() != null || getActiveAbility() != null || isBreathingFire()) {
            return;
        }
        if (isOrderedToSit() || this.isStayOrSitMuted()) {
            return;
        }
        if (bulldozing || leaping || leapImpactRecoveryTicks > 0) {
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


    public boolean canFeed() {
        return this.entityData.get(DATA_FEEDING_COOLDOWN) <= 0;
    }

    public void setFeedingCooldown(int ticks) {
        this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, ticks));
    }

    @Override
    protected int getMaxTextureVariant() {
        // 0 = default, 1 = crimson
        return 1;
    }

    @Override
    protected int chooseSpawnTextureVariant(@NotNull ServerLevelAccessor levelAccessor,
                                            @NotNull DifficultyInstance difficulty,
                                            @NotNull MobSpawnType reason,
                                            @Nullable SpawnGroupData spawnData,
                                            @Nullable CompoundTag spawnTag) {
        return this.getRandom().nextFloat() < 0.05F ? 1 : 0;
    }

    @Override
    protected int chooseAdultTextureVariant() {
        return this.getRandom().nextFloat() < 0.50F ? 1 : 0;
    }

    @Override
    public java.util.Map<String, Integer> getTextureVariantNameMap() {
        return java.util.Map.of(
                "default", 0,
                "crimson", 1
        );
    }

    // ===== FIREBALL CHARGE SYSTEM =====

    /**
     * Gets the current fireball charge level (0 = not charging, 1-3 = charge level)
     */
    public int getFireballChargeLevel() {
        return this.entityData.get(DATA_FIREBALL_CHARGE);
    }

    /**
     * Sets the fireball charge level for UI display sync
     */
    public void setFireballChargeLevel(int level) {
        this.entityData.set(DATA_FIREBALL_CHARGE, Math.max(0, Math.min(3, level)));
    }

    /**
     * Returns true if currently charging a fireball
     */
    public boolean isChargingFireball() {
        return getFireballChargeLevel() > 0;
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
        double fallback = this.getMaxHealth() * TAMING_HEALTH_RATIO;
        double configured = com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader.getInstance()
                .getConfig(com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader.IGNIVORUS_ID)
                .extraDouble("taming_stun_health", fallback);
        double clamped = Math.max(0.0D, Math.min(configured, this.getMaxHealth()));
        return (float) clamped;
    }

    @Override
    public void lockRiderControls(int ticks) {
        super.lockRiderControls(ticks);  // Base handles tick counting and entity data
        // Ignivorus-specific: reset rider inputs and movement states during lock
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


    private void tickCinematicZoom() {
        prevCinematicZoomProgress = cinematicZoomProgress;
        boolean active = this.entityData.get(DATA_CINEMATIC_ZOOM_ACTIVE);
        float target = active ? 1.0F : 0.0F;
        cinematicZoomProgress = Mth.lerp(0.12F, cinematicZoomProgress, target);
        if (Math.abs(cinematicZoomProgress - target) < 0.01F) {
            cinematicZoomProgress = target;
        }
    }

    private void tickBulldozeState() {
        // Only server handles bulldoze logic
        if (!level().isClientSide) {
            boolean currentlyVehicle = this.isVehicle();

            // Tick down cooldown
            if (bulldozeCooldownTicks > 0) {
                bulldozeCooldownTicks--;
            }

            // Tick down hit cooldowns for all entities
            bulldozeHitCooldowns.entrySet().removeIf(entry -> {
                entry.setValue(entry.getValue() - 1);
                return entry.getValue() <= 0;
            });

            // Force sprint while bulldozing (even during transition lock)
            if (bulldozing && currentlyVehicle && !this.isAccelerating()) {
                setAccelerating(true);
            }

            // Disable bulldozing if player dismounts
            if (bulldozing && bulldozeWasVehicle && !currentlyVehicle) {
                bulldozing = false;
                this.entityData.set(DATA_BULLDOZING, false);
                setAccelerating(false);
                bulldozeCooldownTicks = 40; // 2 second cooldown
                clearRiderControlLock(); // Clear any transition lock
                bulldozeHitCooldowns.clear(); // Clear hit tracking
            }

            // Handle collision damage while bulldozing - SUPER DISRESPECTFUL
            if (bulldozing && currentlyVehicle) {
                // Get mouth position as knockback origin
                Vec3 mouthPos = getMouthPosition();

                // Combine hitbox + mouth area for MAXIMUM coverage
                // Use full dragon hitbox inflated, PLUS mouth area
                AABB dragonBox = this.getBoundingBox().inflate(1.5D);
                AABB mouthBox = new AABB(mouthPos, mouthPos).inflate(2.5D);
                AABB combinedBox = dragonBox.minmax(mouthBox);

                java.util.List<LivingEntity> entities = this.level().getEntitiesOfClass(
                    LivingEntity.class,
                    combinedBox,
                    entity -> entity != this && entity != this.getControllingPassenger() && !this.isAlly(entity)
                );

                // Damage and knockback each entity
                for (LivingEntity target : entities) {
                    int entityId = target.getId();

                    // Check if entity is on cooldown (hit recently)
                    if (bulldozeHitCooldowns.containsKey(entityId)) {
                        continue; // Skip this entity, still on cooldown
                    }

                    // Apply configured bulldoze collision damage
                    target.hurt(this.damageSources().mobAttack(this), resolveBulldozeDamage());

                    // Apply knockback from mouth position for that DISRESPECTFUL shove
                    double knockbackStrength = 2.0D;
                    double dx = target.getX() - mouthPos.x;
                    double dz = target.getZ() - mouthPos.z;
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0) {
                        target.knockback(
                            knockbackStrength,
                            -dx / dist,  // Shove away from mouth
                            -dz / dist
                        );
                    }

                    // Add cooldown: 5 ticks = 0.25 seconds = 4 hits per second per entity
                    bulldozeHitCooldowns.put(entityId, 5);
                }
            }

            bulldozeWasVehicle = currentlyVehicle;
        }
    }

    private void tickPhase2State() {
        // Only server handles phase 2 logic
        if (!level().isClientSide) {
            boolean currentlyVehicle = this.isVehicle();

            // Tick down cooldown
            if (phase2CooldownTicks > 0) {
                phase2CooldownTicks--;
            }
            if (aiPhase2LockTicks > 0) {
                aiPhase2LockTicks--;
            }

            // Disable Phase 2 if player dismounts
            if (phase2Active && phase2WasVehicle && !currentlyVehicle) {
                phase2Active = false;
                this.entityData.set(DATA_PHASE2, false);
                phase2CooldownTicks = 40; // 2 second cooldown
                clearRiderControlLock(); // Clear any transition lock
            }

            // AI safety: if there is no valid combat target for a sustained period,
            // fall back to phase 1. Keep rider-driven phase 2 untouched.
            if (phase2Active && !currentlyVehicle) {
                if (hasValidPhase2CombatTarget()) {
                    phase2InvalidTargetTicks = 0;
                } else {
                    phase2InvalidTargetTicks++;
                    if (phase2InvalidTargetTicks >= 40) { // 2s grace to avoid one-tick target flicker
                        phase2Active = false;
                        this.entityData.set(DATA_PHASE2, false);
                        phase2CooldownTicks = 40; // 2 second cooldown
                        if (!isFlying() && !isTakeoff() && !isLanding() && !isHovering()) {
                            animationHandler.triggerPhase2ExitAnimation();
                        }
                    }
                }
            } else {
                phase2InvalidTargetTicks = 0;
            }

            phase2WasVehicle = currentlyVehicle;
        }
    }

    private boolean hasValidPhase2CombatTarget() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (target.level() != this.level()) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return true;
    }

    private void tickLeapState() {
        // Only server handles leap logic
        if (!level().isClientSide) {
            boolean currentlyVehicle = this.isVehicle();

            // Tick down cooldown
            if (leapCooldownTicks > 0) {
                leapCooldownTicks--;
            }

            // Tick down impact recovery (blocks ambient sounds + locks controls)
            if (leapImpactRecoveryTicks > 0) {
                leapImpactRecoveryTicks--;

                // Clear animation state when recovery ends
                if (leapImpactRecoveryTicks == 0 && leapAnimState != LEAP_STATE_NONE) {
                    leapAnimState = LEAP_STATE_NONE;
                    this.entityData.set(DATA_LEAP_ANIM_STATE, LEAP_STATE_NONE);
                }
            }

            // Cancel leap only if it was rider-initiated and rider dismounted mid-leap.
            if (leaping && leapWasVehicle && !currentlyVehicle) {
                leaping = false;
                this.entityData.set(DATA_LEAPING, false);
                leapAnimState = LEAP_STATE_NONE;
                this.entityData.set(DATA_LEAP_ANIM_STATE, LEAP_STATE_NONE);
                leapVelocity = Vec3.ZERO;
                setDeltaMovement(Vec3.ZERO);
                wasAirborneBeforeLanding = false;
                leapImpactTriggered = false;
                leapGroundedTicks = 0;
                // Rider dismount cancel should not trigger AI leap cooldown.
                leapImpactRecoveryTicks = 0; // Clear recovery timer
                leapWasVehicle = false;
            }

            // Handle leap physics and landing detection
            if (leaping) {
                handleLeapMovement();
            }
        }
    }

    private void handleLeapMovement() {
        // Track if we've left the ground (and detect stuck-ground edge cases)
        if (!onGround()) {
            wasAirborneBeforeLanding = true;
            leapGroundedTicks = 0;
        } else {
            leapGroundedTicks++;
        }

        // Trigger impact animation slightly before touchdown when descending
        if (!leapImpactTriggered && wasAirborneBeforeLanding && leapVelocity.y < -0.05D) {
            double groundDistance = getLeapGroundDistance();
            if (groundDistance >= 0.0D && groundDistance <= LEAP_IMPACT_TRIGGER_HEIGHT) {
                animationHandler.triggerLeapImpactAnimation();
                leapImpactTriggered = true;
            }
        }

        // Apply physics: horizontal drag + gravity
        double newX = leapVelocity.x * LEAP_HORIZONTAL_DRAG;
        double newZ = leapVelocity.z * LEAP_HORIZONTAL_DRAG;
        double newY = leapVelocity.y - LEAP_GRAVITY; // Apply gravity

        // Update stored velocity
        leapVelocity = new Vec3(newX, newY, newZ);

        // Apply to entity
        setDeltaMovement(leapVelocity);
        hasImpulse = true;

        // Check for landing
        if (onGround() && wasAirborneBeforeLanding) {
            // We've landed! Apply slam damage
            applyLeapSlamDamage();
            getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_IMPACT.get(), 1.0f, 1.0f, 43);
            if (!leapImpactTriggered) {
                animationHandler.triggerLeapImpactAnimation();
                leapImpactTriggered = true;
            }

            // End leap movement but keep state for impact animation
            leaping = false;
            this.entityData.set(DATA_LEAPING, false);
            // DON'T reset anim state yet - let impact recovery handle it
            leapVelocity = Vec3.ZERO;
            wasAirborneBeforeLanding = false;
            leapImpactTriggered = false;
            if (!leapWasVehicle) {
                leapCooldownTicks = LEAP_COOLDOWN_TICKS;
            }
            leapImpactRecoveryTicks = LEAP_IMPACT_RECOVERY_DURATION; // Block ambient sounds + lock controls during impact
            leapWasVehicle = false;

            // Lock controls during impact animation
            lockRiderControls(LEAP_IMPACT_RECOVERY_DURATION);

            // Clear velocity completely to stop sliding
            setDeltaMovement(Vec3.ZERO);

            // Animation handled by IgnivorusAnimationHandler.handleMovementAnimation()
            leapGroundedTicks = 0;
            return;
        }

        // Failsafe: if we never leave the ground, clear the leap to avoid freezing
        if (onGround() && !wasAirborneBeforeLanding && leapGroundedTicks >= LEAP_GROUNDED_FAILSAFE_TICKS) {
            leaping = false;
            this.entityData.set(DATA_LEAPING, false);
            leapAnimState = LEAP_STATE_NONE;
            this.entityData.set(DATA_LEAP_ANIM_STATE, LEAP_STATE_NONE);
            leapVelocity = Vec3.ZERO;
            setDeltaMovement(Vec3.ZERO);
            wasAirborneBeforeLanding = false;
            leapImpactTriggered = false;
            leapGroundedTicks = 0;
            if (!leapWasVehicle) {
                leapCooldownTicks = LEAP_COOLDOWN_TICKS;
            }
            leapWasVehicle = false;
        }
    }

    private void applyLeapSlamDamage() {
        Level level = level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        // Get landing position
        Vec3 landPos = position();

        // Spawn visual effects - blocks and particles
        spawnLeapImpactBlockEffect(server);
        spawnLeapImpactDirtParticles(server);

        // Create damage area around landing point
        AABB damageArea = new AABB(
            landPos.x - LEAP_SLAM_RADIUS,
            landPos.y - 1.0,
            landPos.z - LEAP_SLAM_RADIUS,
            landPos.x + LEAP_SLAM_RADIUS,
            landPos.y + getBbHeight() + 1.0,
            landPos.z + LEAP_SLAM_RADIUS
        );

        // Find all targets in range
        List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, damageArea,
                entity -> entity != this && entity.isAlive() && entity.attackable() && !isAlly(entity));

        if (targets.isEmpty()) {
            return;
        }

        // Apply configurable damage
        float damage = resolveLeapSlamDamage();
        DamageSource source = server.damageSources().mobAttack(this);

        for (LivingEntity target : targets) {
            // Deal damage
            target.hurt(source, damage);

            // Knockback away from landing point
            Vec3 push = target.position().subtract(landPos);
            if (push.lengthSqr() < 1.0E-4) {
                push = new Vec3(0, 0, 1);
            }
            push = push.normalize();
            target.push(push.x * LEAP_KNOCKBACK, LEAP_LIFT, push.z * LEAP_KNOCKBACK);
            target.hasImpulse = true;
        }
    }

    private float resolveLeapSlamDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                .abilityDamage("leap_slam", LEAP_SLAM_DAMAGE);
    }

    private float resolveBulldozeDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                .abilityDamage("bulldoze", DEFAULT_BULLDOZE_DAMAGE);
    }

    /**
     * Spawns visual falling blocks in rings around the leap impact point
     */
    private void spawnLeapImpactBlockEffect(ServerLevel level) {
        RandomSource random = getRandom();
        BlockPos dragonPos = blockPosition();
        java.util.List<net.minecraft.core.BlockPos> blockPositions = new java.util.ArrayList<>();

        // Spawn blocks in rings based on leap radius (20 blocks)
        // Outer ring - radius 16-20 blocks
        addRingBlockPositions(blockPositions, dragonPos, 16, 20, random, 25);

        // Middle ring - radius 10-15 blocks
        addRingBlockPositions(blockPositions, dragonPos, 10, 15, random, 20);

        // Inner ring - radius 5-9 blocks
        addRingBlockPositions(blockPositions, dragonPos, 5, 9, random, 15);

        // Spawn falling blocks at each position
        for (net.minecraft.core.BlockPos pos : blockPositions) {
            spawnLeapFallingBlockAt(level, pos, random);
        }
    }

    /**
     * Spawns dirt particles in expanding rings around the leap impact
     */
    private void spawnLeapImpactDirtParticles(ServerLevel level) {
        RandomSource random = getRandom();
        Vec3 dragonPos = position();
        net.minecraft.core.particles.BlockParticleOption dirtParticles =
            new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
            net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());

        // Inner ring - 5-10 block radius, 40 particles
        spawnLeapParticleRing(level, dragonPos, dirtParticles, 5, 10, 40, random);

        // Middle ring - 10-15 block radius, 60 particles
        spawnLeapParticleRing(level, dragonPos, dirtParticles, 10, 15, 60, random);

        // Outer ring - 15-20 block radius, 80 particles
        spawnLeapParticleRing(level, dragonPos, dirtParticles, 15, 20, 80, random);
    }

    private void addRingBlockPositions(java.util.List<net.minecraft.core.BlockPos> positions, BlockPos center,
                                       int minRadius, int maxRadius, RandomSource random, int count) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);
            int xOffset = (int) Math.round(Math.cos(angle) * radius);
            int zOffset = (int) Math.round(Math.sin(angle) * radius);
            BlockPos targetPos = center.offset(xOffset, 0, zOffset);
            positions.add(targetPos);
        }
    }

    private void spawnLeapFallingBlockAt(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos groundPos = findGroundLevel(pos);
        if (groundPos == null) {
            return;
        }

        net.minecraft.world.level.block.state.BlockState groundState = level.getBlockState(groundPos);
        if (groundState.isAir() || groundState.liquid() || groundState.is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
            return;
        }

        double startX = groundPos.getX() + 0.5;
        double startY = groundPos.getY() + 0.5;
        double startZ = groundPos.getZ() + 0.5;

        com.leon.saintsdragons.server.entity.effect.VisualFallingBlockEntity fallingBlock =
            new com.leon.saintsdragons.server.entity.effect.VisualFallingBlockEntity(
                com.leon.saintsdragons.common.registry.ModEntities.VISUAL_FALLING_BLOCK.get(),
                level,
                startX,
                startY,
                startZ,
                groundState,
                200
            );

        double upwardVelocity = 0.5 + random.nextDouble() * 0.7;
        fallingBlock.setDeltaMovement(0, upwardVelocity, 0);
        level.addFreshEntity(fallingBlock);
    }

    private void spawnLeapParticleRing(ServerLevel level, Vec3 center,
                                       net.minecraft.core.particles.BlockParticleOption particleType,
                                       int minRadius, int maxRadius, int count, RandomSource random) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;

            BlockPos groundPos = findGroundLevel(new BlockPos((int)x, (int)center.y, (int)z));
            if (groundPos == null) {
                continue;
            }

            net.minecraft.world.level.block.state.BlockState groundState = level.getBlockState(groundPos);
            if (groundState.isAir() || groundState.liquid()) {
                continue;
            }

            double particleY = groundPos.getY() + 1.02;
            int burstCount = 6;
            for (int j = 0; j < burstCount; j++) {
                double velX = (random.nextDouble() - 0.5) * 0.9;
                double velY = 0.25 + random.nextDouble() * 0.85;
                double velZ = (random.nextDouble() - 0.5) * 0.9;
                level.sendParticles(particleType, x, particleY, z, 0, velX, velY, velZ, 1.0);
            }
        }
    }

    private BlockPos findGroundLevel(BlockPos startPos) {
        int dragonY = blockPosition().getY();
        for (int y = dragonY; y > level().getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
            net.minecraft.world.level.block.state.BlockState state = level().getBlockState(checkPos);
            if (!state.isAir() && !state.liquid() && state.isSolidRender(level(), checkPos)) {
                return checkPos;
            }
        }
        return null;
    }

    /**
     * Breaks ground in a circle pattern - temporarily removes blocks and restores them after a delay
     * Inspired by Epic Fight's Demolition Leap
     */
    private void breakGroundCircle(ServerLevel level, Vec3 center, double radius) {
        if (!DragonGriefingRules.canDestroyBlocks(level)) {
            return;
        }

        int centerX = (int) Math.floor(center.x);
        int centerY = (int) Math.floor(center.y);
        int centerZ = (int) Math.floor(center.z);

        int radiusInt = (int) Math.ceil(radius);
        java.util.List<net.minecraft.core.BlockPos> blocksToRestore = new java.util.ArrayList<>();
        java.util.Map<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState> originalStates = new java.util.HashMap<>();

        // Scan in a circle pattern
        for (int x = -radiusInt; x <= radiusInt; x++) {
            for (int z = -radiusInt; z <= radiusInt; z++) {
                // Check if within circle radius
                double distSqr = x * x + z * z;
                if (distSqr > radius * radius) {
                    continue;
                }

                BlockPos targetPos = new BlockPos(centerX + x, centerY, centerZ + z);
                BlockPos groundPos = findGroundLevelForBreaking(level, targetPos);

                if (groundPos == null) {
                    continue;
                }

                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(groundPos);

                // Skip if not breakable
                if (!canBreakBlock(level, groundPos, state)) {
                    continue;
                }

                // Save original state
                originalStates.put(groundPos.immutable(), state);
                blocksToRestore.add(groundPos.immutable());

                // Remove block (set to air)
                level.setBlock(groundPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

                // Spawn visual falling block at this position
                spawnBreakingFallingBlock(level, groundPos, state);
            }
        }

        // Schedule block restoration after 100 ticks (5 seconds)
        if (!blocksToRestore.isEmpty()) {
            scheduleBlockRestoration(level, originalStates, 100);
        }
    }

    private BlockPos findGroundLevelForBreaking(ServerLevel level, BlockPos startPos) {
        // Search down from impact point to find solid ground
        for (int y = startPos.getY(); y > level.getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(checkPos);

            if (!state.isAir() && !state.liquid() && state.isSolidRender(level, checkPos)) {
                return checkPos;
            }
        }
        return null;
    }

    private boolean canBreakBlock(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        // Don't break air, liquids, bedrock, or blocks with tile entities
        if (state.isAir() || state.liquid()) {
            return false;
        }

        if (state.is(net.minecraft.world.level.block.Blocks.BEDROCK) ||
            state.is(net.minecraft.world.level.block.Blocks.END_PORTAL) ||
            state.is(net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME) ||
            state.is(net.minecraft.world.level.block.Blocks.END_GATEWAY)) {
            return false;
        }

        // Don't break blocks with block entities (chests, furnaces, etc.)
        if (state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock) {
            return false;
        }

        // Check if block has a solid collision shape
        return state.isSolidRender(level, pos);
    }

    private void spawnBreakingFallingBlock(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        double startX = pos.getX() + 0.5;
        double startY = pos.getY() + 0.5;
        double startZ = pos.getZ() + 0.5;

        com.leon.saintsdragons.server.entity.effect.VisualFallingBlockEntity fallingBlock =
            new com.leon.saintsdragons.server.entity.effect.VisualFallingBlockEntity(
                com.leon.saintsdragons.common.registry.ModEntities.VISUAL_FALLING_BLOCK.get(),
                level,
                startX,
                startY,
                startZ,
                state,
                100 // Lifetime - blocks will be restored before this expires
            );

        // Give it upward velocity
        double upwardVelocity = 0.3 + level.random.nextDouble() * 0.4;
        fallingBlock.setDeltaMovement(0, upwardVelocity, 0);
        level.addFreshEntity(fallingBlock);
    }

    private void scheduleBlockRestoration(ServerLevel level, java.util.Map<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState> blocks, int delayTicks) {
        // Use Minecraft's server tick scheduler to restore blocks
        level.getServer().tell(new net.minecraft.server.TickTask(
            level.getServer().getTickCount() + delayTicks,
            () -> {
                for (java.util.Map.Entry<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState> entry : blocks.entrySet()) {
                    BlockPos pos = entry.getKey();
                    net.minecraft.world.level.block.state.BlockState state = entry.getValue();

                    // Only restore if the block is still air (player hasn't placed something)
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, state, 3);
                    }
                }
            }
        ));
    }

    private double getLeapGroundDistance() {
        int groundY = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(getX()), Mth.floor(getZ()));
        if (groundY <= level().getMinBuildHeight()) {
            return -1.0D;
        }
        return getY() - (groundY + 1.0D);
    }

    public boolean isLeaping() {
        return level().isClientSide ? this.entityData.get(DATA_LEAPING) : leaping;
    }

    public boolean isLeapImpactRecovering() {
        return !level().isClientSide && leapImpactRecoveryTicks > 0;
    }

    public int getLeapAnimState() {
        // Use synced entity data on client side, server-side variable on server
        return level().isClientSide ? this.entityData.get(DATA_LEAP_ANIM_STATE) : leapAnimState;
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
                // Calculate fire aim direction for server-side fire path, but DON'T apply it to dragon rotation
                // The rider controller already handles rotation - applyFireLook would fight it
                Vec3 aim = refreshFireAimDirection(start, true);
                if (aim == null) {
                    copyRiderLook(player);
                }
                // Skip applyFireLook when riding - rider controller handles rotation
        } else {
            resetFireAimDirection();
        }
    }

    @Override
    public void removePassenger(@NotNull Entity passenger) {
        // Base implementation handles clearing control lock
        super.removePassenger(passenger);
    }

    @Override
    public void travel(@NotNull Vec3 travelVec) {
        // Handle leap FIRST - preserve leap velocity and ignore rider input
        if (leaping) {
            super.travel(Vec3.ZERO);
            return;
        }

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
        if ((isFlying() || isTakeoff() || isLanding() || isHovering()) && !isGoingUp()) {
            setFlying(false);
            setTakeoff(false);
            setLanding(false);
            setHovering(false);
            timeFlying = 0;
            switchToGroundNavigation();
        }

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

        // Shared component handles waterline breach -> flight transition.
        riderFlightComponent.tryAutoBreachTakeoff();
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
    protected void onRiderAbilityUse(Player player, String abilityName) {
        if (abilityName == null || abilityName.isEmpty()) {
            return;
        }
        if (isBaby() && isBabyAbilityBlocked(abilityName)) {
            return;
        }
        // Block non-attack abilities while in Phase 2
        // Wing swipe, stomp, bite (for air), fire breath, and ultimate are allowed in Phase 2
        if (isPhase2Active() &&
            !abilityName.equals(IgnivorusAbilities.IGNIVORUS_WING_SWIPE_ID) &&
            !abilityName.equals(IgnivorusAbilities.IGNIVORUS_STOMP_ID) &&
            !abilityName.equals(IgnivorusAbilities.IGNIVORUS_BITE_ID) &&
            !abilityName.equals(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH_ID) &&
            !abilityName.equals(IgnivorusAbilities.IGNIVORUS_FIREBALL_ID) &&
            !abilityName.equals(IgnivorusAbilities.IGNIVORUS_ULTIMATE_ID)) {
            return;
        }
        useRidingAbility(abilityName);
    }

    @Override
    protected void onRiderAbilityStop(Player player, String abilityName) {
        if (abilityName != null && !abilityName.isEmpty()) {
            if (IgnivorusAbilities.IGNIVORUS_FIREBALL_ID.equals(abilityName)) {
                var active = combatManager.getActiveAbility();
                if (active != null && active.getAbilityType() == IgnivorusAbilities.IGNIVORUS_FIREBALL) {
                    ((IgnivorusFireballAbility) active).requestRelease();
                    return;
                }
            }
            forceEndActiveAbility();
        }
    }

    @Override
    protected boolean handleCustomRiderAction(ServerPlayer player, DragonRiderAction action,
                                              String abilityName, boolean locked) {
        if (action == null) {
            return false;
        }

        // Allow DOUBLE_TAP_W to toggle bulldoze on/off even while bulldozing
        // (locked check happens inside onRiderBulldoze)
        if (action == DragonRiderAction.DOUBLE_TAP_W) {
            if (isBaby()) {
                return true;
            }
            if (!locked) {
                onRiderBulldoze(player);
            }
            return true;
        }

        // Allow DOUBLE_TAP_S to toggle Phase 2 on/off
        // (locked check happens inside onRiderPhase2Toggle)
        if (action == DragonRiderAction.DOUBLE_TAP_S) {
            if (isBaby()) {
                return true;
            }
            if (!locked) {
                onRiderPhase2Toggle(player);
            }
            return true;
        }

        // Block all other actions while bulldozing or leaping (but allow movement/accelerate)
        if (bulldozing || leaping) {
            return action != DragonRiderAction.ACCELERATE
                    && action != DragonRiderAction.STOP_ACCELERATE;
        }

        if (locked) {
            return false;
        }

        if (action == DragonRiderAction.TOGGLE_PITCH_MODE) {
            setRiderPitchKeyMode(!isRiderPitchKeyMode());
            return true;
        }

        return false;
    }

    // Animation initialization system (fixes T-pose on world rejoin with shaders)
    public boolean isClientAnimationReady() {
        return ClientAnimationInitHelper.isReady(clientAnimInitTicks);
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
    public boolean supportsSleep() {
        return true;
    }

    @Override
    protected boolean useSleepSitDownTimer() {
        return true;
    }

    @Override
    protected boolean requireSeatedBeforeFallAsleep() {
        return true;
    }

    @Override
    protected boolean sleepForceSitDownOnEnter() {
        return true;
    }

    @Override
    protected boolean useSleepSitUpAfterWake() {
        return false;
    }

    @Override
    public boolean isSleepSuppressed() {
        return super.isSleepSuppressed() || getTarget() != null || isFlying() || isInWaterOrBubble() || isVehicle() || isTamingStunned();
    }

    @Override
    public DragonEntity.DragonSleepPreferences getSleepPreferences() {
        // Ignivorus are nocturnal sleepers (sleep at night)
        return DragonEntity.DragonSleepPreferences.NOCTURNAL();
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

    @Override
    protected int getSleepSitDownDuration() {
        return 38; // animation "down" length
    }

    @Override
    protected int getSleepSitUpDuration() {
        return 38; // animation "up" length
    }

    @Override
    protected int getSleepFallAsleepDuration() {
        return 38; // animation "fall_asleep" length
    }

    @Override
    protected int getSleepWakeUpDuration() {
        return 38; // animation "wake_up" length
    }

    @Override
    protected void onSleepLockCommand(int snapshot) {
        // Ignivorus sleep doesn't override command state.
    }

    @Override
    protected void onSleepUnlockCommand(int desired) {
        setOrderedToSit(desired == 1);
        setGroundMoveStateFromAI(0);
    }

    @Override
    protected void onSleepFreezeTick() {
        this.getNavigation().stop();
        this.setDeltaMovement(0, 0, 0);
        this.setRunning(false);
        this.setGroundMoveStateFromAI(0);
        this.setOrderedToSit(true);
    }

    @Override
    protected void onSleepSitDownAnimation() {
        animationHandler.triggerSitDownAnimation();
        setOrderedToSit(true);
    }

    @Override
    protected void onSleepFallAsleepAnimation() {
        animationHandler.triggerFallAsleepAnimation();
    }

    @Override
    protected void onSleepLoopAnimation() {
        animationHandler.triggerSleepAnimation();
        setOrderedToSit(true);
        setGroundMoveStateFromAI(0);
    }

    @Override
    protected void onSleepWakeUpAnimation() {
        animationHandler.triggerWakeUpAnimation();
    }

    @Override
    protected void onSleepExitStarted() {
        setGroundMoveStateFromAI(0);
        setOrderedToSit(true);
    }

    @Override
    protected void onSleepWakeUpImmediate() {
        setOrderedToSit(false);
        setGroundMoveStateFromAI(0);
    }

    private boolean isBabyAbilityBlocked(String abilityName) {
        return IgnivorusAbilities.IGNIVORUS_ULTIMATE_ID.equals(abilityName)
                || IgnivorusAbilities.IGNIVORUS_ROAR_ID.equals(abilityName)
                || IgnivorusAbilities.IGNIVORUS_FIREBALL_ID.equals(abilityName)
                || IgnivorusAbilities.IGNIVORUS_BODY_SLAM_ID.equals(abilityName)
                || IgnivorusAbilities.IGNIVORUS_BITE_ID.equals(abilityName)
                || IgnivorusAbilities.IGNIVORUS_FIRE_BREATH_ID.equals(abilityName)
                || IgnivorusAbilities.IGNIVORUS_WING_SWIPE_ID.equals(abilityName)
                || IgnivorusAbilities.IGNIVORUS_STOMP_ID.equals(abilityName);
    }

    public void useRidingAbility(String abilityName) {
        if (abilityName == null || abilityName.isEmpty()) {
            return;
        }
        if (isBaby() && isBabyAbilityBlocked(abilityName)) {
            return;
        }
        if ((isFlying() || isTakeoff() || isLanding() || isHovering())
                && IgnivorusAbilities.IGNIVORUS_ROAR_ID.equals(abilityName)) {
            abilityName = IgnivorusAbilities.IGNIVORUS_FIREBALL_ID;
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

    public void requestFireballReleaseForAI() {
        var active = combatManager.getActiveAbility();
        if (active != null && active.getAbilityType() == IgnivorusAbilities.IGNIVORUS_FIREBALL) {
            ((IgnivorusFireballAbility) active).requestRelease();
        }
    }

    public boolean isAiSpecialCombatActive() {
        return !level().isClientSide && aiSpecialCombatActive;
    }

    public void setAiSpecialCombatActive(boolean active) {
        if (!level().isClientSide) {
            this.aiSpecialCombatActive = active;
        }
    }

    @Override
    public RiderAbilityBinding getPrimaryRiderAbility() {
        if (isBaby()) {
            return null;
        }
        if (isFlying() || isTakeoff() || isLanding() || isHovering()) {
            // Fireball is now a HOLD ability - charge and release
            return new RiderAbilityBinding(IgnivorusAbilities.IGNIVORUS_FIREBALL_ID, RiderAbilityBinding.Activation.HOLD);
        }
        if (isPhase2Active()) {
            // Fireball is now a HOLD ability - charge and release
            return new RiderAbilityBinding(IgnivorusAbilities.IGNIVORUS_FIREBALL_ID, RiderAbilityBinding.Activation.HOLD);
        }
        return new RiderAbilityBinding(IgnivorusAbilities.IGNIVORUS_ROAR_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getSecondaryRiderAbility() {
        if (isBaby()) {
            return null;
        }
        return new RiderAbilityBinding(IgnivorusAbilities.IGNIVORUS_ULTIMATE_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getTertiaryRiderAbility() {
        if (isBaby()) {
            return null;
        }
        return new RiderAbilityBinding(IgnivorusAbilities.IGNIVORUS_FIRE_BREATH_ID, RiderAbilityBinding.Activation.HOLD);
    }

    @Override
    public RiderAbilityBinding getAttackRiderAbility() {
        if (isBaby()) {
            return null;
        }
        // Phase 2 uses melee mode toggle (wing swipe or stomp)
        if (isPhase2Active()) {
            if (isFlying()) {
                return new RiderAbilityBinding(IgnivorusAbilities.IGNIVORUS_BITE_ID, RiderAbilityBinding.Activation.PRESS);
            }
            String abilityId = getMeleeMode() == 1
                    ? IgnivorusAbilities.IGNIVORUS_STOMP_ID
                    : IgnivorusAbilities.IGNIVORUS_WING_SWIPE_ID;
            return new RiderAbilityBinding(abilityId, RiderAbilityBinding.Activation.PRESS);
        }

        // Normal mode uses melee mode toggle (bite or body slam)
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
        InteractionResult result = interactionHandler.handleInteraction(player, hand);
        if (result == InteractionResult.PASS) {
            return super.mobInteract(player, hand);
        }
        return result;
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
    protected void onRiderBulldoze(Player player) {
        if (isBaby()) {
            return;
        }
        // Only allow bulldoze/leap on ground (not while flying)
        if (isFlying() || isTakeoff() || isLanding() || isHovering()) {
            return;
        }

        // Check if controls are locked (transition in progress)
        if (areRiderControlsLocked()) {
            return;
        }

        // Phase 2: Use leaping body slam instead of bulldoze
        if (isPhase2Active()) {
            onRiderLeapSlam(player);
            return;
        }

        // Phase 1: Normal bulldoze logic
        // Check cooldown
        if (bulldozeCooldownTicks > 0) {
            return;
        }

        // Toggle bulldozing on/off
        if (bulldozing) {
            // Turn OFF
            bulldozing = false;
            this.entityData.set(DATA_BULLDOZING, false);
            setAccelerating(false); // Stop forced sprint
            bulldozeCooldownTicks = 40; // 2 second cooldown
            lockRiderControls(20); // Lock controls for 1 second during exit animation
            animationHandler.triggerBulldozeExitAnimation();
            getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_BULLDOZER_EXIT.get(), 1.0f, 1.0f, 45);
        } else {
            // Turn ON
            bulldozing = true;
            this.entityData.set(DATA_BULLDOZING, true);
            setAccelerating(true); // Force sprint
            lockRiderControls(20); // Lock controls for 1 second during enter animation
            animationHandler.triggerBulldozeEnterAnimation();
            getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_BULLDOZER_ENTER.get(), 1.0f, 1.0f, 41);
        }
    }

    protected void onRiderLeapSlam(Player player) {
        Vec3 look = this.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            float yawRad = (float) Math.toRadians(this.getYRot());
            horizontal = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        }
        startLeapSlam(horizontal);
    }

    /**
     * AI entrypoint for leaping body slam to close distance.
     * Returns true when the leap successfully starts.
     */
    public boolean tryStartLeapSlamForAI(@Nullable LivingEntity target) {
        if (level().isClientSide || target == null) {
            return false;
        }
        if (isBaby() || isTame()) {
            return false;
        }
        if (!isPhase2Active()) {
            return false;
        }
        if (isFlying() || isTakeoff() || isLanding() || isHovering()) {
            return false;
        }
        if (isAiSpecialCombatActive()) {
            return false;
        }
        if (!onGround()) {
            return false;
        }
        if (bulldozing || leaping || leapImpactRecoveryTicks > 0) {
            return false;
        }
        if (areRiderControlsLocked() || getActiveAbility() != null) {
            return false;
        }
        if (!isTargetValid(target)) {
            return false;
        }

        Vec3 toTarget = target.position().subtract(this.position());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            return false;
        }

        // Face target first so leap visuals and hit direction align.
        float yaw = (float) (Mth.atan2(horizontal.z, horizontal.x) * (180F / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;

        return startLeapSlam(horizontal);
    }

    private boolean startLeapSlam(Vec3 horizontalDirection) {
        // Cooldown is AI-only; player-ridden leap uses rider input flow.
        if (leaping) {
            return false;
        }
        if (!this.isVehicle() && leapCooldownTicks > 0) {
            return false;
        }

        Vec3 dir = new Vec3(horizontalDirection.x, 0.0D, horizontalDirection.z);
        if (dir.lengthSqr() < 1.0E-6D) {
            return false;
        }
        dir = dir.normalize();

        Vec3 leapVec = new Vec3(
            dir.x * LEAP_HORIZONTAL_SPEED,
            LEAP_VERTICAL_BOOST,
            dir.z * LEAP_HORIZONTAL_SPEED
        );

        leaping = true;
        leapWasVehicle = this.isVehicle();
        this.entityData.set(DATA_LEAPING, true);
        leapAnimState = LEAP_STATE_TAKEOFF;
        this.entityData.set(DATA_LEAP_ANIM_STATE, LEAP_STATE_TAKEOFF);
        leapVelocity = leapVec;
        wasAirborneBeforeLanding = false;
        leapImpactTriggered = false;
        this.setDeltaMovement(leapVec);
        this.getNavigation().stop();
        this.hasImpulse = true;
        leapGroundedTicks = 0;
        getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LEAP.get(), 1.0f, 1.0f, 58);

        if (level() instanceof ServerLevel server) {
            breakGroundCircle(server, position(), 8.0D);
        }
        return true;
    }

    protected void onRiderPhase2Toggle(Player player) {
        if (isBaby()) {
            return;
        }
        // Only allow Phase 2 on ground (not while flying)
        if (isFlying() || isTakeoff() || isLanding() || isHovering()) {
            return;
        }

        // Can't use Phase 2 while bulldozing or leaping
        if (bulldozing || leaping) {
            return;
        }

        // Check cooldown
        if (phase2CooldownTicks > 0) {
            return;
        }

        // Check if controls are locked (transition in progress)
        if (areRiderControlsLocked()) {
            return;
        }

        // Toggle Phase 2 on/off
        if (phase2Active) {
            // Turn OFF
            phase2Active = false;
            this.entityData.set(DATA_PHASE2, false);
            phase2CooldownTicks = 40; // 2 second cooldown
            lockRiderControls(13); // Lock controls for 0.63 seconds during exit animation
            animationHandler.triggerPhase2ExitAnimation();
            getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_PHASE2_EXIT.get(), 1.0f, 1.0f, 38);
        } else {
            // Turn ON
            phase2Active = true;
            this.entityData.set(DATA_PHASE2, true);
            lockRiderControls(20); // Lock controls for 1 second during enter animation
            animationHandler.triggerPhase2EnterAnimation();
            getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_PHASE2_ENTER.get(), 1.0f, 1.0f, 47);
        }
    }

    public boolean isPhase2Active() {
        return level().isClientSide ? this.entityData.get(DATA_PHASE2) : phase2Active;
    }

    public boolean isAiPhase2Locked() {
        return !level().isClientSide && aiPhase2LockTicks > 0;
    }

    private void startAiPhase2Lock(int ticks) {
        if (!level().isClientSide) {
            aiPhase2LockTicks = Math.max(aiPhase2LockTicks, ticks);
        }
    }

    /**
     * AI-only Phase 2 toggle. Mirrors rider checks but without rider control locking.
     */
    public boolean tryTogglePhase2ForAI(boolean enable) {
        if (level().isClientSide) {
            return false;
        }
        if (isBaby()) {
            return false;
        }
        if (isVehicle() || getControllingPassenger() != null) {
            return false;
        }
        if (isFlying() || isTakeoff() || isLanding() || isHovering()) {
            return false;
        }
        if (bulldozing || leaping) {
            return false;
        }
        if (phase2CooldownTicks > 0) {
            return false;
        }
        if (phase2Active == enable) {
            return false;
        }
        if (getActiveAbility() != null) {
            return false;
        }

        phase2Active = enable;
        this.entityData.set(DATA_PHASE2, enable);
        if (enable) {
            startAiPhase2Lock(20);
            animationHandler.triggerPhase2EnterAnimation();
            getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_PHASE2_ENTER.get(), 1.0f, 1.0f, 47);
        } else {
            phase2CooldownTicks = 40; // 2 second cooldown
            startAiPhase2Lock(13);
            animationHandler.triggerPhase2ExitAnimation();
            getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_PHASE2_EXIT.get(), 1.0f, 1.0f, 38);
        }
        return true;
    }

    public boolean shouldUseRightWingSwipe() {
        return useRightWingSwipe;
    }

    public void toggleWingSwipeSide() {
        useRightWingSwipe = !useRightWingSwipe;
    }

    @Override
    protected void applyRiderVerticalInput(Player player, boolean goingUp, boolean goingDown, boolean locked) {
        boolean inWater = this.isInWater() || this.isInWaterOrBubble();
        boolean canRecover = canRecoverTakeoffFromFall();

        if (inWater) {
            setGoingUp(goingUp);
            setGoingDown(goingDown);
            return;
        }

        if (locked) {
            setGoingUp(false);
            setGoingDown(false);
            return;
        }

        if (goingUp && canRecover) {
            setGoingUp(true);
            setGoingDown(false);
            startTakeoffSequence(0.11D, TAKEOFF_ANIMATION_TICKS);
            return;
        }

        if (isFlying() || canRecover) {
            setGoingUp(goingUp);
            setGoingDown(goingDown);
        } else {
            setGoingUp(false);
            setGoingDown(false);
        }
    }

    @Override
    protected void onRiderTakeoffRequest(Player player) {
        if (canRecoverTakeoffFromFall()) {
            setGoingUp(true);
            setGoingDown(false);
            startTakeoffSequence(0.11D, TAKEOFF_ANIMATION_TICKS);
            return;
        }
        if (!isFlying()) {
            enforcePrimaryMeleeForFlight(player);
            requestRiderTakeoff();
        }
    }

    public boolean isFallingForAnimation() {
        return DragonRiderFallRecovery.isFallingForAnimation(
                isVehicle(),
                isFlying(),
                isTakeoff(),
                isLanding(),
                isHovering(),
                onGround(),
                isInWaterOrBubble(),
                isInLava(),
                this.fallDistance,
                getDeltaMovement()
        );
    }

    private boolean canRecoverTakeoffFromFall() {
        return DragonRiderFallRecovery.canRecoverTakeoffFromFall(
                isTame(),
                isVehicle(),
                isAlive(),
                isBaby(),
                isFlying(),
                isTakeoff(),
                isLanding(),
                isHovering(),
                onGround(),
                isInWaterOrBubble(),
                isInLava(),
                leaping || getLeapAnimState() != 0,
                this.fallDistance,
                getDeltaMovement()
        );
    }

    public void requestRiderTakeoff() {
        riderFlightComponent.requestRiderTakeoff();
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

    public void switchToAirNavigation() {
        this.navigationModeController.switchToAir();
    }

    public void switchToGroundNavigation() {
        this.navigationModeController.switchToGround();
    }

    @Override
    public void markLandedNow() {
        setFlying(false);
        setLanding(false);
        takeoffComponent.clear();
        setHovering(false);
        timeFlying = 0;
        if (!level().isClientSide) {
            switchToGroundNavigation();
            setNoGravity(false);
        }
    }

    public void handleAiLandingComplete() {
        if (isInWaterOrBubble()) {
            suppressSleep(60);
            markLandedNow();
            return;
        }
        if (!level().isClientSide) {
            long now = level().getGameTime();
            if (now - lastAiLandedAnimTick >= 15L) {
                String landedAnim = isPhase2Active() ? "phase2_landed" : "landed";
                triggerAnim("action", landedAnim);
                if (isPhase2Active()) {
                    getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_PHASE2_LANDED.get(), 1.0f, 1.0f, 40);
                } else {
                    getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LANDED.get(), 1.0f, 1.0f, 42);
                }
                lockRiderControls(13);
                suppressSleep(60);
                lastAiLandedAnimTick = now;
            }
        }
        markLandedNow();
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

    public void applyConfiguredAttributes() {
        if (this.level().isClientSide) {
            return;
        }
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        double attackDamage = config.abilityDamage("bite", 15.0D);

        setAttributeBase(Attributes.MAX_HEALTH, isBaby() ? BABY_MAX_HEALTH : config.maxHealth());
        setAttributeBase(Attributes.FLYING_SPEED, config.flyingSpeed());
        setAttributeBase(Attributes.ARMOR, isBaby() ? BABY_ARMOR : config.armor());
        setAttributeBase(Attributes.ATTACK_DAMAGE, isBaby() ? 0.0D : attackDamage);
        // MOVEMENT_SPEED is hardcoded in createAttributes() - no config needed

        double maxHealth = isBaby() ? BABY_MAX_HEALTH : config.maxHealth();
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

    // Ground speeds are now hardcoded constants (RIDER_WALK_SPEED, RIDER_RUN_SPEED)


    @Override
    public boolean canTakeoff() {
        return !isBaby()
                && !isFlying()
                && onGround()
                && !isInWaterOrBubble()
                && !isInLava();
    }

    // ===== STATE MANAGEMENT =====

    public void setFlying(boolean flying) {
        // Never let autonomous/AI paths force flight start while submerged.
        if (flying && !this.isVehicle() && (this.isInWater() || this.isInWaterOrBubble() || this.isInLava())) {
            return;
        }
        boolean wasFlying = isFlying();
        if (flying && !wasFlying && !isTakeoff() && this.onGround()) {
            startTakeoffSequence(0.12D, TAKEOFF_ANIMATION_TICKS);
            return;
        }
        this.entityData.set(DATA_FLYING, flying);

        if (wasFlying != flying) {
            this.setAccelerating(false);
            if (flying) {
                enforcePrimaryMeleeForFlight(getControllingPassenger() instanceof Player p ? p : null);
                switchToAirNavigation();
                setRunning(false);
            } else {
                takeoffComponent.clear();
                if (!isLanding()) {
                    switchToGroundNavigation();
                }
            }
        }
    }

    public void setTakeoff(boolean takeoff) {
        this.entityData.set(DATA_TAKEOFF, takeoff);
        if (!takeoff && takeoffComponent.isActive()) {
            takeoffComponent.clear();
            return;
        }
        if (takeoff && !level().isClientSide) {
            triggerAnim("instant", isPhase2Active() ? "phase2_takeoff" : "takeoff");
            getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_TAKEOFF.get(), 1.0f, 1.0f, 69);
        }
    }

    public void setHovering(boolean hovering) {
        this.entityData.set(DATA_HOVERING, hovering);
    }

    public void setLanding(boolean landing) {
        if (landing && this.onGround() && !this.isFlying() && !this.isTakeoff()) {
            return;
        }
        this.entityData.set(DATA_LANDING, landing);
    }

    @Override
    public void setRunning(boolean running) {
        // MOVEMENT_SPEED is fixed for AI - rider speed is handled by RiderController
    }

    @Override
    public boolean isRunning() {
        return !isFlying() && getEffectiveGroundState() == 2;
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
        double altitude = getY() - level().getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) getX(),
                (int) getZ());
        boolean riddenByOwner = isRiddenByOwner();
        DragonFlightStateEvaluator.FlightInput input = new DragonFlightStateEvaluator.FlightInput(
                isFlying(),
                shouldPlayTakeoff(),
                isHovering(),
                isLanding(),
                riddenByOwner,
                isGoingUp(),
                isGoingDown(),
                isAccelerating(),
                riddenByOwner && shouldForceSurfaceGlide(altitude),
                getX(),
                getY(),
                getZ(),
                this.yo,
                altitude,
                RIDER_GLIDE_ALTITUDE_THRESHOLD,
                RIDER_GLIDE_ALTITUDE_EXIT,
                getDeltaMovement()
        );
        return DragonFlightStateEvaluator.evaluateSyncedMode(flightModeState, input);
    }

    private boolean shouldPlayTakeoff() {
        return isTakeoff();
    }

    private boolean isRiddenByOwner() {
        if (!isTame() || !isVehicle()) {
            return false;
        }
        if (!(getControllingPassenger() instanceof Player player)) {
            return false;
        }
        return isOwnedBy(player);
    }

    private boolean shouldForceSurfaceGlide(double altitudeAboveTerrain) {
        return altitudeAboveTerrain <= RIDER_LOW_ALTITUDE_GLIDE_THRESHOLD || isNearWaterSurface();
    }

    public DragonFlightStateEvaluator.VisualState getVisualFlightState(float partialTick) {
        return DragonFlightStateEvaluator.evaluateVisualState(
                getSyncedFlightMode(),
                isVehicle(),
                getFlightPitchRadians(partialTick),
                getDeltaMovement()
        );
    }

    private boolean isNearWaterSurface() {
        if (level() == null) return false;

        double dragonY = getY();
        if (dragonY > RIDER_WATER_SURFACE_LEVEL + RIDER_WATER_SURFACE_TOLERANCE) {
            return false;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int baseX = Mth.floor(getX());
        int baseY = Mth.floor(dragonY);
        int baseZ = Mth.floor(getZ());

        for (int dx = -RIDER_WATER_SCAN_RADIUS; dx <= RIDER_WATER_SCAN_RADIUS; dx++) {
            for (int dz = -RIDER_WATER_SCAN_RADIUS; dz <= RIDER_WATER_SCAN_RADIUS; dz++) {
                for (int dy = 0; dy <= RIDER_WATER_SCAN_DEPTH; dy++) {
                    cursor.set(baseX + dx, baseY - dy, baseZ + dz);
                    if (!level().hasChunkAt(cursor)) continue;

                    if (!level().getBlockState(cursor).getFluidState().isEmpty()) {
                        double surfaceY = cursor.getY() + 1.0;
                        if (Math.abs(dragonY - surfaceY) <= RIDER_WATER_SURFACE_TOLERANCE) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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

    public float getFireBreathEnergy() {
        return this.entityData.get(DATA_FIRE_BREATH_ENERGY);
    }

    public void setFireBreathEnergy(float energy) {
        float clamped = Mth.clamp(energy, 0.0f, 1.0f);
        this.entityData.set(DATA_FIRE_BREATH_ENERGY, clamped);
        // Hysteresis: deplete at near-empty, re-arm at a higher threshold to avoid flicker.
        if (clamped >= FIRE_BREATH_REARM_THRESHOLD && isFireBreathDepleted()) {
            setFireBreathDepleted(false);
        }
    }

    public boolean hasFireBreathEnergy() {
        return getFireBreathEnergy() > FIRE_BREATH_DEPLETED_THRESHOLD;
    }

    public boolean isFireBreathEnergyFull() {
        return getFireBreathEnergy() >= 0.999f;
    }

    public boolean isFireBreathDepleted() {
        return this.entityData.get(DATA_FIRE_BREATH_DEPLETED);
    }

    public void setFireBreathDepleted(boolean depleted) {
        this.entityData.set(DATA_FIRE_BREATH_DEPLETED, depleted);
    }

    public boolean canUseFireBreath() {
        return hasFireBreathEnergy() && !isFireBreathDepleted();
    }

    private void tickFireBreathEnergy() {
        if (!isBreathingFire() && getFireBreathEnergy() < 1.0f) {
            float regen = (float) com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader.getInstance()
                    .getConfig(com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader.IGNIVORUS_ID)
                    .extraDouble("fire_breath_regen_per_tick", FIRE_BREATH_ENERGY_REGEN);
            regen = Math.max(0.0f, regen);
            if (regen > 0.0f) {
                setFireBreathEnergy(getFireBreathEnergy() + regen);
            }
        }
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
        // Try fireBone first
        Vec3 clientBone = getBonePositionForHitbox("fireBoneOrigin");
        if (clientBone != null) {
            return clientBone;
        }
        // Try mouth_origin as backup (might be synced)
        Vec3 mouthOrigin = getBonePositionForHitbox("mouth_origin");
        if (mouthOrigin != null) {
            return mouthOrigin;
        }
        return computeFireBoneFallback(partialTicks);
    }

    private Vec3 computeFireBoneFallback(float partialTicks) {
        double x = Mth.lerp(partialTicks, this.xo, this.getX());
        double y = Mth.lerp(partialTicks, this.yo, this.getY());
        double z = Mth.lerp(partialTicks, this.zo, this.getZ());

        float yawDeg = Mth.lerp(partialTicks, this.yHeadRotO, this.yHeadRot);

        // When flying and ridden, use rider's pitch (dragon's xRot is locked at 0)
        float pitchDeg;
        if (isVehicle() && isFlying()) {
            Entity rider = getControllingPassenger();
            if (rider != null) {
                pitchDeg = rider.getXRot();
                // Clamp pitch to match neck limits (±20 degrees)
                pitchDeg = Mth.clamp(pitchDeg, -20.0F, 20.0F);
            } else {
                pitchDeg = Mth.lerp(partialTicks, this.xRotO, this.getXRot());
            }
        } else {
            pitchDeg = Mth.lerp(partialTicks, this.xRotO, this.getXRot());
        }

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
        if (isBaby()) {
            return null;
        }
        // Phase 2 uses melee mode toggle (wing swipe or stomp) when grounded
        // But falls back to bite when flying
        if (isPhase2Active()) {
            if (isFlying()) {
                return IgnivorusAbilities.IGNIVORUS_BITE;
            }
            return getMeleeMode() == 1 ? IgnivorusAbilities.IGNIVORUS_STOMP : IgnivorusAbilities.IGNIVORUS_WING_SWIPE;
        }

        // Normal mode uses melee mode toggle (bite or body slam)
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

    // ===== BANKING & PITCHING ANIMATIONS =====

    private void tickBankingLogic() {
        DragonFlightVisuals.tickBanking(
                this.flightVisualState,
                this.isFlying(),
                this.horizontalCollision,
                this.verticalCollision,
                this.getYRot(),
                this.yRotO
        );
    }

    private void tickPitchingLogic() {
        tickRiderLandingBlendTimer();
        DragonFlightVisuals.beginPitchTick(this.flightVisualState);
        if (level().isClientSide) {
            this.flightVisualState.flightPitchRad = this.entityData.get(DATA_FLIGHT_PITCH);
            return;
        }
        // Reset pitching when in water, not flying, or when controls are locked - INSTANT reset
        boolean inWater = this.isInWater() || this.isInWaterOrBubble();
        if (inWater || areRiderControlsLocked() || !isFlying() || isOrderedToSit() || isBreathingFire()) {
            DragonFlightVisuals.resetPitch(this.flightVisualState);
            this.entityData.set(DATA_FLIGHT_PITCH, this.flightVisualState.flightPitchRad);
            return;
        }

        Vec3 velocity = getDeltaMovement();
        float targetPitchRad = 0f;

        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            boolean useKeyPitch = isRiderPitchKeyMode();

            if (useKeyPitch) {
                float rawKeyPitchRad = 0f;
                if (isGoingUp()) {
                    rawKeyPitchRad = (float) Math.toRadians(RIDER_KEY_PITCH_DEG);
                } else if (isGoingDown()) {
                    rawKeyPitchRad = (float) -Math.toRadians(RIDER_KEY_PITCH_DEG);
                }

                targetPitchRad = DragonFlightVisuals.smoothRiderPitchInput(this.flightVisualState, rawKeyPitchRad);
            } else {
                // RIDING: Use player camera for visual pitch WHEN MOVING
                float riderForward = this.entityData.get(DATA_RIDER_FORWARD);
                float riderStrafe = this.entityData.get(DATA_RIDER_STRAFE);
                boolean hasMovementInput = Math.abs(riderForward) > 0.01f || Math.abs(riderStrafe) > 0.01f;

                if (hasMovementInput) {
                    // Player is pressing WASD  use camera pitch for visuals
                    // Negate because Minecraft xRot is positive=down, but we want dragon to pitch up when looking up
                    float rawPlayerPitchRad = -(float)Math.toRadians(player.getXRot());
                    targetPitchRad = DragonFlightVisuals.smoothRiderPitchInput(this.flightVisualState, rawPlayerPitchRad);
                } else {
                    DragonFlightVisuals.clearRiderPitchInput(this.flightVisualState);
                    targetPitchRad = 0f;
                }
            }

            boolean wantsLanding = isGoingDown() || (!useKeyPitch && player.getXRot() > 30.0f);
            if (wantsLanding) {
                double altitude = getAltitudeAboveTerrain();
                if (altitude != Double.POSITIVE_INFINITY && altitude >= -0.25D && altitude <= LANDING_BLEND_ALTITUDE) {
                    float landingPitchRad = (float) -Math.toRadians(35.0f);
                    targetPitchRad = Math.min(targetPitchRad, landingPitchRad);
                }
            }
        } else {
            targetPitchRad = DragonFlightVisuals.computeAiPitchTarget(velocity);
        }
        this.flightVisualState.flightPitchRad =
                DragonFlightVisuals.approachPitch(this.flightVisualState.flightPitchRad, targetPitchRad);
        this.entityData.set(DATA_FLIGHT_PITCH, this.flightVisualState.flightPitchRad);

        // Trigger landing blend when descending close to ground while ridden
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            boolean wantsLanding = isGoingDown() || (!isRiderPitchKeyMode() && player.getXRot() > 30.0f);
            if (wantsLanding) {
                double altitude = getAltitudeAboveTerrain();
                if (altitude != Double.POSITIVE_INFINITY && altitude >= -0.25D && altitude <= LANDING_BLEND_ALTITUDE) {
                    triggerRiderLandingBlend();
                }
            }
        }
        // Pitching is now fully procedural - no need for animation controller directions
    }

    private void tickBarrelRollLogic() {
        float currentRoll = getAccumulatedRoll();
        boolean barrelRollEnabled = SaintsDragonsConfig.BARREL_ROLL_ENABLED.get();
        if (barrelRollEnabled && isVehicle() && getControllingPassenger() != null) {
            float riderForward = this.entityData.get(DATA_RIDER_FORWARD);
            float riderStrafe = this.entityData.get(DATA_RIDER_STRAFE);
            if (riderForward > 0.1f && Math.abs(riderStrafe) > 0.1f) {
                currentRoll += riderStrafe * BARREL_ROLL_INPUT_SPEED;
            }
        }
        DragonBarrelRollHelper.Output output = DragonBarrelRollHelper.tick(
                currentRoll,
                this.smoothedRoll,
                new DragonBarrelRollHelper.Input(
                        isVehicle(),
                        onGround(),
                        isLanding(),
                        barrelRollEnabled && isActivelyBarrelRolling(),
                        shouldEaseAirAutoAlign(),
                        isRiderLandingBlendActive(),
                        LANDING_BLEND_ALTITUDE,
                        getAltitudeAboveTerrain()
                ),
                BARREL_ROLL_CONFIG
        );

        setAccumulatedRoll(output.accumulatedRoll());
        this.prevSmoothedRoll = output.prevSmoothedRoll();
        this.smoothedRoll = output.smoothedRoll();
    }


    private void tickRiderLandingBlendTimer() {
        trackRiderAirborneForLanding();
        boolean inWater = this.isInWater() || this.isInWaterOrBubble();

        if (inWater) {
            riderLandingBlendTicks = 0;
            if (!level().isClientSide) {
                this.entityData.set(DATA_RIDER_LANDING_BLEND, false);
                if (isFlying() || isTakeoff() || isLanding() || isHovering()) {
                    setFlying(false);
                    setTakeoff(false);
                    setLanding(false);
                    setHovering(false);
                    timeFlying = 0;
                    switchToGroundNavigation();
                }
            }
            return;
        }

        if (!isVehicle() || !isFlying() || onGround()) {
            // If we were actively landing and now touched ground, trigger landed animation
            boolean wasLanding = isFlying() && riderLandingBlendTicks > 0 && isRiderLandingBlendActive();
            boolean touchdownFromFlight = consumeRiderTouchdownFromAir(0.15D);
            riderLandingBlendTicks = 0;
            if (!level().isClientSide) {
                this.entityData.set(DATA_RIDER_LANDING_BLEND, false);

                // Trigger landed animation when rider landing completes or when a gentle touchdown happens.
                if ((wasLanding || touchdownFromFlight) && onGround() && isVehicle()) {
                    // Properly clear flight state to prevent T-pose gliding bug
                    setFlying(false);
                    setTakeoff(false);
                    timeFlying = 0;
                    // Use Phase 2 landed animation if in Phase 2 mode
                    String landedAnim = isPhase2Active() ? "phase2_landed" : "landed";
                    triggerAnim("action", landedAnim);  // Trigger as one-shot animation
                    if (isPhase2Active()) {
                        getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_PHASE2_LANDED.get(), 1.0f, 1.0f, 40);
                    } else {
                        getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LANDED.get(), 1.0f, 1.0f, 42);
                    }
                    lockRiderControls(13);  // Lock controls for 0.63 seconds while animation plays
                }
            }
            return;
        }
        if (riderLandingBlendTicks > 0) {
            riderLandingBlendTicks--;
            if (riderLandingBlendTicks == 0 && !level().isClientSide) {
                this.entityData.set(DATA_RIDER_LANDING_BLEND, false);
            }
        }
    }

    private void triggerRiderLandingBlend() {
        riderLandingBlendTicks = RIDER_LANDING_BLEND_DURATION;
        if (!level().isClientSide) {
            this.entityData.set(DATA_RIDER_LANDING_BLEND, true);
        }
    }

    public boolean isRiderLandingBlendActive() {
        return this.entityData.get(DATA_RIDER_LANDING_BLEND);
    }

    private double getAltitudeAboveTerrain() {
        return getAltitudeAboveCollisionTerrain(24, true);
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

            if (!level().hasChunkAt(checkPos)) continue;

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
        if (!DragonGriefingRules.canDestroyBlocks(level())) {
            return;
        }

        // Only break blocks when moving OR being ridden (prevent stationary destruction when wild)
        boolean isBeingRidden = this.isVehicle();
        Vec3 velocity = this.getDeltaMovement();
        double speed = velocity.horizontalDistanceSqr();
        boolean collisionStuck = this.horizontalCollision || this.isInWall();
        boolean chasingTarget = this.getTarget() != null && this.getTarget().isAlive();

        // If not being ridden, require movement unless we're actively stuck while pursuing a target.
        // This preserves anti-grief behavior for idle wild dragons but prevents easy tree/hill cheesing.
        if (!isBeingRidden && speed < 0.01 && !(collisionStuck && chasingTarget)) {
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

        float sitProgress = getSitProgress();
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
                setSitProgress(sitProgress);
            }
        } else {
            // Not ordered to sit - handle standing up
            if (this.isVehicle()) {
                // Instantly reset when ridden
                if (sitProgress != 0f) {
                    clearSitProgress();
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
                setSitProgress(sitProgress);
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

    // ===== SIT TRANSITIONS =====
    @Override
    public boolean isInSitTransition() {
        return isSittingDown || isStandingUp;
    }

    @Override
    public boolean isSittingDownAnimation() {
        return isSittingDown;
    }

    @Override
    public boolean isStandingUpAnimation() {
        return isStandingUp;
    }

    public float getBankAngleDegrees(float partialTick) {
        return Mth.lerp(partialTick, this.flightVisualState.prevBankAngle, this.flightVisualState.bankAngle);
    }
    public float getFlightPitchRadians(float partialTick) {
        return Mth.lerp(partialTick, this.flightVisualState.prevFlightPitchRad, this.flightVisualState.flightPitchRad);
    }

    public float getAccumulatedRoll() {
        return this.entityData.get(DATA_ACCUMULATED_ROLL);
    }

    public void setAccumulatedRoll(float radians) {
        this.entityData.set(DATA_ACCUMULATED_ROLL, radians);
    }

    public void addAccumulatedRoll(float radians) {
        setAccumulatedRoll(getAccumulatedRoll() + radians);
    }

    private boolean shouldEaseAirAutoAlign() {
        if (!isFlying() || areRiderControlsLocked()) {
            return false;
        }

        if (Math.abs(this.entityData.get(DATA_RIDER_STRAFE)) > 0.05f) {
            return false;
        }

        return Math.abs(this.entityData.get(DATA_RIDER_FORWARD)) > 0.05f;
    }

    private boolean isActivelyBarrelRolling() {
        return this.entityData.get(DATA_RIDER_FORWARD) > 0.1f
                && Math.abs(this.entityData.get(DATA_RIDER_STRAFE)) > 0.1f;
    }

    public float getSmoothedRoll(float partialTick) {
        return Mth.lerp(partialTick, prevSmoothedRoll, smoothedRoll);
    }

    public boolean isRiderPitchKeyMode() {
        return this.entityData.get(DATA_PITCH_KEY_MODE);
    }

    public void setRiderPitchKeyMode(boolean enabled) {
        this.entityData.set(DATA_PITCH_KEY_MODE, enabled);
    }


    // ===== GECKOLIB ANIMATION =====

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Movement controller handles idle/walk/run/flight/sit animations
        AnimationController<Ignivorus> movementController =
            new AnimationController<>(this, "movement", 8, animationHandler::handleMovementAnimation);
        movementController.setSoundKeyframeHandler(event -> {
            String soundKey = event.getKeyframeData().getSound();
            if (soundKey != null && !soundKey.isEmpty()) {
                handleAnimationSound(soundKey);
            }
        });

        // Action controller for triggerable animations (sit transitions, fire breath, etc.)
        AnimationController<Ignivorus> actionController =
            new AnimationController<>(this, "action", 3, state -> {
                // CRITICAL: Stop action controller during taming stun to prevent animation bleeding
                if (isTamingStunned()) {
                    return software.bernie.geckolib.core.object.PlayState.STOP;
                }
                return software.bernie.geckolib.core.object.PlayState.STOP;
            });

        AnimationController<Ignivorus> instantController =
            new AnimationController<>(this, "instant", 1, animationHandler::instantActionPredicate);
        animationHandler.setupInstantActionController(instantController);
        instantController.setSoundKeyframeHandler(event -> {
            String soundKey = event.getKeyframeData().getSound();
            if (soundKey != null && !soundKey.isEmpty()) {
                handleAnimationSound(soundKey);
            }
        });

        // Register all action animations via handler
        animationHandler.setupActionController(actionController);
        actionController.setSoundKeyframeHandler(event -> {
            String soundKey = event.getKeyframeData().getSound();
            if (soundKey != null && !soundKey.isEmpty()) {
                handleAnimationSound(soundKey);
            }
        });

        controllers.add(movementController, instantController, actionController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return dragonCache;
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

    private void handleAnimationSound(String soundKey) {
        DragonSoundProfile profile = getSoundProfile();
        if (profile != null) {
            // Let the profile handle it (it knows about flaps, steps, etc.)
            boolean handled = profile.handleAnimationSound(getSoundHandler(), this, soundKey, null);
            if (!handled) {
                // Profile didn't handle it, try as vocal
                getSoundHandler().playVocal(soundKey);
            }
        }
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
    }

    private void tickGroundStepAudio() {
        if (groundStepSoundCooldownTicks > 0) {
            groundStepSoundCooldownTicks--;
        }
        if (isBaby() || isFlying() || isTakeoff() || isLanding() || isHovering() || isInWaterOrBubble() || !onGround()) {
            groundStepSoundCooldownTicks = 0;
            return;
        }
        int moveState = this.entityData.get(DATA_GROUND_MOVE_STATE);
        if (moveState <= 0) {
            double speedSqr = this.getDeltaMovement().horizontalDistanceSqr();
            if (speedSqr > 0.02D) {
                moveState = 2;
            } else if (speedSqr > 0.0008D) {
                moveState = 1;
            }
        }
        if (moveState <= 0) {
            groundStepSoundCooldownTicks = 0;
            return;
        }
        if (groundStepSoundCooldownTicks > 0) {
            return;
        }
        boolean running = moveState == 2;
        if (isPhase2Active()) {
            int duration = running ? 33 : 42;
            getSoundHandler().playMovingEntitySound(
                    running ? ModSounds.IGNIVORUS_PHASE2_RUN.get() : ModSounds.IGNIVORUS_PHASE2_WALK.get(),
                    1.0f, 1.0f, duration
            );
            groundStepSoundCooldownTicks = duration;
            return;
        }
        int duration = running ? 25 : 42;
        getSoundHandler().playMovingEntitySound(
                running ? ModSounds.IGNIVORUS_RUN.get() : ModSounds.IGNIVORUS_WALK.get(),
                1.0f, 1.0f, duration
        );
        groundStepSoundCooldownTicks = duration;
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

    // ===== SERVER BONE POSITION CACHE (synced from client for hitboxes) =====

    private final Map<String, Vec3> serverBonePositionCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Called by network packet to update bone positions on server.
     * These positions come from the client's GeckoLib renderer.
     */
    public void setServerBonePosition(String boneName, Vec3 position) {
        if (boneName == null || position == null) return;
        this.serverBonePositionCache.put(boneName, position);
    }

    /**
     * Get a bone position for hitbox placement.
     * On client: uses clientLocatorCache (from renderer)
     * On server: uses serverBonePositionCache (synced from client)
     */
    public Vec3 getBonePositionForHitbox(String boneName) {
        if (boneName == null) return null;
        if (this.level().isClientSide) {
            return this.clientLocatorCache.get(boneName);
        } else {
            return this.serverBonePositionCache.get(boneName);
        }
    }

    // ===== BREEDING (placeholder) =====

    @Override
    public boolean canMate(@NotNull net.minecraft.world.entity.animal.Animal otherAnimal) {
        if (!this.canBreed()) {
            return false;
        }
        if (otherAnimal instanceof Ignivorus otherDragon) {
            if (this.isFemale() == otherDragon.isFemale()) {
                return false;
            }
            return otherDragon.canBreed();
        }
        return false;
    }

    @Override
    public boolean canBreed() {
        return this.isTame() && !this.isBaby() && this.getHealth() >= this.getMaxHealth() && this.isInLove();
    }

    @Override
    public BlockState getEggBlockState() {
        return ModBlocks.IGNIVORUS_EGG.get().defaultBlockState();
    }

    @Override
    public void configureEggBlockEntity(BlockEntity blockEntity, @Nullable DragonEntity partner) {
        if (!(blockEntity instanceof IgnivorusEggBlockEntity eggEntity)) {
            return;
        }
        java.util.UUID ownerUUID = resolveEggOwnerUUID(partner);
        if (ownerUUID != null) {
            eggEntity.setOwnerUUID(ownerUUID);
        }
        DragonGender babyGender = this.getRandom().nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE;
        eggEntity.setBabyGender(babyGender);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(@NotNull net.minecraft.server.level.ServerLevel level, @NotNull AgeableMob otherParent) {
        Ignivorus baby = com.leon.saintsdragons.common.registry.ModEntities.IGNIVORUS.get().create(level);
        if (baby != null) {
            baby.setGender(this.random.nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE);
            assignMotherToBaby(baby, otherParent);
            java.util.UUID ownerId = this.getOwnerUUID();
            if (ownerId != null) {
                baby.setOwnerUUID(ownerId);
                baby.setTame(true);
            }
            baby.skipRespawnTicks = 5;
            baby.setAge(-24000);
            baby.setBaby(true);
            baby.applyConfiguredAttributes();
            baby.setHealth(baby.getMaxHealth());

            net.minecraft.core.BlockPos safePos = findSafeBabySpawnPos(level, this.blockPosition());
            double spawnY = safePos != null ? safePos.getY() : this.getY();
            baby.moveTo(this.getX(), spawnY, this.getZ(), this.getYRot(), 0.0F);
            registerToOwnerCodex(baby, level);
        }
        return baby;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        saveRideableData(tag);  // Save flight state (flying, hovering, takeoff, etc.)
        tag.putInt("TimeFlying", timeFlying);  // Save flying duration
        this.combatManager.saveToNBT(tag);
        tag.putInt("FeedingCooldownTicks", Math.max(0, this.entityData.get(DATA_FEEDING_COOLDOWN)));
        tag.putBoolean("RiderPitchKeyMode", isRiderPitchKeyMode());
        tag.putBoolean("Bulldozing", bulldozing);
        tag.putInt("BulldozeCooldownTicks", Math.max(0, bulldozeCooldownTicks));
        tag.putBoolean("Phase2Active", phase2Active);
        tag.putInt("Phase2CooldownTicks", Math.max(0, phase2CooldownTicks));
        tag.putBoolean("Leaping", leaping);
        tag.putInt("LeapAnimState", leapAnimState);
        tag.putInt("LeapCooldownTicks", Math.max(0, leapCooldownTicks));
        tag.putFloat("FireBreathEnergy", getFireBreathEnergy());
        tag.putBoolean("FireBreathDepleted", isFireBreathDepleted());
        tamingController.save(tag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadRideableData(tag);  // Restore flight state
        this.timeFlying = tag.getInt("TimeFlying");  // Restore flying duration
        this.combatManager.loadFromNBT(tag);
        if (tag.contains("FeedingCooldownTicks")) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, tag.getInt("FeedingCooldownTicks")));
        }
        if (tag.contains("RiderPitchKeyMode")) {
            setRiderPitchKeyMode(tag.getBoolean("RiderPitchKeyMode"));
        }
        if (tag.contains("Bulldozing")) {
            bulldozing = tag.getBoolean("Bulldozing");
            this.entityData.set(DATA_BULLDOZING, bulldozing);
        }
        if (tag.contains("BulldozeCooldownTicks")) {
            bulldozeCooldownTicks = Math.max(0, tag.getInt("BulldozeCooldownTicks"));
        }
        if (tag.contains("Phase2Active")) {
            phase2Active = tag.getBoolean("Phase2Active");
            this.entityData.set(DATA_PHASE2, phase2Active);
        }
        if (tag.contains("Phase2CooldownTicks")) {
            phase2CooldownTicks = Math.max(0, tag.getInt("Phase2CooldownTicks"));
        }
        if (tag.contains("Leaping")) {
            leaping = tag.getBoolean("Leaping");
            this.entityData.set(DATA_LEAPING, leaping);
            leapWasVehicle = leaping && this.isVehicle();
        }
        if (tag.contains("LeapAnimState")) {
            leapAnimState = tag.getInt("LeapAnimState");
            this.entityData.set(DATA_LEAP_ANIM_STATE, leapAnimState);
        }
        if (tag.contains("LeapCooldownTicks")) {
            leapCooldownTicks = Math.max(0, tag.getInt("LeapCooldownTicks"));
        }
        if (tag.contains("FireBreathEnergy")) {
            setFireBreathEnergy(tag.getFloat("FireBreathEnergy"));
        } else {
            setFireBreathEnergy(1.0f);
        }
        if (tag.contains("FireBreathDepleted")) {
            setFireBreathDepleted(tag.getBoolean("FireBreathDepleted"));
        } else {
            setFireBreathDepleted(false);
        }
        // Treat initial load as "no prior rider" so we don't auto-clear these states before passengers are restored.
        bulldozeWasVehicle = false;
        phase2WasVehicle = false;
        tamingController.load(tag);
        applyConfiguredAttributes();
    }

    /**
     * Override to prevent Minecraft from repositioning flying dragons after world reload.
     * When this returns false, the entity keeps its loaded position, which is critical for
     * flying dragons with passengers - otherwise passengers get ejected during the repositioning.
     */
    @Override
    protected boolean repositionEntityAfterLoad() {
        // Flying dragons should NOT be repositioned - keep exact loaded position
        // This prevents passenger ejection when reloading while flying
        return !isFlying() && !isHovering();
    }

    @Override
    protected void applyLoadedFlightState(boolean flying, boolean takeoff, boolean hovering, boolean landing) {
        // Apply loaded flight state to entity data accessors
        if (!isBaby()) {
            setFlying(flying);
            setTakeoff(takeoff);
            setHovering(hovering);
            setLanding(landing);
        } else {
            setFlying(false);
            setTakeoff(false);
            setHovering(false);
            setLanding(false);
        }
    }

    // ===== TAMING DAMAGE HANDLING =====

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (this.isBaby()) {
            super.setTarget(null);
            return;
        }
        if ((isTamingStunned() || tamingAbortCalmTicks > 0) && target != null) {
            return;
        }
        super.setTarget(target);
    }

    private boolean shouldAggroOnSight() {
        if (isTame() || isBaby()) {
            return false;
        }
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        return config.extraBoolean("aggressive_wild", false);
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
        return com.leon.saintsdragons.server.world.DragonSpawnRules.hasDryGroundSpawnSpace(level, pos)
                && com.leon.saintsdragons.server.world.DragonSpawnRules.passesNearbyDragonDensityCheck(level, reason, pos, Ignivorus.class);
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

    @Override
    protected void dropAllDeathLoot(@NotNull DamageSource source) {
        // Don't drop loot until death animation completes
        if (deathTime < getDeathAnimationDurationTicks()) {
            return;
        }

        super.dropAllDeathLoot(source);

        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        double eggDropChance = config.extraDouble("egg_drop_chance", 0.12D);
        // Female dragons have a configurable chance to drop one egg on death
        if (!level().isClientSide && getGender() == DragonGender.FEMALE && this.random.nextDouble() < eggDropChance) {
            this.spawnAtLocation(ModItems.IGNIVORUS_EGG.get());
        }

        if (!level().isClientSide) {
            if (this.random.nextFloat() < 0.35F) {
                this.spawnAtLocation(ModItems.IGNIVORUS_TOOTH.get());
            }
            if (this.random.nextFloat() < 0.90F) {
                this.spawnAtLocation(ModItems.IGNIVORUS_HEART.get());
            }
        }
    }

    @Override
    public int getMaxHeadXRot() {
        return 180; // Allow full pitch range for head tracking
    }
}
