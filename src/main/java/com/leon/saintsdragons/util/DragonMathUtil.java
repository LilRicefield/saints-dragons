package com.leon.saintsdragons.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Math utilities for wyvern movement and behavior
 */
public class DragonMathUtil {

    /**
     * Smoothly approach a target value with acceleration/deceleration curves
     */
    public static float approachSmooth(float current, float previous, float desired, float desiredSpeed, float deltaSpeed) {
        float prevSpeed = current - previous;
        desiredSpeed = Math.abs(desiredSpeed);
        desiredSpeed = current < desired ? desiredSpeed : -desiredSpeed;
        float speed = Mth.approach(prevSpeed, desiredSpeed, deltaSpeed);

        // Extra math to make speed smaller when current is close to desired
        float speedApproachReduction = (float) (1.0f - Math.pow(
                Mth.clamp(-Math.abs(current - desired) / Math.abs(2 * desiredSpeed / deltaSpeed) + 1.0f, 0, 1), 4
        ));
        speed *= speedApproachReduction;

        return current < desired ?
                Mth.clamp(current + speed, current, desired) :
                Mth.clamp(current + speed, desired, current);
    }

    public static float approachDegreesSmooth(float current, float previous, float desired, float desiredSpeed, float deltaSpeed) {
        float desiredDifference = Mth.degreesDifference(current, desired);
        float previousDifference = Mth.degreesDifference(current, previous);
        return approachSmooth(current, current + previousDifference, current + desiredDifference, desiredSpeed, deltaSpeed);
    }

    /**
     * Calculate angle between two entities in degrees
     */
    public static double getAngleBetweenEntities(Entity first, Entity second) {
        return Math.atan2(second.getZ() - first.getZ(), second.getX() - first.getX()) * (180 / Math.PI) + 90;
    }

    /**
     * Get dot product between entity's facing direction and target
     */
    public static double getDotProductBodyFacingEntity(Entity entity, Entity target) {
        Vec3 vecBetween = target.position().subtract(entity.position()).normalize();
        return vecBetween.dot(Vec3.directionFromRotation(0, entity.getYRot()).normalize());
    }

    /**
     * Calculate circular position around a target
     */
    public static Vec3 circleEntityPosition(Entity target, float radius, float speed, boolean clockwise, int frame, float offset) {
        int direction = clockwise ? 1 : -1;
        double t = direction * frame * 0.5 * speed / radius + offset;
        return target.position().add(radius * Math.cos(t), 0, radius * Math.sin(t));
    }

    /**
     * Get entities nearby with filtering
     */
    public static <T extends Entity> List<T> getEntitiesNearby(Entity entity, Class<T> entityClass, double radius) {
        return entity.level().getEntitiesOfClass(entityClass,
                entity.getBoundingBox().inflate(radius, radius, radius),
                e -> e != entity && entity.distanceTo(e) <= radius + e.getBbWidth() / 2f);
    }

    /**
     * Get attackable living entities nearby
     */
    public static List<LivingEntity> getAttackableEntitiesNearby(Entity entity, double radius) {
        List<Entity> nearbyEntities = entity.level().getEntities(entity,
                entity.getBoundingBox().inflate(radius, radius, radius));

        return nearbyEntities.stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .filter(LivingEntity::attackable)
                .filter(e -> entity.distanceTo(e) <= radius + e.getBbWidth() / 2f)
                .collect(Collectors.toList());
    }


    /**
     * Calculate flight vector towards target with smooth acceleration
     */
    public static Vec3 calculateFlightVector(Entity entity, Vec3 target, double speed, double acceleration) {
        Vec3 direction = target.subtract(entity.position()).normalize();
        Vec3 currentVel = entity.getDeltaMovement();
        Vec3 targetVel = direction.scale(speed);

        // Smooth acceleration towards target velocity
        return currentVel.add(targetVel.subtract(currentVel).scale(acceleration));
    }

    /**
     * Apply smooth look rotation towards target
     */
    public static void smoothLookAt(LivingEntity entity, Entity target, float maxYawChange, float maxPitchChange) {
        double dx = target.getX() - entity.getX();
        double dy = target.getEyeY() - entity.getEyeY();
        double dz = target.getZ() - entity.getZ();

        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float)(-(Math.atan2(dy, horizontalDist) * (180.0 / Math.PI)));

        // Use smooth angular approach that eases near the target to reduce jitter
        float currentYaw = entity.getYRot();
        float prevYaw = entity.yRotO;
        float newYaw = approachDegreesSmooth(currentYaw, prevYaw, targetYaw, maxYawChange, Math.max(1.0f, maxYawChange * 0.5f));
        entity.setYRot(newYaw);

        float currentPitch = entity.getXRot();
        float prevPitch = entity.xRotO;
        float newPitch = approachDegreesSmooth(currentPitch, prevPitch, targetPitch, maxPitchChange, Math.max(1.0f, maxPitchChange * 0.5f));
        entity.setXRot(newPitch);
        entity.yBodyRot = entity.getYRot();
    }

    /**
     * Check if an entity can see another entity (line of sight)
     */
    public static boolean hasLineOfSight(LivingEntity entity, Entity target) {
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            return mob.getSensing().hasLineOfSight(target);
        }
        // Fallback: simple distance check if not a Mob
        return entity.distanceTo(target) < Objects.requireNonNull(entity.getAttribute(Attributes.FOLLOW_RANGE)).getValue();
    }

    /**
     * Calculate optimal dodge direction from projectile
     */
    public static Vec3 calculateDodgeDirection(Entity entity, Entity projectile) {
        Vec3 projectileVel = new Vec3(
                projectile.getX() - projectile.xo,
                projectile.getY() - projectile.yo,
                projectile.getZ() - projectile.zo
        );

        if (projectileVel.lengthSqr() < 0.001) {
            return Vec3.ZERO;
        }

        // Calculate perpendicular dodge direction
        Vec3 lateral = projectileVel.normalize().cross(new Vec3(0, 1, 0));
        if (lateral.lengthSqr() < 1.0e-6) {
            return Vec3.ZERO; // Can't dodge straight up/down projectiles effectively
        }

        lateral = lateral.normalize();

        // Choose the side that moves away from the projectile's predicted impact
        Vec3 entityPos = entity.position();
        Vec3 predictedImpact = projectile.position().add(projectileVel.scale(20)); // Predict 20 ticks ahead

        Vec3 leftPos = entityPos.add(lateral);
        Vec3 rightPos = entityPos.add(lateral.scale(-1));

        // Choose the side further from predicted impact
        if (rightPos.subtract(predictedImpact).lengthSqr() > leftPos.subtract(predictedImpact).lengthSqr()) {
            lateral = lateral.scale(-1);
        }

        return lateral;
    }

    /**
     * Apply smooth easing to a value (ease-in-out cubic)
     */
    public static float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    /**
     * Apply smooth easing to a value (ease-out sine)
     */
    public static float easeOutSine(float t) {
        return (float) Math.sin((t * Math.PI) / 2);
    }

    /**
     * Linear interpolation with easing
     */
    public static float lerpSmooth(float start, float end, float progress, EasingFunction easing) {
        float easedProgress = switch (easing) {
            case LINEAR -> progress;
            case EASE_IN_OUT_CUBIC -> easeInOutCubic(progress);
            case EASE_OUT_SINE -> easeOutSine(progress);
        };
        return Mth.lerp(easedProgress, start, end);
    }

    public enum EasingFunction {
        LINEAR,
        EASE_IN_OUT_CUBIC,
        EASE_OUT_SINE
    }

    /**
     * Clamp a vector's length to a maximum value
     */
    public static Vec3 clampVectorLength(Vec3 vector, double maxLength) {
        double lengthSq = vector.lengthSqr();
        if (lengthSq > maxLength * maxLength) {
            return vector.normalize().scale(maxLength);
        }
        return vector;
    }

    /**
     * Add horizontal movement only (preserve Y component)
     */
    public static Vec3 addHorizontalMovement(Vec3 currentVel, Vec3 addedVel) {
        return new Vec3(
                currentVel.x + addedVel.x,
                currentVel.y,
                currentVel.z + addedVel.z
        );
    }

    /**
     * Calculate repulsion force between entities
     */
    public static Vec3 calculateRepulsionForce(Entity center, Entity target, double radius, double strength) {
        Vec3 direction = target.position().subtract(center.position());
        double distance = direction.length();

        if (distance >= radius || distance == 0) {
            return Vec3.ZERO;
        }

        // Inverse square falloff
        double force = strength * (1.0 - distance / radius) * (1.0 - distance / radius);
        return direction.normalize().scale(force);
    }

    /**
     * Create quaternion from XYZ rotation (for rendering)
     */
    public static Quaternionf quatFromRotationXYZ(float x, float y, float z, boolean degrees) {
        if (degrees) {
            x = (float) Math.toRadians(x);
            y = (float) Math.toRadians(y);
            z = (float) Math.toRadians(z);
        }
        return new Quaternionf().rotationXYZ(x, y, z);
    }
    
    /**
     * Clamp altitude within reasonable bounds above ground
     */
    public static double clampAltitude(double currentY, double groundY, double minAbove, double maxAbove) {
        return Mth.clamp(currentY, groundY + minAbove, groundY + maxAbove);
    }
    
    /**
     * Calculate yaw error from current entity to target
     */
    public static float yawErrorToTarget(Entity self, Entity target) {
        float wantedYaw = (float) (Math.atan2(target.getZ() - self.getZ(), target.getX() - self.getX()) * 180 / Math.PI) - 90f;
        return Math.abs(Mth.degreesDifference(self.getYRot(), wantedYaw));
    }
}
