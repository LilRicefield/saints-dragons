package com.leon.saintsdragons.server.entity.interfaces;

import com.leon.saintsdragons.server.entity.handler.HumanSoundHandler;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Clean, minimal sound profile interface for human NPCs.
 * Defines how an NPC responds to animation sound keyframes.
 *
 * This is much simpler than DragonSoundProfile since humans don't need
 * complex vocal systems, locator bones, or species-specific behaviors.
 */
public interface HumanSoundProfile {

    /**
     * Empty profile that doesn't play any sounds.
     */
    HumanSoundProfile EMPTY = new HumanSoundProfile() {};

    /**
     * Handle a sound keyframe from an animation.
     *
     * @param handler The sound handler instance
     * @param entity The entity playing the sound
     * @param soundKey The sound key from the animation keyframe
     * @param locator Optional locator name (e.g., "mouth", "feet")
     * @param volume Volume from keyframe (default 1.0)
     * @param pitch Pitch from keyframe (default 1.0)
     * @return true if the sound was handled, false to fall back to default behavior
     */
    default boolean handleSound(HumanSoundHandler handler, Mob entity, String soundKey,
                                String locator, float volume, float pitch) {
        return false;
    }

    /**
     * Resolve a locator name to a world position.
     * Called when the handler can't find a bone-based locator.
     *
     * @param handler The sound handler instance
     * @param entity The entity
     * @param locator The locator name
     * @return World position for the sound (defaults to entity position)
     */
    default Vec3 resolveLocator(HumanSoundHandler handler, Mob entity, String locator) {
        return entity.position();
    }
}
