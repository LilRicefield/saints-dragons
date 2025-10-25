package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.RegistryObject;

/**
 * Enum representing different dragon types in the mod.
 * Each dragon has an elemental affinity that defines its damage resistances,
 * immunities, and special interactions with the environment.
 */
public enum DragonType {
    /**
     * Lightning element - electrical attacks, storm affinity
     * Immune to lightning, benefits from rain/storms
     */
    LIGHTNING("lightning", Element.LIGHTNING, Raevyx.class, ModEntities.RAEVYX,
        ElementalProfile.builder(Element.LIGHTNING)
            .immuneTo(DamageTypeTags.IS_LIGHTNING)
            .build()),

    /**
     * Fire element - flame attacks, heat affinity
     * Immune to fire/lava, benefits from hot biomes (Nether, desert)
     */
    FIRE("fire", Element.FIRE, Cindervane.class, ModEntities.CINDERVANE,
        ElementalProfile.builder(Element.FIRE)
            .immuneTo(DamageTypeTags.IS_FIRE)
            .build()),

    /**
     * Physical/brute type - pure melee combat, no elemental attacks
     * Aquatic-adapted but no water-based elemental damage
     */
    PHYSICAL_BRUTE("physical", Element.NONE, Nulljaw.class, ModEntities.NULLJAW,
        ElementalProfile.builder(Element.NONE)
            .resistantTo(DamageTypeTags.IS_DROWNING, 0.5f)  // Semi-aquatic, less drowning damage
            .build()),

    /**
     * Physical/support type - buffs and utility, no elemental attacks
     */
    PHYSICAL_SUPPORT("physical", Element.NONE, Stegonaut.class, ModEntities.STEGONAUT,
        ElementalProfile.builder(Element.NONE)
            .resistantTo(DamageTypeTags.IS_FALL, 0.3f)  // Sturdy, reduced fall damage
            .build());

    private final String name;
    private final Element element;
    private final Class<? extends DragonEntity> entityClass;
    private final RegistryObject<? extends EntityType<? extends DragonEntity>> entityType;
    private final ElementalProfile elementalProfile;

    DragonType(String name, Element element, Class<? extends DragonEntity> entityClass,
               RegistryObject<? extends EntityType<? extends DragonEntity>> entityType,
               ElementalProfile elementalProfile) {
        this.name = name;
        this.element = element;
        this.entityClass = entityClass;
        this.entityType = entityType;
        this.elementalProfile = elementalProfile;
    }

    /**
     * Get the display name of this dragon type
     */
    public String getName() {
        return name;
    }

    /**
     * Get the elemental affinity of this dragon type
     */
    public Element getElement() {
        return element;
    }

    /**
     * Get the elemental profile (resistances, immunities, etc.)
     */
    public ElementalProfile getElementalProfile() {
        return elementalProfile;
    }

    /**
     * Check if this dragon has elemental attacks (not purely physical)
     */
    public boolean hasElementalAttacks() {
        return element.isElemental();
    }

    /**
     * Get the entity class for this dragon type
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