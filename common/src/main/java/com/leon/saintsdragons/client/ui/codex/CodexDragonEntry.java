package com.leon.saintsdragons.client.ui.codex;

import java.util.UUID;
import com.leon.saintsdragons.common.codex.DragonCodexRegistry;


public record CodexDragonEntry(UUID entityId, String displayName, double currentHealth, double maxHealth,
                               double armor, double hunger, double happiness, int variantId, String variantResourceId, byte genderId,
                               boolean genderKnown, String dragonType, boolean isBaby,
                               boolean brushingAvailable, int brushingProgressPercent,
                               double posX, double posY, double posZ, String biomeId) {
    public boolean supportsBrushing() {
        return care().brushing();
    }

    public DragonCodexRegistry.Care care() {
        var definition = DragonCodexRegistry.get(dragonType);
        return definition == null ? DragonCodexRegistry.Care.NONE : definition.care();
    }
}
