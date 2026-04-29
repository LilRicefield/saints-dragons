package com.leon.saintsdragons.server.entity.dragons.volitans;

import com.leon.saintsdragons.common.block.VolitansEggBlock;
import com.leon.saintsdragons.common.block.VolitansEggBlockEntity;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.registry.ModBlocks;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.volitans.VolitansAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonFindWaterGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtByTargetGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtTargetGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonProtectBabiesGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonRandomHuntTargetGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonFollowOwnerGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonGroundWanderGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonLeaveWaterGoal;
import com.leon.saintsdragons.server.ai.goals.base.DirectSwimToTargetGoal;
import com.leon.saintsdragons.server.ai.goals.base.DirectSwimWanderGoal;
import com.leon.saintsdragons.server.ai.goals.volitans.VolitansAirCombatGoal;
import com.leon.saintsdragons.server.ai.goals.volitans.VolitansBreedGoal;
import com.leon.saintsdragons.server.ai.goals.volitans.VolitansFindSleepDepthGoal;
import com.leon.saintsdragons.server.ai.goals.volitans.VolitansFlightGoal;
import com.leon.saintsdragons.server.ai.goals.volitans.VolitansSlamSequenceGoal;
import com.leon.saintsdragons.server.ai.goals.volitans.VolitansGroundCombatGoal;
import com.leon.saintsdragons.server.ai.goals.volitans.VolitansWaterCombatGoal;
import com.leon.saintsdragons.server.ai.navigation.DragonNavigationModeController;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncFlightController;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncFlightMoveControl;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncFlyingPathNavigation;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansBurrowAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansPoisonBallAbility;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.base.DragonVariant;
import com.leon.saintsdragons.server.entity.base.DragonVariantSet;
import com.leon.saintsdragons.server.flight.DragonGroundedAerialRecovery;
import com.leon.saintsdragons.server.flight.DragonRiderFallRecovery;
import com.leon.saintsdragons.server.flight.DragonRiderFlight;
import com.leon.saintsdragons.server.flight.DragonTakeoff;
import com.leon.saintsdragons.server.entity.controller.volitans.VolitansRiderController;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansSpineEntity;
import com.leon.saintsdragons.server.entity.dragons.volitans.handlers.VolitansAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.volitans.handlers.VolitansInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.volitans.handlers.VolitansSoundProfile;
import com.leon.saintsdragons.server.entity.dragons.volitans.handlers.VolitansTamingHandler;
import com.leon.saintsdragons.server.entity.component.ScreenShakeComponent;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Pufferfish;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.TropicalFish;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;

public class Volitans extends RideableFlyingDragon implements DragonFlightCapable, SemiAquaticDragon, SoundHandledDragon, ShakesScreen {
    private static final double BABY_MAX_HEALTH = 60.0D;
    private static final double BABY_ARMOR = 0.0D;
    private static final float BABY_HITBOX_SCALE = 0.55F;
    private static final int SLEEP_AFTER_SPAWN_GRACE_TICKS = 600;
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
    public static final EntityDataAccessor<Boolean> DATA_TAMING_STUNNED =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.BOOLEAN);

    private static final double RIDER_WALK_SPEED = 0.24D;
    private static final double RIDER_RUN_SPEED = 0.34D;
    private static final double RIDER_BURROW_SPEED = 0.40D;
    private static final double RIDER_SWIM_SPEED = 1.42D;
    private static final double RIDER_FLY_SPEED = 0.38D;
    public static final int TAKEOFF_ANIMATION_TICKS = 35;
    private static final int GROUNDED_AERIAL_RECOVERY_TICKS = 8;
    public static final int TAKEOFF_LAUNCH_DELAY_TICKS = 15;
    private static final int SIT_DOWN_ANIMATION_TICKS = 50;
    private static final int SIT_UP_ANIMATION_TICKS = 25;
    private static final int SLEEP_FALL_ASLEEP_ANIMATION_TICKS = 51;
    private static final int SLEEP_WAKE_UP_ANIMATION_TICKS = 43;
    private static final int SLEEP_EXIT_SUPPRESSION_TICKS = 20;
    private static final int SLEEP_WAKE_SUPPRESSION_TICKS = 20;
    private static final int LANDED_CONTROL_LOCK_TICKS = 20;
    private static final int WALK_SOUND_DURATION_TICKS = 33;
    private static final int RUN_SOUND_DURATION_TICKS = 30;
    private static final int EAT_SOUND_DURATION_TICKS = 34;
    private static final long WALK_SOUND_REPLAY_INTERVAL_TICKS = 33;
    private static final long RUN_SOUND_REPLAY_INTERVAL_TICKS = 25;
    private static final int MIN_AMBIENT_DELAY = 220;
    private static final int MAX_AMBIENT_DELAY = 420;
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
    private static final int RIDER_BACK_DASH_SPIKE_DELAY_TICKS = 8;
    private static final double RIDER_BACK_DASH_SPIKE_Y_OFFSET = -3.0D;
    private static final float REACTIVE_HIT_EVADE_CHANCE = 0.35F;
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
    private static final float BREATH_DEPLETED_THRESHOLD = 0.01F;
    private static final float BREATH_REARM_THRESHOLD = 0.20F;
    private static final int SPINE_DROP_COOLDOWN_TICKS = 30;
    private static final float FISH_DROP_CHANCE = 0.40F;
    public static final double LANDING_BLEND_ALTITUDE = RideableFlyingDragon.LANDING_BLEND_ALTITUDE;
    public static final double BREED_PARTNER_RANGE = 20.0D;
    public static final double BREED_DISTANCE_SQR = 16.0D;
    private static final float BURROW_MOVE_SHAKE_INTENSITY = 0.12F;
    private static final int BURROW_EXIT_TAKEOFF_BLOCK_BUFFER_TICKS = 8;
    private static final Map<String, VocalEntry> VOCAL_ENTRIES = new VocalEntryBuilder()
            .add("grumble1", "actions", "animation.volitans.grumble1", ModSounds.VOLITANS_GRUMBLE_1, 1.0f, 0.98f, 0.08f, false, false, false)
            .add("grumble2", "actions", "animation.volitans.grumble2", ModSounds.VOLITANS_GRUMBLE_2, 1.0f, 0.98f, 0.08f, false, false, false)
            .add("grumble3", "actions", "animation.volitans.grumble3", ModSounds.VOLITANS_GRUMBLE_3, 1.0f, 1.0f, 0.06f, false, false, false)
            .build();

    private final AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);
    private final VolitansAnimationHandler animationHandler = new VolitansAnimationHandler(this);
    private final VolitansInteractionHandler interactionHandler = new VolitansInteractionHandler(this);
    private final VolitansTamingHandler tamingController = new VolitansTamingHandler(this);
    private final VolitansRiderController riderController;
    private final DragonRiderFlight riderFlightComponent;
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final java.util.Map<String, Vec3> clientLocatorCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, Vec3> serverBonePositionCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final AsyncFlightController asyncAirController;
    private final AsyncFlightMoveControl asyncAirMoveControl;
    private final DragonPathNavigateGround groundNav;
    private final MoveControl groundMoveControl;
    private final FlyingPathNavigation airNav;
    private final DragonNavigationModeController navigationModeController;
    private final DragonTakeoff takeoffComponent;
    private int timeFlying;
    private int spineDropCooldownTicks;
    private int riderTakeoffTicks;
    private int groundedAerialRecoveryTicks;
    private int ticksInWater;
    private int ticksOutOfWater;
    private float bankSmoothedYaw = 0f;
    private float bankAngle = 0f;
    private float prevBankAngle = 0f;
    private float flightPitchRad = 0f;
    private float prevFlightPitchRad = 0f;
    private float smoothedPlayerPitchRad = 0f;
    private final ScreenShakeComponent screenShakeComponent;
    private int sitTransitionTicks = 0;
    private boolean isSittingDown = false;
    private boolean isStandingUp = false;
    private int riderBackDashCooldownTicks = 0;
    private int tamingAbortCalmTicks = 0;
    private boolean riderForwardDashing = false;
    private int riderForwardDashTicksLeft = 0;
    private int riderForwardDashTicksElapsed = 0;
    private boolean riderForwardDashDamageApplied = false;
    private Vec3 riderForwardDashVec = Vec3.ZERO;
    private boolean riderBackDashing = false;
    private int riderBackDashTicksLeft = 0;
    private Vec3 riderBackDashVec = Vec3.ZERO;
    private int riderBackDashRecoveryTicks = 0;
    private int riderBackDashSpikeDelayTicks = 0;
    private boolean riderSideDodging = false;
    private int riderSideDodgeTicksLeft = 0;
    private Vec3 riderSideDodgeVec = Vec3.ZERO;
    private int riderSideDodgeRecoveryTicks = 0;
    private int takeoffInputBlockTicks = 0;
    private int aiGroundMobilityCooldownTicks = 0;
    private boolean aiSpecialCombatActive = false;
    private boolean aiSpecialCombatReserved = false;
    private int tempInvulnTicks = 0;
    private float sleepLockedYaw = 0.0F;
    private float sleepLockedPitch = 0.0F;
    private int ambientSoundTimer;
    private int nextAmbientSoundDelay;

    public Volitans(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.screenShakeComponent = new ScreenShakeComponent(this, DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        this.setMaxUpStep(1.0F);
        this.riderController = new VolitansRiderController(this);
        this.takeoffComponent = createTakeoffComponent();
        this.riderFlightComponent = createRiderFlightComponent();

        this.asyncAirController = new AsyncFlightController(this);
        this.asyncAirMoveControl = new AsyncFlightMoveControl(this, this.asyncAirController);
        this.groundNav = new DragonPathNavigateGround(this, level);
        this.groundMoveControl = new MoveControl(this);
        this.airNav = new AsyncFlyingPathNavigation(this, level, this.asyncAirController) {
            @Override
            public boolean isStableDestination(@NotNull net.minecraft.core.BlockPos pos) {
                return !this.level.getBlockState(pos.below()).isAir();
            }
        };
        this.airNav.setCanOpenDoors(false);
        this.airNav.setCanPassDoors(false);
        this.airNav.setCanFloat(false);
        this.navigationModeController = new DragonNavigationModeController(
                new DragonNavigationModeController.Host() {
                    @Override
                    public void setActiveNavigation(PathNavigation navigation) {
                        Volitans.this.navigation = navigation;
                    }

                    @Override
                    public void setActiveMoveControl(MoveControl moveControl) {
                        Volitans.this.moveControl = moveControl;
                    }

                    @Override
                    public void afterSwitchToGround() {
                        if (Volitans.this.onGround()) {
                            Volitans.this.setDeltaMovement(Vec3.ZERO);
                            Volitans.this.hasImpulse = false;
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

        if (!level.isClientSide) {
            applyConfiguredAttributes();
        }
        resetAmbientSoundTimer();
    }

    private DragonTakeoff createTakeoffComponent() {
        return new DragonTakeoff(new DragonTakeoff.Host() {
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

            @Override
            public int getTakeoffLiftDelayTicks() { return TAKEOFF_LAUNCH_DELAY_TICKS; }
        });
    }

    private DragonRiderFlight createRiderFlightComponent() {
        return new DragonRiderFlight(new DragonRiderFlight.Host() {
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
        }, new DragonRiderFlight.Config(
                true,
                0,
                0.12D,
                TAKEOFF_ANIMATION_TICKS,
                0.12D,
                TAKEOFF_ANIMATION_TICKS
        ));
    }

    public void startTakeoffSequence(double minUpwardVelocity, int animationTicks) {
        this.setRunning(false);
        this.setAccelerating(false);
        this.setDeltaMovement(Vec3.ZERO);
        if (!level().isClientSide) {
            triggerAnim("instant", "takeoff");
            if (isVehicle() && !isFlying() && TAKEOFF_LAUNCH_DELAY_TICKS > 0) {
                lockRiderControls(TAKEOFF_LAUNCH_DELAY_TICKS);
            }
        }
        takeoffComponent.startTakeoff(animationTicks, minUpwardVelocity);
    }

    public static AttributeSupplier.Builder createAttributes() {
        var config = com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader.getInstance()
                .getConfig(com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader.VOLITANS_ID);
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, config.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FLYING_SPEED, config.flyingSpeed())
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ARMOR, config.armor());
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        EntityDimensions baseDimensions = super.getDimensions(pose);
        return isBaby() ? baseDimensions.scale(BABY_HITBOX_SCALE) : baseDimensions;
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
    public @NotNull SpawnGroupData finalizeSpawn(
            @NotNull net.minecraft.world.level.ServerLevelAccessor level,
            @NotNull net.minecraft.world.DifficultyInstance difficulty,
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
    protected void playStepSound(net.minecraft.core.@NotNull BlockPos pos, @NotNull BlockState state) {
        if (isBaby()) {
            return;
        }
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
        this.goalSelector.addGoal(0, new SitWhenOrderedToGoal(this));
        if (!this.isBaby()) {
            this.goalSelector.addGoal(1, new VolitansSlamSequenceGoal(this));
            this.goalSelector.addGoal(2, new VolitansGroundCombatGoal(this));
            this.goalSelector.addGoal(3, new VolitansAirCombatGoal(this));
            this.goalSelector.addGoal(4, new VolitansWaterCombatGoal(this));
            this.goalSelector.addGoal(5, new VolitansBreedGoal(this, 1.0D, BREED_PARTNER_RANGE, BREED_DISTANCE_SQR));
        }
        this.goalSelector.addGoal(6, new VolitansFindSleepDepthGoal(this, 6.0F, 0.16D));
        this.goalSelector.addGoal(6, new DirectSwimToTargetGoal(this, 8.0F, 0.28D, true) {
            @Override
            public boolean canUse() {
                return !Volitans.this.isSleepLocked() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !Volitans.this.isSleepLocked() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(7, new DragonLeaveWaterGoal<>(this));
        this.goalSelector.addGoal(8, new DragonFindWaterGoal<>(this));
        this.goalSelector.addGoal(9, new DragonFollowOwnerGoal<>(this, DragonFollowOwnerGoal.FollowConfig.forVolitans()) {
            @Override
            protected void startFollowTakeoff() {
                if (Volitans.this.isFlying() || Volitans.this.isTakeoff()) {
                    return;
                }
                Volitans.this.startTakeoffSequence(0.12D, TAKEOFF_ANIMATION_TICKS);
            }

            @Override
            public boolean canUse() {
                return !Volitans.this.isVehicle() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !Volitans.this.isVehicle() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(10, new DirectSwimToTargetGoal(this, 8.0F, 0.24D, false) {
            @Override
            public boolean canUse() {
                return !Volitans.this.isSleepLocked() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !Volitans.this.isSleepLocked() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(11, new VolitansFlightGoal(this));
        this.goalSelector.addGoal(12, new DragonGroundWanderGoal<>(this, 0.9D, 70));
        this.goalSelector.addGoal(14, new DirectSwimWanderGoal(this, 6.0F, 0.20D, 30) {
            @Override
            public boolean canUse() {
                return !Volitans.this.isSleepLocked() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !Volitans.this.isSleepLocked() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(15, new LookAtPlayerGoal(this, Player.class, 8.0F) {
            @Override
            public boolean canUse() {
                return !Volitans.this.isVehicle() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !Volitans.this.isVehicle() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(16, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !Volitans.this.isVehicle() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !Volitans.this.isVehicle() && super.canContinueToUse();
            }
        });
        if (!this.isBaby()) {
            this.targetSelector.addGoal(1, new DragonOwnerHurtByTargetGoal(this) {
                @Override
                public boolean canUse() {
                    return !Volitans.this.isVehicle() && super.canUse();
                }
            });
            this.targetSelector.addGoal(2, new DragonOwnerHurtTargetGoal(this) {
                @Override
                public boolean canUse() {
                    return !Volitans.this.isVehicle() && super.canUse();
                }
            });
            this.targetSelector.addGoal(3, new DragonProtectBabiesGoal<>(this, Volitans.class));
            this.targetSelector.addGoal(4, new HurtByTargetGoal(this) {
                @Override
                public boolean canUse() {
                    return !Volitans.this.isVehicle() && super.canUse();
                }

                @Override
                public boolean canContinueToUse() {
                    return !Volitans.this.isVehicle() && super.canContinueToUse();
                }
            });
            this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                    target -> shouldAggroOnSight()) {
                @Override
                public boolean canUse() {
                    return !Volitans.this.isVehicle() && super.canUse();
                }

                @Override
                public boolean canContinueToUse() {
                    return !Volitans.this.isVehicle() && super.canContinueToUse();
                }
            });
            this.targetSelector.addGoal(6, new DragonRandomHuntTargetGoal(
                    this,
                    80,
                    this::shouldAggroOnSight,
                    this::shouldRandomlyAggroSeaLife
            ));
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

        if (isFlying() || canRecover) {
            setGoingUp(goingUp);
            setGoingDown(goingDown);
        } else {
            setGoingUp(false);
            setGoingDown(false);
        }
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
        this.entityData.define(DATA_TAMING_STUNNED, false);
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
        return evaluateStandardFlightMode(false);
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
        return !isFlying() && getEffectiveGroundState() == 2;
    }

    public boolean isSwimmingMoving() {
        if (!isInWaterOrBubble() || isFlying()) {
            return false;
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
    public void setRunning(boolean running) {
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
        if (takeoff && !wasTakeoff && !level().isClientSide && !isBaby()) {
            getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_TAKEOFF.get(), 2.0f, 1.0f, 50);
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
        return this.onGround() || (this.isInWaterOrBubble() && !this.isUnderWater());
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

    @Override
    protected void onRiderTakeoffRequest(Player player) {
        if (shouldSuppressTakeoffInput()) {
            return;
        }
        if (canRecoverTakeoffFromFall()) {
            setGoingUp(true);
            setGoingDown(false);
            startTakeoffSequence(0.11D, TAKEOFF_ANIMATION_TICKS);
            return;
        }
        if (!this.isInWaterOrBubble()) {
            clearGroundMobilityState();
        }
        riderFlightComponent.requestRiderTakeoff();
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
                shouldSuppressTakeoffInput() || isUltimateSlamActive() || isBurrowing(),
                this.fallDistance,
                getDeltaMovement()
        );
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
    protected boolean tryReleaseHeldRidingAbility(String abilityName) {
        if (isBurrowing()) {
            return true;
        }
        if (VolitansAbilities.VOLITANS_POISON_BALL_ID.equals(abilityName)) {
            var active = combatManager.getActiveAbility();
            if (active != null && active.getAbilityType() == VolitansAbilities.VOLITANS_POISON_BALL) {
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
        if (isBurrowing() || isFlying() || isInWaterOrBubble() || riderBackDashCooldownTicks > 0 || riderForwardDashing
                || riderBackDashing || riderBackDashRecoveryTicks > 0
                || riderSideDodging || riderSideDodgeRecoveryTicks > 0
                || !isAlive() || isDying() || isOrderedToSit() || isInSitTransition() || (isTamingStunned() && !isTame())) {
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
        scheduleBackwardDashSpikes();
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
                || !isAlive() || isDying() || isOrderedToSit() || isInSitTransition() || (isTamingStunned() && !isTame())) {
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
            if (tamingAbortCalmTicks > 0) {
                tamingAbortCalmTicks--;
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
            tickBackwardDashSpikes();
            if (riderSideDodging) {
                handleRiderSideDodgeMovement();
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
            if (aiGroundMobilityCooldownTicks > 0) {
                aiGroundMobilityCooldownTicks--;
            }
            handleAmbientSounds();
            tickWaterPreferenceTimers();
            tickBreathGaugeEnergy();
            takeoffComponent.tick();
            groundedAerialRecoveryTicks = isUltimateSlamActive()
                    ? 0
                    : DragonGroundedAerialRecovery.tick(
                            level(),
                            onGround(),
                            isInWaterOrBubble(),
                            isInLava(),
                            isTakeoff(),
                            isFlying(),
                            isHovering(),
                            isLanding(),
                            true,
                            getDeltaMovement(),
                            groundedAerialRecoveryTicks,
                            GROUNDED_AERIAL_RECOVERY_TICKS,
                            0.05D,
                            this::markLandedNow
                    );
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
        tickBarrelRollLogic();
        tickPitchingLogic();

        this.noPhysics = false;
        boolean shouldUseAirNavigation = isFlying() || isTakeoff() || isLanding() || isHovering();
        if (shouldUseAirNavigation) {
            this.setNoGravity(true);
            switchToAirNavigation();
        } else {
            this.setNoGravity(false);
            switchToGroundNavigation();
        }

        if (!this.level().isClientSide
                && this.navigationModeController.isUsingAirNavigation()
                && shouldUseAirNavigation
                && !this.isVehicle()
                && (this.isLanding() || this.getTarget() == null)
                && !this.isAiSpecialCombatActive()
                && !this.isAiSpecialCombatReserved()) {
            this.asyncAirController.serverTick();
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
        this.navigationModeController.switchToAir();
    }

    private void switchToGroundNavigation() {
        this.navigationModeController.switchToGround();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Volitans> movement =
                new AnimationController<>(this, "movement", 5, animationHandler::movementPredicate);
        movement.setSoundKeyframeHandler(event -> {
            String soundKey = event.getKeyframeData().getSound();
            if (soundKey != null && !soundKey.isEmpty()) {
                handleAnimationSound(soundKey);
            }
        });
        controllers.add(movement);

        AnimationController<Volitans> actions =
                new AnimationController<>(this, "actions", 5, animationHandler::actionPredicate);
        actions.setSoundKeyframeHandler(event -> {
            String soundKey = event.getKeyframeData().getSound();
            if (soundKey != null && !soundKey.isEmpty()) {
                handleAnimationSound(soundKey);
            }
        });
        animationHandler.setupActionController(actions);
        controllers.add(actions);

        AnimationController<Volitans> instant =
                new AnimationController<>(this, "instant", 1, animationHandler::instantActionPredicate);
        instant.setSoundKeyframeHandler(event -> {
            String soundKey = event.getKeyframeData().getSound();
            if (soundKey != null && !soundKey.isEmpty()) {
                handleAnimationSound(soundKey);
            }
        });
        animationHandler.setupInstantActionController(instant);
        controllers.add(instant);
    }

    private void handleAnimationSound(String soundKey) {
        DragonSoundProfile profile = getSoundProfile();
        if (profile != null) {
            boolean handled = profile.handleAnimationSound(getSoundHandler(), this, soundKey, null);
            if (!handled) {
                getSoundHandler().playVocal(soundKey);
            }
        }
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
        Volitans baby = com.leon.saintsdragons.common.registry.ModEntities.VOLITANS.get().create(level);
        if (baby != null) {
            assignMotherToBaby(baby, otherParent);
            baby.setGender(this.random.nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE);
            java.util.UUID ownerId = this.getOwnerUUID();
            if (ownerId != null) {
                baby.setOwnerUUID(ownerId);
                baby.setTame(true);
            }
            baby.setAge(-24000);
            baby.setBaby(true);
            baby.applyConfiguredAttributes();
            baby.setHealth(baby.getMaxHealth());
            net.minecraft.core.BlockPos safePos = findSafeBabySpawnPos(level, this.blockPosition());
            double spawnY = safePos != null ? safePos.getY() : this.getY();
            baby.moveTo(this.getX(), spawnY, this.getZ(), this.getYRot(), 0.0F);
            registerToOwnerCodex(baby, level);
        }
        return baby;
    }

    @Override
    public boolean canMate(@NotNull Animal otherAnimal) {
        if (!this.canBreed()) {
            return false;
        }

        if (otherAnimal instanceof Volitans otherDragon) {
            if (this.isFemale() == otherDragon.isFemale()) {
                return false;
            }
            return otherDragon.canBreed();
        }

        return false;
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
    public void configureEggBlockEntity(BlockEntity blockEntity, @Nullable com.leon.saintsdragons.server.entity.base.DragonEntity partner) {
        if (!(blockEntity instanceof VolitansEggBlockEntity eggEntity)) {
            return;
        }

        java.util.UUID ownerUUID = resolveEggOwnerUUID(partner);
        if (ownerUUID != null) {
            eggEntity.setOwnerUUID(ownerUUID);
        }

        eggEntity.setBabyGender(this.getRandom().nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE);
    }

    @Override
    protected int getMaxTextureVariant() {
        return VARIANTS.maxId();
    }

    @Override
    protected int chooseSpawnTextureVariant(@NotNull net.minecraft.world.level.ServerLevelAccessor levelAccessor,
                                            @NotNull net.minecraft.world.DifficultyInstance difficulty,
                                            @NotNull MobSpawnType reason,
                                            @Nullable net.minecraft.world.entity.SpawnGroupData spawnData,
                                            @Nullable CompoundTag spawnTag) {
        return rollAdultVariant();
    }

    @Override
    protected int chooseAdultTextureVariant() {
        return rollAdultVariant();
    }

    @Override
    public java.util.Map<String, Integer> getTextureVariantNameMap() {
        return VARIANTS.nameMap();
    }

    private int rollAdultVariant() {
        return VARIANTS.roll(this.getRandom());
    }

    @Override
    public boolean isFood(@NotNull ItemStack stack) {
        return stack.is(Items.COD)
                || stack.is(Items.SALMON)
                || stack.is(Items.PUFFERFISH)
                || stack.is(Items.TROPICAL_FISH)
                || stack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
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

    public boolean isAwaitingTamingFeed() {
        return tamingController.isAwaitingFeed();
    }

    public void abortTamingAttempt() {
        clearTamingRecovery();
        resetTamingFailures();
        setTarget(null);
        setAggressive(false);
        setLastHurtByMob(null);
        this.setLastHurtByPlayer(null);
        this.combatManager.clearAllStates();
        this.hurtMarked = false;
        if (isAlive()) {
            setHealth(getMaxHealth());
        }
        tamingAbortCalmTicks = Math.max(tamingAbortCalmTicks, 100);
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
        if (isFlying() || isTakeoff() || isLanding() || isHovering()) {
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
        tag.putBoolean("RiderPitchKeyMode", isRiderPitchKeyMode());
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
        if (level().isClientSide) {
            return;
        }

        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.VOLITANS_ID);
        double eggDropChance = config.extraDouble("egg_drop_chance", 0.12D);

        if (getGender() == DragonGender.FEMALE && this.random.nextDouble() < eggDropChance) {
            this.spawnAtLocation(ModItems.VOLITANS_EGG.get());
        }

        dropFishType(Items.SALMON);
        dropFishType(Items.COD);
        dropFishType(Items.TROPICAL_FISH);
        dropFishType(Items.PUFFERFISH);
    }

    private void dropFishType(net.minecraft.world.item.Item fishItem) {
        if (this.random.nextFloat() >= getConfiguredExtra("fish_drop_chance", FISH_DROP_CHANCE)) {
            return;
        }
        int amount = Mth.nextInt(this.random, 1, 3);
        this.spawnAtLocation(new ItemStack(fishItem, amount));
    }

    /**
     * Keep loaded position for airborne states so reconnect while flying doesn't drop/eject.
     */
    @Override
    protected boolean repositionEntityAfterLoad() {
        return !isFlying() && !isTakeoff() && !isLanding() && !isHovering();
    }

    public boolean isFlightControllerStuck() {
        if (!this.navigationModeController.isUsingAirNavigation()) {
            return false;
        }
        AsyncFlightController.PathState state = this.asyncAirController.getState();
        return state == AsyncFlightController.PathState.STUCK
                || state == AsyncFlightController.PathState.FAILED;
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
        if (source.getEntity() instanceof Pufferfish || source.getDirectEntity() instanceof Pufferfish) {
            return false;
        }
        if (source.is(DamageTypes.MAGIC) && this.hasEffect(MobEffects.POISON)) {
            return false;
        }
        if (tryReactiveHitEvade(source, amount)) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && shouldDropCombatSpine(source, amount)) {
            this.spawnAtLocation(ModItems.VOLITANS_SPINE.get());
            spineDropCooldownTicks = SPINE_DROP_COOLDOWN_TICKS;
        }
        return hurt;
    }

    private boolean shouldDropCombatSpine(@NotNull DamageSource source, float amount) {
        if (level().isClientSide || amount <= 0.0F || spineDropCooldownTicks > 0) {
            return false;
        }
        if (this.random.nextDouble() >= getConfiguredExtra("spine_drop_chance", 1.0D)) {
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
                ? tryAiGroundBackstep(attacker)
                : tryAiGroundDodge(attacker);
        if (!evaded) {
            evaded = this.getRandom().nextBoolean()
                    ? tryAiGroundBackstep(attacker)
                    : tryAiGroundDodge(attacker);
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
    public double getShakeDistance() {
        return 20.0D;
    }

    @Override
    protected boolean isRidingAbilityAllowed(DragonAbilityType<?, ?> abilityType) {
        return abilityType == VolitansAbilities.VOLITANS_ROAR
                || abilityType == VolitansAbilities.VOLITANS_BURROW
                || abilityType == VolitansAbilities.VOLITANS_BITE
                || abilityType == VolitansAbilities.VOLITANS_CLAW
                || abilityType == VolitansAbilities.VOLITANS_BREATH
                || abilityType == VolitansAbilities.VOLITANS_POISON_BALL
                || abilityType == VolitansAbilities.VOLITANS_HORN_GORE
                || abilityType == VolitansAbilities.VOLITANS_ULTIMATE;
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
        float clamped = Mth.clamp(energy, 0.0F, 1.0F);
        this.entityData.set(DATA_WATER_BREATH_ENERGY, clamped);
        this.entityData.set(DATA_POISON_BREATH_ENERGY, clamped);
        if (clamped >= BREATH_REARM_THRESHOLD && (isWaterBreathDepleted() || isPoisonBreathDepleted())) {
            setWaterBreathDepleted(false);
        }
    }

    public float getPoisonBreathEnergy() {
        return getWaterBreathEnergy();
    }

    public void setPoisonBreathEnergy(float energy) {
        setWaterBreathEnergy(energy);
    }

    public float getCurrentBreathEnergy() {
        return getWaterBreathEnergy();
    }

    public void setCurrentBreathEnergy(float energy) {
        setWaterBreathEnergy(energy);
    }

    public boolean hasCurrentBreathEnergy() {
        return getCurrentBreathEnergy() > BREATH_DEPLETED_THRESHOLD;
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

    public boolean isCurrentBreathDepleted() {
        return isWaterBreathDepleted();
    }

    public void setCurrentBreathDepleted(boolean depleted) {
        setWaterBreathDepleted(depleted);
    }

    public boolean canUseCurrentBreathMode() {
        return hasCurrentBreathEnergy() && !isCurrentBreathDepleted();
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
            setWaterBreathEnergy(getWaterBreathEnergy() + (float) getConfiguredExtra("breath_regen_per_tick", 0.0025D));
        }
    }

    public DragonAttributeConfig getConfiguredDragonAttributes() {
        return DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.VOLITANS_ID);
    }

    public float getConfiguredAbilityDamage(String key, float fallback) {
        return (float) getConfiguredDragonAttributes().abilityDamage(key, fallback);
    }

    public double getConfiguredExtra(String key, double fallback) {
        return getConfiguredDragonAttributes().extraDouble(key, fallback);
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

    public void handleAiLandingComplete() {
        if (isInWaterOrBubble()) {
            suppressSleep(60);
            markLandedNow();
            return;
        }
        if (!level().isClientSide) {
            triggerAnim("actions", "landed");
            if (!isBaby()) {
                getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_LANDED.get(), 2.0f, 1.0f, 32);
            }
            suppressSleep(60);
        }
        markLandedNow();
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
        if (isFlying() || isTakeoff() || isLanding() || isHovering() || isInWaterOrBubble() || isBurrowing() || (isTamingStunned() && !isTame())) {
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
        if (isFlying() || isTakeoff() || isLanding() || isHovering() || isInWaterOrBubble() || isBurrowing() || (isTamingStunned() && !isTame())) {
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
        scheduleBackwardDashSpikes();
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
        this.riderBackDashSpikeDelayTicks = RIDER_BACK_DASH_SPIKE_DELAY_TICKS;
        this.setDeltaMovement(vec);
        this.getNavigation().stop();
        this.hasImpulse = true;
    }

    private void scheduleBackwardDashSpikes() {
        this.riderBackDashSpikeDelayTicks = RIDER_BACK_DASH_SPIKE_DELAY_TICKS;
    }

    private void tickBackwardDashSpikes() {
        if (level().isClientSide || riderBackDashSpikeDelayTicks <= 0) {
            return;
        }
        if (--riderBackDashSpikeDelayTicks == 0) {
            fireBackwardDashSpikes();
        }
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
        this.riderBackDashSpikeDelayTicks = 0;
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
        this.riderBackDashSpikeDelayTicks = 0;
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
        this.riderBackDashSpikeDelayTicks = 0;

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

        boolean inWater = this.isInWaterOrBubble();
        if ((!isFlying() && !inWater) || isLanding()) {
            flightPitchRad = 0f;
            smoothedPlayerPitchRad = 0f;
            this.entityData.set(DATA_FLIGHT_PITCH, 0f);
            return;
        }

        float targetPitchRad = 0f;
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            boolean useKeyPitch = isRiderPitchKeyMode();
            if (useKeyPitch) {
                float rawKeyPitchRad = 0f;
                if (isGoingUp()) {
                    rawKeyPitchRad = (float) -Math.toRadians(25.0F);
                } else if (isGoingDown()) {
                    rawKeyPitchRad = (float) Math.toRadians(25.0F);
                }
                smoothedPlayerPitchRad = smoothedPlayerPitchRad * 0.65f + rawKeyPitchRad * 0.35f;
                targetPitchRad = Mth.clamp(smoothedPlayerPitchRad, -Mth.HALF_PI, Mth.HALF_PI);
            } else {
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
            }
        } else {
            Vec3 velocity = getDeltaMovement();
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            double minPitchSpeed = inWater ? 0.05D : 0.15D;
            if (horizontalSpeed > minPitchSpeed) {
                targetPitchRad = (float) Math.atan2(-velocity.y, horizontalSpeed);
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

    public float getAccumulatedRoll() {
        return this.entityData.get(DATA_ACCUMULATED_ROLL);
    }

    public void setAccumulatedRoll(float radians) {
        this.entityData.set(DATA_ACCUMULATED_ROLL, radians);
    }

    public void addAccumulatedRoll(float radians) {
        setAccumulatedRoll(getAccumulatedRoll() + radians);
    }

    @Override
    protected boolean canUseBarrelRoll() {
        return isFlying()
                && !isInWaterOrBubble()
                && !areRiderControlsLocked()
                && !isGroundMobilityActive()
                && !riderForwardDashing
                && !riderBackDashing
                && riderBackDashRecoveryTicks <= 0
                && !riderSideDodging
                && riderSideDodgeRecoveryTicks <= 0;
    }

    @Override
    protected boolean shouldEaseAirAutoAlign() {
        if (!isFlying() || isInWaterOrBubble() || areRiderControlsLocked()) {
            return false;
        }

        if (Math.abs(this.entityData.get(DATA_RIDER_STRAFE)) > 0.05f) {
            return false;
        }

        return Math.abs(this.entityData.get(DATA_RIDER_FORWARD)) > 0.05f;
    }

    @Override
    protected boolean isActivelyBarrelRolling() {
        return isFlying()
                && !isInWaterOrBubble()
                && !areRiderControlsLocked()
                && !isGroundMobilityActive()
                && !riderForwardDashing
                && !riderBackDashing
                && riderBackDashRecoveryTicks <= 0
                && !riderSideDodging
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

    private boolean shouldAggroOnSight() {
        if (isTame() || isBaby()) {
            return false;
        }
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.VOLITANS_ID);
        return config.extraBoolean("aggressive_wild", true);
    }

    private boolean shouldRandomlyAggroSeaLife(@Nullable LivingEntity target) {
        if (!shouldAggroOnSight() || target == null) {
            return false;
        }
        return target instanceof Dolphin
                || target instanceof Squid
                || target instanceof GlowSquid
                || target instanceof TropicalFish
                || target instanceof Pufferfish;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if ((isTamingStunned() || tamingAbortCalmTicks > 0) && target != null) {
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
                return riderFlightComponent.shouldClearFlightStateInWater(riderTakeoffTicks);
            }

            @Override
            public void onWaterFlightCleared() {
                markLandedNow();
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
                triggerAnim("actions", "landed");
                if (!isBaby()) {
                    getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_LANDED.get(), 2.0f, 1.0f, 32);
                }
                lockRiderControls(LANDED_CONTROL_LOCK_TICKS);
                markLandedNow();
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
        var config = com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader.getInstance()
                .getConfig(com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader.VOLITANS_ID);
        setAttributeBase(Attributes.MAX_HEALTH, isBaby() ? BABY_MAX_HEALTH : config.maxHealth());
        setAttributeBase(Attributes.MOVEMENT_SPEED, 0.30D);
        setAttributeBase(Attributes.FLYING_SPEED, config.flyingSpeed());
        setAttributeBase(Attributes.ARMOR, isBaby() ? BABY_ARMOR : config.armor());
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

    private void handleAmbientSounds() {
        if (nextAmbientSoundDelay <= 0) {
            resetAmbientSoundTimer();
        }
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

        ambientSoundTimer++;
        if (ambientSoundTimer < nextAmbientSoundDelay) {
            return;
        }

        playAmbientGrumble();
        resetAmbientSoundTimer();
    }

    private void playAmbientGrumble() {
        float roll = this.getRandom().nextFloat();
        String vocalKey = roll < 0.34f ? "grumble1" : (roll < 0.67f ? "grumble2" : "grumble3");
        this.getSoundHandler().playVocal(vocalKey);
    }

    private void resetAmbientSoundTimer() {
        RandomSource random = getRandom();
        ambientSoundTimer = 0;
        int range = Math.max(1, MAX_AMBIENT_DELAY - MIN_AMBIENT_DELAY + 1);
        nextAmbientSoundDelay = MIN_AMBIENT_DELAY + random.nextInt(range);
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
        return SIT_DOWN_ANIMATION_TICKS;
    }

    private int getSitDownAnimationTicks() {
        return SIT_DOWN_ANIMATION_TICKS;
    }

    private int getSitUpAnimationTicks() {
        return SIT_UP_ANIMATION_TICKS;
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
                && supportsSleep()
                && getSleepPreferences().canSleepDuringConditions(level())
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
        net.minecraft.core.BlockPos.MutableBlockPos cursor = new net.minecraft.core.BlockPos.MutableBlockPos(x, startY, z);

        for (int y = startY; y <= maxY; y++) {
            cursor.setY(y);
            if (!level().getFluidState(cursor).is(net.minecraft.tags.FluidTags.WATER)) {
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
        return SLEEP_FALL_ASLEEP_ANIMATION_TICKS;
    }

    @Override
    protected int getSleepWakeUpDuration() {
        return SLEEP_WAKE_UP_ANIMATION_TICKS;
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
    protected boolean shouldStaySeatedAfterWake() {
        return !isInWaterOrBubble() && (getCommand() == 1 || isOrderedToSit());
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
        setFlying(false);
        setLanding(false);
        setTakeoff(false);
        setHovering(false);
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
            triggerAnim("actions", "fall_asleep_underwater");
        } else {
            triggerAnim("actions", "fall_asleep");
        }
    }

    @Override
    protected void onSleepLoopAnimation() {
        if (isInWaterOrBubble()) {
            triggerAnim("actions", "sleep_underwater");
        } else {
            triggerAnim("actions", "sleep");
            setOrderedToSit(true);
        }
    }

    @Override
    protected void onSleepWakeUpAnimation() {
        if (isInWaterOrBubble()) {
            triggerAnim("actions", "wake_up_underwater");
            setOrderedToSit(false);
        } else {
            triggerAnim("actions", "wake_up");
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

    @Override
    protected void onSleepWakeUpImmediate() {
        setOrderedToSit(false);
    }

    private void updateSittingProgress() {
        if (level().isClientSide) {
            return;
        }

        if (this.isInWaterOrBubble()) {
            if (isSittingDown || isStandingUp || sitTransitionTicks > 0) {
                isSittingDown = false;
                isStandingUp = false;
                sitTransitionTicks = 0;
            }
            if (getSitProgress() != 0f || getPrevSitProgress() != 0f) {
                clearSitProgress();
            }
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
