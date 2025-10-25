package com.leon.saintsdragons.common.registry;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

import java.util.*;

/**
 * Defines the elemental properties of a dragon type.
 * Handles resistances, immunities, and weaknesses to damage types.
 */
public class ElementalProfile {
    private final Element element;
    private final Set<TagKey<DamageType>> immunities;
    private final Map<TagKey<DamageType>, Float> resistances;
    private final Map<TagKey<DamageType>, Float> weaknesses;

    private ElementalProfile(Builder builder) {
        this.element = builder.element;
        this.immunities = Set.copyOf(builder.immunities);
        this.resistances = Map.copyOf(builder.resistances);
        this.weaknesses = Map.copyOf(builder.weaknesses);
    }

    /**
     * Get the element this profile is for
     */
    public Element getElement() {
        return element;
    }

    /**
     * Check if this dragon is immune to a damage type
     */
    public boolean isImmuneTo(DamageSource source) {
        return immunities.stream().anyMatch(source::is);
    }

    /**
     * Get the damage multiplier for a damage source.
     * Returns 1.0 for normal damage, <1.0 for resistances, >1.0 for weaknesses, 0.0 for immunities.
     */
    public float getDamageMultiplier(DamageSource source) {
        // Check immunities first
        if (isImmuneTo(source)) {
            return 0.0f;
        }

        // Check resistances
        for (Map.Entry<TagKey<DamageType>, Float> entry : resistances.entrySet()) {
            if (source.is(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Check weaknesses
        for (Map.Entry<TagKey<DamageType>, Float> entry : weaknesses.entrySet()) {
            if (source.is(entry.getKey())) {
                return entry.getValue();
            }
        }

        return 1.0f;  // Normal damage
    }

    /**
     * Create a new builder for an elemental profile
     */
    public static Builder builder(Element element) {
        return new Builder(element);
    }

    /**
     * Builder for creating elemental profiles
     */
    public static class Builder {
        private final Element element;
        private final Set<TagKey<DamageType>> immunities = new HashSet<>();
        private final Map<TagKey<DamageType>, Float> resistances = new HashMap<>();
        private final Map<TagKey<DamageType>, Float> weaknesses = new HashMap<>();

        private Builder(Element element) {
            this.element = element;
        }

        /**
         * Add immunity to a damage type (0% damage taken)
         */
        public Builder immuneTo(TagKey<DamageType> damageType) {
            immunities.add(damageType);
            return this;
        }

        /**
         * Add resistance to a damage type (reduced damage, multiplier < 1.0)
         * @param multiplier Damage multiplier (e.g., 0.5 = 50% damage, 0.25 = 25% damage)
         */
        public Builder resistantTo(TagKey<DamageType> damageType, float multiplier) {
            if (multiplier < 0.0f || multiplier >= 1.0f) {
                throw new IllegalArgumentException("Resistance multiplier must be between 0.0 and 1.0 (exclusive)");
            }
            resistances.put(damageType, multiplier);
            return this;
        }

        /**
         * Add weakness to a damage type (increased damage, multiplier > 1.0)
         * @param multiplier Damage multiplier (e.g., 1.5 = 150% damage, 2.0 = 200% damage)
         */
        public Builder weakTo(TagKey<DamageType> damageType, float multiplier) {
            if (multiplier <= 1.0f) {
                throw new IllegalArgumentException("Weakness multiplier must be greater than 1.0");
            }
            weaknesses.put(damageType, multiplier);
            return this;
        }

        /**
         * Build the elemental profile
         */
        public ElementalProfile build() {
            return new ElementalProfile(this);
        }
    }
}
