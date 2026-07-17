package com.leon.saintsdragons.server.ai.pathfinding;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class DragonPathSearchDebug {
    private static final int MAX_CLOSED_NODES = 384;
    private static final int MAX_OPEN_NODES = 192;
    private static final int MAX_CANDIDATE_NODES = 192;

    private static volatile Set<UUID> activeDragons = Set.of();
    private static final ConcurrentHashMap<UUID, Snapshot> SNAPSHOTS = new ConcurrentHashMap<>();
    private static final AtomicLong NEXT_SEARCH_ID = new AtomicLong();

    private DragonPathSearchDebug() {
    }

    public static void setActiveDragons(Collection<UUID> dragonIds) {
        Set<UUID> updated = Set.copyOf(dragonIds);
        activeDragons = updated;
        SNAPSHOTS.keySet().removeIf(id -> !updated.contains(id));
    }

    public static boolean isActive(UUID dragonId) {
        return activeDragons.contains(dragonId);
    }

    public static @Nullable Snapshot getSnapshot(UUID dragonId) {
        return SNAPSHOTS.get(dragonId);
    }

    public static @Nullable NodeCollector beginNodeSearch(Mob mob, SearchType type, Vec3 target) {
        if (!isActive(mob.getUUID())) {
            return null;
        }
        return new NodeCollector(mob.getUUID(), type, mob.position(), target, mob.getBbWidth());
    }

    public static void publishGridSearch(UUID dragonId,
                                         SearchType type,
                                         Vec3 start,
                                         Vec3 target,
                                         Collection<Vec3> closedNodes,
                                         Collection<Vec3> openNodes,
                                         Collection<Vec3> candidateNodes,
                                         boolean reached,
                                         long startedNanos) {
        publish(
                dragonId,
                type,
                start,
                target,
                closedNodes,
                openNodes,
                candidateNodes,
                reached,
                startedNanos
        );
    }

    private static void publish(UUID dragonId,
                                SearchType type,
                                Vec3 start,
                                Vec3 target,
                                Collection<Vec3> closedNodes,
                                Collection<Vec3> openNodes,
                                Collection<Vec3> candidateNodes,
                                boolean reached,
                                long startedNanos) {
        if (!isActive(dragonId)) {
            return;
        }

        SNAPSHOTS.put(dragonId, new Snapshot(
                NEXT_SEARCH_ID.incrementAndGet(),
                type,
                start,
                target,
                sample(closedNodes, MAX_CLOSED_NODES),
                closedNodes.size(),
                sample(openNodes, MAX_OPEN_NODES),
                openNodes.size(),
                sample(candidateNodes, MAX_CANDIDATE_NODES),
                candidateNodes.size(),
                reached,
                Math.max(0L, (System.nanoTime() - startedNanos) / 1_000L)
        ));
    }

    private static List<Vec3> sample(Collection<Vec3> positions, int limit) {
        if (positions.isEmpty()) {
            return List.of();
        }
        List<Vec3> source = new ArrayList<>(positions);
        if (source.size() <= limit) {
            return List.copyOf(source);
        }

        List<Vec3> sampled = new ArrayList<>(limit);
        double step = (source.size() - 1.0D) / (limit - 1.0D);
        for (int i = 0; i < limit; i++) {
            sampled.add(source.get((int) Math.round(i * step)));
        }
        return List.copyOf(sampled);
    }

    public enum SearchType {
        GROUND,
        AIR,
        SWIM
    }

    public record Snapshot(long searchId,
                           SearchType type,
                           Vec3 start,
                           Vec3 target,
                           List<Vec3> closedNodes,
                           int closedNodeCount,
                           List<Vec3> openNodes,
                           int openNodeCount,
                           List<Vec3> candidateNodes,
                           int candidateNodeCount,
                           boolean reached,
                           long durationMicros) {
    }

    public static final class NodeCollector {
        private final UUID dragonId;
        private final SearchType type;
        private final Vec3 start;
        private final Vec3 target;
        private final double horizontalOffset;
        private final long startedNanos = System.nanoTime();
        private final Set<Node> nodes = new LinkedHashSet<>();

        private NodeCollector(UUID dragonId, SearchType type, Vec3 start, Vec3 target, float mobWidth) {
            this.dragonId = dragonId;
            this.type = type;
            this.start = start;
            this.target = target;
            this.horizontalOffset = type == SearchType.GROUND
                    ? Math.floor(mobWidth + 1.0F) * 0.5D
                    : 0.5D;
        }

        public void recordExpansion(Node current, Node[] neighbors, int neighborCount) {
            this.nodes.add(current);
            for (int i = 0; i < neighborCount; i++) {
                if (neighbors[i] != null) {
                    this.nodes.add(neighbors[i]);
                }
            }
        }

        public void complete(@Nullable Path path) {
            List<Vec3> closed = new ArrayList<>();
            List<Vec3> open = new ArrayList<>();
            List<Vec3> candidates = new ArrayList<>();
            for (Node node : this.nodes) {
                Vec3 position = new Vec3(
                        node.x + this.horizontalOffset,
                        node.y + (this.type == SearchType.GROUND ? 0.1D : 0.5D),
                        node.z + this.horizontalOffset
                );
                if (node.closed) {
                    closed.add(position);
                } else if (node.inOpenSet()) {
                    open.add(position);
                } else {
                    candidates.add(position);
                }
            }

            publish(
                    this.dragonId,
                    this.type,
                    this.start,
                    this.target,
                    closed,
                    open,
                    candidates,
                    path != null && path.canReach(),
                    this.startedNanos
            );
        }
    }
}
