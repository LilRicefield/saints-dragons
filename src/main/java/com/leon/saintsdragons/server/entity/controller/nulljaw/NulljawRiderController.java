package com.leon.saintsdragons.server.entity.controller.nulljaw;

import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Handles all riding mechanics for the Rift Drake
 * Ground-based drake with aquatic capabilities
 */
public record NulljawRiderController(Nulljaw drake) {
    // ===== SEAT TUNING CONSTANTS =====
    // Baseline vertical offset relative to drake height (FALLBACK ONLY - bone positioning is preferred)
    private static final double SEAT_BASE_FACTOR = 0.45D; // 0.0..1.0 of bbHeight
    // Additional vertical lift to avoid clipping (FALLBACK ONLY)
    private static final double SEAT_LIFT = 0.75D;
    // Forward/back relative to body (blocks). +forward = toward head, - = toward tail (FALLBACK ONLY)
    private static final double SEAT_FORWARD = 3.0D;
    // Sideways relative to body (blocks). +side = to the drake's right, - = left (FALLBACK ONLY)
    private static final double SEAT_SIDE = 0.00D;

    // ===== GROUND MOVEMENT TUNING =====
    private static final double GROUND_SPEED_MULT = 0.50D;  // Base ground speed multiplier
    private static final double WATER_SPEED_MULT = 1.2D;  // Enhanced speed in water

    // ===== RIDING UTILITIES =====

    @Nullable
    public Player getRidingPlayer() {
        if (drake.getFirstPassenger() instanceof Player player) {
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

        if (drake.isInWater()) {
            // Aquatic movement - enhanced responsiveness in water
            return new Vec3(player.xxa * 0.6F, 0.0F, player.zza * 1.0F * f);
        } else {
            // Ground movement - standard responsiveness
            return new Vec3(player.xxa * 0.5F, 0.0D, player.zza * 0.9F * f);
        }
    }

    /**
     * Main rider tick method - handles rotation and movement state
     */
    public void tickRidden(Player player, @SuppressWarnings("unused") Vec3 travelVector) {
        // Prevent accidental rider fall damage while mounted
        player.fallDistance = 0.0F;
        drake.fallDistance = 0.0F;
        
        // Clear target when being ridden to prevent AI interference
        drake.setTarget(null);
        
        // Make drake responsive to player look direction - use conditional like other dragons
        float yawDiff = Math.abs(player.getYRot() - drake.getYRot());
        if (player.zza != 0 || player.xxa != 0 || yawDiff > 5.0f) {
            float currentYaw = drake.getYRot();
            float targetYaw = player.getYRot();
            float rawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
            float blend = drake.isPhaseTwoActive() ? 0.28f : 0.9f;
            float newYaw = currentYaw + (rawDiff * blend);
            
            // Set rotation
            drake.setYRot(newYaw);
            drake.setXRot(0.0F);
            
            // Force entity to be dirty for network sync
            drake.setDeltaMovement(drake.getDeltaMovement());
            
            // Update body and head rotation
            drake.yBodyRot = newYaw;
            drake.yHeadRot = newYaw;
        }
    }

    /**
     * Handle rider movement - NOT USED for ground-based dragons
     * Ground movement is handled directly in travel() method using super.travel()
     */
    public void handleRiderMovement(Player player, Vec3 motion) {
        // This method should not be called for ground-based dragons
        // Ground movement is handled directly in the travel() method
        throw new UnsupportedOperationException("handleRiderMovement should not be called for ground-based dragons");
    }

    /**
     * Get the speed for ridden movement
     */
    public float getRiddenSpeed(Player player) {
        float baseSpeed = (float) drake.getAttributeValue(Attributes.MOVEMENT_SPEED);
        
        if (drake.isInWater()) {
            // Enhanced speed in water
            float swimSpeed = (float) drake.getSwimSpeed();
            return drake.isAccelerating() ? swimSpeed * (float)WATER_SPEED_MULT : swimSpeed;
        } else {
            // Ground movement with sprint capability
            return drake.isAccelerating() ? baseSpeed * 1.0F : baseSpeed * (float)GROUND_SPEED_MULT;
        }
    }

    /**
     * Get the riding offset for passengers (FALLBACK ONLY)
     */
    public double getPassengersRidingOffset() {
        return (drake.getBbHeight() * SEAT_BASE_FACTOR) + SEAT_LIFT;
    }

    /**
     * Position a rider on the drake using bone-based positioning
     */
    public void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (passenger == null) return;

        // Get the bone position from the renderer's cache (updated each render frame)
        Vec3 passengerLoc = drake.getClientLocatorPosition("passengerLocator");

        if (passengerLoc != null) {
            // The cached position is in world-space but may be from the previous frame
            // We need to convert to drake-local space to handle both movement AND rotation

            // Get drake's old position and rotation (from when bone was sampled)
            Vec3 drakeOldPos = new Vec3(drake.xo, drake.yo, drake.zo);
            float oldYaw = drake.yRotO;

            // Calculate offset in world space
            Vec3 worldOffset = passengerLoc.subtract(drakeOldPos);

            // Convert world offset to drake-local space (relative to old rotation)
            double oldYawRad = Math.toRadians(-oldYaw); // Negative because Minecraft yaw is inverted
            double cosOld = Math.cos(oldYawRad);
            double sinOld = Math.sin(oldYawRad);

            // Rotate world offset back to local space
            double localX = worldOffset.x * cosOld - worldOffset.z * sinOld;
            double localY = worldOffset.y;
            double localZ = worldOffset.x * sinOld + worldOffset.z * cosOld;

            // Now rotate local offset to current rotation
            float currentYaw = drake.getYRot();
            double currentYawRad = Math.toRadians(-currentYaw);
            double cosCurrent = Math.cos(currentYawRad);
            double sinCurrent = Math.sin(currentYawRad);

            double currentWorldX = localX * cosCurrent + localZ * sinCurrent;
            double currentWorldZ = -localX * sinCurrent + localZ * cosCurrent;

            // Apply to current drake position
            Vec3 drakeCurrentPos = drake.position();
            Vec3 passengerCurrentPos = drakeCurrentPos.add(currentWorldX, localY, currentWorldZ);

            moveFunction.accept(passenger, passengerCurrentPos.x, passengerCurrentPos.y, passengerCurrentPos.z);
        } else {
            // Fallback to vanilla positioning if bone position not available yet
            // This handles server-side and initial frames before renderer updates the cache
            double seatY = getPassengersRidingOffset();
            Vec3 forward = Vec3.directionFromRotation(0.0F, drake.getYRot());
            Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
            Vec3 offset = forward.scale(SEAT_FORWARD)
                    .add(right.scale(SEAT_SIDE))
                    .add(0.0D, seatY, 0.0D);

            moveFunction.accept(passenger, drake.getX() + offset.x, drake.getY() + offset.y, drake.getZ() + offset.z);
        }
    }

    /**
     * Get dismount location for a passenger - finds safe ground position
     * Based on vanilla AbstractHorse dismount logic
     */
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // Try right side first (based on passenger's main hand)
        Vec3 vec3 = getCollisionHorizontalEscapeVector(
            drake.getBbWidth(),
            passenger.getBbWidth(),
            drake.getYRot() + (passenger.getMainArm() == HumanoidArm.RIGHT ? 90.0F : -90.0F)
        );
        Vec3 rightSide = getDismountLocationInDirection(vec3, passenger);
        if (rightSide != null) {
            return rightSide;
        }

        // Try left side if right side failed
        Vec3 vec32 = getCollisionHorizontalEscapeVector(
            drake.getBbWidth(),
            passenger.getBbWidth(),
            drake.getYRot() + (passenger.getMainArm() == HumanoidArm.LEFT ? 90.0F : -90.0F)
        );
        Vec3 leftSide = getDismountLocationInDirection(vec32, passenger);
        return leftSide != null ? leftSide : drake.position();
    }

    /**
     * Calculate horizontal escape vector for dismounting
     * Reimplementation of Entity.getCollisionHorizontalEscapeVector (which is protected)
     */
    private static Vec3 getCollisionHorizontalEscapeVector(double entityWidth, double passengerWidth, float yaw) {
        double d0 = (entityWidth + passengerWidth + 1.0E-5F) / 2.0D;
        float f = -Mth.sin(yaw * ((float)Math.PI / 180F));
        float f1 = Mth.cos(yaw * ((float)Math.PI / 180F));
        float f2 = Math.max(Math.abs(f), Math.abs(f1));
        return new Vec3((double)f * d0 / (double)f2, 0.0D, (double)f1 * d0 / (double)f2);
    }

    /**
     * Finds a safe dismount location in the given direction by scanning upward for valid ground
     * Based on vanilla AbstractHorse.getDismountLocationInDirection
     */
    @Nullable
    private Vec3 getDismountLocationInDirection(Vec3 offset, LivingEntity passenger) {
        double targetX = drake.getX() + offset.x;
        double minY = drake.getBoundingBox().minY;
        double targetZ = drake.getZ() + offset.z;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // Try all possible poses (standing, crouching, etc.)
        for (Pose pose : passenger.getDismountPoses()) {
            pos.set(targetX, minY, targetZ);
            double maxY = drake.getBoundingBox().maxY + 0.75D;

            // Scan upward to find valid ground
            while (true) {
                double floorHeight = drake.level().getBlockFloorHeight(pos);
                if ((double)pos.getY() + floorHeight > maxY) {
                    break;
                }

                if (DismountHelper.isBlockFloorValid(floorHeight)) {
                    AABB aabb = passenger.getLocalBoundsForPose(pose);
                    Vec3 dismountPos = new Vec3(targetX, (double)pos.getY() + floorHeight, targetZ);
                    if (DismountHelper.canDismountTo(drake.level(), passenger, aabb.move(dismountPos))) {
                        passenger.setPose(pose);
                        return dismountPos;
                    }
                }

                pos.move(Direction.UP);
                if (!((double)pos.getY() < maxY)) {
                    break;
                }
            }
        }

        return null;
    }

    /**
     * Get the controlling passenger (rider)
     */
    @Nullable
    public Player getControllingPassenger() {
        return getRidingPlayer();
    }
}
