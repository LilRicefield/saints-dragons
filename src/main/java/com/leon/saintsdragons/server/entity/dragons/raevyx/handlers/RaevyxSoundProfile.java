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
            Map.entry("raevyx_die", 62),
            Map.entry("baby_raevyx_hurt", 15),
            Map.entry("baby_raevyx_die", 40)
    );

    private static final Map<String, String> EFFECT_TO_VOCAL_KEY = Map.ofEntries(
            Map.entry("raevyx_grumble1", "grumble1"),
            Map.entry("raevyx_grumble2", "grumble2"),
            Map.entry("raevyx_grumble3", "grumble3"),
            Map.entry("raevyx_chuff", "chuff"),
            Map.entry("raevyx_content", "content"),
            Map.entry("raevyx_purr", "purr"),
            Map.entry("raevyx_roar", "roar"),
            Map.entry("raevyx_hurt", "raevyx_hurt"),
            Map.entry("raevyx_die", "raevyx_die"),
            Map.entry("baby_raevyx_hurt", "baby_raevyx_hurt"),
            Map.entry("baby_raevyx_die", "baby_raevyx_die")
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
            // Hurt/die sounds are handled by entity hurt/death methods, skip keyframe
            if ("roar".equals(vocalKey) || "raevyx_hurt".equals(vocalKey) || "raevyx_die".equals(vocalKey) ||
                "baby_raevyx_hurt".equals(vocalKey) || "baby_raevyx_die".equals(vocalKey)) {
                return true; // Block the keyframe, entity plays the sound
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

    @Override
    public boolean handleWingFlapSound(DragonSoundHandler handler, DragonEntity dragon, String key) {
        playWingFlap(handler, dragon, null);
        return true;
    }

    @Override
    public boolean handleStepSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator,
                                   double x, double y, double z, float volume, float pitch) {
        // Play the Raevyx step sound
        dragon.level().playLocalSound(x, y, z, ModSounds.RAEVYX_STEP.get(),
                SoundSource.NEUTRAL, volume, pitch, false);
        return true;
    }

    private void playWingFlap(DragonSoundHandler handler, DragonEntity dragon, String locator) {
        Vec3 bodyPos = handler.resolveLocatorWorldPos(
                locator != null && !locator.isEmpty() ? locator : "bodyLocator"
        );
        double flightSpeed = dragon.getCachedHorizontalSpeed();
        float pitch = 1.0f + (float) (flightSpeed * 0.3f);
        float volume = Math.max(0.6f, 0.9f + (float) (flightSpeed * 0.2f));

        playClientSound(dragon, bodyPos, ModSounds.RAEVYX_FLAP.get(), volume, pitch);
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
