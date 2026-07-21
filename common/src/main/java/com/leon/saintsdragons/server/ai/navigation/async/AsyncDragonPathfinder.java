package com.leon.saintsdragons.server.ai.navigation.async;

import com.leon.saintsdragons.server.ai.navigation.PathFinderGround;
import com.leon.saintsdragons.server.ai.pathfinding.DragonPathSearchDebug;
import com.leon.saintsdragons.server.ai.pathfinding.DragonPathSearchDebuggable;
import com.leon.saintsdragons.server.ai.pathfinding.DragonWalkNodeEvaluator;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

public final class AsyncDragonPathfinder {
    private static final int MAX_SWIM_ASTAR_VISITS = 50000;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "SaintsDragons-Async-Pathfinder");
        thread.setDaemon(true);
        return thread;
    });

    private AsyncDragonPathfinder() {
    }

    public static void calculateFlyingPathAsync(Mob dragon, Vec3 target, Consumer<Path> callback) {
        calculateFlyingPathAsync(dragon, target, callback, false);
    }

    public static Future<?> calculateGroundPathAsync(Mob dragon, Vec3 target, Consumer<Path> callback) {
        int goalAccuracy = Math.max(1, Mth.floor(Math.max(1.5D, dragon.getBbWidth() * 0.75D)));
        return calculateGroundPathAsync(dragon, target, goalAccuracy, callback);
    }

    public static Future<?> calculateGroundPathAsync(Mob dragon,
                                                      Vec3 target,
                                                      int goalAccuracy,
                                                      Consumer<Path> callback) {
        return calculateGroundPathAsync(dragon, target, goalAccuracy, false, callback);
    }

    public static Future<?> calculateGroundPathAsync(Mob dragon,
                                                      Vec3 target,
                                                      int goalAccuracy,
                                                      boolean avoidWater,
                                                      Consumer<Path> callback) {
        if (dragon.level().isClientSide) {
            return CompletableFuture.completedFuture(null);
        }
        MinecraftServer server = dragon.getServer();
        if (server == null) {
            callback.accept(null);
            return CompletableFuture.completedFuture(null);
        }

        BlockPos startPos = dragon.blockPosition();
        BlockPos targetPos = BlockPos.containing(target);
        boolean canPassThroughTrees = dragon instanceof DragonEntity dragonEntity
                && dragon.level() instanceof ServerLevel serverLevel
                && DragonDestructionManager.canApplyPassiveTreeDestruction(serverLevel, dragonEntity);
        int followRange = Math.max((int) dragon.getAttributeValue(Attributes.FOLLOW_RANGE), 128);
        double routeDistance = Math.sqrt(startPos.distSqr(targetPos));
        int searchRange = Mth.clamp(Mth.ceil(routeDistance) + 24, 32, followRange);
        int resolvedGoalAccuracy = Math.max(0, goalAccuracy);
        int horizontalMargin = Math.max(16, Mth.ceil(dragon.getBbWidth()) + 8);
        int verticalMargin = Math.max(12, Mth.ceil(dragon.getBbHeight()) + 6);
        BlockPos minPos = new BlockPos(
                Math.min(startPos.getX(), targetPos.getX()) - horizontalMargin,
                Math.min(startPos.getY(), targetPos.getY()) - verticalMargin,
                Math.min(startPos.getZ(), targetPos.getZ()) - horizontalMargin
        );
        BlockPos maxPos = new BlockPos(
                Math.max(startPos.getX(), targetPos.getX()) + horizontalMargin,
                Math.max(startPos.getY(), targetPos.getY()) + verticalMargin,
                Math.max(startPos.getZ(), targetPos.getZ()) + horizontalMargin
        );

        PathNavigationRegion snapshot;
        try {
            snapshot = new PathNavigationRegion(dragon.level(), minPos, maxPos);
        } catch (Exception exception) {
            callback.accept(null);
            return CompletableFuture.completedFuture(null);
        }

        return EXECUTOR.submit(() -> {
            Path path;
            try {
                NodeEvaluator nodeEvaluator = new DragonWalkNodeEvaluator(canPassThroughTrees, avoidWater);
                nodeEvaluator.setCanPassDoors(true);
                PathFinder pathFinder = new PathFinderGround(nodeEvaluator, 5000);
                path = pathFinder.findPath(
                        snapshot,
                        dragon,
                        Set.of(targetPos),
                        (float) searchRange,
                        resolvedGoalAccuracy,
                        1.0F
                );
            } catch (Exception exception) {
                path = null;
            }

            if (Thread.currentThread().isInterrupted() || server.isStopped() || dragon.isRemoved()) {
                return;
            }
            Path resolvedPath = path;
            server.execute(() -> {
                if (server.isStopped() || dragon.isRemoved() || !dragon.isAlive()) {
                    return;
                }
                callback.accept(resolvedPath);
            });
        });
    }

    public static void calculateSwarmFlyingPathAsync(Mob swarm, Vec3 target, Consumer<Path> callback) {
        calculateFlyingPathAsync(swarm, target, callback, true);
    }

    private static void calculateFlyingPathAsync(Mob dragon, Vec3 target, Consumer<Path> callback,
                                                  boolean useSwarmClearance) {
        if (dragon.level().isClientSide) {
            return;
        }
        MinecraftServer server = dragon.getServer();
        if (server == null) {
            return;
        }

        BlockPos startPos = dragon.blockPosition();
        BlockPos targetPos = BlockPos.containing(target);
        int followRange = Math.max((int) dragon.getAttributeValue(Attributes.FOLLOW_RANGE), 128);
        int margin = Math.max(followRange, 32);

        int minX = Math.min(startPos.getX() - margin, targetPos.getX() - 16);
        int minY = Math.min(startPos.getY() - margin, targetPos.getY() - 16);
        int minZ = Math.min(startPos.getZ() - margin, targetPos.getZ() - 16);
        int maxX = Math.max(startPos.getX() + margin, targetPos.getX() + 16);
        int maxY = Math.max(startPos.getY() + margin, targetPos.getY() + 16);
        int maxZ = Math.max(startPos.getZ() + margin, targetPos.getZ() + 16);

        PathNavigationRegion snapshot;
        try {
            snapshot = new PathNavigationRegion(dragon.level(), new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
        } catch (Exception exception) {
            callback.accept(null);
            return;
        }

        CompletableFuture
                .supplyAsync(() -> {
                    NodeEvaluator nodeEvaluator = useSwarmClearance
                            ? new AsyncSwarmFlyNodeEvaluator()
                            : new AsyncDragonFlyNodeEvaluator();
                    DragonPathSearchDebug.NodeCollector debugCollector = DragonPathSearchDebug.beginNodeSearch(
                            dragon,
                            DragonPathSearchDebug.SearchType.AIR,
                            target
                    );
                    if (nodeEvaluator instanceof DragonPathSearchDebuggable debuggable) {
                        debuggable.setPathSearchDebugCollector(debugCollector);
                    }

                    Path path;
                    try {
                        nodeEvaluator.setCanPassDoors(true);
                        nodeEvaluator.setCanOpenDoors(true);
                        nodeEvaluator.setCanFloat(true);
                        PathFinder pathFinder = new PathFinder(nodeEvaluator, 5000);
                        path = pathFinder.findPath(snapshot, dragon, Set.of(targetPos), (float) followRange, 1, 1.0f);
                    } catch (Exception exception) {
                        path = null;
                    }

                    if (debugCollector != null) {
                        debugCollector.complete(path);
                    }
                    if (nodeEvaluator instanceof DragonPathSearchDebuggable debuggable) {
                        debuggable.setPathSearchDebugCollector(null);
                    }
                    return path;
                }, EXECUTOR)
                .thenAccept(path -> {
                    if (server.isStopped() || dragon.isRemoved()) {
                        return;
                    }
                    server.execute(() -> {
                        if (server.isStopped() || dragon.isRemoved() || !dragon.isAlive()) {
                            return;
                        }
                        callback.accept(path);
                    });
                });
    }

    public static void calculateSwimPathAsync(Mob dragon, Vec3 target, Consumer<List<Vec3>> callback) {
        if (dragon.level().isClientSide) {
            return;
        }
        MinecraftServer server = dragon.getServer();
        if (server == null) {
            return;
        }

        SwimPathSnapshot snapshot;
        try {
            snapshot = SwimPathSnapshot.capture(dragon, target);
        } catch (Exception exception) {
            callback.accept(null);
            return;
        }

        UUID debugDragonId = DragonPathSearchDebug.isActive(dragon.getUUID()) ? dragon.getUUID() : null;
        CompletableFuture
                .supplyAsync(() -> findSwimPath(snapshot, debugDragonId), EXECUTOR)
                .thenAccept(path -> {
                    if (server.isStopped() || dragon.isRemoved()) {
                        return;
                    }
                    server.execute(() -> {
                        if (server.isStopped() || dragon.isRemoved() || !dragon.isAlive()) {
                            return;
                        }
                        callback.accept(path);
                    });
                });
    }

    private static List<Vec3> findSwimPath(SwimPathSnapshot snapshot, UUID debugDragonId) {
        long startedNanos = System.nanoTime();
        int start = snapshot.index(snapshot.startX, snapshot.startY, snapshot.startZ);
        int goal = snapshot.index(snapshot.goalX, snapshot.goalY, snapshot.goalZ);
        if (!snapshot.isWaterIndex(start) || !snapshot.isWaterIndex(goal)) {
            if (debugDragonId != null) {
                DragonPathSearchDebug.publishGridSearch(
                        debugDragonId,
                        DragonPathSearchDebug.SearchType.SWIM,
                        snapshot.toWorld(start),
                        snapshot.toWorld(goal),
                        List.of(),
                        List.of(),
                        List.of(),
                        false,
                        startedNanos
                );
            }
            return null;
        }

        PriorityQueue<SwimNode> open = new PriorityQueue<>(Comparator.comparingDouble(SwimNode::fScore));
        Map<Integer, Integer> cameFrom = new HashMap<>();
        Map<Integer, Double> gScore = new HashMap<>();
        Set<Integer> closed = debugDragonId == null ? new HashSet<>() : new LinkedHashSet<>();
        gScore.put(start, 0.0D);
        open.add(new SwimNode(start, 0.0D, snapshot.heuristic(start, goal)));

        List<Vec3> path = null;
        int visited = 0;
        while (!open.isEmpty() && visited < MAX_SWIM_ASTAR_VISITS) {
            SwimNode current = open.poll();
            double currentScore = gScore.getOrDefault(current.index(), Double.POSITIVE_INFINITY);
            if (current.gScore() > currentScore || !closed.add(current.index())) {
                continue;
            }
            visited++;
            if (current.index() == goal) {
                path = snapshot.reconstructPath(cameFrom, current.index());
                break;
            }

            for (int neighbor : snapshot.neighbors(current.index())) {
                if (closed.contains(neighbor)) {
                    continue;
                }
                double tentativeScore = currentScore + snapshot.stepCost(current.index(), neighbor);
                if (tentativeScore >= gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    continue;
                }

                cameFrom.put(neighbor, current.index());
                gScore.put(neighbor, tentativeScore);
                open.add(new SwimNode(
                        neighbor,
                        tentativeScore,
                        tentativeScore + snapshot.heuristic(neighbor, goal)
                ));
            }
        }

        if (debugDragonId != null) {
            List<Vec3> closedPositions = closed.stream().map(snapshot::toWorld).toList();
            List<Vec3> openPositions = gScore.keySet().stream()
                    .filter(index -> !closed.contains(index))
                    .map(snapshot::toWorld)
                    .toList();
            DragonPathSearchDebug.publishGridSearch(
                    debugDragonId,
                    DragonPathSearchDebug.SearchType.SWIM,
                    snapshot.toWorld(start),
                    snapshot.toWorld(goal),
                    closedPositions,
                    openPositions,
                    List.of(),
                    path != null,
                    startedNanos
            );
        }
        return path;
    }

    private record SwimNode(int index, double gScore, double fScore) {
    }

    private static final class SwimPathSnapshot {
        private static final int HORIZONTAL_PADDING = 16;
        private static final int VERTICAL_PADDING = 8;
        private static final int MAX_HORIZONTAL_SPAN = 96;
        private static final int MAX_VERTICAL_SPAN = 32;

        private final boolean[] water;
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final int startX;
        private final int startY;
        private final int startZ;
        private final int goalX;
        private final int goalY;
        private final int goalZ;

        private SwimPathSnapshot(boolean[] water, int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ,
                                 int startX, int startY, int startZ, int goalX, int goalY, int goalZ) {
            this.water = water;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.startX = startX;
            this.startY = startY;
            this.startZ = startZ;
            this.goalX = goalX;
            this.goalY = goalY;
            this.goalZ = goalZ;
        }

        static SwimPathSnapshot capture(Mob dragon, Vec3 target) {
            BlockPos startPos = dragon.blockPosition();
            BlockPos goalPos = BlockPos.containing(target);
            int minX = Math.min(startPos.getX(), goalPos.getX()) - HORIZONTAL_PADDING;
            int maxX = Math.max(startPos.getX(), goalPos.getX()) + HORIZONTAL_PADDING;
            int minY = Math.min(startPos.getY(), goalPos.getY()) - VERTICAL_PADDING;
            int maxY = Math.max(startPos.getY(), goalPos.getY()) + VERTICAL_PADDING;
            int minZ = Math.min(startPos.getZ(), goalPos.getZ()) - HORIZONTAL_PADDING;
            int maxZ = Math.max(startPos.getZ(), goalPos.getZ()) + HORIZONTAL_PADDING;

            minY = Math.max(minY, dragon.level().getMinBuildHeight());
            maxY = Math.min(maxY, dragon.level().getMaxBuildHeight() - 1);

            if (maxX - minX > MAX_HORIZONTAL_SPAN) {
                int center = Mth.floor((startPos.getX() + goalPos.getX()) * 0.5D);
                minX = center - MAX_HORIZONTAL_SPAN / 2;
                maxX = center + MAX_HORIZONTAL_SPAN / 2;
            }
            if (maxZ - minZ > MAX_HORIZONTAL_SPAN) {
                int center = Mth.floor((startPos.getZ() + goalPos.getZ()) * 0.5D);
                minZ = center - MAX_HORIZONTAL_SPAN / 2;
                maxZ = center + MAX_HORIZONTAL_SPAN / 2;
            }
            if (maxY - minY > MAX_VERTICAL_SPAN) {
                int center = Mth.floor((startPos.getY() + goalPos.getY()) * 0.5D);
                minY = Math.max(dragon.level().getMinBuildHeight(), center - MAX_VERTICAL_SPAN / 2);
                maxY = Math.min(dragon.level().getMaxBuildHeight() - 1, center + MAX_VERTICAL_SPAN / 2);
            }

            int sizeX = maxX - minX + 1;
            int sizeY = maxY - minY + 1;
            int sizeZ = maxZ - minZ + 1;
            boolean[] rawWater = new boolean[sizeX * sizeY * sizeZ];
            boolean[] rawClear = new boolean[rawWater.length];
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    int worldX = minX + x;
                    int worldZ = minZ + z;
                    if (!dragon.level().hasChunkAt(new BlockPos(worldX, startPos.getY(), worldZ))) {
                        continue;
                    }
                    for (int y = 0; y < sizeY; y++) {
                        cursor.set(worldX, minY + y, worldZ);
                        int index = index(x, y, z, sizeX, sizeY);
                        BlockState state = dragon.level().getBlockState(cursor);
                        rawWater[index] = state.getFluidState().is(FluidTags.WATER);
                        rawClear[index] = state.getCollisionShape(dragon.level(), cursor).isEmpty();
                    }
                }
            }

            int horizontalClearance = Math.max(0, Mth.ceil(dragon.getBbWidth() * 0.5F - 0.5F));
            int verticalClearance = Math.max(1, Mth.ceil(dragon.getBbHeight() + 0.5F));
            boolean[] water = buildClearanceMap(
                    rawWater,
                    rawClear,
                    sizeX,
                    sizeY,
                    sizeZ,
                    horizontalClearance,
                    verticalClearance
            );

            int startX = Mth.clamp(startPos.getX() - minX, 0, sizeX - 1);
            int startY = Mth.clamp(startPos.getY() - minY, 0, sizeY - 1);
            int startZ = Mth.clamp(startPos.getZ() - minZ, 0, sizeZ - 1);
            int goalX = Mth.clamp(goalPos.getX() - minX, 0, sizeX - 1);
            int goalY = Mth.clamp(goalPos.getY() - minY, 0, sizeY - 1);
            int goalZ = Mth.clamp(goalPos.getZ() - minZ, 0, sizeZ - 1);

            int nearestStart = nearestWaterIndex(water, sizeX, sizeY, sizeZ, startX, startY, startZ, 6);
            if (nearestStart >= 0) {
                startX = nearestStart % sizeX;
                startY = (nearestStart / sizeX) % sizeY;
                startZ = nearestStart / (sizeX * sizeY);
            }

            int nearestGoal = nearestWaterIndex(water, sizeX, sizeY, sizeZ, goalX, goalY, goalZ, 12);
            if (nearestGoal >= 0) {
                goalX = nearestGoal % sizeX;
                goalY = (nearestGoal / sizeX) % sizeY;
                goalZ = nearestGoal / (sizeX * sizeY);
            }

            return new SwimPathSnapshot(water, minX, minY, minZ, sizeX, sizeY, sizeZ,
                    startX, startY, startZ, goalX, goalY, goalZ);
        }

        private static boolean[] buildClearanceMap(boolean[] rawWater,
                                                   boolean[] rawClear,
                                                   int sizeX,
                                                   int sizeY,
                                                   int sizeZ,
                                                   int horizontalClearance,
                                                   int verticalClearance) {
            boolean[] passable = new boolean[rawWater.length];
            int[] waterPrefix = buildVolumePrefix(rawWater, sizeX, sizeY, sizeZ);
            int[] clearPrefix = buildVolumePrefix(rawClear, sizeX, sizeY, sizeZ);
            int footprintWidth = horizontalClearance * 2 + 1;
            int footprintArea = footprintWidth * footprintWidth;
            int requiredClearVolume = footprintArea * verticalClearance;
            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeY; y++) {
                    for (int z = 0; z < sizeZ; z++) {
                        int center = index(x, y, z, sizeX, sizeY);
                        if (!rawWater[center]) {
                            continue;
                        }

                        int minClearX = x - horizontalClearance;
                        int maxClearX = x + horizontalClearance + 1;
                        int maxClearY = y + verticalClearance;
                        int minClearZ = z - horizontalClearance;
                        int maxClearZ = z + horizontalClearance + 1;
                        if (minClearX < 0 || minClearZ < 0
                                || maxClearX > sizeX || maxClearY > sizeY || maxClearZ > sizeZ) {
                            continue;
                        }

                        int waterCount = volumeCount(
                                waterPrefix,
                                sizeX,
                                sizeY,
                                minClearX,
                                y,
                                minClearZ,
                                maxClearX,
                                y + 1,
                                maxClearZ
                        );
                        if (waterCount != footprintArea) {
                            continue;
                        }
                        int clearCount = volumeCount(
                                clearPrefix,
                                sizeX,
                                sizeY,
                                minClearX,
                                y,
                                minClearZ,
                                maxClearX,
                                maxClearY,
                                maxClearZ
                        );
                        passable[center] = clearCount == requiredClearVolume;
                    }
                }
            }
            return passable;
        }

        private static int[] buildVolumePrefix(boolean[] values, int sizeX, int sizeY, int sizeZ) {
            int prefixSizeX = sizeX + 1;
            int prefixSizeY = sizeY + 1;
            int[] prefix = new int[prefixSizeX * prefixSizeY * (sizeZ + 1)];
            for (int z = 1; z <= sizeZ; z++) {
                for (int y = 1; y <= sizeY; y++) {
                    for (int x = 1; x <= sizeX; x++) {
                        int cellPrefixIndex = prefixIndex(x, y, z, prefixSizeX, prefixSizeY);
                        int valueIndex = index(x - 1, y - 1, z - 1, sizeX, sizeY);
                        prefix[cellPrefixIndex] = (values[valueIndex] ? 1 : 0)
                                + prefix[prefixIndex(x - 1, y, z, prefixSizeX, prefixSizeY)]
                                + prefix[prefixIndex(x, y - 1, z, prefixSizeX, prefixSizeY)]
                                + prefix[prefixIndex(x, y, z - 1, prefixSizeX, prefixSizeY)]
                                - prefix[prefixIndex(x - 1, y - 1, z, prefixSizeX, prefixSizeY)]
                                - prefix[prefixIndex(x - 1, y, z - 1, prefixSizeX, prefixSizeY)]
                                - prefix[prefixIndex(x, y - 1, z - 1, prefixSizeX, prefixSizeY)]
                                + prefix[prefixIndex(x - 1, y - 1, z - 1, prefixSizeX, prefixSizeY)];
                    }
                }
            }
            return prefix;
        }

        private static int volumeCount(int[] prefix,
                                       int sizeX,
                                       int sizeY,
                                       int minX,
                                       int minY,
                                       int minZ,
                                       int maxX,
                                       int maxY,
                                       int maxZ) {
            int prefixSizeX = sizeX + 1;
            int prefixSizeY = sizeY + 1;
            return prefix[prefixIndex(maxX, maxY, maxZ, prefixSizeX, prefixSizeY)]
                    - prefix[prefixIndex(minX, maxY, maxZ, prefixSizeX, prefixSizeY)]
                    - prefix[prefixIndex(maxX, minY, maxZ, prefixSizeX, prefixSizeY)]
                    - prefix[prefixIndex(maxX, maxY, minZ, prefixSizeX, prefixSizeY)]
                    + prefix[prefixIndex(minX, minY, maxZ, prefixSizeX, prefixSizeY)]
                    + prefix[prefixIndex(minX, maxY, minZ, prefixSizeX, prefixSizeY)]
                    + prefix[prefixIndex(maxX, minY, minZ, prefixSizeX, prefixSizeY)]
                    - prefix[prefixIndex(minX, minY, minZ, prefixSizeX, prefixSizeY)];
        }

        private static int prefixIndex(int x, int y, int z, int sizeX, int sizeY) {
            return x + y * sizeX + z * sizeX * sizeY;
        }

        private static int nearestWaterIndex(boolean[] water, int sizeX, int sizeY, int sizeZ, int x, int y, int z, int maxRadius) {
            int center = index(x, y, z, sizeX, sizeY);
            if (water[center]) {
                return center;
            }

            int bestIndex = -1;
            int bestDistanceSqr = Integer.MAX_VALUE;
            int bestVerticalDistance = Integer.MAX_VALUE;
            int bestY = Integer.MIN_VALUE;
            int maxDistanceSqr = maxRadius * maxRadius;
            int minX = Math.max(0, x - maxRadius);
            int maxX = Math.min(sizeX - 1, x + maxRadius);
            int minY = Math.max(0, y - maxRadius);
            int maxY = Math.min(sizeY - 1, y + maxRadius);
            int minZ = Math.max(0, z - maxRadius);
            int maxZ = Math.min(sizeZ - 1, z + maxRadius);
            for (int ix = minX; ix <= maxX; ix++) {
                for (int iy = minY; iy <= maxY; iy++) {
                    for (int iz = minZ; iz <= maxZ; iz++) {
                        int idx = index(ix, iy, iz, sizeX, sizeY);
                        if (!water[idx]) {
                            continue;
                        }

                        int dx = ix - x;
                        int dy = iy - y;
                        int dz = iz - z;
                        int distanceSqr = dx * dx + dy * dy + dz * dz;
                        if (distanceSqr > maxDistanceSqr) {
                            continue;
                        }
                        int verticalDistance = Math.abs(dy);
                        if (distanceSqr < bestDistanceSqr
                                || (distanceSqr == bestDistanceSqr && verticalDistance < bestVerticalDistance)
                                || (distanceSqr == bestDistanceSqr
                                && verticalDistance == bestVerticalDistance
                                && iy > bestY)) {
                            bestIndex = idx;
                            bestDistanceSqr = distanceSqr;
                            bestVerticalDistance = verticalDistance;
                            bestY = iy;
                        }
                    }
                }
            }
            return bestIndex;
        }

        int index(int x, int y, int z) {
            return index(x, y, z, this.sizeX, this.sizeY);
        }

        private static int index(int x, int y, int z, int sizeX, int sizeY) {
            return x + y * sizeX + z * sizeX * sizeY;
        }

        boolean isWaterIndex(int index) {
            return index >= 0 && index < this.water.length && this.water[index];
        }

        List<Integer> neighbors(int index) {
            int x = index % sizeX;
            int y = (index / sizeX) % sizeY;
            int z = index / (sizeX * sizeY);
            List<Integer> neighbors = new ArrayList<>(18);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 2) {
                            continue;
                        }
                        int nx = x + dx;
                        int ny = y + dy;
                        int nz = z + dz;
                        if (nx < 0 || ny < 0 || nz < 0 || nx >= sizeX || ny >= sizeY || nz >= sizeZ) {
                            continue;
                        }
                        int neighbor = index(nx, ny, nz);
                        if (isWaterIndex(neighbor) && canTraverseDiagonal(x, y, z, dx, dy, dz)) {
                            neighbors.add(neighbor);
                        }
                    }
                }
            }
            return neighbors;
        }

        private boolean canTraverseDiagonal(int x, int y, int z, int dx, int dy, int dz) {
            if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= 1) {
                return true;
            }
            return (dx == 0 || isWaterIndex(index(x + dx, y, z)))
                    && (dy == 0 || isWaterIndex(index(x, y + dy, z)))
                    && (dz == 0 || isWaterIndex(index(x, y, z + dz)));
        }

        double stepCost(int from, int to) {
            int fx = from % sizeX;
            int fy = (from / sizeX) % sizeY;
            int fz = from / (sizeX * sizeY);
            int tx = to % sizeX;
            int ty = (to / sizeX) % sizeY;
            int tz = to / (sizeX * sizeY);
            int dx = Math.abs(tx - fx);
            int dy = Math.abs(ty - fy);
            int dz = Math.abs(tz - fz);
            double cost = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dy > 0) {
                cost += 1.25D;
            }
            if (isNearFloor(tx, ty, tz)) {
                cost += 3.0D;
            }
            return cost;
        }

        double heuristic(int from, int to) {
            int fx = from % sizeX;
            int fy = (from / sizeX) % sizeY;
            int fz = from / (sizeX * sizeY);
            int tx = to % sizeX;
            int ty = (to / sizeX) % sizeY;
            int tz = to / (sizeX * sizeY);
            int dx = tx - fx;
            int dy = ty - fy;
            int dz = tz - fz;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        List<Vec3> reconstructPath(Map<Integer, Integer> cameFrom, int current) {
            List<Vec3> path = new ArrayList<>();
            path.add(toWorld(current));
            while (cameFrom.containsKey(current)) {
                current = cameFrom.get(current);
                path.add(0, toWorld(current));
            }
            return path;
        }

        private Vec3 toWorld(int index) {
            int x = index % sizeX;
            int y = (index / sizeX) % sizeY;
            int z = index / (sizeX * sizeY);
            return new Vec3(minX + x + 0.5D, minY + y + 0.5D, minZ + z + 0.5D);
        }

        private boolean isNearFloor(int x, int y, int z) {
            if (y <= 0) {
                return true;
            }
            return !isWaterIndex(index(x, y - 1, z));
        }
    }
}
