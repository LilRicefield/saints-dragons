package com.leon.saintsdragons.server.entity.controller.lightningdragon;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * Handles all flight-related logic for Raevyx including:
 * - Takeoff and landing mechanics
 * - Flight physics and navigation switching
 * - Banking and pitch control
 * - Gliding and hovering behavior
 */
public class RaevyxFlightController {
    private final Raevyx wyvern;
    private net.minecraft.core.BlockPos plannedLandingPos = null;

    // Flight constants
    private static final double TAKEOFF_UPWARD_FORCE = 0.11D;
    private static final double LANDING_DOWNWARD_FORCE = 0.4D;
    private static final double FALLING_RESISTANCE = 0.6D;
    private static final int TAKEOFF_TIME_THRESHOLD = 30;
    private static final int LANDING_TIME_THRESHOLD = 40;

    public RaevyxFlightController(Raevyx wyvern) {
        this.wyvern = wyvern;
    }

    /**
     * Main flight logic handler called every tick
     */
    public void handleFlightLogic() {
        if (wyvern.isFlying()) {
            handleFlyingTick();
        } else {
            handleGroundedTick();
        }

        if (wyvern.isLanding()) {
            handleSimpleLanding();
        }
    }

    /**
     * Switches wyvern to ground navigation mode
     */
    public void switchToGroundNavigation() {
        wyvern.switchToGroundNavigation();
    }

    /**
     * Plan and start a smart landing by selecting a safe nearby flat spot and
     * steering navigation toward it. Called when landing is initiated.
     */
    public void planSmartLanding() {
        if (wyvern.level().isClientSide) return;
        // Prefer owner's vicinity if reasonably close
        net.minecraft.world.entity.LivingEntity owner = wyvern.getOwner();
        net.minecraft.core.BlockPos center;
        if (owner != null && owner.level() == wyvern.level() && wyvern.distanceTo(owner) < 40.0) {
            center = owner.blockPosition();
        } else {
            center = wyvern.blockPosition();
        }

        plannedLandingPos = findSafeLandingSpot(center);
        if (plannedLandingPos != null) {
            // Aim slightly above ground and move toward it
            wyvern.getNavigation().moveTo(
                    plannedLandingPos.getX() + 0.5,
                    plannedLandingPos.getY() + 0.5,
                    plannedLandingPos.getZ() + 0.5,
                    1.2
            );
            wyvern.setHovering(true);
        }
    }

    private net.minecraft.core.BlockPos findSafeLandingSpot(BlockPos center) {
        var level = wyvern.level();
        // Spiral/ring search: radii 2..radius, angle steps ~16
        for (int r = 2; r <= 10; r += 2) {
            for (int step = 0; step < 16; step++) {
                double ang = (Math.PI * 2.0) * (step / 16.0);
                int x = center.getX() + (int) Math.round(Math.cos(ang) * r);
                int z = center.getZ() + (int) Math.round(Math.sin(ang) * r);
                // Probe vertical: find first solid below with air headroom
                int startY = center.getY() + 6; // allow slight uphill landing
                int minY = center.getY() - 8;
                for (int y = startY; y >= minY; y--) {
                    var pos = new net.minecraft.core.BlockPos(x, y, z);
                    var below = pos.below();
                    var bsBelow = level.getBlockState(below);
                    var bsAt = level.getBlockState(pos);
                    boolean solidBelow = !bsBelow.isAir() && !bsBelow.getCollisionShape(level, below).isEmpty();
                    boolean freeAt = bsAt.getCollisionShape(level, pos).isEmpty();
                    boolean fluidOk = bsAt.getFluidState().isEmpty();
                    if (solidBelow && freeAt && fluidOk && isFlatEnough(pos)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private boolean isFlatEnough(net.minecraft.core.BlockPos pos) {
        // Check neighboring heights are within 1 block
        var level = wyvern.level();
        int y = pos.getY();
        int[] dx = {1, -1, 0, 0};
        int[] dz = {0, 0, 1, -1};
        for (int i = 0; i < 4; i++) {
            var p = pos.offset(dx[i], 0, dz[i]);
            int ny = surfaceYAt(p);
            if (Math.abs(ny - y) > 1) return false;
            // Avoid leaves/water/lava as landing base
            var below = new net.minecraft.core.BlockPos(p.getX(), ny - 1, p.getZ());
            var st = level.getBlockState(below);
            if (!st.getFluidState().isEmpty()) return false;
        }
        return true;
    }

    private int surfaceYAt(net.minecraft.core.BlockPos pos) {
        var level = wyvern.level();
        int y = pos.getY();
        for (int dy = 0; dy <= 6; dy++) {
            var p = new net.minecraft.core.BlockPos(pos.getX(), y - dy, pos.getZ());
            var below = p.below();
            var bsBelow = level.getBlockState(below);
            var bsAt = level.getBlockState(p);
            if (!bsBelow.isAir() && !bsBelow.getCollisionShape(level, below).isEmpty() && bsAt.getCollisionShape(level, p).isEmpty()) {
                return p.getY();
            }
        }
        return y;
    }
    /**
     * Forces aggressive landing for combat situations
     */
    public void initiateAggressiveLanding() {
        if (!wyvern.isFlying()) return;

        wyvern.setLanding(true);
        wyvern.setFlying(false);
        wyvern.setTakeoff(false);
        wyvern.setHovering(false);
        wyvern.setRunning(true);
        wyvern.getNavigation().stop();
        wyvern.setHovering(true);
        wyvern.landingTimer = 0;
    }

    /**
     * Handles flight travel behavior based on current flight state
     */
    public void handleFlightTravel(Vec3 motion) {
        if (wyvern.isTakeoff() || wyvern.isHovering()) {
            handleHoveringTravel(motion);
        } else {
            handleGlidingTravel(motion);
        }
    }

    private void handleFlyingTick() {
        wyvern.timeFlying++;

        // Reduce falling speed while flying
        if (wyvern.getDeltaMovement().y < 0 && wyvern.isAlive()) {
            wyvern.setDeltaMovement(wyvern.getDeltaMovement().multiply(1, FALLING_RESISTANCE, 1));
        }

        // Auto-land when sitting or being a passenger
        if (wyvern.isOrderedToSit() || wyvern.isPassenger()) {
            wyvern.setTakeoff(false);
            wyvern.setHovering(false);
            wyvern.setFlying(false);
            return;
        }

        // Server-side logic
        if (!wyvern.level().isClientSide) {
            handleServerFlightLogic();
            handleFlightPitchControl();
        }

        // Apply post-logic stabilizer on both sides to reduce visible jitter while hovering/aiming
        applyHoverStabilizer();
    }

    private void handleServerFlightLogic() {
        // Update takeoff state
        wyvern.setTakeoff(shouldTakeoff() && wyvern.isFlying());

        // Handle takeoff physics
        if (wyvern.isTakeoff() && wyvern.isFlying() && wyvern.isAlive()) {
            // Apply upward force during initial takeoff window
            if (wyvern.timeFlying < TAKEOFF_TIME_THRESHOLD) {
                wyvern.setDeltaMovement(wyvern.getDeltaMovement().add(0, TAKEOFF_UPWARD_FORCE, 0));
            }
            if (wyvern.landingFlag) {
                wyvern.setDeltaMovement(wyvern.getDeltaMovement().add(0, -LANDING_DOWNWARD_FORCE, 0));
            }
        }

        // Landing logic when touching ground - fixed timing
        if (!wyvern.isTakeoff() && wyvern.isFlying() && wyvern.timeFlying > LANDING_TIME_THRESHOLD && wyvern.onGround()) {
            LivingEntity target = wyvern.getTarget();
            // Allow natural ground transition when no target, OR when being ridden (simple ground contact)
            if (target == null || !target.isAlive() || wyvern.isVehicle()) {
                wyvern.setFlying(false);
            }
        }

        // Nudge toward planned landing if any - use desynced checks for navigation refresh
        if (wyvern.isLanding() && plannedLandingPos != null) {
            // Refresh target in case navigation has stopped (check periodically)
            if (wyvern.tickCount % 5 == 0 && wyvern.getNavigation().isDone()) {
                wyvern.getNavigation().moveTo(plannedLandingPos.getX() + 0.5, plannedLandingPos.getY() + 0.5, plannedLandingPos.getZ() + 0.5, 1.1);
            }
            // Clear plan once grounded
            if (wyvern.onGround()) {
                plannedLandingPos = null;
            }
        }
    }

    /**
     * Reduce small oscillations while hovering or beaming by adding a velocity deadzone
     * and a small angular deadzone around the target lock.
     */
    private void applyHoverStabilizer() {
        if (!wyvern.isFlying()) return;
        // Do not interfere with rider control
        if (wyvern.getControllingPassenger() != null) return;

        boolean hoveringLike = wyvern.isHovering() || wyvern.isTakeoff() || wyvern.isBeaming();
        if (!hoveringLike) return;

        // Velocity deadzone for XY drift
        Vec3 v = wyvern.getDeltaMovement();
        double h = Math.sqrt(v.x * v.x + v.z * v.z);
        if (h < 0.03) {
            // Snap tiny drift to zero
            wyvern.setDeltaMovement(0.0, v.y * 0.98, 0.0);
        } else {
            // Heavier horizontal damping while hovering/beaming
            wyvern.setDeltaMovement(v.x * 0.85, v.y * 0.98, v.z * 0.85);
        }

        // Angular jitter is handled via deadzones in MoveHelper/beam logic; avoid forcibly snapping here
    }

    private void handleGroundedTick() {
        wyvern.timeFlying = 0;
    }

    public boolean shouldTakeoff() {
        // Do not auto-takeoff just because there is a target; prefer ground combat if viable
        if (wyvern.getControllingPassenger() != null) {
            // When ridden, honor an explicit rider takeoff window
            return wyvern.getRiderTakeoffTicks() > 0;
        }

        final var target = wyvern.getTarget();
        if (target != null && target.isAlive()) {
            double d = wyvern.distanceTo(target);
            boolean targetFarAway = d > 20.0;
            boolean targetAbove = target.getY() - wyvern.getY() > 5.0;
            boolean cantReachOnGround = d > 10.0 && !wyvern.getSensing().hasLineOfSight(target);
            return wyvern.landingFlag || targetFarAway || targetAbove || cantReachOnGround;
        }

        // During initial takeoff sequence, allow upward force for a short period
        return wyvern.landingFlag || wyvern.timeFlying < TAKEOFF_TIME_THRESHOLD;
    }

    private void handleFlightPitchControl() {
        // Authority separation: when ridden, rider fully controls pitch. Avoid server auto-correct.
        if (wyvern.getControllingPassenger() != null) return;
        if (!wyvern.isFlying() || wyvern.isLanding() || wyvern.isHovering()) return;

        Vec3 velocity = wyvern.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        if (horizontalSpeed > 0.05) {
            float desiredPitch = (float) (Math.atan2(-velocity.y, horizontalSpeed) * 57.295776F);
            desiredPitch = Mth.clamp(desiredPitch, -25.0F, 35.0F);
            wyvern.setXRot(Mth.approachDegrees(wyvern.getXRot(), desiredPitch, 3.0F));
        }
    }

    private void handleSimpleLanding() {
        if (!wyvern.level().isClientSide) {
            wyvern.landingTimer++;

            // Complete landing after a fixed duration or on touching ground
            int landingTime = 60; // ticks
            if (wyvern.landingTimer > landingTime || wyvern.onGround()) {
                wyvern.setLanding(false);
                wyvern.setFlying(false);
                wyvern.setTakeoff(false);
                wyvern.setHovering(false);
                wyvern.markLandedNow();
                switchToGroundNavigation();
            }
        }
    }

    @SuppressWarnings("unused") // Motion parameter for method signature consistency
    private void handleGlidingTravel(Vec3 motion) {
        Vec3 vec3 = wyvern.getDeltaMovement();

        if (vec3.y > -0.5D) {
            wyvern.fallDistance = 1.0F;
        }

        Vec3 moveDirection = wyvern.getLookAngle().normalize();
        float pitchRad = wyvern.getXRot() * ((float) Math.PI / 180F);

        // Enhanced gliding physics that responds to animation state
        vec3 = applyGlidingPhysics(vec3, moveDirection, pitchRad);

        // Dynamic friction based on flight state
        float horizontalFriction = 0.99F;
        float verticalFriction = 0.98F;
        wyvern.setDeltaMovement(vec3.multiply(horizontalFriction, verticalFriction, horizontalFriction));
        wyvern.move(MoverType.SELF, wyvern.getDeltaMovement());
    }

    private Vec3 applyGlidingPhysics(Vec3 currentVel, Vec3 moveDirection, float pitchRad) {
        double horizontalSpeed = Math.sqrt(moveDirection.x * moveDirection.x + moveDirection.z * moveDirection.z);
        if (horizontalSpeed < 0.001) {
            return currentVel;
        }

        double currentHorizontalSpeed = Math.sqrt(currentVel.horizontalDistanceSqr());
        double lookDirectionLength = moveDirection.length();

        float pitchFactor = Mth.cos(pitchRad);
        pitchFactor = (float) ((double) pitchFactor * (double) pitchFactor * Math.min(1.0D, lookDirectionLength / 0.4D));

        double gravity = getGravity();
        Vec3 result = currentVel.add(0.0D, gravity * (-1.0D + (double) pitchFactor * 0.75D), 0.0D);

        // Enhanced lift calculation with animation influence
        if (result.y < 0.0D && horizontalSpeed > 0.0D) {
            double liftFactor = getLiftFactor(result, pitchFactor);
            result = result.add(
                    moveDirection.x * liftFactor / horizontalSpeed,
                    liftFactor,
                    moveDirection.z * liftFactor / horizontalSpeed
            );
        }

        // Dive calculation
        if (pitchRad < 0.0F && horizontalSpeed > 0.0D) {
            double diveFactor = currentHorizontalSpeed * (double) (-Mth.sin(pitchRad)) * 0.04D;
            result = result.add(
                    -moveDirection.x * diveFactor / horizontalSpeed,
                    diveFactor * 3.2D,
                    -moveDirection.z * diveFactor / horizontalSpeed
            );
        }

        // Directional alignment
        if (horizontalSpeed > 0.0D) {
            double alignmentFactor = 0.1D;
            result = result.add(
                    (moveDirection.x / horizontalSpeed * currentHorizontalSpeed - result.x) * alignmentFactor,
                    0.0D,
                    (moveDirection.z / horizontalSpeed * currentHorizontalSpeed - result.z) * alignmentFactor
            );
        }

        return result;
    }

    private double getLiftFactor(Vec3 result, double pitchFactor) {
        double baseLiftFactor = result.y * -0.1D * pitchFactor;

        double liftMultiplier = 1.0;
        if (wyvern.getFlappingFraction() > 0.3f) {
            liftMultiplier += wyvern.getFlappingFraction() * 0.6;
        }
        if (wyvern.getGlidingFraction() > 0.5f) {
            liftMultiplier += wyvern.getGlidingFraction() * 0.4;
        }

        return baseLiftFactor * liftMultiplier;
    }

    private double getGravity() {
        double gravity = 0.08D;

        if (wyvern.getFlappingFraction() > 0.2f) {
            gravity *= (1.0 - wyvern.getFlappingFraction() * 0.5);
        } else if (wyvern.getHoveringFraction() > 0.4f) {
            gravity *= (1.0 - wyvern.getHoveringFraction() * 0.3);
        }

        if (wyvern.getGlidingFraction() > 0.5f) {
            gravity *= (1.0 - wyvern.getGlidingFraction() * 0.2);
        }

        return gravity;
    }

    private void handleHoveringTravel(Vec3 motion) {
        BlockPos ground = new BlockPos((int) wyvern.getX(), (int) (wyvern.getBoundingBox().minY - 1.0D), (int) wyvern.getZ());
        float friction = 0.91F;

        if (wyvern.onGround()) {
            friction = wyvern.level().getBlockState(ground).getFriction(wyvern.level(), ground, wyvern) * 0.91F;
        }

        float frictionFactor = 0.16277137F / (friction * friction * friction);
        friction = 0.91F;

        if (wyvern.onGround()) {
            friction = wyvern.level().getBlockState(ground).getFriction(wyvern.level(), ground, wyvern) * 0.91F;
        }

        wyvern.moveRelative(wyvern.onGround() ? 0.1F * frictionFactor : 0.02F, motion);
        wyvern.move(MoverType.SELF, wyvern.getDeltaMovement());
        wyvern.setDeltaMovement(wyvern.getDeltaMovement().scale(friction));

        BlockPos destination = wyvern.getNavigation().getTargetPos();
        if (destination != null) {
            double dx = destination.getX() - wyvern.getX();
            double dy = destination.getY() - wyvern.getY();
            double dz = destination.getZ() - wyvern.getZ();
            double distanceToDest = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distanceToDest < 0.1) {
                wyvern.setDeltaMovement(0, 0, 0);
            }
        }
    }
}
