package com.leon.saintsdragons.server.entity.controller.raevyx;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxBeamAbility;
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

    // ===== DIVE BOOST CURVE =====
    private static final float DIVE_START_ANGLE = 25.0f;
    private static final float DIVE_MAX_ANGLE = 90.0f;
    private static final double DIVE_MIN_SPEED_MULT = 1.0;
    private static final double DIVE_MAX_SPEED_MULT = 2.0;
    private static final double DIVE_MIN_ACCEL = 0.35;
    private static final double DIVE_MAX_ACCEL = 0.40;
    private static final double DIVE_MIN_DRAG = 0.08;
    private static final double DIVE_MAX_DRAG = 0.03;
    private static final float DIVE_CURVE_POWER = 2.0f;

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
            // Flying movement - pitch affects vertical direction for 3D flight feel.
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

        // Pitch control - DON'T set entity xRot when flying
        // Visual pitch is handled by velocity-based flightPitchRad in tickPitchingLogic()
        // Only reset pitch when on ground
        if (!flying) {
            wyvern.setXRot(0.0F);
        }
        // When flying, xRot stays at 0 - visual pitch comes from the model's applyFlightPitch()

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
            float base = (float) (running ? Raevyx.RIDER_RUN_SPEED : Raevyx.RIDER_WALK_SPEED);
            return base * wyvern.getHappinessSpeedMultiplier();
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
            double targetSpeed = (sprinting ? SPRINT_SPEED_MULT : CRUISE_SPEED_MULT) * baseSpeed;

            // Get current velocity (split horizontal and vertical for independent control)
            Vec3 currentVelocity = wyvern.getDeltaMovement();

            // === DIVE SPEED BOOST ===
            // Smooth progressive speed boost when diving (like real birds)
            // NOTE: Minecraft xRot is POSITIVE when looking down (90° = straight down)
            float pitchRad = getEffectivePitchRadians(player);
            float pitchDegrees = (float) Math.toDegrees(pitchRad);

            // Calculate smooth dive intensity from 0.0 (shallow) to 1.0 (straight down)
            float diveIntensity = 0.0f;
            if (pitchDegrees >= DIVE_START_ANGLE) {
                // Normalize pitch to 0..1 range between start and max angle
                float normalizedPitch = (pitchDegrees - DIVE_START_ANGLE) / (DIVE_MAX_ANGLE - DIVE_START_ANGLE);
                normalizedPitch = Mth.clamp(normalizedPitch, 0.0f, 1.0f);

                // Apply curve: quadratic (2.0) makes it ramp up faster as you dive steeper
                // linear (1.0) = constant rate, cubic (3.0) = very aggressive late ramp
                diveIntensity = (float) Math.pow(normalizedPitch, DIVE_CURVE_POWER);
            }

            // Smoothly interpolate all dive parameters based on intensity
            double diveMultiplier = Mth.lerp(diveIntensity, DIVE_MIN_SPEED_MULT, DIVE_MAX_SPEED_MULT);
            double diveAcceleration = Mth.lerp(diveIntensity, DIVE_MIN_ACCEL, DIVE_MAX_ACCEL);
            double diveDrag = Mth.lerp(diveIntensity, DIVE_MIN_DRAG, DIVE_MAX_DRAG);

            targetSpeed *= diveMultiplier;

            // === INPUT PROCESSING ===
            double forwardInput = motion.z;   // W/S keys (-1 to 1)
            double strafeInput = motion.x;    // A/D keys (-1 to 1)
            boolean hasInput = Math.abs(forwardInput) > 0.01 || Math.abs(strafeInput) > 0.01;

            // Calculate world-space direction from player input
            // Use PLAYER's pitch for 3D flight direction, not wyvern's (which is 0 for visual reasons)
            float yawRad = (float) Math.toRadians(wyvern.getYRot());
            // pitchRad already calculated above for dive speed
            double forwardXZ = Math.cos(pitchRad);
            double forwardX = -Math.sin(yawRad) * forwardXZ;
            double forwardY = -Math.sin(pitchRad);
            double forwardZ = Math.cos(yawRad) * forwardXZ;
            double rightX = Math.cos(yawRad);
            double rightZ = Math.sin(yawRad);

            // Combine forward and strafe (strafe is constant power now)
            double targetDirX = forwardX * forwardInput + rightX * (strafeInput * STRAFE_POWER);
            double targetDirY = forwardY * forwardInput * 1.35;
            double targetDirZ = forwardZ * forwardInput + rightZ * (strafeInput * STRAFE_POWER);
            double dirLength = Math.sqrt(targetDirX * targetDirX + targetDirY * targetDirY + targetDirZ * targetDirZ);

            Vec3 newVelocity;

            if (hasInput && dirLength > 0.01) {
                // === ACTIVE FLYING (player is pressing keys) ===
                // Normalize direction
                targetDirX /= dirLength;
                targetDirY /= dirLength;
                targetDirZ /= dirLength;

                // Calculate target velocity vector
                Vec3 targetVelocity = new Vec3(
                    targetDirX * targetSpeed,
                    targetDirY * targetSpeed,
                    targetDirZ * targetSpeed
                );

                // Smoothly accelerate toward target velocity (faster when diving)
                newVelocity = new Vec3(
                    Mth.lerp(diveAcceleration, currentVelocity.x, targetVelocity.x),
                    Mth.lerp(diveAcceleration, currentVelocity.y, targetVelocity.y),
                    Mth.lerp(diveAcceleration, currentVelocity.z, targetVelocity.z)
                );

                // Apply drag (reduced when diving for higher top speed)
                newVelocity = newVelocity.scale(1.0 - diveDrag);

            } else {
                // === COASTING (player released keys) ===
                // Apply strong braking for immediate stop feel
                newVelocity = currentVelocity.scale(1.0 - DRAG_NO_INPUT);

                // Stop completely if speed is very low (prevents endless drift)
                if (newVelocity.length() < 0.01) {
                    newVelocity = Vec3.ZERO;
                }
            }

            // === VERTICAL MOVEMENT (ascend/descend overrides) ===
            double verticalVel = newVelocity.y;

            // When diving (pitch >= 45°), use the physics-calculated velocity - don't override!
            // This allows dive speed boost to work properly
            boolean isDiving = pitchDegrees >= 45.0f && hasInput;

            if (!isDiving) {
                // PRIORITY: Respect automated takeoff boost (overrides rider input during takeoff window)
                if (wyvern.getRiderTakeoffTicks() > 0) {
                    // During automated takeoff, apply upward thrust matching normal ascent
                    // to ensure smooth transition when boost window ends
                    verticalVel += ASCEND_THRUST;
                } else if (wyvern.isGoingUp()) {
                    // Apply upward thrust
                    verticalVel += ASCEND_THRUST;
                } else if (wyvern.isGoingDown()) {
                    // Apply downward thrust - DIVES ACCELERATE!
                    verticalVel -= DESCEND_THRUST;
                }

                // Clamp to terminal velocity (only when not diving)
                verticalVel = Mth.clamp(verticalVel, -TERMINAL_VELOCITY, TERMINAL_VELOCITY);
            }
            // When diving, vertical velocity is already calculated by physics above - no override needed!

            // === APPLY MOVEMENT ===
            Vec3 finalVelocity = new Vec3(newVelocity.x, verticalVel, newVelocity.z);
            wyvern.move(MoverType.SELF, finalVelocity);
            wyvern.setDeltaMovement(finalVelocity);
            wyvern.calculateEntityAnimation(true);

            // Clear fall damage while flying
            player.fallDistance = 0.0F;
            wyvern.fallDistance = 0.0F;
        }
    }

    private float getEffectivePitchRadians(Player player) {
        DragonAbility<?> ability = wyvern.getActiveAbility();
        boolean lockPitch = wyvern.isBeaming()
                || (ability instanceof RaevyxBeamAbility && ability.isUsing());
        if (lockPitch) {
            return 0.0f;
        }
        if (wyvern.isRiderPitchKeyMode()) {
            return getKeyPitchRadians();
        }
        return (float) Math.toRadians(player.getXRot());
    }

    private float getKeyPitchRadians() {
        if (wyvern.isGoingUp()) {
            return (float) Math.toRadians(Raevyx.RIDER_KEY_PITCH_DEG);
        }
        if (wyvern.isGoingDown()) {
            return (float) -Math.toRadians(Raevyx.RIDER_KEY_PITCH_DEG);
        }
        return 0.0f;
    }

    // ===== RIDING SUPPORT =====
    
    public double getPassengersRidingOffset() {
        return (double) wyvern.getBbHeight() * SEAT_BASE_FACTOR;
    }
    
    public void positionRider(@NotNull Entity passenger, Entity.@NotNull MoveFunction moveFunction) {
        if (!wyvern.hasPassenger(passenger)) return;

        // Get the bone position from the renderer's cache (updated each render frame)
        // Optimize: Only check cache on client side to avoid map lookup on server
        Vec3 passengerLoc = wyvern.level().isClientSide ? wyvern.getClientLocatorPosition("passengerLocator") : null;

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
            double x = wyvern.getX();
            double y = wyvern.getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset();
            double z = wyvern.getZ();
            moveFunction.accept(passenger, x, y, z);
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

        // Ensure clean state and give the takeoff request some upward intent even before new input packets arrive
        wyvern.getNavigation().stop();
        wyvern.setGoingDown(false);
        wyvern.setGoingUp(true); // Latch ascend so we don't rely on a follow-up look/move packet

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

        // Give an initial upward push so the wyvern actually leaves the ground before rider input changes
        Vec3 current = wyvern.getDeltaMovement();
        double upward = Math.max(current.y, 0.55D); // big first shove to eliminate post-takeoff stutter
        wyvern.setDeltaMovement(current.x, upward, current.z);
        wyvern.hasImpulse = true;
    }
}
