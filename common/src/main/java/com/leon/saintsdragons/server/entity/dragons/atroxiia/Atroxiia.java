package com.leon.saintsdragons.server.entity.dragons.atroxiia;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonGroundWanderGoal;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.atroxiia.AtroxiiaHelheimQuakeAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.atroxiia.AtroxiiaPreciseStrikeAbility;
import com.leon.saintsdragons.server.entity.base.RideableGroundDragon;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.component.DragonMotionMath;
import com.leon.saintsdragons.server.entity.component.ScreenShakeComponent;
import com.leon.saintsdragons.server.entity.controller.atroxiia.AtroxiiaRiderController;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.handlers.AtroxiiaAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.handlers.AtroxiiaInteractionHandler;
import com.leon.saintsdragons.server.entity.interfaces.ShakesScreen;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Atroxiia extends RideableGroundDragon implements ShakesScreen {
    private static final EntityDataAccessor<Float> DATA_SCREEN_SHAKE_AMOUNT =
            SynchedEntityData.defineId(Atroxiia.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_PRECISE_STRIKE_NUDGE_TICKS =
            SynchedEntityData.defineId(Atroxiia.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_PRECISE_STRIKE_NUDGE_X =
            SynchedEntityData.defineId(Atroxiia.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PRECISE_STRIKE_NUDGE_Z =
            SynchedEntityData.defineId(Atroxiia.class, EntityDataSerializers.FLOAT);
    private static final int MOVEMENT_TRANSITION_TICKS = 4;
    private static final int SIT_DOWN_TICKS = 48;
    private static final int SIT_UP_TICKS = 25;
    private static final int FALL_ASLEEP_TICKS = 38;
    private static final int WAKE_UP_TICKS = 42;
    private static final double RIDER_JUMP_STRENGTH = 0.75D;
    private static final double RIDER_JUMP_FORWARD_BOOST = 0.7D;
    public static final double RIDER_WALK_SPEED = 0.12D;
    public static final double RIDER_RUN_SPEED = 0.28D;
    private static final double PRECISE_STRIKE_NUDGE_DRAG = 0.78D;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final AtroxiiaRiderController riderController = new AtroxiiaRiderController(this);
    private final AtroxiiaAnimationHandler animationHandler = new AtroxiiaAnimationHandler(this);
    private final AtroxiiaInteractionHandler interactionHandler = new AtroxiiaInteractionHandler(this);
    private final ScreenShakeComponent screenShakeComponent;
    private boolean nextMeleeRightSide = true;

    public Atroxiia(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.screenShakeComponent = new ScreenShakeComponent(this, DATA_SCREEN_SHAKE_AMOUNT, 0.18F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        this.entityData.define(DATA_PRECISE_STRIKE_NUDGE_TICKS, 0);
        this.entityData.define(DATA_PRECISE_STRIKE_NUDGE_X, 0.0F);
        this.entityData.define(DATA_PRECISE_STRIKE_NUDGE_Z, 0.0F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        var movementController = new AnimationController<>(this, AnimationHelper.MOVEMENT_CONTROLLER, MOVEMENT_TRANSITION_TICKS,
                animationHandler::movementPredicate);
        AnimationHelper.registerStepKeyframes(this, movementController);
        animationHandler.setupMovementController(movementController);
        controllers.add(movementController);
    }

    public static AttributeSupplier.Builder createAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.ATROXIIA_ID);
        return TamableAnimal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, config.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D);
    }

    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case ABILITY_USE, ABILITY_STOP -> true;
            default -> super.supportsRiderAction(action);
        };
    }

    public void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(11, new DragonGroundWanderGoal<>(this, 1.0, 100));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));
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
    public void travel(@NotNull Vec3 motion) {
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            travelStandardRiddenGround(player, getRiddenInput(player, motion), riderController.getRiddenSpeed(player));
            applyPreciseStrikeNudgeMotion();
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

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
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
        return new RiderAbilityBinding(ModAbilities.ATROXIIA_PRECISE_STRIKE.getName(), RiderAbilityBinding.Activation.PRESS);
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
        return abilityType == ModAbilities.ATROXIIA_SLAM
                || abilityType == ModAbilities.ATROXIIA_SWIPE
                || abilityType == ModAbilities.ATROXIIA_PRECISE_STRIKE
                || abilityType == ModAbilities.ATROXIIA_DEVASTATING_SWEEP
                || abilityType == ModAbilities.ATROXIIA_HELHEIM_QUAKE;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        screenShakeComponent.tick();
        tickRiderControlLock();
        tickPreciseStrikeNudge();
        if (!level().isClientSide) {
            tickAtroxiiaAnimationStates();
        }
    }

    @Override
    public boolean supportsSleep() {
        return true;
    }

    @Override
    public boolean isSleepSuppressed() {
        return super.isSleepSuppressed() || getTarget() != null || isVehicle();
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
        setDeltaMovement(getDeltaMovement().add(nudgeVector.x, 0.0D, nudgeVector.z));
        hasImpulse = true;
        hurtMarked = true;
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
            clearPreciseStrikeNudge();
            return;
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
    public @Nullable AgeableMob getBreedOffspring(@NotNull ServerLevel serverLevel, @NotNull AgeableMob ageableMob) {
        return null;
    }
}
