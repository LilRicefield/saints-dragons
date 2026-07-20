package com.leon.saintsdragons.server.ai.dragonbrain.perception;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.entity.base.DragonEntity;

public final class DragonInvestigation {
    private static final long STRONGER_OBSERVATION_GRACE_TICKS = 20L;

    private DragonInvestigation() {
    }

    public static boolean remember(DragonEntity dragon, DragonSensoryObservation observation) {
        DragonSensoryObservation existing = dragon.getBrain()
                .getMemory(DragonMemories.INVESTIGATION_TARGET)
                .orElse(null);
        if (!shouldReplace(existing, observation)) {
            return false;
        }

        DragonPerceptionProfile profile = DragonPerceptionProfile.forDragon(dragon);
        dragon.getBrain().setMemoryWithExpiry(
                DragonMemories.INVESTIGATION_TARGET,
                observation,
                profile.investigationMemoryTicks(dragon, observation.position())
        );
        return true;
    }

    public static boolean isMeaningfulSound(DragonSensoryObservation observation) {
        if (observation.confidence() < 0.15F) {
            return false;
        }
        return switch (observation.kind()) {
            case EXPLOSION, ROAR, COMBAT, PROJECTILE, BLOCK, TELEPORT -> true;
            case SIGHT, SPLASH, STEP, OTHER -> false;
        };
    }

    private static boolean shouldReplace(DragonSensoryObservation existing,
                                         DragonSensoryObservation candidate) {
        if (existing == null) {
            return true;
        }
        if (existing.observedAt() >= candidate.observedAt()) {
            return false;
        }

        int existingPriority = priority(existing.kind());
        int candidatePriority = priority(candidate.kind());
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
            case SPLASH -> 2;
            case STEP -> 1;
            case OTHER -> 0;
        };
    }
}
