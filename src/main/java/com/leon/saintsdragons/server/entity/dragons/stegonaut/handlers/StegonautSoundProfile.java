package com.leon.saintsdragons.server.entity.dragons.stegonaut.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Primitive Drake specific vocal metadata.
 */
public final class StegonautSoundProfile implements DragonSoundProfile {

    public static final StegonautSoundProfile INSTANCE = new StegonautSoundProfile();

    private static final Map<String, Integer> VOCAL_WINDOWS = Map.ofEntries(
            Map.entry("grumble1", 40),
            Map.entry("grumble2", 60),
            Map.entry("grumble3", 30),
            Map.entry("hurt", 20),
            Map.entry("die", 75)
    );

    private static final Map<String, DragonEntity.VocalEntry> FALLBACK_VOCALS =
            new DragonEntity.VocalEntryBuilder()
                    .add("grumble1", "action", "animation.stegonaut.grumble1",
                            ModSounds.STEGONAUT_GRUMBLE_1, 0.6f, 1.1f, 0.2f, false, false, false)
                    .add("grumble2", "action", "animation.stegonaut.grumble2",
                            ModSounds.STEGONAUT_GRUMBLE_2, 0.6f, 1.1f, 0.2f, false, false, false)
                    .add("grumble3", "action", "animation.stegonaut.grumble3",
                            ModSounds.STEGONAUT_GRUMBLE_3, 0.6f, 1.1f, 0.2f, false, false, false)
                    .add("hurt", "action", "animation.stegonaut.hurt",
                            ModSounds.STEGONAUT_HURT, 1.0f, 0.95f, 0.1f, false, true, true)
                    .add("die", "action", "animation.stegonaut.die",
                            ModSounds.STEGONAUT_DIE, 1.2f, 1.0f, 0.0f, false, true, true)
                    .build();

    private static final Map<String, String> EFFECT_TO_VOCAL_KEY = Map.ofEntries(
            Map.entry("stegonaut_grumble1", "grumble1"),
            Map.entry("stegonaut_grumble2", "grumble2"),
            Map.entry("stegonaut_grumble3", "grumble3"),
            Map.entry("stegonaut_hurt", "hurt"),
            Map.entry("stegonaut_die", "die")
    );

    private StegonautSoundProfile() {}

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        // Handler already blocks server-side, we're only called on client
        String vocalKey = EFFECT_TO_VOCAL_KEY.get(key);
        if (vocalKey != null) {
            playVocalEntry(handler, dragon, vocalKey, locator);
            return true;
        }

        return false;
    }

    @Override
    public boolean handleSoundByName(DragonSoundHandler handler, DragonEntity dragon, String key) {
        // Handler already blocks server-side, we're only called on client
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

    /**
     * Play vocal entry with proper positioning and pitch variation
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

        // Play sound on client side using local playback
        playClientSound(dragon, at, entry.soundSupplier().get(), entry.volume(), pitch);
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
