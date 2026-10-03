package com.leon.saintsdragons.client.debug;

import com.leon.saintsdragons.common.network.MessageDragonVisionDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

public final class DragonVisionDebugClient {
    private static @Nullable MessageDragonVisionDebug snapshot;
    private static @Nullable ClientLevel snapshotLevel;
    private static int age;

    private DragonVisionDebugClient() {}

    public static void apply(MessageDragonVisionDebug message) {
        if (!message.active()) { clear(); return; }
        snapshot = message;
        snapshotLevel = Minecraft.getInstance().level;
        age = 0;
    }

    public static @Nullable MessageDragonVisionDebug getSnapshot() { return snapshot; }

    public static void tick() {
        if (snapshot != null && (++age > 40 || snapshotLevel != Minecraft.getInstance().level)) clear();
    }

    public static void clear() {
        snapshot = null;
        snapshotLevel = null;
        age = 0;
    }
}
