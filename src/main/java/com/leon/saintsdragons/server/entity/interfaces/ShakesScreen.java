package com.leon.saintsdragons.server.entity.interfaces;

import net.minecraft.world.entity.Entity;

/**
 * Interface for entities that can shake the screen when performing certain actions.
 * Used by dragons during powerful abilities like Roar and Summon Storm.
 */
public interface ShakesScreen {

    /**
     * Determines if a player can feel the screen shake from this entity.
     * Default implementation only allows shaking if the player is on the ground.
     * 
     * @param player The player entity to check
     * @return true if the player can feel the shake
     */
    default boolean canFeelShake(Entity player) {
        return player.onGround();
    }

    /**
     * Gets the current screen shake intensity for smooth interpolation.
     * 
     * @param partialTicks The partial tick for interpolation
     * @return The shake intensity (0.0 to 1.0+)
     */
    float getScreenShakeAmount(float partialTicks);

    /**
     * Gets the maximum distance at which players can feel the screen shake.
     * 
     * @return The shake distance in blocks
     */
    default double getShakeDistance() {
        return 20.0;
    }
}
