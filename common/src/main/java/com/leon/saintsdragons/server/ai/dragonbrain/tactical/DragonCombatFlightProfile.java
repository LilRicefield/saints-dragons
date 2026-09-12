package com.leon.saintsdragons.server.ai.dragonbrain.tactical;

public record DragonCombatFlightProfile(double firingRange,
                                       double approachRadius,
                                       double approachHeight,
                                       int groundCommitmentTicks,
                                       int airCommitmentTicks,
                                       int airborneConfirmationTicks,
                                       int groundedConfirmationTicks) {
    public static DragonCombatFlightProfile raevyx(double beamRange) {
        return new DragonCombatFlightProfile(beamRange * 1.1D, 22.0D, 8.0D, 80, 100, 8, 30);
    }
}
