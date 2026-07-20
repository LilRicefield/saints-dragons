package com.leon.saintsdragons.server.entity.dragons.cindervane;

import com.mojang.serialization.Dynamic;
import com.leon.saintsdragons.util.animation.AnimationHelper;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModBlocks;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.DragonAirCombatSettings;
import com.leon.saintsdragons.server.ai.DragonAirCombatSettingsProvider;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrain;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane.CindervanePackFlightCoordinator;
import com.leon.saintsdragons.server.ai.dragonbrain.profiles.CindervaneBrain;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.base.DragonVariant;
import com.leon.saintsdragons.server.entity.base.DragonVariantSet;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.controller.cindervane.CindervaneRiderController;
import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import com.leon.saintsdragons.server.flight.DragonFlightVisuals;
import com.leon.saintsdragons.server.flight.DragonRiderFlight;
import com.leon.saintsdragons.server.entity.dragons.cindervane.handlers.CindervaneAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.cindervane.handlers.CindervaneInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.cindervane.handlers.CindervaneSoundProfile;
import com.leon.saintsdragons.server.entity.dragons.util.DragonElementalImmunity;
import com.leon.saintsdragons.server.entity.dragons.util.DragonGriefingRules;
import com.leon.saintsdragons.server.entity.dragons.util.DragonUtilities;
import com.leon.saintsdragons.server.entity.component.ScreenShakeComponent;
import com.leon.saintsdragons.server.entity.interfaces.DragonChestCarrier;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.interfaces.PackMember;
import com.leon.saintsdragons.server.loot.DragonLootTables;
import com.leon.saintsdragons.server.entity.interfaces.ShakesScreen;
import com.leon.saintsdragons.server.menu.DragonInventoryMenu;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.network.MessageDragonMeleeMode;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.server.world.DragonSpawnRules;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.Brain;
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
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;
import javax.annotation.Nonnull;

public class Cindervane extends RideableFlyingDragon implements ShakesScreen, PackMember<Cindervane>, DragonChestCarrier, DragonAirCombatSettingsProvider {
    private static final CindervaneBrain DRAGON_BRAIN = new CindervaneBrain();
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
    private static final EntityDataAccessor<Boolean> DATA_HAS_CHEST =
            SynchedEntityData.defineId(Cindervane.class, EntityDataSerializers.BOOLEAN);
    private static final int LANDED_RECOVERY_TICKS = 34;
    public static final int TAKEOFF_ANIMATION_TICKS = 23;
    public static final DragonAirCombatSettings AI_AIR_COMBAT_SETTINGS =
            new DragonAirCombatSettings(
                    TAKEOFF_ANIMATION_TICKS,
                    2.2D,
                    30,
                    16.0D,
                    2.0D,
                    8.0D,
                    5.0D
            );
    private static final double FIRE_BODY_CRASH_MIN_DROP = 7.0D;
    private static final float FIRE_BODY_EXPLOSION_RADIUS = 15.0F;
    private static final double FIRE_BODY_IMPRINT_RADIUS = 9.0D;
    private static final int RIDER_LANDING_BLEND_DURATION = 3;
    private static final double FIRE_BODY_IMPRINT_DEPTH_FACTOR = 0.6D;
    private static final float FIRE_BODY_EXPLOSION_DAMAGE = 200.0F;
    private static final float FIRE_BODY_SELF_DAMAGE_ON_CRASH = 40.0F;
    public static final double BREED_PARTNER_RANGE = 20.0D;
    public static final double BREED_DISTANCE_SQR = 2500.0D;
    private static final int MAX_PACK_SIZE = 4;
    private static final double PACK_SEARCH_RADIUS = 48.0D;
    private static final int MIN_AMBIENT_DELAY = 180;
    private static final int MAX_AMBIENT_DELAY = 420;
    private static final int CINDERVANE_CHEST_SLOTS = 15;
    public static final double RIDER_WALK_SPEED = 0.18D;
    public static final double RIDER_RUN_SPEED = 0.26D;
    public static final float RIDER_KEY_PITCH_DEG = 25.0f;
    private static final Map<String, VocalEntry> VOCAL_ENTRIES =
            new VocalEntryBuilder()
                    .add("grumble1", AnimationHelper.VOCAL_CONTROLLER, "animation.cindervane.grumble1", ModSounds.CINDERVANE_GRUMBLE_1, 1.1f, 0.98f, 0.06f, false, false, false)
                    .add("grumble2", AnimationHelper.VOCAL_CONTROLLER, "animation.cindervane.grumble2", ModSounds.CINDERVANE_GRUMBLE_2, 1.2f, 0.96f, 0.08f, false, false, false)
                    .add("grumble3", AnimationHelper.VOCAL_CONTROLLER, "animation.cindervane.grumble3", ModSounds.CINDERVANE_GRUMBLE_3, 1.0f, 1.0f, 0.05f, false, false, false)
                    .add("roar", CindervaneAnimationHandler.ACTION_CONTROLLER, "animation.cindervane.roar", ModSounds.CINDERVANE_ROAR, 1.5f, 0.95f, 0.1f, false, false, false)
                    .add("cindervane_hurt", AnimationHelper.INTERACTION_CONTROLLER, "animation.cindervane.hurt", ModSounds.CINDERVANE_HURT, 1.2f, 0.95f, 0.1f, false, false, false)
                    .add("cindervane_die", AnimationHelper.INTERACTION_CONTROLLER, "animation.cindervane.die", ModSounds.CINDERVANE_DIE, 1.5f, 1.0f, 0.0f, false, false, false)
                    .build();

    public AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);
    private final CindervaneAnimationHandler animationHandler = new CindervaneAnimationHandler(this);
    private final CindervaneInteractionHandler interactionHandler = new CindervaneInteractionHandler(this);
    private final CindervaneRiderController riderController;
    private final AnimationController<Cindervane> movementController;
    private final AnimationController<Cindervane> actionController;
    private final AnimationController<Cindervane> fastActionController;
    private final AnimationController<Cindervane> flightController;
    private final AnimationController<Cindervane> vocalController;
    private final AnimationController<Cindervane> interactionController;
    private final SimpleContainer cindervaneChestInventory = new SimpleContainer(CINDERVANE_CHEST_SLOTS);
    private int targetCooldown;
    private int airTicks;
    public int groundTicks;
    public int timeFlying = 0;
    private boolean fireBodyCrashArmed;
    private double fireBodyCrashMaxHeight;
    private boolean autoGrabPassengerMountAllowed;
    @Nullable
    private UUID slashGrabPassengerUuid;
    @Nullable
    private UUID packLeaderUuid;
    private final DragonFlightVisuals.State flightVisualState = new DragonFlightVisuals.State();
    private final ScreenShakeComponent screenShakeComponent;
    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case ABILITY_USE, ABILITY_STOP, OPEN_INVENTORY -> true;
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
        this.movementController = new AnimationController<>(this, "movement", 2, animationHandler::movementPredicate);
        this.actionController = new AnimationController<>(this, CindervaneAnimationHandler.ACTION_CONTROLLER, 5, animationHandler::actionPredicate);
        this.fastActionController = new AnimationController<>(this, CindervaneAnimationHandler.FAST_ACTION_CONTROLLER, 1, animationHandler::fastActionPredicate);
        this.flightController = AnimationHelper.createFlightController(this, getFlightAnimationTransitionTicks(), animationHandler::flightPredicate);
        this.vocalController = new AnimationController<>(this, AnimationHelper.VOCAL_CONTROLLER, 2, AnimationHelper::vocalIdle);
        this.interactionController = new AnimationController<>(this, AnimationHelper.INTERACTION_CONTROLLER, 1, AnimationHelper::interactionIdle);
        setupAnimationControllers();
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
                .add(Attributes.FOLLOW_RANGE, 64.0D)
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
        this.entityData.define(DATA_HAS_CHEST, false);
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
        syncFlightAnimationState();
        tickAsyncFlightNavigation();
        if (isFlying()) {
            timeFlying++;
        } else {
            timeFlying = 0;
        }
        tickFeedingCooldown();
        handleAmbientSounds();

        if (isSleeping() || isSleepingEntering() || isSleepTransitioning()) {
            if (this.getTarget() != null || this.isAggressive()) {
                startSleepExit();
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
            resetForRiderTransition();
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

    private static class CindervaneFamilyData extends AgeableMob.AgeableMobGroupData {
        public CindervaneFamilyData(boolean shouldSpawnBaby) {
            super(shouldSpawnBaby);
        }
    }

    private void updateSittingProgress() {
        tickSitTransition(
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
                triggerAnim(AnimationHelper.MOVEMENT_CONTROLLER, AnimationHelper.LANDED);
                getSoundHandler().playMovingEntitySound(ModSounds.CINDERVANE_LANDED.get(), 1.0f, 1.0f, 59);
                completeTouchdownLanding(LandingSource.RIDER);
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

    private void tickScreenShake() {
        screenShakeComponent.tick();
    }

    public boolean isRiderPitchKeyMode() {
        return this.entityData.get(DATA_PITCH_KEY_MODE);
    }

    public void setRiderPitchKeyMode(boolean enabled) {
        this.entityData.set(DATA_PITCH_KEY_MODE, enabled);
    }

    @Override
    public boolean isGoingUp() {
        return this.entityData.get(DATA_GOING_UP);
    }

    @Override
    protected void afterInitializeAnimationState() {
        groundTicks = 0;
        airTicks = 0;
    }

    @Override
    protected Brain.Provider<Cindervane> brainProvider() {
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
    public int getFlightMode() {
        return evaluateStandardFlightMode(false);
    }

    public DragonFlightStateEvaluator.VisualState getVisualFlightState(float partialTick) {
        return evaluateVisualFlightState(partialTick, getFlightPitchRadians(partialTick));
    }

    @Override
    protected void onRiderToggleMelee(Player player) {
        if (isAerial() && player instanceof ServerPlayer serverPlayer && !level().isClientSide) {
            serverPlayer.displayClientMessage(
                    Component.translatable("saintsdragons.message.cindervane_slashing_ground_only"),
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
        if (passenger instanceof IvyTheDragonMerchant ivy
                && ivy.isTame()
                && java.util.Objects.equals(ivy.getOwnerUUID(), getOwnerUUID())) {
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

    @Override
    public boolean isWildAggressionEnabled() {
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

        boolean inWater = this.isInWater() || this.isInWaterOrBubble() || this.isInLava();

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
        ServerPlayer responsiblePlayer = DragonUtilities.resolveResponsiblePlayer(this);
        if (responsiblePlayer != null) {
            DragonUtilities.awardAdvancement(responsiblePlayer, "tactical_nuke", "tactical_nuke");
        }
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
                living -> living.isAlive()
                        && !immuneIds.contains(living.getId())
                        && !this.isAlly(living)
                        && !DragonElementalImmunity.isFireImmune(living));

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
        controllers.add(movementController, vocalController, actionController, fastActionController, flightController, interactionController);
    }

    private void setupAnimationControllers() {
        AnimationHelper.registerSoundKeyframes(this, movementController, actionController,
                fastActionController, flightController, vocalController, interactionController);
        animationHandler.setupMovementController(movementController);
        animationHandler.setupActionController(actionController);
        animationHandler.setupFastActionController(fastActionController);
        animationHandler.setupFlightController(flightController);
        AnimationHelper.registerGrumbles(vocalController, this);
        animationHandler.setupInteractionController(interactionController);
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

    private void enforcePrimaryMeleeForFlight(@Nullable Player rider) {
        if (level().isClientSide || getMeleeMode() == 0) {
            return;
        }
        setMeleeMode(0);
        if (rider instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(
                    Component.translatable("saintsdragons.message.cindervane_slashing_ground_only"),
                    true
            );
        }
        syncMeleeMode(rider);
    }

    private void syncMeleeMode(@Nullable Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendToPlayer(serverPlayer, new MessageDragonMeleeMode(getMeleeMode()));
        }
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
        rememberIncomingProjectile(source);
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

        if (isSleeping() || isSleepingEntering() || isSleepTransitioning()) {
            startSleepExit();
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
    protected void onRiderOpenInventory(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            openCindervaneInventory(serverPlayer);
        }
    }

    private void openCindervaneInventory(ServerPlayer player) {
        if (!this.isAlive() || player.distanceToSqr(this) > 64.0D) {
            return;
        }
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, ignored) -> new DragonInventoryMenu(containerId, playerInventory, this),
                this.getDisplayName()
        ));
    }

    private void dropCindervaneChestContents() {
        for (int slot = 0; slot < cindervaneChestInventory.getContainerSize(); slot++) {
            ItemStack stack = cindervaneChestInventory.getItem(slot);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack.copy());
                cindervaneChestInventory.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    public void removeCindervaneChestAndDropContents() {
        if (this.level().isClientSide || !hasCindervaneChest()) {
            return;
        }
        dropCindervaneChestContents();
        setCindervaneChest(false);
        this.playSound(SoundEvents.DONKEY_CHEST, 1.0F, 1.0F);
    }

    public boolean hasCindervaneChest() {
        return this.entityData.get(DATA_HAS_CHEST);
    }

    @Override
    public boolean hasAttachedChest() {
        return hasCindervaneChest();
    }

    public void setCindervaneChest(boolean value) {
        this.entityData.set(DATA_HAS_CHEST, value);
        if (!value) {
            cindervaneChestInventory.clearContent();
        }
    }

    @Override
    public void setAttachedChest(boolean value) {
        setCindervaneChest(value);
    }

    public Container getCindervaneChestInventory() {
        return cindervaneChestInventory;
    }

    @Override
    public Container getAttachedChestInventory() {
        return getCindervaneChestInventory();
    }

    @Override
    public void removeAttachedChestAndDropContents() {
        removeCindervaneChestAndDropContents();
    }

    @Override
    protected void dropAdditionalDeathLootAfterBase(@NotNull DamageSource source) {
        if (!level().isClientSide && getGender() == DragonGender.FEMALE) {
            DragonLootTables.dropEntityLoot(this, DragonLootTables.CINDERVANE_FEMALE_DEATH, source);
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
        tag.putBoolean("CindervaneHasChest", hasCindervaneChest());
        if (hasCindervaneChest()) {
            tag.put("CindervaneChestItems", cindervaneChestInventory.createTag());
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
            airTicks = 0;
        } else {
            airTicks = Math.max(airTicks, 1);
        }
        groundTicks = 0;

        this.setNoGravity(isFlying() || isHovering());
        if (tag.contains("FeedingCooldownTicks")) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, tag.getInt("FeedingCooldownTicks")));
        }
        setCindervaneChest(tag.getBoolean("CindervaneHasChest"));
        if (hasCindervaneChest() && tag.contains("CindervaneChestItems", Tag.TAG_LIST)) {
            cindervaneChestInventory.fromTag(tag.getList("CindervaneChestItems", Tag.TAG_COMPOUND));
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
    public DragonAirCombatSettings getAiAirCombatSettings() {
        return AI_AIR_COMBAT_SETTINGS;
    }

    @Override
    public boolean handleDirectAirPackFollow(Vec3 target, double speed) {
        if (!isAerial() || isLanding()) {
            return false;
        }
        UUID leaderUuid = getPackLeaderUuid();
        if (leaderUuid == null || level().isClientSide) {
            return false;
        }
        var leaderEntity = ((net.minecraft.server.level.ServerLevel) level()).getEntity(leaderUuid);
        if (!(leaderEntity instanceof Cindervane leader) || leader == this || !leader.isAerial()) {
            return false;
        }
        Vec3 coordinatedTarget = CindervanePackFlightCoordinator.followFormationTarget(this, leader);
        getAIMovement().setWaypoint(coordinatedTarget, speed);
        return true;
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
    protected void onFlyingStateChanged(boolean wasFlying, boolean flying) {
        if (flying) {
            enforcePrimaryMeleeForFlight(getControllingPassenger() instanceof Player player ? player : null);
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
        enforcePrimaryMeleeForFlight(getControllingPassenger() instanceof Player player ? player : null);
        triggerAnim(AnimationHelper.FLIGHT_CONTROLLER, AnimationHelper.TAKEOFF);
        getSoundHandler().playMovingEntitySound(ModSounds.CINDERVANE_TAKEOFF.get(), 1.2f, 1.0f, 55);
    }

    @Override
    protected void onLandingDataSet(boolean landing) {
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
        if (this.isInWaterOrBubble()) {
            return false;
        }
        return this.onGround();
    }

    @Override
    protected void resetTimeFlyingAfterLanding() {
        this.timeFlying = 0;
    }

    public void handleAiLandingComplete() {
        if (isInWaterOrBubble()) {
            suppressSleep(60);
            completeTouchdownLanding(LandingSource.AI);
            return;
        }
        if (!level().isClientSide) {
            triggerAnim(AnimationHelper.MOVEMENT_CONTROLLER, AnimationHelper.LANDED);
            getSoundHandler().playMovingEntitySound(ModSounds.CINDERVANE_LANDED.get(), 1.0f, 1.0f, 59);
            suppressSleep(60);
        }
        completeTouchdownLanding(LandingSource.AI);
        startStandardLandedRecovery(LANDED_RECOVERY_TICKS);
    }

    @Override
    protected void completeGroundedAerialRecoveryLanding() {
        handleAiLandingComplete();
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
