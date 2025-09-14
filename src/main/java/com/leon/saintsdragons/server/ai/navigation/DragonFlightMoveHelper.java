package com.leon.saintsdragons.server.ai.navigation;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.leon.saintsdragons.util.DragonMathUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

/**
 * Dragon flight movement controller - handles AI flight pathfinding
 * Banking is handled elsewhere; MoveHelper focuses on movement only
 */
public class DragonFlightMoveHelper extends MoveControl {
    private final LightningDragonEntity dragon;
    private float speedFactor = 1.0F;

    // Constants for smooth movement
    private static final float MAX_YAW_CHANGE = 4.0F;
    private static final float MAX_PITCH_CHANGE = 8.0F;
    private static final float SPEED_FACTOR_MIN = 0.5F;
    private static final float SPEED_FACTOR_MAX = 3.2F; // Higher ceiling for snappier flight

    public DragonFlightMoveHelper(LightningDragonEntity dragon) {
        super(dragon);
        this.dragon = dragon;
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
        if (dragon.horizontalCollision) {
            dragon.setYRot(dragon.getYRot() + 180.0F);
            this.speedFactor = SPEED_FACTOR_MIN;
            dragon.getNavigation().stop();
            return;
        }

        // Calculate movement vectors to target
        float distX = (float) (this.wantedX - dragon.getX());
        float distY = (float) (this.wantedY - dragon.getY());
        float distZ = (float) (this.wantedZ - dragon.getZ());

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
        float currentYaw = dragon.getYRot();
        float desiredYaw = (float) Mth.atan2(distZ, distX) * 57.295776F; // Convert to degrees

        // Smooth yaw approach
        float wrappedCurrentYaw = Mth.wrapDegrees(currentYaw + 90.0F);
        float wrappedDesiredYaw = Mth.wrapDegrees(desiredYaw);
        dragon.setYRot(Mth.approachDegrees(wrappedCurrentYaw, wrappedDesiredYaw, MAX_YAW_CHANGE) - 90.0F);

        // Banking handled in animation/predicate; keep MoveHelper focused on movement
        // MoveHelper only handles movement - no banking calculation needed

        // Body rotation follows head
        dragon.yBodyRot = dragon.getYRot();

        // === PITCH CALCULATION ===
        float desiredPitch = (float) (-(Mth.atan2(-distY, horizontalDist) * 57.295776F));
        dragon.setXRot(Mth.approachDegrees(dragon.getXRot(), desiredPitch, MAX_PITCH_CHANGE));

        // === ENHANCED SPEED MODULATION ===
        float yawDifference = Math.abs(Mth.wrapDegrees(dragon.getYRot() - currentYaw));

        // Base speed factor adjustments
        float targetSpeedFactor;
        if (yawDifference < 3.0F) {
            // Facing right direction - speed up
            targetSpeedFactor = SPEED_FACTOR_MAX;
        } else {
            // Turning - slow down based on turn severity using yaw difference
            float turnSeverity = Mth.clamp(yawDifference / 15.0f, 0.0f, 1.0f); // Normalize to 0-1
            targetSpeedFactor = DragonMathUtil.lerpSmooth(0.6f, SPEED_FACTOR_MAX, 1.0f - turnSeverity,
                    DragonMathUtil.EasingFunction.EASE_OUT_SINE);
        }

        // Distance-based speed scaling (IaF-inspired): keep speed up at range, ease gently near goal
        float distScale = Mth.clamp((float) (totalDist / 45.0) + 0.35f, 0.35f, 1.0f);
        targetSpeedFactor *= distScale;

        // Combat bias: fly a bit faster when we have a live target and are airborne
        if (dragon.isFlying() && dragon.getTarget() != null && dragon.getTarget().isAlive()) {
            targetSpeedFactor *= 1.12f; // modest global boost in combat
        }

        // If straight path to wanted position is obstructed by blocks, damp speed to avoid wall pushing
        if (isLineObstructed(dragon.position(), new Vec3(this.wantedX, this.wantedY, this.wantedZ))) {
            targetSpeedFactor *= 0.5f;
        }

        this.speedFactor = Mth.clamp(Mth.approach(this.speedFactor, targetSpeedFactor, 0.15F),
                SPEED_FACTOR_MIN, SPEED_FACTOR_MAX); // Faster speed transitions with clamping

        // === 3D MOVEMENT APPLICATION (robust, normalized toward target) ===
        Vec3 dir = new Vec3(distX, distY, distZ).scale(1.0 / totalDist); // normalized
        Vec3 motion = dragon.getDeltaMovement();
        Vec3 targetVel = dir.scale(this.speedFactor);
        // Blend toward target velocity with per-axis acceleration cap to reduce twitch/overshoot
        Vec3 delta = targetVel.subtract(motion).scale(0.16D); // stronger blend toward target velocity
        double accelCap = 0.22D;
        // Additional dampening when obstructed
        if (isLineObstructed(dragon.position(), new Vec3(this.wantedX, this.wantedY, this.wantedZ))) {
            accelCap *= 0.6D;
            delta = delta.scale(0.6D);
        }
        delta = clampPerAxis(delta, accelCap);
        Vec3 blended = motion.add(delta);
        dragon.setDeltaMovement(blended);
    }

    /**
     * Hovering movement - simpler, more direct control
     */
    private void handleHoveringMovement() {
        // Look at target if we have one - use smooth looking with deadzone to avoid jitter
        if (dragon.getTarget() != null && dragon.distanceToSqr(dragon.getTarget()) < 1600.0D) {
            var tgt = dragon.getTarget();
            float yawErr = DragonMathUtil.yawErrorToTarget(dragon, tgt);
            if (yawErr > 4.0f) {
                DragonMathUtil.smoothLookAt(dragon, tgt, 10.0f, 10.0f);
            } // else: within deadzone, do not adjust this tick
        }

        if (this.operation == Operation.MOVE_TO) {
            Vec3 targetVec = new Vec3(
                    this.wantedX - dragon.getX(),
                    this.wantedY - dragon.getY(),
                    this.wantedZ - dragon.getZ()
            );
            double distance = targetVec.length();
            targetVec = targetVec.normalize();

            // Simple collision check for hovering
            if (checkCollisions(targetVec, Mth.ceil(distance))) {
                dragon.setDeltaMovement(dragon.getDeltaMovement().add(targetVec.scale(0.1D)));
            } else {
                this.operation = Operation.WAIT;
            }
        }
    }

    /**
     * Collision checking for hovering mode
     */
    private boolean checkCollisions(Vec3 direction, int steps) {
        var boundingBox = dragon.getBoundingBox();
        for (int i = 1; i < steps; ++i) {
            boundingBox = boundingBox.move(direction);
            if (!dragon.level().noCollision(dragon, boundingBox)) {
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
        HitResult hit = dragon.level().clip(new ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                dragon
        ));
        return hit.getType() != HitResult.Type.MISS;
    }

    public boolean hasGivenUp() {
        return this.operation == Operation.WAIT;
    }
}
