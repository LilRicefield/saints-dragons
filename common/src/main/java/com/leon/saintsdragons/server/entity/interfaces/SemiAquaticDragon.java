package com.leon.saintsdragons.server.entity.interfaces;

/**
 * Capability interface for dragons that are adapted to aquatic traversal.
 */
public interface SemiAquaticDragon {
    /**
     * @return desired swim speed in blocks per tick.
     */
    default double getSwimSpeed() {
        return 1.2D;
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
     * @return range in blocks when searching for nearby water targets.
     */
    default int getWaterSearchRange() {
        return 12;
    }
}
