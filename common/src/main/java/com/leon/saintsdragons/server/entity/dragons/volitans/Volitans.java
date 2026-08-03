package com.leon.saintsdragons.server.entity.dragons.volitans;

import com.leon.saintsdragons.server.ai.navigation.GenericSwimSteeringController;
import com.mojang.serialization.Dynamic;
import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;

import com.leon.saintsdragons.common.block.VolitansEggBlock;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.registry.ModBlocks;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.DragonAirCombatSettings;
import com.leon.saintsdragons.server.ai.DragonAirCombatSettingsProvider;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrain;
import com.leon.saintsdragons.server.ai.dragonbrain.profiles.VolitansBrain;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansBurrowAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansPoisonBallAbility;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.base.DragonVariant;
import com.leon.saintsdragons.server.entity.base.DragonVariantSet;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.component.DragonBreathComponent;
import com.leon.saintsdragons.server.entity.component.DragonMotionMath;
import com.leon.saintsdragons.server.entity.component.DragonForwardMovementComponent;
import com.leon.saintsdragons.server.entity.controller.DragonRiderControllerHelper;
import com.leon.saintsdragons.server.flight.DragonFlightVisuals;
import com.leon.saintsdragons.server.flight.DragonRiderFlight;
import com.leon.saintsdragons.server.entity.controller.volitans.VolitansRiderController;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansSpineEntity;
import com.leon.saintsdragons.server.entity.dragons.volitans.handlers.VolitansAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.volitans.handlers.VolitansInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.volitans.handlers.VolitansSoundProfile;
import com.leon.saintsdragons.server.entity.dragons.volitans.handlers.VolitansTamingHandler;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import com.leon.saintsdragons.server.entity.component.ScreenShakeComponent;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.interfaces.DragonMovementCapability;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.interfaces.PassiveTreeDestroyer;
import com.leon.saintsdragons.server.entity.interfaces.ShakesScreen;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import com.leon.saintsdragons.server.entity.interfaces.ScentAssessingDragon;
import com.leon.saintsdragons.server.loot.DragonLootTables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Volitans extends RideableFlyingDragon implements SemiAquaticDragon, ShakesScreen, DragonAirCombatSettingsProvider, PassiveTreeDestroyer, ScentAssessingDragon {
    private static final VolitansBrain DRAGON_BRAIN = new VolitansBrain();
    @Override
    public EnumSet<DragonMovementCapability> movementCapabilities() {
        return EnumSet.of(
                DragonMovementCapability.WALK,
                DragonMovementCapability.FLY,
                DragonMovementCapability.SWIM
        );
    }

    @Override
    protected ResourceLocation getDragonAttributesId() {
        return DragonAttributeConfigLoader.VOLITANS_ID;
    }

    private static final double BABY_MAX_HEALTH = 60.0D;
    private static final double BABY_ARMOR = 0.0D;
    private static final float BABY_HITBOX_SCALE = 0.55F;
    private static final int SLEEP_AFTER_SPAWN_GRACE_TICKS = 600;
    private static final int SCENT_ASSESSMENT_ANIMATION_TICKS = 90;

    @Override
    public int getScentAssessmentDurationTicks() {
        return SCENT_ASSESSMENT_ANIMATION_TICKS;
    }

    private static final int WATER_SURFACE_SLEEP_SCAN_BLOCKS = 16;
    private static final double MIN_UNDERWATER_SLEEP_SURFACE_CLEARANCE = 10D;
    public static final int VARIANT_DEFAULT = 0;
    public static final int VARIANT_BLOODSHOT = 1;
    private static final DragonVariantSet VARIANTS = DragonVariantSet.of(
            DragonVariant.of(VARIANT_DEFAULT, "default", 85),
            DragonVariant.of(VARIANT_BLOODSHOT, "bloodshot", 15)
    );
    private static final EntityDataAccessor<Float> DATA_FLIGHT_PITCH =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ACCUMULATED_ROLL =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_PITCH_KEY_MODE =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_SCREEN_SHAKE_AMOUNT =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_ULTIMATE_SLAM_ACTIVE =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BREATH_MODE =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BREATHING =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_WATER_BREATH_ENERGY =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_POISON_BREATH_ENERGY =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_WATER_BREATH_DEPLETED =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_POISON_BREATH_DEPLETED =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BURROWING =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_FEEDING_COOLDOWN =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VENOM_NEUTRALIZED_TICKS =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DATA_TAMING_STUNNED =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_RIDER_NUDGE_TICKS =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RIDER_NUDGE_X =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIDER_NUDGE_Z =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIDER_NUDGE_DRAG =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_RIDER_NUDGE_MODE =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RIDER_NUDGE_STEER_OFFSET =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.FLOAT);

    private static final double RIDER_WALK_SPEED = 0.24D;
    private static final double RIDER_RUN_SPEED = 0.34D;
    private static final double RIDER_BURROW_SPEED = 0.40D;
    private static final double RIDER_SWIM_SPEED = 1.42D;
    public static final int TAKEOFF_ANIMATION_TICKS = 31;
    public static final DragonAirCombatSettings AI_AIR_COMBAT_SETTINGS =
            new DragonAirCombatSettings(
                    TAKEOFF_ANIMATION_TICKS,
                    1.6D,
                    0,
                    48.0D,
                    8.0D,
                    8.0D,
                    5.0D
            );
    public static final int TAKEOFF_LAUNCH_DELAY_TICKS = 15;
    private static final int SLEEP_EXIT_SUPPRESSION_TICKS = 20;
    private static final int SLEEP_WAKE_SUPPRESSION_TICKS = 20;
    private static final int LANDED_CONTROL_LOCK_TICKS = 20;
    private static final int LANDED_RECOVERY_TICKS = 23;
    private static final int EAT_SOUND_DURATION_TICKS = 34;
    private static final int MIN_AMBIENT_DELAY = 220;
    private static final int MAX_AMBIENT_DELAY = 420;
    private static final int RIDER_BACK_DASH_COOLDOWN_TICKS = 30;
    private static final int RIDER_DASH_SOUND_TICKS = 60; // 3.0s
    private static final int RIDER_DODGE_SOUND_TICKS = 60; // 3.0s
    private static final int FLEX_CONTROL_LOCK_TICKS = 65;
    private static final int FLEX_COOLDOWN_TICKS = 65;
    private static final int RIDER_BACK_DASH_LOCK_TICKS = 0;
    private static final int RIDER_BACK_DASH_DURATION_TICKS = 8;
    private static final double RIDER_BACK_DASH_DISTANCE_BLOCKS = 12.0D;
    private static final double RIDER_BACK_DASH_HORIZONTAL_DRAG = 0.90D;
    private static final double RIDER_BACK_DASH_VERTICAL_DRAG = 0.95D;
    private static final int RIDER_BACK_DASH_RECOVERY_TICKS = 5;
    private static final double RIDER_BACK_DASH_RECOVERY_DRAG = 0.82D;
    private static final int RIDER_BACK_DASH_SPIKE_COUNT = 3;
    private static final float RIDER_BACK_DASH_SPIKE_DAMAGE = 5.0F;
    private static final int RIDER_BACK_DASH_SPIKE_POISON_DURATION_TICKS = 100;
    private static final int RIDER_BACK_DASH_SPIKE_POISON_AMPLIFIER = 0;
    private static final float RIDER_BACK_DASH_SPIKE_SPEED = 2.9F;
    private static final float RIDER_BACK_DASH_SPIKE_INACCURACY = 0.10F;
    private static final int RIDER_BACK_DASH_SPIKE_DELAY_TICKS = 8;
    private static final double RIDER_BACK_DASH_SPIKE_Y_OFFSET = -3.0D;
    private static final float REACTIVE_HIT_EVADE_CHANCE = 0.35F;
    private static final int RIDER_FORWARD_DASH_DURATION_TICKS = 25; // 1.25s
    private static final double RIDER_FORWARD_DASH_DISTANCE_BLOCKS = 26.0D;
    private static final double RIDER_FORWARD_DASH_HORIZONTAL_DRAG = 0.90D;
    private static final int RIDER_FORWARD_DASH_DAMAGE_TICK = 14; // late hit near animation end
    private static final float RIDER_FORWARD_DASH_DAMAGE = 16.0F;
    private static final double RIDER_FORWARD_DASH_DAMAGE_RADIUS = 8.0D;
    private static final int RIDER_SIDE_DODGE_DURATION_TICKS = 7;
    private static final double RIDER_SIDE_DODGE_VERTICAL_DRAG = 0.95D;
    private static final double RIDER_SIDE_DODGE_DISTANCE_BLOCKS = 7.0D;
    private static final int RIDER_SIDE_DODGE_RECOVERY_TICKS = 5;
    private static final double RIDER_SIDE_DODGE_RECOVERY_DRAG = 0.82D;
    private static final int RIDER_NUDGE_NONE = 0;
    private static final int RIDER_NUDGE_FORWARD_DASH = 1;
    private static final int RIDER_NUDGE_BACK_DASH = 2;
    private static final int RIDER_NUDGE_SIDE_DODGE = 3;
    private static final float BREATH_DEPLETED_THRESHOLD = 0.01F;
    private static final float BREATH_REARM_THRESHOLD = 0.20F;
    private static final int SPINE_DROP_COOLDOWN_TICKS = 30;
    public static final double LANDING_BLEND_ALTITUDE = RideableFlyingDragon.LANDING_BLEND_ALTITUDE;
    public static final double BREED_PARTNER_RANGE = 20.0D;
    public static final double BREED_DISTANCE_SQR = 16.0D;
    private static final float BURROW_MOVE_SHAKE_INTENSITY = 0.12F;
    private static final int BURROW_EXIT_TAKEOFF_BLOCK_BUFFER_TICKS = 8;
    private static final Map<String, VocalEntry> VOCAL_ENTRIES = new VocalEntryBuilder()
            .add("grumble1", AnimationHelper.VOCAL_CONTROLLER, "animation.volitans.grumble1", ModSounds.VOLITANS_GRUMBLE_1, 1.0f, 0.98f, 0.08f, false, false, false)
            .add("grumble2", AnimationHelper.VOCAL_CONTROLLER, "animation.volitans.grumble2", ModSounds.VOLITANS_GRUMBLE_2, 1.0f, 0.98f, 0.08f, false, false, false)
            .add("grumble3", AnimationHelper.VOCAL_CONTROLLER, "animation.volitans.grumble3", ModSounds.VOLITANS_GRUMBLE_3, 1.0f, 1.0f, 0.06f, false, false, false)
            .build();

    private final AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);
    private final VolitansAnimationHandler animationHandler = new VolitansAnimationHandler(this);
    private final VolitansInteractionHandler interactionHandler = new VolitansInteractionHandler(this);
    private final VolitansTamingHandler tamingController = new VolitansTamingHandler(this);
    private final VolitansRiderController riderController;
    private final AnimationController<Volitans> movementController;
    private final AnimationController<Volitans> actionController;
    private final AnimationController<Volitans> fastActionController;
    private final AnimationController<Volitans> flightController;
    private final AnimationController<Volitans> airActionController;
    private final AnimationController<Volitans> vocalController;
    private final AnimationController<Volitans> interactionController;
    private final Map<String, Vec3> serverBonePositionCache = new ConcurrentHashMap<>();
    private int timeFlying;
    private int spineDropCooldownTicks;
    private int ticksInWater;
    private int ticksOutOfWater;
    private final DragonFlightVisuals.State flightVisualState = new DragonFlightVisuals.State();
    private float flightPitchRad = 0f;
    private float prevFlightPitchRad = 0f;
    private float smoothedPlayerPitchRad = 0f;
    private boolean verticalKeyPitchSmoothing = false;
    private final ScreenShakeComponent screenShakeComponent;
    private int riderBackDashCooldownTicks = 0;
    private boolean riderForwardDashDamageApplied = false;
    private final DragonForwardMovementComponent riderGroundNudge = new DragonForwardMovementComponent(
            this,
            new DragonForwardMovementComponent.StateAccess() {
                @Override
                public void start(int ticks, Vec3 velocity, boolean dashing, boolean dodging, double horizontalDrag) {
                    setRiderNudgeState(ticks, velocity, horizontalDrag);
                    entityData.set(DATA_RIDER_NUDGE_MODE, dashing ? RIDER_NUDGE_FORWARD_DASH : dodging ? RIDER_NUDGE_SIDE_DODGE : RIDER_NUDGE_NONE);
                }

                @Override
                public int ticks() {
                    return entityData.get(DATA_RIDER_NUDGE_TICKS);
                }

                @Override
                public void setTicks(int ticks) {
                    entityData.set(DATA_RIDER_NUDGE_TICKS, Math.max(0, ticks));
                }

                @Override
                public Vec3 velocity() {
                    return getRiderNudgeVelocity();
                }

                @Override
                public void setVelocity(Vec3 velocity) {
                    setRiderNudgeVelocity(velocity);
                }

                @Override
                public double horizontalDrag() {
                    return entityData.get(DATA_RIDER_NUDGE_DRAG);
                }

                @Override
                public void clear() {
                    clearRiderNudgeState();
                }
            }
    );
    private Vec3 riderBackDashVec = Vec3.ZERO;
    private int riderBackDashRecoveryTicks = 0;
    private int riderBackDashSpikeDelayTicks = 0;
    private Vec3 riderSideDodgeVec = Vec3.ZERO;
    private int riderSideDodgeRecoveryTicks = 0;
    private int takeoffInputBlockTicks = 0;
    private int aiGroundMobilityCooldownTicks = 0;
    private boolean aiSpecialCombatActive = false;
    private boolean aiSpecialCombatReserved = false;
    private int tempInvulnTicks = 0;
    private float sleepLockedYaw = 0.0F;
    private float sleepLockedPitch = 0.0F;
    private final GenericSwimSteeringController swimSteering;
    private final AsyncSwimController asyncSwimController;

    public Volitans(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.screenShakeComponent = new ScreenShakeComponent(this, DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        this.setMaxUpStep(1.0F);
        this.riderController = new VolitansRiderController(this);
        this.swimSteering = new GenericSwimSteeringController(this);
        this.asyncSwimController = new AsyncSwimController(this, this.swimSteering);
        this.movementController = new AnimationController<>(this, "movement",
                VolitansAnimationHandler.MOVEMENT_TRIGGER_TRANSITION_TICKS, animationHandler::movementPredicate);
        this.actionController = new AnimationController<>(this, VolitansAnimationHandler.ACTION_CONTROLLER, 4, animationHandler::actionPredicate);
        this.fastActionController = new AnimationController<>(this, VolitansAnimationHandler.FAST_ACTION_CONTROLLER, 1, animationHandler::fastActionPredicate);
        this.flightController = AnimationHelper.createFlightController(this, getFlightAnimationTransitionTicks(), animationHandler::flightPredicate);
        this.airActionController = new AnimationController<>(this, VolitansAnimationHandler.AIR_ACTION_CONTROLLER, 1, animationHandler::airActionPredicate);
        this.vocalController = new AnimationController<>(this, AnimationHelper.VOCAL_CONTROLLER, 2, AnimationHelper::vocalIdle);
        this.interactionController = new AnimationController<>(this, AnimationHelper.INTERACTION_CONTROLLER, 1, AnimationHelper::interactionIdle);
        setupAnimationControllers();

        if (!level.isClientSide) {
            applyConfiguredAttributes();
        }
        resetAmbientSoundTimer(MIN_AMBIENT_DELAY, MAX_AMBIENT_DELAY);
    }

    @Override
    public AsyncSwimController getAiSwimController() {
        return this.asyncSwimController;
    }

    @Override
    protected DragonRiderFlight.Config getRiderFlightConfig() {
        return new DragonRiderFlight.Config(
                true,
                0,
                0.12D,
                TAKEOFF_ANIMATION_TICKS,
                0.12D,
                TAKEOFF_ANIMATION_TICKS
        );
    }

    @Override
    public void startTakeoffSequence(double minUpwardVelocity, int animationTicks) {
        if (!canStartTakeoffSequence()) {
            return;
        }
        this.setRunning(false);
        this.setAccelerating(false);
        this.setDeltaMovement(Vec3.ZERO);
        if (!level().isClientSide) {
            triggerAnim(AnimationHelper.FLIGHT_CONTROLLER, AnimationHelper.TAKEOFF);
            if (isVehicle() && !isFlying() && TAKEOFF_LAUNCH_DELAY_TICKS > 0) {
                lockRiderControls(TAKEOFF_LAUNCH_DELAY_TICKS);
            }
        }
        super.startTakeoffSequence(minUpwardVelocity, animationTicks);
    }

    @Override
    public void startRiderWaterBreachTakeoffSequence(double minUpwardVelocity, int animationTicks) {
        startTakeoffSequence(minUpwardVelocity, animationTicks);
    }

    @Override
    protected void onTakeoffStarted() {
        this.timeFlying = 0;
    }

    @Override
    protected int getTakeoffLiftDelayTicks() {
        return TAKEOFF_LAUNCH_DELAY_TICKS;
    }

    public static AttributeSupplier.Builder createAttributes() {
        var config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.VOLITANS_ID);
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, config.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FLYING_SPEED, config.flyingSpeed())
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ARMOR, config.armor());
    }

    @Override
    protected float getBabyHitboxScale() {
        return BABY_HITBOX_SCALE;
    }

    @Override
    public void ageBoundaryReached() {
        super.ageBoundaryReached();
        applyConfiguredAttributes();
        refreshDimensions();
    }

    public static boolean canSpawnHere(EntityType<? extends Volitans> type,
                                       LevelAccessor level,
                                       MobSpawnType spawnType,
                                       BlockPos pos,
                                       RandomSource random) {
        if (!TamableAnimal.checkMobSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }

        FluidState fluidAt = level.getFluidState(pos);
        FluidState fluidBelow = level.getFluidState(pos.below());
        if (!fluidAt.isEmpty() || !fluidBelow.isEmpty()) {
            return false;
        }

        BlockState below = level.getBlockState(pos.below());
        return below.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(
            @NotNull ServerLevelAccessor level,
            @NotNull DifficultyInstance difficulty,
            @NotNull MobSpawnType spawnReason,
            @Nullable SpawnGroupData spawnData,
            @Nullable CompoundTag dataTag
    ) {
        spawnData = super.finalizeSpawn(level, difficulty, spawnReason, spawnData, dataTag);
        applyConfiguredAttributes();
        this.setHealth(this.getMaxHealth());
        return spawnData;
    }


    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case ABILITY_USE, ABILITY_STOP, DOUBLE_TAP_W, DOUBLE_TAP_A, DOUBLE_TAP_D, DOUBLE_TAP_S -> true;
            default -> super.supportsRiderAction(action);
        };
    }

    @Override
    protected RiderFlexSpec getRiderFlexSpec() {
        return new RiderFlexSpec(FLEX_CONTROL_LOCK_TICKS, FLEX_COOLDOWN_TICKS);
    }

    @Override
    protected boolean canRiderFlex(ServerPlayer player, RiderFlexSpec spec) {
        return super.canRiderFlex(player, spec)
                && !isBaby()
                && !isDying()
                && onGround()
                && !isFlying()
                && !isTakeoff()
                && !isLanding()
                && !isHovering()
                && !isSwimming()
                && !isInWaterOrBubble()
                && !isOrderedToSit()
                && !isInSitTransition()
                && !isSleeping()
                && !isSleepTransitioning()
                && !isBurrowing()
                && !isUltimateSlamActive()
                && getActiveAbility() == null;
    }

    @Override
    protected void playRiderFlex(ServerPlayer player, RiderFlexSpec spec) {
        triggerAnim(VolitansAnimationHandler.MOVEMENT_CONTROLLER, "roar");
        getSoundHandler().playClientSound(this, position(), ModSounds.VOLITANS_ROAR.get(), 1.6f, 1.0f);
    }

    @Override
    protected void applyRiderVerticalInput(Player player, boolean goingUp, boolean goingDown, boolean locked) {
        if (shouldSuppressTakeoffInput()) {
            setGoingUp(false);
            if (!isFlying()) {
                setGoingDown(false);
            }
            return;
        }
        if (locked && isUltimateSlamActive()) {
            return;
        }
        super.applyRiderVerticalInput(player, goingUp, goingDown, locked);
    }

    @Override
    protected void defineRideableDragonData() {
        this.entityData.define(DATA_FLIGHT_PITCH, 0.0F);
        this.entityData.define(DATA_ACCUMULATED_ROLL, 0.0F);
        this.entityData.define(DATA_PITCH_KEY_MODE, false);
        this.entityData.define(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        this.entityData.define(DATA_ULTIMATE_SLAM_ACTIVE, false);
        this.entityData.define(DATA_BREATH_MODE, 0); // 0=water, 1=poison
        this.entityData.define(DATA_BREATHING, false);
        this.entityData.define(DATA_WATER_BREATH_ENERGY, 1.0F);
        this.entityData.define(DATA_POISON_BREATH_ENERGY, 1.0F);
        this.entityData.define(DATA_WATER_BREATH_DEPLETED, false);
        this.entityData.define(DATA_POISON_BREATH_DEPLETED, false);
        this.entityData.define(DATA_BURROWING, false);
        this.entityData.define(DATA_FEEDING_COOLDOWN, 0);
        this.entityData.define(DATA_VENOM_NEUTRALIZED_TICKS, 0);
        this.entityData.define(DATA_TAMING_STUNNED, false);
        this.entityData.define(DATA_RIDER_NUDGE_TICKS, 0);
        this.entityData.define(DATA_RIDER_NUDGE_X, 0.0F);
        this.entityData.define(DATA_RIDER_NUDGE_Z, 0.0F);
        this.entityData.define(DATA_RIDER_NUDGE_DRAG, 1.0F);
        this.entityData.define(DATA_RIDER_NUDGE_MODE, RIDER_NUDGE_NONE);
        this.entityData.define(DATA_RIDER_NUDGE_STEER_OFFSET, 0.0F);
    }

    @Override
    protected void applyLoadedFlightState(boolean flying, boolean takeoff, boolean hovering, boolean landing) {
        if (!isBaby()) {
            setFlying(flying);
            setTakeoff(takeoff);
            setHovering(hovering);
            setLanding(landing);
        } else {
            clearAerialStateForInterrupt();
        }
    }

    @Override
    protected int getFlightMode() {
        return evaluateStandardFlightMode(false);
    }

    @Override
    protected EntityDataAccessor<Boolean> getFlyingDataAccessor() {
        return DATA_FLYING;
    }

    @Override
    protected EntityDataAccessor<Boolean> getTakeoffDataAccessor() {
        return DATA_TAKEOFF;
    }

    @Override
    protected EntityDataAccessor<Boolean> getHoveringDataAccessor() {
        return DATA_HOVERING;
    }

    @Override
    protected EntityDataAccessor<Boolean> getLandingDataAccessor() {
        return DATA_LANDING;
    }

    @Override
    protected boolean shouldUpdateRiderGroundMoveState() {
        return super.shouldUpdateRiderGroundMoveState() && !isInWaterOrBubble();
    }

    public boolean isSwimmingMoving() {
        if (!isInWaterOrBubble() || isFlying()) {
            return false;
        }

        if (this.asyncSwimController.isMoving()) {
            return true;
        }

        if (this.getNavigation().isInProgress() && this.getNavigation().getPath() != null) {
            return true;
        }

        if (this.isVehicle()) {
            float forward = Math.abs(this.entityData.get(DATA_RIDER_FORWARD));
            float strafe = Math.abs(this.entityData.get(DATA_RIDER_STRAFE));
            if (forward > 0.03F || strafe > 0.03F) {
                return true;
            }
        }

        return this.getDeltaMovement().horizontalDistanceSqr() > 0.0025D;
    }

    @Override
    protected boolean shouldReapplyUnchangedFlyingState(boolean flying) {
        return !flying;
    }

    @Override
    protected boolean shouldRedirectFlyingStartToTakeoff() {
        return false;
    }

    @Override
    protected void onFlyingStateChanged(boolean wasFlying, boolean flying) {
        if (!flying) {
            takeoffComponent.clear();
            this.entityData.set(DATA_TAKEOFF, false);
            this.entityData.set(DATA_HOVERING, false);
            if (!isLanding()) {
                this.entityData.set(DATA_LANDING, false);
            }
        }
    }

    @Override
    protected void onTakeoffStateStarted() {
        if (!isBaby()) {
            getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_TAKEOFF.get(), 2.0f, 1.0f, 50);
        }
    }

    @Override
    protected boolean canApplyLandingState(boolean landing) {
        return true;
    }

    @Override
    public float getFlightSpeed() {
        float baseSpeed = (float) this.getAttributeValue(Attributes.FLYING_SPEED);
        if (this.isTame()) {
            return baseSpeed;
        }
        return (float) (baseSpeed * getConfiguredExtra("wild_flying_speed_multiplier", 1.0D));
    }

    @Override
    public double getPreferredFlightAltitude() {
        return 18.0D;
    }

    @Override
    public boolean canTakeoff() {
        if (this.isBaby() || !this.isAlive()) {
            return false;
        }
        return this.onGround() || (this.isInWaterOrBubble() && !this.isUnderWater()) || this.isInLava();
    }

    @Override
    protected void resetTimeFlyingAfterLanding() {
        this.timeFlying = 0;
    }

    @Override
    protected void afterSwitchToGroundNavigation() {
        if (onGround()) {
            setDeltaMovement(Vec3.ZERO);
            hasImpulse = false;
        }
    }

    @Override
    protected void onLocomotionModeTick(DragonLocomotionMode mode) {
        if (mode == DragonLocomotionMode.WATER && this.isVehicle()) {
            this.asyncSwimController.clear();
        }
    }

    @Override
    protected void onExitLocomotionMode(DragonLocomotionMode previousMode, DragonLocomotionMode nextMode) {
        super.onExitLocomotionMode(previousMode, nextMode);
        if (previousMode == DragonLocomotionMode.WATER && nextMode != DragonLocomotionMode.WATER) {
            this.asyncSwimController.clear();
        }
    }

    @Override
    protected void onEnterLocomotionMode(DragonLocomotionMode mode, DragonLocomotionMode previousMode) {
        super.onEnterLocomotionMode(mode, previousMode);
        if (mode == DragonLocomotionMode.WATER) {
            this.swimSteering.clear();
        } else if (mode == DragonLocomotionMode.AIR) {
            this.asyncSwimController.clear();
        } else {
            this.asyncSwimController.clear();
            this.swimSteering.clear();
        }
    }

    @Override
    protected void onRiderTakeoffRequest(Player player) {
        if (shouldSuppressTakeoffInput()) {
            return;
        }
        super.onRiderTakeoffRequest(player);
    }

    @Override
    protected boolean isRiderFallRecoveryBlocked() {
        return shouldSuppressTakeoffInput() || isUltimateSlamActive() || isBurrowing();
    }

    @Override
    protected boolean shouldSkipGroundedAerialRecovery() {
        return isUltimateSlamActive();
    }

    @Override
    protected boolean shouldIgnoreGroundedTakeoffRecovery() {
        return true;
    }

    @Override
    protected void beforeStandardRiderTakeoff(Player player) {
        if (!this.isInWaterOrBubble()) {
            clearGroundMobilityState();
        }
    }

    @Override
    protected void onRiderAbilityUse(Player player, String abilityName) {
        if (isBurrowing()) {
            if (ModAbilities.VOLITANS_BURROW.getName().equals(abilityName)) {
                var active = combatManager.getActiveAbility();
                if (active instanceof VolitansBurrowAbility burrowAbility
                        && active.getAbilityType() == ModAbilities.VOLITANS_BURROW) {
                    burrowAbility.requestExit(true);
                } else {
                    setBurrowing(false);
                }
            }
            return;
        }
        if (abilityName != null && !abilityName.isEmpty()) {
            if (ModAbilities.VOLITANS_BURROW.getName().equals(abilityName)) {
                var active = combatManager.getActiveAbility();
                if (active instanceof VolitansBurrowAbility burrowAbility
                        && active.getAbilityType() == ModAbilities.VOLITANS_BURROW) {
                    burrowAbility.requestExit(true);
                    return;
                }
            }
            useRidingAbility(abilityName);
        }
    }

    @Override
    protected void onRiderAccelerationStart(Player player) {
        if (isBurrowing()) {
            setAccelerating(false);
            return;
        }
        super.onRiderAccelerationStart(player);
    }

    @Override
    protected void onRiderAccelerationStop(Player player) {
        if (isBurrowing()) {
            setAccelerating(false);
            return;
        }
        super.onRiderAccelerationStop(player);
    }

    @Override
    protected boolean tryReleaseHeldRidingAbility(String abilityName) {
        if (isBurrowing()) {
            return true;
        }
        if (ModAbilities.VOLITANS_POISON_BALL.getName().equals(abilityName)) {
            var active = combatManager.getActiveAbility();
            if (active != null && active.getAbilityType() == ModAbilities.VOLITANS_POISON_BALL) {
                ((VolitansPoisonBallAbility) active).requestRelease();
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean handleCustomRiderAction(ServerPlayer player, DragonRiderAction action, String abilityName, boolean locked) {
        if (locked) {
            return false;
        }
        if (isBurrowing() && action == DragonRiderAction.TOGGLE_MELEE) {
            return true;
        }

        if (action == DragonRiderAction.DOUBLE_TAP_S) {
            onRiderBackwardDodge(player);
            return true;
        }
        if (action == DragonRiderAction.DOUBLE_TAP_W) {
            onRiderDash(player);
            return true;
        }
        if (action == DragonRiderAction.DOUBLE_TAP_A) {
            onRiderDodge(player, true);
            return true;
        }
        if (action == DragonRiderAction.DOUBLE_TAP_D) {
            onRiderDodge(player, false);
            return true;
        }

        if (action == DragonRiderAction.TOGGLE_MELEE) {
            if (combatManager.isAbilityActive(ModAbilities.VOLITANS_BREATH)) {
                toggleBreathMode();
                player.displayClientMessage(
                        Component.translatable(
                                isPoisonBreathMode()
                                        ? "saintsdragons.message.volitans_breath_poison"
                                        : "saintsdragons.message.volitans_breath_water"
                        ),
                        true
                );
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onRiderBackwardDodge(Player player) {
        if (isBurrowing() || isFlying() || isInWaterOrBubble() || riderBackDashCooldownTicks > 0 || isRiderForwardDashing()
                || isRiderBackDashing() || riderBackDashRecoveryTicks > 0
                || isRiderSideDodging() || riderSideDodgeRecoveryTicks > 0
                || !isAlive() || isDying() || isOrderedToSit() || isInSitTransition() || (isTamingStunned() && !isTame())) {
            return;
        }

        Vec3 forward = getRiderDashForwardVector();
        Vec3 backward = new Vec3(-forward.x, 0.0D, -forward.z).normalize();
        double perTickSpeed = DragonMotionMath.speedForIntegratedDistance(
                RIDER_BACK_DASH_DISTANCE_BLOCKS,
                RIDER_BACK_DASH_HORIZONTAL_DRAG,
                RIDER_BACK_DASH_DURATION_TICKS
        );
        Vec3 launch = new Vec3(backward.x * perTickSpeed, 0.0D, backward.z * perTickSpeed);

        beginRiderBackDash(launch);
        if (RIDER_BACK_DASH_LOCK_TICKS > 0) {
            lockRiderControls(RIDER_BACK_DASH_LOCK_TICKS);
        }
        riderBackDashCooldownTicks = RIDER_BACK_DASH_COOLDOWN_TICKS;
        triggerAnim(VolitansAnimationHandler.MOVEMENT_CONTROLLER, "dash_backwards");
        playBackwardDashSound();
    }

    @Override
    protected void onRiderDash(Player player) {
        if (isBurrowing() || isFlying() || isInWaterOrBubble() || riderBackDashCooldownTicks > 0
                || isRiderForwardDashing() || isRiderBackDashing() || riderBackDashRecoveryTicks > 0
                || isRiderSideDodging() || riderSideDodgeRecoveryTicks > 0
                || !isAlive() || isDying() || isOrderedToSit() || isInSitTransition()) {
            return;
        }

        float yawRad = (float) Math.toRadians(this.getYRot());
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double perTickSpeed = DragonMotionMath.speedForIntegratedDistance(
                RIDER_FORWARD_DASH_DISTANCE_BLOCKS,
                RIDER_FORWARD_DASH_HORIZONTAL_DRAG,
                RIDER_FORWARD_DASH_DURATION_TICKS
        );
        if (perTickSpeed <= 0.0D) {
            return;
        }
        Vec3 dashVec = new Vec3(forwardX * perTickSpeed, 0.0D, forwardZ * perTickSpeed);
        beginRiderForwardDash(dashVec);
        riderBackDashCooldownTicks = RIDER_BACK_DASH_COOLDOWN_TICKS;
        triggerAnim(VolitansAnimationHandler.MOVEMENT_CONTROLLER, "dash_forward");
        playForwardDashSound();
    }

    @Override
    protected void onRiderDodge(Player player, boolean isLeft) {
        if (isBurrowing() || isFlying() || isInWaterOrBubble() || riderBackDashCooldownTicks > 0
                || isRiderForwardDashing() || isRiderBackDashing() || riderBackDashRecoveryTicks > 0
                || isRiderSideDodging() || riderSideDodgeRecoveryTicks > 0
                || !isAlive() || isDying() || isOrderedToSit() || isInSitTransition() || (isTamingStunned() && !isTame())) {
            return;
        }

        float yawRad = (float) Math.toRadians(this.getYRot());
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);
        double perTickSpeed = RIDER_SIDE_DODGE_DISTANCE_BLOCKS / RIDER_SIDE_DODGE_DURATION_TICKS;
        if (perTickSpeed <= 0.0D) {
            return;
        }

        double dodgeDirX = rightX * (isLeft ? 1 : -1);
        double dodgeDirZ = rightZ * (isLeft ? 1 : -1);
        Vec3 dodgeVector = new Vec3(dodgeDirX * perTickSpeed, 0.0D, dodgeDirZ * perTickSpeed);

        beginRiderSideDodge(dodgeVector);
        setRiderNudgeSteerOffset(isLeft ? -90.0F : 90.0F);
        riderBackDashCooldownTicks = RIDER_BACK_DASH_COOLDOWN_TICKS;
        triggerAnim(VolitansAnimationHandler.MOVEMENT_CONTROLLER, isLeft ? "dodge_left" : "dodge_right");
        playDodgeSound();
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel serverLevel) {
            DragonDestructionManager.applyPassiveTreeDestruction(serverLevel, this);
        }
        tickScreenShake();

        if (!this.level().isClientSide) {
            if (isTamingStunned()) {
                if (getTarget() != null) {
                    super.setTarget(null);
                }
                if (getActiveAbility() != null) {
                    combatManager.forceEndActiveAbility();
                }
                setAggressive(false);
                getNavigation().stop();
            }
            tamingController.tickServer();
            if (isTamingStunned()) {
                tamingController.enforceGroundingTick();
            }
            tickTemporaryInvulnerability();
            tickRiderControlLock();
            if (takeoffInputBlockTicks > 0) {
                takeoffInputBlockTicks--;
                // Flush latched vertical rider input while burrow exit settles.
                this.setGoingUp(false);
                this.setGoingDown(false);
            }
            if (isFlying()
                    && areRiderControlsLocked()
                    && !isUltimateSlamActive()
                    && combatManager.getActiveAbility() == null
                    && !isRiderForwardDashing()
                    && !isRiderBackDashing()
                    && riderBackDashRecoveryTicks <= 0
                    && !isRiderSideDodging()
                    && riderSideDodgeRecoveryTicks <= 0) {
                // Safety net for stale lock state mismatches while airborne.
                clearRiderControlLock();
            }
            if (isRiderForwardDashing()) {
                riderGroundNudge.tickServerState();
                tickRiderForwardDashDamage();
            }
            if (isRiderBackDashing()) {
                riderGroundNudge.tickServerState();
                if (!riderGroundNudge.isActive()) {
                    riderBackDashVec = riderGroundNudge.getLastVelocity();
                    riderBackDashRecoveryTicks = RIDER_BACK_DASH_RECOVERY_TICKS;
                }
            }
            if (riderBackDashRecoveryTicks > 0) {
                handleRiderBackDashRecoveryMovement();
            }
            tickBackwardDashSpikes();
            if (isRiderSideDodging()) {
                riderGroundNudge.tickServerState();
                if (!riderGroundNudge.isActive()) {
                    riderSideDodgeVec = riderGroundNudge.getLastVelocity();
                    riderSideDodgeRecoveryTicks = RIDER_SIDE_DODGE_RECOVERY_TICKS;
                }
            }
            if (riderSideDodgeRecoveryTicks > 0) {
                handleRiderSideDodgeRecoveryMovement();
            }
            tickFeedingCooldown();
            if (spineDropCooldownTicks > 0) {
                spineDropCooldownTicks--;
            }
            if (riderBackDashCooldownTicks > 0) {
                riderBackDashCooldownTicks--;
            }
            tickVenomNeutralized();
            if (aiGroundMobilityCooldownTicks > 0) {
                aiGroundMobilityCooldownTicks--;
            }
            handleAmbientSounds();
            tickWaterPreferenceTimers();
            tickBreathGaugeEnergy();
            tickStandardTakeoffAndGroundedAerialRecovery();
            tickRiderTakeoff();

            if (isFlying()) {
                timeFlying++;
            } else {
                timeFlying = 0;
            }

            tickRiderLandingBlendTimer();
            updateSittingProgress();

            if (isLanding() && onGround()) {
                handleAiLandingComplete();
            }

            if (!isUltimateSlamActive() && this.isInWaterOrBubble() && shouldClearRiderFlightStateInWater()) {
                markLandedNow();
            }

            if (isFlying() && !isUltimateSlamActive()) {
                boolean riddenByOwner = isRiddenByOwner();
                if (this.onGround() && !isTakeoff() && !isGoingUp()) {
                    handleAiLandingComplete();
                } else setHovering(!riddenByOwner && this.getDeltaMovement().horizontalDistanceSqr() < 0.01D);
            }

        }

        tickBankingLogic();
        tickBarrelRollLogic();
        tickPitchingLogic();

        this.noPhysics = false;
        boolean shouldUseAirNavigation = isAerial();
        if (shouldUseAirNavigation) {
            this.setNoGravity(true);
            switchToAirNavigation();
        } else {
            this.setNoGravity(false);
            switchToGroundNavigation();
        }

        syncFlightAnimationState();
        tickAsyncFlightNavigation(isAiSpecialCombatActive() || isAiSpecialCombatReserved());

        if (!this.level().isClientSide) {
            tickAnimationStates();
            tickBurrowRumbleShake();
        }
    }

    @Override
    protected Brain.Provider<Volitans> brainProvider() {
        return DRAGON_BRAIN.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return DragonBrain.makeBrain(DRAGON_BRAIN, dynamic);
    }

    @Override
    protected void customServerAiStep() {
        DragonBrain.tick(DRAGON_BRAIN, this);
        super.customServerAiStep();
    }

    @Override
    public DragonAirCombatSettings getAiAirCombatSettings() {
        return AI_AIR_COMBAT_SETTINGS;
    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
        if (isUltimateSlamActive()) {
            player.fallDistance = 0.0F;
            this.fallDistance = 0.0F;
            this.setTarget(null);
            copyRiderYaw(player);
            this.setAccelerating(false);
            return;
        }

        super.tickRidden(player, travelVector);

        if (areRiderControlsLocked()) {
            player.fallDistance = 0.0F;
            this.fallDistance = 0.0F;
            this.setTarget(null);
            copyRiderYaw(player);
            this.setAccelerating(false);
            if (!this.isFlying()) {
                this.setGoingUp(false);
                this.setGoingDown(false);
            }
            return;
        }

        riderController.tickRidden(player);
    }

    public boolean isRiderPitchKeyMode() {
        return this.entityData.get(DATA_PITCH_KEY_MODE);
    }

    public void setRiderPitchKeyMode(boolean enabled) {
        this.entityData.set(DATA_PITCH_KEY_MODE, enabled);
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        Vec3 input = riderController.getRiddenInput(player, deltaIn);
        return super.getRiddenInput(player, input);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        if (this.getFirstPassenger() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return riderController.getPassengersRidingOffset();
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull Entity.MoveFunction moveFunction) {
        riderController.positionRider(passenger, moveFunction);
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        return riderController.getDismountLocationForPassenger(passenger);
    }

    @Override
    public void travel(@NotNull Vec3 motion) {
        if (isUltimateSlamActive()) {
            Vec3 current = this.getDeltaMovement();
            double verticalMotion = this.isGoingDown() ? Math.min(current.y, -2.5D) : 0.0D;
            Vec3 slamMotion = new Vec3(current.x, verticalMotion, current.z);
            this.setDeltaMovement(slamMotion);
            this.move(MoverType.SELF, slamMotion);
            this.hasImpulse = true;
            this.hurtMarked = true;
            return;
        }

        if (isRiderForwardDashing()) {
            super.travel(Vec3.ZERO);
            riderGroundNudge.steerHorizontal(DragonMotionMath.horizontalForward(this.getYRot()));
            riderGroundNudge.applyTravelMotion();
            return;
        }
        if (isRiderBackDashing()) {
            super.travel(Vec3.ZERO);
            riderGroundNudge.steerHorizontal(riderGroundBurstSteeringDirection());
            riderGroundNudge.applyTravelMotion();
            return;
        }
        if (riderBackDashRecoveryTicks > 0) {
            super.travel(Vec3.ZERO);
            return;
        }
        if (isRiderSideDodging()) {
            super.travel(Vec3.ZERO);
            if (getControllingPassenger() instanceof Player) {
                riderGroundNudge.steerHorizontal(riderGroundBurstSteeringDirection());
            }
            riderGroundNudge.applyTravelMotion();
            return;
        }
        if (riderSideDodgeRecoveryTicks > 0) {
            super.travel(Vec3.ZERO);
            return;
        }

        if (areRiderControlsLocked()) {
            super.travel(Vec3.ZERO);
            return;
        }

        if (this.isVehicle() && this.getControllingPassenger() instanceof Player rider
                && this.isTame() && this.isOwnedBy(rider)) {
            Vec3 riddenInput = this.getRiddenInput(rider, motion);
            boolean inWater = this.isInWaterOrBubble() || this.isInLava();
            if (inWater && !level().isClientSide) {
                clearRiderFlightStateInWaterIfNeeded();
            }
            if (isFlying() && (!inWater || shouldUseRiderFlightMovementInWater())) {
                riderController.handleFlightTravel(rider, riddenInput);
                return;
            }

            if (inWater) {
                riderController.handleSwimTravel(rider, riddenInput);
                if (!level().isClientSide) {
                    setGroundMoveStateFromRider(0);
                }
                if (!level().isClientSide) {
                    tryAutoBreachRiderTakeoff();
                }
                return;
            }

            if (isBurrowing()) {
                this.setRunning(false);
                this.setAccelerating(false);
                this.setSpeed((float) RIDER_BURROW_SPEED);
                super.travel(motion);
                return;
            }

            float groundSpeed = (float) (this.isAccelerating() ? RIDER_RUN_SPEED : RIDER_WALK_SPEED);
            this.setRunning(this.isAccelerating() && rider.zza > 0.05F);
            this.setSpeed(groundSpeed);
            super.travel(motion);
            return;
        }

        super.travel(motion);
    }

    @Override
    protected boolean isRiderInputLocked(Player player) {
        return areRiderControlsLocked();
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        InteractionResult handlerResult = interactionHandler.handleInteraction(player, hand);
        if (handlerResult != InteractionResult.PASS) {
            return handlerResult;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(movementController, vocalController, actionController, fastActionController, flightController, airActionController, interactionController);
    }

    private void setupAnimationControllers() {
        AnimationHelper.registerSoundKeyframes(this, movementController, actionController,
                fastActionController, flightController, airActionController, vocalController, interactionController);
        animationHandler.setupActionController(actionController);
        animationHandler.setupFastActionController(fastActionController);
        animationHandler.setupFlightController(flightController);
        animationHandler.setupAirActionController(airActionController);
        AnimationHelper.registerGrumbles(vocalController, this);
        animationHandler.setupInteractionController(interactionController);
        animationHandler.setupMovementController(movementController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return dragonCache;
    }

    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        return getMeleeMode() == 0 ? ModAbilities.VOLITANS_BITE : ModAbilities.VOLITANS_HORN_GORE;
    }

    @Override
    public DragonAbilityType<?, ?> getRoaringAbility() {
        return ModAbilities.VOLITANS_ROAR;
    }

    @Override
    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return ModAbilities.VOLITANS_HURT;
    }

    @Override
    protected DragonAbilityType<?, ?> getDeathAbilityType() {
        return ModAbilities.VOLITANS_DIE;
    }

    @Override
    public RiderAbilityBinding getPrimaryRiderAbility() {
        return new RiderAbilityBinding(ModAbilities.VOLITANS_ROAR.getName(), RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getSecondaryRiderAbility() {
        String abilityId = (isFlying() || !isGroundedForAction())
                ? ModAbilities.VOLITANS_ULTIMATE.getName()
                : ModAbilities.VOLITANS_BURROW.getName();
        return new RiderAbilityBinding(abilityId, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getAttackRiderAbility() {
        String abilityId = getMeleeMode() == 0
                ? ModAbilities.VOLITANS_BITE.getName()
                : ModAbilities.VOLITANS_HORN_GORE.getName();
        return new RiderAbilityBinding(abilityId, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getTertiaryRiderAbility() {
        return new RiderAbilityBinding(ModAbilities.VOLITANS_CLAW.getName(), RiderAbilityBinding.Activation.PRESS);
    }

    public Vec3 getBreathOrigin() {
        Vec3 breathLocator = getBonePositionForBreath("breathBoneOrigin");
        if (breathLocator != null) {
            return breathLocator;
        }
        return computeBreathOriginFallback();
    }

    private Vec3 computeBreathOriginFallback() {
        double x = Mth.lerp((float) 1.0, this.xo, this.getX());
        double y = Mth.lerp((float) 1.0, this.yo, this.getY());
        double z = Mth.lerp((float) 1.0, this.zo, this.getZ());
        float yawDeg = Mth.lerp((float) 1.0, this.yHeadRotO, this.yHeadRot);
        float pitchDeg = Mth.lerp((float) 1.0, this.xRotO, this.getXRot());
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double localRight = 0.0D;
        double localUp = this.getBbHeight() * 0.72D;
        double localForward = this.getBbWidth() * 0.75D;
        double cp = Math.cos(pitch);
        double sp = Math.sin(pitch);
        double cy = Math.cos(-yaw);
        double sy = Math.sin(-yaw);
        double x1 = localRight;
        double y1 = localUp * cp - localForward * sp;
        double z1 = localUp * sp + localForward * cp;
        double wx = x1 * cy - z1 * sy;
        double wz = x1 * sy + z1 * cy;
        return new Vec3(x + wx, y + y1, z + wz);
    }

    public void setServerBonePosition(String boneName, Vec3 position) {
        if (boneName == null || position == null) {
            return;
        }
        this.serverBonePositionCache.put(boneName, position);
    }

    public Vec3 getBonePositionForBreath(String boneName) {
        if (boneName == null) {
            return null;
        }
        if (this.level().isClientSide) {
            return this.clientLocatorCache.get(boneName);
        }
        return this.serverBonePositionCache.get(boneName);
    }

    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob otherParent) {
        return createBreedOffspring(level, otherParent, ModEntities.VOLITANS.get(), Volitans::applyConfiguredAttributes);
    }

    @Override
    public boolean canBreed() {
        return this.isTame() && !this.isBaby() && this.getHealth() >= this.getMaxHealth() && this.isInLove();
    }

    @Override
    public BlockState getEggBlockState() {
        return ModBlocks.VOLITANS_EGG.get().defaultBlockState().setValue(VolitansEggBlock.WATERLOGGED, isInWaterOrBubble());
    }

    @Override
    protected DragonVariantSet getVariantSet() {
        return VARIANTS;
    }

    @Override
    public boolean isFood(@NotNull ItemStack stack) {
        return stack.is(ModTags.Items.VOLITANS_FOODS);
    }

    public boolean isTamingStunned() {
        return !this.isBaby() && this.entityData.get(DATA_TAMING_STUNNED);
    }

    public void enterTamingStun() {
        if (this.isBaby()) {
            return;
        }
        tamingController.enterStun();
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

    public boolean isReadyForTamingFeed() {
        return tamingController.isReadyForTamingFeed();
    }

    public boolean isBelowTamingThreshold() {
        if (this.isBaby()) {
            return false;
        }
        return this.getHealth() <= getTamingThreshold();
    }

    public float getTamingThreshold() {
        double configured = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.VOLITANS_ID)
                .extraDouble("taming_stun_health", 60.0D);
        double clamped = Math.max(0.0D, Math.min(configured, this.getMaxHealth()));
        return (float) clamped;
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadRideableData(tag);
        tamingController.load(tag);
        if (tag.contains("VolitansBreathMode")) {
            setBreathMode(tag.getInt("VolitansBreathMode"));
        }
        if (tag.contains("VolitansWaterBreathEnergy")) {
            setWaterBreathEnergy(tag.getFloat("VolitansWaterBreathEnergy"));
        } else {
            setWaterBreathEnergy(1.0F);
        }
        if (tag.contains("VolitansPoisonBreathEnergy")) {
            setPoisonBreathEnergy(tag.getFloat("VolitansPoisonBreathEnergy"));
        } else {
            setPoisonBreathEnergy(1.0F);
        }
        if (tag.contains("VolitansWaterBreathDepleted")) {
            setWaterBreathDepleted(tag.getBoolean("VolitansWaterBreathDepleted"));
        }
        if (tag.contains("VolitansPoisonBreathDepleted")) {
            setPoisonBreathDepleted(tag.getBoolean("VolitansPoisonBreathDepleted"));
        }
        if (tag.contains("FeedingCooldownTicks")) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, tag.getInt("FeedingCooldownTicks")));
        }
        if (tag.contains("VenomNeutralizedTicks")) {
            this.entityData.set(DATA_VENOM_NEUTRALIZED_TICKS, Math.max(0, tag.getInt("VenomNeutralizedTicks")));
        }
        tempInvulnTicks = Math.max(0, tag.getInt("VolitansTempInvulnTicks"));
        if (tempInvulnTicks > 0) {
            setInvulnerable(true);
        } else if (!isDying()) {
            setInvulnerable(false);
        }
        setBurrowing(false);
        if (isAerial()) {
            switchToAirNavigation();
            setNoGravity(true);
        } else {
            switchToGroundNavigation();
            setNoGravity(false);
        }
        applyConfiguredAttributes();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        saveRideableData(tag);
        tag.putInt("VolitansBreathMode", getBreathMode());
        tag.putFloat("VolitansWaterBreathEnergy", getWaterBreathEnergy());
        tag.putFloat("VolitansPoisonBreathEnergy", getPoisonBreathEnergy());
        tag.putBoolean("VolitansWaterBreathDepleted", isWaterBreathDepleted());
        tag.putBoolean("VolitansPoisonBreathDepleted", isPoisonBreathDepleted());
        tag.putInt("FeedingCooldownTicks", Math.max(0, this.entityData.get(DATA_FEEDING_COOLDOWN)));
        tag.putInt("VenomNeutralizedTicks", Math.max(0, this.entityData.get(DATA_VENOM_NEUTRALIZED_TICKS)));
        tag.putInt("VolitansTempInvulnTicks", tempInvulnTicks);
        tamingController.save(tag);
    }

    public boolean canFeed() {
        return this.entityData.get(DATA_FEEDING_COOLDOWN) <= 0;
    }

    public void setFeedingCooldown(int ticks) {
        this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, ticks));
    }

    public void grantTemporaryInvulnerability(int ticks) {
        tempInvulnTicks = Math.max(tempInvulnTicks, Math.max(0, ticks));
        if (tempInvulnTicks > 0) {
            setInvulnerable(true);
        }
    }

    private void tickTemporaryInvulnerability() {
        if (tempInvulnTicks <= 0) {
            return;
        }
        tempInvulnTicks--;
        if (tempInvulnTicks <= 0) {
            if (!isDying()) {
                setInvulnerable(false);
            }
        }
    }

    private void tickFeedingCooldown() {
        int cooldownTicks = this.entityData.get(DATA_FEEDING_COOLDOWN);
        if (cooldownTicks > 0) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, cooldownTicks - 1);
        }
    }

    private void tickVenomNeutralized() {
        int ticks = this.entityData.get(DATA_VENOM_NEUTRALIZED_TICKS);
        if (ticks > 0) {
            this.entityData.set(DATA_VENOM_NEUTRALIZED_TICKS, ticks - 1);
        }
    }

    public void neutralizeVenom(int ticks) {
        this.entityData.set(DATA_VENOM_NEUTRALIZED_TICKS,
                Math.max(this.entityData.get(DATA_VENOM_NEUTRALIZED_TICKS), Math.max(0, ticks)));
    }

    public boolean isVenomNeutralized() {
        return this.entityData.get(DATA_VENOM_NEUTRALIZED_TICKS) > 0;
    }

    private void tickWaterPreferenceTimers() {
        if (isInWaterOrBubble()) {
            this.setAirSupply(this.getMaxAirSupply());
            this.ticksInWater = Math.min(this.ticksInWater + 1, 1200);
            this.ticksOutOfWater = 0;
        } else {
            this.ticksOutOfWater = Math.min(this.ticksOutOfWater + 1, 1200);
            this.ticksInWater = 0;
        }
    }

    @Override
    public int getDeathAnimationDurationTicks() {
        return 55;
    }

    @Override
    protected void dropAdditionalDeathLootAfterBase(@NotNull DamageSource source) {
        if (!level().isClientSide && getGender() == DragonGender.FEMALE) {
            DragonLootTables.dropEntityLoot(this, DragonLootTables.VOLITANS_FEMALE_DEATH, source);
        }
    }

    @Override
    public double getSwimSpeed() {
        return RIDER_SWIM_SPEED;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean shouldEnterWater() {
        if (isOrderedToSit() || isVehicle() || isFlying() || isTakeoff() || isLanding()) {
            return false;
        }
        if (isOnFire()) {
            return true;
        }
        if (getTarget() != null && getHealth() < getMaxHealth() * 0.5F) {
            return true;
        }
        return this.ticksOutOfWater > 1000 && this.getRandom().nextFloat() < 0.08F;
    }

    @Override
    public boolean shouldLeaveWater() {
        if (isOrderedToSit() || isVehicle()) {
            return false;
        }

        LivingEntity owner = getOwner();
        if (isTame() && owner != null && !owner.isInWater() && distanceToSqr(owner) > 100.0D) {
            return true;
        }

        LivingEntity target = getTarget();
        if (target != null && !target.isInWater()) {
            return true;
        }

        return this.ticksInWater >= 1200 && this.getRandom().nextFloat() < 0.08F;
    }

    @Override
    public int getWaterSearchRange() {
        return 20;
    }

    public boolean canBeBound() {
        return !isDying()
                && !isTakeoff()
                && !isLanding()
                && !isBurrowing()
                && !areRiderControlsLocked()
                && !isGroundMobilityActive()
                && combatManager.getActiveAbility() == null;
    }

    public void prepareForBinderStorage() {
        clearVolitansBinderTransientState();
        setBoundInBinder(true);
    }

    public void prepareAfterBinderRelease() {
        clearVolitansBinderTransientState();
        setBoundInBinder(false);
    }

    private void clearVolitansBinderTransientState() {
        setTarget(null);
        setAggressive(false);
        setLastHurtByMob(null);
        this.setLastHurtByPlayer(null);
        setInvulnerable(false);
        setBurrowing(false);
        stopUltimateSlamMovement();
        setAiSpecialCombatActive(false);
        setAiSpecialCombatReserved(false);
        clearGroundMobilityState();
        combatManager.clearAllStates();
        setGoingUp(false);
        setGoingDown(false);
        setAccelerating(false);
        setRunning(false);
        setGroundMoveStateFromAI(0);
        setGroundMoveStateFromRider(0);
        setDeltaMovement(Vec3.ZERO);
        this.hurtMarked = false;
    }

    @Override
    public boolean ignoresLeashPull() {
        return true;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effectInstance) {
        if (effectInstance.getEffect() == MobEffects.POISON) {
            return false;
        }
        return super.canBeAffected(effectInstance);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (isDying() || !isAlive()) {
            return false;
        }
        rememberIncomingProjectile(source);
        if (source.getEntity() instanceof Pufferfish || source.getDirectEntity() instanceof Pufferfish) {
            return false;
        }
        if (source.is(DamageTypes.MAGIC) && this.hasEffect(MobEffects.POISON)) {
            return false;
        }
        if (tamingController.tryEnterHoldStateFromDamage(source, amount)) {
            return true;
        }
        if (tryReactiveHitEvade(source, amount)) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && shouldDropCombatSpine(source, amount)) {
            if (DragonLootTables.dropEntityLoot(this, DragonLootTables.VOLITANS_HIT, source)) {
                spineDropCooldownTicks = SPINE_DROP_COOLDOWN_TICKS;
            }
        }
        return hurt;
    }

    private boolean shouldDropCombatSpine(@NotNull DamageSource source, float amount) {
        if (level().isClientSide || amount <= 0.0F || spineDropCooldownTicks > 0) {
            return false;
        }
        if (source.getEntity() instanceof LivingEntity) {
            return true;
        }
        return source.getDirectEntity() instanceof Projectile;
    }

    private boolean tryReactiveHitEvade(@NotNull DamageSource source, float amount) {
        if (level().isClientSide || amount <= 0.0F || isVehicle() || !isAlive() || isDying() || (isTamingStunned() && !isTame())) {
            return false;
        }

        LivingEntity attacker = null;
        if (source.getEntity() instanceof LivingEntity living) {
            attacker = living;
        } else if (source.getDirectEntity() instanceof LivingEntity living) {
            attacker = living;
        } else if (source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof LivingEntity living) {
            attacker = living;
        }

        if (attacker instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        if (attacker == null && source.getEntity() == null && source.getDirectEntity() == null) {
            return false;
        }
        if (this.getRandom().nextFloat() >= REACTIVE_HIT_EVADE_CHANCE) {
            return false;
        }

        boolean evaded = this.getRandom().nextBoolean()
                ? tryReactiveGroundBackstep(attacker)
                : tryReactiveGroundDodge(attacker);
        if (!evaded) {
            evaded = this.getRandom().nextBoolean()
                    ? tryReactiveGroundBackstep(attacker)
                    : tryReactiveGroundDodge(attacker);
        }
        if (!evaded) {
            return false;
        }

        if (attacker != null) {
            this.setLastHurtByMob(attacker);
            if (!this.isAlly(attacker)) {
                this.setTarget(attacker);
            }
        }
        return true;
    }

    @Override
    public Map<String, VocalEntry> getVocalEntries() {
        return VOCAL_ENTRIES;
    }

    @Override
    public DragonSoundProfile getSoundProfile() {
        return VolitansSoundProfile.INSTANCE;
    }

    @Override
    protected ScreenShakeComponent getScreenShakeComponent() {
        return screenShakeComponent;
    }

    @Override
    protected boolean isRidingAbilityAllowed(DragonAbilityType<?, ?> abilityType) {
        return abilityType == ModAbilities.VOLITANS_ROAR
                || abilityType == ModAbilities.VOLITANS_BURROW
                || abilityType == ModAbilities.VOLITANS_BITE
                || abilityType == ModAbilities.VOLITANS_CLAW
                || abilityType == ModAbilities.VOLITANS_BREATH
                || abilityType == ModAbilities.VOLITANS_POISON_BALL
                || abilityType == ModAbilities.VOLITANS_HORN_GORE
                || abilityType == ModAbilities.VOLITANS_ULTIMATE;
    }

    public int getBreathMode() {
        return this.entityData.get(DATA_BREATH_MODE);
    }

    public void setBreathMode(int mode) {
        this.entityData.set(DATA_BREATH_MODE, Mth.clamp(mode, 0, 1));
    }

    public boolean isPoisonBreathMode() {
        return getBreathMode() == 1;
    }

    public float getWaterBreathEnergy() {
        return this.entityData.get(DATA_WATER_BREATH_ENERGY);
    }

    public void setWaterBreathEnergy(float energy) {
        DragonBreathComponent.setEnergy(getCurrentBreathGauge(), energy, BREATH_REARM_THRESHOLD);
    }

    public float getPoisonBreathEnergy() {
        return getWaterBreathEnergy();
    }

    public void setPoisonBreathEnergy(float energy) {
        setWaterBreathEnergy(energy);
    }

    public boolean isWaterBreathDepleted() {
        return this.entityData.get(DATA_WATER_BREATH_DEPLETED);
    }

    public void setWaterBreathDepleted(boolean depleted) {
        this.entityData.set(DATA_WATER_BREATH_DEPLETED, depleted);
        this.entityData.set(DATA_POISON_BREATH_DEPLETED, depleted);
    }

    public boolean isPoisonBreathDepleted() {
        return isWaterBreathDepleted();
    }

    public void setPoisonBreathDepleted(boolean depleted) {
        setWaterBreathDepleted(depleted);
    }

    public boolean canUseCurrentBreathMode() {
        return DragonBreathComponent.canUse(getCurrentBreathGauge(), BREATH_DEPLETED_THRESHOLD);
    }

    public boolean drainCurrentBreathEnergy(float amount) {
        return DragonBreathComponent.drain(getCurrentBreathGauge(), amount, BREATH_DEPLETED_THRESHOLD, BREATH_REARM_THRESHOLD);
    }

    public boolean isBreathing() {
        return this.entityData.get(DATA_BREATHING);
    }

    public void setBreathing(boolean breathing) {
        this.entityData.set(DATA_BREATHING, breathing);
    }

    public boolean isBurrowing() {
        return this.entityData.get(DATA_BURROWING);
    }

    public void setBurrowing(boolean burrowing) {
        this.entityData.set(DATA_BURROWING, burrowing);
    }

    public void blockTakeoffAfterBurrowExit(int ticks) {
        blockTakeoffInput(Math.max(0, ticks) + BURROW_EXIT_TAKEOFF_BLOCK_BUFFER_TICKS);
    }

    public void blockTakeoffInput(int ticks) {
        this.takeoffInputBlockTicks = Math.max(this.takeoffInputBlockTicks, Math.max(0, ticks));
        this.setGoingUp(false);
        this.setGoingDown(false);
    }

    public void toggleBreathMode() {
        setBreathMode(isPoisonBreathMode() ? 0 : 1);
    }

    private void tickBreathGaugeEnergy() {
        if (isBreathing()) {
            return;
        }

        if (getWaterBreathEnergy() < 1.0F) {
            DragonBreathComponent.regen(getCurrentBreathGauge(), (float) getConfiguredExtra("breath_regen_per_tick", 0.0025D), BREATH_REARM_THRESHOLD);
        }
    }

    public DragonBreathComponent.Gauge getCurrentBreathGauge() {
        return new DragonBreathComponent.Gauge() {
            @Override
            public float getEnergy() {
                return Volitans.this.getWaterBreathEnergy();
            }

            @Override
            public void setEnergyRaw(float energy) {
                float clamped = Mth.clamp(energy, 0.0F, 1.0F);
                Volitans.this.entityData.set(DATA_WATER_BREATH_ENERGY, clamped);
                Volitans.this.entityData.set(DATA_POISON_BREATH_ENERGY, clamped);
            }

            @Override
            public boolean isDepleted() {
                return Volitans.this.isWaterBreathDepleted();
            }

            @Override
            public void setDepleted(boolean depleted) {
                Volitans.this.setWaterBreathDepleted(depleted);
            }
        };
    }

    public int getConfiguredPoisonLevel(String key, int fallbackLevel) {
        int level = Mth.floor(getConfiguredExtra(key, fallbackLevel));
        return Mth.clamp(level, 0, 4);
    }

    public int getConfiguredPoisonAmplifier(String key, int fallbackLevel) {
        int level = getConfiguredPoisonLevel(key, fallbackLevel);
        return level <= 0 ? -1 : level - 1;
    }

    public boolean isUltimateSlamActive() {
        return this.entityData.get(DATA_ULTIMATE_SLAM_ACTIVE);
    }

    public void startUltimateSlamMovement() {
        clearGroundMobilityState();
        getAIMovement().stop();
        getNavigation().stop();
        this.entityData.set(DATA_ULTIMATE_SLAM_ACTIVE, true);
        setFlying(true);
        setTakeoff(false);
        setHovering(false);
        setLanding(false);
    }

    public void stopUltimateSlamMovement() {
        if (!isUltimateSlamActive()) {
            return;
        }
        this.entityData.set(DATA_ULTIMATE_SLAM_ACTIVE, false);
        if (isAiSpecialCombatActive() && !onGround() && !isVehicle() && !isPassenger() && !isInWaterOrBubble() && !isInLava()) {
            beginAiFlight();
        } else if (!onGround() && !isVehicle() && !isPassenger() && !isInWaterOrBubble() && !isInLava()) {
            beginAiFlight();
        }
    }

    public void beginAiTakeoff() {
        setGoingUp(true);
        setGoingDown(false);
        startTakeoffSequence(0.12D, TAKEOFF_ANIMATION_TICKS);
    }

    public void beginAiFlight() {
        setFlying(true);
        setTakeoff(false);
        setLanding(false);
        setHovering(false);
        setGoingUp(false);
        setGoingDown(false);
    }

    public void beginAiLanding() {
        setTakeoff(false);
        setHovering(false);
        setFlying(false);
        setLanding(true);
        setGoingUp(false);
        setGoingDown(false);
    }

    public void handleAiLandingComplete() {
        if (isInWaterOrBubble()) {
            suppressSleep(60);
            completeTouchdownLanding(LandingSource.AI);
            return;
        }
        if (!level().isClientSide) {
            triggerAnim(AnimationHelper.MOVEMENT_CONTROLLER, AnimationHelper.LANDED);
            if (!isBaby()) {
                getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_LANDED.get(), 2.0f, 1.0f, 32);
            }
            suppressSleep(60);
        }
        completeTouchdownLanding(LandingSource.AI);
        startStandardLandedRecovery(LANDED_RECOVERY_TICKS);
    }

    @Override
    protected void completeGroundedAerialRecoveryLanding() {
        handleAiLandingComplete();
    }

    public boolean isAiSpecialCombatActive() {
        return aiSpecialCombatActive;
    }

    public void setAiSpecialCombatActive(boolean active) {
        this.aiSpecialCombatActive = active;
        if (active) {
            this.aiSpecialCombatReserved = false;
        }
    }

    public boolean isAiSpecialCombatReserved() {
        return aiSpecialCombatReserved;
    }

    public void setAiSpecialCombatReserved(boolean reserved) {
        this.aiSpecialCombatReserved = reserved;
        if (reserved) {
            this.aiSpecialCombatActive = false;
        }
    }

    public boolean isGroundMobilityActive() {
        return isRiderBackDashing()
                || riderBackDashRecoveryTicks > 0
                || isRiderSideDodging()
                || riderSideDodgeRecoveryTicks > 0;
    }

    public boolean isGroundCombatAbilityActive() {
        return isAbilityActive(ModAbilities.VOLITANS_BITE)
                || isAbilityActive(ModAbilities.VOLITANS_CLAW)
                || isAbilityActive(ModAbilities.VOLITANS_HORN_GORE)
                || isAbilityActive(ModAbilities.VOLITANS_ROAR)
                || isAbilityActive(ModAbilities.VOLITANS_POISON_BALL)
                || isAbilityActive(ModAbilities.VOLITANS_BREATH)
                || isAbilityActive(ModAbilities.VOLITANS_BURROW);
    }

    public boolean isAiRootedByAbility() {
        if (isAbilityActive(ModAbilities.VOLITANS_ROAR) && !isFlying() && !isInWaterOrBubble()) {
            return true;
        }
        if (isAbilityActive(ModAbilities.VOLITANS_POISON_BALL) && !isFlying() && !isInWaterOrBubble()) {
            return true;
        }
        return isAbilityActive(ModAbilities.VOLITANS_BURROW) && !isBurrowing();
    }

    public boolean shouldAiHoldPositionForAbility() {
        return isAiRootedByAbility()
                || (isAbilityActive(ModAbilities.VOLITANS_BREATH) && !isFlying());
    }

    public void requestPoisonBallRelease() {
        var active = combatManager.getActiveAbility();
        if (active instanceof VolitansPoisonBallAbility poisonBallAbility
                && active.getAbilityType() == ModAbilities.VOLITANS_POISON_BALL) {
            poisonBallAbility.requestRelease();
        }
    }

    public void requestBurrowExit(boolean withBurst) {
        var active = combatManager.getActiveAbility();
        if (active instanceof VolitansBurrowAbility burrowAbility
                && active.getAbilityType() == ModAbilities.VOLITANS_BURROW) {
            burrowAbility.requestExit(withBurst);
        }
    }

    private boolean tryReactiveGroundDodge(@Nullable LivingEntity threat) {
        if (isAerial() || isInWaterOrBubble() || isBurrowing() || (isTamingStunned() && !isTame())) {
            return false;
        }
        if (isGroundMobilityActive() || aiGroundMobilityCooldownTicks > 0 || areRiderControlsLocked()) {
            return false;
        }

        float yawRad = (float) Math.toRadians(this.getYRot());
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);
        double perTickSpeed = RIDER_SIDE_DODGE_DISTANCE_BLOCKS / RIDER_SIDE_DODGE_DURATION_TICKS;

        boolean isLeft = this.getRandom().nextBoolean();
        if (threat != null) {
            Vec3 away = this.position().subtract(threat.position());
            double side = away.x * rightX + away.z * rightZ;
            if (Math.abs(side) > 0.05D) {
                isLeft = side > 0.0D;
            }
        }

        double dodgeDirX = rightX * (isLeft ? 1 : -1);
        double dodgeDirZ = rightZ * (isLeft ? 1 : -1);
        beginRiderSideDodge(new Vec3(dodgeDirX * perTickSpeed, 0.0D, dodgeDirZ * perTickSpeed));
        riderBackDashCooldownTicks = RIDER_BACK_DASH_COOLDOWN_TICKS;
        aiGroundMobilityCooldownTicks = 20;
        triggerAnim(VolitansAnimationHandler.MOVEMENT_CONTROLLER, isLeft ? "dodge_left" : "dodge_right");
        playDodgeSound();
        return true;
    }

    private boolean tryReactiveGroundBackstep(@Nullable LivingEntity threat) {
        if (isAerial() || isInWaterOrBubble() || isBurrowing() || (isTamingStunned() && !isTame())) {
            return false;
        }
        if (isGroundMobilityActive() || aiGroundMobilityCooldownTicks > 0 || riderBackDashCooldownTicks > 0 || areRiderControlsLocked()) {
            return false;
        }

        if (threat != null) {
            double dx = threat.getX() - this.getX();
            double dz = threat.getZ() - this.getZ();
            if (dx * dx + dz * dz > 1.0E-4D) {
                float targetYaw = (float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
                this.setYRot(targetYaw);
                this.yBodyRot = targetYaw;
                this.yHeadRot = targetYaw;
            }
        }

        Vec3 forward = getRiderDashForwardVector();
        Vec3 backward = new Vec3(-forward.x, 0.0D, -forward.z).normalize();
        double perTickSpeed = DragonMotionMath.speedForIntegratedDistance(
                RIDER_BACK_DASH_DISTANCE_BLOCKS,
                RIDER_BACK_DASH_HORIZONTAL_DRAG,
                RIDER_BACK_DASH_DURATION_TICKS
        );
        Vec3 launch = new Vec3(backward.x * perTickSpeed, 0.0D, backward.z * perTickSpeed);
        beginRiderBackDash(launch);
        riderBackDashCooldownTicks = RIDER_BACK_DASH_COOLDOWN_TICKS;
        aiGroundMobilityCooldownTicks = 26;
        triggerAnim(VolitansAnimationHandler.MOVEMENT_CONTROLLER, "dash_backwards");
        playBackwardDashSound();
        return true;
    }

    private void tickBankingLogic() {
        DragonFlightVisuals.tickBanking(
                this.flightVisualState,
                isFlying() && !isLanding(),
                this.horizontalCollision,
                this.verticalCollision,
                this.getYRot(),
                this.yRotO
        );
    }

    private Vec3 getRiderDashForwardVector() {
        LivingEntity rider = getControllingPassenger();
        if (rider != null) {
            Vec3 riderLook = rider.getLookAngle();
            if (riderLook.lengthSqr() > 1.0E-6D) {
                return riderLook.normalize();
            }
        }
        Vec3 look = getLookAngle();
        if (look.lengthSqr() > 1.0E-6D) {
            return look.normalize();
        }
        float yawRad = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad)).normalize();
    }

    private void setRiderNudgeState(int ticks, Vec3 velocity, double horizontalDrag) {
        this.entityData.set(DATA_RIDER_NUDGE_TICKS, Math.max(1, ticks));
        this.entityData.set(DATA_RIDER_NUDGE_X, (float) velocity.x);
        this.entityData.set(DATA_RIDER_NUDGE_Z, (float) velocity.z);
        this.entityData.set(DATA_RIDER_NUDGE_DRAG, (float) horizontalDrag);
    }

    private Vec3 getRiderNudgeVelocity() {
        return new Vec3(
                this.entityData.get(DATA_RIDER_NUDGE_X),
                0.0D,
                this.entityData.get(DATA_RIDER_NUDGE_Z)
        );
    }

    private void setRiderNudgeVelocity(Vec3 velocity) {
        this.entityData.set(DATA_RIDER_NUDGE_X, (float) velocity.x);
        this.entityData.set(DATA_RIDER_NUDGE_Z, (float) velocity.z);
    }

    private void clearRiderNudgeState() {
        this.entityData.set(DATA_RIDER_NUDGE_TICKS, 0);
        this.entityData.set(DATA_RIDER_NUDGE_X, 0.0F);
        this.entityData.set(DATA_RIDER_NUDGE_Z, 0.0F);
        this.entityData.set(DATA_RIDER_NUDGE_DRAG, 1.0F);
        this.entityData.set(DATA_RIDER_NUDGE_MODE, RIDER_NUDGE_NONE);
    }

    private boolean isRiderForwardDashing() {
        return this.entityData.get(DATA_RIDER_NUDGE_MODE) == RIDER_NUDGE_FORWARD_DASH;
    }

    private boolean isRiderBackDashing() {
        return this.entityData.get(DATA_RIDER_NUDGE_MODE) == RIDER_NUDGE_BACK_DASH;
    }

    private boolean isRiderSideDodging() {
        return this.entityData.get(DATA_RIDER_NUDGE_MODE) == RIDER_NUDGE_SIDE_DODGE;
    }

    private void beginRiderBackDash(Vec3 vec) {
        blockTakeoffInput(RIDER_BACK_DASH_DURATION_TICKS + RIDER_BACK_DASH_RECOVERY_TICKS);
        this.riderGroundNudge.cancelActive();
        this.riderForwardDashDamageApplied = false;
        this.riderBackDashRecoveryTicks = 0;
        this.riderSideDodgeVec = Vec3.ZERO;
        this.riderSideDodgeRecoveryTicks = 0;
        this.riderBackDashSpikeDelayTicks = RIDER_BACK_DASH_SPIKE_DELAY_TICKS;
        setRiderNudgeSteerOffset(180.0F);
        if (this.riderGroundNudge.startDash(
                vec,
                RIDER_BACK_DASH_DURATION_TICKS,
                0,
                RIDER_BACK_DASH_HORIZONTAL_DRAG
        )) {
            this.entityData.set(DATA_RIDER_NUDGE_MODE, RIDER_NUDGE_BACK_DASH);
        }
    }

    private void tickBackwardDashSpikes() {
        if (level().isClientSide || riderBackDashSpikeDelayTicks <= 0) {
            return;
        }
        if (--riderBackDashSpikeDelayTicks == 0) {
            fireBackwardDashSpikes();
        }
    }

    private void handleRiderBackDashRecoveryMovement() {
        riderBackDashVec = steerRiderGroundBurstVector(riderBackDashVec);
        double yVel = getGroundBurstVerticalVelocity();
        double horizontalX = riderBackDashVec.x;
        double horizontalZ = riderBackDashVec.z;
        if (!this.onGround()) {
            horizontalX *= 0.90D;
            horizontalZ *= 0.90D;
        }
        this.setDeltaMovement(horizontalX, yVel, horizontalZ);
        this.hasImpulse = true;
        riderBackDashVec = riderBackDashVec.multiply(
                RIDER_BACK_DASH_RECOVERY_DRAG,
                RIDER_BACK_DASH_VERTICAL_DRAG,
                RIDER_BACK_DASH_RECOVERY_DRAG
        );
        if (--riderBackDashRecoveryTicks <= 0) {
            riderBackDashVec = Vec3.ZERO;
            setRiderNudgeSteerOffset(0.0F);
        }
    }

    private void beginRiderSideDodge(Vec3 vec) {
        blockTakeoffInput(RIDER_SIDE_DODGE_DURATION_TICKS + RIDER_SIDE_DODGE_RECOVERY_TICKS);
        this.riderGroundNudge.cancelActive();
        this.riderForwardDashDamageApplied = false;
        this.riderBackDashRecoveryTicks = 0;
        this.riderBackDashVec = Vec3.ZERO;
        this.riderBackDashSpikeDelayTicks = 0;
        this.riderSideDodgeRecoveryTicks = 0;
        this.riderGroundNudge.startDodge(vec, RIDER_SIDE_DODGE_DURATION_TICKS);
    }

    private void handleRiderSideDodgeRecoveryMovement() {
        riderSideDodgeVec = steerRiderGroundBurstVector(riderSideDodgeVec);
        double yVel = getGroundBurstVerticalVelocity();
        double horizontalX = riderSideDodgeVec.x;
        double horizontalZ = riderSideDodgeVec.z;
        if (!this.onGround()) {
            horizontalX *= 0.88D;
            horizontalZ *= 0.88D;
        }
        this.setDeltaMovement(horizontalX, yVel, horizontalZ);
        this.hasImpulse = true;
        riderSideDodgeVec = riderSideDodgeVec.multiply(
                RIDER_SIDE_DODGE_RECOVERY_DRAG,
                RIDER_SIDE_DODGE_VERTICAL_DRAG,
                RIDER_SIDE_DODGE_RECOVERY_DRAG
        );
        if (--riderSideDodgeRecoveryTicks <= 0) {
            riderSideDodgeVec = Vec3.ZERO;
            setRiderNudgeSteerOffset(0.0F);
        }
    }

    private void beginRiderForwardDash(Vec3 vec) {
        blockTakeoffInput(RIDER_FORWARD_DASH_DURATION_TICKS);
        this.riderGroundNudge.startDash(
                vec,
                RIDER_FORWARD_DASH_DURATION_TICKS,
                0,
                RIDER_FORWARD_DASH_HORIZONTAL_DRAG
        );
        setRiderNudgeSteerOffset(0.0F);
        this.riderForwardDashDamageApplied = false;

        this.riderBackDashRecoveryTicks = 0;
        this.riderBackDashVec = Vec3.ZERO;
        this.riderBackDashSpikeDelayTicks = 0;
        this.riderSideDodgeRecoveryTicks = 0;
        this.riderSideDodgeVec = Vec3.ZERO;
    }

    private void setRiderNudgeSteerOffset(float offsetDegrees) {
        this.entityData.set(DATA_RIDER_NUDGE_STEER_OFFSET, offsetDegrees);
    }

    private Vec3 riderGroundBurstSteeringDirection() {
        if (getControllingPassenger() instanceof Player) {
            return DragonMotionMath.horizontalRelative(
                    this.getYRot(),
                    this.entityData.get(DATA_RIDER_NUDGE_STEER_OFFSET)
            );
        }
        return DragonMotionMath.horizontalForward(this.getYRot()).scale(-1.0D);
    }

    private Vec3 steerRiderGroundBurstVector(Vec3 velocity) {
        if (!(getControllingPassenger() instanceof Player)) {
            return velocity;
        }
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed < 1.0E-6D) {
            return velocity;
        }
        Vec3 direction = riderGroundBurstSteeringDirection();
        return new Vec3(direction.x * horizontalSpeed, velocity.y, direction.z * horizontalSpeed);
    }

    private void clearGroundMobilityState() {
        this.riderGroundNudge.cancelActive();
        setRiderNudgeSteerOffset(0.0F);
        this.riderForwardDashDamageApplied = false;

        this.riderBackDashRecoveryTicks = 0;
        this.riderBackDashVec = Vec3.ZERO;
        this.riderBackDashSpikeDelayTicks = 0;

        this.riderSideDodgeRecoveryTicks = 0;
        this.riderSideDodgeVec = Vec3.ZERO;
    }

    private double getGroundBurstVerticalVelocity() {
        double yVel = this.getDeltaMovement().y;
        if (!this.onGround()) {
            yVel = Math.min((yVel - 0.22D) * 0.98D, -0.45D);
        } else {
            yVel = Math.max(0.0D, yVel);
        }
        return yVel;
    }

    private void tickRiderForwardDashDamage() {
        if (riderForwardDashDamageApplied || riderGroundNudge.getElapsedTicks() < RIDER_FORWARD_DASH_DAMAGE_TICK
                || level().isClientSide || !this.isVehicle()) {
            return;
        }
        riderForwardDashDamageApplied = true;
        Vec3 center = this.getBoundingBox().getCenter().add(getLookAngle().normalize().scale(this.getBbWidth() * 0.75D));
        AABB box = new AABB(center, center).inflate(RIDER_FORWARD_DASH_DAMAGE_RADIUS);
        for (LivingEntity target : level().getEntitiesOfClass(
                LivingEntity.class,
                box,
                entity -> entity != this
                        && entity != this.getControllingPassenger()
                        && entity.isAlive()
                        && entity.attackable()
                        && !this.isAlly(entity))) {
            target.hurt(this.damageSources().mobAttack(this), RIDER_FORWARD_DASH_DAMAGE);
        }
    }

    private void playForwardDashSound() {
        if (isBaby()) {
            return;
        }
        if (this.level().isClientSide) {
            return;
        }
        this.getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_DASH_FORWARD.get(), 1.6f, 1.0f, RIDER_DASH_SOUND_TICKS);
    }

    private void playBackwardDashSound() {
        if (isBaby()) {
            return;
        }
        if (this.level().isClientSide) {
            return;
        }
        this.getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_DASH_BACKWARDS.get(), 1.6f, 1.0f, RIDER_DASH_SOUND_TICKS);
    }

    private void playDodgeSound() {
        if (isBaby()) {
            return;
        }
        if (this.level().isClientSide) {
            return;
        }
        this.getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_DODGE.get(), 1.6f, 1.0f, RIDER_DODGE_SOUND_TICKS);
    }

    private void fireBackwardDashSpikes() {
        if (level().isClientSide) {
            return;
        }

        Vec3 muzzle = getBreathOrigin();
        Vec3 dir = getDragonHorizontalForwardVector();
        Vec3 right = dir.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            float yawRad = getYRot() * Mth.DEG_TO_RAD;
            right = new Vec3(Mth.cos(yawRad), 0.0D, Mth.sin(yawRad));
        } else {
            right = right.normalize();
        }

        for (int i = 0; i < RIDER_BACK_DASH_SPIKE_COUNT; i++) {
            double spread = (i - 1) * 0.24D;
            Vec3 shootDir = dir.add(right.scale(spread)).normalize();
            Vec3 origin = muzzle.add(shootDir.scale(Math.max(1.2D, getBbWidth() * 0.7D)))
                    .add(0.0D, RIDER_BACK_DASH_SPIKE_Y_OFFSET, 0.0D);
            VolitansSpineEntity spine = new VolitansSpineEntity(level(), this);
            spine.setPos(origin.x, origin.y, origin.z);
            spine.setImpactEffects(
                    RIDER_BACK_DASH_SPIKE_DAMAGE,
                    RIDER_BACK_DASH_SPIKE_POISON_DURATION_TICKS,
                    RIDER_BACK_DASH_SPIKE_POISON_AMPLIFIER
            );
            spine.setNoGravity(true);
            spine.shoot(shootDir.x, shootDir.y, shootDir.z, RIDER_BACK_DASH_SPIKE_SPEED, RIDER_BACK_DASH_SPIKE_INACCURACY);
            spine.pickup = AbstractArrow.Pickup.DISALLOWED;
            level().addFreshEntity(spine);
        }
    }

    private Vec3 getDragonHorizontalForwardVector() {
        float yawRad = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad)).normalize();
    }

    @Override
    protected DragonFlightVisuals.State getFlightVisualState() {
        return this.flightVisualState;
    }

    @Override
    protected EntityDataAccessor<Float> getFlightPitchAccessor() {
        return DATA_FLIGHT_PITCH;
    }

    @Override
    protected EntityDataAccessor<Float> getAccumulatedRollAccessor() {
        return DATA_ACCUMULATED_ROLL;
    }

    // remove this and voli ain't pitching right
    private void tickPitchingLogic() {
        prevFlightPitchRad = flightPitchRad;
        if (level().isClientSide) {
            flightPitchRad = this.entityData.get(DATA_FLIGHT_PITCH);
            return;
        }

        boolean inWater = this.isInWaterOrBubble();
        if (!isFlying() && !isLanding() && !inWater) {
            flightPitchRad = 0f;
            smoothedPlayerPitchRad = 0f;
            verticalKeyPitchSmoothing = false;
            this.entityData.set(DATA_FLIGHT_PITCH, 0f);
            return;
        }

        float targetPitchRad = 0f;
        boolean slowVerticalKeyPitch = false;
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            float riderForward = this.entityData.get(DATA_RIDER_FORWARD);
            float riderStrafe = this.entityData.get(DATA_RIDER_STRAFE);
            boolean hasMovementInput = Math.abs(riderForward) > 0.01f
                    || Math.abs(riderStrafe) > 0.01f
                    || Math.abs(player.zza) > 0.01f
                    || Math.abs(player.xxa) > 0.01f;
            boolean verticalKeyPitch = isGoingUp() != isGoingDown();
            boolean verticalKeyPitchRelease = verticalKeyPitchSmoothing
                    && !verticalKeyPitch
                    && !isRiderPitchKeyMode();
            slowVerticalKeyPitch = verticalKeyPitch || verticalKeyPitchRelease;
            float rawRiderPitchRad = DragonRiderControllerHelper.resolveRiderPitchRadians(
                    this,
                    player,
                    25.0F
            );
            if (rawRiderPitchRad != 0.0F || isRiderPitchKeyMode() || hasMovementInput || verticalKeyPitch || verticalKeyPitchRelease) {
                float memory = slowVerticalKeyPitch ? 0.82f : 0.65f;
                float blend = slowVerticalKeyPitch ? 0.18f : 0.35f;
                smoothedPlayerPitchRad = smoothedPlayerPitchRad * memory + rawRiderPitchRad * blend;
                targetPitchRad = Mth.clamp(smoothedPlayerPitchRad, -Mth.HALF_PI, Mth.HALF_PI);
            } else {
                smoothedPlayerPitchRad = 0f;
                verticalKeyPitchSmoothing = false;
            }
            if (isLanding()) {
                targetPitchRad = 35.0F * Mth.DEG_TO_RAD;
            }
        } else {
            Vec3 velocity = getDeltaMovement();
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            double minPitchSpeed = inWater ? 0.05D : 0.15D;
            if (horizontalSpeed > minPitchSpeed) {
                targetPitchRad = (float) Math.atan2(-velocity.y, horizontalSpeed);
                targetPitchRad = Mth.clamp(targetPitchRad, -Mth.HALF_PI, Mth.HALF_PI);
            }
            if (isLanding()) {
                float landingPitchRad = getStandardAiLandingPitchDegrees() * Mth.DEG_TO_RAD;
                targetPitchRad = Math.max(targetPitchRad, landingPitchRad);
            }
        }

        boolean verticalKeyPitch = isVehicle() && isGoingUp() != isGoingDown();
        flightPitchRad = Mth.lerp(slowVerticalKeyPitch ? 0.18f : 0.35f, flightPitchRad, targetPitchRad);
        verticalKeyPitchSmoothing = verticalKeyPitch
                || (isVehicle()
                        && verticalKeyPitchSmoothing
                        && !isGoingUp()
                        && !isGoingDown()
                        && Math.abs(flightPitchRad - targetPitchRad) > 0.01f);
        if (Math.abs(flightPitchRad) < 0.001f) {
            flightPitchRad = 0f;
        }
        this.entityData.set(DATA_FLIGHT_PITCH, flightPitchRad);
    }

    @Override
    public float getFlightPitchRadians(float partialTick) {
        return Mth.lerp(partialTick, prevFlightPitchRad, flightPitchRad);
    }

    @Override
    protected boolean canUseBarrelRoll() {
        return isFlying()
                && !isInWaterOrBubble()
                && !areRiderControlsLocked()
                && !isGroundMobilityActive()
                && !isRiderForwardDashing()
                && !isRiderBackDashing()
                && riderBackDashRecoveryTicks <= 0
                && !isRiderSideDodging()
                && riderSideDodgeRecoveryTicks <= 0;
    }

    @Override
    protected boolean shouldEaseAirAutoAlign() {
        if (!isFlying() || isInWaterOrBubble() || areRiderControlsLocked()) {
            return false;
        }

        return !(Math.abs(this.entityData.get(DATA_RIDER_STRAFE)) > 0.05f);
    }

    @Override
    protected boolean isActivelyBarrelRolling() {
        return isFlying()
                && !isInWaterOrBubble()
                && !areRiderControlsLocked()
                && !isGroundMobilityActive()
                && !isRiderForwardDashing()
                && !isRiderBackDashing()
                && riderBackDashRecoveryTicks <= 0
                && !isRiderSideDodging()
                && riderSideDodgeRecoveryTicks <= 0
                && this.entityData.get(DATA_RIDER_FORWARD) > 0.1f
                && Math.abs(this.entityData.get(DATA_RIDER_STRAFE)) > 0.1f;
    }

    @Override
    protected boolean isBarrelRollLandingBlendActive() {
        return isLanding();
    }

    @Override
    protected double getBarrelRollAltitudeAboveTerrain() {
        return getAltitudeAboveCollisionTerrain(16, true);
    }

    private void tickScreenShake() {
        screenShakeComponent.tick();
    }

    private void tickBurrowRumbleShake() {
        if (!isBurrowing()) {
            return;
        }
        boolean moving = Math.abs(this.entityData.get(DATA_RIDER_FORWARD)) > 0.03F
                || Math.abs(this.entityData.get(DATA_RIDER_STRAFE)) > 0.03F
                || this.getDeltaMovement().horizontalDistanceSqr() > 0.0016D;
        float target = moving ? BURROW_MOVE_SHAKE_INTENSITY : 0.0F;
        screenShakeComponent.force(target);
    }

    private boolean shouldSuppressTakeoffInput() {
        var activeAbility = combatManager.getActiveAbility();
        boolean activeAbilityBlocksTakeoff = activeAbility != null
                && activeAbility.getAbilityType() != ModAbilities.VOLITANS_POISON_BALL;
        return isBurrowing()
                || takeoffInputBlockTicks > 0
                || isRiderForwardDashing()
                || isRiderBackDashing()
                || riderBackDashRecoveryTicks > 0
                || isRiderSideDodging()
                || riderSideDodgeRecoveryTicks > 0
                || areRiderControlsLocked()
                || isInSitTransition()
                || activeAbilityBlocksTakeoff;
    }

    @Override
    public boolean isWildAggressionEnabled() {
        if (isTame() || isBaby()) {
            return false;
        }
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.VOLITANS_ID);
        return config.extraBoolean("aggressive_wild", true);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (isTamingStunned() && target != null) {
            return;
        }
        if (isBaby()) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    private void tickRiderLandingBlendTimer() {
        tickStandardRiderLandingBlend(new RiderLandingBlendHooks() {
            @Override
            public boolean skipGenericLandingHandling() {
                return isUltimateSlamActive();
            }

            @Override
            public double touchdownVelocity() {
                return 0.25D;
            }

            @Override
            public boolean shouldClearFlightStateInWater() {
                return shouldClearRiderFlightStateInWater();
            }

            @Override
            public void onWaterFlightCleared() {
                completeTouchdownLanding(LandingSource.RIDER);
            }

            @Override
            public boolean isCompletedLanding() {
                return isLanding() && onGround();
            }

            @Override
            public boolean shouldStartLandingBlend() {
                return !isLanding() && shouldTriggerLandingBlend();
            }

            @Override
            public void startLandingBlend() {
                setLanding(true);
            }

            @Override
            public void onRiderLanded() {
                triggerAnim(AnimationHelper.MOVEMENT_CONTROLLER, AnimationHelper.LANDED);
                if (!isBaby()) {
                    getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_LANDED.get(), 2.0f, 1.0f, 32);
                }
                lockRiderControls(LANDED_CONTROL_LOCK_TICKS);
                completeTouchdownLanding(LandingSource.RIDER);
            }
        });
    }

    private boolean shouldTriggerLandingBlend() {
        if (!isFlying() || !isVehicle() || isTakeoff() || isLanding()) {
            return false;
        }

        if (isInWaterOrBubble()) {
            return false;
        }

        Vec3 velocity = getDeltaMovement();
        if (velocity.y >= -0.02D) {
            return false;
        }

        return getAltitudeAboveCollisionTerrain(16, true) <= LANDING_BLEND_ALTITUDE;
    }

    public void applyConfiguredAttributes() {
        var config = getConfiguredDragonAttributes();
        applyConfiguredFlyingHealthAndArmor(config, BABY_MAX_HEALTH, BABY_ARMOR);
        setAttributeBase(Attributes.MOVEMENT_SPEED, 0.30D);
        clampHealthToMax();
    }

    private void handleAmbientSounds() {
        if (isBaby()
                || isDying()
                || isSleeping()
                || isSleepTransitioning()
                || isOrderedToSit()
                || isInSitTransition()
                || areRiderControlsLocked()) {
            return;
        }
        if (getTarget() != null || getActiveAbility() != null || isBreathing() || isBurrowing()) {
            return;
        }
        if (isVehicle() || isTakeoff() || isLanding() || isHovering()) {
            return;
        }

        tickAmbientVocalSounds(MIN_AMBIENT_DELAY, MAX_AMBIENT_DELAY, this::selectAmbientGrumble);
    }

    private String selectAmbientGrumble() {
        float roll = this.getRandom().nextFloat();
        return roll < 0.34f ? "grumble1" : (roll < 0.67f ? "grumble2" : "grumble3");
    }

    public void playEatMovingSound() {
        if (level().isClientSide) {
            return;
        }
        float pitch = isBaby() ? 1.6f : 1.0f;
        getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_EAT.get(), 1.0f, pitch, EAT_SOUND_DURATION_TICKS);
    }

    @Override
    public float maxSitTicks() {
        return getSitDownAnimationTicks();
    }

    private int getSitDownAnimationTicks() {
        return 50;
    }

    private int getSitUpAnimationTicks() {
        return 25;
    }

    private int getFallAsleepAnimationTicks() {
        return 51;
    }

    private int getWakeUpAnimationTicks() {
        return 43;
    }

    @Override
    public boolean supportsSleep() {
        return true;
    }

    @Override
    public DragonEntity.DragonSleepPreferences getSleepPreferences() {
        return DragonEntity.DragonSleepPreferences.DIURNAL();
    }

    @Override
    public boolean canSleepNow() {
        boolean basicAllowed = canSeekSleepDepthNow();
        if (!basicAllowed) {
            return false;
        }
        if (isInWaterOrBubble()) {
            return isDeepEnoughForUnderwaterSleep();
        }
        return true;
    }

    public boolean canSeekSleepDepthNow() {
        return !isVehicle()
                && !isBurrowing()
                && !isFlying()
                && !isHovering()
                && !isTakeoff()
                && !isLanding()
                && !isBreathing()
                && getActiveAbility() == null
                && (isSleeping() || isSleepTransitioning() || tickCount >= SLEEP_AFTER_SPAWN_GRACE_TICKS);
    }

    @Override
    public boolean canSleepInWater() {
        return true;
    }

    public boolean shouldSeekUnderwaterSleepDepth() {
        return !isTame()
                && wantsToSleep()
                && !isSleeping()
                && !isSleepTransitioning()
                && !isSleepSuppressed()
                && getTarget() == null
                && canSeekSleepDepthNow()
                && isInWaterOrBubble()
                && !isDeepEnoughForUnderwaterSleep();
    }

    private boolean isDeepEnoughForUnderwaterSleep() {
        if (!isUnderWater()) {
            return false;
        }

        double bodyTopY = getBoundingBox().maxY;
        double surfaceY = getWaterSurfaceYAbove(getX(), getZ(), bodyTopY);
        return surfaceY - bodyTopY >= MIN_UNDERWATER_SLEEP_SURFACE_CLEARANCE;
    }

    public boolean isDeepEnoughForUnderwaterSleepAt(Vec3 position) {
        double bodyTopY = position.y + getBbHeight();
        double surfaceY = getWaterSurfaceYAbove(position.x, position.z, bodyTopY);
        return surfaceY - bodyTopY >= MIN_UNDERWATER_SLEEP_SURFACE_CLEARANCE;
    }

    private double getWaterSurfaceYAbove(double positionX, double positionZ, double fromY) {
        int x = Mth.floor(positionX);
        int z = Mth.floor(positionZ);
        int startY = Mth.floor(fromY);
        int maxY = Math.min(level().getMaxBuildHeight() - 1, startY + WATER_SURFACE_SLEEP_SCAN_BLOCKS);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, startY, z);

        for (int y = startY; y <= maxY; y++) {
            cursor.setY(y);
            if (!level().getFluidState(cursor).is(FluidTags.WATER)) {
                return y;
            }
        }

        return Double.POSITIVE_INFINITY;
    }

    @Override
    protected boolean useSleepSitDownTimer() {
        return !isInWaterOrBubble();
    }

    @Override
    protected boolean requireSeatedBeforeFallAsleep() {
        return !isInWaterOrBubble();
    }

    @Override
    protected boolean sleepForceSitDownOnEnter() {
        return !isInWaterOrBubble();
    }

    @Override
    protected boolean useSleepSitUpAfterWake() {
        return !isInWaterOrBubble();
    }

    @Override
    protected int getSleepSitDownDuration() {
        return getSitDownAnimationTicks() + 1;
    }

    @Override
    protected int getSleepFallAsleepDuration() {
        return getFallAsleepAnimationTicks();
    }

    @Override
    protected int getSleepWakeUpDuration() {
        return getWakeUpAnimationTicks();
    }

    @Override
    protected int getSleepSitUpDuration() {
        return getSitUpAnimationTicks() + 1;
    }

    @Override
    protected int getSleepExitSuppressionTicks() {
        return SLEEP_EXIT_SUPPRESSION_TICKS;
    }

    @Override
    protected int getSleepWakeUpSuppressionTicks() {
        return SLEEP_WAKE_SUPPRESSION_TICKS;
    }

    @Override
    protected boolean isAlreadySeatedForSleep() {
        return isInWaterOrBubble() || getSitProgress() >= maxSitTicks() || isOrderedToSit() || getCommand() == 1;
    }

    @Override
    protected boolean shouldStaySeatedAfterWake(int sleepCommandSnapshot) {
        return !isInWaterOrBubble() && sleepCommandSnapshot == 1;
    }

    @Override
    protected void onSleepLockCommand(int snapshot) {
        sleepLockedYaw = getYRot();
        sleepLockedPitch = getXRot();
        if (getCommand() != 1) {
            setCommand(1);
            setOrderedToSit(true);
        }
        getNavigation().stop();
        setTarget(null);
        setRunning(false);
        setGroundMoveStateFromAI(0);
        setDeltaMovement(Vec3.ZERO);
        clearAerialStateForInterrupt();
    }

    @Override
    protected void onSleepUnlockCommand(int desired) {
        if (desired >= 0 && desired != getCommand()) {
            setCommand(desired);
            setOrderedToSit(desired == 1 && !isInWaterOrBubble());
        } else if (isInWaterOrBubble()) {
            setOrderedToSit(false);
        }
        getNavigation().stop();
        setRunning(false);
        setGroundMoveStateFromAI(0);
    }

    @Override
    protected void onSleepFreezeTick() {
        super.onSleepFreezeTick();
        setYRot(sleepLockedYaw);
        yRotO = sleepLockedYaw;
        yBodyRot = sleepLockedYaw;
        yBodyRotO = sleepLockedYaw;
        yHeadRot = sleepLockedYaw;
        yHeadRotO = sleepLockedYaw;
        setXRot(sleepLockedPitch);
        xRotO = sleepLockedPitch;
    }

    @Override
    protected void onSleepSitDownAnimation() {
        if (!isInWaterOrBubble()) {
            animationHandler.triggerSitDownAnimation();
            setOrderedToSit(true);
        }
    }

    @Override
    protected void onSleepFallAsleepAnimation() {
        if (isInWaterOrBubble()) {
            AnimationHelper.triggerRestAnimation(this, "fall_asleep_underwater");
        } else {
            AnimationHelper.triggerRestAnimation(this, AnimationHelper.FALL_ASLEEP);
        }
    }

    @Override
    protected void onSleepLoopAnimation() {
        if (!isInWaterOrBubble()) {
            setOrderedToSit(true);
        }
    }

    @Override
    protected void onSleepWakeUpAnimation() {
        if (isInWaterOrBubble()) {
            AnimationHelper.triggerRestAnimation(this, "wake_up_underwater");
            setOrderedToSit(false);
        } else {
            AnimationHelper.triggerRestAnimation(this, AnimationHelper.WAKE_UP);
            setOrderedToSit(true);
        }
    }

    @Override
    protected void onSleepSitUpAnimation() {
        if (!isInWaterOrBubble()) {
            animationHandler.triggerSitUpAnimation();
            setOrderedToSit(false);
        }
    }

    @Override
    protected void onSleepExitSeated() {
        if (!isInWaterOrBubble()) {
            setOrderedToSit(true);
            setSitProgress(maxSitTicks());
        } else {
            setOrderedToSit(false);
        }
    }

    @Override
    protected void onSleepExitStarted() {
        if (!isInWaterOrBubble()) {
            setOrderedToSit(true);
        }
    }

    private void updateSittingProgress() {
        if (level().isClientSide) {
            return;
        }

        if (this.isInWaterOrBubble()) {
            clearSitTransitionFlags();
            if (getSitProgress() != 0f || getPrevSitProgress() != 0f) {
                clearSitProgress();
            }
            return;
        }

        tickSitTransition(
                getSitDownAnimationTicks(),
                getSitUpAnimationTicks(),
                animationHandler::triggerSitDownAnimation,
                animationHandler::triggerSitUpAnimation
        );
    }

    @Override
    protected float getBodyTurnSpeed() {
        return 0.55F;
    }

    @Override
    protected double getCullingInflateX() {
        return 4.0D;
    }

    @Override
    protected double getCullingInflateY() {
        return 2.0D;
    }

    @Override
    protected double getCullingInflateZ() {
        return 4.0D;
    }
}
