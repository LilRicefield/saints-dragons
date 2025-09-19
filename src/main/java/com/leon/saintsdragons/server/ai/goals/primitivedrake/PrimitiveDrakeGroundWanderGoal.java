package com.leon.saintsdragons.server.ai.goals.primitivedrake;

import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Cute ground wandering for Primitive Drake with "run away from players" behavior!
 * Simple and adorable - runs away when players get close, wanders peacefully when alone.
 */
public class PrimitiveDrakeGroundWanderGoal extends RandomStrollGoal {

    private final PrimitiveDrakeEntity drake;
    private static final double RUN_AWAY_DISTANCE = 8.0; // Run away when player is within 8 blocks
    private static final double SAFE_DISTANCE = 16.0; // Consider safe when player is 16+ blocks away

    public PrimitiveDrakeGroundWanderGoal(PrimitiveDrakeEntity drake, double speed, int interval) {
        super(drake, speed, interval);
        this.drake = drake;
    }

    @Override
    public boolean canUse() {
        // Don't interfere with important behaviors
        if (drake.isOrderedToSit() || drake.isVehicle() || drake.isPassenger()) {
            return false;
        }

        // Don't wander during combat
        if (drake.getTarget() != null && drake.getTarget().isAlive()) {
            return false;
        }

        // If tamed, respect command system
        if (drake.isTame()) {
            int command = drake.getCommand();
            if (command != 2) { // 0=Follow, 1=Sit, 2=Wander
                return false;
            }
        }

        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if combat starts
        if (drake.getTarget() != null && drake.getTarget().isAlive()) {
            return false;
        }

        // Stop if ordered to sit
        if (drake.isOrderedToSit()) {
            return false;
        }

        return super.canContinueToUse();
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        // Check for nearby players
        Player nearestPlayer = drake.level().getNearestPlayer(drake, RUN_AWAY_DISTANCE);
        
        if (nearestPlayer != null) {
            // RUN AWAY! 🏃‍♂️💨
            Vec3 runAwayPos = DefaultRandomPos.getPosAway(
                this.mob,
                12, // range to run
                8,  // vertical range
                nearestPlayer.position()
            );
            
            if (runAwayPos != null) {
                return runAwayPos;
            }
        }

        // If tamed and owner is far, bias movement towards owner
        if (drake.isTame()) {
            var owner = drake.getOwner();
            if (owner != null && drake.distanceToSqr(owner) > SAFE_DISTANCE * SAFE_DISTANCE) {
                // Move generally towards owner but not directly (maintain some independence)
                return DefaultRandomPos.getPosTowards(
                        this.mob,
                        16, // range
                        7,  // vertical range
                        owner.position(),
                        (float) Math.PI / 3F // 60-degree cone towards owner
                );
            }
        }

        // Default peaceful wandering when no players nearby
        return DefaultRandomPos.getPos(this.mob, 16, 6); // Smaller range for a cute little drake
    }
}
