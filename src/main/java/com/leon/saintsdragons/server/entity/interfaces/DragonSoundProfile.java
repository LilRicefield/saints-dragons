package com.leon.saintsdragons.server.entity.interfaces;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;

/**
 * Defines dragon-specific sound customisations that extend the shared logic in {@link DragonSoundHandler}.
 * Implementations should focus on species data (sound keys, durations, fallback vocals) and defer the
 * common routing/timing responsibilities to the handler.
 */
public interface DragonSoundProfile {

    DragonSoundProfile EMPTY = new DragonSoundProfile() {};

    /**
     * @return {@code true} if the sound key was handled and no further processing is required.
     */
    default boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        return false;
    }

    /**
     * Allows per-dragon handling for ad-hoc sound triggers from gameplay code (non-animation context).
     *
     * @return {@code true} if handled; {@code false} to fall back to shared logic.
     */
    default boolean handleSoundByName(DragonSoundHandler handler, DragonEntity dragon, String key) {
        return false;
    }

    /**
     * Supplies an optional fallback {@link DragonEntity.VocalEntry} when a dragon does not expose the key
     * via {@link DragonEntity#getVocalEntries()}.
     *
     * @return vocal entry or {@code null} if the handler should treat the key as unknown.
     */
    default DragonEntity.VocalEntry getFallbackVocalEntry(String key) {
        return null;
    }

    /**
     * Provides a species-specific animation window for synchronising vocals with action animations.
     *
     * @return window (ticks) or {@code -1} to use the shared default.
     */
    default int getVocalAnimationWindowTicks(String key) {
        return -1;
    }
}
