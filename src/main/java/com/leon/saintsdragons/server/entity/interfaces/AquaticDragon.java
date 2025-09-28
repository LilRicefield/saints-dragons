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

    /**
     * @return true when the entity should actively seek a water source.
     */
    default boolean shouldEnterWater() {
        return false;
    }

    /**
     * @return true when the entity should move back onto land or to the surface.
     */
    default boolean shouldLeaveWater() {
        return false;
    }

    /**
     * @return true if the entity should halt locomotion (e.g., basking or sitting states).
     */
    default boolean shouldStopMoving() {
        return false;
    }

    /**
     * @return range in blocks when searching for nearby water targets.
     */
    default int getWaterSearchRange() {
        return 12;
    }
}
