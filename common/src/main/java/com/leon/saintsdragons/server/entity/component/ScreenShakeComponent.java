package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;

public class ScreenShakeComponent {
    private final Entity entity;
    private final EntityDataAccessor<Float> syncedAmountAccessor;
    private final float decayPerTick;

    private float previousAmount = 0.0F;
    private float amount = 0.0F;
    private int holdTicks = 0;
    private float holdIntensity = 0.0F;

    public ScreenShakeComponent(Entity entity, EntityDataAccessor<Float> syncedAmountAccessor, float decayPerTick) {
        this.entity = entity;
        this.syncedAmountAccessor = syncedAmountAccessor;
        this.decayPerTick = decayPerTick;
    }

    public void tick() {
        SynchedEntityData entityData = entity.getEntityData();
        if (entity.level().isClientSide) {
            previousAmount = amount;
            amount = entityData.get(syncedAmountAccessor);
            return;
        }

        if (!SaintsDragonsConfig.SCREEN_SHAKE_ENABLED.get()) {
            clear();
            return;
        }

        previousAmount = amount;

        if (holdTicks > 0) {
            holdTicks--;
            amount = Math.max(amount, holdIntensity);
            entityData.set(syncedAmountAccessor, amount);
            if (holdTicks == 0) {
                holdIntensity = 0.0F;
                amount = 0.0F;
                entityData.set(syncedAmountAccessor, 0.0F);
            }
            return;
        }

        if (decayPerTick <= 0.0F) {
            if (amount != 0.0F || entityData.get(syncedAmountAccessor) != 0.0F) {
                amount = 0.0F;
                entityData.set(syncedAmountAccessor, 0.0F);
            }
            return;
        }

        if (amount > 0.0F) {
            amount = Math.max(0.0F, amount - decayPerTick);
            entityData.set(syncedAmountAccessor, amount);
        } else if (entityData.get(syncedAmountAccessor) != 0.0F) {
            entityData.set(syncedAmountAccessor, 0.0F);
        }
    }

    public float getAmount(float partialTicks) {
        float currentAmount = entity.getEntityData().get(syncedAmountAccessor);
        return previousAmount + (currentAmount - previousAmount) * partialTicks;
    }

    public void trigger(float intensity) {
        float clamped = Math.max(0.0F, intensity);
        if (clamped <= 0.0F || entity.level().isClientSide) {
            return;
        }
        amount = Math.max(amount, clamped);
        entity.getEntityData().set(syncedAmountAccessor, amount);
    }

    public void hold(float intensity, int durationTicks) {
        if (durationTicks <= 0) {
            trigger(intensity);
            return;
        }
        if (entity.level().isClientSide) {
            return;
        }
        holdIntensity = Math.max(holdIntensity, Math.max(0.0F, intensity));
        holdTicks = Math.max(holdTicks, durationTicks);
        amount = Math.max(amount, holdIntensity);
        entity.getEntityData().set(syncedAmountAccessor, amount);
    }

    public void force(float targetAmount) {
        if (entity.level().isClientSide) {
            return;
        }
        amount = Math.max(0.0F, targetAmount);
        previousAmount = amount;
        holdTicks = 0;
        holdIntensity = 0.0F;
        entity.getEntityData().set(syncedAmountAccessor, amount);
    }

    public void clear() {
        if (entity.level().isClientSide) {
            return;
        }
        holdTicks = 0;
        holdIntensity = 0.0F;
        amount = 0.0F;
        entity.getEntityData().set(syncedAmountAccessor, 0.0F);
    }
}
