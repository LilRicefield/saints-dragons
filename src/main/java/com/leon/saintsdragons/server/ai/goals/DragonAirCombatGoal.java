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
        double desiredY = com.leon.saintsdragons.util.DragonMathUtil.clampAltitude(target.getY() + 12.0, groundLevel, 10, 25);
        
        // Position slightly ahead of target for better engagement
        Vec3 lead = leadPoint(target, 4.0);
        Vec3 goal = new Vec3(lead.x, desiredY, lead.z);
        
        dragon.getMoveControl().setWantedPosition(goal.x, goal.y, goal.z, 1.1);
        dragon.getLookControl().setLookAt(target, 20f, 20f);
        
        // Transition to air fight when we're at proper altitude and position
        if (dragon.distanceToSqr(goal) < 16.0 && dragon.getY() > target.getY() + 8.0) {
            phase = Phase.AIR_FIGHT;
            phaseTimer = 0;
            mode = null;
        }
        
        // Timeout climb phase to prevent getting stuck
        if (phaseTimer > 60) {
            phase = Phase.AIR_FIGHT;
            phaseTimer = 0;
            mode = null;
        }
    }

    private void handleAirFight(LivingEntity target) {
        double dist = dragon.distanceTo(target);
        
        // Dynamic mode selection based on distance and situation
        if (mode == null) {
            // Choose mode based on distance and terrain
            boolean hasClearDivePath = isPathClear(dragon.position(), target.position(), 15);
            boolean targetOnGround = target.onGround();
            
            if (dist > 30.0 || !hasClearDivePath || !targetOnGround) {
                mode = AirMode.RANGED_CIRCLE;
            } else {
                mode = AirMode.DIVE_BOMB;
            }
            circleClockwise = null;
        }
        
        // Execute current mode
        if (mode == AirMode.DIVE_BOMB) {
            doDiveBomb(target);
        } else {
            doRangedCircle(target);
        }

        // Check if we should land (closer to target, been airborne too long)
        boolean shouldLand = (dist < 8.0 && target.onGround()) || phaseTimer > 300;
        if (shouldLand) {
            phase = Phase.LANDING;
            phaseTimer = 0;
            dragon.setLanding(true);
            mode = null; // Reset mode for next air combat session
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
        int windup = 15;  // Longer windup for dramatic effect
        int commit = 20;  // Longer commit phase for better tracking
        int recover = 25; // Longer recovery

        if (phaseTimer < windup) {
            // Position above and slightly ahead of target
            Vec3 lead = leadPoint(target, 6.0);
            Vec3 aim = new Vec3(lead.x, target.getEyeY() + 8.0, lead.z);
            Vec3 p = ensureClearArc(aim, 8);
            dragon.getMoveControl().setWantedPosition(p.x, p.y, p.z, 1.0);
            com.leon.saintsdragons.util.DragonMathUtil.smoothLookAt(dragon, target, 20f, 20f);
            dragon.setAttackKind(ATTACK_KIND_DIVE);
            dragon.setAttackPhase(PHASE_WINDUP);
            return;
        }

        if (phaseTimer < windup + commit) {
            // Dive down toward target with lead prediction
            Vec3 lead = leadPoint(target, 3.0);
            Vec3 aim = new Vec3(lead.x, target.getY() + 0.5, lead.z);
            Vec3 p = ensureClearArc(aim, 12);
            dragon.getMoveControl().setWantedPosition(p.x, p.y, p.z, 1.6); // Faster dive speed
            com.leon.saintsdragons.util.DragonMathUtil.smoothLookAt(dragon, target, 40f, 40f);
            dragon.setAttackKind(ATTACK_KIND_DIVE);
            dragon.setAttackPhase(PHASE_COMMIT);
            
            // Dramatic dive impulse at commit start
            if (phaseTimer == windup) {
                Vec3 fwd = dragon.getLookAngle().normalize();
                Vec3 impulse = new Vec3(fwd.x * 0.6, -0.2, fwd.z * 0.6); // Stronger dive
                dragon.setDeltaMovement(dragon.getDeltaMovement().add(impulse));
            }
            
            // Attack when close enough with line of sight
            if (dragon.distanceTo(target) <= 4.0 && com.leon.saintsdragons.util.DragonMathUtil.hasLineOfSight(dragon, target)) {
                dragon.combatManager.tryUseAbility(ModAbilities.BITE);
            }
            return;
        }

        // Recovery phase - climb back up
        dragon.setAttackPhase(PHASE_RECOVER);
        if (phaseTimer < windup + commit + recover) {
            // Climb back up to maintain altitude
            Vec3 climbPos = new Vec3(target.getX(), target.getY() + 12.0, target.getZ());
            dragon.getMoveControl().setWantedPosition(climbPos.x, climbPos.y, climbPos.z, 0.8);
            return;
        }
        
        // Reset for next attack
        phaseTimer = 0;
        mode = null;
        airDwell = Math.max(airDwell, MIN_AIR_DWELL_TICKS);
    }

    private void doRangedCircle(LivingEntity target) {
        if (circleClockwise == null) circleClockwise = dragon.getRandom().nextBoolean();
        
        double d = dragon.distanceTo(target);
        float radius = (float) Math.min(40.0, Math.max(20.0, d * 0.8)); // Better radius calculation
        float speed = 0.03f; // Slower circling for better aim
        
        // Calculate circling position
        Vec3 circle = com.leon.saintsdragons.util.DragonMathUtil.circleEntityPosition(target, radius, speed, circleClockwise, phaseTimer, 0);
        
        // Add altitude variation for more dynamic movement
        double altitudeOffset = Math.sin(phaseTimer * 0.1) * 3.0; // Gentle altitude variation
        Vec3 goal = circle.add(0, 8.0 + altitudeOffset, 0);
        goal = ensureClearArc(goal, 8);
        
        dragon.getMoveControl().setWantedPosition(goal.x, goal.y, goal.z, 0.9);
        com.leon.saintsdragons.util.DragonMathUtil.smoothLookAt(dragon, target, 25f, 25f);

        // Check for beam attack opportunity
        float yawErr = com.leon.saintsdragons.util.DragonMathUtil.yawErrorToTarget(dragon, target);
        boolean los = com.leon.saintsdragons.util.DragonMathUtil.hasLineOfSight(dragon, target);
        
        // More lenient aiming requirements for better beam usage
        if (yawErr <= 8f && los && dragon.combatManager.canStart(ModAbilities.LIGHTNING_BEAM)) {
            dragon.setAttackKind(ATTACK_KIND_BEAM);
            dragon.setAttackPhase(PHASE_COMMIT);
            dragon.combatManager.tryUseAbility(ModAbilities.LIGHTNING_BEAM);
            
            // Brief pause after beam to avoid spam
            phaseTimer += 20;
        }
        
        // Switch circling direction occasionally for unpredictability
        if (phaseTimer > 200 && dragon.getRandom().nextFloat() < 0.02f) {
            circleClockwise = !circleClockwise;
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
