package com.leon.saintsdragons.server.ai.dragonbrain.perception;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class DragonAwarenessMemory {
    private static final int MAX_FAMILIAR_SOURCES = 24;
    private static final long FAMILIARITY_FORGET_TICKS = 20L * 60L * 2L;
    private static final long PASSIVE_QUIET_RESET_TICKS = 20L * 30L;
    private static final long PRUNE_INTERVAL_TICKS = 20L * 10L;
    private static final float ATTENTION_SWITCH_MARGIN = 0.12F;

    private final Map<UUID, Familiarity> familiarity =
            new LinkedHashMap<>(MAX_FAMILIAR_SOURCES, 0.75F, true);
    private final Map<DragonSensoryObservation.Kind, Long> anonymousCooldowns =
            new EnumMap<>(DragonSensoryObservation.Kind.class);

    @Nullable
    private Attention attention;
    private long lastPrunedAt = Long.MIN_VALUE;

    public static DragonAwarenessMemory get(DragonEntity dragon) {
        DragonAwarenessMemory memory = dragon.getBrain()
                .getMemory(DragonMemories.AWARENESS)
                .orElse(null);
        if (memory == null) {
            memory = new DragonAwarenessMemory();
            dragon.getBrain().setMemory(DragonMemories.AWARENESS, memory);
        }
        return memory;
    }

    public void rememberSound(DragonSensoryObservation observation,
                              boolean threatening,
                              long gameTime) {
        rememberObservation(observation, threatening, gameTime);
    }

    public boolean rememberScent(DragonSensoryObservation observation, long gameTime) {
        UUID sourceUuid = observation.sourceUuid();
        Familiarity record = sourceUuid == null ? null : familiarity.get(sourceUuid);
        if (record != null && gameTime < record.nextPassiveAttentionAt) {
            return false;
        }
        return rememberObservation(observation, false, gameTime);
    }

    private boolean rememberObservation(DragonSensoryObservation observation,
                                        boolean threatening,
                                        long gameTime) {
        prune(gameTime);
        boolean passive = isPassive(observation.kind());
        UUID sourceUuid = observation.sourceUuid();

        if (sourceUuid != null) {
            Familiarity record = familiarity.computeIfAbsent(sourceUuid, ignored -> new Familiarity());
            trimOldestSources();
            long quietTicks = record.lastObservedAt == Long.MIN_VALUE
                    ? Long.MAX_VALUE
                    : gameTime - record.lastObservedAt;
            if (quietTicks >= PASSIVE_QUIET_RESET_TICKS) {
                record.harmlessObservations = Math.max(0, record.harmlessObservations - 3);
            }
            record.lastObservedAt = gameTime;

            if (threatening) {
                record.harmlessObservations = 0;
                record.nextPassiveAttentionAt = gameTime;
            } else if (passive) {
                record.harmlessObservations = Math.min(12, record.harmlessObservations + 1);
                if (gameTime < record.nextPassiveAttentionAt) {
                    return false;
                }
                record.nextPassiveAttentionAt = gameTime + passiveCooldown(record.harmlessObservations);
            }
        } else if (passive) {
            long nextAttentionAt = anonymousCooldowns.getOrDefault(observation.kind(), Long.MIN_VALUE);
            if (gameTime < nextAttentionAt) {
                return false;
            }
            anonymousCooldowns.put(observation.kind(), gameTime + 80L);
        }

        if (observation.confidence() < attentionThreshold(observation.kind())) {
            return false;
        }
        proposeAttention(observation, threatening, gameTime);
        return true;
    }

    public void rememberThreat(UUID sourceUuid, long gameTime) {
        Familiarity record = familiarity.computeIfAbsent(sourceUuid, ignored -> new Familiarity());
        record.harmlessObservations = 0;
        record.nextPassiveAttentionAt = gameTime;
        record.lastObservedAt = gameTime;
        trimOldestSources();
    }

    @Nullable
    public DragonSensoryObservation attention(long gameTime) {
        if (attention == null) {
            return null;
        }
        if (gameTime >= attention.endsAt) {
            attention = null;
            return null;
        }
        return attention.observation;
    }

    public boolean hasAttention(long gameTime) {
        return attention(gameTime) != null;
    }

    public float passiveDisturbanceMultiplier(DragonSensoryObservation observation, long gameTime) {
        if (!isPassive(observation.kind()) || observation.sourceUuid() == null) {
            return 1.0F;
        }
        Familiarity record = familiarity.get(observation.sourceUuid());
        if (record == null || gameTime - record.lastObservedAt >= PASSIVE_QUIET_RESET_TICKS) {
            return 1.0F;
        }
        return switch (record.harmlessObservations) {
            case 0, 1 -> 1.0F;
            case 2 -> 0.65F;
            case 3 -> 0.35F;
            default -> 0.12F;
        };
    }

    public int familiarSourceCount() {
        return (int)familiarity.values().stream()
                .filter(record -> record.harmlessObservations > 0)
                .count();
    }

    private void proposeAttention(DragonSensoryObservation observation,
                                  boolean threatening,
                                  long gameTime) {
        int duration = attentionDuration(observation.kind());
        if (duration <= 0) {
            return;
        }

        float score = attentionPriority(observation.kind())
                + observation.confidence() * 0.35F
                + (threatening ? 0.25F : 0.0F);
        if (attention != null && gameTime < attention.endsAt) {
            boolean sameSource = observation.sourceUuid() != null
                    && observation.sourceUuid().equals(attention.observation.sourceUuid());
            if (!sameSource && score < attention.score + ATTENTION_SWITCH_MARGIN) {
                return;
            }
            if (sameSource && isPassive(observation.kind())) {
                return;
            }
        }
        attention = new Attention(observation, gameTime + duration, score);
    }

    private void prune(long gameTime) {
        if (lastPrunedAt != Long.MIN_VALUE && gameTime - lastPrunedAt < PRUNE_INTERVAL_TICKS) {
            return;
        }
        lastPrunedAt = gameTime;
        Iterator<Familiarity> iterator = familiarity.values().iterator();
        while (iterator.hasNext()) {
            Familiarity record = iterator.next();
            if (gameTime - record.lastObservedAt >= FAMILIARITY_FORGET_TICKS) {
                iterator.remove();
            }
        }
        trimOldestSources();
    }

    private void trimOldestSources() {
        while (familiarity.size() > MAX_FAMILIAR_SOURCES) {
            Iterator<UUID> eldest = familiarity.keySet().iterator();
            if (!eldest.hasNext()) {
                break;
            }
            eldest.next();
            eldest.remove();
        }
    }

    private static boolean isPassive(DragonSensoryObservation.Kind kind) {
        return kind == DragonSensoryObservation.Kind.STEP
                || kind == DragonSensoryObservation.Kind.SCENT
                || kind == DragonSensoryObservation.Kind.SPLASH
                || kind == DragonSensoryObservation.Kind.IMPACT;
    }

    private static int passiveCooldown(int harmlessObservations) {
        if (harmlessObservations <= 1) {
            return 20 * 5;
        }
        if (harmlessObservations == 2) {
            return 20 * 10;
        }
        return 20 * 30;
    }

    private static int attentionDuration(DragonSensoryObservation.Kind kind) {
        return switch (kind) {
            case SCENT -> 18;
            case STEP -> 10;
            case SPLASH -> 14;
            case IMPACT -> 16;
            case BLOCK, SIGHT -> 20;
            case TELEPORT -> 24;
            case PROJECTILE -> 28;
            case COMBAT -> 32;
            case ROAR -> 34;
            case EXPLOSION -> 40;
            case OTHER -> 0;
        };
    }

    private static float attentionThreshold(DragonSensoryObservation.Kind kind) {
        return switch (kind) {
            case SCENT -> 0.20F;
            case STEP -> 0.18F;
            case SPLASH -> 0.15F;
            case IMPACT -> 0.12F;
            default -> 0.08F;
        };
    }

    private static float attentionPriority(DragonSensoryObservation.Kind kind) {
        return switch (kind) {
            case SCENT -> 0.25F;
            case STEP -> 0.20F;
            case SPLASH -> 0.30F;
            case IMPACT -> 0.40F;
            case BLOCK -> 0.50F;
            case SIGHT -> 0.55F;
            case TELEPORT -> 0.65F;
            case ROAR -> 0.75F;
            case COMBAT -> 0.80F;
            case PROJECTILE -> 0.85F;
            case EXPLOSION -> 1.00F;
            case OTHER -> 0.0F;
        };
    }

    private static final class Familiarity {
        private long lastObservedAt = Long.MIN_VALUE;
        private long nextPassiveAttentionAt = Long.MIN_VALUE;
        private int harmlessObservations;
    }

    private record Attention(DragonSensoryObservation observation, long endsAt, float score) {
    }
}
