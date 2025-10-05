package com.leon.saintsdragons.server.entity.conductivity;

import net.minecraft.world.entity.LivingEntity;

/**
 * Implemented by dragons or other mobs whose electric abilities gain bonuses when wet.
 */
public interface ElectricalConductivityCapable {

    LivingEntity asConductiveEntity();

    ElectricalConductivityProfile getConductivityProfile();

    default ElectricalConductivityState getConductivityState() {
        return ElectricalConductivityHelper.evaluate(this);
    }

    default float getConductivityDamageMultiplier() {
        return getConductivityState().damageMultiplier();
    }

    default double getConductivityRangeMultiplier() {
        return getConductivityState().rangeMultiplier();
    }
}
