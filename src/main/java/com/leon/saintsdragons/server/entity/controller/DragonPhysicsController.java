package com.leon.saintsdragons.server.entity.controller;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * Handles all animation logic for the Lightning Dragon
 */
public class DragonPhysicsController {
    private final LightningDragonEntity dragon;
    // Takeoff animation timing tuning
    private static final int TAKEOFF_ANIM_MAX_TICKS = 24;   // previously 30
    private static final int TAKEOFF_ANIM_EARLY_TICKS = 16; // start even sooner once airborne

    // ===== FLIGHT ANIMATION CONTROLLERS =====
    public static class FlightAnimationController {
        private float timer = 0f;
        private final float maxTime;

        public FlightAnimationController(float maxTime) {
            this.maxTime = maxTime;
        }

        public void increaseTimer() { timer = Math.min(timer + 0.2f, maxTime); }

        public void decreaseTimer() { timer = Math.max(timer - 0.2f, 0f); }
    }

    // Animation controllers
    public final FlightAnimationController glidingController = new FlightAnimationController(25f);
    public final FlightAnimationController flappingController = new FlightAnimationController(20f);
    public final FlightAnimationController hoveringController = new FlightAnimationController(15f);
    // Physics envelopes (only used when USE_PHYSICS_ENVELOPES)
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
    private RawAnimation currentFlightAnimation = LightningDragonEntity.FLY_GLIDE;

    // Enhanced flap timing
    private int discreteFlapCooldown = 0;
    private boolean hasPlayedFlapSound = false;
    private static final int FLAP_MIN_HOLD_TICKS = 28; // ensure near-full visible cycle incl. blend
    // Temporary lock to force flapping (e.g., when starting to climb)
    private int flapLockTicks = 0;
    // Temporary lock to hold glide state briefly

    // Wing beat intensity for sound timing
    private float wingBeatIntensity = 0f;

    // Sound timing constants
    private static final float BEAT_THRESHOLD = 0.7f;

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
    public DragonPhysicsController(LightningDragonEntity dragon) {
        this.dragon = dragon;
    }

    /**
     * Main tick method - call this from your entity's tick()
     */
    public void tick() {
        // Store previous values for interpolation
        prevGlidingFraction = glidingFraction;
        prevFlappingFraction = flappingFraction;
        prevHoveringFraction = hoveringFraction;
        // no previous wingbeat interpolation needed; value is used internally
        updatePhysicsEnvelopes();
    }

    // 0=glide, 1=flap/forward, 2=hover, 3=takeoff, -1=ground/none
    public int computeFlightModeForSync() {
        if (!dragon.isFlying()) return -1;
        if (shouldPlayTakeoff()) return 3;
        // Use same hysteresis tendencies as predicate: if flapping dominates → forward, else glide; hovering when hoveringFraction is significant
        float hoverWeight = hoveringFraction;
        float flapWeight = flappingFraction;
        boolean hovering = hoverWeight > 0.35f; // slightly above predicate’s exit
        if (hovering) return 2;
        boolean flap = flapWeight > 0.40f; // coarse threshold for sync
        return flap ? 1 : 0;
    }
    public PlayState handleMovementAnimation(AnimationState<LightningDragonEntity> state) {
        // TODO: Handle new Dragon ability system animations

        // Default transition length (safe baseline); override per-branch below
        state.getController().transitionLength(6);
        // While dying or sleeping, suppress movement animations entirely; action controller plays die/sleep clips
        if (dragon.isDying() || dragon.isSleeping()) {
            return PlayState.STOP;
        }
        // Drive SIT from pose/progress instead of owner order to avoid desync
        if (dragon.isInSittingPose() || dragon.getSitProgress() > 0.5f) {
            state.setAndContinue(LightningDragonEntity.SIT);
        } else if (dragon.isDodging()) {
            state.setAndContinue(LightningDragonEntity.DODGE);
        } else if (dragon.isLanding()) {
            state.setAndContinue(LightningDragonEntity.LANDING);
        } else if (dragon.isFlying()) {
            // Ensure short ascents finish a full flap cycle
            if (flapLockTicks > 0) {
                flapLockTicks--;
                state.getController().transitionLength(4);
                boolean ascendingNow = dragon.isGoingUp() || dragon.getDeltaMovement().y > 0.02;
                state.setAndContinue(ascendingNow ? LightningDragonEntity.FLAP : LightningDragonEntity.FLY_FORWARD);
                return PlayState.CONTINUE;
            }
            // Prefer server-synced flight mode when available for observer consistency
            int syncedMode = dragon.getEffectiveFlightMode();
            if (syncedMode == 3) {
                state.getController().transitionLength(4);
                state.setAndContinue(LightningDragonEntity.TAKEOFF);
                return PlayState.CONTINUE;
            }
            if (syncedMode == 2) {
                // Stationary/hover: play dedicated air hover clip
                state.getController().transitionLength(6);
                state.setAndContinue(LightningDragonEntity.FLAP);
                return PlayState.CONTINUE;
            }
            if (syncedMode == 1) {
                state.getController().transitionLength(4);
                Vec3 vNow = dragon.getDeltaMovement();
                boolean ascendingNow = dragon.isGoingUp() || vNow.y > 0.02;
                boolean stationaryAir = (vNow.horizontalDistanceSqr() < 0.0025 && Math.abs(vNow.y) < 0.02)
                        || dragon.isHovering()
                        || hoveringFraction > 0.45f;
                RawAnimation desired = (ascendingNow || stationaryAir)
                        ? LightningDragonEntity.FLAP
                        : LightningDragonEntity.FLY_FORWARD;
                state.setAndContinue(desired);
                return PlayState.CONTINUE;
            }
            if (syncedMode == 0) {
                // Server says GLIDE: render GLIDE unconditionally for consistency
                state.getController().transitionLength(6);
                state.setAndContinue(LightningDragonEntity.FLY_GLIDE);
                return PlayState.CONTINUE;
            }

            if (shouldPlayTakeoff()) {
                // Snappier blend into takeoff when leaving ground
                state.getController().transitionLength(4);
                state.setAndContinue(LightningDragonEntity.TAKEOFF);
            } else {
                // HYSTERESIS - prevent rapid switching between animations
                float hoverWeight = hoveringFraction;
                float flapWeight = flappingFraction;

                // Base thresholds for entering/exiting flap (without locks)
                boolean shouldFlapBase = (currentFlightAnimation == LightningDragonEntity.FLY_FORWARD)
                        ? (flapWeight > 0.22f || hoverWeight > 0.28f) // Lower threshold to exit
                        : (flapWeight > 0.55f || hoverWeight > 0.65f); // Higher threshold to enter

                // If we are clearly in hover without a synced mode (fallback), play air hover
                if (hoverWeight > 0.45f) {
                    state.getController().transitionLength(6);
                    currentFlightAnimation = LightningDragonEntity.FLAP;
                    state.setAndContinue(LightningDragonEntity.FLAP);
                    return PlayState.CONTINUE;
                }

                if (shouldFlapBase || dragon.getDeltaMovement().y >= -0.005) {
                    boolean ascendingNow = dragon.isGoingUp() || dragon.getDeltaMovement().y > 0.02;
                    RawAnimation desired = ascendingNow
                            ? LightningDragonEntity.FLAP
                            : LightningDragonEntity.FLY_FORWARD;
                    if (currentFlightAnimation != desired) {
                        // Slightly quicker blend into flap so the beat reads
                        state.getController().transitionLength(4);
                        currentFlightAnimation = desired;
                    }
                    state.setAndContinue(desired);
                } else {
                    if (currentFlightAnimation != LightningDragonEntity.FLY_GLIDE) {
                        // Smooth but not too long blend out of flap
                        state.getController().transitionLength(6);
                        currentFlightAnimation = LightningDragonEntity.FLY_GLIDE;
                    }
                    state.setAndContinue(LightningDragonEntity.FLY_GLIDE);
                }
            }
        } else {
            // Ground movement transitions tuned to be snappier
            if (dragon.isActuallyRunning()) {
                state.getController().transitionLength(3); // even faster into run
                state.setAndContinue(LightningDragonEntity.GROUND_RUN);
            } else if (dragon.isWalking()) {
                state.getController().transitionLength(3); // quicker walk engage/disengage
                state.setAndContinue(LightningDragonEntity.GROUND_WALK);
            } else {
                state.getController().transitionLength(4); // slightly softer into idle
                state.setAndContinue(LightningDragonEntity.GROUND_IDLE);
            }
        }
        return PlayState.CONTINUE;
    }

    private boolean shouldPlayTakeoff() {
        // Play takeoff at the very start of flight, and also as soon as we clear the ground or start ascending
        if (dragon.timeFlying < TAKEOFF_ANIM_EARLY_TICKS) return true;

        boolean airborne = !dragon.onGround();
        boolean ascending = dragon.getDeltaMovement().y > 0.08;

        return (dragon.timeFlying < TAKEOFF_ANIM_MAX_TICKS) && (airborne || ascending);
    }

    private void updateFlightAnimationControllers() {
        if (!dragon.isFlying()) {
            // Ground state - smoothly fade out all flight animations
            glidingController.decreaseTimer();
            flappingController.decreaseTimer();
            hoveringController.decreaseTimer();
            return;
        }

        Vec3 velocity = dragon.getDeltaMovement();
        Vec3 lookDirection = dragon.getLookAngle();
        double speedSqr = velocity.horizontalDistanceSqr();

        // Enhanced flight condition analysis
        boolean isDiving = lookDirection.y < -0.15 && velocity.y < -0.08;
        // More sensitive climb detection to encourage flaps
        boolean isClimbing = velocity.y > 0.06;
        boolean isSlowSpeed = speedSqr < 0.0036f;
        boolean isHoveringMode = dragon.isHovering() || (dragon.getTarget() != null && speedSqr < 0.03f);
        boolean isDescending = velocity.y < -0.04 && !isDiving;

        // Determine primary flight mode
        if (isHoveringMode || dragon.isLanding()) {
            // HOVERING MODE - for combat and precise movement
            hoveringController.increaseTimer();
            glidingController.decreaseTimer();

            // Hover-flapping (gentle wing beats to maintain position)
            if (isClimbing || isSlowSpeed || Math.abs(velocity.y) > 0.05) {
                // Smooth every-tick updates with reduced increment
                flappingController.increaseTimer();
                // Force a brief flap burst on initiating a climb
                if (isClimbing && flapLockTicks == 0 && discreteFlapCooldown <= 0) {
                    flapLockTicks = FLAP_MIN_HOLD_TICKS;
                    discreteFlapCooldown = FLAP_MIN_HOLD_TICKS;
                }
            } else {
                flappingController.decreaseTimer();
            }

            // Discrete hover flaps - less frequent than combat flaps
            if (discreteFlapCooldown <= 0 && (isClimbing || dragon.getRandom().nextFloat() < 0.03)) {
                triggerDiscreteFlapAnimation();
                discreteFlapCooldown = 30;
            }

        } else {
            // GLIDING MODE - the bread and butter of dragon flight
            hoveringController.decreaseTimer();

            // Intelligent flap detection
            boolean needsActiveFlapping = isDiving || isClimbing || isSlowSpeed || isDescending;

            if (needsActiveFlapping) {
                // Smooth every-tick updates with reduced increment
                flappingController.increaseTimer();
                glidingController.decreaseTimer();

                // Discrete flap trigger (only when physics demands it)
                if (discreteFlapCooldown <= 0) {
                    boolean shouldTriggerFlap = isDiving && velocity.y < -0.15;

                    if (isClimbing && velocity.y > 0.15) shouldTriggerFlap = true;
                    if (isDescending && dragon.getRandom().nextFloat() < 0.08) shouldTriggerFlap = true;

                    if (shouldTriggerFlap) {
                        triggerDiscreteFlapAnimation();
                        discreteFlapCooldown = 28 + dragon.getRandom().nextInt(16);
                        flapLockTicks = Math.max(flapLockTicks, 10);
                    }
                }

            } else {
                flappingController.decreaseTimer();
                glidingController.increaseTimer();

                // Reset flap sound flag during smooth gliding
                hasPlayedFlapSound = false;
            }
        }
    }

    private void updateWingBeatIntensity() {
        // Calculate wing beat intensity for realistic sound timing
        float targetIntensity = 0f;

        if (dragon.isFlying()) {
            // Base intensity on flight state
            if (hoveringFraction > 0.5f) {
                targetIntensity = 0.6f + flappingFraction * 0.4f; // Steady hover beats
            } else if (flappingFraction > 0.3f) {
                targetIntensity = 0.4f + flappingFraction * 0.6f; // Active flight beats
            } else {
                targetIntensity = glidingFraction * 0.2f; // Minimal gliding adjustments
            }


            Vec3 velocity = dragon.getDeltaMovement();
            double speed = velocity.horizontalDistanceSqr();

            targetIntensity += (float) (speed * 2.0f);
            targetIntensity = Mth.clamp(targetIntensity, 0f, 1f);
        }

        // Smooth approach to target intensity
        wingBeatIntensity = Mth.approach(wingBeatIntensity, targetIntensity, 0.05f);

        // Sound triggering logic
        if (dragon.isFlying() && !dragon.level().isClientSide) {
            handleFlightSounds();
        }
    }

    private void handleFlightSounds() {
        if (dragon.isStayOrSitMuted()) return;
        if (wingBeatIntensity <= BEAT_THRESHOLD || hasPlayedFlapSound) {
            if (wingBeatIntensity < 0.3f) {
                hasPlayedFlapSound = false;
            }
            return;
        }

        // Wing beat sound timing
        boolean shouldPlaySound = false;

        if (hoveringFraction > 0.5f) {
            shouldPlaySound = discreteFlapCooldown <= 0;
        } else if (flappingFraction > 0.4f) {
            shouldPlaySound = true;
        }

        if (shouldPlaySound) {
            playFlappingSound();
            hasPlayedFlapSound = true;
            discreteFlapCooldown = Math.max(discreteFlapCooldown, 15);
        }
    }

    private void triggerDiscreteFlapAnimation() {
        // TODO: Check with new Dragon ability system if animation can be triggered
        playFlappingSound();
    }

    private void playFlappingSound() {
        if (dragon.isStayOrSitMuted()) return;
        if (!dragon.level().isClientSide) {
            // Vary sound based on flight state
            float volume = 0.6f + wingBeatIntensity * 0.4f;
            float pitch = 0.9f + dragon.getRandom().nextFloat() * 0.4f;

            // Different sounds for different flight modes
            if (hoveringFraction > 0.5f) {
                pitch *= 1.1f; // Higher pitch for hovering
                volume *= 0.8f; // Softer for hovering
            } else if (glidingFraction > 0.5f) {
                volume *= 0.6f; // Very quiet for gliding adjustments
                pitch *= 0.9f; // Lower pitch for gliding
            }

            dragon.level().playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(),
                    SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE,
                    volume, pitch);
        }
    }

    // ===== GETTERS FOR RENDERER =====

    private void updatePhysicsEnvelopes() {
        Vec3 v = dragon.getDeltaMovement();
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
                dragon.isHovering() || dragon.isLanding() || dragon.isBeaming() ||
                (vH < 0.02f && Math.abs(vY) < 0.02f)
        ) ? 1f : 0f;
        float glideTarget = Mth.clamp(1f - flapTarget, 0.15f, 1f);

        // Explicit ascent bias so climbing always triggers visible flaps
        // Rider-controlled ascent
        if (dragon.isFlying()) {
            if (dragon.getControllingPassenger() != null && dragon.isGoingUp()) {
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

        // Wing beat intensity for sound timing and flap sound handling
        updateWingBeatIntensity();

        // Handle flap cooldowns
        if (discreteFlapCooldown > 0) {
            discreteFlapCooldown--;
        }
    }




    // ===== SAVE/LOAD SUPPORT =====
    public void writeToNBT(net.minecraft.nbt.CompoundTag tag) {
        // Store envelope values (authoritative for physics system)
        tag.putFloat("GlideVal", glideEnv.raw());
        tag.putFloat("FlapVal", flapEnv.raw());
        tag.putFloat("HoverVal", hoverEnv.raw());
        tag.putFloat("WingBeatIntensity", wingBeatIntensity);
        tag.putFloat("GlidingFraction", glidingFraction);
        tag.putFloat("FlappingFraction", flappingFraction);
        tag.putFloat("HoveringFraction", hoveringFraction);
        tag.putInt("DiscreteFlapCooldown", discreteFlapCooldown);
    }

    public void readFromNBT(net.minecraft.nbt.CompoundTag tag) {
        // Restore all animation state after load
        wingBeatIntensity = tag.getFloat("WingBeatIntensity");

        if (tag.contains("GlideVal")) {
            glideEnv.setRaw(tag.getFloat("GlideVal"));
            flapEnv.setRaw(tag.getFloat("FlapVal"));
            hoverEnv.setRaw(tag.getFloat("HoverVal"));
            glidingFraction = glideEnv.raw();
            flappingFraction = flapEnv.raw();
            hoveringFraction = hoverEnv.raw();
        } else {
            // Backward compatibility: fall back to old fractions
            glidingFraction = tag.getFloat("GlidingFraction");
            flappingFraction = tag.getFloat("FlappingFraction");
            hoveringFraction = tag.getFloat("HoveringFraction");
            glideEnv.setRaw(glidingFraction);
            flapEnv.setRaw(flappingFraction);
            hoverEnv.setRaw(hoveringFraction);
        }

        prevGlidingFraction = glidingFraction;
        prevFlappingFraction = flappingFraction;
        prevHoveringFraction = hoveringFraction;

        discreteFlapCooldown = tag.getInt("DiscreteFlapCooldown");
    }
}
