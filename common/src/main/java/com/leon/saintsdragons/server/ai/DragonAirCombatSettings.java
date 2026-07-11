package com.leon.saintsdragons.server.ai;

public record DragonAirCombatSettings(int takeoffAnimationTicks,
                                      double landingSpeed,
                                      int lostSightLandingTicks,
                                      double fallbackFollowRange,
                                      double targetAirborneHeight,
                                      double takeoffTargetMinHeightAboveGround,
                                      double takeoffTargetMinHeightAboveDragon) {
}
