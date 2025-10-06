package com.leon.saintsdragons.server.entity.controller.riftdrake;

import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Handles all riding mechanics for the Rift Drake
 * Ground-based dragon with aquatic capabilities
 */
public record RiftDrakeRiderController(RiftDrakeEntity dragon) {
    // ===== SEAT TUNING CONSTANTS =====
    // Baseline vertical offset relative to dragon height
    private static final double SEAT_BASE_FACTOR = 0.45D; // 0.0..1.0 of bbHeight
    // Additional vertical lift to avoid clipping
    private static final double SEAT_LIFT = 0.75D;
    private static final double PHASE_TWO_LIFT = 3.5D;
    // Forward/back relative to body (blocks). +forward = toward head, - = toward tail
    private static final double SEAT_FORWARD = 3.0D;
    // Sideways relative to body (blocks). +side = to the dragon's right, - = left
    private static final double SEAT_SIDE = 0.00D;

    // ===== GROUND MOVEMENT TUNING =====
    private static final double GROUND_SPEED_MULT = 0.50D;  // Base ground speed multiplier
    private static final double WATER_SPEED_MULT = 1.2D;  // Enhanced speed in water

    // ===== RIDING UTILITIES =====

    @Nullable
    public Player getRidingPlayer() {
        if (dragon.getFirstPassenger() instanceof Player player) {
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

        if (dragon.isInWater()) {
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
        dragon.fallDistance = 0.0F;
        
        // Clear target when being ridden to prevent AI interference
        dragon.setTarget(null);
        
        // Make dragon responsive to player look direction - use conditional like other dragons
        float yawDiff = Math.abs(player.getYRot() - dragon.getYRot());
        if (player.zza != 0 || player.xxa != 0 || yawDiff > 5.0f) {
            float currentYaw = dragon.getYRot();
            float targetYaw = player.getYRot();
            float rawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
            float blend = dragon.isPhaseTwoActive() ? 0.28f : 0.9f;
            float newYaw = currentYaw + (rawDiff * blend);
            
            // Set rotation
            dragon.setYRot(newYaw);
            dragon.setXRot(0.0F);
            
            // Force entity to be dirty for network sync
            dragon.setDeltaMovement(dragon.getDeltaMovement());
            
            // Update body and head rotation
            dragon.yBodyRot = newYaw;
            dragon.yHeadRot = newYaw;
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
        float baseSpeed = (float) dragon.getAttributeValue(Attributes.MOVEMENT_SPEED);
        
        if (dragon.isInWater()) {
            // Enhanced speed in water
            float swimSpeed = (float) dragon.getSwimSpeed();
            return dragon.isAccelerating() ? swimSpeed * (float)WATER_SPEED_MULT : swimSpeed;
        } else {
            // Ground movement with sprint capability
            return dragon.isAccelerating() ? baseSpeed * 1.0F : baseSpeed * (float)GROUND_SPEED_MULT;
        }
    }

    /**
     * Get the riding offset for passengers
     */
    private double computeSeatY() {
        double seat = (dragon.getBbHeight() * SEAT_BASE_FACTOR) + SEAT_LIFT;
        if (dragon.isPhaseTwoActive()) {
            seat += PHASE_TWO_LIFT;
        }
        return seat;
    }

    public double getPassengersRidingOffset() {
        return computeSeatY();
    }

    /**
     * Position a rider on the dragon
     */
    public void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (passenger == null) return;

        double seatY = computeSeatY();
        double seatForward = SEAT_FORWARD;
        double seatSide = SEAT_SIDE;

        // Convert dragon-relative offsets to world coordinates
        Vec3 forward = Vec3.directionFromRotation(0.0F, dragon.getYRot());
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);

        Vec3 offset = forward.scale(seatForward)
                .add(right.scale(seatSide))
                .add(0.0D, seatY, 0.0D);

        moveFunction.accept(passenger, dragon.getX() + offset.x, dragon.getY() + offset.y, dragon.getZ() + offset.z);
    }

    /**
     * Get dismount location for a passenger
     */
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // Simple dismount - place passenger beside the dragon
        Vec3 dragonPos = dragon.position();
        Vec3 forward = Vec3.directionFromRotation(0.0F, dragon.getYRot());
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        
        // Place passenger to the right side of the dragon
        return dragonPos.add(right.scale(2.0D));
    }

    /**
     * Get the controlling passenger (rider)
     */
    @Nullable
    public Player getControllingPassenger() {
        return getRidingPlayer();
    }
}
