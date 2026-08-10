package com.leon.saintsdragons.server.ai.navigation.async;

import com.leon.saintsdragons.server.ai.navigation.PathNavigateGround;
import com.leon.saintsdragons.server.ai.pathfinding.DragonPathSearchDebug;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AsyncDragonPathfinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncDragonPathfinder.class);
    private static final int MAX_SWIM_ASTAR_VISITS = 50000;
    private static final int MAX_ASYNC_GROUND_ROUTE = 128;
    private static final int MAX_ASYNC_FLIGHT_ROUTE = 128;
    private static final int WORKER_COUNT = 2;
    private static final int MAX_QUEUED_PATHS = 32;
    private static final AtomicInteger WORKER_NUMBER = new AtomicInteger();
    private static final Map<MinecraftServer, Set<PathRequest>> ACTIVE_REQUESTS =
            new ConcurrentHashMap<>();
    private static final Set<MinecraftServer> STOPPING_SERVERS = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>())
    );
    private static final Semaphore WORKER_CAPACITY = new Semaphore(WORKER_COUNT + MAX_QUEUED_PATHS);
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            WORKER_COUNT,
            WORKER_COUNT,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_QUEUED_PATHS),
            runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "SaintsDragons-Async-Pathfinder-" + WORKER_NUMBER.incrementAndGet()
                );
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy()
    );

    private AsyncDragonPathfinder() {
    }

    /**
     * Cancels every queued or running path request owned by a server that is shutting down.
     * The shared daemon executor intentionally survives integrated-server restarts.
     */
    public static void onServerStopping(MinecraftServer server) {
        STOPPING_SERVERS.add(server);
        Set<PathRequest> requests = ACTIVE_REQUESTS.remove(server);
        if (requests != null) {
            for (PathRequest request : requests) {
                request.cancel(true);
            }
        }
        EXECUTOR.purge();
    }

    public static Future<?> calculateFlyingPathAsync(Mob dragon, Vec3 target, Consumer<Path> callback) {
        return calculateFlyingPathAsyncInternal(dragon, target, callback);
    }

    public static Future<?> calculateGroundPathAsync(Mob dragon, Vec3 target, Consumer<Path> callback) {
        MinecraftServer server = dragon.getServer();
        if (server != null && !server.isSameThread()) {
            return rejectOffThreadRequest(server, dragon, "ground path request", callback);
        }
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
        MinecraftServer server = dragon.getServer();
        if (server == null) {
            return CompletableFuture.completedFuture(null);
        }
        if (!server.isSameThread()) {
            return rejectOffThreadRequest(server, dragon, "ground path request", callback);
        }
        if (dragon.level().isClientSide) {
            return CompletableFuture.completedFuture(null);
        }

        if (!(dragon.level() instanceof ServerLevel serverLevel)) {
            return completeOnServer(server, dragon, callback, null);
        }

        String dragonId = dragon.getStringUUID();
        PathRequest request = tryNewWorkerRequest(server);
        if (request == null) {
            LOGGER.warn("Rejected ground path calculation for {} because path capacity is full", dragonId);
            return completeOnServer(server, dragon, callback, null);
        }
        if (request.isCancelled()) {
            return request;
        }

        try {
            Vec3 origin = dragon.position();
            BlockPos startPos = dragon.blockPosition();
            int followRange = Math.max((int) dragon.getAttributeValue(Attributes.FOLLOW_RANGE), 128);
            double maximumRoute = Math.min(followRange, MAX_ASYNC_GROUND_ROUTE);
            Vec3 route = target.subtract(origin);
            Vec3 planningTarget = route.length() <= maximumRoute
                    ? target
                    : origin.add(route.normalize().scale(maximumRoute));
            BlockPos planningTargetPos = BlockPos.containing(planningTarget);
            int footprintSize = Math.max(1, Mth.floor(dragon.getBbWidth() + 1.0F));
            int footprintOffset = footprintSize / 2;
            BlockPos startNode = new BlockPos(
                    startPos.getX() - footprintOffset,
                    startPos.getY(),
                    startPos.getZ() - footprintOffset
            );
            BlockPos planningTargetNode = new BlockPos(
                    planningTargetPos.getX() - footprintOffset,
                    planningTargetPos.getY(),
                    planningTargetPos.getZ() - footprintOffset
            );
            int maxStepUp = Mth.floor(Math.max(1.0F, dragon.maxUpStep()));
            int maxDropDown = Math.max(1, dragon.getMaxFallDistance());
            int searchRange = Mth.clamp(
                    Mth.ceil(origin.distanceTo(planningTarget)) + 24,
                    32,
                    (int) maximumRoute
            );
            int horizontalMargin = Math.max(8, Mth.ceil(dragon.getBbWidth()) + 4);
            int verticalMargin = Math.max(8, Mth.ceil(dragon.getBbHeight()) + maxDropDown + 2);
            BlockPos minNode = new BlockPos(
                    Math.min(startNode.getX(), planningTargetNode.getX()) - horizontalMargin,
                    Math.max(serverLevel.getMinBuildHeight(),
                            Math.min(startNode.getY(), planningTargetNode.getY()) - verticalMargin),
                    Math.min(startNode.getZ(), planningTargetNode.getZ()) - horizontalMargin
            );
            BlockPos maxNode = new BlockPos(
                    Math.max(startNode.getX(), planningTargetNode.getX()) + horizontalMargin,
                    Math.min(serverLevel.getMaxBuildHeight() - 1,
                            Math.max(startNode.getY(), planningTargetNode.getY()) + verticalMargin),
                    Math.max(startNode.getZ(), planningTargetNode.getZ()) + horizontalMargin
            );
            int horizontalClearance = Mth.ceil(dragon.getBbWidth()) + 2;
            int verticalClearance = Mth.ceil(dragon.getBbHeight()) + 2;
            BlockPos captureMin = minNode.offset(-2, -1, -2);
            BlockPos captureMax = maxNode.offset(horizontalClearance, verticalClearance, horizontalClearance);
            ImmutableBlockSnapshot snapshot = ImmutableBlockSnapshot.capture(serverLevel, captureMin, captureMax);
            AABB relativeBounds = dragon.getBoundingBox().move(-origin.x, -origin.y, -origin.z);
            boolean canPassThroughTrees = dragon instanceof DragonEntity dragonEntity
                    && DragonDestructionManager.canApplyPassiveTreeDestruction(serverLevel, dragonEntity);
            float configuredWaterMalus = dragon.getPathfindingMalus(BlockPathTypes.WATER);
            boolean waterEntryAllowed = dragon.isInWaterOrBubble()
                    || dragon.getNavigation() instanceof PathNavigateGround navigation
                    && navigation.isWaterEntryAllowed();
            boolean allowWater = !avoidWater
                    && waterEntryAllowed
                    && (configuredWaterMalus >= 0.0F || dragon.isInWaterOrBubble());
            Map<BlockPathTypes, Float> pathMalus = new EnumMap<>(BlockPathTypes.class);
            for (BlockPathTypes pathType : BlockPathTypes.values()) {
                pathMalus.put(pathType, dragon.getPathfindingMalus(pathType));
            }
            DragonPathSearchDebug.SearchSession debugSession =
                    DragonPathSearchDebug.beginGridSearch(dragon.getUUID());

            return submitWorker(
                    request,
                    server,
                    dragon,
                    "ground path calculation for " + dragonId,
                    cancelled -> new AsyncGroundPathSearch(
                            snapshot,
                            origin,
                            planningTarget,
                            target,
                            relativeBounds,
                            minNode,
                            maxNode,
                            footprintSize,
                            maxStepUp,
                            maxDropDown,
                            goalAccuracy,
                            searchRange,
                            allowWater,
                            configuredWaterMalus,
                            canPassThroughTrees,
                            pathMalus,
                            cancelled
                    ).findPath(debugSession),
                    callback
            );
        } catch (Exception exception) {
            LOGGER.warn("Failed to capture immutable ground path region for {}", dragonId, exception);
            return completeOnServer(request, server, dragon, callback, null);
        }
    }

    public static Future<?> calculateSwarmFlyingPathAsync(Mob swarm, Vec3 target, Consumer<Path> callback) {
        return calculateFlyingPathAsyncInternal(swarm, target, callback);
    }

    private static Future<?> calculateFlyingPathAsyncInternal(Mob dragon,
                                                               Vec3 target,
                                                               Consumer<Path> callback) {
        MinecraftServer server = dragon.getServer();
        if (server == null) {
            return CompletableFuture.completedFuture(null);
        }
        if (!server.isSameThread()) {
            return rejectOffThreadRequest(server, dragon, "air path request", callback);
        }
        if (dragon.level().isClientSide) {
            return CompletableFuture.completedFuture(null);
        }

        if (!(dragon.level() instanceof ServerLevel serverLevel)) {
            return completeOnServer(server, dragon, callback, null);
        }

        String dragonId = dragon.getStringUUID();
        PathRequest request = tryNewWorkerRequest(server);
        if (request == null) {
            LOGGER.warn("Rejected air path calculation for {} because path capacity is full", dragonId);
            return completeOnServer(server, dragon, callback, null);
        }
        if (request.isCancelled()) {
            return request;
        }
        try {
            Vec3 origin = dragon.position();
            BlockPos startPos = dragon.blockPosition();
            int followRange = Math.max((int) dragon.getAttributeValue(Attributes.FOLLOW_RANGE), 128);
            double maximumRoute = Math.min(followRange, MAX_ASYNC_FLIGHT_ROUTE);
            Vec3 route = target.subtract(origin);
            Vec3 planningTarget = route.length() <= maximumRoute
                    ? target
                    : origin.add(route.normalize().scale(maximumRoute));
            BlockPos planningTargetPos = BlockPos.containing(planningTarget);
            int horizontalMargin = Math.max(12, Mth.ceil(dragon.getBbWidth()) + 6);
            int verticalMargin = Math.max(8, Mth.ceil(dragon.getBbHeight()) + 4);
            BlockPos minNode = new BlockPos(
                    Math.min(startPos.getX(), planningTargetPos.getX()) - horizontalMargin,
                    Math.max(serverLevel.getMinBuildHeight(),
                            Math.min(startPos.getY(), planningTargetPos.getY()) - verticalMargin),
                    Math.min(startPos.getZ(), planningTargetPos.getZ()) - horizontalMargin
            );
            BlockPos maxNode = new BlockPos(
                    Math.max(startPos.getX(), planningTargetPos.getX()) + horizontalMargin,
                    Math.min(serverLevel.getMaxBuildHeight() - 1,
                            Math.max(startPos.getY(), planningTargetPos.getY()) + verticalMargin),
                    Math.max(startPos.getZ(), planningTargetPos.getZ()) + horizontalMargin
            );
            int horizontalClearance = Mth.ceil(dragon.getBbWidth() * 0.5D) + 2;
            int verticalClearance = Mth.ceil(dragon.getBbHeight()) + 2;
            BlockPos captureMin = minNode.offset(-horizontalClearance, -2, -horizontalClearance);
            BlockPos captureMax = maxNode.offset(horizontalClearance, verticalClearance, horizontalClearance);
            ImmutableBlockSnapshot snapshot = ImmutableBlockSnapshot.capture(serverLevel, captureMin, captureMax);
            net.minecraft.world.phys.AABB relativeBounds = dragon.getBoundingBox().move(
                    -origin.x,
                    -origin.y,
                    -origin.z
            );
            DragonPathSearchDebug.SearchSession debugSession =
                    DragonPathSearchDebug.beginGridSearch(dragon.getUUID());

            return submitWorker(
                    request,
                    server,
                    dragon,
                    "air path calculation for " + dragonId,
                    cancelled -> new AsyncFlightPathSearch(
                            snapshot,
                            origin,
                            planningTarget,
                            target,
                            relativeBounds,
                            minNode,
                            maxNode,
                            cancelled
                    ).findPath(debugSession),
                    callback
            );
        } catch (Exception exception) {
            LOGGER.warn("Failed to capture immutable air path region for {}", dragonId, exception);
            return completeOnServer(request, server, dragon, callback, null);
        }
    }

    public static Future<?> calculateSwimPathAsync(Mob dragon, Vec3 target, Consumer<List<Vec3>> callback) {
        MinecraftServer server = dragon.getServer();
        if (server == null) {
            return CompletableFuture.completedFuture(null);
        }
        if (!server.isSameThread()) {
            return rejectOffThreadRequest(server, dragon, "swim path request", callback);
        }
        if (dragon.level().isClientSide) {
            return CompletableFuture.completedFuture(null);
        }

        String dragonId = dragon.getStringUUID();
        PathRequest request = tryNewWorkerRequest(server);
        if (request == null) {
            LOGGER.warn("Rejected swim path calculation for {} because path capacity is full", dragonId);
            return completeOnServer(server, dragon, callback, null);
        }
        if (request.isCancelled()) {
            return request;
        }

        SwimPathSnapshot snapshot;
        try {
            snapshot = SwimPathSnapshot.capture(dragon, target);
        } catch (Exception exception) {
            LOGGER.warn("Failed to capture swim path region for {}", dragonId, exception);
            return completeOnServer(request, server, dragon, callback, null);
        }

        DragonPathSearchDebug.SearchSession debugSession =
                DragonPathSearchDebug.beginGridSearch(dragon.getUUID());
        return submitWorker(
                request,
                server,
                dragon,
                "swim path calculation for " + dragonId,
                cancelled -> findSwimPath(snapshot, debugSession, cancelled),
                callback
        );
    }

    private static List<Vec3> findSwimPath(SwimPathSnapshot snapshot,
                                           DragonPathSearchDebug.SearchSession debugSession,
                                           BooleanSupplier cancelled) {
        long startedNanos = System.nanoTime();
        if (!snapshot.prepare(cancelled)) {
            return null;
        }
        int start = snapshot.index(snapshot.startX, snapshot.startY, snapshot.startZ);
        int goal = snapshot.index(snapshot.goalX, snapshot.goalY, snapshot.goalZ);
        if (!snapshot.isWaterIndex(start) || !snapshot.isWaterIndex(goal)) {
            if (debugSession != null) {
                DragonPathSearchDebug.publishGridSearch(
                        debugSession,
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
        Set<Integer> closed = debugSession == null ? new HashSet<>() : new LinkedHashSet<>();
        gScore.put(start, 0.0D);
        open.add(new SwimNode(start, 0.0D, snapshot.heuristic(start, goal)));

        List<Vec3> path = null;
        int visited = 0;
        while (!open.isEmpty() && visited < MAX_SWIM_ASTAR_VISITS) {
            if (cancelled.getAsBoolean()) {
                return null;
            }
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
                if (cancelled.getAsBoolean()) {
                    return null;
                }
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

        if (debugSession != null) {
            List<Vec3> closedPositions = closed.stream().map(snapshot::toWorld).toList();
            List<Vec3> openPositions = gScore.keySet().stream()
                    .filter(index -> !closed.contains(index))
                    .map(snapshot::toWorld)
                    .toList();
            DragonPathSearchDebug.publishGridSearch(
                    debugSession,
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

    private static <T> Future<?> submitWorker(PathRequest request,
                                               MinecraftServer server,
                                               Mob dragon,
                                               String operation,
                                               CancellableComputation<T> computation,
                                               Consumer<T> callback) {
        if (request.isCancelled()) {
            return request;
        }
        int requestTick = server.getTickCount();
        try {
            Future<?> worker = EXECUTOR.submit(() -> {
                if (request.isCancelled()) {
                    request.complete();
                    return;
                }

                T result = null;
                try {
                    result = computation.calculate(() -> request.isCancelled()
                            || Thread.currentThread().isInterrupted());
                } catch (Exception exception) {
                    if (!request.isCancelled()) {
                        LOGGER.warn("Async {} failed", operation, exception);
                    }
                } catch (Error error) {
                    request.complete();
                    throw error;
                }

                if (request.isCancelled()) {
                    request.complete();
                    return;
                }
                T resolvedResult = result;
                try {
                    server.tell(new TickTask(requestTick, () -> applyResult(
                            request,
                            server,
                            dragon,
                            operation,
                            callback,
                            resolvedResult
                    )));
                } catch (RejectedExecutionException exception) {
                    request.complete();
                }
            });
            request.attach(worker);
        } catch (RejectedExecutionException exception) {
            LOGGER.warn("Rejected {} because the bounded path queue is full", operation);
            try {
                server.tell(new TickTask(requestTick, () -> applyResult(
                        request,
                        server,
                        dragon,
                        operation,
                        callback,
                        null
                )));
            } catch (RejectedExecutionException schedulingException) {
                request.complete();
            }
        }
        return request;
    }

    private static <T> Future<?> completeOnServer(MinecraftServer server,
                                                   Mob dragon,
                                                   Consumer<T> callback,
                                                   T result) {
        PathRequest request = newRequest(server);
        return completeOnServer(request, server, dragon, callback, result);
    }

    private static <T> Future<?> completeOnServer(PathRequest request,
                                                   MinecraftServer server,
                                                   Mob dragon,
                                                   Consumer<T> callback,
                                                   T result) {
        if (request.isCancelled()) {
            return request;
        }
        int requestTick = server.getTickCount();
        try {
            server.tell(new TickTask(requestTick, () -> applyResult(
                    request,
                    server,
                    dragon,
                    "path completion",
                    callback,
                    result
            )));
        } catch (RejectedExecutionException exception) {
            request.complete();
        }
        return request;
    }

    private static <T> Future<?> rejectOffThreadRequest(MinecraftServer server,
                                                         Mob dragon,
                                                         String operation,
                                                         Consumer<T> callback) {
        LOGGER.error("Rejected {} because async path requests must originate on the server thread", operation);
        PathRequest request = newRequest(server);
        if (request.isCancelled()) {
            return request;
        }
        try {
            server.execute(() -> applyResult(request, server, dragon, operation, callback, null));
        } catch (RejectedExecutionException exception) {
            request.complete();
        }
        return request;
    }

    private static PathRequest newRequest(MinecraftServer server) {
        return registerRequest(new PathRequest(server));
    }

    private static PathRequest tryNewWorkerRequest(MinecraftServer server) {
        if (!WORKER_CAPACITY.tryAcquire()) {
            return null;
        }
        return registerRequest(new PathRequest(server, WORKER_CAPACITY::release));
    }

    private static PathRequest registerRequest(PathRequest request) {
        MinecraftServer server = request.server;
        if (isServerStopping(server)) {
            request.cancel(false);
            return request;
        }

        ACTIVE_REQUESTS.compute(server, (ignored, requests) -> {
            Set<PathRequest> active = requests == null ? ConcurrentHashMap.newKeySet() : requests;
            active.add(request);
            return active;
        });

        // Close the race where shutdown begins between the first check and registration.
        if (isServerStopping(server)) {
            request.cancel(true);
        }
        return request;
    }

    private static boolean isServerStopping(MinecraftServer server) {
        return STOPPING_SERVERS.contains(server)
                || (server.isSameThread() && server.isStopped());
    }

    private static void unregister(PathRequest request) {
        ACTIVE_REQUESTS.computeIfPresent(request.server, (ignored, requests) -> {
            requests.remove(request);
            return requests.isEmpty() ? null : requests;
        });
    }

    private static <T> void applyResult(PathRequest request,
                                        MinecraftServer server,
                                        Mob dragon,
                                        String operation,
                                        Consumer<T> callback,
                                        T result) {
        try {
            if (!request.isCancelled()
                    && !server.isStopped()
                    && !dragon.isRemoved()
                    && dragon.isAlive()) {
                callback.accept(result);
            }
        } catch (Exception exception) {
            LOGGER.error("Callback for {} failed", operation, exception);
        } finally {
            request.complete();
        }
    }

    @FunctionalInterface
    private interface CancellableComputation<T> {
        T calculate(BooleanSupplier cancelled);
    }

    private static final class PathRequest implements Future<Void> {
        private final MinecraftServer server;
        private final Runnable releaseCapacity;
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicBoolean resourcesReleased = new AtomicBoolean();
        private final CompletableFuture<Void> completion = new CompletableFuture<>();
        private volatile Future<?> worker;

        private PathRequest(MinecraftServer server) {
            this(server, () -> {
            });
        }

        private PathRequest(MinecraftServer server, Runnable releaseCapacity) {
            this.server = server;
            this.releaseCapacity = releaseCapacity;
        }

        void attach(Future<?> worker) {
            this.worker = worker;
            if (this.cancelled.get()) {
                cancelWorker(worker, true);
            }
        }

        synchronized void complete() {
            if (this.completion.complete(null)) {
                this.releaseResources();
            }
        }

        @Override
        public synchronized boolean cancel(boolean mayInterruptIfRunning) {
            if (this.completion.isDone() || !this.cancelled.compareAndSet(false, true)) {
                return false;
            }
            Future<?> activeWorker = this.worker;
            if (activeWorker != null) {
                cancelWorker(activeWorker, mayInterruptIfRunning);
            }
            this.completion.cancel(false);
            this.releaseResources();
            return true;
        }

        private void releaseResources() {
            if (this.resourcesReleased.compareAndSet(false, true)) {
                unregister(this);
                this.releaseCapacity.run();
            }
        }

        private static void cancelWorker(Future<?> worker, boolean mayInterruptIfRunning) {
            worker.cancel(mayInterruptIfRunning);
            if (worker instanceof Runnable queuedTask) {
                EXECUTOR.remove(queuedTask);
            }
        }

        @Override
        public boolean isCancelled() {
            return this.cancelled.get();
        }

        @Override
        public boolean isDone() {
            return this.completion.isDone();
        }

        @Override
        public Void get() throws ExecutionException, InterruptedException {
            return this.completion.get();
        }

        @Override
        public Void get(long timeout, TimeUnit unit)
                throws ExecutionException, InterruptedException, TimeoutException {
            return this.completion.get(timeout, unit);
        }
    }

    private record SwimNode(int index, double gScore, double fScore) {
    }

    private static final class SwimPathSnapshot {
        private static final int HORIZONTAL_PADDING = 16;
        private static final int VERTICAL_PADDING = 8;
        private static final int MAX_HORIZONTAL_SPAN = 96;
        private static final int MAX_VERTICAL_SPAN = 32;
        private static final int NEAREST_START_WATER_RADIUS = 6;
        private static final int NEAREST_GOAL_WATER_RADIUS = 32;

        private final ImmutableBlockSnapshot blocks;
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final int horizontalClearance;
        private final int verticalClearance;
        private boolean[] water;
        private int startX;
        private int startY;
        private int startZ;
        private int goalX;
        private int goalY;
        private int goalZ;

        private SwimPathSnapshot(ImmutableBlockSnapshot blocks,
                                 int minX,
                                 int minY,
                                 int minZ,
                                 int sizeX,
                                 int sizeY,
                                 int sizeZ,
                                 int startX,
                                 int startY,
                                 int startZ,
                                 int goalX,
                                 int goalY,
                                 int goalZ,
                                 int horizontalClearance,
                                 int verticalClearance) {
            this.blocks = blocks;
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
            this.horizontalClearance = horizontalClearance;
            this.verticalClearance = verticalClearance;
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
            int horizontalClearance = Math.max(0, Mth.ceil(dragon.getBbWidth() * 0.5F - 0.5F));
            int verticalClearance = Math.max(1, Mth.ceil(dragon.getBbHeight() + 0.5F));
            int startX = Mth.clamp(startPos.getX() - minX, 0, sizeX - 1);
            int startY = Mth.clamp(startPos.getY() - minY, 0, sizeY - 1);
            int startZ = Mth.clamp(startPos.getZ() - minZ, 0, sizeZ - 1);
            int goalX = Mth.clamp(goalPos.getX() - minX, 0, sizeX - 1);
            int goalY = Mth.clamp(goalPos.getY() - minY, 0, sizeY - 1);
            int goalZ = Mth.clamp(goalPos.getZ() - minZ, 0, sizeZ - 1);
            if (!(dragon.level() instanceof ServerLevel serverLevel)) {
                throw new IllegalStateException("Swim path snapshots require a server level");
            }
            BlockPos captureMin = new BlockPos(
                    minX - horizontalClearance - 1,
                    minY - 1,
                    minZ - horizontalClearance - 1
            );
            BlockPos captureMax = new BlockPos(
                    maxX + horizontalClearance + 1,
                    maxY + verticalClearance + 1,
                    maxZ + horizontalClearance + 1
            );
            ImmutableBlockSnapshot blocks = ImmutableBlockSnapshot.capture(serverLevel, captureMin, captureMax);

            return new SwimPathSnapshot(
                    blocks,
                    minX,
                    minY,
                    minZ,
                    sizeX,
                    sizeY,
                    sizeZ,
                    startX,
                    startY,
                    startZ,
                    goalX,
                    goalY,
                    goalZ,
                    horizontalClearance,
                    verticalClearance
            );
        }

        boolean prepare(BooleanSupplier cancelled) {
            boolean[] rawWater = new boolean[this.sizeX * this.sizeY * this.sizeZ];
            boolean[] rawClear = new boolean[rawWater.length];
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int x = 0; x < this.sizeX; x++) {
                for (int z = 0; z < this.sizeZ; z++) {
                    for (int y = 0; y < this.sizeY; y++) {
                        if (cancelled.getAsBoolean()) {
                            return false;
                        }
                        cursor.set(this.minX + x, this.minY + y, this.minZ + z);
                        int index = index(x, y, z, this.sizeX, this.sizeY);
                        BlockState state = this.blocks.getBlockState(cursor);
                        rawWater[index] = state.getFluidState().is(FluidTags.WATER);
                        rawClear[index] = this.blocks.collisionBoxes(cursor).isEmpty();
                    }
                }
            }

            this.water = buildClearanceMap(
                    rawWater,
                    rawClear,
                    this.sizeX,
                    this.sizeY,
                    this.sizeZ,
                    this.horizontalClearance,
                    this.verticalClearance
            );
            if (cancelled.getAsBoolean()) {
                return false;
            }

            int nearestStart = nearestWaterIndex(
                    this.water,
                    this.sizeX,
                    this.sizeY,
                    this.sizeZ,
                    this.startX,
                    this.startY,
                    this.startZ,
                    NEAREST_START_WATER_RADIUS
            );
            if (nearestStart >= 0) {
                this.startX = nearestStart % this.sizeX;
                this.startY = (nearestStart / this.sizeX) % this.sizeY;
                this.startZ = nearestStart / (this.sizeX * this.sizeY);
            }

            int nearestGoal = nearestWaterIndex(
                    this.water,
                    this.sizeX,
                    this.sizeY,
                    this.sizeZ,
                    this.goalX,
                    this.goalY,
                    this.goalZ,
                    NEAREST_GOAL_WATER_RADIUS
            );
            if (nearestGoal >= 0) {
                this.goalX = nearestGoal % this.sizeX;
                this.goalY = (nearestGoal / this.sizeX) % this.sizeY;
                this.goalZ = nearestGoal / (this.sizeX * this.sizeY);
            }
            return true;
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
