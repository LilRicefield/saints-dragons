package com.leon.saintsdragons.server.entity.interfaces;

import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

/**
 * Capability interface for dragons that are adapted to aquatic traversal.
 */
public interface AquaticDragon {

    /**
     * @return true if the dragon can breathe underwater without drowning.
     */
    default boolean canBreatheUnderwater() {
        return true;
    }

    /**
     * @return desired swim speed in blocks per tick.
     */
    default double getSwimSpeed() {
        return 1.2D;
    }

    /**
     * Additional buoyancy vector applied while swimming.
     */
    default Vec3 getBuoyancyVector() {
        return new Vec3(0, 0.02, 0);
    }

    /**
     * @return navigation instance optimized for water pathfinding.
     */
    PathNavigation getAquaticNavigation();

    /**
     * Called when the dragon transitions into water.
     */
    default void onEnterWater() {
    }

    /**
     * Called when the dragon leaves water.
     */
    default void onExitWater() {
    }
}
