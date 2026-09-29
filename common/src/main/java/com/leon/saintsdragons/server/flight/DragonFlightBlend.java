package com.leon.saintsdragons.server.flight;

import net.minecraft.util.Mth;

public final class DragonFlightBlend {
    private float flap;
    private float sprint;
    private float previousFlap;
    private float previousSprint;
    private float idle;
    private float previousIdle;
    private double phase;
    private double previousPhase;
    private double phaseCorrection;
    private float lastSyncedPhase = Float.NaN;

    public void tick(float effort, boolean powered, boolean idleRequested, float syncedPhase, Profile profile) {
        previousFlap = flap;
        previousSprint = sprint;
        previousIdle = idle;
        previousPhase = phase;
        float power = powered ? Mth.clamp((effort - profile.glideEffort())
                / (profile.flapEffort() - profile.glideEffort()), 0.0F, 1.0F) : 0.0F;
        float fast = powered ? Mth.clamp((effort - profile.flapEffort())
                / (profile.sprintEffort() - profile.flapEffort()), 0.0F, 1.0F) : 0.0F;
        // Keep the outgoing forward-flight mix under idle so stopping never detours through glide
        if (!idleRequested) {
            flap = Mth.lerp(profile.response(), flap, power * (1.0F - fast));
            sprint = Mth.lerp(profile.response(), sprint, fast);
        }
        idle = Mth.lerp(profile.idleResponse(), idle, idleRequested ? 1.0F : 0.0F);
        if (flap < 0.0001F) flap = 0.0F;
        if (sprint < 0.0001F) sprint = 0.0F;
        if (idle < 0.0001F) idle = 0.0F;
        if (idle > 0.9999F) idle = 1.0F;
        float sprintShare = flap + sprint > 0.0001F ? sprint / (flap + sprint) : 0.0F;
        float forwardTicks = Mth.lerp(sprintShare, profile.flapTicks(), profile.sprintTicks());
        phase += 1.0D / Mth.lerp(idle, forwardTicks, profile.idleTicks());

        // Clients run the same clock between packets and gently correct drift, without rewinding a stroke
        if (!Float.isNaN(syncedPhase) && syncedPhase != lastSyncedPhase) {
            if (Float.isNaN(lastSyncedPhase)) {
                phase = previousPhase = syncedPhase;
            } else {
                double error = syncedPhase - phase;
                phaseCorrection = error - Math.floor(error + 0.5D);
            }
            lastSyncedPhase = syncedPhase;
        }
        double correction = Mth.clamp(phaseCorrection * 0.25D, -0.01D, 0.01D);
        phase += correction;
        phaseCorrection -= correction;
    }

    public void reset() {
        flap = sprint = previousFlap = previousSprint = idle = previousIdle = 0.0F;
        phase = previousPhase = phaseCorrection = 0.0D;
        lastSyncedPhase = Float.NaN;
    }

    public float flap(float partialTick) {
        return Mth.lerp(partialTick, previousFlap, flap);
    }

    public float sprint(float partialTick) {
        return Mth.lerp(partialTick, previousSprint, sprint);
    }

    public float idle(float partialTick) {
        return Mth.lerp(partialTick, previousIdle, idle);
    }

    public float glide(float partialTick) {
        return Mth.clamp(1.0F - flap(partialTick) - sprint(partialTick), 0.0F, 1.0F)
                * (1.0F - idle(partialTick));
    }

    public double phase(float partialTick) {
        return Mth.lerp(partialTick, previousPhase, phase);
    }

    public float syncedPhase() {
        // Include the cycle count so new observers also join long, multi-stroke body animations in phase
        return (float) phase;
    }

    public record Profile(float glideEffort, float flapEffort, float sprintEffort,
                          float response, float flapTicks, float sprintTicks,
                          float idleResponse, float idleTicks) {
    }
}
