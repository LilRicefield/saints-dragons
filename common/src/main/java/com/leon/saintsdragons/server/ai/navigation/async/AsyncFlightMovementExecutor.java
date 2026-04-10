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

    public void executeMovement(Vec3 lookAheadTarget, Vec3 currentWaypoint, double speedModifier, double arrivalDist,
                                boolean queueEmpty, boolean landingTarget) {
        Vec3 dragonPos = this.dragon.position();
        Vec3 currentVelocity = this.dragon.getDeltaMovement();
        Vec3 target = lookAheadTarget != null ? lookAheadTarget : currentWaypoint;
        Vec3 toTarget = target.subtract(dragonPos);
        Vec3 toWaypoint = currentWaypoint.subtract(dragonPos);
        double distToTarget = toTarget.length();
        if (distToTarget < 0.1) {
            return;
        }

        double desiredSpeed = Math.max(0.0, this.flightCapable.getFlightSpeed()) * speedModifier;
        double distToFinalWaypoint = dragonPos.distanceTo(currentWaypoint);
        double decelStartDist = arrivalDist * 2.0;
        if (distToFinalWaypoint < decelStartDist && queueEmpty) {
            desiredSpeed *= Math.max(0.3, distToFinalWaypoint / decelStartDist);
        }

        Vec3 desiredDirection = toTarget.normalize();
        double desiredVertical = Math.abs(toTarget.y) < VERTICAL_TARGET_DEADZONE && !landingTarget ? 0.0D : desiredDirection.y;
        if (!landingTarget && toWaypoint.y > 1.0D) {
            double waypointVertical = toWaypoint.normalize().y;
            desiredVertical = Math.max(desiredVertical, waypointVertical);

            // If the short look-ahead segment is flat but the final waypoint is still well above us,
            // keep a minimum upward bias so the dragon continues climbing instead of skating forward.
            if (desiredVertical < 0.12D) {
                desiredVertical = Math.min(0.35D, Math.max(0.12D, toWaypoint.y * 0.08D));
            }
        }
        if (landingTarget && currentWaypoint.y <= dragonPos.y && distToFinalWaypoint < arrivalDist * 3.0D) {
            desiredVertical = Math.min(desiredVertical, -0.15D);
        }
        Vec3 targetVelocity = new Vec3(
                desiredDirection.x * desiredSpeed,
                desiredVertical * desiredSpeed,
                desiredDirection.z * desiredSpeed
        );
        Vec3 velocityBaseline = this.smoothedVelocity;
        if (velocityBaseline.lengthSqr() < 1.0E-4 && currentVelocity.lengthSqr() > 1.0E-4) {
            velocityBaseline = currentVelocity;
        }
        this.smoothedVelocity = lerpVelocity(velocityBaseline, targetVelocity, landingTarget);
        if (this.flightCapable.isTakeoff() && currentVelocity.y > 0.0D) {
            this.smoothedVelocity = new Vec3(
                    this.smoothedVelocity.x,
                    Math.max(this.smoothedVelocity.y, currentVelocity.y),
                    this.smoothedVelocity.z
            );
        }
        boolean shouldPreserveVerticalMotion = landingTarget
                || this.flightCapable.isTakeoff()
                || Math.abs(toWaypoint.y) > 1.0D
                || Math.abs(desiredVertical) > 0.1D;
        if (!shouldPreserveVerticalMotion && Math.abs(this.smoothedVelocity.y) < VERTICAL_SPEED_DEADZONE) {
            this.smoothedVelocity = new Vec3(this.smoothedVelocity.x, 0.0D, this.smoothedVelocity.z);
        }
        this.dragon.setDeltaMovement(this.smoothedVelocity);
        this.dragon.hasImpulse = true;
        this.updateRotation();
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

    private static Vec3 lerpVec3(Vec3 from, Vec3 to, double t) {
        return new Vec3(
                Mth.lerp(t, from.x, to.x),
                Mth.lerp(t, from.y, to.y),
                Mth.lerp(t, from.z, to.z)
        );
    }

    private static Vec3 lerpVelocity(Vec3 from, Vec3 to, boolean landingTarget) {
        double verticalLerp = landingTarget ? 0.18D : VERTICAL_VELOCITY_LERP;
        return new Vec3(
                Mth.lerp(HORIZONTAL_VELOCITY_LERP, from.x, to.x),
                Mth.lerp(verticalLerp, from.y, to.y),
                Mth.lerp(HORIZONTAL_VELOCITY_LERP, from.z, to.z)
        );
    }
}
