package com.leon.saintsdragons.server.ai.navigation;

import com.leon.saintsdragons.server.ai.pathfinding.DragonPathSearchDebug;
import com.leon.saintsdragons.server.ai.pathfinding.DragonPathSearchDebuggable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PathFinderGround extends PathFinder {
    private final NodeEvaluator dragonNodeEvaluator;

    public PathFinderGround(NodeEvaluator processor, int maxVisitedNodes) {
        super(processor, maxVisitedNodes);
        this.dragonNodeEvaluator = processor;
    }

    @Nullable
    @Override
    public Path findPath(@Nonnull PathNavigationRegion regionIn, @Nonnull Mob mob, @Nonnull Set<BlockPos> targetPositions, float maxRange, int accuracy, float searchDepthMultiplier) {
        BlockPos target = targetPositions.stream().findFirst().orElse(mob.blockPosition());
        DragonPathSearchDebug.NodeCollector debugCollector = DragonPathSearchDebug.beginNodeSearch(
                mob,
                DragonPathSearchDebug.SearchType.GROUND,
                Vec3.atCenterOf(target)
        );
        if (this.dragonNodeEvaluator instanceof DragonPathSearchDebuggable debuggable) {
            debuggable.setPathSearchDebugCollector(debugCollector);
        }

        Path path = null;
        try {
            path = super.findPath(regionIn, mob, targetPositions, maxRange, accuracy, searchDepthMultiplier);
            return path == null ? null : new DragonPatchedPath(path);
        } finally {
            if (debugCollector != null) {
                debugCollector.complete(path);
            }
            if (this.dragonNodeEvaluator instanceof DragonPathSearchDebuggable debuggable) {
                debuggable.setPathSearchDebugCollector(null);
            }
        }
    }

    static class DragonPatchedPath extends Path {
        public DragonPatchedPath(Path original) {
            super(copyPathPoints(original), original.getTarget(), original.canReach());
        }

        @Override
        public @NotNull Vec3 getEntityPosAtNode(@Nonnull Entity entity, int index) {
            Node point = this.getNode(index);
            // Properly center the entity based on its actual bounding box width
            double d0 = point.x + Mth.floor(entity.getBbWidth() + 1.0F) * 0.5D;
            double d1 = point.y;
            double d2 = point.z + Mth.floor(entity.getBbWidth() + 1.0F) * 0.5D;
            return new Vec3(d0, d1, d2);
        }

        private static List<Node> copyPathPoints(Path original) {
            List<Node> points = new ArrayList<>();
            for (int i = 0; i < original.getNodeCount(); i++) {
                points.add(original.getNode(i));
            }
            return points;
        }
    }
}
