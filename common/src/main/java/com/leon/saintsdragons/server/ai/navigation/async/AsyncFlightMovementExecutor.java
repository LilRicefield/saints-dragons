package com.leon.saintsdragons.server.ai.navigation.async;

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
    private static final float MAX_YAW_STEP = 5.0f;
    private static final float MAX_PITCH_STEP = 2.5f;
    private static final float PITCH_DEADZONE_DEGREES = 3.5f;

    private final Mob dragon;
    private final DragonFlightCapable flightCapable;
    private Vec3 smoothedVelocity = Vec3.ZERO;
    private Vec3 smoothedDirection = Vec3.ZERO;

    AsyncFlightMovementExecutor(Mob dragon, DragonFlightCapable flightCapable) {
        this.dragon = dragon;
        this.flightCapable = flightCapable;
    }

    public void executeMovement(Vec3 lookAheadTarget,
                                Vec3 currentWaypoint,
                                double speedModifier,
                                double arrivalDist,
                                boolean queueEmpty,
                                AsyncFlightController.LandingPhase landingPhase) {
        Vec3 dragonPos = this.dragon.position();
        Vec3 currentVelocity = this.dragon.getDeltaMovement();
        boolean landingTarget = landingPhase.isCommitted();
        if (landingTarget
                && (this.dragon.onGround()
                    || (landingPhase == AsyncFlightController.LandingPhase.TOUCHDOWN
                        && this.hasLandingContact()))) {
            this.zeroVelocity();
            return;
        }

        Vec3 target = lookAheadTarget != null ? lookAheadTarget : currentWaypoint;
        Vec3 toTarget = target.subtract(dragonPos);
        double distToTarget = toTarget.length();
        if (distToTarget < 0.1) {
            return;
        }

        double desiredSpeed = Math.max(0.0, this.flightCapable.getFlightSpeed()) * speedModifier;
        double distToFinalWaypoint = dragonPos.distanceTo(currentWaypoint);
        double decelStartDist = arrivalDist * 2.0;
        desiredSpeed *= switch (landingPhase) {
            case GLIDE -> GLIDE_SPEED_SCALE;
            case FLARE -> FLARE_SPEED_SCALE;
            case TOUCHDOWN -> TOUCHDOWN_SPEED_SCALE;
            default -> 1.0D;
        };
        boolean ordinaryFinalWaypoint = landingPhase == AsyncFlightController.LandingPhase.NONE
                || landingPhase == AsyncFlightController.LandingPhase.GO_AROUND;
        if (ordinaryFinalWaypoint && distToFinalWaypoint < decelStartDist && queueEmpty) {
            desiredSpeed *= Math.max(0.3, distToFinalWaypoint / decelStartDist);
        }

        Vec3 desiredDirection = toTarget.normalize();
        double desiredVertical = Math.abs(toTarget.y) < VERTICAL_TARGET_DEADZONE && !landingTarget ? 0.0D : desiredDirection.y;
        Vec3 targetVelocity = new Vec3(
                desiredDirection.x * desiredSpeed,
                desiredVertical * desiredSpeed,
                desiredDirection.z * desiredSpeed
        );
        targetVelocity = this.shapeLandingVelocity(landingPhase, targetVelocity, dragonPos, currentWaypoint);
        Vec3 velocityBaseline = this.smoothedVelocity;
        if (velocityBaseline.lengthSqr() < 1.0E-4 && currentVelocity.lengthSqr() > 1.0E-4) {
            velocityBaseline = currentVelocity;
        }
        this.smoothedVelocity = lerpVelocity(velocityBaseline, targetVelocity, landingPhase);
        this.smoothedVelocity = limitLandingMomentum(landingPhase, this.smoothedVelocity);
        if (this.flightCapable.isTakeoff() && currentVelocity.y > 0.0D) {
            this.smoothedVelocity = new Vec3(
                    this.smoothedVelocity.x,
                    Math.max(this.smoothedVelocity.y, currentVelocity.y),
                    this.smoothedVelocity.z
            );
        }
        boolean shouldPreserveVerticalMotion = landingTarget
                || this.flightCapable.isTakeoff()
                || Math.abs(desiredVertical) > 0.1D;
        if (!shouldPreserveVerticalMotion && Math.abs(this.smoothedVelocity.y) < VERTICAL_SPEED_DEADZONE) {
            this.smoothedVelocity = new Vec3(this.smoothedVelocity.x, 0.0D, this.smoothedVelocity.z);
        }
        this.dragon.setDeltaMovement(this.smoothedVelocity);
        this.dragon.hasImpulse = true;
        this.updateRotation();
    }

    private Vec3 shapeLandingVelocity(AsyncFlightController.LandingPhase phase,
                                      Vec3 targetVelocity,
                                      Vec3 dragonPosition,
                                      Vec3 phaseTarget) {
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
                yield new Vec3(targetVelocity.x, -descentSpeed, targetVelocity.z);
            }
            default -> targetVelocity;
        };
    }

    public void updateRotation() {
        Vec3 velocity = this.smoothedVelocity;
        if (velocity.lengthSqr() < 1.0E-4) {
            return;
        }

        Vec3 horizDir = new Vec3(velocity.x, 0.0, velocity.z);
        if (horizDir.lengthSqr() < 1.0E-4) {
            return;
        }

        horizDir = horizDir.normalize();
        if (this.smoothedDirection.lengthSqr() < 0.001) {
            this.smoothedDirection = horizDir;
        } else {
            this.smoothedDirection = lerpVec3(this.smoothedDirection, horizDir, 0.12);
            if (this.smoothedDirection.lengthSqr() > 0.001) {
                this.smoothedDirection = this.smoothedDirection.normalize();
            }
        }

        float targetYaw = -(float) Math.toDegrees(Mth.atan2(this.smoothedDirection.x, this.smoothedDirection.z));
        float yawDiff = Mth.wrapDegrees(targetYaw - this.dragon.getYRot());
        float newYaw = this.dragon.getYRot() + Mth.clamp(yawDiff, -MAX_YAW_STEP, MAX_YAW_STEP);
        this.dragon.setYRot(newYaw);
        this.dragon.yBodyRot = newYaw;
        this.dragon.setYHeadRot(newYaw);

        float targetPitch = (float) (-Math.toDegrees(Mth.atan2(velocity.y, Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z))));
        float pitchDiff = Mth.wrapDegrees(targetPitch - this.dragon.getXRot());
        if (Math.abs(pitchDiff) < PITCH_DEADZONE_DEGREES) {
            pitchDiff = 0.0f;
        }
        this.dragon.setXRot(this.dragon.getXRot() + Mth.clamp(pitchDiff, -MAX_PITCH_STEP, MAX_PITCH_STEP));
    }

    public void applyIdleFriction() {
        if (this.smoothedVelocity.lengthSqr() > 1.0E-4) {
            this.smoothedVelocity = this.smoothedVelocity.scale(0.85);
            this.dragon.setDeltaMovement(this.smoothedVelocity);
        } else {
            this.smoothedVelocity = Vec3.ZERO;
            this.dragon.setDeltaMovement(Vec3.ZERO);
        }
    }

    public void zeroVelocity() {
        this.smoothedVelocity = Vec3.ZERO;
        this.dragon.setDeltaMovement(Vec3.ZERO);
    }

    boolean hasLandingContact() {
        Vec3 currentVelocity = this.dragon.getDeltaMovement();
        return this.dragon.onGround()
                || (this.dragon.verticalCollision && currentVelocity.y <= 0.0D);
    }

    private static Vec3 lerpVec3(Vec3 from, Vec3 to, double t) {
        return new Vec3(
                Mth.lerp(t, from.x, to.x),
                Mth.lerp(t, from.y, to.y),
                Mth.lerp(t, from.z, to.z)
        );
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
