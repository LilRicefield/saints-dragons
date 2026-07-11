package com.leon.saintsdragons.server.ai.pathfinding;

import com.leon.saintsdragons.server.ai.navigation.PathFinderGround;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.PathNavigationRegion;
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

    public static void calculateGroundPathAsync(Mob dragon, Vec3 target, Consumer<Path> callback) {
        if (dragon.level().isClientSide) {
            return;
        }
        MinecraftServer server = dragon.getServer();
        if (server == null) {
            callback.accept(null);
            return;
        }

        BlockPos startPos = dragon.blockPosition();
        BlockPos targetPos = BlockPos.containing(target);
        int followRange = Math.max((int) dragon.getAttributeValue(Attributes.FOLLOW_RANGE), 128);
        int margin = Math.max(followRange, 32);
        BlockPos minPos = new BlockPos(
                Math.min(startPos.getX() - margin, targetPos.getX() - 16),
                Math.min(startPos.getY() - margin, targetPos.getY() - 16),
                Math.min(startPos.getZ() - margin, targetPos.getZ() - 16)
        );
        BlockPos maxPos = new BlockPos(
                Math.max(startPos.getX() + margin, targetPos.getX() + 16),
                Math.max(startPos.getY() + margin, targetPos.getY() + 16),
                Math.max(startPos.getZ() + margin, targetPos.getZ() + 16)
        );

        PathNavigationRegion snapshot;
        try {
            snapshot = new PathNavigationRegion(dragon.level(), minPos, maxPos);
        } catch (Exception exception) {
            callback.accept(null);
            return;
        }

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        NodeEvaluator nodeEvaluator = new DragonWalkNodeEvaluator();
                        nodeEvaluator.setCanPassDoors(true);
                        PathFinder pathFinder = new PathFinderGround(nodeEvaluator, 5000);
                        return pathFinder.findPath(
                                snapshot,
                                dragon,
                                Set.of(targetPos),
                                (float) followRange,
                                0,
                                1.0F
                        );
                    } catch (Exception exception) {
                        return null;
                    }
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
                    try {
                        NodeEvaluator nodeEvaluator = useSwarmClearance
                                ? new AsyncSwarmFlyNodeEvaluator()
                                : new AsyncDragonFlyNodeEvaluator();
                        nodeEvaluator.setCanPassDoors(true);
                        nodeEvaluator.setCanOpenDoors(true);
                        nodeEvaluator.setCanFloat(true);
                        PathFinder pathFinder = new PathFinder(nodeEvaluator, 5000);
                        return pathFinder.findPath(snapshot, dragon, Set.of(targetPos), (float) followRange, 1, 1.0f);
                    } catch (Exception exception) {
                        return null;
                    }
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

        CompletableFuture
                .supplyAsync(() -> findSwimPath(snapshot), EXECUTOR)
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

    private static List<Vec3> findSwimPath(SwimPathSnapshot snapshot) {
        int start = snapshot.index(snapshot.startX, snapshot.startY, snapshot.startZ);
        int goal = snapshot.index(snapshot.goalX, snapshot.goalY, snapshot.goalZ);
        if (!snapshot.isWaterIndex(start) || !snapshot.isWaterIndex(goal)) {
            return null;
        }

        PriorityQueue<SwimNode> open = new PriorityQueue<>(Comparator.comparingDouble(SwimNode::fScore));
        Map<Integer, Integer> cameFrom = new HashMap<>();
        Map<Integer, Double> gScore = new HashMap<>();
        gScore.put(start, 0.0D);
        open.add(new SwimNode(start, snapshot.heuristic(start, goal)));

        int visited = 0;
        while (!open.isEmpty() && visited++ < MAX_SWIM_ASTAR_VISITS) {
            SwimNode current = open.poll();
            if (current.index() == goal) {
                return snapshot.reconstructPath(cameFrom, current.index());
            }

            double currentScore = gScore.getOrDefault(current.index(), Double.POSITIVE_INFINITY);
            for (int neighbor : snapshot.neighbors(current.index())) {
                double tentativeScore = currentScore + snapshot.stepCost(current.index(), neighbor);
                if (tentativeScore >= gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    continue;
                }

                cameFrom.put(neighbor, current.index());
                gScore.put(neighbor, tentativeScore);
                open.add(new SwimNode(neighbor, tentativeScore + snapshot.heuristic(neighbor, goal)));
            }
        }
        return null;
    }

    private record SwimNode(int index, double fScore) {
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
                        rawWater[index(x, y, z, sizeX, sizeY)] = dragon.level().getFluidState(cursor).is(FluidTags.WATER);
                    }
                }
            }

            int horizontalClearance = Math.max(0, Mth.ceil(dragon.getBbWidth() * 0.5F - 0.25F));
            boolean[] water = buildClearanceMap(rawWater, sizeX, sizeY, sizeZ, horizontalClearance);

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

        private static boolean[] buildClearanceMap(boolean[] rawWater, int sizeX, int sizeY, int sizeZ, int horizontalClearance) {
            if (horizontalClearance <= 0) {
                return rawWater;
            }

            boolean[] passable = new boolean[rawWater.length];
            int clearanceSqr = horizontalClearance * horizontalClearance;
            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeY; y++) {
                    for (int z = 0; z < sizeZ; z++) {
                        int center = index(x, y, z, sizeX, sizeY);
                        if (!rawWater[center]) {
                            continue;
                        }

                        boolean clear = true;
                        for (int dx = -horizontalClearance; dx <= horizontalClearance && clear; dx++) {
                            for (int dz = -horizontalClearance; dz <= horizontalClearance; dz++) {
                                if (dx * dx + dz * dz > clearanceSqr) {
                                    continue;
                                }
                                int nx = x + dx;
                                int nz = z + dz;
                                if (nx < 0 || nz < 0 || nx >= sizeX || nz >= sizeZ
                                        || !rawWater[index(nx, y, nz, sizeX, sizeY)]) {
                                    clear = false;
                                    break;
                                }
                            }
                        }
                        passable[center] = clear;
                    }
                }
            }
            return passable;
        }

        private static int nearestWaterIndex(boolean[] water, int sizeX, int sizeY, int sizeZ, int x, int y, int z, int maxRadius) {
            int center = index(x, y, z, sizeX, sizeY);
            if (water[center]) {
                return center;
            }
            for (int radius = 1; radius <= maxRadius; radius++) {
                int minX = Math.max(0, x - radius);
                int maxX = Math.min(sizeX - 1, x + radius);
                int minY = Math.max(0, y - radius);
                int maxY = Math.min(sizeY - 1, y + radius);
                int minZ = Math.max(0, z - radius);
                int maxZ = Math.min(sizeZ - 1, z + radius);
                for (int ix = minX; ix <= maxX; ix++) {
                    for (int iy = minY; iy <= maxY; iy++) {
                        for (int iz = minZ; iz <= maxZ; iz++) {
                            int idx = index(ix, iy, iz, sizeX, sizeY);
                            if (water[idx]) {
                                return idx;
                            }
                        }
                    }
                }
            }
            return -1;
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
                        if (isWaterIndex(neighbor)) {
                            neighbors.add(neighbor);
                        }
                    }
                }
            }
            return neighbors;
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
