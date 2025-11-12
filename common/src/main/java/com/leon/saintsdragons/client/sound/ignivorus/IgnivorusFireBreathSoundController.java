package com.leon.saintsdragons.client.sound.ignivorus;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
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

/**
 * Tracks Ignivorus instances on the client and keeps their fire breath loop sound in sync with the ability state.
 */
@Environment(EnvType.CLIENT)
public final class IgnivorusFireBreathSoundController {
    private static final Map<Integer, IgnivorusFireBreathLoopSound> ACTIVE_SOUNDS = new HashMap<>();
    private static final Map<Integer, Boolean> LAST_BREATHING_STATE = new HashMap<>();

    private IgnivorusFireBreathSoundController() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        if (minecraft.isPaused()) {
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
            if (entity instanceof Ignivorus ignivorus) {
                int id = ignivorus.getId();
                seen.add(id);
                boolean breathing = ignivorus.isBreathingFire();
                boolean wasBreathing = LAST_BREATHING_STATE.getOrDefault(id, false);

                if (breathing && !wasBreathing) {
                    startLoop(minecraft, ignivorus);
                } else if (!breathing && wasBreathing) {
                    stopLoop(minecraft, id);
                }
                LAST_BREATHING_STATE.put(id, breathing);
            }
        }

        Iterator<Map.Entry<Integer, Boolean>> iterator = LAST_BREATHING_STATE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Boolean> entry = iterator.next();
            if (!seen.contains(entry.getKey())) {
                stopLoop(minecraft, entry.getKey());
                iterator.remove();
            }
        }
    }

    private static void cleanupFinished(Minecraft minecraft) {
        Iterator<Map.Entry<Integer, IgnivorusFireBreathLoopSound>> iterator = ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, IgnivorusFireBreathLoopSound> entry = iterator.next();
            IgnivorusFireBreathLoopSound sound = entry.getValue();
            if (sound == null || sound.isStopped()) {
                minecraft.getSoundManager().stop(sound);
                iterator.remove();
                LAST_BREATHING_STATE.put(entry.getKey(), false);
            }
        }
    }

    private static void startLoop(Minecraft minecraft, Ignivorus ignivorus) {
        stopLoop(minecraft, ignivorus.getId());
        IgnivorusFireBreathLoopSound sound = new IgnivorusFireBreathLoopSound(ignivorus);
        ACTIVE_SOUNDS.put(ignivorus.getId(), sound);
        minecraft.getSoundManager().play(sound);
    }

    private static void stopLoop(Minecraft minecraft, int entityId) {
        IgnivorusFireBreathLoopSound sound = ACTIVE_SOUNDS.remove(entityId);
        if (sound != null) {
            minecraft.getSoundManager().stop(sound);
        }
    }

    private static void stopAll(Minecraft minecraft) {
        for (IgnivorusFireBreathLoopSound sound : ACTIVE_SOUNDS.values()) {
            minecraft.getSoundManager().stop(sound);
        }
        ACTIVE_SOUNDS.clear();
        LAST_BREATHING_STATE.clear();
    }
}
