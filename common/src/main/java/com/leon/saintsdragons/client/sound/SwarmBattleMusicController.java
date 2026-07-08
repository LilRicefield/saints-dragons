package com.leon.saintsdragons.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public final class SwarmBattleMusicController {
    private static SwarmBattleLoopSound activeSound;
    private static long activeUntilGameTime;

    private SwarmBattleMusicController() {
    }

    public static void signal(boolean active, int durationTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        long now = minecraft.level.getGameTime();
        if (active) {
            activeUntilGameTime = Math.max(activeUntilGameTime, now + Math.max(1, durationTicks));
        } else {
            activeUntilGameTime = now;
        }
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft == null || minecraft.isPaused()) {
            return;
        }
        if (minecraft.level == null || minecraft.player == null) {
            stopCurrent(minecraft);
            activeUntilGameTime = 0L;
            return;
        }

        if (activeSound != null && (activeSound.isStopped() || !minecraft.getSoundManager().isActive(activeSound))) {
            activeSound = null;
        }

        if (isBattleActive(minecraft) && activeSound == null) {
            activeSound = new SwarmBattleLoopSound();
            minecraft.getSoundManager().play(activeSound);
        }
    }

    static boolean isBattleActive(Minecraft minecraft) {
        return minecraft != null && minecraft.level != null && minecraft.level.getGameTime() < activeUntilGameTime;
    }

    private static void stopCurrent(Minecraft minecraft) {
        if (minecraft != null && activeSound != null) {
            minecraft.getSoundManager().stop(activeSound);
        }
        activeSound = null;
    }
}
