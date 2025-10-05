package com.leon.saintsdragons.server.entity.conductivity;

/**
 * Defines how electric-style entities respond to conductive environments.
 * Values are expressed as additive multipliers applied on top of a base of 1.0.
 */
public record ElectricalConductivityProfile(
        float baseDamageMultiplier,
        float submergedDamageBonus,
        float wetDamageBonus,
        double baseRangeMultiplier,
        double submergedRangeBonus,
        double wetRangeBonus) {

    public static final ElectricalConductivityProfile DEFAULT =
            new ElectricalConductivityProfile(1.0f, 0.5f, 0.2f, 1.0, 0.3, 0.1);

    public ElectricalConductivityProfile {
        if (baseDamageMultiplier <= 0.0f) {
            throw new IllegalArgumentException("baseDamageMultiplier must be positive");
        }
        if (baseRangeMultiplier <= 0.0) {
            throw new IllegalArgumentException("baseRangeMultiplier must be positive");
        }
    }
}
