package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.RegistryObject;

/**
 * Enum representing different wyvern types in the mod.
 * Provides easy access to wyvern entities and their properties.
 */
public enum DragonType {
    LIGHTNING("lightning", Raevyx.class, ModEntities.RAEVYX);

    private final String name;
    private final Class<? extends DragonEntity> entityClass;
    private final RegistryObject<? extends EntityType<? extends DragonEntity>> entityType;

    DragonType(String name, Class<? extends DragonEntity> entityClass, RegistryObject<? extends EntityType<? extends DragonEntity>> entityType) {
        this.name = name;
        this.entityClass = entityClass;
        this.entityType = entityType;
    }

    /**
     * Get the display name of this wyvern type
     */
    public String getName() {
        return name;
    }

    /**
     * Get the entity class for this wyvern type
     */
    public Class<? extends DragonEntity> getEntityClass() {
        return entityClass;
    }

    /**
     * Get the entity type registry object for this wyvern type
     */
    public RegistryObject<? extends EntityType<? extends DragonEntity>> getEntityType() {
        return entityType;
    }

    /**
     * Get the actual entity type for this wyvern type
     */
    public EntityType<? extends DragonEntity> getEntityTypeValue() {
        return entityType.get();
    }

    /**
     * Check if the given entity is of this wyvern type
     */
    public boolean isInstance(DragonEntity entity) {
        return entityClass.isInstance(entity);
    }

    /**
     * Get wyvern type from entity class
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
     * Get wyvern type from entity instance
     */
    public static DragonType fromEntity(DragonEntity entity) {
        return fromEntityClass(entity.getClass());
    }
}