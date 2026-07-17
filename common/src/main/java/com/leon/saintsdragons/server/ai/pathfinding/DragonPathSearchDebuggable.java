package com.leon.saintsdragons.server.ai.pathfinding;

import org.jetbrains.annotations.Nullable;

public interface DragonPathSearchDebuggable {
    void setPathSearchDebugCollector(@Nullable DragonPathSearchDebug.NodeCollector collector);
}
