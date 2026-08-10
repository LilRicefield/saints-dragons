package com.leon.saintsdragons.server.ai.navigation.async;

import com.leon.saintsdragons.server.ai.pathfinding.DragonPathSearchDebug;
import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class AsyncGroundPathSearch {
    private static final int MAX_VISITED_NODES = 5000;
    private static final double SQRT_TWO = Math.sqrt(2.0D);
    private static final double SUPPORT_EPSILON = 1.0E-5D;
    private static final double MAX_SUPPORT_GAP = 1.0D - SUPPORT_EPSILON;
    private static final double TREE_NODE_MALUS = 4.0D;
    private static final int[][] CARDINAL_DIRECTIONS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };
    private static final int[][] DIAGONAL_DIRECTIONS = {
            {1, 1},
            {1, -1},
            {-1, 1},
            {-1, -1}
    };

    private final TerrainView terrain;
    private final Vec3 origin;
    private final Vec3 planningTarget;
    private final Vec3 requestedTarget;
    private final AABB relativeBounds;
    private final BlockPos rawStartNode;
    private final BlockPos goalNode;
    private final BlockPos minNode;
    private final BlockPos maxNode;
    private final double pathOffset;
    private final int maxStepUp;
    private final int maxDropDown;
    private final int goalAccuracy;
    private final double maxRange;
    private final boolean allowWater;
    private final double waterMalus;
    private final boolean completeRoute;
    private final BooleanSupplier cancelled;
    private final Map<Long, NodeEvaluation> nodeEvaluations = new HashMap<>();

    AsyncGroundPathSearch(ImmutableBlockSnapshot snapshot,
                          Vec3 origin,
                          Vec3 planningTarget,
                          Vec3 requestedTarget,
                          AABB relativeBounds,
                          BlockPos minNode,
                          BlockPos maxNode,
                          int footprintSize,
                          int maxStepUp,
                          int maxDropDown,
                          int goalAccuracy,
                          double maxRange,
                          boolean allowWater,
                          double waterMalus,
                          boolean canPassThroughTrees,
                          Map<BlockPathTypes, Float> pathMalus,
                          BooleanSupplier cancelled) {
        this(
                new SnapshotTerrainView(snapshot, canPassThroughTrees, pathMalus),
                origin,
                planningTarget,
                requestedTarget,
                relativeBounds,
                minNode,
                maxNode,
                footprintSize,
                maxStepUp,
                maxDropDown,
                goalAccuracy,
                maxRange,
                allowWater,
                waterMalus,
                cancelled
        );
    }

    AsyncGroundPathSearch(TerrainView terrain,
                          Vec3 origin,
                          Vec3 planningTarget,
                          Vec3 requestedTarget,
                          AABB relativeBounds,
                          BlockPos minNode,
                          BlockPos maxNode,
                          int footprintSize,
                          int maxStepUp,
                          int maxDropDown,
                          int goalAccuracy,
                          double maxRange,
                          boolean allowWater,
                          double waterMalus,
                          BooleanSupplier cancelled) {
        this.terrain = terrain;
        this.origin = origin;
        this.planningTarget = planningTarget;
        this.requestedTarget = requestedTarget;
        this.relativeBounds = relativeBounds;
        this.minNode = minNode;
        this.maxNode = maxNode;
        this.pathOffset = footprintSize * 0.5D;
        int footprintOffset = footprintSize / 2;
        this.rawStartNode = new BlockPos(
                BlockPos.containing(origin).getX() - footprintOffset,
                BlockPos.containing(origin).getY(),
                BlockPos.containing(origin).getZ() - footprintOffset
        );
        BlockPos targetPos = BlockPos.containing(planningTarget);
        this.goalNode = new BlockPos(
                targetPos.getX() - footprintOffset,
                targetPos.getY(),
                targetPos.getZ() - footprintOffset
        );
        this.maxStepUp = Math.max(0, maxStepUp);
        this.maxDropDown = Math.max(1, maxDropDown);
        this.goalAccuracy = Math.max(0, goalAccuracy);
        this.maxRange = Math.max(1.0D, maxRange);
        this.allowWater = allowWater;
        this.waterMalus = Math.max(0.0D, waterMalus);
        this.completeRoute = planningTarget.distanceToSqr(requestedTarget) < 1.0E-8D;
        this.cancelled = cancelled;
    }

    @Nullable
    Path findPath(@Nullable DragonPathSearchDebug.SearchSession debugSession) {
        long startedNanos = System.nanoTime();
        BlockPos startNode = resolveStartNode();
        if (startNode == null || this.cancelled.getAsBoolean()) {
            return null;
        }

        long startKey = startNode.asLong();
        PriorityQueue<OpenNode> open = new PriorityQueue<>(
                Comparator.comparingDouble(OpenNode::fScore)
                        .thenComparingDouble(OpenNode::hScore)
        );
        Map<Long, Long> cameFrom = new HashMap<>();
        Map<Long, Double> gScore = new HashMap<>();
        Set<Long> closed = debugSession == null ? new HashSet<>() : new LinkedHashSet<>();

        double startHeuristic = heuristic(startNode);
        gScore.put(startKey, 0.0D);
        open.add(new OpenNode(startKey, 0.0D, startHeuristic, startHeuristic));
        long bestKey = startKey;
        double bestHeuristic = startHeuristic;
        boolean reached = isGoal(startNode);
        int visited = 0;

        while (!open.isEmpty() && visited < MAX_VISITED_NODES && !reached) {
            if (this.cancelled.getAsBoolean()) {
                return null;
            }

            OpenNode current = open.poll();
            double knownScore = gScore.getOrDefault(current.key(), Double.POSITIVE_INFINITY);
            if (current.gScore() > knownScore || !closed.add(current.key())) {
                continue;
            }
            visited++;

            BlockPos currentPos = BlockPos.of(current.key());
            double currentHeuristic = heuristic(currentPos);
            if (currentHeuristic < bestHeuristic) {
                bestHeuristic = currentHeuristic;
                bestKey = current.key();
            }
            if (isGoal(currentPos)) {
                bestKey = current.key();
                reached = true;
                break;
            }

            for (int[] direction : CARDINAL_DIRECTIONS) {
                visitNeighbor(
                        startKey,
                        current,
                        currentPos,
                        direction[0],
                        direction[1],
                        false,
                        knownScore,
                        closed,
                        cameFrom,
                        gScore,
                        open
                );
            }
            for (int[] direction : DIAGONAL_DIRECTIONS) {
                visitNeighbor(
                        startKey,
                        current,
                        currentPos,
                        direction[0],
                        direction[1],
                        true,
                        knownScore,
                        closed,
                        cameFrom,
                        gScore,
                        open
                );
            }
        }

        if (!reached && bestKey == startKey) {
            publishDebug(debugSession, closed, gScore, false, startedNanos);
            return null;
        }

        boolean reachedRequestedTarget = reached && this.completeRoute;
        Path path = buildPath(cameFrom, bestKey, reachedRequestedTarget);
        publishDebug(debugSession, closed, gScore, reachedRequestedTarget, startedNanos);
        return path;
    }

    private void visitNeighbor(long startKey,
                               OpenNode current,
                               BlockPos currentPos,
                               int dx,
                               int dz,
                               boolean diagonal,
                               double knownScore,
                               Set<Long> closed,
                               Map<Long, Long> cameFrom,
                               Map<Long, Double> gScore,
                               PriorityQueue<OpenNode> open) {
        if (this.cancelled.getAsBoolean()) {
            return;
        }
        BlockPos neighbor = resolveNeighbor(startKey, current.key(), currentPos, dx, dz);
        if (neighbor == null || (diagonal && !hasDiagonalClearance(startKey, current.key(), currentPos, neighbor, dx, dz))) {
            return;
        }

        long neighborKey = neighbor.asLong();
        if (closed.contains(neighborKey)) {
            return;
        }
        NodeEvaluation evaluation = evaluateNode(neighbor);
        double verticalDifference = neighbor.getY() - currentPos.getY();
        double stepCost = diagonal ? SQRT_TWO : 1.0D;
        if (verticalDifference > 0.0D) {
            stepCost += verticalDifference * 0.75D;
        } else if (verticalDifference < 0.0D) {
            stepCost += -verticalDifference * 0.15D;
        }
        stepCost += evaluation.malus();

        double tentativeScore = knownScore + stepCost;
        if (tentativeScore >= gScore.getOrDefault(neighborKey, Double.POSITIVE_INFINITY)) {
            return;
        }

        cameFrom.put(neighborKey, current.key());
        gScore.put(neighborKey, tentativeScore);
        double nextHeuristic = heuristic(neighbor);
        open.add(new OpenNode(
                neighborKey,
                tentativeScore,
                nextHeuristic,
                tentativeScore + nextHeuristic
        ));
    }

    private @Nullable BlockPos resolveStartNode() {
        if (withinBounds(this.rawStartNode) && evaluateNode(this.rawStartNode).usable()) {
            return this.rawStartNode;
        }
        for (int up = 1; up <= this.maxStepUp; up++) {
            BlockPos candidate = this.rawStartNode.above(up);
            if (withinBounds(candidate) && evaluateNode(candidate).usable()) {
                return candidate;
            }
        }
        for (int down = 1; down <= this.maxDropDown; down++) {
            BlockPos candidate = this.rawStartNode.below(down);
            if (withinBounds(candidate) && evaluateNode(candidate).usable()) {
                return candidate;
            }
        }
        return null;
    }

    private @Nullable BlockPos resolveNeighbor(long startKey,
                                               long currentKey,
                                               BlockPos current,
                                               int dx,
                                               int dz) {
        BlockPos candidate = new BlockPos(current.getX() + dx, current.getY(), current.getZ() + dz);
        if (isUsableTransition(startKey, currentKey, current, candidate)) {
            return candidate;
        }
        for (int up = 1; up <= this.maxStepUp; up++) {
            candidate = new BlockPos(current.getX() + dx, current.getY() + up, current.getZ() + dz);
            if (isUsableTransition(startKey, currentKey, current, candidate)) {
                return candidate;
            }
        }
        for (int down = 1; down <= this.maxDropDown; down++) {
            candidate = new BlockPos(current.getX() + dx, current.getY() - down, current.getZ() + dz);
            if (isUsableTransition(startKey, currentKey, current, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isUsableTransition(long startKey,
                                       long currentKey,
                                       BlockPos current,
                                       BlockPos candidate) {
        return withinBounds(candidate)
                && withinRange(candidate)
                && evaluateNode(candidate).usable()
                && isTransitionClear(startKey, currentKey, current, candidate);
    }

    private boolean hasDiagonalClearance(long startKey,
                                         long currentKey,
                                         BlockPos current,
                                         BlockPos diagonal,
                                         int dx,
                                         int dz) {
        BlockPos firstSide = resolveNeighbor(startKey, currentKey, current, dx, 0);
        BlockPos secondSide = resolveNeighbor(startKey, currentKey, current, 0, dz);
        return firstSide != null
                && secondSide != null
                && firstSide.getY() == diagonal.getY()
                && secondSide.getY() == diagonal.getY();
    }

    private boolean isTransitionClear(long startKey,
                                      long currentKey,
                                      BlockPos current,
                                      BlockPos candidate) {
        Vec3 from = currentKey == startKey ? this.origin : entityPosition(current);
        Vec3 to = entityPosition(candidate);
        AABB startBox = this.relativeBounds.move(from);
        double dy = to.y - from.y;
        Vec3 horizontal = new Vec3(to.x - from.x, 0.0D, to.z - from.z);

        if (dy > 1.0E-7D) {
            Vec3 lift = new Vec3(0.0D, dy, 0.0D);
            return this.terrain.isClear(startBox, lift)
                    && this.terrain.isClear(startBox.move(lift), horizontal);
        }
        if (dy < -1.0E-7D) {
            return this.terrain.isClear(startBox, horizontal)
                    && this.terrain.isClear(startBox.move(horizontal), new Vec3(0.0D, dy, 0.0D));
        }
        return this.terrain.isClear(startBox, horizontal);
    }

    private NodeEvaluation evaluateNode(BlockPos node) {
        return this.nodeEvaluations.computeIfAbsent(node.asLong(), ignored -> {
            AABB body = bodyAt(node);
            if (!this.terrain.isClear(body, Vec3.ZERO) || this.terrain.intersectsLava(body)) {
                return NodeEvaluation.BLOCKED;
            }
            boolean water = this.terrain.intersectsWater(body);
            if (water && !this.allowWater) {
                return NodeEvaluation.BLOCKED;
            }
            if (!water && !this.terrain.hasSupport(body) && !this.terrain.isClimbable(body)) {
                return NodeEvaluation.BLOCKED;
            }
            double malus = this.terrain.malus(body) + (water ? this.waterMalus : 0.0D);
            if (!Double.isFinite(malus)) {
                return NodeEvaluation.BLOCKED;
            }
            return new NodeEvaluation(true, water, malus);
        });
    }

    private boolean isGoal(BlockPos node) {
        int dx = Math.abs(node.getX() - this.goalNode.getX());
        int dz = Math.abs(node.getZ() - this.goalNode.getZ());
        int dy = Math.abs(node.getY() - this.goalNode.getY());
        return Math.max(dx, dz) <= this.goalAccuracy
                && dy <= Math.max(1, this.goalAccuracy);
    }

    private boolean withinBounds(BlockPos node) {
        return node.getX() >= this.minNode.getX() && node.getX() <= this.maxNode.getX()
                && node.getY() >= this.minNode.getY() && node.getY() <= this.maxNode.getY()
                && node.getZ() >= this.minNode.getZ() && node.getZ() <= this.maxNode.getZ();
    }

    private boolean withinRange(BlockPos node) {
        double dx = node.getX() - this.rawStartNode.getX();
        double dy = node.getY() - this.rawStartNode.getY();
        double dz = node.getZ() - this.rawStartNode.getZ();
        return dx * dx + dy * dy + dz * dz <= this.maxRange * this.maxRange;
    }

    private double heuristic(BlockPos node) {
        int dx = Math.abs(node.getX() - this.goalNode.getX());
        int dz = Math.abs(node.getZ() - this.goalNode.getZ());
        int diagonal = Math.min(dx, dz);
        int straight = Math.max(dx, dz) - diagonal;
        int dy = Math.abs(node.getY() - this.goalNode.getY());
        return diagonal * SQRT_TWO + straight + dy * 0.75D;
    }

    private Path buildPath(Map<Long, Long> cameFrom, long endKey, boolean reached) {
        List<Node> nodes = new ArrayList<>();
        long current = endKey;
        while (true) {
            BlockPos pos = BlockPos.of(current);
            Node node = new Node(pos.getX(), pos.getY(), pos.getZ());
            NodeEvaluation evaluation = evaluateNode(pos);
            node.type = evaluation.water() ? BlockPathTypes.WATER : BlockPathTypes.WALKABLE;
            node.costMalus = (float) evaluation.malus();
            nodes.add(node);
            Long previous = cameFrom.get(current);
            if (previous == null) {
                break;
            }
            current = previous;
        }
        Collections.reverse(nodes);
        return new Path(nodes, BlockPos.containing(this.requestedTarget), reached);
    }

    private void publishDebug(@Nullable DragonPathSearchDebug.SearchSession debugSession,
                              Set<Long> closed,
                              Map<Long, Double> gScore,
                              boolean reached,
                              long startedNanos) {
        if (debugSession == null || this.cancelled.getAsBoolean()) {
            return;
        }
        List<Vec3> closedPositions = closed.stream()
                .map(BlockPos::of)
                .map(this::entityPosition)
                .toList();
        List<Vec3> openPositions = gScore.keySet().stream()
                .filter(key -> !closed.contains(key))
                .map(BlockPos::of)
                .map(this::entityPosition)
                .toList();
        DragonPathSearchDebug.publishGridSearch(
                debugSession,
                DragonPathSearchDebug.SearchType.GROUND,
                this.origin,
                this.requestedTarget,
                closedPositions,
                openPositions,
                List.of(),
                reached,
                startedNanos
        );
    }

    private Vec3 entityPosition(BlockPos node) {
        return new Vec3(node.getX() + this.pathOffset, node.getY(), node.getZ() + this.pathOffset);
    }

    private AABB bodyAt(BlockPos node) {
        return this.relativeBounds.move(entityPosition(node));
    }

    interface TerrainView {
        boolean isClear(AABB startBox, Vec3 movement);

        boolean hasSupport(AABB body);

        boolean intersectsWater(AABB body);

        boolean intersectsLava(AABB body);

        boolean isClimbable(AABB body);

        double malus(AABB body);
    }

    private static final class SnapshotTerrainView implements TerrainView {
        private final ImmutableBlockSnapshot snapshot;
        private final Predicate<BlockState> passableTreeBlocks;
        private final Predicate<BlockState> ignoredBlocks;
        private final Map<BlockPathTypes, Float> pathMalus;

        private SnapshotTerrainView(ImmutableBlockSnapshot snapshot,
                                    boolean canPassThroughTrees,
                                    Map<BlockPathTypes, Float> pathMalus) {
            this.snapshot = snapshot;
            this.passableTreeBlocks = canPassThroughTrees
                    ? DragonDestructionManager::isPassivelyBreakableTreeBlock
                    : state -> false;
            this.ignoredBlocks = state -> state.is(Blocks.LADDER) || this.passableTreeBlocks.test(state);
            this.pathMalus = Map.copyOf(pathMalus);
        }

        @Override
        public boolean isClear(AABB startBox, Vec3 movement) {
            return VoxelAabbSweeper.isClear(this.snapshot, startBox, movement, this.ignoredBlocks);
        }

        @Override
        public boolean hasSupport(AABB body) {
            int minX = floorInside(body.minX);
            int maxX = floorInside(body.maxX - SUPPORT_EPSILON);
            int minY = floorInside(body.minY - MAX_SUPPORT_GAP);
            int maxY = floorInside(body.minY + SUPPORT_EPSILON);
            int minZ = floorInside(body.minZ);
            int maxZ = floorInside(body.maxZ - SUPPORT_EPSILON);
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        cursor.set(x, y, z);
                        if (this.passableTreeBlocks.test(this.snapshot.getBlockState(cursor))) {
                            continue;
                        }
                        for (AABB obstacle : this.snapshot.collisionBoxes(cursor)) {
                            double gap = body.minY - obstacle.maxY;
                            if (gap >= -SUPPORT_EPSILON
                                    && gap <= MAX_SUPPORT_GAP
                                    && overlapsHorizontally(body, obstacle)) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public boolean intersectsWater(AABB body) {
            return intersectsFluid(body, true);
        }

        @Override
        public boolean intersectsLava(AABB body) {
            return intersectsFluid(body, false);
        }

        private boolean intersectsFluid(AABB body, boolean water) {
            return anyBlock(body, state -> water
                    ? state.getFluidState().is(FluidTags.WATER)
                    : state.getFluidState().is(FluidTags.LAVA));
        }

        @Override
        public boolean isClimbable(AABB body) {
            return anyBlock(body, state -> state.is(Blocks.LADDER));
        }

        @Override
        public double malus(AABB body) {
            double result = anyBlock(body, this.passableTreeBlocks)
                    ? TREE_NODE_MALUS
                    : 0.0D;
            int minX = floorInside(body.minX);
            int maxX = floorInside(body.maxX - SUPPORT_EPSILON);
            int y = floorInside(body.minY + SUPPORT_EPSILON);
            int minZ = floorInside(body.minZ);
            int maxZ = floorInside(body.maxZ - SUPPORT_EPSILON);
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    BlockState state = this.snapshot.getBlockState(cursor);
                    if (this.passableTreeBlocks.test(state)) {
                        continue;
                    }
                    BlockPathTypes pathType = state.is(Blocks.LADDER)
                            ? BlockPathTypes.WALKABLE
                            : WalkNodeEvaluator.getBlockPathTypeStatic(this.snapshot, cursor);
                    if (pathType == BlockPathTypes.WATER
                            || pathType == BlockPathTypes.WATER_BORDER
                            || pathType == BlockPathTypes.LAVA) {
                        continue;
                    }
                    float configuredMalus = this.pathMalus.getOrDefault(pathType, 0.0F);
                    if (configuredMalus < 0.0F) {
                        return Double.POSITIVE_INFINITY;
                    }
                    result = Math.max(result, configuredMalus);
                }
            }
            return result;
        }

        private boolean anyBlock(AABB body, Predicate<BlockState> predicate) {
            int minX = floorInside(body.minX);
            int maxX = floorInside(body.maxX - SUPPORT_EPSILON);
            int minY = floorInside(body.minY + SUPPORT_EPSILON);
            int maxY = floorInside(body.maxY - SUPPORT_EPSILON);
            int minZ = floorInside(body.minZ);
            int maxZ = floorInside(body.maxZ - SUPPORT_EPSILON);
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        cursor.set(x, y, z);
                        if (predicate.test(this.snapshot.getBlockState(cursor))) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private static int floorInside(double coordinate) {
            return (int) Math.floor(coordinate);
        }

        private static boolean overlapsHorizontally(AABB first, AABB second) {
            return first.maxX > second.minX + SUPPORT_EPSILON
                    && first.minX < second.maxX - SUPPORT_EPSILON
                    && first.maxZ > second.minZ + SUPPORT_EPSILON
                    && first.minZ < second.maxZ - SUPPORT_EPSILON;
        }
    }

    private record NodeEvaluation(boolean usable, boolean water, double malus) {
        private static final NodeEvaluation BLOCKED = new NodeEvaluation(false, false, 0.0D);
    }

    private record OpenNode(long key, double gScore, double hScore, double fScore) {
    }
}
