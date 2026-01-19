package com.leon.saintsdragons.server.entity.dragons.cindervane.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Amphithere-specific hooks for sound playback.
 * Uses client-side local playback for animation keyframe sounds.
 */
public final class CindervaneSoundProfile implements DragonSoundProfile {

    public static final CindervaneSoundProfile INSTANCE = new CindervaneSoundProfile();

    private static final Map<String, Integer> VOCAL_WINDOWS = Map.ofEntries(
            Map.entry("grumble1", 90),
            Map.entry("grumble2", 150),
            Map.entry("grumble3", 75),
            Map.entry("roar", 45),
            Map.entry("cindervane_hurt", 20),
            Map.entry("cindervane_die", 95)
    );

    private static final Map<String, String> EFFECT_TO_VOCAL_KEY = Map.ofEntries(
            Map.entry("cindervane_grumble1", "grumble1"),
            Map.entry("cindervane_grumble2", "grumble2"),
            Map.entry("cindervane_grumble3", "grumble3"),
            Map.entry("cindervane_roar", "roar"),
            Map.entry("cindervane_hurt", "cindervane_hurt"),
            Map.entry("cindervane_die", "cindervane_die")
    );

    private static final Map<String, DragonEntity.VocalEntry> FALLBACK_VOCALS =
            new DragonEntity.VocalEntryBuilder()
                    .add("cindervane_bite", "actions", "animation.cindervane.bite",
                            ModSounds.CINDERVANE_BITE, 1.0f, 0.95f, 0.1f, false, false, false)
                    .build();

    private CindervaneSoundProfile() {}

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        // Handler already blocks server-side, we're only called on client

        // Check if this is a vocal key that should use the vocal entry system
        String vocalKey = EFFECT_TO_VOCAL_KEY.get(key);
        if (vocalKey != null) {
            playVocalEntry(handler, dragon, vocalKey, locator);
            return true;
        }

        // Handle non-vocal animation sounds
        return switch (key) {
            case "cindervane_magma_blast" -> {
                playSimpleSound(handler, dragon, locator, ModSounds.CINDERVANE_MAGMA_BLAST.get(), 2.0f, 1.0f, 0.0f);
                yield true;
            }
            case "cindervane_run" -> {
                playSimpleSound(handler, dragon, locator, ModSounds.CINDERVANE_RUN.get(), 2.0f, 1.0f, 0.0f);
                yield true;
            }
            case "cindervane_bite" -> {
                playSimpleSound(handler, dragon, "mouth_origin", ModSounds.CINDERVANE_BITE.get(), 1.0f, 0.95f, 0.1f);
                yield true;
            }
            case "cindervane_landed" -> {
                playSimpleSound(handler, dragon, locator, ModSounds.CINDERVANE_LANDED.get(), 2.0f, 1.0f, 0.0f);
                yield true;
            }
            case "cindervane_eat" -> {
                playSimpleSound(handler, dragon, "mouth_origin", ModSounds.CINDERVANE_EAT.get(), 1.0f, 1.0f, 0.0f);
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public DragonEntity.VocalEntry getFallbackVocalEntry(String key) {
        return FALLBACK_VOCALS.get(key);
    }

    @Override
    public int getVocalAnimationWindowTicks(String key) {
        return VOCAL_WINDOWS.getOrDefault(key, -1);
    }

    @Override
    public boolean handleWingFlapSound(DragonSoundHandler handler, DragonEntity dragon, String key) {
        float volume = dragon.isBaby() ? 0.6f : 1.1f;
        float pitch = 0.98f + (dragon.getRandom().nextFloat() - 0.5f) * 0.1f;
        dragon.level().playLocalSound(dragon.getX(), dragon.getY(), dragon.getZ(),
                ModSounds.CINDERVANE_FLAP.get(), SoundSource.NEUTRAL, volume, pitch, false);
        return true;
    }

    /**
     * Play vocal entry with proper positioning and pitch variation.
     * Follows Raevyx approach with sleep/sitting state checks.
     */
    private void playVocalEntry(DragonSoundHandler handler, DragonEntity dragon, String vocalKey, String locator) {
        DragonEntity.VocalEntry entry = dragon.getVocalEntries().get(vocalKey);
        if (entry == null) {
            entry = FALLBACK_VOCALS.get(vocalKey);
        }
        if (entry == null) {
            return;
        }

        // Check if allowed during sleep/sitting
        if (!entry.allowDuringSleep() && (dragon.isSleeping() || dragon.isSleepTransitioning())) {
            return;
        }
        if (!entry.allowWhenSitting() && dragon.isStayOrSitMuted()) {
            return;
        }

        // Resolve position (use mouth_origin for vocals, or entity position as fallback)
        Vec3 at = handler.resolveLocatorWorldPos(locator != null && !locator.isEmpty() ? locator : "mouth_origin");

        // Calculate pitch with variance
        float pitch = entry.basePitch();
        if (entry.pitchVariance() != 0f) {
            pitch += dragon.getRandom().nextFloat() * entry.pitchVariance();
        }

        playClientSound(dragon, at, entry.soundSupplier().get(), entry.volume(), pitch);
    }

    /**
     * Play simple sound with locator support and pitch variance.
     */
    private void playSimpleSound(DragonSoundHandler handler, DragonEntity dragon, String locator,
                                  net.minecraft.sounds.SoundEvent sound, float volume, float basePitch, float variance) {
        Vec3 at = handler.resolveLocatorWorldPos(locator != null && !locator.isEmpty() ? locator : "mouth_origin");
        float pitch = basePitch;
        if (variance != 0f) {
            pitch += dragon.getRandom().nextFloat() * variance;
        }

        playClientSound(dragon, at, sound, volume, pitch);
    }

    /**
     * Play sound on client side using local playback.
     * More efficient than server broadcast for animation keyframe sounds.
     */
    private void playClientSound(DragonEntity dragon, Vec3 position, net.minecraft.sounds.SoundEvent sound,
                                 float volume, float pitch) {
        double x = position != null ? position.x : dragon.getX();
        double y = position != null ? position.y : dragon.getY();
        double z = position != null ? position.z : dragon.getZ();

        dragon.level().playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, volume, pitch, false);
    }
}
