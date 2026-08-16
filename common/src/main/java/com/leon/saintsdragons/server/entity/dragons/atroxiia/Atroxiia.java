package com.leon.saintsdragons.server.entity.dragons.atroxiia;

import com.mojang.serialization.Dynamic;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.common.registry.ModBlocks;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrain;
import com.leon.saintsdragons.server.ai.dragonbrain.profiles.AtroxiiaBrain;
import com.leon.saintsdragons.server.ai.navigation.PathNavigateGround;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.atroxiia.AtroxiiaHelheimQuakeAbility;
import com.leon.saintsdragons.server.entity.base.RideableGroundDragon;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.component.DragonForwardMovementComponent;
import com.leon.saintsdragons.server.entity.component.DragonMotionMath;
import com.leon.saintsdragons.server.entity.component.ScreenShakeComponent;
import com.leon.saintsdragons.server.entity.controller.DragonRiderControllerHelper;
import com.leon.saintsdragons.server.entity.controller.atroxiia.AtroxiiaRiderController;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.handlers.AtroxiiaAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.handlers.AtroxiiaInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.handlers.AtroxiiaSoundProfile;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.handlers.AtroxiiaTamingHandler;
import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;
import com.leon.saintsdragons.server.entity.interfaces.PassiveTreeDestroyer;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.interfaces.ShakesScreen;
import com.leon.saintsdragons.server.entity.interfaces.ScentAssessingDragon;
import com.leon.saintsdragons.server.world.DragonSpawnRules;
import com.leon.saintsdragons.server.loot.DragonLootTables;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;
import java.util.function.Supplier;

public class Atroxiia extends RideableGroundDragon implements ShakesScreen, PassiveTreeDestroyer, ScentAssessingDragon {
    public static final EntityDataAccessor<Boolean> DATA_TAMING_STUNNED =
            SynchedEntityData.defineId(Atroxiia.class, EntityDataSerializers.BOOLEAN);
    private static final AtroxiiaBrain DRAGON_BRAIN = new AtroxiiaBrain();
    private static final EntityDataAccessor<Float> DATA_SCREEN_SHAKE_AMOUNT =
            SynchedEntityData.defineId(Atroxiia.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_PRECISE_STRIKE_NUDGE_TICKS =
            SynchedEntityData.defineId(Atroxiia.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_PRECISE_STRIKE_NUDGE_X =
            SynchedEntityData.defineId(Atroxiia.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PRECISE_STRIKE_NUDGE_Z =
            SynchedEntityData.defineId(Atroxiia.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_SLITHERING =
            SynchedEntityData.defineId(Atroxiia.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SLITHER_MOVEMENT_TICKS =
            SynchedEntityData.defineId(Atroxiia.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SLITHER_MOVEMENT_X =
            SynchedEntityData.defineId(Atroxiia.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SLITHER_MOVEMENT_Z =
            SynchedEntityData.defineId(Atroxiia.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_FEEDING_COOLDOWN =
            SynchedEntityData.defineId(Atroxiia.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PITCH_KEY_MODE =
            SynchedEntityData.defineId(Atroxiia.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_SWIM_PITCH_RAD =
            SynchedEntityData.defineId(Atroxiia.class, EntityDataSerializers.FLOAT);
    private static final int MOVEMENT_TRANSITION_TICKS = 4;
    private static final int MIN_AMBIENT_DELAY = 200;
    private static final int MAX_AMBIENT_DELAY = 600;
    private static final int FLEX_CONTROL_LOCK_TICKS = 67;
    private static final int FLEX_COOLDOWN_TICKS = 120;
    public static final double BREED_PARTNER_RANGE = 20.0D;
    public static final double BREED_DISTANCE_SQR = 36.0D;
    private static final int SIT_DOWN_TICKS = 48;
    private static final int SIT_UP_TICKS = 25;
    private static final int FALL_ASLEEP_TICKS = 38;
    private static final int WAKE_UP_TICKS = 42;
    private static final int SCENT_ASSESSMENT_ANIMATION_TICKS = 70;
    public static final int HURT_ANIMATION_TICKS = 15;
    public static final int DEATH_ANIMATION_TICKS = 40;
    public static final int EAT_ANIMATION_TICKS = 40;
    public static final int HURT_SOUND_TICKS = 20;
    public static final int EAT_SOUND_TICKS = 60;
    private static final double BABY_MAX_HEALTH = 30.0D;
    private static final double BABY_ARMOR = 0.0D;
    private static final double GROUND_MOVEMENT_SPEED = 0.33D;
    private static final double RIDER_JUMP_STRENGTH = 1.15D;
    private static final double RIDER_JUMP_FORWARD_BOOST = 0.7D;
    private static final float MAX_UP_STEP = 1.25F;
    public static final double RIDER_WALK_SPEED = 0.12D;
    public static final double RIDER_RUN_SPEED = 0.28D;
    private static final double SLITHER_NUDGE_DISTANCE = 24.0D;
    private static final double SLITHER_AUTO_MOVE_SPEED = RIDER_RUN_SPEED * 4.0D;
    private static final double PRECISE_STRIKE_NUDGE_DRAG = 0.78D;
    private static final float RIDER_KEY_PITCH_DEG = 25.0F;
    private static final float DEFAULT_TAMING_STUN_HEALTH = 60.0F;

    @Override
    public int getScentAssessmentDurationTicks() {
        return SCENT_ASSESSMENT_ANIMATION_TICKS;
    }

    private static final Map<String, VocalEntry> VOCAL_ENTRIES = new VocalEntryBuilder()
            .add("grumble1", AnimationHelper.VOCAL_CONTROLLER, "animation.atroxiia.grumble1",
                    ModSounds.ATROXIIA_GRUMBLE_1, 1.0F, 0.95F, 0.1F, false, false, true)
            .add("grumble2", AnimationHelper.VOCAL_CONTROLLER, "animation.atroxiia.grumble2",
                    ModSounds.ATROXIIA_GRUMBLE_2, 1.0F, 0.95F, 0.1F, false, false, true)
            .add("grumble3", AnimationHelper.VOCAL_CONTROLLER, "animation.atroxiia.grumble3",
                    ModSounds.ATROXIIA_GRUMBLE_3, 1.0F, 0.95F, 0.1F, false, false, true)
            .add("atroxiia_flex", AtroxiiaAnimationHandler.MOVEMENT_CONTROLLER, "animation.atroxiia.flex",
                    ModSounds.ATROXIIA_FLEX, 1.4F, 0.95F, 0.05F, false, false, false)
            .add("atroxiia_hurt", AnimationHelper.INTERACTION_CONTROLLER, "animation.atroxiia.hurt",
                    ModSounds.ATROXIIA_HURT, 1.2F, 0.95F, 0.1F, false, true, true)
            .add("atroxiia_die", AnimationHelper.INTERACTION_CONTROLLER, "animation.atroxiia.die",
                    ModSounds.ATROXIIA_DIE, 1.5F, 1.0F, 0.0F, false, true, true)
            .build();

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final AtroxiiaRiderController riderController = new AtroxiiaRiderController(this);
    private final AtroxiiaAnimationHandler animationHandler = new AtroxiiaAnimationHandler(this);
    private final AtroxiiaInteractionHandler interactionHandler = new AtroxiiaInteractionHandler(this);
    private final AtroxiiaTamingHandler tamingController = new AtroxiiaTamingHandler(this);
    private final ScreenShakeComponent screenShakeComponent;
    private final DragonForwardMovementComponent slitherMovement = new DragonForwardMovementComponent(
            this,
            new DragonForwardMovementComponent.StateAccess() {
                @Override
                public void start(int ticks, Vec3 velocity, boolean dashing, boolean dodging, double horizontalDrag) {
                    entityData.set(DATA_SLITHER_MOVEMENT_TICKS, Math.max(1, ticks));
                    setSlitherMovementVelocity(velocity);
                }

                @Override
                public int ticks() {
                    return entityData.get(DATA_SLITHER_MOVEMENT_TICKS);
                }

                @Override
                public void setTicks(int ticks) {
                    entityData.set(DATA_SLITHER_MOVEMENT_TICKS, Math.max(0, ticks));
                }

                @Override
                public Vec3 velocity() {
                    return getSlitherMovementVelocity();
                }

                @Override
                public void setVelocity(Vec3 velocity) {
                    setSlitherMovementVelocity(velocity);
                }

                @Override
                public double horizontalDrag() {
                    return 1.0D;
                }

                @Override
                public void clear() {
                    clearSlitherMovementState();
                }
            }
    );
    private boolean nextMeleeRightSide = true;
    private float swimPitchRad;
    private float previousSwimPitchRad;
    private float clientSwimPitchRad;
    private float previousClientSwimPitchRad;

    public Atroxiia(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setMaxUpStep(MAX_UP_STEP);
        this.setPathfindingMalus(BlockPathTypes.POWDER_SNOW, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_POWDER_SNOW, 0.0F);
        this.screenShakeComponent = new ScreenShakeComponent(this, DATA_SCREEN_SHAKE_AMOUNT, 0.18F);
        seedAmbientSoundTimer(MIN_AMBIENT_DELAY, MAX_AMBIENT_DELAY, 80);
        if (!level.isClientSide) {
            applyConfiguredAttributes();
            this.setHealth(this.getMaxHealth());
        }
    }

    public static boolean canSpawnHere(EntityType<? extends Atroxiia> type,
                                       LevelAccessor level,
                                       MobSpawnType spawnType,
                                       BlockPos pos,
                                       RandomSource random) {
        return DragonSpawnRules.hasDryGroundSpawnSpace(level, pos)
                && DragonSpawnRules.passesNearbyDragonDensityCheck(level, spawnType, pos, Atroxiia.class);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TAMING_STUNNED, false);
        this.entityData.define(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        this.entityData.define(DATA_PRECISE_STRIKE_NUDGE_TICKS, 0);
        this.entityData.define(DATA_PRECISE_STRIKE_NUDGE_X, 0.0F);
        this.entityData.define(DATA_PRECISE_STRIKE_NUDGE_Z, 0.0F);
        this.entityData.define(DATA_SLITHERING, false);
        this.entityData.define(DATA_SLITHER_MOVEMENT_TICKS, 0);
        this.entityData.define(DATA_SLITHER_MOVEMENT_X, 0.0F);
        this.entityData.define(DATA_SLITHER_MOVEMENT_Z, 0.0F);
        this.entityData.define(DATA_FEEDING_COOLDOWN, 0);
        this.entityData.define(DATA_PITCH_KEY_MODE, false);
        this.entityData.define(DATA_SWIM_PITCH_RAD, 0.0F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        var movementController = new AnimationController<>(this, AnimationHelper.MOVEMENT_CONTROLLER, MOVEMENT_TRANSITION_TICKS,
                animationHandler::movementPredicate);
        var interactionController = new AnimationController<>(this, AnimationHelper.INTERACTION_CONTROLLER, 1,
                AnimationHelper::interactionIdle);
        var fastActionController = new AnimationController<>(this, AtroxiiaAnimationHandler.FAST_ACTION_CONTROLLER, 1,
                animationHandler::fastActionPredicate);
        var vocalController = new AnimationController<>(this, AnimationHelper.VOCAL_CONTROLLER, 2,
                AnimationHelper::vocalIdle);
        AnimationHelper.registerStepKeyframes(this, movementController);
        AnimationHelper.registerSoundKeyframes(this, movementController, vocalController, interactionController);
        AnimationHelper.registerGrumbles(vocalController, this);
        animationHandler.setupMovementController(movementController);
        animationHandler.setupInteractionController(interactionController);
        animationHandler.setupFastActionController(fastActionController);
        controllers.add(movementController, vocalController, fastActionController, interactionController);
    }

    @Override
    public Map<String, VocalEntry> getVocalEntries() {
        return VOCAL_ENTRIES;
    }

    @Override
    public DragonSoundProfile getSoundProfile() {
        return AtroxiiaSoundProfile.INSTANCE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.ATROXIIA_ID);
        return TamableAnimal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, config.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, GROUND_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, config.armor())
                .add(Attributes.ATTACK_DAMAGE, 10.0D);
    }

    public void applyConfiguredAttributes() {
        DragonAttributeConfig config = getConfiguredDragonAttributes();
        applyConfiguredHealthAndArmor(config, BABY_MAX_HEALTH, BABY_ARMOR);
        setAttributeBase(Attributes.MOVEMENT_SPEED, GROUND_MOVEMENT_SPEED);
        clampHealthToMax();
    }

    @Override
    protected float getBabyHitboxScale() {
        return 0.30F;
    }

    @Override
    public void ageBoundaryReached() {
        super.ageBoundaryReached();
        applyConfiguredAttributes();
        refreshDimensions();
    }

    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case ABILITY_USE, ABILITY_STOP, DOUBLE_TAP_W -> true;
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
                && isGroundedForAction()
                && !isInWaterOrBubble()
                && !isOrderedToSit()
                && !isInSitTransition()
                && !isSleeping()
                && !isSleepTransitioning()
                && !isTamingStunned()
                && getActiveAbility() == null;
    }

    @Override
    protected void playRiderFlex(ServerPlayer player, RiderFlexSpec spec) {
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        getSoundHandler().playVocal("atroxiia_flex");
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel serverLevel) {
            DragonDestructionManager.applyPassiveTreeDestruction(serverLevel, this);
        }
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        return new PathNavigateGround(this, level);
    }

    @Override
    protected Brain.Provider<Atroxiia> brainProvider() {
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
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        InteractionResult handlerResult = interactionHandler.handleInteraction(player, hand);
        if (handlerResult != InteractionResult.PASS) {
            return handlerResult;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        if (areRiderControlsLocked()) {
            return Vec3.ZERO;
        }

        Vec3 input = riderController.getRiddenInput(player, deltaIn);
        if (!level().isClientSide) {
            float forward = (float) Math.max(-1.0D, Math.min(1.0D, input.z));
            float strafe = (float) Math.max(-1.0D, Math.min(1.0D, input.x));
            setLastRiderForward(Math.abs(forward) > 0.02F ? forward : 0.0F);
            setLastRiderStrafe(Math.abs(strafe) > 0.02F ? strafe : 0.0F);
        }
        return input;
    }

    @Override
    public float getRiddenSpeed(@NotNull Player rider) {
        return riderController.getRiddenSpeed(rider);
    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        riderController.tickRidden(player, travelVector);
    }

    @Override
    protected void applyRiderVerticalInput(Player player, boolean goingUp, boolean goingDown, boolean locked) {
        if (locked || !isInWaterOrBubble()) {
            setGoingUp(false);
            setGoingDown(false);
            return;
        }
        setGoingUp(goingUp);
        setGoingDown(goingDown);
    }

    @Override
    public boolean isRiderPitchKeyMode() {
        return this.entityData.get(DATA_PITCH_KEY_MODE);
    }

    @Override
    public void setRiderPitchKeyMode(boolean enabled) {
        this.entityData.set(DATA_PITCH_KEY_MODE, enabled);
    }

    @Override
    public void travel(@NotNull Vec3 motion) {
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }
            if (isSlithering()) {
                travelStandardRiddenGround(player, Vec3.ZERO, riderController.getRiddenSpeed(player));
                if (slitherMovement.isActive()) {
                    slitherMovement.steerHorizontal(DragonMotionMath.horizontalForward(getYRot()));
                    slitherMovement.applyContinuousTravelMotion();
                } else {
                    Vec3 current = getDeltaMovement();
                    setDeltaMovement(0.0D, current.y, 0.0D);
                }
                return;
            }
            Vec3 input = getRiddenInput(player, motion);
            if (isInWaterOrBubble()) {
                riderController.handleRiddenSwimming(player, input);
            } else {
                travelStandardRiddenGround(player, input, riderController.getRiddenSpeed(player));
                applyPreciseStrikeNudgeMotion();
            }
            return;
        }

        super.travel(motion);
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
    public @Nullable LivingEntity getControllingPassenger() {
        return riderController.getControllingPassenger();
    }

    public boolean useRightMeleeSide() {
        boolean rightSide = nextMeleeRightSide;
        nextMeleeRightSide = !nextMeleeRightSide;
        return rightSide;
    }

    public boolean canUseGroundCombatAbility() {
        if (isBaby() || isTamingStunned() || isInWaterOrBubble() || !isGroundedForAction()) {
            return false;
        }
        if (getControllingPassenger() != null) {
            return true;
        }
        LivingEntity target = getTarget();
        return !isVehicle()
                && !isPassenger()
                && target != null
                && isTargetValid(target)
                && canTarget(target);
    }

    @Override
    protected void onRiderDash(Player player) {
        if (isSlithering() || !canUseGroundCombatAbility()) {
            return;
        }
        useRidingAbility(ModAbilities.ATROXIIA_SLITHER.getName());
    }

    public boolean isSlithering() {
        return this.entityData.get(DATA_SLITHERING);
    }

    public void startSlitherAnimation() {
        this.entityData.set(DATA_SLITHERING, true);
        this.getNavigation().stop();
        setLastRiderForward(0.0F);
        setLastRiderStrafe(0.0F);
        triggerAnim(AtroxiiaAnimationHandler.MOVEMENT_CONTROLLER, "slither");
    }

    public void startSlitherNudge(int durationTicks) {
        double speed = SLITHER_NUDGE_DISTANCE / Math.max(1, durationTicks);
        setSlitherMovement(speed, durationTicks);
    }

    public void startSlitherAutoMove(int durationTicks) {
        setSlitherMovement(SLITHER_AUTO_MOVE_SPEED, durationTicks);
    }

    private void setSlitherMovement(double speed, int durationTicks) {
        if (level().isClientSide || !isSlithering()) {
            return;
        }
        Vec3 velocity = DragonMotionMath.horizontalForward(getYRot()).scale(speed);
        if (slitherMovement.isActive()) {
            slitherMovement.updateContinuous(velocity, durationTicks);
        } else {
            slitherMovement.startContinuous(velocity, durationTicks);
        }
    }

    public void stopSlitherMovement() {
        slitherMovement.cancelActive();
        Vec3 current = getDeltaMovement();
        setDeltaMovement(0.0D, current.y, 0.0D);
        hasImpulse = true;
        hurtMarked = true;
    }

    public void finishSlitherAnimation() {
        stopSlitherMovement();
        this.entityData.set(DATA_SLITHERING, false);
    }

    private Vec3 getSlitherMovementVelocity() {
        return new Vec3(
                this.entityData.get(DATA_SLITHER_MOVEMENT_X),
                0.0D,
                this.entityData.get(DATA_SLITHER_MOVEMENT_Z)
        );
    }

    private void setSlitherMovementVelocity(Vec3 velocity) {
        this.entityData.set(DATA_SLITHER_MOVEMENT_X, (float) velocity.x);
        this.entityData.set(DATA_SLITHER_MOVEMENT_Z, (float) velocity.z);
    }

    private void clearSlitherMovementState() {
        this.entityData.set(DATA_SLITHER_MOVEMENT_TICKS, 0);
        this.entityData.set(DATA_SLITHER_MOVEMENT_X, 0.0F);
        this.entityData.set(DATA_SLITHER_MOVEMENT_Z, 0.0F);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        if (isInWaterOrBubble()) {
            return ModAbilities.ATROXIIA_UNDERWATER_BITE;
        }
        return getMeleeMode() == 0
                ? ModAbilities.ATROXIIA_SLAM
                : ModAbilities.ATROXIIA_SWIPE;
    }

    @Override
    public RiderAbilityBinding getPrimaryRiderAbility() {
        return new RiderAbilityBinding(ModAbilities.ATROXIIA_DEVASTATING_SWEEP.getName(), RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getAttackRiderAbility() {
        if (isInWaterOrBubble()) {
            return new RiderAbilityBinding(
                    ModAbilities.ATROXIIA_UNDERWATER_BITE.getName(),
                    RiderAbilityBinding.Activation.PRESS
            );
        }
        String abilityId = getMeleeMode() == 0
                ? ModAbilities.ATROXIIA_SLAM.getName()
                : ModAbilities.ATROXIIA_SWIPE.getName();
        return new RiderAbilityBinding(abilityId, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getSecondaryRiderAbility() {
        return new RiderAbilityBinding(ModAbilities.ATROXIIA_HELHEIM_QUAKE.getName(), RiderAbilityBinding.Activation.HOLD);
    }

    @Override
    public RiderAbilityBinding getTertiaryRiderAbility() {
        return new RiderAbilityBinding(ModAbilities.ATROXIIA_GUNGNIR_STAB.getName(), RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    protected boolean tryReleaseHeldRidingAbility(String abilityName) {
        if (ModAbilities.ATROXIIA_HELHEIM_QUAKE.getName().equals(abilityName)) {
            var active = combatManager.getActiveAbility();
            if (active != null && active.getAbilityType() == ModAbilities.ATROXIIA_HELHEIM_QUAKE) {
                ((AtroxiiaHelheimQuakeAbility) active).requestRelease();
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean handleCustomRiderAction(ServerPlayer player, DragonRiderAction action,
                                              String abilityName, boolean locked) {
        if (action == DragonRiderAction.DOUBLE_TAP_W) {
            if (!locked) {
                onRiderDash(player);
            }
            return true;
        }
        if (action == DragonRiderAction.ABILITY_USE
                && ModAbilities.ATROXIIA_HELHEIM_QUAKE.getName().equals(abilityName)) {
            var active = combatManager.getActiveAbility();
            if (active != null && active.getAbilityType() == ModAbilities.ATROXIIA_HELHEIM_QUAKE) {
                ((AtroxiiaHelheimQuakeAbility) active).requestChain();
                return true;
            }
        }
        if (action == DragonRiderAction.ABILITY_STOP && tryReleaseHeldRidingAbility(abilityName)) {
            return true;
        }
        return super.handleCustomRiderAction(player, action, abilityName, locked);
    }

    @Override
    protected boolean isRidingAbilityAllowed(DragonAbilityType<?, ?> abilityType) {
        if (isInWaterOrBubble()) {
            return abilityType == ModAbilities.ATROXIIA_UNDERWATER_BITE;
        }
        return abilityType == ModAbilities.ATROXIIA_SLAM
                || abilityType == ModAbilities.ATROXIIA_SWIPE
                || abilityType == ModAbilities.ATROXIIA_GUNGNIR_STAB
                || abilityType == ModAbilities.ATROXIIA_PRECISE_STRIKE
                || abilityType == ModAbilities.ATROXIIA_DEVASTATING_SWEEP
                || abilityType == ModAbilities.ATROXIIA_HELHEIM_QUAKE
                || abilityType == ModAbilities.ATROXIIA_SLITHER;
    }

    @Override
    protected void onRiderToggleMelee(Player player) {
        if (!isInWaterOrBubble()) {
            super.onRiderToggleMelee(player);
        }
    }

    @Override
    public boolean hasSecondaryMelee() {
        return !isInWaterOrBubble();
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
    public void aiStep() {
        super.aiStep();
        screenShakeComponent.tick();
        tickRiderControlLock();
        tickPreciseStrikeNudge();
        tickSwimPitchVisual();
        if (!level().isClientSide) {
            slitherMovement.tickServerState();
            tamingController.tickServer();
            if (isTamingStunned()) {
                tamingController.enforceGroundingTick();
            }
            tickFeedingCooldown();
            tickAtroxiiaAnimationStates();
            handleAmbientSounds();
        }
    }

    private void handleAmbientSounds() {
        if (isDying()
                || isBaby()
                || getTarget() != null
                || getActiveAbility() != null
                || isTamingStunned()
                || isSleeping()
                || isSleepTransitioning()
                || isInSitTransition()
                || areRiderControlsLocked()) {
            return;
        }
        tickAmbientVocalSounds(MIN_AMBIENT_DELAY, MAX_AMBIENT_DELAY, this::selectAmbientGrumble);
    }

    private String selectAmbientGrumble() {
        return selectWeightedAmbientVocal("grumble1", 1.0F / 3.0F,
                "grumble2", 2.0F / 3.0F, "grumble3");
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float amount) {
        if (isDying()) {
            return false;
        }
        if (tamingController.tryEnterHoldStateFromDamage(damageSource, amount)) {
            return true;
        }
        return super.hurt(damageSource, amount);
    }

    @Override
    protected void dropAdditionalDeathLootAfterBase(@NotNull DamageSource source) {
        if (!level().isClientSide && getGender() == DragonGender.FEMALE) {
            DragonLootTables.dropEntityLoot(this, DragonLootTables.ATROXIIA_FEMALE_DEATH, source);
        }
    }

    @Override
    public boolean supportsSleep() {
        return true;
    }

    @Override
    public boolean isSleepSuppressed() {
        return super.isSleepSuppressed() || getTarget() != null || isVehicle() || isTamingStunned();
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
        return SIT_DOWN_TICKS;
    }

    @Override
    protected int getSleepSitUpDuration() {
        return SIT_UP_TICKS;
    }

    @Override
    protected int getSleepFallAsleepDuration() {
        return FALL_ASLEEP_TICKS;
    }

    @Override
    protected int getSleepWakeUpDuration() {
        return WAKE_UP_TICKS;
    }

    @Override
    protected int getSleepExitSuppressionTicks() {
        return 0;
    }

    @Override
    protected int getSleepWakeUpSuppressionTicks() {
        return 0;
    }

    @Override
    protected boolean isAlreadySeatedForSleep() {
        return isOrderedToSit() || shouldStaySeatedCommand() || getSitProgress() >= maxSitTicks();
    }

    @Override
    protected boolean shouldStaySeatedAfterWake(int sleepCommandSnapshot) {
        return isTame() && sleepCommandSnapshot == 1;
    }

    @Override
    protected void onSleepFreezeTick() {
        super.onSleepFreezeTick();
        if (!isSleepingExiting()) {
            setOrderedToSit(true);
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
    protected void onSleepExitStarted() {
        setOrderedToSit(true);
    }

    @Override
    protected void onSleepExitSeated() {
        setOrderedToSit(true);
        setSitProgress(Math.max(getSitProgress(), maxSitTicks()));
        setGroundMoveStateFromAI(0);
    }

    @Override
    public DragonEntity.DragonSleepPreferences getSleepPreferences() {
        return DragonEntity.DragonSleepPreferences.NOCTURNAL();
    }

    @Override
    public boolean canSleepNow() {
        return !DragonEntity.DragonSleepPreferences.isNaturalDay(level());
    }

    @Override
    public float maxSitTicks() {
        return SIT_DOWN_TICKS;
    }

    @Override
    public void setOrderedToSit(boolean sitting) {
        boolean wasSitting = isOrderedToSit();
        super.setOrderedToSit(sitting);
        if (level().isClientSide || wasSitting == sitting || isSleeping() || isSleepTransitioning()) {
            return;
        }
        setGroundMoveStateFromAI(0);
    }

    private void tickAtroxiiaAnimationStates() {
        if (isInWaterOrBubble()) {
            clearSitTransitionFlags();
            if (getSitProgress() != 0.0F || getPrevSitProgress() != 0.0F) {
                clearSitProgress();
            }
            return;
        }

        tickSitTransition(SIT_DOWN_TICKS, SIT_UP_TICKS,
                animationHandler::triggerSitDownAnimation,
                animationHandler::triggerSitUpAnimation);
    }

    public void beginPreciseStrikeNudge(int durationTicks, double distanceBlocks) {
        double perTickSpeed = DragonMotionMath.speedForIntegratedDistance(
                distanceBlocks,
                PRECISE_STRIKE_NUDGE_DRAG,
                durationTicks
        );
        if (perTickSpeed <= 0.0D) {
            return;
        }
        Vec3 nudgeVector = DragonMotionMath.horizontalForward(getYRot()).scale(perTickSpeed);
        this.entityData.set(DATA_PRECISE_STRIKE_NUDGE_X, (float) nudgeVector.x);
        this.entityData.set(DATA_PRECISE_STRIKE_NUDGE_Z, (float) nudgeVector.z);
        this.entityData.set(DATA_PRECISE_STRIKE_NUDGE_TICKS, Math.max(1, durationTicks));
        if (isVehicle()) {
            setDeltaMovement(getDeltaMovement().add(nudgeVector.x, 0.0D, nudgeVector.z));
            hasImpulse = true;
            hurtMarked = true;
        } else {
            getAIMovement().stopAndClearAllMovement();
        }
    }

    private void tickSwimPitchVisual() {
        if (level().isClientSide) {
            previousClientSwimPitchRad = clientSwimPitchRad;
            clientSwimPitchRad = Mth.lerp(0.5F, clientSwimPitchRad, this.entityData.get(DATA_SWIM_PITCH_RAD));
            return;
        }

        previousSwimPitchRad = swimPitchRad;
        float targetPitchRad = 0.0F;
        if (isInWaterOrBubble()) {
            if (isVehicle() && getControllingPassenger() instanceof Player player) {
                boolean hasMovementInput = Math.abs(getLastRiderForward()) > 0.01F
                        || Math.abs(getLastRiderStrafe()) > 0.01F;
                targetPitchRad = DragonRiderControllerHelper.resolveRiderSwimVisualPitchRadians(
                        this,
                        player,
                        RIDER_KEY_PITCH_DEG,
                        hasMovementInput
                );
            } else if (!isVehicle()) {
                Vec3 velocity = getDeltaMovement();
                double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                if (horizontalSpeed > 0.05D) {
                    targetPitchRad = Mth.clamp(
                            (float) Math.atan2(velocity.y, horizontalSpeed),
                            -Mth.HALF_PI,
                            Mth.HALF_PI
                    );
                }
            }
        }

        swimPitchRad = Mth.lerp(0.35F, swimPitchRad, targetPitchRad);
        if (Math.abs(swimPitchRad) < 0.001F) {
            swimPitchRad = 0.0F;
        }
        this.entityData.set(DATA_SWIM_PITCH_RAD, swimPitchRad);
    }

    public float getSwimPitchRadians(float partialTick) {
        if (level().isClientSide) {
            return Mth.lerp(partialTick, previousClientSwimPitchRad, clientSwimPitchRad);
        }
        return Mth.lerp(partialTick, previousSwimPitchRad, swimPitchRad);
    }

    public void steerPreciseStrikeNudge(Vec3 direction) {
        if (level().isClientSide || getPreciseStrikeNudgeTicks() <= 0) {
            return;
        }

        Vec3 horizontalDirection = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontalDirection.lengthSqr() < 1.0E-6D) {
            return;
        }

        double nudgeX = this.entityData.get(DATA_PRECISE_STRIKE_NUDGE_X);
        double nudgeZ = this.entityData.get(DATA_PRECISE_STRIKE_NUDGE_Z);
        double nudgeSpeed = Math.sqrt(nudgeX * nudgeX + nudgeZ * nudgeZ);
        if (nudgeSpeed < 1.0E-6D) {
            return;
        }

        Vec3 steered = horizontalDirection.normalize().scale(nudgeSpeed);
        this.entityData.set(DATA_PRECISE_STRIKE_NUDGE_X, (float) steered.x);
        this.entityData.set(DATA_PRECISE_STRIKE_NUDGE_Z, (float) steered.z);
    }

    private void tickPreciseStrikeNudge() {
        if (level().isClientSide) {
            return;
        }
        int ticks = getPreciseStrikeNudgeTicks();
        if (ticks <= 0) {
            return;
        }
        if (!isVehicle()) {
            if (!combatManager.isAbilityActive(ModAbilities.ATROXIIA_PRECISE_STRIKE)
                    && !combatManager.isAbilityActive(ModAbilities.ATROXIIA_GUNGNIR_STAB)) {
                clearPreciseStrikeNudge();
                return;
            }
            applyAiPreciseStrikeNudgeMotion();
        }
        this.entityData.set(DATA_PRECISE_STRIKE_NUDGE_TICKS, ticks - 1);
        if (ticks - 1 <= 0) {
            this.entityData.set(DATA_PRECISE_STRIKE_NUDGE_X, 0.0F);
            this.entityData.set(DATA_PRECISE_STRIKE_NUDGE_Z, 0.0F);
        }
    }

    private void applyPreciseStrikeNudgeMotion() {
        int ticks = getPreciseStrikeNudgeTicks();
        if (ticks <= 0) {
            return;
        }
        Vec3 nudgeDelta = new Vec3(
                this.entityData.get(DATA_PRECISE_STRIKE_NUDGE_X),
                0.0D,
                this.entityData.get(DATA_PRECISE_STRIKE_NUDGE_Z)
        );
        move(MoverType.SELF, nudgeDelta);
        setDeltaMovement(getDeltaMovement().add(nudgeDelta.x, 0.0D, nudgeDelta.z));
        hasImpulse = true;
        hurtMarked = true;
    }

    private void applyAiPreciseStrikeNudgeMotion() {
        Vec3 nudgeDelta = new Vec3(
                this.entityData.get(DATA_PRECISE_STRIKE_NUDGE_X),
                0.0D,
                this.entityData.get(DATA_PRECISE_STRIKE_NUDGE_Z)
        );
        getAIMovement().stopAndClearAllMovement();
        move(MoverType.SELF, nudgeDelta);
        Vec3 current = getDeltaMovement();
        setDeltaMovement(0.0D, current.y, 0.0D);
        hasImpulse = true;
        hurtMarked = true;
    }

    private int getPreciseStrikeNudgeTicks() {
        return this.entityData.get(DATA_PRECISE_STRIKE_NUDGE_TICKS);
    }

    private void clearPreciseStrikeNudge() {
        this.entityData.set(DATA_PRECISE_STRIKE_NUDGE_TICKS, 0);
        this.entityData.set(DATA_PRECISE_STRIKE_NUDGE_X, 0.0F);
        this.entityData.set(DATA_PRECISE_STRIKE_NUDGE_Z, 0.0F);
    }

    private boolean shouldStaySeatedCommand() {
        return isTame() && getCommand() == 1;
    }

    @Override
    protected ResourceLocation getDragonAttributesId() {
        return DragonAttributeConfigLoader.ATROXIIA_ID;
    }

    @Override
    public boolean isWildAggressionEnabled() {
        if (isTame() || isBaby()) {
            return false;
        }
        return DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.ATROXIIA_ID)
                .extraBoolean("aggressive_wild", false);
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
    protected ScreenShakeComponent getScreenShakeComponent() {
        return screenShakeComponent;
    }

    @Override
    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return ModAbilities.ATROXIIA_HURT;
    }

    @Override
    public int getHurtAnimationDurationTicks() {
        return HURT_ANIMATION_TICKS;
    }

    @Override
    protected DragonAbilityType<?, ?> getDeathAbilityType() {
        return ModAbilities.ATROXIIA_DIE;
    }

    @Override
    public int getDeathAnimationDurationTicks() {
        return DEATH_ANIMATION_TICKS;
    }

    public void playEatMovingSound() {
        if (level().isClientSide) {
            return;
        }
        getSoundHandler().playMovingEntitySound(ModSounds.ATROXIIA_EAT.get(), 1.0F, 1.0F, EAT_SOUND_TICKS);
    }

    @Override
    public boolean isFood(@NotNull ItemStack stack) {
        return stack.is(ModTags.Items.ATROXIIA_FOODS);
    }

    public boolean canFeed() {
        return this.entityData.get(DATA_FEEDING_COOLDOWN) <= 0;
    }

    public boolean isTamingStunned() {
        return !isBaby() && this.entityData.get(DATA_TAMING_STUNNED);
    }

    public boolean isReadyForTamingFeed() {
        return tamingController.isReadyForTamingFeed();
    }

    public boolean isBelowTamingThreshold() {
        return getHealth() <= getTamingThreshold();
    }

    public float getTamingThreshold() {
        double configured = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.ATROXIIA_ID)
                .extraDouble("taming_stun_health", DEFAULT_TAMING_STUN_HEALTH);
        return (float) Mth.clamp(configured, 0.0D, getMaxHealth());
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

    public void setFeedingCooldown(int ticks) {
        this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, ticks));
    }

    private void tickFeedingCooldown() {
        int cooldown = this.entityData.get(DATA_FEEDING_COOLDOWN);
        if (cooldown > 0) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, cooldown - 1);
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        saveRideableData(tag);
        tag.putInt("FeedingCooldownTicks", Math.max(0, this.entityData.get(DATA_FEEDING_COOLDOWN)));
        tamingController.save(tag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadRideableData(tag);
        if (tag.contains("FeedingCooldownTicks")) {
            setFeedingCooldown(tag.getInt("FeedingCooldownTicks"));
        }
        tamingController.load(tag);
        applyConfiguredAttributes();
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(@NotNull ServerLevel serverLevel, @NotNull AgeableMob ageableMob) {
        return createBreedOffspring(serverLevel, ageableMob, ModEntities.ATROXIIA.get(), Atroxiia::applyConfiguredAttributes);
    }

    @Override
    public boolean canBreed() {
        return isTame() && !isBaby() && getHealth() >= getMaxHealth() && isInLove();
    }

    public boolean canBeBound() {
        return !isSleeping() && !isDying();
    }

    @Override
    protected Supplier<? extends Block> getEggBlock() {
        return ModBlocks.ATROXIIA_EGG;
    }
}
