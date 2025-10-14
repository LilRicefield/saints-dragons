package com.leon.saintsdragons.server.entity.dragons.raevyx.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;

/**
 * Raevyx-specific vocal timing metadata and animation sound routing.
 * Uses client-side local playback for animation keyframe sounds (more efficient).
 */
public final class RaevyxSoundProfile implements DragonSoundProfile {

    public static final RaevyxSoundProfile INSTANCE = new RaevyxSoundProfile();

    private static final Map<String, Integer> VOCAL_WINDOWS = Map.ofEntries(
            Map.entry("grumble1", 120),
            Map.entry("grumble2", 180),
            Map.entry("grumble3", 60),
            Map.entry("content", 100),
            Map.entry("purr", 110),
            Map.entry("snort", 24),
            Map.entry("chuff", 28),
            Map.entry("roar", 69),
            Map.entry("roar_ground", 69),
            Map.entry("roar_air", 69),
            Map.entry("raevyx_hurt", 20),
            Map.entry("raevyx_die", 62)
    );

    private static final Map<String, String> EFFECT_TO_VOCAL_KEY = Map.of(
            "raevyx_grumble1", "grumble1",
            "raevyx_grumble2", "grumble2",
            "raevyx_grumble3", "grumble3",
            "raevyx_chuff", "chuff",
            "raevyx_content", "content",
            "raevyx_purr", "purr",
            "raevyx_roar", "roar",
            "raevyx_hurt", "raevyx_hurt",
            "raevyx_die", "raevyx_die"
    );

    private static final Set<String> STEP_KEYS = Set.of(
            "raevyx_step1", "raevyx_step2",
            "raevyx_run_step1", "raevyx_run_step2"
    );

    private RaevyxSoundProfile() {}

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        // Handler already blocks server-side, we're only called on client

        if (key.startsWith("raevyx_flap")) {
            playWingFlap(handler, dragon, locator);
            return true;
        }
        if (STEP_KEYS.contains(key)) {
            return false; // let shared handler normalise & process footsteps
        }
        String vocalKey = EFFECT_TO_VOCAL_KEY.get(key);
        if (vocalKey != null) {
            // Roar sound is handled by RaevyxRoarAbility with precise timing, skip keyframe
            if ("roar".equals(vocalKey)) {
                return true; // Block the keyframe, ability plays the sound
            }
            playVocalEntry(handler, dragon, vocalKey, locator);
            return true;
        }
        return switch (key) {
            case "raevyx_bite" -> {
                playSimpleMouthSound(handler, dragon, locator, ModSounds.RAEVYX_BITE.get(), 1.0f, 0.95f, 0.1f);
                yield true;
            }
            case "raevyx_horngore" -> {
                playSimpleMouthSound(handler, dragon, locator, ModSounds.RAEVYX_HORNGORE.get(), 1.3f, 0.9f, 0.2f);
                yield true;
            }
            case "raevyx_summon_storm" -> {
                playSummonStorm(handler, dragon, locator);
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public boolean handleSoundByName(DragonSoundHandler handler, DragonEntity dragon, String key) {
        // Handler already blocks server-side, we're only called on client
        if (key.startsWith("raevyx_flap")) {
            playWingFlap(handler, dragon, null);
            return true;
        }
        return false;
    }

    @Override
    public int getVocalAnimationWindowTicks(String key) {
        return VOCAL_WINDOWS.getOrDefault(key, -1);
    }

    private void playWingFlap(DragonSoundHandler handler, DragonEntity dragon, String locator) {
        Vec3 bodyPos = handler.resolveLocatorWorldPos(
                locator != null && !locator.isEmpty() ? locator : "bodyLocator"
        );
        double flightSpeed = dragon.getCachedHorizontalSpeed();
        float pitch = 1.0f + (float) (flightSpeed * 0.3f);
        float volume = Math.max(0.6f, 0.9f + (float) (flightSpeed * 0.2f));

        playClientSound(dragon, bodyPos, ModSounds.RAEVYX_FLAP1.get(), volume, pitch);
    }

    private void playVocalEntry(DragonSoundHandler handler, DragonEntity dragon, String vocalKey, String locator) {
        DragonEntity.VocalEntry entry = dragon.getVocalEntries().get(vocalKey);
        if (entry == null) {
            return;
        }
        if (!entry.allowDuringSleep() && (dragon.isSleeping() || dragon.isSleepTransitioning())) {
            return;
        }
        if (!entry.allowWhenSitting() && dragon.isStayOrSitMuted()) {
            return;
        }
        Vec3 at = handler.resolveLocatorWorldPos(locator != null && !locator.isEmpty() ? locator : "mouth_origin");
        float pitch = entry.basePitch();
        if (entry.pitchVariance() != 0f) {
            pitch += dragon.getRandom().nextFloat() * entry.pitchVariance();
        }

        playClientSound(dragon, at, entry.soundSupplier().get(), entry.volume(), pitch);
    }

    private void playSimpleMouthSound(DragonSoundHandler handler, DragonEntity dragon, String locator,
                                      net.minecraft.sounds.SoundEvent sound, float volume, float basePitch, float variance) {
        Vec3 at = handler.resolveLocatorWorldPos(locator != null && !locator.isEmpty() ? locator : "mouth_origin");
        float pitch = basePitch;
        if (variance != 0f) {
            pitch += dragon.getRandom().nextFloat() * variance;
        }

        playClientSound(dragon, at, sound, volume, pitch);
    }

    private void playSummonStorm(DragonSoundHandler handler, DragonEntity dragon, String locator) {
        Vec3 at = handler.resolveLocatorWorldPos(locator != null && !locator.isEmpty() ? locator : "mouth_origin");

        playClientSound(dragon, at, ModSounds.RAEVYX_SUMMON_STORM.get(), 1.6f, 1.0f);
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
