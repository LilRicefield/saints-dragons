package com.leon.saintsdragons.server.entity.controller.raevyx;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxBeamAbility;
import com.leon.saintsdragons.server.flight.DragonRiderFlightPhysics;
import com.leon.saintsdragons.server.flight.DragonRiderSeat;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record RaevyxRiderController(Raevyx wyvern) {
    private static final double SEAT_BASE_FACTOR = 0.50D; // 0.0..1.0 of bbHeight
    private static final double SEAT_HEIGHT_ADJUST = 0.00D;
    private static final double CRUISE_SPEED_MULT = 4.95;      // Use configured flight speed directly
    private static final double SPRINT_SPEED_MULT = 5.75;      // No hidden rider speed boost     // Gentle drag when player is actively flying
    private static final double DRAG_NO_INPUT = 0.5;         // Strong braking when player releases controls
    private static final double STRAFE_POWER = 0.5;
    private static final double ASCEND_THRUST = 1.2D;       // Upward thrust when climbing
    private static final double DESCEND_THRUST = 1.0D;       // Downward thrust when diving (accelerates)
    private static final double TERMINAL_VELOCITY = 1.5D;    // Max falling speed

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
            return (float) getMountedFlightBaseSpeed();
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
            final double baseSpeed = getMountedFlightBaseSpeed();
            final boolean sprinting = wyvern.isAccelerating();
            double targetSpeed = (sprinting ? SPRINT_SPEED_MULT : CRUISE_SPEED_MULT) * baseSpeed;

            // Get current velocity (split horizontal and vertical for independent control)
            Vec3 currentVelocity = wyvern.getDeltaMovement();

            // === DIVE SPEED BOOST ===
            // Smooth progressive speed boost when diving (like real birds)
            // NOTE: Minecraft xRot is POSITIVE when looking down (90° = straight down)
            final boolean keyPitchMode = wyvern.isRiderPitchKeyMode();
            float pitchRad = getEffectivePitchRadians(player);
            if (keyPitchMode) {
                // In pitch-lock mode, forward input should not alter altitude.
                pitchRad = 0.0f;
            }
            float pitchDegrees = (float) Math.toDegrees(pitchRad);

            DragonRiderFlightPhysics.DiveResponse diveResponse =
                    DragonRiderFlightPhysics.computeDiveResponse(pitchDegrees, keyPitchMode);
            double diveMultiplier = diveResponse.speedMultiplier();
            double diveAcceleration = diveResponse.acceleration();
            double diveDrag = diveResponse.drag();

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
            double forwardY = keyPitchMode ? 0.0 : -Math.sin(pitchRad);
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
            boolean isDiving = !keyPitchMode && pitchDegrees >= 45.0f && hasInput;

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
            return (float) -Math.toRadians(Raevyx.RIDER_KEY_PITCH_DEG);
        }
        if (wyvern.isGoingDown()) {
            return (float) Math.toRadians(Raevyx.RIDER_KEY_PITCH_DEG);
        }
        return 0.0f;
    }

    private double getMountedFlightBaseSpeed() {
        return wyvern.getFlightSpeed();
    }

    // ===== RIDING SUPPORT =====
    
    public double getPassengersRidingOffset() {
        return (double) wyvern.getBbHeight() * SEAT_BASE_FACTOR;
    }
    
    public void positionRider(@NotNull Entity passenger, Entity.@NotNull MoveFunction moveFunction) {
        DragonRiderSeat.positionLocatorRider(
                wyvern,
                passenger,
                moveFunction,
                getPassengersRidingOffset() + SEAT_HEIGHT_ADJUST,
                wyvern.getClientLocatorPosition("passengerLocator")
        );
    }
    
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        return DragonRiderSeat.findRadialGroundDismount(
                passenger,
                wyvern,
                new double[] { 2.5D, 3.5D, 1.8D },
                new int[] { 0, 30, -30, 60, -60, 90, -90, 150, -150, 180 },
                6,
                2.0D
        );
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
        wyvern.requestRiderTakeoff();
    }

}
