package com.leon.saintsdragons.server.ai.dragonbrain.perception;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

public final class DragonInvestigation {
    private static final long STRONGER_OBSERVATION_GRACE_TICKS = 20L;
    private static final double MIN_DIRECTION_LENGTH_SQR = 1.0E-4D;
    private static final double MIN_PROJECTILE_SEARCH_DISTANCE = 12.0D;
    private static final double OWNERLESS_SEARCH_RANGE_FACTOR = 0.75D;
    private static final double MIN_OWNER_TRAJECTORY_ALIGNMENT = 0.25D;
    private static final long SAME_SOURCE_PROJECTILE_COALESCE_TICKS = 20L * 3L;
    private static final long OWNERLESS_PROJECTILE_COALESCE_TICKS = 10L;
    private static final double OWNERLESS_PROJECTILE_COALESCE_DISTANCE_SQR = 4.0D * 4.0D;

    private DragonInvestigation() {
    }

    public static boolean remember(DragonEntity dragon, DragonSensoryObservation observation) {
        DragonSensoryObservation existing = dragon.getBrain()
                .getMemory(DragonMemories.INVESTIGATION_TARGET)
                .orElse(null);
        DragonPerceptionProfile profile = DragonPerceptionProfile.forDragon(dragon);
        if (shouldCoalesce(existing, observation)) {
            dragon.getBrain().setMemoryWithExpiry(
                    DragonMemories.INVESTIGATION_TARGET,
                    existing,
                    profile.investigationMemoryTicks(dragon, observation.position())
            );
            return true;
        }
        if (!shouldReplace(existing, observation)) {
            return false;
        }

        dragon.getBrain().setMemoryWithExpiry(
                DragonMemories.INVESTIGATION_TARGET,
                observation,
                profile.investigationMemoryTicks(dragon, observation.position())
        );
        return true;
    }

    private static boolean shouldCoalesce(DragonSensoryObservation existing,
                                          DragonSensoryObservation candidate) {
        if (existing == null
                || existing.kind() != DragonSensoryObservation.Kind.PROJECTILE
                || candidate.kind() != DragonSensoryObservation.Kind.PROJECTILE
                || candidate.observedAt() < existing.observedAt()) {
            return false;
        }
        long age = candidate.observedAt() - existing.observedAt();
        if (existing.sourceUuid() != null && existing.sourceUuid().equals(candidate.sourceUuid())) {
            return age <= SAME_SOURCE_PROJECTILE_COALESCE_TICKS;
        }
        return existing.sourceUuid() == null
                && candidate.sourceUuid() == null
                && age <= OWNERLESS_PROJECTILE_COALESCE_TICKS
                && existing.position().distanceToSqr(candidate.position())
                <= OWNERLESS_PROJECTILE_COALESCE_DISTANCE_SQR;
    }

    public static boolean rememberProjectileOrigin(DragonEntity dragon, Projectile projectile) {
        if (dragon.level().isClientSide
                || !dragon.getBrain().checkMemory(
                        DragonMemories.INVESTIGATION_TARGET,
                        MemoryStatus.REGISTERED
                )) {
            return false;
        }

        Vec3 impactPosition = projectile.position();
        Entity owner = projectile.getOwner();
        if (owner == dragon) {
            return false;
        }
        boolean hasUsableOwner = owner != null
                && owner.isAlive()
                && owner.level() == dragon.level();
        Vec3 ownerDirection = hasUsableOwner
                ? owner.getBoundingBox().getCenter().subtract(impactPosition)
                : Vec3.ZERO;
        Vec3 trajectoryDirection = projectile.getDeltaMovement().scale(-1.0D);
        boolean hasOwnerDirection = ownerDirection.lengthSqr() >= MIN_DIRECTION_LENGTH_SQR;
        boolean hasTrajectoryDirection = trajectoryDirection.lengthSqr() >= MIN_DIRECTION_LENGTH_SQR;
        Vec3 sourceDirection = ownerDirection;
        if (hasTrajectoryDirection && (!hasOwnerDirection
                || trajectoryDirection.normalize().dot(ownerDirection.normalize())
                        >= MIN_OWNER_TRAJECTORY_ALIGNMENT)) {
            sourceDirection = trajectoryDirection;
        }
        if (sourceDirection.lengthSqr() < MIN_DIRECTION_LENGTH_SQR) {
            return false;
        }

        DragonPerceptionProfile profile = DragonPerceptionProfile.forDragon(dragon);
        double maxSearchDistance = Math.max(
                MIN_PROJECTILE_SEARCH_DISTANCE,
                profile.hearingRange()
        );
        double searchDistance = hasOwnerDirection
                ? Math.min(ownerDirection.length(), maxSearchDistance)
                : maxSearchDistance * OWNERLESS_SEARCH_RANGE_FACTOR;
        Vec3 inferredOrigin = impactPosition.add(sourceDirection.normalize().scale(searchDistance));
        DragonSensoryObservation observation = new DragonSensoryObservation(
                inferredOrigin,
                hasUsableOwner ? owner.getUUID() : null,
                DragonSensoryObservation.Kind.PROJECTILE,
                hasUsableOwner ? 0.95F : 0.75F,
                dragon.level().getGameTime()
        );
        return remember(dragon, observation);
    }

    public static boolean isMeaningfulSound(DragonSensoryObservation observation) {
        if (observation.confidence() < 0.15F) {
            return false;
        }
        return switch (observation.kind()) {
            case EXPLOSION, ROAR, COMBAT, PROJECTILE, BLOCK, TELEPORT -> true;
            case SIGHT, SCENT, SPLASH, STEP, IMPACT, OTHER -> false;
        };
    }

    private static boolean shouldReplace(DragonSensoryObservation existing,
                                         DragonSensoryObservation candidate) {
        if (existing == null) {
            return true;
        }
        if (existing.observedAt() > candidate.observedAt()) {
            return false;
        }

        int existingPriority = priority(existing.kind());
        int candidatePriority = priority(candidate.kind());
        if (existing.observedAt() == candidate.observedAt()) {
            return candidatePriority > existingPriority
                    || (candidatePriority == existingPriority
                    && candidate.confidence() > existing.confidence());
        }
        long age = candidate.observedAt() - existing.observedAt();
        if (existingPriority > candidatePriority && age < STRONGER_OBSERVATION_GRACE_TICKS) {
            return false;
        }
        return candidatePriority > existingPriority
                || candidate.confidence() >= existing.confidence()
                || age >= STRONGER_OBSERVATION_GRACE_TICKS;
    }

    private static int priority(DragonSensoryObservation.Kind kind) {
        return switch (kind) {
            case SIGHT -> 7;
            case EXPLOSION -> 6;
            case COMBAT, PROJECTILE -> 5;
            case ROAR, TELEPORT -> 4;
            case BLOCK -> 3;
            case SPLASH, IMPACT -> 2;
            case SCENT, STEP -> 1;
            case OTHER -> 0;
        };
    }
}
