package com.leon.saintsdragons.server.entity.handler;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonEntity.VocalEntry;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles all sound effects for dragons
 * Separates sound logic from entity class for cleaner organization
 */
public class DragonSoundHandler {
    private final DragonEntity dragon;
    private final DragonSoundProfile profile;
    private static final int MIN_OVERLAP_GUARD_TICKS = 5;
    private static final Set<String> DEFAULT_NON_OVERLAPPING_KEYS = Set.of(
            "hurt", "stegonaut_hurt", "cindervane_hurt", "primitive_drake_hurt", "die",
            "raevyx_hurt", "raevyx_die", "baby_raevyx_hurt", "baby_raevyx_die",
            "nulljaw_hurt", "nulljaw_die"
    );
    private static final Map<String, Integer> GENERIC_VOCAL_WINDOWS = Map.of(
            "hurt", 20,
            "die", 62
    );
    private final Map<String, Integer> vocalCooldowns = new HashMap<>();
    private long lastStepTick = -100; // Track last step sound tick for cooldown

    public DragonSoundHandler(DragonEntity dragon) {
        this.dragon = dragon;
        DragonSoundProfile providedProfile = dragon.getSoundProfile();
        this.profile = providedProfile != null ? providedProfile : DragonSoundProfile.EMPTY;
    }

    /** Call every entity tick to update cooldowns */
    public void tick() {
        // Cooldown management only - no pending steps, we trust Blockbench timing
    }

    /**
     * Handle keyframe-based sound effects during animations
     * Call this from animation controller sound handlers (legacy support)
     */
    public void handleAnimationSound(DragonEntity entity, Object keyframeData, software.bernie.geckolib.core.animation.AnimationController<?> controller) {
        if (dragon.isDying()) return;
        // IMPORTANT: GeckoLib fires animation sound events on BOTH client and server!
        // We ONLY want to handle on client side for local playback
        if (!dragon.level().isClientSide) return; // Block server-side completely
        if (keyframeData == null) return;
        String controllerName = null;
        try {
            if (controller != null && controller.getName() != null) {
                controllerName = controller.getName();
            }
        } catch (Throwable ignored) {}
        boolean sittingMuted = dragon.isStayOrSitMuted();
        boolean sleeping = dragon.isSleeping();
        boolean sleepTransitioning = dragon.isSleepTransitioning();
        // Sleep always silences keyframes, sitting is handled per vocal profile opt-ins
        if (sleeping) return;
        if (sleepTransitioning && (!"action".equals(controllerName))) return;
        String raw;
        String locator = null;
        try {
            // Use reflection to call getSound() method on the keyframe data
            raw = (String) keyframeData.getClass().getMethod("getSound").invoke(keyframeData);
            // Try to get locator if available
            try {
                locator = (String) keyframeData.getClass().getMethod("getLocator").invoke(keyframeData);
            } catch (Exception ignored) {
                // Locator might not be present in all sound keyframes
            }
        } catch (Exception e) {
            return; // If we can't get the sound data, skip
        }
        if (raw == null || raw.isEmpty()) return;
        String sound = raw.toLowerCase(java.util.Locale.ROOT);

        // Auto format: namespace:soundid or namespace:soundid|vol|pitch
        if (sound.contains(":")) {
            handleAutoSoundSpec(sound);
            return;
        }
        String normalizedForFlap = null;
        if (sound.contains("flap")) {
            normalizedForFlap = sound.substring(sound.indexOf("flap"));
        }
        String normalizedForStep = null;
        if (sound.contains("step")) {
            normalizedForStep = sound.substring(sound.indexOf("step"));
        }
        if (profile.handleAnimationSound(this, dragon, sound, locator)) {
            return;
        }
        if (sittingMuted) {
            return;
        }
        // Allow flexible keys from animation JSON: flap1, flap_right, raevyx_flap1, step2, raevyx_run_step1, etc.
        if (normalizedForFlap != null && normalizedForFlap.startsWith("flap")) {
            handleWingFlapSound(normalizedForFlap);
            return;
        }
        if (normalizedForStep != null && (normalizedForStep.startsWith("step") || normalizedForStep.startsWith("run_step"))) {
            handleStepSound(normalizedForStep);
            return;
        }
        playFallbackSound(sound, locator);
    }

    private void handleAutoSoundSpec(String spec) {
        try {
            String[] parts = spec.split("\\|");
            String soundId = parts[0];
            float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
            net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(soundId);
            if (rl != null) {
                dragon.level().playLocalSound(dragon.getX(), dragon.getY(), dragon.getZ(),
                        net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(rl),
                        SoundSource.NEUTRAL, volume, pitch, false);
            }
        } catch (Exception ignored) {}
    }

    private void handleWingFlapSound(String key) {
        if (profile.handleWingFlapSound(this, dragon, key)) {
            return;
        }
        float volume = dragon.isBaby() ? 0.6f : 1.0f;
        float pitch = 1.0f;
        if (key.contains("left")) {
            pitch += 0.05f;
        } else if (key.contains("right")) {
            pitch -= 0.05f;
        }
        dragon.level().playLocalSound(dragon.getX(), dragon.getY(), dragon.getZ(),
                ModSounds.RAEVYX_FLAP.get(), SoundSource.NEUTRAL, volume, pitch, false);
    }

    private void handleStepSound(String key) {
        Vec3 pos = dragon.position();
        if (profile.handleStepSound(this, dragon, key, null, pos.x, pos.y, pos.z, 1.0f, 1.0f)) {
            return;
        }
        playFootstepSound(dragon.level(), pos, dragon);
    }

    private void playFallbackSound(String key, String locator) {
        DragonSoundProfile profile = dragon.getSoundProfile();
        if (profile != null && profile.handleAnimationSound(this, dragon, key, locator)) {
            return;
        }
        if (DEFAULT_NON_OVERLAPPING_KEYS.contains(key)) {
            if (isInCooldown(key)) {
                return;
            }
            startCooldown(key);
        }
        playClientSound(dragon, resolveLocator(locator), ModSounds.RAEVYX_CHUFF.get(), 1.0f, 1.0f);
    }

    private boolean isInCooldown(String key) {
        Integer cooldown = vocalCooldowns.get(key);
        return cooldown != null && cooldown > dragon.tickCount;
    }

    private void startCooldown(String key) {
        int window = GENERIC_VOCAL_WINDOWS.getOrDefault(key, 20);
        vocalCooldowns.put(key, dragon.tickCount + Math.max(window, MIN_OVERLAP_GUARD_TICKS));
    }

    private Vec3 resolveLocator(String locator) {
        if (locator == null || locator.isEmpty()) {
            return dragon.position();
        }
        return profile.resolveLocator(this, dragon, locator);
    }

    public void playVocal(String vocalKey) {
        if (dragon.isRemoved() || dragon.isDeadOrDying()) {
            return;
        }
        if (isInCooldown(vocalKey)) {
            return;
        }
        startCooldown(vocalKey);
        DragonSoundProfile profile = dragon.getSoundProfile();
        if (profile != null && profile.handleVocal(this, dragon, vocalKey)) {
            return;
        }

        DragonEntity.VocalEntry entry = resolveVocalEntry(profile, vocalKey);
        if (entry == null || entry.soundSupplier() == null) {
            return;
        }
        if (!entry.allowDuringSleep() && (dragon.isSleeping() || dragon.isSleepTransitioning())) {
            return;
        }
        if (!entry.allowWhenSitting() && dragon.isStayOrSitMuted()) {
            return;
        }

        // Trigger the animation if this vocal has one
        if (entry.animationId() != null && !entry.animationId().isEmpty()) {
            String controllerId = entry.controllerId() != null && !entry.controllerId().isEmpty() 
                ? entry.controllerId() 
                : "action"; // Default to "action" controller if not specified
            dragon.triggerAnim(controllerId, vocalKey);
        }

        // IMPORTANT: playVocal should ONLY trigger animations!
        // Sounds are played by the animation keyframes via handleAnimationSound on the client.
        // This allows proper locator positioning (mouth_origin, etc.) from the animation JSON.
    }

    public void playClientSound(DragonEntity dragon, Vec3 position, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        double x = position != null ? position.x : dragon.getX();
        double y = position != null ? position.y : dragon.getY();
        double z = position != null ? position.z : dragon.getZ();
        Level level = dragon.level();
        if (level.isClientSide) {
            level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, volume, pitch, false);
        } else {
            level.playSound(null, x, y, z, sound, SoundSource.NEUTRAL, volume, pitch);
        }
    }

    public void playFootstepSound(Level level, Vec3 pos, DragonEntity dragon) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_BIG_FALL, SoundSource.NEUTRAL,
                dragon.isBaby() ? 0.5f : 0.9f,
                dragon.getRandom().nextFloat() * 0.2f + 0.9f);
    }

    private DragonEntity.VocalEntry resolveVocalEntry(DragonSoundProfile profile, String key) {
        DragonEntity.VocalEntry entry = dragon.getVocalEntries().get(key);
        if (entry != null) {
            return entry;
        }
        if (profile != null) {
            entry = profile.getFallbackVocalEntry(key);
            if (entry != null) {
                return entry;
            }
        }
        int underscore = key.indexOf('_');
        while (underscore > 0) {
            String suffix = key.substring(underscore + 1);
            entry = dragon.getVocalEntries().get(suffix);
            if (entry != null) {
                return entry;
            }
            if (profile != null) {
                entry = profile.getFallbackVocalEntry(suffix);
                if (entry != null) {
                    return entry;
                }
            }
            underscore = key.indexOf('_', underscore + 1);
        }
        return null;
    }

    public DragonSoundProfile getProfile() {
        return profile;
    }

    public DragonEntity getDragon() {
        return dragon;
    }

    public boolean allowOverlap(String locator) {
        return profile.allowOverlap(locator);
    }

    public boolean shouldPreventOverlap(String key) {
        return DEFAULT_NON_OVERLAPPING_KEYS.contains(key);
    }

    public Vec3 resolveLocatorWorldPos(String locator) {
        return resolveLocator(locator);
    }

    public long getLastStepTick() {
        return lastStepTick;
    }

    public void setLastStepTick(long tick) {
        this.lastStepTick = tick;
    }
}
