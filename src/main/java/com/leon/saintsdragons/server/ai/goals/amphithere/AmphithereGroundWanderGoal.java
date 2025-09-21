package com.leon.saintsdragons.server.ai.goals.amphithere;

import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Custom ground wandering for Amphithere when not flying
 * Provides better control over movement behavior and animation triggers
 */
public class AmphithereGroundWanderGoal extends RandomStrollGoal {

    private final AmphithereEntity dragon;

    public AmphithereGroundWanderGoal(AmphithereEntity dragon, double speed, int interval) {
        // Fixed interval wandering
        super(dragon, speed, interval);
        this.dragon = dragon;
    }

    @Override
    public boolean canUse() {
        // Only wander on ground when not flying
        if (dragon.isFlying()) {
            return false;
        }

        // Don't interfere with important behaviors
        if (dragon.isOrderedToSit() || dragon.isVehicle() || dragon.isPassenger()) {
            return false;
        }

        // Don't wander during transitional states
        if (dragon.isTakeoff() || dragon.isLanding() || dragon.isHovering()) {
            return false;
        }

        // Don't wander during combat
        var target = dragon.getTarget();
        if (target != null && target.isAlive()) {
            return false;
        }

        // Hook up to command system - only wander when command is 2 (Wander) or when untamed
        if (dragon.isTame()) {
            int command = dragon.getCommand();
            if (command != 2) { // 0=Follow, 1=Sit, 2=Wander
                return false;
            }
        }

        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if we start flying
        if (dragon.isFlying()) {
            return false;
        }

        // Stop if we enter transitional states
        if (dragon.isTakeoff() || dragon.isLanding() || dragon.isHovering()) {
            return false;
        }

        // Stop if combat starts
        var target = dragon.getTarget();
        if (target != null && target.isAlive()) {
            return false;
        }

        // Stop if ordered to sit
        if (dragon.isOrderedToSit()) {
            return false;
        }

        return super.canContinueToUse();
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        // If tamed and owner is far, bias movement towards owner
        if (dragon.isTame()) {
            var owner = dragon.getOwner();
            if (owner != null && dragon.distanceToSqr(owner) > 20.0 * 20.0) {
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

        // Default random wandering
        return DefaultRandomPos.getPos(this.mob, 20, 8); // Slightly larger range for a dragon
    }
}
