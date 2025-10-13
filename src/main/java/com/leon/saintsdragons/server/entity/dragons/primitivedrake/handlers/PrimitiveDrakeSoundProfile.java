package com.leon.saintsdragons.server.entity.dragons.primitivedrake.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;

import java.util.Map;

/**
 * Primitive Drake specific vocal metadata.
 */
public final class PrimitiveDrakeSoundProfile implements DragonSoundProfile {

    public static final PrimitiveDrakeSoundProfile INSTANCE = new PrimitiveDrakeSoundProfile();

    private static final Map<String, Integer> VOCAL_WINDOWS = Map.of(
            "primitivedrake_grumble1", 40,
            "primitivedrake_grumble2", 60,
            "primitivedrake_grumble3", 30
    );

    private static final Map<String, DragonEntity.VocalEntry> FALLBACK_VOCALS =
            new DragonEntity.VocalEntryBuilder()
                    .add("primitivedrake_grumble1", "action", "animation.primitive_drake.grumble1",
                            ModSounds.PRIMITIVE_DRAKE_GRUMBLE_1, 0.6f, 1.1f, 0.2f, false, false, false)
                    .add("primitivedrake_grumble2", "action", "animation.primitive_drake.grumble2",
                            ModSounds.PRIMITIVE_DRAKE_GRUMBLE_2, 0.6f, 1.1f, 0.2f, false, false, false)
                    .add("primitivedrake_grumble3", "action", "animation.primitive_drake.grumble3",
                            ModSounds.PRIMITIVE_DRAKE_GRUMBLE_3, 0.6f, 1.1f, 0.2f, false, false, false)
                    .build();

    private PrimitiveDrakeSoundProfile() {}

    @Override
    public DragonEntity.VocalEntry getFallbackVocalEntry(String key) {
        return FALLBACK_VOCALS.get(key);
    }

    @Override
    public int getVocalAnimationWindowTicks(String key) {
        return VOCAL_WINDOWS.getOrDefault(key, -1);
    }
}
