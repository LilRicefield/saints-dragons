package com.leon.saintsdragons.server.ai.dragonbrain.learning;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import org.jetbrains.annotations.Nullable;

public interface DragonCombatLearner {
    DragonCombatLearning getCombatLearning();

    default boolean canLearnCombat() { return true; }

    static @Nullable DragonCombatLearning get(DragonEntity dragon) {
        return dragon instanceof DragonCombatLearner learner ? learner.getCombatLearning() : null;
    }
}
