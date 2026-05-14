package com.leon.saintsdragons.server.entity.dragons.cindervane;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModBlocks;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.goals.base.*;
import com.leon.saintsdragons.server.ai.goals.cindervane.*;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.base.DragonSitTransitionController;
import com.leon.saintsdragons.server.entity.base.DragonVariant;
import com.leon.saintsdragons.server.entity.base.DragonVariantSet;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.controller.cindervane.CindervaneRiderController;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import com.leon.saintsdragons.server.flight.DragonFlightVisuals;
import com.leon.saintsdragons.server.flight.DragonRiderFlight;
import com.leon.saintsdragons.server.entity.dragons.cindervane.handlers.CindervaneAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.cindervane.handlers.CindervaneInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.cindervane.handlers.CindervaneSoundProfile;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonInteractionAnimationHelper;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonStateAnimationHelper;
import com.leon.saintsdragons.server.entity.dragons.util.DragonGriefingRules;
import com.leon.saintsdragons.server.entity.component.ScreenShakeComponent;
import java.util.Map;
import java.util.HashMap;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.interfaces.PackMember;
import com.leon.saintsdragons.server.entity.interfaces.ShakesScreen;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.server.world.DragonSpawnRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Block;
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
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;
import javax.annotation.Nonnull;

public class Cindervane extends RideableFlyingDragon implements ShakesScreen, PackMember<Cindervane> {
    @Override
    protected ResourceLocation getDragonAttributesId() {
        return DragonAttributeConfigLoader.CINDERVANE_ID;
    }

    public static final int VARIANT_DEFAULT = 0;
    public static final int VARIANT_ALBINO = 1;
    private static final double BABY_MAX_HEALTH = 40.0D;
    private static final double BABY_ARMOR = 0.0D;
    private static final DragonVariantSet VARIANTS = DragonVariantSet.of(
            DragonVariant.of(VARIANT_DEFAULT, "default", 85),
            DragonVariant.of(VARIANT_ALBINO, "albino", 15)
    );

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
    private static final int LANDING_SETTLE_TICKS = 4;
    public static final int TAKEOFF_ANIMATION_TICKS = 25;
    private static final double FIRE_BODY_CRASH_MIN_DROP = 7.0D;
    private static final float FIRE_BODY_EXPLOSION_RADIUS = 15.0F;
    private static final double FIRE_BODY_IMPRINT_RADIUS = 9.0D;
    private static final int RIDER_LANDING_BLEND_DURATION = 3;
    private static final double FIRE_BODY_IMPRINT_DEPTH_FACTOR = 0.6D;
    private static final float FIRE_BODY_EXPLOSION_DAMAGE = 200.0F;
    private static final float FIRE_BODY_SELF_DAMAGE_ON_CRASH = 40.0F;
    private static final double BREED_PARTNER_RANGE = 20.0D;
    private static final double BREED_DISTANCE_SQR = 2500.0D;
    private static final int MAX_PACK_SIZE = 6;
    private static final double PACK_SEARCH_RADIUS = 48.0D;
    private static final int MIN_AMBIENT_DELAY = 180;
    private static final int MAX_AMBIENT_DELAY = 420;
    public static final double RIDER_WALK_SPEED = 0.18D;
    public static final double RIDER_RUN_SPEED = 0.26D;
    public static final float RIDER_KEY_PITCH_DEG = 25.0f;
    private static final Map<String, VocalEntry> VOCAL_ENTRIES =
            new VocalEntryBuilder()
                    .add("grumble1", com.leon.saintsdragons.server.entity.dragons.handlers.DragonVocalAnimationHelper.CONTROLLER, "animation.cindervane.grumble1", ModSounds.CINDERVANE_GRUMBLE_1, 1.1f, 0.98f, 0.06f, false, false, false)
                    .add("grumble2", com.leon.saintsdragons.server.entity.dragons.handlers.DragonVocalAnimationHelper.CONTROLLER, "animation.cindervane.grumble2", ModSounds.CINDERVANE_GRUMBLE_2, 1.2f, 0.96f, 0.08f, false, false, false)
                    .add("grumble3", com.leon.saintsdragons.server.entity.dragons.handlers.DragonVocalAnimationHelper.CONTROLLER, "animation.cindervane.grumble3", ModSounds.CINDERVANE_GRUMBLE_3, 1.0f, 1.0f, 0.05f, false, false, false)
                    .add("roar", CindervaneAnimationHandler.ACTION_CONTROLLER, "animation.cindervane.roar", ModSounds.CINDERVANE_ROAR, 1.5f, 0.95f, 0.1f, false, false, false)
                    .add("cindervane_hurt", DragonInteractionAnimationHelper.CONTROLLER, "animation.cindervane.hurt", ModSounds.CINDERVANE_HURT, 1.2f, 0.95f, 0.1f, false, false, false)
                    .add("cindervane_die", DragonInteractionAnimationHelper.CONTROLLER, "animation.cindervane.die", ModSounds.CINDERVANE_DIE, 1.5f, 1.0f, 0.0f, false, false, false)
                    .build();

    private int groundStepSoundCooldownTicks = 0;
    public AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);
    private final CindervaneAnimationHandler animationHandler = new CindervaneAnimationHandler(this);
    private final CindervaneInteractionHandler interactionHandler = new CindervaneInteractionHandler(this);
    private final CindervaneRiderController riderController;
    private int targetCooldown;
    private int airTicks;
    public int groundTicks;
    public int timeFlying = 0;
    private int landingTicks;
    private boolean fireBodyCrashArmed;
    private double fireBodyCrashMaxHeight;
    private boolean autoGrabPassengerMountAllowed;
    @Nullable
    private UUID slashGrabPassengerUuid;
    @Nullable
    private UUID packLeaderUuid;
    private final DragonFlightVisuals.State flightVisualState = new DragonFlightVisuals.State();
    private final ScreenShakeComponent screenShakeComponent;
    private final DragonSitTransitionController sitTransitions = new DragonSitTransitionController(this);
    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case ABILITY_USE, ABILITY_STOP -> true;
            default -> super.supportsRiderAction(action);
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return dragonCache;
    }


    @Override
    public float maxSitTicks() {
        return getSitDownAnimationTicks();
    }

    private int getSitDownAnimationTicks() {
        return 33;
    }

    private int getSitUpAnimationTicks() {
        return 17;
    }

    private int getFallAsleepAnimationTicks() {
        return 33;
    }

    private int getWakeUpAnimationTicks() {
        return 33;
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

    public boolean isAlbinoVariant() {
        return getTextureVariant() == VARIANT_ALBINO;
    }

    private final Map<String, Vec3> serverBonePositionCache = new java.util.concurrent.ConcurrentHashMap<>();

    public Cindervane(EntityType<? extends Cindervane> type, Level level) {
        super(type, level);
        this.screenShakeComponent = new ScreenShakeComponent(this, DATA_SCREEN_SHAKE_AMOUNT, 0.12F);
        this.setMaxUpStep(1.1F);
        this.riderController = new CindervaneRiderController(this);
        this.setPathfindingMalus(BlockPathTypes.LEAVES, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
        seedAmbientSoundTimer(MIN_AMBIENT_DELAY, MAX_AMBIENT_DELAY, 80);

        if (!level.isClientSide) {
            applyConfiguredAttributes();
        }
    }

    @Override
    protected DragonRiderFlight.Config getRiderFlightConfig() {
        return new DragonRiderFlight.Config(
                true,
                0,
                0.55D,
                TAKEOFF_ANIMATION_TICKS,
                0.45D,
                TAKEOFF_ANIMATION_TICKS
        );
    }

    @Override
    protected void onTakeoffStarted() {
        this.timeFlying = 0;
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
                    scheduleFamilyBabies(1);
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
        DragonAttributeConfig config = getConfiguredDragonAttributes();
        applyConfiguredFlyingHealthAndArmor(config, BABY_MAX_HEALTH, BABY_ARMOR);
        clampHealthToMax();
    }

    public static AttributeSupplier.Builder createAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.CINDERVANE_ID);
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, config.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.FLYING_SPEED, config.flyingSpeed())
                .add(Attributes.ARMOR, config.armor());
    }

    @Override
    public void ageBoundaryReached() {
        super.ageBoundaryReached();
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
        return DragonSpawnRules.hasDryGroundSpawnSpace(level, pos)
                && DragonSpawnRules.passesNearbyDragonDensityCheck(level, spawnType, pos, Cindervane.class);
    }

    private static final EntityDataAccessor<Integer> DATA_FEEDING_COOLDOWN =
            SynchedEntityData.defineId(Cindervane.class, EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
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
        this.goalSelector.addGoal(0, new DragonFloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        if (!this.isBaby()) {
            this.goalSelector.addGoal(3, new CindervaneFlightGoal(this));
            this.goalSelector.addGoal(4, new CindervaneAirCombatGoal(this));
            this.goalSelector.addGoal(5, new CindervaneCombatGoal(this));
        }
        this.goalSelector.addGoal(5, new DragonFollowParentGoal<>(this, Cindervane.class, 1.15D));
        this.goalSelector.addGoal(6, new DragonFollowOwnerGoal<>(this, DragonFollowOwnerGoal.FollowConfig.forCindervane()) {
            @Override
            protected void startFollowTakeoff() {
                if (Cindervane.this.isFlying() || Cindervane.this.isTakeoff()) {
                    return;
                }
                Cindervane.this.startTakeoffSequence(0.12D, TAKEOFF_ANIMATION_TICKS);
            }

        });
        if (!this.isBaby()) {
            this.goalSelector.addGoal(7, new DragonBreedGoal<>(this, 1.0D, Cindervane.class, BREED_PARTNER_RANGE, BREED_DISTANCE_SQR));
        }
        this.goalSelector.addGoal(8, new DragonPackFollowLeaderGoal<>(this, Cindervane.class, 1.0D, 20.0D, 10.0D));
        this.goalSelector.addGoal(9, new DragonGroundWanderGoal<>(this, 0.6D, 160));
        this.goalSelector.addGoal(10, new DirectSwimWanderGoal(this, 8.0F, 0.12D, 1, true));
        this.targetSelector.addGoal(1, new DragonOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new DragonOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new DragonProtectBabiesGoal<>(this, Cindervane.class));
        this.targetSelector.addGoal(4, new DragonPackDefendPackGoal<>(this, Cindervane.class, 36.0D));
        this.targetSelector.addGoal(5, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, target -> shouldAggroOnSight()));
        this.targetSelector.addGoal(7, new DragonRandomHuntTargetGoal(this, 80, () -> true,
                target -> DragonTargetingHelper.isTaggedHuntTarget(target, ModTags.EntityTypes.CINDERVANE_TARGETS)));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this) {@Override public boolean canUse() {return !Cindervane.this.isVehicle() && super.canUse();}});
        this.goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 8.0F) {@Override public boolean canUse() {
                return !Cindervane.this.isVehicle() && super.canUse();
            }
        });
    }

    private void tickFlightLifecycle() {
        if (!this.level().isClientSide) {
            if (!this.isOrderedToSit() && getSitProgress() != 0f) {
                clearSitProgress();
            }
            boolean onGroundNow = this.onGround() && !this.isInWater();
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

            tickAnimationStates();
        }
        this.setNoGravity(isFlying() || isTakeoff() || isHovering() || isLanding());
        if (!isFlying() && !isTakeoff() && !isLanding() && isUsingAirNavigation()) {
            switchToGroundNavigation();
        }
    }

    public void tick() {
        super.tick();
        tickRiderControlLock();
        tickBankingLogic();
        tickBarrelRollLogic();
        tickStandardPitchingLogic();
        tickScreenShake();
        tickFlightLifecycle();
        if (level().isClientSide) {
            return;
        }
        tickStandardTakeoffAndGroundedAerialRecovery();
        tickSittingState();
        tickRiderTakeoff();
        updateSittingProgress();
        spawnPendingFamilyBabies(ModEntities.CINDERVANE.get(), Cindervane::applyConfiguredAttributes);
        if (isBreathingFire() || fireBodyCrashArmed) {
            handleFireBodyCrash();
        }

        if (isVehicle() && getSitProgress() != 0f) {
            clearSitProgress();
        }

        if (targetCooldown > 0) {
            targetCooldown--;
        }
        if (!level().isClientSide && isAerial()) {
            this.entityData.set(DATA_FLIGHT_MODE, getFlightMode());
        }
        tickAsyncFlightNavigation();
        if (isFlying()) {
            timeFlying++;
        } else {
            timeFlying = 0;
        }
        if (this.tickCount == 1) {
            initializeAnimationState();
        }

        tickFeedingCooldown();
        handleAmbientSounds();
        tickGroundStepAudio();

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

    private void tickSittingState() {
        if (!this.level().isClientSide && this.isVehicle()
                && (this.isOrderedToSit() || this.getCommand() == 1 || this.getSitProgress() != 0f || this.isInSittingPose())) {
            clearStateForMountedRider();
            clearLocalSitTransitionForMount();
        }
    }

    @Override
    protected void onMountedStateStarted() {
        super.onMountedStateStarted();
        clearStatesWhenMounted();
    }

    @Override
    protected void onMountedStateStopped() {
        this.setBreathingFire(false);
        combatManager.forceEndActiveAbility();
        this.entityData.set(DATA_FLIGHT_MODE, -1);
        super.onMountedStateStopped();
        this.syncAnimState(0, -1);
    }

    @Override
    protected void clearLocalSitTransitionForMount() {
        sitTransitions.clear();
        setInSittingPose(false);
    }

    private static class CindervaneFamilyData extends AgeableMob.AgeableMobGroupData {
        public CindervaneFamilyData(boolean shouldSpawnBaby) {
            super(shouldSpawnBaby);
        }
    }

    private void updateSittingProgress() {
        sitTransitions.tick(
                getSitDownAnimationTicks(),
                getSitUpAnimationTicks(),
                animationHandler::triggerSitDownAnimation,
                animationHandler::triggerSitUpAnimation
        );
    }

    private void tickClientSideUpdates() {
    }

    private void handleAmbientSounds() {
        if (isBaby() || isDying() || isSleeping() || isSleepTransitioning() || isInSitTransition() || getSleepAmbientCooldownTicks() > 0 || areRiderControlsLocked()) {
            return;
        }

        if (getActiveAbility() != null || isBreathingFire() || this.getTarget() != null) {
            return;
        }

        if (isVehicle() || isTakeoff() || isLanding()) {
            return;
        }

        tickAmbientVocalSounds(MIN_AMBIENT_DELAY, MAX_AMBIENT_DELAY, this::selectAmbientGrumble);
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

    private void tickGroundStepAudio() {
        if (groundStepSoundCooldownTicks > 0) {
            groundStepSoundCooldownTicks--;
        }
        if (isBaby() || isAerial() || isInWaterOrBubble() || !onGround()) {
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
        setRiderTakeoffTicks(0);

        if (!isFlying()) {
            airTicks = 0;
        }

        if (this.getNavigation().getPath() != null) {
            this.getNavigation().stop();
        }

        if (!isFlying() && isUsingAirNavigation()) {
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
                setFlying(false);
                setTakeoff(false);
                setLanding(false);
                setHovering(false);
                timeFlying = 0;
                switchToGroundNavigation();
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
                setFlying(false);
                setTakeoff(false);
                timeFlying = 0;
                triggerAnim(CindervaneAnimationHandler.ACTION_CONTROLLER, "landed");
                getSoundHandler().playMovingEntitySound(ModSounds.CINDERVANE_LANDED.get(), 1.0f, 1.0f, 59);
                lockRiderControls(34);
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
    protected boolean shouldResetStandardPitchInHover() {
        return true;
    }

    @Override
    protected boolean shouldResetStandardPitchForSit() {
        return isOrderedToSit();
    }

    private void tickScreenShake() {
        screenShakeComponent.tick();
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

    @Override
    public boolean isGoingUp() {
        return this.entityData.get(DATA_GOING_UP);
    }

    @Override
    public void setRunning(boolean running) {
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

    @Override
    public void initializeAnimationState() {
        super.initializeAnimationState();
        if (!level().isClientSide) {
            groundTicks = 0;
            airTicks = 0;
            landingTicks = 0;
        }
    }

    @Override
    public int getFlightMode() {
        return evaluateStandardFlightMode(false);
    }

    public DragonFlightStateEvaluator.VisualState getVisualFlightState(float partialTick) {
        return evaluateVisualFlightState(partialTick, getFlightPitchRadians(partialTick));
    }

    @Override
    protected boolean handleCustomRiderAction(ServerPlayer player, DragonRiderAction action,
                                              String abilityName, boolean locked) {
        if (locked) {
            return false;
        }

        return switch (action) {
            default -> false;
        };
    }

    @Override
    public RiderAbilityBinding getTertiaryRiderAbility() {
        return new RiderAbilityBinding(ModAbilities.CINDERVANE_FIRE_BODY.getName(), RiderAbilityBinding.Activation.HOLD);
    }

    @Override
    public RiderAbilityBinding getPrimaryRiderAbility() {
        return new RiderAbilityBinding(ModAbilities.CINDERVANE_ROAR.getName(), RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getSecondaryRiderAbility() {
        return new RiderAbilityBinding(ModAbilities.CINDERVANE_FIRE_BREATH_VOLLEY.getName(), RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getAttackRiderAbility() {
        String abilityId = getMeleeMode() == 0
                ? ModAbilities.CINDERVANE_BITE.getName()
                : ModAbilities.CINDERVANE_SLASH_GRAB.getName();
        return new RiderAbilityBinding(abilityId, RiderAbilityBinding.Activation.PRESS);
    }

    protected int getMaxPassengers() {
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
            riderController.tickRidden(player, travelVector);
        } else {
            if (combatManager.getActiveAbility() == null && !combatManager.hasActiveOverlay()) {
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
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@Nonnull Player player, @Nonnull Vec3 deltaIn) {
        Vec3 input = riderController.getRiddenInput(player, deltaIn);
        if (!level().isClientSide && !isFlying()) {
            float fwd = (float) Mth.clamp(input.z, -1.0, 1.0);
            float str = (float) Mth.clamp(input.x, -1.0, 1.0);
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
    }

    @Override
    public void travel(@NotNull Vec3 motion) {
        if (areRiderControlsLocked()) {
            super.travel(Vec3.ZERO);
            return;
        }

        boolean inWater = this.isInWater() || this.isInWaterOrBubble();

        if (inWater && !level().isClientSide) {
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
                this.setSpeed(riderController.getRiddenSpeed(player));
                super.travel(motion);
            }
            return;
        }
        super.travel(motion);
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
        if (fireActive && isFireBodyForwardCrashImpact()) {
            triggerFireBodyCrash(findFireBodyForwardCrashImpact());
            fireBodyCrashArmed = false;
            fireBodyCrashMaxHeight = 0.0D;
            return;
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

    private boolean isFireBodyForwardCrashImpact() {
        if (this.onGround() || !this.isAerial()) {
            return false;
        }
        return this.horizontalCollision || isFireBodyForwardCrashBlockedAhead();
    }

    private boolean isFireBodyForwardCrashBlockedAhead() {
        if (!(level() instanceof ServerLevel server)) {
            return false;
        }
        Vec3 direction = getFireBodyForwardCrashDirection();
        if (direction == null) {
            return false;
        }

        Vec3 origin = this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D);
        double reach = getFireBodyForwardCrashProbeDistance();
        BlockHitResult hit = server.clip(new ClipContext(
                origin,
                origin.add(direction.scale(reach)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return true;
        }

        AABB probe = this.getBoundingBox()
                .move(direction.scale(reach))
                .inflate(0.08D, 0.08D, 0.08D);
        return !server.noCollision(this, probe);
    }

    private Vec3 findFireBodyForwardCrashImpact() {
        if (!(level() instanceof ServerLevel server)) {
            return this.position();
        }
        Vec3 direction = getFireBodyForwardCrashDirection();
        if (direction == null) {
            return this.position();
        }

        Vec3 origin = this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D);
        double reach = getFireBodyForwardCrashProbeDistance();
        BlockHitResult hit = server.clip(new ClipContext(
                origin,
                origin.add(direction.scale(reach)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getLocation();
        }
        return this.position().add(direction.scale(this.getBbWidth() * 0.5D + 0.75D));
    }

    @Nullable
    private Vec3 getFireBodyForwardCrashDirection() {
        Vec3 movement = this.getDeltaMovement();
        Vec3 direction = new Vec3(movement.x, 0.0D, movement.z);
        if (direction.lengthSqr() < 1.0E-4D) {
            Vec3 look = this.getLookAngle();
            direction = new Vec3(look.x, 0.0D, look.z);
        }
        if (direction.lengthSqr() < 1.0E-4D) {
            return null;
        }
        return direction.normalize();
    }

    private double getFireBodyForwardCrashProbeDistance() {
        Vec3 movement = this.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        return Mth.clamp(this.getBbWidth() * 0.75D + horizontalSpeed * 2.0D + 0.75D, 1.5D, 4.0D);
    }

    private void triggerFireBodyCrash() {
        triggerFireBodyCrash(this.position());
    }

    private void triggerFireBodyCrash(Vec3 impact) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        boolean allowGriefing = DragonGriefingRules.canDestroyBlocks(server);
        double x = impact.x;
        double y = impact.y;
        double z = impact.z;
        List<Entity> immune = new ArrayList<>(this.getPassengers());
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
        for (LivingEntity entity : protectedEntities) {
            entity.setInvulnerable(true);
        }
        explosion.explode();
        explosion.finalizeExplosion(true);
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
            int baseY = Mth.floor(y);
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

    public boolean isBreathingFire() {
        return this.entityData.get(DATA_FIRE_BREATHING);
    }

    public void setBreathingFire(boolean breathing) {
        this.entityData.set(DATA_FIRE_BREATHING, breathing);
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

        AnimationController<Cindervane> actions = new AnimationController<>(this, CindervaneAnimationHandler.ACTION_CONTROLLER, 5, animationHandler::actionPredicate);
        animationHandler.setupActionController(actions);
        actions.setSoundKeyframeHandler(event -> {
            String soundKey = event.getKeyframeData().getSound();
            if (soundKey != null && !soundKey.isEmpty()) {
                handleAnimationSound(soundKey);
            }
        });
        controllers.add(actions);

        AnimationController<Cindervane> fastActionController = new AnimationController<>(this, CindervaneAnimationHandler.FAST_ACTION_CONTROLLER, 1,
                animationHandler::fastActionPredicate);
        animationHandler.setupFastActionController(fastActionController);
        fastActionController.setSoundKeyframeHandler(event -> {
            String soundKey = event.getKeyframeData().getSound();
            if (soundKey != null && !soundKey.isEmpty()) {
                handleAnimationSound(soundKey);
            }
        });
        controllers.add(fastActionController);

        AnimationController<Cindervane> vocalController = new AnimationController<>(this, com.leon.saintsdragons.server.entity.dragons.handlers.DragonVocalAnimationHelper.CONTROLLER, 2,
                com.leon.saintsdragons.server.entity.dragons.handlers.DragonVocalAnimationHelper::idle);
        com.leon.saintsdragons.server.entity.dragons.handlers.DragonVocalAnimationHelper.registerGrumbles(vocalController, this);
        vocalController.setSoundKeyframeHandler(event -> {
            String soundKey = event.getKeyframeData().getSound();
            if (soundKey != null && !soundKey.isEmpty()) {
                handleAnimationSound(soundKey);
            }
        });
        controllers.add(vocalController);

        AnimationController<Cindervane> interactionController = new AnimationController<>(this, DragonInteractionAnimationHelper.CONTROLLER, 1,
                DragonInteractionAnimationHelper::idle);
        animationHandler.setupInteractionController(interactionController);
        interactionController.setSoundKeyframeHandler(event -> {
            String soundKey = event.getKeyframeData().getSound();
            if (soundKey != null && !soundKey.isEmpty()) {
                handleAnimationSound(soundKey);
            }
        });
        controllers.add(interactionController);

        AnimationController<Cindervane> stateController = new AnimationController<>(this, DragonStateAnimationHelper.CONTROLLER, 1,
                DragonStateAnimationHelper::idle);
        animationHandler.setupStateController(stateController);
        stateController.setSoundKeyframeHandler(event -> {
            String soundKey = event.getKeyframeData().getSound();
            if (soundKey != null && !soundKey.isEmpty()) {
                handleAnimationSound(soundKey);
            }
        });
        controllers.add(stateController);
    }

    @Override
    public Map<String, VocalEntry> getVocalEntries() {
        return VOCAL_ENTRIES;
    }

    @Override
    public DragonSoundProfile getSoundProfile() {
        return CindervaneSoundProfile.INSTANCE;
    }

    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        return getMeleeMode() == 0
                ? ModAbilities.CINDERVANE_BITE
                : ModAbilities.CINDERVANE_SLASH_GRAB;
    }

    @Override
    public boolean hasSecondaryMelee() {
        return true;
    }

    @Override
    public DragonAbilityType<?, ?> getRoaringAbility() {
        return ModAbilities.CINDERVANE_ROAR;
    }

    @Override
    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return ModAbilities.CINDERVANE_HURT;
    }

    @Override
    public int getDeathAnimationDurationTicks() {
        return 50;
    }

    @Override
    public boolean hurt(@Nonnull DamageSource source, float amount) {
        if (isDying()) {
            return false;
        }
        if (source.is(DamageTypes.IN_FIRE) ||
            source.is(DamageTypes.ON_FIRE) ||
            source.is(DamageTypes.LAVA) ||
            source.is(DamageTypes.HOT_FLOOR)) {
            if (this.isOnFire() || this.getRemainingFireTicks() > 0) {
                this.clearFire();
                this.setRemainingFireTicks(0);
            }
            return false;
        }

        if (source.is(DamageTypes.FALL)) {
            return false;
        }

        if (isSleeping() || isSleepingEntering() || isSleepingExiting()) {
            wakeUpImmediately();
            suppressSleep(200);
        }

        return super.hurt(source, amount);
    }

    @Override
    protected double getCullingInflateX() {
        return 6.0D;
    }

    @Override
    protected double getCullingInflateY() {
        return 3.0D;
    }

    @Override
    protected double getCullingInflateZ() {
        return 6.0D;
    }

    @Override
    protected DragonAbilityType<?, ?> getDeathAbilityType() {
        return ModAbilities.CINDERVANE_DIE;
    }

    @Override
    protected boolean isRidingAbilityAllowed(DragonAbilityType<?, ?> abilityType) {
        return abilityType == ModAbilities.CINDERVANE_BITE
                || abilityType == ModAbilities.CINDERVANE_SLASH_GRAB
                || abilityType == ModAbilities.CINDERVANE_FIRE_BODY
                || abilityType == ModAbilities.CINDERVANE_ROAR
                || abilityType == ModAbilities.CINDERVANE_FIRE_BREATH_VOLLEY;
    }

    @Override
    public void forceEndActiveAbility() {
        combatManager.forceEndActiveAbility();
        fireBodyCrashArmed = false;
        this.setBreathingFire(false);
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        if (!this.isVehicle() && !this.isOrderedToSit()) {
            combatManager.tryUseAbility(getPrimaryAttackAbility());
        }
        return true;
    }

    @Override
    public boolean isFood(@Nonnull ItemStack stack) {
        return stack.is(ModTags.Items.CINDERVANE_FOODS);
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
    protected void dropAdditionalDeathLootAfterBase(@NotNull DamageSource source) {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.CINDERVANE_ID);
        double eggDropChance = config.extraDouble("egg_drop_chance", 0.12D);
        if (!level().isClientSide && getGender() == DragonGender.FEMALE && this.random.nextDouble() < eggDropChance) {
            this.spawnAtLocation(ModItems.CINDERVANE_EGG.get());
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@Nonnull ServerLevel level, @Nonnull AgeableMob partner) {
        return createBreedOffspring(level, partner, ModEntities.CINDERVANE.get(), Cindervane::applyConfiguredAttributes);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TimeFlying", timeFlying);
        saveRideableData(tag);
        tag.putInt("FeedingCooldownTicks", Math.max(0, this.entityData.get(DATA_FEEDING_COOLDOWN)));

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
        this.packLeaderUuid = tag.hasUUID("PackLeaderUuid") ? tag.getUUID("PackLeaderUuid") : null;
        if (this.isTame()) {
            this.packLeaderUuid = null;
        }
        if (!savedFlying) {
            landingTicks = 0;
            airTicks = 0;
        } else {
            airTicks = Math.max(airTicks, 1);
        }
        groundTicks = 0;

        this.setNoGravity(isFlying() || isHovering());
        if (tag.contains("FeedingCooldownTicks")) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, tag.getInt("FeedingCooldownTicks")));
        }
        if (!level().isClientSide) {
            this.tickCount = 0;
        }

        applyConfiguredAttributes();
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
    public int getMaxPackSize() {
        return MAX_PACK_SIZE;
    }

    @Override
    public double getPackSearchRadius() {
        return PACK_SEARCH_RADIUS;
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
        return !(flying && !isVehicle() && (isInWater() || isInWaterOrBubble() || isInLava()));
    }

    @Override
    protected int getRedirectedFlyingTakeoffTicks() {
        return TAKEOFF_ANIMATION_TICKS;
    }

    @Override
    protected void onFlyingStateChanged(boolean wasFlying, boolean flying) {
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
    protected void onTakeoffStateStarted() {
        triggerAnim(CindervaneAnimationHandler.FAST_ACTION_CONTROLLER, "takeoff");
        getSoundHandler().playMovingEntitySound(ModSounds.CINDERVANE_TAKEOFF.get(), 1.2f, 1.0f, 55);
    }

    @Override
    protected void onLandingDataSet(boolean landing) {
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

        return true;
    }

    @Override
    protected void resetTimeFlyingAfterLanding() {
        this.timeFlying = 0;
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
            triggerAnim(CindervaneAnimationHandler.ACTION_CONTROLLER, "landed");
            getSoundHandler().playMovingEntitySound(ModSounds.CINDERVANE_LANDED.get(), 1.0f, 1.0f, 59);
            suppressSleep(60);
        }
        markLandedNow();
    }

    @Override
    public boolean isFlapping() {
        return isFlying() && this.getDeltaMovement().y > -0.1D;
    }

    public boolean canBeBound() {
        return !isDying() && !isAccelerating();
    }

    @Override
    public DragonAbilityType<?, ?> getChannelingAbility() {
        return ModAbilities.CINDERVANE_FIRE_BREATH_VOLLEY;
    }

    public void setServerBonePosition(String boneName, Vec3 position) {
        if (boneName == null || position == null) return;
        this.serverBonePositionCache.put(boneName, position);
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
    protected ScreenShakeComponent getScreenShakeComponent() {
        return screenShakeComponent;
    }

    @Override
    public double getShakeDistance() {
        return 18.0;
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
        return DragonEntity.DragonSleepPreferences.NOCTURNAL();
    }

    @Override
    public boolean canSleepNow() {
        return !isBreathingFire() && !isVehicle() && getActiveAbility() == null;
    }

    @Override
    protected Supplier<? extends Block> getEggBlock() {
        return ModBlocks.CINDERVANE_EGG;
    }

}
