package com.leon.saintsdragons.server.entity.dragons.raevyx.handlers;

import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;

import java.util.Map;

/**
 * Raevyx-specific vocal timing metadata.
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

    private RaevyxSoundProfile() {}

    @Override
    public int getVocalAnimationWindowTicks(String key) {
        return VOCAL_WINDOWS.getOrDefault(key, -1);
    }
}
