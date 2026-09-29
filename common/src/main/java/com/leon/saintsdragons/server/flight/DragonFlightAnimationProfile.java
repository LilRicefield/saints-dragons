package com.leon.saintsdragons.server.flight;

/**
 * Opt-in flight animation settings. Speed multipliers describe existing movement, never change it
 * New fliers return this profile from getFlightAnimationProfile(), install DragonFlightAnimationController,
 * and select their glide base in the flight predicate while isFlightBlendActive()
 * Supply glide, flap, sprint and idle clips, and drive procedural dive offsets with
 * getFlightAnimationDivePose() so wing folding follows the blend's glide weight
 * Keep takeoff, landing and species-specific action guards ahead of that predicate branch
 */
public record DragonFlightAnimationProfile(
        DragonFlightEffort.Profile effort,
        DragonFlightBlend.Profile blend,
        Clips clips,
        Speeds speeds,
        float divePosePriorityThreshold,
        int entryTicks,
        float minimumAudibleWingbeatWeight) {

    public record Speeds(double riderCruise, double riderSprint, double aiCruise) {}

    public record Clips(String glide, Clip flap, Clip sprint, Clip idle, String wingbeatSound) {}

   //   Source timing is separate from the shared playback clock. Long clips can contain many strokes
    public record Clip(String name, double cycleTicks, double startOffsetTicks,
                       double downstrokePhase, boolean continuousMolang) {
        public static Clip keyframed(String name, double downstrokePhase) {
            return new Clip(name, 0.0D, 0.0D, downstrokePhase, false);
        }

        public static Clip procedural(String name, double cycleTicks, double startOffsetTicks) {
            return new Clip(name, cycleTicks, startOffsetTicks, 0.5D, true);
        }

        public double sourceTick(double phase, double animationLength) {
            double cycle = Math.floor(phase);
            double fraction = phase - cycle;
            double clipPhase = fraction < 0.5D ? fraction * 2.0D * downstrokePhase
                    : downstrokePhase + (fraction - 0.5D) * 2.0D * (1.0D - downstrokePhase);
            double duration = cycleTicks > 0.0D ? cycleTicks : animationLength;
            return ((continuousMolang ? cycle : 0.0D) + clipPhase) * duration + startOffsetTicks;
        }

        public double soundPhase(double soundTick, double animationLength) {
            double duration = cycleTicks > 0.0D ? cycleTicks : animationLength;
            double phase = (soundTick - startOffsetTicks) / duration;
            phase -= Math.floor(phase);
            return phase < downstrokePhase ? phase / downstrokePhase * 0.5D
                    : 0.5D + (phase - downstrokePhase) / (1.0D - downstrokePhase) * 0.5D;
        }
    }
}
