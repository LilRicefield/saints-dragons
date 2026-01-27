package com.leon.saintsdragons.server.entity.controller.ignivorus;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireBreathAbility;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
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
 * Handles all riding mechanics for the Ignivorus
 */
public record IgnivorusRiderController(Ignivorus dragon) {

    private static final double SEAT_BASE_FACTOR = 0.50D; // for fallback if no bone was found

    // ===== GROUND MOVEMENT SPEED MULTIPLIERS =====

    // ===== LANDING LOGIC =====
    private static final double LANDING_HEIGHT_TRIGGER = 4.0D; // Blocks above ground to start landing animation
    private static final int MAX_GROUND_CHECK_DISTANCE = 10; // Max blocks to check below dragon

    // ===== FLIGHT PHYSICS =====
    private static final double CRUISE_SPEED_MULT = 8.5;
    private static final double SPRINT_SPEED_MULT = 12.5;
    private static final double ACCELERATION = 0.15;
    private static final double DRAG_WITH_INPUT = 0.08;
    private static final double DRAG_NO_INPUT = 0.5;
    private static final double STRAFE_POWER = 0.5;

    // ===== VERTICAL PHYSICS =====
    private static final double ASCEND_THRUST = 0.08D;
    private static final double DESCEND_THRUST = 1.0D;
    private static final double TERMINAL_VELOCITY = 1.5D;
    private static final double VERTICAL_DRAG = 0.92D;

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

    @Nullable
    public Player getRidingPlayer() {
        if (dragon.getControllingPassenger() instanceof Player player) {
            return player;
        }
        return null;
    }

    public Vec3 getRiddenInput(Player player, Vec3 deltaIn) {
        float f = player.zza < 0.0F ? 0.5F : 1.0F;
        if (dragon.isFlying()) {
            return new Vec3(player.xxa * 0.4F, 0.0F, player.zza * 1.0F * f);
        } else {
            return new Vec3(player.xxa * 0.5F, 0.0D, player.zza * 0.9F * f);
        }
    }

    public void tickRidden(Player player, Vec3 travelVector) {
        player.fallDistance = 0.0F;
        dragon.fallDistance = 0.0F;
        dragon.setTarget(null);

        boolean flying = dragon.isFlying();

        // Always sync yaw with player's look direction
        float currentYaw = dragon.getYRot();
        float targetYaw = player.getYRot();
        float rawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float blend = flying ? 0.35f : 0.28f;
        float newYaw = currentYaw + (rawDiff * blend);

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

        // Landing logic for riders
        if (flying && !dragon.isTakeoff()) {
            double distanceToGround = getDistanceToGround();
            boolean nearGround = distanceToGround >= 0 && distanceToGround <= LANDING_HEIGHT_TRIGGER;
            boolean atWaterSurface = isNearWaterSurface();

            // Trigger landing animation when close to ground, descending, and not at water surface
            if (nearGround && dragon.isGoingDown() && !atWaterSurface && !dragon.isLanding()) {
                dragon.setLanding(true);
            }

            // Complete landing when touching ground
            if (dragon.onGround()) {
                dragon.setFlying(false);
                dragon.setLanding(false);
                dragon.setTakeoff(false);
            }
        }

        if (dragon.onGround()) {
            player.fallDistance = 0.0F;
            dragon.fallDistance = 0.0F;
        }
    }

    /**
     * Check distance to solid ground below the dragon
     * @return Distance in blocks, or -1 if no ground found within MAX_GROUND_CHECK_DISTANCE
     */
    private double getDistanceToGround() {
        var level = dragon.level();
        if (level == null) return -1;

        net.minecraft.core.BlockPos dragonPos = dragon.blockPosition();
        int startY = dragonPos.getY();

        for (int checkY = startY; checkY > startY - MAX_GROUND_CHECK_DISTANCE && checkY >= level.getMinBuildHeight(); checkY--) {
            net.minecraft.core.BlockPos checkPos = new net.minecraft.core.BlockPos(dragonPos.getX(), checkY, dragonPos.getZ());
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(checkPos);

            // Check if it's a solid block (not air, not water, not lava)
            if (!state.isAir() && state.getFluidState().isEmpty() && state.isSolidRender(level, checkPos)) {
                // Found solid ground, return distance
                return dragon.getY() - (checkY + 1); // +1 because we want distance to top of block
            }
        }

        return -1; // No ground found
    }

    /**
     * Check if dragon is near water surface level (Y=62)
     * @return true if within tolerance of water surface
     */
    private boolean isNearWaterSurface() {
        double dragonY = dragon.getY();
        return Math.abs(dragonY - Ignivorus.RIDER_WATER_SURFACE_LEVEL) <= Ignivorus.RIDER_WATER_SURFACE_TOLERANCE;
    }

    public float getRiddenSpeed(Player rider) {
        if (dragon.isFlying()) {
            return (float) dragon.getAttributeValue(Attributes.FLYING_SPEED);
        }

        // Check if actually moving to prevent sprint animation when standing still
        boolean isMoving = dragon.getDeltaMovement().horizontalDistanceSqr() > 0.0001;

        // Check if in Phase 2 mode for slower speeds
        boolean isPhase2 = dragon.getEntityData().get(Ignivorus.DATA_PHASE2);

        if (dragon.isAccelerating() && isMoving) {
            // L-Ctrl pressed AND moving - trigger run animation and use appropriate run speed
            dragon.setRunning(true);
            return (float) (isPhase2 ? Ignivorus.RIDER_PHASE2_RUN_SPEED : Ignivorus.RIDER_RUN_SPEED);
        } else {
            // Normal ground speed - use walk animation and appropriate walk speed
            dragon.setRunning(false);
            return (float) (isPhase2 ? Ignivorus.RIDER_PHASE2_WALK_SPEED : Ignivorus.RIDER_WALK_SPEED);
        }
    }

    public void handleRiderMovement(Player player, Vec3 motion) {
        if (dragon.getNavigation().getPath() != null) {
            dragon.getNavigation().stop();
        }

        // Ground movement is handled by Ignivorus.travel() calling super.travel()
        if (dragon.isFlying()) {
            final double baseSpeed = dragon.getAttributeValue(Attributes.FLYING_SPEED);
            final boolean sprinting = dragon.isAccelerating();
            double targetSpeed = (sprinting ? SPRINT_SPEED_MULT : CRUISE_SPEED_MULT) * baseSpeed;

            Vec3 currentVelocity = dragon.getDeltaMovement();

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

            double forwardInput = motion.z;
            double strafeInput = motion.x;
            boolean hasInput = Math.abs(forwardInput) > 0.01 || Math.abs(strafeInput) > 0.01;

            // Calculate world-space direction from player input
            // Use PLAYER's pitch for 3D flight direction, not dragon's (which is 0 for visual reasons)
            float yawRad = (float) Math.toRadians(dragon.getYRot());
            // pitchRad already calculated above for dive speed
            double forwardXZ = Math.cos(pitchRad);
            double forwardX = -Math.sin(yawRad) * forwardXZ;
            double forwardY = -Math.sin(pitchRad);
            double forwardZ = Math.cos(yawRad) * forwardXZ;
            double rightX = Math.cos(yawRad);
            double rightZ = Math.sin(yawRad);

            double targetDirX = forwardX * forwardInput + rightX * (strafeInput * STRAFE_POWER);
            double targetDirY = forwardY * forwardInput * 1.35;
            double targetDirZ = forwardZ * forwardInput + rightZ * (strafeInput * STRAFE_POWER);
            double dirLength = Math.sqrt(targetDirX * targetDirX + targetDirY * targetDirY + targetDirZ * targetDirZ);

            Vec3 newVelocity;

            if (hasInput && dirLength > 0.01) {
                targetDirX /= dirLength;
                targetDirY /= dirLength;
                targetDirZ /= dirLength;

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
                newVelocity = currentVelocity.scale(1.0 - DRAG_NO_INPUT);
                if (newVelocity.length() < 0.01) {
                    newVelocity = Vec3.ZERO;
                }
            }

            double verticalVel = newVelocity.y;

            // When diving (pitch >= 45°), use the physics-calculated velocity - don't override!
            // This allows dive speed boost to work properly
            boolean isDiving = pitchDegrees >= 45.0f && hasInput;

            if (!isDiving) {
                // Vertical control - takeoff provides optional boost but doesn't block descent
                if (dragon.isTakeoff() && dragon.isGoingUp()) {
                    // Apply modest boost during takeoff if Space is held
                    double boost = ASCEND_THRUST * 0.65;
                    verticalVel = Math.max(verticalVel + boost, 0.20);
                } else if (dragon.isGoingUp()) {
                    verticalVel += ASCEND_THRUST;
                } else if (dragon.isGoingDown()) {
                    verticalVel -= DESCEND_THRUST;
                }
                // Clamp to terminal velocity (only when not diving)
                verticalVel = Mth.clamp(verticalVel, -TERMINAL_VELOCITY, TERMINAL_VELOCITY);
            }
            // When diving, vertical velocity is already calculated by physics above - no override needed!

            Vec3 finalVelocity = new Vec3(newVelocity.x, verticalVel, newVelocity.z);
            dragon.move(MoverType.SELF, finalVelocity);
            dragon.setDeltaMovement(finalVelocity);
            dragon.calculateEntityAnimation(true);

            player.fallDistance = 0.0F;
            dragon.fallDistance = 0.0F;
        }
    }

    private float getEffectivePitchRadians(Player player) {
        DragonAbility<?> ability = dragon.getActiveAbility();
        boolean lockPitch = dragon.isBreathingFire()
                || (ability instanceof IgnivorusFireBreathAbility && ability.isUsing());
        if (lockPitch) {
            return 0.0f;
        }
        if (dragon.isRiderPitchKeyMode()) {
            return getKeyPitchRadians();
        }
        return (float) Math.toRadians(player.getXRot());
    }

    private float getKeyPitchRadians() {
        if (dragon.isGoingUp()) {
            return (float) -Math.toRadians(Ignivorus.RIDER_KEY_PITCH_DEG);
        }
        if (dragon.isGoingDown()) {
            return (float) Math.toRadians(Ignivorus.RIDER_KEY_PITCH_DEG);
        }
        return 0.0f;
    }

    public double getPassengersRidingOffset() {
        return (double) dragon.getBbHeight() * SEAT_BASE_FACTOR;
    }

    public void positionRider(@NotNull Entity passenger, Entity.@NotNull MoveFunction moveFunction) {
        if (!dragon.hasPassenger(passenger)) return;

        Vec3 passengerLoc = dragon.level().isClientSide ? dragon.getClientLocatorPosition("passengerLocator") : null;

        if (passengerLoc != null) {
            Vec3 dragonOldPos = new Vec3(dragon.xo, dragon.yo, dragon.zo);
            float oldYaw = dragon.yRotO;
            Vec3 worldOffset = passengerLoc.subtract(dragonOldPos);

            double oldYawRad = Math.toRadians(-oldYaw);
            double cosOld = Math.cos(oldYawRad);
            double sinOld = Math.sin(oldYawRad);
            double localX = worldOffset.x * cosOld - worldOffset.z * sinOld;
            double localY = worldOffset.y;
            double localZ = worldOffset.x * sinOld + worldOffset.z * cosOld;

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
            double x = dragon.getX();
            double y = dragon.getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset();
            double z = dragon.getZ();
            moveFunction.accept(passenger, x, y, z);
        }
    }

    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        passenger.fallDistance = 0.0F;
        Vec3 base = dragon.position();
        Vec3 direction = dragon.getViewVector(1.0F);
        return base.add(direction.scale(2.0));
    }

    @Nullable
    public LivingEntity getControllingPassenger() {
        Entity entity = dragon.getFirstPassenger();
        if (entity instanceof Player player && dragon.isTame() && dragon.isOwnedBy(player)) {
            return player;
        }
        return null;
    }

    public void requestRiderTakeoff() {
        if (!dragon.isTame() || getRidingPlayer() == null || dragon.isFlying()) return;

        dragon.getNavigation().stop();
        dragon.setGoingDown(false);
        dragon.setGoingUp(true); // latch ascend intent at takeoff so holding Space keeps climb

        dragon.timeFlying = 0;
        dragon.setFlying(true);
        dragon.setTakeoff(true);
        dragon.setHovering(false);
        dragon.setLanding(false);

        Vec3 current = dragon.getDeltaMovement();
        double upward = Math.max(current.y, 0.25D); // slightly stronger initial shove but still controlled
        dragon.setDeltaMovement(current.x, upward, current.z);
        dragon.hasImpulse = true;
    }
}
