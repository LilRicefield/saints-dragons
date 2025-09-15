package com.leon.saintsdragons.server.ai.goals.lightningdragon;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonMeleeAttackGoalBase;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Lightning Dragon specific melee attack goal with FSM precision.
 * Uses base class structure but implements FSM mechanics for precise combat.
 */
public class LightningDragonMeleeAttackGoal extends DragonMeleeAttackGoalBase {
    
    // FSM State Management
    enum Move { NONE, HORN, BITE }
    enum Phase { APPROACH, ALIGN, WINDUP, COMMIT, RECOVER }
    
    private Move move = Move.NONE;
    private Phase phase = Phase.APPROACH;
    private int timer = 0;
    private int pathCommitmentTicks = 0;
    private boolean directChase = false;
    private int turnHoldTicks = 0;
    private int lastTurnDir = 0;
    private static final int MIN_TURN_HOLD = 8; // Increased from 6 to 8 for smoother turns
    
    public LightningDragonMeleeAttackGoal(LightningDragonEntity dragon) {
        super(dragon);
        configureLightningDragonCombat();
    }
    
    private void configureLightningDragonCombat() {
        // Lightning Dragon specific combat parameters
        this.attackRange = 3.5;           // Lightning dragons have longer reach
        this.directChaseRange = 10.0;      // Direct chase range
        this.pathCooldown = 5;             // Much faster path recalculation (was 15)
        this.attackCooldown = 20;          // Attack cooldown
    }
    
    @Override
    public void start() {
        super.start();
        phase = Phase.APPROACH;
        timer = 0;
        move = Move.NONE;
        setRun(true); // Always run when chasing!
        // Ensure any stale flight flags are cleared for ground pursuit
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        lightningDragon.setLanding(false);
        lightningDragon.setTakeoff(false);
        lightningDragon.setHovering(false);
        lightningDragon.setFlying(false);
    }
    
    @Override
    public void stop() {
        super.stop();
        setRun(false);
        dragon.getNavigation().stop();
        setAttackFlags(0, 0);
        phase = Phase.APPROACH;
        move = Move.NONE;
        timer = 0;
        directChase = false;
        pathCommitmentTicks = 0;
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
        float yawErr = Mth.degreesDifference(dragon.getYRot(), desiredYaw);
        int turnDir = yawErr > 5 ? 1 : (yawErr < -5 ? -1 : 0); // Increased threshold from 2 to 5 degrees
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
                setRun(true); // Always run during approach!
                
                boolean canDirect = los && dist < 10.0;
                if (canDirect) {
                    directChase = true;
                    dragon.getNavigation().stop();
                    // Always update movement - dragon is running so use faster speed
                    dragon.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.6);
                } else {
                    if (directChase) {
                        directChase = false;
                        pathCooldown = 0;
                    }
                    pathCommitmentTicks++;
                    if (pathCooldown-- <= 0) {
                        double speed = dist > 12 ? 1.6 : 1.4; // Running speeds - faster than walking
                        dragon.getNavigation().moveTo(target, speed);
                        pathCooldown = 5; // Much faster repathing (was 15)
                        pathCommitmentTicks = 0;
                    }
                }
                
                // Fallback: if dragon has been stuck for too long, force direct chase
                if (pathCommitmentTicks > 20 && dist > 5.0) {
                    dragon.getNavigation().stop();
                    dragon.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.4); // Running fallback speed
                    pathCommitmentTicks = 0;
                }
                // Smoother rotation to reduce animation thrashing
                rotateBodyToward(desiredYaw, dist < 8 ? 12f : 8f); // Reduced rotation speed
                dragon.getLookControl().setLookAt(target, 15.0f, 15.0f); // Reduced look speed
                // Only try to attack if we're close enough AND have good positioning
                if (dist <= 6.0 && chooseMove(dist, angle, los)) {
                    phase = Phase.ALIGN;
                    timer = 0;
                }
                // If we're too close but can't attack, keep moving past the target
                else if (dist <= 3.0) {
                    // Move past the target instead of stopping
                    Vec3 pastTarget = target.position().add(target.getForward().scale(2.0));
                    dragon.getMoveControl().setWantedPosition(pastTarget.x, pastTarget.y, pastTarget.z, 1.6);
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
                } else if (++timer > 4 || !los || dist > 8.0) { // Much shorter timeout to avoid stalling
                    phase = Phase.APPROACH;
                    move = Move.NONE;
                }
            }

            case WINDUP -> {
                dragon.getNavigation().stop();
                faceTargetHard(target);
                int windup = (move == Move.HORN) ? 2 : 1; // Faster windup
                if (++timer >= windup) {
                    phase = Phase.COMMIT;
                    timer = 0;
                    setAttackFlags(kindId(move), 2); // COMMIT
                }
            }

            case COMMIT -> {
                if (timer == 0) {
                    if (move == Move.HORN) dashForward(1.05);
                    else if (move == Move.BITE) dashForward(0.6);
                    triggerAbility(move);
                }
                int commit = (move == Move.HORN) ? 3 : 2; // Faster commit phase
                if (++timer >= commit) {
                    setAbilityCooldown(move);
                    phase = Phase.RECOVER;
                    timer = 0;
                    setAttackFlags(kindId(move), 3); // RECOVER
                }
            }

            case RECOVER -> {
                setRun(true); // Always run during recovery!
                if (pathCooldown-- <= 0 || dragon.getNavigation().isDone()) {
                    dragon.getNavigation().moveTo(target, 1.5); // Running recovery speed
                    pathCooldown = 3; // Even faster during recovery (was 5)
                }
                
                if (++timer >= ((move == Move.HORN) ? 2 : 1)) { // Much faster recovery - almost instant
                    setAttackFlags(0, 0);
                    move = Move.NONE;
                    phase = Phase.APPROACH;
                    timer = 0;
                }
            }
        }
    }
    
    private boolean chooseMove(double dist, double angle, boolean los) {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        boolean hornReady = lightningDragon.combatManager.canStart(com.leon.saintsdragons.common.registry.LightningDragonAbilities.HORN_GORE);
        boolean biteReady = lightningDragon.combatManager.canStart(com.leon.saintsdragons.common.registry.LightningDragonAbilities.BITE);

        boolean hornOk = hornReady && los && dist >= 2.0 && dist <= 6.0 && Math.abs(angle) <= 35.0; // Tighter range for horn
        boolean biteOk = biteReady && los && dist >= 1.0 && dist <= 4.0 && Math.abs(angle) <= 50.0; // Tighter range for bite

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
        double limit = (move == Move.HORN) ? 35.0 : 50.0; // Match the tighter ranges
        return Math.abs(angle) <= limit;
    }

    private void triggerAbility(Move m) {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        if (m == Move.HORN) lightningDragon.combatManager.tryUseAbility(com.leon.saintsdragons.common.registry.LightningDragonAbilities.HORN_GORE);
        else if (m == Move.BITE) lightningDragon.combatManager.tryUseAbility(com.leon.saintsdragons.common.registry.LightningDragonAbilities.BITE);
    }

    private void setAbilityCooldown(Move m) {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        if (m == Move.HORN) lightningDragon.combatManager.setAbilityCooldown(com.leon.saintsdragons.common.registry.LightningDragonAbilities.HORN_GORE, 50);
        else if (m == Move.BITE) lightningDragon.combatManager.setAbilityCooldown(com.leon.saintsdragons.common.registry.LightningDragonAbilities.BITE, 18);
    }

    private static int kindId(Move m) {
        return m == Move.HORN ? 1 : m == Move.BITE ? 2 : 0;
    }

    private void setRun(boolean v) {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        lightningDragon.setRunning(v);
    }

    private void setAttackFlags(int kind, int phase) {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        lightningDragon.setAttackKind(kind);
        lightningDragon.setAttackPhase(phase);
    }

    // Helper methods
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

    private void rotateBodyToward(float desiredYaw, float degPerTick) {
        float newYaw = Mth.approachDegrees(dragon.getYRot(), desiredYaw, degPerTick);
        dragon.setYRot(newYaw);
        dragon.yBodyRot = newYaw;
    }

    private void faceTargetHard(LivingEntity target) {
        dragon.getLookControl().setLookAt(target, 90.0f, 90.0f);
    }

    private void dashForward(double strength) {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        Vec3 fwd = lightningDragon.getForward().normalize();
        lightningDragon.setDeltaMovement(lightningDragon.getDeltaMovement().add(fwd.scale(strength)));
    }
    
    @Override
    protected void onAttackPerformed(LivingEntity target) {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Lightning Dragon specific attack effects
        if (lightningDragon.getRandom().nextFloat() < 0.3f) {
            // 30% chance for electrical effect
            lightningDragon.playLightningEffect(target.position());
        }
        
        // Lightning Dragon uses abilities during AI combat!
        if (lightningDragon.canUseAbility()) {
            float abilityChance = lightningDragon.getRandom().nextFloat();
            
            if (abilityChance < 0.25f) {
                // 25% chance to use Horn Gore ability
                lightningDragon.tryActivateAbility(com.leon.saintsdragons.common.registry.LightningDragonAbilities.HORN_GORE);
            } else if (abilityChance < 0.4f) {
                // 15% chance to use Lightning Bite ability
                lightningDragon.tryActivateAbility(com.leon.saintsdragons.common.registry.LightningDragonAbilities.BITE);
            }
        }
    }
    
    @Override
    public boolean canUse() {
        // Lightning Dragon specific conditions
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Don't attack if flying (Lightning Dragons prefer aerial combat when airborne)
        if (lightningDragon.isFlying()) {
            return false;
        }
        
        // Make sure dragon has a target and can attack
        LivingEntity target = dragon.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        
        // Check if dragon can melee attack (from interface)
        if (!lightningDragon.canMeleeAttack()) {
            return false;
        }
        
        // Allow activation for targets within reasonable pursuit range
        // Don't use the base class distance check which is too restrictive
        double distSqr = dragon.distanceToSqr(target);
        return distSqr <= (directChaseRange * directChaseRange * 4); // Allow pursuit up to 4x direct chase range
    }
    
    @Override
    public boolean canContinueToUse() {
        // Lightning Dragon specific conditions
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Don't continue if flying
        if (lightningDragon.isFlying()) {
            return false;
        }
        
        // Make sure dragon has a target and can attack
        LivingEntity target = dragon.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        
        // Check if dragon can melee attack (from interface)
        if (!lightningDragon.canMeleeAttack()) {
            return false;
        }
        
        // Continue pursuing targets within extended range
        double distSqr = dragon.distanceToSqr(target);
        return distSqr <= (directChaseRange * directChaseRange * 8); // Allow continued pursuit up to 8x direct chase range (was 6x)
    }
}
