package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EntityType;
import java.util.function.Supplier;

public enum DragonType {

    RAEVYX("raevyx", ElementalClass.LIGHTNING,
        DragonAttributeConfigLoader.RAEVYX_ID,
        Raevyx.class, ModEntities.RAEVYX,
        ElementalProfile.builder()
            .immuneTo(DamageTypeTags.IS_LIGHTNING)
            .build()),

    CINDERVANE("cindervane", ElementalClass.FIRE,
        DragonAttributeConfigLoader.CINDERVANE_ID,
        Cindervane.class, ModEntities.CINDERVANE,
        ElementalProfile.builder()
            // Note: Specific vanilla fire immunity is handled in Cindervane.hurt()
            // This allows magical fire from mods to deal damage
            .build()),

    VARASUCHUS("varasuchus", ElementalClass.NON_ELEMENTAL,
        DragonAttributeConfigLoader.VARASUCHUS_ID,
        Varasuchus.class, ModEntities.VARASUCHUS,
        ElementalProfile.builder()
            .resistantTo(DamageTypeTags.IS_DROWNING, 0.5f)  // Semi-aquatic, less drowning damage
            .build()),

    STEGONAUT("stegonaut", ElementalClass.NON_ELEMENTAL,
        DragonAttributeConfigLoader.STEGONAUT_ID,
        Stegonaut.class, ModEntities.STEGONAUT,
        ElementalProfile.builder()
            .resistantTo(DamageTypeTags.IS_FALL, 0.3f)  // Sturdy, reduced fall damage
            .build()),

    VOLITANS("volitans", ElementalClass.NON_ELEMENTAL,
        DragonAttributeConfigLoader.VOLITANS_ID,
        Volitans.class, ModEntities.VOLITANS,
        ElementalProfile.builder()
            .build()),

    IGNIVORUS("ignivorus", ElementalClass.FIRE,
        DragonAttributeConfigLoader.IGNIVORUS_ID,
        com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus.class, ModEntities.IGNIVORUS,
        ElementalProfile.builder()
            // Note: Specific vanilla fire immunity is handled in Ignivorus.hurt()
            // This allows magical fire from mods to deal damage
            .build());

    private final String name;
    private final ElementalClass elementalClass;
    private final ResourceLocation configId;
    private final Class<? extends DragonEntity> entityClass;
    private final Supplier<? extends EntityType<? extends DragonEntity>> entityType;
    private final ElementalProfile elementalProfile;

    DragonType(String name,
               ElementalClass elementalClass,
               ResourceLocation configId,
               Class<? extends DragonEntity> entityClass,
               Supplier<? extends EntityType<? extends DragonEntity>> entityType,
               ElementalProfile elementalProfile) {
        this.name = name;
        this.elementalClass = elementalClass;
        this.configId = configId;
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
     * Get the elemental class for shared elemental logic.
     */
    public ElementalClass getElementalClass() {
        return elementalClass;
    }

    public String getElementId() {
        return elementalClass.id;
    }

    public int getElementColor() {
        return elementalClass.color;
    }

    public boolean isAffectedByWeather() {
        return elementalClass.affectedByWeather;
    }

    public boolean isAffectedByBiome() {
        return elementalClass.affectedByBiome;
    }

    public boolean isFireElement() {
        return elementalClass == ElementalClass.FIRE;
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
        return elementalClass != ElementalClass.NON_ELEMENTAL;
    }

    public ResourceLocation getConfigId() {
        return configId;
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
    public Supplier<? extends EntityType<? extends DragonEntity>> getEntityType() {
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
            if (type.entityClass.isAssignableFrom(entityClass)) {
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

    public enum ElementalClass {
        LIGHTNING("lightning", 0x7DD3FC, true, false),
        FIRE("fire", 0xFF6B35, false, true),
        NON_ELEMENTAL("none", 0xFFFFFF, false, false);

        private final String id;
        private final int color;
        private final boolean affectedByWeather;
        private final boolean affectedByBiome;

        ElementalClass(String id, int color, boolean affectedByWeather, boolean affectedByBiome) {
            this.id = id;
            this.color = color;
            this.affectedByWeather = affectedByWeather;
            this.affectedByBiome = affectedByBiome;
        }
    }
}
