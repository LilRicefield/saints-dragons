package com.leon.saintsdragons.server.ai.pathfinding;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

public final class AsyncDragonPathfinder {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "SaintsDragons-Async-Pathfinder");
        thread.setDaemon(true);
        return thread;
    });

    private AsyncDragonPathfinder() {
    }

    public static void calculateFlyingPathAsync(Mob dragon, Vec3 target, Consumer<Path> callback) {
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
                        NodeEvaluator nodeEvaluator = new AsyncDragonFlyNodeEvaluator();
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
}
