package com.leon.saintsdragons.client.sound.volitans;

import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
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
public final class VolitansBreathSoundController {
    private static final Map<Integer, VolitansBreathLoopSound> ACTIVE_SOUNDS = new HashMap<>();
    private static final Map<Integer, Boolean> LAST_BREATHING_STATE = new HashMap<>();

    private VolitansBreathSoundController() {
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
            if (entity instanceof Volitans volitans) {
                int id = volitans.getId();
                seen.add(id);
                boolean breathing = volitans.isBreathing();
                boolean wasBreathing = LAST_BREATHING_STATE.getOrDefault(id, false);

                if (breathing && !wasBreathing) {
                    startLoop(minecraft, volitans);
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
        Iterator<Map.Entry<Integer, VolitansBreathLoopSound>> iterator = ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, VolitansBreathLoopSound> entry = iterator.next();
            VolitansBreathLoopSound sound = entry.getValue();
            if (sound == null || sound.isStopped()) {
                minecraft.getSoundManager().stop(sound);
                iterator.remove();
                LAST_BREATHING_STATE.put(entry.getKey(), false);
            }
        }
    }

    private static void startLoop(Minecraft minecraft, Volitans volitans) {
        stopLoop(minecraft, volitans.getId());
        VolitansBreathLoopSound sound = new VolitansBreathLoopSound(volitans);
        ACTIVE_SOUNDS.put(volitans.getId(), sound);
        minecraft.getSoundManager().play(sound);
    }

    private static void stopLoop(Minecraft minecraft, int entityId) {
        VolitansBreathLoopSound sound = ACTIVE_SOUNDS.remove(entityId);
        if (sound != null) {
            minecraft.getSoundManager().stop(sound);
        }
    }

    private static void stopAll(Minecraft minecraft) {
        for (VolitansBreathLoopSound sound : ACTIVE_SOUNDS.values()) {
            minecraft.getSoundManager().stop(sound);
        }
        ACTIVE_SOUNDS.clear();
        LAST_BREATHING_STATE.clear();
    }
}
