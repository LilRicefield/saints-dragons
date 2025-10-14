package com.leon.saintsdragons.server.entity.controller.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import static com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxConstantsHandler.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * Clean animation controller for Raevyx - simple and maintainable
 */
public class RaevyxPhysicsController {
    private final Raevyx wyvern;
    // Takeoff animation timing tuning
    private static final int TAKEOFF_ANIM_MAX_TICKS = 24;   // previously 30
    private static final int TAKEOFF_ANIM_EARLY_TICKS = 16; // start even sooner once airborne

    // Physics envelopes for renderer effects
    private final Envelope01 glideEnv = new Envelope01(0.25f, 0.25f);
    private final Envelope01 flapEnv  = new Envelope01(0.25f, 0.18f);
    private final Envelope01 hoverEnv = new Envelope01(0.40f, 0.15f);

    // Animation fraction values for smooth blending
    public float glidingFraction = 0f;
    public float prevGlidingFraction = 0f;
    public float flappingFraction = 0f;
    public float prevFlappingFraction = 0f;
    public float hoveringFraction = 0f;
    public float prevHoveringFraction = 0f;

    // Flight animation state tracking
    private RawAnimation currentFlightAnimation = FLY_GLIDE;

    // ===== Envelopes and lift model =====
    public static class Envelope01 {
        private float val = 0f;
        private float prev = 0f;
        private final float upRate;
        private final float downRate;
        public Envelope01(float upRate, float downRate) { this.upRate = upRate; this.downRate = downRate; }
        public void tickToward(float target) {
            prev = val;
            float rate = target > val ? upRate : downRate;
            val += (target - val) * rate;
            if (val < 0f) val = 0f; else if (val > 1f) val = 1f;
        }
        public float raw() { return val; }
        public float get(float pt) { return Mth.lerp(pt, prev, val); }
        public void setRaw(float v) { prev = val = Mth.clamp(v, 0f, 1f); }
    }

    // Physics envelopes enabled by default
    private static final float MASS = 1.3f;
    private static final float LIFT_K = 11.0f;
    private static final float CLIMB_COST = 6.0f;
    private static final float RESPONSE = 1.5f;
    public RaevyxPhysicsController(Raevyx wyvern) {
        this.wyvern = wyvern;
    }

    /**
     * Main tick method - call this from your entity's tick()
     */
    public void tick() {
        // Store previous values for interpolation
        prevGlidingFraction = glidingFraction;
        prevFlappingFraction = flappingFraction;
        prevHoveringFraction = hoveringFraction;

        updatePhysicsEnvelopes();
    }

    // 0=glide, 1=flap/forward, 2=hover, 3=takeoff, -1=ground/none
    public int computeFlightModeForSync() {
        if (!wyvern.isFlying()) return -1;
        if (shouldPlayTakeoff()) return 3;
        // Use same hysteresis tendencies as predicate: if flapping dominates → forward, else glide; hovering when hoveringFraction is significant
        float hoverWeight = hoveringFraction;
        float flapWeight = flappingFraction;
        boolean hovering = hoverWeight > 0.35f; // slightly above predicate's exit
        if (hovering) return 2;
        boolean flap = flapWeight > 0.40f; // coarse threshold for sync
        return flap ? 1 : 0;
    }
    public PlayState handleMovementAnimation(AnimationState<Raevyx> state) {
        // Default transition length (safe baseline); override per-branch below
        state.getController().transitionLength(6);
        // While dying or sleeping (including transitions), suppress movement animations entirely; action controller plays die/sleep clips
        if (wyvern.isDying() || wyvern.isSleeping() || wyvern.isSleepingEntering() || wyvern.isSleepingExiting()) {
            return PlayState.STOP;
        }
        // Drive SIT from our custom progress system only to avoid desync
        if (wyvern.getSitProgress() > 0.5f) {
            state.setAndContinue(SIT);
        } else if (wyvern.isDodging()) {
            state.setAndContinue(DODGE);
        } else if (wyvern.isLanding()) {
            state.setAndContinue(LANDING);
        } else if (wyvern.isFlying()) {
            // Prefer server-synced flight mode when available for observer consistency
            int syncedMode = wyvern.getSyncedFlightMode();
            Vec3 vNow = wyvern.getDeltaMovement();
            if (syncedMode == 3) {
                state.getController().transitionLength(4);
                state.setAndContinue(TAKEOFF);
                return PlayState.CONTINUE;
            }

            boolean manualRiderControl = wyvern.isTame() && wyvern.isVehicle();
            if (manualRiderControl) {
                if (wyvern.isGoingUp()) {
                    RawAnimation upward = FLAP;
                    if (currentFlightAnimation != upward) {
                        state.getController().transitionLength(4);
                        currentFlightAnimation = upward;
                    }
                    state.setAndContinue(upward);
                    return PlayState.CONTINUE;
                }
                if (wyvern.isGoingDown()) {
                    RawAnimation descend = GLIDE_DOWN;
                    if (currentFlightAnimation != descend) {
                        state.getController().transitionLength(6);
                        currentFlightAnimation = descend;
                    }
                    state.setAndContinue(descend);
                    return PlayState.CONTINUE;
                }
            }
            if (syncedMode == 2) {
                // Stationary/hover: play dedicated air hover clip
                state.getController().transitionLength(6);
                state.setAndContinue(FLAP);
                return PlayState.CONTINUE;
            }
            if (syncedMode == 1) {
                // Mode 1 = FLAP (low altitude or physics demands flapping)
                state.getController().transitionLength(4);
                state.setAndContinue(FLAP);
                return PlayState.CONTINUE;
            }
            if (syncedMode == 0) {
                // Server says GLIDE: evaluate descend heuristics for tamed dragons
                // Longer transition for smoother high-altitude gliding feel
                state.getController().transitionLength(12);
                state.setAndContinue(resolveGlideAnimation(vNow));
                return PlayState.CONTINUE;
            }

            if (shouldPlayTakeoff()) {
                // Snappier blend into takeoff when leaving ground
                state.getController().transitionLength(4);
                state.setAndContinue(TAKEOFF);
            } else {
                // HYSTERESIS - prevent rapid switching between animations
                float hoverWeight = hoveringFraction;
                float flapWeight = flappingFraction;

                boolean descendingNow = vNow.y < -0.03;
                if (wyvern.isVehicle()) {
                    descendingNow |= wyvern.isGoingDown();
                } else {
                    descendingNow |= wyvern.getPitchDirection() > 0;
                }

                // Base thresholds for entering/exiting flap (without locks)
                boolean shouldFlapBase = (currentFlightAnimation == FLAP)
                        ? (flapWeight > 0.55f || hoverWeight > 0.65f) // Higher threshold to stay flapping
                        : (flapWeight > 0.22f || hoverWeight > 0.28f); // Lower threshold to enter flapping

                // If we are clearly in hover without a synced mode (fallback), play air hover
                if (hoverWeight > 0.45f) {
                    state.getController().transitionLength(6);
                    currentFlightAnimation = FLAP;
                    state.setAndContinue(FLAP);
                    return PlayState.CONTINUE;
                }

                // PHYSICS-BASED: Use flap/glide envelope weights
                boolean ascendingNow = wyvern.isGoingUp() || vNow.y > 0.02;

                // Always flap when ascending
                if (ascendingNow) {
                    if (currentFlightAnimation != FLAP) {
                        state.getController().transitionLength(4);
                        currentFlightAnimation = FLAP;
                    }
                    state.setAndContinue(FLAP);
                }
                // Use physics envelope to decide
                else if (shouldFlapBase) {
                    if (currentFlightAnimation != FLAP) {
                        state.getController().transitionLength(4);
                        currentFlightAnimation = FLAP;
                    }
                    state.setAndContinue(FLAP);
                } else {
                    // Default to gliding when not ascending and physics doesn't demand flapping
                    RawAnimation glideAnimation = resolveGlideAnimation(vNow);
                    if (currentFlightAnimation != glideAnimation) {
                        state.getController().transitionLength(8);
                        currentFlightAnimation = glideAnimation;
                    }
                    state.setAndContinue(glideAnimation);
                }
            }
        } else {
            // Ground movement transitions tuned to be snappier
            if (wyvern.isActuallyRunning()) {
                state.getController().transitionLength(3); // even faster into run
                state.setAndContinue(GROUND_RUN);
            } else if (wyvern.isWalking()) {
                state.getController().transitionLength(3); // quicker walk engage/disengage
                state.setAndContinue(GROUND_WALK);
            } else {
                state.getController().transitionLength(4); // slightly softer into idle
                state.setAndContinue(GROUND_IDLE);
            }
        }
        return PlayState.CONTINUE;
    }

    private boolean shouldPlayTakeoff() {
        // Play takeoff at the very start of flight, and also as soon as we clear the ground or start ascending
        if (wyvern.timeFlying < TAKEOFF_ANIM_EARLY_TICKS) return true;

        boolean airborne = !wyvern.onGround();
        boolean ascending = wyvern.getDeltaMovement().y > 0.08;

        return (wyvern.timeFlying < TAKEOFF_ANIM_MAX_TICKS) && (airborne || ascending);
    }

    private void updatePhysicsEnvelopes() {
        Vec3 v = wyvern.getDeltaMovement();
        float vH = (float)Math.hypot(v.x, v.z);
        float vY = (float)v.y;

        float glideLift = LIFT_K * vH * vH;
        float climbNeed = vY > 0 ? (vY * CLIMB_COST) : 0f;
        float need = MASS + climbNeed - glideLift;

        float flapTarget = need <= 0 ? 0f : (need / (need + RESPONSE));
        flapTarget = Mth.clamp(flapTarget, 0f, 1f);

        // Treat hover as a near-stationary state: only when horizontal speed is tiny AND vertical nearly zero,
        // or when explicitly flagged (hovering/landing/beaming)
        float hoverTarget = (
                wyvern.isHovering() || wyvern.isLanding() || wyvern.isBeaming() ||
                (vH < 0.02f && Math.abs(vY) < 0.02f)
        ) ? 1f : 0f;
        float glideTarget = Mth.clamp(1f - flapTarget, 0.15f, 1f);

        // Explicit ascent bias so climbing always triggers visible flaps
        // Rider-controlled ascent
        if (wyvern.isFlying()) {
            if (wyvern.getControllingPassenger() != null && wyvern.isGoingUp()) {
                flapTarget = Math.max(flapTarget, 0.6f);
            } else if (vY > 0.06f) {
                // AI/physics ascent: scale bias by vertical speed
                float ascentBias = Mth.clamp((vY - 0.02f) * 3.0f, 0.2f, 0.8f);
                flapTarget = Math.max(flapTarget, ascentBias);
            }
            // Recompute glide target after bias
            glideTarget = Mth.clamp(1f - flapTarget, 0.15f, 1f);
        }

        flapEnv.tickToward(flapTarget);
        hoverEnv.tickToward(hoverTarget);
        glideEnv.tickToward(glideTarget);

        // Update animation fractions for renderer from envelopes
        glidingFraction = glideEnv.raw();
        flappingFraction = flapEnv.raw();
        hoveringFraction = hoverEnv.raw();
    }

    private RawAnimation resolveGlideAnimation(Vec3 velocity) {
        if (!wyvern.isTame()) {
            return FLY_GLIDE;
        }

        Vec3 motion = velocity == null ? Vec3.ZERO : velocity;
        double verticalSpeed = motion.y;
        double horizontalSpeedSqr = motion.horizontalDistanceSqr();

        boolean riderDescending = wyvern.isVehicle() && wyvern.isGoingDown();
        boolean pitchingDown = !wyvern.isVehicle() && wyvern.getPitchDirection() > 0;
        boolean fallingFast = verticalSpeed < -0.06;
        boolean moderateDescent = verticalSpeed < -0.025;
        boolean sustainedGlide = glidingFraction > 0.18f || flapEnv.raw() < 0.35f;
        boolean hasForwardSpeed = horizontalSpeedSqr > 0.0009;

        if ((pitchingDown || riderDescending || fallingFast || moderateDescent) && sustainedGlide && hasForwardSpeed) {
            return GLIDE_DOWN;
        }

        return FLY_GLIDE;
    }

    // ===== SAVE/LOAD SUPPORT =====
    public void writeToNBT(net.minecraft.nbt.CompoundTag tag) {
        // Store envelope values (authoritative for physics system)
        tag.putFloat("GlideVal", glideEnv.raw());
        tag.putFloat("FlapVal", flapEnv.raw());
        tag.putFloat("HoverVal", hoverEnv.raw());
    }

    public void readFromNBT(net.minecraft.nbt.CompoundTag tag) {
        // Restore all animation state after load
        if (tag.contains("GlideVal")) {
            glideEnv.setRaw(tag.getFloat("GlideVal"));
            flapEnv.setRaw(tag.getFloat("FlapVal"));
            hoverEnv.setRaw(tag.getFloat("HoverVal"));
        }

        glidingFraction = glideEnv.raw();
        flappingFraction = flapEnv.raw();
        hoveringFraction = hoverEnv.raw();

        prevGlidingFraction = glidingFraction;
        prevFlappingFraction = flappingFraction;
        prevHoveringFraction = hoveringFraction;
    }
}