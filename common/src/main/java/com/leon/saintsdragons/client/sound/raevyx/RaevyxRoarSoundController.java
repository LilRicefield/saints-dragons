package com.leon.saintsdragons.client.sound.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class RaevyxRoarSoundController {
    private static final Map<Integer, RaevyxRoarMovingSound> ACTIVE_SOUNDS = new HashMap<>();

    private RaevyxRoarSoundController() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft == null || minecraft.isPaused()) {
            return;
        }
        ClientLevel level = minecraft.level;
        if (level == null) {
            stopAll(minecraft);
            return;
        }

        cleanupFinished(minecraft);

        Set<Integer> seen = new HashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof Raevyx raevyx) {
                seen.add(raevyx.getId());
            }
        }

        Iterator<Map.Entry<Integer, RaevyxRoarMovingSound>> iterator = ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, RaevyxRoarMovingSound> entry = iterator.next();
            if (!seen.contains(entry.getKey())) {
                minecraft.getSoundManager().stop(entry.getValue());
                iterator.remove();
            }
        }
    }

    public static void playRoar(Raevyx raevyx, float volume, float pitch) {
        if (raevyx == null || !raevyx.level().isClientSide) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getSoundManager() == null) {
            return;
        }

        cleanupFinished(minecraft);

        int id = raevyx.getId();
        RaevyxRoarMovingSound existing = ACTIVE_SOUNDS.get(id);
        if (existing != null && !existing.isStopped()) {
            return;
        }

        RaevyxRoarMovingSound sound = new RaevyxRoarMovingSound(raevyx, volume, pitch);
        ACTIVE_SOUNDS.put(id, sound);
        minecraft.getSoundManager().play(sound);
    }

    private static void cleanupFinished(Minecraft minecraft) {
        Iterator<Map.Entry<Integer, RaevyxRoarMovingSound>> iterator = ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, RaevyxRoarMovingSound> entry = iterator.next();
            RaevyxRoarMovingSound sound = entry.getValue();
            if (sound == null || sound.isStopped() || sound.getRaevyx() == null || sound.getRaevyx().isRemoved()) {
                if (sound != null) {
                    minecraft.getSoundManager().stop(sound);
                }
                iterator.remove();
            }
        }
    }

    private static void stopAll(Minecraft minecraft) {
        for (RaevyxRoarMovingSound sound : ACTIVE_SOUNDS.values()) {
            minecraft.getSoundManager().stop(sound);
        }
        ACTIVE_SOUNDS.clear();
    }
}

