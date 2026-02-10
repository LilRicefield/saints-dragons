package com.leon.saintsdragons.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class DragonMovingSoundController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DragonMovingSoundController.class);
    private static final Map<String, ActiveEntry> ACTIVE_SOUNDS = new HashMap<>();
    private static final long DUPLICATE_SIGNAL_WINDOW_TICKS = 2L;

    private DragonMovingSoundController() {
    }

    public static void play(int entityId, String soundId, float volume, float pitch, int durationTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getSoundManager() == null || minecraft.level == null) {
            return;
        }

        ResourceLocation resourceLocation = ResourceLocation.tryParse(soundId);
        if (resourceLocation == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Failed to parse sound ID for entity {}: {}", entityId, soundId);
            }
            return;
        }
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(resourceLocation);
        if (sound == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Sound event not found in registry for entity {}: {}", entityId, resourceLocation);
            }
            return;
        }

        long now = minecraft.level.getGameTime();
        int safeDuration = Math.max(1, durationTicks);
        String key = entityId + "|" + soundId;
        ActiveEntry existing = ACTIVE_SOUNDS.get(key);
        if (existing != null && existing.endTick > now) {
            boolean duplicateBurst = (now - existing.lastSignalTick) <= DUPLICATE_SIGNAL_WINDOW_TICKS;
            existing.lastSignalTick = now;
            existing.endTick = now + safeDuration;
            existing.volume = volume;
            existing.pitch = pitch;
            existing.sound = sound;
            if (existing.instance == null || existing.instance.isStopped()) {
                existing.instance = null;
                tryStart(existing, minecraft, now);
            } else {
                existing.instance.updateMix(volume, pitch);
                if (!duplicateBurst) {
                    minecraft.getSoundManager().stop(existing.instance);
                    existing.instance = null;
                    tryStart(existing, minecraft, now);
                }
            }
            return;
        }

        ActiveEntry entry = new ActiveEntry(entityId, soundId, sound, volume, pitch, now + safeDuration, now);
        ACTIVE_SOUNDS.put(key, entry);
        tryStart(entry, minecraft, now);
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft == null || minecraft.isPaused()) {
            return;
        }
        if (minecraft.level == null) {
            stopAll(minecraft);
            return;
        }

        long now = minecraft.level.getGameTime();
        Iterator<Map.Entry<String, ActiveEntry>> iterator = ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            ActiveEntry entry = iterator.next().getValue();
            if (entry == null || now >= entry.endTick) {
                if (entry != null && entry.instance != null) {
                    minecraft.getSoundManager().stop(entry.instance);
                }
                iterator.remove();
                continue;
            }
            if (entry.instance == null || entry.instance.isStopped()) {
                entry.instance = null;
                tryStart(entry, minecraft, now);
            }
        }
    }

    private static void tryStart(ActiveEntry entry, Minecraft minecraft, long now) {
        int remainingTicks = (int) Math.max(1L, entry.endTick - now);
        DragonMovingOneShotSound movingSound = new DragonMovingOneShotSound(
                entry.entityId,
                entry.sound,
                entry.volume,
                entry.pitch,
                remainingTicks
        );
        entry.instance = movingSound;
        minecraft.getSoundManager().play(movingSound);
        if (movingSound.isStopped()) {
            entry.instance = null;
        }
    }

    private static void stopAll(Minecraft minecraft) {
        for (ActiveEntry entry : ACTIVE_SOUNDS.values()) {
            if (entry != null && entry.instance != null) {
                minecraft.getSoundManager().stop(entry.instance);
            }
        }
        ACTIVE_SOUNDS.clear();
    }

    private static final class ActiveEntry {
        final int entityId;
        final String soundId;
        SoundEvent sound;
        float volume;
        float pitch;
        long endTick;
        long lastSignalTick;
        DragonMovingOneShotSound instance;

        private ActiveEntry(int entityId, String soundId, SoundEvent sound, float volume, float pitch, long endTick, long lastSignalTick) {
            this.entityId = entityId;
            this.soundId = soundId;
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.endTick = endTick;
            this.lastSignalTick = lastSignalTick;
        }
    }
}
