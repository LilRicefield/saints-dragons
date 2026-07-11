package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane;

import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public final class CindervanePackFlightCoordinator {
    private static final double NEIGHBOR_RADIUS = 42.0D;
    private static final double SEPARATION_RANGE = 30.0D;
    private static final double FORMATION_MIN_DISTANCE = 32.0D;
    private static final double FORMATION_MAX_DISTANCE = 90.0D;
    private static final double CRUISE_COHESION_WEIGHT = 0.50D;
    private static final double CRUISE_ALIGNMENT_WEIGHT = 0.45D;
    private static final double CRUISE_SEPARATION_WEIGHT = 1.35D;
    private static final double FOLLOW_FORMATION_WEIGHT = 1.20D;
    private static final double FOLLOW_COHESION_WEIGHT = 0.25D;
    private static final double FOLLOW_ALIGNMENT_WEIGHT = 0.35D;
    private static final double FOLLOW_SEPARATION_WEIGHT = 1.55D;

    private CindervanePackFlightCoordinator() {
    }

    public static Vec3 biasCruiseTarget(Cindervane dragon, Vec3 cruiseTarget) {
        if (cruiseTarget == null
                || !dragon.canParticipateInPack()
                || !(dragon.level() instanceof ServerLevel serverLevel)
                || !dragon.isFlying()
                || dragon.isLanding()
                || dragon.getTarget() != null) {
            return cruiseTarget;
        }

        List<Cindervane> neighbors = findAirbornePackmates(serverLevel, dragon);
        if (neighbors.isEmpty()) {
            return cruiseTarget;
        }

        Vec3 cruiseDirection = cruiseTarget.subtract(dragon.position());
        if (cruiseDirection.lengthSqr() < 1.0D) {
            return cruiseTarget;
        }

        BoidVectors boids = calculateBoids(dragon, neighbors);
        Vec3 heading = normalizeOrZero(cruiseDirection).scale(1.0D)
                .add(normalizeOrZero(boids.cohesion()).scale(CRUISE_COHESION_WEIGHT))
                .add(normalizeOrZero(boids.alignment()).scale(CRUISE_ALIGNMENT_WEIGHT))
                .add(normalizeOrZero(boids.separation()).scale(CRUISE_SEPARATION_WEIGHT));
        if (heading.lengthSqr() < 1.0E-4D) {
            return cruiseTarget;
        }

        double distance = clamp(cruiseDirection.length(), FORMATION_MIN_DISTANCE, FORMATION_MAX_DISTANCE);
        Vec3 biasedTarget = dragon.position().add(heading.normalize().scale(distance));
        biasedTarget = new Vec3(biasedTarget.x, cruiseTarget.y, biasedTarget.z);
        return dragon.isValidStandardFlightTarget(biasedTarget) ? biasedTarget : cruiseTarget;
    }

    public static Vec3 followFormationTarget(Cindervane dragon, Cindervane leader) {
        Vec3 formation = formationSlot(dragon, leader);
        if (!(dragon.level() instanceof ServerLevel serverLevel)) {
            return formation;
        }

        List<Cindervane> neighbors = findAirbornePackmates(serverLevel, dragon);
        if (neighbors.isEmpty()) {
            return formation;
        }

        BoidVectors boids = calculateBoids(dragon, neighbors);
        Vec3 formationDirection = formation.subtract(dragon.position());
        Vec3 heading = normalizeOrZero(formationDirection).scale(FOLLOW_FORMATION_WEIGHT)
                .add(normalizeOrZero(boids.cohesion()).scale(FOLLOW_COHESION_WEIGHT))
                .add(normalizeOrZero(boids.alignment()).scale(FOLLOW_ALIGNMENT_WEIGHT))
                .add(normalizeOrZero(boids.separation()).scale(FOLLOW_SEPARATION_WEIGHT));
        if (heading.lengthSqr() < 1.0E-4D) {
            return formation;
        }

        double distance = clamp(formationDirection.length(), 6.0D, 28.0D);
        Vec3 coordinated = dragon.position().add(heading.normalize().scale(distance));
        return new Vec3(coordinated.x, formation.y, coordinated.z);
    }

    private static Vec3 formationSlot(Cindervane dragon, Cindervane leader) {
        Vec3 leaderLook = horizontalOrFallback(leader.getLookAngle());
        Vec3 lateral = new Vec3(-leaderLook.z, 0.0D, leaderLook.x).normalize();

        int slot = Math.floorMod(dragon.getUUID().hashCode(), 5);
        double lateralOffset = switch (slot) {
            case 0 -> -7.0D;
            case 1 -> 7.0D;
            case 2 -> -13.0D;
            case 3 -> 13.0D;
            default -> 0.0D;
        };
        double trailingOffset = switch (slot) {
            case 2, 3 -> 14.0D;
            case 4 -> 18.0D;
            default -> 9.0D;
        };
        double heightOffset = switch (slot) {
            case 2, 3 -> 2.5D;
            case 4 -> 4.0D;
            default -> 1.5D;
        };

        return leader.position()
                .subtract(leaderLook.scale(trailingOffset))
                .add(lateral.scale(lateralOffset))
                .add(0.0D, leader.getBbHeight() + heightOffset, 0.0D);
    }

    private static List<Cindervane> findAirbornePackmates(ServerLevel level, Cindervane dragon) {
        AABB searchBox = dragon.getBoundingBox().inflate(NEIGHBOR_RADIUS);
        return level.getEntitiesOfClass(Cindervane.class, searchBox, other ->
                other != dragon
                        && other.isAlive()
                        && !other.isRemoved()
                        && other.canParticipateInPack()
                        && other.isAerial()
                        && dragon.distanceToSqr(other) <= NEIGHBOR_RADIUS * NEIGHBOR_RADIUS
                        && isPackCompatible(dragon, other));
    }

    private static BoidVectors calculateBoids(Cindervane dragon, List<Cindervane> neighbors) {
        Vec3 separation = Vec3.ZERO;
        Vec3 alignment = Vec3.ZERO;
        Vec3 cohesion = Vec3.ZERO;
        int alignmentCount = 0;

        for (Cindervane other : neighbors) {
            Vec3 offset = other.position().subtract(dragon.position());
            double distance = Math.max(0.001D, offset.length());
            if (distance < SEPARATION_RANGE) {
                separation = separation.subtract(offset.normalize().scale((SEPARATION_RANGE - distance) / SEPARATION_RANGE));
            }
            Vec3 velocity = other.getDeltaMovement();
            if (velocity.lengthSqr() > 1.0E-4D) {
                alignment = alignment.add(velocity.normalize());
                alignmentCount++;
            }
            cohesion = cohesion.add(other.position());
        }

        if (alignmentCount > 0) {
            alignment = alignment.scale(1.0D / alignmentCount);
        }
        cohesion = cohesion.scale(1.0D / neighbors.size()).subtract(dragon.position());
        return new BoidVectors(separation, alignment, cohesion);
    }

    private static boolean isPackCompatible(Cindervane dragon, Cindervane other) {
        if (dragon.isTame() != other.isTame()) {
            return false;
        }
        if (!dragon.isTame()) {
            return true;
        }
        LivingEntity owner = dragon.getOwner();
        return owner != null && other.isOwnedBy(owner);
    }

    private static Vec3 horizontalOrFallback(Vec3 vector) {
        Vec3 horizontal = new Vec3(vector.x, 0.0D, vector.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }
        return horizontal.normalize();
    }

    private static Vec3 normalizeOrZero(Vec3 vector) {
        return vector.lengthSqr() > 1.0E-4D ? vector.normalize() : Vec3.ZERO;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record BoidVectors(Vec3 separation, Vec3 alignment, Vec3 cohesion) {
    }
}
