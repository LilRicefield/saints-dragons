package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.util.DragonMathUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * - picks the projectile MOST moving toward us (dot product)
 * - chooses the better lateral side (left/right)
 * - predicts impact line and initiates a multi-tick dodge burst via entity.beginDodge(...)
 */
public class RaevyxDodgeGoal extends Goal {
    private final Raevyx wyvern;

    // tuning
    private static final double SCAN_RADIUS_H = 32.0;
    private static final double SCAN_RADIUS_V = 22.0;
    private static final int    SCAN_INTERVAL = 2;     // faster scans
    private static final int    DODGE_TICKS   = 9;     // slightly longer dodge burst
    private static final int    COOLDOWN      = 8;     // reduced cooldown between dodges

    private static final double DOT_THREAT    = 0.65;  // more permissive threat angle
    private static final double MIN_SPEED2    = 0.0015; // accept slightly slower projectiles

    // Dodge impulse constants - flight only
    private static final double DODGE_LAT_IMPULSE = 0.80;
    private static final double DODGE_UP_IMPULSE  = 0.40;

    private long nextScanTime = 0L;
    private long nextAllowedDodgeTime = 0L; // <-- time-based cooldown

    private List<Projectile> getCachedThreats() {
        return wyvern.getCachedNearbyProjectiles().stream()
                .filter(p -> p.isAlive() &&
                        p.getOwner() != wyvern &&
                        p.getOwner() != wyvern.getOwner() &&
                        p.getDeltaMovement().lengthSqr() > MIN_SPEED2)
                .collect(Collectors.toList());
    }

    public RaevyxDodgeGoal(Raevyx wyvern) {
        this.wyvern = wyvern;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!wyvern.isAlive()) return false;
        if (wyvern.isTame() && wyvern.isVehicle()) return false;
        if (wyvern.isDodging()) return false;
        if (!wyvern.isFlying()) return false;

        long now = wyvern.level().getGameTime();
        if (now < nextScanTime) return false;
        nextScanTime = now + SCAN_INTERVAL;

        if (now < nextAllowedDodgeTime) return false;

        // Use cached nearby projectiles for better performance
        List<Projectile> threats = getCachedThreats().stream()
                .filter(p -> DragonMathUtil.hasLineOfSight(wyvern, p)) // prefer LOS
                .collect(Collectors.toList());

        // If none pass LOS (e.g., player between us and projectile), fall back to no-LOS set
        if (threats.isEmpty()) {
            threats = getCachedThreats();
        }

        Projectile mostThreatening = mostMovingTowardMeFromList(threats, wyvern);
        if (mostThreatening == null) return false;

        Vec3 dv = new Vec3(
                mostThreatening.getX() - mostThreatening.xo,
                mostThreatening.getY() - mostThreatening.yo,
                mostThreatening.getZ() - mostThreatening.zo
        );

        if (dv.lengthSqr() < MIN_SPEED2) return false;

        // Use dodge calculation function instead of manually doing it
        Vec3 dodgeDirection = DragonMathUtil.calculateDodgeDirection(wyvern, mostThreatening);

        if (dodgeDirection.equals(Vec3.ZERO)) return false;

        // Since we only dodge when flying, use flight constants directly
        Vec3 dodgeVec = new Vec3(
                dodgeDirection.x * DODGE_LAT_IMPULSE,
                DODGE_UP_IMPULSE,
                dodgeDirection.z * DODGE_LAT_IMPULSE
        );

        dodgeVec = DragonMathUtil.clampVectorLength(dodgeVec, 1.5); // Max dodge strength

        wyvern.beginDodge(dodgeVec, DODGE_TICKS);
        nextAllowedDodgeTime = now + COOLDOWN;
        return true;
    }

    @Override
    public void start() {
        wyvern.getNavigation().stop();
        // Trigger dodge animation every time
        wyvern.triggerDodgeAnimation();
        // Play annoyed sound when dodging attacks
        wyvern.playAnnoyedSound();
    }

    @Override public boolean canContinueToUse() { return false; }
    @Override public void stop() { /* nothing; cooldown is time-based */ }

    // ========== HELPERS ==========

    private Vec3 guessProjectileDestination(Projectile projectile) {
        Vec3 from = projectile.position();
        Vec3 vel  = projectile.getDeltaMovement();
        if (vel.lengthSqr() < 1.0e-6) return from;
        Vec3 to   = from.add(vel.scale(50)); // long ray
        return projectile.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile)).getLocation();
    }

    @Nullable
    private <T extends Projectile> T mostMovingTowardMe(Class<? extends T> cls,
                                                        Predicate<? super T> pred,
                                                        LivingEntity me,
                                                        AABB box) {
        return mostMovingTowardMeFromList(me.level().getEntitiesOfClass(cls, box, pred), me);
    }

    private <T extends Projectile> T mostMovingTowardMeFromList(List<? extends T> entities, LivingEntity me) {
        double best = DOT_THREAT;
        T bestEnt = null;
        for (T p : entities) {
            Vec3 dv = new Vec3(p.getX() - p.xo, p.getY() - p.yo, p.getZ() - p.zo);
            double ls = dv.lengthSqr();
            if (ls < MIN_SPEED2) continue;
            double dot = dv.normalize().dot(me.position().subtract(p.position()).normalize());
            if (dot > best) { best = dot; bestEnt = p; }
        }
        return bestEnt;
    }
}
