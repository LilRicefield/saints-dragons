package com.leon.saintsdragons.server.entity.base;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.server.flight.DragonBarrelRollHelper;
import com.leon.saintsdragons.server.flight.DragonFlightStateEvaluator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.function.BooleanSupplier;

public abstract class RideableFlyingDragon extends RideableDragonBase {
    protected static final double RIDER_GLIDE_ALTITUDE_THRESHOLD = 40.0D;
    protected static final double RIDER_GLIDE_ALTITUDE_EXIT = 30.0D;
    protected static final double RIDER_LOW_ALTITUDE_GLIDE_THRESHOLD = 6.0D;
    public static final double LANDING_BLEND_ALTITUDE = 8.0D;
    protected static final double RIDER_WATER_SURFACE_LEVEL = 62.0D;
    protected static final double RIDER_WATER_SURFACE_TOLERANCE = 2.0D;
    protected static final int RIDER_WATER_SCAN_RADIUS = 2;
    protected static final int RIDER_WATER_SCAN_DEPTH = 8;
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
    private float accumulatedRoll = 0.0F;
    private int riderLandingBlendTicks = 0;
    private float prevSmoothedRoll = 0.0F;
    private float smoothedRoll = 0.0F;

    protected RideableFlyingDragon(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
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
