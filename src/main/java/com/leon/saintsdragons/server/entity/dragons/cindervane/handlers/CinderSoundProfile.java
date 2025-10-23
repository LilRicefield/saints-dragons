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
public final class CinderSoundProfile implements DragonSoundProfile {

    public static final CinderSoundProfile INSTANCE = new CinderSoundProfile();

    private static final Map<String, Integer> VOCAL_WINDOWS = Map.ofEntries(
            Map.entry("grumble1", 90),
            Map.entry("grumble2", 150),
            Map.entry("grumble3", 75),
            Map.entry("roar", 45),
            Map.entry("roar_ground", 45),
            Map.entry("roar_air", 45),
            Map.entry("cindervane_hurt", 20),
            Map.entry("cindervane_die", 95)
    );

    private static final Map<String, String> EFFECT_TO_VOCAL_KEY = Map.ofEntries(
            Map.entry("cindervane_grumble1", "grumble1"),
            Map.entry("cindervane_grumble2", "grumble2"),
            Map.entry("cindervane_grumble3", "grumble3"),
            Map.entry("cindervane_roar", "roar"),
            Map.entry("roar_ground", "roar_ground"),
            Map.entry("roar_air", "roar_air"),
            Map.entry("cindervane_hurt", "cindervane_hurt"),
            Map.entry("cindervane_die", "cindervane_die")
    );

    private static final Map<String, DragonEntity.VocalEntry> FALLBACK_VOCALS =
            new DragonEntity.VocalEntryBuilder()
                    .add("cindervane_bite", "actions", "animation.cindervane.bite",
                            ModSounds.CINDERVANE_BITE, 1.0f, 0.95f, 0.1f, false, false, false)
                    .build();

    private CinderSoundProfile() {}

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        // Handler already blocks server-side, we're only called on client

        if ("cindervane_bite".equals(key)) {
            Vec3 mouthPos = handler.resolveLocatorWorldPos("mouth_origin");
            float pitch = 0.95f + dragon.getRandom().nextFloat() * 0.1f;

            // Client-side local playback
            double x = mouthPos != null ? mouthPos.x : dragon.getX();
            double y = mouthPos != null ? mouthPos.y : dragon.getY();
            double z = mouthPos != null ? mouthPos.z : dragon.getZ();
            dragon.level().playLocalSound(x, y, z, ModSounds.CINDERVANE_BITE.get(),
                    SoundSource.NEUTRAL, 1.0f, pitch, false);
            return true;
        }

        if ("cindervane_hurt".equals(key)) {
            return true; // Server now broadcasts hurt vocal immediately
        }

        String vocalKey = EFFECT_TO_VOCAL_KEY.get(key);
        if (vocalKey != null) {
            playVocalEntry(handler, dragon, vocalKey, locator);
            return true;
        }

        return false;
    }

    @Override
    public DragonEntity.VocalEntry getFallbackVocalEntry(String key) {
        return FALLBACK_VOCALS.get(key);
    }

    @Override
    public int getVocalAnimationWindowTicks(String key) {
        return VOCAL_WINDOWS.getOrDefault(key, -1);
    }

    private void playVocalEntry(DragonSoundHandler handler, DragonEntity dragon, String vocalKey, String locator) {
        DragonEntity.VocalEntry entry = dragon.getVocalEntries().get(vocalKey);
        if (entry == null) {
            entry = FALLBACK_VOCALS.get(vocalKey);
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

        Vec3 at = handler.resolveLocatorWorldPos(locator != null && !locator.isEmpty() ? locator.trim() : "mouth_origin");
        float pitch = entry.basePitch();
        if (entry.pitchVariance() != 0f) {
            pitch += dragon.getRandom().nextFloat() * entry.pitchVariance();
        }

        double x = at != null ? at.x : dragon.getX();
        double y = at != null ? at.y : dragon.getY();
        double z = at != null ? at.z : dragon.getZ();

        dragon.level().playLocalSound(x, y, z, entry.soundSupplier().get(),
                SoundSource.NEUTRAL, entry.volume(), pitch, false);
    }
}
