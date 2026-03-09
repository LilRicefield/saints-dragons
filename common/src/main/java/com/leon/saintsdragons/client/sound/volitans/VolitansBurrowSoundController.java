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
public final class VolitansBurrowSoundController {
    private static final Map<Integer, VolitansBurrowLoopSound> ACTIVE_SOUNDS = new HashMap<>();
    private static final Map<Integer, Boolean> LAST_BURROWING_STATE = new HashMap<>();

    private VolitansBurrowSoundController() {
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
                boolean burrowing = volitans.isBurrowing();
                boolean wasBurrowing = LAST_BURROWING_STATE.getOrDefault(id, false);

                if (burrowing) {
                    VolitansBurrowLoopSound.Mode desiredMode = resolveMode(volitans);
                    VolitansBurrowLoopSound currentSound = ACTIVE_SOUNDS.get(id);
                    if (!wasBurrowing || currentSound == null || currentSound.getMode() != desiredMode || currentSound.isStopped()) {
                        startLoop(minecraft, volitans, desiredMode);
                    }
                } else if (wasBurrowing) {
                    stopLoop(minecraft, id);
                }

                LAST_BURROWING_STATE.put(id, burrowing);
            }
        }

        Iterator<Map.Entry<Integer, Boolean>> iterator = LAST_BURROWING_STATE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Boolean> entry = iterator.next();
            if (!seen.contains(entry.getKey())) {
                stopLoop(minecraft, entry.getKey());
                iterator.remove();
            }
        }
    }

    private static VolitansBurrowLoopSound.Mode resolveMode(Volitans volitans) {
        int groundState = volitans.getEffectiveGroundState();
        if (groundState > 0 || volitans.getDeltaMovement().horizontalDistanceSqr() > 0.0025D) {
            return VolitansBurrowLoopSound.Mode.MOVE;
        }
        return VolitansBurrowLoopSound.Mode.IDLE;
    }

    private static void cleanupFinished(Minecraft minecraft) {
        Iterator<Map.Entry<Integer, VolitansBurrowLoopSound>> iterator = ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, VolitansBurrowLoopSound> entry = iterator.next();
            VolitansBurrowLoopSound sound = entry.getValue();
            if (sound == null || sound.isStopped()) {
                minecraft.getSoundManager().stop(sound);
                iterator.remove();
            }
        }
    }

    private static void startLoop(Minecraft minecraft, Volitans volitans, VolitansBurrowLoopSound.Mode mode) {
        stopLoop(minecraft, volitans.getId());
        VolitansBurrowLoopSound sound = new VolitansBurrowLoopSound(volitans, mode);
        ACTIVE_SOUNDS.put(volitans.getId(), sound);
        minecraft.getSoundManager().play(sound);
    }

    private static void stopLoop(Minecraft minecraft, int entityId) {
        VolitansBurrowLoopSound sound = ACTIVE_SOUNDS.remove(entityId);
        if (sound != null) {
            minecraft.getSoundManager().stop(sound);
        }
    }

    private static void stopAll(Minecraft minecraft) {
        for (VolitansBurrowLoopSound sound : ACTIVE_SOUNDS.values()) {
            minecraft.getSoundManager().stop(sound);
        }
        ACTIVE_SOUNDS.clear();
        LAST_BURROWING_STATE.clear();
    }
}
