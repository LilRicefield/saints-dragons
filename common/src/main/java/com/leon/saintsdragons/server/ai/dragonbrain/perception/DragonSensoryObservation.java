package com.leon.saintsdragons.server.ai.dragonbrain.perception;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record DragonSensoryObservation(
        Vec3 position,
        @Nullable UUID sourceUuid,
        Kind kind,
        float confidence,
        long observedAt
) {
    public DragonSensoryObservation {
        confidence = Math.max(0.0F, Math.min(1.0F, confidence));
    }

    public enum Kind {
        SIGHT,
        STEP,
        IMPACT,
        SPLASH,
        COMBAT,
        PROJECTILE,
        BLOCK,
        ROAR,
        EXPLOSION,
        TELEPORT,
        OTHER
    }
}
