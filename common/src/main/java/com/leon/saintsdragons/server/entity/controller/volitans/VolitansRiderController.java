package com.leon.saintsdragons.server.entity.controller.volitans;

import com.leon.saintsdragons.server.flight.DragonRiderFlightPhysics;
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
    private static final double CRUISE_SPEED_MULT = 4.0;
    private static final double SPRINT_SPEED_MULT = 5.0;
    private static final double STRAFE_POWER = 0.4;
    private static final double ASCEND_THRUST = 0.45D;
    private static final double DESCEND_THRUST = 0.85D;
    private static final double TERMINAL_VELOCITY = 1.5D;
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
        if (dragon.getControllingPassenger() instanceof Player player) {
            return player;
        }
        return null;
    }

    public Vec3 getRiddenInput(Player rider, Vec3 deltaIn) {
        float forward = rider.zza;
        float strafe = rider.xxa;
        if (forward < 0.0F) {
            forward *= 0.5F;
        }

        if (dragon.isFlying()) {
            return new Vec3(strafe * 0.45F, 0.0D, forward * 0.9F);
        }
        if (dragon.isInWaterOrBubble()) {
            return new Vec3(strafe * 0.6F, 0.0D, forward);
        }
        return new Vec3(strafe * 0.4F, 0.0D, forward * 0.8F);
    }

    public void tickRidden(Player rider) {
        rider.fallDistance = 0.0F;
        dragon.fallDistance = 0.0F;
        dragon.setTarget(null);

        if ((dragon.isFlying() || dragon.isInWaterOrBubble()) && !dragon.isRiderPitchKeyMode()) {
            syncRiderLook(rider);
        } else {
            syncRiderYaw(rider);
            dragon.setXRot(0.0F);
        }
    }

    private void syncRiderLook(Player rider) {
        syncRiderYaw(rider);
        float targetPitch = Mth.clamp(rider.getXRot(), -45.0F, 45.0F);
        float blendedPitch = Mth.lerp(0.18F, dragon.getXRot(), targetPitch);
        dragon.xRotO = dragon.getXRot();
        dragon.setXRot(blendedPitch);
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
        float pitchRad = resolveRiderPitchRad(rider);
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
        double baseSpeed = dragon.getFlightSpeed();
        double targetSpeed = (dragon.isAccelerating() ? SPRINT_SPEED_MULT : CRUISE_SPEED_MULT) * baseSpeed;

        float pitchDeg = resolveRiderPitchDeg(rider);
        DragonRiderFlightPhysics.DiveResponse diveResponse =
                DragonRiderFlightPhysics.computeDiveResponse(pitchDeg, dragon.isRiderPitchKeyMode());
        double diveAcceleration = diveResponse.acceleration();
        double diveDrag = diveResponse.drag();
        targetSpeed *= diveResponse.speedMultiplier();

        double forwardInput = motion.z;
        double strafeInput = motion.x;
        float yawRad = (float) Math.toRadians(dragon.getYRot());
        float pitchRad = resolveRiderPitchRad(rider);

        double forwardXZ = Math.cos(pitchRad);
        double forwardX = -Math.sin(yawRad) * forwardXZ;
        double forwardY = -Math.sin(pitchRad);
        double forwardZ = Math.cos(yawRad) * forwardXZ;
        double strafeX = Math.cos(yawRad);
        double strafeZ = Math.sin(yawRad);

        Vec3 targetDir = new Vec3(
                forwardX * forwardInput + strafeX * strafeInput * STRAFE_POWER,
                forwardY * forwardInput,
                forwardZ * forwardInput + strafeZ * strafeInput * STRAFE_POWER
        );
        if (targetDir.lengthSqr() > 1.0E-6D) {
            targetDir = targetDir.normalize();
        }

        Vec3 current = dragon.getDeltaMovement();
        Vec3 targetVel = targetDir.scale(targetSpeed);
        Vec3 blended = new Vec3(
                Mth.lerp((float) diveAcceleration, current.x, targetVel.x),
                Mth.lerp((float) diveAcceleration, current.y, targetVel.y),
                Mth.lerp((float) diveAcceleration, current.z, targetVel.z)
        ).scale(1.0D - diveDrag);

        double verticalVel = blended.y;
        boolean hasInput = Math.abs(motion.z) > 0.01D || Math.abs(motion.x) > 0.01D;
        boolean isDiving = !dragon.isRiderPitchKeyMode() && pitchDeg >= 45.0F && hasInput;

        if (!isDiving) {
            if (dragon.isGoingUp()) {
                verticalVel += ASCEND_THRUST;
            } else if (dragon.isGoingDown()) {
                verticalVel -= DESCEND_THRUST;
            }
        }
        verticalVel = Mth.clamp(verticalVel, -TERMINAL_VELOCITY, TERMINAL_VELOCITY);

        if (blended.length() > targetSpeed) {
            blended = blended.normalize().scale(targetSpeed);
        }

        blended = new Vec3(blended.x, verticalVel, blended.z);
        dragon.setSpeed((float) targetSpeed);
        dragon.move(MoverType.SELF, blended);
        dragon.setDeltaMovement(blended);
        dragon.calculateEntityAnimation(true);
    }

    private float resolveRiderPitchDeg(Player rider) {
        if (!dragon.isRiderPitchKeyMode()) {
            return rider.getXRot();
        }
        if (dragon.isGoingUp()) {
            return -RIDER_KEY_PITCH_DEG;
        }
        if (dragon.isGoingDown()) {
            return RIDER_KEY_PITCH_DEG;
        }
        return 0.0F;
    }

    private float resolveRiderPitchRad(Player rider) {
        return (float) Math.toRadians(resolveRiderPitchDeg(rider));
    }
}
