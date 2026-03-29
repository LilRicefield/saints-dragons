package com.leon.saintsdragons.server.entity.controller.volitans;

import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class VolitansRiderController {
    private static final float RIDER_KEY_PITCH_DEG = 25.0F;
    private static final double CRUISE_SPEED_MULT = 7.55;
    private static final double SPRINT_SPEED_MULT = 8.75;
    private static final double DIVE_START_ANGLE = 25.0;
    private static final double DIVE_MAX_ANGLE = 90.0;
    private static final double DIVE_MIN_SPEED_MULT = 1.0;
    private static final double DIVE_MAX_SPEED_MULT = 2.0;
    private static final double DIVE_CURVE_POWER = 2.0;
    private static final double FLIGHT_ACCEL = 0.12;
    private static final double FLIGHT_DRAG = 0.94;
    private static final double STRAFE_POWER = 0.4;

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
            dragon.syncRiderLookLock(rider);
        } else {
            dragon.syncRiderYawLock(rider);
            dragon.setXRot(0.0F);
        }
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

        double targetDirX = forwardX * forwardInput + rightX * strafeInput * 0.5D;
        double targetDirY = forwardY * forwardInput * 1.2D;
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
            verticalVel = Math.min(swimSpeed * 0.8D, verticalVel + 0.12D * swimSpeed);
        } else if (dragon.isGoingDown()) {
            verticalVel = Math.max(-swimSpeed * 0.8D, verticalVel - 0.12D * swimSpeed);
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
        double diveIntensity = 0.0;
        if (!dragon.isRiderPitchKeyMode() && pitchDeg >= DIVE_START_ANGLE) {
            double normalized = (pitchDeg - DIVE_START_ANGLE) / (DIVE_MAX_ANGLE - DIVE_START_ANGLE);
            normalized = Mth.clamp(normalized, 0.0, 1.0);
            diveIntensity = Math.pow(normalized, DIVE_CURVE_POWER);
        }
        double diveSpeedMult = Mth.lerp((float) diveIntensity, (float) DIVE_MIN_SPEED_MULT, (float) DIVE_MAX_SPEED_MULT);
        targetSpeed *= diveSpeedMult;

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
                Mth.lerp((float) FLIGHT_ACCEL, current.x, targetVel.x),
                Mth.lerp((float) FLIGHT_ACCEL, current.y, targetVel.y),
                Mth.lerp((float) FLIGHT_ACCEL, current.z, targetVel.z)
        ).scale(FLIGHT_DRAG);

        if (dragon.isGoingUp()) {
            blended = blended.add(0.0D, 0.08D, 0.0D);
        } else if (dragon.isGoingDown()) {
            blended = blended.add(0.0D, -0.10D, 0.0D);
        }

        if (blended.length() > targetSpeed) {
            blended = blended.normalize().scale(targetSpeed);
        }

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
