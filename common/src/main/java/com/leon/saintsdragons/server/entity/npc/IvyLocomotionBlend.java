package com.leon.saintsdragons.server.entity.npc;

import net.minecraft.util.Mth;

public final class IvyLocomotionBlend {
    private static final float WALK_BLEND_SPEED = 0.26F;
    private static final float RUN_BLEND_SPEED = 0.44F;
    private static final float WALK_CYCLE_TICKS = 0.8333F * 20.0F;
    private static final float RUN_CYCLE_TICKS = 0.6667F * 20.0F;
    private static final float MOVE_START_SPEED = 0.008F;
    private static final float FULL_MOVE_SPEED = 0.07F;
    private static final float START_RESPONSE = 0.65F;
    private static final float STOP_RESPONSE = 0.5F;
    private static final float RUN_RESPONSE = 0.4F;
    private static final double TELEPORT_DISTANCE_SQR = 4.0D;

    private float moving;
    private float previousMoving;
    private float running;
    private float previousRunning;
    private double phase;
    private double previousPhase;

    public void tick(IvyTheDragonMerchant ivy) {
        previousMoving = moving;
        previousRunning = running;
        previousPhase = phase;
        double dx = ivy.getX() - ivy.xo;
        double dz = ivy.getZ() - ivy.zo;
        double distanceSqr = dx * dx + dz * dz;
        if (ivy.tickCount <= 1 || distanceSqr > TELEPORT_DISTANCE_SQR || ivy.isPassenger()) {
            moving = previousMoving = running = previousRunning = 0.0F;
            return;
        }

        float speed = (float) Math.sqrt(distanceSqr);
        float targetMoving = smoothstep(MOVE_START_SPEED, FULL_MOVE_SPEED, speed);
        moving = Mth.lerp(targetMoving > moving ? START_RESPONSE : STOP_RESPONSE, moving, targetMoving);
        if (targetMoving > 0.0F && targetMoving >= moving) {
            float targetRunning = smoothstep(WALK_BLEND_SPEED, RUN_BLEND_SPEED, speed);
            running = Mth.lerp(RUN_RESPONSE, running, targetRunning);
        }
        moving = snapWeight(moving);
        running = snapWeight(running);
        float cycleTicks = Mth.lerp(running, WALK_CYCLE_TICKS, RUN_CYCLE_TICKS);
        if (speed > MOVE_START_SPEED) {
            phase += 1.0D / cycleTicks;
        }
    }

    public float moving(float partialTick) {
        return Mth.lerp(partialTick, previousMoving, moving);
    }

    public float running(float partialTick) {
        return Mth.lerp(partialTick, previousRunning, running);
    }

    public double phase(float partialTick) {
        return Mth.lerp(partialTick, previousPhase, phase);
    }

    private static float smoothstep(float low, float high, float value) {
        float weight = Mth.clamp((value - low) / (high - low), 0.0F, 1.0F);
        return weight * weight * (3.0F - 2.0F * weight);
    }

    private static float snapWeight(float value) {
        return value < 0.0001F ? 0.0F : value > 0.9999F ? 1.0F : value;
    }
}
