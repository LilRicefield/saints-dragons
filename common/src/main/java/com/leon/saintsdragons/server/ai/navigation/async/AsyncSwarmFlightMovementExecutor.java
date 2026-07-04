package com.leon.saintsdragons.server.ai.navigation.async;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

class AsyncSwarmFlightMovementExecutor {
    private static final double HORIZONTAL_VELOCITY_LERP = 0.16D;
    private static final double VERTICAL_VELOCITY_LERP = 0.12D;
    private static final double IDLE_DRAG = 0.86D;
    private static final float MAX_YAW_STEP = 7.0F;
    private static final float MAX_PITCH_STEP = 4.0F;

    private final Mob mob;
    private Vec3 smoothedVelocity = Vec3.ZERO;
    private Vec3 smoothedDirection = Vec3.ZERO;

    AsyncSwarmFlightMovementExecutor(Mob mob) {
        this.mob = mob;
    }

    void executeMovement(Vec3 lookAheadTarget, Vec3 finalWaypoint, double speed, double arrivalDistance, boolean finalTarget) {
        Vec3 target = lookAheadTarget != null ? lookAheadTarget : finalWaypoint;
        Vec3 toTarget = target.subtract(this.mob.position());
        if (toTarget.lengthSqr() < 0.01D) {
            return;
        }

        double targetSpeed = Math.max(0.0D, speed);
        if (finalTarget) {
            double finalDistance = this.mob.position().distanceTo(finalWaypoint);
            double slowDownStart = arrivalDistance * 2.5D;
            if (finalDistance < slowDownStart) {
                targetSpeed *= Math.max(0.25D, finalDistance / slowDownStart);
            }
        }

        Vec3 direction = toTarget.normalize();
        Vec3 targetVelocity = direction.scale(targetSpeed);
        Vec3 baseline = this.smoothedVelocity.lengthSqr() > 1.0E-4D
                ? this.smoothedVelocity
                : this.mob.getDeltaMovement();
        this.smoothedVelocity = new Vec3(
                Mth.lerp(HORIZONTAL_VELOCITY_LERP, baseline.x, targetVelocity.x),
                Mth.lerp(VERTICAL_VELOCITY_LERP, baseline.y, targetVelocity.y),
                Mth.lerp(HORIZONTAL_VELOCITY_LERP, baseline.z, targetVelocity.z)
        );

        this.mob.setDeltaMovement(this.smoothedVelocity);
        this.mob.hasImpulse = true;
        updateRotation();
    }

    void applyIdleFriction() {
        if (this.smoothedVelocity.lengthSqr() <= 1.0E-4D) {
            this.smoothedVelocity = Vec3.ZERO;
            this.mob.setDeltaMovement(Vec3.ZERO);
            return;
        }

        this.smoothedVelocity = this.smoothedVelocity.scale(IDLE_DRAG);
        this.mob.setDeltaMovement(this.smoothedVelocity);
        this.mob.hasImpulse = true;
    }

    void zeroVelocity() {
        this.smoothedVelocity = Vec3.ZERO;
        this.smoothedDirection = Vec3.ZERO;
        this.mob.setDeltaMovement(Vec3.ZERO);
    }

    private void updateRotation() {
        Vec3 velocity = this.smoothedVelocity;
        if (velocity.lengthSqr() < 1.0E-4D) {
            return;
        }

        Vec3 horizontal = new Vec3(velocity.x, 0.0D, velocity.z);
        if (horizontal.lengthSqr() > 1.0E-4D) {
            horizontal = horizontal.normalize();
            this.smoothedDirection = this.smoothedDirection.lengthSqr() < 1.0E-4D
                    ? horizontal
                    : this.smoothedDirection.lerp(horizontal, 0.16D).normalize();

            float targetYaw = -(float) Math.toDegrees(Mth.atan2(this.smoothedDirection.x, this.smoothedDirection.z));
            float yawDiff = Mth.wrapDegrees(targetYaw - this.mob.getYRot());
            float yaw = this.mob.getYRot() + Mth.clamp(yawDiff, -MAX_YAW_STEP, MAX_YAW_STEP);
            this.mob.setYRot(yaw);
        }

        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float targetPitch = (float) -Math.toDegrees(Mth.atan2(velocity.y, horizontalSpeed));
        float pitchDiff = Mth.wrapDegrees(targetPitch - this.mob.getXRot());
        this.mob.setXRot(this.mob.getXRot() + Mth.clamp(pitchDiff, -MAX_PITCH_STEP, MAX_PITCH_STEP));
    }
}
