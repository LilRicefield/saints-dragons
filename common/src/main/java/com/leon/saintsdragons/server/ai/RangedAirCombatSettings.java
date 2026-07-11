package com.leon.saintsdragons.server.ai;

public record RangedAirCombatSettings(double directChaseSpeed,
                                      double diveChaseSpeed,
                                      double diveMinHeightAdvantage,
                                      double diveMaxHorizontalDistance,
                                      double meleeRange,
                                      double meleeApproachDistance,
                                      double rangedMinRange,
                                      double rangedMaxRange,
                                      double engagementDistance,
                                      int meleeAttackCooldownTicks,
                                      int rangedAttackCooldownTicks,
                                      int rangedCooldownTicks) {
}
