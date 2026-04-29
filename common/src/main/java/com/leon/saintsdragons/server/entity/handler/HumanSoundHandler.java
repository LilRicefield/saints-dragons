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
        if (!entity.level().isClientSide) return;
        if (keyframeData == null) return;
        String raw = extractSoundString(keyframeData);
        if (raw == null || raw.isEmpty()) return;
        String sound = raw.toLowerCase(java.util.Locale.ROOT);
        String[] parts = sound.split("\\|");
        String soundKey = parts[0];
        float volume = parts.length > 1 ? parseFloat(parts[1], 1.0f) : 1.0f;
        float pitch = parts.length > 2 ? parseFloat(parts[2], 1.0f) : 1.0f;

        String locator = extractLocator(keyframeData);
        if (soundKey.contains(":")) {
            playDirectSound(soundKey, locator, volume, pitch);
            return;
        }
        if (profile.handleSound(this, entity, soundKey, locator, volume, pitch)) {
            return;
        }

    }

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

    public boolean playSound(String soundKey, SoundEvent sound, float volume, float pitch, int cooldownTicks) {
        return playSound(soundKey, sound, entity.position(), volume, pitch, cooldownTicks);
    }

    public void playSoundImmediate(SoundEvent sound, Vec3 position, float volume, float pitch) {
        Level level = entity.level();
        level.playLocalSound(position.x, position.y, position.z, sound, SoundSource.NEUTRAL, volume, pitch, false);
    }

    public Vec3 resolveLocator(String locator) {
        if (locator == null || locator.isEmpty()) {
            return entity.position();
        }
        Vec3 locatorPos = getClientLocatorPosition(locator);
        if (locatorPos != null) {
            return locatorPos;
        }
        return profile.resolveLocator(this, entity, locator);
    }

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

    public void setCooldown(String soundKey, int ticks) {
        soundCooldowns.put(soundKey, entity.tickCount + ticks);
    }
    public Mob getEntity() {
        return entity;
    }
    public HumanSoundProfile getProfile() {
        return profile;
    }
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
        if (entity instanceof GeoEntity) {

            return null;
        }
        return null;
    }
}