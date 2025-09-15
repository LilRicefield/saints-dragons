package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.RegistryObject;

/**
 * Enum representing different dragon types in the mod.
 * Provides easy access to dragon entities and their properties.
 */
public enum DragonType {
    LIGHTNING("lightning", LightningDragonEntity.class, ModEntities.LIGHTNING_DRAGON);

    private final String name;
    private final Class<? extends DragonEntity> entityClass;
    private final RegistryObject<? extends EntityType<? extends DragonEntity>> entityType;

    DragonType(String name, Class<? extends DragonEntity> entityClass, RegistryObject<? extends EntityType<? extends DragonEntity>> entityType) {
        this.name = name;
        this.entityClass = entityClass;
        this.entityType = entityType;
    }

    /**
     * Get the display name of this dragon type
     */
    public String getName() {
        return name;
    }

    /**
     * Get the entity class for this dragon type
     */
    public Class<? extends DragonEntity> getEntityClass() {
        return entityClass;
    }

    /**
     * Get the entity type registry object for this dragon type
     */
    public RegistryObject<? extends EntityType<? extends DragonEntity>> getEntityType() {
        return entityType;
    }

    /**
     * Get the actual entity type for this dragon type
     */
    public EntityType<? extends DragonEntity> getEntityTypeValue() {
        return entityType.get();
    }

    /**
     * Check if the given entity is of this dragon type
     */
    public boolean isInstance(DragonEntity entity) {
        return entityClass.isInstance(entity);
    }

    /**
     * Get dragon type from entity class
     */
    public static DragonType fromEntityClass(Class<? extends DragonEntity> entityClass) {
        for (DragonType type : values()) {
            if (type.entityClass.equals(entityClass)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Get dragon type from entity instance
     */
    public static DragonType fromEntity(DragonEntity entity) {
        return fromEntityClass(entity.getClass());
    }
}
