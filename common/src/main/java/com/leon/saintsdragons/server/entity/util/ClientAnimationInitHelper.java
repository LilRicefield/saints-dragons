package com.leon.saintsdragons.server.entity.util;

public final class ClientAnimationInitHelper {
    public static final int DEFAULT_GRACE_TICKS = 8;

    private ClientAnimationInitHelper() {
    }

    public static int tickClientCounter(boolean clientSide, int currentTicks) {
        return tickClientCounter(clientSide, currentTicks, DEFAULT_GRACE_TICKS);
    }

    public static int tickClientCounter(boolean clientSide, int currentTicks, int graceTicks) {
        if (!clientSide) {
            return currentTicks;
        }
        if (currentTicks < 0) {
            return 0;
        }
        if (currentTicks >= graceTicks) {
            return currentTicks;
        }
        return currentTicks + 1;
    }

    public static boolean isReady(int currentTicks) {
        return isReady(currentTicks, DEFAULT_GRACE_TICKS);
    }

    public static boolean isReady(int currentTicks, int graceTicks) {
        return currentTicks >= graceTicks;
    }
}
