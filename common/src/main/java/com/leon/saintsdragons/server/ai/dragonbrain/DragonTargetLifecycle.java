package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonTargetMemory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import org.jetbrains.annotations.Nullable;

public final class DragonTargetLifecycle {
    private DragonTargetLifecycle() {
    }

    public static void combatTargetChanged(DragonEntity dragon, @Nullable LivingEntity target) {
        Brain<?> brain = dragon.getBrain();
        LivingEntity previousTarget = brain.getMemory(DragonMemories.ATTACK_TARGET).orElse(null);
        clearTargetMemories(brain,
                target == null
                        && DragonTargetMemory.hasSearchable(brain, dragon.level().getGameTime())
                        && (previousTarget == null || dragon.isTargetValid(previousTarget)
                        && DragonTargetMemory.shouldPreserveCompatibilityTarget(
                        brain, previousTarget, dragon.level().getGameTime())));
        if (target == null) return;
        DragonTargetMemory.forget(brain);
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
        brain.eraseMemory(DragonMemories.RECENT_TARGET_SIGHT);
        brain.eraseMemory(DragonMemories.LAST_SEEN_WALK_TARGET);
        brain.eraseMemory(DragonMemories.LAST_SEEN_TARGET);
        brain.eraseMemory(DragonMemories.HEARD_TARGET);
    }

    public static void clearPerceptionMemories(DragonMemoryMap memories) {
        memories.erase(DragonMemories.TARGET_VISIBLE);
        memories.erase(DragonMemories.RECENT_TARGET_SIGHT);
        memories.erase(DragonMemories.LAST_SEEN_WALK_TARGET);
        memories.erase(DragonMemories.LAST_SEEN_TARGET);
        memories.erase(DragonMemories.HEARD_TARGET);
    }

    public static void clearTargetMemories(Brain<?> brain) {
        brain.eraseMemory(DragonMemories.ATTACK_TARGET);
        brain.eraseMemory(DragonMemories.TARGET_AIRBORNE);
        clearPerceptionMemories(brain);
        DragonTargetMemory.forget(brain);
    }

    public static void clearTargetMemories(DragonMemoryMap memories) {
        memories.erase(DragonMemories.ATTACK_TARGET);
        memories.erase(DragonMemories.TARGET_AIRBORNE);
        clearPerceptionMemories(memories);
        memories.erase(DragonMemories.TARGET_TRACK);
    }

    private static void clearTargetMemories(Brain<?> brain, boolean preserveTarget) {
        // ATTACK_TARGET remains as a compatibility handle only while the
        // retained track is actively pursuing a lost source.
        if (!preserveTarget) {
            brain.eraseMemory(DragonMemories.ATTACK_TARGET);
            DragonTargetMemory.forget(brain);
        }
        brain.eraseMemory(DragonMemories.TARGET_AIRBORNE);
        clearPerceptionMemories(brain);
    }

    private static void clearTargetMemories(DragonMemoryMap memories,
                                            boolean preserveTarget) {
        if (!preserveTarget) {
            memories.erase(DragonMemories.ATTACK_TARGET);
            memories.erase(DragonMemories.TARGET_TRACK);
        }
        memories.erase(DragonMemories.TARGET_AIRBORNE);
        clearPerceptionMemories(memories);
    }

    public static <T extends DragonEntity> void clearCombatTarget(Brain<T> brain,
                                                                   T dragon,
                                                                   boolean clearInvestigation) {
        LivingEntity target = brain.getMemory(DragonMemories.ATTACK_TARGET).orElse(null);
        long gameTime = dragon.level().getGameTime();
        clearTargetMemories(brain,
                !clearInvestigation && DragonTargetMemory.hasSearchable(brain, gameTime)
                        && (target == null || dragon.isTargetValid(target)
                        && DragonTargetMemory.shouldPreserveCompatibilityTarget(brain, target, gameTime)));
        if (clearInvestigation) {
            brain.eraseMemory(DragonMemories.INVESTIGATION_TARGET);
        }
        clearEntityCombatTarget(dragon);
    }

    public static <T extends DragonEntity> void clearCombatTarget(DragonMemoryMap memories,
                                                                   T dragon,
                                                                   boolean clearInvestigation) {
        LivingEntity target = memories.get(DragonMemories.ATTACK_TARGET).orElse(null);
        long gameTime = dragon.level().getGameTime();
        clearTargetMemories(memories,
                !clearInvestigation && DragonTargetMemory.hasSearchable(
                        memories.get(DragonMemories.TARGET_TRACK).orElse(null), gameTime)
                        && (target == null || dragon.isTargetValid(target)
                        && DragonTargetMemory.shouldPreserveCompatibilityTarget(memories, target, gameTime)));
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
