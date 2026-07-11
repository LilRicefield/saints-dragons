package com.leon.saintsdragons.server.entity.interfaces;

import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;

public interface SemiAquaticDragon {
    AsyncSwimController getAiSwimController();

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
