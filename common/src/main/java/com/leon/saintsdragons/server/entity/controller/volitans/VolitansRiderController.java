package com.leon.saintsdragons.server.entity.controller.volitans;

import com.leon.saintsdragons.server.entity.controller.DragonRiderControllerHelper;
import com.leon.saintsdragons.server.flight.DragonRiderFlightController;
import com.leon.saintsdragons.server.flight.DragonRiderFlightSettings;
import com.leon.saintsdragons.server.flight.DragonRiderSeat;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VolitansRiderController {
    private static final float RIDER_KEY_PITCH_DEG = 25.0F;
    private static final double SEAT_BASE_FACTOR = 0.45D;
    private static final double BASE_FLIGHT_SPEED_MULT = 4.0;
    private static final double SPRINT_FLIGHT_SPEED_MULT = 5.0;
    private static final double STRAFE_POWER = 0.4;
    private static final double ASCEND_THRUST = 0.45D;
    private static final double DESCEND_THRUST = 0.85D;
    private static final double TERMINAL_VELOCITY = 1.5D;
    private static final double FLIGHT_ACCELERATION = 0.35D;
    private static final double DIVE_SPEED_MULTIPLIER = 2.75D;
    private static final double DIVE_ACCELERATION = 0.30D;
    private static final double SWIM_ASCEND_THRUST = 0.18D;
    private static final double SWIM_DESCEND_THRUST = 0.20D;
    private static final double SWIM_VERTICAL_LIMIT = 0.55D;
    private static final double SWIM_PITCH_VERTICAL_SCALE = 0.65D;

    private final Volitans dragon;

    public VolitansRiderController(Volitans dragon) {
        this.dragon = dragon;
    }

    @Nullable
    public Player getRidingPlayer() {
        return DragonRiderControllerHelper.getRidingPlayer(dragon);
    }

    public Vec3 getRiddenInput(Player rider, Vec3 deltaIn) {
        if (dragon.isFlying()) {
            return DragonRiderControllerHelper.riddenInput(rider, true, 0.4D, 0.8D, 0.45D, 0.9D);
        }
        if (dragon.isInWaterOrBubble()) {
            return DragonRiderControllerHelper.riddenInput(rider, true, 0.4D, 0.8D, 0.6D, 1.0D);
        }
        return DragonRiderControllerHelper.riddenInput(rider, false, 0.4D, 0.8D, 0.45D, 0.9D);
    }

    public void tickRidden(Player rider) {
        DragonRiderControllerHelper.clearRiderFallAndTarget(dragon, rider);

        if ((dragon.isFlying() || dragon.isInWaterOrBubble()) && !dragon.isRiderPitchKeyMode()) {
            syncRiderLook(rider);
        } else {
            syncRiderYaw(rider);
            dragon.setXRot(0.0F);
        }
    }

    private void syncRiderLook(Player rider) {
        syncRiderYaw(rider);
        DragonRiderControllerHelper.syncPitchToRider(dragon, rider, 0.18F, 45.0F);
    }

    private void syncRiderYaw(Player rider) {
        float currentYaw = dragon.getYRot();
        float yawDelta = Mth.wrapDegrees(rider.getYRot() - currentYaw);
        float blendedYaw = currentYaw + yawDelta * 0.18F;
        dragon.setYRot(blendedYaw);
        dragon.yBodyRotO = dragon.yBodyRot;
        dragon.yBodyRot = blendedYaw;
        dragon.yHeadRotO = dragon.yHeadRot;
        dragon.setYHeadRot(blendedYaw);
    }

    public double getPassengersRidingOffset() {
        return dragon.getBbHeight() * SEAT_BASE_FACTOR;
    }

    public void positionRider(@NotNull Entity passenger, Entity.@NotNull MoveFunction moveFunction) {
        DragonRiderSeat.positionLocatorRider(
                dragon,
                passenger,
                moveFunction,
                getPassengersRidingOffset(),
                dragon.getClientLocatorPosition("passengerLocator")
        );
    }

    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        return DragonRiderSeat.findRadialGroundDismount(
                passenger,
                dragon,
                new double[] { 2.5D, 3.5D, 1.8D },
                new int[] { 0, 30, -30, 60, -60, 90, -90, 150, -150, 180 },
                6,
                2.0D
        );
    }

    public void handleGroundTravel(Player rider, Vec3 motion) {
        float speed = (float) (dragon.isAccelerating() ? 0.34D : 0.24D);
        dragon.setRunning(dragon.isAccelerating() && rider.zza > 0.05F);
        dragon.setSpeed(speed);
        dragon.moveRelative(speed, motion);
        dragon.move(MoverType.SELF, dragon.getDeltaMovement());
        dragon.setDeltaMovement(dragon.getDeltaMovement().multiply(0.82D, 1.0D, 0.82D));
        dragon.calculateEntityAnimation(true);
    }

    public void handleSwimTravel(Player rider, Vec3 motion) {
        dragon.setRunning(false);
        dragon.setAirSupply(dragon.getMaxAirSupply());
        Vec3 velocity = dragon.getDeltaMovement();

        double swimSpeed = dragon.getSwimSpeed();
        if (dragon.isAccelerating()) {
            swimSpeed *= 1.6D;
        }

        double forwardInput = motion.z;
        double strafeInput = motion.x;
        boolean hasInput = Math.abs(forwardInput) > 0.01D || Math.abs(strafeInput) > 0.01D;

        float yawRad = (float) Math.toRadians(dragon.getYRot());
        float pitchRad = DragonRiderControllerHelper.resolveRiderPitchRadians(dragon, rider, RIDER_KEY_PITCH_DEG);
        double forwardXZ = Math.cos(pitchRad);
        double forwardX = -Math.sin(yawRad) * forwardXZ;
        double forwardY = -Math.sin(pitchRad);
        double forwardZ = Math.cos(yawRad) * forwardXZ;
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);
        boolean verticalInputActive = dragon.isGoingUp() || dragon.isGoingDown();

        double targetDirX = forwardX * forwardInput + rightX * strafeInput * 0.5D;
        double targetDirY = verticalInputActive ? 0.0D : forwardY * forwardInput * SWIM_PITCH_VERTICAL_SCALE;
        double targetDirZ = forwardZ * forwardInput + rightZ * strafeInput * 0.5D;
        double dirLength = Math.sqrt(targetDirX * targetDirX + targetDirY * targetDirY + targetDirZ * targetDirZ);

        Vec3 desired;
        if (hasInput && dirLength > 0.01D) {
            targetDirX /= dirLength;
            targetDirY /= dirLength;
            targetDirZ /= dirLength;
            desired = new Vec3(targetDirX * swimSpeed, targetDirY * swimSpeed, targetDirZ * swimSpeed);
        } else {
            desired = new Vec3(0.0D, velocity.y * 0.9D, 0.0D);
        }

        Vec3 blended = velocity.add(desired.subtract(velocity).scale(0.28D));
        blended = blended.multiply(0.92D, 0.96D, 0.92D);

        double verticalVel = blended.y;
        if (dragon.isGoingUp()) {
            verticalVel = Math.min(SWIM_VERTICAL_LIMIT, verticalVel + SWIM_ASCEND_THRUST);
        } else if (dragon.isGoingDown()) {
            verticalVel = Math.max(-SWIM_VERTICAL_LIMIT, verticalVel - SWIM_DESCEND_THRUST);
        }

        blended = new Vec3(blended.x, verticalVel, blended.z);
        dragon.setDeltaMovement(blended);
        dragon.move(MoverType.SELF, blended);
        dragon.calculateEntityAnimation(true);
    }

    public void handleFlightTravel(Player rider, Vec3 motion) {
        dragon.setRunning(false);
        float pitchRad = DragonRiderControllerHelper.resolveRiderPitchRadians(dragon, rider, RIDER_KEY_PITCH_DEG);
        DragonRiderFlightController.tick(
                dragon,
                rider,
                motion,
                pitchRad,
                dragon.isRiderPitchKeyMode(),
                flightSettings(),
                false
        );
    }

    private DragonRiderFlightSettings flightSettings() {
        double baseSpeed = dragon.getFlightSpeed();
        return new DragonRiderFlightSettings(
                baseSpeed * BASE_FLIGHT_SPEED_MULT,
                baseSpeed * SPRINT_FLIGHT_SPEED_MULT,
                FLIGHT_ACCELERATION,
                DIVE_SPEED_MULTIPLIER,
                DIVE_ACCELERATION,
                STRAFE_POWER,
                0.5D,
                ASCEND_THRUST,
                DESCEND_THRUST,
                TERMINAL_VELOCITY,
                ASCEND_THRUST * 0.65D
        );
    }
}
