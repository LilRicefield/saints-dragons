package com.leon.saintsdragons.server.ai.goals;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.leon.saintsdragons.util.DragonMathUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Merged aerial combat + flight decision goal.
 */
public class DragonAirCombatGoal extends Goal {
    private final LightningDragonEntity dragon;

    // Engagement window
    private static final double ENGAGE_MAX_RANGE_SQR = 80 * 80;
    private static final double AIR_PREFERRED_DIST = 22.0;
    private static final double AIR_ALT_GAP = 6.0;
    private static final int MIN_AIR_DWELL_TICKS = 100;
    private static final int MIN_GROUND_DWELL_TICKS = 60;

    // Attack kinds/phases for animation hooks
    private static final int ATTACK_KIND_DIVE = 4;
    private static final int ATTACK_KIND_BEAM = 5;
    private static final int PHASE_WINDUP = 1;
    private static final int PHASE_COMMIT = 2;
    private static final int PHASE_RECOVER = 3;

    private enum Phase { DECIDE, TAKEOFF, CLIMB, AIR_FIGHT, LANDING }
    private enum AirMode { DIVE_BOMB, RANGED_CIRCLE }

    private Phase phase = Phase.DECIDE;
    private int phaseTimer = 0;
    private AirMode mode = null;

    // No persistent waypoints; compute per tick
    private Boolean circleClockwise = null;

    private int airDwell = 0;
    private int groundDwell = 0;

    public DragonAirCombatGoal(LightningDragonEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = dragon.getTarget();
        if (t == null || !t.isAlive()) return false;
        if (dragon.distanceToSqr(t) > ENGAGE_MAX_RANGE_SQR) return false;

        // If we're already airborne or in the process, continue to manage air combat
        if (dragon.isFlying() || dragon.isTakeoff() || dragon.isHovering()) return true;

        // Otherwise, only use air combat when we actually prefer air over ground
        double dist = dragon.distanceTo(t);
        boolean far = dist >= AIR_PREFERRED_DIST;
        boolean highGap = (t.getY() - dragon.getY()) >= AIR_ALT_GAP;
        boolean groundPathBad = dist > 10 && !DragonMathUtil.hasLineOfSight(dragon, t);
        return far || highGap || groundPathBad;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = dragon.getTarget();
        if (t == null || !t.isAlive()) return false;
        if (dragon.distanceToSqr(t) > ENGAGE_MAX_RANGE_SQR) return false;

        // If we're airborne/in transition, continue
        if (dragon.isFlying() || dragon.isTakeoff() || dragon.isHovering()) return true;

        // If grounded and we don't prefer air, yield to ground melee
        double dist = dragon.distanceTo(t);
        boolean far = dist >= AIR_PREFERRED_DIST;
        boolean highGap = (t.getY() - dragon.getY()) >= AIR_ALT_GAP;
        boolean groundPathBad = dist > 10 && !DragonMathUtil.hasLineOfSight(dragon, t);
        return far || highGap || groundPathBad;
    }

    @Override
    public void start() {
        phase = Phase.DECIDE;
        phaseTimer = 0;
        // no persistent waypoints
        circleClockwise = null;
    }

    @Override
    public void stop() {
        // no persistent waypoints
        mode = null;
        phase = Phase.DECIDE;
    }

    @Override
    public void tick() {
        phaseTimer++;
        if (airDwell > 0) airDwell--; else airDwell = 0;
        if (groundDwell > 0) groundDwell--; else groundDwell = 0;

        LivingEntity target = dragon.getTarget();
        if (target == null || !target.isAlive()) return;

        switch (phase) {
            case DECIDE -> decidePhase(target);
            case TAKEOFF -> handleTakeoff(target);
            case CLIMB -> handleClimb(target);
            case AIR_FIGHT -> handleAirFight(target);
            case LANDING -> handleLanding();
        }
    }

    private void decidePhase(LivingEntity target) {
        double dist = dragon.distanceTo(target);
        boolean far = dist >= AIR_PREFERRED_DIST;
        boolean highGap = (target.getY() - dragon.getY()) >= AIR_ALT_GAP;
        boolean groundPathBad = dist > 10 && !DragonMathUtil.hasLineOfSight(dragon, target);

        boolean preferAir = (far || highGap || groundPathBad);

        if ((preferAir || airDwell > 0) && groundDwell == 0) {
            if (!dragon.isFlying()) {
                dragon.setFlying(true);
                dragon.setTakeoff(true);
                dragon.setHovering(true);
                dragon.setLanding(false);
                phase = Phase.TAKEOFF;
            } else {
                // Already airborne: proceed directly to fight if we have altitude
                phase = Phase.AIR_FIGHT;
            }
            phaseTimer = 0;
            return;
        }

        if (dragon.isFlying() || dragon.isTakeoff() || dragon.isHovering()) {
            phase = Phase.LANDING;
            phaseTimer = 0;
            dragon.setLanding(true);
        } else {
            stop(); // yield to ground melee
        }
    }

    private void handleTakeoff(LivingEntity target) {
        if (phaseTimer > 10) {
            phase = Phase.CLIMB;
            phaseTimer = 0;
        } else {
            dragon.getLookControl().setLookAt(target, 20f, 20f);
        }
    }

    private void handleClimb(LivingEntity target) {
        double groundLevel = groundLevelAt(target.position());
        double desiredY = com.leon.saintsdragons.util.DragonMathUtil.clampAltitude(target.getY() + 8.0, groundLevel, 8, 22);
        Vec3 goal = new Vec3(target.getX(), desiredY, target.getZ());
        dragon.getMoveControl().setWantedPosition(goal.x, goal.y, goal.z, 1.0);
        if (dragon.distanceToSqr(goal) < 9.0) {
            phase = Phase.AIR_FIGHT;
            phaseTimer = 0;
            mode = null;
        }
    }

    private void handleAirFight(LivingEntity target) {
        if (mode == null) {
            double d = dragon.distanceTo(target);
            mode = (d > 26.0) ? AirMode.RANGED_CIRCLE : AirMode.DIVE_BOMB;
            circleClockwise = null;
        }
        if (mode == AirMode.DIVE_BOMB) doDiveBomb(target); else doRangedCircle(target);

        if (phaseTimer > 140 && dragon.distanceTo(target) < 10.0) {
            phase = Phase.LANDING;
            phaseTimer = 0;
            dragon.setLanding(true);
        }
    }

    private void handleLanding() {
        if (dragon.onGround()) {
            dragon.setLanding(false);
            dragon.setFlying(false);
            dragon.setTakeoff(false);
            dragon.setHovering(false);
            dragon.markLandedNow();
            airDwell = 0;
            groundDwell = MIN_GROUND_DWELL_TICKS;
            stop();
        } else {
            dragon.setHovering(true);
        }
    }

    // ===== Air modes =====
    private void doDiveBomb(LivingEntity target) {
        int windup = 10;
        int commit = 14;

        if (phaseTimer < windup) {
            Vec3 lead = leadPoint(target, 4.0);
            Vec3 aim = new Vec3(lead.x, target.getEyeY() + 1.0, lead.z);
            Vec3 p = ensureClearArc(aim, 8);
            dragon.getMoveControl().setWantedPosition(p.x, p.y, p.z, 1.2);
            com.leon.saintsdragons.util.DragonMathUtil.smoothLookAt(dragon, target, 25f, 25f);
            dragon.setAttackKind(ATTACK_KIND_DIVE);
            dragon.setAttackPhase(PHASE_WINDUP);
            return;
        }

        if (phaseTimer < windup + commit) {
            Vec3 lead = leadPoint(target, 2.0);
            Vec3 aim = new Vec3(lead.x, Math.min(dragon.getY(), target.getY() + 0.3), lead.z);
            Vec3 p = ensureClearArc(aim, 10);
            dragon.getMoveControl().setWantedPosition(p.x, p.y, p.z, 1.4);
            com.leon.saintsdragons.util.DragonMathUtil.smoothLookAt(dragon, target, 35f, 35f);
            dragon.setAttackKind(ATTACK_KIND_DIVE);
            dragon.setAttackPhase(PHASE_COMMIT);
            // Dive impulse at commit start for dramatic acceleration
            if (phaseTimer == windup) {
                Vec3 fwd = dragon.getLookAngle().normalize();
                // Forward-and-down bias to sell the dive
                Vec3 impulse = new Vec3(fwd.x * 0.45, -0.12, fwd.z * 0.45);
                dragon.setDeltaMovement(dragon.getDeltaMovement().add(impulse));
            }
            if (dragon.distanceTo(target) <= 3.2 && com.leon.saintsdragons.util.DragonMathUtil.hasLineOfSight(dragon, target)) {
                dragon.combatManager.tryUseAbility(ModAbilities.BITE);
            }
            return;
        }

        dragon.setAttackPhase(PHASE_RECOVER);
        if (phaseTimer > windup + commit + 18) {
            phaseTimer = 0;
            mode = null;
            airDwell = Math.max(airDwell, MIN_AIR_DWELL_TICKS);
        }
    }

    private void doRangedCircle(LivingEntity target) {
        if (circleClockwise == null) circleClockwise = dragon.getRandom().nextBoolean();
        double d = dragon.distanceTo(target);
        float radius = (float) Math.min(35.0, Math.max(25.0, d * 0.75));
        float speed = 0.05f;
        Vec3 circle = com.leon.saintsdragons.util.DragonMathUtil.circleEntityPosition(target, radius, speed, circleClockwise, phaseTimer, 0);
        Vec3 goal = circle.add(0, 10, 0);
        goal = ensureClearArc(goal, 8);
        dragon.getMoveControl().setWantedPosition(goal.x, goal.y, goal.z, 1.0);
        com.leon.saintsdragons.util.DragonMathUtil.smoothLookAt(dragon, target, 20f, 20f);

        float yawErr = com.leon.saintsdragons.util.DragonMathUtil.yawErrorToTarget(dragon, target);
        boolean los = com.leon.saintsdragons.util.DragonMathUtil.hasLineOfSight(dragon, target);
        if (yawErr <= 6f && los && dragon.combatManager.canStart(ModAbilities.LIGHTNING_BEAM)) {
            dragon.setAttackKind(ATTACK_KIND_BEAM);
            dragon.setAttackPhase(PHASE_COMMIT);
            dragon.combatManager.tryUseAbility(ModAbilities.LIGHTNING_BEAM);
            phaseTimer = 0;
        }
    }

    // ===== Helpers =====
    private Vec3 leadPoint(LivingEntity target, double ahead) {
        Vec3 tv = target.getDeltaMovement();
        if (tv.lengthSqr() < 1.0e-4) return target.position();
        return target.position().add(tv.normalize().scale(Math.max(0.0, ahead)));
    }

    private Vec3 ensureClearArc(Vec3 candidate, int steps) {
        if (isPathClear(dragon.position(), candidate, steps)) return candidate;
        for (int i = 0; i < 2; i++) {
            Vec3 alt = sampleAround(candidate);
            if (alt != null && isPathClear(dragon.position(), alt, steps)) return alt;
        }
        return candidate;
    }

    private Vec3 sampleAround(Vec3 around) {
        var rnd = dragon.getRandom();
        for (int i = 0; i < 8; i++) {
            float yaw = (float) (rnd.nextFloat() * Math.PI * 2);
            float r = (float) 3.0 + rnd.nextFloat() * Math.max(0.01f, ((float) 8.0 - (float) 3.0));
            Vec3 p = new Vec3(around.x + Math.cos(yaw) * r, around.y + 0.5 + rnd.nextFloat() * 2.5, around.z + Math.sin(yaw) * r);
            if (isPathClear(dragon.position(), p, 8)) return p;
        }
        return around;
    }

    private boolean isPathClear(Vec3 from, Vec3 to, int steps) {
        if (steps <= 0) return true;
        Vec3 delta = to.subtract(from);
        Vec3 step = delta.scale(1.0 / steps);
        var aabb = dragon.getBoundingBox();
        for (int i = 1; i <= steps; i++) {
            aabb = aabb.move(step);
            if (!dragon.level().noCollision(dragon, aabb)) return false;
        }
        var hit = dragon.level().clip(new net.minecraft.world.level.ClipContext(
                from, to,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                dragon));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    private double groundLevelAt(Vec3 pos) {
        for (int y = (int) pos.y; y > dragon.level().getMinBuildHeight(); y--) {
            if (!dragon.level().getBlockState(new net.minecraft.core.BlockPos((int) pos.x, y, (int) pos.z)).isAir()) {
                return y + 1;
            }
        }
        return dragon.level().getMinBuildHeight();
    }
}
