package com.leon.saintsdragons.server.ai.dragonbrain.perception;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemoryMap;
import com.leon.saintsdragons.server.entity.base.DragonEntity;

public final class DragonScentEligibility {
    private DragonScentEligibility() {
    }

    public static boolean isAvailable(DragonEntity dragon, DragonMemoryMap memories) {
        return dragon.isAlive()
                && !dragon.isDying()
                && !dragon.isBaby()
                && dragon.getTarget() == null
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isInSittingPose()
                && !dragon.isInSitTransition()
                && !dragon.isSleepLocked()
                && !dragon.isSleeping()
                && !dragon.isSleepTransitioning()
                && !dragon.isInLove()
                && !dragon.isDancing()
                && !dragon.isHuntFoodPursuitActive()
                && (!dragon.isTame() || dragon.getCommand() != 0)
                && dragon.getActiveAbility() == null
                && !memories.has(DragonMemories.ATTACK_TARGET)
                && !memories.has(DragonMemories.BREED_TARGET)
                && !memories.has(DragonMemories.INVESTIGATION_TARGET)
                && !memories.has(DragonMemories.RESCUE_TARGET)
                && !memories.has(DragonMemories.INTERCEPT_PROJECTILE);
    }
}
