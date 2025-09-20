package com.leon.saintsdragons.server.ai.goals.primitivedrake;

import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;

/**
 * Custom LookAtPlayerGoal for Primitive Drake that respects play dead state
 */
public class PrimitiveDrakeLookAtPlayerGoal extends LookAtPlayerGoal {
    private final PrimitiveDrakeEntity drake;

    public PrimitiveDrakeLookAtPlayerGoal(PrimitiveDrakeEntity drake, Class<? extends Player> playerClass, float maxDistance) {
        super(drake, playerClass, maxDistance);
        this.drake = drake;
    }

    @Override
    public boolean canUse() {
        // Don't look at players while playing dead
        if (drake.isPlayingDead()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Stop looking if start playing dead
        if (drake.isPlayingDead()) {
            return false;
        }
        return super.canContinueToUse();
    }
}
