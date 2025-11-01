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

    /**
     * Handle dragon-specific wing flap sounds.
     * Called when a wing flap keyframe is detected in animations.
     *
     * @param handler The sound handler
     * @param dragon The dragon entity
     * @param key The flap sound key (e.g., "flap1", "flap_right")
     * @return {@code true} if handled; {@code false} to use generic fallback
     */
    default boolean handleWingFlapSound(DragonSoundHandler handler, DragonEntity dragon, String key) {
        return false;
    }

    /**
     * Handle dragon-specific footstep sounds.
     * Called when a step keyframe is detected in walk/run animations.
     *
     * @param handler The sound handler
     * @param dragon The dragon entity
     * @param key The step key (e.g., "step1", "step2", "run_step1", "run_step2")
     * @param locator The locator name for the foot position
     * @param x World X position (from locator or entity center)
     * @param y World Y position (from locator or entity center)
     * @param z World Z position (from locator or entity center)
     * @param volume Calculated volume based on weight/speed
     * @param pitch Calculated pitch based on weight/randomness
     * @return {@code true} if handled; {@code false} to use generic fallback
     */
    default boolean handleStepSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator,
                                   double x, double y, double z, float volume, float pitch) {
        return false;
    }
}
