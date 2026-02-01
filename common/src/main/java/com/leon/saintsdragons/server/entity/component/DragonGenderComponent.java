package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;

public final class DragonGenderComponent {
    private final DragonEntity dragon;
    private final EntityDataAccessor<Byte> dataAccessor;
    private boolean genderInitialized = false;

    public DragonGenderComponent(DragonEntity dragon, EntityDataAccessor<Byte> dataAccessor) {
        this.dragon = dragon;
        this.dataAccessor = dataAccessor;
    }

    public DragonGender getGender() {
        return DragonGender.fromId(dragon.getEntityData().get(dataAccessor));
    }

    public void setGender(@Nullable DragonGender gender) {
        DragonGender resolved = gender == null ? DragonGender.MALE : gender;
        dragon.getEntityData().set(dataAccessor, resolved.getId());
        this.genderInitialized = true;
    }

    public boolean isFemale() {
        return getGender() == DragonGender.FEMALE;
    }

    public void setFemale(boolean female) {
        setGender(female ? DragonGender.FEMALE : DragonGender.MALE);
    }

    public boolean hasGender() {
        return genderInitialized;
    }

    public void ensureInitialized() {
        if (dragon.level() != null && dragon.level().isClientSide) {
            return;
        }
        if (!genderInitialized) {
            setGender(dragon.getRandom().nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE);
        }
    }

    public void saveToNBT(CompoundTag tag) {
        byte genderId = dragon.getEntityData().get(dataAccessor);
        tag.putByte("Gender", genderId);
        tag.putBoolean("IsFemale", genderId == DragonGender.FEMALE.getId());
        tag.putBoolean("GenderInitialized", genderInitialized);
    }

    public void loadFromNBT(CompoundTag tag) {
        if (tag.contains("Gender", Tag.TAG_BYTE)) {
            byte savedGenderId = tag.getByte("Gender");
            boolean savedGenderInit = tag.contains("GenderInitialized") ? tag.getBoolean("GenderInitialized") : true;
            setGender(DragonGender.fromId(savedGenderId));
            this.genderInitialized = savedGenderInit;
        } else if (tag.contains("IsFemale")) {
            setFemale(tag.getBoolean("IsFemale"));
            this.genderInitialized = tag.contains("GenderInitialized") ? tag.getBoolean("GenderInitialized") : true;
        } else {
            this.genderInitialized = false;
            ensureInitialized();
        }
    }

    public void setInitialized(boolean initialized) {
        this.genderInitialized = initialized;
    }

    public boolean isInitialized() {
        return genderInitialized;
    }
}
