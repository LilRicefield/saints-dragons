package com.leon.saintsdragons.server.ai.navigation.async;

import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

class AsyncFlightMovementExecutor {
    private static final double VERTICAL_TARGET_DEADZONE = 0.10D;
    private static final double VERTICAL_SPEED_DEADZONE = 0.04D;
    private static final double HORIZONTAL_VELOCITY_LERP = 0.18D;
    private static final double VERTICAL_VELOCITY_LERP = 0.10D;
    private static final double MAX_ACCELERATION = FlightMotionPolicy.MAX_ACCELERATION;
    private static final float MAX_YAW_STEP = 12.0f;
    private static final float MAX_PITCH_STEP = 2.5f;
    private static final float DIVE_MAX_PITCH_STEP = 7.0f;
    private static final float PITCH_DEADZONE_DEGREES = 3.5f;

    private final Mob dragon;
    private final DragonFlightCapable flightCapable;
    private final DragonFlightSteering steering;
    private Vec3 smoothedVelocity = Vec3.ZERO;

    AsyncFlightMovementExecutor(Mob dragon, DragonFlightCapable flightCapable) {
        this(dragon, flightCapable, new DragonFlightSteering(dragon));
    }

    AsyncFlightMovementExecutor(Mob dragon, DragonFlightCapable flightCapable, DragonFlightSteering steering) {
        this.dragon = dragon;
        this.flightCapable = flightCapable;
        this.steering = steering;
    }

    public void executeMovement(Vec3 lookAheadTarget,
                                Vec3 currentWaypoint,
                                double speedModifier,
                                double arrivalDist,
                                boolean queueEmpty,
                                DragonFlightRequest.Arrival arrival,
                                boolean diveCommit,
                                boolean touchdown) {
        Vec3 dragonPos = this.dragon.position();
        Vec3 currentVelocity = this.dragon.getDeltaMovement();
        boolean takingOff = this.flightCapable.isTakeoff();
        boolean diving = diveCommit && !takingOff && !touchdown;
        if (touchdown) {
            this.executeTouchdown(currentWaypoint, speedModifier);
            return;
        }

        double desiredSpeed = FlightMotionPolicy.requestedSpeed(this.flightCapable.getFlightSpeed(), speedModifier);
        if (takingOff) desiredSpeed = Math.min(desiredSpeed, FlightMotionPolicy.TAKEOFF_MAX_SPEED);
        Vec3 target = this.steering.guide(
                lookAheadTarget != null ? lookAheadTarget : currentWaypoint,
                currentWaypoint, this.flightCapable.isTakeoff(), diving, desiredSpeed);
        Vec3 toTarget = target.subtract(dragonPos);
        double distToTarget = toTarget.length();
        if (distToTarget < 0.1) {
            return;
        }

        double distToFinalWaypoint = dragonPos.distanceTo(currentWaypoint);
        if (queueEmpty) {
            desiredSpeed = FlightMotionPolicy.arrivalSpeed(desiredSpeed, distToFinalWaypoint, arrivalDist, arrival);
        }

        Vec3 desiredDirection = toTarget.normalize();
        if (diving) {
            desiredSpeed = FlightMotionPolicy.diveSpeed(desiredSpeed, desiredDirection.y);
        }
        double verticalSpeed = desiredSpeed;
        if (currentVelocity.horizontalDistanceSqr() > 0.01D
                && desiredDirection.horizontalDistanceSqr() > 0.01D) {
            double alignment = currentVelocity.multiply(1, 0, 1).normalize()
                    .dot(desiredDirection.multiply(1, 0, 1).normalize());
            desiredSpeed = FlightMotionPolicy.turnSpeed(desiredSpeed, alignment, toTarget.horizontalDistance());
        }
        Vec3 steeringDirection = !this.flightCapable.isTakeoff()
                ? this.steering.turnToward(desiredDirection, currentVelocity.length()) : desiredDirection;
        double desiredVertical = Math.abs(toTarget.y) < VERTICAL_TARGET_DEADZONE ? 0.0D : desiredDirection.y;
        Vec3 targetVelocity = new Vec3(
                steeringDirection.x * desiredSpeed,
                desiredVertical * verticalSpeed,
                steeringDirection.z * desiredSpeed
        );
        if (takingOff) {
            targetVelocity = new Vec3(targetVelocity.x,
                    FlightMotionPolicy.takeoffVerticalSpeed(targetVelocity.y, toTarget.y), targetVelocity.z);
        }
        Vec3 velocityBaseline = currentVelocity;
        this.smoothedVelocity = lerpVelocity(velocityBaseline, targetVelocity);
        if (diving) {
            this.smoothedVelocity = new Vec3(
                    this.smoothedVelocity.x,
                    Mth.lerp(FlightMotionPolicy.DIVE_VERTICAL_LERP, velocityBaseline.y, targetVelocity.y),
                    this.smoothedVelocity.z
            );
        }
        if (!this.flightCapable.isTakeoff()) {
            double horizontalSpeed = Mth.lerp(HORIZONTAL_VELOCITY_LERP,
                    currentVelocity.horizontalDistance(), targetVelocity.horizontalDistance());
            Vec3 horizontalDirection = steeringDirection.multiply(1.0D, 0.0D, 1.0D).normalize();
            this.smoothedVelocity = horizontalDirection.scale(horizontalSpeed)
                    .add(0.0D, this.smoothedVelocity.y, 0.0D);
            double acceleration = arrival == DragonFlightRequest.Arrival.PASS_THROUGH
                    ? FlightMotionPolicy.sprintAcceleration(desiredSpeed) : MAX_ACCELERATION;
            this.smoothedVelocity = diving
                    ? FlightMotionPolicy.limitDiveAcceleration(velocityBaseline, this.smoothedVelocity, acceleration)
                    : FlightMotionPolicy.limitAcceleration(velocityBaseline, this.smoothedVelocity, acceleration);
        } else if (takingOff) {
            this.smoothedVelocity = FlightMotionPolicy.limitAcceleration(velocityBaseline, this.smoothedVelocity);
        }
        if (takingOff && currentVelocity.y > 0.0D) {
            this.smoothedVelocity = new Vec3(
                    this.smoothedVelocity.x,
                    Math.max(this.smoothedVelocity.y, Math.min(currentVelocity.y, FlightMotionPolicy.TAKEOFF_MIN_LIFT)),
                    this.smoothedVelocity.z
            );
        }
        boolean shouldPreserveVerticalMotion = this.flightCapable.isTakeoff()
                || Math.abs(desiredVertical) > 0.1D;
        if (!shouldPreserveVerticalMotion && Math.abs(this.smoothedVelocity.y) < VERTICAL_SPEED_DEADZONE) {
            this.smoothedVelocity = new Vec3(this.smoothedVelocity.x, 0.0D, this.smoothedVelocity.z);
        }
        if (!takingOff || this.flightCapable.isFlying() && !this.dragon.onGround()) {
            this.smoothedVelocity = this.steering.checkedVelocity(this.smoothedVelocity);
        }
        this.dragon.setDeltaMovement(this.smoothedVelocity);
        this.dragon.hasImpulse = true;
        this.updateRotation(false,
                diving ? DIVE_MAX_PITCH_STEP : MAX_PITCH_STEP);
    }

    private void executeTouchdown(Vec3 target, double speedModifier) {
        Vec3 position = this.dragon.position();
        double speed = FlightMotionPolicy.requestedSpeed(this.flightCapable.getFlightSpeed(), speedModifier);
        Vec3 velocity = FlightLandingMotion.velocity(this.dragon.getDeltaMovement(), target.subtract(position), speed);
        // Validate lateral motion and the descent down to the support surface. Allow only the
        // final downward contact to cross it, so Minecraft can set onGround instead of hovering above it.
        double altitude = Math.max(0.0D, position.y - target.y);
        Vec3 checked = new Vec3(velocity.x, Math.max(-altitude, velocity.y), velocity.z);
        if (!VoxelAabbSweeper.isClear(this.dragon.level(), this.dragon,
                this.dragon.getBoundingBox().deflate(1.0E-4D), checked)) {
            velocity = Vec3.ZERO;
        }
        this.smoothedVelocity = velocity;
        this.dragon.setDeltaMovement(velocity);
        this.dragon.hasImpulse = true;
        this.updateRotation(true, MAX_PITCH_STEP);
    }

    private void updateRotation(boolean holdLandingHeading, float maxPitchStep) {
        Vec3 velocity = this.smoothedVelocity;
        if (velocity.lengthSqr() < 1.0E-4) {
            return;
        }

        if (!holdLandingHeading && velocity.horizontalDistanceSqr() > 1.0E-4D) {
            float targetYaw = -(float) Math.toDegrees(Mth.atan2(velocity.x, velocity.z));
            if (this.dragon instanceof RideableFlyingDragon flying) {
                targetYaw = flying.getCombatAim().flightYaw(targetYaw);
            }
            float yawDiff = Mth.wrapDegrees(targetYaw - this.dragon.getYRot());
            float newYaw = this.dragon.getYRot() + Mth.clamp(yawDiff, -MAX_YAW_STEP, MAX_YAW_STEP);
            this.dragon.setYRot(newYaw);
            this.dragon.yBodyRot = newYaw;
            if (!(this.dragon instanceof RideableFlyingDragon flying)
                    || !flying.getCombatAim().isActive()) {
                this.dragon.setYHeadRot(newYaw);
            }
        }

        if (this.dragon instanceof RideableFlyingDragon flying && flying.getCombatAim().isActive()) {
            flying.getCombatAim().applyFacing();
            return;
        }
        float targetPitch = (float) (-Math.toDegrees(Mth.atan2(velocity.y, Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z))));
        float pitchDiff = Mth.wrapDegrees(targetPitch - this.dragon.getXRot());
        if (Math.abs(pitchDiff) < PITCH_DEADZONE_DEGREES) {
            pitchDiff = 0.0f;
        }
        this.dragon.setXRot(this.dragon.getXRot() + Mth.clamp(pitchDiff, -maxPitchStep, maxPitchStep));
    }

    public void applyIdleFriction() {
        this.steering.reset();
        this.smoothedVelocity = this.dragon.getDeltaMovement();
        if (this.smoothedVelocity.lengthSqr() > 1.0E-4) {
            this.smoothedVelocity = this.smoothedVelocity.scale(0.85);
            this.dragon.setDeltaMovement(this.smoothedVelocity);
        } else {
            this.smoothedVelocity = Vec3.ZERO;
            this.dragon.setDeltaMovement(Vec3.ZERO);
        }
    }

    public void zeroVelocity() {
        this.steering.reset();
        this.smoothedVelocity = Vec3.ZERO;
        this.dragon.setDeltaMovement(Vec3.ZERO);
    }

    void resetSteering() {
        this.steering.reset();
    }

    String steeringSummary() {
        return this.steering.debugSummary();
    }

    private static Vec3 lerpVelocity(Vec3 from, Vec3 to) {
        return new Vec3(
                Mth.lerp(HORIZONTAL_VELOCITY_LERP, from.x, to.x),
                Mth.lerp(VERTICAL_VELOCITY_LERP, from.y, to.y),
                Mth.lerp(HORIZONTAL_VELOCITY_LERP, from.z, to.z));
    }
}
