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
            "raevyx_hurt", "raevyx_die", "baby_raevyx_hurt", "baby_raevyx_die"
    );
    private static final Map<String, Integer> GENERIC_VOCAL_WINDOWS = Map.of(
            "hurt", 20,
            "die", 62
    );
    private final Map<String, Integer> vocalCooldowns = new HashMap<>();
    
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
            // Only handle footsteps from the movement controller (runs walk/run)
            try {
                if (controller != null && controller.getName() != null) {
                    String ctrl = controller.getName();
                    if (!"movement".equals(ctrl)) return; // ignore non-movement controllers for steps
                }
            } catch (Throwable ignored) {}
            // Use locator position if provided by the keyframe
            String stepLocator = (locator != null && !locator.isEmpty()) ? locator : mapStepKeyToLocator(normalizedForStep);
            handleStepSound(normalizedForStep, stepLocator);
            return;
        }
        switch (sound) {
            case "wing_flap" -> handleWingFlapSound(sound);
            case "dragon_step" -> handleStepSound(sound, null);
            case "takeoff_whoosh" -> handleTakeoffSound();
            case "landing_thud" -> handleLandingSound();
            default -> handleSoundByName(sound);
        }
    }

    /**
     * Handle sound by name - for direct sound triggering
     */
    public void handleSoundByName(String soundName) {
        if (dragon.isDying()) return;
        if (dragon.level().isClientSide) return;
        if (dragon.isStayOrSitMuted() || dragon.isSleeping() || dragon.isSleepTransitioning()) return; // Suppress during sit/sleep/transition
        if (soundName == null || soundName.isEmpty()) return;
        String key = soundName.toLowerCase(java.util.Locale.ROOT);
        if (profile.handleSoundByName(this, dragon, key)) {
            return;
        }
        if (key.startsWith("flap")) { handleWingFlapSound(key); return; }
        if (key.startsWith("step") || key.startsWith("run_step")) { handleStepSound(key, null); return; }
        switch (key) {
            case "wing_flap" -> handleWingFlapSound(key);
            case "dragon_step" -> handleStepSound(key, null);
            case "takeoff_whoosh" -> handleTakeoffSound();
            case "landing_thud" -> handleLandingSound();
            default -> {}
        }
    }

    /**
     * Plays a vocal sound and triggers a matching action animation.
     * Expected keys (examples): grumble1, grumble2, grumble3, purr, snort, chuff, content, annoyed,
     * growl_warning, roar, hurt
     */

    public void playVocal(String key) {
        if (key == null || key.isEmpty() || dragon.level().isClientSide) {
            return;
        }

        int currentTick = dragon.tickCount;
        if (!vocalCooldowns.isEmpty()) {
            vocalCooldowns.entrySet().removeIf(entry -> entry.getValue() <= currentTick);
        }

        VocalEntry entry = dragon.getVocalEntries().get(key);
        if (entry == null) {
            entry = profile.getFallbackVocalEntry(key);
        }

        boolean suppressOverlap = entry != null ? entry.preventOverlap() : DEFAULT_NON_OVERLAPPING_KEYS.contains(key);
        if (suppressOverlap) {
            Integer guard = vocalCooldowns.get(key);
            if (guard != null && guard > currentTick) {
                return;
            }
        }

        if (entry == null) {
            return;
        }

        if (!entry.allowDuringSleep() && (dragon.isSleeping() || dragon.isSleepTransitioning())) {
            return;
        }
        if (!entry.allowWhenSitting() && dragon.isStayOrSitMuted()) {
            return;
        }

        int window = getVocalAnimationWindowTicks(key);
        if (suppressOverlap) {
            vocalCooldowns.put(key, currentTick + Math.max(window, MIN_OVERLAP_GUARD_TICKS));
        }

        // Check if this vocal has an animation or is sound-only
        boolean hasAnimation = entry.animationId() != null && !entry.animationId().isEmpty();

        if (hasAnimation) {
            if (!dragon.level().isClientSide) {
                if (shouldBroadcastInstantly(key)) {
                    float pitch = entry.basePitch();
                    float variance = entry.pitchVariance();
                    if (variance != 0f) {
                        pitch += dragon.getRandom().nextFloat() * variance;
                    }
                    playServerBroadcast(entry.soundSupplier().get(), entry.volume(), pitch, null);
                }
                if (!dragon.isSleeping() && !dragon.isSleepTransitioning() && window > 0) {
                    dragon.triggerAnim(entry.controllerId(), key);
                }
            }
        } else {
            // Sound-only vocal - play sound directly without animation
            if (!dragon.level().isClientSide) {
                float volume = entry.volume();
                float pitch = entry.basePitch();
                float variance = entry.pitchVariance();
                if (variance != 0f) {
                    pitch += dragon.getRandom().nextFloat() * variance;
                }

                playServerBroadcast(entry.soundSupplier().get(), volume, pitch, null);
            }
        }
    }

    /**
     * Returns an appropriate action-controller window length (in ticks) for a vocal animation key.
     * Values mirror the animation lengths defined in raevyx.animation.json (rounded up).
     */
    private int getVocalAnimationWindowTicks(String key) {
        if (key == null) return 0;
        int custom = profile.getVocalAnimationWindowTicks(key);
        if (custom >= 0) {
            return custom;
        }
        Integer generic = GENERIC_VOCAL_WINDOWS.get(key);
        if (generic != null) {
            return generic;
        }
        return 40;
    }

    private boolean shouldBroadcastInstantly(String key) {
        return DEFAULT_NON_OVERLAPPING_KEYS.contains(key);
    }
    
    /**
     * Wing flap sound with dynamic speed variation
     * Delegates to profile for dragon-specific flap sounds
     */
    private void handleWingFlapSound(String key) {
        if (dragon.isStayOrSitMuted()) return;
        if (!dragon.level().isClientSide) return; // Client-side only

        // Let the profile handle it - each dragon has unique flap sounds
        if (profile.handleWingFlapSound(this, dragon, key)) {
            return;
        }

        // Fallback: play generic sound if profile doesn't handle it
        double flightSpeed = dragon.getCachedHorizontalSpeed();
        float pitch = 1.0f + (float)(flightSpeed * 0.3f);
        float volume = Math.max(0.6f, 0.9f + (float)(flightSpeed * 0.2f));
        dragon.level().playLocalSound(dragon.getX(), dragon.getY(), dragon.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.NEUTRAL, volume, pitch, false);
    }
    
    /**
     * Dragon step sound - trusts Blockbench keyframe timing completely
     * No delays, no spacing logic - plays immediately when keyframe fires
     */
    private void handleStepSound(String key, String locator) {
        if (dragon.isStayOrSitMuted()) return;
        // Trust Blockbench timing - play the sound immediately
        actuallyPlayStep(key, locator);
    }

    private void actuallyPlayStep(String which, String locator) {
        if (!dragon.level().isClientSide) return; // Client-side only

        // Heavier steps when running or carrying rider
        float weight = 1.0f;
        if (dragon.isRunning()) weight *= 1.2f;
        if (dragon.isVehicle()) weight *= 1.1f;
        if (dragon.getHealth() < dragon.getMaxHealth() * 0.5f) weight *= 0.9f;

        float volume = 0.65f * weight;
        float pitch = (0.9f + dragon.getRandom().nextFloat() * 0.2f) / weight;
        Vec3 at = resolveLocatorWorldPos(locator);

        double x = at != null ? at.x : dragon.getX();
        double y = at != null ? at.y : dragon.getY();
        double z = at != null ? at.z : dragon.getZ();

        // Let profile handle dragon-specific step sounds
        if (profile.handleStepSound(this, dragon, which, locator, x, y, z, volume, pitch)) {
            return;
        }

        // Fallback: use generic step sound if profile doesn't handle it
        dragon.level().playLocalSound(x, y, z, SoundEvents.GENERIC_SMALL_FALL,
                SoundSource.NEUTRAL, volume, pitch, false);
    }

    /**
     * Parses and plays sounds specified as namespace:soundid or namespace:soundid|vol|pitch
     * Uses client-side local playback for animation keyframe sounds.
     */
    private void handleAutoSoundSpec(String spec) {
        if (dragon.isStayOrSitMuted() || dragon.isSleeping()) return;
        if (!dragon.level().isClientSide) return; // Only on client for animation sounds
        if (spec == null) return;
        String[] parts = spec.split("\\|");
        String id = parts[0];
        float vol = 1.0f;
        float pitch = 1.0f;
        try {
            if (parts.length >= 2) vol = Float.parseFloat(parts[1]);
            if (parts.length >= 3) pitch = Float.parseFloat(parts[2]);
        } catch (Exception ignored) {}

        net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(id);
        if (rl == null) return;
        net.minecraft.sounds.SoundEvent evt = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(rl);
        if (evt == null) return;

        // Client-side local playback
        dragon.level().playLocalSound(dragon.getX(), dragon.getY(), dragon.getZ(),
                evt, SoundSource.NEUTRAL, vol, pitch, false);
    }

    /**
     * Server-side sound broadcast for vocals (hurt, die, ambient sounds).
     * NOT used for animation keyframe sounds - those use client-side playback.
     */
    private void playServerBroadcast(net.minecraft.sounds.SoundEvent sound, float volume, float pitch, Vec3 position) {
        if (dragon.level().isClientSide) return; // Server only

        double x = position != null ? position.x : dragon.getX();
        double y = position != null ? position.y : dragon.getY();
        double z = position != null ? position.z : dragon.getZ();

        // Server broadcasts to all nearby clients
        dragon.level().playSound(null, x, y, z, sound, SoundSource.NEUTRAL, volume, pitch);
    }

    /**
     * In our walk cycle, step1 = right foot, step2 = left foot as authored in the animation JSON.
     * If the GeckoLib event does not expose the locator directly, infer it from the key.
     */
    private String mapStepKeyToLocator(String key) {
        if (key == null) return null;
        String k = key.toLowerCase(java.util.Locale.ROOT).trim();
        if (k.endsWith("1")) return "rightfeetLocator";
        if (k.endsWith("2")) return "leftfeetLocator";
        return null;
    }

    /**
     * Get the world position for a locator from renderer-cached positions.
     * Returns null if locator is not found (will fall back to entity position).
     * NO hardcoded positions - each dragon's renderer calculates and caches these accurately.
     */
    public Vec3 resolveLocatorWorldPos(String locator) {
        if (locator == null || locator.isEmpty()) return null;

        // Special case: mouth position may have dynamic getter
        if ("mouth_origin".equals(locator)) {
            Vec3 dynamic = dragon.getMouthPosition();
            if (dynamic != null) return dynamic;
        }

        // Use renderer-sampled position (calculated from actual animated bone matrices)
        Vec3 cached = dragon.getClientLocatorPosition(locator);
        if (cached != null) return cached;

        // No fallback - if renderer hasn't cached it, return null
        // Sound will play at entity center position instead
        return null;
    }
    
    /**
     * Takeoff sound with urgency variation
     */
    private void handleTakeoffSound() {
        if (dragon.isStayOrSitMuted() || dragon.isSleeping() || dragon.isSleepTransitioning()) return;
        if (!dragon.level().isClientSide) return; // Client-side only
        float urgency = dragon.getTarget() != null ? 1.3f : 1.0f;
        // Use vanilla Ender Dragon flap for takeoff
        dragon.level().playLocalSound(dragon.getX(), dragon.getY(), dragon.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.NEUTRAL, urgency * 1.2f, 0.85f, false);
    }
    
    /**
     * Landing sound with impact variation
     */
    private void handleLandingSound() {
        if (dragon.isStayOrSitMuted() || dragon.isSleeping()) return;
        double impactSpeed = Math.abs(dragon.getDeltaMovement().y);
        float volume = (float) Math.max(0.8f, 1.0f + impactSpeed * 2.0f);
        float pitch = (float) Math.max(0.7f, 1.0f - impactSpeed * 0.3f);
        
        dragon.playSound(SoundEvents.GENERIC_EXPLODE, volume * 0.6f, pitch);
    }
}

