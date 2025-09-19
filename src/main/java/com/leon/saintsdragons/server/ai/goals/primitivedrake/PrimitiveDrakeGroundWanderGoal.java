package com.leon.saintsdragons.server.ai.goals.primitivedrake;

import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Simple ground wandering for Primitive Drake
 */
public class PrimitiveDrakeGroundWanderGoal extends RandomStrollGoal {

    private final PrimitiveDrakeEntity drake;

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

        // Hook up to command system - only wander when command is 2 (Wander) or when untamed
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
        // If tamed and owner is far, bias movement towards owner
        if (drake.isTame()) {
            var owner = drake.getOwner();
            if (owner != null && drake.distanceToSqr(owner) > 20.0 * 20.0) {
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
        return DefaultRandomPos.getPos(this.mob, 20, 8);
    }
}