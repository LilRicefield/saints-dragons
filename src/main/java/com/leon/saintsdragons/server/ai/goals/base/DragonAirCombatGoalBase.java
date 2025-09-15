package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.util.DragonMathUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Base class for dragon air combat goals.
 * Works with any dragon that implements DragonFlightCapable.
 */
public abstract class DragonAirCombatGoalBase extends Goal {
    protected final DragonEntity dragon;
    protected final DragonFlightCapable flightCapable;
    
    // Engagement parameters - can be overridden by subclasses
    protected double engageMaxRangeSqr = 80 * 80;
    protected double airPreferredDist = 22.0;
    protected double airAltGap = 6.0;
    protected int minAirDwellTicks = 100;
    protected int minGroundDwellTicks = 60;
    
    // Attack phases for animation hooks
    protected static final int ATTACK_KIND_DIVE = 4;
    protected static final int ATTACK_KIND_BEAM = 5;
    protected static final int PHASE_WINDUP = 1;
    protected static final int PHASE_COMMIT = 2;
    protected static final int PHASE_RECOVER = 3;
    
    protected enum Phase { DECIDE, TAKEOFF, CLIMB, AIR_FIGHT, LANDING }
    protected enum AirMode { DIVE_BOMB, RANGED_CIRCLE }
    
    protected Phase phase = Phase.DECIDE;
    protected int phaseTimer = 0;
    protected AirMode mode = null;
    
    // No persistent waypoints; compute per tick
    protected Boolean circleClockwise = null;
    
    protected int airDwell = 0;
    protected int groundDwell = 0;
    
    public DragonAirCombatGoalBase(DragonEntity dragon) {
        this.dragon = dragon;
        this.flightCapable = (DragonFlightCapable) dragon;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        LivingEntity target = dragon.getTarget();
        if (target == null || !target.isAlive()) return false;
        
        double distSqr = dragon.distanceToSqr(target);
        if (distSqr > engageMaxRangeSqr) return false;
        
        // Use interface methods instead of hardcoded dragon type
        return flightCapable.canTakeoff() && 
               !flightCapable.isLanding() &&
               dragon.getRandom().nextFloat() < 0.1f; // 10% chance per tick
    }
    
    @Override
    public boolean canContinueToUse() {
        LivingEntity target = dragon.getTarget();
        if (target == null || !target.isAlive()) return false;
        
        double distSqr = dragon.distanceToSqr(target);
        if (distSqr > engageMaxRangeSqr * 1.5) return false;
        
        return flightCapable.isFlying() || flightCapable.isTakeoff() || 
               flightCapable.isHovering() || phase != Phase.DECIDE;
    }
    
    @Override
    public void start() {
        phase = Phase.DECIDE;
        phaseTimer = 0;
        mode = null;
        circleClockwise = null;
    }
    
    @Override
    public void stop() {
        phase = Phase.DECIDE;
        phaseTimer = 0;
        mode = null;
        circleClockwise = null;
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
    
    protected void decidePhase(LivingEntity target) {
        double dist = dragon.distanceTo(target);
        boolean far = dist >= airPreferredDist;
        boolean highGap = (target.getY() - dragon.getY()) >= airAltGap;
        boolean groundPathBad = dist > 10 && !DragonMathUtil.hasLineOfSight(dragon, target);

        boolean preferAir = (far || highGap || groundPathBad);

        if ((preferAir || airDwell > 0) && groundDwell == 0) {
            if (!flightCapable.isFlying()) {
                flightCapable.setFlying(true);
                flightCapable.setTakeoff(true);
                flightCapable.setHovering(true);
                flightCapable.setLanding(false);
                phase = Phase.TAKEOFF;
            } else {
                // Already airborne: proceed directly to fight if we have altitude
                phase = Phase.AIR_FIGHT;
            }
            phaseTimer = 0;
            return;
        }

        if (flightCapable.isFlying() || flightCapable.isTakeoff() || flightCapable.isHovering()) {
            phase = Phase.LANDING;
            phaseTimer = 0;
            flightCapable.setLanding(true);
        } else {
            stop(); // yield to ground melee
        }
    }
    
    protected void handleTakeoff(LivingEntity target) {
        if (phaseTimer > 10) {
            phase = Phase.CLIMB;
            phaseTimer = 0;
        } else {
            dragon.getLookControl().setLookAt(target, 20f, 20f);
        }
    }
    
    protected void handleClimb(LivingEntity target) {
        Vec3 targetPos = target.position();
        Vec3 dragonPos = dragon.position();
        
        // Position slightly ahead of target
        Vec3 aheadPos = targetPos.add(target.getDeltaMovement().scale(2.0));
        Vec3 climbTarget = new Vec3(aheadPos.x, aheadPos.y + flightCapable.getPreferredFlightAltitude(), aheadPos.z);
        
        dragon.getMoveControl().setWantedPosition(climbTarget.x, climbTarget.y, climbTarget.z, flightCapable.getFlightSpeed());
        dragon.getLookControl().setLookAt(target, 20f, 20f);
        
        // Timeout for climb phase
        if (phaseTimer > 60 || dragon.position().distanceTo(climbTarget) < 5.0) {
            phase = Phase.AIR_FIGHT;
            phaseTimer = 0;
            mode = null;
        }
    }
    
    protected void handleAirFight(LivingEntity target) {
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
            flightCapable.setLanding(true);
            mode = null; // Reset mode for next air combat session
        }
    }
    
    protected void handleLanding() {
        if (dragon.onGround()) {
            flightCapable.setLanding(false);
            flightCapable.setFlying(false);
            flightCapable.setTakeoff(false);
            flightCapable.setHovering(false);
            flightCapable.markLandedNow();
            airDwell = 0;
            groundDwell = minGroundDwellTicks;
            stop();
        } else {
            flightCapable.setHovering(true);
        }
    }
    
    // ===== Air modes =====
    protected void doDiveBomb(LivingEntity target) {
        Vec3 targetPos = target.position();
        Vec3 dragonPos = dragon.position();
        
        // Calculate dive trajectory
        Vec3 diveDirection = targetPos.subtract(dragonPos).normalize();
        Vec3 diveTarget = targetPos.add(diveDirection.scale(2.0));
        
        // Apply dive movement
        dragon.getMoveControl().setWantedPosition(diveTarget.x, diveTarget.y, diveTarget.z, flightCapable.getFlightSpeed() * 1.5f);
        dragon.getLookControl().setLookAt(target, 30f, 30f);
        
        // Check for dive attack opportunity
        if (dragon.distanceTo(target) < 8.0) {
            performDiveAttack(target);
        }
    }
    
    protected void doRangedCircle(LivingEntity target) {
        Vec3 targetPos = target.position();
        Vec3 dragonPos = dragon.position();
        
        // Calculate circling parameters
        double radius = Math.max(15.0, dragon.distanceTo(target) * 0.6);
        double altitude = targetPos.y + flightCapable.getPreferredFlightAltitude();
        
        // Determine circle direction
        if (circleClockwise == null) {
            circleClockwise = dragon.getRandom().nextBoolean();
        }
        
        // Calculate circle position
        double angle = (dragon.tickCount * 0.02) * (circleClockwise ? 1 : -1);
        Vec3 circlePos = new Vec3(
            targetPos.x + Math.cos(angle) * radius,
            altitude + Math.sin(dragon.tickCount * 0.05) * 3.0, // Altitude variation
            targetPos.z + Math.sin(angle) * radius
        );
        
        dragon.getMoveControl().setWantedPosition(circlePos.x, circlePos.y, circlePos.z, flightCapable.getFlightSpeed() * 0.8f);
        dragon.getLookControl().setLookAt(target, 20f, 20f);
        
        // Perform ranged attack
        if (dragon.tickCount % 40 == 0) { // Every 2 seconds
            performRangedAttack(target);
        }
    }
    
    protected boolean isPathClear(Vec3 start, Vec3 end, double maxDist) {
        // Simple line of sight check - can be overridden by subclasses
        // For now, use simple distance check
        return dragon.position().distanceTo(end) < maxDist;
    }
    
    /**
     * Perform a dive attack. Override for dragon-specific behavior.
     */
    protected abstract void performDiveAttack(LivingEntity target);
    
    /**
     * Perform a ranged attack. Override for dragon-specific behavior.
     */
    protected abstract void performRangedAttack(LivingEntity target);
    
    /**
     * Configure air combat parameters. Override in subclasses for different dragon types.
     */
    protected void configureAirCombatParameters() {
        // Default parameters - can be overridden
        this.engageMaxRangeSqr = 80 * 80;
        this.airPreferredDist = 22.0;
        this.airAltGap = 6.0;
        this.minAirDwellTicks = 100;
        this.minGroundDwellTicks = 60;
    }
}
