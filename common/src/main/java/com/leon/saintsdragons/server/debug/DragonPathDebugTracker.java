package com.leon.saintsdragons.server.debug;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.network.MessageDragonPathDebug;
import com.leon.saintsdragons.common.network.MessageDragonBrainDebug;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.server.ai.navigation.DragonAIMovementController;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonOwnerFollowTarget;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonDrinkBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonRescueFallingOwnerBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonTargetingBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.FirstApplicableDragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ReturnToRoostBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.debug.DragonBrainDiagnostics;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonSensoryObservation;
import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonTacticalCommitment;
import com.leon.saintsdragons.server.ai.navigation.PathNavigateGround;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncFlightController;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.ai.pathfinding.DragonPathSearchDebug;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class DragonPathDebugTracker {
    private static final int SNAPSHOT_INTERVAL_TICKS = 4;
    private static final int MAX_SYNC_NODES = 512;
    private static final int HISTORY_NODES = 32;

    private static final Map<UUID, TrackingEntry> TRACKED_DRAGONS = new HashMap<>();

    private DragonPathDebugTracker() {
    }

    public static void toggle(ServerPlayer player, DragonEntity dragon) {
        if (!player.canUseGameMasterBlocks()) {
            return;
        }

        TrackingEntry current = TRACKED_DRAGONS.get(player.getUUID());
        if (current != null && current.dragonId.equals(dragon.getUUID())) {
            TRACKED_DRAGONS.remove(player.getUUID());
            refreshActiveSearchDebug();
            NetworkHandler.sendToPlayer(player, MessageDragonPathDebug.clear());
            NetworkHandler.sendToPlayer(player, MessageDragonBrainDebug.clear());
            player.displayClientMessage(Component.literal("Dragon debug: OFF"), true);
            SaintsDragonsCommon.LOGGER.info(
                    "[Dragon Path Debug] event=unselected player={} id={} uuid={}",
                    player.getGameProfile().getName(),
                    dragon.getId(),
                    dragon.getUUID()
            );
            return;
        }

        TRACKED_DRAGONS.put(player.getUUID(), new TrackingEntry(dragon.getUUID()));
        refreshActiveSearchDebug();
        player.displayClientMessage(
                Component.literal("Dragon debug: ").append(dragon.getDisplayName()),
                true
        );
        SaintsDragonsCommon.LOGGER.info(
                "[Dragon Path Debug] event=selected player={} id={} uuid={} type={} pos={}",
                player.getGameProfile().getName(),
                dragon.getId(),
                dragon.getUUID(),
                dragon.getType(),
                dragon.blockPosition()
        );
        sendSnapshot(player, dragon, TRACKED_DRAGONS.get(player.getUUID()), true);
    }

    public static void tick(MinecraftServer server) {
        if (TRACKED_DRAGONS.isEmpty() || server.getTickCount() % SNAPSHOT_INTERVAL_TICKS != 0) {
            return;
        }

        boolean trackingChanged = false;
        Iterator<Map.Entry<UUID, TrackingEntry>> iterator = TRACKED_DRAGONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TrackingEntry> tracked = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(tracked.getKey());
            if (player == null) {
                iterator.remove();
                trackingChanged = true;
                continue;
            }

            Entity entity = player.serverLevel().getEntity(tracked.getValue().dragonId);
            if (!(entity instanceof DragonEntity dragon) || dragon.isRemoved() || !dragon.isAlive()) {
                NetworkHandler.sendToPlayer(player, MessageDragonPathDebug.clear());
                NetworkHandler.sendToPlayer(player, MessageDragonBrainDebug.clear());
                player.displayClientMessage(Component.literal("Dragon debug: target unavailable"), true);
                iterator.remove();
                trackingChanged = true;
                continue;
            }

            sendSnapshot(player, dragon, tracked.getValue(), false);
        }
        if (trackingChanged) {
            refreshActiveSearchDebug();
        }
    }

    public static void clear(ServerPlayer player) {
        if (player != null) {
            TRACKED_DRAGONS.remove(player.getUUID());
            refreshActiveSearchDebug();
        }
    }

    public static void clearAll() {
        TRACKED_DRAGONS.clear();
        DragonPathSearchDebug.setActiveDragons(List.of());
    }

    private static void sendSnapshot(ServerPlayer player,
                                     DragonEntity dragon,
                                     TrackingEntry tracking,
                                     boolean forceLog) {
        MessageDragonPathDebug snapshot = capture(dragon);
        NetworkHandler.sendToPlayer(player, snapshot);
        NetworkHandler.sendToPlayer(player, DragonBrainDebugTracker.capture(dragon));

        LogState logState = LogState.capture(dragon, snapshot);
        if (!forceLog && logState.equals(tracking.lastLogState)) {
            return;
        }
        tracking.lastLogState = logState;

        SaintsDragonsCommon.LOGGER.info(
                "[Dragon Path Debug] event=state player={} id={} pos={} locomotion={} movement={} "
                        + "navigation={}/{} shown={} swim={}/{} shown={} calculating={} moving={} "
                        + "stuckTicks={} retries={} movementTarget={} swimTarget={} swimEndpoint={} "
                        + "rejectedTarget={} combatTarget={} hunger={}/{} huntFood={} sleep={} roost={} drinking={} rescue={} ownerFollow={} wildAggressive={} "
                        + "onGround={} horizontalCollision={} verticalCollision={} fallDistance={} velocity={} groundNav={} groundPath={} "
                        + "navigationDone={} navigationStuck={} "
                        + "search={}#{} reached={} closed={} open={} candidates={} searchMicros={} "
                        + "perception={} tactical={} pursuit={} coordination={} activity={} behaviours={}",
                player.getGameProfile().getName(),
                dragon.getId(),
                dragon.blockPosition(),
                snapshot.locomotionMode(),
                snapshot.movementMode(),
                snapshot.navigationNextIndex(),
                snapshot.navigationNodeCount(),
                snapshot.navigationNodes().size(),
                snapshot.swimNextIndex(),
                snapshot.swimNodeCount(),
                snapshot.swimNodes().size(),
                snapshot.swimCalculating(),
                snapshot.swimMoving(),
                snapshot.swimStuckTicks(),
                snapshot.swimRetries(),
                blockPosition(snapshot.movementTarget()),
                blockPosition(snapshot.swimTarget()),
                blockPosition(snapshot.swimEndpoint()),
                blockPosition(snapshot.rejectedTarget()),
                blockPosition(snapshot.combatTarget()),
                dragon.getHunger(),
                DragonEntity.HUNGER_MAX,
                dragon.isHuntFoodPursuitActive(),
                logState.sleep,
                logState.roost,
                logState.drinking,
                logState.rescue,
                logState.ownerFollow,
                dragon.isWildAggressionEnabled(),
                dragon.onGround(),
                dragon.horizontalCollision,
                dragon.verticalCollision,
                dragon.fallDistance,
                dragon.getDeltaMovement(),
                groundNavigationSummary(dragon),
                dragon instanceof RideableDragonBase rideable
                        ? rideable.getAIMovement().getDebugGroundPathDetails()
                        : "unsupported",
                dragon.getNavigation().isDone(),
                dragon.getNavigation().isStuck(),
                snapshot.searchType(),
                snapshot.searchId(),
                snapshot.searchReached(),
                snapshot.searchClosedNodeCount(),
                snapshot.searchOpenNodeCount(),
                snapshot.searchCandidateNodeCount(),
                snapshot.searchDurationMicros(),
                logState.perception,
                logState.tactical,
                logState.pursuit,
                logState.coordination,
                logState.activity,
                logState.behaviours
        );
    }

    private static MessageDragonPathDebug capture(DragonEntity dragon) {
        Path navigationPath = dragon.getNavigation().getPath();
        PathSlice navigation = sliceNavigationPath(dragon, navigationPath);

        @Nullable AsyncFlightController.DebugSnapshot flight = null;
        if (dragon instanceof RideableFlyingDragon flyingDragon) {
            flight = flyingDragon.getAiFlightDebugSnapshot();
            if (!flight.pathNodes().isEmpty()) {
                navigation = slicePositions(flight.pathNodes(), flight.pathIndex());
            }
        }

        AsyncSwimController.DebugSnapshot swim = dragon.getAiSwimController().getDebugSnapshot();
        PathSlice swimPath = slicePositions(swim.pathNodes(), swim.pathIndex());
        @Nullable DragonPathSearchDebug.Snapshot search = DragonPathSearchDebug.getSnapshot(dragon.getUUID());

        String movementMode = "NONE";
        @Nullable Vec3 movementTarget = null;
        if (dragon instanceof RideableDragonBase rideable) {
            DragonAIMovementController movement = rideable.getAIMovement();
            movementMode = movement.getDebugMovementMode() + "/" + movement.getDebugGroundPathState();
            movementTarget = movement.getDebugMovementTarget();
        }
        if (flight != null) {
            movementMode = movementMode + "/AIR_" + flight.state().name();
            if (movementTarget == null) {
                movementTarget = flight.waypoint();
            }
        }
        if (movementTarget == null && navigationPath != null && navigationPath.getTarget() != null) {
            movementTarget = Vec3.atCenterOf(navigationPath.getTarget());
        }

        LivingEntity combatTarget = dragon.getTarget();
        return new MessageDragonPathDebug(
                true,
                dragon.getId(),
                dragon.getLocomotionMode().name(),
                movementMode,
                navigation.nodes,
                navigation.firstIndex,
                navigation.nextIndex,
                navigation.totalNodes,
                swimPath.nodes,
                swimPath.firstIndex,
                swimPath.nextIndex,
                swimPath.totalNodes,
                search == null ? "NONE" : search.type().name(),
                search == null ? 0L : search.searchId(),
                search == null ? List.of() : search.closedNodes(),
                search == null ? 0 : search.closedNodeCount(),
                search == null ? List.of() : search.openNodes(),
                search == null ? 0 : search.openNodeCount(),
                search == null ? List.of() : search.candidateNodes(),
                search == null ? 0 : search.candidateNodeCount(),
                search == null ? null : search.start(),
                search == null ? null : search.target(),
                search != null && search.reached(),
                search == null ? 0L : search.durationMicros(),
                movementTarget,
                swim.target(),
                swim.endpoint(),
                swim.rejectedTarget(),
                combatTarget == null ? null : combatTarget.getBoundingBox().getCenter(),
                swim.calculating(),
                swim.moving(),
                swim.stuckTicks(),
                swim.retries()
        );
    }

    private static PathSlice sliceNavigationPath(DragonEntity dragon, @Nullable Path path) {
        if (path == null || path.getNodeCount() == 0) {
            return PathSlice.EMPTY;
        }

        int total = path.getNodeCount();
        int next = Mth.clamp(path.getNextNodeIndex(), 0, total);
        int first = Math.max(0, next - HISTORY_NODES);
        int end = Math.min(total, first + MAX_SYNC_NODES);
        List<Vec3> positions = new ArrayList<>(end - first);
        for (int i = first; i < end; i++) {
            positions.add(path.getEntityPosAtNode(dragon, i));
        }
        return new PathSlice(positions, first, next, total);
    }

    private static PathSlice slicePositions(List<Vec3> positions, int requestedNextIndex) {
        if (positions.isEmpty()) {
            return PathSlice.EMPTY;
        }

        int total = positions.size();
        int next = Mth.clamp(requestedNextIndex, 0, total);
        int first = Math.max(0, next - HISTORY_NODES);
        int end = Math.min(total, first + MAX_SYNC_NODES);
        return new PathSlice(List.copyOf(positions.subList(first, end)), first, next, total);
    }

    private static @Nullable BlockPos blockPosition(@Nullable Vec3 position) {
        return position == null ? null : BlockPos.containing(position);
    }

    private static String groundNavigationSummary(DragonEntity dragon) {
        Path path = dragon.getNavigation().getPath();
        String nodeSummary = "none";
        if (path != null && path.getNextNodeIndex() < path.getNodeCount()) {
            int index = path.getNextNodeIndex();
            var node = path.getNode(index);
            Vec3 waypoint = path.getEntityPosAtNode(dragon, index);
            Vec3 delta = waypoint.subtract(dragon.position());
            nodeSummary = index + "@" + node.x + "," + node.y + "," + node.z
                    + " waypoint=" + vectorSummary(waypoint)
                    + " delta=" + vectorSummary(delta);
        }

        String navigationState = "gate=unknown,skipped=0,stall=0,issued=unknown";
        if (dragon.getNavigation() instanceof PathNavigateGround groundNavigation) {
            PathNavigateGround.DebugSnapshot debug = groundNavigation.getDebugSnapshot();
            String issued = debug.moveCommandIssued()
                    ? vectorSummary(debug.moveTarget()) + "@" + decimalSummary(debug.moveSpeed())
                    : "none";
            String finalTolerance = Float.isFinite(debug.finalWaypointTolerance())
                    ? decimalSummary(debug.finalWaypointTolerance())
                    : "default";
            String ascentAssist = debug.blockedAscentJumpCount() > 0
                    ? debug.blockedAscentJumpCount() + "@" + debug.lastBlockedAscentNode()
                            + "/cd" + debug.blockedAscentJumpCooldown()
                    : "none";
            navigationState = "active=" + debug.pathActive()
                    + ",gate=" + (debug.canUpdatePath() ? "open" : "blocked")
                    + ",skipped=" + debug.skippedFollowTicks()
                    + ",stall=" + debug.nodeStallTicks()
                    + ",issued=" + issued
                    + ",finalTol=" + finalTolerance
                    + ",ascentAssist=" + ascentAssist;
        }

        AABB bounds = dragon.getBoundingBox();
        double inset = Math.min(0.05D, Math.max(0.0D, dragon.getBbWidth() * 0.1D));
        AABB supportProbe = new AABB(
                bounds.minX + inset,
                bounds.minY - 0.08D,
                bounds.minZ + inset,
                bounds.maxX - inset,
                bounds.minY + 0.01D,
                bounds.maxZ - inset
        );
        boolean hasSupport = !dragon.level().noCollision(dragon, supportProbe);
        boolean bodyClear = dragon.level().noCollision(dragon, bounds.deflate(0.01D));
        return navigationState
                + ",node=" + nodeSummary
                + ",support=" + hasSupport
                + ",bodyClear=" + bodyClear
                + ",noPhysics=" + dragon.noPhysics;
    }

    private static String vectorSummary(@Nullable Vec3 vector) {
        if (vector == null) {
            return "none";
        }
        return String.format(Locale.ROOT, "(%.2f,%.2f,%.2f)", vector.x, vector.y, vector.z);
    }

    private static String decimalSummary(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static List<String> runningBehaviours(DragonEntity dragon) {
        List<String> names = new ArrayList<>();
        for (BehaviorControl<?> behaviour : dragon.getBrain().getRunningBehaviors()) {
            names.add(behaviour.getClass().getSimpleName());
            if (behaviour instanceof FirstApplicableDragonBehaviour<?> firstApplicable
                    && firstApplicable.runningBehaviour() != null) {
                names.add(firstApplicable.runningBehaviour().getClass().getSimpleName());
            }
        }
        names.sort(String::compareTo);
        return List.copyOf(names);
    }

    private static String drinkingSummary(DragonEntity dragon) {
        for (DragonBrainDiagnostics.RegisteredBehaviour registered
                : DragonBrainDiagnostics.getBehaviours(dragon, dragon.getBrain())) {
            if (!(registered.behaviour() instanceof FirstApplicableDragonBehaviour<?> firstApplicable)) {
                continue;
            }
            for (var child : firstApplicable.childBehaviours()) {
                if (child instanceof DragonDrinkBehaviour<?> drinking) {
                    Map<String, String> details = drinking.getDragonBrainDebugDetails();
                    boolean drinkReady = drinking.cooldownRemaining(dragon.level().getGameTime()) == 0L;
                    return "phase=" + details.getOrDefault("drink_phase", "unknown")
                            + ",decision=" + details.getOrDefault("drink_decision", "unknown")
                            + ",site=" + details.getOrDefault("drink_site", "none")
                            + ",candidates=" + details.getOrDefault("drink_candidates", "0/0")
                            + ",water=" + details.getOrDefault("drink_water_sources", "0")
                            + ",valid=" + details.getOrDefault("drink_valid_sites", "0")
                            + ",cooldown=" + (drinkReady ? "ready" : "waiting");
                }
            }
        }
        return "disabled";
    }

    private static String rescueSummary(DragonEntity dragon) {
        if (!(dragon instanceof RideableFlyingDragon flyingDragon)) {
            return "disabled";
        }
        for (DragonBrainDiagnostics.RegisteredBehaviour registered
                : DragonBrainDiagnostics.getBehaviours(dragon, dragon.getBrain())) {
            if (registered.behaviour() instanceof DragonRescueFallingOwnerBehaviour<?> rescue) {
                return rescue.getRescueDebugSummary(flyingDragon);
            }
        }
        return "disabled";
    }

    private static String perceptionSummary(DragonEntity dragon) {
        if (!dragon.getBrain().checkMemory(DragonMemories.TARGET_VISIBLE, MemoryStatus.REGISTERED)) {
            return "disabled";
        }
        String visible = dragon.getBrain().getMemory(DragonMemories.TARGET_VISIBLE)
                .map(Object::toString)
                .orElse("none");
        String lastSeen = dragon.getBrain().getMemory(DragonMemories.LAST_SEEN_TARGET)
                .map(observation -> observationSummary(dragon, observation))
                .orElse("none");
        String investigation = dragon.getBrain().getMemory(DragonMemories.INVESTIGATION_TARGET)
                .map(observation -> observationSummary(dragon, observation))
                .orElse("none");
        String heard = dragon.getBrain().getMemory(DragonMemories.HEARD_STIMULUS)
                .map(observation -> observationSummary(dragon, observation))
                .orElse("none");
        String heardTarget = dragon.getBrain().getMemory(DragonMemories.HEARD_TARGET)
                .map(observation -> observationSummary(dragon, observation))
                .orElse("none");
        String wakeTarget = dragon.getBrain().getMemory(DragonMemories.WAKE_TARGET)
                .map(target -> target.getId() + "@" + target.blockPosition().toShortString())
                .orElse("none");
        return "visible=" + visible + ",last=" + lastSeen + ",investigate=" + investigation
                + ",heard=" + heard + ",targetHeard=" + heardTarget
                + ",wakeTarget=" + wakeTarget;
    }

    private static String observationSummary(DragonEntity dragon,
                                             DragonSensoryObservation observation) {
        return observation.kind().name()
                + "@" + BlockPos.containing(observation.position()).toShortString()
                + "(" + String.format(java.util.Locale.ROOT, "%.2f", observation.confidence())
                + ",age=" + Math.max(0L, dragon.level().getGameTime() - observation.observedAt())
                + "t)";
    }

    private static void refreshActiveSearchDebug() {
        HashSet<UUID> dragonIds = new HashSet<>();
        for (TrackingEntry entry : TRACKED_DRAGONS.values()) {
            dragonIds.add(entry.dragonId);
        }
        DragonPathSearchDebug.setActiveDragons(dragonIds);
    }

    private static final class TrackingEntry {
        private final UUID dragonId;
        private @Nullable LogState lastLogState;

        private TrackingEntry(UUID dragonId) {
            this.dragonId = dragonId;
        }
    }

    private record PathSlice(List<Vec3> nodes, int firstIndex, int nextIndex, int totalNodes) {
        private static final PathSlice EMPTY = new PathSlice(List.of(), 0, 0, 0);
    }

    private record LogState(String locomotionMode,
                            String movementMode,
                            int navigationNextIndex,
                            int navigationNodeCount,
                            int navigationHash,
                            int swimNextIndex,
                            int swimNodeCount,
                            int swimHash,
                            long searchId,
                            boolean swimCalculating,
                            boolean swimMoving,
                            int swimStuckTicks,
                            int swimRetries,
                            @Nullable BlockPos movementTarget,
                            @Nullable BlockPos swimTarget,
                            @Nullable BlockPos swimEndpoint,
                            @Nullable BlockPos rejectedTarget,
                            int combatTargetId,
                            int hunger,
                            boolean huntFoodPursuit,
                            boolean onGround,
                            boolean horizontalCollision,
                            boolean verticalCollision,
                            boolean navigationDone,
                            boolean navigationStuck,
                            String sleep,
                            String roost,
                            String drinking,
                            String rescue,
                            String ownerFollow,
                            String perception,
                            String tactical,
                            String pursuit,
                            String coordination,
                            String activity,
                            List<String> behaviours) {
        private static LogState capture(DragonEntity dragon, MessageDragonPathDebug snapshot) {
            LivingEntity combatTarget = dragon.getTarget();
            String activity = dragon.getBrain().getActiveNonCoreActivity()
                    .map(Object::toString)
                    .orElse("none");
            String coordination = dragon instanceof Nulljaw nulljaw
                    ? nulljaw.getCombatFormationDebugSummary()
                    : "none";
            return new LogState(
                    snapshot.locomotionMode(),
                    snapshot.movementMode(),
                    snapshot.navigationNextIndex(),
                    snapshot.navigationNodeCount(),
                    snapshot.navigationNodes().hashCode(),
                    snapshot.swimNextIndex(),
                    snapshot.swimNodeCount(),
                    snapshot.swimNodes().hashCode(),
                    snapshot.searchId(),
                    snapshot.swimCalculating(),
                    snapshot.swimMoving(),
                    snapshot.swimStuckTicks(),
                    snapshot.swimRetries(),
                    blockPosition(snapshot.movementTarget()),
                    blockPosition(snapshot.swimTarget()),
                    blockPosition(snapshot.swimEndpoint()),
                    blockPosition(snapshot.rejectedTarget()),
                    combatTarget == null ? -1 : combatTarget.getId(),
                    dragon.getHunger(),
                    dragon.isHuntFoodPursuitActive(),
                    dragon.onGround(),
                    dragon.horizontalCollision,
                    dragon.verticalCollision,
                    dragon.getNavigation().isDone(),
                    dragon.getNavigation().isStuck(),
                    sleepSummary(dragon),
                    roostSummary(dragon),
                    drinkingSummary(dragon),
                    rescueSummary(dragon),
                    ownerFollowSummary(dragon),
                    perceptionSummary(dragon),
                    tacticalSummary(dragon),
                    pursuitSummary(dragon),
                    coordination,
                    activity,
                    runningBehaviours(dragon)
            );
        }
    }

    private static String ownerFollowSummary(DragonEntity dragon) {
        if (!(dragon instanceof RideableDragonBase rideable)) {
            return "unsupported";
        }
        LivingEntity owner = rideable.getOwner();
        Entity ownerAnchor = owner == null ? null : DragonOwnerFollowTarget.anchor(owner);
        String ownerSummary = owner == null
                ? "none"
                : owner.getName().getString() + "@"
                + String.format(
                        java.util.Locale.ROOT,
                        "%.2f",
                        Math.sqrt(DragonOwnerFollowTarget.anchorDistanceToSqr(rideable, owner))
                );
        LivingEntity target = rideable.getTarget();
        boolean ownerPriority = rideable.isTame()
                && rideable.getCommand() == 0
                && owner != null
                && owner.isAlive()
                && owner.level() == rideable.level()
                && (target == null || !target.isAlive());
        String reservation = "none";
        if (rideable.isSleeping()) {
            reservation = "sleeping";
        } else if (rideable.isSleepTransitioning()) {
            reservation = "sleep-transition";
        } else if (rideable.isOrderedToSit()) {
            reservation = "ordered-sit";
        } else if (rideable.isInSittingPose()) {
            reservation = "sitting-pose";
        } else if (rideable.isInSitTransition()) {
            reservation = "sit-transition";
        } else if (rideable.isHuntFoodPursuitActive()) {
            reservation = "hunt-food";
        } else if (rideable.getBrain().hasMemoryValue(DragonMemories.SCENT_CANDIDATE)) {
            reservation = "scent";
        } else if (rideable.getBrain().hasMemoryValue(DragonMemories.INVESTIGATION_TARGET)
                && !ownerPriority
                && !rideable.getBrain().getMemory(DragonMemories.TARGET_VISIBLE).orElse(false)) {
            reservation = "unseen-investigation";
        }
        return "baby=" + rideable.isBaby()
                + ",tame=" + rideable.isTame()
                + ",owner=" + ownerSummary
                + ",mount=" + (ownerAnchor == null || ownerAnchor == owner
                ? "none"
                : ownerAnchor.getType().getDescriptionId() + "@" + ownerAnchor.blockPosition())
                + ",command=" + rideable.getCommand()
                + ",priority=" + ownerPriority
                + ",reserved=" + reservation
                + ",sitDown=" + rideable.isSittingDownAnimation()
                + ",love=" + rideable.isInLove()
                + ",vehicle=" + rideable.isVehicle()
                + ",passenger=" + rideable.isPassenger()
                + ",water=" + rideable.isInWaterOrBubble()
                + ",target=" + (target == null ? "none" : target.getName().getString() + "/" + target.isAlive());
    }

    private static String sleepSummary(DragonEntity dragon) {
        if (!dragon.getBrain().checkMemory(DragonMemories.SLEEP_PRESSURE, MemoryStatus.REGISTERED)) {
            return "legacy";
        }
        int pressure = Math.round(dragon.getBrain().getMemory(DragonMemories.SLEEP_PRESSURE).orElse(0.0F));
        boolean intent = dragon.getBrain().getMemory(DragonMemories.SLEEP_INTENT).orElse(false);
        int disturbance = Math.round(dragon.getSleepDisturbance());
        int wakeDisturbance = Math.round(dragon.getSleepDisturbanceThreshold());
        return pressure + "/100,intent=" + intent
                + ",disturbance=" + disturbance + "/" + wakeDisturbance
                + ",cause=" + dragon.getSleepDisturbanceCause()
                + ",decision=" + dragon.getSleepDecision();
    }

    private static String roostSummary(DragonEntity dragon) {
        ReturnToRoostBehaviour<?> roost = null;
        for (DragonBrainDiagnostics.RegisteredBehaviour registered
                : DragonBrainDiagnostics.getBehaviours(dragon, dragon.getBrain())) {
            if (registered.behaviour() instanceof ReturnToRoostBehaviour<?> returnToRoost) {
                roost = returnToRoost;
                break;
            }
            if (registered.behaviour() instanceof FirstApplicableDragonBehaviour<?> firstApplicable) {
                for (var child : firstApplicable.childBehaviours()) {
                    if (child instanceof ReturnToRoostBehaviour<?> returnToRoost) {
                        roost = returnToRoost;
                        break;
                    }
                }
            }
            if (roost != null) {
                break;
            }
        }
        if (roost == null) {
            return "disabled";
        }

        Map<String, String> details = roost.getDragonBrainDebugDetails();
        boolean ready = roost.cooldownRemaining(dragon.level().getGameTime()) == 0L;
        GlobalPos home = dragon.getBrain().getMemory(DragonMemories.HOME).orElse(null);
        GlobalPos sleepSite = dragon.getBrain().getMemory(DragonMemories.ROOST_SLEEP_POSITION).orElse(null);
        return "phase=" + details.getOrDefault("phase", "unknown")
                + ",reason=" + details.getOrDefault("reason", "unknown")
                + ",retry=" + details.getOrDefault("route_retry", "false")
                + ",failures=" + details.getOrDefault("ground_failures", "0")
                + ",site=" + (sleepSite == null ? "none" : sleepSite.pos().toShortString())
                + ",siteSource=" + (sleepSite == null
                ? "none"
                : sleepSite.equals(home) ? "home-fallback" : "dedicated")
                + ",cooldown=" + (ready ? "ready" : "waiting");
    }

    private static String tacticalSummary(DragonEntity dragon) {
        if (!dragon.getBrain().checkMemory(DragonMemories.TACTICAL_COMMITMENT, MemoryStatus.REGISTERED)) {
            return "disabled";
        }
        DragonTacticalCommitment commitment = dragon.getBrain()
                .getMemory(DragonMemories.TACTICAL_COMMITMENT)
                .orElse(null);
        return commitment == null ? "none" : commitment.summary();
    }

    private static String pursuitSummary(DragonEntity dragon) {
        for (DragonBrainDiagnostics.RegisteredBehaviour registered :
                DragonBrainDiagnostics.getBehaviours(dragon, dragon.getBrain())) {
            if (registered.behaviour() instanceof DragonTargetingBehaviour<?> targeting) {
                return targeting.getPursuitDebugSummary();
            }
        }
        return "disabled";
    }
}
