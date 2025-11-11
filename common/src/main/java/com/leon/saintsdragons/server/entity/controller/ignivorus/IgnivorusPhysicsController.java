package com.leon.saintsdragons.server.entity.controller.ignivorus;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Physics controller for Ignivorus - handles flight mode computation
 */
public class IgnivorusPhysicsController {
    private final Ignivorus dragon;
    private boolean riderHighAltitudeGlide = false;

    private static final int TAKEOFF_ANIM_MAX_TICKS = 30;   // Match 1.5s animation length (30 ticks at 20 TPS)
    private static final int TAKEOFF_ANIM_EARLY_TICKS = 5; // Start checking conditions slightly earlier

    public IgnivorusPhysicsController(Ignivorus dragon) {
        this.dragon = dragon;
    }

    public void tick() {
        // Future physics logic here
    }

    /**
     * Computes the flight mode for network sync
     * 0 = glide, 1 = flap/forward, 2 = hover, 3 = takeoff, -1 = ground/none
     */
    public int computeFlightModeForSync() {
        if (!dragon.isFlying()) {
            riderHighAltitudeGlide = false;
            return -1;
        }
        if (shouldPlayTakeoff()) {
            riderHighAltitudeGlide = false;
            return 3;
        }

        if (dragon.isHovering() || dragon.isLanding()) {
            riderHighAltitudeGlide = false;
            return 2;
        }

        double altitude = dragon.getY() - dragon.level().getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) dragon.getX(),
                (int) dragon.getZ());

        Vec3 velocity = dragon.getDeltaMovement();
        boolean ascending = velocity.y > 0.02;
        boolean riderAscending = dragon.isVehicle() && dragon.isGoingUp();

        if (isRiddenByOwner()) {
            if (shouldForceSurfaceGlide(altitude)) {
                riderHighAltitudeGlide = false;
                return 0;
            }

            if (ascending || riderAscending) {
                return 1;
            }

            if (riderHighAltitudeGlide) {
                if (altitude > Ignivorus.RIDER_GLIDE_ALTITUDE_EXIT) {
                    return 0;
                }
                riderHighAltitudeGlide = false;
            } else if (altitude > Ignivorus.RIDER_GLIDE_ALTITUDE_THRESHOLD) {
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

    private boolean shouldPlayTakeoff() {
        int timeFlying = dragon.timeFlying;

        if (timeFlying < TAKEOFF_ANIM_EARLY_TICKS) return true;

        boolean airborne = !dragon.onGround();
        boolean ascending = dragon.getDeltaMovement().y > 0.05;

        return (timeFlying < TAKEOFF_ANIM_MAX_TICKS) && (airborne || ascending);
    }

    private boolean isRiddenByOwner() {
        if (!dragon.isTame() || !dragon.isVehicle()) {
            return false;
        }
        if (!(dragon.getControllingPassenger() instanceof Player player)) {
            return false;
        }
        return dragon.isOwnedBy(player);
    }

    private boolean shouldForceSurfaceGlide(double altitudeAboveTerrain) {
        return altitudeAboveTerrain <= Ignivorus.RIDER_LOW_ALTITUDE_GLIDE_THRESHOLD || isNearWaterSurface();
    }

    private boolean isNearWaterSurface() {
        if (dragon.level() == null) {
            return false;
        }

        double dragonY = dragon.getY();
        if (dragonY > Ignivorus.RIDER_WATER_SURFACE_LEVEL + Ignivorus.RIDER_WATER_SURFACE_TOLERANCE) {
            return false;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int baseX = Mth.floor(dragon.getX());
        int baseY = Mth.floor(dragonY);
        int baseZ = Mth.floor(dragon.getZ());

        for (int dx = -Ignivorus.RIDER_WATER_SCAN_RADIUS; dx <= Ignivorus.RIDER_WATER_SCAN_RADIUS; dx++) {
            for (int dz = -Ignivorus.RIDER_WATER_SCAN_RADIUS; dz <= Ignivorus.RIDER_WATER_SCAN_RADIUS; dz++) {
                for (int dy = 0; dy <= Ignivorus.RIDER_WATER_SCAN_DEPTH; dy++) {
                    cursor.set(baseX + dx, baseY - dy, baseZ + dz);
                    if (!dragon.level().hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockState state = dragon.level().getBlockState(cursor);
                    if (!state.getFluidState().isEmpty()) {
                        double surfaceY = cursor.getY() + 1.0;
                        if (Math.abs(dragonY - surfaceY) <= Ignivorus.RIDER_WATER_SURFACE_TOLERANCE) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
