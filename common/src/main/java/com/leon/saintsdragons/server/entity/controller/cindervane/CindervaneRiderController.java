package com.leon.saintsdragons.server.entity.controller.cindervane;

import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles all riding mechanics for the Amphithere
 * Adapted from Lightning Dragon but tuned for glider behavior
 */
public record CindervaneRiderController(Cindervane dragon) {
    // ===== SEAT TUNING CONSTANTS =====
    // Baseline vertical offset relative to dragon height
    private static final double SEAT_BASE_FACTOR = 0.05D; // 0.0..1.0 of bbHeight
    // Additional vertical lift to avoid clipping
    private static final double SEAT_LIFT = 0.70D;

    // SEAT 0 (DRIVER - OWNER ONLY) - Front seat position
    private static final double SEAT_0_FORWARD = 8.0D;  // Toward head
    private static final double SEAT_0_SIDE = 0.00D;

    // SEAT 1 (PASSENGER) - Back seat position
    private static final double SEAT_1_FORWARD = 4.0D;  // Behind driver
    private static final double SEAT_1_SIDE = 0.00D;

    // ===== SIMPLIFIED ARCADE FLIGHT PHYSICS =====
    // Speed multipliers relative to base FLYING_SPEED attribute
    private static final double CRUISE_SPEED_MULT = 4.5;   // Slower base speed than Raevyx but value is almost same cuz we modify base speed(glider feel)
    private static final double SPRINT_SPEED_MULT = 6.5;   // More modest sprint boost

    // Acceleration and drag
    private static final double ACCELERATION = 0.12;        // Slightly slower acceleration than Raevyx
    private static final double DRAG_WITH_INPUT = 0.06;    // Gentle deceleration while flying
    private static final double DRAG_NO_INPUT = 0.45;      // Strong braking when coasting

    // Strafe and vertical
    private static final double STRAFE_POWER = 0.4;        // Slightly weaker strafe than Raevyx (glider feel)

    // ===== VERTICAL PHYSICS (dive acceleration, no gravity) =====
    private static final double ASCEND_THRUST = 0.06D;      // Slightly weaker climb than Raevyx
    private static final double DESCEND_THRUST = 0.85D;     // Strong dive acceleration
    private static final double TERMINAL_VELOCITY = 1.2D;   // Slower terminal velocity (wing drag)
    private static final double VERTICAL_DRAG = 0.95D;      // Higher air resistance than Raevyx (massive wings)

    // ===== RIDING UTILITIES =====

    @Nullable
    public Player getRidingPlayer() {
        if (dragon.getControllingPassenger() instanceof Player player) {
            return player;
        }
        return null;
    }
    
    // ===== RIDER INPUT PROCESSING =====

    /**
     * Processes rider input and converts to movement vector
     */
    public Vec3 getRiddenInput(Player player, @SuppressWarnings("unused") Vec3 deltaIn) {
        float f = player.zza < 0.0F ? 0.5F : 1.0F;

        if (dragon.isFlying()) {
            // Flying movement - pitch affects vertical direction for 3D flight feel.
            return new Vec3(player.xxa * 0.3F, 0.0F, player.zza * 0.8F * f);
        } else {
            // Ground movement - no vertical component, responsive controls
            return new Vec3(player.xxa * 0.4F, 0.0D, player.zza * 0.7F * f);
        }
    }

    /**
     * Main rider tick method - handles rotation
     * Smooth turning handled by DragonBodyControl + bodyRotDeviation system
     */
    public void tickRidden(Player player, @SuppressWarnings("unused") Vec3 travelVector) {
        // Prevent accidental rider fall damage while mounted
        player.fallDistance = 0.0F;
        dragon.fallDistance = 0.0F;
        // Clear target when being ridden to prevent AI interference
        dragon.setTarget(null);

        boolean flying = dragon.isFlying();

        // Always sync yaw with player's look direction (like Ignivorus)
        // This ensures spectators see smooth head tracking even when dragon is standing still
        float currentYaw = dragon.getYRot();
        float targetYaw = player.getYRot();
        float rawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float blend = flying ? 0.30f : 0.25f; // Glider feel - slower than Raevyx
        float newYaw = currentYaw + (rawDiff * blend);

        // Set rotation - syncs immediately to all clients
        dragon.setYRot(newYaw);
        dragon.yBodyRot = newYaw;
        dragon.yHeadRot = newYaw;

        // Pitch control - DON'T set entity xRot when flying
        // Visual pitch is handled by velocity-based flightPitchRad in tickPitchingLogic()
        // Only reset pitch when on ground
        if (!flying) {
            dragon.setXRot(0.0F);
        }
        // When flying, xRot stays at 0 - visual pitch comes from the model's applyFlightPitch()

        // Extra safety: if we just touched ground, ensure rider has no fall damage
        if (dragon.onGround()) {
            player.fallDistance = 0.0F;
            dragon.fallDistance = 0.0F;
        }
    }

    /**
     * Calculate riding speed based on current state
     */
    public float getRiddenSpeed(@SuppressWarnings("unused") Player rider) {
        if (dragon.isFlying()) {
            // Flying speed - use ONLY the attributed flying speed, no modifiers
            return (float) dragon.getAttributeValue(Attributes.FLYING_SPEED);
        } else {
            // Ground speed - HARDCODED (not configurable)
            boolean running = dragon.isAccelerating();
            dragon.setRunning(running);
            return (float) (running ? Cindervane.RIDER_RUN_SPEED : Cindervane.RIDER_WALK_SPEED);
        }
    }

    // ===== RIDING MOVEMENT =====

    /**
     * Handle rider movement - called from travel() method
     * Simplified arcade physics for responsive, predictable glider-style flight
     */
    public void handleRiderMovement(Player player, Vec3 motion) {
        // Clear any AI navigation when being ridden
        if (dragon.getNavigation().getPath() != null) {
            dragon.getNavigation().stop();
        }

        if (dragon.isFlying()) {
            // === SETUP ===
            final double baseSpeed = dragon.getAttributeValue(Attributes.FLYING_SPEED);
            final boolean sprinting = dragon.isAccelerating();
            double targetSpeed = (sprinting ? SPRINT_SPEED_MULT : CRUISE_SPEED_MULT) * baseSpeed;

            // Current velocity
            Vec3 currentVel = dragon.getDeltaMovement();

            // === DIVE SPEED BOOST ===
            // Progressive speed boost when diving steeply (like real birds)
            // NOTE: Minecraft xRot is POSITIVE when looking down (90° = straight down)
            float pitchRad = (float) Math.toRadians(player.getXRot());  // Player camera pitch
            float pitchDegrees = (float) Math.toDegrees(pitchRad);
            double diveMultiplier = 1.0;
            double diveAcceleration = ACCELERATION; // Default 0.15
            double diveDrag = DRAG_WITH_INPUT; // Default 0.08

            if (pitchDegrees >= 40.0f) {
                // Steep dive (60° to 90°): 1.3x to 1.5x speed
                float t = (pitchDegrees - 60.0f) / 30.0f; // 0 at 60°, 1 at 90°
                t = Mth.clamp(t, 0.0f, 1.0f);
                diveMultiplier = 1.3 + (t * 0.2);
                // Moderately faster acceleration and reduced drag
                diveAcceleration = 0.20;
                diveDrag = 0.05;
            } else if (pitchDegrees >= 30.0f) {
                // Medium dive (45° to 60°): 1.15x to 1.3x speed
                float t = (pitchDegrees - 45.0f) / 15.0f; // 0 at 45°, 1 at 60°
                t = Mth.clamp(t, 0.0f, 1.0f);
                diveMultiplier = 1.15 + (t * 0.15);
                // Slightly faster acceleration and slightly less drag
                diveAcceleration = 0.18;
                diveDrag = 0.06;
            }
            // Below 45°: no dive boost (diveMultiplier = 1.0)

            targetSpeed *= diveMultiplier;

            // Get inputs (local space: motion.x = strafe, motion.z = forward)
            double forwardInput = motion.z;
            double strafeInput = motion.x;

            // === DIRECTIONAL CONTROL ===
            // Calculate world-space direction from player input
            // Use PLAYER's pitch for 3D flight direction, not dragon's (which is 0 for visual reasons)
            float yawRad = (float) Math.toRadians(dragon.getYRot());
            // pitchRad already calculated above for dive speed
            double forwardXZ = Math.cos(pitchRad);
            double forwardX = -Math.sin(yawRad) * forwardXZ;
            double forwardY = -Math.sin(pitchRad);
            double forwardZ = Math.cos(yawRad) * forwardXZ;
            double strafeX = Math.cos(yawRad);
            double strafeZ = Math.sin(yawRad);

            // Calculate target direction with constant strafe power
            double targetDirX = forwardX * forwardInput + strafeX * strafeInput * STRAFE_POWER;
            double targetDirY = forwardY * forwardInput * 1.35;
            double targetDirZ = forwardZ * forwardInput + strafeZ * strafeInput * STRAFE_POWER;
            double dirLength = Math.sqrt(targetDirX * targetDirX + targetDirY * targetDirY + targetDirZ * targetDirZ);

            // === ARCADE PHYSICS WITH TWO MODES ===
            Vec3 newVelocity;
            boolean hasInput = Math.abs(forwardInput) > 0.01 || Math.abs(strafeInput) > 0.01;

            if (hasInput && dirLength > 0.01) {
                // === ACTIVE FLYING: smooth acceleration with gentle drag ===
                targetDirX /= dirLength;
                targetDirY /= dirLength;
                targetDirZ /= dirLength;

                // Target velocity in desired direction
                Vec3 targetVelocity = new Vec3(
                    targetDirX * targetSpeed,
                    targetDirY * targetSpeed,
                    targetDirZ * targetSpeed
                );

                // Lerp current velocity toward target (acceleration - faster when diving)
                newVelocity = new Vec3(
                    Mth.lerp(diveAcceleration, currentVel.x, targetVelocity.x),
                    Mth.lerp(diveAcceleration, currentVel.y, targetVelocity.y),
                    Mth.lerp(diveAcceleration, currentVel.z, targetVelocity.z)
                );

                // Apply drag (reduced when diving for higher top speed)
                newVelocity = newVelocity.scale(1.0 - diveDrag);

            } else {
                // === COASTING: strong braking for immediate stop ===
                newVelocity = currentVel.scale(1.0 - DRAG_NO_INPUT);

                // Hard stop at very low speeds to prevent endless drift
                if (newVelocity.length() < 0.01) {
                    newVelocity = Vec3.ZERO;
                }
            }

            // Final speed cap (single clamp, no double clamping)
            double finalSpeed = newVelocity.length();
            if (finalSpeed > targetSpeed) {
                newVelocity = newVelocity.scale(targetSpeed / finalSpeed);
            }

            // === VERTICAL MOVEMENT (dive acceleration, no gravity) ===
            double newVerticalVel = newVelocity.y;

            // When diving (pitch >= 45°), use the physics-calculated velocity - don't override!
            // This allows dive speed boost to work properly
            boolean isDiving = pitchDegrees >= 45.0f && hasInput;

            if (!isDiving) {
                // PRIORITY: Respect automated takeoff boost (overrides rider input during takeoff window)
                if (dragon.getRiderTakeoffTicks() > 0) {
                    // During automated takeoff, apply a firm upward shove so climb begins immediately
                    // and stays smooth even if the rider isn't pitching/pressing anything yet.
                    double boost = ASCEND_THRUST * 0.85;
                    newVerticalVel = Math.max(newVerticalVel + boost, 0.45);
                } else if (dragon.isGoingUp()) {
                    // Apply upward thrust
                    newVerticalVel += ASCEND_THRUST;
                } else if (dragon.isGoingDown()) {
                    // Apply downward thrust - DIVES ACCELERATE!
                    newVerticalVel -= DESCEND_THRUST;
                }

                // Clamp to terminal velocity (only when not diving)
                newVerticalVel = Mth.clamp(newVerticalVel, -TERMINAL_VELOCITY, TERMINAL_VELOCITY);
            }
            // When diving, vertical velocity is already calculated by physics above - no override needed!

            // === FINAL VELOCITY & MOVEMENT ===
            Vec3 finalVelocity = new Vec3(newVelocity.x, newVerticalVel, newVelocity.z);
            dragon.move(MoverType.SELF, finalVelocity);
            dragon.setDeltaMovement(finalVelocity);
            dragon.calculateEntityAnimation(true);

            // Clear fall damage while airborne
            player.fallDistance = 0.0F;
            dragon.fallDistance = 0.0F;
        }
    }

    // ===== RIDING SUPPORT =====
    
    public double getPassengersRidingOffset() {
        return (double) dragon.getBbHeight() * SEAT_BASE_FACTOR;
    }
    
    public void positionRider(@NotNull Entity passenger, Entity.@NotNull MoveFunction moveFunction) {
        if (!dragon.hasPassenger(passenger)) return;

        // Determine which seat the passenger is in (0 = driver, 1 = passenger)
        var passengers = dragon.getPassengers();
        int seatIndex = passengers.indexOf(passenger);

        if (seatIndex == -1) return; // Passenger not found

        // Try to use bone-based positioning from renderer cache
        String locatorName = (seatIndex == 0) ? "passengerSeat0" : "passengerSeat1";
        Vec3 passengerLoc = dragon.getClientLocatorPosition(locatorName);

        if (passengerLoc != null) {
            // Bone-based positioning with rotation-aware offset
            // Convert to dragon-local space to handle both movement AND rotation
            Vec3 dragonOldPos = new Vec3(dragon.xo, dragon.yo, dragon.zo);
            float oldYaw = dragon.yRotO;
            Vec3 worldOffset = passengerLoc.subtract(dragonOldPos);

            // Convert world offset to dragon-local space using old rotation
            double oldYawRad = Math.toRadians(-oldYaw);
            double cosOld = Math.cos(oldYawRad);
            double sinOld = Math.sin(oldYawRad);

            double localX = worldOffset.x * cosOld - worldOffset.z * sinOld;
            double localY = worldOffset.y;
            double localZ = worldOffset.x * sinOld + worldOffset.z * cosOld;

            // Rotate local offset to current rotation
            float currentYaw = dragon.getYRot();
            double currentYawRad = Math.toRadians(-currentYaw);
            double cosCurrent = Math.cos(currentYawRad);
            double sinCurrent = Math.sin(currentYawRad);

            double currentWorldX = localX * cosCurrent + localZ * sinCurrent;
            double currentWorldZ = -localX * sinCurrent + localZ * cosCurrent;

            Vec3 dragonCurrentPos = dragon.position();
            Vec3 passengerCurrentPos = dragonCurrentPos.add(currentWorldX, localY, currentWorldZ);

            moveFunction.accept(passenger, passengerCurrentPos.x, passengerCurrentPos.y, passengerCurrentPos.z);
        } else {
            // Fallback to vanilla positioning if bone data not available
            double offsetY = getPassengersRidingOffset() + SEAT_LIFT;
            double forward;
            double side;

            if (seatIndex == 0) {
                // Seat 0 - Driver (front)
                forward = SEAT_0_FORWARD;
                side = SEAT_0_SIDE;
            } else {
                // Seat 1+ - Passenger (back)
                forward = SEAT_1_FORWARD;
                side = SEAT_1_SIDE;
            }

            double rad = Math.toRadians(dragon.yBodyRot);
            double dx = -Math.sin(rad) * forward + Math.cos(rad) * side;
            double dz =  Math.cos(rad) * forward + Math.sin(rad) * side;
            moveFunction.accept(passenger, dragon.getX() + dx, dragon.getY() + offsetY, dragon.getZ() + dz);
        }
    }
    
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        passenger.fallDistance = 0.0F;
        var level = dragon.level();
        Vec3 base = dragon.position();

        // Sample radial candidates around the dragon for a safe dismount
        double[] radii = new double[] { 2.5, 3.5, 1.8 };
        int[] angles = new int[] { 0, 30, -30, 60, -60, 90, -90, 150, -150, 180 };

        for (double r : radii) {
            for (int a : angles) {
                double rad = Math.toRadians(dragon.getYRot() + a);
                double cx = base.x + Math.cos(rad) * r;
                double cz = base.z + Math.sin(rad) * r;

                // Project down to find ground up to 6 blocks below current Y
                int startY = (int) Math.floor(base.y + 1.0);
                for (int dy = 0; dy <= 6; dy++) {
                    int y = startY - dy;
                    var pos = new net.minecraft.core.BlockPos((int) Math.floor(cx), y, (int) Math.floor(cz));
                    var below = pos.below();
                    var bsBelow = level.getBlockState(below);
                    var bsAt = level.getBlockState(pos);
                    boolean solidBelow = !bsBelow.isAir() && !bsBelow.getCollisionShape(level, below).isEmpty();
                    boolean spaceFree = bsAt.getCollisionShape(level, pos).isEmpty();
                    boolean fluidOk = bsAt.getFluidState().isEmpty();
                    if (solidBelow && spaceFree && fluidOk) {
                        // Found a safe spot; return precise center with a slight lift
                        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5);
                    }
                }
            }
        }

        // Fallback: ahead of dragon
        Vec3 direction = dragon.getViewVector(1.0F);
        return base.add(direction.scale(2.0));
    }
    
    @Nullable
    public LivingEntity getControllingPassenger() {
        // Only seat 0 (first passenger) can control, and only if they're the owner
        var passengers = dragon.getPassengers();
        if (passengers.isEmpty()) {
            return null;
        }

        Entity firstPassenger = passengers.get(0);
        if (firstPassenger instanceof Player player && dragon.isTame() && dragon.isOwnedBy(player)) {
            return player;
        }
        return null;
    }
    
    /**
     * Forces the dragon to take off when being ridden. Called when player presses Space while on ground.
     */
    public void requestRiderTakeoff() {
        if (!dragon.isTame() || getRidingPlayer() == null || dragon.isFlying()) return;
        if (!dragon.canTakeoff()) return;

        dragon.getNavigation().stop();
        dragon.setGoingDown(false);
        dragon.setGoingUp(true); // latch ascend so we don't depend on a follow-up packet
        dragon.setFlying(true);
        dragon.setTakeoff(true);
        dragon.setHovering(false);
        dragon.setLanding(false);
        dragon.setRiderTakeoffTicks(25);

        Vec3 current = dragon.getDeltaMovement();
        double upward = Math.max(current.y, 0.55D); // strong initial shove to clear the ground smoothly
        dragon.setDeltaMovement(current.x, upward, current.z);
        dragon.hasImpulse = true;
    }
}
