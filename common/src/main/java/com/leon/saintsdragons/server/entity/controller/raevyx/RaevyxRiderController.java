package com.leon.saintsdragons.server.entity.controller.raevyx;

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

    // No head-follow offsets; keep static body seat

    // ===== SIMPLIFIED FLIGHT PHYSICS =====
    // Clean, responsive arcade-style flight controls
    private static final double CRUISE_SPEED_MULT = 3.75;     // Normal flight speed multiplier
    private static final double SPRINT_SPEED_MULT = 7.85;     // Sprint speed multiplier (80% faster)
    private static final double ACCELERATION = 0.15;         // How quickly dragon reaches target speed
    private static final double DRAG_WITH_INPUT = 0.08;      // Gentle drag when player is actively flying
    private static final double DRAG_NO_INPUT = 0.5;         // Strong braking when player releases controls
    private static final double STRAFE_POWER = 0.5;          // Strafe input strength (constant)

    // ===== VERTICAL PHYSICS (dive acceleration only) =====
    private static final double ASCEND_THRUST = 1.2D;       // Upward thrust when climbing
    private static final double DESCEND_THRUST = 1.0D;       // Downward thrust when diving (accelerates)
    private static final double TERMINAL_VELOCITY = 1.5D;    // Max falling speed
    private static final double VERTICAL_DRAG = 0.02D;       // Damping when no vertical input (air resistance)

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
     * Main rider tick method - handles rotation
     * Smooth turning handled by DragonBodyControl + bodyRotDeviation system
     */
    public void tickRidden(Player player, @SuppressWarnings("unused") Vec3 travelVector) {
        // Prevent accidental rider fall damage while mounted
        player.fallDistance = 0.0F;
        wyvern.fallDistance = 0.0F;
        // Clear target when being ridden to prevent AI interference
        wyvern.setTarget(null);

        boolean flying = wyvern.isFlying();

        // Always sync yaw with player's look direction (like Ignivorus)
        // This ensures spectators see smooth head tracking even when dragon is standing still
        float currentYaw = wyvern.getYRot();
        float targetYaw = player.getYRot();
        float rawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float blend = flying ? 0.35f : 0.28f; // Slightly more responsive when flying
        float newYaw = currentYaw + (rawDiff * blend);

        // Set rotation - syncs immediately to all clients
        wyvern.setYRot(newYaw);
        wyvern.yBodyRot = newYaw;
        wyvern.yHeadRot = newYaw;

        // Pitch control
        if (flying) {
            float targetPitch = Mth.clamp(player.getXRot() * 0.55f, -35.0f, 30.0f);
            wyvern.setXRot(targetPitch);
        } else {
            wyvern.setXRot(0.0F);
        }

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
            // Ground speed - HARDCODED (not configurable)
            // Check if actually moving to prevent sprint animation when standing still
            boolean isMoving = wyvern.getDeltaMovement().horizontalDistanceSqr() > 0.0001;
            boolean running = wyvern.isAccelerating() && isMoving;
            wyvern.setRunning(running);
            return (float) (running ? Raevyx.RIDER_RUN_SPEED : Raevyx.RIDER_WALK_SPEED);
        }
    }

    // ===== RIDING MOVEMENT =====

    /**
     * Handle rider movement - called from travel() method
     * CLEAN SIMPLIFIED PHYSICS - responsive arcade-style flight
     */
    public void handleRiderMovement(Player player, Vec3 motion) {
        // Clear any AI navigation when being ridden
        if (wyvern.getNavigation().getPath() != null) {
            wyvern.getNavigation().stop();
        }

        if (wyvern.isFlying()) {
            // === SETUP ===
            final double baseSpeed = wyvern.getAttributeValue(Attributes.FLYING_SPEED);
            final boolean sprinting = wyvern.isAccelerating();
            final double targetSpeed = (sprinting ? SPRINT_SPEED_MULT : CRUISE_SPEED_MULT) * baseSpeed;

            // Get current velocity (split horizontal and vertical for independent control)
            Vec3 currentVelocity = wyvern.getDeltaMovement();
            Vec3 horizontalVel = new Vec3(currentVelocity.x, 0.0, currentVelocity.z);
            double currentSpeed = horizontalVel.length();

            // === INPUT PROCESSING ===
            double forwardInput = motion.z;   // W/S keys (-1 to 1)
            double strafeInput = motion.x;    // A/D keys (-1 to 1)
            boolean hasInput = Math.abs(forwardInput) > 0.01 || Math.abs(strafeInput) > 0.01;

            // Calculate world-space direction from player input
            float yawRad = (float) Math.toRadians(wyvern.getYRot());
            double forwardX = -Math.sin(yawRad);
            double forwardZ = Math.cos(yawRad);
            double rightX = Math.cos(yawRad);
            double rightZ = Math.sin(yawRad);

            // Combine forward and strafe (strafe is constant power now)
            double targetDirX = forwardX * forwardInput + rightX * (strafeInput * STRAFE_POWER);
            double targetDirZ = forwardZ * forwardInput + rightZ * (strafeInput * STRAFE_POWER);
            double dirLength = Math.hypot(targetDirX, targetDirZ);

            Vec3 newHorizontalVel;

            if (hasInput && dirLength > 0.01) {
                // === ACTIVE FLYING (player is pressing keys) ===
                // Normalize direction
                targetDirX /= dirLength;
                targetDirZ /= dirLength;

                // Calculate target velocity vector
                Vec3 targetVelocity = new Vec3(targetDirX * targetSpeed, 0, targetDirZ * targetSpeed);

                // Smoothly accelerate toward target velocity
                newHorizontalVel = new Vec3(
                    Mth.lerp(ACCELERATION, horizontalVel.x, targetVelocity.x),
                    0,
                    Mth.lerp(ACCELERATION, horizontalVel.z, targetVelocity.z)
                );

                // Apply gentle drag for smooth cruising
                newHorizontalVel = newHorizontalVel.scale(1.0 - DRAG_WITH_INPUT);

            } else {
                // === COASTING (player released keys) ===
                // Apply strong braking for immediate stop feel
                newHorizontalVel = horizontalVel.scale(1.0 - DRAG_NO_INPUT);

                // Stop completely if speed is very low (prevents endless drift)
                if (newHorizontalVel.length() < 0.01) {
                    newHorizontalVel = Vec3.ZERO;
                }
            }

            // === VERTICAL MOVEMENT (dive acceleration, no gravity) ===
            double verticalVel = currentVelocity.y;

            if (wyvern.isGoingUp()) {
                // Apply upward thrust
                verticalVel += ASCEND_THRUST;
            } else if (wyvern.isGoingDown()) {
                // Apply downward thrust - DIVES ACCELERATE!
                verticalVel -= DESCEND_THRUST;
            } else {
                // Coasting - apply air resistance to slow vertical movement
                verticalVel *= VERTICAL_DRAG;
            }

            // Clamp to terminal velocity during dives
            verticalVel = Mth.clamp(verticalVel, -TERMINAL_VELOCITY, TERMINAL_VELOCITY);

            // === APPLY MOVEMENT ===
            Vec3 finalVelocity = new Vec3(newHorizontalVel.x, verticalVel, newHorizontalVel.z);
            wyvern.move(MoverType.SELF, finalVelocity);
            wyvern.setDeltaMovement(finalVelocity);
            wyvern.calculateEntityAnimation(true);

            // Clear fall damage while flying
            player.fallDistance = 0.0F;
            wyvern.fallDistance = 0.0F;
        }
    }

    // ===== RIDING SUPPORT =====
    
    public double getPassengersRidingOffset() {
        return (double) wyvern.getBbHeight() * SEAT_BASE_FACTOR;
    }

    public Vec3 getPassengerPosition(@NotNull Entity passenger) {
        if (!wyvern.hasPassenger(passenger)) {
            return passenger.position();
        }

        Vec3 passengerLoc = wyvern.getClientLocatorPosition("passengerLocator");
        if (passengerLoc != null) {
            Vec3 wyvernOldPos = new Vec3(wyvern.xo, wyvern.yo, wyvern.zo);
            float oldYaw = wyvern.yRotO;
            Vec3 worldOffset = passengerLoc.subtract(wyvernOldPos);

            double oldYawRad = Math.toRadians(-oldYaw);
            double cosOld = Math.cos(oldYawRad);
            double sinOld = Math.sin(oldYawRad);
            double localX = worldOffset.x * cosOld - worldOffset.z * sinOld;
            double localY = worldOffset.y;
            double localZ = worldOffset.x * sinOld + worldOffset.z * cosOld;

            float currentYaw = wyvern.getYRot();
            double currentYawRad = Math.toRadians(-currentYaw);
            double cosCurrent = Math.cos(currentYawRad);
            double sinCurrent = Math.sin(currentYawRad);
            double currentWorldX = localX * cosCurrent + localZ * sinCurrent;
            double currentWorldZ = -localX * sinCurrent + localZ * cosCurrent;

            Vec3 wyvernCurrentPos = wyvern.position();
            return wyvernCurrentPos.add(currentWorldX, localY, currentWorldZ);
        }

        double x = wyvern.getX();
        double y = wyvern.getY() + getPassengersRidingOffset() + passenger.getBbHeight() * 0.5D;
        double z = wyvern.getZ();
        return new Vec3(x, y, z);
    }

    public void positionRider(@NotNull Entity passenger, Entity.@NotNull MoveFunction moveFunction) {
        if (!wyvern.hasPassenger(passenger)) return;

        // Get the bone position from the renderer's cache (updated each render frame)
        Vec3 passengerLoc = wyvern.getClientLocatorPosition("passengerLocator");

        if (passengerLoc != null) {
            // The cached position is in world-space but may be from the previous frame
            // We need to convert to dragon-local space to handle both movement AND rotation

            // Get dragon's old position and rotation (from when bone was sampled)
            Vec3 dragonOldPos = new Vec3(wyvern.xo, wyvern.yo, wyvern.zo);
            float oldYaw = wyvern.yRotO;

            // Calculate offset in world space
            Vec3 worldOffset = passengerLoc.subtract(dragonOldPos);

            // Convert world offset to dragon-local space (relative to old rotation)
            double oldYawRad = Math.toRadians(-oldYaw); // Negative because Minecraft yaw is inverted
            double cosOld = Math.cos(oldYawRad);
            double sinOld = Math.sin(oldYawRad);

            // Rotate world offset back to local space
            double localX = worldOffset.x * cosOld - worldOffset.z * sinOld;
            double localY = worldOffset.y;
            double localZ = worldOffset.x * sinOld + worldOffset.z * cosOld;

            // Now rotate local offset to current rotation
            float currentYaw = wyvern.getYRot();
            double currentYawRad = Math.toRadians(-currentYaw);
            double cosCurrent = Math.cos(currentYawRad);
            double sinCurrent = Math.sin(currentYawRad);

            double currentWorldX = localX * cosCurrent + localZ * sinCurrent;
            double currentWorldZ = -localX * sinCurrent + localZ * cosCurrent;

            // Apply to current dragon position
            Vec3 dragonCurrentPos = wyvern.position();
            Vec3 passengerCurrentPos = dragonCurrentPos.add(currentWorldX, localY, currentWorldZ);

            moveFunction.accept(passenger, passengerCurrentPos.x, passengerCurrentPos.y, passengerCurrentPos.z);
        } else {
            // Fallback to vanilla positioning if bone position not available yet
            Vec3 pos = getPassengerPosition(passenger);
            moveFunction.accept(passenger, pos.x, pos.y, pos.z);
        }
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
        if (lastLand != Long.MIN_VALUE && (now - lastLand) < 5.0) { // ~1s cooldown
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
