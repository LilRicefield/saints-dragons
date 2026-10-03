package com.leon.saintsdragons.server.ai.dragonbrain.perception;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The dragon's working belief about one hostile entity.
 *
 * <p>This is deliberately separate from Brain's {@code ATTACK_TARGET}. The
 * latter is an action binding used by combat behaviours; this track is the
 * knowledge that survives a short loss of sight.</p>
 */
public final class DragonTargetTrack {
    private static final long DEFAULT_LOST_CONTACT_TICKS = 20L * 12L;
    private static final long DEFAULT_SEARCH_TICKS = 20L * 12L;

    private final UUID sourceUuid;
    private Vec3 lastKnownPosition;
    private Vec3 lastKnownDirection = Vec3.ZERO;
    private DragonSensoryObservation.Kind strongestEvidence;
    private long lastEvidenceAt = Long.MIN_VALUE;
    private long lastVisibleAt = Long.MIN_VALUE;
    private long lostContactAt = Long.MIN_VALUE;
    private long expiresAt = Long.MIN_VALUE;
    private long searchExpiresAt = Long.MIN_VALUE;
    private float confidence;
    private float threat;
    private boolean visible;
    private boolean personallyWitnessed;
    private Phase phase = Phase.NONE;

    public DragonTargetTrack(UUID sourceUuid,
                             Vec3 position,
                             DragonSensoryObservation.Kind evidence,
                             float confidence,
                             long observedAt) {
        this.sourceUuid = sourceUuid;
        this.lastKnownPosition = position;
        this.strongestEvidence = evidence;
        this.confidence = clamp(confidence);
        this.threat = 1.0F;
        this.lastEvidenceAt = observedAt;
        // A historical observation is evidence, not proof that the source is
        // visible in this tick. Live sight is promoted explicitly by
        // DragonTargetMemory.observeVisible.
        this.visible = false;
        this.lastVisibleAt = Long.MIN_VALUE;
        this.expiresAt = observedAt + DEFAULT_LOST_CONTACT_TICKS;
        this.searchExpiresAt = observedAt + DEFAULT_SEARCH_TICKS;
        this.phase = Phase.LOST_PURSUIT;
        this.personallyWitnessed = evidence == DragonSensoryObservation.Kind.SIGHT;
    }

    public UUID sourceUuid() {
        return sourceUuid;
    }

    public Vec3 lastKnownPosition() {
        return lastKnownPosition;
    }

    public Vec3 lastKnownDirection() {
        return lastKnownDirection;
    }

    public DragonSensoryObservation.Kind strongestEvidence() {
        return strongestEvidence;
    }

    public long lastEvidenceAt() {
        return lastEvidenceAt;
    }

    public long lastVisibleAt() {
        return lastVisibleAt;
    }

    public long lostContactAt() {
        return lostContactAt;
    }

    public long expiresAt() {
        return expiresAt;
    }

    public long searchExpiresAt() {
        return searchExpiresAt;
    }

    public float confidence() {
        return confidence;
    }

    public float threat() {
        return threat;
    }

    public boolean visible() {
        return visible;
    }

    public boolean personallyWitnessed() {
        return personallyWitnessed;
    }

    public Phase phase() {
        return phase;
    }

    public boolean isActive(long gameTime) {
        return phase != Phase.NONE && gameTime < expiresAt;
    }

    public boolean isSearchable(long gameTime) {
        return isActive(gameTime)
                && (phase == Phase.LOST_PURSUIT || phase == Phase.AGGRESSIVE_SEARCH)
                && gameTime < searchExpiresAt;
    }

    public void observe(DragonSensoryObservation observation, long gameTime, boolean hostile) {
        if (!sourceUuid.equals(observation.sourceUuid()) || observation.observedAt() < lastEvidenceAt) {
            return;
        }

        lastKnownPosition = observation.position();
        strongestEvidence = strongerEvidence(strongestEvidence, observation.kind());
        lastEvidenceAt = observation.observedAt();
        confidence = Math.max(confidence * 0.65F, observation.confidence());
        threat = hostile ? 1.0F : Math.max(threat, observation.confidence() * 0.75F);
        personallyWitnessed |= observation.kind() == DragonSensoryObservation.Kind.SIGHT;
        expiresAt = Math.max(expiresAt, gameTime + DEFAULT_LOST_CONTACT_TICKS);

        if (visible) {
            lastVisibleAt = gameTime;
            lostContactAt = Long.MIN_VALUE;
            searchExpiresAt = Long.MIN_VALUE;
            phase = Phase.VISIBLE_COMBAT;
        } else if (phase == Phase.NONE || phase == Phase.DISENGAGED) {
            beginLostPursuit(gameTime);
        }
    }

    public void markVisible(Vec3 position, long gameTime) {
        lastKnownPosition = position;
        lastVisibleAt = gameTime;
        lastEvidenceAt = gameTime;
        confidence = 1.0F;
        threat = 1.0F;
        visible = true;
        lostContactAt = Long.MIN_VALUE;
        searchExpiresAt = Long.MIN_VALUE;
        expiresAt = gameTime + DEFAULT_LOST_CONTACT_TICKS;
        strongestEvidence = DragonSensoryObservation.Kind.SIGHT;
        personallyWitnessed = true;
        phase = Phase.VISIBLE_COMBAT;
    }

    public void markLost(long gameTime) {
        if (!visible && phase != Phase.VISIBLE_COMBAT) {
            return;
        }
        visible = false;
        lostContactAt = gameTime;
        searchExpiresAt = gameTime + DEFAULT_SEARCH_TICKS;
        expiresAt = Math.max(expiresAt, searchExpiresAt);
        phase = Phase.LOST_PURSUIT;
    }

    public void beginSearch(long gameTime) {
        visible = false;
        if (lostContactAt == Long.MIN_VALUE) {
            lostContactAt = gameTime;
        }
        searchExpiresAt = Math.max(searchExpiresAt, gameTime + DEFAULT_SEARCH_TICKS);
        expiresAt = Math.max(expiresAt, searchExpiresAt);
        phase = Phase.AGGRESSIVE_SEARCH;
    }

    public void markDisengaged() {
        visible = false;
        phase = Phase.DISENGAGED;
    }

    public void expireIfNeeded(long gameTime) {
        if (isActive(gameTime) && gameTime >= searchExpiresAt && phase == Phase.AGGRESSIVE_SEARCH) {
            phase = Phase.DISENGAGED;
        }
        if (gameTime >= expiresAt) {
            visible = false;
            phase = Phase.NONE;
        }
    }

    @Nullable
    public LivingEntity resolve(ServerLevel level) {
        Entity entity = level.getEntity(sourceUuid);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    public void updateDirection(Vec3 direction) {
        if (direction.lengthSqr() > 1.0E-4D) {
            lastKnownDirection = direction.normalize();
        }
    }

    private void beginLostPursuit(long gameTime) {
        visible = false;
        lostContactAt = gameTime;
        searchExpiresAt = gameTime + DEFAULT_SEARCH_TICKS;
        expiresAt = Math.max(expiresAt, searchExpiresAt);
        phase = Phase.LOST_PURSUIT;
    }

    private static DragonSensoryObservation.Kind strongerEvidence(
            DragonSensoryObservation.Kind current,
            DragonSensoryObservation.Kind candidate) {
        if (candidate == DragonSensoryObservation.Kind.SIGHT) {
            return candidate;
        }
        if (current == DragonSensoryObservation.Kind.SIGHT) {
            return current;
        }
        return candidate;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    public enum Phase {
        NONE,
        VISIBLE_COMBAT,
        LOST_PURSUIT,
        AGGRESSIVE_SEARCH,
        DISENGAGED
    }
}
