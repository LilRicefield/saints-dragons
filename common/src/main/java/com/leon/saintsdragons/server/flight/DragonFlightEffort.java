package com.leon.saintsdragons.server.flight;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class DragonFlightEffort {
    private Vec3 motion = Vec3.ZERO;
    private float effort;
    private int mode = DragonFlightStateEvaluator.MODE_FLY_IDLE;
    private int pendingMode = DragonFlightStateEvaluator.MODE_FLY_IDLE;
    private int pendingTicks;
    private int cruiseGlideTicks;
    private int cruiseFlapTicksRemaining;

    public void tick(Vec3 measuredMotion, double cruiseSpeed, double requestedSpeed,
                     boolean riderControlled, boolean sprintRequested, boolean holdingDiveMomentum,
                     boolean diving, RandomSource random, Profile profile) {
        double referenceSpeed = Math.max(0.1D, cruiseSpeed);
        double previousSpeed = motion.length();
        motion = motion.lerp(measuredMotion, 0.35D);
        double speed = motion.length();
        double speedRatio = speed / referenceSpeed;
        if (diving) {
            // The glide base lets the existing procedural glide_down pose own the wings
            effort = Mth.lerp(profile.response(), effort, 0.0F);
            mode = pendingMode = DragonFlightStateEvaluator.MODE_GLIDE;
            pendingTicks = 0;
            resetCruiseCycle();
            return;
        }
        double speedDeficit = Mth.clamp((requestedSpeed - speed) / referenceSpeed, 0.0D, 1.0D);
        double acceleration = Mth.clamp((speed - previousSpeed) / (referenceSpeed * 0.08D), 0.0D, 1.0D);
        double climb = Mth.clamp(motion.y / Math.max(referenceSpeed * 0.35D, speed * 0.65D), 0.0D, 1.0D);
        double descent = Mth.clamp(-motion.y / Math.max(referenceSpeed * 0.35D, speed * 0.65D), 0.0D, 1.0D);
        double surplus = requestedSpeed > 0.0D
                ? Mth.clamp((speed - requestedSpeed) / (referenceSpeed * 0.5D), 0.0D, 1.0D) : 0.0D;
        double diveSupport = holdingDiveMomentum
                ? Mth.clamp((speedRatio - 0.45D) / 0.55D, 0.0D, 1.0D) : 0.0D;
        boolean poweredSprint = sprintRequested && requestedSpeed > 0.0D;

        double target = requestedSpeed > 0.0D
                ? (poweredSprint ? profile.sprintEffort() : profile.cruiseEffort()) : 0.0D;
        target += 0.50D * speedDeficit + 0.70D * climb;
        if (requestedSpeed > 0.0D) target += 0.25D * acceleration;
        target = Mth.clamp(target, 0.0D, 1.0D);
        target *= 1.0D - 0.95D * descent;
        target *= 1.0D - 0.90D * Math.max(surplus, diveSupport);
        // Sprint remains powered flight after reaching speed while diving was handled above
        if (poweredSprint) target = Math.max(target, profile.sprintEffort());

        // Rider start/stop intent is already known so smoothing speed here delays the idle blend twice
        // AI keeps motion-based hysteresis so small course corrections do not flicker in and out of idle
        boolean hovering = riderControlled ? requestedSpeed <= 0.0D
                : speedRatio < (mode == DragonFlightStateEvaluator.MODE_FLY_IDLE ? 0.20D : 0.12D);
        boolean cruising = requestedSpeed > 0.0D && !poweredSprint && !hovering
                && !holdingDiveMomentum && descent < 0.15D && target < profile.cruiseFlapEffort();
        target = applyCruiseFlaps(target, cruising, random, profile);
        if (hovering) target = Math.max(target, 0.80D);
        effort = Mth.lerp(profile.response(), effort, (float) target);

        int nextMode;
        if (hovering) {
            nextMode = DragonFlightStateEvaluator.MODE_FLY_IDLE;
        } else if (poweredSprint || effort >= (mode == DragonFlightStateEvaluator.MODE_SPRINT_FLAP
                ? profile.sprintExit() : profile.sprintEnter())) {
            nextMode = DragonFlightStateEvaluator.MODE_SPRINT_FLAP;
        } else if (effort <= (mode == DragonFlightStateEvaluator.MODE_GLIDE
                ? profile.flapEnter() : profile.glideEnter())) {
            nextMode = DragonFlightStateEvaluator.MODE_GLIDE;
        } else {
            nextMode = DragonFlightStateEvaluator.MODE_FLAP;
        }

        if (nextMode == DragonFlightStateEvaluator.MODE_FLY_IDLE || mode == DragonFlightStateEvaluator.MODE_FLY_IDLE) {
            // The pose mixer eases idle entry/exit. We reserve the settle timer for changes within moving flight
            mode = pendingMode = nextMode;
            pendingTicks = 0;
        } else if (nextMode == mode) {
            pendingMode = mode;
            pendingTicks = 0;
        } else {
            if (nextMode != pendingMode) {
                pendingMode = nextMode;
                pendingTicks = 0;
            }
            if (++pendingTicks >= profile.settleTicks()) {
                mode = nextMode;
                pendingTicks = 0;
            }
        }
    }

    private double applyCruiseFlaps(double target, boolean cruising, RandomSource random, Profile profile) {
        if (!cruising) {
            resetCruiseCycle();
            return target;
        }
        if (cruiseGlideTicks < profile.cruiseGlideTicks()) {
            cruiseGlideTicks++;
            return target;
        }
        if (cruiseFlapTicksRemaining == 0) {
            // Pick a duration once per burst. The pose mixer owns all blending and wingbeat timing
            cruiseFlapTicksRemaining = profile.cruiseFlapTicks()
                    + random.nextInt(profile.cruiseFlapExtraTicks() + 1);
        }
        if (--cruiseFlapTicksRemaining == 0) cruiseGlideTicks = 0;
        return Math.max(target, profile.cruiseFlapEffort());
    }

    private void resetCruiseCycle() {
        cruiseGlideTicks = cruiseFlapTicksRemaining = 0;
    }

    public void reset() {
        motion = Vec3.ZERO;
        effort = 0.0F;
        mode = pendingMode = DragonFlightStateEvaluator.MODE_FLY_IDLE;
        pendingTicks = 0;
        resetCruiseCycle();
    }

    public float effort() {
        return effort;
    }

    public int mode() {
        return mode;
    }

    public record Profile(float cruiseEffort, float sprintEffort, float glideEnter, float flapEnter,
                          float sprintEnter, float sprintExit, float response, int settleTicks,
                          int cruiseGlideTicks, int cruiseFlapTicks, int cruiseFlapExtraTicks, float cruiseFlapEffort) {
        public Profile {
            if (cruiseGlideTicks < 1 || cruiseFlapTicks < 1 || cruiseFlapExtraTicks < 0) {
                throw new IllegalArgumentException("Invalid cruise animation durations");
            }
        }
    }
}
