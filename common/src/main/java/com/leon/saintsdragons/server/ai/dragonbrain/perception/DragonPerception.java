package com.leon.saintsdragons.server.ai.dragonbrain.perception;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonTargetLifecycle;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import org.jetbrains.annotations.Nullable;

public final class DragonPerception {
    private DragonPerception() {
    }

    public static <T extends DragonEntity> @Nullable LivingEntity refreshTargetVisibility(Brain<T> brain,
                                                                                          T dragon,
                                                                                          long gameTime) {
        LivingEntity target = brain.getMemory(DragonMemories.ATTACK_TARGET).orElse(null);
        if (!DragonTargetLifecycle.isValidTarget(dragon, target)) {
            DragonTargetLifecycle.clearPerceptionMemories(brain);
            if (target != null) {
                DragonSensoryObservation investigation = brain
                        .getMemory(DragonMemories.INVESTIGATION_TARGET)
                        .orElse(null);
                if (investigation != null && target.getUUID().equals(investigation.sourceUuid())) {
                    brain.eraseMemory(DragonMemories.INVESTIGATION_TARGET);
                }
            }
            return null;
        }

        boolean visible = dragon.getSensing().hasLineOfSight(target);
        brain.setMemoryWithExpiry(DragonMemories.TARGET_VISIBLE, visible, 3L);
        if (visible) {
            brain.eraseMemory(DragonMemories.INVESTIGATION_TARGET);
            DragonPerceptionProfile profile = DragonPerceptionProfile.forDragon(dragon);
            brain.setMemoryWithExpiry(
                    DragonMemories.LAST_SEEN_TARGET,
                    new DragonSensoryObservation(
                            target.getBoundingBox().getCenter(),
                            target.getUUID(),
                            DragonSensoryObservation.Kind.SIGHT,
                            1.0F,
                            gameTime
                    ),
                    profile.targetMemoryTicks()
            );
        }
        return target;
    }
}
