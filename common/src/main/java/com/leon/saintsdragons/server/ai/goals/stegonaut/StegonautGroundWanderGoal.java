package com.leon.saintsdragons.server.ai.goals.stegonaut;

import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Simple ground wandering for Primitive Drake
 */
public class StegonautGroundWanderGoal extends RandomStrollGoal {

    private final Stegonaut drake;

    public StegonautGroundWanderGoal(Stegonaut drake, double speed, int interval) {
        super(drake, speed, interval);
        this.drake = drake;
    }

    @Override
    public boolean canUse() {
        // Don't interfere with important behaviors
        if (drake.isOrderedToSit() || drake.isVehicle() || drake.isPassenger()) {
            return false;
        }

        if (drake.isInWaterOrBubble()) {
            return false;
        }

        // Don't wander while playing dead
        if (false) {
            return false;
        }

        // Don't wander during valid combat; clear stale/unattackable targets so AI can recover.
        if (drake.getTarget() != null && drake.getTarget().isAlive()) {
            if (!drake.isTargetValid(drake.getTarget()) || !drake.canTarget(drake.getTarget())) {
                drake.setTarget(null);
            } else {
                return false;
            }
        }

        if (drake.isInLove()) {
            return false;
        }

        // Hook up to command system - only wander when command is 2 (Wander) or when untamed
        if (drake.isTame()) {
            int command = drake.getCommand();
            if (command != 2) { // 0=Follow, 1=Sit, 2=Wander
                return false;
            }
        }
        // Untamed drakes can always wander (they don't follow commands)

        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Stop only for valid combat; clear stale targets.
        if (drake.getTarget() != null && drake.getTarget().isAlive()) {
            if (!drake.isTargetValid(drake.getTarget()) || !drake.canTarget(drake.getTarget())) {
                drake.setTarget(null);
            } else {
                return false;
            }
        }

        if (drake.isInLove()) {
            return false;
        }

        if (drake.isInWaterOrBubble()) {
            return false;
        }

        // Stop if ordered to sit
        if (drake.isOrderedToSit()) {
            return false;
        }

        // Stop if playing dead
        if (false) {
            return false;
        }

        return super.canContinueToUse();
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        // Fully independent wandering (no owner tether in Wander mode).
        return DefaultRandomPos.getPos(this.mob, 20, 8);
    }
}
