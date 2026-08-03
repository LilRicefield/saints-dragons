package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonPerceptionProfile;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonSensoryObservation;
import com.leon.saintsdragons.server.ai.navigation.DragonAIMovementController;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DragonInvestigateTargetBehaviour<T extends DragonEntity> extends DragonBehaviour<T> {
    private static final int RECENT_LOCATION_MEMORY_TICKS = 20 * 30;
    private static final int FAILED_LOCATION_MEMORY_TICKS = 20 * 5;
    private static final int MAX_RECENT_LOCATIONS = 4;
    private static final double DESTINATION_REFRESH_DISTANCE_SQR = 1.0D;

    private final Deque<RecentLocation> recentLocations = new ArrayDeque<>();
    private int searchTicks;
    private boolean issuedMovement;
    private long movementGeneration = Long.MIN_VALUE;
    private Vec3 destination;
    private DragonSensoryObservation activeObservation;
    private String investigationKind = "none";
    private Phase phase = Phase.IDLE;
    private String outcome = "none";

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return canInvestigate(context);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return canInvestigate(context);
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        searchTicks = 0;
        issuedMovement = false;
        movementGeneration = Long.MIN_VALUE;
        destination = null;
        activeObservation = null;
        investigationKind = "none";
        phase = Phase.IDLE;
        outcome = "none";
        pruneRecentLocations(context.gameTime());
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        if (!(context.dragon() instanceof RideableDragonBase dragon)) {
            return;
        }
        DragonSensoryObservation observation = context.memories()
                .get(DragonMemories.INVESTIGATION_TARGET)
                .orElse(null);
        if (observation == null) {
            return;
        }

        DragonPerceptionProfile profile = DragonPerceptionProfile.forDragon(dragon);
        if (!observation.equals(activeObservation)) {
            beginObservation(context, dragon, observation, profile);
            if (activeObservation == null) {
                return;
            }
        }

        if (!isDestinationUsable(context, destination)) {
            finish(context, dragon, Phase.FAILED, "invalid-destination", FAILED_LOCATION_MEMORY_TICKS);
            return;
        }
        if (sourceBecameVisible(context, dragon)) {
            finish(context, dragon, Phase.COMPLETE, "source-visible", 0);
            return;
        }

        DragonAIMovementController movement = dragon.getAIMovement();
        if (issuedMovement && !movement.isMovementCommandCurrent(movementGeneration)) {
            issuedMovement = false;
            finish(context, dragon, Phase.SUPERSEDED, "movement-replaced", 0);
            return;
        }
        if (issuedMovement && movement.hasFailed()) {
            finish(context, dragon, Phase.FAILED, "path-failed", FAILED_LOCATION_MEMORY_TICKS);
            return;
        }

        dragon.getLookControl().setLookAt(
                destination.x,
                destination.y,
                destination.z,
                10.0F,
                dragon.getMaxHeadXRot()
        );

        double arrivalDistance = profile.arrivalDistance();
        boolean movementArrived = issuedMovement && movement.hasArrived();
        if (!movementArrived
                && dragon.position().distanceToSqr(destination) > arrivalDistance * arrivalDistance) {
            searchTicks = 0;
            if (!issuedMovement) {
                if (!movement.setWaypoint(destination, profile.investigationSpeed())) {
                    finish(context, dragon, Phase.FAILED, "movement-rejected", FAILED_LOCATION_MEMORY_TICKS);
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
        outcome = "searching";
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
        destination = observation.position();
        investigationKind = observation.kind().name().toLowerCase(java.util.Locale.ROOT);
        searchTicks = 0;
        phase = Phase.TRAVELLING;
        outcome = "new-observation";

        double recentRadius = Math.max(2.0D, profile.arrivalDistance());
        if (wasRecentlySearched(destination, context.gameTime(), recentRadius * recentRadius)) {
            finish(context, dragon, Phase.SKIPPED_RECENT, "recent-location", 0);
        }
    }

    private boolean isDestinationUsable(DragonBrainContext<T> context, Vec3 target) {
        if (target == null || !Double.isFinite(target.x) || !Double.isFinite(target.y) || !Double.isFinite(target.z)) {
            return false;
        }
        BlockPos blockPos = BlockPos.containing(target.x, target.y, target.z);
        return context.level().getWorldBorder().isWithinBounds(blockPos)
                && context.level().hasChunkAt(blockPos);
    }

    private boolean sourceBecameVisible(DragonBrainContext<T> context, RideableDragonBase dragon) {
        if (activeObservation == null || activeObservation.sourceUuid() == null) {
            return false;
        }
        Entity source = context.level().getEntity(activeObservation.sourceUuid());
        return source instanceof LivingEntity living
                && living.isAlive()
                && dragon.hasLineOfSight(living);
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

    private boolean wasRecentlySearched(Vec3 target, long gameTime, double distanceSqr) {
        pruneRecentLocations(gameTime);
        return recentLocations.stream()
                .anyMatch(location -> location.position().distanceToSqr(target) <= distanceSqr);
    }

    private void rememberLocation(Vec3 target, long expiresAt) {
        recentLocations.removeIf(location -> location.position().distanceToSqr(target)
                <= DESTINATION_REFRESH_DISTANCE_SQR);
        recentLocations.addLast(new RecentLocation(target, expiresAt));
        while (recentLocations.size() > MAX_RECENT_LOCATIONS) {
            recentLocations.removeFirst();
        }
    }

    private void pruneRecentLocations(long gameTime) {
        recentLocations.removeIf(location -> location.expiresAt() <= gameTime);
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        if (context.dragon() instanceof RideableDragonBase dragon) {
            stopOwnedMovement(dragon);
        }
        if (activeObservation != null && (phase == Phase.TRAVELLING || phase == Phase.SEARCHING)) {
            clearOwnedInvestigationMemories(context);
            phase = Phase.CANCELLED;
            outcome = "state-changed";
        }
        destination = null;
        activeObservation = null;
        searchTicks = 0;
    }

    private boolean canInvestigate(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        boolean targetVisible = target != null
                && context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false);
        return dragon instanceof RideableDragonBase
                && (target == null || target.isAlive())
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
        details.put("phase", phase.name().toLowerCase(java.util.Locale.ROOT));
        details.put("outcome", outcome);
        details.put("destination", destination == null ? "none" : destination.toString());
        details.put("kind", investigationKind);
        details.put("search_ticks", Integer.toString(searchTicks));
        details.put("movement_owned", Boolean.toString(issuedMovement));
        details.put("recent_locations", Integer.toString(recentLocations.size()));
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

    private record RecentLocation(Vec3 position, long expiresAt) {
    }
}
