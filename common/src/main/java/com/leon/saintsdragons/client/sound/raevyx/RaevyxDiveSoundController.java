package com.leon.saintsdragons.client.sound.raevyx;

import com.leon.saintsdragons.client.camera.DragonDiveEffectIntensity;
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
public final class RaevyxDiveSoundController {
    private static final Map<Integer, RaevyxDiveLoopSound> ACTIVE_SOUNDS = new HashMap<>();

    private RaevyxDiveSoundController() {
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

        Iterator<Integer> iterator = ACTIVE_SOUNDS.keySet().iterator();
        while (iterator.hasNext()) {
            int id = iterator.next();
            if (!seen.contains(id) || !(level.getEntity(id) instanceof Raevyx)) {
                RaevyxDiveLoopSound sound = ACTIVE_SOUNDS.get(id);
                if (sound != null) {
                    minecraft.getSoundManager().stop(sound);
                }
                iterator.remove();
            }
        }
    }

    private static void processRaevyx(Minecraft minecraft, Raevyx raevyx, Set<Integer> seen) {
        int id = raevyx.getId();
        seen.add(id);

        if (!raevyx.isCustomDiveLoopEnabled()) {
            stopLoop(minecraft, id);
            return;
        }
        if (!raevyx.isVehicle()) {
            stopLoop(minecraft, id);
            return;
        }

        float intensity = DragonDiveEffectIntensity.get(raevyx);
        if (intensity <= 0.0F) {
            return;
        }

        RaevyxDiveLoopSound sound = ACTIVE_SOUNDS.get(id);
        if (sound == null || sound.isStopped() || !minecraft.getSoundManager().isActive(sound)) {
            startLoop(minecraft, raevyx);
        }
    }

    private static void cleanupFinished(Minecraft minecraft) {
        Iterator<Map.Entry<Integer, RaevyxDiveLoopSound>> iterator = ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, RaevyxDiveLoopSound> entry = iterator.next();
            RaevyxDiveLoopSound sound = entry.getValue();
            if (sound == null || sound.isStopped()) {
                if (sound != null) {
                    minecraft.getSoundManager().stop(sound);
                }
                iterator.remove();
            }
        }
    }

    private static void startLoop(Minecraft minecraft, Raevyx raevyx) {
        stopLoop(minecraft, raevyx.getId());
        RaevyxDiveLoopSound sound = new RaevyxDiveLoopSound(raevyx);
        ACTIVE_SOUNDS.put(raevyx.getId(), sound);
        minecraft.getSoundManager().play(sound);
    }

    private static void stopLoop(Minecraft minecraft, int entityId) {
        RaevyxDiveLoopSound sound = ACTIVE_SOUNDS.remove(entityId);
        if (sound != null) {
            minecraft.getSoundManager().stop(sound);
        }
    }

    private static void stopAll(Minecraft minecraft) {
        for (RaevyxDiveLoopSound sound : ACTIVE_SOUNDS.values()) {
            minecraft.getSoundManager().stop(sound);
        }
        ACTIVE_SOUNDS.clear();
    }
}
