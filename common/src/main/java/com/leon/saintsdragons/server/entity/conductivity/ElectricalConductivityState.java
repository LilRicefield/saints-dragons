package com.leon.saintsdragons.server.entity.conductivity;

/**
 * Snapshot of the current conductivity factors for an electrically aligned entity.
 */
public record ElectricalConductivityState(
        boolean submerged,
        boolean wetEnvironment,
        float damageMultiplier,
        double rangeMultiplier) {
}
