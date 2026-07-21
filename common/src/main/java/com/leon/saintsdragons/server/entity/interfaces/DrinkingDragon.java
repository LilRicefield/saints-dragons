package com.leon.saintsdragons.server.entity.interfaces;

public interface DrinkingDragon {
    int getDrinkingDurationTicks();

    double getDrinkingReach();

    void startDrinkingAnimation();

    void stopDrinkingAnimation();
}
