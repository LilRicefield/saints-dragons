package com.leon.saintsdragons.server.ai.dragonbrain.tactical;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record DragonTacticalCommitment(
        DragonTactic tactic,
        int score,
        DragonTactic candidate,
        int candidateScore,
        long startedAt,
        long minimumEndsAt,
        long expiresAt,
        @Nullable UUID targetUuid,
        @Nullable Vec3 focus,
        String reason,
        Map<DragonTactic, Integer> scores
) {
    public DragonTacticalCommitment {
        Objects.requireNonNull(tactic, "tactic");
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(reason, "reason");
        scores = Collections.unmodifiableMap(new EnumMap<>(scores));
    }

    public String summary() {
        return tactic
                + "(score=" + score
                + ",candidate=" + candidate + ":" + candidateScore
                + ",reason=" + reason
                + ",lockUntil=" + minimumEndsAt
                + ",expires=" + expiresAt
                + ",target=" + shortTarget()
                + ",focus=" + (focus == null ? "none" : BlockPos.containing(focus))
                + ",scores=" + scoresSummary()
                + ")";
    }

    public String scoresSummary() {
        StringBuilder result = new StringBuilder();
        for (DragonTactic value : DragonTactic.values()) {
            Integer valueScore = scores.get(value);
            if (valueScore == null) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append('/');
            }
            result.append(value.name()).append('=').append(valueScore);
        }
        return result.toString();
    }

    private String shortTarget() {
        if (targetUuid == null) {
            return "none";
        }
        String value = targetUuid.toString();
        return value.substring(0, Math.min(8, value.length()));
    }
}
