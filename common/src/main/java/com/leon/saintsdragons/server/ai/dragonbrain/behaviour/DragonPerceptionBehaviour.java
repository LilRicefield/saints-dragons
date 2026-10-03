package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonTargetLifecycle;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonInvestigation;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonAwarenessMemory;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonSensoryObservation;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonTargetMemory;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonTargetTrack;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class DragonPerceptionBehaviour<T extends DragonEntity> extends DragonBehaviour<T> {
    private boolean targetVisible;
    private String lastObservation = "none";
    private int familiarSources;
    private String trackPhase = "none";
    private String trackPosition = "none";
    private long trackAge;
    private float trackConfidence;

    public DragonPerceptionBehaviour() {
        super(false);
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return true;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return true;
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        DragonTargetTrack track = context.memories().get(DragonMemories.TARGET_TRACK).orElse(null);
        if (track == null) {
            trackPhase = "none";
            trackPosition = "none";
            trackAge = 0L;
            trackConfidence = 0.0F;
        } else {
            track.expireIfNeeded(context.gameTime());
            trackPhase = track.phase().name().toLowerCase(Locale.ROOT);
            trackPosition = track.lastKnownPosition().toString();
            trackAge = Math.max(0L, context.gameTime() - track.lastEvidenceAt());
            trackConfidence = track.confidence();
        }
        DragonAwarenessMemory awareness = DragonAwarenessMemory.get(dragon);
        familiarSources = awareness.familiarSourceCount();
        if (target == null) {
            targetVisible = false;
            if (track != null && DragonTargetMemory.hasSearchable(dragon.getBrain(), context.gameTime())) {
                DragonInvestigation.remember(dragon, new DragonSensoryObservation(
                        track.lastKnownPosition(),
                        track.sourceUuid(),
                        track.strongestEvidence(),
                        track.confidence(),
                        track.lastEvidenceAt()
                ));
                DragonTargetMemory.beginSearch(dragon.getBrain(), context.gameTime());
                lastObservation = "track_" + track.phase().name().toLowerCase(Locale.ROOT);
            } else {
                lastObservation = "none";
            }
            lookTowardAttention(context);
            return;
        }

        awareness.rememberThreat(target.getUUID(), context.gameTime());

        targetVisible = context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false);

        if (targetVisible) {
            context.memories().erase(DragonMemories.INVESTIGATION_TARGET);
            lastObservation = "sight";
            return;
        }

        DragonSensoryObservation remembered = context.memories()
                .get(DragonMemories.LAST_SEEN_TARGET)
                .orElse(null);
        DragonSensoryObservation heard = context.memories()
                .get(DragonMemories.HEARD_TARGET)
                .filter(observation -> observation.sourceUuid() != null
                        && observation.sourceUuid().equals(target.getUUID()))
                .orElse(null);
        if (heard != null && (remembered == null || heard.observedAt() > remembered.observedAt())) {
            remembered = heard;
            // Hearing can guide investigation without replacing the last actual sighting.
            lastObservation = "heard_target_"
                    + heard.kind().name().toLowerCase(Locale.ROOT);
        }
        if (remembered == null && track != null
                && DragonTargetMemory.hasSearchable(dragon.getBrain(), context.gameTime())
                && target.getUUID().equals(track.sourceUuid())) {
            remembered = new DragonSensoryObservation(
                    track.lastKnownPosition(),
                    track.sourceUuid(),
                    track.strongestEvidence(),
                    track.confidence(),
                    track.lastEvidenceAt()
            );
            DragonTargetMemory.beginSearch(dragon.getBrain(), context.gameTime());
            lastObservation = "track_" + track.phase().name().toLowerCase(Locale.ROOT);
        }
        boolean hasFreshEvidence = remembered != null
                && target.getUUID().equals(remembered.sourceUuid());
        if (hasFreshEvidence) {
            DragonInvestigation.remember(dragon, remembered);
        }

        DragonSensoryObservation investigation = context.memories()
                .get(DragonMemories.INVESTIGATION_TARGET)
                .filter(observation -> target.getUUID().equals(observation.sourceUuid()))
                .orElse(null);
        if (!hasFreshEvidence && investigation == null) {
            if (!DragonTargetMemory.hasSearchable(dragon.getBrain(), context.gameTime())) {
                if (dragon.getTarget() == null || dragon.getTarget() == target) {
                    DragonTargetLifecycle.clearCombatTarget(context.memories(), dragon, false);
                } else {
                    DragonTargetLifecycle.clearTargetMemories(context.memories());
                }
                lastObservation = "forgotten";
                return;
            }
        }

        DragonSensoryObservation focus = hasFreshEvidence ? remembered : investigation;
        if (focus == null) {
            return;
        }
        if (!hasFreshEvidence) {
            lastObservation = "investigating_"
                    + focus.kind().name().toLowerCase(Locale.ROOT);
        } else if (!lastObservation.startsWith("heard_target_")) {
            lastObservation = "last_seen";
        }
        Vec3 position = focus.position();
        dragon.getLookControl().setLookAt(
                position.x,
                position.y,
                position.z,
                10.0F,
                dragon.getMaxHeadXRot()
        );
    }

    private void lookTowardAttention(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (dragon.isVehicle() || dragon.isOrderedToSit() || dragon.isSleepLocked()) {
            return;
        }
        DragonSensoryObservation heard = DragonAwarenessMemory.get(dragon)
                .attention(context.gameTime());
        if (heard == null) {
            return;
        }
        Vec3 position = heard.position();
        dragon.getLookControl().setLookAt(
                position.x,
                position.y,
                position.z,
                8.0F,
                dragon.getMaxHeadXRot()
        );
        lastObservation = "heard_" + heard.kind().name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("target_visible", Boolean.toString(targetVisible));
        details.put("observation", lastObservation);
        details.put("familiar_sources", Integer.toString(familiarSources));
        details.put("track_phase", trackPhase);
        details.put("track_position", trackPosition);
        details.put("track_age", Long.toString(trackAge));
        details.put("track_confidence", Float.toString(trackConfidence));
        return Map.copyOf(details);
    }
}
