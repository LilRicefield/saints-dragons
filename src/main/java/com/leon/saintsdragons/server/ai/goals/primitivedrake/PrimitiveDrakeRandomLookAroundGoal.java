package com.leon.saintsdragons.server.ai.goals.primitivedrake;

import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;

/**
 * Custom RandomLookAroundGoal for Primitive Drake that respects play dead state
 */
public class PrimitiveDrakeRandomLookAroundGoal extends RandomLookAroundGoal {
    private final PrimitiveDrakeEntity drake;

    public PrimitiveDrakeRandomLookAroundGoal(PrimitiveDrakeEntity drake) {
        super(drake);
        this.drake = drake;
    }

    @Override
    public boolean canUse() {
        // Don't look around while playing dead
        if (drake.isPlayingDead()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Stop looking around if start playing dead
        if (drake.isPlayingDead()) {
            return false;
        }
        return super.canContinueToUse();
    }
}
