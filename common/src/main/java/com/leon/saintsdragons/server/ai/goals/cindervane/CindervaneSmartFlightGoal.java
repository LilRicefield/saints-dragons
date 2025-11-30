package com.leon.saintsdragons.server.ai.goals.cindervane;

import com.leon.saintsdragons.server.ai.goals.base.DragonSmartFlightGoal;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Cindervane flight behavior - ELEGANT, graceful, day-loving sky dragon.
 *
 * Flight characteristics:
 * - Medium waypoint acceptance (4 blocks) for precise navigation
 * - Patient stuck recovery (re-paths after 3 ticks)
 * - AVOIDS storms - flies lower and lands quicker in bad weather
 * - Medium flight range (80-200 blocks)
 * - Prefers daytime flight (won't takeoff at night unless tamed)
 * - "Wander" command: stays near owner (tethered mode)
 */
public class CindervaneSmartFlightGoal extends DragonSmartFlightGoal<Cindervane> {

    public CindervaneSmartFlightGoal(Cindervane cindervane) {
        super(cindervane);
    }

    @Override
    protected double getWaypointAcceptanceRadiusSqr() {
        return 16.0; // 4 blocks - precise navigation
    }

    @Override
    protected int getStuckThreshold() {
        return 3; // Patient re-pathing
    }

    @Override
    protected int getLandingCooldownTicks() {
        return 40; // Shorter cooldown - lands more frequently
    }

    @Override
    protected int getTakeoffChance(boolean thundering, boolean raining) {
        if (thundering) {
            return 200; // 0.5% - HATES thunder
        } else if (raining) {
            return 100; // 1% - dislikes rain
        } else {
            return 40; // 2.5% - prefers clear weather
        }
    }

    @Override
    protected int getKeepFlyingChance(boolean thundering, boolean raining) {
        if (thundering) {
            return 200; // ~10 seconds - lands quickly in storms
        } else if (raining) {
            return 400; // ~20 seconds
        } else {
            return 3600; // ~3 minutes - flies long in clear weather
        }
    }

    @Override
    protected int getFlightDecisionInterval(boolean thundering, boolean raining) {
        if (thundering) return 2;
        if (raining) return 5;
        return 8; // Frequent decisions
    }

    @Override
    protected double findSafeFlightHeight(double x, double z, boolean thundering, boolean raining) {
        int ix = (int) x;
        int iz = (int) z;
        int groundY = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);

        boolean tethered = isTamedWander();
        double base = tethered ? 12.0 + dragon.getRandom().nextDouble() * 12.0 :
                                 25.0 + dragon.getRandom().nextDouble() * 35.0;

        // Cindervane AVOIDS storms - flies LOWER in bad weather
        double capAboveGround;
        if (tethered) {
            capAboveGround = thundering ? 12.0 : (raining ? 18.0 : 32.0);
        } else {
            capAboveGround = thundering ? 20.0 : (raining ? 30.0 : 80.0);
        }

        double target = groundY + base;
        double cap = groundY + capAboveGround;
        double worldCap = dragon.level().getMaxBuildHeight() - 10.0;

        return Math.min(Math.min(target, cap), worldCap);
    }

    @Override
    protected float[] getFlightRange(boolean isStuck) {
        if (isStuck) {
            return new float[]{40.0f, 100.0f}; // 40-100 blocks when stuck
        } else {
            return new float[]{80.0f, 200.0f}; // 80-200 blocks - medium range
        }
    }

    @Override
    protected boolean shouldProtectBabies() {
        return false; // Cindervane doesn't protect babies
    }

    // ===== CINDERVANE-SPECIFIC BEHAVIOR =====

    @Override
    protected boolean additionalCanUseChecks() {
        // Wild Cindervane doesn't fly at night
        if (!dragon.isTame()) {
            long dayTime = dragon.level().getDayTime() % 24000;
            boolean isNight = dayTime >= 13000 && dayTime < 23000;
            if (isNight) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean additionalCanContinueChecks() {
        // Wild Cindervane lands at night
        if (!dragon.isTame()) {
            long dayTime = dragon.level().getDayTime() % 24000;
            boolean isNight = dayTime >= 13000 && dayTime < 23000;
            if (isNight) {
                return dragon.getRandom().nextInt(100) != 0; // 99% chance to continue (gradual landing)
            }
        }
        return true;
    }

    @Override
    protected Vec3 generateFlightCandidate(Vec3 dragonPos, int attempt) {
        boolean tethered = isTamedWander();

        if (tethered) {
            // Tethered mode: circle around owner
            Vec3 anchor = getFlightAnchor();
            double min = 10.0 + dragon.getRandom().nextDouble() * 6.0;
            double max = 24.0 + dragon.getRandom().nextDouble() * 6.0;
            double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0;
            double radius = min + dragon.getRandom().nextDouble() * (max - min);
            double cx = anchor.x + Math.cos(angle) * radius;
            double cz = anchor.z + Math.sin(angle) * radius;

            boolean thundering = dragon.level().isThundering();
            boolean raining = !thundering && dragon.level().isRaining();
            double targetY = findSafeFlightHeight(cx, cz, thundering, raining);
            return new Vec3(cx, targetY, cz);
        } else {
            // Normal mode: use base implementation
            return super.generateFlightCandidate(dragonPos, attempt);
        }
    }

    @Override
    protected void onFlightStart() {
        // Cindervane doesn't set takeoff flag when tethered
        if (isTamedWander()) {
            dragon.setTakeoff(false);
        }
    }

    @Override
    protected void onFlightStop() {
        dragon.markLandedNow();
    }

    // ===== HELPER METHODS =====

    private boolean isTamedWander() {
        return dragon.isTame() && dragon.getCommand() == 2 && dragon.getOwner() != null;
    }

    private Vec3 getFlightAnchor() {
        if (isTamedWander()) {
            LivingEntity owner = dragon.getOwner();
            if (owner != null) {
                return owner.position();
            }
        }
        return dragon.position();
    }
}
