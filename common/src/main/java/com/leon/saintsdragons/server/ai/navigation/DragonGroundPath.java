package com.leon.saintsdragons.server.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import java.util.List;

public final class DragonGroundPath extends Path {
    private final boolean frontier;

    public DragonGroundPath(List<Node> nodes, BlockPos target, boolean reached, boolean frontier) {
        super(nodes, target, reached);
        this.frontier = frontier;
    }

    public boolean endsAtSearchBoundary() {
        return frontier;
    }
}
