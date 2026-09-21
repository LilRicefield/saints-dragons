package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.DragonAirCombatHelper;
import com.leon.saintsdragons.server.ai.DragonAirCombatSettings;
import com.leon.saintsdragons.server.ai.DragonAirCombatSettingsProvider;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonTargetLifecycle;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonAwarenessMemory;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonInvestigation;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonPerceptionProfile;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonSensoryObservation;
import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonCombatPositioning;
import com.leon.saintsdragons.server.ai.navigation.DragonAIMovementController;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class DragonInvestigateTargetBehaviour<T extends DragonEntity> extends DragonBehaviour<T> {
    private static final int RECENT_LOCATION_MEMORY_TICKS = 20 * 30;
    private static final int FAILED_LOCATION_MEMORY_TICKS = 20 * 5;
    private static final int MAX_RECENT_LOCATIONS = 4;
    private static final double DESTINATION_REFRESH_DISTANCE_SQR = 1.0D;
    private static final int SOURCE_WAYPOINT_REFRESH_TICKS = 10;
    private static final double SOURCE_WAYPOINT_REFRESH_DISTANCE_SQR = 2.0D * 2.0D;
    private static final double SOURCE_WAYPOINT_MIN_ADJUSTMENT_SQR = 0.75D * 0.75D;
    private static final int AIR_SEARCH_TICKS = 20 * 6;
    private static final int AIR_SEARCH_WAYPOINT_TICKS = 20 * 2;
    private static final int GROUND_PURSUIT_SEARCH_TICKS = 20 * 5;
    private static final int GROUND_SEARCH_WAYPOINT_TICKS = 20 * 2;
    private static final int MAX_GROUND_SEARCH_WAYPOINTS = 3;
    private static final double GROUND_PURSUIT_ARRIVAL_DISTANCE = 1.5D;
    private static final int MOVEMENT_RETRY_TICKS = 20;
    private static final int MAX_MOVEMENT_FAILURES = 3;

    private final Deque<RecentLocation> recentLocations = new ArrayDeque<>();
    private final Deque<Vec3> searchVisits = new ArrayDeque<>();
    private int searchTicks;
    private boolean issuedMovement;
    private long movementGeneration = Long.MIN_VALUE;
    private Vec3 destination;
    private DragonSensoryObservation activeObservation;
    private long nextSourceWaypointRefreshAt;
    private boolean trackingProjectileSource;
    private boolean airborneSearch;
    private Vec3 searchWaypoint;
    private double searchAngle;
    private int searchWaypointTicks;
    private Vec3 groundSearchForward = Vec3.ZERO;
    private int groundSearchAttempts;
    private boolean combatPursuit;
    private double pursuitSpeed;
    private int movementFailures;
    private long nextMovementAttemptAt;
    private String investigationKind = "none";
    private Phase phase = Phase.IDLE;
    private String outcome = "none";
    private boolean actionPaused;

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return !hasCommittedAction(context.dragon()) && canInvestigate(context);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return canInvestigate(context)
                && (!airborneSearch || (context.dragon().isAerial()
                && !context.dragon().onGround() && !context.dragon().isInWaterOrBubble()));
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        searchVisits.clear();
        searchTicks = 0;
        issuedMovement = false;
        movementGeneration = Long.MIN_VALUE;
        destination = null;
        activeObservation = null;
        nextSourceWaypointRefreshAt = 0L;
        trackingProjectileSource = false;
        airborneSearch = false;
        searchWaypoint = null;
        searchWaypointTicks = 0;
        groundSearchForward = Vec3.ZERO;
        groundSearchAttempts = 0;
        investigationKind = "none";
        combatPursuit = false;
        movementFailures = 0;
        nextMovementAttemptAt = 0;
        phase = Phase.IDLE;
        outcome = "none";
        actionPaused = false;
        context.dragon().combatManager.recordAiDecision("investigation", "started");
        pruneRecentLocations(context.gameTime());
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        if (!(context.dragon() instanceof RideableDragonBase dragon)) {
            return;
        }
        if (hasCommittedAction(dragon)) {
            if (!actionPaused) {
                dragon.combatManager.recordAiDecision("investigation", "paused:committed-action");
                actionPaused = true;
            }
            return;
        }
        if (actionPaused) {
            actionPaused = false;
            dragon.combatManager.recordAiDecision("investigation", "resumed");
        }
        DragonSensoryObservation observation = context.memories()
                .get(DragonMemories.INVESTIGATION_TARGET)
                .orElse(null);
        if (observation == null) {
            return;
        }

        DragonPerceptionProfile profile = DragonPerceptionProfile.forDragon(dragon);
        if (!observation.equals(activeObservation)) {
            boolean searching = phase == Phase.TRAVELLING || phase == Phase.SEARCHING;
            if (searching && isSameProjectileSource(activeObservation, observation)) {
                activeObservation = observation;
                trackingProjectileSource = true;
                outcome = "evidence-refreshed";
            } else if (searching && (refreshesNearbyPursuit(observation)
                    || DragonInvestigation.refreshesAmbientSearch(activeObservation, observation, destination,
                    dragon.getTarget() != null || context.memories().has(DragonMemories.ATTACK_TARGET)))) {
                activeObservation = observation;
                outcome = "evidence-refreshed";
            } else {
                beginObservation(context, dragon, observation, profile);
            }
            if (activeObservation == null || phase == Phase.SKIPPED_RECENT) {
                return;
            }
        }

        LivingEntity source = resolveLivingSource(context);
        if (sourceBecameVisible(context, dragon, source)) {
            finish(context, dragon, Phase.COMPLETE, "source-visible", 0);
            return;
        }
        if (source != null && activeObservation.kind() == DragonSensoryObservation.Kind.PROJECTILE) {
            updateProjectileSourceDestination(context, dragon, source);
        }

        if (!isDestinationUsable(context, destination)) {
            finish(context, dragon, Phase.FAILED, "invalid-destination", FAILED_LOCATION_MEMORY_TICKS);
            return;
        }

        DragonAIMovementController movement = dragon.getAIMovement();
        if (issuedMovement && !movement.isMovementCommandCurrent(movementGeneration)) {
            issuedMovement = false;
            finish(context, dragon, Phase.SUPERSEDED, "movement-replaced", 0);
            return;
        }
        if (issuedMovement && movement.hasFailed()) {
            if (phase == Phase.SEARCHING && (airborneSearch || canSearchGround(dragon))) {
                stopOwnedMovement(dragon);
                searchWaypoint = null;
                nextMovementAttemptAt = context.gameTime() + MOVEMENT_RETRY_TICKS;
            } else {
                retryMovement(context, dragon, "path-failed");
                return;
            }
        }

        dragon.getLookControl().setLookAt(
                destination.x,
                destination.y,
                destination.z,
                10.0F,
                dragon.getMaxHeadXRot()
        );

        if (phase == Phase.SEARCHING) {
            tickSearch(context, dragon, profile);
            return;
        }

        double arrivalDistance = canSearchGround(dragon)
                ? GROUND_PURSUIT_ARRIVAL_DISTANCE : profile.arrivalDistance();
        boolean movementArrived = issuedMovement && movement.hasArrived();
        if (!movementArrived
                && dragon.position().distanceToSqr(destination) > arrivalDistance * arrivalDistance) {
            searchTicks = 0;
            if (!issuedMovement) {
                if (context.gameTime() < nextMovementAttemptAt) return;
                boolean accepted;
                if (airborneSearch) {
                    accepted = movement.setAsyncAirWaypoint(destination, Math.max(3.0D, pursuitSpeed));
                } else if (combatPursuit && dragon.isGroundedForAi() && !dragon.isInWaterOrBubble()) {
                    accepted = movement.moveToProgressiveGroundPosition(destination, pursuitSpeed, true,
                            GROUND_PURSUIT_ARRIVAL_DISTANCE);
                } else {
                    accepted = movement.setWaypoint(destination, pursuitSpeed, combatPursuit);
                }
                if (!accepted) {
                    retryMovement(context, dragon, "movement-rejected");
                    return;
                }
                movementGeneration = movement.getMovementCommandGeneration();
                issuedMovement = true;
                phase = Phase.TRAVELLING;
                outcome = "approaching";
            }
            return;
        }

        stopOwnedMovement(dragon);
        phase = Phase.SEARCHING;
        tickSearch(context, dragon, profile);
    }

    private void tickSearch(DragonBrainContext<T> context, RideableDragonBase dragon,
                            DragonPerceptionProfile profile) {
        outcome = "searching";
        if (airborneSearch) {
            tickAirSearch(context, dragon, profile);
            return;
        }
        if (canSearchGround(dragon)) {
            tickGroundSearch(context, dragon);
            return;
        }
        searchTicks++;
        double angle = Math.toRadians((context.gameTime() * 9L) % 360L);
        dragon.getLookControl().setLookAt(
                destination.x + Math.cos(angle) * 4.0D,
                destination.y + 1.0D,
                destination.z + Math.sin(angle) * 4.0D,
                12.0F,
                dragon.getMaxHeadXRot()
        );
        if (searchTicks >= profile.searchTicks()) {
            finish(context, dragon, Phase.COMPLETE, "searched", RECENT_LOCATION_MEMORY_TICKS);
        }
    }

    private void beginObservation(DragonBrainContext<T> context,
                                  RideableDragonBase dragon,
                                  DragonSensoryObservation observation,
                                  DragonPerceptionProfile profile) {
        stopOwnedMovement(dragon);
        activeObservation = observation;
        searchVisits.clear();
        LivingEntity source = resolveLivingSource(context);
        trackingProjectileSource = observation.kind() == DragonSensoryObservation.Kind.PROJECTILE
                && source != null;
        airborneSearch = !trackingProjectileSource
                && dragon instanceof RideableFlyingDragon flying
                && flying.isAerial() && !flying.onGround() && !flying.isInWaterOrBubble();
        searchWaypoint = null;
        searchWaypointTicks = 0;
        searchAngle = Math.atan2(dragon.getZ() - observation.position().z,
                dragon.getX() - observation.position().x);
        destination = trackingProjectileSource
                ? source.getBoundingBox().getCenter()
                : observation.position();
        nextSourceWaypointRefreshAt = context.gameTime();
        investigationKind = observation.kind().name().toLowerCase(Locale.ROOT);
        searchTicks = 0;
        phase = Phase.TRAVELLING;
        outcome = "new-observation";
        movementFailures = 0;
        nextMovementAttemptAt = 0;
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        combatPursuit = target != null && target.getUUID().equals(observation.sourceUuid());
        pursuitSpeed = profile.investigationSpeed();
        if (combatPursuit) {
            var rememberedWalk = context.memories().get(DragonMemories.LAST_SEEN_WALK_TARGET).orElse(null);
            pursuitSpeed = rememberedWalk == null ? Math.max(1.25D, pursuitSpeed) : rememberedWalk.getSpeedModifier();
            if (!airborneSearch && !trackingProjectileSource
                    && observation.kind() == DragonSensoryObservation.Kind.SIGHT && rememberedWalk != null) {
                destination = rememberedWalk.getTarget().currentPosition();
            }
        }
        groundSearchForward = destination.subtract(dragon.position()).multiply(1.0D, 0.0D, 1.0D).normalize();
        if (groundSearchForward.lengthSqr() < 1.0E-4D) {
            groundSearchForward = dragon.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        }
        if (groundSearchForward.lengthSqr() < 1.0E-4D) {
            groundSearchForward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        groundSearchAttempts = 0;

        double recentRadius = Math.max(2.0D, profile.arrivalDistance());
        if (!trackingProjectileSource
                && wasRecentlySearched(destination, context.gameTime(), observation.observedAt(), recentRadius * recentRadius)) {
            finish(context, dragon, Phase.SKIPPED_RECENT, "recent-location", 0);
        }
    }

    private boolean refreshesNearbyPursuit(DragonSensoryObservation observation) {
        // Footsteps near the remembered corner should update evidence without restarting the route or search clock.
        return combatPursuit && activeObservation != null && destination != null
                && activeObservation.sourceUuid() != null
                && activeObservation.sourceUuid().equals(observation.sourceUuid())
                && activeObservation.kind() != DragonSensoryObservation.Kind.PROJECTILE
                && observation.kind() != DragonSensoryObservation.Kind.PROJECTILE
                && observation.kind() != DragonSensoryObservation.Kind.SIGHT
                && observation.position().distanceToSqr(destination) <= 16.0D;
    }

    private boolean canSearchGround(RideableDragonBase dragon) {
        return combatPursuit && !trackingProjectileSource && !airborneSearch
                && dragon.isGroundedForAi() && !dragon.isInWaterOrBubble();
    }

    private void tickGroundSearch(DragonBrainContext<T> context, RideableDragonBase dragon) {
        outcome = "searching-ground";
        if (++searchTicks >= GROUND_PURSUIT_SEARCH_TICKS) {
            finish(context, dragon, Phase.COMPLETE, "searched", RECENT_LOCATION_MEMORY_TICKS);
            return;
        }
        DragonAIMovementController movement = dragon.getAIMovement();
        if (issuedMovement && (movement.hasArrived() || ++searchWaypointTicks >= GROUND_SEARCH_WAYPOINT_TICKS)) {
            stopOwnedMovement(dragon);
            searchWaypoint = null;
        }
        if (issuedMovement || groundSearchAttempts >= MAX_GROUND_SEARCH_WAYPOINTS
                || context.gameTime() < nextMovementAttemptAt) {
            return;
        }

        // Probe ahead and either side of the last known position; never query the hidden target's position.
        double radius = Math.min(8.0D, Math.max(4.0D, dragon.getBbWidth() * 1.25D));
        boolean spatialSearch = dragon instanceof RideableFlyingDragon flying && flying.getCombatDecisionSupport() != null;
        if (spatialSearch && activeObservation != null) {
            radius += Math.min(4.0D, Math.max(0, context.gameTime() - activeObservation.observedAt()) / 40.0D);
        }
        int attempt = groundSearchAttempts++;
        Vec3 side = new Vec3(-groundSearchForward.z, 0.0D, groundSearchForward.x);
        Vec3 candidate = destination.add(groundSearchForward.scale(radius * (attempt == 0 ? 1.0D : 0.5D)))
                .add(side.scale(attempt == 0 ? 0.0D : attempt == 1 ? radius : -radius));
        nextMovementAttemptAt = context.gameTime() + MOVEMENT_RETRY_TICKS;
        if (!isDestinationUsable(context, candidate)) return;
        Vec3 grounded;
        if (spatialSearch && dragon instanceof RideableFlyingDragon flying) {
            grounded = DragonCombatPositioning.searchPosition(flying, destination, candidate, option -> {
                if (!isDestinationUsable(context, option)) return null;
                Vec3 fit = movement.findGroundWaypointBelow(option);
                return isDestinationUsable(context, fit) && Math.abs(fit.y - destination.y) <= 3
                        && unvisitedSearchPosition(fit) ? fit : null;
            });
        } else {
            grounded = movement.findGroundWaypointBelow(candidate);
        }
        if (!isDestinationUsable(context, grounded)
                || Math.abs(grounded.y - destination.y) > 3.0D
                || !context.level().noCollision(dragon,
                dragon.getBoundingBox().move(grounded.subtract(dragon.position())).deflate(1.0E-3D))) {
            return;
        }
        if (movement.moveToProgressiveGroundPosition(grounded, pursuitSpeed, true,
                GROUND_PURSUIT_ARRIVAL_DISTANCE)) {
            searchWaypoint = grounded;
            rememberSearchVisit(grounded);
            movementGeneration = movement.getMovementCommandGeneration();
            issuedMovement = true;
            searchWaypointTicks = 0;
        }
    }

    private void retryMovement(DragonBrainContext<T> context, RideableDragonBase dragon, String reason) {
        stopOwnedMovement(dragon);
        nextMovementAttemptAt = context.gameTime() + MOVEMENT_RETRY_TICKS;
        if (++movementFailures >= MAX_MOVEMENT_FAILURES) {
            double nearbyDistance = Math.max(8.0D, dragon.getBbWidth() * 2.0D);
            if (canSearchGround(dragon) && destination != null
                    && dragon.position().distanceToSqr(destination) <= nearbyDistance * nearbyDistance) {
                // A target may have disappeared into a gap too small for this dragon. Search around it.
                phase = Phase.SEARCHING;
                searchTicks = 0;
                outcome = "blocked-approach:search-nearby";
                return;
            }
            finish(context, dragon, Phase.FAILED, reason, FAILED_LOCATION_MEMORY_TICKS);
            return;
        }
        phase = Phase.TRAVELLING;
        outcome = "retry:" + reason;
    }

    private void tickAirSearch(DragonBrainContext<T> context,
                               RideableDragonBase dragon,
                               DragonPerceptionProfile profile) {
        outcome = "searching-air";

        if (++searchTicks >= AIR_SEARCH_TICKS) {
            finish(context, dragon, Phase.COMPLETE, "searched", RECENT_LOCATION_MEMORY_TICKS);
            return;
        }
        DragonAIMovementController movement = dragon.getAIMovement();
        if (issuedMovement && (movement.hasArrived() || ++searchWaypointTicks >= AIR_SEARCH_WAYPOINT_TICKS)) {
            stopOwnedMovement(dragon);
            searchWaypoint = null;
        }
        if (issuedMovement || context.gameTime() < nextMovementAttemptAt) {
            return;
        }

        double radius = Math.max(6.0D, dragon.getBbWidth() * 1.5D);
        searchAngle += Math.PI / 2.0D;
        Vec3 candidate = destination.add(Math.cos(searchAngle) * radius, 2.0D,
                Math.sin(searchAngle) * radius);
        if (dragon instanceof RideableFlyingDragon flying && flying.getCombatDecisionSupport() != null) {
            nextMovementAttemptAt = context.gameTime() + MOVEMENT_RETRY_TICKS;
            candidate = DragonCombatPositioning.searchPosition(flying, destination, candidate, option -> {
                if (!isDestinationUsable(context, option)) return null;
                Vec3 fit = movement.flightSpace().fitDestination(option);
                return isDestinationUsable(context, fit) && unvisitedSearchPosition(fit) ? fit : null;
            });
        }
        if (!isDestinationUsable(context, candidate)
                || !context.level().noCollision(dragon,
                dragon.getBoundingBox().move(candidate.subtract(dragon.position())).deflate(1.0E-3D))) {
            return;
        }
        if (movement.setAsyncAirWaypoint(candidate, Math.max(1.5D, profile.investigationSpeed()))) {
            searchWaypoint = candidate;
            rememberSearchVisit(candidate);
            movementGeneration = movement.getMovementCommandGeneration();
            issuedMovement = true;
            searchWaypointTicks = 0;
        }
    }

    private boolean unvisitedSearchPosition(Vec3 position) {
        return searchVisits.stream().noneMatch(visited -> visited.distanceToSqr(position) < 9);
    }

    private void rememberSearchVisit(Vec3 position) {
        if (searchVisits.size() >= 8) searchVisits.removeFirst();
        searchVisits.addLast(position);
    }

    private void updateProjectileSourceDestination(DragonBrainContext<T> context,
                                                   RideableDragonBase dragon,
                                                   LivingEntity source) {
        DragonAIMovementController movement = dragon.getAIMovement();
        Vec3 sourcePosition = source.getBoundingBox().getCenter();
        boolean useAirDestination = prepareAirInvestigation(dragon, source);
        Vec3 desiredDestination = useAirDestination
                ? sourcePosition
                : movement.findGroundWaypointBelow(sourcePosition);
        if (desiredDestination == null) {
            desiredDestination = activeObservation.position();
        }

        double adjustmentSqr = destination == null
                ? Double.POSITIVE_INFINITY
                : destination.distanceToSqr(desiredDestination);
        boolean sourceMovedFar = adjustmentSqr >= SOURCE_WAYPOINT_REFRESH_DISTANCE_SQR;
        boolean periodicAdjustment = context.gameTime() >= nextSourceWaypointRefreshAt
                && adjustmentSqr >= SOURCE_WAYPOINT_MIN_ADJUSTMENT_SQR;
        if (sourceMovedFar || periodicAdjustment) {
            stopOwnedMovement(dragon);
            destination = desiredDestination;
            phase = Phase.TRAVELLING;
            outcome = useAirDestination ? "tracking-airborne-source" : "tracking-source";
            nextSourceWaypointRefreshAt = context.gameTime() + SOURCE_WAYPOINT_REFRESH_TICKS;
        }
    }

    private boolean isSameProjectileSource(DragonSensoryObservation current,
                                           DragonSensoryObservation candidate) {
        return current != null
                && current.kind() == DragonSensoryObservation.Kind.PROJECTILE
                && candidate.kind() == DragonSensoryObservation.Kind.PROJECTILE
                && current.sourceUuid() != null
                && current.sourceUuid().equals(candidate.sourceUuid());
    }

    private boolean prepareAirInvestigation(RideableDragonBase dragon, LivingEntity source) {
        if (!(dragon instanceof RideableFlyingDragon flyingDragon)
                || !(dragon instanceof DragonAirCombatSettingsProvider settingsProvider)) {
            return false;
        }
        DragonAirCombatSettings settings = settingsProvider.getAiAirCombatSettings();
        boolean sourceAirborne = DragonAirCombatHelper.isTargetAirborne(
                flyingDragon,
                source,
                settingsProvider.getAiTargetAirborneHeight(source)
        );
        if (!sourceAirborne) {
            return flyingDragon.isAerial() || flyingDragon.isTakeoff();
        }
        if (!flyingDragon.isAerial()
                && !flyingDragon.isTakeoff()
                && DragonAirCombatHelper.canTriggerAiFlightForTarget(
                flyingDragon,
                source,
                settings.takeoffTargetMinHeightAboveGround(),
                settings.takeoffTargetMinHeightAboveDragon()
        )) {
            stopOwnedMovement(flyingDragon);
            DragonAirCombatHelper.startOrResumeFlight(flyingDragon, settings.takeoffAnimationTicks());
            phase = Phase.TRAVELLING;
            outcome = "taking-off-for-source";
        }
        return flyingDragon.isAerial() || flyingDragon.isTakeoff();
    }

    private boolean isDestinationUsable(DragonBrainContext<T> context, Vec3 target) {
        if (target == null || !Double.isFinite(target.x) || !Double.isFinite(target.y) || !Double.isFinite(target.z)) {
            return false;
        }
        BlockPos blockPos = BlockPos.containing(target.x, target.y, target.z);
        return !context.level().isOutsideBuildHeight(blockPos)
                && context.level().getWorldBorder().isWithinBounds(blockPos)
                && context.level().hasChunkAt(blockPos);
    }

    private LivingEntity resolveLivingSource(DragonBrainContext<T> context) {
        if (activeObservation == null || activeObservation.sourceUuid() == null) {
            return null;
        }
        Entity source = context.level().getEntity(activeObservation.sourceUuid());
        return source instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private boolean sourceBecameVisible(DragonBrainContext<T> context,
                                        RideableDragonBase dragon,
                                        LivingEntity source) {
        if (source == null) {
            return false;
        }
        if (DragonInvestigation.isVisibleAmbientSource(dragon, source, activeObservation.kind())) {
            return true;
        }
        if (airborneSearch) {
            return context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null) == source
                    && context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false);
        }
        if (activeObservation.kind() == DragonSensoryObservation.Kind.PROJECTILE
                && DragonAwarenessMemory.get(dragon).isProjectileThreat(
                activeObservation.sourceUuid(),
                context.gameTime()
        )) {
            return false;
        }
        return dragon.hasLineOfSight(source);
    }

    private void finish(DragonBrainContext<T> context,
                        RideableDragonBase dragon,
                        Phase finalPhase,
                        String finalOutcome,
                        int rememberTicks) {
        stopOwnedMovement(dragon);
        if (rememberTicks > 0 && destination != null) {
            rememberLocation(destination, context.gameTime() + rememberTicks);
        }
        clearOwnedInvestigationMemories(context);
        phase = finalPhase;
        outcome = finalOutcome;
        dragon.combatManager.recordAiDecision("investigation", finalOutcome);
        if (finalPhase == Phase.FAILED || finalPhase == Phase.SKIPPED_RECENT
                || (finalPhase == Phase.COMPLETE && "searched".equals(finalOutcome))) {
            landAfterAirSearch(context, dragon);
        }
    }

    private void landAfterAirSearch(DragonBrainContext<T> context, RideableDragonBase dragon) {
        if (!airborneSearch || activeObservation == null
                || !(dragon instanceof RideableFlyingDragon flying)
                || !(dragon instanceof DragonAirCombatSettingsProvider settings)
                || !flying.isAerial() || flying.onGround()
                || dragon.isVehicle() || dragon.isPassenger() || dragon.isOrderedToSit()
                || dragon.isSleepLocked() || dragon.isDying()
                || context.memories().has(DragonMemories.RESCUE_TARGET)
                || context.memories().has(DragonMemories.INVESTIGATION_TARGET)
                || context.memories().has(DragonMemories.LAST_SEEN_TARGET)
                || context.memories().has(DragonMemories.HEARD_TARGET)
                || context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false)) {
            return;
        }
        LivingEntity target = dragon.getTarget();
        LivingEntity rememberedTarget = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if ((target != null && !target.getUUID().equals(activeObservation.sourceUuid()))
                || (rememberedTarget != null && !rememberedTarget.getUUID().equals(activeObservation.sourceUuid()))) {
            return;
        }
        DragonTargetLifecycle.clearCombatTarget(context.memories(), context.dragon(), false);
        dragon.getAIMovement().requestGroundTransition((LivingEntity) null,
                settings.getAiAirCombatSettings().landingSpeed());
    }

    private void stopOwnedMovement(RideableDragonBase dragon) {
        if (issuedMovement) {
            dragon.getAIMovement().stopIfMovementCommandCurrent(movementGeneration);
        }
        issuedMovement = false;
        movementGeneration = Long.MIN_VALUE;
    }

    private void clearOwnedInvestigationMemories(DragonBrainContext<T> context) {
        if (activeObservation == null) {
            return;
        }
        context.memories().get(DragonMemories.INVESTIGATION_TARGET)
                .filter(activeObservation::equals)
                .ifPresent(ignored -> context.memories().erase(DragonMemories.INVESTIGATION_TARGET));
        eraseEvidenceIfConsumed(context, DragonMemories.LAST_SEEN_TARGET);
        eraseEvidenceIfConsumed(context, DragonMemories.HEARD_TARGET);
    }

    private void eraseEvidenceIfConsumed(DragonBrainContext<T> context,
                                         MemoryModuleType<DragonSensoryObservation> memoryType) {
        context.memories().get(memoryType)
                .filter(observation -> observation.observedAt() <= activeObservation.observedAt())
                .filter(observation -> Objects.equals(observation.sourceUuid(), activeObservation.sourceUuid()))
                .ifPresent(ignored -> context.memories().erase(memoryType));
    }

    private boolean wasRecentlySearched(Vec3 target, long gameTime, long observedAt, double distanceSqr) {
        pruneRecentLocations(gameTime);
        boolean freshSighting = combatPursuit && activeObservation.kind() == DragonSensoryObservation.Kind.SIGHT;
        return recentLocations.stream()
                .anyMatch(location -> (!freshSighting || observedAt <= location.observedAt())
                        && location.position().distanceToSqr(target) <= distanceSqr);
    }

    private void rememberLocation(Vec3 target, long expiresAt) {
        recentLocations.removeIf(location -> location.position().distanceToSqr(target)
                <= DESTINATION_REFRESH_DISTANCE_SQR);
        recentLocations.addLast(new RecentLocation(target, activeObservation.observedAt(), expiresAt));
        while (recentLocations.size() > MAX_RECENT_LOCATIONS) {
            recentLocations.removeFirst();
        }
    }

    private void pruneRecentLocations(long gameTime) {
        recentLocations.removeIf(location -> location.expiresAt() <= gameTime);
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        searchVisits.clear();
        boolean expired = activeObservation != null
                && (phase == Phase.TRAVELLING || phase == Phase.SEARCHING)
                && !context.memories().has(DragonMemories.INVESTIGATION_TARGET)
                && !context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false)
                && (!issuedMovement || (context.dragon() instanceof RideableDragonBase dragon
                && dragon.getAIMovement().isMovementCommandCurrent(movementGeneration)));
        if (context.dragon() instanceof RideableDragonBase dragon) {
            stopOwnedMovement(dragon);
        }
        if (activeObservation != null && (phase == Phase.TRAVELLING || phase == Phase.SEARCHING)) {
            clearOwnedInvestigationMemories(context);
            phase = Phase.CANCELLED;
            outcome = interruptionReason(context);
        }
        if (expired && context.dragon() instanceof RideableDragonBase dragon) {
            landAfterAirSearch(context, dragon);
            outcome = "memory-expired";
        }
        destination = null;
        actionPaused = false;
        context.dragon().combatManager.recordAiDecision("investigation", "stopped:" + outcome);
        activeObservation = null;
        nextSourceWaypointRefreshAt = 0L;
        trackingProjectileSource = false;
        airborneSearch = false;
        searchWaypoint = null;
        searchWaypointTicks = 0;
        searchTicks = 0;
    }

    private String interruptionReason(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (!dragon.isAlive() || dragon.isDying()) return "dying";
        if (dragon.isVehicle() || dragon.isPassenger()) return "rider-control";
        if (dragon.isOrderedToSit()) return "sit-command";
        if (dragon.isSleepLocked()) return "sleep";
        if ((dragon.isInLove() || context.memories().has(DragonMemories.BREED_TARGET))
                && !context.memories().has(DragonMemories.ATTACK_TARGET)) return "breeding";
        if (dragon instanceof RideableDragonBase rideable
                && DragonFollowOwnerBehaviour.hasOwnerFollowPriority(rideable)) return "owner-follow";
        if (context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false)) return "target-visible";
        if (activity() != null && !dragon.getBrain().getActiveActivities().contains(activity())) return "activity-changed";
        return "state-changed";
    }

    private boolean canInvestigate(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (!(dragon instanceof RideableDragonBase rideable)
                || DragonFollowOwnerBehaviour.hasOwnerFollowPriority(rideable)) {
            return false;
        }
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        boolean targetVisible = target != null
                && context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false);
        return (target == null || target.isAlive())
                && (target != null || !dragon.isInLove() && !context.memories().has(DragonMemories.BREED_TARGET))
                && !targetVisible
                && context.memories().has(DragonMemories.INVESTIGATION_TARGET)
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSleepLocked()
                && !dragon.isDying();
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("phase", phase.name().toLowerCase(Locale.ROOT));
        details.put("outcome", outcome);
        details.put("action_paused", Boolean.toString(actionPaused));
        details.put("destination", destination == null ? "none" : destination.toString());
        details.put("kind", investigationKind);
        details.put("search_ticks", Integer.toString(searchTicks));
        details.put("movement_owned", Boolean.toString(issuedMovement));
        details.put("tracking_source", Boolean.toString(trackingProjectileSource));
        details.put("airborne_search", Boolean.toString(airborneSearch));
        details.put("search_waypoint", searchWaypoint == null ? "none" : searchWaypoint.toString());
        details.put("ground_search_attempts", Integer.toString(groundSearchAttempts));
        details.put("recent_locations", Integer.toString(recentLocations.size()));
        details.put("combat_pursuit", Boolean.toString(combatPursuit));
        details.put("movement_failures", Integer.toString(movementFailures));
        return Map.copyOf(details);
    }

    private enum Phase {
        IDLE,
        TRAVELLING,
        SEARCHING,
        COMPLETE,
        FAILED,
        SUPERSEDED,
        SKIPPED_RECENT,
        CANCELLED
    }

    private record RecentLocation(Vec3 position, long observedAt, long expiresAt) {
    }
}
