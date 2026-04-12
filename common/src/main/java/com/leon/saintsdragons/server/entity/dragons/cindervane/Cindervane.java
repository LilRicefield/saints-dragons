package com.leon.saintsdragons.server.entity.dragons.cindervane;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.cindervane.CindervaneAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonPackDefendPackGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonPackFollowLeaderGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonRandomHuntTargetGoal;
import com.leon.saintsdragons.server.ai.goals.cindervane.*;
import com.leon.saintsdragons.server.ai.navigation.DragonNavigationModeController;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncFlightController;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncFlightMoveControl;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncFlyingPathNavigation;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.controller.cindervane.CindervaneRiderController;
import com.leon.saintsdragons.server.flight.DragonBarrelRollHelper;
import com.leon.saintsdragons.server.flight.DragonGroundedAerialRecovery;
import com.leon.saintsdragons.server.flight.DragonFlightOrientationHelper;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import com.leon.saintsdragons.server.flight.DragonFlightVisuals;
import com.leon.saintsdragons.server.flight.DragonRiderFallRecovery;
import com.leon.saintsdragons.server.flight.DragonRiderFlight;
import com.leon.saintsdragons.server.flight.DragonTakeoff;
import com.leon.saintsdragons.server.entity.dragons.cindervane.handlers.CindervaneAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.cindervane.handlers.CindervaneInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.cindervane.handlers.CindervaneSoundProfile;
import com.leon.saintsdragons.server.entity.dragons.util.DragonGriefingRules;
import com.leon.saintsdragons.server.entity.component.ScreenShakeComponent;
import com.leon.saintsdragons.server.entity.util.ClientAnimationInitHelper;
import java.util.Map;
import java.util.HashMap;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.interfaces.PackMember;
import com.leon.saintsdragons.server.entity.interfaces.SoundHandledDragon;
import com.leon.saintsdragons.server.entity.interfaces.ShakesScreen;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
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
import java.util.UUID;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
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
import software.bernie.geckolib.util.GeckoLibUtil;
import javax.annotation.Nonnull;

public class Cindervane extends RideableDragonBase implements DragonFlightCapable, SoundHandledDragon, ShakesScreen, PackMember<Cindervane> {
    private static final float ALBINO_VARIANT_CHANCE = 0.50F;
    private static final int LANDING_SETTLE_TICKS = 4;
    private static final float AIR_AUTO_ALIGN_DECAY = 0.88f;
    private static final float LANDING_AUTO_ALIGN_STEP = 0.30f;
    private static final float BARREL_ROLL_INPUT_SPEED = 0.275f;
    private static final DragonBarrelRollHelper.Config BARREL_ROLL_CONFIG =
            new DragonBarrelRollHelper.Config(
                    AIR_AUTO_ALIGN_DECAY,
                    LANDING_AUTO_ALIGN_STEP,
                    0.04f,
                    0.005f,
                    Mth.HALF_PI
            );
    public static final int TAKEOFF_ANIMATION_TICKS = 24;
    private static final int GROUNDED_AERIAL_RECOVERY_TICKS = 8;
    private static final double FIRE_BODY_CRASH_MIN_DROP = 7.0D;
    private static final float FIRE_BODY_EXPLOSION_RADIUS = 15.0F;
    private static final double FIRE_BODY_IMPRINT_RADIUS = 9.0D;
    private static final double FIRE_BODY_IMPRINT_DEPTH_FACTOR = 0.6D;
    private static final float FIRE_BODY_EXPLOSION_DAMAGE = 200.0F;
    private static final float FIRE_BODY_SELF_DAMAGE_ON_CRASH = 40.0F;
    private static final double BREED_PARTNER_RANGE = 20.0D;
    private static final double BREED_DISTANCE_SQR = 2500.0D;
    private static final int MAX_PACK_SIZE = 6;
    private static final double PACK_SEARCH_RADIUS = 48.0D;

    private static final int MIN_AMBIENT_DELAY = 180;
    private static final int MAX_AMBIENT_DELAY = 420;

    private int ambientSoundTimer;
    private int nextAmbientSoundDelay;
    private int groundStepSoundCooldownTicks = 0;

    private boolean shouldSpawnBabies = false;
    private int babiesToSpawn = 0;

    private static final Map<String, VocalEntry> VOCAL_ENTRIES =
            new VocalEntryBuilder()
                    .add("grumble1", "actions", "animation.cindervane.grumble1", ModSounds.CINDERVANE_GRUMBLE_1, 1.1f, 0.98f, 0.06f, false, false, false)
                    .add("grumble2", "actions", "animation.cindervane.grumble2", ModSounds.CINDERVANE_GRUMBLE_2, 1.2f, 0.96f, 0.08f, false, false, false)
                    .add("grumble3", "actions", "animation.cindervane.grumble3", ModSounds.CINDERVANE_GRUMBLE_3, 1.0f, 1.0f, 0.05f, false, false, false)
                    .add("roar", "actions", "animation.cindervane.roar", ModSounds.CINDERVANE_ROAR, 1.5f, 0.95f, 0.1f, false, false, false)
                    .add("cindervane_hurt", "instant", "animation.cindervane.hurt", ModSounds.CINDERVANE_HURT, 1.2f, 0.95f, 0.1f, false, false, false)
                    .add("cindervane_die", "instant", "animation.cindervane.die", ModSounds.CINDERVANE_DIE, 1.5f, 1.0f, 0.0f, false, false, false)
                    .build();

    public AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);
    private final CindervaneAnimationHandler animationHandler = new CindervaneAnimationHandler(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final CindervaneInteractionHandler interactionHandler = new CindervaneInteractionHandler(this);
    private final CindervaneRiderController riderController;
    private final DragonRiderFlight riderFlightComponent;

    private final DragonPathNavigateGround groundNav;
    private final AsyncFlightController asyncAirController;
    private final AsyncFlightMoveControl asyncAirMoveControl;
    private final MoveControl groundMoveControl;
    private final FlyingPathNavigation airNav;
    private final DragonNavigationModeController navigationModeController;

    // ===== HARDCODED GROUND SPEEDS =====
    public static final double RIDER_WALK_SPEED = 0.18D;
    public static final double RIDER_RUN_SPEED = 0.26D;
    public static final float RIDER_KEY_PITCH_DEG = 25.0f;

    private int targetCooldown;
    private int airTicks;
    public int groundTicks;
    public int timeFlying = 0;
    private int landingTicks;
    private int riderTakeoffTicks;
    private int groundedAerialRecoveryTicks;
    private boolean wasVehicleLastTick;
    private int forceOwnerFollowTicks;
    private boolean fireBodyCrashArmed;
    private double fireBodyCrashMaxHeight;
    private boolean autoGrabPassengerMountAllowed;
    @Nullable
    private UUID slashGrabPassengerUuid;
    @Nullable
    private UUID packLeaderUuid;

    private final DragonFlightStateEvaluator.State flightModeState = new DragonFlightStateEvaluator.State();
    private final DragonFlightVisuals.State flightVisualState = new DragonFlightVisuals.State();
    private float prevSmoothedRoll = 0.0f;
    private float smoothedRoll = 0.0f;

    // Client-side animation initialization grace period (fixes T-pose on world rejoin with shaders)
    private int clientAnimInitTicks = 0;

    private final ScreenShakeComponent screenShakeComponent;
    private final DragonTakeoff takeoffComponent;

    private static final double MODEL_SCALE = 1.0D;

    public static final double RIDER_GLIDE_ALTITUDE_THRESHOLD = 40.0D;
    public static final double RIDER_GLIDE_ALTITUDE_EXIT = 30.0D;
    public static final double RIDER_LOW_ALTITUDE_GLIDE_THRESHOLD = 6.0D;
    public static final double RIDER_WATER_SURFACE_LEVEL = 62.0D;
    public static final double RIDER_WATER_SURFACE_TOLERANCE = 2.0D;
    public static final int RIDER_WATER_SCAN_RADIUS = 2;
    public static final int RIDER_WATER_SCAN_DEPTH = 8;

    // ===== RIDER LANDING BLEND SYSTEM =====
    public static final double LANDING_BLEND_ALTITUDE = 8.0D; // Trigger landing animation at this altitude
    private static final int RIDER_LANDING_BLEND_DURATION = 3; // ticks to keep landing blend active
    private int riderLandingBlendTicks = 0;

    // ===== SIT TRANSITION SYSTEM =====
    private int sitTransitionTicks = 0; // Counts down during down/up animations
    private boolean isSittingDown = false; // True during "down" animation (45 ticks)
    private boolean isStandingUp = false;  // True during "up" animation (46 ticks)

    // Controllable sucka
    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case TOGGLE_PITCH_MODE, ABILITY_USE, ABILITY_STOP -> true;
            default -> super.supportsRiderAction(action);
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return dragonCache;
    }


    @Override
    public float maxSitTicks() {
        return 33.0F; // Matches sit_down animation (1.6667s = ~33 ticks)
    }

    private int getSitDownAnimationTicks() {
        return 33; // 1.6667s = ~33 ticks
    }

    private int getSitUpAnimationTicks() {
        return 17; // 0.8333s = ~17 ticks
    }

    @Override
    protected int getSleepSitDownDuration() {
        return getSitDownAnimationTicks();
    }

    @Override
    protected int getSleepFallAsleepDuration() {
        return 33; // 1.6667s = ~33 ticks
    }

    @Override
    protected int getSleepWakeUpDuration() {
        return 33; // 1.6667s = ~33 ticks
    }

    @Override
    protected int getSleepSitUpDuration() {
        return getSitUpAnimationTicks();
    }

    // Feeding cooldown synced via DATA_FEEDING_COOLDOWN entity data accessor

    public boolean canFeed() {
        int cooldownTicks = this.entityData.get(DATA_FEEDING_COOLDOWN);
        return cooldownTicks <= 0;
    }

    public void setFeedingCooldown(int ticks) {
        this.entityData.set(DATA_FEEDING_COOLDOWN, ticks);
    }

    @Override
    protected int getMaxTextureVariant() {
        // 0 = default, 1 = albino
        return 1;
    }

    @Override
    public java.util.Map<String, Integer> getTextureVariantNameMap() {
        return java.util.Map.of(
                "default", 0,
                "albino", 1
        );
    }

    @Override
    protected int chooseAdultTextureVariant() {
        return this.getRandom().nextFloat() < ALBINO_VARIANT_CHANCE ? 1 : 0;
    }

    // ===== CLIENT LOCATOR CACHE (client-side only) =====
    private final Map<String, Vec3> clientLocatorCache = new java.util.concurrent.ConcurrentHashMap<>();
    // ===== SERVER BONE POSITION CACHE (synced from rider client for precision mounting) =====
    private final Map<String, Vec3> serverBonePositionCache = new java.util.concurrent.ConcurrentHashMap<>();

    // ===== Client animation overrides (for robust observer sync) =====

    public Cindervane(EntityType<? extends Cindervane> type, Level level) {
        super(type, level);
        this.screenShakeComponent = new ScreenShakeComponent(this, DATA_SCREEN_SHAKE_AMOUNT, 0.12F);
        this.setMaxUpStep(1.1F);

        this.asyncAirController = new AsyncFlightController(this);
        this.asyncAirMoveControl = new AsyncFlightMoveControl(this, this.asyncAirController);
        this.groundNav = new DragonPathNavigateGround(this, level);
        this.groundMoveControl = new MoveControl(this);
        this.airNav = new AsyncFlyingPathNavigation(this, level, this.asyncAirController) {
            @Override
            public boolean isStableDestination(@Nonnull BlockPos pos) {
                BlockState below = this.level.getBlockState(pos.below());
                return !below.isAir();
            }
        };
        this.airNav.setCanOpenDoors(false);
        this.airNav.setCanFloat(false);
        this.airNav.setCanPassDoors(false);

        this.navigationModeController = new DragonNavigationModeController(
                new DragonNavigationModeController.Host() {
                    @Override
                    public void setActiveNavigation(PathNavigation navigation) {
                        Cindervane.this.navigation = navigation;
                    }

                    @Override
                    public void setActiveMoveControl(MoveControl moveControl) {
                        Cindervane.this.moveControl = moveControl;
                    }

                    @Override
                    public void afterSwitchToGround() {
                        // Touchdown should hand authority back to ground cleanly.
                        // Damp leftover async-air momentum so the landed animation can settle.
                        if (Cindervane.this.onGround()) {
                            Cindervane.this.setDeltaMovement(Vec3.ZERO);
                            Cindervane.this.hasImpulse = false;
                        } else {
                            Vec3 motion = Cindervane.this.getDeltaMovement();
                            Cindervane.this.setDeltaMovement(motion.x * 0.25D, motion.y, motion.z * 0.25D);
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
        this.riderController = new CindervaneRiderController(this);
        this.takeoffComponent = createTakeoffComponent();
        this.riderFlightComponent = createRiderFlightComponent();

        this.setPathfindingMalus(BlockPathTypes.LEAVES, -1.0F);
        // Fire dragon behavior: path through fire like Ignivorus when chasing.
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);

        RandomSource rng = this.getRandom();
        this.ambientSoundTimer = rng.nextInt(80);
        this.nextAmbientSoundDelay = MIN_AMBIENT_DELAY + rng.nextInt(Math.max(1, MAX_AMBIENT_DELAY - MIN_AMBIENT_DELAY + 1));

        if (!level.isClientSide) {
            applyConfiguredAttributes();
        }
    }

    private DragonRiderFlight createRiderFlightComponent() {
        return new DragonRiderFlight(new DragonRiderFlight.Host() {
            @Override
            public Entity asEntity() { return Cindervane.this; }

            @Override
            public Level level() { return Cindervane.this.level(); }

            @Override
            public AABB getBoundingBox() { return Cindervane.this.getBoundingBox(); }

            @Override
            public boolean isVehicle() { return Cindervane.this.isVehicle(); }

            @Override
            public boolean isFlying() { return Cindervane.this.isFlying(); }

            @Override
            public boolean isTakeoff() { return Cindervane.this.isTakeoff(); }

            @Override
            public boolean isGoingUp() { return Cindervane.this.isGoingUp(); }

            @Override
            public boolean isUnderWater() { return Cindervane.this.isUnderWater(); }

            @Override
            public boolean isInWaterOrBubble() { return Cindervane.this.isInWaterOrBubble(); }

            @Override
            public boolean isTame() { return Cindervane.this.isTame(); }

            @Override
            public boolean hasControllingRider() { return riderController.getRidingPlayer() != null; }

            @Override
            public boolean canTakeoff() { return Cindervane.this.canTakeoff(); }

            @Override
            public void setFlying(boolean value) { Cindervane.this.setFlying(value); }

            @Override
            public void setHovering(boolean value) { Cindervane.this.setHovering(value); }

            @Override
            public void setLanding(boolean value) { Cindervane.this.setLanding(value); }

            @Override
            public void switchToAirNavigation() { Cindervane.this.switchToAirNavigation(); }

            @Override
            public void setGoingUp(boolean value) { Cindervane.this.setGoingUp(value); }

            @Override
            public void setGoingDown(boolean value) { Cindervane.this.setGoingDown(value); }

            @Override
            public void stopNavigation() { Cindervane.this.getNavigation().stop(); }

            @Override
            public void startTakeoffSequence(double minUpwardVelocity, int animationTicks) {
                Cindervane.this.startTakeoffSequence(minUpwardVelocity, animationTicks);
            }

            @Override
            public Vec3 getDeltaMovement() { return Cindervane.this.getDeltaMovement(); }

            @Override
            public void setDeltaMovement(Vec3 movement) { Cindervane.this.setDeltaMovement(movement); }

            @Override
            public void markImpulse() { Cindervane.this.hasImpulse = true; }

            @Override
            public long getGameTime() { return Cindervane.this.level().getGameTime(); }

            @Override
            public void setRiderTakeoffTicks(int ticks) { Cindervane.this.setRiderTakeoffTicks(ticks); }
        }, new DragonRiderFlight.Config(
                true,
                0,
                0.55D,
                TAKEOFF_ANIMATION_TICKS,
                0.45D,
                TAKEOFF_ANIMATION_TICKS
        ));
    }

    private DragonTakeoff createTakeoffComponent() {
        return new DragonTakeoff(new DragonTakeoff.Host() {
            @Override
            public Level level() { return Cindervane.this.level(); }

            @Override
            public boolean isFlying() { return Cindervane.this.isFlying(); }

            @Override
            public void setFlying(boolean value) { Cindervane.this.setFlying(value); }

            @Override
            public void setTakeoff(boolean value) { Cindervane.this.setTakeoff(value); }

            @Override
            public void setHovering(boolean value) { Cindervane.this.setHovering(value); }

            @Override
            public void setLanding(boolean value) { Cindervane.this.setLanding(value); }

            @Override
            public void switchToAirNavigation() { Cindervane.this.switchToAirNavigation(); }

            @Override
            public Vec3 getDeltaMovement() { return Cindervane.this.getDeltaMovement(); }

            @Override
            public void setDeltaMovement(Vec3 movement) { Cindervane.this.setDeltaMovement(movement); }

            @Override
            public void markImpulse() { Cindervane.this.hasImpulse = true; }

            @Override
            public void onTakeoffStarted() { Cindervane.this.timeFlying = 0; }
        });
    }

    public void startTakeoffSequence(double minUpwardVelocity, int animationTicks) {
        takeoffComponent.startTakeoff(animationTicks, minUpwardVelocity);
    }

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType spawnType,
                                                 @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnData, dataTag);

        if (spawnType == MobSpawnType.CHUNK_GENERATION || spawnType == MobSpawnType.NATURAL) {
            if (!(data instanceof CindervaneFamilyData)) {
                if (this.random.nextFloat() < 0.05F) {
                    data = new CindervaneFamilyData(false);
                    this.setGender(DragonGender.FEMALE);
                    this.shouldSpawnBabies = true;
                    this.babiesToSpawn = 1; // 1 baby
                }
            }
        }

        if (!this.isTame()) {
            this.setOwnerUUID(null);
            this.setCommand(2);
            this.setOrderedToSit(false);
        }

        applyConfiguredAttributes();
        this.setHealth(this.getMaxHealth());
        return data;
    }

    public void applyConfiguredAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.CINDERVANE_ID);

        if (this.isBaby()) {
            // Baby Cindervanes have reduced stats
            setAttributeBase(Attributes.MAX_HEALTH, 40.0);
            setAttributeBase(Attributes.ARMOR, 0.0);
            setAttributeBase(Attributes.FLYING_SPEED, config.flyingSpeed() * 0.7); // Slower flight as baby
        } else {
            // Adult attributes from config
            setAttributeBase(Attributes.MAX_HEALTH, config.maxHealth());
            setAttributeBase(Attributes.FLYING_SPEED, config.flyingSpeed());
            setAttributeBase(Attributes.ARMOR, config.armor());
        }
        // MOVEMENT_SPEED is hardcoded in createAttributes() - no config needed

        if (this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    private void setAttributeBase(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    // Ground speeds are now hardcoded constants (RIDER_WALK_SPEED, RIDER_RUN_SPEED)

    public static AttributeSupplier.Builder createAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.CINDERVANE_ID);
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, config.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, 0.45D) // Hardcoded AI pathfinding speed
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.FLYING_SPEED, config.flyingSpeed()) // Slower for glider behavior
                .add(Attributes.ARMOR, config.armor());
    }

    @Override
    public void ageBoundaryReached() {
        super.ageBoundaryReached();
        // Refresh attributes when baby grows into adult
        applyConfiguredAttributes();
        this.refreshDimensions();
    }

    public static boolean canSpawnHere(EntityType<? extends Cindervane> type,
                                       LevelAccessor level,
                                       MobSpawnType spawnType,
                                       BlockPos pos,
                                       RandomSource random) {
        if (!Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }
        return com.leon.saintsdragons.server.world.DragonSpawnRules.hasDryGroundSpawnSpace(level, pos)
                && com.leon.saintsdragons.server.world.DragonSpawnRules.passesNearbyDragonDensityCheck(level, spawnType, pos, Cindervane.class);
    }

    // Amphithere-specific entity data accessors
    private static final EntityDataAccessor<Boolean> DATA_FIRE_BREATHING =
            SynchedEntityData.defineId(Cindervane.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Float> DATA_SCREEN_SHAKE_AMOUNT =
            SynchedEntityData.defineId(Cindervane.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_RIDER_LANDING_BLEND =
            SynchedEntityData.defineId(Cindervane.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_FLIGHT_PITCH =
            SynchedEntityData.defineId(Cindervane.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ACCUMULATED_ROLL =
            SynchedEntityData.defineId(Cindervane.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_PITCH_KEY_MODE =
            SynchedEntityData.defineId(Cindervane.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SLASH_GRAB_PASSENGER_ID =
            SynchedEntityData.defineId(Cindervane.class, EntityDataSerializers.INT);

    // Sleep system entity data accessors

    /**
     * Entity data accessor for feeding cooldown ticks
     */
    private static final EntityDataAccessor<Integer> DATA_FEEDING_COOLDOWN =
            SynchedEntityData.defineId(Cindervane.class, EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        // Define Amphithere-specific data
        this.entityData.define(DATA_FIRE_BREATHING, false);
        this.entityData.define(DATA_SCREEN_SHAKE_AMOUNT, 0f);
        this.entityData.define(DATA_RIDER_LANDING_BLEND, false);
        this.entityData.define(DATA_FLIGHT_PITCH, 0f);
        this.entityData.define(DATA_ACCUMULATED_ROLL, 0f);
        this.entityData.define(DATA_PITCH_KEY_MODE, false);
        this.entityData.define(DATA_FEEDING_COOLDOWN, 0);
    }

    @Override
    protected void defineRideableDragonData() {
        // Define all rideable dragon data keys for AmphithereEntity
        this.entityData.define(DATA_SLASH_GRAB_PASSENGER_ID, -1);
    }

    @Override
    protected void applyLoadedFlightState(boolean flying, boolean takeoff, boolean hovering, boolean landing) {
        setFlying(flying);
        setTakeoff(takeoff);
        setHovering(hovering);
        setLanding(landing);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new com.leon.saintsdragons.server.ai.goals.base.DragonFloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));

        // Babies don't have flight or combat abilities
        if (!this.isBaby()) {
            this.goalSelector.addGoal(3, new CindervaneFlightGoal(this));
            this.goalSelector.addGoal(4, new CindervaneAirCombatGoal(this));
            this.goalSelector.addGoal(5, new CindervaneCombatGoal(this));
        }

        // Baby-specific: follow nearby adult dragons (wild babies only)
        this.goalSelector.addGoal(5, new com.leon.saintsdragons.server.ai.goals.base.DragonFollowParentGoal<>(this, Cindervane.class, 1.15D));

        this.goalSelector.addGoal(6, new com.leon.saintsdragons.server.ai.goals.base.DragonFollowOwnerGoal<>(this, com.leon.saintsdragons.server.ai.goals.base.DragonFollowOwnerGoal.FollowConfig.forCindervane()) {
            @Override
            protected void startFollowTakeoff() {
                if (Cindervane.this.isFlying() || Cindervane.this.isTakeoff()) {
                    return;
                }
                Cindervane.this.startTakeoffSequence(0.12D, TAKEOFF_ANIMATION_TICKS);
            }

            @Override
            protected boolean shouldForceFollow() {
                return Cindervane.this.forceOwnerFollowTicks > 0;
            }

            @Override
            protected void clearForceFollow() {
                Cindervane.this.forceOwnerFollowTicks = 0;
            }
        });
        this.goalSelector.addGoal(7, new DragonPackFollowLeaderGoal<>(this, Cindervane.class, 1.0D, 20.0D, 10.0D));
        this.goalSelector.addGoal(8, new com.leon.saintsdragons.server.ai.goals.base.DragonGroundWanderGoal<>(this, 0.6D, 160));
        // Idle water behavior: prefer swimming toward shore rather than sinking/hovering in place.
        this.goalSelector.addGoal(9, new com.leon.saintsdragons.server.ai.goals.base.DirectSwimWanderGoal(this, 8.0F, 0.12D, 1, true));

        // Adults can breed, babies cannot
        if (!this.isBaby()) {
            this.goalSelector.addGoal(10, new com.leon.saintsdragons.server.ai.goals.base.DragonBreedGoal<>(
                this, 1.0D, Cindervane.class, BREED_PARTNER_RANGE, BREED_DISTANCE_SQR
            ));
        }

        this.targetSelector.addGoal(1, new com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new com.leon.saintsdragons.server.ai.goals.base.DragonProtectBabiesGoal<>(this, Cindervane.class)); // Protect and stay with babies
        this.targetSelector.addGoal(4, new DragonPackDefendPackGoal<>(this, Cindervane.class, 36.0D));
        this.targetSelector.addGoal(5, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> shouldAggroOnSight()));
        this.targetSelector.addGoal(7, new DragonRandomHuntTargetGoal(
                this,
                80,
                this::shouldAggroOnSight,
                target -> target instanceof Chicken
        ));
        // Look goals that skip when being ridden (so rider has full control)
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !Cindervane.this.isVehicle() && super.canUse();
            }
        });
        this.goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 8.0F) {
            @Override
            public boolean canUse() {
                return !Cindervane.this.isVehicle() && super.canUse();
            }
        });
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            if (!this.isOrderedToSit() && getSitProgress() != 0f) {
                clearSitProgress();
            }
            // Only consider SOLID ground, not water (water should not trigger landing)
            boolean onGroundNow = this.onGround() && !this.isInWater();

            // Auto-complete landing once we're actually on ground (like Ignivorus)
            // This check is OUTSIDE isFlying() because FollowOwnerGoal sets flying=false before touchdown
            if (isLanding() && onGroundNow) {
                handleAiLandingComplete();
            }

            if (isFlying()) {
                airTicks++;
                groundTicks = 0;
                this.fallDistance = 0.0F;

                if (onGroundNow && !isTakeoff()) {
                    if (isLanding()) {
                        handleAiLandingComplete();
                    } else {
                        setLanding(false);
                    }
                    setFlying(false);
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

        this.setNoGravity(isFlying() || isTakeoff() || isHovering() || isLanding());
        if (!isFlying() && !isTakeoff() && !isLanding() && navigationModeController.isUsingAirNavigation()) {
            switchToGroundNavigation();
        }
    }

    public void tick() {
        // === CORE TICK (every tick) ===
        super.tick();
        tickRiderControlLock();
        tickBankingLogic();
        tickBarrelRollLogic();
        tickPitchingLogic();
        tickScreenShake();
        // === CLIENT-SIDE ONLY ===
        if (level().isClientSide) {
            // Grace period avoids shader-time race conditions that can cause brief T-poses on rejoin.
            clientAnimInitTicks = ClientAnimationInitHelper.tickClientCounter(true, clientAnimInitTicks);
            return; // Early exit for client - nothing else needed
        }

        // === SERVER-SIDE: EVERY TICK (lightweight or critical) ===
        takeoffComponent.tick();
        groundedAerialRecoveryTicks = DragonGroundedAerialRecovery.tick(
                level(),
                onGround(),
                isInWaterOrBubble(),
                isInLava(),
                isTakeoff(),
                isFlying(),
                isHovering(),
                isLanding(),
                false,
                getDeltaMovement(),
                groundedAerialRecoveryTicks,
                GROUNDED_AERIAL_RECOVERY_TICKS,
                0.05D,
                this::markLandedNow
        );
        tickSittingState();
        tickRiderTakeoff();
        tickMountedState();
        updateSittingProgress();
        spawnBabiesIfNeeded();
        if (isBreathingFire() || fireBodyCrashArmed) {
            handleFireBodyCrash();
        }

        // Ensure sit animation is cleared for riders even if packets arrive late
        if (isVehicle() && getSitProgress() != 0f) {
            clearSitProgress();
        }

        if (targetCooldown > 0) {
            targetCooldown--;
        }

        tickOwnerFollowRecovery();

        if (this.navigationModeController.isUsingAirNavigation()
                && (this.isFlying() || this.isTakeoff() || this.isLanding())
                && !this.isVehicle()
                && !isDirectAirCombatActive()) {
            this.asyncAirController.serverTick();
        }

        // Update timeFlying counter
        if (isFlying()) {
            timeFlying++;
        } else {
            timeFlying = 0;
        }

        // Initialize animation state on first tick after loading to prevent thrashing
        if (this.tickCount == 1) {
            initializeAnimationState();
        }

        // === SERVER-SIDE: EVERY TICK (precise timing needed) ===
        tickFeedingCooldown();

        // === SERVER-SIDE: EVERY 5 TICKS (timers/cooldowns/state machines - no precision needed) ===
        if (tickCount % 5 == 0) {
            handleAmbientSounds();
        }
        tickGroundStepAudio();

        // === SERVER-SIDE: SLEEP WAKE-UP LOGIC ===
        // Wake up if sleeping and conditions changed
        if (isSleeping() || isSleepingEntering() || isSleepingExiting()) {
            if (this.getTarget() != null || this.isAggressive()) {
                wakeUpImmediately();
                suppressSleep(200);
            } else if (this.isInWaterOrBubble() || this.isInLava()) {
                wakeUpImmediately();
                suppressSleep(200);
            }
        }

        tickClientSideUpdates();
    }

    private boolean isDirectAirCombatActive() {
        LivingEntity target = this.getTarget();
        return !this.isLanding()
                && this.isAggressive()
                && target != null
                && this.isTargetValid(target);
    }

    private void tickSittingState() {
        // Clear sitting state if the dragon is being ridden
        if (!this.level().isClientSide && this.isVehicle() && this.isOrderedToSit()) {
            this.setOrderedToSit(false);
        }
    }

    private void tickMountedState() {
        boolean mounted = this.isVehicle();

        if (!mounted && forceOwnerFollowTicks > 0) {
            forceOwnerFollowTicks--;
        }

        if (mounted && !wasVehicleLastTick) {
            clearSitProgress();
            clearStatesWhenMounted();

            if (this.isOrderedToSit()) {
                this.setOrderedToSit(false);
                if (this.getCommand() == 1) {
                    this.setCommand(0);
                }
            }

            // Clear sleep states when mounted
            if (isSleeping() || isSleepingEntering() || isSleepingExiting()) {
                wakeUpImmediately();
                suppressSleep(300); // ~15 seconds
            }
        }

        if (!mounted && wasVehicleLastTick) {
            this.setBreathingFire(false);
            combatManager.forceEndActiveAbility();
            clearSitProgress();
            this.entityData.set(DATA_GROUND_MOVE_STATE, 0);
            this.entityData.set(DATA_FLIGHT_MODE, -1);
            this.entityData.set(DATA_RIDER_FORWARD, 0f);
            this.entityData.set(DATA_RIDER_STRAFE, 0f);
            this.syncAnimState(0, -1);
            if (this.isTame() && this.getOwner() != null && this.getCommand() == 0) {
                this.forceOwnerFollowTicks = 40;
            }
        }

        wasVehicleLastTick = mounted;
    }

    private void tickOwnerFollowRecovery() {
        if (!this.isTame() || this.isOrderedToSit() || this.getCommand() != 0) {
            return;
        }

        LivingEntity owner = this.getOwner();
        if (owner == null || !owner.isAlive() || owner.level() != this.level()) {
            return;
        }

        if (this.isVehicle() || this.isPassenger() || this.getControllingPassenger() != null) {
            return;
        }

        if (this.isFlying()) {
            return;
        }

        double distSq = this.distanceToSqr(owner);
        if (distSq < (18.0D * 18.0D)) {
            if (!this.getNavigation().isInProgress()) {
                this.getNavigation().moveTo(owner, 0.8D);
            }
            return;
        }

        boolean moveGoalActive = this.goalSelector.getRunningGoals().anyMatch(wrapped -> {
            Goal goal = wrapped.getGoal();
            return goal instanceof com.leon.saintsdragons.server.ai.goals.base.DragonFollowOwnerGoal
                    || goal instanceof CindervaneCombatGoal
                    || goal instanceof CindervaneFlightGoal;
        });
        if (moveGoalActive) {
            return;
        }

        switchToGroundNavigation();
        boolean shouldRun = distSq > (25.0D * 25.0D);
        setRunning(shouldRun);
        setGroundMoveStateFromAI(shouldRun ? 2 : 1);
        double speed = shouldRun ? 1.15D : 0.8D;
        if (!this.getNavigation().moveTo(owner, speed)) {
            this.getNavigation().stop();
            attemptOwnerTeleport(owner);
        }
    }

    private void attemptOwnerTeleport(LivingEntity owner) {
        BlockPos ownerPos = owner.blockPosition();
        for (int i = 0; i < 8; i++) {
            int dx = this.random.nextInt(7) - 3;
            int dz = this.random.nextInt(7) - 3;
            BlockPos candidate = ownerPos.offset(dx, 0, dz);
            if (isTeleportFriendlyBlock(candidate)) {
                this.teleportTo(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
                this.getNavigation().stop();
                return;
            }
        }
    }

    private boolean isTeleportFriendlyBlock(BlockPos pos) {
        BlockPos below = pos.below();
        BlockState floor = level().getBlockState(below);
        BlockState body = level().getBlockState(pos);
        BlockState above = level().getBlockState(pos.above());
        return floor.isSolidRender(level(), below) && body.isAir() && above.isAir();
    }

    private void spawnBabiesIfNeeded() {
        if (!shouldSpawnBabies || babiesToSpawn <= 0) {
            return;
        }

        shouldSpawnBabies = false;

        if (!(level() instanceof ServerLevel serverLevel)) {
            babiesToSpawn = 0;
            return;
        }

        int spawnCount = babiesToSpawn;
        babiesToSpawn = 0;

        serverLevel.getServer().execute(() -> {
            if (this.isRemoved()) {
                return;
            }

            for (int i = 0; i < spawnCount; i++) {
                Cindervane baby = ModEntities.CINDERVANE.get().create(serverLevel);
                if (baby != null) {
                    baby.setGender(this.random.nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE);
                    assignMotherToBaby(baby, null);

                    baby.skipRespawnTicks = 5;
                    baby.setBaby(true);
                    baby.setAge(-24000);
                    baby.applyConfiguredAttributes();
                    baby.setHealth(baby.getMaxHealth());

                    double angle = (Math.PI * 2.0 * i) / spawnCount;
                    double distance = 1.0 + this.random.nextDouble() * 0.5;
                    double offsetX = Math.cos(angle) * distance;
                    double offsetZ = Math.sin(angle) * distance;

                    baby.moveTo(
                            this.getX() + offsetX,
                            this.getY(),
                            this.getZ() + offsetZ,
                            this.random.nextFloat() * 360.0F,
                            0.0F
                    );

                    serverLevel.addFreshEntity(baby);
                }
            }
        });
    }

    /**
     * Custom SpawnGroupData to track family spawning and prevent recursive baby spawning.
     */
    private static class CindervaneFamilyData extends AgeableMob.AgeableMobGroupData {
        public CindervaneFamilyData(boolean shouldSpawnBaby) {
            super(shouldSpawnBaby);
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
                isSittingDown = false;
                isStandingUp = false;
            }
        }

        float sitProgress = getSitProgress();
        if (this.isOrderedToSit()) {
            // Trigger sit down animation when:
            // 1. Starting from standing (sitProgress == 0), OR
            // 2. Interrupting a stand-up animation (isStandingUp = true)
            if ((sitProgress == 0f || isStandingUp) && !isSittingDown) {
                animationHandler.triggerSitDownAnimation();
                isSittingDown = true;
                isStandingUp = false; // Cancel the stand-up
                sitTransitionTicks = getSitDownAnimationTicks();
            }

            if (sitProgress < maxSitTicks()) {
                sitProgress++;
                setSitProgress(sitProgress);
            }
        } else {
            // NOT ordered to sit - standing up sequence
            if (isVehicle()) {
                if (sitProgress != 0f) {
                    clearSitProgress();
                }
            } else if (sitProgress > 0f) {
                // Trigger sit up animation when:
                // 1. At or near max sitting and ready to stand, OR
                // 2. Interrupting a sit-down animation (isSittingDown = true)
                if ((sitProgress >= maxSitTicks() - 1 || isSittingDown) && !isStandingUp) {
                    animationHandler.triggerSitUpAnimation();
                    isStandingUp = true;
                    isSittingDown = false; // Cancel the sit-down
                    sitTransitionTicks = getSitUpAnimationTicks();
                }

                // Decrement sitProgress to match stand-up animation duration
                float decrementRate = maxSitTicks() / (float) getSitUpAnimationTicks();
                sitProgress -= decrementRate;
                if (sitProgress < 0f) sitProgress = 0f;
                setSitProgress(sitProgress);
            }
        }
    }

    private void tickClientSideUpdates() {
        // Client-side sit progress is synced centrally.
    }

    private void handleAmbientSounds() {
        if (nextAmbientSoundDelay <= 0) {
            resetAmbientSoundTimer();
        }

        // Suppress ambient sounds during transitions to prevent animation snapping
        if (isBaby() || isDying() || isSleeping() || isSleepTransitioning() || isInSitTransition() || getSleepAmbientCooldownTicks() > 0 || areRiderControlsLocked()) {
            return;
        }

        if (getActiveAbility() != null || isBreathingFire() || this.getTarget() != null) {
            return;
        }

        if (isVehicle() || isTakeoff() || isLanding() || isHovering()) {
            return;
        }

        ambientSoundTimer++;
        if (ambientSoundTimer < nextAmbientSoundDelay) {
            return;
        }

        String vocal = selectAmbientGrumble();
        if (vocal != null) {
            this.getSoundHandler().playVocal(vocal);
        }
        resetAmbientSoundTimer();
    }

    private String selectAmbientGrumble() {
        RandomSource random = getRandom();
        float roll = random.nextFloat();

        if (roll < 0.45f) {
            return "grumble1";
        }
        if (roll < 0.8f) {
            return "grumble2";
        }
        return "grumble3";
    }

    private void resetAmbientSoundTimer() {
        ambientSoundTimer = 0;
        RandomSource random = getRandom();
        int range = Math.max(1, MAX_AMBIENT_DELAY - MIN_AMBIENT_DELAY + 1);
        nextAmbientSoundDelay = MIN_AMBIENT_DELAY + random.nextInt(range);
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
        if (moveState != 2) {
            double speedSqr = this.getDeltaMovement().horizontalDistanceSqr();
            if (speedSqr > 0.02D) {
                moveState = 2;
            }
        }
        if (moveState != 2) {
            groundStepSoundCooldownTicks = 0;
            return;
        }
        if (groundStepSoundCooldownTicks > 0) {
            return;
        }
        getSoundHandler().playMovingEntitySound(ModSounds.CINDERVANE_RUN.get(), 1.0f, 1.0f, 22);
        groundStepSoundCooldownTicks = 30;
    }

    public void playEatMovingSound() {
        if (level().isClientSide) {
            return;
        }
        float pitch = isBaby() ? 1.6f : 1.0f;
        getSoundHandler().playMovingEntitySound(ModSounds.CINDERVANE_EAT.get(), 1.0f, pitch, 33);
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

        if (!isFlying() && navigationModeController.isUsingAirNavigation()) {
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
        }
    }

    private void tickBankingLogic() {
        // Apply banking to all flying Cindervanes (ridden and wild)
        boolean shouldBank = isFlying() && !isLanding() && !isHovering()
                && (!isOrderedToSit() || riderOverridesSittingCommand());
        DragonFlightVisuals.tickBanking(
                this.flightVisualState,
                shouldBank,
                this.horizontalCollision,
                this.verticalCollision,
                this.getYRot(),
                this.yRotO
        );
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
                    triggerAnim("actions", "landed");
                    getSoundHandler().playMovingEntitySound(ModSounds.CINDERVANE_LANDED.get(), 1.0f, 1.0f, 59);
                    lockRiderControls(34);  // Lock controls for 1.67 seconds while animation plays
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

    // Animation initialization system (fixes T-pose on world rejoin with shaders)
    public boolean isClientAnimationReady() {
        return ClientAnimationInitHelper.isReady(clientAnimInitTicks);
    }

    private double getAltitudeAboveTerrain() {
        return getAltitudeAboveCollisionTerrain(24, true);
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
        if (inWater || areRiderControlsLocked() || ((!isFlying() && !isLanding()) || isHovering()) || (isOrderedToSit() && !riderOverridesSittingCommand())) {
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
                    // Hovering (no WASD)  pitch = 0, even if ascending/descending with Spacebar/L-Alt
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
            if (isLanding()) {
                float landingPitchRad = (float) -Math.toRadians(18.0f);
                targetPitchRad = Math.min(targetPitchRad, landingPitchRad);
            }
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
        boolean inWater = isInWaterOrBubble();
        boolean barrelRollAllowed = barrelRollEnabled && !inWater;
        if (barrelRollAllowed && isVehicle() && getControllingPassenger() != null) {
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
                        onGround() || inWater,
                        isLanding(),
                        barrelRollAllowed && isActivelyBarrelRolling(),
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

    private void tickScreenShake() {
        screenShakeComponent.tick();
    }

    /**
     * Interpolated bank angle for smooth client-side rendering.
     */
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

    public int getTimeFlying() {
        return timeFlying;
    }

    // ===== Rider Control Methods =====
    @Override
    public boolean isGoingUp() {
        return this.entityData.get(DATA_GOING_UP);
    }

    // ===== Animation State Methods =====

    @Override
    public boolean isRunning() {
        return !isFlying() && getEffectiveGroundState() == 2;
    }

    @Override
    public void setRunning(boolean running) {
        // MOVEMENT_SPEED is fixed for AI - rider speed is handled by RiderController
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

    public DragonFlightStateEvaluator.VisualState getVisualFlightState(float partialTick) {
        return DragonFlightStateEvaluator.evaluateAnimationVisualState(
                getSyncedFlightMode(),
                isVehicle(),
                getFlightPitchRadians(partialTick),
                getDeltaMovement(),
                isLanding(),
                getAltitudeAboveTerrain(),
                LANDING_BLEND_ALTITUDE,
                isRiderLandingBlendActive()
        );
    }

    private boolean shouldForceSurfaceGlide(double altitudeAboveTerrain) {
        return altitudeAboveTerrain <= RIDER_LOW_ALTITUDE_GLIDE_THRESHOLD || isNearWaterSurface();
    }

    private boolean isNearWaterSurface() {
        if (level() == null) {
            return false;
        }

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
                    if (!level().hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockState state = level().getBlockState(cursor);
                    if (!state.getFluidState().isEmpty()) {
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

        if (this.isFlying() || canRecover) {
            setGoingUp(goingUp);
            setGoingDown(goingDown);
        } else {
            setGoingUp(false);
            setGoingDown(false);
        }
    }

    @Override
    protected void applyRiderMovementInput(Player player, float forward, float strafe, float yaw, boolean locked) {
        float fwd = locked ? 0f : applyInputDeadzone(forward);
        float str = locked ? 0f : applyInputDeadzone(strafe);
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
    protected void onRiderTakeoffRequest(Player player) {
        if (canRecoverTakeoffFromFall()) {
            setGoingUp(true);
            setGoingDown(false);
            startTakeoffSequence(0.11D, TAKEOFF_ANIMATION_TICKS);
            return;
        }
        requestRiderTakeoff();
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
                false,
                this.fallDistance,
                getDeltaMovement()
        );
    }

    @Override
    protected void onRiderAbilityUse(Player player, String abilityName) {
        if (abilityName != null && !abilityName.isEmpty()) {
            useRidingAbility(abilityName);
        }
    }

    @Override
    protected void onRiderAbilityStop(Player player, String abilityName) {
        if (abilityName != null && !abilityName.isEmpty()) {
            forceEndActiveAbility();
        }
    }

    @Override
    protected boolean handleCustomRiderAction(ServerPlayer player, DragonRiderAction action,
                                              String abilityName, boolean locked) {
        if (locked) {
            return false;
        }

        return switch (action) {
            case TOGGLE_PITCH_MODE -> {
                setRiderPitchKeyMode(!isRiderPitchKeyMode());
                yield true;
            }
            default -> false;
        };
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
        String abilityId = getMeleeMode() == 0
                ? CindervaneAbilities.BITE_ID
                : CindervaneAbilities.SLASH_GRAB_ID;
        return new RiderAbilityBinding(abilityId, RiderAbilityBinding.Activation.PRESS);
    }


    // ===== Riding System Methods =====

    protected int getMaxPassengers() {
        // Two-seater: Seat 0 (driver/owner) + Seat 1 (passenger)
        return 2;
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity passenger) {
        if (passenger instanceof Player) {
            return this.getPassengers().size() < getMaxPassengers();
        }
        if (autoGrabPassengerMountAllowed && passenger instanceof LivingEntity) {
            return this.getPassengers().size() < getMaxPassengers();
        }
        return false;
    }

    public void setAutoGrabPassengerMountAllowed(boolean allowed) {
        this.autoGrabPassengerMountAllowed = allowed;
    }

    public void setSlashGrabPassenger(@Nullable Entity passenger) {
        UUID newUuid = passenger == null ? null : passenger.getUUID();
        int newId = passenger == null ? -1 : passenger.getId();
        if (java.util.Objects.equals(this.slashGrabPassengerUuid, newUuid)
                && this.entityData.get(DATA_SLASH_GRAB_PASSENGER_ID) == newId) {
            return;
        }
        this.slashGrabPassengerUuid = newUuid;
        this.entityData.set(DATA_SLASH_GRAB_PASSENGER_ID, newId);
    }

    public boolean isSlashGrabPassenger(Entity passenger) {
        if (passenger == null) {
            return false;
        }
        int syncedId = this.entityData.get(DATA_SLASH_GRAB_PASSENGER_ID);
        if (syncedId >= 0 && passenger.getId() == syncedId) {
            return true;
        }
        return slashGrabPassengerUuid != null && slashGrabPassengerUuid.equals(passenger.getUUID());
    }

    public void requestRiderTakeoff() {
        riderFlightComponent.requestRiderTakeoff();
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
    public void removePassenger(@NotNull Entity passenger) {
        if (isSlashGrabPassenger(passenger)) {
            setSlashGrabPassenger(null);
        }
        super.removePassenger(passenger);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (this.isBaby()) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    private boolean shouldAggroOnSight() {
        if (isTame() || isBaby()) {
            return false;
        }
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.CINDERVANE_ID);
        return config.extraBoolean("aggressive_wild", false);
    }

    @Override
    protected float getRiddenSpeed(@Nonnull Player rider) {
        return riderController.getRiddenSpeed(rider);
    }

    @Override
    protected boolean isRiderInputLocked(Player player) {
        return areRiderControlsLocked();
    }

    @Override
    protected void tickRidden(@Nonnull Player player, @Nonnull Vec3 travelVector) {
        super.tickRidden(player, travelVector);

        if (!areRiderControlsLocked()) {
            // Normal riding: use rider controller for rotation
            riderController.tickRidden(player, travelVector);
        } else {
            // Controls locked (e.g., during landed animation): use yaw-only rider rotation
            // Only when not using any ability to preserve ability-specific pitch animation
            if (combatManager.getActiveAbility() == null && !combatManager.hasActiveOverlay()) {
                player.fallDistance = 0.0F;
                this.fallDistance = 0.0F;
                this.setTarget(null);
                copyRiderYaw(player);

                // Stop acceleration during lock
                this.setAccelerating(false);
                if (!this.isFlying()) {
                    this.setGoingUp(false);
                    this.setGoingDown(false);
                }
            }
        }
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@Nonnull Player player, @Nonnull Vec3 deltaIn) {
        Vec3 input = riderController.getRiddenInput(player, deltaIn);

        // Capture rider inputs for animation state (like Lightning Dragon)
        if (!level().isClientSide && !isFlying()) {
            float fwd = (float) Mth.clamp(input.z, -1.0, 1.0);
            float str = (float) Mth.clamp(input.x, -1.0, 1.0);
            // Apply simple threshold to filter noise
            this.entityData.set(DATA_RIDER_FORWARD, Math.abs(fwd) > 0.02f ? fwd : 0f);
            this.entityData.set(DATA_RIDER_STRAFE, Math.abs(str) > 0.02f ? str : 0f);
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
                setSitProgress(getSitProgress());
            }
        }
        // Don't clear sitProgress when standing - let updateSittingProgress() handle the "up" animation transition
    }

    @Override
    public void travel(@NotNull Vec3 motion) {
        // Block ALL movement when controls are locked (e.g., during landed animation)
        if (areRiderControlsLocked()) {
            super.travel(Vec3.ZERO);
            return;
        }

        boolean inWater = this.isInWater() || this.isInWaterOrBubble();

        if (inWater && !level().isClientSide) {
            if (riderFlightComponent.shouldClearFlightStateInWater(this.riderTakeoffTicks)) {
                this.setFlying(false);
                this.setTakeoff(false);
                this.setHovering(false);
                this.setLanding(false);
                this.switchToGroundNavigation();
            }
        }

        // Riding logic
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            // Clear any AI navigation when being ridden
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }

            if (inWater) {
                handleWaterSwimming(motion);
            } else if (isFlying()) {
                // Delegate flying movement to rider controller for consistency
                this.riderController.handleRiderMovement(player, motion);
            } else {
                // Ground movement - use vanilla system which calls getRiddenInput()
                this.setSpeed(riderController.getRiddenSpeed(player));
                super.travel(motion);
            }
            return;
        }

        // Normal AI movement
        super.travel(motion);
    }

    /**
     * Handles rider-controlled swimming when the Cindervane is submerged.
     * Mimics Raevyx behaviour: slow horizontal movement, gradual sinking, manual ascend/descend.
     */
    private void handleWaterSwimming(Vec3 input) {
        Vec3 velocity = this.getDeltaMovement();

        double swimSpeed = 0.4D;
        if (isAccelerating()) {
            swimSpeed *= 1.3D;
        }

        Vec3 desired = getSwimVec3(input, swimSpeed, velocity);
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

        riderFlightComponent.tryAutoBreachTakeoff();
    }

    private Vec3 getSwimVec3(Vec3 wishDir, double swimSpeed, Vec3 velocity) {
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

    public void switchToAirNavigation() {
        this.navigationModeController.switchToAir();
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
        boolean allowGriefing = DragonGriefingRules.canDestroyBlocks(server);
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

        Explosion.BlockInteraction blockInteraction = allowGriefing
                ? Explosion.BlockInteraction.DESTROY
                : Explosion.BlockInteraction.KEEP;
        Explosion explosion = new Explosion(server, this, server.damageSources().explosion(this, this), calculator,
                x, y + 0.2D, z, FIRE_BODY_EXPLOSION_RADIUS, true, blockInteraction);

        List<LivingEntity> allies = grantAlliesExplosionImmunity(server, x, y, z);
        double protectionRadius = FIRE_BODY_EXPLOSION_RADIUS + 4.0D;
        AABB protectionArea = new AABB(
                x - protectionRadius, y - protectionRadius, z - protectionRadius,
                x + protectionRadius, y + protectionRadius, z + protectionRadius
        );
        List<LivingEntity> protectedEntities = server.getEntitiesOfClass(
                LivingEntity.class,
                protectionArea,
                entity -> entity.isAlive() && entity != this
        );
        Map<Integer, Boolean> previousInvulnerability = new HashMap<>();
        for (LivingEntity entity : protectedEntities) {
            previousInvulnerability.put(entity.getId(), entity.isInvulnerable());
        }

        // Prevent vanilla Explosion from applying entity damage.
        // Custom configured blast damage is applied manually after the explosion resolves.
        for (LivingEntity entity : protectedEntities) {
            entity.setInvulnerable(true);
        }

        explosion.explode();
        explosion.finalizeExplosion(true);

        // Restore prior invulnerability state.
        for (LivingEntity entity : protectedEntities) {
            Boolean wasInvulnerable = previousInvulnerability.get(entity.getId());
            entity.setInvulnerable(wasInvulnerable != null && wasInvulnerable);
        }

        applyFireBodyBlastDamage(server, x, y, z, immune);
        applyFireBodyCrashSelfDamage(server);

        if (allowGriefing) {
            carveFireBodyImprint(server, BlockPos.containing(x, y, z));
        }

        server.sendParticles(ParticleTypes.FLAME, x, y + 0.8D, z, 150, 2.0D, 1.0D, 2.0D, 0.2D);
        server.sendParticles(ParticleTypes.SMALL_FLAME, x, y + 0.5D, z, 120, 1.8D, 0.8D, 1.8D, 0.15D);
        server.sendParticles(ParticleTypes.LAVA, x, y + 0.5D, z, 40, 1.3D, 0.6D, 1.3D, 0.12D);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.5D, z, 80, 2.2D, 0.7D, 2.2D, 0.05D);

        if (allowGriefing) {
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
        double configuredDamage = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.CINDERVANE_ID)
                .extraDouble("fire_body_explosion_damage", FIRE_BODY_EXPLOSION_DAMAGE);
        float blastDamage = (float) Math.max(0.0D, configuredDamage);

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
            if (target.hurt(server.damageSources().explosion(this, this), blastDamage)) {
                target.setSecondsOnFire(8);
            }
        }
    }

    private void applyFireBodyCrashSelfDamage(ServerLevel server) {
        double configuredSelfDamage = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.CINDERVANE_ID)
                .extraDouble("fire_body_self_damage_on_crash", FIRE_BODY_SELF_DAMAGE_ON_CRASH);
        float selfDamage = (float) Math.max(0.0D, configuredSelfDamage);
        if (selfDamage <= 0.0F) {
            return;
        }
        this.hurt(server.damageSources().explosion(this, this), selfDamage);
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
        this.navigationModeController.switchToGround();
    }

    // Fire immunity (vanilla environmental fire only) is handled in hurt() method above
    // Magical fire is intentionally NOT blocked to allow compatibility with fire magic mods

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
        movement.setSoundKeyframeHandler(event -> {
            String soundKey = event.getKeyframeData().getSound();
            if (soundKey != null && !soundKey.isEmpty()) {
                handleAnimationSound(soundKey);
            }
        });
        controllers.add(movement);

        AnimationController<Cindervane> actions = new AnimationController<>(this, "actions", 5, animationHandler::actionPredicate);
        animationHandler.setupActionController(actions);
        actions.setSoundKeyframeHandler(event -> {
            String soundKey = event.getKeyframeData().getSound();
            if (soundKey != null && !soundKey.isEmpty()) {
                handleAnimationSound(soundKey);
            }
        });
        controllers.add(actions);

        AnimationController<Cindervane> instantController = new AnimationController<>(this, "instant", 1,
                animationHandler::instantActionPredicate);
        animationHandler.setupInstantActionController(instantController);
        instantController.setSoundKeyframeHandler(event -> {
            String soundKey = event.getKeyframeData().getSound();
            if (soundKey != null && !soundKey.isEmpty()) {
                handleAnimationSound(soundKey);
            }
        });
        controllers.add(instantController);
    }

    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
    }


    @Override
    public Map<String, VocalEntry> getVocalEntries() {
        return VOCAL_ENTRIES;
    }

    @Override
    public DragonSoundProfile getSoundProfile() {
        return CindervaneSoundProfile.INSTANCE;
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
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        return getMeleeMode() == 0
                ? CindervaneAbilities.BITE
                : CindervaneAbilities.SLASH_GRAB;
    }

    @Override
    public boolean hasSecondaryMelee() {
        return true;
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
        return 50;
    }

    // Death handling now uses base class helpers

    @Override
    public boolean hurt(@Nonnull DamageSource source, float amount) {
        // During dying sequence, ignore all damage (entity is already dead, playing death animation)
        if (isDying()) {
            return false;
        }

        // Immune to vanilla environmental fire only (mundane fire can't harm fire dragons)
        // Magical fire from mods (e.g., Iron's Spells) is NOT blocked - magic fire is different!
        if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.LAVA) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR)) {
            if (this.isOnFire() || this.getRemainingFireTicks() > 0) {
                this.clearFire();
                this.setRemainingFireTicks(0);
            }
            return false;
        }

        // Immune to fall damage (flying dragon)
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
            return false;
        }

        // Wake if sleeping and suppress re-entry on damage
        if (isSleeping() || isSleepingEntering() || isSleepingExiting()) {
            wakeUpImmediately();
            suppressSleep(200);
        }

        return super.hurt(source, amount);
    }

    /**
     * Returns a larger bounding box for frustum culling to prevent the model from
     * disappearing when the entity's collision box is off-screen but the visual model
     * (wings, tail, etc.) should still be visible.
     */
    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(6.0, 3.0, 6.0);
    }

    @Override
    protected DragonAbilityType<?, ?> getDeathAbilityType() {
        return CindervaneAbilities.DIE;
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
        if (type == CindervaneAbilities.BITE
                || type == CindervaneAbilities.SLASH_GRAB
                || type == CindervaneAbilities.FIRE_BODY
                || type == CindervaneAbilities.ROAR
                || type == CindervaneAbilities.FIRE_BREATH_VOLLEY) {
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
        // Use currently selected melee ability.
        if (!this.isVehicle() && !this.isOrderedToSit()) {
            combatManager.tryUseAbility(getPrimaryAttackAbility());
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
        Vec3 locator = getClientLocatorPosition("mouth_origin");
        if (locator != null) {
            return locator;
        }
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
        return stack.is(Items.COD) ||
                stack.is(Items.SALMON) ||
                stack.is(Items.CHICKEN) ||
                stack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
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

    @Override
    protected void dropAllDeathLoot(@NotNull DamageSource source) {
        // Don't drop loot until death animation completes
        if (deathTime < getDeathAnimationDurationTicks()) {
            return;
        }

        super.dropAllDeathLoot(source);

        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.CINDERVANE_ID);
        double eggDropChance = config.extraDouble("egg_drop_chance", 0.12D);
        // Female dragons have a configurable chance to drop one egg on death
        if (!level().isClientSide && getGender() == DragonGender.FEMALE && this.random.nextDouble() < eggDropChance) {
            this.spawnAtLocation(ModItems.CINDERVANE_EGG.get());
        }
    }


    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@Nonnull ServerLevel level, @Nonnull AgeableMob partner) {
        Cindervane baby = ModEntities.CINDERVANE.get().create(level);
        if (baby != null) {
            baby.setGender(this.random.nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE);
            assignMotherToBaby(baby, partner);
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

            // Position the baby near the parent to prevent Y=0 spawning
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
        tag.putInt("TimeFlying", timeFlying);
        saveRideableData(tag);
        tag.putBoolean("RiderPitchKeyMode", isRiderPitchKeyMode());

        // Persist feeding cooldown (synced via entity data but saved for redundancy)
        tag.putInt("FeedingCooldownTicks", Math.max(0, this.entityData.get(DATA_FEEDING_COOLDOWN)));

        if (shouldSpawnBabies) {
            tag.putBoolean("FamilySpawnPending", true);
            tag.putInt("FamilySpawnCount", babiesToSpawn);
        }
        if (this.packLeaderUuid != null) {
            tag.putUUID("PackLeaderUuid", this.packLeaderUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        loadRideableData(tag);
        boolean savedFlying = tag.getBoolean("Flying");
        this.timeFlying = tag.getInt("TimeFlying");
        if (tag.contains("RiderPitchKeyMode")) {
            setRiderPitchKeyMode(tag.getBoolean("RiderPitchKeyMode"));
        }

        if (tag.contains("FamilySpawnPending")) {
            this.shouldSpawnBabies = tag.getBoolean("FamilySpawnPending");
            this.babiesToSpawn = tag.getInt("FamilySpawnCount");
        }
        this.packLeaderUuid = tag.hasUUID("PackLeaderUuid") ? tag.getUUID("PackLeaderUuid") : null;
        if (this.isTame()) {
            this.packLeaderUuid = null;
        }
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

        // Restore feeding cooldown (synced via entity data but loaded for redundancy)
        if (tag.contains("FeedingCooldownTicks")) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, tag.getInt("FeedingCooldownTicks")));
        }

        // Force animation state sync after loading to prevent thrashing
        if (!level().isClientSide) {
            // Delay the sync slightly to ensure all systems are initialized
            this.tickCount = 0; // Reset tick counter to ensure proper initialization
        }


        // Apply config attributes when loading from NBT (Forge fix)
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

    public void prepareForMounting() {
        if (level().isClientSide) {
            return;
        }

        this.setOrderedToSit(false);
        if (this.getCommand() == 1) {
            this.setCommand(0);
        }
            clearSitProgress();
            this.setTarget(null);

        if (this.getNavigation().getPath() != null) {
            this.getNavigation().stop();
        }

        clearStatesWhenMounted();
    }

    @Override
    public @Nullable UUID getPackLeaderUuid() {
        return this.packLeaderUuid;
    }

    @Override
    public void setPackLeaderUuid(@Nullable UUID leaderUuid) {
        this.packLeaderUuid = leaderUuid;
    }

    @Override
    public boolean canParticipateInPack() {
        if (this.isTame()) {
            return false;
        }
        if (this.isBaby() || this.isDying()) {
            return false;
        }
        if (!this.isAlive() || this.isRemoved()) {
            return false;
        }
        return !this.isOrderedToSit() && this.getCommand() != 1;
    }

    @Override
    public boolean canLeadPack() {
        return canParticipateInPack() && !this.isFemale();
    }

    @Override
    public int getPackLeadershipPriority() {
        return (int) Math.round((this.getHealth() / Math.max(1.0F, this.getMaxHealth())) * 100.0F);
    }

    @Override
    public int getMaxPackSize() {
        return MAX_PACK_SIZE;
    }

    @Override
    public double getPackSearchRadius() {
        return PACK_SEARCH_RADIUS;
    }

    @Override
    public int getPackLeaderRefreshIntervalTicks() {
        return 60;
    }

    // ===== DragonFlightCapable =====

    @Override
    protected boolean isDragonFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    @Override
    public void setFlying(boolean flying) {
        // Never let autonomous/AI paths force flight start while submerged.
        if (flying && !this.isVehicle() && (this.isInWater() || this.isInWaterOrBubble() || this.isInLava())) {
            return;
        }
        if (flying == isFlying()) {
            return;
        }
        if (flying && !isTakeoff() && this.onGround()) {
            startTakeoffSequence(0.12D, TAKEOFF_ANIMATION_TICKS);
            return;
        }
        this.entityData.set(DATA_FLYING, flying);
        if (flying) {
            switchToAirNavigation();
            setLanding(false);
            this.getNavigation().stop();
        } else {
            takeoffComponent.clear();
            if (!isLanding()) {
                switchToGroundNavigation();
                setHovering(false);
            }
        }
    }

    @Override
    public boolean isTakeoff() {
        return this.entityData.get(DATA_TAKEOFF);
    }

    @Override
    public void setTakeoff(boolean takeoff) {
        boolean wasTakeoff = isTakeoff();
        this.entityData.set(DATA_TAKEOFF, takeoff);
        if (!takeoff && takeoffComponent.isActive()) {
            takeoffComponent.clear();
            return;
        }
        if (takeoff && !wasTakeoff && !level().isClientSide) {
            triggerAnim("instant", "takeoff");
            getSoundHandler().playMovingEntitySound(ModSounds.CINDERVANE_TAKEOFF.get(), 1.2f, 1.0f, 55);
        }
    }

    private boolean riderOverridesSittingCommand() {
        return this.isVehicle() && this.getControllingPassenger() instanceof Player;
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
        return (float) this.getAttributeValue(Attributes.FLYING_SPEED);
    }

    @Override
    public double getPreferredFlightAltitude() {
        double base = 24.0D + this.getRandom().nextDouble() * 10.0D;
        return Mth.clamp(base, 20.0D, 45.0D);
    }

    @Override
    public boolean canTakeoff() {
        if (this.isBaby() || !this.isAlive()) {
            return false;
        }
        if (this.isInWaterOrBubble() || this.isInLava()) {
            return false;
        }
        if (!this.onGround()) {
            return false;
        }
        boolean riderOverride = this.isVehicle() && this.getControllingPassenger() instanceof Player;
        if (!riderOverride && this.isOrderedToSit()) {
            return false;
        }
        return true;
    }

    @Override
    public void markLandedNow() {
        setFlying(false);
        setTakeoff(false);
        setLanding(false);
        setHovering(false);
        takeoffComponent.clear();
        this.riderTakeoffTicks = 0;
        this.timeFlying = 0;
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
            triggerAnim("actions", "landed");
            getSoundHandler().playMovingEntitySound(ModSounds.CINDERVANE_LANDED.get(), 1.0f, 1.0f, 59);
            suppressSleep(60);
        }
        markLandedNow();
    }

    public int getRiderTakeoffTicks() {
        return riderTakeoffTicks;
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
        return !isDying() && !isAccelerating();
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

    public void setServerBonePosition(String boneName, Vec3 position) {
        if (boneName == null || position == null) return;
        this.serverBonePositionCache.put(boneName, position);
    }

    /**
     * Get a client-side locator position (used by rider controller to position passengers)
     */
    @Override
    public Vec3 getClientLocatorPosition(String name) {
        if (name == null) return null;
        return this.clientLocatorCache.get(name);
    }

    public Vec3 getBonePositionForPassenger(String boneName) {
        if (boneName == null) {
            return null;
        }
        if (this.level().isClientSide) {
            return this.clientLocatorCache.get(boneName);
        }
        return this.serverBonePositionCache.get(boneName);
    }

    @Override
    public float getScreenShakeAmount(float partialTicks) {
        return screenShakeComponent.getAmount(partialTicks);
    }

    @Override
    public double getShakeDistance() {
        return 18.0;
    }

    @Override
    public boolean canFeelShake(Entity player) {
        // Allow screen shake regardless of whether player is on ground
        // This is important for dragon riding scenarios
        return true;
    }

    public void triggerScreenShake(float intensity) {
        screenShakeComponent.trigger(intensity);
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
    protected int getSleepExitSuppressionTicks() {
        return 20;
    }

    @Override
    protected int getSleepWakeUpSuppressionTicks() {
        return 20;
    }

    @Override
    protected void onSleepLockCommand(int snapshot) {
        if (getCommand() != 1) {
            setCommand(1);
            setOrderedToSit(true);
        }
    }

    @Override
    protected void onSleepUnlockCommand(int desired) {
        if (desired >= 0 && desired != getCommand()) {
            setCommand(desired);
            setOrderedToSit(desired == 1);
        }
    }

    @Override
    protected void onSleepFreezeTick() {
        this.getNavigation().stop();
        this.setDeltaMovement(0, 0, 0);
        this.setRunning(false);
        // Sleep entry only happens once the dragon is already grounded.
        // Do not force-clear flight states here; that bypasses landing logic
        // and can create vertical drop behavior with async air movement.
        setGroundMoveStateFromAI(0);
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
    }

    @Override
    protected void onSleepWakeUpAnimation() {
        animationHandler.triggerWakeUpAnimation();
    }

    @Override
    protected void onSleepSitUpAnimation() {
        animationHandler.triggerSitUpAnimation();
        setOrderedToSit(false);
    }

    @Override
    protected void onSleepExitSeated() {
        setOrderedToSit(true);
        setSitProgress(Math.max(getSitProgress(), maxSitTicks()));
    }

    private void tickFeedingCooldown() {
        int cooldownTicks = this.entityData.get(DATA_FEEDING_COOLDOWN);
        if (cooldownTicks > 0) {
            cooldownTicks--;
            this.entityData.set(DATA_FEEDING_COOLDOWN, cooldownTicks);
        }
    }

    @Override
    public DragonEntity.DragonSleepPreferences getSleepPreferences() {
        // Cindervane are nocturnal sleepers (sleep at night, active during day)
        return DragonEntity.DragonSleepPreferences.NOCTURNAL();
    }

    @Override
    public boolean canSleepNow() {
        return !isBreathingFire() && !isVehicle() && getActiveAbility() == null;
    }

    // ===== EGG BREEDING SYSTEM =====

    @Override
    public BlockState getEggBlockState() {
        return com.leon.saintsdragons.common.registry.ModBlocks.CINDERVANE_EGG.get().defaultBlockState();
    }

    @Override
    public void configureEggBlockEntity(BlockEntity blockEntity, @Nullable DragonEntity partner) {
        if (!(blockEntity instanceof com.leon.saintsdragons.common.block.CindervaneEggBlockEntity eggEntity)) {
            return;
        }

        java.util.UUID ownerUUID = resolveEggOwnerUUID(partner);
        if (ownerUUID != null) {
            eggEntity.setOwnerUUID(ownerUUID);
        }

        DragonGender babyGender =
            this.getRandom().nextBoolean() ?
            DragonGender.FEMALE :
            DragonGender.MALE;
        eggEntity.setBabyGender(babyGender);
    }
}
