package com.leon.saintsdragons.server.ai.dragonbrain.tactical;

public record DragonCombatFlightProfile(double firingRange,
                                       double approachRadius,
                                       double approachHeight,
                                       int groundCommitmentTicks,
                                       int airCommitmentTicks,
                                       int airborneConfirmationTicks,
                                       int groundedConfirmationTicks,
                                       int groundPreference) {
    public static DragonCombatFlightProfile raevyx(double beamRange) {
        return new DragonCombatFlightProfile(beamRange * 1.1D, 22.0D, 8.0D, 80, 100, 8, 30, 0);
    }

    public static DragonCombatFlightProfile ignivorus(double breathRange, boolean phase2) {
        return new DragonCombatFlightProfile(breathRange * 0.65D, 32.0D, 10.0D,
                phase2 ? 240 : 180, 140, 8, 30, phase2 ? 22 : 12);
    }

    public static DragonCombatFlightProfile volitans(double breathRange) {
        return new DragonCombatFlightProfile(breathRange * 0.9D, 22.0D, 8.0D, 120, 100, 8, 30, 6);
    }
}
