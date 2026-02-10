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

/**
 * Keeps the Raevyx lightning beam loop in sync with its beaming state per-client.
 */
@Environment(EnvType.CLIENT)
public final class RaevyxLightningBeamSoundController {
    private static final Map<Integer, RaevyxLightningBeamLoopSound> ACTIVE_SOUNDS = new HashMap<>();
    private static final Map<Integer, Boolean> LAST_BEAMING_STATE = new HashMap<>();

    private RaevyxLightningBeamSoundController() {
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
        if (minecraft.player != null && minecraft.player.getVehicle() instanceof Raevyx riddenRaevyx) {
            processRaevyx(minecraft, riddenRaevyx, seen);
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof Raevyx raevyx) {
                processRaevyx(minecraft, raevyx, seen);
            }
        }

        Iterator<Map.Entry<Integer, Boolean>> iterator = LAST_BEAMING_STATE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Boolean> entry = iterator.next();
            int id = entry.getKey();
            Entity entity = level.getEntity(id);
            if (!(entity instanceof Raevyx)) {
                stopLoop(minecraft, entry.getKey());
                iterator.remove();
            } else if (seen.contains(id)) {
                continue;
            }
        }
    }

    private static void processRaevyx(Minecraft minecraft, Raevyx raevyx, Set<Integer> seen) {
        int id = raevyx.getId();
        seen.add(id);
        boolean beaming = raevyx.isBeaming();
        boolean wasBeaming = LAST_BEAMING_STATE.getOrDefault(id, false);

        if (beaming && !wasBeaming) {
            startLoop(minecraft, raevyx);
        } else if (!beaming && wasBeaming) {
            stopLoop(minecraft, id);
        }
        LAST_BEAMING_STATE.put(id, beaming);
    }

    private static void cleanupFinished(Minecraft minecraft) {
        Iterator<Map.Entry<Integer, RaevyxLightningBeamLoopSound>> iterator = ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, RaevyxLightningBeamLoopSound> entry = iterator.next();
            RaevyxLightningBeamLoopSound sound = entry.getValue();
            if (sound == null || sound.isStopped()) {
                minecraft.getSoundManager().stop(sound);
                iterator.remove();
                LAST_BEAMING_STATE.put(entry.getKey(), false);
            }
        }
    }

    private static void startLoop(Minecraft minecraft, Raevyx raevyx) {
        stopLoop(minecraft, raevyx.getId());
        RaevyxLightningBeamLoopSound sound = new RaevyxLightningBeamLoopSound(raevyx);
        ACTIVE_SOUNDS.put(raevyx.getId(), sound);
        minecraft.getSoundManager().play(sound);
    }

    private static void stopLoop(Minecraft minecraft, int entityId) {
        RaevyxLightningBeamLoopSound sound = ACTIVE_SOUNDS.remove(entityId);
        if (sound != null) {
            minecraft.getSoundManager().stop(sound);
        }
    }

    private static void stopAll(Minecraft minecraft) {
        for (RaevyxLightningBeamLoopSound sound : ACTIVE_SOUNDS.values()) {
            minecraft.getSoundManager().stop(sound);
        }
        ACTIVE_SOUNDS.clear();
        LAST_BEAMING_STATE.clear();
    }
}
