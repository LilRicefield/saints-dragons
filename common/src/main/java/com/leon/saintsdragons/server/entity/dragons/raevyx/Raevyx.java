// zap van dink
package com.leon.saintsdragons.server.entity.dragons.raevyx;

import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.particle.raevyx.*;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.common.registry.ModBlocks;
import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.server.ai.goals.base.*;
import com.leon.saintsdragons.server.ai.goals.raevyx.RaevyxFlightGoal;
import com.leon.saintsdragons.server.ai.goals.raevyx.*;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.base.DragonSitTransitionController;
import com.leon.saintsdragons.server.entity.base.DragonVariant;
import com.leon.saintsdragons.server.entity.base.DragonVariantSet;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.interfaces.*;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxSoundProfile;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxTamingHandler;
import com.leon.saintsdragons.server.entity.controller.raevyx.RaevyxRiderController;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import com.leon.saintsdragons.server.flight.DragonFlightVisuals;
import com.leon.saintsdragons.server.flight.DragonRiderFlight;
import com.leon.saintsdragons.server.entity.effect.raevyx.RaevyxGroundRendTrailEntity;
import com.leon.saintsdragons.server.entity.component.ScreenShakeComponent;
import com.leon.saintsdragons.server.entity.ability.DragonAimHelper;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxDiveImpactAbility;
import com.leon.saintsdragons.server.entity.component.DragonMotionMath;
import com.leon.saintsdragons.server.entity.component.DragonForwardMovementComponent;
import com.leon.saintsdragons.server.loot.DragonLootTables;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import java.util.Map;
import com.leon.saintsdragons.server.world.DragonSpawnRules;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.util.GeckoLibUtil;
import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Raevyx extends RideableFlyingDragon implements ShakesScreen {
    @Override
    protected ResourceLocation getDragonAttributesId() {
        return DragonAttributeConfigLoader.RAEVYX_ID;
    }

    public static final double RIDER_WALK_SPEED = 0.20D;
    public static final double RIDER_RUN_SPEED = 0.35D;
    private static final float TAMING_HEALTH_RATIO = 1.0F / 3.0F;
    private static final float DEFAULT_DASH_DAMAGE = 10.0F;
    private static final double GROUND_REND_BOLT_LINK_START_REACH = 1.1D;
    private static final double GROUND_REND_MAX_LINK_DISTANCE_SQR = 16.0D;
    private static final int GROUND_REND_BOLT_LIFETIME = 5;
    public static final int VARIANT_DEFAULT = 0;
    public static final int VARIANT_NIGHT_GOLD = 1;
    private static final DragonVariantSet VARIANTS = DragonVariantSet.of(
            DragonVariant.of(VARIANT_DEFAULT, "default", 90),
            DragonVariant.of(VARIANT_NIGHT_GOLD, "night_gold", 10)
    );
    public static final int MIN_AMBIENT_DELAY = 200;
    public static final int MAX_AMBIENT_DELAY = 600;
    public static final float MODEL_SCALE = 1.0f;
    public static final int TAKEOFF_ANIMATION_TICKS = 31;
    private static final int LANDED_RECOVERY_TICKS = 38;
    public static final int AGGRO_TTL_TICKS = 200;
    public static final double BREED_PARTNER_RANGE = 8.0D;
    public static final double BREED_DISTANCE_SQR = 16.0D;
    private static final int DODGE_DURATION_TICKS = 12;
    private static final int DODGE_IFRAMES_TICKS = 8;
    private static final int RIDER_DODGE_COOLDOWN_TICKS = 30;
    private static final int FLEX_CONTROL_LOCK_TICKS = 65;
    private static final int FLEX_COOLDOWN_TICKS = 120;
    private static final int AI_DODGE_COOLDOWN_TICKS = 60;
    private static final double DODGE_DISTANCE_BLOCKS = 10.0D;
    private static final double AIR_DODGE_DISTANCE_MULTIPLIER = 3.0D;
    private static final double DASH_NUDGE_DRAG = 0.9D;
    private static final float REACTIVE_HIT_DODGE_CHANCE = 0.35F;
    public static final EntityDataAccessor<Boolean> DATA_LANDED = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> DATA_SCREEN_SHAKE_AMOUNT = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> DATA_BEAMING = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_BEAM_GLOW = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_RIDER_LANDING_BLEND = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_BEAM_END_SET = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> DATA_BEAM_END_X = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_BEAM_END_Y = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_BEAM_END_Z = SynchedEntityData.defineId(Raevyx.class,EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> DATA_BEAM_START_SET = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> DATA_BEAM_START_X = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_BEAM_START_Y = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_BEAM_START_Z = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> DATA_DODGING = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_DASHING = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> DATA_RIDER_NUDGE_TICKS = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> DATA_RIDER_NUDGE_X = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_RIDER_NUDGE_Z = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_RIDER_NUDGE_DRAG = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> DATA_LAST_DASH_RIGHT = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_GROUND_RENDING = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_GROUND_REND_X = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GROUND_REND_Z = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Integer> DATA_FEEDING_COOLDOWN = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DATA_TAMING_STUNNED = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> DATA_FLIGHT_PITCH = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> DATA_PITCH_KEY_MODE = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> DATA_BEAM_ENERGY = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> DATA_BEAM_DEPLETED = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> DATA_ACCUMULATED_ROLL = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> DATA_CUSTOM_DIVE_LOOP_ENABLED = SynchedEntityData.defineId(Raevyx.class, EntityDataSerializers.BOOLEAN);
    public static final float MAX_BEAM_YAW_DEG = 40.0f;
    public static final float MAX_BEAM_PITCH_DEG = 50.0f;
    public static final float RIDER_KEY_PITCH_DEG = 25.0f;
    private static final int RIDER_LANDING_BLEND_DURATION = 5; // ticks to keep landing blend active after triggering
    private static final double BABY_MAX_HEALTH = 60.0D;
    private static final Map<String, VocalEntry> VOCAL_ENTRIES = new VocalEntryBuilder()
            .add("grumble1", AnimationHelper.VOCAL_CONTROLLER, "animation.raevyx.grumble1", ModSounds.RAEVYX_GRUMBLE_1, 0.8f, 0.95f, 0.1f, false, false, false)
            .add("grumble2", AnimationHelper.VOCAL_CONTROLLER, "animation.raevyx.grumble2", ModSounds.RAEVYX_GRUMBLE_2, 0.8f, 0.95f, 0.1f, false, false, false)
            .add("grumble3", AnimationHelper.VOCAL_CONTROLLER, "animation.raevyx.grumble3", ModSounds.RAEVYX_GRUMBLE_3, 0.8f, 0.95f, 0.1f, false, false, false)
            .add("roar", RaevyxAnimationHandler.FAST_ACTION_CONTROLLER, "animation.raevyx.roar", ModSounds.RAEVYX_ROAR, 1.4f, 0.9f, 0.15f, false, false, false)
            .add("roar_ground", RaevyxAnimationHandler.FAST_ACTION_CONTROLLER, "animation.raevyx.roar_ground", ModSounds.RAEVYX_ROAR, 1.4f, 0.9f, 0.15f, false, false, false)
            .add("roar_air", RaevyxAnimationHandler.FAST_ACTION_CONTROLLER, "animation.raevyx.roar_air", ModSounds.RAEVYX_ROAR, 1.4f, 0.9f, 0.15f, false, false, false)
            .add("flex", RaevyxAnimationHandler.MOVEMENT_CONTROLLER, "animation.raevyx.flex", ModSounds.RAEVYX_FLEX, 1.4f, 0.95f, 0.05f, false, false, false)
            .add("raevyx_hurt", AnimationHelper.INTERACTION_CONTROLLER, "animation.raevyx.hurt", ModSounds.RAEVYX_HURT, 1.2f, 0.95f, 0.1f, true, true, true)
            .add("raevyx_die", AnimationHelper.INTERACTION_CONTROLLER, "animation.raevyx.die", ModSounds.RAEVYX_DIE, 1.5f, 0.95f, 0.1f, false, true, true)
            .build();

    private final ScreenShakeComponent screenShakeComponent;
    public AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);
    public int timeFlying = 0;
    public boolean landingFlag = false;
    public boolean landedFlag = false;
    public int landingTimer = 0;
    public int landedTimer = 0;
    private final DragonFlightVisuals.State flightVisualState = new DragonFlightVisuals.State();
    private final DragonForwardMovementComponent dashDodgeNudge = new DragonForwardMovementComponent(
            this,
            new DragonForwardMovementComponent.StateAccess() {
                @Override
                public void start(int ticks, Vec3 velocity, boolean dashing, boolean dodging, double horizontalDrag) {
                    setRaevyxNudgeState(ticks, velocity, dashing, dodging, horizontalDrag);
                }

                @Override
                public int ticks() {
                    return getRaevyxNudgeTicks();
                }

                @Override
                public void setTicks(int ticks) {
                    setRaevyxNudgeTicks(ticks);
                }

                @Override
                public Vec3 velocity() {
                    return getRaevyxNudgeVector();
                }

                @Override
                public void setVelocity(Vec3 velocity) {
                    setRaevyxNudgeVelocity(velocity);
                }

                @Override
                public double horizontalDrag() {
                    return getRaevyxNudgeDrag();
                }

                @Override
                public void clear() {
                    clearRaevyxNudgeState();
                }
            }
    );
    int dodgeCooldownTicks = 0;
    int aiDodgeCooldownTicks = 0;
    int dodgeIFramesTicks = 0;
    private boolean lastDashWasRight = false;
    private final Map<Integer, Integer> dashHitCooldowns = new HashMap<>();
    private boolean groundRending = false;
    private float groundRendTravelSpeed = 0.0F;
    @Nullable
    private Vec3 groundRendLeftTrailAnchor = null;
    @Nullable
    private Vec3 groundRendRightTrailAnchor = null;
    private boolean allowGroundBeamDuringStorm = false;
    private final DragonSitTransitionController sitTransitions = new DragonSitTransitionController(this);
    private int postStandUnlockTicks = 0;
    private final RaevyxTamingHandler tamingController = new RaevyxTamingHandler(this);
    private final RaevyxInteractionHandler lightningInteractionHandler;
    private final RaevyxAnimationHandler animationHandler;
    private final RaevyxRiderController riderController;
    private final RaevyxDiveImpactAbility diveImpactAbility;
    private final AnimationController<Raevyx> movementController;
    private final AnimationController<Raevyx> transitionController;
    private final AnimationController<Raevyx> actionController;
    private final AnimationController<Raevyx> fastActionController;
    private final AnimationController<Raevyx> flightController;
    private final AnimationController<Raevyx> vocalController;
    private final AnimationController<Raevyx> interactionController;
    private int tempInvulnTicks = 0;
    private long lastLandingGameTime = Long.MIN_VALUE;
    private Vec3 prevClientBeamEnd = null;
    private Vec3 clientBeamEnd = null;
    private Vec3 beamLookLerp = null;
    private Vec3 beamAimDir = null;
    private float beamYawOffsetRad = 0.0f;
    private float beamPitchOffsetRad = 0.0f;
    private int beamTime = 0;
    private int beamAimRefreshTick = -1;
    private int beamPathRefreshTick = -1;
    private Vec3 beamServerTarget = null;

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new DragonFloatGoal(this, 0.004D, -0.03D, 0.1F));
        if (!this.isBaby()) {
            this.goalSelector.addGoal(3, new RaevyxAirCombatGoal(this));
            this.goalSelector.addGoal(3, new RaevyxGroundCombatGoal(this));
        }
        this.goalSelector.addGoal(5, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(7, new DragonFollowParentGoal<>(this, Raevyx.class, 1.15D));
        if (!this.isBaby()) {
            this.goalSelector.addGoal(7, new DragonBreedGoal<>(this, 1.0D, Raevyx.class, BREED_PARTNER_RANGE, BREED_DISTANCE_SQR));
        }
        this.goalSelector.addGoal(8, new DragonFollowOwnerGoal<>(this, DragonFollowOwnerGoal.FollowConfig.forRaevyx()) {
            @Override
            protected void startFollowTakeoff() {
                if (Raevyx.this.isFlying() || Raevyx.this.isTakeoff()) {
                    return;
                }
                Raevyx.this.startTakeoffSequence(0.12D, TAKEOFF_ANIMATION_TICKS);
            }
        });
        this.goalSelector.addGoal(9, new DragonGroundWanderGoal<>(this, 0.6D, 60));
        this.goalSelector.addGoal(10, new DragonWaterEscapeGoal<>(this, 8.0F, 0.12D));
        this.goalSelector.addGoal(11, new RaevyxFlightGoal(this));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !Raevyx.this.isVehicle() && super.canUse();
            }
        });
        this.goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 8.0F) {
            @Override
            public boolean canUse() {
                return !Raevyx.this.isVehicle() && super.canUse();
            }
        });
        this.targetSelector.addGoal(1, new DragonOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new DragonOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new DragonProtectBabiesGoal<>(this, Raevyx.class));  // Protect nearby babies
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> shouldAggroOnSight()));
        this.targetSelector.addGoal(6, new DragonRaidDefenseTargetGoal(this));
        this.targetSelector.addGoal(7, new DragonRandomHuntTargetGoal(
                this,
                80,
                () -> true,
                target -> DragonTargetingHelper.isTaggedHuntTarget(target, ModTags.EntityTypes.RAEVYX_TARGETS)
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return dragonCache;
    }

    private boolean getBooleanData(EntityDataAccessor<Boolean> accessor) {
        return this.entityData.get(accessor);
    }
    private void setBooleanData(EntityDataAccessor<Boolean> accessor, boolean value) {
        this.entityData.set(accessor, value);
    }
    private int getIntegerData(EntityDataAccessor<Integer> accessor) {
        return this.entityData.get(accessor);
    }

    private float getFloatData(EntityDataAccessor<Float> accessor) {
        return this.entityData.get(accessor);
    }

    public boolean isCustomDiveLoopEnabled() {
        return this.entityData.get(DATA_CUSTOM_DIVE_LOOP_ENABLED);
    }

    private void syncCustomDiveLoopEnabled() {
        boolean enabled = getConfiguredDragonAttributes().extraBoolean("dive_loop_enabled", true);
        if (this.entityData.get(DATA_CUSTOM_DIVE_LOOP_ENABLED) != enabled) {
            this.entityData.set(DATA_CUSTOM_DIVE_LOOP_ENABLED, enabled);
        }
    }

    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        if (isGroundRending()) {
            return false;
        }
        return switch (action) {
            case DOUBLE_TAP_A, DOUBLE_TAP_D, DOUBLE_TAP_W, DOUBLE_TAP_S,
                 ABILITY_USE, ABILITY_STOP -> true;
            default -> super.supportsRiderAction(action);
        };
    }

    public boolean canFeed() {
        int cooldownTicks = this.entityData.get(DATA_FEEDING_COOLDOWN);
        return cooldownTicks <= 0;
    }

    public void setFeedingCooldown(int ticks) {
        this.entityData.set(DATA_FEEDING_COOLDOWN, ticks);
    }

    @Override
    protected DragonVariantSet getVariantSet() {
        return VARIANTS;
    }

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
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID)
                .extraDouble("taming_stun_health", fallback);
        double clamped = Math.max(0.0D, Math.min(configured, this.getMaxHealth()));
        return (float) clamped;
    }

    public long getLastLandingGameTime() {
        return lastLandingGameTime;
    }

    @Override
    protected boolean isRiderTakeoffLocked() {
        return isGroundRending() || isTakeoffLocked();
    }

    @Override
    protected void afterStandardLandingStateReset() {
        this.setLanded(true);
        this.landingTimer = 0;
    }

    @Override
    protected void resetTimeFlyingAfterLanding() {
        this.timeFlying = 0;
    }

    @Override
    protected void onStandardServerLanding() {
        this.lastLandingGameTime = level().getGameTime();
    }

    @Override
    protected void switchToGroundNavigationAfterLanding() {
        switchToGroundNavigation();
    }

    public void handleAiLandingComplete() {
        if (isInWaterOrBubble()) {
            suppressSleep(60);
            markLandedNow();
            return;
        }
        if (!level().isClientSide) {
            triggerAnim(AnimationHelper.MOVEMENT_CONTROLLER, AnimationHelper.LANDED);
            getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_LANDED.get(), 1.0f, 1.0f, 72);
            suppressSleep(60);
        }
        markLandedNow();
        startStandardLandedRecovery(LANDED_RECOVERY_TICKS);
    }

    @Override
    protected void completeGroundedAerialRecoveryLanding() {
        handleAiLandingComplete();
    }

    @Override
    public boolean isInSittingPose() {
        return super.isInSittingPose() && !(this.isVehicle() || this.isPassenger() || this.isFlying());
    }

    public Raevyx(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.screenShakeComponent = new ScreenShakeComponent(this, DATA_SCREEN_SHAKE_AMOUNT, 0.34F);
        this.setMaxUpStep(1.25F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.FENCE, -1.0F);
        this.lightningInteractionHandler = new RaevyxInteractionHandler(this);
        this.animationHandler = new RaevyxAnimationHandler(this);
        this.riderController = new RaevyxRiderController(this);
        this.diveImpactAbility = new RaevyxDiveImpactAbility(this);
        this.movementController = new AnimationController<>(this, "movement", 2, animationHandler::movementPredicate);
        this.transitionController = new AnimationController<>(this, AnimationHelper.TRANSITION_CONTROLLER, 4, AnimationHelper::transitionIdle);
        this.actionController = new AnimationController<>(this, RaevyxAnimationHandler.ACTION_CONTROLLER, 3, state -> {
            if (isTamingStunned()) {
                return PlayState.STOP;
            }
            return animationHandler.raevyxActionPredicate(state);
        });
        this.fastActionController = new AnimationController<>(this, RaevyxAnimationHandler.FAST_ACTION_CONTROLLER, 1, animationHandler::raevyxFastActionPredicate);
        this.flightController = AnimationHelper.createFlightController(this, getFlightAnimationTransitionTicks(), animationHandler::flightPredicate);
        this.vocalController = new AnimationController<>(this, AnimationHelper.VOCAL_CONTROLLER, 2, AnimationHelper::vocalIdle);
        this.interactionController = new AnimationController<>(this, AnimationHelper.INTERACTION_CONTROLLER, 1, AnimationHelper::interactionIdle);
        setupAnimationControllers();
        seedAmbientSoundTimer(MIN_AMBIENT_DELAY, MAX_AMBIENT_DELAY, 80);

        if (!level.isClientSide) {
            applyConfiguredAttributes();
        }
    }

    @Override
    protected DragonRiderFlight.Config getRiderFlightConfig() {
        return new DragonRiderFlight.Config(
                true,
                5,
                0.55D,
                TAKEOFF_ANIMATION_TICKS,
                0.45D,
                TAKEOFF_ANIMATION_TICKS
        );
    }

    @Override
    protected void onRiderDiveExited(Player rider, double diveIntensity) {
        awardDiveAdvancement(rider);
    }

    private void awardDiveAdvancement(Player rider) {
        if (!(rider instanceof ServerPlayer serverPlayer)) {
            return;
        }
        var advancement = serverPlayer.server.getAdvancements()
                .getAdvancement(SaintsDragonsCommon.rl("raevyx_dive_exit"));
        if (advancement != null) {
            serverPlayer.getAdvancements().award(advancement, "raevyx_dive_exit");
        }
    }

    @Override
    protected void onTakeoffStarted() {
        this.timeFlying = 0;
        this.landingFlag = false;
        this.landingTimer = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, config.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, 0.55D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.FLYING_SPEED, config.flyingSpeed())
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, config.armor());
    }
    private int hurtSoundCooldown = 0;
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        this.entityData.define(DATA_BEAMING, false);
        this.entityData.define(DATA_BEAM_GLOW, false);
        this.entityData.define(DATA_RIDER_LANDING_BLEND, false);
        this.entityData.define(DATA_BEAM_END_SET, false);
        this.entityData.define(DATA_BEAM_END_X, 0f);
        this.entityData.define(DATA_BEAM_END_Y, 0f);
        this.entityData.define(DATA_BEAM_END_Z, 0f);
        this.entityData.define(DATA_BEAM_START_SET, false);
        this.entityData.define(DATA_BEAM_START_X, 0f);
        this.entityData.define(DATA_BEAM_START_Y, 0f);
        this.entityData.define(DATA_BEAM_START_Z, 0f);
        this.entityData.define(DATA_FEEDING_COOLDOWN, 0);
        this.entityData.define(DATA_TAMING_STUNNED, false);
        this.entityData.define(DATA_FLIGHT_PITCH, 0f);
        this.entityData.define(DATA_PITCH_KEY_MODE, false);
        this.entityData.define(DATA_BEAM_ENERGY, 1.0f); // Start with full energy
        this.entityData.define(DATA_BEAM_DEPLETED, false); // Start unlocked
        this.entityData.define(DATA_ACCUMULATED_ROLL, 0.0f); // Start upright
        this.entityData.define(DATA_CUSTOM_DIVE_LOOP_ENABLED, true);
    }

    @Override
    protected void defineRideableDragonData() {
        this.entityData.define(DATA_LANDED, false);
        this.entityData.define(DATA_DODGING, false);
        this.entityData.define(DATA_DASHING, false);
        this.entityData.define(DATA_RIDER_NUDGE_TICKS, 0);
        this.entityData.define(DATA_RIDER_NUDGE_X, 0.0F);
        this.entityData.define(DATA_RIDER_NUDGE_Z, 0.0F);
        this.entityData.define(DATA_RIDER_NUDGE_DRAG, 1.0F);
        this.entityData.define(DATA_LAST_DASH_RIGHT, false);
        this.entityData.define(DATA_GROUND_RENDING, false);
        this.entityData.define(DATA_GROUND_REND_X, 0.0F);
        this.entityData.define(DATA_GROUND_REND_Z, 0.0F);
    }

    @Override
    protected void applyLoadedFlightState(boolean flying, boolean takeoff, boolean hovering, boolean landing) {
        setFlying(flying);
        setTakeoff(takeoff);
        setHovering(hovering);
        setLanding(landing);
    }

    @Override
    protected boolean canUseResolvedRidingAbility(DragonAbilityType<?, ?> abilityType) {
        if (isBaby() || isGroundRending() || areRiderControlsLocked()) {
            return false;
        }
        return super.canUseResolvedRidingAbility(abilityType);
    }

    @Override
    protected boolean isRidingAbilityAllowed(DragonAbilityType<?, ?> abilityType) {
        return abilityType == ModAbilities.RAEVYX_BITE
            || abilityType == ModAbilities.RAEVYX_HORN_GORE
            || abilityType == ModAbilities.RAEVYX_LIGHTNING_BEAM
            || abilityType == ModAbilities.RAEVYX_SUMMON_STORM
            || abilityType == ModAbilities.RAEVYX_GROUND_REND
            || abilityType == ModAbilities.RAEVYX_ROAR;
    }

    @Override
    public <T extends DragonEntity> void tryActivateAbility(DragonAbilityType<T, ?> abilityType) {
        if (isBaby()) {
            return;
        }
        super.tryActivateAbility(abilityType);
    }


    @Override
    protected boolean isRiderInputLocked(Player player) {
        return areRiderControlsLocked();
    }

    @Override
    protected boolean handleCustomRiderAction(ServerPlayer player, DragonRiderAction action,
                                              String abilityName, boolean locked) {
        if (isGroundRending()) {
            return false;
        }

        return switch (action) {
            case DOUBLE_TAP_W -> {
                onRiderDash(player);
                yield true;
            }
            case DOUBLE_TAP_A -> {
                onRiderDodge(player, true);
                yield true;
            }
            case DOUBLE_TAP_S -> {
                onRiderBackwardDodge(player);
                yield true;
            }
            case DOUBLE_TAP_D -> {
                onRiderDodge(player, false);
                yield true;
            }
            default -> false;
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
                && onGround()
                && !isFlying()
                && !isTakeoff()
                && !isLanding()
                && !isInWaterOrBubble()
                && !isGroundRending()
                && !isBeaming();
    }

    @Override
    protected void playRiderFlex(ServerPlayer player, RiderFlexSpec spec) {
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        getSoundHandler().playVocal("flex");
    }

    public Vec3 computeBeamStartFallback(float partialTicks) {
        double x = Mth.lerp(partialTicks, this.xo, this.getX());
        double y = Mth.lerp(partialTicks, this.yo, this.getY());
        double z = Mth.lerp(partialTicks, this.zo, this.getZ());
        float yawDeg = Mth.lerp(partialTicks, this.yHeadRotO, this.yHeadRot);
        float pitchDeg = Mth.lerp(partialTicks, this.xRotO, this.getXRot());
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double R = (-0.5 / 16.0) * MODEL_SCALE;
        double U = (47.26875 / 16.0) * MODEL_SCALE;
        double F = (98.75 / 16.0) * MODEL_SCALE;
        double cp = Math.cos(pitch), sp = Math.sin(pitch);
        double up = U * cp - F * sp;
        double fwd = U * sp + F * cp;
        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        double offX = R * cy - fwd * sy;
        double offZ = R * sy + fwd * cy;
        return new Vec3(x + offX, y + up, z + offZ);
    }

    public Vec3 getBeamStartAnchor(float partialTicks) {
        Vec3 clientBone = getClientLocatorPosition("beamBoneOrigin");
        if (clientBone != null) {
            return clientBone;
        }
        return computeBeamStartFallback(partialTicks);
    }

    public boolean isBeaming() { return getBooleanData(DATA_BEAMING); }
    public void setBeaming(boolean beaming) {
        boolean wasBeaming = getBooleanData(DATA_BEAMING);
        setBooleanData(DATA_BEAMING, beaming);
        if (!beaming) {
            clearBeamPath();
            beamTime = 0;
            beamServerTarget = null;
        }
        if (!beaming || !wasBeaming) {
            resetBeamAim();
        }
        if (beaming && !wasBeaming) {
            beamTime = 0;
            beamServerTarget = createInitialBeamTarget();
        }
    }

    public boolean isBeamGlowActive() {
        return this.entityData.get(DATA_BEAM_GLOW);
    }

    public void setBeamGlowActive(boolean active) {
        this.entityData.set(DATA_BEAM_GLOW, active);
    }

    public float getBeamEnergy() {
        return this.entityData.get(DATA_BEAM_ENERGY);
    }

    public void setBeamEnergy(float energy) {
        float clampedEnergy = Math.max(0.0f, Math.min(1.0f, energy));
        this.entityData.set(DATA_BEAM_ENERGY, clampedEnergy);
        if (clampedEnergy >= 0.999f && isBeamDepleted()) {
            setBeamDepleted(false);
        }
    }

    public void consumeBeamEnergy(float amount) {
        setBeamEnergy(getBeamEnergy() - amount);
    }

    public void regenerateBeamEnergy(float amount) {
        setBeamEnergy(getBeamEnergy() + amount);
    }

    public boolean hasBeamEnergy() {
        return getBeamEnergy() > 0.01f;
    }

    public boolean isBeamDepleted() {
        return this.entityData.get(DATA_BEAM_DEPLETED);
    }

    public void setBeamDepleted(boolean depleted) {
        this.entityData.set(DATA_BEAM_DEPLETED, depleted);
    }

    public boolean canUseBeam() {
        return hasBeamEnergy() && !isBeamDepleted();
    }

    public boolean isRiderPitchKeyMode() {
        return this.entityData.get(DATA_PITCH_KEY_MODE);
    }

    public void setRiderPitchKeyMode(boolean enabled) {
        this.entityData.set(DATA_PITCH_KEY_MODE, enabled);
    }

    public void setBeamEndPosition(@Nullable Vec3 pos) {

        if (pos == null) {
            this.entityData.set(DATA_BEAM_END_SET, false);
        } else {
            this.entityData.set(DATA_BEAM_END_SET, true);
            this.entityData.set(DATA_BEAM_END_X, (float) pos.x);
            this.entityData.set(DATA_BEAM_END_Y, (float) pos.y);
            this.entityData.set(DATA_BEAM_END_Z, (float) pos.z);
        }
    }

    public Vec3 getBeamEndPosition() {
        if (!getBooleanData(DATA_BEAM_END_SET)) return null;
        return new Vec3(
                getFloatData(DATA_BEAM_END_X),
                getFloatData(DATA_BEAM_END_Y),
                getFloatData(DATA_BEAM_END_Z)
        );
    }

    public Vec3 getClientBeamEndPosition(float partialTicks) {
        if (clientBeamEnd != null && prevClientBeamEnd != null) {
            Vec3 d = clientBeamEnd.subtract(prevClientBeamEnd);
            return prevClientBeamEnd.add(d.scale(partialTicks));
        }
        Vec3 serverPos = getBeamEndPosition();
        return clientBeamEnd != null ? clientBeamEnd : (serverPos != null ? serverPos : Vec3.ZERO);
    }

    public void setBeamStartPosition(@org.jetbrains.annotations.Nullable Vec3 pos) {
        if (pos == null) {
            this.entityData.set(DATA_BEAM_START_SET, false);
        } else {
            this.entityData.set(DATA_BEAM_START_SET, true);
            this.entityData.set(DATA_BEAM_START_X, (float) pos.x);
            this.entityData.set(DATA_BEAM_START_Y, (float) pos.y);
            this.entityData.set(DATA_BEAM_START_Z, (float) pos.z);
        }
    }

    public Vec3 getBeamStartPosition() {
        if (!getBooleanData(DATA_BEAM_START_SET)) return null;
        return new Vec3(
                getFloatData(DATA_BEAM_START_X),
                getFloatData(DATA_BEAM_START_Y),
                getFloatData(DATA_BEAM_START_Z)
        );
    }

    public void syncBeamPath(@Nullable Vec3 start, @Nullable Vec3 end) {
        setBeamStartPosition(start);
        setBeamEndPosition(end);
    }

    public void clearBeamPath() {
        setBeamStartPosition(null);
        setBeamEndPosition(null);
    }

    public boolean updateBeamPathFromAim() {
        if (beamPathRefreshTick == tickCount && getBeamStartPosition() != null && getBeamEndPosition() != null) {
            return true;
        }

        Vec3 origin = getBeamStartAnchor(1.0f);
        if (origin == null) {
            clearBeamPath();
            return false;
        }

        boolean smoothAim = getControllingPassenger() == null;
        Vec3 aimDir = refreshBeamAimDirection(origin, smoothAim);
        if (aimDir == null || aimDir.lengthSqr() < 1.0E-6) {
            clearBeamPath();
            return false;
        }

        syncBeamPath(origin, traceBeamImpact(origin, aimDir));
        beamPathRefreshTick = tickCount;
        return true;
    }

    private Vec3 traceBeamImpact(Vec3 origin, Vec3 aimDir) {
        final double maxBeamRange = 64.0D;
        Vec3 reach = origin.add(aimDir.scale(maxBeamRange));
        var context = new ClipContext(origin, reach, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this);
        var hit = level().clip(context);
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            return reach;
        }
        return hit.getLocation();
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
    protected boolean normalizeFlyingStateRequest(boolean flying) {
        return flying && !isBaby();
    }

    @Override
    protected boolean canApplyFlyingState(boolean flying) {
        return isAiWaterBreachTakeoffActive() || flying || !isVehicle() || isOrderedToSit() || onGround();
    }

    @Override
    protected double getRedirectedFlyingTakeoffVelocity() {
        return 0.11D;
    }

    @Override
    protected int getRedirectedFlyingTakeoffTicks() {
        return TAKEOFF_ANIMATION_TICKS;
    }

    @Override
    protected boolean normalizeTakeoffStateRequest(boolean takeoff) {
        return takeoff && !isBaby();
    }

    @Override
    protected void onTakeoffStateStarted() {
        setLanded(false);
        triggerAnim(AnimationHelper.FLIGHT_CONTROLLER,
                getControllingPassenger() != null ? AnimationHelper.RIDER_TAKEOFF : AnimationHelper.TAKEOFF);
        float pitch = 0.94f + getRandom().nextFloat() * 0.12f;
        getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_TAKEOFF.get(), 1.2f, pitch, 56);
    }

    @Override
    protected void onFlyingStateChanged(boolean wasFlying, boolean flying) {
        if (flying) {
            setLanded(false);
        }
        super.onFlyingStateChanged(wasFlying, flying);
    }

    @Override
    protected boolean normalizeHoveringStateRequest(boolean hovering) {
        return hovering && !isBaby();
    }

    public boolean isActuallyRunning() {
        if (isFlying()) return false;
        int s = level().isClientSide ? getEffectiveGroundState() : this.entityData.get(DATA_GROUND_MOVE_STATE);
        return s == 2;
    }


    @Override
    protected boolean canApplyLandingState(boolean landing) {
        return !(landing && isVehicle()) && super.canApplyLandingState(landing);
    }

    @Override
    protected void onLandingDataSet(boolean landing) {
        if (landing) {
            landingTimer = 0;
            this.setTakeoff(false);
            this.setHovering(false);
            this.landingFlag = true;
        } else {
            this.landingFlag = false;
        }
    }

    public void setLanded(boolean landed) {
        this.entityData.set(DATA_LANDED, landed);
        if (landed) {
            landedTimer = 0;
            landedFlag = true;
        } else {
            landedFlag = false;
        }
    }

    public int getSyncedFlightMode() { return getIntegerData(DATA_FLIGHT_MODE); }

    public int getGroundMoveState() { return getIntegerData(DATA_GROUND_MOVE_STATE); }
    
    @Override
    protected int getFlightMode() {
        return evaluateStandardFlightMode(false);
    }

    public DragonFlightStateEvaluator.VisualState getVisualFlightState(float partialTick) {
        return evaluateVisualFlightState(partialTick, getFlightPitchRadians(partialTick));
    }

    @Override
    protected boolean isRiderFallRecoveryBlocked() {
        return isGroundRending();
    }

    public boolean isLanded() {
        return getBooleanData(DATA_LANDED);
    }

    @Override
    protected void onStandardLandedRecoveryFinished() {
        setLanded(false);
        landedTimer = 0;
    }
    
    @Nullable
    public Player getRidingPlayer() {
        if (this.getControllingPassenger() instanceof Player player) {
            return player;
        }
        return null;
    }

    @Override
    public RiderAbilityBinding getTertiaryRiderAbility() {
        if (isBaby()) {
            return null;
        }
        return new RiderAbilityBinding(ModAbilities.RAEVYX_LIGHTNING_BEAM.getName(), RiderAbilityBinding.Activation.HOLD);
    }

    @Override
    public RiderAbilityBinding getPrimaryRiderAbility() {
        if (isBaby()) {
            return null;
        }
        return new RiderAbilityBinding(ModAbilities.RAEVYX_ROAR.getName(), RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getSecondaryRiderAbility() {
        if (isBaby()) {
            return null;
        }
        return new RiderAbilityBinding(ModAbilities.RAEVYX_SUMMON_STORM.getName(), RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getAttackRiderAbility() {
        if (isBaby()) {
            return null;
        }
        if (getMeleeMode() == 0) {
            return new RiderAbilityBinding(ModAbilities.RAEVYX_BITE.getName(), RiderAbilityBinding.Activation.PRESS);
        } else {
            return new RiderAbilityBinding(ModAbilities.RAEVYX_HORN_GORE.getName(), RiderAbilityBinding.Activation.PRESS);
        }
    }

    // ===== RIDING SUPPORT =====
    @Override
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

    public boolean isDodging() { return this.entityData.get(DATA_DODGING); }
    public boolean isDashing() {
        return this.entityData.get(DATA_DASHING);
    }

    private static double constantNudgeSpeed(double distanceBlocks, int durationTicks) {
        return durationTicks <= 0 ? 0.0D : distanceBlocks / durationTicks;
    }

    void setRaevyxNudgeState(int ticks, Vec3 velocity, boolean dashing, boolean dodging, double horizontalDrag) {
        this.entityData.set(DATA_RIDER_NUDGE_TICKS, Math.max(1, ticks));
        this.entityData.set(DATA_RIDER_NUDGE_X, (float) velocity.x);
        this.entityData.set(DATA_RIDER_NUDGE_Z, (float) velocity.z);
        this.entityData.set(DATA_RIDER_NUDGE_DRAG, (float) horizontalDrag);
        this.entityData.set(DATA_DASHING, dashing);
        this.entityData.set(DATA_DODGING, dodging);
    }

    int getRaevyxNudgeTicks() {
        return this.entityData.get(DATA_RIDER_NUDGE_TICKS);
    }

    void setRaevyxNudgeTicks(int ticks) {
        this.entityData.set(DATA_RIDER_NUDGE_TICKS, Math.max(0, ticks));
    }

    Vec3 getRaevyxNudgeVector() {
        return new Vec3(
                this.entityData.get(DATA_RIDER_NUDGE_X),
                0.0D,
                this.entityData.get(DATA_RIDER_NUDGE_Z)
        );
    }

    void setRaevyxNudgeVelocity(Vec3 velocity) {
        this.entityData.set(DATA_RIDER_NUDGE_X, (float) velocity.x);
        this.entityData.set(DATA_RIDER_NUDGE_Z, (float) velocity.z);
    }

    double getRaevyxNudgeDrag() {
        return this.entityData.get(DATA_RIDER_NUDGE_DRAG);
    }

    void clearRaevyxNudgeState() {
        this.entityData.set(DATA_RIDER_NUDGE_TICKS, 0);
        this.entityData.set(DATA_RIDER_NUDGE_X, 0.0F);
        this.entityData.set(DATA_RIDER_NUDGE_Z, 0.0F);
        this.entityData.set(DATA_RIDER_NUDGE_DRAG, 1.0F);
        this.entityData.set(DATA_DASHING, false);
        this.entityData.set(DATA_DODGING, false);
        dashHitCooldowns.clear();
    }

    public boolean wasLastDashRight() {
        return this.entityData.get(DATA_LAST_DASH_RIGHT);
    }
    public boolean isGroundRending() { return this.entityData.get(DATA_GROUND_RENDING); }
    public void setGroundRending(boolean rending) {
        boolean wasGroundRending = this.groundRending;
        this.groundRending = rending;
        this.entityData.set(DATA_GROUND_RENDING, rending);
        if (rending && !wasGroundRending) {
            // Ground Rend owns continuous horizontal movement for its full animation.
            dashDodgeNudge.cancelActive();
            clearGroundRendTrailAnchors();
        }
        if (!rending) {
            dashDodgeNudge.cancelActive();
            setGroundRendVelocity(Vec3.ZERO);
            this.groundRendTravelSpeed = 0.0F;
            clearGroundRendTrailAnchors();
        }
    }
    public void setGroundRendVelocity(Vec3 vec) {
        this.entityData.set(DATA_GROUND_REND_X, (float) vec.x);
        this.entityData.set(DATA_GROUND_REND_Z, (float) vec.z);
        if (!isGroundRending()) {
            return;
        }
        if (vec.horizontalDistanceSqr() < 1.0E-6D) {
            dashDodgeNudge.cancelActive();
        } else if (dashDodgeNudge.isActive()) {
            dashDodgeNudge.updateContinuous(vec, 3);
        } else {
            dashDodgeNudge.startContinuous(vec, 3);
        }
    }

    private Vec3 getGroundRendVelocity() {
        return new Vec3(
                this.entityData.get(DATA_GROUND_REND_X),
                0.0D,
                this.entityData.get(DATA_GROUND_REND_Z)
        );
    }
    public void setGroundRendTravelSpeed(float speed) {
        this.groundRendTravelSpeed = Math.max(0.0F, speed);
    }
    public void clearGroundRendTrailAnchors() {
        this.groundRendLeftTrailAnchor = null;
        this.groundRendRightTrailAnchor = null;
    }
    public float getRiderForwardInput() { return this.entityData.get(DATA_RIDER_FORWARD); }

    public void beginDodge(Vec3 vec, int ticks) {
        dashDodgeNudge.startDodge(vec, ticks);
    }

    @Override
    protected void onRiderDodge(Player player, boolean isLeft) {
        if (isGroundRending() || (isTamingStunned() && !isTame())) {
            return;
        }
        if (isInWaterOrBubble()) {
            return;
        }
        if (isDashing()) {
            dashDodgeNudge.cancelActive();
        }

        if (dodgeCooldownTicks > 0) {
            return;
        }

        double dodgeDistance = isFlying() ? DODGE_DISTANCE_BLOCKS * AIR_DODGE_DISTANCE_MULTIPLIER : DODGE_DISTANCE_BLOCKS;
        double perTickSpeed = constantNudgeSpeed(dodgeDistance, DODGE_DURATION_TICKS);
        if (perTickSpeed <= 0.0D) {
            return;
        }
        Vec3 dodgeVector = DragonMotionMath.horizontalRight(this.getYRot()).scale(isLeft ? perTickSpeed : -perTickSpeed);
        beginDodge(dodgeVector, DODGE_DURATION_TICKS);
        dodgeCooldownTicks = RIDER_DODGE_COOLDOWN_TICKS;
        dodgeIFramesTicks = DODGE_IFRAMES_TICKS;

        if (isFlying()) {
            if (isLeft) {
                animationHandler.triggerDodgeAirLeftAnimation();
            } else {
                animationHandler.triggerDodgeAirRightAnimation();
            }
        } else if (isLeft) {
            animationHandler.triggerDodgeLeftAnimation();
        } else {
            animationHandler.triggerDodgeRightAnimation();
        }
    }

    @Override
    protected void onRiderBackwardDodge(Player player) {
        if (isGroundRending() || (isTamingStunned() && !isTame())) {
            return;
        }
        if (isInWaterOrBubble()) {
            return;
        }
        if (isDashing()) {
            dashDodgeNudge.cancelActive();
        }

        if (dodgeCooldownTicks > 0) {
            return;
        }

        double dodgeDistance = isFlying() ? DODGE_DISTANCE_BLOCKS * AIR_DODGE_DISTANCE_MULTIPLIER : DODGE_DISTANCE_BLOCKS;
        double perTickSpeed = constantNudgeSpeed(dodgeDistance, DODGE_DURATION_TICKS);
        if (perTickSpeed <= 0.0D) {
            return;
        }
        Vec3 dodgeVector = DragonMotionMath.horizontalForward(this.getYRot()).scale(-perTickSpeed);
        beginDodge(dodgeVector, DODGE_DURATION_TICKS);
        dodgeCooldownTicks = RIDER_DODGE_COOLDOWN_TICKS;
        dodgeIFramesTicks = DODGE_IFRAMES_TICKS;

        if (isFlying()) {
            animationHandler.triggerDodgeAirRightAnimation();
        } else {
            animationHandler.triggerDodgeBackwardAnimation();
        }
    }

    public boolean tryAIGroundDodge(@Nullable LivingEntity threat) {
        if (isFlying() || isInWaterOrBubble() || isDodging() || (isTamingStunned() && !isTame())) {
            return false;
        }

        if (isDashing()) {
            dashDodgeNudge.cancelActive();
        }

        if (aiDodgeCooldownTicks > 0) {
            return false;
        }

        double perTickSpeed = constantNudgeSpeed(DODGE_DISTANCE_BLOCKS, DODGE_DURATION_TICKS);
        if (perTickSpeed <= 0.0D) {
            return false;
        }

        boolean doBackward = threat != null
                && this.distanceToSqr(threat) < 36.0D
                && this.getRandom().nextFloat() < 0.35f;
        boolean isLeft = this.getRandom().nextBoolean();

        Vec3 dodgeVector;
        if (doBackward) {
            dodgeVector = DragonMotionMath.horizontalForward(this.getYRot()).scale(-perTickSpeed);
        } else {
            dodgeVector = DragonMotionMath.horizontalRight(this.getYRot()).scale(isLeft ? perTickSpeed : -perTickSpeed);
        }

        beginDodge(dodgeVector, DODGE_DURATION_TICKS);
        aiDodgeCooldownTicks = AI_DODGE_COOLDOWN_TICKS;
        dodgeIFramesTicks = DODGE_IFRAMES_TICKS;

        if (doBackward) {
            animationHandler.triggerDodgeBackwardAnimation();
        } else if (isLeft) {
            animationHandler.triggerDodgeLeftAnimation();
        } else {
            animationHandler.triggerDodgeRightAnimation();
        }

        return true;
    }

    private boolean tryReactiveHitDodge(@Nonnull DamageSource damageSource, float amount) {
        if (level().isClientSide || amount <= 0.0F || aiDodgeCooldownTicks > 0 || isVehicle() || !isAlive() || isDying() || (isTamingStunned() && !isTame())) {
            return false;
        }

        LivingEntity attacker = null;
        if (damageSource.getEntity() instanceof LivingEntity living) {
            attacker = living;
        } else if (damageSource.getDirectEntity() instanceof LivingEntity living) {
            attacker = living;
        } else if (damageSource.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof LivingEntity living) {
            attacker = living;
        }

        if (attacker instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        if (attacker == null && damageSource.getEntity() == null && damageSource.getDirectEntity() == null) {
            return false;
        }
        if (this.getRandom().nextFloat() >= REACTIVE_HIT_DODGE_CHANCE) {
            return false;
        }
        if (!tryAIGroundDodge(attacker)) {
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

    private void tickDashState() {
        dashDodgeNudge.tickServerState();

        dashHitCooldowns.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });

        if (isDashing()) {
            Vec3 damageCenter = this.getBoundingBox().getCenter()
                    .add(getLookAngle().normalize().scale(this.getBbWidth() * 0.75D));
            AABB dragonBox = this.getBoundingBox().inflate(1.5D);
            AABB forwardBox = new AABB(damageCenter, damageCenter).inflate(2.0D);
            AABB combinedBox = dragonBox.minmax(forwardBox);
            List<LivingEntity> entities;
            if (this.isVehicle()) {
                entities = this.level().getEntitiesOfClass(
                    LivingEntity.class,
                    combinedBox,
                    entity -> entity != this && entity != this.getControllingPassenger() && !this.isAlly(entity)
                );
            } else {
                LivingEntity currentTarget = this.getTarget();
                if (currentTarget == null
                        || !currentTarget.isAlive()
                        || currentTarget == this
                        || this.isAlly(currentTarget)
                        || !combinedBox.intersects(currentTarget.getBoundingBox())) {
                    entities = Collections.emptyList();
                } else {
                    entities = List.of(currentTarget);
                }
            }

            for (LivingEntity target : entities) {
                int entityId = target.getId();

                if (dashHitCooldowns.containsKey(entityId)) {
                    continue;
                }

                target.hurt(this.damageSources().mobAttack(this), getConfiguredDashDamage());
                double knockbackStrength = 1.5D;
                double dx = target.getX() - damageCenter.x;
                double dz = target.getZ() - damageCenter.z;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0) {
                    target.knockback(
                        knockbackStrength,
                        -dx / dist,
                        -dz / dist
                    );
                }
                dashHitCooldowns.put(entityId, 5);
            }
        }
    }

    @Override
    protected void onRiderDash(Player player) {
        if (isGroundRending()) {
            return;
        }
        if (isAerial() || isInWaterOrBubble()) {
            return;
        }
        if (dashDodgeNudge.getDashCooldownTicks() > 0) {
            return;
        }
        if (isDashing()) {
            return;
        }

        final int DASH_DURATION = 27;
        final int DASH_COOLDOWN = 30;
        final double DASH_DISTANCE = 32.0D;
        if (!beginForwardDashMotion(DASH_DURATION, DASH_COOLDOWN, DASH_DISTANCE)) {
            return;
        }
        triggerDashFeedback();
    }

    public void triggerDashFeedback() {
        lastDashWasRight = !lastDashWasRight;
        this.entityData.set(DATA_LAST_DASH_RIGHT, lastDashWasRight);
        getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_DASH.get(), 1.6f, 1.0f, 56);
    }

    public boolean beginForwardDashMotion(int durationTicks, int cooldownTicks, double distanceBlocks) {
        double perTickSpeed = DragonMotionMath.speedForIntegratedDistance(distanceBlocks, DASH_NUDGE_DRAG, durationTicks);
        if (perTickSpeed <= 0.0D) {
            return false;
        }
        Vec3 dashVector = DragonMotionMath.horizontalForward(this.getYRot()).scale(perTickSpeed);
        return dashDodgeNudge.startDash(dashVector, durationTicks, cooldownTicks, DASH_NUDGE_DRAG);
    }

    private float getConfiguredDashDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID)
                .abilityDamage("dash", DEFAULT_DASH_DAMAGE);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel serverLevel) {
            DragonDestructionManager.applyPassiveTreeDestruction(serverLevel, this);
        }
        tickControllers();
        tickBankingLogic();
        tickStandardPitchingLogic();
        tickBarrelRollLogic();
        tickScreenShake();
        if (!level().isClientSide) {
            diveImpactAbility.tickServer();
        }
        tickFlightLifecycle();
        if (!level().isClientSide) {
            syncCustomDiveLoopEnabled();
        }
        if (level().isClientSide) {
            return;
        }
        tickSittingState();
        tickStandardTakeoffAndGroundedAerialRecovery();
        tickRiderTakeoff();
        tickHurtSoundCooldown();
        spawnPendingFamilyBabies(ModEntities.RAEVYX.get(), Raevyx::applyConfiguredAttributes);
        if (isFlying()) {
            timeFlying++;
        } else {
            timeFlying = 0;
        }
        if (isFlying() && getControllingPassenger() != null) {
            if (!isLanding() && !isBeaming() && !isTakeoff() && isHovering()) {
                setHovering(false);
            }
        }

        if (postStandUnlockTicks > 0) {
            postStandUnlockTicks--;
        }
        tickRiderControlLock();

        tickAsyncFlightNavigation();
        if (tickCount % 2 == 0) {
            tickRiderControlLockMovement();
        }
        tickFeedingCooldown();
        if (!level().isClientSide && isTamingStunned()) {
            if (getTarget() != null) {
                super.setTarget(null);
            }
            if (getActiveAbility() != null) {
                combatManager.forceEndActiveAbility();
            }
            setAggressive(false);
            getNavigation().stop();
        }
        if (dodgeCooldownTicks > 0) {
            dodgeCooldownTicks--;
        }
        if (aiDodgeCooldownTicks > 0) {
            aiDodgeCooldownTicks--;
        }
        if (dodgeIFramesTicks > 0) {
            dodgeIFramesTicks--;
        }
        if (dashDodgeNudge.isActive() || dashDodgeNudge.getDashCooldownTicks() > 0 || !dashHitCooldowns.isEmpty()) {
            tickDashState();
        }
        tamingController.tickServer();
        if (isTamingStunned()) {
            tamingController.enforceGroundingTick();
        }
        handleAmbientSounds();
        tickBeamEnergy();
        if (isFlying() || isTakeoff()) {
            tickFlightPhysics();
        }
        if (tickCount % 5 == 0) {
            tickSuperchargeTimer();
            tickTempInvulnTimer();
            tickSuperchargeVfx();
        }
        if (tickCount % 100 == 0) {
            tickRecentAggroCleanup();
        }

        if (isSleeping() || isSleepingEntering() || isSleepingExiting()) {
            if (this.isVehicle()) {
                wakeUpImmediately();
                clearLocalSitTransitionForMount();
            } else if (this.getTarget() != null || this.isAggressive()) {

                wakeUpImmediately();
                suppressSleep(200);
            } else if (this.isInWaterOrBubble() || this.isInLava()) {
                wakeUpImmediately();
                suppressSleep(200);
            }
        }
        if (!this.level().isClientSide) {
            tickAnimationStates();
        }
        if (this.isDodging()) {
            return;
        }

        if (isBeaming() || beamAimDir != null) {
            tickBeamLook();
        }

        if (!level().isClientSide && isBaby()) {
            if (getTarget() != null) {
                super.setTarget(null);
            }
            if (getActiveAbility() != null) {
                combatManager.forceEndActiveAbility();
            }
            setAggressive(false);
        }

        tickClientSideUpdates();
    }

    private void tickFlightLifecycle() {
        if (!this.level().isClientSide) {
            boolean onGroundNow = this.onGround() && !this.isInWater();

            if (isFlying()) {
                this.fallDistance = 0.0F;
                if (onGroundNow && !isTakeoff()) {
                    if (isLanding()) {
                        handleAiLandingComplete();
                    } else {
                        setLanding(false);
                        setLanded(false);
                        landingTimer = 0;
                        setFlying(false);
                    }
                }
            } else {
                if (isLanding() && onGroundNow) {
                    handleAiLandingComplete();
                } else if (!isLanding()) {
                    landingTimer = 0;
                }
            }
            syncFlightAnimationState();
        }

        this.setNoGravity(isFlying() || isTakeoff() || isHovering() || isLanding());

        if (!isFlying() && !isTakeoff() && !isLanding() && isUsingAirNavigation()) {
            switchToGroundNavigation();
        }
    }

    private void tickScreenShake() {
        screenShakeComponent.tick();
    }

    private void tickFlightPhysics() {
        if (level().isClientSide) return;
        if (isFlying() && !isLanding() && getDeltaMovement().y < 0 && isAlive()) {
            setDeltaMovement(getDeltaMovement().multiply(1, 0.6, 1));
        }
    }

    private void tickHurtSoundCooldown() {
        if (hurtSoundCooldown > 0) hurtSoundCooldown--;
    }

    private void tickBeamEnergy() {
        if (!isBeaming() && getBeamEnergy() < 1.0f) {
            float regen = (float) DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.RAEVYX_ID)
                    .extraDouble("beam_regen_per_tick", 0.0025D);
            regen = Math.max(0.0f, regen);
            if (regen > 0.0f) {
                regenerateBeamEnergy(regen);
            }
        }
    }

    private void tickSittingState() {
        if (!this.level().isClientSide && this.isVehicle() && this.isOrderedToSit()) {
            this.setOrderedToSit(false);
        }
    }
    
    @Override
    protected void onMountedStateStopped() {
        super.onMountedStateStopped();
    }

    @Override
    protected void clearLocalSitTransitionForMount() {
        sitTransitions.clear();
        this.setInSittingPose(false);
    }
    
    private void tickBeamLook() {
        if (!isBeaming()) {
            resetBeamAim();
            return;
        }

        Vec3 start = getBeamStartAnchor(1.0f);
        if (start == null) {
            resetBeamAim();
            return;
        }

        boolean riderControlled = getControllingPassenger() != null;
        Vec3 aimDir = refreshBeamAimDirection(start, riderControlled);
        if (aimDir == null) {
            beamAimDir = Vec3.directionFromRotation(this.getXRot(), this.yHeadRot).normalize();
            aimDir = beamAimDir;
            updateBeamOffsets(aimDir);
        }
        if (!riderControlled) {
            applyBeamLook(aimDir);
        }
        if (!level().isClientSide) {
            updateBeamPathFromAim();
        }
    }

    public Vec3 getBeamAimDirection() {
        return beamAimDir;
    }

    public Vec3 refreshBeamAimDirection(Vec3 start, boolean smooth) {
        if (beamAimRefreshTick == tickCount && beamAimDir != null) {
            updateBeamOffsets(beamAimDir);
            return beamAimDir;
        }

        Vec3 desiredDir = computeRawBeamAimDirection(start);
        if (desiredDir == null) {
            updateBeamOffsets(null);
            return null;
        }
        Vec3 clamped = clampBeamDirection(desiredDir);
        if (clamped == null) {
            updateBeamOffsets(null);
            return null;
        }

        beamAimDir = DragonAimHelper.blendDirection(beamAimDir, clamped, smooth, 0.35D);

        updateBeamOffsets(beamAimDir);
        beamAimRefreshTick = tickCount;
        return beamAimDir;
    }

    private Vec3 computeRawBeamAimDirection(Vec3 start) {
        Vec3 riderLook = DragonAimHelper.riderViewDirection(this);
        if (riderLook != null) {
            return riderLook;
        }
        if (!level().isClientSide) {
            tickBeamTargeting(start);
        }

        if (beamServerTarget != null) {
            Vec3 towardTarget = DragonAimHelper.directionTo(start, beamServerTarget);
            if (towardTarget != null) {
                return towardTarget;
            }
        }
        Vec3 fallbackDir = DragonAimHelper.fallbackHeadDirection(this);
        return fallbackDir != null ? fallbackDir : Vec3.ZERO;
    }

    private Vec3 clampBeamDirection(Vec3 desiredDir) {
        Vec3 clamped = DragonAimHelper.clampDirectionToHead(
                desiredDir,
                this.yHeadRot,
                this.getXRot(),
                MAX_BEAM_YAW_DEG,
                MAX_BEAM_PITCH_DEG
        );
        if (clamped == null) {
            updateBeamOffsets(null);
            return null;
        }
        return clamped;
    }

    private Vec3 createInitialBeamTarget() {
        LivingEntity target = getTarget();
        Vec3 shootFrom = getBeamStartAnchor(1.0f);
        if (shootFrom == null) {
            shootFrom = position().add(0, getBbHeight() * 0.5, 0);
        }

        if (target != null && target.isAlive()) {
            Vec3 randomOffset = new Vec3(
                -4 + random.nextFloat() * 8F,
                -2 + random.nextFloat() * 4F,
                -4 + random.nextFloat() * 8F
            );
            return target.position().add(randomOffset);
        } else {
            Vec3 forward = new Vec3(0, random.nextBoolean() ? 50 : 10, 30)
                .yRot((float) Math.toRadians(-this.yBodyRot));
            return shootFrom.add(forward);
        }
    }

    private void tickBeamTargeting(Vec3 shootFrom) {
        beamTime++;

        LivingEntity target = getTarget();
        Vec3 currentTarget = beamServerTarget != null ? beamServerTarget : shootFrom;

        if (target != null && target.isAlive()) {
            float maxBeamTime = 16.0F;
            float time = Math.min(beamTime, (int) maxBeamTime) / maxBeamTime;
            float accuracy = 1.0F - time;

            Vec3 wobbleOffset = new Vec3(
                Math.sin(tickCount * 0.2F) * 1.4,
                Math.sin(tickCount * 0.15F) * 0.8,
                Math.cos(tickCount * 0.2F) * -1.4
            ).yRot((float) Math.toRadians(-this.yBodyRot)).scale(accuracy);
            Vec3 targetPoint = target.getEyePosition().add(0, -0.25, 0).add(wobbleOffset);
            beamServerTarget = targetPoint.subtract(currentTarget).scale(0.35F).add(currentTarget);
        } else {
            Vec3 sweepOffset = new Vec3(
                Math.sin(tickCount * 0.1F) * 10,
                0,
                6
            ).yRot((float) Math.toRadians(-this.yBodyRot));
            Vec3 sweepTarget = shootFrom.add(sweepOffset);
            beamServerTarget = sweepTarget.subtract(currentTarget).scale(0.1F).add(currentTarget);
        }
    }

    private void applyBeamLook(@Nullable Vec3 aimDir) {
        if (aimDir == null) {
            return;
        }
        float desiredYaw = (float)(Math.atan2(-aimDir.x, aimDir.z) * (180.0 / Math.PI));
        float desiredPitch = (float)(-Math.atan2(aimDir.y, Math.sqrt(aimDir.x * aimDir.x + aimDir.z * aimDir.z)) * (180.0 / Math.PI));

        float headYawSpeed = 15.0F;
        float headPitchSpeed = 12.0F;

        this.yHeadRot = Mth.approachDegrees(this.yHeadRot, desiredYaw, headYawSpeed);

        float currentPitch = this.getXRot();
        float pitchDelta = desiredPitch - currentPitch;
        float pitchChange = Mth.clamp(pitchDelta, -headPitchSpeed, headPitchSpeed);
        this.setXRot(currentPitch + pitchChange);

        float yawDiff = Mth.degreesDifferenceAbs(desiredYaw, Mth.wrapDegrees(this.yBodyRot));
        if (yawDiff > MAX_BEAM_YAW_DEG * 0.6F) {
            float bodyRotSpeed = 8.0F;
            this.setYRot(Mth.approachDegrees(this.getYRot(), desiredYaw, bodyRotSpeed));
            this.yBodyRot = Mth.approachDegrees(this.yBodyRot, desiredYaw, bodyRotSpeed);
        }
    }

    private void resetBeamAim() {
        beamLookLerp = null;
        beamAimDir = null;
        beamYawOffsetRad = 0.0f;
        beamPitchOffsetRad = 0.0f;
        beamAimRefreshTick = -1;
        beamPathRefreshTick = -1;
    }

    private void updateBeamOffsets(@org.jetbrains.annotations.Nullable Vec3 direction) {
        if (direction == null || direction.lengthSqr() < 1.0E-6) {
            beamYawOffsetRad = 0.0f;
            beamPitchOffsetRad = 0.0f;
            return;
        }

        Vec3 dir = direction.normalize();
        float finalYawDeg = (float)(Math.atan2(-dir.x, dir.z) * (180.0 / Math.PI));
        float finalPitchDeg = (float)(-Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * (180.0 / Math.PI));

        float headYaw = this.yHeadRot;
        float headPitch = this.getXRot();

        float yawOffsetDeg = Mth.degreesDifference(headYaw, finalYawDeg);
        float pitchOffsetDeg = finalPitchDeg - headPitch;

        beamYawOffsetRad = yawOffsetDeg * Mth.DEG_TO_RAD;
        beamPitchOffsetRad = pitchOffsetDeg * Mth.DEG_TO_RAD;
    }

    private void tickClientSideUpdates() {
        if (level().isClientSide) {
            this.prevClientBeamEnd = this.clientBeamEnd;
            this.clientBeamEnd = getBeamEndPosition();
        }
    }
    private void tickControllers() {
        updateSittingProgress();
    }

    private void updateSittingProgress() {
        if (!level().isClientSide && super.isInSittingPose() && !isOrderedToSit()) {
            setInSittingPose(false);
        }
        sitTransitions.tick(
                getSitDownAnimationTicks(),
                getSitUpAnimationTicks(),
                animationHandler::triggerSitDownAnimation,
                animationHandler::triggerSitUpAnimation
        );
    }
    
    private void tickRiderControlLockMovement() {
        if (!areRiderControlsLocked() || dashDodgeNudge.isActive()) {
            return;
        }
        if (getControllingPassenger() == null) {
            clearRiderControlLock();
            return;
        }

        this.getNavigation().stop();
        this.setTarget(null);
        this.setDeltaMovement(0, 0, 0);
    }
    
    private void tickSuperchargeTimer() {
        if (superchargeTicks > 0) {
            superchargeTicks -= 5;
            if (superchargeTicks <= 0) {
                superchargeTicks = 0;
                DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
                Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(configuredMaxHealth(config, BABY_MAX_HEALTH));
                if (this.getHealth() > this.getMaxHealth()) {
                    this.setHealth(this.getMaxHealth());
                }
                allowGroundBeamDuringStorm = false;
            }
        }
    }

    private void tickTempInvulnTimer() {
        if (tempInvulnTicks > 0) {
            tempInvulnTicks -= 5;
            if (tempInvulnTicks <= 0) {
                tempInvulnTicks = 0;
                if (!isDying()) this.setInvulnerable(false);
            }
        }
    }
    
    private void tickSuperchargeVfx() {
        if (isSupercharged() && this.level().isThundering() && superchargeVfxCooldown-- <= 0) {
            spawnSuperchargeVfx();
            superchargeVfxCooldown = 6 + this.random.nextInt(6);
        }
    }
    
    private void tickFeedingCooldown() {
        if (level().isClientSide) {
            return;
        }
        int cooldownTicks = this.entityData.get(DATA_FEEDING_COOLDOWN);
        if (cooldownTicks > 0) {
            cooldownTicks--;
            this.entityData.set(DATA_FEEDING_COOLDOWN, cooldownTicks);
        }
    }

    private void tickBankingLogic() {
        boolean shouldBank = isFlying() && !isLanding() && !isHovering();
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
        tickStandardRiderLandingBlend(new RiderLandingBlendHooks() {
            @Override
            public void onWaterFlightCleared() {
                clearAerialStateAndUseGroundNavigation();
                timeFlying = 0;
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
                triggerAnim(AnimationHelper.MOVEMENT_CONTROLLER, AnimationHelper.LANDED);
                getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_LANDED.get(), 1.0f, 1.0f, 72);
                markLandedNow();
                lockRiderControls(30);
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
        return isBeaming();
    }

    @Override
    protected float getStandardAiLandingPitchDegrees() {
        return 24.0F;
    }

    @Override
    protected void playHurtSound(@Nonnull DamageSource source) {
    }

    @Override
    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return ModAbilities.RAEVYX_HURT;
    }

    @Override
    protected DragonAbilityType<?, ?> getDeathAbilityType() {
        return ModAbilities.RAEVYX_DIE;
    }

    @Override
    protected void onSuccessfulDamage(DamageSource source, float amount) {
        if (isDying()) {
            return;
        }
        if (hurtSoundCooldown > 0) {
            return;
        }
        super.onSuccessfulDamage(source, amount);
        this.hurtSoundCooldown = this.isVehicle() ? 15 : 8;
    }

    private String selectAmbientGrumble() {
        if (isDying() || isBaby() || isAggressive() || isBeaming() || getActiveAbility() != null) {
            return null;
        }
        if (isOrderedToSit()) {
            return "grumble1";
        }
        return selectWeightedAmbientVocal("grumble1", 0.4f, "grumble2", 0.7f, "grumble3");
    }

    private void handleAmbientSounds() {

        if (isDying() || isSleeping() || isSleepTransitioning() || isInSitTransition() || getSleepAmbientCooldownTicks() > 0 || areRiderControlsLocked()) return;
        tickAmbientVocalSounds(MIN_AMBIENT_DELAY, MAX_AMBIENT_DELAY, this::selectAmbientGrumble);
    }

    @Override
    public void travel(@NotNull Vec3 motion) {
        if (dashDodgeNudge.isActive()) {
            if (this.isVehicle() && this.getControllingPassenger() instanceof Player player && !isFlying() && !isInWaterOrBubble() && !isInLava()) {
                this.setSpeed(this.getRiddenSpeed(player));
                super.travel(motion);
            }
            if (isGroundRending()) {
                dashDodgeNudge.applyContinuousTravelMotion();
            } else {
                dashDodgeNudge.applyTravelMotion();
            }
            return;
        }
        if (isGroundRending()) {
            super.travel(Vec3.ZERO);
            return;
        }
        boolean sittingLocked = !this.isVehicle() && (this.isOrderedToSit() || this.isInSittingPose()) && postStandUnlockTicks <= 0;
        if (sittingLocked || this.isDying() || this.isSleeping() || this.isSleepTransitioning()) {
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }
            motion = Vec3.ZERO;
            super.travel(motion);
            return;
        }
        boolean inWater = this.isInWater() || this.isInWaterOrBubble() || this.isInLava();

        if (inWater) {
            clearRiderFlightStateInWaterIfNeeded();
        }

        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }
            if (shouldUseRiderFlightMovementInWater()) {
                this.riderController.handleRiderMovement(player, motion);
            } else if (inWater) {
                handleRiderWaterSwimming(motion);
            } else if (isFlying()) {
                this.riderController.handleRiderMovement(player, motion);
            } else {
                this.setSpeed(this.getRiddenSpeed(player));
                super.travel(motion);
            }
        } else {
            super.travel(motion);
        }
    }

    @SuppressWarnings("unused")
    public static boolean canSpawnHere(EntityType<Raevyx> type,
                                       LevelAccessor level,
                                       MobSpawnType reason,
                                       BlockPos pos,
                                       RandomSource random) {
        if (SaintsDragonsConfig.isRaevyxCustomSpawningEnabled()
                && DragonSpawnRules.isNaturalWildSpawn(reason)
                && !DragonSpawnRules.isThundering(level)) {
            return false;
        }
        return DragonSpawnRules.hasDryGroundSpawnSpace(level, pos)
                && DragonSpawnRules.passesNearbyDragonDensityCheck(level, reason, pos, Raevyx.class);
    }

    public @NotNull SpawnGroupData finalizeSpawn(
            @Nonnull ServerLevelAccessor level,
            @Nonnull DifficultyInstance difficulty,
            @Nonnull MobSpawnType spawnReason,
            @Nullable SpawnGroupData spawnData,
            @Nullable CompoundTag dataTag
    ) {
        spawnData = super.finalizeSpawn(level, difficulty, spawnReason, spawnData, dataTag);
        if (spawnReason == MobSpawnType.CHUNK_GENERATION) {
            if (!(spawnData instanceof RaevyxFamilyData)) {
                if (this.random.nextFloat() < 0.05F) {
                    spawnData = new RaevyxFamilyData(false);
                    this.setGender(DragonGender.FEMALE);
                    scheduleFamilyBabies(2 + this.random.nextInt(2));
                }
            }
        }

        applyConfiguredAttributes();
        this.setHealth(this.getMaxHealth());
        return spawnData;
    }

    public void applyConfiguredAttributes() {
        DragonAttributeConfig config = getConfiguredDragonAttributes();
        applyConfiguredFlyingHealthAndArmor(config, BABY_MAX_HEALTH, 0.0D);

        clampHealthToMax();
    }

    private static class RaevyxFamilyData extends AgeableMob.AgeableMobGroupData {
        public RaevyxFamilyData(boolean shouldSpawnBaby) {
            super(shouldSpawnBaby);
        }
    }

    @Override
    protected double getCullingInflateX() {
        return 5.0D;
    }

    @Override
    protected double getCullingInflateY() {
        return 3.0D;
    }

    @Override
    protected double getCullingInflateZ() {
        return 5.0D;
    }

    @Override
    public boolean canTarget(Entity entity) {
        if (this.isBaby()) {
            return false;
        }
        if (entity instanceof Player player && !this.isTame()) {
            return this.getLastHurtByMob() == player || this.getTarget() == player;
        }

        return super.canTarget(entity);
    }

    @Override
    public boolean hurt(@Nonnull DamageSource damageSource, float amount) {
        if (isDying()) {
            return false;
        }
        if (dodgeIFramesTicks > 0) {
            return false;
        }
        if (isGroundRending()) {
            return false;
        }
        if (damageSource.is(DamageTypeTags.IS_LIGHTNING)) {
            return false;
        }
        if (isSleeping() || isSleepingEntering() || isSleepingExiting()) {
            wakeUpImmediately();
            suppressSleep(200);
        }
        if (damageSource.is(DamageTypes.FALL)) {
            return false;
        }

        if (isTamingStunned() && !isTame()) {

            return super.hurt(damageSource, amount);
        }

        if (tryReactiveHitDodge(damageSource, amount)) {
            return false;
        }

        boolean wasFlying = isFlying();
        boolean wasRidden = isVehicle();
        boolean result = super.hurt(damageSource, amount);

        if (result && wasRidden && wasFlying && isVehicle()) {
            setFlying(true);
            setLanding(false);
            switchToAirNavigation();
        }

        return result;
    }

    @Override
    public int getMaxAirSupply() {
        return 20 * 60 * 3;
    }

    @Override
    public int increaseAirSupply(int currentAir) {
        int refillPerTick = 50;
        return Math.min(getMaxAirSupply(), currentAir + refillPerTick);
    }

    @Override
    public void thunderHit(@Nonnull ServerLevel level, @Nonnull LightningBolt lightning) {
        if (this.isOnFire()) this.clearFire();
    }
    @Override
    protected boolean canUseBarrelRoll() {
        return isFlying()
                && !areRiderControlsLocked()
                && !isDodging()
                && !isDashing()
                && !isGroundRending();
    }

    @Override
    protected boolean shouldForceBarrelRollUpright() {
        return onGround();
    }

    @Override
    protected boolean isBarrelRollRiddenForHelper(boolean ridden, boolean canBarrelRoll) {
        return canBarrelRoll;
    }

    @Override
    protected boolean isActivelyBarrelRolling() {
        return isFlying()
                && !areRiderControlsLocked()
                && !isDodging()
                && !isDashing()
                && !isGroundRending()
                && this.entityData.get(DATA_RIDER_FORWARD) > 0.1f
                && Math.abs(this.entityData.get(DATA_RIDER_STRAFE)) > 0.1f;
    }

    private int superchargeTicks = 0;
    public void startSupercharge(int ticks) {
        boolean wasNotSupercharged = !isSupercharged();
        this.superchargeTicks = Math.max(this.superchargeTicks, Math.max(0, ticks));
        if (wasNotSupercharged && isSupercharged()) {
            DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
            Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(configuredMaxHealth(config, BABY_MAX_HEALTH) * 2.0D);
            this.setHealth(this.getMaxHealth());
            this.allowGroundBeamDuringStorm = true;
            this.superchargeVfxCooldown = 20;
        }
    }
    
    
    public boolean isSupercharged() { return superchargeTicks > 0; }
    public float getDamageMultiplier() {
        if (!isSupercharged()) {
            return 1.0f;
        }
        double configured = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID)
                .extraDouble("summon_storm_supercharge_damage_multiplier", 2.0D);
        return (float) Math.max(0.0D, configured);
    }

    public void startTemporaryInvuln(int ticks) {
        this.tempInvulnTicks = Math.max(this.tempInvulnTicks, Math.max(0, ticks));
        this.setInvulnerable(true);
    }

    private int superchargeVfxCooldown = 0;
    private void spawnSuperchargeVfx() {
        if (!(this.level() instanceof ServerLevel server)) return;
        Vec3 center = this.position().add(0, this.getBbHeight() * 0.6, 0);
        double radius = Math.max(this.getBoundingBox().getXsize(), this.getBoundingBox().getZsize()) * 0.55;
        int bursts = 2 + this.random.nextInt(3);
        for (int i = 0; i < bursts; i++) {
            Vec3 dir = randomUnit(this.random);
            double length = 0.4 + this.random.nextDouble() * 0.7;
            Vec3 offset = randomUnit(this.random).scale(radius * 0.35);
            Vec3 from = center.add(offset);
            Vec3 to = from.add(dir.scale(length));
            float size = 0.5f + this.random.nextFloat() * 0.25f;
            emitMicroArc(server, from, to, size);
        }
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y, center.z,
                3, radius * 0.15, radius * 0.15, radius * 0.15, 0.0);
    }
    private void emitMicroArc(ServerLevel server, Vec3 from, Vec3 to, float size) {
        Vec3 delta = to.subtract(from);
        int steps = 2 + this.random.nextInt(3);
        Vec3 step = delta.scale(1.0 / steps);
        Vec3 pos = from;
        Vec3 dir = step.lengthSqr() > 1.0e-6 ? step.normalize() : randomUnit(this.random);
        for (int i = 0; i <= steps; i++) {
            if (this.random.nextFloat() < 0.7f) {
                server.sendParticles(new RaevyxLightningStormData(size, this.getTextureVariant() == VARIANT_NIGHT_GOLD),
                        pos.x, pos.y, pos.z, 1, dir.x, dir.y, dir.z, 0.0);
            }
            pos = pos.add(step);
        }
    }
    public void spawnGroundRendTrailParticles(Vec3 forwardDir, double speed) {
        if (!(this.level() instanceof ServerLevel server) || !this.onGround() || speed <= 0.0D) {
            clearGroundRendTrailAnchors();
            return;
        }

        Vec3 horizontal = new Vec3(forwardDir.x, 0.0D, forwardDir.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            clearGroundRendTrailAnchors();
            return;
        }

        horizontal = horizontal.normalize();
        Vec3 right = new Vec3(-horizontal.z, 0.0D, horizontal.x);
        Vec3 base = this.position()
                .subtract(horizontal.scale(this.getBbWidth() * 0.42D))
                .add(0.0D, 0.08D, 0.0D);
        double spread = this.getBbWidth() * 0.26D;
        double minY = this.getBoundingBox().minY - 0.2D;

        for (int i = 0; i < 5; i++) {
            double lateral = (i - 2) * spread * 0.6D + (this.random.nextDouble() - 0.5D) * 0.14D;
            Vec3 sample = base.add(right.scale(lateral));
            BlockPos groundPos = BlockPos.containing(sample.x, minY, sample.z);
            BlockState groundState = this.level().getBlockState(groundPos);
            if (groundState.isAir() || groundState.liquid()) {
                groundPos = groundPos.below();
                groundState = this.level().getBlockState(groundPos);
            }
            if (groundState.isAir() || groundState.liquid()) {
                continue;
            }

            double particleY = groundPos.getY() + 1.02D;
            server.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, groundState),
                    sample.x,
                    particleY,
                    sample.z,
                    0,
                    -horizontal.x * 0.12D + (this.random.nextDouble() - 0.5D) * 0.08D,
                    0.08D + this.random.nextDouble() * 0.10D,
                    -horizontal.z * 0.12D + (this.random.nextDouble() - 0.5D) * 0.08D,
                    1.0D
            );
        }

        spawnGroundRendLightningTrail(server, base, horizontal, right, -spread * 0.85D, false);
        spawnGroundRendLightningTrail(server, base, horizontal, right, spread * 0.85D, true);
    }

    private void spawnGroundRendLightningTrail(ServerLevel server, Vec3 base, Vec3 forward, Vec3 right, double lateralOffset, boolean rightSideTrail) {
        Vec3 anchorSample = base.add(right.scale(lateralOffset));
        Vec3 currentAnchor = resolveGroundTrailPoint(anchorSample);
        if (currentAnchor == null) {
            setGroundRendTrailAnchor(rightSideTrail, null);
            return;
        }

        Vec3 previousAnchor = getGroundRendTrailAnchor(rightSideTrail);
        setGroundRendTrailAnchor(rightSideTrail, currentAnchor);

        Vec3 start;
        Vec3 end;
        if (previousAnchor != null && previousAnchor.distanceToSqr(currentAnchor) <= GROUND_REND_MAX_LINK_DISTANCE_SQR) {
            start = previousAnchor;
            end = currentAnchor;
        } else {
            start = resolveGroundTrailPoint(anchorSample.subtract(forward.scale(GROUND_REND_BOLT_LINK_START_REACH)));
            end = currentAnchor;
            if (start == null) {
                return;
            }
        }

        if (start.distanceToSqr(end) < 0.04D) {
            return;
        }

        float size = 0.56F + this.random.nextFloat() * 0.14F;
        Vec3 midpoint = start.lerp(end, 0.5D);
        server.addFreshEntity(new RaevyxGroundRendTrailEntity(server, start, end, size, GROUND_REND_BOLT_LIFETIME, this.random.nextLong()));
        server.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                midpoint.x,
                midpoint.y,
                midpoint.z,
                1,
                0.03D,
                0.02D,
                0.03D,
                0.0D
        );
    }

    @Nullable
    private Vec3 getGroundRendTrailAnchor(boolean rightSideTrail) {
        return rightSideTrail ? this.groundRendRightTrailAnchor : this.groundRendLeftTrailAnchor;
    }

    private void setGroundRendTrailAnchor(boolean rightSideTrail, @Nullable Vec3 anchor) {
        if (rightSideTrail) {
            this.groundRendRightTrailAnchor = anchor;
        } else {
            this.groundRendLeftTrailAnchor = anchor;
        }
    }

    @Nullable
    private Vec3 resolveGroundTrailPoint(Vec3 sample) {
        BlockPos groundPos = BlockPos.containing(sample.x, this.getBoundingBox().minY - 0.2D, sample.z);
        BlockState groundState = this.level().getBlockState(groundPos);
        if (groundState.isAir() || groundState.liquid()) {
            groundPos = groundPos.below();
            groundState = this.level().getBlockState(groundPos);
        }
        if (groundState.isAir() || groundState.liquid()) {
            return null;
        }
        return new Vec3(sample.x, groundPos.getY() + 1.02D, sample.z);
    }
    private static Vec3 randomUnit(RandomSource rnd) {
        double u = rnd.nextDouble();
        double v = rnd.nextDouble();
        double theta = 2 * Math.PI * u;
        double z = 2 * v - 1;
        double r = Math.sqrt(1 - z * z);
        return new Vec3(r * Math.cos(theta), z, r * Math.sin(theta));
    }

    public boolean isInSitTransition() {
        return sitTransitions.isInTransition();
    }
    public boolean isSittingDownAnimation() {
        return sitTransitions.isSittingDown();
    }
    public boolean isStandingUpAnimation() {
        return sitTransitions.isStandingUp();
    }
    private int getSitDownAnimationTicks() {
        return 29;
    }
    private int getSitUpAnimationTicks() {
        return 20;
    }
    private int getFallAsleepAnimationTicks() {
        return 50;
    }
    private int getWakeUpAnimationTicks() {
        return 53;
    }

    @Override
    public float maxSitTicks() {
        return getSitDownAnimationTicks();
    }

    @Override
    public boolean supportsSleep() {
        return true;
    }

    @Override
    public DragonSleepPreferences getSleepPreferences() {
        return DragonSleepPreferences.DIURNAL();
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
    protected int getSleepSitDownDuration() {
        return getSitDownAnimationTicks();
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
        return getSitUpAnimationTicks();
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
    protected boolean isAlreadySeatedForSleep() {
        return getSitProgress() >= maxSitTicks() || shouldStaySeatedCommand();
    }

    @Override
    protected boolean shouldStaySeatedAfterWake(int sleepCommandSnapshot) {
        return isTame() && sleepCommandSnapshot == 1;
    }

    @Override
    public boolean isSleepSuppressed() {
        return super.isSleepSuppressed() || isTamingStunned();
    }

    @Override
    protected void onSleepLockCommand(int snapshot) {
        if (snapshot == 1) {
            this.setCommand(1);
        } else {
            this.setCommand(1);
        }
        this.setOrderedToSit(true);
        this.getNavigation().stop();
        this.setTarget(null);
        this.setRunning(false);
        this.setGroundMoveStateFromAI(0);
        this.setDeltaMovement(Vec3.ZERO);
        this.clearAerialStateForInterrupt();
    }

    @Override
    protected void onSleepUnlockCommand(int desired) {
        if (desired == 1) {
            this.setCommand(1);
            this.setOrderedToSit(true);
        } else {
            this.setCommand(desired);
            this.setOrderedToSit(false);
        }
        this.getNavigation().stop();
        this.setRunning(false);
        this.setGroundMoveStateFromAI(0);
    }

    @Override
    protected void onSleepSitDownAnimation() {
        animationHandler.triggerSitDownAnimation();
    }

    @Override
    protected void onSleepFallAsleepAnimation() {
        animationHandler.triggerFallAsleepAnimation();
    }

    @Override
    protected void onSleepLoopAnimation() {
    }

    @Override
    protected void onSleepWakeUpAnimation() {
        animationHandler.triggerWakeUpAnimation();
        setOrderedToSit(true);
    }

    @Override
    protected void onSleepSitUpAnimation() {
        animationHandler.triggerSitUpAnimation();
        setOrderedToSit(false);
    }

    @Override
    protected void onSleepExitSeated() {
        setOrderedToSit(true);
        setSitProgress(maxSitTicks());
    }

    @Override
    protected void onSleepExitStarted() {
        setOrderedToSit(true);
    }

    @Override
    protected void onSleepWakeUpImmediate() {
        setOrderedToSit(false);
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        InteractionResult result = lightningInteractionHandler.handleInteraction(player, hand);
        if (result == InteractionResult.PASS) {
            return super.mobInteract(player, hand);
        }
        
        return result;
    }

    @Override
    public void setOrderedToSit(boolean sitting) {
        boolean wasSitting = isOrderedToSit();
        super.setOrderedToSit(sitting);

        if (sitting) {
            if (isFlying()) {
                this.setLanding(true);
            }
            this.setRunning(false);
            this.getNavigation().stop();
        } else if (wasSitting) {
            if (!level().isClientSide) {
                clearAerialStateForInterrupt();
                switchToGroundNavigation();
                postStandUnlockTicks = Math.max(postStandUnlockTicks, 20);
            }
            if (this.getCommand() == 1) {
                this.setCommand(0);
            }
        }
    }

    @Override
    public boolean isFood(@Nonnull ItemStack stack) {
        return stack.is(ModTags.Items.RAEVYX_FOODS);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        saveRideableData(tag);
        tag.putInt("TimeFlying", timeFlying);
        tag.putBoolean("UsingAirNav", isUsingAirNavigation());
        tag.putLong("LastLandingGameTime", lastLandingGameTime);
        tag.putBoolean("LandingFlag", landingFlag);
        tag.putInt("LandingTimer", landingTimer);
        tag.putBoolean("LandedFlag", landedFlag);
        tag.putInt("LandedTimer", landedTimer);
        this.combatManager.saveToNBT(tag);
        tag.putInt("SuperchargeTicks", Math.max(0, this.superchargeTicks));
        tag.putInt("TempInvulnTicks", Math.max(0, this.tempInvulnTicks));
        tag.putBoolean("AllowGroundBeamStorm", this.allowGroundBeamDuringStorm);
        tag.putInt("FeedingCooldownTicks", Math.max(0, this.entityData.get(DATA_FEEDING_COOLDOWN)));
        tag.putFloat("BeamEnergy", getBeamEnergy());
        tag.putBoolean("BeamDepleted", isBeamDepleted());
        tamingController.save(tag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadRideableData(tag);
        boolean savedFlying = tag.getBoolean("Flying");
        this.timeFlying = tag.getInt("TimeFlying");
        boolean savedUsingAirNav = tag.getBoolean("UsingAirNav");
        this.lastLandingGameTime = tag.contains("LastLandingGameTime") ? tag.getLong("LastLandingGameTime") : Long.MIN_VALUE;
        this.landingFlag = tag.contains("LandingFlag") && tag.getBoolean("LandingFlag");
        this.landingTimer = tag.contains("LandingTimer") ? tag.getInt("LandingTimer") : 0;
        this.landedFlag = tag.contains("LandedFlag") && tag.getBoolean("LandedFlag");
        this.landedTimer = tag.contains("LandedTimer") ? tag.getInt("LandedTimer") : 0;
        if (!savedFlying) {
            landingTimer = 0;
        }
        this.clearRiderControlLock();
        this.takeoffLockTicks = 0;
        this.combatManager.loadFromNBT(tag);
        if (tag.contains("SuperchargeTicks")) {
            this.superchargeTicks = Math.max(0, tag.getInt("SuperchargeTicks"));
        }
        if (tag.contains("TempInvulnTicks")) {
            this.tempInvulnTicks = Math.max(0, tag.getInt("TempInvulnTicks"));
            if (this.tempInvulnTicks > 0) {
                this.setInvulnerable(true);
            }
        }

        if (tag.contains("AllowGroundBeamStorm")) {
            this.allowGroundBeamDuringStorm = tag.getBoolean("AllowGroundBeamStorm");
        }
        if (tag.contains("FeedingCooldownTicks")) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, tag.getInt("FeedingCooldownTicks")));
        }
        if (tag.contains("BeamEnergy")) {
            setBeamEnergy(tag.getFloat("BeamEnergy"));
        } else {
            setBeamEnergy(1.0f);
        }
        if (tag.contains("BeamDepleted")) {
            setBeamDepleted(tag.getBoolean("BeamDepleted"));
        } else {
            setBeamDepleted(false);
        }
        tamingController.load(tag);
        if (savedUsingAirNav) {
            switchToAirNavigation();
        } else {
            switchToGroundNavigation();
        }
        if (this.getCommand() != 1 && this.isOrderedToSit()) {
            this.setOrderedToSit(false);
        }
        boolean shouldHaveNoGravity = isFlying() || isHovering();
        this.setNoGravity(shouldHaveNoGravity);

        if (this.isOrderedToSit()) {
            this.getNavigation().stop();
            this.setTarget(null);
            this.setAggressive(false);
        }

        if (!this.isTame()) {
            this.setCommand(0);
            this.setOrderedToSit(false);
            this.setInSittingPose(false);
            sitTransitions.clear();
            clearSleepCooldowns();
        }
        applyConfiguredAttributes();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(movementController, transitionController, vocalController, actionController, fastActionController, flightController, interactionController);
    }

    private void setupAnimationControllers() {
        AnimationHelper.registerSoundKeyframes(this, movementController, transitionController, actionController,
                fastActionController, flightController, vocalController, interactionController);
        AnimationHelper.registerGrumbles(vocalController, this);
        animationHandler.setupMovementController(movementController);
        animationHandler.setupTransitionController(transitionController);
        animationHandler.setupActionController(actionController);
        animationHandler.setupFastActionController(fastActionController);
        animationHandler.setupFlightController(flightController);
        animationHandler.setupInteractionController(interactionController);
    }

    @Override
    public Map<String, VocalEntry> getVocalEntries() {
        return VOCAL_ENTRIES;
    }

    @Override
    public DragonSoundProfile getSoundProfile() {
        return RaevyxSoundProfile.INSTANCE;
    }

    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        return getMeleeMode() == 0 ?
            ModAbilities.RAEVYX_BITE :
            ModAbilities.RAEVYX_HORN_GORE;
    }

    @Override
    public DragonAbilityType<?, ?> getRoaringAbility() {
        return ModAbilities.RAEVYX_ROAR;
    }

    @Override
    public DragonAbilityType<?, ?> getChannelingAbility() {
        return ModAbilities.RAEVYX_SUMMON_STORM;
    }

    @Override
    public void lockRiderControls(int ticks) {
        super.lockRiderControls(ticks);
        this.setAccelerating(false);
        this.setGoingUp(false);
        this.setGoingDown(false);
        this.setDeltaMovement(Vec3.ZERO);
        if (!this.level().isClientSide) {
            this.getNavigation().stop();
            this.setTarget(null);
        }
    }

    private int takeoffLockTicks = 0;
    public boolean isTakeoffLocked() { return takeoffLockTicks > 0; }
    public void lockTakeoff(int ticks) { this.takeoffLockTicks = Math.max(this.takeoffLockTicks, Math.max(0, ticks)); }
    public void clearTakeoffLock() { this.takeoffLockTicks = 0; }
    private void tickTakeoffLock() { if (takeoffLockTicks > 0) takeoffLockTicks--; }
    public void clearTemporaryInvuln() {
        this.tempInvulnTicks = 0;
        if (!isDying()) this.setInvulnerable(false);
    }

    private final Map<Integer, Long> recentAggroIds = new ConcurrentHashMap<>();

    public void noteAggroFrom(LivingEntity target) {
        if (target == null || target.level().isClientSide) return;
        recentAggroIds.put(target.getId(), this.level().getGameTime() + AGGRO_TTL_TICKS);
    }

    public List<LivingEntity> getRecentAggro() {
        List<LivingEntity> out = new ArrayList<>();
        long now = this.level().getGameTime();
        Iterator<Map.Entry<Integer, Long>> it = recentAggroIds.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (e.getValue() < now) { it.remove(); continue; }
            Entity ent = this.level().getEntity(e.getKey());
            if (ent instanceof LivingEntity le && le.isAlive()) {
                out.add(le);
            } else {
                it.remove();
            }
        }
        return out;
    }

    private void tickRecentAggroCleanup() {
        if (recentAggroIds.isEmpty()) {
            return;
        }
        long now = this.level().getGameTime();
        recentAggroIds.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    @Override
    protected float getBabyHitboxScale() {
        return 0.4F;
    }

    @Override
    public void ageBoundaryReached() {
        super.ageBoundaryReached();
        applyConfiguredAttributes();
        this.refreshDimensions();
    }

    @Override
    public boolean canBreed() {
        return !this.isBaby() && this.getHealth() >= this.getMaxHealth() && this.isInLove();
    }

    @Override
    protected Supplier<? extends Block> getEggBlock() {
        return ModBlocks.RAEVYX_EGG;
    }

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(@Nonnull ServerLevel level, @Nonnull AgeableMob otherParent) {
        return createBreedOffspring(level, otherParent, ModEntities.RAEVYX.get(), Raevyx::applyConfiguredAttributes);
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@Nonnull Player player, @Nonnull Vec3 deltaIn) {
        if (areRiderControlsLocked()) {
            return Vec3.ZERO;
        }
        Vec3 input = riderController.getRiddenInput(player, deltaIn);
        return super.getRiddenInput(player, input);
    }
    @Override
    protected void tickRidden(@Nonnull Player player, @Nonnull Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        if (!this.level().isClientSide) {
            tickTakeoffLock();
        }
        if (!areRiderControlsLocked()) {
            riderController.tickRidden(player, travelVector);
        } else {
            player.fallDistance = 0.0F;
            this.fallDistance = 0.0F;
            this.setTarget(null);
            copyRiderYaw(player);
            this.setAccelerating(false);
            if (!this.isFlying()) {
                this.setGoingUp(false);
                this.setGoingDown(false);
            }
        }
    }

    @Override
    protected float getRiddenSpeed(@Nonnull Player rider) {
        if (areRiderControlsLocked()) {
            return 0.0F;
        }
        return riderController.getRiddenSpeed(rider);
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
        LivingEntity previousTarget = this.getTarget();
        super.setTarget(target);
    }

    private boolean shouldAggroOnSight() {
        if (isTame() || isBaby()) {
            return false;
        }
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        return config.extraBoolean("aggressive_wild", false);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return riderController.getControllingPassenger();
    }

    @Override
    public float getFlightSpeed() {
        float baseSpeed = (float) this.getAttributeValue(Attributes.FLYING_SPEED);
        if (this.isTame()) {
            return baseSpeed;
        }
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        return (float) (baseSpeed * config.extraDouble("wild_flying_speed_multiplier", 1.0D));
    }

    @Override
    public double getPreferredFlightAltitude() {
        return 15.0;
    }
    
    @Override
    public boolean canTakeoff() {
        return !isBaby() && !isGroundRending() && !isInWaterOrBubble() && onGround();
    }

    private boolean shouldStaySeatedCommand() {
        return this.isTame() && this.getCommand() == 1;
    }

    public boolean canBeBound() {
        return !isSleeping() && !isDying() && !isBeaming();
    }

    @Override
    public boolean ignoresLeashPull() {
        return true;
    }

    @Override
    protected ScreenShakeComponent getScreenShakeComponent() {
        return screenShakeComponent;
    }

    @Override
    public double getShakeDistance() {
        return 25.0;
    }

    @Override
    protected void dropAdditionalDeathLootAfterBase(@NotNull DamageSource source) {
        if (!level().isClientSide && getGender() == DragonGender.FEMALE) {
            DragonLootTables.dropEntityLoot(this, DragonLootTables.RAEVYX_FEMALE_DEATH, source);
        }
    }
}
