package com.leon.saintsdragons.client.debug;

import com.leon.saintsdragons.common.network.MessageDragonBrainDebug;
import org.jetbrains.annotations.Nullable;

public final class DragonBrainDebugClient {
    private static final int SNAPSHOT_TIMEOUT_TICKS = 40;

    private static @Nullable MessageDragonBrainDebug snapshot;
    private static int ticksSinceUpdate;

    private DragonBrainDebugClient() {
    }

    public static void apply(MessageDragonBrainDebug message) {
        if (!message.active()) {
            clear();
            return;
        }
        snapshot = message;
        ticksSinceUpdate = 0;
    }

    public static @Nullable MessageDragonBrainDebug getSnapshot() {
        return snapshot;
    }

    public static void tick() {
        if (snapshot != null && ++ticksSinceUpdate > SNAPSHOT_TIMEOUT_TICKS) {
            clear();
        }
    }

    public static void clear() {
        snapshot = null;
        ticksSinceUpdate = 0;
    }
}
