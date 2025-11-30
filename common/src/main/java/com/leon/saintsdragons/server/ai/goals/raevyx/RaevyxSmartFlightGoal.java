package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.ai.goals.base.DragonSmartFlightGoal;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Raevyx flight behavior - FAST, aggressive, storm-loving lightning dragon.
 *
 * Flight characteristics:
 * - Large waypoint acceptance (5 blocks) for high-speed turns
 * - Aggressive stuck recovery (re-paths after 2 ticks)
 * - LOVES storms - flies higher and longer in bad weather
 * - Long flight range (50-130 blocks)
 * - Protects babies - won't abandon them
 */
public class RaevyxSmartFlightGoal extends DragonSmartFlightGoal<Raevyx> {

    public RaevyxSmartFlightGoal(Raevyx raevyx) {
        super(raevyx);
    }

    @Override
    protected double getWaypointAcceptanceRadiusSqr() {
        return 25.0; // 5 blocks - fast dragon needs larger acceptance
    }

    @Override
    protected int getStuckThreshold() {
        return 2; // Aggressive re-pathing
    }

    @Override
    protected int getLandingCooldownTicks() {
        return 100; // Longer cooldown - Raevyx stays airborne
    }

    @Override
    protected int getTakeoffChance(boolean thundering, boolean raining) {
        if (thundering) {
            return 4; // 25% - LOVES thunder
        } else if (raining) {
            return 8; // 12.5% - likes rain
        } else {
            return 80; // 1.25% - rare in clear weather
        }
    }

    @Override
    protected int getKeepFlyingChance(boolean thundering, boolean raining) {
        if (thundering) {
            return 3000; // ~2.5 minutes - flies LONG in storms
        } else if (raining) {
            return 1800; // ~90 seconds
        } else {
            return 200; // ~10 seconds - lands quickly in clear weather
        }
    }

    @Override
    protected int getFlightDecisionInterval(boolean thundering, boolean raining) {
        if (thundering) return 2; // Very frequent decisions in storms
        if (raining) return 8;
        return 25; // Normal interval
    }

    @Override
    protected double findSafeFlightHeight(double x, double z, boolean thundering, boolean raining) {
        int ix = (int) x;
        int iz = (int) z;
        int groundY = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);

        double base = 15.0 + dragon.getRandom().nextDouble() * 20.0; // 15-35 above surface

        // Raevyx LOVES storms - flies HIGHER in bad weather
        double capAboveGround = thundering ? 90.0 : (raining ? 70.0 : 50.0);

        double target = groundY + base;
        double cap = groundY + capAboveGround;
        double worldCap = dragon.level().getMaxBuildHeight() - 10.0;

        return Math.min(Math.min(target, cap), worldCap);
    }

    @Override
    protected float[] getFlightRange(boolean isStuck) {
        if (isStuck) {
            return new float[]{30.0f, 70.0f}; // 30-70 blocks when stuck
        } else {
            return new float[]{50.0f, 130.0f}; // 50-130 blocks - LONG range
        }
    }

    @Override
    protected boolean shouldProtectBabies() {
        return true; // Raevyx protects babies
    }
}
