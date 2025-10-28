package com.leon.saintsdragons.server.ai.goals.cindervane;

import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Custom ground wandering for Amphithere when not flying
 * Provides better control over movement behavior and animation triggers
 */
public class CindervaneGroundWanderGoal extends RandomStrollGoal {

    private final Cindervane amphithere;

    public CindervaneGroundWanderGoal(Cindervane amphithere, double speed, int interval) {
        // Fixed interval wandering
        super(amphithere, speed, interval);
        this.amphithere = amphithere;
    }

    @Override
    public boolean canUse() {
        // Only wander on ground when not flying
        if (amphithere.isFlying()) {
            return false;
        }

        // Don't interfere with important behaviors
        if (amphithere.isOrderedToSit() || amphithere.isVehicle() || amphithere.isPassenger()) {
            return false;
        }

        // Don't wander while sitting down (but standing up is OK)
        if (amphithere.isSittingDownAnimation()) {
            return false;
        }

        // Don't wander during transitional states
        if (amphithere.isTakeoff() || amphithere.isLanding() || amphithere.isHovering()) {
            return false;
        }

        // Don't wander during combat
        var target = amphithere.getTarget();
        if (target != null && target.isAlive()) {
            return false;
        }

        // Hook up to command system - only wander when command is 2 (Wander) or when untamed
        if (amphithere.isTame()) {
            int command = amphithere.getCommand();
            if (command != 2) { // 0=Follow, 1=Sit, 2=Wander
                return false;
            }
        }

        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if we start flying
        if (amphithere.isFlying()) {
            return false;
        }

        // Stop if we enter transitional states
        if (amphithere.isTakeoff() || amphithere.isLanding() || amphithere.isHovering()) {
            return false;
        }

        // Stop if combat starts
        var target = amphithere.getTarget();
        if (target != null && target.isAlive()) {
            return false;
        }

        // Stop if ordered to sit
        if (amphithere.isOrderedToSit()) {
            return false;
        }

        return super.canContinueToUse();
    }


    @Nullable
    @Override
    protected Vec3 getPosition() {
        // If tamed and owner is far, bias movement towards owner
        if (amphithere.isTame()) {
            var owner = amphithere.getOwner();
            if (owner != null && amphithere.distanceToSqr(owner) > 20.0 * 20.0) {
                // Move generally towards owner but not directly (maintain some independence)
                return DefaultRandomPos.getPosTowards(
                        this.mob,
                        16, // range
                        7,  // vertical range
                        owner.position(),
                        (float) Math.PI / 3F // 60-degree cone towards owner
                );
            } else if (owner != null) {
                Vec3 aroundOwner = DefaultRandomPos.getPosTowards(
                        this.mob,
                        12,
                        6,
                        owner.position(),
                        (float) Math.PI
                );
                if (aroundOwner != null) {
                    return aroundOwner;
                }
            }
        }

        // Default random wandering
        return DefaultRandomPos.getPos(this.mob, 20, 8); // Slightly larger range for a dragon
    }
}
