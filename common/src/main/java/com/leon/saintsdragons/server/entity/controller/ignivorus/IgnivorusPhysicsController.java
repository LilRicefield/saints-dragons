package com.leon.saintsdragons.server.entity.controller.ignivorus;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.phys.Vec3;

/**
 * Physics controller for Ignivorus - handles flight mode computation
 */
public class IgnivorusPhysicsController {
    private final Ignivorus dragon;

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
        if (!dragon.isFlying()) return -1;
        if (shouldPlayTakeoff()) return 3;

        if (dragon.isHovering() || dragon.isLanding()) return 2;

        double altitude = dragon.getY() - dragon.level().getHeight(
            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (int) dragon.getX(),
            (int) dragon.getZ()
        );

        Vec3 velocity = dragon.getDeltaMovement();
        boolean ascending = velocity.y > 0.02;
        boolean riderAscending = dragon.isVehicle() && dragon.isGoingUp();

        if (ascending || riderAscending) return 1;

        if (altitude > 35.0) {
            return 0; // Glide
        } else {
            return 1; // Flap
        }
    }

    private boolean shouldPlayTakeoff() {
        int timeFlying = dragon.timeFlying;

        if (timeFlying < TAKEOFF_ANIM_EARLY_TICKS) return true;

        boolean airborne = !dragon.onGround();
        boolean ascending = dragon.getDeltaMovement().y > 0.05;

        return (timeFlying < TAKEOFF_ANIM_MAX_TICKS) && (airborne || ascending);
    }
}
