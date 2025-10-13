package com.leon.saintsdragons.server.entity.dragons.amphithere.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Amphithere-specific hooks for sound playback.
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
        if ("amphithere_bite".equals(key)) {
            Vec3 mouthPos = handler.resolveLocatorWorldPos("mouth_origin");
            float pitch = 0.95f + dragon.getRandom().nextFloat() * 0.1f;
            handler.emitSound(ModSounds.AMPHITHERE_BITE.get(), 1.0f, pitch, mouthPos, false);
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
