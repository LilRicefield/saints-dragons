package com.leon.saintsdragons.server.flight;

import net.minecraft.util.Mth;

public final class DragonBarrelRollHelper {
    private DragonBarrelRollHelper() {
    }

    public record Config(
            float airAutoAlignDecay,
            float landingAutoAlignStep,
            float smoothingFactor,
            float snapToUprightThresholdRad,
            float maxAutoAlignOffsetRad
    ) {
    }

    public record Input(
            boolean ridden,
            boolean grounded,
            boolean landing,
            boolean activelyRolling,
            boolean easeAllowed,
            boolean riderLandingBlendActive,
            double landingBlendAltitude,
            double altitudeAboveTerrain
    ) {
    }

    public record Output(
            float accumulatedRoll,
            float prevSmoothedRoll,
            float smoothedRoll
    ) {
    }

    public static Output tick(float accumulatedRoll, float smoothedRoll, Input input, Config config) {
        float currentRoll = accumulatedRoll;
        float currentSmoothedRoll = smoothedRoll;
        float currentPrevSmoothedRoll = smoothedRoll;

        if (input.grounded()) {
            float uprightRoll = getNearestUprightRoll(currentRoll);
            return new Output(uprightRoll, uprightRoll, uprightRoll);
        }

        if (!input.ridden()) {
            float uprightRoll = getNearestUprightRoll(currentRoll);
            return new Output(uprightRoll, uprightRoll, uprightRoll);
        }

        if (input.landing()) {
            currentRoll = 0.0f;
            currentPrevSmoothedRoll = currentSmoothedRoll;
            currentSmoothedRoll += (currentRoll - currentSmoothedRoll) * config.smoothingFactor();
            return new Output(currentRoll, currentPrevSmoothedRoll, currentSmoothedRoll);
        }

        if (!input.activelyRolling() && input.easeAllowed()) {
            float nearestUprightRoll = getNearestUprightRoll(currentRoll);
            float uprightOffset = currentRoll - nearestUprightRoll;

            if (Math.abs(uprightOffset) > 0.001f && Math.abs(uprightOffset) <= config.maxAutoAlignOffsetRad()) {
                float decay = config.airAutoAlignDecay();

                if (input.riderLandingBlendActive() && input.altitudeAboveTerrain() != Double.POSITIVE_INFINITY) {
                    float blendFactor = 1.0f - (float) Mth.clamp(
                            input.altitudeAboveTerrain() / input.landingBlendAltitude(),
                            0.0D,
                            1.0D
                    );
                    decay = Mth.lerp(blendFactor, decay, config.landingAutoAlignStep());
                }

                currentRoll = nearestUprightRoll + uprightOffset * decay;
                if (Math.abs(currentRoll - nearestUprightRoll) < config.snapToUprightThresholdRad()) {
                    currentRoll = nearestUprightRoll;
                }
            }
        }

        currentPrevSmoothedRoll = currentSmoothedRoll;
        currentSmoothedRoll += (currentRoll - currentSmoothedRoll) * config.smoothingFactor();
        return new Output(currentRoll, currentPrevSmoothedRoll, currentSmoothedRoll);
    }

    public static float getNearestUprightRoll(float rollRadians) {
        return Math.round(rollRadians / Mth.TWO_PI) * Mth.TWO_PI;
    }
}
