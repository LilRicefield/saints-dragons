package com.leon.saintsdragons.server.ai.goals;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * FSM-based melee attack goal with deterministic move selection and precise timing phases.
 * Phases: APPROACH -> ALIGN -> WINDUP -> COMMIT -> RECOVER
 */
public class DragonMeleeAttackGoal extends Goal {
    private final LightningDragonEntity dragon;
    
    enum Move { NONE, HORN, BITE }
    enum Phase { APPROACH, ALIGN, WINDUP, COMMIT, RECOVER }
    
    private Move move = Move.NONE;
    private Phase phase = Phase.APPROACH;
    private int timer = 0;
    private int pathCooldown = 0;
    private boolean directChase = false; // straight-line closing under short range
    private int turnHoldTicks = 0;   // prevent rapid turn direction flipping
    private int lastTurnDir = 0;      // -1 left, 0 none, 1 right
    private static final int MIN_TURN_HOLD = 6;

    public DragonMeleeAttackGoal(LightningDragonEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        // Initialize attack path cooldown
        this.pathCooldown = 0;
    }

    @Override
    public boolean canUse() {
        // Do not run while ridden
        if (dragon.getControllingPassenger() != null) return false;
        // Only run when actually on ground; ignore stale flight flags
        if (!dragon.onGround()) return false;
        
        LivingEntity target = dragon.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        phase = Phase.APPROACH;
        timer = 0;
        move = Move.NONE;
        setRun(true);
        // Ensure any stale flight flags are cleared for ground pursuit
        dragon.setLanding(false);
        dragon.setTakeoff(false);
        dragon.setHovering(false);
        dragon.setFlying(false);
    }

    @Override
    public void stop() {
        setRun(false);
        dragon.getNavigation().stop();
        setAttackFlags(0, 0);
        phase = Phase.APPROACH;
        move = Move.NONE;
        timer = 0;
        directChase = false;
    }

    @Override
    public void tick() {
        LivingEntity target = dragon.getTarget();
        if (target == null || !target.isAlive()) {
            stop();
            return;
        }

        double dist = dragon.distanceTo(target);
        double angle = angleToTargetDeg(dragon, target);
        float desiredYaw = desiredYawTo(dragon, target);
        float yawErr = net.minecraft.util.Mth.degreesDifference(dragon.getYRot(), desiredYaw);
        int turnDir = yawErr > 2 ? 1 : (yawErr < -2 ? -1 : 0);
        if (turnDir != 0 && turnDir != lastTurnDir) {
            if (turnHoldTicks < MIN_TURN_HOLD) {
                turnDir = lastTurnDir;
            } else {
                lastTurnDir = turnDir;
                turnHoldTicks = 0;
            }
        } else if (turnDir == lastTurnDir) {
            turnHoldTicks = Math.min(turnHoldTicks + 1, 2 * MIN_TURN_HOLD);
        }
        boolean los = dragon.getSensing().hasLineOfSight(target);

        switch (phase) {
            case APPROACH -> {
                // Ensure running during approach
                setRun(true);
                
                // Straight-line close when very near and with LOS; else pathfind
                boolean canDirect = los && dist < 6.5;
                if (canDirect) {
                    directChase = true;
                    dragon.getNavigation().stop();
                    dragon.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.45);
                } else {
                    if (directChase) {
                        directChase = false;
                        pathCooldown = 0;
                    }
                    // Throttled re-path with higher speed for approach
                    if (pathCooldown-- <= 0 || dragon.getNavigation().isDone()) {
                        double speed = dist > 12 ? 1.6 : 1.35; // Faster approach speeds
                        dragon.getNavigation().moveTo(target, speed);
                        pathCooldown = 5; // repath more frequently
                    }
                }
                // Deliberate rotation toward target to reduce spin
                rotateBodyToward(desiredYaw, dist < 8 ? 18f : 12f);
                dragon.getLookControl().setLookAt(target, 25.0f, 25.0f);
                // When in either horn or bite band, pick a move
                if (chooseMove(dist, angle, los)) {
                    phase = Phase.ALIGN;
                    timer = 0;
                }
            }

            case ALIGN -> {
                dragon.getNavigation().stop();
                rotateBodyToward(desiredYaw, 28f);
                dragon.getLookControl().setLookAt(target, 40.0f, 40.0f);
                if (isAligned(angle)) {
                    phase = Phase.WINDUP;
                    timer = 0;
                    setAttackFlags(kindId(move), 1); // WINDUP
                } else if (++timer > 8 || !los) { // Short timeout to avoid stalling
                    phase = Phase.APPROACH;
                    move = Move.NONE;
                }
            }

            case WINDUP -> {
                dragon.getNavigation().stop();
                faceTargetHard(target);
                int windup = (move == Move.HORN) ? 3 : 2; // Ultra-fast windup
                if (++timer >= windup) {
                    phase = Phase.COMMIT;
                    timer = 0;
                    setAttackFlags(kindId(move), 2); // COMMIT
                }
            }

            case COMMIT -> {
                if (timer == 0) {
                    if (move == Move.HORN) dashForward(dragon, 1.05); // stronger impulse
                    else if (move == Move.BITE) dashForward(dragon, 0.6); // small lunge for bites
                    // fire ability exactly once
                    triggerAbility(move);
                }
                int commit = (move == Move.HORN) ? 5 : 3;
                if (++timer >= commit) {
                    setAbilityCooldown(move);
                    phase = Phase.RECOVER;
                    timer = 0;
                    setAttackFlags(kindId(move), 3); // RECOVER
                }
            }

            case RECOVER -> {
                // Immediately chase during recovery - no standing around! Desynced cooldown
                if (pathCooldown-- <= 0 || dragon.getNavigation().isDone()) {
                    dragon.getNavigation().moveTo(target, 1.45);
                    pathCooldown = 5; // quicker re-path
                }
                
                if (++timer >= ((move == Move.HORN) ? 5 : 4)) { // Shorter recovery
                    setAttackFlags(0, 0);
                    move = Move.NONE;
                    phase = Phase.APPROACH;
                    timer = 0;
                }
            }
        }
    }

    private boolean chooseMove(double dist, double angle, boolean los) {
        boolean hornReady = dragon.combatManager.canStart(ModAbilities.HORN_GORE);
        boolean biteReady = dragon.combatManager.canStart(ModAbilities.BITE);

        // Much more generous overlapping ranges - commit faster!
        boolean hornOk = hornReady && los && dist >= 2.0 && dist <= 8.0 && Math.abs(angle) <= 45.0;
        boolean biteOk = biteReady && los && dist >= 1.0 && dist <= 6.0 && Math.abs(angle) <= 65.0;

        // prefer horn if reasonably lined up; else bite for everything else
        if (hornOk) {
            move = Move.HORN;
            return true;
        }
        if (biteOk) {
            move = Move.BITE;
            return true;
        }
        return false;
    }

    private boolean isAligned(double angle) {
        double limit = (move == Move.HORN) ? 45.0 : 65.0; // Match the generous selection ranges
        return Math.abs(angle) <= limit;
    }

    private void triggerAbility(Move m) {
        if (m == Move.HORN) dragon.combatManager.tryUseAbility(ModAbilities.HORN_GORE);
        else if (m == Move.BITE) dragon.combatManager.tryUseAbility(ModAbilities.BITE);
    }

    private void setAbilityCooldown(Move m) {
        // Base cooldowns handled in DragonCombatManager.setAbilityCooldown
        if (m == Move.HORN) dragon.combatManager.setAbilityCooldown(ModAbilities.HORN_GORE, 50);
        else if (m == Move.BITE) dragon.combatManager.setAbilityCooldown(ModAbilities.BITE, 18);
    }

    private static int kindId(Move m) {
        return m == Move.HORN ? 1 : m == Move.BITE ? 2 : 0;
    }

    private void setRun(boolean v) {
        dragon.setRunning(v);
    }

    private void setAttackFlags(int kind, int phase) {
        dragon.setAttackKind(kind);
        dragon.setAttackPhase(phase);
    }

    // ===== Helper methods =====

    private static double angleToTargetDeg(LivingEntity self, LivingEntity target) {
        Vec3 fwd = self.getForward().normalize();
        Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(self.position().add(0, self.getBbHeight() * 0.5, 0)).normalize();
        double dot = Mth.clamp(fwd.dot(dir), -1.0, 1.0);
        return Math.toDegrees(Math.acos(dot));
    }

    private static float desiredYawTo(LivingEntity self, LivingEntity target) {
        Vec3 to = target.position().subtract(self.position());
        return (float)(Math.atan2(to.z, to.x) * (180.0 / Math.PI)) - 90.0f;
    }

    private void faceTargetRateLimited(LivingEntity target, float degPerTick) {
        dragon.getLookControl().setLookAt(target, degPerTick, degPerTick);
    }

    private void rotateBodyToward(float desiredYaw, float degPerTick) {
        float newYaw = net.minecraft.util.Mth.approachDegrees(dragon.getYRot(), desiredYaw, degPerTick);
        dragon.setYRot(newYaw);
        dragon.yBodyRot = newYaw;
    }

    private void faceTargetHard(LivingEntity target) {
        dragon.getLookControl().setLookAt(target, 90.0f, 90.0f);
    }

    private static void dashForward(LightningDragonEntity dragon, double strength) {
        Vec3 fwd = dragon.getForward().normalize();
        dragon.setDeltaMovement(dragon.getDeltaMovement().add(fwd.scale(strength)));
    }
}
