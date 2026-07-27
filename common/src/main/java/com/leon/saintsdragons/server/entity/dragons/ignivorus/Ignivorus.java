package com.leon.saintsdragons.server.entity.dragons.ignivorus;

import com.mojang.serialization.Dynamic;
import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;

import com.leon.saintsdragons.util.animation.AnimationHelper;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.network.MessageDragonMeleeMode;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.ModBlocks;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.DragonAirCombatSettings;
import com.leon.saintsdragons.server.ai.DragonAirCombatSettingsProvider;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrain;
import com.leon.saintsdragons.server.ai.dragonbrain.profiles.IgnivorusBrain;
import com.leon.saintsdragons.server.entity.ability.DragonAimHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.base.DragonVariant;
import com.leon.saintsdragons.server.entity.base.DragonVariantSet;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.controller.ignivorus.IgnivorusRiderController;
import com.leon.saintsdragons.server.entity.effect.VisualFallingBlockEntity;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import com.leon.saintsdragons.server.flight.DragonFlightVisuals;
import com.leon.saintsdragons.server.flight.DragonRiderFlight;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireballAbility;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusSoundProfile;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusTamingHandler;
import com.leon.saintsdragons.server.entity.dragons.util.DragonGriefingRules;
import com.leon.saintsdragons.server.entity.component.DragonBreathComponent;
import com.leon.saintsdragons.server.entity.component.DragonForwardMovementComponent;
import com.leon.saintsdragons.server.entity.component.ScreenShakeComponent;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.interfaces.DrinkingDragon;
import com.leon.saintsdragons.server.entity.interfaces.PassiveTreeDestroyer;
import com.leon.saintsdragons.server.entity.interfaces.ShakesScreen;
import com.leon.saintsdragons.server.loot.DragonLootTables;
import com.leon.saintsdragons.server.world.DragonSpawnRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import javax.annotation.Nonnull;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Ignivorus extends RideableFlyingDragon implements ShakesScreen, DragonAirCombatSettingsProvider,
        PassiveTreeDestroyer, DrinkingDragon {
    private static final IgnivorusBrain DRAGON_BRAIN = new IgnivorusBrain();

    @Override
    protected ResourceLocation getDragonAttributesId() {
        return DragonAttributeConfigLoader.IGNIVORUS_ID;
    }
    public static final int VARIANT_DEFAULT = 0;
    public static final int VARIANT_CRIMSON = 1;
    private static final DragonVariantSet VARIANTS = DragonVariantSet.of(
            DragonVariant.of(VARIANT_DEFAULT, "default", 95),
            DragonVariant.of(VARIANT_CRIMSON, "crimson", 5)
    );
    public static final int TAKEOFF_ANIMATION_TICKS = 27;
    public static final int DRINKING_ANIMATION_TICKS = 140;
    public static final DragonAirCombatSettings AI_AIR_COMBAT_SETTINGS =
            new DragonAirCombatSettings(
                    TAKEOFF_ANIMATION_TICKS,
                    1.5D,
                    0,
                    64.0D,
                    2.5D,
                    8.0D,
                    5.0D
            );
    private static final int LANDED_RECOVERY_TICKS = 18;
    private static final int PHASE2_ENTER_LOCK_TICKS = 13;
    private static final int PHASE2_EXIT_LOCK_TICKS = 13;
    private static final int PHASE2_LANDED_RECOVERY_TICKS = 20;
    private static final int PHASE2_RIDER_TAKEOFF_TICKS = 60;
    private static final int PHASE2_RIDER_TAKEOFF_LAUNCH_DELAY_TICKS = 40;
    private static final double PHASE2_RIDER_TAKEOFF_UPWARD_STEP = 1.0D;
    private static final UUID PHASE2_FOLLOW_RANGE_MODIFIER_UUID =
            UUID.fromString("9bc2f319-58d7-47b7-84e8-bb0ed524030f");
    private static final AttributeModifier PHASE2_FOLLOW_RANGE_MODIFIER = new AttributeModifier(
            PHASE2_FOLLOW_RANGE_MODIFIER_UUID,
            "Ignivorus phase 2 follow range",
            32.0D,
            AttributeModifier.Operation.ADDITION
    );
    public static final EntityDataAccessor<Boolean> DATA_RIDER_LANDING_BLEND =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_BULLDOZING =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_PHASE2 =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PHASE2_RIDER_TAKEOFF =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_LEAPING =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> DATA_LEAP_ANIM_STATE =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LEAP_MOVE_TICKS =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_LEAP_MOVE_X =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LEAP_MOVE_Y =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LEAP_MOVE_Z =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Integer> DATA_FEEDING_COOLDOWN =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DATA_TAMING_STUNNED =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> DATA_FLIGHT_PITCH =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_ACCUMULATED_ROLL =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> DATA_PITCH_KEY_MODE =
            SynchedEntityData.defineId(Ignivorus.class, EntityDataSerializers.BOOLEAN);
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
    private static final float BARREL_ROLL_INPUT_SPEED = 0.235f;
    private static final int RIDER_LANDING_BLEND_DURATION = 5;
    public static final double BREED_PARTNER_RANGE = 20.0D;
    public static final double BREED_DISTANCE_SQR = 2500.0D;
    public static final double RIDER_WALK_SPEED = 0.225D;
    public static final double RIDER_RUN_SPEED = 0.4D;
    public static final double RIDER_BULLDOZE_SPEED = 0.55D;
    public static final float RIDER_KEY_PITCH_DEG = 25.0f;
    public static final double RIDER_PHASE2_WALK_SPEED = 0.15D;
    public static final double RIDER_PHASE2_RUN_SPEED = 0.32D;
    private static final float DEFAULT_MAX_UP_STEP = 1.5F;
    private static final float BULLDOZE_MAX_UP_STEP = 0;
    private static final double BULLDOZE_TUNNEL_REACH = 2.5D;
    private static final double BULLDOZE_HEAD_FORWARD_FALLBACK = 0.5D;
    private static final double BULLDOZE_TUNNEL_HALF_WIDTH = 7.0D;
    private static final int BULLDOZE_TUNNEL_HEIGHT = 7;
    private static final int BULLDOZE_TUNNEL_MAX_BREAKS_PER_TICK = 60;
    private static final double BULLDOZE_BODY_COLLISION_REACH = 8.0D;
    private static final double BULLDOZE_DAMAGE_FORWARD_REACH = 4.5D;
    private static final double BULLDOZE_DAMAGE_HALF_WIDTH = 7.0D;
    private static final double BULLDOZE_DAMAGE_HALF_HEIGHT = 2.5D;
    private static final int BULLDOZE_ENTER_TICKS = 25;
    private static final float MAX_FIRE_YAW_DEG = 70.0F;
    private static final float MAX_FIRE_PITCH_DEG = 55.0F;
    private static final double LEAP_ARC_FORWARD_DISTANCE = 42.0D;
    private static final double LEAP_ARC_HEIGHT = 15.0D;
    private static final int LEAP_ARC_ASCENT_TICKS = 20;
    private static final int LEAP_ARC_DESCENT_TICKS = 10;
    private static final int LEAP_ARC_DURATION_TICKS = LEAP_ARC_ASCENT_TICKS + LEAP_ARC_DESCENT_TICKS;
    private static final float LEAP_SLAM_DAMAGE = 50.0F;
    private static final float DEFAULT_BULLDOZE_DAMAGE = 10.0F;
    private static final double LEAP_SLAM_RADIUS = 20.0D;
    private static final double LEAP_KNOCKBACK = 5.5D;
    private static final double LEAP_LIFT = 0.8D;
    private static final double LEAP_IMPACT_TRIGGER_HEIGHT = 7.0D;
    private static final int LEAP_GROUNDED_FAILSAFE_TICKS = 6;
    private static final int LEAP_COOLDOWN_TICKS = 140;
    private static final int LEAP_WINDUP_TICKS = 20;
    private static final int LEAP_STATE_NONE = 0;
    private static final int LEAP_STATE_TAKEOFF = 1;
    private static final int LEAP_IMPACT_RECOVERY_DURATION = 18;
    private static final int FLEX_CONTROL_LOCK_TICKS = 170;
    private static final int FLEX_COOLDOWN_TICKS = 200;
    private static final float SHAKE_DECAY_PER_TICK = 0.025F;
    private static final double BABY_MAX_HEALTH = 90.0D;
    private static final double BABY_ARMOR = 0.0D;
    private static final float BABY_HITBOX_SCALE = 0.55F;
    private static final Map<String, VocalEntry> VOCAL_ENTRIES =
            new DragonEntity.VocalEntryBuilder()
                    .add("ignivorus_roar", IgnivorusAnimationHandler.ACTION_CONTROLLER, "animation.ignivorus.roar",
                            ModSounds.IGNIVORUS_ROAR, 1.8f, 0.85f, 0.15f,
                            false, false, false)
                    .add("ignivorus_flex", IgnivorusAnimationHandler.MOVEMENT_CONTROLLER, "animation.ignivorus.flex",
                            ModSounds.IGNIVORUS_FLEX, 2.0f, 0.95f, 0.05f,
                            false, false, true)
                    .add("ignivorus_grumble1", AnimationHelper.VOCAL_CONTROLLER, "animation.ignivorus.grumble1",
                            ModSounds.IGNIVORUS_GRUMBLE_1, 1.1f, 0.95f, 0.08f,
                            true, false, true)
                    .add("ignivorus_grumble2", AnimationHelper.VOCAL_CONTROLLER, "animation.ignivorus.grumble2",
                            ModSounds.IGNIVORUS_GRUMBLE_2, 1.15f, 1.0f, 0.08f,
                            true, false, true)
                    .add("ignivorus_grumble3", AnimationHelper.VOCAL_CONTROLLER, "animation.ignivorus.grumble3",
                            ModSounds.IGNIVORUS_GRUMBLE_3, 1.2f, 0.9f, 0.08f,
                            true, false, true)
                    .add("ignivorus_hurt", AnimationHelper.INTERACTION_CONTROLLER, "animation.ignivorus.hurt",
                            ModSounds.IGNIVORUS_HURT, 1.6f, 0.95f, 0.1f,
                            true, true, true)
                    .add("ignivorus_die", AnimationHelper.INTERACTION_CONTROLLER, "animation.ignivorus.die",
                            ModSounds.IGNIVORUS_DIE, 1.8f, 0.9f, 0.05f,
                            false, true, true)
                    .build();

    public AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);
    private final IgnivorusAnimationHandler animationHandler = new IgnivorusAnimationHandler(this);
    private final IgnivorusRiderController riderController;
    private final AnimationController<Ignivorus> movementController;
    private final AnimationController<Ignivorus> actionController;
    private final AnimationController<Ignivorus> fastActionController;
    private final AnimationController<Ignivorus> flightController;
    private final AnimationController<Ignivorus> vocalController;
    private final AnimationController<Ignivorus> interactionController;
    private final IgnivorusInteractionHandler interactionHandler = new IgnivorusInteractionHandler(this);
    private final IgnivorusTamingHandler tamingController = new IgnivorusTamingHandler(this);
    public int timeFlying = 0;
    private int airTicks;
    public int groundTicks;
    private Vec3 fireAimDir;
    private int fireTime = 0;
    private Vec3 fireServerTarget = null;
    private final DragonFlightVisuals.State flightVisualState = new DragonFlightVisuals.State();
    private boolean bulldozing = false;
    private int bulldozeCooldownTicks = 0;
    private final Map<Integer, Integer> bulldozeHitCooldowns = new HashMap<>();
    private boolean bulldozeWasVehicle = false;
    private boolean phase2Active = false;
    private int phase2CooldownTicks = 0;
    private boolean phase2RiderTakeoffActive = false;
    private boolean useRightWingSwipe = true;
    private boolean phase2WasVehicle = false;
    private boolean wildPhase2UltimateTriggered = false;
    private boolean aiSpecialCombatActive = false;
    private boolean leaping = false;
    private boolean leapWasVehicle = false;
    private int leapAnimState = LEAP_STATE_NONE;
    private Vec3 leapVelocity = Vec3.ZERO;
    private Vec3 leapArcDirection = Vec3.ZERO;
    private int leapArcTick = 0;
    private int leapCooldownTicks = 0;
    private int leapWindupTicks = 0;
    private int leapImpactRecoveryTicks = 0;
    private boolean leapImpactTriggered = false;
    private boolean wasAirborneBeforeLanding = false;
    private int leapGroundedTicks = 0;
    private final DragonForwardMovementComponent leapMovement = new DragonForwardMovementComponent(
            this,
            new DragonForwardMovementComponent.StateAccess() {
                @Override
                public void start(int ticks, Vec3 velocity, boolean dashing, boolean dodging, double horizontalDrag) {
                    entityData.set(DATA_LEAP_MOVE_TICKS, Math.max(1, ticks));
                    setVelocity(velocity);
                }

                @Override
                public int ticks() {
                    return entityData.get(DATA_LEAP_MOVE_TICKS);
                }

                @Override
                public void setTicks(int ticks) {
                    entityData.set(DATA_LEAP_MOVE_TICKS, Math.max(0, ticks));
                }

                @Override
                public Vec3 velocity() {
                    return new Vec3(
                            entityData.get(DATA_LEAP_MOVE_X),
                            entityData.get(DATA_LEAP_MOVE_Y),
                            entityData.get(DATA_LEAP_MOVE_Z)
                    );
                }

                @Override
                public void setVelocity(Vec3 velocity) {
                    entityData.set(DATA_LEAP_MOVE_X, (float) velocity.x);
                    entityData.set(DATA_LEAP_MOVE_Y, (float) velocity.y);
                    entityData.set(DATA_LEAP_MOVE_Z, (float) velocity.z);
                }

                @Override
                public double horizontalDrag() {
                    return 1.0D;
                }

                @Override
                public void clear() {
                    entityData.set(DATA_LEAP_MOVE_TICKS, 0);
                    entityData.set(DATA_LEAP_MOVE_X, 0.0F);
                    entityData.set(DATA_LEAP_MOVE_Y, 0.0F);
                    entityData.set(DATA_LEAP_MOVE_Z, 0.0F);
                }
            }
    );
    private long lastAiLandedAnimTick = -40L;
    private final ScreenShakeComponent screenShakeComponent;
    private float cinematicZoomProgress = 0.0F;
    private float prevCinematicZoomProgress = 0.0F;
    private static final int MIN_AMBIENT_DELAY = 180;
    private static final int MAX_AMBIENT_DELAY = 520;
    private int teethChipDropCooldownTicks = 0;
    public Ignivorus(EntityType<? extends Ignivorus> type, Level level) {
        super(type, level);
        this.screenShakeComponent = new ScreenShakeComponent(this, DATA_SCREEN_SHAKE_AMOUNT, SHAKE_DECAY_PER_TICK);
        this.setMaxUpStep(DEFAULT_MAX_UP_STEP);

        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);

        this.riderController = new IgnivorusRiderController(this);
        this.movementController = new AnimationController<>(this, "movement", 2, animationHandler::movementPredicate);
        this.actionController = new AnimationController<>(this, IgnivorusAnimationHandler.ACTION_CONTROLLER, 4, state -> {
            if (isTamingStunned()) {
                return PlayState.STOP;
            }
            return animationHandler.actionPredicate(state);
        });
        this.fastActionController = new AnimationController<>(this, IgnivorusAnimationHandler.FAST_ACTION_CONTROLLER, 1, animationHandler::fastActionPredicate);
        this.flightController = AnimationHelper.createFlightController(this, getFlightAnimationTransitionTicks(), animationHandler::flightPredicate);
        this.vocalController = new AnimationController<>(this, AnimationHelper.VOCAL_CONTROLLER, 2, AnimationHelper::vocalIdle);
        this.interactionController = new AnimationController<>(this, AnimationHelper.INTERACTION_CONTROLLER, 1, AnimationHelper::interactionIdle);
        setupAnimationControllers();
        resetAmbientSoundTimer(MIN_AMBIENT_DELAY, MAX_AMBIENT_DELAY);
        if (!level.isClientSide) {
            applyConfiguredAttributes();
        }
    }

    @Override
    protected DragonRiderFlight.Config getRiderFlightConfig() {
        return new DragonRiderFlight.Config(
                true,
                0,
                0.25D,
                0,
                0.45D,
                TAKEOFF_ANIMATION_TICKS
        );
    }

    @Override
    protected void onTakeoffStarted() {
        this.timeFlying = 0;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_RIDER_LANDING_BLEND, false);
        this.entityData.define(DATA_BULLDOZING, false);
        this.entityData.define(DATA_PHASE2, false);
        this.entityData.define(DATA_PHASE2_RIDER_TAKEOFF, false);
        this.entityData.define(DATA_LEAPING, false);
        this.entityData.define(DATA_LEAP_ANIM_STATE, 0);
        this.entityData.define(DATA_LEAP_MOVE_TICKS, 0);
        this.entityData.define(DATA_LEAP_MOVE_X, 0.0F);
        this.entityData.define(DATA_LEAP_MOVE_Y, 0.0F);
        this.entityData.define(DATA_LEAP_MOVE_Z, 0.0F);
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

    public static AttributeSupplier.Builder createAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        double attackDamage = config.abilityDamage("bite", 15.0D);
        return createMobAttributes()
            .add(Attributes.MAX_HEALTH, config.maxHealth())
            .add(Attributes.MOVEMENT_SPEED, 0.3D)
            .add(Attributes.FLYING_SPEED, config.flyingSpeed())
            .add(Attributes.ATTACK_DAMAGE, attackDamage)
            .add(Attributes.FOLLOW_RANGE, 64.0D)
            .add(Attributes.ARMOR, config.armor())
            .add(Attributes.KNOCKBACK_RESISTANCE, 2.0D);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case DOUBLE_TAP_A, DOUBLE_TAP_D, DOUBLE_TAP_W, DOUBLE_TAP_S,
                 ABILITY_USE, ABILITY_STOP -> true;
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

        if (level() instanceof ServerLevel serverLevel) {
            DragonDestructionManager.applyPassiveTreeDestruction(serverLevel, this);
        }

        if (isDying() || this.dead) {
            stopCustomStateForDeath();
            return;
        }
        tickRiderControlLock();
        tickBulldozeState();
        tickPhase2State();
        updatePhase2FollowRange();
        tickLeapState();
        tickScreenShake();
        tickCinematicZoom();
        tickStandardTakeoffAndGroundedAerialRecovery();
        if (isFlying()) {
            airTicks++;
            groundTicks = 0;
            timeFlying++;

        } else {
            groundTicks++;
            airTicks = 0;
            timeFlying = 0;
        }

        syncFlightAnimationState();

        if (!level().isClientSide && isLanding() && onGround()) {
            handleAiLandingComplete();
        }

        if (!level().isClientSide && !isFlying() && isHovering() && onGround()) {
            setHovering(false);
        }

        this.setNoGravity(isFlying() || isTakeoff() || isHovering() || isLanding());

        tickAsyncFlightNavigation();

        tickBankingLogic();
        tickBarrelRollLogic();
        tickStandardPitchingLogic();

        if (!level().isClientSide) {
            if (isBaby()) {
                if (isAerial()) {
                    clearAerialStateForInterrupt();
                }
                if (getTarget() != null) {
                    setTarget(null);
                }
                if (getActiveAbility() != null) {
                    combatManager.forceEndActiveAbility();
                }
                setAggressive(false);
            }
            tamingController.tickServer();
            if (isTamingStunned()) {
                tamingController.enforceGroundingTick();
            }
            tickFireBreathEnergy();
            tickTerrainClearing();
            handleAmbientSounds();
            int cooldown = this.entityData.get(DATA_FEEDING_COOLDOWN);
            if (cooldown > 0) {
                this.entityData.set(DATA_FEEDING_COOLDOWN, cooldown - 1);
            }
            if (teethChipDropCooldownTicks > 0) {
                teethChipDropCooldownTicks--;
            }
        }
        if (!level().isClientSide) {
            tickAnimationStates();
        }
        updateSittingProgress();
    }

    private void stopCustomStateForDeath() {
        if (!level().isClientSide) {
            clearRiderControlLock();
            this.getNavigation().stop();
            this.setTarget(null);
        }

        bulldozing = false;
        this.entityData.set(DATA_BULLDOZING, false);

        phase2Active = false;
        this.entityData.set(DATA_PHASE2, false);
        phase2RiderTakeoffActive = false;
        this.entityData.set(DATA_PHASE2_RIDER_TAKEOFF, false);
        phase2WasVehicle = false;

        leaping = false;
        leapWasVehicle = false;
        leapImpactTriggered = false;
        wasAirborneBeforeLanding = false;
        leapGroundedTicks = 0;
        leapVelocity = Vec3.ZERO;
        leapArcDirection = Vec3.ZERO;
        leapArcTick = 0;
        leapWindupTicks = 0;
        leapMovement.clear();
        leapAnimState = LEAP_STATE_NONE;
        leapImpactRecoveryTicks = 0;
        this.entityData.set(DATA_LEAPING, false);
        this.entityData.set(DATA_LEAP_ANIM_STATE, LEAP_STATE_NONE);

        clearAerialStateForInterrupt();
        setNoGravity(false);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
    }

    @Override
    public boolean hurt(@Nonnull DamageSource damageSource, float amount) {
        if (isDying()) {
            return false;
        }
        rememberIncomingProjectile(damageSource);
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
        if (isSleeping() || isSleepingEntering() || isSleepingExiting()) {
            wakeUpImmediately();
            suppressSleep(200);
        }
        if (tamingController.tryEnterHoldStateFromDamage(damageSource, amount)) {
            return true;
        }
        boolean hurt = super.hurt(damageSource, amount);
        if (hurt
                && !level().isClientSide
                && amount > 0.0F
                && damageSource.getEntity() != null
                && teethChipDropCooldownTicks <= 0) {
            if (DragonLootTables.dropEntityLoot(this, DragonLootTables.IGNIVORUS_HIT, damageSource)) {
                teethChipDropCooldownTicks = 30;
            }
        }
        return hurt;
    }

    @Override
    protected double getCullingInflateX() {
        return 8.0D;
    }

    @Override
    protected double getCullingInflateY() {
        return 4.0D;
    }

    @Override
    protected double getCullingInflateZ() {
        return 8.0D;
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

        tickAmbientVocalSounds(MIN_AMBIENT_DELAY, MAX_AMBIENT_DELAY, this::selectAmbientGrumble);
    }

    private String selectAmbientGrumble() {
        float roll = this.getRandom().nextFloat();
        return roll < 0.34f ? "ignivorus_grumble1"
                : (roll < 0.67f ? "ignivorus_grumble2" : "ignivorus_grumble3");
    }

    public boolean canFeed() {
        return this.entityData.get(DATA_FEEDING_COOLDOWN) <= 0;
    }

    public void setFeedingCooldown(int ticks) {
        this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, ticks));
    }

    @Override
    protected DragonVariantSet getVariantSet() {
        return VARIANTS;
    }

    public int getFireballChargeLevel() {
        return this.entityData.get(DATA_FIREBALL_CHARGE);
    }

    public void setFireballChargeLevel(int level) {
        this.entityData.set(DATA_FIREBALL_CHARGE, Math.max(0, Math.min(3, level)));
    }

    public boolean isChargingFireball() {
        return getFireballChargeLevel() > 0;
    }

    private static final float TAMING_HEALTH_RATIO = 1.0F / 3.0F;
    public boolean isTamingStunned() {
        return this.entityData.get(DATA_TAMING_STUNNED);
    }

    public void enterTamingStun() {
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


    public boolean isAwaitingTamingFeed() {
        return tamingController.isAwaitingFeed();
    }

    public boolean isBelowTamingThreshold() {
        return this.getHealth() <= getTamingThreshold();
    }

    public float getTamingThreshold() {
        double fallback = this.getMaxHealth() * TAMING_HEALTH_RATIO;
        double configured = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                .extraDouble("taming_stun_health", fallback);
        double clamped = Math.max(0.0D, Math.min(configured, this.getMaxHealth()));
        return (float) clamped;
    }

    @Override
    public void lockRiderControls(int ticks) {
        super.lockRiderControls(ticks);
        this.setAccelerating(false);
        this.setLastRiderForward(0.0F);
        this.setLastRiderStrafe(0.0F);
        this.setGroundMoveStateFromRider(0);
        this.setGoingUp(false);
        this.setGoingDown(false);
        this.setDeltaMovement(Vec3.ZERO);
        if (!this.level().isClientSide) {
            this.getNavigation().stop();
            if (this.isVehicle() || this.getControllingPassenger() != null) {
                this.setTarget(null);
            }
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
        updateBulldozeStepHeight();
        if (!level().isClientSide) {
            boolean currentlyVehicle = this.isVehicle();
            if (bulldozeCooldownTicks > 0) {
                bulldozeCooldownTicks--;
            }

            bulldozeHitCooldowns.entrySet().removeIf(entry -> {
                entry.setValue(entry.getValue() - 1);
                return entry.getValue() <= 0;
            });

            if (bulldozing && currentlyVehicle && !this.isAccelerating()) {
                setAccelerating(true);
            }

            if (bulldozing && bulldozeWasVehicle && !currentlyVehicle) {
                bulldozing = false;
                this.entityData.set(DATA_BULLDOZING, false);
                setAccelerating(false);
                bulldozeCooldownTicks = 40;
                clearRiderControlLock();
                bulldozeHitCooldowns.clear();
            }

            if (bulldozing && currentlyVehicle) {
                Vec3 forward = getBulldozeTunnelDirection(getDeltaMovement());
                if (forward.lengthSqr() < 1.0E-6D) {
                    forward = getLookAngle().normalize();
                }
                Vec3 damageCenter = this.getBoundingBox().getCenter()
                        .add(forward.scale(BULLDOZE_DAMAGE_FORWARD_REACH));

                AABB dragonBox = this.getBoundingBox().inflate(
                        BULLDOZE_DAMAGE_HALF_WIDTH,
                        BULLDOZE_DAMAGE_HALF_HEIGHT,
                        BULLDOZE_DAMAGE_HALF_WIDTH
                );
                AABB forwardBox = new AABB(damageCenter, damageCenter).inflate(
                        BULLDOZE_DAMAGE_HALF_WIDTH,
                        BULLDOZE_DAMAGE_HALF_HEIGHT,
                        BULLDOZE_DAMAGE_HALF_WIDTH
                );
                AABB combinedBox = dragonBox.minmax(forwardBox);

                List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, combinedBox,
                    entity -> entity != this && entity != this.getControllingPassenger() && !this.isAlly(entity)
                );
                for (LivingEntity target : entities) {
                    int entityId = target.getId();
                    if (bulldozeHitCooldowns.containsKey(entityId)) {
                        continue;
                    }

                    target.hurt(this.damageSources().mobAttack(this), resolveBulldozeDamage());
                    double knockbackStrength = 2.0D;
                    double dx = this.getX() - target.getX();
                    double dz = this.getZ() - target.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 1.0E-6D) {
                        target.knockback(
                            knockbackStrength,
                            dx / dist,
                            dz / dist
                        );
                    } else {
                        target.knockback(knockbackStrength, -forward.x, -forward.z);
                    }
                    bulldozeHitCooldowns.put(entityId, 5);
                }
            }
            bulldozeWasVehicle = currentlyVehicle;
        }
    }

    private void updateBulldozeStepHeight() {
        setMaxUpStep(bulldozing && isVehicle() ? BULLDOZE_MAX_UP_STEP : DEFAULT_MAX_UP_STEP);
    }

    private void tickPhase2State() {
        if (!level().isClientSide) {
            boolean currentlyVehicle = this.isVehicle();

            if (phase2CooldownTicks > 0) {
                phase2CooldownTicks--;
            }
            if (phase2Active && wildPhase2UltimateTriggered && !isTame()) {
                LivingEntity target = getTarget();
                if (target == null || !target.isAlive() || target.isRemoved()) {
                    exitPhase2(true);
                    return;
                }
            }
            if (phase2Active && phase2WasVehicle && !currentlyVehicle) {
                phase2Active = false;
                this.entityData.set(DATA_PHASE2, false);
                phase2CooldownTicks = 40;
                clearRiderControlLock();
            }

            phase2WasVehicle = currentlyVehicle;
        }
    }

    private void updatePhase2FollowRange() {
        if (level().isClientSide) {
            return;
        }

        AttributeInstance followRange = getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange == null) {
            return;
        }

        boolean wildTransitionUltimate = !isTame()
                && !wildPhase2UltimateTriggered
                && isAbilityActive(ModAbilities.IGNIVORUS_ULTIMATE);
        boolean shouldExtendRange = !isTame() && (phase2Active || wildTransitionUltimate);
        boolean hasModifier = followRange.getModifier(PHASE2_FOLLOW_RANGE_MODIFIER_UUID) != null;

        if (shouldExtendRange && !hasModifier) {
            followRange.addTransientModifier(PHASE2_FOLLOW_RANGE_MODIFIER);
        } else if (!shouldExtendRange && hasModifier) {
            followRange.removeModifier(PHASE2_FOLLOW_RANGE_MODIFIER_UUID);
        }
    }

    private void tickLeapState() {
        if (!level().isClientSide) {
            boolean currentlyVehicle = this.isVehicle();

            if (leapCooldownTicks > 0) {
                leapCooldownTicks--;
            }

            if (leapImpactRecoveryTicks > 0) {
                leapImpactRecoveryTicks--;
                if (leapImpactRecoveryTicks == 0 && leapAnimState != LEAP_STATE_NONE) {
                    leapAnimState = LEAP_STATE_NONE;
                    this.entityData.set(DATA_LEAP_ANIM_STATE, LEAP_STATE_NONE);
                }
            }

            if (leaping && leapWasVehicle && !currentlyVehicle) {
                leaping = false;
                this.entityData.set(DATA_LEAPING, false);
                leapAnimState = LEAP_STATE_NONE;
                this.entityData.set(DATA_LEAP_ANIM_STATE, LEAP_STATE_NONE);
                leapVelocity = Vec3.ZERO;
                leapArcDirection = Vec3.ZERO;
                leapArcTick = 0;
                setDeltaMovement(Vec3.ZERO);
                wasAirborneBeforeLanding = false;
                leapImpactTriggered = false;
                leapGroundedTicks = 0;
                leapWindupTicks = 0;
                leapMovement.cancelActive();
                leapImpactRecoveryTicks = 0;
                leapWasVehicle = false;
            }
            if (leaping) {
                if (leapWindupTicks > 0) {
                    handleLeapWindup();
                } else {
                    handleLeapMovement();
                }
            }
            if (leapMovement.isActive()) {
                leapMovement.tickServerState();
            }
        }
    }

    private void handleLeapWindup() {
        this.getNavigation().stop();
        this.getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0D);
        setDeltaMovement(Vec3.ZERO);
        hasImpulse = true;

        leapWindupTicks--;
        if (leapWindupTicks > 0) {
            return;
        }

        setDeltaMovement(Vec3.ZERO);
        hasImpulse = true;
        leapGroundedTicks = 0;
        leapMovement.startContinuous(leapVelocity, 3);
        if (level() instanceof ServerLevel server) {
            breakGroundCircle(server, position(), 8.0D);
        }
    }

    private void handleLeapMovement() {
        if (!onGround()) {
            wasAirborneBeforeLanding = true;
            leapGroundedTicks = 0;
        } else {
            leapGroundedTicks++;
        }

        if (!leapImpactTriggered && wasAirborneBeforeLanding && leapVelocity.y < -0.05D) {
            double groundDistance = getLeapGroundDistance();
            if (groundDistance >= 0.0D && groundDistance <= LEAP_IMPACT_TRIGGER_HEIGHT) {
                animationHandler.triggerLeapImpactAnimation();
                leapImpactTriggered = true;
            }
        }

        updateRiderSteeredLeapDirection();
        leapArcTick++;
        leapVelocity = calculateLeapArcStep(leapArcDirection, leapArcTick);
        leapMovement.updateContinuous(leapVelocity, 3);
        if (onGround() && wasAirborneBeforeLanding) {
            applyLeapSlamDamage();
            getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_IMPACT.get(), 1.0f, 1.0f, 43);
            if (!leapImpactTriggered) {
                animationHandler.triggerLeapImpactAnimation();
                leapImpactTriggered = true;
            }
            leaping = false;
            this.entityData.set(DATA_LEAPING, false);
            leapVelocity = Vec3.ZERO;
            leapArcDirection = Vec3.ZERO;
            leapArcTick = 0;
            leapWindupTicks = 0;
            leapMovement.cancelActive();
            wasAirborneBeforeLanding = false;
            leapImpactTriggered = false;
            if (!leapWasVehicle) {
                leapCooldownTicks = LEAP_COOLDOWN_TICKS;
            }
            leapImpactRecoveryTicks = LEAP_IMPACT_RECOVERY_DURATION;
            leapWasVehicle = false;
            lockRiderControls(LEAP_IMPACT_RECOVERY_DURATION);
            setDeltaMovement(Vec3.ZERO);
            leapGroundedTicks = 0;
            return;
        }
        if (onGround() && !wasAirborneBeforeLanding && leapGroundedTicks >= LEAP_GROUNDED_FAILSAFE_TICKS) {
            leaping = false;
            this.entityData.set(DATA_LEAPING, false);
            leapAnimState = LEAP_STATE_NONE;
            this.entityData.set(DATA_LEAP_ANIM_STATE, LEAP_STATE_NONE);
            leapVelocity = Vec3.ZERO;
            leapArcDirection = Vec3.ZERO;
            leapArcTick = 0;
            leapWindupTicks = 0;
            leapMovement.cancelActive();
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

        Vec3 landPos = position();
        spawnLeapImpactBlockEffect(server);
        spawnLeapImpactDirtParticles(server);
        AABB damageArea = new AABB(
            landPos.x - LEAP_SLAM_RADIUS,
            landPos.y - 1.0,
            landPos.z - LEAP_SLAM_RADIUS,
            landPos.x + LEAP_SLAM_RADIUS,
            landPos.y + getBbHeight() + 1.0,
            landPos.z + LEAP_SLAM_RADIUS
        );
        List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, damageArea,
                entity -> entity != this && entity.isAlive() && entity.attackable() && !isAlly(entity));

        if (targets.isEmpty()) {
            return;
        }

        float damage = resolveLeapSlamDamage();
        DamageSource source = server.damageSources().mobAttack(this);

        for (LivingEntity target : targets) {
            target.hurt(source, damage);
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

    private void spawnLeapImpactBlockEffect(ServerLevel level) {
        RandomSource random = getRandom();
        BlockPos dragonPos = blockPosition();
        List<BlockPos> blockPositions = new ArrayList<>();
        addRingBlockPositions(blockPositions, dragonPos, 16, 20, random, 25);
        addRingBlockPositions(blockPositions, dragonPos, 10, 15, random, 20);
        addRingBlockPositions(blockPositions, dragonPos, 5, 9, random, 15);
        for (BlockPos pos : blockPositions) {
            spawnLeapFallingBlockAt(level, pos, random);
        }
    }

    private void spawnLeapImpactDirtParticles(ServerLevel level) {
        RandomSource random = getRandom();
        Vec3 dragonPos = position();
        BlockParticleOption dirtParticles = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
        spawnLeapParticleRing(level, dragonPos, dirtParticles, 5, 10, 40, random);
        spawnLeapParticleRing(level, dragonPos, dirtParticles, 10, 15, 60, random);
        spawnLeapParticleRing(level, dragonPos, dirtParticles, 15, 20, 80, random);
    }

    private void addRingBlockPositions(List<BlockPos> positions, BlockPos center,
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

        BlockState groundState = level.getBlockState(groundPos);
        if (groundState.isAir() || groundState.liquid() || groundState.is(Blocks.BEDROCK)) {
            return;
        }

        double startX = groundPos.getX() + 0.5;
        double startY = groundPos.getY() + 0.5;
        double startZ = groundPos.getZ() + 0.5;

        VisualFallingBlockEntity fallingBlock =
            new VisualFallingBlockEntity(
                ModEntities.VISUAL_FALLING_BLOCK.get(),
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
                                       BlockParticleOption particleType,
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

            BlockState groundState = level.getBlockState(groundPos);
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
            BlockState state = level().getBlockState(checkPos);
            if (!state.isAir() && !state.liquid() && state.isSolidRender(level(), checkPos)) {
                return checkPos;
            }
        }
        return null;
    }

    private void breakGroundCircle(ServerLevel level, Vec3 center, double radius) {
        if (!DragonGriefingRules.canDestroyBlocks(level)) {
            return;
        }

        int centerX = (int) Math.floor(center.x);
        int centerY = (int) Math.floor(center.y);
        int centerZ = (int) Math.floor(center.z);

        int radiusInt = (int) Math.ceil(radius);
        List<BlockPos> blocksToRestore = new ArrayList<>();
        Map<BlockPos, BlockState> originalStates = new HashMap<>();
        for (int x = -radiusInt; x <= radiusInt; x++) {
            for (int z = -radiusInt; z <= radiusInt; z++) {
                double distSqr = x * x + z * z;
                if (distSqr > radius * radius) {
                    continue;
                }

                BlockPos targetPos = new BlockPos(centerX + x, centerY, centerZ + z);
                BlockPos groundPos = findGroundLevelForBreaking(level, targetPos);

                if (groundPos == null) {
                    continue;
                }
                BlockState state = level.getBlockState(groundPos);
                if (!canBreakBlock(level, groundPos, state)) {
                    continue;
                }

                originalStates.put(groundPos.immutable(), state);
                blocksToRestore.add(groundPos.immutable());

                level.setBlock(groundPos, Blocks.AIR.defaultBlockState(), 3);

                spawnBreakingFallingBlock(level, groundPos, state);
            }
        }

        if (!blocksToRestore.isEmpty()) {
            scheduleBlockRestoration(level, originalStates, 100);
        }
    }

    private BlockPos findGroundLevelForBreaking(ServerLevel level, BlockPos startPos) {
        for (int y = startPos.getY(); y > level.getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
            BlockState state = level.getBlockState(checkPos);

            if (!state.isAir() && !state.liquid() && state.isSolidRender(level, checkPos)) {
                return checkPos;
            }
        }
        return null;
    }

    private boolean canBreakBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.liquid()) {
            return false;
        }

        if (state.is(Blocks.BEDROCK) ||
            state.is(Blocks.END_PORTAL) ||
            state.is(Blocks.END_PORTAL_FRAME) ||
            state.is(Blocks.END_GATEWAY)) {
            return false;
        }
        if (state.getBlock() instanceof EntityBlock) {
            return false;
        }

        return state.isSolidRender(level, pos);
    }

    private void spawnBreakingFallingBlock(ServerLevel level, BlockPos pos, BlockState state) {
        double startX = pos.getX() + 0.5;
        double startY = pos.getY() + 0.5;
        double startZ = pos.getZ() + 0.5;

        VisualFallingBlockEntity fallingBlock = new VisualFallingBlockEntity(
                ModEntities.VISUAL_FALLING_BLOCK.get(),
                level,
                startX,
                startY,
                startZ,
                state,
                100
            );
        double upwardVelocity = 0.3 + level.random.nextDouble() * 0.4;
        fallingBlock.setDeltaMovement(0, upwardVelocity, 0);
        level.addFreshEntity(fallingBlock);
    }

    private void scheduleBlockRestoration(ServerLevel level, Map<BlockPos, BlockState> blocks, int delayTicks) {
        level.getServer().tell(new TickTask(
            level.getServer().getTickCount() + delayTicks,
            () -> {
                for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
                    BlockPos pos = entry.getKey();
                    BlockState state = entry.getValue();
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
        return level().isClientSide ? this.entityData.get(DATA_LEAP_ANIM_STATE) : leapAnimState;
    }

    public void setUltimateCameraZoomActive(boolean active) {
        this.entityData.set(DATA_CINEMATIC_ZOOM_ACTIVE, active);
    }


    @Override
    protected boolean isRiderInputLocked(Player player) {
        return areRiderControlsLocked() || isLeaping();
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
                refreshFireAimDirection(start, true);
        } else {
            resetFireAimDirection();
        }
    }

    @Override
    public void removePassenger(@NotNull Entity passenger) {
        super.removePassenger(passenger);
    }

    @Override
    public void travel(@NotNull Vec3 travelVec) {
        if (isLeaping()) {
            setDeltaMovement(Vec3.ZERO);
            if (leapMovement.isActive()) {
                leapMovement.applyDirectTravelMotion();
            }
            return;
        }

        if (isPhase2RiderTakeoffAnimating() && isFlying()) {
            move(MoverType.SELF, new Vec3(0.0D, PHASE2_RIDER_TAKEOFF_UPWARD_STEP, 0.0D));
            setDeltaMovement(Vec3.ZERO);
            hasImpulse = true;
            hurtMarked = true;
            fallDistance = 0.0F;
            return;
        }

        if (areRiderControlsLocked()) {
            super.travel(Vec3.ZERO);
            return;
        }

        boolean inWater = this.isInWater() || this.isInWaterOrBubble() || this.isInLava();

        if (inWater) {
            clearRiderFlightStateInWaterIfNeeded();
        }

        if (this.isVehicle() && riderController.getRidingPlayer() != null) {
            Player rider = riderController.getRidingPlayer();
            if (shouldUseRiderFlightMovementInWater()) {
                riderController.handleRiderMovement(rider, travelVec);
            } else if (inWater) {
                handleRiderWaterSwimming(travelVec);
            } else if (isFlying()) {
                riderController.handleRiderMovement(rider, travelVec);
            } else {
                this.setSpeed(riderController.getRiddenSpeed(rider));
                super.travel(travelVec);
            }
            return;
        }

        if (inWater) {
            handleRiderWaterSwimming(travelVec);
            return;
        }

        super.travel(travelVec);
    }

    @Override
    public float getRiddenSpeed(@NotNull Player rider) {
        return riderController.getRiddenSpeed(rider);
    }

    @Override
    protected void onRiderFlightStateClearedInWater() {
        timeFlying = 0;
    }

    private boolean isPhaseTwoRidingAbilityBlocked(String abilityName) {
        return isPhase2Active()
                && !abilityName.equals(ModAbilities.IGNIVORUS_WING_SWIPE.getName())
                && !abilityName.equals(ModAbilities.IGNIVORUS_STOMP.getName())
                && !abilityName.equals(ModAbilities.IGNIVORUS_BITE.getName())
                && !abilityName.equals(ModAbilities.IGNIVORUS_ROAR.getName())
                && !abilityName.equals(ModAbilities.IGNIVORUS_FIRE_BREATH.getName())
                && !abilityName.equals(ModAbilities.IGNIVORUS_FIREBALL.getName())
                && !abilityName.equals(ModAbilities.IGNIVORUS_ULTIMATE.getName());
    }

    @Override
    protected DragonAbilityType<?, ?> resolveRidingAbilityType(String abilityName) {
        if (abilityName == null || abilityName.isEmpty()) {
            return null;
        }
        if (isBaby() && isBabyAbilityBlocked(abilityName)) {
            return null;
        }
        if (isPhaseTwoRidingAbilityBlocked(abilityName)) {
            return null;
        }
        return super.resolveRidingAbilityType(abilityName);
    }

    @Override
    protected boolean canUseResolvedRidingAbility(DragonAbilityType<?, ?> abilityType) {
        if (isBulldozing() && isBulldozeBlockedMelee(abilityType)) {
            return false;
        }
        return super.canUseResolvedRidingAbility(abilityType);
    }

    private boolean isBulldozeBlockedMelee(DragonAbilityType<?, ?> abilityType) {
        return abilityType == ModAbilities.IGNIVORUS_BITE
                || abilityType == ModAbilities.IGNIVORUS_BODY_SLAM;
    }

    @Override
    protected boolean tryReleaseHeldRidingAbility(String abilityName) {
        if (ModAbilities.IGNIVORUS_FIREBALL.getName().equals(abilityName)) {
            var active = combatManager.getActiveAbility();
            if (active != null && active.getAbilityType() == ModAbilities.IGNIVORUS_FIREBALL) {
                ((IgnivorusFireballAbility) active).requestRelease();
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean handleCustomRiderAction(ServerPlayer player, DragonRiderAction action,
                                              String abilityName, boolean locked) {
        if (action == null) {
            return false;
        }
        if (action == DragonRiderAction.DOUBLE_TAP_W) {
            if (isBaby()) {
                return true;
            }
            if (!locked) {
                onRiderBulldoze(player);
            }
            return true;
        }
        if (action == DragonRiderAction.DOUBLE_TAP_S) {
            if (isBaby()) {
                return true;
            }
            if (!locked) {
                onRiderPhase2Toggle(player);
            }
            return true;
        }
        if (bulldozing || leaping) {
            return action != DragonRiderAction.ACCELERATE
                    && action != DragonRiderAction.STOP_ACCELERATE;
        }
        if (locked) {
            return false;
        }
        return false;
    }

    @Override
    protected RiderFlexSpec getRiderFlexSpec() {
        return new RiderFlexSpec(FLEX_CONTROL_LOCK_TICKS, FLEX_COOLDOWN_TICKS);
    }

    @Override
    protected boolean canRiderFlex(ServerPlayer player, RiderFlexSpec spec) {
        return super.canRiderFlex(player, spec)
                && !isBaby()
                && onGround()
                && !isPhase2Active()
                && !isFlying()
                && !isTakeoff()
                && !isLanding()
                && !isInWaterOrBubble()
                && !bulldozing
                && !leaping;
    }

    @Override
    protected void playRiderFlex(ServerPlayer player, RiderFlexSpec spec) {
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        getSoundHandler().playVocal("ignivorus_flex");
    }

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
        return DragonEntity.DragonSleepPreferences.NOCTURNAL();
    }

    @Override
    public boolean canSleepNow() {
        boolean ownerSleeping = false;
        if (isTame()) {
            var owner = getOwner();
            ownerSleeping = owner instanceof Player player && player.isSleeping();
        }
        return !DragonEntity.DragonSleepPreferences.isNaturalDay(level()) || ownerSleeping;
    }

    @Override
    protected int getSleepSitDownDuration() {
        return getSitDownAnimationTicks();
    }
    @Override
    protected int getSleepSitUpDuration() {
        return getSitUpAnimationTicks();
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
    protected void onSleepLockCommand(int snapshot) {
    }
    @Override
    protected void onSleepUnlockCommand(int desired) {
        setOrderedToSit(desired == 1);
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
        return ModAbilities.IGNIVORUS_ULTIMATE.getName().equals(abilityName)
                || ModAbilities.IGNIVORUS_ROAR.getName().equals(abilityName)
                || ModAbilities.IGNIVORUS_FIREBALL.getName().equals(abilityName)
                || ModAbilities.IGNIVORUS_BODY_SLAM.getName().equals(abilityName)
                || ModAbilities.IGNIVORUS_BITE.getName().equals(abilityName)
                || ModAbilities.IGNIVORUS_FIRE_BREATH.getName().equals(abilityName)
                || ModAbilities.IGNIVORUS_WING_SWIPE.getName().equals(abilityName)
                || ModAbilities.IGNIVORUS_STOMP.getName().equals(abilityName);
    }

    @Override
    public void forceEndActiveAbility() {
        this.combatManager.forceEndActiveAbility();
        clearFireBreathPath();
        setBreathingFire(false);
    }

    @Override
    public void forceEndAbility(DragonAbilityType<?, ?> abilityType) {
        combatManager.forceEndAbility(abilityType);
        if (abilityType == ModAbilities.IGNIVORUS_FIRE_BREATH) {
            clearFireBreathPath();
            setBreathingFire(false);
        }
    }


    public boolean isAiSpecialCombatActive() {
        return !level().isClientSide && aiSpecialCombatActive;
    }

    @Override
    public RiderAbilityBinding getPrimaryRiderAbility() {
        if (isBaby()) {
            return null;
        }
        return new RiderAbilityBinding(ModAbilities.IGNIVORUS_ROAR.getName(), RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getSecondaryRiderAbility() {
        if (isBaby()) {
            return null;
        }
        return new RiderAbilityBinding(ModAbilities.IGNIVORUS_ULTIMATE.getName(), RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getTertiaryRiderAbility() {
        if (isBaby()) {
            return null;
        }
        return new RiderAbilityBinding(ModAbilities.IGNIVORUS_FIRE_BREATH.getName(), RiderAbilityBinding.Activation.HOLD);
    }

    @Override
    public RiderAbilityBinding getAttackRiderAbility() {
        if (isBaby() || isBulldozing()) {
            return null;
        }
        if (isPhase2Active()) {
            if (isFlying()) {
                return new RiderAbilityBinding(ModAbilities.IGNIVORUS_BITE.getName(), RiderAbilityBinding.Activation.PRESS);
            }
            String abilityId = getMeleeMode() == 1
                    ? ModAbilities.IGNIVORUS_STOMP.getName()
                    : ModAbilities.IGNIVORUS_WING_SWIPE.getName();
            return new RiderAbilityBinding(abilityId, RiderAbilityBinding.Activation.PRESS);
        }
        String abilityId = getMeleeMode() == 1
                ? ModAbilities.IGNIVORUS_BODY_SLAM.getName()
                : ModAbilities.IGNIVORUS_BITE.getName();
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
        return stack.is(ModTags.Items.IGNIVORUS_FOODS);
    }

    @Override
    protected void onRiderToggleMelee(Player player) {
        if (isAerial() && player instanceof ServerPlayer serverPlayer && !level().isClientSide) {
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
        if (isAerial()) {
            return;
        }
        if (areRiderControlsLocked()) {
            return;
        }
        if (isPhase2Active()) {
            onRiderLeapSlam();
            return;
        }

        if (bulldozeCooldownTicks > 0) {
            return;
        }
        if (bulldozing) {
            bulldozing = false;
            this.entityData.set(DATA_BULLDOZING, false);
            setAccelerating(false);
            bulldozeCooldownTicks = 40;
            lockRiderControls(20);
            animationHandler.triggerBulldozeExitAnimation();
            getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_BULLDOZER_EXIT.get(), 1.0f, 1.0f, 45);
        } else {

            bulldozing = true;
            this.entityData.set(DATA_BULLDOZING, true);
            setAccelerating(true);
            lockRiderControls(BULLDOZE_ENTER_TICKS);
            animationHandler.triggerBulldozeEnterAnimation();
            getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_BULLDOZER_ENTER.get(), 1.0f, 1.0f, 80);
        }
    }

    protected void onRiderLeapSlam() {
        Entity rider = getControllingPassenger();
        Vec3 look = rider instanceof LivingEntity living ? living.getLookAngle() : this.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            float yawRad = (float) Math.toRadians(this.getYRot());
            horizontal = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        }
        startLeapSlam(horizontal);
    }

    private void updateRiderSteeredLeapDirection() {
        if (!leapWasVehicle || !(getControllingPassenger() instanceof Player)) {
            return;
        }

        float yawRadians = this.getYRot() * Mth.DEG_TO_RAD;
        leapArcDirection = new Vec3(-Mth.sin(yawRadians), 0.0D, Mth.cos(yawRadians));
    }

    public boolean tryStartLeapSlamForAI(@Nullable LivingEntity target) {
        if (!canStartLeapSlamForAI(target)) {
            return false;
        }

        Vec3 toTarget = target.position().subtract(this.position());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            return false;
        }

        float yaw = (float) (Mth.atan2(horizontal.z, horizontal.x) * (180F / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;

        return startLeapSlam(horizontal);
    }

    public boolean canStartLeapSlamForAI(@Nullable LivingEntity target) {
        if (level().isClientSide || target == null || leapCooldownTicks > 0) {
            return false;
        }
        if (isBaby() || isTame() || !isPhase2Active() || isAerial() || isAiSpecialCombatActive()) {
            return false;
        }
        if (!isGroundedForAction() || bulldozing || leaping || leapImpactRecoveryTicks > 0) {
            return false;
        }
        if (areRiderControlsLocked() || getActiveAbility() != null || !isTargetValid(target)) {
            return false;
        }
        Vec3 horizontal = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        return horizontal.lengthSqr() >= 1.0E-6D;
    }

    private boolean startLeapSlam(Vec3 horizontalDirection) {
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

        Vec3 leapVec = calculateLeapArcStep(dir, 0);

        leaping = true;
        leapWasVehicle = this.isVehicle();
        this.entityData.set(DATA_LEAPING, true);
        leapAnimState = LEAP_STATE_TAKEOFF;
        this.entityData.set(DATA_LEAP_ANIM_STATE, LEAP_STATE_TAKEOFF);
        leapVelocity = leapVec;
        leapArcDirection = dir;
        leapArcTick = 0;
        leapWindupTicks = LEAP_WINDUP_TICKS;
        leapMovement.cancelActive();
        wasAirborneBeforeLanding = false;
        leapImpactTriggered = false;
        this.setDeltaMovement(Vec3.ZERO);
        this.getNavigation().stop();
        this.getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0D);
        this.hasImpulse = true;
        leapGroundedTicks = 0;
        getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LEAP.get(), 1.0f, 1.0f, 58);
        return true;
    }

    private Vec3 calculateLeapArcStep(Vec3 direction, int step) {
        double horizontalStep = LEAP_ARC_FORWARD_DISTANCE / LEAP_ARC_DURATION_TICKS;
        double startHeight = calculateLeapArcHeight(step);
        double endHeight = calculateLeapArcHeight(step + 1);
        return new Vec3(
                direction.x * horizontalStep,
                endHeight - startHeight,
                direction.z * horizontalStep
        );
    }

    private double calculateLeapArcHeight(int tick) {
        if (tick <= LEAP_ARC_ASCENT_TICKS) {
            double progress = (double) tick / LEAP_ARC_ASCENT_TICKS;
            double remaining = 1.0D - progress;
            return LEAP_ARC_HEIGHT * (1.0D - remaining * remaining);
        }

        double progress = (double) (tick - LEAP_ARC_ASCENT_TICKS) / LEAP_ARC_DESCENT_TICKS;
        return LEAP_ARC_HEIGHT * (1.0D - progress * progress);
    }

    protected void onRiderPhase2Toggle(Player player) {
        if (isBaby()) {
            return;
        }

        if (isAerial()) {
            return;
        }

        if (bulldozing || leaping) {
            return;
        }

        if (phase2CooldownTicks > 0) {
            return;
        }

        if (areRiderControlsLocked()) {
            return;
        }
        if (phase2Active) {
            exitPhase2(true);
        } else {
            phase2Active = true;
            this.entityData.set(DATA_PHASE2, true);
            lockRiderControls(PHASE2_ENTER_LOCK_TICKS);
            animationHandler.triggerPhase2EnterAnimation();
            getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_PHASE2_ENTER.get(), 1.0f, 1.0f, 47);
        }
    }

    public boolean isPhase2Active() {
        return level().isClientSide ? this.entityData.get(DATA_PHASE2) : phase2Active;
    }

    public boolean hasTriggeredWildPhase2Ultimate() {
        return wildPhase2UltimateTriggered;
    }

    public void completeWildPhase2Transition() {
        if (level().isClientSide || isTame() || isBaby()) {
            return;
        }
        wildPhase2UltimateTriggered = true;
        phase2Active = true;
        this.entityData.set(DATA_PHASE2, true);
        phase2CooldownTicks = 0;
        phase2WasVehicle = false;
    }

    public void exitWildPhase2ForAirPursuit() {
        if (level().isClientSide || isTame() || isVehicle() || !phase2Active) {
            return;
        }
        exitPhase2(true);
    }

    public void clearPhase2ForTamingStun() {
        boolean wasPhase2Active = phase2Active;
        phase2Active = false;
        this.entityData.set(DATA_PHASE2, false);
        phase2RiderTakeoffActive = false;
        this.entityData.set(DATA_PHASE2_RIDER_TAKEOFF, false);
        phase2WasVehicle = false;
        phase2CooldownTicks = 0;
        if (wasPhase2Active && !level().isClientSide) {
            animationHandler.triggerPhase2ExitAnimation();
            getSoundHandler().playMovingEntitySound(
                    ModSounds.IGNIVORUS_PHASE2_EXIT.get(),
                    1.0f,
                    1.0f,
                    38
            );
        }
    }

    private void exitPhase2(boolean lockControls) {
        phase2Active = false;
        this.entityData.set(DATA_PHASE2, false);
        phase2CooldownTicks = 40;
        phase2WasVehicle = false;
        if (lockControls) {
            lockRiderControls(PHASE2_EXIT_LOCK_TICKS);
        }
        animationHandler.triggerPhase2ExitAnimation();
        getSoundHandler().playMovingEntitySound(
                ModSounds.IGNIVORUS_PHASE2_EXIT.get(),
                1.0f,
                1.0f,
                38
        );
    }

    public boolean shouldUseRightWingSwipe() {
        return useRightWingSwipe;
    }

    public void toggleWingSwipeSide() {
        useRightWingSwipe = !useRightWingSwipe;
    }

    @Override
    protected boolean isRiderFallRecoveryBlocked() {
        return leaping || getLeapAnimState() != 0;
    }

    @Override
    protected void beforeStandardRiderTakeoff(Player player) {
        enforcePrimaryMeleeForFlight(player);
    }

    @Override
    public void startTakeoffSequence(double minUpwardVelocity, int animationTicks) {
        if (!isPhase2RiderTakeoff()) {
            super.startTakeoffSequence(minUpwardVelocity, animationTicks);
            return;
        }
        if (!canStartTakeoffSequence()) {
            return;
        }

        phase2RiderTakeoffActive = true;
        entityData.set(DATA_PHASE2_RIDER_TAKEOFF, true);
        setRunning(false);
        setAccelerating(false);
        setDeltaMovement(Vec3.ZERO);
        if (!level().isClientSide) {
            lockRiderControls(PHASE2_RIDER_TAKEOFF_TICKS);
        }
        takeoffComponent.startTakeoff(
                PHASE2_RIDER_TAKEOFF_TICKS,
                PHASE2_RIDER_TAKEOFF_UPWARD_STEP
        );
        setGoingUp(true);
        setGoingDown(false);
    }

    @Override
    protected int getTakeoffLiftDelayTicks() {
        return phase2RiderTakeoffActive
                ? PHASE2_RIDER_TAKEOFF_LAUNCH_DELAY_TICKS
                : super.getTakeoffLiftDelayTicks();
    }

    @Override
    protected boolean shouldIgnoreGroundedTakeoffRecovery() {
        return phase2RiderTakeoffActive || super.shouldIgnoreGroundedTakeoffRecovery();
    }

    @Override
    protected void onTakeoffEnded() {
        super.onTakeoffEnded();
        if (!phase2RiderTakeoffActive) {
            return;
        }

        phase2RiderTakeoffActive = false;
        entityData.set(DATA_PHASE2_RIDER_TAKEOFF, false);
        setGoingUp(false);
        setGoingDown(false);
        setAccelerating(false);
        if (isFlying()) {
            setDeltaMovement(Vec3.ZERO);
            primeRiderFlightIdleMode();
        }
    }

    private boolean isPhase2RiderTakeoff() {
        return isPhase2Active() && getControllingPassenger() instanceof Player;
    }

    public boolean isPhase2RiderTakeoffAnimating() {
        return level().isClientSide
                ? entityData.get(DATA_PHASE2_RIDER_TAKEOFF)
                : phase2RiderTakeoffActive;
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

    @Override
    protected void resetTimeFlyingAfterLanding() {
        timeFlying = 0;
    }

    public void handleAiLandingComplete() {
        if (isInWaterOrBubble()) {
            suppressSleep(60);
            completeTouchdownLanding(LandingSource.AI);
            return;
        }
        if (!level().isClientSide) {
            long now = level().getGameTime();
            if (now - lastAiLandedAnimTick >= 15L) {
                String landedAnim = isPhase2Active() ? "phase2_landed" : "landed";
                triggerAnim(AnimationHelper.MOVEMENT_CONTROLLER, landedAnim);
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
        completeTouchdownLanding(LandingSource.AI);
        startStandardLandedRecovery(isPhase2Active() ? PHASE2_LANDED_RECOVERY_TICKS : LANDED_RECOVERY_TICKS);
    }

    @Override
    protected Brain.Provider<Ignivorus> brainProvider() {
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
    public double getAiTargetAirborneHeight(LivingEntity target) {
        return Math.max(AI_AIR_COMBAT_SETTINGS.targetAirborneHeight(), target.getBbHeight() * 0.75D);
    }

    @Override
    protected void completeGroundedAerialRecoveryLanding() {
        handleAiLandingComplete();
    }

    @Override
    public float getFlightSpeed() {
        return (float) this.getAttributeValue(Attributes.FLYING_SPEED);
    }

    @Override
    public double getPreferredFlightAltitude() {
        return 20.0D;
    }

    public boolean canBeBound() {
        return !isDying()
                && !isBreathingFire()
                && !areRiderControlsLocked()
                && getActiveAbility() == null;
    }

    @Override
    public boolean ignoresLeashPull() {
        return true;
    }

    public void applyConfiguredAttributes() {
        if (this.level().isClientSide) {
            return;
        }
        DragonAttributeConfig config = getConfiguredDragonAttributes();
        double attackDamage = config.abilityDamage("bite", 15.0D);

        applyConfiguredFlyingHealthAndArmor(config, BABY_MAX_HEALTH, BABY_ARMOR);
        setAttributeBase(Attributes.ATTACK_DAMAGE, isBaby() ? 0.0D : attackDamage);
        clampHealthToMax();
    }

    @Override
    public boolean canTakeoff() {
        return !isBaby()
                && (!isPhase2Active() || getControllingPassenger() instanceof Player)
                && !isFlying()
                && onGround()
                && !isInWaterOrBubble();
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
    protected boolean canApplyFlyingState(boolean flying) {
        return isAiWaterBreachTakeoffActive()
                || !(flying && !isVehicle() && (isInWater() || isInWaterOrBubble() || isInLava()));
    }

    @Override
    protected int getRedirectedFlyingTakeoffTicks() {
        return TAKEOFF_ANIMATION_TICKS;
    }

    @Override
    protected void onFlyingStarted() {
        enforcePrimaryMeleeForFlight(getControllingPassenger() instanceof Player p ? p : null);
        switchToAirNavigation();
        setRunning(false);
    }

    @Override
    protected boolean shouldRunTakeoffStateStarted(boolean wasTakeoff, boolean takeoff) {
        return takeoff;
    }

    @Override
    protected void onTakeoffStateStarted() {
        if (!isPhase2Active()) {
            triggerAnim(AnimationHelper.FLIGHT_CONTROLLER, AnimationHelper.TAKEOFF);
        }
        getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_TAKEOFF.get(), 1.0f, 1.0f, 69);
    }

    @Override
    protected void onLandingDataSet(boolean landing) {
    }

    @Override
    public int getFlightMode() {
        return evaluateStandardFlightMode(false);
    }

    public DragonFlightStateEvaluator.VisualState getVisualFlightState(float partialTick) {
        return evaluateVisualFlightState(partialTick, getFlightPitchRadians(partialTick));
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
            fireTime = 0;
            fireServerTarget = createInitialFireTarget();
        }
    }

    public int getFireBreathProgress() {
        return this.entityData.get(DATA_FIRE_BREATH_PROGRESS);
    }

    public void setFireBreathProgress(int progress) {
        this.entityData.set(DATA_FIRE_BREATH_PROGRESS, Mth.clamp(progress, 0, 40));
    }

    public float getFireBreathEnergy() {
        return this.entityData.get(DATA_FIRE_BREATH_ENERGY);
    }

    public void setFireBreathEnergy(float energy) {
        DragonBreathComponent.setEnergy(getFireBreathGauge(), energy, FIRE_BREATH_REARM_THRESHOLD);
    }

    public boolean hasFireBreathEnergy() {
        return DragonBreathComponent.canUse(getFireBreathGauge(), FIRE_BREATH_DEPLETED_THRESHOLD);
    }

    public boolean isFireBreathDepleted() {
        return this.entityData.get(DATA_FIRE_BREATH_DEPLETED);
    }

    public void setFireBreathDepleted(boolean depleted) {
        this.entityData.set(DATA_FIRE_BREATH_DEPLETED, depleted);
    }

    public boolean canUseFireBreath() {
        return DragonBreathComponent.canUse(getFireBreathGauge(), FIRE_BREATH_DEPLETED_THRESHOLD);
    }

    public boolean drainFireBreathEnergy(float amount) {
        return DragonBreathComponent.drain(getFireBreathGauge(), amount, FIRE_BREATH_DEPLETED_THRESHOLD, FIRE_BREATH_REARM_THRESHOLD);
    }

    private void tickFireBreathEnergy() {
        if (!isBreathingFire() && getFireBreathEnergy() < 1.0f) {
            float regen = (float) DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                    .extraDouble("fire_breath_regen_per_tick", FIRE_BREATH_ENERGY_REGEN);
            DragonBreathComponent.regen(getFireBreathGauge(), regen, FIRE_BREATH_REARM_THRESHOLD);
        }
    }

    public DragonBreathComponent.Gauge getFireBreathGauge() {
        return new DragonBreathComponent.Gauge() {
            @Override
            public float getEnergy() {
                return Ignivorus.this.getFireBreathEnergy();
            }

            @Override
            public void setEnergyRaw(float energy) {
                Ignivorus.this.entityData.set(DATA_FIRE_BREATH_ENERGY, Mth.clamp(energy, 0.0F, 1.0F));
            }

            @Override
            public boolean isDepleted() {
                return Ignivorus.this.isFireBreathDepleted();
            }

            @Override
            public void setDepleted(boolean depleted) {
                Ignivorus.this.setFireBreathDepleted(depleted);
            }
        };
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

    public Vec3 getFireBreathStartAnchor(float partialTicks) {
        Vec3 clientBone = getBonePositionForHitbox("fireBoneOrigin");
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
        float pitchDeg;
        if (isVehicle() && isFlying()) {
            Entity rider = getControllingPassenger();
            if (rider != null) {
                pitchDeg = rider.getXRot();
                pitchDeg = Mth.clamp(pitchDeg, -20.0F, 20.0F);
            } else {
                pitchDeg = Mth.lerp(partialTicks, this.xRotO, this.getXRot());
            }
        } else {
            pitchDeg = Mth.lerp(partialTicks, this.xRotO, this.getXRot());
        }
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
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

        fireAimDir = DragonAimHelper.blendDirection(fireAimDir, clamped, smooth, 0.35D);
        return fireAimDir;
    }

    private Vec3 computeRawFireAimDirection(Vec3 start) {
        Vec3 riderLook = DragonAimHelper.riderViewDirection(this);
        if (riderLook != null) {
            return riderLook;
        }

        if (!level().isClientSide) {
            tickFireTargeting(start);
        }

        if (fireServerTarget != null) {
            Vec3 towardTarget = DragonAimHelper.directionTo(start, fireServerTarget);
            if (towardTarget != null) {
                return towardTarget;
            }
        }

        return DragonAimHelper.fallbackHeadDirection(this);
    }

    private Vec3 clampFireDirection(Vec3 desiredDir) {
        return DragonAimHelper.clampDirectionToHead(
                desiredDir,
                this.yHeadRot,
                this.getXRot(),
                MAX_FIRE_YAW_DEG,
                MAX_FIRE_PITCH_DEG
        );
    }

    private void resetFireAimDirection() {
        fireAimDir = null;
    }
    private Vec3 createInitialFireTarget() {
        LivingEntity target = getTarget();
        Vec3 shootFrom = getFireBreathStartAnchor(1.0f);
        if (shootFrom == null) {
            shootFrom = position().add(0, getBbHeight() * 0.5, 0);
        }

        if (target != null && target.isAlive()) {
            Vec3 randomOffset = new Vec3(
                -50 + random.nextFloat() * 100F,
                -20 + random.nextFloat() * 40F,
                -50 + random.nextFloat() * 100F
            );
            return target.position().add(randomOffset);
        } else {
            Vec3 forward = new Vec3(0, random.nextBoolean() ? 50 : 10, 30)
                .yRot((float) Math.toRadians(-this.yBodyRot));
            return shootFrom.add(forward);
        }
    }

    private void tickFireTargeting(Vec3 shootFrom) {
        fireTime++;

        LivingEntity target = getTarget();
        Vec3 currentTarget = fireServerTarget != null ? fireServerTarget : shootFrom;

        if (target != null && target.isAlive()) {
            float maxFireTime = 60.0F;
            float time = (float) fireTime / maxFireTime;
            float accuracy = 1.0F - (Math.min(0.75F, time) / 0.75F);
            Vec3 wobbleOffset = new Vec3(
                Math.sin(tickCount * 0.2F) * 4.0,
                Math.sin(tickCount * 0.15F) * 2.0,
                Math.cos(tickCount * 0.2F) * -4.0
            ).yRot((float) Math.toRadians(-this.yBodyRot)).scale(accuracy);
            Vec3 targetPoint = target.getEyePosition().add(0, -0.2, 0).add(wobbleOffset);
            fireServerTarget = targetPoint.subtract(currentTarget).scale(0.1F).add(currentTarget);
        } else {
            Vec3 sweepOffset = new Vec3(
                Math.sin(tickCount * 0.1F) * 10,
                0,
                6
            ).yRot((float) Math.toRadians(-this.yBodyRot));
            Vec3 sweepTarget = shootFrom.add(sweepOffset);
            fireServerTarget = sweepTarget.subtract(currentTarget).scale(0.1F).add(currentTarget);
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

    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        if (isBaby() || isBulldozing()) {
            return null;
        }
        if (isPhase2Active()) {
            if (isFlying()) {
                return ModAbilities.IGNIVORUS_BITE;
            }
            return getMeleeMode() == 1 ? ModAbilities.IGNIVORUS_STOMP : ModAbilities.IGNIVORUS_WING_SWIPE;
        }

        return getMeleeMode() == 1 ? ModAbilities.IGNIVORUS_BODY_SLAM : ModAbilities.IGNIVORUS_BITE;
    }

    @Override
    public int getDrinkingDurationTicks() {
        return DRINKING_ANIMATION_TICKS;
    }

    public boolean isBulldozing() {
        return this.entityData.get(DATA_BULLDOZING);
    }

    @Override
    public double getDrinkingReach() {
        return 7.5D;
    }

    @Override
    public void startDrinkingAnimation() {
        animationHandler.triggerDrinkingAnimation();
    }

    @Override
    public void stopDrinkingAnimation() {
        stopTriggeredAnimation(
                IgnivorusAnimationHandler.MOVEMENT_CONTROLLER,
                IgnivorusAnimationHandler.DRINKING_TRIGGER
        );
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
        return ModAbilities.IGNIVORUS_HURT;
    }

    @Override
    protected DragonAbilityType<?, ?> getDeathAbilityType() {
        return ModAbilities.IGNIVORUS_DIE;
    }

    @Override
    public int getDeathAnimationDurationTicks() {
        return 80;
    }

    @Override
    public void onDeathAbilityStarted() {
        setBreathingFire(false);
        clearFireBreathPath();
        super.onDeathAbilityStarted();
    }

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

    @Override
    protected void tickPitchingLandingBlendTimer() {
        tickRiderLandingBlendTimer();
    }

    @Override
    protected void triggerPitchingLandingBlend() {
        triggerRiderLandingBlend();
    }

    @Override
    protected boolean isStandardPitchActionBlocked() {
        return isBreathingFire();
    }

    @Override
    protected float getStandardAiLandingPitchDegrees() {
        return 32.0F;
    }

    private void tickRiderLandingBlendTimer() {
        tickStandardRiderLandingBlend(new RiderLandingBlendHooks() {
            @Override
            public void onWaterFlightCleared() {
                clearAerialStateAndUseGroundNavigation();
                timeFlying = 0;
            }

            @Override
            public boolean shouldClearFlightStateInWater() {
                return shouldClearRiderFlightStateInWater();
            }

            @Override
            public boolean isLandingBlendSynced() {
                return isRiderLandingBlendActive();
            }

            @Override
            public void clearLandingBlendSync() {
                entityData.set(DATA_RIDER_LANDING_BLEND, false);
            }

            @Override
            public void onRiderLanded() {
                String landedAnim = isPhase2Active() ? "phase2_landed" : "landed";
                triggerAnim(AnimationHelper.MOVEMENT_CONTROLLER, landedAnim);
                if (isPhase2Active()) {
                    getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_PHASE2_LANDED.get(), 1.0f, 1.0f, 40);
                } else {
                    getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LANDED.get(), 1.0f, 1.0f, 42);
                }
                completeTouchdownLanding(LandingSource.RIDER);
                lockRiderControls(13);
            }
        });
    }

    private void triggerRiderLandingBlend() {
        triggerRiderLandingBlendTicks(RIDER_LANDING_BLEND_DURATION);
        if (!level().isClientSide) {
            this.entityData.set(DATA_RIDER_LANDING_BLEND, true);
        }
    }

    public boolean isRiderLandingBlendActive() {
        return this.entityData.get(DATA_RIDER_LANDING_BLEND);
    }

    private void tickTerrainClearing() {
        if (level().isClientSide || this.isBaby() || !this.isAlive()) {
            return;
        }
        if (!DragonGriefingRules.canDestroyBlocks(level())) {
            return;
        }

        boolean isBeingRidden = this.isVehicle();
        Vec3 velocity = this.getDeltaMovement();
        if (isBeingRidden && bulldozing) {
            clearBulldozeTunnel(getBulldozeTunnelDirection(velocity));
            return;
        }

        double speed = velocity.horizontalDistanceSqr();
        boolean collisionStuck = this.horizontalCollision || this.isInWall();
        boolean chasingTarget = this.getTarget() != null && this.getTarget().isAlive();
        if (!isBeingRidden && speed < 0.01 && !(collisionStuck && chasingTarget)) {
            return;
        }

        int tickInterval = isBeingRidden ? 1 : 3;
        if (this.tickCount % tickInterval != 0) {
            return;
        }

        AABB rawBounds = this.getBoundingBox();
        AABB bounds = rawBounds.inflate(0.1);
        if (isBeingRidden) {
            Vec3 planarVelocity = new Vec3(velocity.x, 0.0, velocity.z);
            if (planarVelocity.lengthSqr() > 0.0004) {
                double reach = isBeingRidden ? 1.1 : 0.6;
                Vec3 forwardProbe = planarVelocity.normalize().scale(reach);
                bounds = bounds.expandTowards(forwardProbe.x, 0.0, forwardProbe.z);
            }
        }
        int minX = Mth.floor(bounds.minX);
        int maxX = Mth.floor(bounds.maxX);
        int minZ = Mth.floor(bounds.minZ);
        int maxZ = Mth.floor(bounds.maxZ);
        int baseY = Mth.floor(rawBounds.minY);
        int minBreakY = baseY + 1;
        int maxY = Mth.floor(bounds.maxY);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int brokenThisTick = 0;
        int maxBreakPerTick = isBeingRidden ? 24 : 8;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minBreakY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (brokenThisTick >= maxBreakPerTick) {
                        return;
                    }

                    cursor.set(x, y, z);
                    if (!level().hasChunkAt(cursor)) {
                        continue;
                    }

                    BlockState state = level().getBlockState(cursor);
                    if (state.isAir() || !state.getFluidState().isEmpty()) {
                        continue;
                    }
                    float hardness = state.getDestroySpeed(level(), cursor);
                    if (hardness < 0 || hardness > 5.0F || state.hasBlockEntity()) {
                        continue;
                    }

                    level().destroyBlock(cursor, true, this);
                    brokenThisTick++;
                }
            }
        }
    }

    private Vec3 getBulldozeTunnelDirection(Vec3 velocity) {
        Entity rider = getControllingPassenger();
        Vec3 look = rider instanceof LivingEntity living ? living.getLookAngle() : getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(velocity.x, 0.0D, velocity.z);
        }
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = Vec3.directionFromRotation(0.0F, getYRot());
        }
        return horizontal.lengthSqr() > 1.0E-6D ? horizontal.normalize() : Vec3.ZERO;
    }

    private void clearBulldozeTunnel(Vec3 forward) {
        if (forward.lengthSqr() < 1.0E-6D) {
            return;
        }

        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        if (right.lengthSqr() < 1.0E-6D) {
            return;
        }
        right = right.normalize();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Set<BlockPos> visited = new HashSet<>();
        int[] brokenThisTick = new int[] {0};

        cutBulldozeVolume(
                getBulldozeHeadOrigin(forward).add(forward.scale(0.35D)),
                forward,
                right,
                BULLDOZE_TUNNEL_REACH,
                BULLDOZE_TUNNEL_HALF_WIDTH,
                BULLDOZE_TUNNEL_HEIGHT,
                visited,
                cursor,
                brokenThisTick
        );
        clearBulldozeBodyCollisionVolume(forward, visited, cursor, brokenThisTick);
    }

    private void clearBulldozeBodyCollisionVolume(Vec3 forward, Set<BlockPos> visited, BlockPos.MutableBlockPos cursor, int[] brokenThisTick) {
        if (brokenThisTick[0] >= BULLDOZE_TUNNEL_MAX_BREAKS_PER_TICK) {
            return;
        }

        AABB bodyClearance = getBoundingBox()
                .inflate(0.15D, 0.05D, 0.15D)
                .expandTowards(forward.scale(BULLDOZE_BODY_COLLISION_REACH));
        int floorSafeY = Mth.floor(getBoundingBox().minY) + 1;
        int minX = Mth.floor(bodyClearance.minX);
        int maxX = Mth.floor(bodyClearance.maxX);
        int minY = Math.max(floorSafeY, Mth.floor(bodyClearance.minY));
        int maxY = Mth.floor(bodyClearance.maxY);
        int minZ = Mth.floor(bodyClearance.minZ);
        int maxZ = Mth.floor(bodyClearance.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (brokenThisTick[0] >= BULLDOZE_TUNNEL_MAX_BREAKS_PER_TICK) {
                        return;
                    }

                    cursor.set(x, y, z);
                    BlockPos immutablePos = cursor.immutable();
                    if (isProtectedBulldozeFloorBlock(immutablePos, forward)) {
                        continue;
                    }
                    if (!visited.add(immutablePos) || !level().hasChunkAt(immutablePos)) {
                        continue;
                    }

                    BlockState state = level().getBlockState(immutablePos);
                    if (!canBulldozeDestroyBlock(state, immutablePos)) {
                        continue;
                    }

                    level().destroyBlock(immutablePos, true, this);
                    brokenThisTick[0]++;
                }
            }
        }
    }

    private void cutBulldozeVolume(Vec3 origin, Vec3 forward, Vec3 right, double reach, double halfWidth, int height, Set<BlockPos> visited, BlockPos.MutableBlockPos cursor, int[] brokenThisTick) {
        int floorSafeY = Mth.floor(getBoundingBox().minY) + 1;
        int minBreakY = Math.max(floorSafeY, Mth.floor(origin.y - height * 0.45D));
        int maxBreakY = minBreakY + height - 1;

        for (double forwardOffset = 0.5D; forwardOffset <= reach; forwardOffset += 0.75D) {
            Vec3 forwardPoint = origin.add(forward.scale(forwardOffset));
            for (double sideOffset = -halfWidth; sideOffset <= halfWidth; sideOffset += 0.75D) {
                Vec3 sample = forwardPoint.add(right.scale(sideOffset));
                for (int y = minBreakY; y <= maxBreakY; y++) {
                    if (brokenThisTick[0] >= BULLDOZE_TUNNEL_MAX_BREAKS_PER_TICK) {
                        return;
                    }

                    cursor.set(Mth.floor(sample.x), y, Mth.floor(sample.z));
                    BlockPos immutablePos = cursor.immutable();
                    if (!visited.add(immutablePos) || !level().hasChunkAt(immutablePos)) {
                        continue;
                    }

                    BlockState state = level().getBlockState(immutablePos);
                    if (!canBulldozeDestroyBlock(state, immutablePos)) {
                        continue;
                    }

                    level().destroyBlock(immutablePos, true, this);
                    brokenThisTick[0]++;
                }
            }
        }
    }

    private boolean canBulldozeDestroyBlock(BlockState state, BlockPos pos) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        float hardness = state.getDestroySpeed(level(), pos);
        return hardness >= 0.0F && hardness <= 5.0F && !state.hasBlockEntity();
    }

    private boolean isProtectedBulldozeFloorBlock(BlockPos pos, Vec3 forward) {
        int floorY = Mth.floor(getBoundingBox().minY);
        if (pos.getY() > floorY) {
            return false;
        }

        Vec3 center = getBoundingBox().getCenter();
        double currentFront = center.dot(forward) + Math.max(getBbWidth() * 0.5D, 0.75D);
        double blockProjection = (pos.getX() + 0.5D) * forward.x + (pos.getZ() + 0.5D) * forward.z;
        return blockProjection <= currentFront;
    }

    private Vec3 getBulldozeHeadOrigin(Vec3 forward) {
        Vec3 head = getBonePositionForHitbox("headController");
        if (head != null) {
            return head;
        }
        return getBoundingBox().getCenter().add(forward.scale(getBbWidth() * BULLDOZE_HEAD_FORWARD_FALLBACK));
    }

    private void updateSittingProgress() {
        tickSitTransition(
                getSitDownAnimationTicks(),
                getSitUpAnimationTicks(),
                animationHandler::triggerSitDownAnimation,
                animationHandler::triggerSitUpAnimation
        );
    }

    @Override
    public float maxSitTicks() {
        return getSitDownAnimationTicks();
    }

    private int getSitDownAnimationTicks() {
        return 38;
    }

    private int getSitUpAnimationTicks() {
        return 38;
    }

    private int getFallAsleepAnimationTicks() {
        return 38;
    }

    private int getWakeUpAnimationTicks() {
        return 38;
    }
    @Override
    protected float getBarrelRollInputSpeed() {
        return BARREL_ROLL_INPUT_SPEED;
    }

    @Override
    protected boolean canUseBarrelRoll() {
        return isFlying()
                && !isInWaterOrBubble()
                && !isLeaping()
                && !areRiderControlsLocked();
    }

    public boolean isRiderPitchKeyMode() {
        return this.entityData.get(DATA_PITCH_KEY_MODE);
    }
    public void setRiderPitchKeyMode(boolean enabled) {
        this.entityData.set(DATA_PITCH_KEY_MODE, enabled);
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(movementController, vocalController, actionController, fastActionController, flightController, interactionController);
    }

    private void setupAnimationControllers() {
        AnimationHelper.registerSoundKeyframes(this, movementController, actionController,
                fastActionController, flightController, vocalController, interactionController);
        AnimationHelper.registerGrumbles(vocalController, this);
        animationHandler.setupMovementController(movementController);
        animationHandler.setupFastActionController(fastActionController);
        animationHandler.setupFlightController(flightController);
        animationHandler.setupInteractionController(interactionController);
        animationHandler.setupActionController(actionController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return dragonCache;
    }

    @Override
    public Map<String, VocalEntry> getVocalEntries() {
        return VOCAL_ENTRIES;
    }

    @Override
    public DragonSoundProfile getSoundProfile() {
        return IgnivorusSoundProfile.INSTANCE;
    }

    private final Map<String, Vec3> serverBonePositionCache = new ConcurrentHashMap<>();

    public void setServerBonePosition(String boneName, Vec3 position) {
        if (boneName == null || position == null) return;
        this.serverBonePositionCache.put(boneName, position);
    }

    public Vec3 getBonePositionForHitbox(String boneName) {
        if (boneName == null) return null;
        if (this.level().isClientSide) {
            return this.clientLocatorCache.get(boneName);
        } else {
            return this.serverBonePositionCache.get(boneName);
        }
    }

    @Override
    public boolean canBreed() {
        return this.isTame() && !this.isBaby() && this.getHealth() >= this.getMaxHealth() && this.isInLove();
    }

    @Override
    protected Supplier<? extends Block> getEggBlock() {
        return ModBlocks.IGNIVORUS_EGG;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob otherParent) {
        return createBreedOffspring(level, otherParent, ModEntities.IGNIVORUS.get(), Ignivorus::applyConfiguredAttributes);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        saveRideableData(tag);
        tag.putInt("TimeFlying", timeFlying);
        this.combatManager.saveToNBT(tag);
        tag.putInt("FeedingCooldownTicks", Math.max(0, this.entityData.get(DATA_FEEDING_COOLDOWN)));
        tag.putBoolean("Bulldozing", bulldozing);
        tag.putInt("BulldozeCooldownTicks", Math.max(0, bulldozeCooldownTicks));
        tag.putBoolean("Phase2Active", phase2Active);
        tag.putInt("Phase2CooldownTicks", Math.max(0, phase2CooldownTicks));
        tag.putBoolean("WildPhase2UltimateTriggered", wildPhase2UltimateTriggered);
        tag.putInt("LeapCooldownTicks", Math.max(0, leapCooldownTicks));
        tag.putFloat("FireBreathEnergy", getFireBreathEnergy());
        tag.putBoolean("FireBreathDepleted", isFireBreathDepleted());
        tamingController.save(tag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadRideableData(tag);
        this.timeFlying = tag.getInt("TimeFlying");
        this.combatManager.loadFromNBT(tag);
        if (tag.contains("FeedingCooldownTicks")) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, tag.getInt("FeedingCooldownTicks")));
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
        wildPhase2UltimateTriggered = tag.contains("WildPhase2UltimateTriggered")
                ? tag.getBoolean("WildPhase2UltimateTriggered")
                : phase2Active;
        if (tag.contains("Phase2CooldownTicks")) {
            phase2CooldownTicks = Math.max(0, tag.getInt("Phase2CooldownTicks"));
        }
        boolean interruptedLeap = tag.getBoolean("Leaping")
                || tag.getInt("LeapAnimState") != LEAP_STATE_NONE;
        leaping = false;
        leapWasVehicle = false;
        leapAnimState = LEAP_STATE_NONE;
        leapVelocity = Vec3.ZERO;
        leapArcDirection = Vec3.ZERO;
        leapArcTick = 0;
        leapWindupTicks = 0;
        leapImpactRecoveryTicks = 0;
        leapImpactTriggered = false;
        wasAirborneBeforeLanding = false;
        leapGroundedTicks = 0;
        leapMovement.clear();
        this.entityData.set(DATA_LEAPING, false);
        this.entityData.set(DATA_LEAP_ANIM_STATE, LEAP_STATE_NONE);
        if (interruptedLeap) {
            clearRiderControlLock();
            setDeltaMovement(Vec3.ZERO);
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
        bulldozeWasVehicle = false;
        phase2WasVehicle = false;
        tamingController.load(tag);
        applyConfiguredAttributes();
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
    public void setTarget(@Nullable LivingEntity target) {
        if (this.isBaby()) {
            super.setTarget(null);
            return;
        }
        if (isTamingStunned() && target != null) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean isWildAggressionEnabled() {
        if (isTame() || isBaby()) {
            return false;
        }
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        return config.extraBoolean("aggressive_wild", false);
    }

    public static boolean canSpawnHere(EntityType<Ignivorus> type,
                                       LevelAccessor level,
                                       MobSpawnType reason,
                                       BlockPos pos,
                                       RandomSource random) {
        if (!Animal.checkAnimalSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        return DragonSpawnRules.hasDryGroundSpawnSpace(level, pos)
                && DragonSpawnRules.passesNearbyDragonDensityCheck(level, reason, pos, Ignivorus.class);
    }
    private void tickScreenShake() {
        screenShakeComponent.tick();
    }

    @Override
    protected ScreenShakeComponent getScreenShakeComponent() {
        return screenShakeComponent;
    }

    @Override
    public double getShakeDistance() {
        return 30.0;
    }

    @Override
    protected void dropAdditionalDeathLootAfterBase(@NotNull DamageSource source) {
        if (!level().isClientSide && getGender() == DragonGender.FEMALE) {
            DragonLootTables.dropEntityLoot(this, DragonLootTables.IGNIVORUS_FEMALE_DEATH, source);
        }

    }
    @Override
    public int getMaxHeadXRot() {
        return 180;
    }
}
