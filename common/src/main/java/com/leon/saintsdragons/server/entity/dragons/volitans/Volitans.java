package com.leon.saintsdragons.server.entity.dragons.volitans;

import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.common.registry.volitans.VolitansAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonFloatGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonFollowOwnerGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonGroundWanderGoal;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.controller.volitans.VolitansRiderController;
import com.leon.saintsdragons.server.entity.dragons.volitans.handlers.VolitansAnimationHandler;
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
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
    private static final EntityDataAccessor<Float> DATA_SCREEN_SHAKE_AMOUNT =
            SynchedEntityData.defineId(Volitans.class, EntityDataSerializers.FLOAT);

    private static final double RIDER_WALK_SPEED = 0.24D;
    private static final double RIDER_RUN_SPEED = 0.34D;
    private static final double RIDER_SWIM_SPEED = 0.42D;
    private static final double RIDER_FLY_SPEED = 0.38D;
    public static final int TAKEOFF_ANIMATION_TICKS = 30;
    private static final int LANDED_CONTROL_LOCK_TICKS = 20;
    private static final double LANDING_BLEND_ALTITUDE = 8.0D;
    public static final double RIDER_GLIDE_ALTITUDE_THRESHOLD = 40.0D;
    public static final double RIDER_GLIDE_ALTITUDE_EXIT = 30.0D;
    public static final double RIDER_LOW_ALTITUDE_GLIDE_THRESHOLD = 6.0D;
    public static final double RIDER_WATER_SURFACE_LEVEL = 62.0D;
    public static final double RIDER_WATER_SURFACE_TOLERANCE = 2.0D;
    public static final int RIDER_WATER_SCAN_RADIUS = 2;
    public static final int RIDER_WATER_SCAN_DEPTH = 8;
    private static final float SHAKE_DECAY_PER_TICK = 0.045F;

    private final AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);
    private final VolitansAnimationHandler animationHandler = new VolitansAnimationHandler(this);
    private final VolitansRiderController riderController;
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final java.util.Map<String, Vec3> clientLocatorCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final DragonPathNavigateGround groundNav;
    private final FlyingPathNavigation airNav;
    private boolean usingAirNav;
    private int takeoffTicks;
    private int timeFlying;
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

    public Volitans(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setRideable();
        this.setMaxUpStep(1.0F);
        this.riderController = new VolitansRiderController(this);

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
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new DragonFloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(4, new DragonFollowOwnerGoal<>(this, DragonFollowOwnerGoal.FollowConfig.forCindervane()));
        this.goalSelector.addGoal(5, new DragonGroundWanderGoal<>(this, 0.9D, 70));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        return new DragonPathNavigateGround(this, level);
    }

    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case ABILITY_USE, ABILITY_STOP -> true;
            default -> super.supportsRiderAction(action);
        };
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
        this.entityData.define(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
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
    protected int getFlightMode() {
        if (!isFlying()) {
            riderHighAltitudeGlide = false;
            return -1;
        }
        if (isTakeoff() && takeoffTicks > 0) {
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
            this.entityData.set(DATA_TAKEOFF, false);
            this.entityData.set(DATA_HOVERING, false);
            this.entityData.set(DATA_LANDING, false);
            this.takeoffTicks = 0;
        }
    }

    @Override
    public void setTakeoff(boolean takeoff) {
        this.entityData.set(DATA_TAKEOFF, takeoff);
        if (takeoff) {
            this.takeoffTicks = TAKEOFF_ANIMATION_TICKS;
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
        return this.onGround() || this.isInWaterOrBubble();
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
        if (canTakeoff()) {
            setFlying(true);
            setTakeoff(true);
            setLanding(false);
            setHovering(false);
            setGoingUp(true);
            switchToAirNavigation();
        }
    }

    @Override
    protected void onRiderAbilityUse(Player player, String abilityName) {
        if (abilityName != null && !abilityName.isEmpty()) {
            useRidingAbility(abilityName);
        }
    }

    @Override
    protected void onRiderAbilityStop(Player player, String abilityName) {
        if (abilityName != null && !abilityName.isEmpty()) {
            forceEndActiveAbility();
        }
    }

    @Override
    protected boolean handleCustomRiderAction(ServerPlayer player, DragonRiderAction action, String abilityName, boolean locked) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        tickScreenShake();

        if (!this.level().isClientSide) {
            tickRiderControlLock();
            this.soundHandler.tick();

            if (takeoffTicks > 0) {
                takeoffTicks--;
                if (takeoffTicks == 0) {
                    setTakeoff(false);
                }
            }

            if (isFlying()) {
                timeFlying++;
            } else {
                timeFlying = 0;
            }

            tickRiderLandingBlendTimer();

            if (this.isInWaterOrBubble() && isFlying() && !isGoingUp()) {
                markLandedNow();
            }

            if (isFlying()) {
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

        if (isFlying()) {
            this.setNoGravity(true);
            switchToAirNavigation();
            if (takeoffTicks > 0 && this.getDeltaMovement().y < 0.12D) {
                this.setDeltaMovement(this.getDeltaMovement().x, 0.12D, this.getDeltaMovement().z);
                this.hasImpulse = true;
            }
        } else {
            this.setNoGravity(false);
            switchToGroundNavigation();
        }

        if (!this.level().isClientSide) {
            tickAnimationStates();
        }
    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
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
        if (areRiderControlsLocked()) {
            super.travel(Vec3.ZERO);
            return;
        }

        if (this.isVehicle() && this.getControllingPassenger() instanceof Player rider
                && this.isTame() && this.isOwnedBy(rider)) {
            if (!isFlying() && isGoingUp() && canTakeoff()) {
                setFlying(true);
                setTakeoff(true);
                setLanding(false);
                setHovering(false);
                switchToAirNavigation();
            }

            Vec3 riddenInput = this.getRiddenInput(rider, motion);
            if (isFlying()) {
                riderController.handleFlightTravel(rider, riddenInput);
                return;
            }

            if (this.isInWaterOrBubble()) {
                riderController.handleSwimTravel(rider, riddenInput);
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
        ItemStack heldItem = player.getItemInHand(hand);

        if (this.isTame() && this.isOwnedBy(player) && hand == InteractionHand.MAIN_HAND) {
            if (!player.isCrouching() && heldItem.isEmpty() && !this.isVehicle() && this.canOwnerMount(player)) {
                if (!this.level().isClientSide) {
                    if (this.isOrderedToSit()) {
                        this.setOrderedToSit(false);
                    }
                    if (this.getCommand() == 1) {
                        this.setCommand(0);
                    }
                    this.setTarget(null);
                    this.getNavigation().stop();
                    player.startRiding(this);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
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
    public RiderAbilityBinding getPrimaryRiderAbility() {
        return new RiderAbilityBinding(VolitansAbilities.VOLITANS_ROAR_ID, RiderAbilityBinding.Activation.PRESS);
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
        return this.position().add(0.0D, this.getBbHeight() * 0.72D, 0.9D);
    }

    public void setClientLocatorPosition(String name, Vec3 pos) {
        if (name == null || pos == null) {
            return;
        }
        this.clientLocatorCache.put(name, pos);
    }

    @Override
    public Vec3 getClientLocatorPosition(String name) {
        if (name == null) {
            return null;
        }
        return this.clientLocatorCache.get(name);
    }

    @Override
    public AgeableMob getBreedOffspring(net.minecraft.server.level.ServerLevel level, AgeableMob otherParent) {
        return com.leon.saintsdragons.common.registry.ModEntities.VOLITANS.get().create(level);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadRideableData(tag);
        applyConfiguredAttributes();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        saveRideableData(tag);
    }

    @Override
    public double getSwimSpeed() {
        return RIDER_SWIM_SPEED;
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
                || type == VolitansAbilities.VOLITANS_BITE
                || type == VolitansAbilities.VOLITANS_CLAW
                || type == VolitansAbilities.VOLITANS_HORN_GORE) {
            combatManager.tryUseAbility(type);
        }
    }

    public void forceEndActiveAbility() {
        combatManager.forceEndActiveAbility();
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

    private boolean isRiddenByOwner() {
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

    private void tickRiderLandingBlendTimer() {
        trackRiderAirborneForLanding();

        if (!isVehicle() || !isFlying() || onGround()) {
            boolean touchdownFromFlight = consumeRiderTouchdownFromAir(0.25D);
            boolean completedLanding = isLanding() && onGround();
            if ((completedLanding || touchdownFromFlight) && onGround() && !level().isClientSide) {
                triggerAnim("actions", "landed");
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
        return 24.0F;
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
