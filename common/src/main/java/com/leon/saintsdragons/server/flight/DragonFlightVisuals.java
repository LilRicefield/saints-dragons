package com.leon.saintsdragons.server.flight;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class DragonFlightVisuals {
    private static final float BANK_COLLISION_DAMP = 0.45f;
    private static final float BANK_COLLISION_RETURN = 0.55f;
    private static final float BANK_YAW_MEMORY = 0.70f;
    private static final float BANK_YAW_BLEND = 0.30f;
    private static final float BANK_SCALE = 5.5f;
    private static final float BANK_MAX_ANGLE = 90.0f;
    private static final float BANK_LERP = 0.32f;
    private static final float RIDER_PITCH_MEMORY = 0.20f;
    private static final float RIDER_PITCH_BLEND = 0.80f;
    private static final float RIDER_PITCH_LERP = 0.62f;
    private static final float RIDER_VERTICAL_KEY_PITCH_MEMORY = 0.72f;
    private static final float RIDER_VERTICAL_KEY_PITCH_BLEND = 0.28f;
    private static final float RIDER_VERTICAL_KEY_PITCH_LERP = 0.24f;
    private static final float AI_PITCH_LERP = 0.34f;
    private static final double AI_PITCH_MIN_HORIZONTAL_SPEED = 0.22D;
    private static final double AI_PITCH_VERTICAL_DEADZONE = 0.06D;
    private static final float AI_PITCH_DEADZONE_RAD = (float) Math.toRadians(4.0D);

    private DragonFlightVisuals() {
    }

    public static void tickBanking(State state, boolean flying, boolean horizontalCollision,
                                   boolean verticalCollision, float yRot, float yRotO) {
        state.prevBankAngle = state.bankAngle;

        if (!flying) {
            state.bankSmoothedYaw = 0f;
            state.bankAngle = 0f;
            state.prevBankAngle = 0f;
            return;
        }

        if (horizontalCollision || verticalCollision) {
            state.bankSmoothedYaw *= BANK_COLLISION_DAMP;
            state.bankAngle = Mth.lerp(BANK_COLLISION_RETURN, state.bankAngle, 0f);
            if (Math.abs(state.bankAngle) < 0.01f) {
                state.bankAngle = 0f;
            }
            return;
        }

        float yawChange = Mth.wrapDegrees(yRot - yRotO);
        state.bankSmoothedYaw = state.bankSmoothedYaw * BANK_YAW_MEMORY + yawChange * BANK_YAW_BLEND;

        float targetAngle = Mth.clamp(state.bankSmoothedYaw * BANK_SCALE, -BANK_MAX_ANGLE, BANK_MAX_ANGLE);
        state.bankAngle = Mth.lerp(BANK_LERP, state.bankAngle, targetAngle);
        if (Math.abs(state.bankAngle) < 0.01f) {
            state.bankAngle = 0f;
        }
    }

    public static void beginPitchTick(State state) {
        state.prevFlightPitchRad = state.flightPitchRad;
    }

    public static void resetPitch(State state) {
        state.flightPitchRad = 0f;
        state.smoothedPlayerPitchRad = 0f;
        state.verticalKeyPitchSmoothing = false;
    }

    public static float smoothRiderPitchInput(State state, float rawPitchRad) {
        state.smoothedPlayerPitchRad = state.smoothedPlayerPitchRad * RIDER_PITCH_MEMORY + rawPitchRad * RIDER_PITCH_BLEND;
        return Mth.clamp(state.smoothedPlayerPitchRad, -Mth.HALF_PI, Mth.HALF_PI);
    }

    public static float smoothRiderVerticalKeyPitchInput(State state, float rawPitchRad) {
        state.smoothedPlayerPitchRad = state.smoothedPlayerPitchRad * RIDER_VERTICAL_KEY_PITCH_MEMORY
                + rawPitchRad * RIDER_VERTICAL_KEY_PITCH_BLEND;
        return Mth.clamp(state.smoothedPlayerPitchRad, -Mth.HALF_PI, Mth.HALF_PI);
    }

    public static void clearRiderPitchInput(State state) {
        state.smoothedPlayerPitchRad = 0f;
    }

    public static float computeAiPitchTarget(Vec3 velocity) {
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed <= AI_PITCH_MIN_HORIZONTAL_SPEED) {
            return 0f;
        }

        double verticalSpeed = Math.abs(velocity.y) < AI_PITCH_VERTICAL_DEADZONE ? 0.0D : velocity.y;
        float targetPitchRad = (float) Math.atan2(verticalSpeed, horizontalSpeed);
        if (Math.abs(targetPitchRad) < AI_PITCH_DEADZONE_RAD) {
            return 0f;
        }
        return Mth.clamp(targetPitchRad, -Mth.HALF_PI, Mth.HALF_PI);
    }

    public static float approachRiderPitch(float currentPitchRad, float targetPitchRad) {
        return approachPitch(currentPitchRad, targetPitchRad, RIDER_PITCH_LERP);
    }

    public static float approachRiderVerticalKeyPitch(float currentPitchRad, float targetPitchRad) {
        return approachPitch(currentPitchRad, targetPitchRad, RIDER_VERTICAL_KEY_PITCH_LERP);
    }

    public static float approachAiPitch(float currentPitchRad, float targetPitchRad) {
        return approachPitch(currentPitchRad, targetPitchRad, AI_PITCH_LERP);
    }

    private static float approachPitch(float currentPitchRad, float targetPitchRad, float lerp) {
        float next = Mth.lerp(lerp, currentPitchRad, targetPitchRad);
        return Math.abs(next) < 0.001f ? 0f : next;
    }

    public static final class State {
        public float bankSmoothedYaw;
        public float bankAngle;
        public float prevBankAngle;
        public float flightPitchRad;
        public float prevFlightPitchRad;
        public float smoothedPlayerPitchRad;
        public boolean verticalKeyPitchSmoothing;
    }
}
