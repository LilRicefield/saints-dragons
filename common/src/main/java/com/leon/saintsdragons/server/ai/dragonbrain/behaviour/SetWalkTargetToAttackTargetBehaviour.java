/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Attack-target memory wiring is adapted from SmartBrainLib's
 * SetWalkTargetToAttackTarget behaviour.
 */
package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * Converts the current attack target into vanilla Brain movement memories.
 */
public class SetWalkTargetToAttackTargetBehaviour<T extends RideableDragonBase> extends DragonBehaviour<T> {
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
                && dragon.isTargetValid(target)
                && !dragon.isAerial()
                && !dragon.isInWaterOrBubble()
                && dragon.getLocomotionMode() == DragonLocomotionMode.GROUND
                && !context.memories().get(DragonMemories.TARGET_AIRBORNE).orElse(false)
                && !context.memories().get(DragonMemories.GROUND_ROUTE_ABANDONED).orElse(false);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return false;
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null) {
            return;
        }

        double closeEnough = Math.max(0.0D, closeEnoughDistance.apply(dragon, target));
        if (movementLocked.test(dragon, target)
                || dragon.getSensing().hasLineOfSight(target)
                && dragon.distanceToSqr(target) <= closeEnough * closeEnough) {
            context.memories().erase(DragonMemories.WALK_TARGET);
            return;
        }

        context.memories().set(DragonMemories.LOOK_TARGET, new EntityTracker(target, true));
        context.memories().set(DragonMemories.WALK_TARGET, new WalkTarget(
                new EntityTracker(target, false),
                speedModifier.apply(dragon, target),
                (int)Math.floor(closeEnough)
        ));
    }
}
