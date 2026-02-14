package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;

/**
 * Shared baby/offspring utilities for dragon entities.
 * Keeps ownership and Codex registration behavior consistent across species.
 */
public final class DragonBabyComponent {
    private final DragonEntity dragon;

    public DragonBabyComponent(DragonEntity dragon) {
        this.dragon = dragon;
    }

    @Nullable
    public UUID resolveEggOwnerUUID(@Nullable DragonEntity partner) {
        if (dragon.isTame() && dragon.getOwnerUUID() != null) {
            return dragon.getOwnerUUID();
        }
        if (partner != null && partner.isTame() && partner.getOwnerUUID() != null) {
            return partner.getOwnerUUID();
        }
        return null;
    }

    public void registerToOwnerCodex(@Nullable DragonEntity offspring, @Nullable ServerLevel level) {
        if (offspring == null || level == null || level.isClientSide) {
            return;
        }
        if (offspring.isTame() && offspring.getOwnerUUID() != null) {
            DragonCodexSavedData.get(level).addDragon(offspring.getOwnerUUID(), offspring);
        }
    }
}
