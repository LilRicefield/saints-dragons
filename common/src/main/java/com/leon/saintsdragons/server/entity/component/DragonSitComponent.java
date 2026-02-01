package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.Mth;

public final class DragonSitComponent {
    private final DragonEntity dragon;
    private final EntityDataAccessor<Float> dataAccessor;
    private float sitProgress = 0f;
    private float prevSitProgress = 0f;

    public DragonSitComponent(DragonEntity dragon, EntityDataAccessor<Float> dataAccessor) {
        this.dragon = dragon;
        this.dataAccessor = dataAccessor;
    }

    public float getSitProgress() {
        return sitProgress;
    }

    public float getPrevSitProgress() {
        return prevSitProgress;
    }

    public void setSitProgress(float value) {
        float clamped = Mth.clamp(value, 0f, dragon.maxSitTicks());
        sitProgress = clamped;
        dragon.getEntityData().set(dataAccessor, clamped);
    }

    public void setPrevSitProgress(float value) {
        prevSitProgress = value;
    }

    public void forceSitProgress(float value) {
        float clamped = Mth.clamp(value, 0f, dragon.maxSitTicks());
        sitProgress = clamped;
        prevSitProgress = clamped;
        dragon.getEntityData().set(dataAccessor, clamped);
    }

    public void clearSitProgress() {
        sitProgress = 0f;
        prevSitProgress = 0f;
        dragon.getEntityData().set(dataAccessor, 0f);
    }

    public void syncClientProgress() {
        prevSitProgress = sitProgress;
        sitProgress = dragon.getEntityData().get(dataAccessor);
    }

    public void saveToNBT(CompoundTag tag) {
        tag.putFloat("SitProgress", sitProgress);
    }

    public void loadFromNBT(CompoundTag tag, boolean orderedToSit) {
        float savedSitProgress = tag.contains("SitProgress")
                ? tag.getFloat("SitProgress")
                : (orderedToSit ? dragon.maxSitTicks() : 0f);
        sitProgress = Mth.clamp(savedSitProgress, 0f, dragon.maxSitTicks());
        prevSitProgress = sitProgress;
        dragon.getEntityData().set(dataAccessor, sitProgress);
    }
}
