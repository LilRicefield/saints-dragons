package com.leon.saintsdragons.server.entity.base;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.server.ai.navigation.DragonNavigationModeController;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncFlightController;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncFlightMoveControl;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncFlyingPathNavigation;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.flight.DragonBarrelRollHelper;
import com.leon.saintsdragons.server.flight.DragonFallRecovery;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import com.leon.saintsdragons.server.flight.DragonFlightVisuals;
import com.leon.saintsdragons.server.flight.DragonGroundedAerialRecovery;
import com.leon.saintsdragons.server.flight.DragonRiderFlight;
import com.leon.saintsdragons.server.flight.DragonTakeoff;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.BooleanSupplier;

public abstract class RideableFlyingDragon extends RideableDragonBase implements FlyingAnimal, DragonFlightCapable {
    protected static final double RIDER_GLIDE_ALTITUDE_THRESHOLD = 40.0D;
    protected static final double RIDER_GLIDE_ALTITUDE_EXIT = 30.0D;
    protected static final double RIDER_LOW_ALTITUDE_GLIDE_THRESHOLD = 6.0D;
    public static final double LANDING_BLEND_ALTITUDE = 8.0D;
    protected static final double RIDER_WATER_SURFACE_LEVEL = 62.0D;
    protected static final double RIDER_WATER_SURFACE_TOLERANCE = 2.0D;
    protected static final int RIDER_WATER_SCAN_RADIUS = 2;
    protected static final int RIDER_WATER_SCAN_DEPTH = 8;
    protected static final double MIN_AIRBORNE_LANDING_HORIZONTAL = 6.0D;
    protected static final int DEFAULT_GROUNDED_AERIAL_RECOVERY_TICKS = 8;
    protected static final double DEFAULT_GROUNDED_AERIAL_RECOVERY_UPWARD_TOLERANCE = 0.05D;
    protected static final float DEFAULT_BARREL_ROLL_INPUT_SPEED = 0.275F;
    protected static final DragonBarrelRollHelper.Config DEFAULT_BARREL_ROLL_CONFIG =
            new DragonBarrelRollHelper.Config(
                    0.88F,
                    0.30F,
                    0.04F,
                    0.005F,
                    Mth.HALF_PI
            );

    private final DragonFlightStateEvaluator.State flightModeState = new DragonFlightStateEvaluator.State();
    protected final DragonPathNavigateGround groundNav;
    protected final FlyingPathNavigation airNav;
    protected final AsyncFlightController asyncAirController;
    protected final AsyncFlightMoveControl asyncAirMoveControl;
    protected final MoveControl groundMoveControl;
    protected final DragonNavigationModeController navigationModeController;
    protected final DragonTakeoff takeoffComponent;
    protected final DragonRiderFlight riderFlightComponent;
    private int groundedAerialRecoveryTicks = 0;
    private int riderTakeoffTicks = 0;
    private float accumulatedRoll = 0.0F;
    private int riderLandingBlendTicks = 0;
    private float prevSmoothedRoll = 0.0F;
    private float smoothedRoll = 0.0F;

    protected RideableFlyingDragon(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.asyncAirController = new AsyncFlightController(this);
        this.asyncAirMoveControl = new AsyncFlightMoveControl(this, this.asyncAirController);
        this.groundNav = createGroundNavigation(level);
        this.groundMoveControl = createGroundMoveControl();
        this.airNav = createAirNavigation(level, this.asyncAirController);
        configureAirNavigation(this.airNav);
        this.navigationModeController = new DragonNavigationModeController(
                new DragonNavigationModeController.Host() {
                    @Override
                    public void setActiveNavigation(PathNavigation navigation) {
                        RideableFlyingDragon.this.navigation = navigation;
                    }

                    @Override
                    public void setActiveMoveControl(MoveControl moveControl) {
                        RideableFlyingDragon.this.moveControl = moveControl;
                    }

                    @Override
                    public void afterSwitchToGround() {
                        RideableFlyingDragon.this.afterSwitchToGroundNavigation();
                    }
                },
                this.groundNav,
                this.airNav,
                this.groundMoveControl,
                this.asyncAirMoveControl
        );
        this.navigation = this.groundNav;
        this.moveControl = this.groundMoveControl;
        this.takeoffComponent = createTakeoffComponent();
        this.riderFlightComponent = createRiderFlightComponent();
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        return new DragonPathNavigateGround(this, level);
    }

    protected DragonPathNavigateGround createGroundNavigation(Level level) {
        return new DragonPathNavigateGround(this, level);
    }

    protected MoveControl createGroundMoveControl() {
        return new MoveControl(this);
    }

    protected FlyingPathNavigation createAirNavigation(Level level, AsyncFlightController controller) {
        return new AsyncFlyingPathNavigation(this, level, controller) {
            @Override
            public boolean isStableDestination(@NotNull BlockPos pos) {
                return !this.level.getBlockState(pos.below()).isAir();
            }
        };
    }

    protected void configureAirNavigation(FlyingPathNavigation navigation) {
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(false);
        navigation.setCanPassDoors(false);
    }

    protected void afterSwitchToGroundNavigation() {
        if (onGround()) {
            setDeltaMovement(Vec3.ZERO);
            hasImpulse = false;
        } else {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x * 0.25D, motion.y, motion.z * 0.25D);
        }
    }

    public void switchToAirNavigation() {
        this.navigationModeController.switchToAir();
    }

    public void switchToGroundNavigation() {
        this.navigationModeController.switchToGround();
    }

    private DragonTakeoff createTakeoffComponent() {
        return new DragonTakeoff(new DragonTakeoff.Host() {
            @Override
            public Level level() { return RideableFlyingDragon.this.level(); }

            @Override
            public boolean isFlying() { return RideableFlyingDragon.this.isFlying(); }

            @Override
            public void setFlying(boolean value) { RideableFlyingDragon.this.setFlying(value); }

            @Override
            public void setTakeoff(boolean value) { RideableFlyingDragon.this.setTakeoff(value); }

            @Override
            public void setHovering(boolean value) { RideableFlyingDragon.this.setHovering(value); }

            @Override
            public void setLanding(boolean value) { RideableFlyingDragon.this.setLanding(value); }

            @Override
            public void switchToAirNavigation() { RideableFlyingDragon.this.switchToAirNavigation(); }

            @Override
            public Vec3 getDeltaMovement() { return RideableFlyingDragon.this.getDeltaMovement(); }

            @Override
            public void setDeltaMovement(Vec3 movement) { RideableFlyingDragon.this.setDeltaMovement(movement); }

            @Override
            public void markImpulse() { RideableFlyingDragon.this.hasImpulse = true; }

            @Override
            public void onTakeoffStarted() { RideableFlyingDragon.this.onTakeoffStarted(); }

            @Override
            public void onTakeoffEnded() { RideableFlyingDragon.this.onTakeoffEnded(); }

            @Override
            public int getTakeoffLiftDelayTicks() { return RideableFlyingDragon.this.getTakeoffLiftDelayTicks(); }
        });
    }

    private DragonRiderFlight createRiderFlightComponent() {
        return new DragonRiderFlight(new DragonRiderFlight.Host() {
            @Override
            public Entity asEntity() { return RideableFlyingDragon.this; }

            @Override
            public Level level() { return RideableFlyingDragon.this.level(); }

            @Override
            public AABB getBoundingBox() { return RideableFlyingDragon.this.getBoundingBox(); }

            @Override
            public boolean isVehicle() { return RideableFlyingDragon.this.isVehicle(); }

            @Override
            public boolean isFlying() { return RideableFlyingDragon.this.isFlying(); }

            @Override
            public boolean isTakeoff() { return RideableFlyingDragon.this.isTakeoff(); }

            @Override
            public boolean isGoingUp() { return RideableFlyingDragon.this.isGoingUp(); }

            @Override
            public boolean isUnderWater() { return RideableFlyingDragon.this.isUnderWater(); }

            @Override
            public boolean isInWaterOrBubble() { return RideableFlyingDragon.this.isInWaterOrBubble(); }

            @Override
            public boolean isTame() { return RideableFlyingDragon.this.isTame(); }

            @Override
            public boolean hasControllingRider() { return RideableFlyingDragon.this.getControllingPassenger() instanceof Player; }

            @Override
            public boolean canTakeoff() { return RideableFlyingDragon.this.canTakeoff(); }

            @Override
            public void setFlying(boolean value) { RideableFlyingDragon.this.setFlying(value); }

            @Override
            public void setHovering(boolean value) { RideableFlyingDragon.this.setHovering(value); }

            @Override
            public void setLanding(boolean value) { RideableFlyingDragon.this.setLanding(value); }

            @Override
            public void switchToAirNavigation() { RideableFlyingDragon.this.switchToAirNavigation(); }

            @Override
            public void setGoingUp(boolean value) { RideableFlyingDragon.this.setGoingUp(value); }

            @Override
            public void setGoingDown(boolean value) { RideableFlyingDragon.this.setGoingDown(value); }

            @Override
            public void stopNavigation() { RideableFlyingDragon.this.getNavigation().stop(); }

            @Override
            public void startTakeoffSequence(double minUpwardVelocity, int animationTicks) {
                RideableFlyingDragon.this.startTakeoffSequence(minUpwardVelocity, animationTicks);
            }

            @Override
            public Vec3 getDeltaMovement() { return RideableFlyingDragon.this.getDeltaMovement(); }

            @Override
            public void setDeltaMovement(Vec3 movement) { RideableFlyingDragon.this.setDeltaMovement(movement); }

            @Override
            public void markImpulse() { RideableFlyingDragon.this.hasImpulse = true; }

            @Override
            public long getGameTime() { return RideableFlyingDragon.this.level().getGameTime(); }

            @Override
            public long getLastLandingGameTime() { return RideableFlyingDragon.this.getLastLandingGameTime(); }

            @Override
            public boolean isTakeoffLocked() { return RideableFlyingDragon.this.isRiderTakeoffLocked(); }

            @Override
            public void onManualTakeoffStart() { RideableFlyingDragon.this.onManualRiderTakeoffStart(); }

            @Override
            public void setRiderTakeoffTicks(int ticks) { RideableFlyingDragon.this.setRiderTakeoffTicks(ticks); }
        }, getRiderFlightConfig());
    }

    protected DragonRiderFlight.Config getRiderFlightConfig() {
        return new DragonRiderFlight.Config(true, 0, 0.55D, 0, 0.45D, 0);
    }

    protected long getLastLandingGameTime() {
        return Long.MIN_VALUE;
    }

    protected boolean isRiderTakeoffLocked() {
        return false;
    }

    protected void onManualRiderTakeoffStart() {
    }

    public void startTakeoffSequence(double minUpwardVelocity, int animationTicks) {
        if (!canStartTakeoffSequence()) {
            return;
        }
        takeoffComponent.startTakeoff(animationTicks, minUpwardVelocity);
    }

    protected boolean canStartTakeoffSequence() {
        return canTakeoff();
    }

    protected void onTakeoffStarted() {
    }

    protected void onTakeoffEnded() {
    }

    protected int getTakeoffLiftDelayTicks() {
        return 0;
    }

    protected void tickStandardTakeoffAndGroundedAerialRecovery() {
        takeoffComponent.tick();
        groundedAerialRecoveryTicks = shouldSkipGroundedAerialRecovery()
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
                        shouldIgnoreGroundedTakeoffRecovery(),
                        getDeltaMovement(),
                        groundedAerialRecoveryTicks,
                        getGroundedAerialRecoveryGraceTicks(),
                        getGroundedAerialRecoveryUpwardVelocityTolerance(),
                        this::markLandedNow
                );
    }

    protected boolean shouldSkipGroundedAerialRecovery() {
        return false;
    }

    protected boolean shouldIgnoreGroundedTakeoffRecovery() {
        return false;
    }

    protected int getGroundedAerialRecoveryGraceTicks() {
        return DEFAULT_GROUNDED_AERIAL_RECOVERY_TICKS;
    }

    protected double getGroundedAerialRecoveryUpwardVelocityTolerance() {
        return DEFAULT_GROUNDED_AERIAL_RECOVERY_UPWARD_TOLERANCE;
    }

    public boolean isFallingForAnimation() {
        return DragonFallRecovery.isFallingForAnimation(
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

    protected boolean isUsingAirNavigation() {
        return this.navigationModeController.isUsingAirNavigation();
    }

    public boolean isFlightControllerStuck() {
        if (!isUsingAirNavigation()) {
            return false;
        }
        AsyncFlightController.PathState state = this.asyncAirController.getState();
        return state == AsyncFlightController.PathState.STUCK
                || state == AsyncFlightController.PathState.FAILED;
    }

    protected void tickAsyncFlightNavigation(boolean blockedByDirectAirCombat) {
        if (!level().isClientSide
                && isUsingAirNavigation()
                && (isFlying() || isTakeoff() || isLanding())
                && !isVehicle()
                && !blockedByDirectAirCombat) {
            this.asyncAirController.serverTick();
        }
    }

    @Override
    protected void applyRiderVerticalInput(Player player, boolean goingUp, boolean goingDown, boolean locked) {
        if (isInWater() || isInWaterOrBubble()) {
            setGoingUp(goingUp);
            setGoingDown(goingDown);
            return;
        }

        if (locked) {
            setGoingUp(false);
            setGoingDown(false);
            return;
        }

        boolean canRecover = canRecoverRiderTakeoffFromFall();
        if (goingUp && canRecover) {
            setGoingUp(true);
            setGoingDown(false);
            startTakeoffSequence(getRiderFallRecoveryTakeoffVelocity(), getRiderFallRecoveryTakeoffAnimationTicks());
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
    protected void onRiderTakeoffRequest(Player player) {
        if (canRecoverRiderTakeoffFromFall()) {
            setGoingUp(true);
            setGoingDown(false);
            startTakeoffSequence(getRiderFallRecoveryTakeoffVelocity(), getRiderFallRecoveryTakeoffAnimationTicks());
            return;
        }
        if (!isFlying()) {
            beforeStandardRiderTakeoff(player);
            requestRiderTakeoff();
        }
    }

    public void tryRiderTakeoff(Player player) {
        onRiderTakeoffRequest(player);
    }

    protected boolean canRecoverRiderTakeoffFromFall() {
        return DragonFallRecovery.canRecoverTakeoffFromFall(
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
                isRiderFallRecoveryBlocked(),
                this.fallDistance,
                getDeltaMovement()
        );
    }

    protected boolean isRiderFallRecoveryBlocked() {
        return false;
    }

    protected double getRiderFallRecoveryTakeoffVelocity() {
        return 0.11D;
    }

    protected int getRiderFallRecoveryTakeoffAnimationTicks() {
        return 20;
    }

    protected void beforeStandardRiderTakeoff(Player player) {
    }

    protected void requestRiderTakeoff() {
        riderFlightComponent.requestRiderTakeoff();
    }

    protected boolean tryAutoBreachRiderTakeoff() {
        return riderFlightComponent.tryAutoBreachTakeoff();
    }

    protected boolean hasRiderBreachTakeoffClearance() {
        return riderFlightComponent.hasBreachTakeoffClearance();
    }

    protected boolean shouldClearRiderFlightStateInWater() {
        return riderFlightComponent.shouldClearFlightStateInWater(this.riderTakeoffTicks);
    }

    protected void tickRiderTakeoff() {
        if (!level().isClientSide && riderTakeoffTicks > 0 && !isDying()) {
            riderTakeoffTicks--;
        }
    }

    public int getRiderTakeoffTicks() {
        return riderTakeoffTicks;
    }

    public void setRiderTakeoffTicks(int ticks) {
        this.riderTakeoffTicks = Math.max(0, ticks);
    }

    public @Nullable Vec3 findStandardAiLandingTarget(@Nullable LivingEntity target) {
        BlockPos origin = target != null && target.isAlive() ? target.blockPosition() : blockPosition();
        double currentAltitude = Math.max(0.0D, getY()
                - level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, getBlockX(), getBlockZ()));
        double minHorizontalDistance = currentAltitude > 6.0D ? MIN_AIRBORNE_LANDING_HORIZONTAL : 0.0D;

        for (int radius = 8; radius <= 40; radius += 8) {
            Vec3 landing = findRandomLandingTargetAround(origin, radius, minHorizontalDistance, 16);
            if (landing != null) {
                return landing;
            }
        }

        if (minHorizontalDistance > 0.0D) {
            double relaxedMinHorizontal = Math.max(3.0D, minHorizontalDistance * 0.5D);
            for (int radius = 8; radius <= 40; radius += 8) {
                Vec3 landing = findRandomLandingTargetAround(origin, radius, relaxedMinHorizontal, 16);
                if (landing != null) {
                    return landing;
                }
            }
        }

        for (int radius = 8; radius <= 40; radius += 8) {
            Vec3 landing = findRandomLandingTargetAround(origin, radius, 0.0D, 12);
            if (landing != null) {
                return landing;
            }
        }

        return null;
    }

    private @Nullable Vec3 findRandomLandingTargetAround(BlockPos origin, int radius, double minHorizontalDistance, int attempts) {
        double minHorizontalDistanceSqr = minHorizontalDistance * minHorizontalDistance;
        for (int attempt = 0; attempt < attempts; attempt++) {
            int dx = radius == 0 ? 0 : getRandom().nextInt(radius * 2 + 1) - radius;
            int dz = radius == 0 ? 0 : getRandom().nextInt(radius * 2 + 1) - radius;
            if (dx * dx + dz * dz < minHorizontalDistanceSqr) {
                continue;
            }

            Vec3 landing = landingTargetAt(origin.offset(dx, 0, dz));
            if (landing != null) {
                return landing;
            }
        }
        return null;
    }

    private @Nullable Vec3 landingTargetAt(BlockPos column) {
        if (!level().hasChunkAt(column)) {
            return null;
        }

        int surfaceY = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        BlockPos ground = new BlockPos(column.getX(), surfaceY - 1, column.getZ());
        if (!isValidStandardLandingSurface(ground)) {
            return null;
        }
        return new Vec3(column.getX() + 0.5D, ground.getY() + 1.0D, column.getZ() + 0.5D);
    }

    protected boolean isValidStandardLandingSurface(BlockPos ground) {
        if (!level().hasChunkAt(ground)) {
            return false;
        }

        BlockState state = level().getBlockState(ground);
        if (state.isAir() || !state.getFluidState().isEmpty() || !state.isFaceSturdy(level(), ground, net.minecraft.core.Direction.UP)) {
            return false;
        }

        BlockPos above = ground.above();
        BlockPos aboveTwo = above.above();
        BlockState aboveState = level().getBlockState(above);
        BlockState aboveTwoState = level().getBlockState(aboveTwo);
        return aboveState.getCollisionShape(level(), above).isEmpty()
                && aboveState.getFluidState().isEmpty()
                && aboveTwoState.getCollisionShape(level(), aboveTwo).isEmpty()
                && aboveTwoState.getFluidState().isEmpty();
    }

    public void moveAiFlightTo(@Nullable Vec3 target, double speed) {
        if (target != null) {
            getNavigation().moveTo(target.x, target.y, target.z, speed);
        }
    }

    public @Nullable Vec3 findStandardAiFlightTarget(double maxTurnDegrees, double minRange, double extraRange,
                                                     double maxHeightAboveGround, boolean widerSearch) {
        Vec3 dragonPos = position();

        for (int attempt = 0; attempt < 16; attempt++) {
            Vec3 candidate = generateStandardFlightCandidate(dragonPos, attempt, maxTurnDegrees, minRange, extraRange,
                    maxHeightAboveGround, widerSearch);
            if (isValidStandardFlightTarget(candidate)) {
                return candidate;
            }
        }

        return new Vec3(dragonPos.x, findStandardSafeFlightHeight(dragonPos.x, dragonPos.z, maxHeightAboveGround), dragonPos.z);
    }

    private @Nullable Vec3 generateStandardFlightCandidate(Vec3 dragonPos, int attempt, double maxTurnDegrees,
                                                           double minRange, double extraRange,
                                                           double maxHeightAboveGround, boolean widerSearch) {
        boolean isStuck = horizontalCollision || isFlightControllerStuck();
        float maxRot = (float) (isStuck || widerSearch ? 360.0D : maxTurnDegrees);
        float range = (float) (isStuck
                ? 30.0D + getRandom().nextDouble() * 40.0D
                : minRange + getRandom().nextDouble() * extraRange);

        float yRotOffset;
        if (isStuck && attempt < 8) {
            yRotOffset = (float) Math.toRadians(180.0D + getRandom().nextDouble() * 120.0D - 60.0D);
        } else {
            yRotOffset = (float) Math.toRadians(getRandom().nextDouble() * maxRot - (maxRot * 0.5D));
        }

        float xRotOffset = (float) Math.toRadians((getRandom().nextDouble() - 0.5D) * 20.0D);
        Vec3 targetVec = getLookAngle().scale(range).yRot(yRotOffset).xRot(xRotOffset);
        Vec3 candidate = dragonPos.add(targetVec);
        candidate = new Vec3(candidate.x, findStandardSafeFlightHeight(candidate.x, candidate.z, maxHeightAboveGround), candidate.z);

        if (!level().isLoaded(BlockPos.containing(candidate))) {
            return null;
        }
        return candidate;
    }

    protected double findStandardSafeFlightHeight(double x, double z, double maxHeightAboveGround) {
        int ix = Mth.floor(x);
        int iz = Mth.floor(z);
        int groundY = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);
        double base = 15.0D + getRandom().nextDouble() * 20.0D;
        double target = groundY + base;
        double cap = groundY + maxHeightAboveGround;
        double worldCap = level().getMaxBuildHeight() - 10.0D;
        return Math.min(Math.min(target, cap), worldCap);
    }

    public boolean isValidStandardFlightTarget(@Nullable Vec3 target) {
        if (target == null) {
            return false;
        }

        BlockHitResult result = level().clip(new ClipContext(
                getEyePosition(),
                target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));

        if (result.getType() == HitResult.Type.MISS) {
            return true;
        }

        double distanceToHit = result.getLocation().distanceTo(position());
        double distanceToTarget = target.distanceTo(position());
        return distanceToHit > distanceToTarget * 0.95D;
    }

    public boolean hasStandardTakeoffClearance(int checkHeight) {
        BlockPos dragonPos = blockPosition();
        int checkRadius = (int) Math.ceil(getBbWidth() / 2.0D);

        for (int dy = 1; dy <= checkHeight; dy++) {
            for (int dx = -checkRadius; dx <= checkRadius; dx++) {
                for (int dz = -checkRadius; dz <= checkRadius; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > checkRadius + 1) {
                        continue;
                    }

                    BlockPos checkPos = dragonPos.offset(dx, dy, dz);
                    BlockState state = level().getBlockState(checkPos);
                    if (!state.isAir() && !state.getCollisionShape(level(), checkPos).isEmpty()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public boolean isOverStandardFlightDanger() {
        BlockPos dragonPos = blockPosition();
        boolean foundSolid = false;
        boolean nearFluid = false;

        for (int i = 1; i <= 25; i++) {
            BlockPos checkPos = dragonPos.below(i);
            BlockState state = level().getBlockState(checkPos);
            if (!state.getCollisionShape(level(), checkPos).isEmpty()
                    || state.isFaceSturdy(level(), checkPos, net.minecraft.core.Direction.UP)) {
                foundSolid = true;
                break;
            }

            if (i <= 10 && !level().getFluidState(checkPos).isEmpty()) {
                nearFluid = true;
            }
        }

        return nearFluid || (!foundSolid && dragonPos.getY() < level().getMinBuildHeight() + 20);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RiderTakeoffTicks", riderTakeoffTicks);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.riderTakeoffTicks = tag.contains("RiderTakeoffTicks") ? tag.getInt("RiderTakeoffTicks") : 0;
    }

    @Override
    public void markLandedNow() {
        setFlying(false);
        setTakeoff(false);
        setLanding(false);
        setHovering(false);
        afterStandardLandingStateReset();
        clearTakeoffState();
        resetRiderTakeoffTicksAfterLanding();
        resetTimeFlyingAfterLanding();

        if (!level().isClientSide) {
            onStandardServerLanding();
            switchToGroundNavigationAfterLanding();
            setNoGravity(false);
        }
    }

    protected void afterStandardLandingStateReset() {
    }

    protected void clearTakeoffState() {
        takeoffComponent.clear();
    }

    protected void resetRiderTakeoffTicksAfterLanding() {
        this.riderTakeoffTicks = 0;
    }

    protected void resetTimeFlyingAfterLanding() {
    }

    protected void onStandardServerLanding() {
    }

    protected void switchToGroundNavigationAfterLanding() {
    }

    @Override
    public boolean canTakeoff() {
        return !isBaby();
    }

    @Override
    protected abstract int getFlightMode();

    protected int evaluateStandardFlightMode(boolean forceSurfaceGlide) {
        double altitudeAboveTerrain = getHeightmapAltitudeAboveTerrain();
        DragonFlightStateEvaluator.FlightInput input = new DragonFlightStateEvaluator.FlightInput(
                isFlying(),
                shouldPlayTakeoffForFlightMode(),
                isHovering(),
                isLanding(),
                isRiddenByOwner(),
                isGoingUp(),
                isGoingDown(),
                isAccelerating(),
                isRiddenByOwner() && (forceSurfaceGlide || shouldForceSurfaceGlide(altitudeAboveTerrain)),
                getX(),
                getY(),
                getZ(),
                this.yo,
                altitudeAboveTerrain,
                RIDER_GLIDE_ALTITUDE_THRESHOLD,
                RIDER_GLIDE_ALTITUDE_EXIT,
                getDeltaMovement()
        );
        return DragonFlightStateEvaluator.evaluateSyncedMode(flightModeState, input);
    }

    protected DragonFlightStateEvaluator.VisualState evaluateVisualFlightState(float partialTick, float flightPitchRadians) {
        return DragonFlightStateEvaluator.evaluateAnimationVisualState(
                getSyncedFlightMode(),
                isVehicle(),
                flightPitchRadians,
                getDeltaMovement(),
                isLanding(),
                getAltitudeAboveTerrain(),
                LANDING_BLEND_ALTITUDE,
                isRiderLandingBlendActive()
        );
    }

    protected void tickStandardPitchingLogic() {
        tickPitchingLandingBlendTimer();

        DragonFlightVisuals.State state = getFlightVisualState();
        EntityDataAccessor<Float> pitchAccessor = getFlightPitchAccessor();
        if (state == null || pitchAccessor == null) {
            return;
        }

        DragonFlightVisuals.beginPitchTick(state);
        if (level().isClientSide) {
            state.flightPitchRad = this.entityData.get(pitchAccessor);
            return;
        }

        if (shouldResetStandardPitch()) {
            DragonFlightVisuals.resetPitch(state);
            this.entityData.set(pitchAccessor, state.flightPitchRad);
            return;
        }

        Vec3 velocity = getDeltaMovement();
        float targetPitchRad;

        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            boolean useKeyPitch = isRiderPitchKeyMode();

            if (useKeyPitch) {
                float rawKeyPitchRad = 0f;
                if (isGoingUp()) {
                    rawKeyPitchRad = (float) Math.toRadians(getRiderKeyPitchDegrees());
                } else if (isGoingDown()) {
                    rawKeyPitchRad = (float) -Math.toRadians(getRiderKeyPitchDegrees());
                }
                targetPitchRad = DragonFlightVisuals.smoothRiderPitchInput(state, rawKeyPitchRad);
            } else {
                float riderForward = this.entityData.get(DATA_RIDER_FORWARD);
                float riderStrafe = this.entityData.get(DATA_RIDER_STRAFE);
                boolean hasMovementInput = Math.abs(riderForward) > 0.01f || Math.abs(riderStrafe) > 0.01f;

                if (hasMovementInput) {
                    float rawPlayerPitchRad = -(float) Math.toRadians(player.getXRot());
                    targetPitchRad = DragonFlightVisuals.smoothRiderPitchInput(state, rawPlayerPitchRad);
                } else {
                    DragonFlightVisuals.clearRiderPitchInput(state);
                    targetPitchRad = 0f;
                }
            }

            if (wantsRiderLandingPitch(player, useKeyPitch) && isNearStandardPitchLandingBlendTerrain()) {
                float landingPitchRad = (float) -Math.toRadians(35.0f);
                targetPitchRad = Math.min(targetPitchRad, landingPitchRad);
            }
        } else {
            targetPitchRad = DragonFlightVisuals.computeAiPitchTarget(velocity);
            if (isLanding() && shouldApplyStandardAiLandingPitch()) {
                float landingPitchRad = (float) -Math.toRadians(getStandardAiLandingPitchDegrees());
                targetPitchRad = Math.min(targetPitchRad, landingPitchRad);
            }
        }

        state.flightPitchRad = DragonFlightVisuals.approachPitch(state.flightPitchRad, targetPitchRad);
        this.entityData.set(pitchAccessor, state.flightPitchRad);

        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            if (wantsRiderLandingPitch(player, isRiderPitchKeyMode()) && isNearStandardPitchLandingBlendTerrain()) {
                triggerPitchingLandingBlend();
            }
        }
    }

    protected DragonFlightVisuals.State getFlightVisualState() {
        return null;
    }

    protected EntityDataAccessor<Float> getFlightPitchAccessor() {
        return null;
    }

    protected void tickPitchingLandingBlendTimer() {
    }

    protected void triggerPitchingLandingBlend() {
    }

    public boolean isRiderPitchKeyMode() {
        return false;
    }

    protected float getRiderKeyPitchDegrees() {
        return 25.0F;
    }

    protected boolean shouldResetStandardPitch() {
        boolean inWater = this.isInWater() || this.isInWaterOrBubble();
        return inWater
                || areRiderControlsLocked()
                || (!isFlying() && !(allowsStandardPitchWhileLanding() && isLanding()))
                || (isHovering() && shouldResetStandardPitchInHover())
                || shouldResetStandardPitchForSit()
                || isStandardPitchActionBlocked();
    }

    protected boolean allowsStandardPitchWhileLanding() {
        return true;
    }

    protected boolean shouldResetStandardPitchInHover() {
        return false;
    }

    protected boolean shouldResetStandardPitchForSit() {
        return isOrderedToSit();
    }

    protected boolean isStandardPitchActionBlocked() {
        return false;
    }

    protected boolean shouldApplyStandardAiLandingPitch() {
        return true;
    }

    protected float getStandardAiLandingPitchDegrees() {
        return 18.0F;
    }

    private boolean wantsRiderLandingPitch(Player player, boolean useKeyPitch) {
        return isGoingDown() || (!useKeyPitch && player.getXRot() > 30.0f);
    }

    private boolean isNearStandardPitchLandingBlendTerrain() {
        double altitude = getAltitudeAboveTerrain();
        return altitude != Double.POSITIVE_INFINITY && altitude >= -0.25D && altitude <= LANDING_BLEND_ALTITUDE;
    }

    protected void tickBarrelRollLogic() {
        float currentRoll = getAccumulatedRoll();
        boolean serverSide = !level().isClientSide;
        boolean ridden = isVehicle() && getControllingPassenger() != null;
        boolean barrelRollEnabled = level().isClientSide || SaintsDragonsConfig.BARREL_ROLL_ENABLED.get();
        boolean canBarrelRoll = barrelRollEnabled && ridden && canUseBarrelRoll();

        if (serverSide && canBarrelRoll && isBarrelRollInputActive()) {
            currentRoll += this.entityData.get(DATA_RIDER_STRAFE) * getBarrelRollInputSpeed();
        }

        DragonBarrelRollHelper.Output output = DragonBarrelRollHelper.tick(
                currentRoll,
                smoothedRoll,
                new DragonBarrelRollHelper.Input(
                        isBarrelRollRiddenForHelper(ridden, canBarrelRoll),
                        shouldForceBarrelRollUpright(),
                        isLanding(),
                        serverSide && canBarrelRoll && isActivelyBarrelRolling(),
                        shouldEaseAirAutoAlign(),
                        isBarrelRollLandingBlendActive(),
                        LANDING_BLEND_ALTITUDE,
                        getBarrelRollAltitudeAboveTerrain()
                ),
                getBarrelRollConfig()
        );

        setAccumulatedRoll(output.accumulatedRoll());
        prevSmoothedRoll = output.prevSmoothedRoll();
        smoothedRoll = output.smoothedRoll();
    }

    public float getAccumulatedRoll() {
        return accumulatedRoll;
    }

    public void setAccumulatedRoll(float radians) {
        accumulatedRoll = radians;
    }

    public void addAccumulatedRoll(float radians) {
        setAccumulatedRoll(getAccumulatedRoll() + radians);
    }

    public float getSmoothedRoll(float partialTick) {
        return Mth.lerp(partialTick, prevSmoothedRoll, smoothedRoll);
    }

    protected float getBarrelRollInputSpeed() {
        return DEFAULT_BARREL_ROLL_INPUT_SPEED;
    }

    protected DragonBarrelRollHelper.Config getBarrelRollConfig() {
        return DEFAULT_BARREL_ROLL_CONFIG;
    }

    protected boolean canUseBarrelRoll() {
        return !isInWaterOrBubble();
    }

    protected boolean shouldForceBarrelRollUpright() {
        return onGround() || isInWaterOrBubble();
    }

    protected boolean isBarrelRollRiddenForHelper(boolean ridden, boolean canBarrelRoll) {
        return ridden;
    }

    protected boolean shouldEaseAirAutoAlign() {
        if (!isFlying() || areRiderControlsLocked()) {
            return false;
        }

        if (Math.abs(this.entityData.get(DATA_RIDER_STRAFE)) > 0.05F) {
            return false;
        }

        return Math.abs(this.entityData.get(DATA_RIDER_FORWARD)) > 0.05F;
    }

    protected boolean isActivelyBarrelRolling() {
        return isBarrelRollInputActive();
    }

    protected boolean isBarrelRollInputActive() {
        return this.entityData.get(DATA_RIDER_FORWARD) > 0.1F
                && Math.abs(this.entityData.get(DATA_RIDER_STRAFE)) > 0.1F;
    }

    protected boolean isBarrelRollLandingBlendActive() {
        return isRiderLandingBlendActive();
    }

    protected double getBarrelRollAltitudeAboveTerrain() {
        return getAltitudeAboveTerrain();
    }

    protected boolean shouldPlayTakeoffForFlightMode() {
        return isTakeoff();
    }

    protected boolean shouldForceSurfaceGlide(double altitudeAboveTerrain) {
        return altitudeAboveTerrain <= RIDER_LOW_ALTITUDE_GLIDE_THRESHOLD || isNearWaterSurface();
    }

    protected boolean isNearWaterSurface() {
        if (level() == null) {
            return false;
        }

        double dragonY = getY();
        if (dragonY > RIDER_WATER_SURFACE_LEVEL + RIDER_WATER_SURFACE_TOLERANCE) {
            return false;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
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

    protected double getHeightmapAltitudeAboveTerrain() {
        return getY() - level().getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(getX()),
                Mth.floor(getZ())
        );
    }

    protected double getAltitudeAboveTerrain() {
        return getAltitudeAboveCollisionTerrain(24, true);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        this.fallDistance = 0.0F;
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof LivingEntity living) {
                living.fallDistance = 0.0F;
            }
        }
        return false;
    }

    protected boolean isRiderLandingBlendActive() {
        return riderLandingBlendTicks > 0;
    }

    protected boolean hasRiderLandingBlendTicks() {
        return riderLandingBlendTicks > 0;
    }

    protected void clearRiderLandingBlendTicks() {
        riderLandingBlendTicks = 0;
    }

    protected void triggerRiderLandingBlendTicks(int durationTicks) {
        riderLandingBlendTicks = Math.max(0, durationTicks);
    }

    protected boolean decrementRiderLandingBlendTicks(BooleanSupplier syncedBlendStillActive) {
        if (riderLandingBlendTicks <= 0) {
            return false;
        }
        riderLandingBlendTicks--;
        return riderLandingBlendTicks == 0 && syncedBlendStillActive.getAsBoolean();
    }

    protected void tickStandardRiderLandingBlend(RiderLandingBlendHooks hooks) {
        if (hooks.skipGenericLandingHandling()) {
            consumeRiderTouchdownFromAir(hooks.waterTouchdownVelocity());
            return;
        }

        trackRiderAirborneForLanding();

        if (isInWaterOrBubble()) {
            clearRiderLandingBlendTicks();
            consumeRiderTouchdownFromAir(hooks.waterTouchdownVelocity());
            if (!level().isClientSide) {
                hooks.clearLandingBlendSync();
                if (isInFlightState() && hooks.shouldClearFlightStateInWater()) {
                    hooks.onWaterFlightCleared();
                }
            }
            return;
        }

        if (!isVehicle() || !isFlying() || onGround()) {
            boolean wasLandingBlend = isFlying() && hasRiderLandingBlendTicks() && hooks.isLandingBlendSynced();
            boolean touchdownFromFlight = consumeRiderTouchdownFromAir(hooks.touchdownVelocity());
            boolean completedLanding = hooks.isCompletedLanding();
            clearRiderLandingBlendTicks();
            if (!level().isClientSide) {
                hooks.clearLandingBlendSync();
                if ((wasLandingBlend || touchdownFromFlight || completedLanding) && onGround() && isVehicle()) {
                    hooks.onRiderLanded();
                }
            }
            return;
        }

        if (hooks.shouldStartLandingBlend()) {
            hooks.startLandingBlend();
            return;
        }

        if (!level().isClientSide && decrementRiderLandingBlendTicks(hooks::isLandingBlendSynced)) {
            hooks.clearLandingBlendSync();
        }
    }

    protected boolean isInFlightState() {
        return isFlying() || isTakeoff() || isLanding() || isHovering();
    }

    protected interface RiderLandingBlendHooks {
        default boolean skipGenericLandingHandling() {
            return false;
        }

        default double waterTouchdownVelocity() {
            return 1.0D;
        }

        default double touchdownVelocity() {
            return 0.15D;
        }

        default boolean shouldClearFlightStateInWater() {
            return true;
        }

        default void onWaterFlightCleared() {
        }

        default boolean isLandingBlendSynced() {
            return false;
        }

        default void clearLandingBlendSync() {
        }

        default boolean isCompletedLanding() {
            return false;
        }

        default boolean shouldStartLandingBlend() {
            return false;
        }

        default void startLandingBlend() {
        }

        void onRiderLanded();
    }

    @Override
    protected abstract boolean isDragonFlying();

    @Override
    public abstract boolean isTakeoff();

    @Override
    public abstract boolean isLanding();

    @Override
    public abstract boolean isHovering();
}
