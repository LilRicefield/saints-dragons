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

    // ===== FLIGHT VERTICAL RATES (SLOWER THAN LIGHTNING DRAGON) =====
    // Up/down rates while flying controlled by keybinds - gliders are slower
    private static final double ASCEND_RATE = 0.03D;  // Slower than Lightning Dragon (0.05D)
    private static final double DESCEND_RATE = 0.08D;  // Much slower than Lightning Dragon (0.5D) for glider behavior

    // ===== AIR SPRINT / ACCELERATION TUNING (MUCH SLOWER FOR GLIDER) =====
    // These are relative to the entity's `Attributes.FLYING_SPEED` each tick.
    // Base cruise cap is intentionally lower; sprint raises cap and accel.
    private static final double CRUISE_MAX_MULT = 10.0;
    private static final double SPRINT_MAX_MULT = 20.0;    // Moderate sprint boost for glider (vs Lightning Dragon's 50.0)
    private static final double AIR_ACCEL_MULT = 0.10;
    private static final double SPRINT_ACCEL_MULT = 0.18;  // Moderate sprint acceleration (vs Lightning Dragon's 0.25)
    private static final double AIR_DRAG = 0.05;           // More drag for glider behavior
    
    // ===== AMPHITHERE GLIDER MANEUVERABILITY =====
    // Gliders have different turning characteristics - more graceful, less sharp
    private static final double TURN_ACCEL_MULT = 0.06;   // Much less aggressive than Lightning Dragon (0.15)
    private static final double MIN_TURN_SPEED = 0.15;     // Lower threshold than Lightning Dragon (0.3)
    private static final double TURN_MOMENTUM_FACTOR = 0.95; // Much more momentum preservation than Lightning Dragon (0.8)

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
            // Flying movement – NO pitch-based vertical movement.
            // Vertical is controlled exclusively by ascend/descend keybinds.
            return new Vec3(player.xxa * 0.3F, 0.0F, player.zza * 0.8F * f); // Slower than Lightning Dragon
        } else {
            // Ground movement - no vertical component, responsive controls
            return new Vec3(player.xxa * 0.4F, 0.0D, player.zza * 0.7F * f); // Slower than Lightning Dragon
        }
    }

    /**
     * Main rider tick method - handles rotation and banking
     */
    public void tickRidden(Player player, @SuppressWarnings("unused") Vec3 travelVector) {
        // Prevent accidental rider fall damage while mounted
        player.fallDistance = 0.0F;
        dragon.fallDistance = 0.0F;
        // Clear target when being ridden to prevent AI interference
        dragon.setTarget(null);

        // Make dragon responsive to player look direction
        if (dragon.isFlying()) {
            float targetYaw = player.getYRot();
            float targetPitch = player.getXRot() * 0.4f; // More gentle than Lightning Dragon (0.5f)

            // SMOOTHER RESPONSE for glider behavior - less aggressive than Lightning Dragon
            float currentYaw = dragon.getYRot();
            float yawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
            float newYaw = currentYaw + (yawDiff * 0.7f); // Slower than Lightning Dragon (0.85f)
            dragon.setYRot(newYaw);

            float currentPitch = dragon.getXRot();
            float pitchDiff = targetPitch - currentPitch;
            float newPitch = currentPitch + (pitchDiff * 0.6f); // Slower than Lightning Dragon (0.8f)
            dragon.setXRot(newPitch);

            // Banking currently disabled for stability
        } else {
            // Ground: Also direct response 
            float yawDiff = Math.abs(player.getYRot() - dragon.getYRot());
            if (player.zza != 0 || player.xxa != 0 || yawDiff > 5.0f) {
                float currentYaw = dragon.getYRot();
                float targetYaw = player.getYRot();
                float rawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
                float newYaw = currentYaw + (rawDiff * 0.6f); // Slower than Lightning Dragon (0.75f)
                dragon.setYRot(newYaw);
                dragon.setXRot(0); // Keep level on ground
            }
        }

        // Update body and head rotation
        dragon.yBodyRot = dragon.getYRot();
        dragon.yHeadRot = dragon.getYRot();

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
            // Ground speed - use movement speed attribute with acceleration multipliers
            float baseSpeed = (float) dragon.getAttributeValue(Attributes.MOVEMENT_SPEED);

            if (dragon.isAccelerating()) {
                return baseSpeed * 0.60F; // Just a tad slower than Lightning Dragon (0.7F)
            } else {
                return baseSpeed * 0.5F; // Normal speed (same as Lightning Dragon)
            }
        }
    }

    // ===== RIDING MOVEMENT =====

    /**
     * Handle rider movement - called from travel() method
     */
    public void handleRiderMovement(Player player, Vec3 motion) {
        // Clear any AI navigation when being ridden
        if (dragon.getNavigation().getPath() != null) {
            dragon.getNavigation().stop();
        }

        if (dragon.isFlying()) {
            // Directional input comes in local space via `motion` (strafe X, forward Z)
            // We implement a throttle-based acceleration model with drag and speed caps.
            final double base = dragon.getAttributeValue(Attributes.FLYING_SPEED);
            final boolean sprinting = dragon.isAccelerating();
            final double accel = (sprinting ? SPRINT_ACCEL_MULT : AIR_ACCEL_MULT) * base;
            final double maxSpeed = (sprinting ? SPRINT_MAX_MULT : CRUISE_MAX_MULT) * base;

            // Current velocity split into horizontal and vertical
            Vec3 cur = dragon.getDeltaMovement();
            Vec3 horiz = new Vec3(cur.x, 0.0, cur.z);

            // First apply horizontal drag (decay) to the carried velocity
            horiz = horiz.scale(1.0 - AIR_DRAG);

            // Build desired heading from forward + strafe.
            // Allow pure lateral thrust (strafe) even without forward input.
            // Support backward movement by using full motion.z range
            double forwardInput = motion.z;               // allow forward/backward movement
            double strafeInput = motion.x;                // allow left/right

            float yawRad = (float) Math.toRadians(dragon.getYRot());
            // Basis vectors
            double fx = -Math.sin(yawRad);  // forward X
            double fz =  Math.cos(yawRad);  // forward Z
            double rx =  Math.cos(yawRad);  // right X
            double rz =  Math.sin(yawRad);  // right Z

            // When moving forward/backward, give strafe less authority to keep heading stable.
            // When not moving forward/backward, give strafe more authority for pure lateral movement.
            double strafeWeight = Math.abs(forwardInput) > 0.2 ? 0.3 : 0.7; // Less aggressive than Lightning Dragon
            double dx = fx * forwardInput + rx * (strafeInput * strafeWeight);
            double dz = fz * forwardInput + rz * (strafeInput * strafeWeight);
            double len = Math.hypot(dx, dz);
            if (len > 1e-4) {
                dx /= len; dz /= len;
                // Scale thrust by overall input magnitude (so tiny taps give smaller accel)
                double inputMag = Math.min(1.0, Math.hypot(Math.abs(forwardInput), Math.abs(strafeInput * strafeWeight)));
                
                // AMPHITHERE GLIDER MANEUVERABILITY
                // Calculate current speed and direction for enhanced turning
                double currentSpeed = Math.hypot(horiz.x, horiz.z);
                Vec3 currentDirection = currentSpeed > 1e-4 ? new Vec3(horiz.x / currentSpeed, 0, horiz.z / currentSpeed) : Vec3.ZERO;
                Vec3 desiredDirection = new Vec3(dx, 0, dz);
                
                // Calculate turn angle (dot product gives us the angle between vectors)
                double turnAngle = currentSpeed > 1e-4 ? Math.acos(Math.max(-1, Math.min(1, currentDirection.dot(desiredDirection)))) : 0;
                
                // Glider turning - more graceful, less sharp than Lightning Dragon
                double turnMultiplier = 1.0;
                if (currentSpeed > MIN_TURN_SPEED && turnAngle > 0.1) { // Significant direction change
                    // Gliders turn more gracefully - preserve more momentum
                    turnMultiplier = TURN_MOMENTUM_FACTOR + (TURN_ACCEL_MULT * (1.0 - turnAngle / Math.PI));
                    turnMultiplier = Math.max(0.4, Math.min(1.1, turnMultiplier)); // More conservative than Lightning Dragon
                }
                
                // Apply glider maneuverability
                double enhancedAccel = accel * turnMultiplier;
                horiz = horiz.add(dx * enhancedAccel * inputMag, 0.0, dz * enhancedAccel * inputMag);
            }

            // Clamp horizontal speed to cap after drag and thrust
            double speed = Math.hypot(horiz.x, horiz.z);
            if (speed > maxSpeed) {
                double s = maxSpeed / speed;
                horiz = new Vec3(horiz.x * s, 0.0, horiz.z * s);
            }

            // Vertical movement from rider input (decoupled from horizontal model)
            double vy = cur.y;
            if (dragon.isGoingUp()) {
                vy += ASCEND_RATE;
            } else if (dragon.isGoingDown()) {
                vy -= DESCEND_RATE;
            } else {
                // Mild vertical damping to stabilize when no vertical input
                vy *= 0.98;
            }

            Vec3 next = new Vec3(horiz.x, vy, horiz.z);
            // Final hard clamp on horizontal component before applying
            double nextH = Math.hypot(next.x, next.z);
            if (nextH > maxSpeed) {
                double s = maxSpeed / nextH;
                next = new Vec3(next.x * s, next.y, next.z * s);
            }
            dragon.move(MoverType.SELF, next);
            dragon.setDeltaMovement(next);
            dragon.calculateEntityAnimation(true);

            // While airborne and ridden, continuously clear fall damage
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
        dragon.setFlying(true);
        dragon.setTakeoff(true);
        dragon.setHovering(false);
        dragon.setLanding(false);
        dragon.setRiderTakeoffTicks(25);

        Vec3 current = dragon.getDeltaMovement();
        double upward = Math.max(current.y, 0.25D);
        dragon.setDeltaMovement(current.x, upward, current.z);
    }
}
