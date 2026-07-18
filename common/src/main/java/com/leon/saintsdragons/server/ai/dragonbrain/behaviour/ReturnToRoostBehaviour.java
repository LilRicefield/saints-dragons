package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReturnToRoostBehaviour<T extends RideableDragonBase> extends DragonBehaviour<T> {
    private static final int SHORE_SEARCH_RADIUS = 32;
    private static final int SHORE_VERTICAL_SEARCH = 10;
    private static final int SHORE_SURFACE_SEARCH_UP = 32;
    private static final int MAX_SHORE_STEP = 2;
    private static final int SHORE_RESCAN_TICKS = 40;
    private static final int WATER_ENTRY_SEARCH_RADIUS = 28;
    private static final int WATER_ENTRY_TRANSITION_TIMEOUT = 40;
    private static final int MAX_GROUND_ROUTE_FAILURES = 5;
    private static final int FAILED_ROUTE_RETRY_TICKS = 100;
    private static final int SHORE_APPROACH_NO_PROGRESS_TICKS = 60;
    private static final double MIN_GROUND_SEGMENT_PROGRESS = 2.0D;
    private static final double MAX_WATER_ENTRY_DETOUR = 2.0D;
    private static final double MAX_SHORE_HOME_DETOUR = 2.0D;
    private static final double MIN_SHORE_APPROACH_PROGRESS = 0.75D;

    private final double arrivalRadiusSqr;
    private final double territoryRadiusSqr;
    private final double territoryReturnRadiusSqr;
    private final float groundSpeedModifier;
    private final double swimSpeedModifier;
    private final float swimTurnSpeed;
    @Nullable
    private ShoreTarget shoreTarget;
    private final Set<BlockPos> rejectedShoreTargets = new HashSet<>();
    private int shoreRescanTicks;
    private int shoreApproachNoProgressTicks;
    private double bestShoreApproachDistance = Double.NaN;
    private boolean exitingWater;
    private boolean approachingShore;
    private boolean returningToTerritory;
    @Nullable
    private ShoreTarget waterEntryTarget;
    private final Set<BlockPos> rejectedWaterEntries = new HashSet<>();
    private int waterEntryTransitionTicks;
    private int groundRouteFailures;
    private boolean routeRetryRequested;
    private double groundAttemptStartDistance = Double.NaN;
    private ReturnPhase returnPhase = ReturnPhase.INACTIVE;
    private String returnPhaseReason = "not-started";

    public ReturnToRoostBehaviour(double arrivalRadius,
                                  double territoryRadius,
                                  double territoryReturnRadius,
                                  float groundSpeedModifier,
                                  double swimSpeedModifier,
                                  float swimTurnSpeed) {
        super(Map.of(
                DragonMemories.HOME, MemoryStatus.VALUE_PRESENT,
                DragonMemories.ROOST_SLEEP_POSITION, MemoryStatus.REGISTERED,
                DragonMemories.MOVEMENT_INTENT, MemoryStatus.REGISTERED
        ));
        this.arrivalRadiusSqr = arrivalRadius * arrivalRadius;
        this.territoryRadiusSqr = territoryRadius * territoryRadius;
        this.territoryReturnRadiusSqr = territoryReturnRadius * territoryReturnRadius;
        this.groundSpeedModifier = groundSpeedModifier;
        this.swimSpeedModifier = swimSpeedModifier;
        this.swimTurnSpeed = swimTurnSpeed;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        boolean returningForSleep = shouldReturnForSleep(context);
        return (returningForSleep && needsSleepReturnMovement(context))
                || isOutsideTerritory(context, territoryRadiusSqr);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        if (routeRetryRequested) {
            return false;
        }
        boolean returningForSleep = shouldReturnForSleep(context);
        if (returningForSleep) {
            return needsSleepReturnMovement(context);
        }
        if (!returningToTerritory && isOutsideTerritory(context, territoryRadiusSqr)) {
            returningToTerritory = true;
        }
        return returningToTerritory && isOutsideTerritory(context, territoryReturnRadiusSqr);
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        shoreTarget = null;
        rejectedShoreTargets.clear();
        waterEntryTarget = null;
        rejectedWaterEntries.clear();
        shoreRescanTicks = 0;
        resetShoreApproachProgress();
        waterEntryTransitionTicks = 0;
        groundRouteFailures = 0;
        routeRetryRequested = false;
        groundAttemptStartDistance = Double.NaN;
        exitingWater = context.dragon().isInWaterOrBubble();
        approachingShore = false;
        returningToTerritory = !shouldReturnForSleep(context)
                && isOutsideTerritory(context, territoryRadiusSqr);
        returnPhase = ReturnPhase.INACTIVE;
        returnPhaseReason = "starting";
        updateReturnMovement(context);
    }

    @Override
    protected int cooldownForTicks(DragonBrainContext<T> context) {
        return routeRetryRequested ? FAILED_ROUTE_RETRY_TICKS : 0;
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        updateReturnMovement(context);
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        boolean returningForSleep = shouldReturnForSleep(context);
        setReturnPhase(
                context,
                ReturnPhase.INACTIVE,
                "behaviour-stopped",
                returningForSleep,
                getDestination(context, returningForSleep)
        );
        stopReturnMovement(context);
    }

    @Override
    public List<MemoryModuleType<?>> clearMemoriesWhenStopped() {
        return List.of(
                DragonMemories.MOVEMENT_INTENT,
                DragonMemories.WALK_TARGET,
                DragonMemories.PATH,
                DragonMemories.CANT_REACH_WALK_TARGET_SINCE
        );
    }

    private boolean shouldReturnForSleep(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        GlobalPos home = context.memories().get(DragonMemories.HOME).orElse(null);
        return home != null
                && home.dimension().equals(context.level().dimension())
                && !dragon.isTame()
                && dragon.supportsSleep()
                && dragon.getSleepPreferences().canSleepDuringConditions(context.level())
                && dragon.isAlive()
                && !dragon.isDying()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSleeping()
                && !dragon.isSleepTransitioning()
                && dragon.hurtTime <= 0
                && dragon.getTarget() == null;
    }

    private boolean needsSleepReturnMovement(DragonBrainContext<T> context) {
        GlobalPos destination = getDestination(context, true);
        return context.dragon().isInWaterOrBubble()
                || destination.pos().distSqr(context.dragon().blockPosition()) > arrivalRadiusSqr;
    }

    private void updateReturnMovement(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (routeRetryRequested) {
            return;
        }
        GlobalPos home = context.memories().get(DragonMemories.HOME).orElse(null);
        if (home == null) {
            return;
        }
        boolean returningForSleep = shouldReturnForSleep(context);
        GlobalPos destination = getDestination(context, returningForSleep);

        boolean needsMovement = returningForSleep
                ? needsSleepReturnMovement(context)
                : dragon.isInWaterOrBubble()
                        || isOutsideTerritory(context, territoryReturnRadiusSqr);
        if (!needsMovement) {
            setReturnPhase(
                    context,
                    ReturnPhase.ARRIVED,
                    returningForSleep ? "inside-sleep-radius" : "inside-territory-return-radius",
                    returningForSleep,
                    destination
            );
            logReturnState(context, returningForSleep, destination, false);
            stopReturnMovement(context);
            return;
        }

        if (dragon.isInWaterOrBubble()) {
            exitingWater = true;
            waterEntryTarget = null;
            rejectedWaterEntries.clear();
            waterEntryTransitionTicks = 0;
            groundRouteFailures = 0;
            groundAttemptStartDistance = Double.NaN;
        } else if (exitingWater && dragon.onGround()) {
            exitingWater = false;
            approachingShore = false;
        }

        if (exitingWater) {
            context.memories().erase(DragonMemories.MOVEMENT_INTENT);
            context.memories().erase(DragonMemories.WALK_TARGET);
            dragon.getNavigation().stop();
            AsyncSwimController controller = dragon.getAiSwimController();
            double speed = swimSpeedModifier;
            if (dragon instanceof SemiAquaticDragon swimmer) {
                speed *= swimmer.getSwimSpeed();
            }

            double endpointRadius = Math.max(3.5D, dragon.getBbWidth() + 2.0D);
            if (!approachingShore
                    && (controller.hasReachedPathEnd()
                            || (dragon.horizontalCollision && controller.isNearPathEnd(endpointRadius)))) {
                approachingShore = true;
                String reason = controller.hasReachedPathEnd()
                        ? "swim-path-end"
                        : "collision-near-swim-end";
                controller.clear();
                shoreTarget = null;
                shoreRescanTicks = 0;
                resetShoreApproachProgress();
                setReturnPhase(context, ReturnPhase.SHORE_SEARCH, reason, returningForSleep, destination);
            }

            if (!approachingShore) {
                Vec3 finalTarget = Vec3.atBottomCenterOf(destination.pos());
                if (controller.trackTarget(finalTarget, speed, swimTurnSpeed)) {
                    setReturnPhase(
                            context,
                            ReturnPhase.WATER_ROUTE,
                            "path-to-final-destination",
                            returningForSleep,
                            destination
                    );
                    controller.serverTick();
                    logReturnState(context, returningForSleep, destination, false);
                    return;
                }
                approachingShore = true;
                controller.clear();
                shoreTarget = null;
                shoreRescanTicks = 0;
                resetShoreApproachProgress();
                setReturnPhase(
                        context,
                        ReturnPhase.SHORE_SEARCH,
                        "final-swim-target-rejected",
                        returningForSleep,
                        destination
                );
            }

            updateShoreTarget(context, destination.pos());

            if (shoreTarget == null) {
                setReturnPhase(
                        context,
                        ReturnPhase.SHORE_SEARCH,
                        "no-valid-local-shore",
                        returningForSleep,
                        destination
                );
                controller.stop();
                holdAtSurface(dragon);
                logReturnState(context, returningForSleep, destination, false);
                return;
            }

            if (hasShoreApproachStalled(dragon)) {
                rejectCurrentShoreTarget(controller);
                setReturnPhase(
                        context,
                        ReturnPhase.SHORE_SEARCH,
                        "shore-approach-no-progress",
                        returningForSleep,
                        destination
                );
                holdAtSurface(dragon);
                logReturnState(context, returningForSleep, destination, true);
                return;
            }

            if (shouldBeginShoreTransition(dragon, shoreTarget)) {
                setReturnPhase(
                        context,
                        ReturnPhase.SHORE_TRANSITION,
                        "inside-shore-assist-radius",
                        returningForSleep,
                        destination
                );
                controller.stop();
                applyShoreTransition(dragon, shoreTarget.landPosition());
                logReturnState(context, returningForSleep, destination, false);
                return;
            }

            Vec3 swimTarget = shoreTarget.waterApproach();
            boolean accepted = controller.trackTarget(swimTarget, speed, swimTurnSpeed);
            if (accepted) {
                setReturnPhase(
                        context,
                        ReturnPhase.SHORE_APPROACH,
                        "path-to-local-shore",
                        returningForSleep,
                        destination
                );
                controller.serverTick();
            } else {
                rejectCurrentShoreTarget(controller);
                setReturnPhase(
                        context,
                        ReturnPhase.SHORE_SEARCH,
                        "local-shore-target-rejected",
                        returningForSleep,
                        destination
                );
            }
            logReturnState(context, returningForSleep, destination, false);
            return;
        }

        shoreTarget = null;
        rejectedShoreTargets.clear();
        shoreRescanTicks = 0;
        resetShoreApproachProgress();
        exitingWater = false;
        approachingShore = false;
        dragon.getAiSwimController().stop();

        if (waterEntryTarget != null) {
            Vec3 waterEntryLand = waterEntryTarget.landPosition();
            if (dragon.getAIMovement().hasFailed()) {
                if (retryGroundSegmentAfterProgress(
                        context,
                        waterEntryLand,
                        returningForSleep,
                        destination,
                        ReturnPhase.WATER_ENTRY_ROUTE,
                        "water-entry-partial-path-made-progress"
                )) {
                    return;
                }
                rejectWaterEntryAndTryAnother(context, returningForSleep, destination, "water-entry-ground-path-failed");
                return;
            }

            if (shouldBeginWaterEntryTransition(dragon, waterEntryTarget)) {
                if (returnPhase != ReturnPhase.WATER_ENTRY_TRANSITION) {
                    clearGroundReturnMovement(context);
                    waterEntryTransitionTicks = 0;
                    groundAttemptStartDistance = Double.NaN;
                }
                waterEntryTransitionTicks++;
                setReturnPhase(
                        context,
                        ReturnPhase.WATER_ENTRY_TRANSITION,
                        "entering-water-toward-roost",
                        returningForSleep,
                        destination
                );
                applyWaterEntryTransition(dragon, waterEntryTarget.waterApproach());
                if (waterEntryTransitionTicks >= WATER_ENTRY_TRANSITION_TIMEOUT) {
                    rejectWaterEntryAndTryAnother(
                            context,
                            returningForSleep,
                            destination,
                            "water-entry-transition-timed-out"
                    );
                    return;
                }
                logReturnState(context, returningForSleep, destination, false);
                return;
            }

            context.memories().erase(DragonMemories.WALK_TARGET);
            beginGroundAttempt(dragon, waterEntryLand);
            context.memories().set(
                    DragonMemories.MOVEMENT_INTENT,
                    DragonMovementIntent.progressiveGround(waterEntryLand, groundSpeedModifier, false)
            );
            setReturnPhase(
                    context,
                    ReturnPhase.WATER_ENTRY_ROUTE,
                    "path-to-roost-facing-water-entry",
                    returningForSleep,
                    destination
            );
            logReturnState(context, returningForSleep, destination, false);
            return;
        }

        Vec3 finalGroundTarget = Vec3.atBottomCenterOf(destination.pos());
        if (dragon.getAIMovement().hasFailed()) {
            if (retryGroundSegmentAfterProgress(
                    context,
                    finalGroundTarget,
                    returningForSleep,
                    destination,
                    ReturnPhase.GROUND_ROUTE,
                    "partial-ground-route-made-progress"
            )) {
                return;
            }
            groundRouteFailures++;
            clearGroundReturnMovement(context);
            waterEntryTarget = findWaterEntryTarget(context, destination.pos());
            groundAttemptStartDistance = Double.NaN;
            if (waterEntryTarget == null || groundRouteFailures >= MAX_GROUND_ROUTE_FAILURES) {
                failReturnRoute(context, returningForSleep, destination, "ground-route-unreachable-no-water-entry");
            } else {
                setReturnPhase(
                        context,
                        ReturnPhase.WATER_ENTRY_ROUTE,
                        "ground-route-failed-found-water-entry",
                        returningForSleep,
                        destination
                );
            }
            return;
        }

        context.memories().erase(DragonMemories.WALK_TARGET);
        beginGroundAttempt(dragon, finalGroundTarget);
        context.memories().set(
                DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.progressiveGround(finalGroundTarget, groundSpeedModifier, false)
        );
        setReturnPhase(
                context,
                ReturnPhase.GROUND_ROUTE,
                "path-to-final-destination",
                returningForSleep,
                destination
        );
        logReturnState(context, returningForSleep, destination, false);
    }

    private GlobalPos getDestination(DragonBrainContext<T> context, boolean returningForSleep) {
        GlobalPos home = context.memories().get(DragonMemories.HOME).orElse(null);
        if (!returningForSleep || home == null) {
            return home;
        }
        return context.memories().get(DragonMemories.ROOST_SLEEP_POSITION)
                .filter(position -> position.dimension().equals(context.level().dimension()))
                .orElse(home);
    }

    private boolean isOutsideTerritory(DragonBrainContext<T> context, double radiusSqr) {
        T dragon = context.dragon();
        GlobalPos home = context.memories().get(DragonMemories.HOME).orElse(null);
        if (home == null
                || !home.dimension().equals(context.level().dimension())
                || dragon.isTame()
                || !dragon.isAlive()
                || dragon.isDying()
                || dragon.isVehicle()
                || dragon.isPassenger()
                || dragon.isOrderedToSit()) {
            return false;
        }
        double dx = dragon.getX() - (home.pos().getX() + 0.5D);
        double dz = dragon.getZ() - (home.pos().getZ() + 0.5D);
        return dx * dx + dz * dz > radiusSqr;
    }

    private void stopReturnMovement(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        shoreTarget = null;
        rejectedShoreTargets.clear();
        waterEntryTarget = null;
        rejectedWaterEntries.clear();
        shoreRescanTicks = 0;
        resetShoreApproachProgress();
        waterEntryTransitionTicks = 0;
        groundRouteFailures = 0;
        groundAttemptStartDistance = Double.NaN;
        exitingWater = false;
        approachingShore = false;
        returningToTerritory = false;
        dragon.getAiSwimController().stop();
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        context.memories().erase(DragonMemories.WALK_TARGET);
        context.memories().erase(DragonMemories.PATH);
        context.memories().erase(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
        dragon.getAIMovement().stop();
    }

    private void rejectWaterEntryAndTryAnother(DragonBrainContext<T> context,
                                               boolean returningForSleep,
                                               GlobalPos destination,
                                               String reason) {
        if (waterEntryTarget != null) {
            rejectedWaterEntries.add(BlockPos.containing(
                    waterEntryTarget.landPosition().x,
                    waterEntryTarget.landPosition().y,
                    waterEntryTarget.landPosition().z
            ));
        }
        groundRouteFailures++;
        waterEntryTransitionTicks = 0;
        waterEntryTarget = null;
        groundAttemptStartDistance = Double.NaN;
        clearGroundReturnMovement(context);

        if (groundRouteFailures < MAX_GROUND_ROUTE_FAILURES) {
            waterEntryTarget = findWaterEntryTarget(context, destination.pos());
        }
        if (waterEntryTarget == null) {
            failReturnRoute(context, returningForSleep, destination, reason);
            return;
        }
        setReturnPhase(
                context,
                ReturnPhase.WATER_ENTRY_ROUTE,
                reason + "-trying-alternate",
                returningForSleep,
                destination
        );
    }

    private void beginGroundAttempt(T dragon, Vec3 target) {
        if (Double.isNaN(groundAttemptStartDistance)) {
            groundAttemptStartDistance = Math.sqrt(dragon.position().distanceToSqr(target));
        }
    }

    private boolean retryGroundSegmentAfterProgress(DragonBrainContext<T> context,
                                                    Vec3 target,
                                                    boolean returningForSleep,
                                                    GlobalPos destination,
                                                    ReturnPhase retryPhase,
                                                    String reason) {
        T dragon = context.dragon();
        double currentDistance = Math.sqrt(dragon.position().distanceToSqr(target));
        if (Double.isNaN(groundAttemptStartDistance)
                || groundAttemptStartDistance - currentDistance < MIN_GROUND_SEGMENT_PROGRESS) {
            return false;
        }

        clearGroundReturnMovement(context);
        dragon.getAIMovement().clearGroundPathFailureRetry();
        groundAttemptStartDistance = currentDistance;
        setReturnPhase(context, retryPhase, reason, returningForSleep, destination);
        return true;
    }

    private void failReturnRoute(DragonBrainContext<T> context,
                                 boolean returningForSleep,
                                 GlobalPos destination,
                                 String reason) {
        routeRetryRequested = true;
        clearGroundReturnMovement(context);
        context.dragon().getAiSwimController().stop();
        setReturnPhase(context, ReturnPhase.ROUTE_FAILED, reason, returningForSleep, destination);
        logReturnState(context, returningForSleep, destination, true);
    }

    private void clearGroundReturnMovement(DragonBrainContext<T> context) {
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        context.memories().erase(DragonMemories.WALK_TARGET);
        context.memories().erase(DragonMemories.PATH);
        context.memories().erase(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
        context.dragon().getAIMovement().stop();
    }

    private void updateShoreTarget(DragonBrainContext<T> context, BlockPos homePos) {
        if (shoreTarget != null) {
            return;
        }
        if (shoreRescanTicks > 0) {
            shoreRescanTicks--;
            return;
        }
        shoreTarget = findShoreTarget(context, homePos);
        shoreRescanTicks = SHORE_RESCAN_TICKS;
        if (shoreTarget != null) {
            bestShoreApproachDistance = Math.sqrt(
                    context.dragon().position().distanceToSqr(shoreTarget.waterApproach())
            );
            shoreApproachNoProgressTicks = 0;
        }
    }

    private boolean hasShoreApproachStalled(T dragon) {
        if (shoreTarget == null) {
            return false;
        }
        double currentDistance = Math.sqrt(dragon.position().distanceToSqr(shoreTarget.waterApproach()));
        if (Double.isNaN(bestShoreApproachDistance)
                || bestShoreApproachDistance - currentDistance >= MIN_SHORE_APPROACH_PROGRESS) {
            bestShoreApproachDistance = currentDistance;
            shoreApproachNoProgressTicks = 0;
            return false;
        }
        return ++shoreApproachNoProgressTicks >= SHORE_APPROACH_NO_PROGRESS_TICKS;
    }

    private void rejectCurrentShoreTarget(AsyncSwimController controller) {
        if (shoreTarget != null) {
            rejectedShoreTargets.add(BlockPos.containing(shoreTarget.landPosition()));
        }
        shoreTarget = null;
        shoreRescanTicks = 0;
        resetShoreApproachProgress();
        controller.clear();
    }

    private void resetShoreApproachProgress() {
        shoreApproachNoProgressTicks = 0;
        bestShoreApproachDistance = Double.NaN;
    }

    @Nullable
    private ShoreTarget findWaterEntryTarget(DragonBrainContext<T> context, BlockPos destination) {
        T dragon = context.dragon();
        BlockPos origin = dragon.blockPosition();
        int bodyMargin = Math.max(1, Mth.ceil(dragon.getBbWidth() * 0.5F));
        ShoreTarget best = null;
        double bestScore = Double.MAX_VALUE;

        for (int radius = 1; radius <= WATER_ENTRY_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                ShoreTarget north = evaluateWaterEntryColumn(
                        context,
                        origin.offset(dx, 0, -radius),
                        bodyMargin,
                        origin.getY()
                );
                double northScore = waterEntryScore(dragon, north, destination);
                if (northScore < bestScore) {
                    best = north;
                    bestScore = northScore;
                }

                ShoreTarget south = evaluateWaterEntryColumn(
                        context,
                        origin.offset(dx, 0, radius),
                        bodyMargin,
                        origin.getY()
                );
                double southScore = waterEntryScore(dragon, south, destination);
                if (southScore < bestScore) {
                    best = south;
                    bestScore = southScore;
                }
            }
            for (int dz = -radius + 1; dz <= radius - 1; dz++) {
                ShoreTarget west = evaluateWaterEntryColumn(
                        context,
                        origin.offset(-radius, 0, dz),
                        bodyMargin,
                        origin.getY()
                );
                double westScore = waterEntryScore(dragon, west, destination);
                if (westScore < bestScore) {
                    best = west;
                    bestScore = westScore;
                }

                ShoreTarget east = evaluateWaterEntryColumn(
                        context,
                        origin.offset(radius, 0, dz),
                        bodyMargin,
                        origin.getY()
                );
                double eastScore = waterEntryScore(dragon, east, destination);
                if (eastScore < bestScore) {
                    best = east;
                    bestScore = eastScore;
                }
            }
        }
        return best;
    }

    @Nullable
    private ShoreTarget evaluateWaterEntryColumn(DragonBrainContext<T> context,
                                                  BlockPos column,
                                                  int bodyMargin,
                                                  int referenceY) {
        ShoreTarget candidate = evaluateShoreColumn(context, column, bodyMargin, referenceY);
        if (candidate == null) {
            return null;
        }
        BlockPos land = BlockPos.containing(
                candidate.landPosition().x,
                candidate.landPosition().y,
                candidate.landPosition().z
        );
        return rejectedWaterEntries.contains(land) ? null : candidate;
    }

    private double waterEntryScore(T dragon, @Nullable ShoreTarget candidate, BlockPos destination) {
        if (candidate == null) {
            return Double.MAX_VALUE;
        }
        Vec3 destinationCenter = Vec3.atBottomCenterOf(destination);
        double currentDistance = Math.sqrt(dragon.position().distanceToSqr(destinationCenter));
        double entryDistance = Math.sqrt(candidate.waterApproach().distanceToSqr(destinationCenter));
        if (entryDistance > currentDistance + MAX_WATER_ENTRY_DETOUR) {
            return Double.MAX_VALUE;
        }
        return entryDistance * entryDistance * 4.0D
                + candidate.landPosition().distanceToSqr(dragon.position());
    }

    @Nullable
    private ShoreTarget findShoreTarget(DragonBrainContext<T> context, BlockPos homePos) {
        T dragon = context.dragon();
        BlockPos searchOrigin = dragon.blockPosition();
        BlockPos currentSurface = findSurfaceWater(context, searchOrigin, searchOrigin.getY());
        if (currentSurface == null) {
            return null;
        }

        ShoreTarget best = null;
        double bestScore = Double.MAX_VALUE;
        int bodyMargin = Math.max(1, Mth.ceil(dragon.getBbWidth() * 0.5F));

        for (int radius = 1; radius <= SHORE_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                ShoreTarget north = evaluateShoreColumn(
                        context, searchOrigin.offset(dx, 0, -radius), bodyMargin, currentSurface.getY()
                );
                double northScore = shoreScore(dragon, north, homePos);
                if (northScore < bestScore) {
                    best = north;
                    bestScore = northScore;
                }
                ShoreTarget south = evaluateShoreColumn(
                        context, searchOrigin.offset(dx, 0, radius), bodyMargin, currentSurface.getY()
                );
                double southScore = shoreScore(dragon, south, homePos);
                if (southScore < bestScore) {
                    best = south;
                    bestScore = southScore;
                }
            }
            for (int dz = -radius + 1; dz <= radius - 1; dz++) {
                ShoreTarget west = evaluateShoreColumn(
                        context, searchOrigin.offset(-radius, 0, dz), bodyMargin, currentSurface.getY()
                );
                double westScore = shoreScore(dragon, west, homePos);
                if (westScore < bestScore) {
                    best = west;
                    bestScore = westScore;
                }
                ShoreTarget east = evaluateShoreColumn(
                        context, searchOrigin.offset(radius, 0, dz), bodyMargin, currentSurface.getY()
                );
                double eastScore = shoreScore(dragon, east, homePos);
                if (eastScore < bestScore) {
                    best = east;
                    bestScore = eastScore;
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    @Nullable
    private ShoreTarget evaluateShoreColumn(DragonBrainContext<T> context,
                                             BlockPos column,
                                             int bodyMargin,
                                             int waterSurfaceY) {
        if (!context.level().hasChunkAt(column)) {
            return null;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacentWater = findSurfaceWater(
                    context,
                    column.relative(direction),
                    waterSurfaceY,
                    MAX_SHORE_STEP + 2,
                    MAX_SHORE_STEP + 2
            );
            if (adjacentWater == null) {
                continue;
            }
            BlockPos edgeLand = findStandableLand(
                    context,
                    column.getX(),
                    column.getZ(),
                    adjacentWater.getY() + 1,
                    MAX_SHORE_STEP
            );
            if (edgeLand == null) {
                continue;
            }
            BlockPos approachColumn = edgeLand.relative(direction, bodyMargin + 1);
            BlockPos approachWater = findSurfaceWater(
                    context,
                    approachColumn,
                    adjacentWater.getY(),
                    MAX_SHORE_STEP,
                    MAX_SHORE_STEP
            );
            if (approachWater == null
                    || Math.abs(approachWater.getY() - adjacentWater.getY()) > MAX_SHORE_STEP) {
                continue;
            }
            BlockPos inlandColumn = edgeLand.relative(direction.getOpposite(), bodyMargin);
            BlockPos landPosition = findStandableLand(
                    context,
                    inlandColumn.getX(),
                    inlandColumn.getZ(),
                    edgeLand.getY(),
                    MAX_SHORE_STEP
            );
            if (landPosition == null) {
                continue;
            }
            Vec3 waterApproach = Vec3.atCenterOf(approachWater);
            if (!hasBodyClearance(context, waterApproach)) {
                continue;
            }
            return new ShoreTarget(waterApproach, Vec3.atBottomCenterOf(landPosition));
        }
        return null;
    }

    @Nullable
    private BlockPos findStandableLand(DragonBrainContext<T> context,
                                       int x,
                                       int z,
                                       int referenceY,
                                       int verticalRange) {
        for (int offset = 0; offset <= verticalRange; offset++) {
            BlockPos above = new BlockPos(x, referenceY + offset, z);
            if (isStandableLand(context, above)) {
                return above;
            }
            if (offset == 0) {
                continue;
            }
            BlockPos below = new BlockPos(x, referenceY - offset, z);
            if (isStandableLand(context, below)) {
                return below;
            }
        }
        return null;
    }

    private boolean isStandableLand(DragonBrainContext<T> context, BlockPos feet) {
        return context.level().getFluidState(feet).isEmpty()
                && !context.level().getBlockState(feet.below()).getCollisionShape(context.level(), feet.below()).isEmpty()
                && hasVerticalClearance(context, feet);
    }

    private boolean hasVerticalClearance(DragonBrainContext<T> context, BlockPos feet) {
        int requiredHeight = Math.max(2, Mth.ceil(context.dragon().getBbHeight()));
        for (int dy = 0; dy < requiredHeight; dy++) {
            BlockPos check = feet.above(dy);
            if (!context.level().getFluidState(check).isEmpty()
                    || !context.level().getBlockState(check).getCollisionShape(context.level(), check).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private BlockPos findSurfaceWater(DragonBrainContext<T> context, BlockPos column, int referenceY) {
        return findSurfaceWater(
                context,
                column,
                referenceY,
                SHORE_SURFACE_SEARCH_UP,
                SHORE_VERTICAL_SEARCH
        );
    }

    @Nullable
    private BlockPos findSurfaceWater(DragonBrainContext<T> context,
                                      BlockPos column,
                                      int referenceY,
                                      int searchUp,
                                      int searchDown) {
        for (int y = referenceY + searchUp; y >= referenceY - searchDown; y--) {
            BlockPos water = new BlockPos(column.getX(), y, column.getZ());
            if (context.level().getFluidState(water).is(FluidTags.WATER)
                    && !context.level().getFluidState(water.above()).is(FluidTags.WATER)
                    && context.level().getBlockState(water).getCollisionShape(context.level(), water).isEmpty()) {
                return water;
            }
        }
        return null;
    }

    private void holdAtSurface(T dragon) {
        Vec3 velocity = dragon.getDeltaMovement();
        if (!dragon.isInWaterOrBubble()) {
            dragon.setDeltaMovement(velocity.x * 0.8D, Math.min(velocity.y, -0.04D), velocity.z * 0.8D);
            dragon.hasImpulse = true;
            return;
        }
        double upward = dragon.getFluidHeight(FluidTags.WATER) > dragon.getBbHeight() * 0.65D
                ? 0.10D
                : 0.04D;
        dragon.setDeltaMovement(velocity.x * 0.8D, Math.max(velocity.y, upward), velocity.z * 0.8D);
        dragon.hasImpulse = true;
    }

    private boolean hasBodyClearance(DragonBrainContext<T> context, Vec3 feetPosition) {
        T dragon = context.dragon();
        double halfWidth = dragon.getBbWidth() * 0.5D;
        AABB bounds = new AABB(
                feetPosition.x - halfWidth,
                feetPosition.y,
                feetPosition.z - halfWidth,
                feetPosition.x + halfWidth,
                feetPosition.y + dragon.getBbHeight(),
                feetPosition.z + halfWidth
        );
        return context.level().noCollision(dragon, bounds);
    }

    private double shoreScore(T dragon, @Nullable ShoreTarget candidate, BlockPos homePos) {
        if (candidate == null) {
            return Double.MAX_VALUE;
        }
        BlockPos candidateLand = BlockPos.containing(candidate.landPosition());
        if (rejectedShoreTargets.contains(candidateLand)) {
            return Double.MAX_VALUE;
        }
        Vec3 homeCenter = Vec3.atBottomCenterOf(homePos);
        double currentHomeDistance = Math.sqrt(dragon.position().distanceToSqr(homeCenter));
        double landHomeDistance = Math.sqrt(candidate.landPosition().distanceToSqr(homeCenter));
        if (landHomeDistance > currentHomeDistance + MAX_SHORE_HOME_DETOUR) {
            return Double.MAX_VALUE;
        }
        return landHomeDistance * landHomeDistance * 4.0D
                + candidate.waterApproach().distanceToSqr(dragon.position());
    }

    private boolean shouldBeginShoreTransition(T dragon, ShoreTarget target) {
        double assistRadius = Math.max(2.5D, dragon.getBbWidth() * 0.75D + 1.0D);
        return dragon.position().distanceToSqr(target.waterApproach()) <= assistRadius * assistRadius;
    }

    private boolean shouldBeginWaterEntryTransition(T dragon, ShoreTarget target) {
        double assistRadius = Math.max(2.5D, dragon.getBbWidth() * 0.75D + 1.0D);
        return dragon.position().distanceToSqr(target.landPosition()) <= assistRadius * assistRadius;
    }

    private void applyShoreTransition(T dragon, Vec3 landPosition) {
        Vec3 toLand = landPosition.subtract(dragon.position());
        Vec3 horizontal = new Vec3(toLand.x, 0.0D, toLand.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return;
        }

        Vec3 direction = horizontal.normalize();
        Vec3 velocity = dragon.getDeltaMovement();
        double horizontalBoost = dragon.horizontalCollision ? 0.48D : 0.36D;
        double upward = dragon.horizontalCollision ? 0.58D : 0.34D;
        dragon.setDeltaMovement(
                velocity.x * 0.45D + direction.x * horizontalBoost,
                Math.max(velocity.y, upward),
                velocity.z * 0.45D + direction.z * horizontalBoost
        );
        dragon.getMoveControl().setWantedPosition(landPosition.x, landPosition.y, landPosition.z, 1.15D);
        dragon.hasImpulse = true;
    }

    private void applyWaterEntryTransition(T dragon, Vec3 waterPosition) {
        Vec3 toWater = waterPosition.subtract(dragon.position());
        Vec3 horizontal = new Vec3(toWater.x, 0.0D, toWater.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return;
        }

        Vec3 direction = horizontal.normalize();
        Vec3 velocity = dragon.getDeltaMovement();
        double horizontalBoost = dragon.horizontalCollision ? 0.45D : 0.34D;
        double upward = dragon.onGround() ? 0.20D : velocity.y;
        dragon.setDeltaMovement(
                velocity.x * 0.4D + direction.x * horizontalBoost,
                Math.max(velocity.y, upward),
                velocity.z * 0.4D + direction.z * horizontalBoost
        );
        dragon.getMoveControl().setWantedPosition(
                waterPosition.x,
                waterPosition.y - 0.35D,
                waterPosition.z,
                1.1D
        );
        dragon.hasImpulse = true;
    }

    private void setReturnPhase(DragonBrainContext<T> context,
                                ReturnPhase phase,
                                String reason,
                                boolean returningForSleep,
                                @Nullable GlobalPos destination) {
        returnPhase = phase;
        returnPhaseReason = reason;
    }

    private void logReturnState(DragonBrainContext<T> context,
                                boolean returningForSleep,
                                @Nullable GlobalPos destination,
                                boolean force) {
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of(
                "phase", returnPhase.name().toLowerCase(),
                "reason", returnPhaseReason,
                "exiting_water", Boolean.toString(exitingWater),
                "approaching_shore", Boolean.toString(approachingShore),
                "shore_target", shoreTarget == null ? "none" : shoreTarget.waterApproach().toString(),
                "route_retry", Boolean.toString(routeRetryRequested)
        );
    }

    private enum ReturnPhase {
        INACTIVE,
        WATER_ROUTE,
        SHORE_SEARCH,
        SHORE_APPROACH,
        SHORE_TRANSITION,
        WATER_ENTRY_ROUTE,
        WATER_ENTRY_TRANSITION,
        GROUND_ROUTE,
        ROUTE_FAILED,
        ARRIVED
    }

    private record ShoreTarget(Vec3 waterApproach, Vec3 landPosition) {
    }
}
