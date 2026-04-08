package com.leon.saintsdragons.server.flight;

import net.minecraft.world.phys.Vec3;


public final class DragonFlightStateEvaluator {
    public static final int MODE_GROUND = -1;
    public static final int MODE_GLIDE = 0;
    public static final int MODE_FLAP = 1;
    public static final int MODE_HOVER = 2;
    public static final int MODE_TAKEOFF = 3;
    public static final int MODE_SPRINT_FLAP = 4;
    public static final int MODE_FLY_IDLE = 5;
    public static final int MODE_LANDING = 6;

    private static final double RIDER_MOVEMENT_POSITION_THRESHOLD_SQR = 0.01D;
    private static final int RIDER_IDLE_TICKS = 10;
    private static final double ASCENT_SPEED_THRESHOLD = 0.02D;
    private static final double AI_IDLE_HORIZONTAL_SPEED_SQR = 0.0016D;
    private static final double AI_IDLE_VERTICAL_SPEED = 0.03D;
    private static final double AI_GLIDE_ENTER_ALTITUDE = 46.0D;
    private static final double AI_GLIDE_EXIT_ALTITUDE = 32.0D;
    private static final float RIDER_GLIDE_DOWN_DEGREES = -12.0f;
    private static final double AI_GLIDE_DOWN_MIN_HORIZONTAL_SPEED = 0.14D;
    private static final double AI_GLIDE_DOWN_MIN_DESCENT = -0.12D;
    private static final double AI_GLIDE_DOWN_DEGREES = 24.0D;
    private static final double LANDING_TOUCHDOWN_ALTITUDE = 3.0D;

    private DragonFlightStateEvaluator() {
    }

    public static int evaluateSyncedMode(State state, FlightInput input) {
        if (!input.flying) {
            reset(state);
            return MODE_GROUND;
        }

        if (input.takeoff) {
            state.riderHighAltitudeGlide = false;
            resetAiState(state);
            return MODE_TAKEOFF;
        }

        if (input.landing) {
            state.riderHighAltitudeGlide = false;
            resetAiState(state);
            return MODE_LANDING;
        }

        if (input.hovering) {
            state.riderHighAltitudeGlide = false;
            resetAiState(state);
            return MODE_HOVER;
        }

        if (input.riddenByOwner) {
            resetAiState(state);
            return evaluateRiderMode(state, input);
        }

        state.riderHighAltitudeGlide = false;
        return evaluateAiMode(state, input);
    }

    public static VisualState evaluateVisualState(int syncedMode, boolean ridden, float flightPitchRadians, Vec3 velocity) {
        return switch (syncedMode) {
            case MODE_TAKEOFF -> VisualState.TAKEOFF;
            case MODE_LANDING -> VisualState.GLIDE_DOWN;
            case MODE_FLY_IDLE -> VisualState.FLY_IDLE;
            case MODE_SPRINT_FLAP -> shouldUseGlideDown(ridden, flightPitchRadians, velocity)
                    ? VisualState.GLIDE_DOWN
                    : VisualState.SPRINT_FLAP;
            case MODE_HOVER, MODE_FLAP -> shouldUseGlideDown(ridden, flightPitchRadians, velocity)
                    ? VisualState.GLIDE_DOWN
                    : VisualState.FLAP;
            case MODE_GLIDE -> shouldUseGlideDown(ridden, flightPitchRadians, velocity)
                    ? VisualState.GLIDE_DOWN
                    : VisualState.GLIDE;
            default -> VisualState.GROUND;
        };
    }

    public static VisualState evaluateAnimationVisualState(int syncedMode, boolean ridden, float flightPitchRadians,
                                                           Vec3 velocity, boolean landing, double altitudeAboveTerrain,
                                                           double landingBlendAltitude,
                                                           boolean riderLandingBlendActive) {
        boolean nearTouchdownTerrain = altitudeAboveTerrain != Double.POSITIVE_INFINITY
                && altitudeAboveTerrain >= -0.25D
                && altitudeAboveTerrain <= Math.min(landingBlendAltitude, LANDING_TOUCHDOWN_ALTITUDE);
        if (riderLandingBlendActive || (landing && nearTouchdownTerrain)) {
            return VisualState.LANDING;
        }

        return evaluateVisualState(syncedMode, ridden, flightPitchRadians, velocity);
    }

    public static boolean shouldUseGlideDown(boolean ridden, float flightPitchRadians, Vec3 velocity) {
        if (ridden) {
            float pitchDegrees = (float) Math.toDegrees(flightPitchRadians);
            return pitchDegrees < RIDER_GLIDE_DOWN_DEGREES;
        }

        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed <= AI_GLIDE_DOWN_MIN_HORIZONTAL_SPEED || velocity.y >= AI_GLIDE_DOWN_MIN_DESCENT) {
            return false;
        }

        double pitchDegrees = Math.toDegrees(Math.atan2(-velocity.y, horizontalSpeed));
        return pitchDegrees > AI_GLIDE_DOWN_DEGREES;
    }

    private static int evaluateRiderMode(State state, FlightInput input) {
        double deltaX = input.x - state.lastCheckedX;
        double deltaY = input.y - state.lastCheckedY;
        double deltaZ = input.z - state.lastCheckedZ;
        double positionChangeSqr = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;

        if (positionChangeSqr > RIDER_MOVEMENT_POSITION_THRESHOLD_SQR
                || input.goingUp
                || input.goingDown
                || input.accelerating) {
            state.ticksSinceLastMovement = 0;
            state.lastCheckedX = input.x;
            state.lastCheckedY = input.y;
            state.lastCheckedZ = input.z;
        } else {
            state.ticksSinceLastMovement++;
        }

        if (state.ticksSinceLastMovement > RIDER_IDLE_TICKS) {
            return MODE_FLY_IDLE;
        }

        if (input.accelerating) {
            return MODE_SPRINT_FLAP;
        }

        boolean ascending = input.velocity.y > ASCENT_SPEED_THRESHOLD || input.goingUp;
        if (input.forceSurfaceGlide) {
            state.riderHighAltitudeGlide = false;
            return MODE_GLIDE;
        }

        if (ascending) {
            return MODE_FLAP;
        }

        if (state.riderHighAltitudeGlide) {
            if (input.altitudeAboveTerrain > input.riderGlideExitAltitude) {
                return MODE_GLIDE;
            }
            state.riderHighAltitudeGlide = false;
        } else if (input.altitudeAboveTerrain > input.riderGlideEnterAltitude) {
            state.riderHighAltitudeGlide = true;
            return MODE_GLIDE;
        }

        return MODE_FLAP;
    }

    private static int evaluateAiMode(State state, FlightInput input) {
        double horizontalSpeedSqr = input.velocity.horizontalDistanceSqr();
        double verticalDelta = input.y - input.previousY;
        int rawMode;

        if (horizontalSpeedSqr < AI_IDLE_HORIZONTAL_SPEED_SQR && Math.abs(verticalDelta) < AI_IDLE_VERTICAL_SPEED) {
            rawMode = MODE_FLY_IDLE;
        } else if (input.velocity.y > ASCENT_SPEED_THRESHOLD || input.goingUp) {
            rawMode = MODE_FLAP;
        } else {
            boolean stayGliding = state.aiStableMode == MODE_GLIDE && input.altitudeAboveTerrain > AI_GLIDE_EXIT_ALTITUDE;
            rawMode = (input.altitudeAboveTerrain > AI_GLIDE_ENTER_ALTITUDE || stayGliding) ? MODE_GLIDE : MODE_FLAP;
        }

        return stabilizeAiMode(state, rawMode);
    }

    private static int stabilizeAiMode(State state, int rawMode) {
        if (state.aiStableMode == MODE_GROUND) {
            state.aiStableMode = rawMode;
            state.aiPendingMode = MODE_GROUND;
            state.aiPendingModeTicks = 0;
            return rawMode;
        }

        if (rawMode == state.aiStableMode) {
            state.aiPendingMode = MODE_GROUND;
            state.aiPendingModeTicks = 0;
            return state.aiStableMode;
        }

        if (rawMode != state.aiPendingMode) {
            state.aiPendingMode = rawMode;
            state.aiPendingModeTicks = 1;
            return state.aiStableMode;
        }

        state.aiPendingModeTicks++;
        int requiredTicks = switch (rawMode) {
            case MODE_FLY_IDLE -> 10;
            case MODE_GLIDE, MODE_FLAP -> 6;
            default -> 4;
        };

        if (state.aiPendingModeTicks >= requiredTicks) {
            state.aiStableMode = rawMode;
            state.aiPendingMode = MODE_GROUND;
            state.aiPendingModeTicks = 0;
        }

        return state.aiStableMode;
    }

    public static void reset(State state) {
        state.riderHighAltitudeGlide = false;
        state.ticksSinceLastMovement = 0;
        state.lastCheckedX = 0.0D;
        state.lastCheckedY = 0.0D;
        state.lastCheckedZ = 0.0D;
        resetAiState(state);
    }

    private static void resetAiState(State state) {
        state.aiStableMode = MODE_GROUND;
        state.aiPendingMode = MODE_GROUND;
        state.aiPendingModeTicks = 0;
    }

    public enum VisualState {
        GROUND,
        TAKEOFF,
        LANDING,
        GLIDE,
        GLIDE_DOWN,
        FLAP,
        SPRINT_FLAP,
        FLY_IDLE
    }

    public static final class State {
        public boolean riderHighAltitudeGlide;
        public double lastCheckedX;
        public double lastCheckedY;
        public double lastCheckedZ;
        public int ticksSinceLastMovement;
        public int aiStableMode = MODE_GROUND;
        public int aiPendingMode = MODE_GROUND;
        public int aiPendingModeTicks;
    }

    public static final class FlightInput {
        public final boolean flying;
        public final boolean takeoff;
        public final boolean hovering;
        public final boolean landing;
        public final boolean riddenByOwner;
        public final boolean goingUp;
        public final boolean goingDown;
        public final boolean accelerating;
        public final boolean forceSurfaceGlide;
        public final double x;
        public final double y;
        public final double z;
        public final double previousY;
        public final double altitudeAboveTerrain;
        public final double riderGlideEnterAltitude;
        public final double riderGlideExitAltitude;
        public final Vec3 velocity;

        public FlightInput(boolean flying, boolean takeoff, boolean hovering, boolean landing,
                           boolean riddenByOwner, boolean goingUp, boolean goingDown,
                           boolean accelerating, boolean forceSurfaceGlide,
                           double x, double y, double z, double previousY,
                           double altitudeAboveTerrain, double riderGlideEnterAltitude,
                           double riderGlideExitAltitude, Vec3 velocity) {
            this.flying = flying;
            this.takeoff = takeoff;
            this.hovering = hovering;
            this.landing = landing;
            this.riddenByOwner = riddenByOwner;
            this.goingUp = goingUp;
            this.goingDown = goingDown;
            this.accelerating = accelerating;
            this.forceSurfaceGlide = forceSurfaceGlide;
            this.x = x;
            this.y = y;
            this.z = z;
            this.previousY = previousY;
            this.altitudeAboveTerrain = altitudeAboveTerrain;
            this.riderGlideEnterAltitude = riderGlideEnterAltitude;
            this.riderGlideExitAltitude = riderGlideExitAltitude;
            this.velocity = velocity;
        }
    }
}
