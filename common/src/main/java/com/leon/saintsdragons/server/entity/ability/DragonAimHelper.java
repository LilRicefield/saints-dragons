package com.leon.saintsdragons.server.entity.ability;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class DragonAimHelper {
    private static final double EPSILON = 1.0E-6D;
    public static final Vec3 DEFAULT_FORWARD = new Vec3(0.0D, 0.0D, 1.0D);

    private DragonAimHelper() {
    }

    @Nullable
    public static Vec3 riderViewDirection(Entity dragon) {
        Entity rider = dragon.getControllingPassenger();
        if (rider instanceof LivingEntity living) {
            return normalizeOrNull(living.getLookAngle());
        }
        return null;
    }

    public static Vec3 targetAimPoint(LivingEntity target, double leadFactor) {
        return target.getEyePosition().add(target.getDeltaMovement().scale(leadFactor));
    }

    @Nullable
    public static Vec3 directionTo(Vec3 origin, Vec3 aimPoint) {
        return normalizeOrNull(aimPoint.subtract(origin));
    }

    @Nullable
    public static Vec3 directionToTarget(Vec3 origin, @Nullable LivingEntity target, double leadFactor) {
        if (target == null || !target.isAlive()) {
            return null;
        }
        return directionTo(origin, targetAimPoint(target, leadFactor));
    }

    public static Vec3 lookDirectionOrDefault(Entity entity) {
        Vec3 look = normalizeOrNull(entity.getLookAngle());
        return look != null ? look : DEFAULT_FORWARD;
    }

    @Nullable
    public static Vec3 fallbackHeadDirection(Entity entity) {
        return normalizeOrNull(Vec3.directionFromRotation(entity.getXRot(), entity.getYHeadRot()));
    }

    public static Vec3 riderTargetOrLookDirection(Entity dragon,
                                                  Vec3 origin,
                                                  @Nullable LivingEntity target,
                                                  double leadFactor) {
        Vec3 riderDirection = riderViewDirection(dragon);
        if (riderDirection != null) {
            return riderDirection;
        }

        Vec3 targetDirection = directionToTarget(origin, target, leadFactor);
        if (targetDirection != null) {
            return targetDirection;
        }

        return lookDirectionOrDefault(dragon);
    }

    public static Vec3 blendDirection(@Nullable Vec3 current, Vec3 desired, boolean smooth, double blend) {
        Vec3 normalizedDesired = normalizeOrNull(desired);
        if (normalizedDesired == null) {
            return DEFAULT_FORWARD;
        }
        if (current == null || !smooth) {
            return normalizedDesired;
        }

        Vec3 mixed = current.add(normalizedDesired.subtract(current).scale(blend));
        Vec3 normalizedMixed = normalizeOrNull(mixed);
        return normalizedMixed != null ? normalizedMixed : normalizedDesired;
    }

    @Nullable
    public static Vec3 clampDirectionToHead(Vec3 desiredDir,
                                            float headYaw,
                                            float headPitch,
                                            float maxYawDeg,
                                            float maxPitchDeg) {
        Vec3 dir = normalizeOrNull(desiredDir);
        if (dir == null) {
            return null;
        }

        float desiredYaw = (float) (Math.atan2(-dir.x, dir.z) * (180.0D / Math.PI));
        float desiredPitch = (float) (-Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * (180.0D / Math.PI));
        float yawErr = Mth.degreesDifference(headYaw, desiredYaw);
        float pitchErr = desiredPitch - headPitch;
        float finalYaw = headYaw + Mth.clamp(yawErr, -maxYawDeg, maxYawDeg);
        float finalPitch = headPitch + Mth.clamp(pitchErr, -maxPitchDeg, maxPitchDeg);

        return normalizeOrNull(Vec3.directionFromRotation(finalPitch, finalYaw));
    }

    @Nullable
    public static Vec3 normalizeOrNull(Vec3 vec) {
        return vec.lengthSqr() > EPSILON ? vec.normalize() : null;
    }
}
