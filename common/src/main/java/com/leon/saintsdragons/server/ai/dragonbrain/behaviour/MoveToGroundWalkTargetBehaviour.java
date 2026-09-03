/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Ground walk-target execution is adapted from SmartBrainLib's
 * MoveToWalkTarget behaviour.
 */
package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import com.leon.saintsdragons.server.ai.navigation.PathNavigateGround;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MoveToGroundWalkTargetBehaviour<T extends RideableDragonBase> extends DragonBehaviour<T> {
    private static final int WATER_ENTRY_TRANSITION_TIMEOUT = 30;
    private static final int FAILED_ENTRY_MEMORY_TICKS = 100;

    @Nullable
    private Path path;
    @Nullable
    private BlockPos lastTargetPos;
    private float speedModifier;
    private boolean waterHandoffActive;
    private float originalWaterMalus;
    private float originalWaterBorderMalus;
    @Nullable
    private Vec3 resolvedPathTarget;
    private boolean targetingWaterEntry;
    @Nullable
    private DragonWaterEntryTargeting.Target waterEntryTarget;
    private final Set<BlockPos> rejectedWaterEntries = new HashSet<>();
    private boolean enteringWater;
    private boolean waterEntryTransitionFailed;
    private int waterEntryTransitionTicks;
    private long rejectedWaterEntriesExpireAt;
    private long nextPathAttemptAt;

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
            releaseWaterHandoff(context.dragon());
            context.memories().erase(DragonMemories.WALK_TARGET);
            context.memories().erase(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
            clearWaterEntryState(true);
            return false;
        }

        ensureGroundNavigation(context.dragon());
        configureWaterHandoff(context);
        boolean requiresWaterEntry = requiresWaterEntry(context);
        if ((requiresWaterEntry || !hasReachedTarget(context.dragon(), walkTarget))
                && requestPath(context, walkTarget)) {
            lastTargetPos = walkTarget.getTarget().currentBlockPosition();
            return true;
        }

        if (!requiresWaterEntry && hasReachedTarget(context.dragon(), walkTarget)) {
            releaseWaterHandoff(context.dragon());
            context.memories().erase(DragonMemories.WALK_TARGET);
            context.memories().erase(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
            clearWaterEntryState(true);
        }
        return false;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        if (enteringWater) {
            if (context.dragon().isInWaterOrBubble()) {
                context.memories().erase(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
                return false;
            }
            if (!hasSubmergedAttackTarget(context)
                    || waterEntryTransitionTicks >= WATER_ENTRY_TRANSITION_TIMEOUT) {
                waterEntryTransitionFailed = true;
                markCantReach(context);
                return false;
            }
            return true;
        }
        if (lastTargetPos == null || !isGroundMovementContext(context)) {
            return false;
        }
        if (targetingWaterEntry && !hasSubmergedAttackTarget(context)) {
            return false;
        }
        WalkTarget walkTarget = context.memories().get(DragonMemories.WALK_TARGET).orElse(null);
        if (context.dragon().getAIMovement().hasFailed()) {
            markCantReach(context);
            return false;
        }
        if (waterEntryTarget != null
                && DragonWaterEntryTargeting.isCloseEnoughToEnter(context.dragon(), waterEntryTarget)) {
            beginWaterEntry(context);
            return true;
        }
        return context.dragon().getAIMovement().isPathing()
                && walkTarget != null
                && (targetingWaterEntry || !hasReachedTarget(context.dragon(), walkTarget));
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        syncPathMemory(context);
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        syncPathMemory(context);

        if (enteringWater) {
            waterEntryTransitionTicks++;
            if (waterEntryTarget != null) {
                DragonWaterEntryTargeting.moveIntoWater(dragon, waterEntryTarget);
            }
            if (dragon.isInWaterOrBubble()) {
                context.memories().erase(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
            }
            return;
        }

        if (waterEntryTarget != null
                && DragonWaterEntryTargeting.isCloseEnoughToEnter(dragon, waterEntryTarget)) {
            beginWaterEntry(context);
            DragonWaterEntryTargeting.moveIntoWater(dragon, waterEntryTarget);
            return;
        }

        WalkTarget walkTarget = context.memories().get(DragonMemories.WALK_TARGET).orElse(null);
        if (lastTargetPos != null && walkTarget != null
                && !targetingWaterEntry
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
        boolean failed = context.dragon().getAIMovement().hasFailed() || waterEntryTransitionFailed;
        if (failed && waterEntryTarget != null) {
            rejectedWaterEntries.add(BlockPos.containing(waterEntryTarget.landPosition()));
            rejectedWaterEntriesExpireAt = context.gameTime() + FAILED_ENTRY_MEMORY_TICKS;
        }
        context.dragon().getAIMovement().stop();
        releaseWaterHandoff(context.dragon());
        context.memories().erase(DragonMemories.WALK_TARGET);
        context.memories().erase(DragonMemories.PATH);
        path = null;
        lastTargetPos = null;
        resolvedPathTarget = null;
        targetingWaterEntry = false;
        waterEntryTarget = null;
        enteringWater = false;
        waterEntryTransitionFailed = false;
        waterEntryTransitionTicks = 0;
    }

    @Override
    protected int cooldownForTicks(DragonBrainContext<T> context) {
        return context.dragon().getAIMovement().hasFailed() || waterEntryTransitionFailed
                ? 20 + context.dragon().getRandom().nextInt(21)
                : 0;
    }

    private boolean requestPath(DragonBrainContext<T> context, WalkTarget walkTarget) {
        if (context.gameTime() < nextPathAttemptAt) {
            return false;
        }
        T dragon = context.dragon();
        Vec3 requestedTarget = walkTarget.getTarget().currentPosition();
        resolvedPathTarget = resolveGroundPathTarget(context, requestedTarget);
        if (resolvedPathTarget == null) {
            markCantReach(context);
            nextPathAttemptAt = context.gameTime() + 20;
            return false;
        }
        speedModifier = walkTarget.getSpeedModifier();
        context.memories().erase(DragonMemories.PATH);
        path = null;
        boolean requested = dragon.getAIMovement().moveToGroundPosition(
                resolvedPathTarget,
                speedModifier,
                true
        );
        if (!requested) {
            nextPathAttemptAt = context.gameTime() + 20;
        }
        return requested;
    }

    @Nullable
    private Vec3 resolveGroundPathTarget(DragonBrainContext<T> context, Vec3 requested) {
        T dragon = context.dragon();
        LivingEntity attackTarget = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (!(dragon instanceof SemiAquaticDragon)
                || !dragon.canSwim()
                || dragon.isInWaterOrBubble()
                || attackTarget == null
                || !DragonTargetingHelper.isMovementAnchorInWater(attackTarget)) {
            targetingWaterEntry = false;
            waterEntryTarget = null;
            return requested;
        }

        if (context.gameTime() >= rejectedWaterEntriesExpireAt) {
            rejectedWaterEntries.clear();
        }
        targetingWaterEntry = true;
        waterEntryTarget = DragonWaterEntryTargeting.find(
                context,
                DragonTargetingHelper.movementAnchor(attackTarget).position(),
                rejectedWaterEntries
        );
        return waterEntryTarget == null ? null : waterEntryTarget.landPosition();
    }

    private boolean hasSubmergedAttackTarget(DragonBrainContext<T> context) {
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        return target != null
                && target.isAlive()
                && DragonTargetingHelper.isMovementAnchorInWater(target);
    }

    private boolean requiresWaterEntry(DragonBrainContext<T> context) {
        return context.dragon() instanceof SemiAquaticDragon
                && context.dragon().canSwim()
                && !context.dragon().isInWaterOrBubble()
                && hasSubmergedAttackTarget(context);
    }

    private void beginWaterEntry(DragonBrainContext<T> context) {
        if (enteringWater) {
            return;
        }
        enteringWater = true;
        waterEntryTransitionTicks = 0;
        context.dragon().getAIMovement().stop();
        context.memories().erase(DragonMemories.PATH);
        path = null;
    }

    private void clearWaterEntryState(boolean clearRejected) {
        targetingWaterEntry = false;
        waterEntryTarget = null;
        enteringWater = false;
        waterEntryTransitionFailed = false;
        waterEntryTransitionTicks = 0;
        resolvedPathTarget = null;
        nextPathAttemptAt = 0L;
        if (clearRejected) {
            rejectedWaterEntries.clear();
            rejectedWaterEntriesExpireAt = 0L;
        }
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
        double closeEnough = Math.max(0.0D, walkTarget.getCloseEnoughDist());
        return dragon.position().distanceToSqr(walkTarget.getTarget().currentPosition())
                <= closeEnough * closeEnough;
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

    private void configureWaterHandoff(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (!context.memories().has(DragonMemories.ATTACK_TARGET)
                || !(dragon.getNavigation() instanceof PathNavigateGround navigation)) {
            releaseWaterHandoff(dragon);
            return;
        }
        if (waterHandoffActive) {
            return;
        }

        originalWaterMalus = dragon.getPathfindingMalus(BlockPathTypes.WATER);
        originalWaterBorderMalus = dragon.getPathfindingMalus(BlockPathTypes.WATER_BORDER);
        waterHandoffActive = true;
        navigation.setWaterEntryAllowed(true);
        dragon.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        dragon.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
    }

    private void releaseWaterHandoff(T dragon) {
        if (!waterHandoffActive) {
            return;
        }
        if (dragon.getNavigation() instanceof PathNavigateGround navigation) {
            navigation.setWaterEntryAllowed(false);
        }
        dragon.setPathfindingMalus(BlockPathTypes.WATER, originalWaterMalus);
        dragon.setPathfindingMalus(BlockPathTypes.WATER_BORDER, originalWaterBorderMalus);
        waterHandoffActive = false;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of(
                "path_target", resolvedPathTarget == null ? "none" : resolvedPathTarget.toString(),
                "water_entry", Boolean.toString(targetingWaterEntry),
                "water_handoff", Boolean.toString(waterHandoffActive),
                "entering_water", Boolean.toString(enteringWater),
                "water_target", waterEntryTarget == null ? "none" : waterEntryTarget.waterPosition().toString()
        );
    }
}
