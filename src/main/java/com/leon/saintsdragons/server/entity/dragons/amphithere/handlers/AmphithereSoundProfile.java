package com.leon.saintsdragons.server.entity.dragons.amphithere.handlers;

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
public final class AmphithereSoundProfile implements DragonSoundProfile {

    public static final AmphithereSoundProfile INSTANCE = new AmphithereSoundProfile();

    private static final Map<String, Integer> VOCAL_WINDOWS = Map.of(
            "amphithere_roar", 45,
            "amphithere_hurt", 20
    );

    private static final Map<String, DragonEntity.VocalEntry> FALLBACK_VOCALS =
            new DragonEntity.VocalEntryBuilder()
                    .add("amphithere_bite", "actions", "animation.amphithere.bite",
                            ModSounds.AMPHITHERE_BITE, 1.0f, 0.95f, 0.1f, false, false, false)
                    .build();

    private AmphithereSoundProfile() {}

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        // Handler already blocks server-side, we're only called on client

        if ("amphithere_bite".equals(key)) {
            Vec3 mouthPos = handler.resolveLocatorWorldPos("mouth_origin");
            float pitch = 0.95f + dragon.getRandom().nextFloat() * 0.1f;

            // Client-side local playback
            double x = mouthPos != null ? mouthPos.x : dragon.getX();
            double y = mouthPos != null ? mouthPos.y : dragon.getY();
            double z = mouthPos != null ? mouthPos.z : dragon.getZ();
            dragon.level().playLocalSound(x, y, z, ModSounds.AMPHITHERE_BITE.get(),
                    SoundSource.NEUTRAL, 1.0f, pitch, false);
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
}
