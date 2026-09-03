/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Attack-target memory wiring is adapted from SmartBrainLib's
 * SetWalkTargetToAttackTarget behaviour.
 */
package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourUtils;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonOneShotBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonTargetLifecycle;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * Converts the current attack target into vanilla Brain movement memories.
 */
public class SetWalkTargetToAttackTargetBehaviour<T extends RideableDragonBase> extends DragonOneShotBehaviour<T> {
    private final BiFunction<T, LivingEntity, Float> speedModifier;
    private final BiFunction<T, LivingEntity, Double> closeEnoughDistance;
    private final BiPredicate<T, LivingEntity> movementLocked;

    public SetWalkTargetToAttackTargetBehaviour(float speedModifier,
                                                BiFunction<T, LivingEntity, Double> closeEnoughDistance,
                                                BiPredicate<T, LivingEntity> movementLocked) {
        super(Map.of(
                DragonMemories.WALK_TARGET, MemoryStatus.REGISTERED,
                DragonMemories.LOOK_TARGET, MemoryStatus.REGISTERED,
                DragonMemories.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT
        ), false);
        this.speedModifier = (dragon, target) -> speedModifier;
        this.closeEnoughDistance = closeEnoughDistance;
        this.movementLocked = movementLocked;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        return target != null
                && DragonTargetLifecycle.isValidTarget(dragon, target)
                && !dragon.isAerial()
                && !dragon.isInWaterOrBubble()
                && dragon.getLocomotionMode() == DragonLocomotionMode.GROUND
                && !context.memories().get(DragonMemories.TARGET_AIRBORNE).orElse(false)
                && !context.memories().get(DragonMemories.GROUND_ROUTE_ABANDONED).orElse(false);
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null) {
            return;
        }

        double targetCloseEnough = Math.max(0.0D, closeEnoughDistance.apply(dragon, target));
        Entity movementAnchor = DragonTargetingHelper.movementAnchor(target);
        double movementCloseEnough = DragonTargetingHelper.movementStopDistance(target, targetCloseEnough);
        boolean requiresWaterEntry = dragon.canSwim()
                && movementAnchor.isInWaterOrBubble()
                && !dragon.isInWaterOrBubble();
        if (movementLocked.test(dragon, target)
                || !requiresWaterEntry
                && dragon.getSensing().hasLineOfSight(target)
                && dragon.distanceToSqr(movementAnchor) <= movementCloseEnough * movementCloseEnough) {
            context.memories().erase(DragonMemories.WALK_TARGET);
            return;
        }

        DragonBehaviourUtils.setWalkAndLookTarget(
                context,
                target,
                movementAnchor,
                speedModifier.apply(dragon, target),
                (int)Math.floor(movementCloseEnough)
        );
    }
}
