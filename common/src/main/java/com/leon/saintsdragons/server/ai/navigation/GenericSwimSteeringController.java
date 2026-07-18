package com.leon.saintsdragons.server.ai.navigation;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class GenericSwimSteeringController {
    private static final double HORIZONTAL_VELOCITY_LERP = 0.28D;
    private static final double VERTICAL_VELOCITY_LERP = 0.22D;

    private final Mob mob;
    private double currentYaw;
    private double currentPitch;
    private Vec3 smoothedVelocity = Vec3.ZERO;
    private boolean initialized;
    private boolean moving;

    public GenericSwimSteeringController(Mob mob) {
        this.mob = mob;
    }

    public void resetFromMob() {
        this.currentYaw = mob.getYRot();
        this.currentPitch = mob.getXRot();
        this.smoothedVelocity = mob.getDeltaMovement();
        this.initialized = true;
        this.moving = false;
    }

    public void clear() {
        this.initialized = false;
        this.moving = false;
        this.smoothedVelocity = Vec3.ZERO;
    }

    public void slow(double factor) {
        Vec3 velocity = mob.getDeltaMovement().scale(factor);
        mob.setDeltaMovement(velocity);
        this.smoothedVelocity = velocity;
        this.moving = velocity.horizontalDistanceSqr() > 0.0025D;
    }

    public boolean isMoving() {
        return moving;
    }

    public void moveToward(Vec3 target, double speed, float turnSpeedDegrees) {
        if (!initialized) {
            resetFromMob();
        }

        Vec3 origin = mob.position().add(0.0D, mob.getBbHeight() * 0.18D, 0.0D);
        Vec3 offset = target.subtract(origin);
        double horizontalDist = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        if (horizontalDist < 1.0E-5D && Math.abs(offset.y) < 1.0E-5D) {
            this.moving = false;
            return;
        }

        double targetYaw = Mth.atan2(offset.z, offset.x) * Mth.RAD_TO_DEG - 90.0D;
        double targetPitch = -(Mth.atan2(offset.y, horizontalDist) * Mth.RAD_TO_DEG);
        targetPitch = Mth.clamp(targetPitch, -85.0D, 85.0D);

        double yawDelta = Mth.wrapDegrees(targetYaw - currentYaw);
        currentYaw = Mth.wrapDegrees(currentYaw + Mth.clamp(yawDelta, -turnSpeedDegrees, turnSpeedDegrees));

        double pitchStep = Math.max(1.0D, turnSpeedDegrees * 0.45D);
        double pitchDelta = Mth.clamp(targetPitch - currentPitch, -pitchStep, pitchStep);
        currentPitch += pitchDelta;

        mob.setYRot((float) currentYaw);
        mob.yBodyRot = (float) currentYaw;
        mob.yHeadRot = (float) currentYaw;
        mob.setXRot((float) currentPitch);

        double yawRad = currentYaw * Mth.DEG_TO_RAD;
        double pitchRad = currentPitch * Mth.DEG_TO_RAD;
        double headingAlignment = (Math.cos(yawDelta * Mth.DEG_TO_RAD) + 1.0D) * 0.5D;
        double alignedSpeed = speed * Mth.clamp(0.18D + headingAlignment * 0.82D, 0.18D, 1.0D);
        Vec3 targetVelocity = new Vec3(
                -Math.sin(yawRad) * Math.cos(pitchRad) * alignedSpeed,
                -Math.sin(pitchRad) * alignedSpeed,
                Math.cos(yawRad) * Math.cos(pitchRad) * alignedSpeed
        );

        this.smoothedVelocity = new Vec3(
                lerp(smoothedVelocity.x, targetVelocity.x, HORIZONTAL_VELOCITY_LERP),
                lerp(smoothedVelocity.y, targetVelocity.y, VERTICAL_VELOCITY_LERP),
                lerp(smoothedVelocity.z, targetVelocity.z, HORIZONTAL_VELOCITY_LERP)
        );
        mob.setDeltaMovement(smoothedVelocity);
        mob.hasImpulse = true;
        this.moving = true;
    }

    private static double lerp(double from, double to, double amount) {
        return from + (to - from) * amount;
    }
}
