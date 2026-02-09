package com.leon.saintsdragons.server.ai.navigation;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
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
    private static final String WILD_FLYING_SPEED_MULTIPLIER_KEY = "wild_flying_speed_multiplier";
    private static final float COLLISION_AVOID_YAW_STEP = 42.0F;
    private static final int COLLISION_RECOVERY_WINDOW_TICKS = 14;
    private static final float COLLISION_RECOVERY_SPEED_CAP = 1.05F;
    private static final float COLLISION_RECOVERY_YAW_STEP_CAP = 2.5F;
    private static final float COLLISION_RECOVERY_PITCH_MAX_DOWN = 12.0F;
    private static final double COLLISION_FORWARD_SAMPLE_DISTANCE = 4.0D;
    private static final int ESCAPE_MODE_TICKS = 14;
    private static final int COLLISION_WINDOW_TICKS = 20;
    private static final int ESCAPE_TRIGGER_COLLISIONS = 4;
    private static final int REPATHE_TRIGGER_COLLISIONS = 7;
    private static final double ESCAPE_SIDE_DISTANCE = 8.0D;
    private static final double ESCAPE_FORWARD_DISTANCE = 5.0D;
    private static final double ESCAPE_UP_DISTANCE = 3.0D;

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

    // Performance optimization: cache obstruction check
    private int obstructionCheckCooldown = 0;
    private boolean cachedObstructionResult = false;
    private int collisionRecoveryTicks = 0;
    private int collisionWindowTicks = 0;
    private int collisionHitsInWindow = 0;
    private int lastAvoidTurnDirection = 1;
    private int escapeTicks = 0;
    private Vec3 escapeTarget = null;

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

    // Flight parameters for different types
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

        // Landing should never be treated as stationary hover movement.
        // Goals decide WHEN to land; MoveHelper just ensures motion follows that intent.
        if (dragon.isLanding()) {
            handleGlidingMovement();
        } else if (dragon.isHovering()) {
            handleHoveringMovement();
        } else {
            handleGlidingMovement();
        }
    }

    /**
     * Gliding movement - this is where the magic happens
     */
    private void handleGlidingMovement() {
        // Collision handling - avoid repeated yaw flips that cause visual wobble
        if (mob.horizontalCollision) {
            collisionWindowTicks = COLLISION_WINDOW_TICKS;
            collisionHitsInWindow++;
            if (collisionRecoveryTicks <= 0) {
                int avoidDirection = chooseAvoidTurnDirection();
                float avoidYaw = mob.getYRot() + avoidDirection * COLLISION_AVOID_YAW_STEP;
                mob.setYRot(avoidYaw);
                mob.yBodyRot = avoidYaw;
                mob.getNavigation().stop();
                collisionRecoveryTicks = COLLISION_RECOVERY_WINDOW_TICKS;
            } else {
                collisionRecoveryTicks--;
            }

            this.speedFactor = Mth.clamp(
                    Math.min(COLLISION_RECOVERY_SPEED_CAP, this.speedFactor * 0.68F),
                    Math.max(0.35F, speedFactorMin * 0.7F),
                    COLLISION_RECOVERY_SPEED_CAP
            );
            if (collisionHitsInWindow >= ESCAPE_TRIGGER_COLLISIONS && escapeTicks <= 0) {
                activateEscapeMode();
            }
            if (collisionHitsInWindow >= REPATHE_TRIGGER_COLLISIONS) {
                this.operation = Operation.WAIT;
                mob.getNavigation().stop();
                collisionRecoveryTicks = 0;
                collisionWindowTicks = 0;
                collisionHitsInWindow = 0;
                escapeTicks = 0;
                escapeTarget = null;
                return;
            }
            Vec3 motion = mob.getDeltaMovement();
            double lift = dragon.isTakeoff() ? 0.16D : (collisionHitsInWindow >= ESCAPE_TRIGGER_COLLISIONS ? 0.14D : 0.10D);
            mob.setDeltaMovement(motion.x * 0.35D, Math.max(motion.y, lift), motion.z * 0.35D);
            return;
        }

        if (collisionWindowTicks > 0) {
            collisionWindowTicks--;
            if (collisionWindowTicks == 0) {
                collisionHitsInWindow = 0;
            }
        }
        if (escapeTicks > 0) {
            escapeTicks--;
            if (escapeTicks <= 0) {
                escapeTarget = null;
            }
        }
        if (collisionRecoveryTicks > 0) {
            collisionRecoveryTicks--;
        }

        // Calculate movement vectors to target
        Vec3 wantedPos = (escapeTicks > 0 && escapeTarget != null) ? escapeTarget : new Vec3(this.wantedX, this.wantedY, this.wantedZ);
        float distX = (float) (wantedPos.x - mob.getX());
        float distY = (float) (wantedPos.y - mob.getY());
        float distZ = (float) (wantedPos.z - mob.getZ());

        // Performance optimization: check for obstruction once every 5 ticks instead of twice per tick
        // This reduces expensive raycasting from ~64 calls/second to ~4 calls/second per dragon
        if (obstructionCheckCooldown <= 0) {
            cachedObstructionResult = isBodyPathObstructed(mob.position(), wantedPos);
            obstructionCheckCooldown = 5; // Check every 5 ticks (0.25 seconds)
        } else {
            obstructionCheckCooldown--;
        }

        // Reduce Y influence on horizontal movement (guard against division by zero)
        double horizontalDist = Math.sqrt(distX * distX + distZ * distZ);
        if (horizontalDist > 1.0e-6) {
            double yFractionReduction = 1.0D - (double) Mth.abs(distY * 0.7F) / horizontalDist;
            distX = (float) (distX * yFractionReduction);
            distZ = (float) (distZ * yFractionReduction);
            horizontalDist = Math.sqrt(distX * distX + distZ * distZ);
        }
        double totalDist = Math.sqrt(distX * distX + distZ * distZ + distY * distY);

        // Use bounding box diagonal as threshold, with minimum of 5 blocks for smaller dragons
        double hoverThreshold = Math.max(5.0, mob.getBoundingBox().getSize());
        if (totalDist < hoverThreshold) {
            this.operation = Operation.WAIT;
            // Gradually slow down instead of instant stop
            mob.setDeltaMovement(mob.getDeltaMovement().scale(0.75));
            return;
        }

        // === YAW CALCULATION ===
        float currentYaw = mob.getYRot();
        float desiredYaw = (float) Mth.atan2(distZ, distX) * 57.295776F; // Convert to degrees

        // Smooth yaw approach
        float wrappedCurrentYaw = Mth.wrapDegrees(currentYaw + 90.0F);
        float wrappedDesiredYaw = Mth.wrapDegrees(desiredYaw);
        float yawStep = collisionRecoveryTicks > 0
                ? Math.min(maxYawChange, COLLISION_RECOVERY_YAW_STEP_CAP)
                : maxYawChange;
        mob.setYRot(Mth.approachDegrees(wrappedCurrentYaw, wrappedDesiredYaw, yawStep) - 90.0F);

        // Banking handled in animation/predicate; keep MoveHelper focused on movement
        // MoveHelper only handles movement - no banking calculation needed

        // Body rotation follows head
        mob.yBodyRot = mob.getYRot();

        // === PITCH CALCULATION ===
        float desiredPitch = (float) (-(Mth.atan2(-distY, horizontalDist) * 57.295776F));
        if (collisionRecoveryTicks > 0) {
            desiredPitch = Math.min(desiredPitch, COLLISION_RECOVERY_PITCH_MAX_DOWN);
        }
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
        targetSpeedFactor *= getWildFlyingSpeedMultiplier();

        // Combat bias: fly a bit faster when we have a live target and are airborne
        var target = mob.getTarget();
        boolean isFlying = (dragon instanceof com.leon.saintsdragons.server.entity.base.DragonEntity baseDragon)
                && baseDragon.isFlying();
        if (isFlying && target != null && target.isAlive()) {
            targetSpeedFactor *= 1.12f; // modest global boost in combat
        }

        // If straight path to wanted position is obstructed by blocks, damp speed to avoid wall pushing
        if (cachedObstructionResult) {
            targetSpeedFactor *= 0.5f;
        }
        if (collisionRecoveryTicks > 0) {
            targetSpeedFactor = Math.min(targetSpeedFactor, COLLISION_RECOVERY_SPEED_CAP);
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
        // Additional dampening when obstructed (use cached result)
        if (cachedObstructionResult) {
            accelCap *= 0.6D;
            delta = delta.scale(0.6D);
        }
        delta = clampPerAxis(delta, accelCap);
        Vec3 blended = motion.add(delta);
        mob.setDeltaMovement(blended);
    }

    private float getWildFlyingSpeedMultiplier() {
        if (!(mob instanceof DragonEntity dragonEntity) || dragonEntity.isTame()) {
            return 1.0F;
        }

        DragonAttributeConfig config;
        if (mob instanceof Cindervane) {
            config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.CINDERVANE_ID);
        } else if (mob instanceof Raevyx) {
            config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        } else if (mob instanceof Ignivorus) {
            config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        } else {
            return 1.0F;
        }

        double multiplier = config.extraDouble(WILD_FLYING_SPEED_MULTIPLIER_KEY, 1.0D);
        return (float) Mth.clamp(multiplier, 0.05D, 10.0D);
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

    private int chooseAvoidTurnDirection() {
        Vec3 from = mob.position().add(0.0D, mob.getBbHeight() * 0.5D, 0.0D);
        Vec3 leftDir = Vec3.directionFromRotation(0.0F, mob.getYRot() + COLLISION_AVOID_YAW_STEP);
        Vec3 rightDir = Vec3.directionFromRotation(0.0F, mob.getYRot() - COLLISION_AVOID_YAW_STEP);

        boolean leftBlocked = isBodyPathObstructed(from, from.add(leftDir.scale(COLLISION_FORWARD_SAMPLE_DISTANCE)));
        boolean rightBlocked = isBodyPathObstructed(from, from.add(rightDir.scale(COLLISION_FORWARD_SAMPLE_DISTANCE)));

        int chosen;
        if (leftBlocked != rightBlocked) {
            chosen = leftBlocked ? -1 : 1;
        } else {
            chosen = -lastAvoidTurnDirection;
        }
        lastAvoidTurnDirection = chosen;
        return chosen;
    }

    private void activateEscapeMode() {
        int direction = chooseAvoidTurnDirection();
        Vec3 side = Vec3.directionFromRotation(0.0F, mob.getYRot() + direction * 90.0F).normalize();
        Vec3 forward = Vec3.directionFromRotation(0.0F, mob.getYRot()).normalize();
        Vec3 base = mob.position();
        escapeTarget = base.add(side.scale(ESCAPE_SIDE_DISTANCE))
                .add(forward.scale(ESCAPE_FORWARD_DISTANCE))
                .add(0.0D, ESCAPE_UP_DISTANCE, 0.0D);
        escapeTicks = ESCAPE_MODE_TICKS;
    }

    private boolean isBodyPathObstructed(Vec3 from, Vec3 to) {
        Vec3 flat = new Vec3(to.x - from.x, 0.0D, to.z - from.z);
        Vec3 side = flat.lengthSqr() > 1.0e-6
                ? new Vec3(-flat.z, 0.0D, flat.x).normalize()
                : Vec3.ZERO;
        double halfWidth = mob.getBbWidth() * 0.45D + 0.2D;
        Vec3 midOffset = new Vec3(0.0D, mob.getBbHeight() * 0.5D, 0.0D);

        Vec3 fromMid = from.add(midOffset);
        Vec3 toMid = to.add(midOffset);
        if (isLineObstructed(fromMid, toMid)) {
            return true;
        }
        if (side.lengthSqr() <= 1.0e-6) {
            return false;
        }

        Vec3 lateral = side.scale(halfWidth);
        return isLineObstructed(fromMid.add(lateral), toMid.add(lateral))
                || isLineObstructed(fromMid.subtract(lateral), toMid.subtract(lateral));
    }
}
