package com.leon.saintsdragons.server.ai.dragonbrain.perception;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemoryMap;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import org.jetbrains.annotations.Nullable;

/**
 * The small adapter between perception evidence and the dragon Brain's
 * retained target track. Keeping these operations here prevents individual
 * sensors and behaviours from inventing their own forgetting rules.
 */
public final class DragonTargetMemory {
    private DragonTargetMemory() {
    }

    public static @Nullable DragonTargetTrack get(Brain<?> brain) {
        return brain.getMemory(DragonMemories.TARGET_TRACK).orElse(null);
    }

    public static void observeVisible(Brain<?> brain, LivingEntity target, long gameTime) {
        DragonTargetTrack track = getForSource(brain, target.getUUID(), target.position(),
                DragonSensoryObservation.Kind.SIGHT, 1.0F, gameTime);
        track.markVisible(target.getBoundingBox().getCenter(), gameTime);
        brain.setMemory(DragonMemories.TARGET_TRACK, track);
    }

    public static void observe(Brain<?> brain,
                               DragonSensoryObservation observation,
                               boolean hostile,
                               long gameTime) {
        if (observation.sourceUuid() == null) {
            return;
        }
        DragonTargetTrack track = getForSource(
                brain,
                observation.sourceUuid(),
                observation.position(),
                observation.kind(),
                observation.confidence(),
                observation.observedAt()
        );
        track.observe(observation, gameTime, hostile);
        brain.setMemory(DragonMemories.TARGET_TRACK, track);
    }

    public static void markLost(Brain<?> brain, long gameTime) {
        DragonTargetTrack track = get(brain);
        if (track != null) {
            track.markLost(gameTime);
        }
    }

    public static void beginSearch(Brain<?> brain, long gameTime) {
        DragonTargetTrack track = get(brain);
        if (track != null) {
            track.beginSearch(gameTime);
        }
    }

    public static boolean hasActive(Brain<?> brain, long gameTime) {
        DragonTargetTrack track = get(brain);
        if (track == null) {
            return false;
        }
        track.expireIfNeeded(gameTime);
        return track.isActive(gameTime);
    }

    public static boolean hasSearchable(Brain<?> brain, long gameTime) {
        return hasSearchable(get(brain), gameTime);
    }

    public static boolean hasSearchable(@Nullable DragonTargetTrack track, long gameTime) {
        if (track == null) {
            return false;
        }
        track.expireIfNeeded(gameTime);
        return track.isSearchable(gameTime);
    }

    /**
     * Resolves a remembered identity only when the entity is visible again.
     * This is the reacquisition edge; a hidden target remains evidence, not a
     * free source of omniscient targeting.
     */
    public static @Nullable LivingEntity findVisibleReacquisition(DragonEntity dragon,
                                                                   Brain<?> brain,
                                                                   long gameTime) {
        DragonTargetTrack track = get(brain);
        if (track == null) {
            return null;
        }
        track.expireIfNeeded(gameTime);
        if (!track.isActive(gameTime) || !(dragon.level() instanceof ServerLevel level)) {
            return null;
        }
        LivingEntity target = track.resolve(level);
        if (target == null || !dragon.isTargetValid(target) || !dragon.canTarget(target)
                || !dragon.getVision().recognizes(target)) {
            return null;
        }
        track.markVisible(target.getBoundingBox().getCenter(), gameTime);
        return target;
    }

    /**
     * Compatibility memory may remain bound to a hidden entity while its
     * retained track is still alive. This keeps existing activity profiles
     * stable until the explicit track expires.
     */
    public static boolean shouldPreserveCompatibilityTarget(Brain<?> brain,
                                                             @Nullable LivingEntity target,
                                                             long gameTime) {
        DragonTargetTrack track = get(brain);
        if (track == null || target == null || !target.isAlive()
                || !track.sourceUuid().equals(target.getUUID())) {
            return false;
        }
        track.expireIfNeeded(gameTime);
        return track.isSearchable(gameTime);
    }

    public static boolean shouldPreserveCompatibilityTarget(DragonMemoryMap memories,
                                                             @Nullable LivingEntity target,
                                                             long gameTime) {
        DragonTargetTrack track = memories.get(DragonMemories.TARGET_TRACK).orElse(null);
        if (track == null || target == null || !target.isAlive()
                || !track.sourceUuid().equals(target.getUUID())) {
            return false;
        }
        track.expireIfNeeded(gameTime);
        return track.isSearchable(gameTime);
    }

    public static void disengage(Brain<?> brain) {
        DragonTargetTrack track = get(brain);
        if (track != null) {
            track.markDisengaged();
        }
    }

    public static void forget(Brain<?> brain) {
        brain.eraseMemory(DragonMemories.TARGET_TRACK);
    }

    private static DragonTargetTrack getForSource(Brain<?> brain,
                                                  java.util.UUID sourceUuid,
                                                  net.minecraft.world.phys.Vec3 position,
                                                  DragonSensoryObservation.Kind evidence,
                                                  float confidence,
                                                  long observedAt) {
        DragonTargetTrack current = get(brain);
        if (current != null && current.sourceUuid().equals(sourceUuid)) {
            return current;
        }
        return new DragonTargetTrack(sourceUuid, position, evidence, confidence, observedAt);
    }
}
