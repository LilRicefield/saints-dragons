package com.leon.saintsdragons.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class DragonMovingSoundController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DragonMovingSoundController.class);
    private static final Map<SourceSoundKey, ActiveEntry> ACTIVE_SOUNDS = new HashMap<>();
    private static ClientLevel activeLevel;

    private DragonMovingSoundController() {
    }

    public static void play(
            UUID playbackId,
            int entityId,
            UUID entityUuid,
            String soundId,
            float volume,
            float pitch,
            int durationTicks
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        updateLevel(minecraft);
        if (minecraft.level == null || playbackId == null || entityUuid == null) {
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
        SourceSoundKey key = new SourceSoundKey(entityUuid, soundId);
        ActiveEntry existing = ACTIVE_SOUNDS.get(key);
        if (existing != null && existing.playbackId.equals(playbackId)) {
            existing.volume = volume;
            existing.pitch = pitch;
            existing.sound = sound;
            if (!existing.started) {
                tryStart(existing, minecraft, now);
            } else if (existing.instance != null && !existing.instance.isStopped()) {
                existing.instance.updateMix(volume, pitch);
            }
            return;
        }

        if (existing != null && existing.instance != null) {
            minecraft.getSoundManager().stop(existing.instance);
        }
        ActiveEntry entry = new ActiveEntry(
                playbackId,
                entityId,
                entityUuid,
                sound,
                volume,
                pitch,
                now + safeDuration
        );
        ACTIVE_SOUNDS.put(key, entry);
        tryStart(entry, minecraft, now);
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        updateLevel(minecraft);
        if (minecraft.level == null) {
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }

        long now = minecraft.level.getGameTime();
        Iterator<Map.Entry<SourceSoundKey, ActiveEntry>> iterator = ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            ActiveEntry entry = iterator.next().getValue();
            if (entry == null || now >= entry.endTick) {
                if (entry != null && entry.instance != null) {
                    minecraft.getSoundManager().stop(entry.instance);
                }
                iterator.remove();
                continue;
            }
            if (!entry.started) {
                tryStart(entry, minecraft, now);
                continue;
            }
            if (entry.instance == null || entry.instance.isStopped()) {
                iterator.remove();
            }
        }
    }

    private static void tryStart(ActiveEntry entry, Minecraft minecraft, long now) {
        if (entry.started || minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(entry.entityId);
        if (entity == null
                || !entry.entityUuid.equals(entity.getUUID())
                || entity.isRemoved()
                || !entity.isAlive()
                || entity.isSilent()) {
            return;
        }
        int remainingTicks = (int) Math.max(1L, entry.endTick - now);
        DragonMovingOneShotSound movingSound = new DragonMovingOneShotSound(
                entry.entityId,
                entry.entityUuid,
                entry.sound,
                entry.volume,
                entry.pitch,
                remainingTicks
        );
        entry.started = true;
        entry.instance = movingSound;
        minecraft.getSoundManager().play(movingSound);
    }

    private static void updateLevel(Minecraft minecraft) {
        ClientLevel currentLevel = minecraft != null ? minecraft.level : null;
        if (activeLevel == currentLevel) {
            return;
        }
        if (minecraft != null) {
            stopAll(minecraft);
        } else {
            ACTIVE_SOUNDS.clear();
        }
        activeLevel = currentLevel;
    }

    private static void stopAll(Minecraft minecraft) {
        for (ActiveEntry entry : ACTIVE_SOUNDS.values()) {
            if (entry != null && entry.instance != null) {
                minecraft.getSoundManager().stop(entry.instance);
            }
        }
        ACTIVE_SOUNDS.clear();
    }

    private record SourceSoundKey(UUID entityUuid, String soundId) {
    }

    private static final class ActiveEntry {
        final UUID playbackId;
        final int entityId;
        final UUID entityUuid;
        SoundEvent sound;
        float volume;
        float pitch;
        long endTick;
        boolean started;
        DragonMovingOneShotSound instance;

        private ActiveEntry(
                UUID playbackId,
                int entityId,
                UUID entityUuid,
                SoundEvent sound,
                float volume,
                float pitch,
                long endTick
        ) {
            this.playbackId = playbackId;
            this.entityId = entityId;
            this.entityUuid = entityUuid;
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.endTick = endTick;
        }
    }
}
