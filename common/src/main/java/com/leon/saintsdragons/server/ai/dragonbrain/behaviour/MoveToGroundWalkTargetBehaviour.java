/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Ground walk-target execution is adapted from SmartBrainLib's
 * MoveToWalkTarget behaviour.
 */
package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Executes vanilla WALK_TARGET memories through the dragon's existing ground navigator.
 * This mirrors SmartBrainLib's MoveToWalkTarget contract without taking a dependency on it.
 */
public class MoveToGroundWalkTargetBehaviour<T extends RideableDragonBase> extends DragonBehaviour<T> {
    @Nullable
    private Path path;
    @Nullable
    private BlockPos lastTargetPos;
    private float speedModifier;

    public MoveToGroundWalkTargetBehaviour() {
        super(Map.of(
                DragonMemories.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED,
                DragonMemories.PATH, MemoryStatus.VALUE_ABSENT,
                DragonMemories.WALK_TARGET, MemoryStatus.VALUE_PRESENT
        ));
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        WalkTarget walkTarget = context.memories().get(DragonMemories.WALK_TARGET).orElse(null);
        if (walkTarget == null || !isGroundMovementContext(context)) {
            context.memories().erase(DragonMemories.WALK_TARGET);
            context.memories().erase(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
            return false;
        }

        ensureGroundNavigation(context.dragon());
        if (!hasReachedTarget(context.dragon(), walkTarget)
                && requestPath(context, walkTarget)) {
            lastTargetPos = walkTarget.getTarget().currentBlockPosition();
            return true;
        }

        context.memories().erase(DragonMemories.WALK_TARGET);
        context.memories().erase(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
        return false;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        if (lastTargetPos == null || !isGroundMovementContext(context)) {
            return false;
        }
        WalkTarget walkTarget = context.memories().get(DragonMemories.WALK_TARGET).orElse(null);
        if (context.dragon().getAIMovement().hasFailed()) {
            markCantReach(context);
            return false;
        }
        return context.dragon().getAIMovement().isPathing()
                && walkTarget != null
                && !hasReachedTarget(context.dragon(), walkTarget);
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        syncPathMemory(context);
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        syncPathMemory(context);

        WalkTarget walkTarget = context.memories().get(DragonMemories.WALK_TARGET).orElse(null);
        if (lastTargetPos != null && walkTarget != null
                && walkTarget.getTarget().currentBlockPosition().distSqr(lastTargetPos) > 4.0D
                && !hasReachedTarget(dragon, walkTarget)
                && requestPath(context, walkTarget)) {
            lastTargetPos = walkTarget.getTarget().currentBlockPosition();
        }

        if (dragon.getAIMovement().hasFailed()) {
            markCantReach(context);
        } else if (path != null && path.canReach()) {
            context.memories().erase(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
        }
        dragon.getAIMovement().setGroundMoveState(true);
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        context.dragon().getAIMovement().stop();
        context.memories().erase(DragonMemories.WALK_TARGET);
        context.memories().erase(DragonMemories.PATH);
        path = null;
        lastTargetPos = null;
    }

    @Override
    protected int cooldownForTicks(DragonBrainContext<T> context) {
        return context.dragon().getAIMovement().hasFailed()
                ? context.dragon().getRandom().nextInt(40)
                : 0;
    }

    private boolean requestPath(DragonBrainContext<T> context, WalkTarget walkTarget) {
        T dragon = context.dragon();
        BlockPos targetPos = walkTarget.getTarget().currentBlockPosition();
        speedModifier = walkTarget.getSpeedModifier();
        context.memories().erase(DragonMemories.PATH);
        path = null;
        return dragon.getAIMovement().moveToGroundPosition(
                Vec3.atBottomCenterOf(targetPos),
                speedModifier,
                true
        );
    }

    private void syncPathMemory(DragonBrainContext<T> context) {
        Path navigationPath = context.dragon().getNavigation().getPath();
        if (path == navigationPath) {
            return;
        }
        path = navigationPath;
        if (path != null) {
            context.memories().set(DragonMemories.PATH, path);
        } else {
            context.memories().erase(DragonMemories.PATH);
        }
    }

    private void markCantReach(DragonBrainContext<T> context) {
        if (context.memories().get(DragonMemories.CANT_REACH_WALK_TARGET_SINCE).isEmpty()) {
            context.memories().set(DragonMemories.CANT_REACH_WALK_TARGET_SINCE, context.gameTime());
        }
    }

    private boolean hasReachedTarget(T dragon, WalkTarget walkTarget) {
        return walkTarget.getTarget().currentBlockPosition().distManhattan(dragon.blockPosition())
                <= walkTarget.getCloseEnoughDist();
    }

    private boolean isGroundMovementContext(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        return !dragon.isInWaterOrBubble()
                && dragon.getLocomotionMode() == DragonLocomotionMode.GROUND
                && !(dragon instanceof RideableFlyingDragon flyingDragon && flyingDragon.isAerial())
                && !context.memories().get(DragonMemories.GROUND_ROUTE_ABANDONED).orElse(false);
    }

    private void ensureGroundNavigation(T dragon) {
        if (dragon instanceof RideableFlyingDragon flyingDragon) {
            flyingDragon.switchToGroundNavigation();
        }
    }
}
