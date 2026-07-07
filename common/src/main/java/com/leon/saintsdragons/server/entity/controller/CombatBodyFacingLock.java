package com.leon.saintsdragons.server.entity.controller;

public interface CombatBodyFacingLock {
    boolean isCombatBodyFacingLocked();

    float getCombatBodyFacingYaw();

    default float getCombatBodyFacingTurnSpeed() {
        return 18.0F;
    }
}
