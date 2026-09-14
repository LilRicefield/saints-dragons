package com.leon.saintsdragons.server.ai.dragonbrain.perception;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonTargetLifecycle;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.schedule.Activity;
import org.jetbrains.annotations.Nullable;

public final class DragonPerception {
    private static final int SIGHT_INTERRUPTION_TICKS = 8;

    private DragonPerception() {
    }

    public static @Nullable LivingEntity refreshTargetVisibility(Brain<?> brain,
                                                                 DragonEntity dragon,
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
            brain.setMemoryWithExpiry(DragonMemories.RECENT_TARGET_SIGHT, true, SIGHT_INTERRUPTION_TICKS);
            brain.eraseMemory(DragonMemories.INVESTIGATION_TARGET);
            DragonPerceptionProfile profile = DragonPerceptionProfile.forDragon(dragon);
            WalkTarget walking = brain.getMemory(DragonMemories.WALK_TARGET)
                    .orElse(brain.getMemory(DragonMemories.LAST_SEEN_WALK_TARGET).orElse(null));
            float chaseSpeed = walking == null ? 1.25F : walking.getSpeedModifier();
            // A fixed position and speed, never an EntityTracker that follows a hidden target.
            brain.setMemoryWithExpiry(DragonMemories.LAST_SEEN_WALK_TARGET,
                    new WalkTarget(DragonTargetingHelper.movementAnchor(target).position(), chaseSpeed, 1),
                    profile.targetMemoryTicks());
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
        } else if (isSightInterruption(brain) && brain.getActiveActivities().contains(Activity.FIGHT)
                && !dragon.isAerial() && !dragon.isInWaterOrBubble()
                && !dragon.isVehicle() && !dragon.isPassenger() && !dragon.isOrderedToSit()
                && !dragon.isSleepLocked()
                && (brain.hasMemoryValue(DragonMemories.WALK_TARGET) || dragon.getActiveAbility() == null)) {
            brain.getMemory(DragonMemories.LAST_SEEN_WALK_TARGET)
                    .ifPresent(walking -> brain.setMemory(DragonMemories.WALK_TARGET, walking));
        }
        return target;
    }

    public static boolean isSightInterruption(Brain<?> brain) {
        return brain.hasMemoryValue(DragonMemories.ATTACK_TARGET)
                && !brain.getMemory(DragonMemories.TARGET_VISIBLE).orElse(true)
                && brain.getMemory(DragonMemories.RECENT_TARGET_SIGHT).orElse(false)
                && !brain.hasMemoryValue(DragonMemories.RESCUE_TARGET)
                && !brain.hasMemoryValue(DragonMemories.INTERCEPT_PROJECTILE);
    }
}
