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
    private static final double LANDING_HORIZONTAL_VELOCITY_LERP = 0.32D;
    private static final double GLIDE_VERTICAL_VELOCITY_LERP = 0.30D;
    private static final double FLARE_VERTICAL_VELOCITY_LERP = 0.48D;
    private static final double TOUCHDOWN_VERTICAL_VELOCITY_LERP = 0.62D;
    private static final double GLIDE_SPEED_SCALE = 0.95D;
    private static final double FLARE_SPEED_SCALE = 0.75D;
    private static final double TOUCHDOWN_SPEED_SCALE = 0.50D;
    private static final double GLIDE_MAX_DESCENT_SPEED = 0.68D;
    private static final double FLARE_MAX_DESCENT_SPEED = 0.32D;
    private static final double GLIDE_MAX_ACTUAL_DESCENT_SPEED = 0.72D;
    private static final double FLARE_MAX_ACTUAL_DESCENT_SPEED = 0.38D;
    private static final double TOUCHDOWN_MAX_ACTUAL_DESCENT_SPEED = 0.28D;
    private static final double TOUCHDOWN_MIN_DESCENT_SPEED = 0.11D;
    private static final double TOUCHDOWN_MAX_DESCENT_SPEED = 0.28D;
    private static final double MAX_ACCELERATION = FlightMotionPolicy.MAX_ACCELERATION;
    private static final double TOUCHDOWN_HORIZONTAL_DEADZONE = 0.2D;
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
                                AsyncFlightController.LandingPhase landingPhase) {
        Vec3 dragonPos = this.dragon.position();
        Vec3 currentVelocity = this.dragon.getDeltaMovement();
        boolean takingOff = this.flightCapable.isTakeoff();
        boolean landingTarget = landingPhase.isCommitted();
        boolean diving = diveCommit && !this.flightCapable.isTakeoff()
                && landingPhase == AsyncFlightController.LandingPhase.NONE;
        if (landingTarget
                && (this.dragon.onGround()
                    || (landingPhase == AsyncFlightController.LandingPhase.TOUCHDOWN
                        && this.hasLandingContact()))) {
            this.zeroVelocity();
            return;
        }

        double desiredSpeed = FlightMotionPolicy.requestedSpeed(this.flightCapable.getFlightSpeed(), speedModifier);
        if (takingOff) desiredSpeed = Math.min(desiredSpeed, FlightMotionPolicy.TAKEOFF_MAX_SPEED);
        if (landingPhase == AsyncFlightController.LandingPhase.APPROACH) {
            desiredSpeed = Math.max(desiredSpeed, this.flightCapable.getFlightSpeed() * 3.0D);
        } else if (landingPhase == AsyncFlightController.LandingPhase.GLIDE) {
            desiredSpeed = Math.max(desiredSpeed, this.flightCapable.getFlightSpeed() * 2.0D);
        }
        Vec3 target = this.steering.guide(
                lookAheadTarget != null ? lookAheadTarget : currentWaypoint,
                currentWaypoint, landingTarget || this.flightCapable.isTakeoff(), diving, desiredSpeed);
        Vec3 toTarget = target.subtract(dragonPos);
        double distToTarget = toTarget.length();
        if (distToTarget < 0.1 && landingPhase != AsyncFlightController.LandingPhase.TOUCHDOWN) {
            return;
        }

        double distToFinalWaypoint = dragonPos.distanceTo(currentWaypoint);
        double decelStartDist = Math.max(arrivalDist * 2.0D,
                currentVelocity.lengthSqr() / (2.0D * MAX_ACCELERATION) + arrivalDist);
        desiredSpeed *= switch (landingPhase) {
            case GLIDE -> GLIDE_SPEED_SCALE;
            case FLARE -> FLARE_SPEED_SCALE;
            case TOUCHDOWN -> TOUCHDOWN_SPEED_SCALE;
            default -> 1.0D;
        };
        boolean ordinaryFinalWaypoint = landingPhase == AsyncFlightController.LandingPhase.NONE
                || landingPhase == AsyncFlightController.LandingPhase.GO_AROUND
                || landingPhase == AsyncFlightController.LandingPhase.APPROACH;
        if (landingPhase == AsyncFlightController.LandingPhase.NONE && queueEmpty) {
            desiredSpeed = FlightMotionPolicy.arrivalSpeed(desiredSpeed, distToFinalWaypoint, arrivalDist, arrival);
        } else if (ordinaryFinalWaypoint && distToFinalWaypoint < decelStartDist && queueEmpty) {
            desiredSpeed *= Math.max(0.3, distToFinalWaypoint / decelStartDist);
        }

        Vec3 desiredDirection = toTarget.normalize();
        if (diving) {
            desiredSpeed = FlightMotionPolicy.diveSpeed(desiredSpeed, desiredDirection.y);
        }
        double verticalSpeed = desiredSpeed;
        if (!landingTarget && currentVelocity.horizontalDistanceSqr() > 0.01D
                && desiredDirection.horizontalDistanceSqr() > 0.01D) {
            double alignment = currentVelocity.multiply(1, 0, 1).normalize()
                    .dot(desiredDirection.multiply(1, 0, 1).normalize());
            desiredSpeed = FlightMotionPolicy.turnSpeed(desiredSpeed, alignment, toTarget.horizontalDistance());
        }
        Vec3 steeringDirection = !landingTarget && !this.flightCapable.isTakeoff()
                ? this.steering.turnToward(desiredDirection, currentVelocity.length()) : desiredDirection;
        double desiredVertical = Math.abs(toTarget.y) < VERTICAL_TARGET_DEADZONE && !landingTarget ? 0.0D : desiredDirection.y;
        Vec3 targetVelocity = new Vec3(
                steeringDirection.x * desiredSpeed,
                desiredVertical * verticalSpeed,
                steeringDirection.z * desiredSpeed
        );
        if (takingOff) {
            targetVelocity = new Vec3(targetVelocity.x,
                    FlightMotionPolicy.takeoffVerticalSpeed(targetVelocity.y, toTarget.y), targetVelocity.z);
        }
        targetVelocity = this.shapeLandingVelocity(landingPhase, targetVelocity, dragonPos, currentWaypoint);
        Vec3 velocityBaseline = currentVelocity;
        this.smoothedVelocity = lerpVelocity(velocityBaseline, targetVelocity, landingPhase);
        if (diving) {
            this.smoothedVelocity = new Vec3(
                    this.smoothedVelocity.x,
                    Mth.lerp(FlightMotionPolicy.DIVE_VERTICAL_LERP, velocityBaseline.y, targetVelocity.y),
                    this.smoothedVelocity.z
            );
        }
        if (!landingTarget && !this.flightCapable.isTakeoff()) {
            double horizontalSpeed = Mth.lerp(HORIZONTAL_VELOCITY_LERP,
                    currentVelocity.horizontalDistance(), targetVelocity.horizontalDistance());
            Vec3 horizontalDirection = steeringDirection.multiply(1.0D, 0.0D, 1.0D).normalize();
            this.smoothedVelocity = horizontalDirection.scale(horizontalSpeed)
                    .add(0.0D, this.smoothedVelocity.y, 0.0D);
            double acceleration = landingPhase == AsyncFlightController.LandingPhase.NONE
                    && arrival == DragonFlightRequest.Arrival.PASS_THROUGH
                    ? FlightMotionPolicy.sprintAcceleration(desiredSpeed) : MAX_ACCELERATION;
            this.smoothedVelocity = diving
                    ? FlightMotionPolicy.limitDiveAcceleration(velocityBaseline, this.smoothedVelocity, acceleration)
                    : FlightMotionPolicy.limitAcceleration(velocityBaseline, this.smoothedVelocity, acceleration);
        } else if (takingOff) {
            this.smoothedVelocity = FlightMotionPolicy.limitAcceleration(velocityBaseline, this.smoothedVelocity);
        }
        this.smoothedVelocity = limitLandingMomentum(landingPhase, this.smoothedVelocity);
        if (landingPhase == AsyncFlightController.LandingPhase.GLIDE
                || landingPhase == AsyncFlightController.LandingPhase.FLARE) {
            this.smoothedVelocity = limitHorizontalSpeed(this.smoothedVelocity,
                    landingHorizontalSpeedLimit(landingPhase, currentWaypoint.subtract(dragonPos)));
        }
        if (landingPhase == AsyncFlightController.LandingPhase.TOUCHDOWN) {
            double horizontalDistance = currentWaypoint.subtract(dragonPos).horizontalDistance();
            this.smoothedVelocity = limitHorizontalSpeed(this.smoothedVelocity,
                    horizontalDistance <= TOUCHDOWN_HORIZONTAL_DEADZONE ? 0.0D
                            : Math.min(0.28D, horizontalDistance * 0.35D));
        }
        if (takingOff && currentVelocity.y > 0.0D) {
            this.smoothedVelocity = new Vec3(
                    this.smoothedVelocity.x,
                    Math.max(this.smoothedVelocity.y, Math.min(currentVelocity.y, FlightMotionPolicy.TAKEOFF_MIN_LIFT)),
                    this.smoothedVelocity.z
            );
        }
        boolean shouldPreserveVerticalMotion = landingTarget
                || this.flightCapable.isTakeoff()
                || Math.abs(desiredVertical) > 0.1D;
        if (!shouldPreserveVerticalMotion && Math.abs(this.smoothedVelocity.y) < VERTICAL_SPEED_DEADZONE) {
            this.smoothedVelocity = new Vec3(this.smoothedVelocity.x, 0.0D, this.smoothedVelocity.z);
        }
        if (landingPhase != AsyncFlightController.LandingPhase.TOUCHDOWN
                && (!takingOff || this.flightCapable.isFlying() && !this.dragon.onGround())) {
            this.smoothedVelocity = this.steering.checkedVelocity(this.smoothedVelocity);
        }
        this.dragon.setDeltaMovement(this.smoothedVelocity);
        this.dragon.hasImpulse = true;
        this.updateRotation(landingPhase == AsyncFlightController.LandingPhase.TOUCHDOWN,
                diving ? DIVE_MAX_PITCH_STEP : MAX_PITCH_STEP);
    }

    private Vec3 shapeLandingVelocity(AsyncFlightController.LandingPhase phase,
                                      Vec3 targetVelocity,
                                      Vec3 dragonPosition,
                                      Vec3 phaseTarget) {
        Vec3 offset = phaseTarget.subtract(dragonPosition);
        if (phase == AsyncFlightController.LandingPhase.GLIDE
                || phase == AsyncFlightController.LandingPhase.FLARE) {
            targetVelocity = limitHorizontalSpeed(targetVelocity, landingHorizontalSpeedLimit(phase, offset));
        }
        return switch (phase) {
            case GLIDE -> new Vec3(
                    targetVelocity.x,
                    Math.max(targetVelocity.y, -GLIDE_MAX_DESCENT_SPEED),
                    targetVelocity.z
            );
            case FLARE -> new Vec3(
                    targetVelocity.x,
                    Mth.clamp(targetVelocity.y, -FLARE_MAX_DESCENT_SPEED, 0.08D),
                    targetVelocity.z
            );
            case TOUCHDOWN -> {
                double altitude = dragonPosition.y - phaseTarget.y;
                double descentSpeed = altitude >= -VERTICAL_TARGET_DEADZONE
                        ? Mth.clamp(
                                Math.max(0.0D, altitude) * 0.20D,
                                TOUCHDOWN_MIN_DESCENT_SPEED,
                                TOUCHDOWN_MAX_DESCENT_SPEED
                        )
                        : 0.0D;
                Vec3 horizontalCorrection = offset.multiply(0.25D, 0.0D, 0.25D);
                horizontalCorrection = limitHorizontalSpeed(horizontalCorrection, 0.28D);
                yield new Vec3(horizontalCorrection.x, -descentSpeed, horizontalCorrection.z);
            }
            default -> targetVelocity;
        };
    }

    private double landingHorizontalSpeedLimit(AsyncFlightController.LandingPhase phase, Vec3 offset) {
        double arrivalRadius = phase == AsyncFlightController.LandingPhase.GLIDE
                ? Math.max(1.5D, dragon.getBbWidth() * 0.5D)
                : Math.max(0.9D, dragon.getBbWidth() * 0.3D);
        double exitSpeed = phase == AsyncFlightController.LandingPhase.GLIDE ? 0.55D : 0.18D;
        double brakingDistance = Math.max(0.0D, offset.horizontalDistance() - arrivalRadius
                - dragon.getDeltaMovement().horizontalDistance());
        return Math.sqrt(exitSpeed * exitSpeed + 2.0D * MAX_ACCELERATION * brakingDistance);
    }

    private static Vec3 limitHorizontalSpeed(Vec3 velocity, double maximum) {
        double horizontalSpeed = velocity.horizontalDistance();
        if (horizontalSpeed <= maximum || horizontalSpeed < 1.0E-6D) return velocity;
        double scale = maximum / horizontalSpeed;
        return new Vec3(velocity.x * scale, velocity.y, velocity.z * scale);
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

    boolean hasLandingContact() {
        Vec3 currentVelocity = this.dragon.getDeltaMovement();
        return this.dragon.onGround()
                || (this.dragon.verticalCollision && currentVelocity.y <= 0.0D);
    }

    void resetSteering() {
        this.steering.reset();
    }

    String steeringSummary() {
        return this.steering.debugSummary();
    }

    private static Vec3 lerpVelocity(Vec3 from,
                                     Vec3 to,
                                     AsyncFlightController.LandingPhase landingPhase) {
        double horizontalLerp = landingPhase.isCommitted()
                ? LANDING_HORIZONTAL_VELOCITY_LERP
                : HORIZONTAL_VELOCITY_LERP;
        double verticalLerp = switch (landingPhase) {
            case GLIDE -> GLIDE_VERTICAL_VELOCITY_LERP;
            case FLARE -> FLARE_VERTICAL_VELOCITY_LERP;
            case TOUCHDOWN -> TOUCHDOWN_VERTICAL_VELOCITY_LERP;
            default -> VERTICAL_VELOCITY_LERP;
        };
        return new Vec3(
                Mth.lerp(horizontalLerp, from.x, to.x),
                Mth.lerp(verticalLerp, from.y, to.y),
                Mth.lerp(horizontalLerp, from.z, to.z)
        );
    }

    private static Vec3 limitLandingMomentum(AsyncFlightController.LandingPhase phase, Vec3 velocity) {
        return switch (phase) {
            case GLIDE -> withVerticalFloor(velocity, -GLIDE_MAX_ACTUAL_DESCENT_SPEED);
            case FLARE -> withVerticalFloor(velocity, -FLARE_MAX_ACTUAL_DESCENT_SPEED);
            case TOUCHDOWN -> withVerticalFloor(velocity, -TOUCHDOWN_MAX_ACTUAL_DESCENT_SPEED);
            default -> velocity;
        };
    }

    private static Vec3 withVerticalFloor(Vec3 velocity, double minimumVerticalSpeed) {
        return velocity.y >= minimumVerticalSpeed
                ? velocity
                : new Vec3(velocity.x, minimumVerticalSpeed, velocity.z);
    }
}
