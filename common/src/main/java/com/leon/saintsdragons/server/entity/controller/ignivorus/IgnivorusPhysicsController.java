package com.leon.saintsdragons.server.entity.controller.ignivorus;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Physics controller for Ignivorus - handles flight mode computation with smooth envelope transitions
 */
public class IgnivorusPhysicsController {
    private final Ignivorus dragon;
    private boolean riderHighAltitudeGlide = false;

    private static final int TAKEOFF_ANIM_MAX_TICKS = 30;   // Match 1.5s animation length (30 ticks at 20 TPS)
    private static final int TAKEOFF_ANIM_EARLY_TICKS = 5; // Start checking conditions slightly earlier

    // ===== Physics Envelopes =====
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

    // Physics constants - fire dragons are powerful and heavy
    private static final float MASS = 1.4f;      // Slightly heavier than Raevyx (1.3)
    private static final float LIFT_K = 10.5f;   // Slightly less lift than Raevyx (11.0)
    private static final float CLIMB_COST = 6.0f; // Same climb cost as Raevyx
    private static final float RESPONSE = 1.5f;   // Same response as Raevyx

    // ===== Envelope class for smooth transitions =====
    public static class Envelope01 {
        private float val = 0f;
        private float prev = 0f;
        private final float upRate;
        private final float downRate;

        public Envelope01(float upRate, float downRate) {
            this.upRate = upRate;
            this.downRate = downRate;
        }

        public void tickToward(float target) {
            prev = val;
            float rate = target > val ? upRate : downRate;
            val += (target - val) * rate;
            if (val < 0f) val = 0f;
            else if (val > 1f) val = 1f;
        }

        public float raw() { return val; }
        public float get(float pt) { return Mth.lerp(pt, prev, val); }
        public void setRaw(float v) { prev = val = Mth.clamp(v, 0f, 1f); }
    }

    public IgnivorusPhysicsController(Ignivorus dragon) {
        this.dragon = dragon;
    }

    public void tick() {
        // Store previous values for interpolation
        prevGlidingFraction = glidingFraction;
        prevFlappingFraction = flappingFraction;
        prevHoveringFraction = hoveringFraction;

        updatePhysicsEnvelopes();
    }

    /**
     * Computes the flight mode for network sync
     * 0 = glide, 1 = flap/forward, 2 = hover, 3 = takeoff, -1 = ground/none
     */
    public int computeFlightModeForSync() {
        if (!dragon.isFlying()) {
            riderHighAltitudeGlide = false;
            return -1;
        }
        if (shouldPlayTakeoff()) {
            riderHighAltitudeGlide = false;
            return 3;
        }

        if (dragon.isHovering() || dragon.isLanding()) {
            riderHighAltitudeGlide = false;
            return 2;
        }

        // Check for ridden flight modes (sprint and fly_idle) before altitude-based logic
        if (isRiddenByOwner()) {
            // Track position changes manually (xo/yo/zo are synced before this is called)
            double deltaX = dragon.getX() - dragon.lastCheckedX;
            double deltaY = dragon.getY() - dragon.lastCheckedY;
            double deltaZ = dragon.getZ() - dragon.lastCheckedZ;
            double positionChangeSqr = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;

            boolean goingUp = dragon.isGoingUp();
            boolean goingDown = dragon.isGoingDown();
            boolean accelerating = dragon.isAccelerating();

            // Update position tracking and movement timer
            if (positionChangeSqr > 0.0001 || goingUp || goingDown || accelerating) {
                // Dragon is moving or player is giving directional input
                dragon.ticksSinceLastMovement = 0;
                dragon.lastCheckedX = dragon.getX();
                dragon.lastCheckedY = dragon.getY();
                dragon.lastCheckedZ = dragon.getZ();
            } else {
                // No movement detected
                dragon.ticksSinceLastMovement++;
            }

            // FLY_IDLE - stationary hover (only after being still for 3+ ticks)
            if (dragon.ticksSinceLastMovement > 3) {
                return 5; // FLY_IDLE
            }

            // SPRINT_FLAP - accelerating
            if (accelerating) {
                return 4; // SPRINT_FLAP
            }
        }

        double altitude = dragon.getY() - dragon.level().getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) dragon.getX(),
                (int) dragon.getZ());

        Vec3 velocity = dragon.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        boolean ascending = velocity.y > 0.02;
        boolean riderAscending = dragon.isVehicle() && dragon.isGoingUp();

        if (isRiddenByOwner()) {
            if (shouldForceSurfaceGlide(altitude)) {
                riderHighAltitudeGlide = false;
                return 0;
            }

            if (ascending || riderAscending) {
                return 1;
            }

            if (riderHighAltitudeGlide) {
                if (altitude > Ignivorus.RIDER_GLIDE_ALTITUDE_EXIT) {
                    return 0;
                }
                riderHighAltitudeGlide = false;
            } else if (altitude > Ignivorus.RIDER_GLIDE_ALTITUDE_THRESHOLD) {
                riderHighAltitudeGlide = true;
                return 0;
            }

            return 1;
        } else {
            riderHighAltitudeGlide = false;
        }

        // AI-controlled flight: use physics envelopes for smooth transitions
        if (ascending || riderAscending) {
            return 1; // Always flap when ascending
        }

        // Use physics envelope to determine flap vs glide
        // This provides natural transitions based on velocity, altitude, and mass
        float flapWeight = flappingFraction;
        float glideWeight = glidingFraction;

        // Hysteresis: require stronger evidence to switch states
        // This prevents rapid flickering between animations
        boolean shouldFlap = (flapWeight > 0.35f) || (glideWeight < 0.55f);

        return shouldFlap ? 1 : 0;
    }

    private boolean shouldPlayTakeoff() {
        int timeFlying = dragon.timeFlying;

        if (timeFlying < TAKEOFF_ANIM_EARLY_TICKS) return true;

        boolean airborne = !dragon.onGround();
        boolean ascending = dragon.getDeltaMovement().y > 0.05;

        return (timeFlying < TAKEOFF_ANIM_MAX_TICKS) && (airborne || ascending);
    }

    /**
     * Updates physics envelopes for smooth flap/glide transitions
     * Mirrors Raevyx's physics model but tuned for fire dragon (heavier, slightly less lift)
     */
    private void updatePhysicsEnvelopes() {
        Vec3 v = dragon.getDeltaMovement();
        float vH = (float)Math.hypot(v.x, v.z);
        float vY = (float)v.y;

        // Calculate lift from forward speed
        float glideLift = LIFT_K * vH * vH;

        // Calculate energy needed to climb
        float climbNeed = vY > 0 ? (vY * CLIMB_COST) : 0f;

        // Total energy need = mass + climb cost - available lift
        float need = MASS + climbNeed - glideLift;

        // Convert need to flap target (0 = can glide, 1 = must flap)
        float flapTarget = need <= 0 ? 0f : (need / (need + RESPONSE));
        flapTarget = Mth.clamp(flapTarget, 0f, 1f);

        // Hover state: near-stationary in air
        float hoverTarget = (
                dragon.isHovering() || dragon.isLanding() ||
                (vH < 0.02f && Math.abs(vY) < 0.02f)
        ) ? 1f : 0f;

        float glideTarget = Mth.clamp(1f - flapTarget, 0.15f, 1f);

        // Explicit ascent bias - climbing always triggers visible flaps
        if (dragon.isFlying()) {
            // Rider-controlled ascent
            if (dragon.getControllingPassenger() != null && dragon.isGoingUp()) {
                flapTarget = Math.max(flapTarget, 0.6f);
            }
            // AI/physics ascent: scale bias by vertical speed
            else if (vY > 0.06f) {
                float ascentBias = Mth.clamp((vY - 0.02f) * 3.0f, 0.2f, 0.8f);
                flapTarget = Math.max(flapTarget, ascentBias);
            }

            // Recompute glide target after bias
            glideTarget = Mth.clamp(1f - flapTarget, 0.15f, 1f);
        }

        // Apply envelope smoothing
        flapEnv.tickToward(flapTarget);
        hoverEnv.tickToward(hoverTarget);
        glideEnv.tickToward(glideTarget);

        // Update animation fractions
        glidingFraction = glideEnv.raw();
        flappingFraction = flapEnv.raw();
        hoveringFraction = hoverEnv.raw();
    }

    private boolean isRiddenByOwner() {
        if (!dragon.isTame() || !dragon.isVehicle()) {
            return false;
        }
        if (!(dragon.getControllingPassenger() instanceof Player player)) {
            return false;
        }
        return dragon.isOwnedBy(player);
    }

    private boolean shouldForceSurfaceGlide(double altitudeAboveTerrain) {
        return altitudeAboveTerrain <= Ignivorus.RIDER_LOW_ALTITUDE_GLIDE_THRESHOLD || isNearWaterSurface();
    }

    private boolean isNearWaterSurface() {
        if (dragon.level() == null) {
            return false;
        }

        double dragonY = dragon.getY();
        if (dragonY > Ignivorus.RIDER_WATER_SURFACE_LEVEL + Ignivorus.RIDER_WATER_SURFACE_TOLERANCE) {
            return false;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int baseX = Mth.floor(dragon.getX());
        int baseY = Mth.floor(dragonY);
        int baseZ = Mth.floor(dragon.getZ());

        for (int dx = -Ignivorus.RIDER_WATER_SCAN_RADIUS; dx <= Ignivorus.RIDER_WATER_SCAN_RADIUS; dx++) {
            for (int dz = -Ignivorus.RIDER_WATER_SCAN_RADIUS; dz <= Ignivorus.RIDER_WATER_SCAN_RADIUS; dz++) {
                for (int dy = 0; dy <= Ignivorus.RIDER_WATER_SCAN_DEPTH; dy++) {
                    cursor.set(baseX + dx, baseY - dy, baseZ + dz);
                    if (!dragon.level().hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockState state = dragon.level().getBlockState(cursor);
                    if (!state.getFluidState().isEmpty()) {
                        double surfaceY = cursor.getY() + 1.0;
                        if (Math.abs(dragonY - surfaceY) <= Ignivorus.RIDER_WATER_SURFACE_TOLERANCE) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    // ===== SAVE/LOAD SUPPORT =====

    /**
     * Save physics envelope state to NBT
     */
    public void writeToNBT(net.minecraft.nbt.CompoundTag tag) {
        // Store envelope values (authoritative for physics system)
        tag.putFloat("GlideVal", glideEnv.raw());
        tag.putFloat("FlapVal", flapEnv.raw());
        tag.putFloat("HoverVal", hoverEnv.raw());
    }

    /**
     * Load physics envelope state from NBT
     */
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
