package com.leon.saintsdragons.server.ai.goals.ignivorus;

import com.leon.saintsdragons.server.ai.goals.base.DragonSmartFlightGoal;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ignivorus flight behavior - BOLD, fearless, fire-breathing predator.
 *
 * Flight characteristics:
 * - Medium waypoint acceptance (4 blocks) for balanced navigation
 * - Patient stuck recovery (re-paths after 3 ticks)
 * - WEATHER-INDEPENDENT - fire dragon doesn't care about rain/storms
 * - Medium flight range (50-120 blocks)
 * - Continuous patrol behavior - flies for extended periods (~2-3 min)
 * - Checks if owner is flying before landing (tamed behavior)
 * - "Wander" command: patrols around owner (tethered mode)
 */
public class IgnivorusSmartFlightGoal extends DragonSmartFlightGoal<Ignivorus> {

    public IgnivorusSmartFlightGoal(Ignivorus ignivorus) {
        super(ignivorus);
    }

    @Override
    protected double getWaypointAcceptanceRadiusSqr() {
        return 16.0; // 4 blocks - balanced navigation
    }

    @Override
    protected int getStuckThreshold() {
        return 3; // Patient re-pathing
    }

    @Override
    protected int getLandingCooldownTicks() {
        return 60; // 3 seconds
    }

    @Override
    protected int getTakeoffChance(boolean thundering, boolean raining) {
        // Fire dragon ignores weather - bold and aggressive
        return 30; // 3.3% chance - fairly common
    }

    @Override
    protected int getKeepFlyingChance(boolean thundering, boolean raining) {
        // Fire dragon ignores weather - patrols for extended periods
        return 3000; // ~2-3 minutes
    }

    @Override
    protected int getFlightDecisionInterval(boolean thundering, boolean raining) {
        return 10; // Constant ~0.5 seconds - weather independent
    }

    @Override
    protected double findSafeFlightHeight(double x, double z, boolean thundering, boolean raining) {
        int ix = (int) x;
        int iz = (int) z;
        int groundY = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);

        boolean tethered = isTamedWander();
        double base;
        if (tethered) {
            // Tamed: moderate altitude around owner
            base = 15.0 + dragon.getRandom().nextDouble() * 15.0; // 15-30
        } else {
            // Wild: aggressive patrol at medium-high altitude
            base = 20.0 + dragon.getRandom().nextDouble() * 25.0; // 20-45
        }

        // Weather-independent caps
        double capAboveGround = tethered ? 40.0 : 60.0;

        double target = groundY + base;
        double cap = groundY + capAboveGround;
        double worldCap = dragon.level().getMaxBuildHeight() - 10.0;

        double chosen = Math.min(Math.min(target, cap), worldCap);

        // When underground (caves), clamp to space below the ceiling so we don't pick roof height
        if (dragon.getY() + 1.0 < groundY) {
            Vec3 eye = dragon.getEyePosition();
            BlockHitResult hit = dragon.level().clip(new ClipContext(
                    eye,
                    eye.add(0, 64, 0),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    dragon
            ));
            if (hit.getType() != HitResult.Type.MISS) {
                double ceiling = hit.getLocation().y() - 1.0;
                double minY = dragon.getY() + 2.0;
                double maxY = Math.max(minY, ceiling - 2.0);
                chosen = Mth.clamp(chosen, minY, maxY);
            }
        }

        return chosen;
    }

    @Override
    protected float[] getFlightRange(boolean isStuck) {
        if (isStuck) {
            return new float[]{30.0f, 70.0f}; // 30-70 blocks when stuck
        } else {
            return new float[]{50.0f, 120.0f}; // 50-120 blocks - medium range
        }
    }

    @Override
    protected boolean shouldProtectBabies() {
        return false; // Ignivorus doesn't protect babies
    }

    // ===== IGNIVORUS-SPECIFIC BEHAVIOR =====

    @Override
    protected boolean additionalCanContinueChecks() {
        // Tamed: land if owner is not flying
        if (dragon.isTame() && dragon.getOwner() != null) {
            LivingEntity owner = dragon.getOwner();
            boolean ownerFlying = owner.isAlive() && !owner.onGround();

            if (!isOverDanger() && !ownerFlying) {
                return false; // Land when owner is on ground
            }
        }
        return true;
    }

    @Override
    protected Vec3 generateFlightCandidate(Vec3 dragonPos, int attempt) {
        boolean tethered = isTamedWander();

        if (tethered) {
            // Tethered mode: patrol around owner
            Vec3 anchor = getFlightAnchor();
            double min = 15.0 + dragon.getRandom().nextDouble() * 10.0;
            double max = 35.0 + dragon.getRandom().nextDouble() * 15.0;
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
        // Ignivorus doesn't use takeoff animation flag
        dragon.setTakeoff(false);
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
