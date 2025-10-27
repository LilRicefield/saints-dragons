package com.leon.saintsdragons.server.entity.controller.cindervane;

import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Physics controller for Cindervane - handles flight mode computation and animation state sync
 * Mirrors the Raevyx physics controller architecture for consistency
 */
public class CindervanePhysicsController {
    private final Cindervane wyvern;

    // Takeoff animation timing - longer than Raevyx due to different animation length
    private static final int TAKEOFF_ANIM_MAX_TICKS = 30;   // Match animation length
    private static final int TAKEOFF_ANIM_EARLY_TICKS = 28; // Start checking conditions slightly earlier

    public CindervanePhysicsController(Cindervane wyvern) {
        this.wyvern = wyvern;
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
        if (!wyvern.isFlying()) return -1;
        if (shouldPlayTakeoff()) return 3;

        // Simple heuristic for Cindervane: check if hovering/landing
        if (wyvern.isHovering() || wyvern.isLanding()) return 2;

        // Check altitude-based flight mode (if Cindervane has this logic)
        // High altitude = glide (0), low altitude = flap (1)
        double altitude = wyvern.getY() - wyvern.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                                                     (int) wyvern.getX(), (int) wyvern.getZ());

        Vec3 velocity = wyvern.getDeltaMovement();
        boolean ascending = velocity.y > 0.02;

        // Always flap when ascending
        if (ascending) return 1;

        // Use altitude threshold for glide/flap decision
        if (altitude > 15.0) {
            return 0; // Glide at high altitude
        } else {
            return 1; // Flap at low altitude
        }
    }

    /**
     * Determines if takeoff animation should play
     * Mirrors Raevyx logic but with longer timing for Cindervane's longer animation
     */
    private boolean shouldPlayTakeoff() {
        // Get timeFlying from entity
        int timeFlying = wyvern.getTimeFlying();

        // Play takeoff at the very start of flight
        if (timeFlying < TAKEOFF_ANIM_EARLY_TICKS) return true;

        // Continue playing if still within max ticks AND conditions are met
        boolean airborne = !wyvern.onGround();
        boolean ascending = wyvern.getDeltaMovement().y > 0.05;

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
}
