package com.leon.saintsdragons.server.entity.handler;

import com.leon.saintsdragons.server.entity.interfaces.HumanSoundProfile;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;

import java.util.HashMap;
import java.util.Map;

public class HumanSoundHandler {
    private final Mob entity;
    private final HumanSoundProfile profile;
    private final Map<String, Integer> soundCooldowns = new HashMap<>();

    public HumanSoundHandler(Mob entity, HumanSoundProfile profile) {
        this.entity = entity;
        this.profile = profile != null ? profile : HumanSoundProfile.EMPTY;
    }

    public void handleAnimationSound(Object keyframeData, software.bernie.geckolib.core.animation.AnimationController<?> controller) {
        // Only handle on client side for local playback
        if (!entity.level().isClientSide) return;
        if (keyframeData == null) return;

        String raw = extractSoundString(keyframeData);
        if (raw == null || raw.isEmpty()) return;

        String sound = raw.toLowerCase(java.util.Locale.ROOT);

        // Parse sound spec: "key|volume|pitch" or "namespace:id|volume|pitch"
        String[] parts = sound.split("\\|");
        String soundKey = parts[0];
        float volume = parts.length > 1 ? parseFloat(parts[1], 1.0f) : 1.0f;
        float pitch = parts.length > 2 ? parseFloat(parts[2], 1.0f) : 1.0f;

        // Extract locator if available
        String locator = extractLocator(keyframeData);

        // Handle direct sound ID (namespace:sound_id)
        if (soundKey.contains(":")) {
            playDirectSound(soundKey, locator, volume, pitch);
            return;
        }

        // Let profile handle the sound
        if (profile.handleSound(this, entity, soundKey, locator, volume, pitch)) {
            return;
        }

        // Fallback: no sound played (profile should handle all sounds)
    }

    /**
     * Play a sound with cooldown protection to prevent rapid-fire overlaps.
     * Returns true if sound was played, false if on cooldown.
     */
    public boolean playSound(String soundKey, SoundEvent sound, Vec3 position, float volume, float pitch, int cooldownTicks) {
        if (isOnCooldown(soundKey)) {
            return false;
        }

        if (cooldownTicks > 0) {
            setCooldown(soundKey, cooldownTicks);
        }

        Level level = entity.level();
        double x = position.x;
        double y = position.y;
        double z = position.z;

        level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, volume, pitch, false);
        return true;
    }

    /**
     * Play a sound at the entity's position.
     */
    public boolean playSound(String soundKey, SoundEvent sound, float volume, float pitch, int cooldownTicks) {
        return playSound(soundKey, sound, entity.position(), volume, pitch, cooldownTicks);
    }

    /**
     * Play a sound without cooldown protection (use sparingly).
     */
    public void playSoundImmediate(SoundEvent sound, Vec3 position, float volume, float pitch) {
        Level level = entity.level();
        level.playLocalSound(position.x, position.y, position.z, sound, SoundSource.NEUTRAL, volume, pitch, false);
    }

    /**
     * Resolve a locator to a world position.
     * Falls back to entity position if locator is null or can't be resolved.
     */
    public Vec3 resolveLocator(String locator) {
        if (locator == null || locator.isEmpty()) {
            return entity.position();
        }

        // Try to get locator position from GeckoLib renderer (client-side bone positions)
        Vec3 locatorPos = getClientLocatorPosition(locator);
        if (locatorPos != null) {
            return locatorPos;
        }

        // Let profile provide fallback locator logic
        return profile.resolveLocator(this, entity, locator);
    }

    /**
     * Check if a sound key is on cooldown.
     */
    public boolean isOnCooldown(String soundKey) {
        Integer cooldownEnd = soundCooldowns.get(soundKey);
        if (cooldownEnd == null) {
            return false;
        }

        int currentTick = entity.tickCount;
        if (currentTick >= cooldownEnd) {
            soundCooldowns.remove(soundKey);
            return false;
        }

        return true;
    }

    /**
     * Set a cooldown for a sound key.
     */
    public void setCooldown(String soundKey, int ticks) {
        soundCooldowns.put(soundKey, entity.tickCount + ticks);
    }

    /**
     * Get the entity this handler belongs to.
     */
    public Mob getEntity() {
        return entity;
    }

    /**
     * Get the sound profile.
     */
    public HumanSoundProfile getProfile() {
        return profile;
    }

    // ===== INTERNAL HELPERS =====

    private void playDirectSound(String soundId, String locator, float volume, float pitch) {
        try {
            net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(soundId);
            if (rl != null) {
                SoundEvent sound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(rl);
                Vec3 position = resolveLocator(locator);
                playSoundImmediate(sound, position, volume, pitch);
            }
        } catch (Exception ignored) {
            // Invalid sound ID, skip silently
        }
    }

    private String extractSoundString(Object keyframeData) {
        try {
            return (String) keyframeData.getClass().getMethod("getSound").invoke(keyframeData);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractLocator(Object keyframeData) {
        try {
            return (String) keyframeData.getClass().getMethod("getLocator").invoke(keyframeData);
        } catch (Exception e) {
            return null;
        }
    }

    private float parseFloat(String str, float defaultValue) {
        try {
            return Float.parseFloat(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Vec3 getClientLocatorPosition(String locator) {
        // Try to get position from GeoEntity if supported
        if (entity instanceof GeoEntity) {
            // This is a placeholder - actual implementation would need renderer access
            // For now, profiles can override resolveLocator() for custom locator logic
            return null;
        }
        return null;
    }
}
