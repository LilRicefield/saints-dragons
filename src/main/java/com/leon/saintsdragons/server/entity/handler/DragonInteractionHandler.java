package com.leon.saintsdragons.server.entity.handler;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;

/**
 * Minimal interaction helper. The entity itself handles taming and player
 * interactions; this class maintains smooth sitting progress only.
 */
public record DragonInteractionHandler(LightningDragonEntity dragon) {

    /**
     * Updates sitting progress for smooth animations
     */
    public void updateSittingProgress() {
        if (dragon.isOrderedToSit() && dragon.sitProgress < dragon.maxSitTicks()) {
            dragon.sitProgress++;
            if (!dragon.level().isClientSide) {
                dragon.getEntityData().set(LightningDragonEntity.DATA_SIT_PROGRESS, dragon.sitProgress);
            }
        }
        if (!dragon.isOrderedToSit() && dragon.sitProgress > 0F) {
            dragon.sitProgress--;
            if (!dragon.level().isClientSide) {
                dragon.getEntityData().set(LightningDragonEntity.DATA_SIT_PROGRESS, dragon.sitProgress);
            }
        }
    }
}
