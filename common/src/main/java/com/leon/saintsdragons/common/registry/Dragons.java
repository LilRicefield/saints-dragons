package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import java.util.function.Supplier;

public enum Dragons {

    RAEVYX("raevyx",
        DragonAttributeConfigLoader.RAEVYX_ID,
        Raevyx.class, ModEntities.RAEVYX),

    CINDERVANE("cindervane",
        DragonAttributeConfigLoader.CINDERVANE_ID,
        Cindervane.class, ModEntities.CINDERVANE),

    VARASUCHUS("varasuchus",
        DragonAttributeConfigLoader.VARASUCHUS_ID,
        Varasuchus.class, ModEntities.VARASUCHUS),

    STEGONAUT("stegonaut",
        DragonAttributeConfigLoader.STEGONAUT_ID,
        Stegonaut.class, ModEntities.STEGONAUT),

    VOLITANS("volitans",
        DragonAttributeConfigLoader.VOLITANS_ID,
        Volitans.class, ModEntities.VOLITANS),

    NULLJAW("nulljaw",
        DragonAttributeConfigLoader.NULLJAW_ID,
        Nulljaw.class, ModEntities.NULLJAW),

    ATROXIIA("atroxiia",
        DragonAttributeConfigLoader.ATROXIIA_ID,
        Atroxiia.class, ModEntities.ATROXIIA),

    IGNIVORUS("ignivorus",
        DragonAttributeConfigLoader.IGNIVORUS_ID,
        com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus.class, ModEntities.IGNIVORUS);

    private final String name;
    private final ResourceLocation configId;
    private final Class<? extends DragonEntity> entityClass;
    private final Supplier<? extends EntityType<? extends DragonEntity>> entityType;

    Dragons(String name,
            ResourceLocation configId,
            Class<? extends DragonEntity> entityClass,
            Supplier<? extends EntityType<? extends DragonEntity>> entityType) {
        this.name = name;
        this.configId = configId;
        this.entityClass = entityClass;
        this.entityType = entityType;
    }

    public String getName() {
        return name;
    }

    public ResourceLocation getConfigId() {
        return configId;
    }

    public Class<? extends DragonEntity> getEntityClass() {
        return entityClass;
    }

    public Supplier<? extends EntityType<? extends DragonEntity>> getEntityType() {
        return entityType;
    }

    public EntityType<? extends DragonEntity> getEntityTypeValue() {
        return entityType.get();
    }

    public boolean isInstance(DragonEntity entity) {
        return entityClass.isInstance(entity);
    }

    public static Dragons fromEntityClass(Class<? extends DragonEntity> entityClass) {
        for (Dragons type : values()) {
            if (type.entityClass.isAssignableFrom(entityClass)) {
                return type;
            }
        }
        return null;
    }

    public static Dragons fromEntity(DragonEntity entity) {
        return fromEntityClass(entity.getClass());
    }
}
