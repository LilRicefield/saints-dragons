package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import org.jetbrains.annotations.Nullable;

public final class DragonTargetLifecycle {
    private DragonTargetLifecycle() {
    }

    public static void combatTargetChanged(DragonEntity dragon, @Nullable LivingEntity target) {
        Brain<?> brain = dragon.getBrain();
        clearTargetMemories(brain);
        if (target == null) return;
        brain.eraseMemory(DragonMemories.INVESTIGATION_TARGET);
        brain.eraseMemory(DragonMemories.WALK_TARGET);
        brain.eraseMemory(DragonMemories.PATH);
        brain.eraseMemory(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
        brain.eraseMemory(DragonMemories.GROUND_ROUTE_ABANDONED);
        brain.eraseMemory(DragonMemories.TACTICAL_LANDING_POSITION);
        brain.eraseMemory(DragonMemories.TACTICAL_COMMITMENT);
        if (dragon instanceof RideableDragonBase rideable) {
            rideable.getAIMovement().stopAndClearAllMovement();
        }
        new DragonMemoryMap(dragon).erase(DragonMemories.MOVEMENT_INTENT);
        brain.setMemory(DragonMemories.ATTACK_TARGET, target);
    }

    public static boolean isValidTarget(DragonEntity dragon, @Nullable LivingEntity target) {
        return target != null && dragon.isTargetValid(target) && target.level() == dragon.level();
    }

    public static void clearPerceptionMemories(Brain<?> brain) {
        brain.eraseMemory(DragonMemories.TARGET_VISIBLE);
        brain.eraseMemory(DragonMemories.LAST_SEEN_TARGET);
        brain.eraseMemory(DragonMemories.HEARD_TARGET);
    }

    public static void clearPerceptionMemories(DragonMemoryMap memories) {
        memories.erase(DragonMemories.TARGET_VISIBLE);
        memories.erase(DragonMemories.LAST_SEEN_TARGET);
        memories.erase(DragonMemories.HEARD_TARGET);
    }

    public static void clearTargetMemories(Brain<?> brain) {
        brain.eraseMemory(DragonMemories.ATTACK_TARGET);
        brain.eraseMemory(DragonMemories.TARGET_AIRBORNE);
        clearPerceptionMemories(brain);
    }

    public static void clearTargetMemories(DragonMemoryMap memories) {
        memories.erase(DragonMemories.ATTACK_TARGET);
        memories.erase(DragonMemories.TARGET_AIRBORNE);
        clearPerceptionMemories(memories);
    }

    public static <T extends DragonEntity> void clearCombatTarget(Brain<T> brain,
                                                                   T dragon,
                                                                   boolean clearInvestigation) {
        clearTargetMemories(brain);
        if (clearInvestigation) {
            brain.eraseMemory(DragonMemories.INVESTIGATION_TARGET);
        }
        clearEntityCombatTarget(dragon);
    }

    public static <T extends DragonEntity> void clearCombatTarget(DragonMemoryMap memories,
                                                                   T dragon,
                                                                   boolean clearInvestigation) {
        clearTargetMemories(memories);
        if (clearInvestigation) {
            memories.erase(DragonMemories.INVESTIGATION_TARGET);
        }
        clearEntityCombatTarget(dragon);
    }

    private static void clearEntityCombatTarget(DragonEntity dragon) {
        if (dragon.getTarget() != null) {
            dragon.setTarget(null);
        }
        dragon.setAggressive(false);
    }
}
