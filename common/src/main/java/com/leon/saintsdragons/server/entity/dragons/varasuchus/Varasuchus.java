package com.leon.saintsdragons.server.entity.dragons.varasuchus;

import com.leon.saintsdragons.server.ai.navigation.GenericSwimSteeringController;
import com.mojang.serialization.Dynamic;
import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;

import com.leon.saintsdragons.util.animation.AnimationHelper;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.config.dragon.DragonTamingChance;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.common.registry.ModBlocks;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrain;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.profiles.VarasuchusBrain;
import com.leon.saintsdragons.server.entity.controller.DragonRiderControllerHelper;
import com.leon.saintsdragons.server.entity.controller.varasuchus.VarasuchusRiderController;
import com.leon.saintsdragons.server.ai.navigation.PathNavigateGround;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.base.RideableGroundDragon;
import com.leon.saintsdragons.server.entity.component.DragonMotionMath;
import com.leon.saintsdragons.server.entity.component.DragonForwardMovementComponent;
import com.leon.saintsdragons.server.entity.component.DragonRoostComponent;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.handlers.*;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusTailguardAbility;
import com.leon.saintsdragons.server.entity.component.ScreenShakeComponent;
import com.leon.saintsdragons.server.entity.interfaces.*;
import com.leon.saintsdragons.server.entity.interfaces.DragonMovementCapability;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.loot.DragonLootTables;
import com.leon.saintsdragons.server.world.DragonSpawnRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class Varasuchus extends RideableGroundDragon implements SemiAquaticDragon, ShakesScreen,
        PassiveTreeDestroyer, ScentAssessingDragon {
    private static final VarasuchusBrain DRAGON_BRAIN = new VarasuchusBrain();
    private static final ResourceKey<Structure> VARASUCHUS_ROOST_STRUCTURE = ResourceKey.create(
            Registries.STRUCTURE,
            SaintsDragonsCommon.rl("varasuchus_roost")
    );
    public static final double ROOST_SLEEP_RADIUS = 3.0D;
    public static final double ROOST_TERRITORY_RADIUS = 48.0D;
    public static final double ROOST_TERRITORY_RETURN_RADIUS = 32.0D;
    private static final int ROOST_SLEEP_SETTLE_TICKS = 60;
    private final DragonRoostComponent roostComponent = new DragonRoostComponent(
            this,
            VARASUCHUS_ROOST_STRUCTURE,
            ROOST_SLEEP_RADIUS,
            ROOST_TERRITORY_RADIUS,
            ROOST_SLEEP_SETTLE_TICKS
    );
    public Varasuchus(EntityType<? extends Varasuchus> type, Level level) {
        super(type, level);
        this.screenShakeComponent = new ScreenShakeComponent(this, DATA_SCREEN_SHAKE_AMOUNT, SHAKE_DECAY_PER_TICK);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
        this.setMaxUpStep(1.4F);
        this.groundNavigation = new PathNavigateGround(this, level);
        this.landMoveControl = new MoveControl(this);
        this.landLookControl = new VarasuchusLookController(this);
        this.swimSteering = new GenericSwimSteeringController(this);
        this.asyncSwimController = new AsyncSwimController(this, this.swimSteering);
        this.navigation = this.groundNavigation;
        this.moveControl = this.landMoveControl;
        this.lookControl = this.landLookControl;
        this.riderController = new VarasuchusRiderController(this);
        this.movementController = new AnimationController<>(this, "movement", 2, animationHandler::movementPredicate);
        this.actionController = new AnimationController<>(this, VarasuchusAnimationHandler.ACTION_CONTROLLER, 4, animationHandler::actionPredicate);
        this.fastActionController = new AnimationController<>(this, VarasuchusAnimationHandler.FAST_ACTION_CONTROLLER, 1, animationHandler::fastActionPredicate);
        this.vocalController = new AnimationController<>(this, AnimationHelper.VOCAL_CONTROLLER, 2, AnimationHelper::vocalIdle);
        this.interactionController = new AnimationController<>(this, AnimationHelper.INTERACTION_CONTROLLER, 1, AnimationHelper::interactionIdle);
        setupAnimationControllers();
        seedAmbientSoundTimer(MIN_AMBIENT_DELAY, MAX_AMBIENT_DELAY, 80);
        if (!level.isClientSide) {
            applyConfiguredAttributes();
            this.setHealth(this.getMaxHealth());
        }
    }

    public static final ResourceLocation VOID_KISSED_VARIANT_ID = SaintsDragonsCommon.rl("void_kissed");

    public static boolean shouldUseVoidKissedVariant(Level level) {
        return level != null && level.dimension() == Level.END;
    }

    @Override
    public EnumSet<DragonMovementCapability> movementCapabilities() {
        return EnumSet.of(DragonMovementCapability.WALK, DragonMovementCapability.SWIM);
    }

    @Override
    protected ResourceLocation getDragonAttributesId() {
        return DragonAttributeConfigLoader.VARASUCHUS_ID;
    }

    @Override
    protected ResourceLocation chooseSpawnTextureVariantId(@NotNull ServerLevelAccessor levelAccessor,
                                                          @NotNull DifficultyInstance difficulty,
                                                          @NotNull MobSpawnType reason,
                                                          @Nullable SpawnGroupData spawnData,
                                                          @Nullable CompoundTag spawnTag) {
        if (reason == MobSpawnType.SPAWN_EGG && shouldUseVoidKissedVariant(levelAccessor.getLevel())) {
            return VOID_KISSED_VARIANT_ID;
        }
        return super.chooseSpawnTextureVariantId(levelAccessor, difficulty, reason, spawnData, spawnTag);
    }

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor levelAccessor,
                                                 @NotNull DifficultyInstance difficulty,
                                                 @NotNull MobSpawnType reason,
                                                 @Nullable SpawnGroupData spawnData,
                                                 @Nullable CompoundTag spawnTag) {
        SpawnGroupData data = super.finalizeSpawn(levelAccessor, difficulty, reason, spawnData, spawnTag);
        roostComponent.initializeHomeFromSpawn(levelAccessor, reason);
        return data;
    }

    private static final EntityDataAccessor<Boolean> DATA_SWIMMING = SynchedEntityData.defineId(Varasuchus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SWIM_TURN = SynchedEntityData.defineId(Varasuchus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SWIM_PITCH = SynchedEntityData.defineId(Varasuchus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SWIM_PITCH_RAD = SynchedEntityData.defineId(Varasuchus.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_PITCH_KEY_MODE = SynchedEntityData.defineId(Varasuchus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PHASE_TWO = SynchedEntityData.defineId(Varasuchus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_WILD_RIDE_ACTIVE = SynchedEntityData.defineId(Varasuchus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LEAPING = SynchedEntityData.defineId(Varasuchus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_GROUND_DASH_TICKS = SynchedEntityData.defineId(Varasuchus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_GROUND_DASH_X = SynchedEntityData.defineId(Varasuchus.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GROUND_DASH_Z = SynchedEntityData.defineId(Varasuchus.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GROUND_DASH_DRAG = SynchedEntityData.defineId(Varasuchus.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SCREEN_SHAKE_AMOUNT = SynchedEntityData.defineId(Varasuchus.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_FEEDING_COOLDOWN =
            SynchedEntityData.defineId(Varasuchus.class, EntityDataSerializers.INT);
    private static final Map<String, VocalEntry> VOCAL_ENTRIES = new VocalEntryBuilder()
            .add("grumble1", AnimationHelper.VOCAL_CONTROLLER, "animation.varasuchus.grumble1", ModSounds.VARASUCHUS_GRUMBLE_1, 0.8f, 0.95f, 0.1f, false, false, true)
            .add("grumble2", AnimationHelper.VOCAL_CONTROLLER, "animation.varasuchus.grumble2", ModSounds.VARASUCHUS_GRUMBLE_2, 0.8f, 0.95f, 0.1f, false, false, true)
            .add("grumble3", AnimationHelper.VOCAL_CONTROLLER, "animation.varasuchus.grumble3", ModSounds.VARASUCHUS_GRUMBLE_3, 0.8f, 0.95f, 0.1f, false, false, true)
            .add("varasuchus_hurt", AnimationHelper.INTERACTION_CONTROLLER, "animation.varasuchus.hurt", ModSounds.VARASUCHUS_HURT, 1.1f, 0.95f, 0.1f, false, true, true)
            .add("varasuchus_die", AnimationHelper.INTERACTION_CONTROLLER, "animation.varasuchus.die", ModSounds.VARASUCHUS_DIE, 1.35f, 0.9f, 0.05f, false, true, true)
            .build();
    public static final int RECENT_ATTACKER_PRIORITY_TICKS = 20 * 30;
    public static final double RIDER_WALK_SPEED = 0.15D;
    public static final double RIDER_RUN_SPEED = 0.30D;
    public static final float RIDER_KEY_PITCH_DEG = 25.0f;
    private static final double RIDER_JUMP_STRENGTH = 1.0D;
    private static final double RIDER_JUMP_FORWARD_BOOST = 0.4D;
    private static final int MIN_AMBIENT_DELAY = 200;
    private static final int MAX_AMBIENT_DELAY = 600;
    private static final int SCENT_ASSESSMENT_ANIMATION_TICKS = 70;
    private static final double BABY_MAX_HEALTH = 80.0D;
    private static final double BABY_ARMOR = 0.0D;
    private static final double GROUND_MOVEMENT_SPEED = 0.33D;
    private static final float BABY_HITBOX_SCALE = 0.5F;
    private static final double LEAP_HORIZONTAL_DRAG = 0.92D;
    private static final float DEFAULT_DASH_TAIL_SWIPE_DAMAGE = 14.0F;
    private static final float DEFAULT_DASH_CLAW_DAMAGE = 16.0F;
    private static final int MIN_WILD_TAME_TICKS = 60;
    private static final int WILD_RIDE_BUCK_COOLDOWN_TICKS = 20 * 5;
    private static final int MAX_TAMING_PROGRESS = 400;
    private static final int WILD_RIDE_BUCK_DURATION_TICKS = 90;
    private static final double WILD_RIDE_WALK_SPEED = 0.9D;
    public static final double BREED_PARTNER_RANGE = 30.0D;
    public static final double BREED_DISTANCE_SQR = 16.0D;
    private static final int PHASE_TWO_LINGER_TICKS = 20 * 30;
    private static final int FLEX_CONTROL_LOCK_TICKS = 70;
    private static final int FLEX2_CONTROL_LOCK_TICKS = 140;
    private static final int FLEX_COOLDOWN_TICKS = 60;

    @Override
    public int getScentAssessmentDurationTicks() {
        return SCENT_ASSESSMENT_ANIMATION_TICKS;
    }

    private final AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);
    private final VarasuchusAnimationHandler animationHandler = new VarasuchusAnimationHandler(this);
    private final VarasuchusInteractionHandler interactionHandler = new VarasuchusInteractionHandler(this);
    private final VarasuchusRiderController riderController;
    private final AnimationController<Varasuchus> movementController;
    private final AnimationController<Varasuchus> actionController;
    private final AnimationController<Varasuchus> fastActionController;
    private final AnimationController<Varasuchus> vocalController;
    private final AnimationController<Varasuchus> interactionController;
    private final PathNavigation groundNavigation;
    private final MoveControl landMoveControl;
    private final VarasuchusLookController landLookControl;
    private final GenericSwimSteeringController swimSteering;
    private final AsyncSwimController asyncSwimController;
    private boolean swimming;
    private int swimTicks;
    private int ticksInWater;
    private int ticksOutOfWater;
    private float swimTurnSmoothedYaw;
    private int swimTurnState;
    private int swimPitchStateTicks;
    private float swimRollAngle = 0f;
    private float prevSwimRollAngle = 0f;
    private float swimPitchRad = 0f;
    private float prevSwimPitchRad = 0f;
    private float smoothedPlayerSwimPitchRad = 0f;
    private float clientSwimPitchRad = 0f;
    private float prevClientSwimPitchRad = 0f;
    private boolean useLeftClawNext = true;
    private boolean useLeftTailAttackNext = true;
    private static final float SHAKE_DECAY_PER_TICK = 0.02F;
    private final ScreenShakeComponent screenShakeComponent;
    private int phaseTwoLingerTicks = 0;
    private final DragonForwardMovementComponent groundDash = new DragonForwardMovementComponent(
            this,
            new DragonForwardMovementComponent.StateAccess() {
                @Override
                public void start(int ticks, Vec3 velocity, boolean dashing, boolean dodging, double horizontalDrag) {
                    entityData.set(DATA_GROUND_DASH_TICKS, Math.max(1, ticks));
                    entityData.set(DATA_GROUND_DASH_X, (float) velocity.x);
                    entityData.set(DATA_GROUND_DASH_Z, (float) velocity.z);
                    entityData.set(DATA_GROUND_DASH_DRAG, (float) horizontalDrag);
                    entityData.set(DATA_LEAPING, dashing);
                }

                @Override
                public int ticks() {
                    return entityData.get(DATA_GROUND_DASH_TICKS);
                }

                @Override
                public void setTicks(int ticks) {
                    entityData.set(DATA_GROUND_DASH_TICKS, Math.max(0, ticks));
                }

                @Override
                public Vec3 velocity() {
                    return new Vec3(entityData.get(DATA_GROUND_DASH_X), 0.0D, entityData.get(DATA_GROUND_DASH_Z));
                }

                @Override
                public void setVelocity(Vec3 velocity) {
                    entityData.set(DATA_GROUND_DASH_X, (float) velocity.x);
                    entityData.set(DATA_GROUND_DASH_Z, (float) velocity.z);
                }

                @Override
                public double horizontalDrag() {
                    return entityData.get(DATA_GROUND_DASH_DRAG);
                }

                @Override
                public void clear() {
                    entityData.set(DATA_GROUND_DASH_TICKS, 0);
                    entityData.set(DATA_GROUND_DASH_X, 0.0F);
                    entityData.set(DATA_GROUND_DASH_Z, 0.0F);
                    entityData.set(DATA_GROUND_DASH_DRAG, 1.0F);
                    entityData.set(DATA_LEAPING, false);
                }
            }
    );
    private boolean leapDamageApplied = false;
    private boolean lastDashWasRight = false;
    private boolean wildRideActive = false;
    private int wildRideTicks = 0;
    private int nextBuckAttemptTick = 0;
    private int wildRideBuckCooldownTicks = 0;
    private int cumulativeWildRideProgress = 0;
    @Nullable
    private UUID babyProtectionAggroTargetUuid;


    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case DOUBLE_TAP_W,
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
    public boolean areRiderControlsLocked() {
        return super.areRiderControlsLocked() || isWildRideActive();
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
            this.setTarget(null);
        }
    }


    @Override
    protected boolean isRiderInputLocked(Player player) {
        return areRiderControlsLocked();
    }

    @Override
    protected float getBabyHitboxScale() {
        return BABY_HITBOX_SCALE;
    }

    @Override
    protected void applyRiderVerticalInput(Player player, boolean goingUp, boolean goingDown, boolean locked) {
        if (locked) {
            setGoingUp(false);
            setGoingDown(false);
            return;
        }
        boolean inWater = this.isSwimming() || this.isInWaterOrBubble();
        if (inWater) {
            setGoingUp(goingUp);
            setGoingDown(goingDown);
        } else {
            setGoingUp(false);
            setGoingDown(false);
        }
    }

    @Override
    protected boolean handleCustomRiderAction(ServerPlayer player, DragonRiderAction action,
                                              String abilityName, boolean locked) {
        if (action == DragonRiderAction.ABILITY_STOP
                && ModAbilities.VARASUCHUS_TAILGUARD.getName().equals(abilityName)
                && tryReleaseHeldRidingAbility(abilityName)) {
            return true;
        }

        if (locked) {
            return false;
        }

        if (action == DragonRiderAction.DOUBLE_TAP_W) {
            onRiderDash(player);
            return true;
        }
        return super.handleCustomRiderAction(player, action, abilityName, locked);
    }

    @Override
    protected RiderFlexSpec getRiderFlexSpec() {
        return new RiderFlexSpec(
                isPhaseTwoActive() ? FLEX2_CONTROL_LOCK_TICKS : FLEX_CONTROL_LOCK_TICKS,
                FLEX_COOLDOWN_TICKS
        );
    }

    @Override
    protected boolean canRiderFlex(ServerPlayer player, RiderFlexSpec spec) {
        return super.canRiderFlex(player, spec)
                && !isBaby()
                && !isDying()
                && onGround()
                && !isSwimming()
                && !isInWaterOrBubble()
                && !isOrderedToSit()
                && !isInSitTransition()
                && !isSleeping()
                && !isSleepTransitioning()
                && !isGroundDashing()
                && !isWildRideActive()
                && getActiveAbility() == null
                && !isAbilityActive(ModAbilities.VARASUCHUS_PHASE_SHIFT);
    }

    @Override
    protected void playRiderFlex(ServerPlayer player, RiderFlexSpec spec) {
        animationHandler.triggerFlexAnimation();
        this.getSoundHandler().playClientSound(
                this,
                this.position(),
                isPhaseTwoActive() ? ModSounds.VARASUCHUS_FLEX2.get() : ModSounds.VARASUCHUS_FLEX.get(),
                2.0f,
                1.0f
        );
    }

    @Override
    protected void onRiderDash(Player player) {
        onRiderLeap(player);
    }

    protected void onRiderLeap(Player player) {
        startGroundDash();
    }

    public void startClientRiderDashPrediction() {
        if (level().isClientSide) {
            startGroundDash();
        }
    }

    private boolean isGroundDashing() {
        return level().isClientSide ? this.entityData.get(DATA_LEAPING) : groundDash.isActive();
    }

    private void startGroundDash() {
        if (groundDash.getCooldownTicks() > 0) {
            return;
        }

        if (groundDash.isActive()) {
            return;
        }
        final boolean phaseTwo = isPhaseTwoActive();
        if (this.isSwimming() || this.isInWaterOrBubble()) {
            return;
        }
        final int LEAP_DURATION = phaseTwo
                ? (int) Math.round(1.6667 * 20)
                : (int) Math.round(2.3333 * 20);
        final int LEAP_COOLDOWN = phaseTwo
                ? 38
                : 60;
        final double LEAP_DISTANCE = 26.0D;
        Vec3 forward = DragonMotionMath.horizontalForward(this.getYRot());
        double perTickSpeed = DragonMotionMath.speedForIntegratedDistance(
                LEAP_DISTANCE,
                LEAP_HORIZONTAL_DRAG,
                LEAP_DURATION
        );
        if (perTickSpeed <= 0.0D) {
            return;
        }
        Vec3 leapVector = forward.scale(perTickSpeed);

        if (!groundDash.startDash(
                leapVector,
                LEAP_DURATION,
                LEAP_COOLDOWN,
                LEAP_HORIZONTAL_DRAG
        )) {
            return;
        }
        leapDamageApplied = false;
        if (phaseTwo) {
            lastDashWasRight = !lastDashWasRight;
            triggerAnim(VarasuchusAnimationHandler.MOVEMENT_CONTROLLER, lastDashWasRight ? "phase2_dash_right" : "phase2_dash_left");
            if (!this.level().isClientSide) {
                this.getSoundHandler().playMovingEntitySound(ModSounds.VARASUCHUS_PHASE2_DASH.get(), 1.0f, 1.0f, LEAP_DURATION);
            }
        } else {
            triggerAnim(VarasuchusAnimationHandler.MOVEMENT_CONTROLLER, "tail_swipe_left");
            if (!this.level().isClientSide) {
                this.getSoundHandler().playMovingEntitySound(ModSounds.VARASUCHUS_TAIL_SWIPE.get(), 1.0f, 1.0f, 100);
            }
        }
    }

    private void tickLeapState() {
        groundDash.tickState();
        if (!groundDash.isActive()) {
            leapDamageApplied = false;
            return;
        }

        if (leapDamageApplied) {
            return;
        }

        boolean phaseTwo = isPhaseTwoActive();
        int damageTick = phaseTwo ? 18 : 32;
        if (groundDash.getElapsedTicks() >= damageTick && this.isVehicle()) {
            leapDamageApplied = true;
            Vec3 tailPos = this.getBoundingBox().getCenter();
            AABB damageBox = new AABB(tailPos, tailPos).inflate(8.0D);
            List<LivingEntity> entities = this.level().getEntitiesOfClass(
                LivingEntity.class,
                damageBox,
                entity -> entity != this && entity != this.getControllingPassenger() && !this.isAlly(entity)
            );

            for (LivingEntity target : entities) {
                float damage = phaseTwo ? resolveDashClawDamage() : resolveDashTailSwipeDamage();
                target.hurt(this.damageSources().mobAttack(this), damage);

                if (!phaseTwo) {
                    Vec3 knockbackDir = target.position().subtract(this.position()).normalize();
                    target.push(knockbackDir.x * 2.0, 0.8, knockbackDir.z * 2.0);
                    target.hurtMarked = true;
                }
            }
        }
    }

    private float resolveDashTailSwipeDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.VARASUCHUS_ID)
                .abilityDamage("dash_tail_swipe", DEFAULT_DASH_TAIL_SWIPE_DAMAGE);
    }

    private float resolveDashClawDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.VARASUCHUS_ID)
                .abilityDamage("dash_claw", DEFAULT_DASH_CLAW_DAMAGE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.VARASUCHUS_ID);
        return TamableAnimal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, config.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, GROUND_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SWIMMING, false);
        this.entityData.define(DATA_SWIM_TURN, 0);
        this.entityData.define(DATA_SWIM_PITCH, 0);
        this.entityData.define(DATA_SWIM_PITCH_RAD, 0.0F);
        this.entityData.define(DATA_PITCH_KEY_MODE, false);
        this.entityData.define(DATA_PHASE_TWO, false);
        this.entityData.define(DATA_WILD_RIDE_ACTIVE, false);
        this.entityData.define(DATA_LEAPING, false);
        this.entityData.define(DATA_GROUND_DASH_TICKS, 0);
        this.entityData.define(DATA_GROUND_DASH_X, 0.0F);
        this.entityData.define(DATA_GROUND_DASH_Z, 0.0F);
        this.entityData.define(DATA_GROUND_DASH_DRAG, 1.0F);
        this.entityData.define(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        this.entityData.define(DATA_FEEDING_COOLDOWN, 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(movementController, vocalController, actionController, fastActionController, interactionController);

    }

    private void setupAnimationControllers() {
        AnimationHelper.registerSoundKeyframes(this, movementController, actionController,
                fastActionController, vocalController, interactionController);
        animationHandler.setupMovementController(movementController);
        AnimationHelper.registerGrumbles(vocalController, this);
        animationHandler.setupActionController(actionController);
        animationHandler.setupFastActionController(fastActionController);
        animationHandler.setupInteractionController(interactionController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return dragonCache;
    }

    @Override
    public DragonSoundProfile getSoundProfile() {
        return VarasuchusSoundProfile.INSTANCE;
    }

    @Override
    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return ModAbilities.VARASUCHUS_HURT;
    }

    @Override
    protected DragonAbilityType<?, ?> getDeathAbilityType() {
        return ModAbilities.VARASUCHUS_DIE;
    }

    @Override
    public int getDeathAnimationDurationTicks() {
        return 56;
    }

    @Override
    public Map<String, VocalEntry> getVocalEntries() {
        return VOCAL_ENTRIES;
    }

    private String selectAmbientGrumble() {
        if (isDying() || getTarget() != null || getActiveAbility() != null || areRiderControlsLocked()) {
            return null;
        }

        return selectWeightedAmbientVocal("grumble1", 0.4f, "grumble2", 0.7f, "grumble3");
    }

    private void handleAmbientSounds() {
        if (isBaby() || isDying() || isOrderedToSit() || isSleeping() || isSleepTransitioning() || isInSitTransition() || getSleepAmbientCooldownTicks() > 0 || areRiderControlsLocked()) {
            return;
        }
        tickAmbientVocalSounds(MIN_AMBIENT_DELAY, MAX_AMBIENT_DELAY, this::selectAmbientGrumble);
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            roostComponent.tick();
        }
        super.tick();

        if (level() instanceof ServerLevel serverLevel) {
            DragonDestructionManager.applyPassiveTreeDestruction(serverLevel, this);
        }

        tickSittingState();
        tickScreenShake();
        updateSittingProgress();
        tickFeedingCooldown();

        if (level().isClientSide && groundDash.getCooldownTicks() > 0) {
            groundDash.tickCooldown();
        }

        if (!level().isClientSide) {
            tickLeapState();
            handleAmbientSounds();
            tickRiderControlLock();
            tickWildRideBuckCooldown();
            boolean inWater = this.isInWaterOrBubble();
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

            if (this.getControllingPassenger() != null && this.lookControl != landLookControl) {
                this.lookControl = landLookControl;
            }

            if ((isSleeping() || isSleepingEntering() || isSleepTransitioning())
                    && (this.getTarget() != null || this.isAggressive() || this.isInWaterOrBubble())) {
                if (this.getTarget() != null || this.isAggressive()) {
                    startSleepExit();
                } else {
                    wakeUpImmediately();
                }
                suppressSleep(200);
            }

            this.tickAnimationStates();
            if (swimming || isInWaterOrBubble()) {
                this.updateSwimOrientationState();
            }
            if (wildRideActive) {
                this.tickWildRideState();
            }
            if (this.isPhaseTwoActive()) {
                tickPhaseTwoLinger();
            }
        }
        tickClientSideUpdates();
    }

    @Override
    public void travel(@NotNull Vec3 motion) {
        if (isGroundDashing()) {
            super.travel(Vec3.ZERO);
            if (getControllingPassenger() instanceof Player) {
                groundDash.steerHorizontal(DragonMotionMath.horizontalForward(this.getYRot()));
            }
            groundDash.applyTravelMotion();
            return;
        }
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player && !isWildRideActive()) {
            if (areRiderControlsLocked()) {
                this.setDeltaMovement(Vec3.ZERO);
                return;
            }
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }
            if (this.isInWaterOrBubble()) {
                handleRiddenSwimming(motion);
            } else {
                travelStandardRiddenGround(player, getRiddenInput(player, motion), riderController.getRiddenSpeed(player));
            }
        } else {
            super.travel(motion);
        }
    }

    @Override
    public @NotNull PathNavigation getNavigation() {
        return groundNavigation;
    }

    @Override
    public double getSwimSpeed() {
        return 1;
    }

    @Override
    public AsyncSwimController getAiSwimController() {
        return this.asyncSwimController;
    }

    @Override
    protected Brain.Provider<Varasuchus> brainProvider() {
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
    protected void onLocomotionModeTick(DragonLocomotionMode mode) {
        if (mode == DragonLocomotionMode.WATER && !swimming) {
            enterSwimState();
        } else if (mode != DragonLocomotionMode.WATER && swimming) {
            exitSwimState();
        }
    }

    @Override
    protected void onEnterLocomotionMode(DragonLocomotionMode mode, DragonLocomotionMode previousMode) {
        super.onEnterLocomotionMode(mode, previousMode);
        if (mode == DragonLocomotionMode.WATER) {
            enterSwimState();
        }
    }

    @Override
    protected void onExitLocomotionMode(DragonLocomotionMode previousMode, DragonLocomotionMode nextMode) {
        super.onExitLocomotionMode(previousMode, nextMode);
        if (previousMode == DragonLocomotionMode.WATER) {
            exitSwimState();
        }
    }


    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        InteractionResult handlerResult = interactionHandler.handleInteraction(player, hand);
        if (handlerResult != InteractionResult.PASS) {
            return handlerResult;
        }
        return super.mobInteract(player, hand);
    }

    public boolean beginUntamedRide(Player player) {
        if (this.isTame() || this.isBaby() || this.isVehicle() || player.isSecondaryUseActive()) {
            return false;
        }
        if (this.level().isClientSide) {
            return true;
        }
        if (wildRideBuckCooldownTicks > 0) {
            int seconds = Math.max(1, (wildRideBuckCooldownTicks + 19) / 20);
            player.displayClientMessage(
                    Component.translatable("entity.saintsdragons.varasuchus.buck_cooldown", this.getName(), seconds),
                    true
            );
            return false;
        }
        player.startRiding(this);
        startWildRideSequence();
        return true;
    }

    public void awardTamingAdvancement(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var advancement = serverPlayer.server.getAdvancements()
                    .getAdvancement(SaintsDragonsCommon.rl("tame_varasuchus"));
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "tame_varasuchus");
            }
        }
    }

    public void applyConfiguredAttributes() {
        DragonAttributeConfig config = getConfiguredDragonAttributes();
        applyConfiguredHealthAndArmor(config, BABY_MAX_HEALTH, BABY_ARMOR);
        setAttributeBase(Attributes.MOVEMENT_SPEED, GROUND_MOVEMENT_SPEED);
        clampHealthToMax();
    }

    @Override
    public boolean isFood(@Nonnull ItemStack stack) {
        return stack.is(ModTags.Items.VARASUCHUS_FOODS);
    }

    @Override
    protected boolean canRiderGroundRun() {
        if (this.isAbilityActive(ModAbilities.VARASUCHUS_SLASH_BARRAGE)) {
            this.setAccelerating(false);
            return false;
        }
        return true;
    }
    
    @Override
    protected boolean canGroundDragonJump() {
        return !isSwimming() && !isInWaterOrBubble() && !isGroundDashing();
    }

    @Override
    protected boolean usesGroundJumpLandingAnimation() {
        return true;
    }

    @Override
    protected void triggerGroundJumpAnimation() {
        animationHandler.triggerJumpAnimation();
    }

    @Override
    protected void triggerGroundJumpLandedAnimation() {
        animationHandler.triggerJumpLandedAnimation();
    }

    @Override
    protected double getRiderJumpStrength() {
        return RIDER_JUMP_STRENGTH;
    }

    @Override
    protected double getRiderJumpForwardBoost() {
        return RIDER_JUMP_FORWARD_BOOST;
    }

    @Override
    protected int getRiderGroundJumpAnimationFallHoldTicks() {
        return 3;
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        if (areRiderControlsLocked()) {
            return Vec3.ZERO;
        }
        Vec3 input = riderController.getRiddenInput(player, deltaIn);

        if (!level().isClientSide) {
            float fwd = (float) Mth.clamp(input.z, -1.0D, 1.0D);
            float str = (float) Mth.clamp(input.x, -1.0D, 1.0D);
            setLastRiderForward(Math.abs(fwd) > 0.02f ? fwd : 0f);
            setLastRiderStrafe(Math.abs(str) > 0.02f ? str : 0f);
        }
        return input;
    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
        if (!level().isClientSide) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 220, 0, true, false, false));
        }
        if (isWildRideActive()) {
            handleUntamedRideWhileMounted(player);
            return;
        }
        super.tickRidden(player, travelVector);
        if (areRiderControlsLocked()) {
            player.fallDistance = 0.0F;
            this.fallDistance = 0.0F;
            this.setTarget(null);
            copyRiderYaw(player);
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

    @Override
    public void jumpFromGround() {
        super.jumpFromGround();
        this.setGroundMoveStateFromRider(1);
    }

    @Override
    protected boolean canUseResolvedRidingAbility(DragonAbilityType<?, ?> abilityType) {
        if (areRiderControlsLocked()) {
            return false;
        }
        return super.canUseResolvedRidingAbility(abilityType);
    }

    @Override
    public double getPassengersRidingOffset() {
        return riderController.getPassengersRidingOffset();
    }

    @Override
    protected void positionRider(@Nonnull @NotNull Entity passenger, @Nonnull @NotNull Entity.MoveFunction moveFunction) {
        riderController.positionRider(passenger, moveFunction);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return riderController.getControllingPassenger();
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (this.isBaby()) {
            super.setTarget(null);
            return;
        }
        if (!isBabyProtectionAggroTarget(target)) {
            clearBabyProtectionAggroTarget();
        }
        super.setTarget(target);
    }

    @Override
    public boolean isTargetValid(@Nullable LivingEntity target) {
        if (!super.isTargetValid(target)) {
            return false;
        }
        if (target instanceof Player && isBabyProtectionAggroTarget(target)) {
            return hasNearbyWildBaby();
        }
        return true;
    }

    @Override
    public boolean isWildAggressionEnabled() {
        if (isTame() || isBaby()) {
            return false;
        }
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.VARASUCHUS_ID);
        return config.extraBoolean("aggressive_wild", true);
    }

    public boolean hasNearbyWildBaby() {
        return !this.level().getEntitiesOfClass(
                Varasuchus.class,
                this.getBoundingBox().inflate(16.0D),
                baby -> baby != null
                        && baby != this
                        && baby.isBaby()
                        && baby.isAlive()
                        && !baby.isTame()
        ).isEmpty();
    }

    private void triggerFailedTamingAggro(@Nullable Player rider) {
        if (!isWildAggressionEnabled()
                || rider == null
                || !rider.isAlive()
                || rider.isCreative()
                || rider.isSpectator()) {
            return;
        }
        clearBabyProtectionAggroTarget();
        this.getBrain().setMemory(DragonMemories.ATTACK_TARGET, rider);
        this.setTarget(rider);
    }

    public void markBabyProtectionAggroTarget(@Nullable LivingEntity target) {
        if (target instanceof Player player) {
            this.babyProtectionAggroTargetUuid = player.getUUID();
        } else {
            this.babyProtectionAggroTargetUuid = null;
        }
    }

    public void clearBabyProtectionAggroTarget() {
        this.babyProtectionAggroTargetUuid = null;
    }

    private boolean isBabyProtectionAggroTarget(@Nullable LivingEntity target) {
        return target != null
                && this.babyProtectionAggroTargetUuid != null
                && this.babyProtectionAggroTargetUuid.equals(target.getUUID());
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        return riderController.getDismountLocationForPassenger(passenger);
    }

    @Override
    public AgeableMob getBreedOffspring(@Nonnull ServerLevel level, @Nonnull AgeableMob other) {
        return createBreedOffspring(level, other, ModEntities.VARASUCHUS.get(), baby -> {
            baby.applyConfiguredAttributes();
            if (shouldUseVoidKissedVariant(level)) {
                baby.setPendingAdultTextureVariantId(VOID_KISSED_VARIANT_ID);
            }
        });
    }

    @Override
    public void ageBoundaryReached() {
        super.ageBoundaryReached();
        applyConfiguredAttributes();
        refreshDimensions();
    }

    @Override
    public boolean canBreed() {
        return this.isTame() && !this.isBaby() && this.getHealth() >= this.getMaxHealth() && this.isInLove();
    }

    @Override
    protected Supplier<? extends Block> getEggBlock() {
        return ModBlocks.VARASUCHUS_EGG;
    }

    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        boolean useHornGore = getMeleeMode() == 1;
        if (isPhaseTwoActive()) {
            return useHornGore ? ModAbilities.VARASUCHUS_HORN_GORE : ModAbilities.VARASUCHUS_BITE2;
        }
        return useHornGore ? ModAbilities.VARASUCHUS_HORN_GORE : ModAbilities.VARASUCHUS_BITE;
    }

    public static boolean canSpawnHere(EntityType<? extends Varasuchus> type,
                                       LevelAccessor level,
                                       MobSpawnType spawnType,
                                       BlockPos pos,
                                       RandomSource random) {
        boolean mobRules = Mob.checkMobSpawnRules(type, level, spawnType, pos, random);
        return mobRules
                && DragonSpawnRules.hasDryGroundSpawnSpace(level, pos)
                && DragonSpawnRules.passesNearbyDragonDensityCheck(level, spawnType, pos, Varasuchus.class);
    }

    private void enterSwimState() {
        swimming = true;
        this.getNavigation().stop();
        this.swimSteering.resetFromMob();
        this.entityData.set(DATA_SWIMMING, true);
    }

    private void exitSwimState() {
        swimming = false;
        this.asyncSwimController.clear();
        this.getNavigation().stop();
        this.navigation = this.groundNavigation;
        this.moveControl = this.landMoveControl;
        this.lookControl = this.landLookControl;
        Vec3 delta = this.getDeltaMovement();
        this.setDeltaMovement(new Vec3(delta.x, 0.0D, delta.z));
        this.entityData.set(DATA_SWIMMING, false);
        this.entityData.set(DATA_SWIM_TURN, 0);
        this.entityData.set(DATA_SWIM_PITCH, 0);
        this.entityData.set(DATA_SWIM_PITCH_RAD, 0.0F);
        this.swimTurnSmoothedYaw = 0.0F;
        this.swimTurnState = 0;
        this.swimPitchStateTicks = 0;
        this.swimRollAngle = 0f;
        this.prevSwimRollAngle = 0f;
        this.swimPitchRad = 0f;
        this.prevSwimPitchRad = 0f;
        this.smoothedPlayerSwimPitchRad = 0f;
        this.clientSwimPitchRad = 0f;
        this.prevClientSwimPitchRad = 0f;
    }

    private void updateSwimOrientationState() {
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
        prevSwimRollAngle = swimRollAngle;
        if (!isSwimming() || areRiderControlsLocked()) {
            swimRollAngle = 0f;
            prevSwimRollAngle = 0f;
            swimTurnSmoothedYaw = 0f;
            swimPitchRad = 0f;
            prevSwimPitchRad = 0f;
            smoothedPlayerSwimPitchRad = 0f;
            clientSwimPitchRad = 0f;
            prevClientSwimPitchRad = 0f;
            if (!level().isClientSide) {
                this.entityData.set(DATA_SWIM_PITCH_RAD, 0.0F);
            }
            return;
        }
        float yawDelta = Mth.wrapDegrees(this.getYRot() - this.yRotO);
        swimTurnSmoothedYaw = swimTurnSmoothedYaw * 0.6F + yawDelta * 0.4F;
        float targetAngle = Mth.clamp(swimTurnSmoothedYaw * 40.0f, -60f, 60f);
        swimRollAngle = Mth.lerp(0.40f, swimRollAngle, targetAngle);
        if (Math.abs(swimRollAngle) < 0.01f) {
            swimRollAngle = 0f;
        }

        if (!level().isClientSide && this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            float riderForward = this.entityData.get(DATA_RIDER_FORWARD);
            float riderStrafe = this.entityData.get(DATA_RIDER_STRAFE);
            boolean hasMovementInput = Math.abs(riderForward) > 0.01f || Math.abs(riderStrafe) > 0.01f;
            prevSwimPitchRad = swimPitchRad;
            float targetPitchRad = getRiderSwimVisualPitchRadians(player, hasMovementInput);

            swimPitchRad = Mth.lerp(0.35f, swimPitchRad, targetPitchRad);
            this.entityData.set(DATA_SWIM_PITCH_RAD, swimPitchRad);
        }
        if (!this.isVehicle()) {
            prevSwimPitchRad = swimPitchRad;

            Vec3 velocity = this.getDeltaMovement();
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

            float targetPitchRad = 0f;
            if (horizontalSpeed > 0.05) {
                targetPitchRad = (float) Math.atan2(velocity.y, horizontalSpeed);
                targetPitchRad = Mth.clamp(targetPitchRad, -Mth.HALF_PI, Mth.HALF_PI);
            }
            swimPitchRad = Mth.lerp(0.35f, swimPitchRad, targetPitchRad);
            if (!level().isClientSide) {
                this.entityData.set(DATA_SWIM_PITCH_RAD, swimPitchRad);
            }
        }
    }

    public boolean isSwimming() {
        if (level().isClientSide) {
            return this.entityData.get(DATA_SWIMMING);
        }
        return swimming;
    }

    public boolean isSwimmingMoving() {
        if (!isSwimming()) {
            return false;
        }

        if (this.getNavigation().isInProgress() && this.getNavigation().getPath() != null) {
            return true;
        }

        if (this.asyncSwimController.isMoving()) {
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
    public float getSwimRollAngleDegrees(float partialTick) {
        return Mth.lerp(partialTick, prevSwimRollAngle, swimRollAngle);
    }

    public float getSwimPitchRadians(float partialTick) {
        if (level().isClientSide) {
            return Mth.lerp(partialTick, prevClientSwimPitchRad, clientSwimPitchRad);
        }
        return Mth.lerp(partialTick, prevSwimPitchRad, swimPitchRad);
    }

    public boolean isRiderPitchKeyMode() {
        return this.entityData.get(DATA_PITCH_KEY_MODE);
    }

    public void setRiderPitchKeyMode(boolean enabled) {
        this.entityData.set(DATA_PITCH_KEY_MODE, enabled);
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

    public boolean shouldUseLeftClaw() {
        return useLeftClawNext;
    }

    public void toggleClawSide() {
        useLeftClawNext = !useLeftClawNext;
    }

    public boolean shouldUseLeftTailAttack() {
        return useLeftTailAttackNext;
    }

    public void toggleTailAttackSide() {
        useLeftTailAttackNext = !useLeftTailAttackNext;
    }


    @Override
    public RiderAbilityBinding getAttackRiderAbility() {
        if (getMeleeMode() == 1) {
            return new RiderAbilityBinding(ModAbilities.VARASUCHUS_HORN_GORE.getName(), RiderAbilityBinding.Activation.PRESS);
        } else {
            if (isPhaseTwoActive()) {
                return new RiderAbilityBinding(ModAbilities.VARASUCHUS_BITE2.getName(), RiderAbilityBinding.Activation.PRESS);
            }
            return new RiderAbilityBinding(ModAbilities.VARASUCHUS_BITE.getName(), RiderAbilityBinding.Activation.PRESS);
        }
    }

    @Override
    public RiderAbilityBinding getPrimaryRiderAbility() {
        if (isPhaseTwoActive()) {
            return new RiderAbilityBinding(ModAbilities.VARASUCHUS_SLASH_BARRAGE.getName(), RiderAbilityBinding.Activation.PRESS);
        }
        return new RiderAbilityBinding(ModAbilities.VARASUCHUS_TAILGUARD.getName(), RiderAbilityBinding.Activation.HOLD);
    }

    @Override
    protected boolean tryReleaseHeldRidingAbility(String abilityName) {
        if (ModAbilities.VARASUCHUS_TAILGUARD.getName().equals(abilityName)) {
            var active = combatManager.getActiveAbility();
            if (active != null && active.getAbilityType() == ModAbilities.VARASUCHUS_TAILGUARD) {
                ((VarasuchusTailguardAbility) active).requestRelease();
                return true;
            }
        }
        return false;
    }

    @Override
    public RiderAbilityBinding getSecondaryRiderAbility() {
        return new RiderAbilityBinding(ModAbilities.VARASUCHUS_PHASE_SHIFT.getName(), RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getTertiaryRiderAbility() {
        if (isPhaseTwoActive()) {
            return new RiderAbilityBinding(ModAbilities.VARASUCHUS_CLAW.getName(), RiderAbilityBinding.Activation.PRESS);
        }
        return new RiderAbilityBinding(ModAbilities.VARASUCHUS_TAIL_ATTACK.getName(), RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public DragonAbilityType<?, ?> getChannelingAbility() {
        return ModAbilities.VARASUCHUS_PHASE_SHIFT;
    }

    private void handleRiddenSwimming(Vec3 input) {
        Player player = (Player) getControllingPassenger();
        if (player == null) return;
        Vec3 velocity = this.getDeltaMovement();
        double swimSpeed = getSwimSpeed();
        if (isAccelerating()) {
            swimSpeed *= 1.6D;
        }

        double forwardInput = input.z;
        double strafeInput = input.x;
        boolean hasInput = Math.abs(forwardInput) > 0.01 || Math.abs(strafeInput) > 0.01;
        boolean hasRiderInput = Math.abs(player.zza) > 0.01f || Math.abs(player.xxa) > 0.01f;
        float targetPitchRad = getRiderSwimVisualPitchRadians(player, hasRiderInput);
        prevSwimPitchRad = swimPitchRad;
        swimPitchRad = Mth.lerp(0.35f, swimPitchRad, targetPitchRad);

        this.entityData.set(DATA_SWIM_PITCH_RAD, swimPitchRad);
        float yawRad = (float) Math.toRadians(this.getYRot());
        float pitchRad = DragonRiderControllerHelper.resolveRiderPitchRadians(this, player, RIDER_KEY_PITCH_DEG);
        double forwardXZ = Math.cos(pitchRad);
        double forwardX = -Math.sin(yawRad) * forwardXZ;
        double forwardY = -Math.sin(pitchRad);
        double forwardZ = Math.cos(yawRad) * forwardXZ;
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);
        double targetDirX = forwardX * forwardInput + rightX * strafeInput * 0.5;
        double targetDirY = forwardY * forwardInput * 1.2;
        double targetDirZ = forwardZ * forwardInput + rightZ * strafeInput * 0.5;
        double dirLength = Math.sqrt(targetDirX * targetDirX + targetDirY * targetDirY + targetDirZ * targetDirZ);

        Vec3 desired;
        if (hasInput && dirLength > 0.01) {
            targetDirX /= dirLength;
            targetDirY /= dirLength;
            targetDirZ /= dirLength;

            desired = new Vec3(
                targetDirX * swimSpeed,
                targetDirY * swimSpeed,
                targetDirZ * swimSpeed
            );
        } else {
            desired = new Vec3(0, velocity.y * 0.9, 0);
        }
        Vec3 blended = velocity.add(desired.subtract(velocity).scale(0.28D));
        double dragFactor = this.isControlledByLocalInstance() ? 0.92D : 0.94D;
        blended = blended.multiply(dragFactor, 0.96D, dragFactor);
        double verticalVel = blended.y;
        if (isGoingUp()) {
            verticalVel = Math.min(swimSpeed * 0.8, verticalVel + 0.12D * swimSpeed);
        } else if (isGoingDown()) {
            verticalVel = Math.max(-swimSpeed * 0.8, verticalVel - 0.12D * swimSpeed);
        }

        blended = new Vec3(blended.x, verticalVel, blended.z);

        this.setDeltaMovement(blended);
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    private float getRiderSwimVisualPitchRadians(Player player, boolean hasMovementInput) {
        float rawPitchRad = DragonRiderControllerHelper.resolveRiderSwimVisualPitchRadians(
                this,
                player,
                RIDER_KEY_PITCH_DEG,
                hasMovementInput
        );
        smoothedPlayerSwimPitchRad = rawPitchRad == 0.0F
                ? 0.0F
                : smoothedPlayerSwimPitchRad * 0.65f + rawPitchRad * 0.35f;
        return Mth.clamp(smoothedPlayerSwimPitchRad, -Mth.HALF_PI, Mth.HALF_PI);
    }

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

    public static class VarasuchusLookController extends LookControl {
        private final Varasuchus dragon;

        public VarasuchusLookController(Varasuchus dragon) {
            super(dragon);
            this.dragon = dragon;
        }

        @Override
        public void tick() {
            if (!this.dragon.isAlive()) {
                return;
            }
            if (dragon.isSleeping() || dragon.isSleepTransitioning()) {
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

                    boolean allowRiderPitch = !this.dragon.areRiderControlsLocked();
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
        if (!this.level().isClientSide && this.isVehicle() && this.isOrderedToSit()) {
            this.setOrderedToSit(false);
        }
    }

    private void startWildRideSequence() {
        wildRideActive = true;
        this.entityData.set(DATA_WILD_RIDE_ACTIVE, true);
        wildRideTicks = 0;
        nextBuckAttemptTick = WILD_RIDE_BUCK_DURATION_TICKS;
        this.getNavigation().stop();
        this.setTarget(null);
        this.setAccelerating(false);
        this.setGroundMoveStateFromAI(1);
        if (!this.level().isClientSide) {
            this.getSoundHandler().playMovingEntitySound(
                    ModSounds.VARASUCHUS_BUCKING.get(),
                    1.0f,
                    1.0f,
                    WILD_RIDE_BUCK_DURATION_TICKS
            );
        }
    }

    private void tickWildRideState() {
        if (!wildRideActive || this.isTame()) {
            wildRideActive = false;
            this.entityData.set(DATA_WILD_RIDE_ACTIVE, false);
            return;
        }
        Player rider = getWildRideRider();
        if (rider == null) {
            endWildRide(false);
            return;
        }
        wildRideTicks++;
        cumulativeWildRideProgress = Math.min(cumulativeWildRideProgress + 1, MAX_TAMING_PROGRESS);
        applyWildRideWalk();
        if (wildRideTicks >= nextBuckAttemptTick) {
            buckWildRider(rider);
            triggerFailedTamingAggro(rider);
            endWildRide(true);
            return;
        }
        if (wildRideTicks >= MIN_WILD_TAME_TICKS) {
            float progressFactor = (float) cumulativeWildRideProgress / (float) MAX_TAMING_PROGRESS;
            DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.VARASUCHUS_ID);
            double tamingChanceConfig = config.extraDoubles().getOrDefault("taming_chance", 16.6667D);
            float baseChance = (float) (DragonTamingChance.probabilityFromPercent(tamingChanceConfig) / 100.0D);
            float successChance = baseChance * (1.0F + (progressFactor * progressFactor * 4.0F));

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

    private void tickWildRideBuckCooldown() {
        if (wildRideBuckCooldownTicks > 0) {
            wildRideBuckCooldownTicks--;
        }
    }

    private void applyWildRideWalk() {
        this.setAccelerating(false);
        this.setGroundMoveStateFromAI(1);

        if (this.getNavigation().isDone()) {
            double targetX = this.getX() + (this.getRandom().nextDouble() - 0.5D) * 10.0D;
            double targetZ = this.getZ() + (this.getRandom().nextDouble() - 0.5D) * 10.0D;
            double targetY = this.getY();
            this.getNavigation().moveTo(targetX, targetY, targetZ, WILD_RIDE_WALK_SPEED);
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
        Vec3 forward = this.getLookAngle().normalize();
        Vec3 backward = new Vec3(-forward.x, 0.0D, -forward.z);
        if (backward.lengthSqr() < 1.0E-4D) {
            backward = new Vec3(0.0D, 0.0D, -1.0D);
        } else {
            backward = backward.normalize();
        }

        Vec3 right = new Vec3(-backward.z, 0.0D, backward.x);
        double sideSign = this.getRandom().nextBoolean() ? 1.0D : -1.0D;
        double backwardStrength = 1.0D + this.getRandom().nextDouble() * 0.8D;
        double sideStrength = 0.55D + this.getRandom().nextDouble() * 0.55D;
        double upward = 1.0D + this.getRandom().nextDouble() * 0.6D;
        Vec3 launch = backward.scale(backwardStrength).add(right.scale(sideSign * sideStrength)).add(0.0D, upward, 0.0D);
        Vec3 releaseOffset = backward.scale(1.6D).add(right.scale(sideSign * 0.8D)).add(0.0D, 1.35D, 0.0D);
        Vec3 releasePos = this.position().add(releaseOffset);
        rider.stopRiding();
        rider.moveTo(releasePos.x, releasePos.y, releasePos.z, rider.getYRot(), rider.getXRot());
        Vec3 currentVel = rider.getDeltaMovement();
        rider.setDeltaMovement(currentVel.add(launch));
        rider.hurtMarked = true;
        rider.hasImpulse = true;
        rider.fallDistance = 0.0F;
        wildRideBuckCooldownTicks = WILD_RIDE_BUCK_COOLDOWN_TICKS;
        this.level().broadcastEntityEvent(this, (byte) 6);
    }

    private void endWildRide(boolean bucked) {
        if (bucked) {
            cumulativeWildRideProgress = Math.max(0, cumulativeWildRideProgress - 40);
        } else {
            cumulativeWildRideProgress = Math.max(0, cumulativeWildRideProgress - 10);
        }
        wildRideActive = false;
        this.entityData.set(DATA_WILD_RIDE_ACTIVE, false);
        wildRideTicks = 0;
        nextBuckAttemptTick = 0;
    }

    private boolean isWildRideActive() {
        return wildRideActive && !this.isTame();
    }

    public boolean isWildRideAnimationActive() {
        return this.entityData.get(DATA_WILD_RIDE_ACTIVE) && this.isVehicle() && !this.isTame();
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

    private void tickClientSideUpdates() {
        if (level().isClientSide) {
            prevClientSwimPitchRad = clientSwimPitchRad;
            float targetPitch = this.entityData.get(DATA_SWIM_PITCH_RAD);
            clientSwimPitchRad = Mth.lerp(0.5f, clientSwimPitchRad, targetPitch);
        }
    }


    private void tickFeedingCooldown() {
        int cooldownTicks = this.entityData.get(DATA_FEEDING_COOLDOWN);
        if (cooldownTicks > 0) {
            cooldownTicks--;
            this.entityData.set(DATA_FEEDING_COOLDOWN, cooldownTicks);
        }
    }

    private void tickPhaseTwoLinger() {
        if (this.isVehicle()) {
            phaseTwoLingerTicks = 0;
            return;
        }

        if (!this.isPhaseTwoActive()) {
            phaseTwoLingerTicks = 0;
            return;
        }

        if (this.isAbilityActive(ModAbilities.VARASUCHUS_PHASE_SHIFT)) {
            return;
        }

        LivingEntity target = this.getTarget();
        boolean targetTooFar = target != null && isTargetTooFar(target);

        if (target == null || targetTooFar) {
            if (phaseTwoLingerTicks <= 0) {
                phaseTwoLingerTicks = PHASE_TWO_LINGER_TICKS;
            } else {
                phaseTwoLingerTicks--;
            }

            if (phaseTwoLingerTicks <= 0) {
                this.combatManager.tryUseAbility(ModAbilities.VARASUCHUS_PHASE_SHIFT);
            }
        } else {
            phaseTwoLingerTicks = 0;
        }
    }

    private boolean isTargetTooFar(LivingEntity target) {
        double followRange = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 16.0D;
        }
        if (this.isInWaterOrBubble() && target.isInWaterOrBubble()) {
            followRange *= 2.0D;
        }
        double maxDistanceSq = followRange * followRange;
        return this.distanceToSqr(target) > maxDistanceSq;
    }

    @Override
    public float maxSitTicks() {
        return getSitDownAnimationTicks();
    }

    private int getSitDownAnimationTicks() {
        return 22;
    }

    private int getSitUpAnimationTicks() {
        return 22;
    }

    private int getFallAsleepAnimationTicks() {
        return 42;
    }

    private int getWakeUpAnimationTicks() {
        return 129;
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
    protected int getSleepLoopLeadTicks() {
        return 3;
    }

    @Override
    protected void onSleepLockCommand(int snapshot) {
        if (level().isClientSide) {
            return;
        }
        this.setOrderedToSit(true);
        this.getNavigation().stop();
        this.setTarget(null);
        this.setDeltaMovement(Vec3.ZERO);
        this.setRunning(false);
        setGroundMoveStateFromAI(0);
        if (this.getCommand() != 1) {
            this.setCommand(1);
        }
    }

    @Override
    protected void onSleepUnlockCommand(int desired) {
        if (level().isClientSide) {
            return;
        }
        if (desired >= 0 && desired != this.getCommand()) {
            this.setCommand(desired);
            this.setOrderedToSit(desired == 1);
        }
    }

    @Override
    protected void onSleepFreezeTick() {
        super.onSleepFreezeTick();
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
        setSitProgress(Math.max(getSitProgress(), maxSitTicks()));
    }

    @Override
    protected void onSleepExitStarted() {
    }

    @Override
    protected void onSleepWakeUpImmediate() {
    }

    @Override
    public DragonEntity.DragonSleepPreferences getSleepPreferences() {
        return DragonEntity.DragonSleepPreferences.NOCTURNAL();
    }

    @Override
    public boolean canSleepNow() {
        return !isVehicle()
                && !this.isInWaterOrBubble()
                && getActiveAbility() == null
                && !isPhaseTwoActive()
                && canSleepAtRoost();
    }

    @Override
    public boolean wantsToReturnToSleepSite() {
        return roostComponent.wantsToReturnToSleepSite();
    }

    private boolean canSleepAtRoost() {
        return roostComponent.canSleepAtRoost();
    }

    public int getRoostSleepSettleTicks() {
        return roostComponent.getSleepSettleTicks();
    }

    public boolean hasRoostTerritory() {
        return roostComponent.hasTerritory();
    }

    public boolean isWithinRoostTerritory(Vec3 position) {
        return roostComponent.isWithinTerritory(position);
    }

    public boolean isInsideRoostStructure(Vec3 position) {
        return roostComponent.isInsideStructure(position);
    }

    public boolean isOutsideRoostTerritory() {
        return roostComponent.isOutsideTerritory();
    }

    public boolean shouldSuspendRoostWandering() {
        return roostComponent.shouldSuspendWandering();
    }

    private void tickScreenShake() {
        screenShakeComponent.tick();
    }

    @Override
    public boolean hurt(@Nonnull DamageSource damageSource, float amount) {
        if (super.isDying()) {
            return false;
        }
        rememberIncomingProjectile(damageSource);
        DragonAbility<?> activeAbility = getActiveAbility();
        if (activeAbility instanceof VarasuchusTailguardAbility tailguard && tailguard.tryParry(damageSource)) {
            return false;
        }
        if (isSleeping() || isSleepingEntering() || isSleepTransitioning()) {
            startSleepExit();
            suppressSleep(200);
        }
        if (damageSource.is(DamageTypeTags.IS_DROWNING)) {
            amount *= 0.5F;
        }
        return super.hurt(damageSource, amount);
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
    protected void dropAdditionalDeathLootAfterBase(@NotNull DamageSource source) {
        if (!level().isClientSide && getGender() == DragonGender.FEMALE) {
            DragonLootTables.dropEntityLoot(this, DragonLootTables.VARASUCHUS_FEMALE_DEATH, source);
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        saveRideableData(tag);
        tag.putBoolean("PhaseTwo", isPhaseTwoActive());
        tag.putInt("FeedingCooldownTicks", Math.max(0, this.entityData.get(DATA_FEEDING_COOLDOWN)));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!level().isClientSide) {
            roostComponent.restoreMemories();
        }
        loadRideableData(tag);
        if (tag.contains("FeedingCooldownTicks")) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, tag.getInt("FeedingCooldownTicks")));
        }
        if (tag.contains("PhaseTwo")) {
            setPhaseTwoActive(tag.getBoolean("PhaseTwo"), false);
        }
        applyConfiguredAttributes();
    }

    @Override
    protected ScreenShakeComponent getScreenShakeComponent() {
        return screenShakeComponent;
    }

    @Override
    public double getShakeDistance() {
        return 18.0;
    }

    public void startPhaseShiftScreenShake(int durationTicks, float intensity) {
        triggerScreenShake(intensity, durationTicks);
    }

    public void stopPhaseShiftScreenShake() {
        clearScreenShake();
    }

    public boolean canBeBound() {
        return !isDying()
                && !isAccelerating()
                && !areRiderControlsLocked()
                && !isAbilityActive(ModAbilities.VARASUCHUS_PHASE_SHIFT);
    }


    @Override
    public boolean shouldEnterWater() {

        if (this.isOrderedToSit() || this.isVehicle()) {
            return false;
        }
        if (this.isOnFire()) {
            return true;
        }
        if (this.getHealth() < this.getMaxHealth() * 0.5F && this.getTarget() != null) {
            return true;
        }
        return ticksOutOfWater > 1000 && this.getRandom().nextFloat() < 0.08F;
    }

    @Override
    public boolean shouldLeaveWater() {
        if (this.isOrderedToSit()) {
            return false;
        }
        if (this.isTame() && this.getOwner() != null) {
            LivingEntity owner = this.getOwner();
            if (!owner.isInWater() && this.distanceToSqr(owner) > 100.0D) {
                return true;
            }
        }
        return ticksInWater > 1200 && this.getRandom().nextFloat() < 0.08F;
    }
}
