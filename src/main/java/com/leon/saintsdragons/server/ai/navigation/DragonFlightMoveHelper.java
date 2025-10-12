package com.leon.saintsdragons.server.ai.navigation;

import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.util.DragonMathUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

/**
 * Generic flight movement controller - handles AI flight pathfinding for any type
 * Banking is handled elsewhere; MoveHelper focuses on movement only
 * Works with any entity that implements DragonFlightCapable interface
 */
public class DragonFlightMoveHelper extends MoveControl {
    private final DragonFlightCapable dragon;
    private final net.minecraft.world.entity.Mob mob;
    private float speedFactor = 1.0F;

    // Dragon-specific flight parameters
    private final float maxYawChange;
    private final float maxPitchChange;
    private final float speedFactorMin;
    private final float speedFactorMax;
    private final float speedTransitionRate;
    private final double accelerationCap;
    private final double velocityBlendRate;

    public DragonFlightMoveHelper(DragonFlightCapable dragon) {
        this(dragon, getDefaultParameters());
    }

    public DragonFlightMoveHelper(DragonFlightCapable dragon, FlightParameters params) {
        super((net.minecraft.world.entity.Mob) dragon);
        this.dragon = dragon;
        this.mob = (net.minecraft.world.entity.Mob) dragon;
        
        this.maxYawChange = params.maxYawChange;
        this.maxPitchChange = params.maxPitchChange;
        this.speedFactorMin = params.speedFactorMin;
        this.speedFactorMax = params.speedFactorMax;
        this.speedTransitionRate = params.speedTransitionRate;
        this.accelerationCap = params.accelerationCap;
        this.velocityBlendRate = params.velocityBlendRate;
    }

    // Default parameters for Lightning Dragon (fast, agile)
    private static FlightParameters getDefaultParameters() {
        return new FlightParameters(
            4.0F,    // maxYawChange
            8.0F,    // maxPitchChange
            0.5F,    // speedFactorMin
            3.2F,    // speedFactorMax
            0.15F,   // speedTransitionRate
            0.22D,   // accelerationCap
            0.16D    // velocityBlendRate
        );
    }

    // Flight parameters for different wyvern types
    public static class FlightParameters {
        public final float maxYawChange;
        public final float maxPitchChange;
        public final float speedFactorMin;
        public final float speedFactorMax;
        public final float speedTransitionRate;
        public final double accelerationCap;
        public final double velocityBlendRate;

        public FlightParameters(float maxYawChange, float maxPitchChange, float speedFactorMin, 
                              float speedFactorMax, float speedTransitionRate, double accelerationCap, 
                              double velocityBlendRate) {
            this.maxYawChange = maxYawChange;
            this.maxPitchChange = maxPitchChange;
            this.speedFactorMin = speedFactorMin;
            this.speedFactorMax = speedFactorMax;
            this.speedTransitionRate = speedTransitionRate;
            this.accelerationCap = accelerationCap;
            this.velocityBlendRate = velocityBlendRate;
        }
    }

    @Override
    public void tick() {
        if (this.operation != Operation.MOVE_TO) {
            return;
        }

        // Handle different flight modes
        if (dragon.isHovering() || dragon.isLanding()) {
            handleHoveringMovement();
        } else {
            handleGlidingMovement();
        }
    }

    /**
     * Gliding movement - this is where the magic happens
     */
    private void handleGlidingMovement() {
        // Collision handling - simple 180 turn
        if (mob.horizontalCollision) {
            mob.setYRot(mob.getYRot() + 180.0F);
            this.speedFactor = speedFactorMin;
            mob.getNavigation().stop();
            return;
        }

        // Calculate movement vectors to target
        float distX = (float) (this.wantedX - mob.getX());
        float distY = (float) (this.wantedY - mob.getY());
        float distZ = (float) (this.wantedZ - mob.getZ());

        // Reduce Y influence on horizontal movement (guard against division by zero)
        double horizontalDist = Math.sqrt(distX * distX + distZ * distZ);
        if (horizontalDist > 1.0e-6) {
            double yFractionReduction = 1.0D - (double) Mth.abs(distY * 0.7F) / horizontalDist;
            distX = (float) (distX * yFractionReduction);
            distZ = (float) (distZ * yFractionReduction);
            horizontalDist = Math.sqrt(distX * distX + distZ * distZ);
        }
        double totalDist = Math.sqrt(distX * distX + distZ * distZ + distY * distY);
        if (totalDist < 1.0e-6) {
            // We're essentially at the target; stop moving
            this.operation = Operation.WAIT;
            return;
        }

        // === YAW CALCULATION ===
        float currentYaw = mob.getYRot();
        float desiredYaw = (float) Mth.atan2(distZ, distX) * 57.295776F; // Convert to degrees

        // Smooth yaw approach
        float wrappedCurrentYaw = Mth.wrapDegrees(currentYaw + 90.0F);
        float wrappedDesiredYaw = Mth.wrapDegrees(desiredYaw);
        mob.setYRot(Mth.approachDegrees(wrappedCurrentYaw, wrappedDesiredYaw, maxYawChange) - 90.0F);

        // Banking handled in animation/predicate; keep MoveHelper focused on movement
        // MoveHelper only handles movement - no banking calculation needed

        // Body rotation follows head
        mob.yBodyRot = mob.getYRot();

        // === PITCH CALCULATION ===
        float desiredPitch = (float) (-(Mth.atan2(-distY, horizontalDist) * 57.295776F));
        mob.setXRot(Mth.approachDegrees(mob.getXRot(), desiredPitch, maxPitchChange));

        // === ENHANCED SPEED MODULATION ===
        float yawDifference = Math.abs(Mth.wrapDegrees(mob.getYRot() - currentYaw));

        // Base speed factor adjustments
        float targetSpeedFactor;
        if (yawDifference < 3.0F) {
            // Facing right direction - speed up
            targetSpeedFactor = speedFactorMax;
        } else {
            // Turning - slow down based on turn severity using yaw difference
            float turnSeverity = Mth.clamp(yawDifference / 15.0f, 0.0f, 1.0f); // Normalize to 0-1
            targetSpeedFactor = DragonMathUtil.lerpSmooth(0.6f, speedFactorMax, 1.0f - turnSeverity,
                    DragonMathUtil.EasingFunction.EASE_OUT_SINE);
        }

        // Distance-based speed scaling (IaF-inspired): keep speed up at range, ease gently near goal
        float distScale = Mth.clamp((float) (totalDist / 45.0) + 0.35f, 0.35f, 1.0f);
        targetSpeedFactor *= distScale;

        // Combat bias: fly a bit faster when we have a live target and are airborne
        var target = mob.getTarget();
        if (dragon.isFlying() && target != null && target.isAlive()) {
            targetSpeedFactor *= 1.12f; // modest global boost in combat
        }

        // If straight path to wanted position is obstructed by blocks, damp speed to avoid wall pushing
        if (isLineObstructed(mob.position(), new Vec3(this.wantedX, this.wantedY, this.wantedZ))) {
            targetSpeedFactor *= 0.5f;
        }

        this.speedFactor = Mth.clamp(Mth.approach(this.speedFactor, targetSpeedFactor, speedTransitionRate),
                speedFactorMin, speedFactorMax); // Dragon-specific speed transitions with clamping

        // === 3D MOVEMENT APPLICATION (robust, normalized toward target) ===
        Vec3 dir = new Vec3(distX, distY, distZ).scale(1.0 / totalDist); // normalized
        Vec3 motion = mob.getDeltaMovement();
        Vec3 targetVel = dir.scale(this.speedFactor);
        // Blend toward target velocity with per-axis acceleration cap to reduce twitch/overshoot
        Vec3 delta = targetVel.subtract(motion).scale(velocityBlendRate); // wyvern-specific blend rate
        double accelCap = accelerationCap;
        // Additional dampening when obstructed
        if (isLineObstructed(mob.position(), new Vec3(this.wantedX, this.wantedY, this.wantedZ))) {
            accelCap *= 0.6D;
            delta = delta.scale(0.6D);
        }
        delta = clampPerAxis(delta, accelCap);
        Vec3 blended = motion.add(delta);
        mob.setDeltaMovement(blended);
    }

    /**
     * Hovering movement - simpler, more direct control
     */
    private void handleHoveringMovement() {
        // Look at target if we have one - use smooth looking with deadzone to avoid jitter
        var target = mob.getTarget();
        if (target != null && mob.distanceToSqr(target) < 1600.0D) {
            float yawErr = DragonMathUtil.yawErrorToTarget(mob, target);
            if (yawErr > 4.0f) {
                DragonMathUtil.smoothLookAt(mob, target, 10.0f, 10.0f);
            } // else: within deadzone, do not adjust this tick
        }

        if (this.operation == Operation.MOVE_TO) {
            Vec3 targetVec = new Vec3(
                    this.wantedX - mob.getX(),
                    this.wantedY - mob.getY(),
                    this.wantedZ - mob.getZ()
            );
            double distance = targetVec.length();
            targetVec = targetVec.normalize();

            // Simple collision check for hovering
            if (checkCollisions(targetVec, Mth.ceil(distance))) {
                mob.setDeltaMovement(mob.getDeltaMovement().add(targetVec.scale(0.1D)));
            } else {
                this.operation = Operation.WAIT;
            }
        }
    }

    /**
     * Collision checking for hovering mode
     */
    private boolean checkCollisions(Vec3 direction, int steps) {
        var boundingBox = mob.getBoundingBox();
        for (int i = 1; i < steps; ++i) {
            boundingBox = boundingBox.move(direction);
            if (!mob.level().noCollision(mob, boundingBox)) {
                return false;
            }
        }
        return true;
    }

    private static Vec3 clampPerAxis(Vec3 v, double cap) {
        double cx = Mth.clamp(v.x, -cap, cap);
        double cy = Mth.clamp(v.y, -cap, cap);
        double cz = Mth.clamp(v.z, -cap, cap);
        return new Vec3(cx, cy, cz);
    }

    private boolean isLineObstructed(Vec3 from, Vec3 to) {
        HitResult hit = mob.level().clip(new ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mob
        ));
        return hit.getType() != HitResult.Type.MISS;
    }

    public boolean hasGivenUp() {
        return this.operation == Operation.WAIT;
    }
}
