package com.leon.saintsdragons.server.entity.dragons.stegonaut.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;

import java.util.Map;

/**
 * Primitive Drake specific vocal metadata.
 */
public final class StegonautSoundProfile implements DragonSoundProfile {

    public static final StegonautSoundProfile INSTANCE = new StegonautSoundProfile();

    private static final Map<String, Integer> VOCAL_WINDOWS = Map.of(
            "stegonaut_grumble1", 40,
            "stegonaut_grumble2", 60,
            "stegonaut_grumble3", 30
    );

    private static final Map<String, DragonEntity.VocalEntry> FALLBACK_VOCALS =
            new DragonEntity.VocalEntryBuilder()
                    .add("stegonaut_grumble1", "action", "animation.stegonaut.grumble1",
                            ModSounds.STEGONAUT_GRUMBLE_1, 0.6f, 1.1f, 0.2f, false, false, false)
                    .add("stegonaut_grumble2", "action", "animation.stegonaut.grumble2",
                            ModSounds.STEGONAUT_GRUMBLE_2, 0.6f, 1.1f, 0.2f, false, false, false)
                    .add("stegonaut_grumble3", "action", "animation.stegonaut.grumble3",
                            ModSounds.STEGONAUT_GRUMBLE_3, 0.6f, 1.1f, 0.2f, false, false, false)
                    .build();

    private StegonautSoundProfile() {}

    @Override
    public DragonEntity.VocalEntry getFallbackVocalEntry(String key) {
        return FALLBACK_VOCALS.get(key);
    }

    @Override
    public int getVocalAnimationWindowTicks(String key) {
        return VOCAL_WINDOWS.getOrDefault(key, -1);
    }
}
