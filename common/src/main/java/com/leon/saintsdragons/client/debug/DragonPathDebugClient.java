package com.leon.saintsdragons.client.debug;

import com.leon.saintsdragons.common.network.MessageDragonPathDebug;
import org.jetbrains.annotations.Nullable;

public final class DragonPathDebugClient {
    private static final int SNAPSHOT_TIMEOUT_TICKS = 40;

    private static @Nullable MessageDragonPathDebug snapshot;
    private static int ticksSinceUpdate;

    private DragonPathDebugClient() {
    }

    public static void apply(MessageDragonPathDebug message) {
        if (!message.active()) {
            clear();
            return;
        }
        snapshot = message;
        ticksSinceUpdate = 0;
    }

    public static @Nullable MessageDragonPathDebug getSnapshot() {
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
