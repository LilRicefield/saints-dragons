package com.leon.saintsdragons.server.entity.interfaces;


public interface SemiAquaticDragon {
    default double getSwimSpeed() {
        return 1.2D;
    }
    default boolean shouldEnterWater() {
        return false;
    }
    default boolean shouldLeaveWater() {
        return false;
    }
    default int getWaterSearchRange() {
        return 12;
    }
}
