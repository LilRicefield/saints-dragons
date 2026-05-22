package com.leon.saintsdragons.server.flight;

import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class DragonRiderFlightController {
    private static final float DIVE_START_ANGLE_DEG = 30.0F;
    private static final float DIVE_MAX_ANGLE_DEG = 70.0F;
    private static final float DIVE_CURVE_POWER = 1.0F;
    private static final double DIVE_PITCH_GAIN_SCALE = 0.16D;
    public static final int DIVE_EXIT_BOOST_HOLD_TICKS = 60;
    private static final double OVERDRIVE_BLEED_SCALE = 0.035D;
    private static final double GLIDE_BLEED_SCALE = 0.025D;

    private DragonRiderFlightController() {
    }

    public static void tick(RideableFlyingDragon dragon, Player rider, Vec3 input, float pitchRadians,
                            boolean keyPitchMode, DragonRiderFlightSettings settings,
                            boolean takeoffBoostActive) {
        tick(
                dragon,
                rider,
                input,
                pitchRadians,
                keyPitchMode,
                settings,
                takeoffBoostActive,
                true,
                0.20D
        );
    }

    public static void tick(RideableFlyingDragon dragon, Player rider, Vec3 input, float pitchRadians,
                            boolean keyPitchMode, DragonRiderFlightSettings settings,
                            boolean takeoffBoostActive,
                            boolean takeoffBoostRequiresGoingUp,
                            double takeoffMinVertical) {
        Vec3 velocity = computeVelocity(
                dragon,
                input,
                pitchRadians,
                keyPitchMode,
                settings,
                takeoffBoostActive,
                takeoffBoostRequiresGoingUp,
                takeoffMinVertical
        );
        dragon.move(MoverType.SELF, velocity);
        dragon.setDeltaMovement(velocity);
        dragon.calculateEntityAnimation(true);
        rider.fallDistance = 0.0F;
        dragon.fallDistance = 0.0F;
    }

    private static Vec3 computeVelocity(RideableFlyingDragon dragon, Vec3 input, float pitchRadians,
                                        boolean keyPitchMode, DragonRiderFlightSettings settings,
                                        boolean takeoffBoostActive,
                                        boolean takeoffBoostRequiresGoingUp,
                                        double takeoffMinVertical) {
        double forwardInput = input.z;
        double strafeInput = input.x;
        boolean hasInput = Math.abs(forwardInput) > 0.01D || Math.abs(strafeInput) > 0.01D;
        double diveIntensity = diveIntensity(pitchRadians);
        boolean diving = forwardInput > 0.01D && diveIntensity > 0.0D;

        double currentFlightSpeed = tickThrottle(dragon, hasInput, forwardInput, diveIntensity, settings);
        dragon.setRiderFlightThrottle(currentFlightSpeed);

        float yawRadians = (float) Math.toRadians(dragon.getYRot());
        double forwardXZ = Math.cos(pitchRadians);
        double forwardX = -Math.sin(yawRadians) * forwardXZ;
        double forwardY = keyPitchMode ? 0.0D : -Math.sin(pitchRadians);
        double forwardZ = Math.cos(yawRadians) * forwardXZ;
        double rightX = Math.cos(yawRadians);
        double rightZ = Math.sin(yawRadians);

        double targetDirX = forwardX * forwardInput + rightX * strafeInput * settings.strafePower();
        double targetDirY = forwardY * forwardInput * 1.35D;
        double targetDirZ = forwardZ * forwardInput + rightZ * strafeInput * settings.strafePower();
        double dirLength = Math.sqrt(targetDirX * targetDirX + targetDirY * targetDirY + targetDirZ * targetDirZ);

        Vec3 current = dragon.getDeltaMovement();
        Vec3 velocity;
        if (hasInput && dirLength > 0.01D) {
            targetDirX /= dirLength;
            targetDirY /= dirLength;
            targetDirZ /= dirLength;
            Vec3 targetVelocity = new Vec3(
                    targetDirX * currentFlightSpeed,
                    targetDirY * currentFlightSpeed,
                    targetDirZ * currentFlightSpeed
            );
            double acceleration = diving ? settings.diveAcceleration() : settings.flightAcceleration();
            velocity = new Vec3(
                    Mth.lerp(acceleration, current.x, targetVelocity.x),
                    Mth.lerp(acceleration, current.y, targetVelocity.y),
                    Mth.lerp(acceleration, current.z, targetVelocity.z)
            );
        } else {
            velocity = current.scale(1.0D - settings.noInputDrag());
            if (velocity.lengthSqr() < 0.0001D) {
                velocity = Vec3.ZERO;
            }
        }

        double vertical = velocity.y;
        if (!diving) {
            if (takeoffBoostActive && (!takeoffBoostRequiresGoingUp || dragon.isGoingUp())) {
                vertical = Math.max(vertical + settings.takeoffBoost(), takeoffMinVertical);
            } else if (dragon.isGoingUp()) {
                vertical += settings.ascendThrust();
            } else if (dragon.isGoingDown()) {
                vertical -= settings.descendThrust();
            }
        }

        double downwardLimit = diving ? Math.max(settings.verticalSpeedLimit(), currentFlightSpeed) : settings.verticalSpeedLimit();
        vertical = Mth.clamp(vertical, -downwardLimit, settings.verticalSpeedLimit());
        return new Vec3(velocity.x, vertical, velocity.z);
    }

    private static double tickThrottle(RideableFlyingDragon dragon, boolean hasInput, double forwardInput,
                                       double diveIntensity, DragonRiderFlightSettings settings) {
        double baseTarget = dragon.isAccelerating() ? settings.sprintSpeed() : settings.baseSpeed();
        double maxOverdrive = Math.max(baseTarget, settings.sprintSpeed()) * settings.diveSpeedMultiplier();
        double throttle = Mth.clamp(dragon.getRiderFlightThrottle(), 0.0D, maxOverdrive);
        boolean forwardActive = forwardInput > 0.01D;
        boolean diving = forwardActive && diveIntensity > 0.0D;

        if (hasInput) {
            if (throttle <= 0.0D) {
                throttle = baseTarget;
            } else if (throttle < baseTarget) {
                throttle = Mth.lerp(Mth.clamp(settings.flightAcceleration(), 0.0D, 1.0D), throttle, baseTarget);
            }
        } else {
            return Math.max(0.0D, throttle - settings.noInputDrag());
        }

        if (diving) {
            dragon.setRiderDiveBoostHoldTicks(DIVE_EXIT_BOOST_HOLD_TICKS);
            double situationalCap = Mth.lerp(diveIntensity, baseTarget, maxOverdrive);
            double pitchGain = baseTarget * settings.diveAcceleration() * DIVE_PITCH_GAIN_SCALE * diveIntensity;
            if (throttle < situationalCap) {
                throttle = Math.min(situationalCap, throttle + pitchGain);
            }
        } else if (throttle > baseTarget) {
            if (forwardActive && dragon.getRiderDiveBoostHoldTicks() > 0) {
                return Mth.clamp(throttle, 0.0D, maxOverdrive);
            }
            double bleed = baseTarget * OVERDRIVE_BLEED_SCALE;
            throttle = Math.max(baseTarget, throttle - bleed);
        } else if (!dragon.isAccelerating() && throttle > 0.0D) {
            double bleed = baseTarget * GLIDE_BLEED_SCALE;
            throttle = Math.max(0.0D, throttle - bleed);
        }

        return Mth.clamp(throttle, 0.0D, maxOverdrive);
    }

    public static double diveIntensity(float pitchRadians) {
        double pitchDegrees = Math.toDegrees(pitchRadians);
        double normalizedPitch = (pitchDegrees - DIVE_START_ANGLE_DEG) / (DIVE_MAX_ANGLE_DEG - DIVE_START_ANGLE_DEG);
        return Math.pow(Mth.clamp(normalizedPitch, 0.0D, 1.0D), DIVE_CURVE_POWER);
    }
}
