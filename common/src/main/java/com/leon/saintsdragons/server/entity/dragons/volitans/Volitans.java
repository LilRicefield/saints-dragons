package com.leon.saintsdragons.server.entity.dragons.volitans;

import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.volitans.VolitansAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtByTargetGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtTargetGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonProtectBabiesGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonFloatGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonFollowOwnerGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonGroundWanderGoal;
import com.leon.saintsdragons.server.ai.goals.volitans.VolitansAirCombatGoal;
import com.leon.saintsdragons.server.ai.goals.volitans.VolitansSlamSequenceGoal;
import com.leon.saintsdragons.server.ai.goals.volitans.VolitansGroundCombatGoal;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansBurrowAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansPoisonBallAbility;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.component.DragonRiderFlightComponent;
import com.leon.saintsdragons.server.entity.component.DragonTakeoffComponent;
import com.leon.saintsdragons.server.entity.controller.volitans.VolitansRiderController;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansSpineEntity;
import com.leon.saintsdragons.server.entity.dragons.volitans.handlers.VolitansAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.volitans.handlers.VolitansInteractionHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.interfaces.ShakesScreen;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import com.leon.saintsdragons.server.entity.interfaces.SoundHandledDragon;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Volitans extends RideableDragonBase implements DragonFlightCapable, SemiAquaticDragon, SoundHandledDragon, ShakesScreen {
    private static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TAKEOFF =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HOVERING =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LANDING =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RUNNING =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_FLIGHT_MODE =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_FLIGHT_PITCH =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RIDER_FORWARD =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIDER_STRAFE =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_GOING_UP =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GOING_DOWN =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ACCELERATING =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);
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
    private static final EntityDataAccessor<Boolean> DATA_BURROWING =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_FEEDING_COOLDOWN =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.INT);

    private static final double RIDER_WALK_SPEED = 0.24D;
    private static final double RIDER_RUN_SPEED = 0.34D;
    private static final double RIDER_BURROW_SPEED = 0.40D;
    private static final double RIDER_SWIM_SPEED = 1.42D;
    private static final double RIDER_FLY_SPEED = 0.38D;
    public static final int TAKEOFF_ANIMATION_TICKS = 30;
    private static final int SIT_DOWN_ANIMATION_TICKS = 50; // animation.volitans.down = 2.5s
    private static final int SIT_UP_ANIMATION_TICKS = 25;   // animation.volitans.up = 1.25s
    private static final int LANDED_CONTROL_LOCK_TICKS = 20;
    private static final int WALK_SOUND_DURATION_TICKS = 60;
    private static final int RUN_SOUND_DURATION_TICKS = 30;
    private static final long WALK_SOUND_REPLAY_INTERVAL_TICKS = 52;
    private static final long RUN_SOUND_REPLAY_INTERVAL_TICKS = 25;
    private static final int RIDER_BACK_DASH_COOLDOWN_TICKS = 30;
    private static final int RIDER_DASH_SOUND_TICKS = 60; // 3.0s
    private static final int RIDER_DODGE_SOUND_TICKS = 60; // 3.0s
    private static final int RIDER_BACK_DASH_LOCK_TICKS = 0;
    private static final double RIDER_BACK_DASH_SPEED = 2.60D;
    private static final double RIDER_BACK_DASH_GROUND_LIFT = 0.0D;
    private static final int RIDER_BACK_DASH_DURATION_TICKS = 8;
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
    private static final int RIDER_FORWARD_DASH_DURATION_TICKS = 25; // 1.25s
    private static final double RIDER_FORWARD_DASH_DISTANCE_BLOCKS = 32.0D;
    private static final double RIDER_FORWARD_DASH_HORIZONTAL_DRAG = 0.90D;
    private static final double RIDER_FORWARD_DASH_VERTICAL_DRAG = 0.95D;
    private static final int RIDER_FORWARD_DASH_DAMAGE_TICK = 14; // late hit near animation end
    private static final float RIDER_FORWARD_DASH_DAMAGE = 16.0F;
    private static final double RIDER_FORWARD_DASH_DAMAGE_RADIUS = 8.0D;
    private static final int RIDER_SIDE_DODGE_DURATION_TICKS = 7;
    private static final double RIDER_SIDE_DODGE_HORIZONTAL_DRAG = 0.90D;
    private static final double RIDER_SIDE_DODGE_VERTICAL_DRAG = 0.95D;
    private static final double RIDER_SIDE_DODGE_DISTANCE_BLOCKS = 10.0D;
    private static final int RIDER_SIDE_DODGE_RECOVERY_TICKS = 5;
    private static final double RIDER_SIDE_DODGE_RECOVERY_DRAG = 0.82D;
    private static final double LANDING_BLEND_ALTITUDE = 8.0D;
    public static final double RIDER_GLIDE_ALTITUDE_THRESHOLD = 40.0D;
    public static final double RIDER_GLIDE_ALTITUDE_EXIT = 30.0D;
    public static final double RIDER_LOW_ALTITUDE_GLIDE_THRESHOLD = 6.0D;
    public static final double RIDER_WATER_SURFACE_LEVEL = 62.0D;
    public static final double RIDER_WATER_SURFACE_TOLERANCE = 2.0D;
    public static final int RIDER_WATER_SCAN_RADIUS = 2;
    public static final int RIDER_WATER_SCAN_DEPTH = 8;
    private static final float SHAKE_DECAY_PER_TICK = 0.045F;
    private static final float BURROW_MOVE_SHAKE_INTENSITY = 0.12F;
    private static final int BURROW_EXIT_TAKEOFF_BLOCK_BUFFER_TICKS = 8;

    private final AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);
    private final VolitansAnimationHandler animationHandler = new VolitansAnimationHandler(this);
    private final VolitansInteractionHandler interactionHandler = new VolitansInteractionHandler(this);
    private final VolitansRiderController riderController;
    private final DragonRiderFlightComponent riderFlightComponent;
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final java.util.Map<String, Vec3> clientLocatorCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, Vec3> serverBonePositionCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final DragonPathNavigateGround groundNav;
    private final FlyingPathNavigation airNav;
    private final DragonTakeoffComponent takeoffComponent;
    private boolean usingAirNav;
    private int timeFlying;
    private int riderTakeoffTicks;
    private boolean riderHighAltitudeGlide;
    private double lastCheckedX;
    private double lastCheckedY;
    private double lastCheckedZ;
    private int ticksSinceLastMovement;
    private float bankSmoothedYaw = 0f;
    private float bankAngle = 0f;
    private float prevBankAngle = 0f;
    private float flightPitchRad = 0f;
    private float prevFlightPitchRad = 0f;
    private float smoothedPlayerPitchRad = 0f;
    private float prevScreenShakeAmount = 0.0F;
    private float screenShakeAmount = 0.0F;
    private int screenShakeHoldTicks = 0;
    private float screenShakeHoldIntensity = 0.0F;
    private int sitTransitionTicks = 0;
    private boolean isSittingDown = false;
    private boolean isStandingUp = false;
    private int riderBackDashCooldownTicks = 0;
    private boolean riderForwardDashing = false;
    private int riderForwardDashTicksLeft = 0;
    private int riderForwardDashTicksElapsed = 0;
    private boolean riderForwardDashDamageApplied = false;
    private Vec3 riderForwardDashVec = Vec3.ZERO;
    private boolean riderBackDashing = false;
    private int riderBackDashTicksLeft = 0;
    private Vec3 riderBackDashVec = Vec3.ZERO;
    private int riderBackDashRecoveryTicks = 0;
    private boolean riderSideDodging = false;
    private int riderSideDodgeTicksLeft = 0;
    private Vec3 riderSideDodgeVec = Vec3.ZERO;
    private int riderSideDodgeRecoveryTicks = 0;
    private int takeoffInputBlockTicks = 0;
    private int aiGroundMobilityCooldownTicks = 0;
    private boolean aiSpecialCombatActive = false;
    private boolean aiSpecialCombatReserved = false;
    private int tempInvulnTicks = 0;

    public Volitans(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setRideable();
        this.setMaxUpStep(1.0F);
        this.riderController = new VolitansRiderController(this);
        this.takeoffComponent = createTakeoffComponent();
        this.riderFlightComponent = createRiderFlightComponent();

        this.groundNav = new DragonPathNavigateGround(this, level);
        this.airNav = new FlyingPathNavigation(this, level);
        this.airNav.setCanOpenDoors(false);
        this.airNav.setCanPassDoors(false);
        this.airNav.setCanFloat(false);
        this.navigation = groundNav;
        this.moveControl = new MoveControl(this);
        this.usingAirNav = false;

        if (!level.isClientSide) {
            applyConfiguredAttributes();
        }
    }

    private DragonTakeoffComponent createTakeoffComponent() {
        return new DragonTakeoffComponent(new DragonTakeoffComponent.Host() {
            @Override
            public Level level() { return Volitans.this.level(); }

            @Override
            public boolean isFlying() { return Volitans.this.isFlying(); }

            @Override
            public void setFlying(boolean value) { Volitans.this.setFlying(value); }

            @Override
            public void setTakeoff(boolean value) { Volitans.this.setTakeoff(value); }

            @Override
            public void setHovering(boolean value) { Volitans.this.setHovering(value); }

            @Override
            public void setLanding(boolean value) { Volitans.this.setLanding(value); }

            @Override
            public void switchToAirNavigation() { Volitans.this.switchToAirNavigation(); }

            @Override
            public Vec3 getDeltaMovement() { return Volitans.this.getDeltaMovement(); }

            @Override
            public void setDeltaMovement(Vec3 movement) { Volitans.this.setDeltaMovement(movement); }

            @Override
            public void markImpulse() { Volitans.this.hasImpulse = true; }

            @Override
            public void onTakeoffStarted() { Volitans.this.timeFlying = 0; }
        });
    }

    private DragonRiderFlightComponent createRiderFlightComponent() {
        return new DragonRiderFlightComponent(new DragonRiderFlightComponent.Host() {
            @Override
            public Entity asEntity() { return Volitans.this; }

            @Override
            public Level level() { return Volitans.this.level(); }

            @Override
            public AABB getBoundingBox() { return Volitans.this.getBoundingBox(); }

            @Override
            public boolean isVehicle() { return Volitans.this.isVehicle(); }

            @Override
            public boolean isFlying() { return Volitans.this.isFlying(); }

            @Override
            public boolean isTakeoff() { return Volitans.this.isTakeoff(); }

            @Override
            public boolean isGoingUp() { return Volitans.this.isGoingUp(); }

            @Override
            public boolean isUnderWater() { return Volitans.this.isUnderWater(); }

            @Override
            public boolean isInWaterOrBubble() { return Volitans.this.isInWaterOrBubble(); }

            @Override
            public boolean isTame() { return Volitans.this.isTame(); }

            @Override
            public boolean hasControllingRider() { return riderController.getRidingPlayer() != null; }

            @Override
            public boolean canTakeoff() { return Volitans.this.canTakeoff(); }

            @Override
            public void setFlying(boolean value) { Volitans.this.setFlying(value); }

            @Override
            public void setHovering(boolean value) { Volitans.this.setHovering(value); }

            @Override
            public void setLanding(boolean value) { Volitans.this.setLanding(value); }

            @Override
            public void switchToAirNavigation() { Volitans.this.switchToAirNavigation(); }

            @Override
            public void setGoingUp(boolean value) { Volitans.this.setGoingUp(value); }

            @Override
            public void setGoingDown(boolean value) { Volitans.this.setGoingDown(value); }

            @Override
            public void stopNavigation() { Volitans.this.getNavigation().stop(); }

            @Override
            public void startTakeoffSequence(double minUpwardVelocity, int animationTicks) {
                Volitans.this.startTakeoffSequence(minUpwardVelocity, animationTicks);
            }

            @Override
            public Vec3 getDeltaMovement() { return Volitans.this.getDeltaMovement(); }

            @Override
            public void setDeltaMovement(Vec3 movement) { Volitans.this.setDeltaMovement(movement); }

            @Override
            public void markImpulse() { Volitans.this.hasImpulse = true; }

            @Override
            public long getGameTime() { return Volitans.this.level().getGameTime(); }

            @Override
            public void setRiderTakeoffTicks(int ticks) { Volitans.this.riderTakeoffTicks = Math.max(0, ticks); }
        }, new DragonRiderFlightComponent.Config(
                true,
                0,
                0.12D,
                TAKEOFF_ANIMATION_TICKS,
                0.12D,
                TAKEOFF_ANIMATION_TICKS
        ));
    }

    private void startTakeoffSequence(double minUpwardVelocity, int animationTicks) {
        takeoffComponent.startTakeoff(animationTicks, minUpwardVelocity);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 90.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FLYING_SPEED, RIDER_FLY_SPEED)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    public static boolean canSpawnHere(EntityType<? extends Volitans> type,
                                       LevelAccessor level,
                                       MobSpawnType spawnType,
                                       net.minecraft.core.BlockPos pos,
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
        return below.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP);
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, BlockState state) {
        if (this.level().isClientSide) {
            return;
        }
        if (isFlying() || isTakeoff() || isLanding() || isHovering() || isInWaterOrBubble() || isBurrowing()) {
            return;
        }

        long now = this.level().getGameTime();
        boolean running = getGroundMoveState() == 2;
        long minIntervalTicks = running ? RUN_SOUND_REPLAY_INTERVAL_TICKS : WALK_SOUND_REPLAY_INTERVAL_TICKS;
        if (now - getSoundHandler().getLastStepTick() < minIntervalTicks) {
            return;
        }
        getSoundHandler().setLastStepTick(now);

        if (running) {
            getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_RUN.get(), 1.0f, 1.0f, RUN_SOUND_DURATION_TICKS);
        } else {
            getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_WALK.get(), 1.0f, 1.0f, WALK_SOUND_DURATION_TICKS);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new DragonFloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        if (!this.isBaby()) {
            this.goalSelector.addGoal(2, new VolitansSlamSequenceGoal(this));
            this.goalSelector.addGoal(3, new VolitansGroundCombatGoal(this));
            this.goalSelector.addGoal(4, new VolitansAirCombatGoal(this));
        }
        this.goalSelector.addGoal(5, new DragonFollowOwnerGoal<>(this, DragonFollowOwnerGoal.FollowConfig.forCindervane()));
        this.goalSelector.addGoal(6, new DragonGroundWanderGoal<>(this, 0.9D, 70));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        if (!this.isBaby()) {
            this.targetSelector.addGoal(1, new DragonOwnerHurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new DragonOwnerHurtTargetGoal(this));
            this.targetSelector.addGoal(3, new DragonProtectBabiesGoal<>(this, Volitans.class));
            this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
            this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                    target -> shouldAggroOnSight()));
        }
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        return new DragonPathNavigateGround(this, level);
    }

    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case TOGGLE_PITCH_MODE, ABILITY_USE, ABILITY_STOP, DOUBLE_TAP_W, DOUBLE_TAP_A, DOUBLE_TAP_D, DOUBLE_TAP_S -> true;
            default -> super.supportsRiderAction(action);
        };
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
        // During ultimate slam, preserve ability-driven vertical flags.
        if (locked && isUltimateSlamActive()) {
            return;
        }
        super.applyRiderVerticalInput(player, goingUp, goingDown, locked);
    }

    @Override
    protected void defineRideableDragonData() {
        this.entityData.define(DATA_FLYING, false);
        this.entityData.define(DATA_TAKEOFF, false);
        this.entityData.define(DATA_HOVERING, false);
        this.entityData.define(DATA_LANDING, false);
        this.entityData.define(DATA_RUNNING, false);
        this.entityData.define(DATA_FLIGHT_MODE, -1);
        this.entityData.define(DATA_FLIGHT_PITCH, 0.0F);
        this.entityData.define(DATA_GROUND_MOVE_STATE, 0);
        this.entityData.define(DATA_RIDER_FORWARD, 0.0F);
        this.entityData.define(DATA_RIDER_STRAFE, 0.0F);
        this.entityData.define(DATA_GOING_UP, false);
        this.entityData.define(DATA_GOING_DOWN, false);
        this.entityData.define(DATA_ACCELERATING, false);
        this.entityData.define(DATA_PITCH_KEY_MODE, false);
        this.entityData.define(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        this.entityData.define(DATA_ULTIMATE_SLAM_ACTIVE, false);
        this.entityData.define(DATA_BREATH_MODE, 0); // 0=water, 1=poison
        this.entityData.define(DATA_BREATHING, false);
        this.entityData.define(DATA_BURROWING, false);
        this.entityData.define(DATA_FEEDING_COOLDOWN, 0);
    }

    @Override
    protected EntityDataAccessor<Float> getRiderForwardAccessor() {
        return DATA_RIDER_FORWARD;
    }

    @Override
    protected EntityDataAccessor<Float> getRiderStrafeAccessor() {
        return DATA_RIDER_STRAFE;
    }

    @Override
    protected EntityDataAccessor<Integer> getGroundMoveStateAccessor() {
        return DATA_GROUND_MOVE_STATE;
    }

    @Override
    protected EntityDataAccessor<Integer> getFlightModeAccessor() {
        return DATA_FLIGHT_MODE;
    }

    @Override
    protected EntityDataAccessor<Boolean> getGoingUpAccessor() {
        return DATA_GOING_UP;
    }

    @Override
    protected EntityDataAccessor<Boolean> getGoingDownAccessor() {
        return DATA_GOING_DOWN;
    }

    @Override
    protected EntityDataAccessor<Boolean> getAcceleratingAccessor() {
        return DATA_ACCELERATING;
    }

    @Override
    protected void applyLoadedFlightState(boolean flying, boolean takeoff, boolean hovering, boolean landing) {
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

    @Override
    protected int getFlightMode() {
        if (!isFlying()) {
            riderHighAltitudeGlide = false;
            return -1;
        }
        if (isTakeoff()) {
            return 3;
        }

        if (isLanding()) {
            return 2;
        }

        if (isRiddenByOwner()) {
            double deltaX = getX() - lastCheckedX;
            double deltaY = getY() - lastCheckedY;
            double deltaZ = getZ() - lastCheckedZ;
            double positionChangeSqr = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;

            boolean movingIntent = isGoingUp() || isGoingDown() || isAccelerating();

            if (positionChangeSqr > 0.0001D || movingIntent) {
                ticksSinceLastMovement = 0;
                lastCheckedX = getX();
                lastCheckedY = getY();
                lastCheckedZ = getZ();
            } else {
                ticksSinceLastMovement++;
            }

            if (ticksSinceLastMovement > 3) {
                return 5; // fly_idle
            }
            if (isAccelerating()) {
                return 4; // sprint_flap
            }

            double altitude = getY() - level().getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (int) getX(),
                    (int) getZ()
            );
            Vec3 velocity = getDeltaMovement();
            boolean ascending = velocity.y > 0.02D;
            boolean riderAscending = isVehicle() && isGoingUp();

            if (shouldForceSurfaceGlide(altitude)) {
                riderHighAltitudeGlide = false;
                return 0;
            }
            if (ascending || riderAscending) {
                return 1; // flap
            }
            if (riderHighAltitudeGlide) {
                if (altitude > RIDER_GLIDE_ALTITUDE_EXIT) {
                    return 0;
                }
                riderHighAltitudeGlide = false;
            } else if (altitude > RIDER_GLIDE_ALTITUDE_THRESHOLD) {
                riderHighAltitudeGlide = true;
                return 0;
            }
            return 1; // flap
        }
        if (isHovering()) {
            return 2;
        }

        riderHighAltitudeGlide = false;

        Vec3 velocity = getDeltaMovement();
        double horizontalSpeedSqr = velocity.horizontalDistanceSqr();
        double yDelta = getY() - yo;
        if (horizontalSpeedSqr < 0.01D && Math.abs(yDelta) < 0.1D) {
            return 5; // fly_idle
        }

        boolean ascending = velocity.y > 0.02D;
        boolean riderAscending = isVehicle() && isGoingUp();
        if (ascending || riderAscending) {
            return 1;
        }

        double altitude = getY() - level().getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) getX(),
                (int) getZ()
        );
        return altitude > 35.0D ? 0 : 1;
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
    public boolean isLanding() {
        return this.entityData.get(DATA_LANDING);
    }

    @Override
    public boolean isHovering() {
        return this.entityData.get(DATA_HOVERING);
    }

    @Override
    public boolean isRunning() {
        return this.entityData.get(DATA_RUNNING);
    }

    @Override
    public void setRunning(boolean running) {
        this.entityData.set(DATA_RUNNING, running);
    }

    @Override
    public void setFlying(boolean flying) {
        this.entityData.set(DATA_FLYING, flying);
        if (!flying) {
            takeoffComponent.clear();
            this.entityData.set(DATA_TAKEOFF, false);
            this.entityData.set(DATA_HOVERING, false);
            this.entityData.set(DATA_LANDING, false);
        }
    }

    @Override
    public void setTakeoff(boolean takeoff) {
        boolean wasTakeoff = this.entityData.get(DATA_TAKEOFF);
        this.entityData.set(DATA_TAKEOFF, takeoff);
        if (!takeoff && takeoffComponent.isActive()) {
            takeoffComponent.clear();
            return;
        }
        if (takeoff && !wasTakeoff && !level().isClientSide) {
            getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_TAKEOFF.get(), 2.0f, 1.0f, 40);
        }
    }

    @Override
    public void setHovering(boolean hovering) {
        this.entityData.set(DATA_HOVERING, hovering);
    }

    @Override
    public void setLanding(boolean landing) {
        this.entityData.set(DATA_LANDING, landing);
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
        return 18.0D;
    }

    @Override
    public boolean canTakeoff() {
        if (this.isBaby() || !this.isAlive()) {
            return false;
        }
        return this.onGround() || (this.isInWaterOrBubble() && !this.isUnderWater());
    }

    @Override
    public void markLandedNow() {
        setFlying(false);
        setTakeoff(false);
        setLanding(false);
        setHovering(false);
    }

    @Override
    protected void onRiderTakeoffRequest(Player player) {
        if (shouldSuppressTakeoffInput()) {
            return;
        }
        if (!this.isInWaterOrBubble()) {
            clearGroundMobilityState();
        }
        riderFlightComponent.requestRiderTakeoff();
    }

    @Override
    protected void onRiderAbilityUse(Player player, String abilityName) {
        if (isBurrowing()) {
            if (VolitansAbilities.VOLITANS_BURROW_ID.equals(abilityName)) {
                var active = combatManager.getActiveAbility();
                if (active instanceof VolitansBurrowAbility burrowAbility
                        && active.getAbilityType() == VolitansAbilities.VOLITANS_BURROW) {
                    burrowAbility.requestExit(true);
                } else {
                    // Fail-safe for save/rejoin or interrupted ability state: never trap rider in burrow visuals.
                    setBurrowing(false);
                }
            }
            return;
        }
        if (abilityName != null && !abilityName.isEmpty()) {
            if (VolitansAbilities.VOLITANS_BURROW_ID.equals(abilityName)) {
                var active = combatManager.getActiveAbility();
                if (active instanceof VolitansBurrowAbility burrowAbility
                        && active.getAbilityType() == VolitansAbilities.VOLITANS_BURROW) {
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
    protected void onRiderAbilityStop(Player player, String abilityName) {
        if (isBurrowing()) {
            return;
        }
        if (abilityName != null && !abilityName.isEmpty()) {
            if (VolitansAbilities.VOLITANS_POISON_BALL_ID.equals(abilityName)) {
                var active = combatManager.getActiveAbility();
                if (active != null && active.getAbilityType() == VolitansAbilities.VOLITANS_POISON_BALL) {
                    ((VolitansPoisonBallAbility) active).requestRelease();
                    return;
                }
            }
            forceEndActiveAbility();
        }
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
        if (action == DragonRiderAction.TOGGLE_PITCH_MODE) {
            setRiderPitchKeyMode(!isRiderPitchKeyMode());
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

        if (action == DragonRiderAction.TOGGLE_MELEE && !locked) {
            if (combatManager.isAbilityActive(VolitansAbilities.VOLITANS_BREATH)) {
                toggleBreathMode();
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
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
        if (isBurrowing() || riderBackDashCooldownTicks > 0 || riderForwardDashing
                || riderBackDashing || riderBackDashRecoveryTicks > 0
                || riderSideDodging || riderSideDodgeRecoveryTicks > 0
                || !isAlive() || isDying() || isOrderedToSit() || isInSitTransition()) {
            return;
        }

        Vec3 forward = getRiderDashForwardVector();
        Vec3 backward = new Vec3(-forward.x, 0.0D, -forward.z).normalize();
        Vec3 launch = new Vec3(backward.x * RIDER_BACK_DASH_SPEED, getDeltaMovement().y, backward.z * RIDER_BACK_DASH_SPEED);
        if (onGround() && !isFlying() && !isInWaterOrBubble()) {
            launch = launch.add(0.0D, RIDER_BACK_DASH_GROUND_LIFT, 0.0D);
        }

        beginRiderBackDash(launch);
        if (RIDER_BACK_DASH_LOCK_TICKS > 0) {
            lockRiderControls(RIDER_BACK_DASH_LOCK_TICKS);
        }
        riderBackDashCooldownTicks = RIDER_BACK_DASH_COOLDOWN_TICKS;
        triggerAnim("instant", "dash_backwards");
        playBackwardDashSound();
        fireBackwardDashSpikes();
    }

    @Override
    protected void onRiderDash(Player player) {
        if (isBurrowing() || isFlying() || isInWaterOrBubble() || riderBackDashCooldownTicks > 0
                || riderForwardDashing || riderBackDashing || riderBackDashRecoveryTicks > 0
                || riderSideDodging || riderSideDodgeRecoveryTicks > 0
                || !isAlive() || isDying() || isOrderedToSit() || isInSitTransition()) {
            return;
        }

        float yawRad = (float) Math.toRadians(this.getYRot());
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double dragScale = 1.0D - Math.pow(RIDER_FORWARD_DASH_HORIZONTAL_DRAG, RIDER_FORWARD_DASH_DURATION_TICKS);
        if (dragScale <= 1.0E-6D) {
            return;
        }
        double perTickSpeed = RIDER_FORWARD_DASH_DISTANCE_BLOCKS
                * (1.0D - RIDER_FORWARD_DASH_HORIZONTAL_DRAG) / dragScale;

        Vec3 dashVec = new Vec3(forwardX * perTickSpeed, 0.0D, forwardZ * perTickSpeed);
        beginRiderForwardDash(dashVec);
        riderBackDashCooldownTicks = RIDER_BACK_DASH_COOLDOWN_TICKS;
        triggerAnim("instant", "dash_forward");
        playForwardDashSound();
    }

    @Override
    protected void onRiderDodge(Player player, boolean isLeft) {
        if (isBurrowing() || isFlying() || isInWaterOrBubble() || riderBackDashCooldownTicks > 0
                || riderForwardDashing || riderBackDashing || riderBackDashRecoveryTicks > 0
                || riderSideDodging || riderSideDodgeRecoveryTicks > 0
                || !isAlive() || isDying() || isOrderedToSit() || isInSitTransition()) {
            return;
        }

        float yawRad = (float) Math.toRadians(this.getYRot());
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);
        double dragScale = 1.0D - Math.pow(RIDER_SIDE_DODGE_HORIZONTAL_DRAG, RIDER_SIDE_DODGE_DURATION_TICKS);
        if (dragScale <= 1.0E-6D) {
            return;
        }
        double perTickSpeed = RIDER_SIDE_DODGE_DISTANCE_BLOCKS
                * (1.0D - RIDER_SIDE_DODGE_HORIZONTAL_DRAG) / dragScale;

        double dodgeDirX = rightX * (isLeft ? 1 : -1);
        double dodgeDirZ = rightZ * (isLeft ? 1 : -1);
        Vec3 dodgeVector = new Vec3(dodgeDirX * perTickSpeed, 0.0D, dodgeDirZ * perTickSpeed);

        beginRiderSideDodge(dodgeVector);
        riderBackDashCooldownTicks = RIDER_BACK_DASH_COOLDOWN_TICKS;
        triggerAnim("instant", isLeft ? "dodge_left" : "dodge_right");
        playDodgeSound();
    }

    @Override
    public void tick() {
        super.tick();
        tickScreenShake();

        if (!this.level().isClientSide) {
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
                    && !riderForwardDashing
                    && !riderBackDashing
                    && riderBackDashRecoveryTicks <= 0
                    && !riderSideDodging
                    && riderSideDodgeRecoveryTicks <= 0) {
                // Safety net for stale lock state mismatches while airborne.
                clearRiderControlLock();
            }
            this.soundHandler.tick();
            if (riderForwardDashing) {
                handleRiderForwardDashMovement();
                tickRiderForwardDashDamage();
            }
            if (riderBackDashing) {
                handleRiderBackDashMovement();
            }
            if (riderBackDashRecoveryTicks > 0) {
                handleRiderBackDashRecoveryMovement();
            }
            if (riderSideDodging) {
                handleRiderSideDodgeMovement();
            }
            if (riderSideDodgeRecoveryTicks > 0) {
                handleRiderSideDodgeRecoveryMovement();
            }
            tickFeedingCooldown();
            if (riderBackDashCooldownTicks > 0) {
                riderBackDashCooldownTicks--;
            }
            if (aiGroundMobilityCooldownTicks > 0) {
                aiGroundMobilityCooldownTicks--;
            }
            takeoffComponent.tick();
            if (riderTakeoffTicks > 0) {
                riderTakeoffTicks--;
            }

            if (isFlying()) {
                timeFlying++;
            } else {
                timeFlying = 0;
            }

            tickRiderLandingBlendTimer();
            updateSittingProgress();

            if (!isUltimateSlamActive() && this.isInWaterOrBubble() && riderFlightComponent.shouldClearFlightStateInWater(this.riderTakeoffTicks)) {
                markLandedNow();
            }

            if (isFlying() && !isUltimateSlamActive()) {
                boolean riddenByOwner = isRiddenByOwner();
                if (this.onGround() && !isTakeoff() && !isGoingUp()) {
                    markLandedNow();
                } else if (!riddenByOwner && this.getDeltaMovement().horizontalDistanceSqr() < 0.01D) {
                    setHovering(true);
                } else {
                    setHovering(false);
                }
            }

        }

        tickBankingLogic();
        tickPitchingLogic();

        this.noPhysics = false;
        if (isFlying()) {
            this.setNoGravity(true);
            switchToAirNavigation();
        } else {
            this.setNoGravity(false);
            switchToGroundNavigation();
        }

        if (!this.level().isClientSide) {
            tickAnimationStates();
            tickBurrowRumbleShake();
        }
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

    public void syncRiderLookLock(@NotNull Player player) {
        copyRiderLook(player);
    }

    public void syncRiderYawLock(@NotNull Player player) {
        copyRiderYaw(player);
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
    public void travel(@NotNull Vec3 motion) {
        if (isUltimateSlamActive()) {
            // Ultimate slam uses velocity-based movement - bypass normal travel and directly apply velocity
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
            return;
        }

        if (riderForwardDashing) {
            super.travel(Vec3.ZERO);
            return;
        }
        if (riderBackDashing) {
            super.travel(Vec3.ZERO);
            return;
        }
        if (riderBackDashRecoveryTicks > 0) {
            super.travel(Vec3.ZERO);
            return;
        }
        if (riderSideDodging) {
            super.travel(Vec3.ZERO);
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
            if (isFlying()) {
                riderController.handleFlightTravel(rider, riddenInput);
                return;
            }

            if (this.isInWaterOrBubble()) {
                riderController.handleSwimTravel(rider, riddenInput);
                if (!level().isClientSide) {
                    riderFlightComponent.tryAutoBreachTakeoff();
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

    private void switchToAirNavigation() {
        if (!usingAirNav) {
            this.navigation = this.airNav;
            this.moveControl = new MoveControl(this);
            this.usingAirNav = true;
        }
    }

    private void switchToGroundNavigation() {
        if (usingAirNav) {
            this.navigation = this.groundNav;
            this.moveControl = new MoveControl(this);
            this.usingAirNav = false;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Volitans> movement =
                new AnimationController<>(this, "movement", 5, animationHandler::movementPredicate);
        // Volitans does not use keyframed audio on movement tracks.
        movement.setSoundKeyframeHandler(event -> { });
        controllers.add(movement);

        AnimationController<Volitans> actions =
                new AnimationController<>(this, "actions", 5, animationHandler::actionPredicate);
        // Volitans uses manual/programmatic audio routing for abilities; keep keyframe channel consumed to avoid warnings.
        actions.setSoundKeyframeHandler(event -> { });
        animationHandler.setupActionController(actions);
        controllers.add(actions);

        AnimationController<Volitans> instant =
                new AnimationController<>(this, "instant", 1, animationHandler::instantActionPredicate);
        instant.setSoundKeyframeHandler(event -> { });
        animationHandler.setupInstantActionController(instant);
        controllers.add(instant);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return dragonCache;
    }

    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        return getMeleeMode() == 0 ? VolitansAbilities.VOLITANS_BITE : VolitansAbilities.VOLITANS_HORN_GORE;
    }

    @Override
    public DragonAbilityType<?, ?> getRoaringAbility() {
        return VolitansAbilities.VOLITANS_ROAR;
    }

    @Override
    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return VolitansAbilities.HURT;
    }

    @Override
    protected DragonAbilityType<?, ?> getDeathAbilityType() {
        return VolitansAbilities.DIE;
    }

    @Override
    public RiderAbilityBinding getPrimaryRiderAbility() {
        return new RiderAbilityBinding(VolitansAbilities.VOLITANS_ROAR_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getSecondaryRiderAbility() {
        String abilityId = (isFlying() || !onGround())
                ? VolitansAbilities.VOLITANS_ULTIMATE_ID
                : VolitansAbilities.VOLITANS_BURROW_ID;
        return new RiderAbilityBinding(abilityId, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getAttackRiderAbility() {
        String abilityId = getMeleeMode() == 0
                ? VolitansAbilities.VOLITANS_BITE_ID
                : VolitansAbilities.VOLITANS_HORN_GORE_ID;
        return new RiderAbilityBinding(abilityId, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getTertiaryRiderAbility() {
        return new RiderAbilityBinding(VolitansAbilities.VOLITANS_CLAW_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public Vec3 getHeadPosition() {
        return this.position().add(0.0D, this.getBbHeight() * 0.75D, 0.0D);
    }

    @Override
    public Vec3 getMouthPosition() {
        Vec3 breathLocator = getBonePositionForBreath("breathBoneOrigin");
        if (breathLocator != null) {
            return breathLocator;
        }
        Vec3 mouthLocator = getBonePositionForBreath("mouth_origin");
        if (mouthLocator != null) {
            return mouthLocator;
        }
        return computeMouthPositionFallback(1.0F);
    }

    private Vec3 computeMouthPositionFallback(float partialTicks) {
        double x = Mth.lerp(partialTicks, this.xo, this.getX());
        double y = Mth.lerp(partialTicks, this.yo, this.getY());
        double z = Mth.lerp(partialTicks, this.zo, this.getZ());

        float yawDeg = Mth.lerp(partialTicks, this.yHeadRotO, this.yHeadRot);
        float pitchDeg = Mth.lerp(partialTicks, this.xRotO, this.getXRot());

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

    public void setClientLocatorPosition(String name, Vec3 pos) {
        if (name == null || pos == null) {
            return;
        }
        this.clientLocatorCache.put(name, pos);
    }

    public void setServerBonePosition(String boneName, Vec3 position) {
        if (boneName == null || position == null) {
            return;
        }
        this.serverBonePositionCache.put(boneName, position);
    }

    @Override
    public Vec3 getClientLocatorPosition(String name) {
        if (name == null) {
            return null;
        }
        return this.clientLocatorCache.get(name);
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
    public AgeableMob getBreedOffspring(net.minecraft.server.level.ServerLevel level, AgeableMob otherParent) {
        return com.leon.saintsdragons.common.registry.ModEntities.VOLITANS.get().create(level);
    }

    @Override
    public boolean isFood(@NotNull ItemStack stack) {
        return stack.is(Items.COD)
                || stack.is(Items.SALMON)
                || stack.is(Items.PUFFERFISH)
                || stack.is(Items.TROPICAL_FISH)
                || stack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadRideableData(tag);
        if (tag.contains("VolitansBreathMode")) {
            setBreathMode(tag.getInt("VolitansBreathMode"));
        }
        if (tag.contains("FeedingCooldownTicks")) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, tag.getInt("FeedingCooldownTicks")));
        }
        if (tag.contains("RiderPitchKeyMode")) {
            setRiderPitchKeyMode(tag.getBoolean("RiderPitchKeyMode"));
        }
        tempInvulnTicks = Math.max(0, tag.getInt("VolitansTempInvulnTicks"));
        if (tempInvulnTicks > 0) {
            setInvulnerable(true);
        } else if (!isDying()) {
            setInvulnerable(false);
        }
        // Burrow is an active ability state and should never survive reconnect/reload.
        setBurrowing(false);
        if (isFlying() || isHovering()) {
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
        tag.putInt("FeedingCooldownTicks", Math.max(0, this.entityData.get(DATA_FEEDING_COOLDOWN)));
        tag.putBoolean("RiderPitchKeyMode", isRiderPitchKeyMode());
        tag.putInt("VolitansTempInvulnTicks", tempInvulnTicks);
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

    public void clearTemporaryInvulnerability() {
        tempInvulnTicks = 0;
        if (!isDying()) {
            setInvulnerable(false);
        }
    }

    private void tickTemporaryInvulnerability() {
        if (tempInvulnTicks <= 0) {
            return;
        }
        tempInvulnTicks--;
        if (tempInvulnTicks <= 0) {
            tempInvulnTicks = 0;
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

    @Override
    public int getDeathAnimationDurationTicks() {
        return 70; // 3.5s
    }

    /**
     * Keep loaded position for airborne states so reconnect while flying doesn't drop/eject.
     */
    @Override
    protected boolean repositionEntityAfterLoad() {
        return !isFlying() && !isHovering();
    }

    @Override
    public double getSwimSpeed() {
        return RIDER_SWIM_SPEED;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    public boolean canBeBound() {
        return !isDying()
                && !isFlying()
                && !isTakeoff()
                && !isLanding()
                && !isHovering()
                && !isBurrowing()
                && !areRiderControlsLocked()
                && !isGroundMobilityActive()
                && combatManager.getActiveAbility() == null;
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
        if (source.is(DamageTypes.MAGIC) && this.hasEffect(MobEffects.POISON)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, @NotNull DamageSource source) {
        this.fallDistance = 0.0F;
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof LivingEntity living) {
                living.fallDistance = 0.0F;
            }
        }
        return false;
    }

    @Override
    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
    }

    @Override
    public float getScreenShakeAmount(float partialTicks) {
        float current = this.entityData.get(DATA_SCREEN_SHAKE_AMOUNT);
        return prevScreenShakeAmount + (current - prevScreenShakeAmount) * partialTicks;
    }

    @Override
    public double getShakeDistance() {
        return 20.0D;
    }

    @Override
    public boolean canFeelShake(Entity player) {
        return true;
    }

    public void triggerScreenShake(float intensity) {
        screenShakeAmount = Math.max(screenShakeAmount, intensity);
        this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, screenShakeAmount);
    }

    public void triggerScreenShake(float intensity, int durationTicks) {
        if (durationTicks <= 0) {
            triggerScreenShake(intensity);
            return;
        }
        screenShakeHoldIntensity = Math.max(screenShakeHoldIntensity, intensity);
        screenShakeHoldTicks = Math.max(screenShakeHoldTicks, durationTicks);
        screenShakeAmount = Math.max(screenShakeAmount, screenShakeHoldIntensity);
        this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, screenShakeAmount);
    }

    public void useRidingAbility(String abilityName) {
        if (abilityName == null || abilityName.isEmpty()) {
            return;
        }
        Entity controlling = this.getControllingPassenger();
        if (!(controlling instanceof LivingEntity)) {
            return;
        }
        if (this.isTame() && controlling instanceof Player player && !this.isOwnedBy(player)) {
            return;
        }
        DragonAbilityType<?, ?> type = AbilityRegistry.get(abilityName);
        if (type == VolitansAbilities.VOLITANS_ROAR
                || type == VolitansAbilities.VOLITANS_BURROW
                || type == VolitansAbilities.VOLITANS_BITE
                || type == VolitansAbilities.VOLITANS_CLAW
                || type == VolitansAbilities.VOLITANS_BREATH
                || type == VolitansAbilities.VOLITANS_POISON_BALL
                || type == VolitansAbilities.VOLITANS_HORN_GORE
                || type == VolitansAbilities.VOLITANS_ULTIMATE) {
            combatManager.tryUseAbility(type);
        }
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

    public boolean isUltimateSlamActive() {
        return this.entityData.get(DATA_ULTIMATE_SLAM_ACTIVE);
    }

    public void startUltimateSlamMovement() {
        this.entityData.set(DATA_ULTIMATE_SLAM_ACTIVE, true);
        setFlying(true);
        setTakeoff(false);
        setHovering(false);
        setLanding(false);
    }

    public void stopUltimateSlamMovement() {
        this.entityData.set(DATA_ULTIMATE_SLAM_ACTIVE, false);
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
        setLanding(true);
        setFlying(false);
        setGoingUp(false);
        setGoingDown(false);
    }

    public void forceEndActiveAbility() {
        combatManager.forceEndActiveAbility();
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

    public boolean isAbilityActive(DragonAbilityType<?, ?> abilityType) {
        return combatManager.isAbilityActive(abilityType);
    }

    public boolean isGroundMobilityActive() {
        return riderBackDashing
                || riderBackDashRecoveryTicks > 0
                || riderSideDodging
                || riderSideDodgeRecoveryTicks > 0;
    }

    public boolean isGroundCombatAbilityActive() {
        return isAbilityActive(VolitansAbilities.VOLITANS_BITE)
                || isAbilityActive(VolitansAbilities.VOLITANS_CLAW)
                || isAbilityActive(VolitansAbilities.VOLITANS_HORN_GORE)
                || isAbilityActive(VolitansAbilities.VOLITANS_ROAR)
                || isAbilityActive(VolitansAbilities.VOLITANS_POISON_BALL)
                || isAbilityActive(VolitansAbilities.VOLITANS_BREATH)
                || isAbilityActive(VolitansAbilities.VOLITANS_BURROW);
    }

    public boolean isAiRootedByAbility() {
        if (isAbilityActive(VolitansAbilities.VOLITANS_ROAR) && !isFlying() && !isInWaterOrBubble()) {
            return true;
        }
        if (isAbilityActive(VolitansAbilities.VOLITANS_POISON_BALL) && !isFlying() && !isInWaterOrBubble()) {
            return true;
        }
        if (isAbilityActive(VolitansAbilities.VOLITANS_BURROW) && !isBurrowing()) {
            return true;
        }
        return false;
    }

    public boolean shouldAiHoldPositionForAbility() {
        return isAiRootedByAbility()
                || (isAbilityActive(VolitansAbilities.VOLITANS_BREATH) && !isFlying());
    }

    public boolean requestPoisonBallRelease() {
        var active = combatManager.getActiveAbility();
        if (active instanceof VolitansPoisonBallAbility poisonBallAbility
                && active.getAbilityType() == VolitansAbilities.VOLITANS_POISON_BALL) {
            poisonBallAbility.requestRelease();
            return true;
        }
        return false;
    }

    public boolean requestBurrowExit(boolean withBurst) {
        var active = combatManager.getActiveAbility();
        if (active instanceof VolitansBurrowAbility burrowAbility
                && active.getAbilityType() == VolitansAbilities.VOLITANS_BURROW) {
            burrowAbility.requestExit(withBurst);
            return true;
        }
        return false;
    }

    public boolean tryAiGroundDodge(@Nullable LivingEntity threat) {
        if (isFlying() || isTakeoff() || isLanding() || isHovering() || isInWaterOrBubble() || isBurrowing()) {
            return false;
        }
        if (isGroundMobilityActive() || aiGroundMobilityCooldownTicks > 0 || areRiderControlsLocked()) {
            return false;
        }

        float yawRad = (float) Math.toRadians(this.getYRot());
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);
        double dragScale = 1.0D - Math.pow(RIDER_SIDE_DODGE_HORIZONTAL_DRAG, RIDER_SIDE_DODGE_DURATION_TICKS);
        if (dragScale <= 1.0E-6D) {
            return false;
        }
        double perTickSpeed = RIDER_SIDE_DODGE_DISTANCE_BLOCKS
                * (1.0D - RIDER_SIDE_DODGE_HORIZONTAL_DRAG) / dragScale;

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
        triggerAnim("instant", isLeft ? "dodge_left" : "dodge_right");
        playDodgeSound();
        return true;
    }

    public boolean tryAiGroundBackstep(@Nullable LivingEntity threat) {
        if (isFlying() || isTakeoff() || isLanding() || isHovering() || isInWaterOrBubble() || isBurrowing()) {
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
        Vec3 launch = new Vec3(backward.x * RIDER_BACK_DASH_SPEED, 0.0D, backward.z * RIDER_BACK_DASH_SPEED);
        beginRiderBackDash(launch);
        riderBackDashCooldownTicks = RIDER_BACK_DASH_COOLDOWN_TICKS;
        aiGroundMobilityCooldownTicks = 26;
        triggerAnim("instant", "dash_backwards");
        playBackwardDashSound();
        fireBackwardDashSpikes();
        return true;
    }

    private void tickBankingLogic() {
        prevBankAngle = bankAngle;

        boolean shouldBank = isFlying() && !isLanding();
        if (!shouldBank) {
            bankSmoothedYaw = 0f;
            bankAngle = 0f;
            prevBankAngle = 0f;
            return;
        }

        if (horizontalCollision || verticalCollision) {
            bankSmoothedYaw *= 0.45f;
            bankAngle = Mth.lerp(0.55f, bankAngle, 0f);
            if (Math.abs(bankAngle) < 0.01f) {
                bankAngle = 0f;
            }
            return;
        }

        float yawChange = Mth.wrapDegrees(getYRot() - yRotO);
        bankSmoothedYaw = bankSmoothedYaw * 0.75f + yawChange * 0.25f;
        float targetAngle = Mth.clamp(bankSmoothedYaw * 6.0f, -90f, 90f);
        bankAngle = Mth.lerp(0.30f, bankAngle, targetAngle);
        if (Math.abs(bankAngle) < 0.01f) {
            bankAngle = 0f;
        }
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

    private void beginRiderBackDash(Vec3 vec) {
        blockTakeoffInput(RIDER_BACK_DASH_DURATION_TICKS + RIDER_BACK_DASH_RECOVERY_TICKS);
        this.riderBackDashing = true;
        this.riderForwardDashing = false;
        this.riderForwardDashVec = Vec3.ZERO;
        this.riderForwardDashTicksLeft = 0;
        this.riderForwardDashTicksElapsed = 0;
        this.riderForwardDashDamageApplied = false;
        this.riderBackDashRecoveryTicks = 0;
        this.riderSideDodging = false;
        this.riderSideDodgeVec = Vec3.ZERO;
        this.riderSideDodgeTicksLeft = 0;
        this.riderSideDodgeRecoveryTicks = 0;
        this.riderBackDashVec = vec;
        this.riderBackDashTicksLeft = Math.max(1, RIDER_BACK_DASH_DURATION_TICKS);
        this.setDeltaMovement(vec);
        this.getNavigation().stop();
        this.hasImpulse = true;
    }

    private void handleRiderBackDashMovement() {
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
                RIDER_BACK_DASH_HORIZONTAL_DRAG,
                RIDER_BACK_DASH_VERTICAL_DRAG,
                RIDER_BACK_DASH_HORIZONTAL_DRAG
        );
        if (--riderBackDashTicksLeft <= 0) {
            riderBackDashing = false;
            riderBackDashRecoveryTicks = RIDER_BACK_DASH_RECOVERY_TICKS;
        }
    }

    private void handleRiderBackDashRecoveryMovement() {
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
        }
    }

    private void beginRiderSideDodge(Vec3 vec) {
        blockTakeoffInput(RIDER_SIDE_DODGE_DURATION_TICKS + RIDER_SIDE_DODGE_RECOVERY_TICKS);
        this.riderSideDodging = true;
        this.riderForwardDashing = false;
        this.riderForwardDashVec = Vec3.ZERO;
        this.riderForwardDashTicksLeft = 0;
        this.riderForwardDashTicksElapsed = 0;
        this.riderForwardDashDamageApplied = false;
        this.riderBackDashing = false;
        this.riderBackDashRecoveryTicks = 0;
        this.riderBackDashVec = Vec3.ZERO;
        this.riderBackDashTicksLeft = 0;
        this.riderSideDodgeRecoveryTicks = 0;
        this.riderSideDodgeVec = vec;
        this.riderSideDodgeTicksLeft = Math.max(1, RIDER_SIDE_DODGE_DURATION_TICKS);
        this.setDeltaMovement(vec);
        this.getNavigation().stop();
        this.hasImpulse = true;
    }

    private void handleRiderSideDodgeMovement() {
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
                RIDER_SIDE_DODGE_HORIZONTAL_DRAG,
                RIDER_SIDE_DODGE_VERTICAL_DRAG,
                RIDER_SIDE_DODGE_HORIZONTAL_DRAG
        );
        if (--riderSideDodgeTicksLeft <= 0) {
            riderSideDodging = false;
            riderSideDodgeRecoveryTicks = RIDER_SIDE_DODGE_RECOVERY_TICKS;
        }
    }

    private void handleRiderSideDodgeRecoveryMovement() {
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
        }
    }

    private void beginRiderForwardDash(Vec3 vec) {
        blockTakeoffInput(RIDER_FORWARD_DASH_DURATION_TICKS);
        this.riderForwardDashing = true;
        this.riderForwardDashVec = vec;
        this.riderForwardDashTicksLeft = Math.max(1, RIDER_FORWARD_DASH_DURATION_TICKS);
        this.riderForwardDashTicksElapsed = 0;
        this.riderForwardDashDamageApplied = false;

        this.riderBackDashing = false;
        this.riderBackDashTicksLeft = 0;
        this.riderBackDashRecoveryTicks = 0;
        this.riderBackDashVec = Vec3.ZERO;
        this.riderSideDodging = false;
        this.riderSideDodgeTicksLeft = 0;
        this.riderSideDodgeRecoveryTicks = 0;
        this.riderSideDodgeVec = Vec3.ZERO;

        this.setDeltaMovement(vec);
        this.getNavigation().stop();
        this.hasImpulse = true;
    }

    private void clearGroundMobilityState() {
        this.riderForwardDashing = false;
        this.riderForwardDashTicksLeft = 0;
        this.riderForwardDashTicksElapsed = 0;
        this.riderForwardDashDamageApplied = false;
        this.riderForwardDashVec = Vec3.ZERO;

        this.riderBackDashing = false;
        this.riderBackDashTicksLeft = 0;
        this.riderBackDashRecoveryTicks = 0;
        this.riderBackDashVec = Vec3.ZERO;

        this.riderSideDodging = false;
        this.riderSideDodgeTicksLeft = 0;
        this.riderSideDodgeRecoveryTicks = 0;
        this.riderSideDodgeVec = Vec3.ZERO;
    }

    private void handleRiderForwardDashMovement() {
        double yVel = getGroundBurstVerticalVelocity();
        double horizontalX = riderForwardDashVec.x;
        double horizontalZ = riderForwardDashVec.z;
        if (!this.onGround()) {
            horizontalX *= 0.90D;
            horizontalZ *= 0.90D;
        }
        this.setDeltaMovement(horizontalX, yVel, horizontalZ);
        this.hasImpulse = true;
        riderForwardDashVec = riderForwardDashVec.multiply(
                RIDER_FORWARD_DASH_HORIZONTAL_DRAG,
                RIDER_FORWARD_DASH_VERTICAL_DRAG,
                RIDER_FORWARD_DASH_HORIZONTAL_DRAG
        );
        riderForwardDashTicksElapsed++;
        if (--riderForwardDashTicksLeft <= 0) {
            riderForwardDashing = false;
            riderForwardDashVec = Vec3.ZERO;
        }
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
        if (riderForwardDashDamageApplied || riderForwardDashTicksElapsed < RIDER_FORWARD_DASH_DAMAGE_TICK
                || level().isClientSide || !this.isVehicle()) {
            return;
        }
        riderForwardDashDamageApplied = true;
        Vec3 center = getMouthPosition();
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
        if (this.level().isClientSide) {
            return;
        }
        this.getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_DASH_FORWARD.get(), 1.6f, 1.0f, RIDER_DASH_SOUND_TICKS);
    }

    private void playBackwardDashSound() {
        if (this.level().isClientSide) {
            return;
        }
        this.getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_DASH_BACKWARDS.get(), 1.6f, 1.0f, RIDER_DASH_SOUND_TICKS);
    }

    private void playDodgeSound() {
        if (this.level().isClientSide) {
            return;
        }
        this.getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_DODGE.get(), 1.6f, 1.0f, RIDER_DODGE_SOUND_TICKS);
    }

    private void fireBackwardDashSpikes() {
        if (level().isClientSide) {
            return;
        }

        Vec3 muzzle = getMouthPosition();
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
            Vec3 origin = muzzle.add(shootDir.scale(Math.max(1.2D, getBbWidth() * 0.7D))).add(0.0D, 0.15D, 0.0D);
            VolitansSpineEntity spine = new VolitansSpineEntity(level(), this);
            spine.setPos(origin.x, origin.y, origin.z);
            spine.setImpactEffects(
                    RIDER_BACK_DASH_SPIKE_DAMAGE,
                    RIDER_BACK_DASH_SPIKE_POISON_DURATION_TICKS,
                    RIDER_BACK_DASH_SPIKE_POISON_AMPLIFIER
            );
            spine.setNoGravity(true);
            spine.shoot(shootDir.x, shootDir.y, shootDir.z, RIDER_BACK_DASH_SPIKE_SPEED, RIDER_BACK_DASH_SPIKE_INACCURACY);
            spine.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
            level().addFreshEntity(spine);
        }
    }

    private Vec3 getDragonHorizontalForwardVector() {
        float yawRad = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad)).normalize();
    }

    private void tickPitchingLogic() {
        prevFlightPitchRad = flightPitchRad;
        if (level().isClientSide) {
            flightPitchRad = this.entityData.get(DATA_FLIGHT_PITCH);
            return;
        }

        if (!isFlying() || isLanding() || this.isInWaterOrBubble()) {
            flightPitchRad = 0f;
            smoothedPlayerPitchRad = 0f;
            this.entityData.set(DATA_FLIGHT_PITCH, 0f);
            return;
        }

        float targetPitchRad = 0f;
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            float riderForward = this.entityData.get(DATA_RIDER_FORWARD);
            float riderStrafe = this.entityData.get(DATA_RIDER_STRAFE);
            boolean hasMovementInput = Math.abs(riderForward) > 0.01f
                    || Math.abs(riderStrafe) > 0.01f
                    || Math.abs(player.zza) > 0.01f
                    || Math.abs(player.xxa) > 0.01f;
            if (hasMovementInput) {
                float rawPlayerPitchRad = (float) Math.toRadians(player.getXRot());
                smoothedPlayerPitchRad = smoothedPlayerPitchRad * 0.65f + rawPlayerPitchRad * 0.35f;
                targetPitchRad = Mth.clamp(smoothedPlayerPitchRad, -Mth.HALF_PI, Mth.HALF_PI);
            } else {
                smoothedPlayerPitchRad = 0f;
            }
        } else {
            Vec3 velocity = getDeltaMovement();
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            if (horizontalSpeed > 0.15) {
                targetPitchRad = (float) Math.atan2(velocity.y, horizontalSpeed);
                targetPitchRad = Mth.clamp(targetPitchRad, -Mth.HALF_PI, Mth.HALF_PI);
            }
        }

        flightPitchRad = Mth.lerp(0.35f, flightPitchRad, targetPitchRad);
        if (Math.abs(flightPitchRad) < 0.001f) {
            flightPitchRad = 0f;
        }
        this.entityData.set(DATA_FLIGHT_PITCH, flightPitchRad);
    }

    public float getBankAngleDegrees(float partialTick) {
        return Mth.lerp(partialTick, prevBankAngle, bankAngle);
    }

    public float getFlightPitchRadians(float partialTick) {
        return Mth.lerp(partialTick, prevFlightPitchRad, flightPitchRad);
    }

    private void tickScreenShake() {
        if (level().isClientSide) {
            prevScreenShakeAmount = screenShakeAmount;
            screenShakeAmount = this.entityData.get(DATA_SCREEN_SHAKE_AMOUNT);
            return;
        }

        prevScreenShakeAmount = screenShakeAmount;
        if (screenShakeHoldTicks > 0) {
            screenShakeHoldTicks--;
            screenShakeAmount = Math.max(screenShakeAmount, screenShakeHoldIntensity);
            this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, screenShakeAmount);
            if (screenShakeHoldTicks == 0) {
                screenShakeHoldIntensity = 0.0F;
                screenShakeAmount = 0.0F;
                this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
            }
            return;
        }
        if (this.entityData.get(DATA_SCREEN_SHAKE_AMOUNT) != 0.0F) {
            screenShakeAmount = 0.0F;
            this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        }
    }

    private void tickBurrowRumbleShake() {
        if (!isBurrowing()) {
            return;
        }
        boolean moving = Math.abs(this.entityData.get(DATA_RIDER_FORWARD)) > 0.03F
                || Math.abs(this.entityData.get(DATA_RIDER_STRAFE)) > 0.03F
                || this.getDeltaMovement().horizontalDistanceSqr() > 0.0016D;
        float target = moving ? BURROW_MOVE_SHAKE_INTENSITY : 0.0F;
        this.screenShakeAmount = target;
        this.prevScreenShakeAmount = target;
        this.screenShakeHoldTicks = 0;
        this.screenShakeHoldIntensity = 0.0F;
        this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, target);
    }

    private boolean shouldSuppressTakeoffInput() {
        var activeAbility = combatManager.getActiveAbility();
        boolean activeAbilityBlocksTakeoff = activeAbility != null
                && activeAbility.getAbilityType() != VolitansAbilities.VOLITANS_POISON_BALL;
        return isBurrowing()
                || takeoffInputBlockTicks > 0
                || riderForwardDashing
                || riderBackDashing
                || riderBackDashRecoveryTicks > 0
                || riderSideDodging
                || riderSideDodgeRecoveryTicks > 0
                || areRiderControlsLocked()
                || isInSitTransition()
                || activeAbilityBlocksTakeoff;
    }

    public boolean isRiddenByOwner() {
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

    private boolean shouldAggroOnSight() {
        return !isTame() && !isBaby();
    }

    private void tickRiderLandingBlendTimer() {
        // Ultimate slam owns touchdown presentation ("slammed"), so skip generic landed handling.
        if (isUltimateSlamActive()) {
            consumeRiderTouchdownFromAir(1.0D);
            return;
        }

        trackRiderAirborneForLanding();
        boolean inWater = this.isInWater() || this.isInWaterOrBubble();

        if (inWater) {
            consumeRiderTouchdownFromAir(1.0D);
            if (!level().isClientSide && (isFlying() || isTakeoff() || isLanding() || isHovering())
                    && riderFlightComponent.shouldClearFlightStateInWater(this.riderTakeoffTicks)) {
                markLandedNow();
            }
            return;
        }

        if (!isVehicle() || !isFlying() || onGround()) {
            boolean touchdownFromFlight = consumeRiderTouchdownFromAir(0.25D);
            boolean completedLanding = isLanding() && onGround();
            if ((completedLanding || touchdownFromFlight) && onGround() && !level().isClientSide) {
                triggerAnim("actions", "landed");
                getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_LANDED.get(), 2.0f, 1.0f, 32);
                lockRiderControls(LANDED_CONTROL_LOCK_TICKS);
                markLandedNow();
            }
            return;
        }

        if (!isLanding() && shouldTriggerLandingBlend()) {
            setLanding(true);
        }
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

    private boolean isNearWaterSurface() {
        if (level() == null) {
            return false;
        }

        double dragonY = getY();
        if (dragonY > RIDER_WATER_SURFACE_LEVEL + RIDER_WATER_SURFACE_TOLERANCE) {
            return false;
        }

        net.minecraft.core.BlockPos.MutableBlockPos cursor = new net.minecraft.core.BlockPos.MutableBlockPos();
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
                        double surfaceY = cursor.getY() + 1.0D;
                        if (Math.abs(dragonY - surfaceY) <= RIDER_WATER_SURFACE_TOLERANCE) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void applyConfiguredAttributes() {
        setAttributeBase(Attributes.MAX_HEALTH, 90.0D);
        setAttributeBase(Attributes.MOVEMENT_SPEED, 0.30D);
        setAttributeBase(Attributes.FLYING_SPEED, RIDER_FLY_SPEED);
        setAttributeBase(Attributes.ARMOR, 4.0D);
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

    @Override
    public float maxSitTicks() {
        return SIT_DOWN_ANIMATION_TICKS;
    }

    private int getSitDownAnimationTicks() {
        return SIT_DOWN_ANIMATION_TICKS;
    }

    private int getSitUpAnimationTicks() {
        return SIT_UP_ANIMATION_TICKS;
    }

    private void updateSittingProgress() {
        if (level().isClientSide) {
            return;
        }

        if (sitTransitionTicks > 0) {
            sitTransitionTicks--;
            if (sitTransitionTicks == 0) {
                isSittingDown = false;
                isStandingUp = false;
            }
        }

        float sitProgress = getSitProgress();
        if (this.isOrderedToSit()) {
            if ((sitProgress == 0f || isStandingUp) && !isSittingDown) {
                animationHandler.triggerSitDownAnimation();
                isSittingDown = true;
                isStandingUp = false;
                sitTransitionTicks = getSitDownAnimationTicks();
            }

            if (sitProgress < maxSitTicks()) {
                sitProgress++;
                setSitProgress(sitProgress);
            }
        } else {
            if (isVehicle()) {
                if (sitProgress != 0f) {
                    clearSitProgress();
                }
            } else if (sitProgress > 0f) {
                if ((sitProgress >= maxSitTicks() - 1 || isSittingDown) && !isStandingUp) {
                    animationHandler.triggerSitUpAnimation();
                    isStandingUp = true;
                    isSittingDown = false;
                    sitTransitionTicks = getSitUpAnimationTicks();
                }

                float decrementRate = maxSitTicks() / (float) getSitUpAnimationTicks();
                sitProgress -= decrementRate;
                if (sitProgress < 0f) {
                    sitProgress = 0f;
                }
                setSitProgress(sitProgress);
            }
        }
    }

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

    @Override
    protected float getBodyTurnSpeed() {
        return 0.55F;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(4.0D, 2.0D, 4.0D);
    }
}
