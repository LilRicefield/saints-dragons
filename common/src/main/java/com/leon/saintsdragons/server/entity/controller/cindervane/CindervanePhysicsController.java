package com.leon.saintsdragons.server.entity.controller.cindervane;

import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Physics controller for Cindervane - handles flight mode computation and animation state sync
 * Mirrors the Raevyx physics controller architecture for consistency
 */
public class CindervanePhysicsController {
    private final Cindervane amphithere;
    private boolean riderHighAltitudeGlide = false;

    // Takeoff animation timing - longer than Raevyx due to different animation length
    private static final int TAKEOFF_ANIM_MAX_TICKS = 30;   // Match animation length
    private static final int TAKEOFF_ANIM_EARLY_TICKS = 28; // Start checking conditions slightly earlier

    public CindervanePhysicsController(Cindervane amphithere) {
        this.amphithere = amphithere;
    }

    /**
     * Main tick method - call this from entity's tick()
     */
    public void tick() {
        // Future: Can add physics envelopes here like Raevyx if needed
    }

    /**
     * Computes the flight mode for network sync
     * 0 = glide, 1 = flap/forward, 2 = hover, 3 = takeoff, -1 = ground/none
     */
    public int computeFlightModeForSync() {
        if (!amphithere.isFlying()) {
            riderHighAltitudeGlide = false;
            return -1;
        }
        if (shouldPlayTakeoff()) {
            riderHighAltitudeGlide = false;
            return 3;
        }

        if (amphithere.isHovering() || amphithere.isLanding()) {
            riderHighAltitudeGlide = false;
            return 2;
        }

        double altitude = amphithere.getY() - amphithere.level().getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) amphithere.getX(),
                (int) amphithere.getZ());

        Vec3 velocity = amphithere.getDeltaMovement();
        boolean ascending = velocity.y > 0.02;
        boolean riderAscending = amphithere.isVehicle() && amphithere.isGoingUp();

        if (isRiddenByOwner()) {
            if (shouldForceSurfaceGlide(altitude)) {
                riderHighAltitudeGlide = false;
                return 0;
            }

            if (ascending || riderAscending) {
                return 1;
            }

            if (riderHighAltitudeGlide) {
                if (altitude > Cindervane.RIDER_GLIDE_ALTITUDE_EXIT) {
                    return 0;
                }
                riderHighAltitudeGlide = false;
            } else if (altitude > Cindervane.RIDER_GLIDE_ALTITUDE_THRESHOLD) {
                riderHighAltitudeGlide = true;
                return 0;
            }

            return 1;
        } else {
            riderHighAltitudeGlide = false;
        }

        if (ascending || riderAscending) {
            return 1;
        }

        return altitude > 35.0 ? 0 : 1;
    }

    /**
     * Determines if takeoff animation should play
     * Mirrors Raevyx logic but with longer timing for Cindervane's longer animation
     */
    private boolean shouldPlayTakeoff() {
        // Get timeFlying from entity
        int timeFlying = amphithere.getTimeFlying();

        // Play takeoff at the very start of flight
        if (timeFlying < TAKEOFF_ANIM_EARLY_TICKS) return true;

        // Continue playing if still within max ticks AND conditions are met
        boolean airborne = !amphithere.onGround();
        boolean ascending = amphithere.getDeltaMovement().y > 0.05;

        return (timeFlying < TAKEOFF_ANIM_MAX_TICKS) && (airborne || ascending);
    }

    /**
     * Save/load support for future physics state (envelopes, etc.)
     */
    public void writeToNBT(net.minecraft.nbt.CompoundTag tag) {
        // Future: save physics envelope values if we add them
    }

    public void readFromNBT(net.minecraft.nbt.CompoundTag tag) {
        // Future: restore physics envelope values if we add them
    }

    private boolean isRiddenByOwner() {
        if (!amphithere.isTame() || !amphithere.isVehicle()) {
            return false;
        }
        if (!(amphithere.getControllingPassenger() instanceof Player player)) {
            return false;
        }
        return amphithere.isOwnedBy(player);
    }

    private boolean shouldForceSurfaceGlide(double altitudeAboveTerrain) {
        return altitudeAboveTerrain <= Cindervane.RIDER_LOW_ALTITUDE_GLIDE_THRESHOLD || isNearWaterSurface();
    }

    private boolean isNearWaterSurface() {
        if (amphithere.level() == null) {
            return false;
        }

        double dragonY = amphithere.getY();
        if (dragonY > Cindervane.RIDER_WATER_SURFACE_LEVEL + Cindervane.RIDER_WATER_SURFACE_TOLERANCE) {
            return false;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int baseX = Mth.floor(amphithere.getX());
        int baseY = Mth.floor(dragonY);
        int baseZ = Mth.floor(amphithere.getZ());

        for (int dx = -Cindervane.RIDER_WATER_SCAN_RADIUS; dx <= Cindervane.RIDER_WATER_SCAN_RADIUS; dx++) {
            for (int dz = -Cindervane.RIDER_WATER_SCAN_RADIUS; dz <= Cindervane.RIDER_WATER_SCAN_RADIUS; dz++) {
                for (int dy = 0; dy <= Cindervane.RIDER_WATER_SCAN_DEPTH; dy++) {
                    cursor.set(baseX + dx, baseY - dy, baseZ + dz);
                    if (!amphithere.level().hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockState state = amphithere.level().getBlockState(cursor);
                    if (!state.getFluidState().isEmpty()) {
                        double surfaceY = cursor.getY() + 1.0;
                        if (Math.abs(dragonY - surfaceY) <= Cindervane.RIDER_WATER_SURFACE_TOLERANCE) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
