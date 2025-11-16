package com.leon.saintsdragons.server.command;

import com.leon.saintsdragons.server.ai.navigation.pathfinding.AsyncPathfindingHelper;
import com.leon.saintsdragons.server.ai.navigation.pathfinding.AsyncPathfindingManager;
import com.leon.saintsdragons.server.ai.navigation.pathfinding.PathfindingBenchmark;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Debug command to test pathfinding performance.
 * Usage: /dragonbenchmark [quick|full|test|async|stats|smooth]
 */
public class DragonBenchmarkCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dragonbenchmark")
            .requires(source -> source.hasPermission(2)) // OP level 2 required
            .executes(DragonBenchmarkCommand::runQuick) // Default: quick demo
            .then(Commands.literal("quick")
                .executes(DragonBenchmarkCommand::runQuick))
            .then(Commands.literal("full")
                .executes(DragonBenchmarkCommand::runFull))
            .then(Commands.literal("test")
                .executes(DragonBenchmarkCommand::runTest))
            .then(Commands.literal("async")
                .executes(DragonBenchmarkCommand::testAsync))
            .then(Commands.literal("stats")
                .executes(DragonBenchmarkCommand::showStats))
            .then(Commands.literal("smooth")
                .executes(DragonBenchmarkCommand::testSmoothing))
        );
    }

    /**
     * Quick demonstration of binary heap.
     */
    private static int runQuick(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() ->
            Component.literal("§e[Dragon Pathfinding] Running quick demo..."), false);

        try {
            PathfindingBenchmark.quickDemo();
            context.getSource().sendSuccess(() ->
                Component.literal("§a[Dragon Pathfinding] Quick demo complete! Check server console for results."), false);
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§c[Dragon Pathfinding] Error: " + e.getMessage()));
            e.printStackTrace();
        }

        return 1;
    }

    /**
     * Full benchmark comparing performance.
     */
    private static int runFull(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() ->
            Component.literal("§e[Dragon Pathfinding] Running full benchmark (this may take a few seconds)..."), false);

        try {
            long startTime = System.currentTimeMillis();
            PathfindingBenchmark.runBenchmark();
            long duration = System.currentTimeMillis() - startTime;

            context.getSource().sendSuccess(() ->
                Component.literal(String.format("§a[Dragon Pathfinding] Benchmark complete in %dms! Check server console for detailed results.", duration)), false);
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§c[Dragon Pathfinding] Error: " + e.getMessage()));
            e.printStackTrace();
        }

        return 1;
    }

    /**
     * Test heap correctness.
     */
    private static int runTest(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() ->
            Component.literal("§e[Dragon Pathfinding] Running correctness test..."), false);

        try {
            boolean success = PathfindingBenchmark.testCorrectness();

            if (success) {
                context.getSource().sendSuccess(() ->
                    Component.literal("§a[Dragon Pathfinding] ✓ All tests passed!"), false);
            } else {
                context.getSource().sendFailure(
                    Component.literal("§c[Dragon Pathfinding] ✗ Tests failed! Check console for details."));
            }
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§c[Dragon Pathfinding] Error: " + e.getMessage()));
            e.printStackTrace();
        }

        return 1;
    }

    /**
     * Test async pathfinding system.
     */
    private static int testAsync(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getLevel() instanceof ServerLevel)) {
            source.sendFailure(Component.literal("§c[Dragon Pathfinding] Must be run in a server world!"));
            return 0;
        }

        ServerLevel serverLevel = (ServerLevel) source.getLevel();

        source.sendSuccess(() ->
            Component.literal("§e[Dragon Pathfinding] Testing async pathfinding..."), false);

        try {
            Vec3 sourcePos = source.getPosition();
            Vec3 targetPos = sourcePos.add(100, 20, 100); // 100 blocks away, 20 up

            source.sendSuccess(() ->
                Component.literal(String.format("§7Finding path from (%.1f, %.1f, %.1f) to (%.1f, %.1f, %.1f)...",
                    sourcePos.x, sourcePos.y, sourcePos.z,
                    targetPos.x, targetPos.y, targetPos.z)), false);

            long requestTime = System.currentTimeMillis();

            // Request async pathfinding
            AsyncPathfindingHelper.requestPath(
                serverLevel,
                sourcePos,
                targetPos,
                2, // Grid resolution
                result -> {
                    // This callback runs on background thread!
                    long callbackTime = System.currentTimeMillis();
                    long totalTime = callbackTime - requestTime;

                    // Schedule chat message on main thread
                    AsyncPathfindingHelper.scheduleOnMainThread(serverLevel, () -> {
                        if (result.isSuccess()) {
                            source.sendSuccess(() ->
                                Component.literal(String.format(
                                    "§a[Dragon Pathfinding] ✓ Path found! %d waypoints, computed in %dms (total %dms)",
                                    result.getPath().size(),
                                    result.getComputeTimeMs(),
                                    totalTime
                                )), false);
                        } else {
                            source.sendFailure(
                                Component.literal(String.format(
                                    "§c[Dragon Pathfinding] ✗ %s (took %dms)",
                                    result.getFailureReason(),
                                    totalTime
                                )));
                        }
                    });
                }
            );

            source.sendSuccess(() ->
                Component.literal("§a[Dragon Pathfinding] Request submitted! Pathfinding in background..."), false);

        } catch (Exception e) {
            source.sendFailure(
                Component.literal("§c[Dragon Pathfinding] Error: " + e.getMessage()));
            e.printStackTrace();
        }

        return 1;
    }

    /**
     * Show pathfinding statistics.
     */
    private static int showStats(CommandContext<CommandSourceStack> context) {
        try {
            AsyncPathfindingManager.PathfindingStats stats = AsyncPathfindingHelper.getStats();

            context.getSource().sendSuccess(() ->
                Component.literal("§e=== Dragon Pathfinding Stats ==="), false);
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("§7Active requests:    §f%d", stats.activeRequests)), false);
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("§7Completed requests: §f%d", stats.completedRequests)), false);
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("§7Active threads:     §f%d", stats.activeThreads)), false);
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("§7Queued requests:    §f%d", stats.queuedRequests)), false);

        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§c[Dragon Pathfinding] Error: " + e.getMessage()));
            e.printStackTrace();
        }

        return 1;
    }

    /**
     * Test path smoothing by comparing raw vs smoothed paths.
     */
    private static int testSmoothing(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getLevel() instanceof ServerLevel)) {
            source.sendFailure(Component.literal("§c[Dragon Pathfinding] Must be run in a server world!"));
            return 0;
        }

        ServerLevel serverLevel = (ServerLevel) source.getLevel();

        source.sendSuccess(() ->
            Component.literal("§e[Dragon Pathfinding] Testing path smoothing..."), false);

        try {
            Vec3 sourcePos = source.getPosition();
            Vec3 targetPos = sourcePos.add(100, 20, 100);

            source.sendSuccess(() ->
                Component.literal("§7Finding and comparing raw vs smoothed paths..."), false);

            long requestTime = System.currentTimeMillis();

            // Request WITHOUT smoothing
            AsyncPathfindingManager.getInstance()
                .requestPath(serverLevel, sourcePos, targetPos, 2, 100, false)
                .thenAccept(rawResult -> {
                    if (!rawResult.isSuccess()) {
                        AsyncPathfindingHelper.scheduleOnMainThread(serverLevel, () -> {
                            source.sendFailure(Component.literal("§c[Dragon Pathfinding] No path found - try from open air!"));
                        });
                        return;
                    }

                    int rawWaypoints = rawResult.getPath().size();

                    // Request WITH smoothing
                    AsyncPathfindingManager.getInstance()
                        .requestPath(serverLevel, sourcePos, targetPos, 2, 100, true)
                        .thenAccept(smoothResult -> {
                            long totalTime = System.currentTimeMillis() - requestTime;

                            AsyncPathfindingHelper.scheduleOnMainThread(serverLevel, () -> {
                                if (smoothResult.isSuccess()) {
                                    int smoothWaypoints = smoothResult.getPath().size();

                                    source.sendSuccess(() ->
                                        Component.literal("§a=== Path Smoothing Results ==="), false);
                                    source.sendSuccess(() ->
                                        Component.literal(String.format("§7Raw path:      §f%d waypoints", rawWaypoints)), false);
                                    source.sendSuccess(() ->
                                        Component.literal(String.format("§7Smoothed path: §f%d waypoints", smoothWaypoints)), false);

                                    if (smoothWaypoints > rawWaypoints) {
                                        int added = smoothWaypoints - rawWaypoints;
                                        source.sendSuccess(() ->
                                            Component.literal(String.format("§a✓ Added %d interpolated points for smooth curves", added)), false);
                                    } else if (smoothWaypoints < rawWaypoints) {
                                        int removed = rawWaypoints - smoothWaypoints;
                                        source.sendSuccess(() ->
                                            Component.literal(String.format("§a✓ Removed %d redundant waypoints via shortcuts", removed)), false);
                                    }

                                    source.sendSuccess(() ->
                                        Component.literal(String.format("§7Total time: §f%dms", totalTime)), false);
                                } else {
                                    source.sendFailure(Component.literal("§c[Dragon Pathfinding] Smoothing failed!"));
                                }
                            });
                        });
                });

            source.sendSuccess(() ->
                Component.literal("§a[Dragon Pathfinding] Computing both paths..."), false);

        } catch (Exception e) {
            source.sendFailure(
                Component.literal("§c[Dragon Pathfinding] Error: " + e.getMessage()));
            e.printStackTrace();
        }

        return 1;
    }
}
