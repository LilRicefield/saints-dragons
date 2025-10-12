package com.leon.saintsdragons.server.entity.controller.lightningdragon;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
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
 * Handles all riding mechanics for the Lightning Dragon
 */
public record RaevyxRiderController(Raevyx wyvern) {
    // ===== SEAT TUNING CONSTANTS =====
    // Baseline vertical offset relative to wyvern height
    private static final double SEAT_BASE_FACTOR = 0.50D; // 0.0..1.0 of bbHeight
    // Additional vertical lift to avoid clipping
    private static final double SEAT_LIFT = 0.05D;
    // Forward/back relative to body (blocks). +forward = toward head, - = toward tail
    private static final double SEAT_FORWARD = 4.5D;
    // Sideways relative to body (blocks). +side = to the wyvern's right, - = left
    private static final double SEAT_SIDE = 0.00D;

    // No head-follow offsets; keep static body seat
    // ===== FLIGHT VERTICAL RATES =====
    // Up/down rates while flying controlled by keybinds
    private static final double ASCEND_RATE = 0.05D;
    private static final double DESCEND_RATE = 0.5D;

    // ===== AIR SPRINT / ACCELERATION TUNING =====
    // These are relative to the entity's `Attributes.FLYING_SPEED` each tick.
    // Base cruise cap is intentionally lower; sprint raises cap and accel.
    private static final double CRUISE_MAX_MULT = 11.5;   // max horizontal blocks/tick relative to base speed
    private static final double SPRINT_MAX_MULT = 55.5;   // top speed cap while accelerating
    private static final double AIR_ACCEL_MULT = 0.085;    // accel per tick toward forward while holding W
    private static final double SPRINT_ACCEL_MULT = 0.25; // accel per tick when accelerating
    private static final double AIR_DRAG = 0.03;          // per-tick horizontal damping
    
    // ===== LIGHTNING DRAGON MANEUVERABILITY =====
    // Enhanced turning capabilities for lightning wyvern's agility
    private static final double TURN_ACCEL_MULT = 0.15;   // additional acceleration for direction changes
    private static final double MIN_TURN_SPEED = 0.3;     // minimum speed to enable enhanced turning
    private static final double TURN_MOMENTUM_FACTOR = 1.2; // how much momentum to preserve during turns

    // ===== RIDING UTILITIES =====

    @Nullable
    public Player getRidingPlayer() {
        if (wyvern.getControllingPassenger() instanceof Player player) {
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

        if (wyvern.isFlying()) {
            // Flying movement – NO pitch-based vertical movement.
            // Vertical is controlled exclusively by ascend/descend keybinds.
            return new Vec3(player.xxa * 0.4F, 0.0F, player.zza * 1.0F * f);
        } else {
            // Ground movement - no vertical component, responsive controls
            return new Vec3(player.xxa * 0.5F, 0.0D, player.zza * 0.9F * f);
        }
    }

    /**
     * Main rider tick method - handles rotation and banking
     */
    public void tickRidden(Player player, @SuppressWarnings("unused") Vec3 travelVector) {
        // Prevent accidental rider fall damage while mounted
        player.fallDistance = 0.0F;
        wyvern.fallDistance = 0.0F;
        // Clear target when being ridden to prevent AI interference
        wyvern.setTarget(null);

        // Make wyvern responsive to player look direction
        if (wyvern.isFlying()) {
            float targetYaw = player.getYRot();
            float targetPitch = player.getXRot() * 0.5f; // Scale down pitch for stability

            // DIRECT ASSIGNMENT with tiny smoothing - no more slow curves!
            float currentYaw = wyvern.getYRot();
            float yawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
            float newYaw = currentYaw + (yawDiff * 0.85f); // 85% instant response
            wyvern.setYRot(newYaw);

            float currentPitch = wyvern.getXRot();
            float pitchDiff = targetPitch - currentPitch;
            float newPitch = currentPitch + (pitchDiff * 0.8f); // 80% instant response
            wyvern.setXRot(newPitch);

            // Banking currently disabled for stability
        } else {
            // Ground: Also direct response 
            float yawDiff = Math.abs(player.getYRot() - wyvern.getYRot());
            if (player.zza != 0 || player.xxa != 0 || yawDiff > 5.0f) {
                float currentYaw = wyvern.getYRot();
                float targetYaw = player.getYRot();
                float rawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
                float newYaw = currentYaw + (rawDiff * 0.75f); // 75% instant response
                wyvern.setYRot(newYaw);
                wyvern.setXRot(0); // Keep level on ground
            }
        }

        // Update body and head rotation
        wyvern.yBodyRot = wyvern.getYRot();
        wyvern.yHeadRot = wyvern.getYRot();

        // Extra safety: if we just touched ground, ensure rider has no fall damage
        if (wyvern.onGround()) {
            player.fallDistance = 0.0F;
            wyvern.fallDistance = 0.0F;
        }
    }

    /**
     * Calculate riding speed based on current state
     */
    public float getRiddenSpeed(@SuppressWarnings("unused") Player rider) {
        if (wyvern.isFlying()) {
            // Flying speed - use ONLY the attributed flying speed, no modifiers
            return (float) wyvern.getAttributeValue(Attributes.FLYING_SPEED);
        } else {
            // Ground speed - use movement speed attribute with acceleration multipliers
            float baseSpeed = (float) wyvern.getAttributeValue(Attributes.MOVEMENT_SPEED);

            // Check if actually moving to prevent sprint animation when standing still
            boolean isMoving = wyvern.getDeltaMovement().horizontalDistanceSqr() > 0.0001;

            if (wyvern.isAccelerating() && isMoving) {
                // L-Ctrl pressed AND moving - trigger run animation and boost speed
                wyvern.setRunning(true);
                return baseSpeed * 0.7F;
            } else {
                // Normal ground speed - use walk animation, stop running
                wyvern.setRunning(false);
                return baseSpeed * 0.5F;
            }
        }
    }

    // ===== RIDING MOVEMENT =====

    /**
     * Handle rider movement - called from travel() method
     */
    public void handleRiderMovement(Player player, Vec3 motion) {
        // Clear any AI navigation when being ridden
        if (wyvern.getNavigation().getPath() != null) {
            wyvern.getNavigation().stop();
        }

        if (wyvern.isFlying()) {
            // Directional input comes in local space via `motion` (strafe X, forward Z)
            // We implement a throttle-based acceleration model with drag and speed caps.
            final double base = wyvern.getAttributeValue(Attributes.FLYING_SPEED);
            final boolean sprinting = wyvern.isAccelerating();
            final double accel = (sprinting ? SPRINT_ACCEL_MULT : AIR_ACCEL_MULT) * base;
            final double maxSpeed = (sprinting ? SPRINT_MAX_MULT : CRUISE_MAX_MULT) * base;

            // Current velocity split into horizontal and vertical
            Vec3 cur = wyvern.getDeltaMovement();
            Vec3 horiz = new Vec3(cur.x, 0.0, cur.z);

            // First apply horizontal drag (decay) to the carried velocity
            horiz = horiz.scale(1.0 - AIR_DRAG);

            // Build desired heading from forward + strafe.
            // Allow pure lateral thrust (strafe) even without forward input.
            // Support backward movement by using full motion.z range
            double forwardInput = motion.z;               // allow forward/backward movement
            double strafeInput = motion.x;                // allow left/right

            float yawRad = (float) Math.toRadians(wyvern.getYRot());
            // Basis vectors
            double fx = -Math.sin(yawRad);  // forward X
            double fz =  Math.cos(yawRad);  // forward Z
            double rx =  Math.cos(yawRad);  // right X
            double rz =  Math.sin(yawRad);  // right Z

            // When moving forward/backward, give strafe less authority to keep heading stable.
            // When not moving forward/backward, give strafe more authority for pure lateral movement.
            double strafeWeight = Math.abs(forwardInput) > 0.2 ? 0.35 : 0.8;
            double dx = fx * forwardInput + rx * (strafeInput * strafeWeight);
            double dz = fz * forwardInput + rz * (strafeInput * strafeWeight);
            double len = Math.hypot(dx, dz);
            if (len > 1e-4) {
                dx /= len; dz /= len;
                // Scale thrust by overall input magnitude (so tiny taps give smaller accel)
                double inputMag = Math.min(1.0, Math.hypot(Math.abs(forwardInput), Math.abs(strafeInput * strafeWeight)));
                
                // LIGHTNING DRAGON ENHANCED MANEUVERABILITY
                // Calculate current speed and direction for enhanced turning
                double currentSpeed = Math.hypot(horiz.x, horiz.z);
                Vec3 currentDirection = currentSpeed > 1e-4 ? new Vec3(horiz.x / currentSpeed, 0, horiz.z / currentSpeed) : Vec3.ZERO;
                Vec3 desiredDirection = new Vec3(dx, 0, dz);
                
                // Calculate turn angle (dot product gives us the angle between vectors)
                double turnAngle = currentSpeed > 1e-4 ? Math.acos(Math.max(-1, Math.min(1, currentDirection.dot(desiredDirection)))) : 0;
                
                // Enhanced turning for lightning wyvern - more responsive direction changes
                double turnMultiplier = 1.0;
                if (currentSpeed > MIN_TURN_SPEED && turnAngle > 0.1) { // Significant direction change
                    // Lightning dragons can turn more sharply - reduce momentum penalty
                    turnMultiplier = TURN_MOMENTUM_FACTOR + (TURN_ACCEL_MULT * (1.0 - turnAngle / Math.PI));
                    turnMultiplier = Math.max(0.3, Math.min(1.2, turnMultiplier)); // Clamp for safety
                }
                
                // Apply enhanced maneuverability
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
            if (wyvern.isGoingUp()) {
                vy += ASCEND_RATE;
            } else if (wyvern.isGoingDown()) {
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
            wyvern.move(MoverType.SELF, next);
            wyvern.setDeltaMovement(next);
            wyvern.calculateEntityAnimation(true);

            // While airborne and ridden, continuously clear fall damage
            player.fallDistance = 0.0F;
            wyvern.fallDistance = 0.0F;
        }
    }

    // ===== RIDING SUPPORT =====
    
    public double getPassengersRidingOffset() {
        return (double) wyvern.getBbHeight() * SEAT_BASE_FACTOR;
    }
    
    public void positionRider(@NotNull Entity passenger, Entity.@NotNull MoveFunction moveFunction) {
        if (!wyvern.hasPassenger(passenger)) return;
        
        // Simple vanilla positioning - let the render layer handle bone positioning
        double offsetY = getPassengersRidingOffset() + SEAT_LIFT;
        double forward = SEAT_FORWARD;
        double side = SEAT_SIDE;
        double rad = Math.toRadians(wyvern.yBodyRot);
        double dx = -Math.sin(rad) * forward + Math.cos(rad) * side;
        double dz =  Math.cos(rad) * forward + Math.sin(rad) * side;
        moveFunction.accept(passenger, wyvern.getX() + dx, wyvern.getY() + offsetY, wyvern.getZ() + dz);
    }
    
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        passenger.fallDistance = 0.0F;
        var level = wyvern.level();
        Vec3 base = wyvern.position();

        // Sample radial candidates around the wyvern for a safe dismount
        double[] radii = new double[] { 2.5, 3.5, 1.8 };
        int[] angles = new int[] { 0, 30, -30, 60, -60, 90, -90, 150, -150, 180 };

        for (double r : radii) {
            for (int a : angles) {
                double rad = Math.toRadians(wyvern.getYRot() + a);
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

        // Fallback: ahead of wyvern
        Vec3 direction = wyvern.getViewVector(1.0F);
        return base.add(direction.scale(2.0));
    }
    
    @Nullable 
    public LivingEntity getControllingPassenger() {
        Entity entity = wyvern.getFirstPassenger();
        if (entity instanceof Player player && wyvern.isTame() && wyvern.isOwnedBy(player)) {
            return player;
        }
        return null;
    }
    
    /**
     * Forces the wyvern to take off when being ridden. Called when player presses Space while on ground.
     */
    public void requestRiderTakeoff() {
        if (!wyvern.isTame() || getRidingPlayer() == null || wyvern.isFlying()) return;
        // Block takeoff if locked (e.g., during roar)
        if (wyvern.isTakeoffLocked()) return;
        // Gentle cooldown after landing to prevent spam takeoff/land jitter
        long now = wyvern.level().getGameTime();
        long lastLand = wyvern.getLastLandingGameTime();
        if (lastLand != Long.MIN_VALUE && (now - lastLand) < 20L) { // ~1s cooldown
            return;
        }
        
        // Reset all flight states for a fresh takeoff
        wyvern.timeFlying = 0;
        wyvern.landingFlag = false;
        wyvern.landingTimer = 0;
        
        // Initiate takeoff sequence
        wyvern.setFlying(true);
        wyvern.setTakeoff(true);
        wyvern.setHovering(false);
        wyvern.setLanding(false);
        // Keep takeoff active for a brief window so server flight logic applies upward force
        wyvern.setRiderTakeoffTicks(30);
    }
}
