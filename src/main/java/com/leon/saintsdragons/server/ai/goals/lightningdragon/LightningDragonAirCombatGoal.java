package com.leon.saintsdragons.server.ai.goals.lightningdragon;

import com.leon.saintsdragons.common.registry.lightningdragon.LightningDragonAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonAirCombatGoalBase;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Lightning Dragon specific air combat goal with FSM precision.
 * Uses base class structure but implements FSM mechanics for precise air combat.
 */
public class LightningDragonAirCombatGoal extends DragonAirCombatGoalBase {
    
    // FSM State Management for Lightning Dragon air combat
    private int airCombatTimer = 0;
    private int diveCooldown = 0;
    private int beamCooldown = 0;
    private boolean isDiving = false;
    private boolean isCircling = false;
    private Vec3 lastTargetPos = Vec3.ZERO;
    
    public LightningDragonAirCombatGoal(LightningDragonEntity dragon) {
        super(dragon);
        configureLightningDragonAirCombat();
    }
    
    private void configureLightningDragonAirCombat() {
        // Lightning Dragon specific air combat parameters
        this.engageMaxRangeSqr = 80 * 80;      // Long engagement range
        this.airPreferredDist = 22.0;           // Preferred air distance
        this.airAltGap = 6.0;                   // Altitude gap preference
        this.minAirDwellTicks = 100;            // Minimum air time
        this.minGroundDwellTicks = 60;          // Minimum ground time
    }
    
    @Override
    protected void performDiveAttack(LivingEntity target) {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Lightning Dragon specific dive attack
        if (lightningDragon.canUseAbility() && lightningDragon.getRandom().nextFloat() < 0.4f) {
            // 40% chance for lightning dive attack
            lightningDragon.tryActivateAbility(LightningDragonAbilities.ROAR);
        }
        
        // Trigger dive attack animation
        // lightningDragon.setDiveAttackAnimation(true);
    }
    
    @Override
    protected void performRangedAttack(LivingEntity target) {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Lightning Dragon specific ranged attack
        if (lightningDragon.canUseAbility() && lightningDragon.getRandom().nextFloat() < 0.6f) {
            // 60% chance for lightning beam attack
            lightningDragon.tryActivateAbility(LightningDragonAbilities.LIGHTNING_BEAM);
        }
        
        // Trigger ranged attack animation
        // lightningDragon.setRangedAttackAnimation(true);
    }
    
    @Override
    protected boolean isPathClear(Vec3 start, Vec3 end, double maxDist) {
        // Lightning Dragons can see through some obstacles due to electrical senses
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Enhanced line of sight for Lightning Dragons
        return lightningDragon.hasEnhancedLineOfSight(end) || 
               super.isPathClear(start, end, maxDist);
    }
    
    @Override
    public boolean canUse() {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Lightning Dragons prefer aerial combat when they have energy
        if (lightningDragon.getEnergyLevel() < 0.3f) {
            return false; // Not enough energy for air combat
        }
        
        // Override base class random chance - Lightning Dragons are more aggressive about air combat
        LivingEntity target = dragon.getTarget();
        if (target == null || !target.isAlive()) return false;
        
        double distSqr = dragon.distanceToSqr(target);
        if (distSqr > engageMaxRangeSqr) return false;
        
        // Lightning Dragons have higher chance for air combat when target is far or elevated
        double dist = dragon.distanceTo(target);
        boolean targetFar = dist >= airPreferredDist;
        boolean targetHigh = (target.getY() - dragon.getY()) >= airAltGap;
        boolean groundPathBlocked = dist > 10 && !dragon.getSensing().hasLineOfSight(target);
        
        // Much higher activation chance for Lightning Dragons
        float activationChance = (targetFar || targetHigh || groundPathBlocked) ? 0.8f : 0.3f;
        
        return lightningDragon.canTakeoff() && 
               !lightningDragon.isLanding() &&
               dragon.getRandom().nextFloat() < activationChance;
    }
    
    @Override
    public boolean canContinueToUse() {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Continue if we have energy and are already airborne
        if (lightningDragon.getEnergyLevel() < 0.1f) {
            return false; // Too low energy to continue
        }
        
        LivingEntity target = dragon.getTarget();
        if (target == null || !target.isAlive()) return false;
        
        double distSqr = dragon.distanceToSqr(target);
        if (distSqr > engageMaxRangeSqr * 1.5) return false;
        
        // Continue air combat if already flying or in air combat phases
        return lightningDragon.isFlying() || lightningDragon.isTakeoff() || 
               lightningDragon.isHovering() || phase != Phase.DECIDE;
    }
    
    @Override
    public void start() {
        super.start();
        airCombatTimer = 0;
        diveCooldown = 0;
        beamCooldown = 0;
        isDiving = false;
        isCircling = false;
        lastTargetPos = Vec3.ZERO;
    }
    
    @Override
    public void stop() {
        super.stop();
        airCombatTimer = 0;
        diveCooldown = 0;
        beamCooldown = 0;
        isDiving = false;
        isCircling = false;
        lastTargetPos = Vec3.ZERO;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Lightning Dragon FSM air combat enhancements
        airCombatTimer++;
        if (diveCooldown > 0) diveCooldown--;
        if (beamCooldown > 0) beamCooldown--;
        
        LivingEntity target = dragon.getTarget();
        if (target == null || !target.isAlive()) return;
        
        // Enhanced air combat logic for Lightning Dragon
        if (phase == Phase.AIR_FIGHT) {
            enhanceAirFight(target);
        }
    }
    
    private void enhanceAirFight(LivingEntity target) {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        double dist = dragon.distanceTo(target);
        Vec3 targetPos = target.position();
        
        // Lightning Dragon specific air combat behaviors
        if (mode == AirMode.DIVE_BOMB && !isDiving && diveCooldown <= 0) {
            // Enhanced dive attack with Lightning Dragon precision
            if (dist < 25.0 && dragon.getSensing().hasLineOfSight(target)) {
                performLightningDive(target);
                isDiving = true;
                diveCooldown = 60; // 3 second cooldown
            }
        } else if (mode == AirMode.RANGED_CIRCLE && !isCircling && beamCooldown <= 0) {
            // Enhanced ranged attack with Lightning Dragon precision
            if (dist > 15.0 && dist < 40.0) {
                performLightningBeam(target);
                isCircling = true;
                beamCooldown = 40; // 2 second cooldown
            }
        }
        
        // Reset states
        if (isDiving && dist > 30.0) {
            isDiving = false;
        }
        if (isCircling && dist < 10.0) {
            isCircling = false;
        }
        
        // Update last target position for movement prediction
        lastTargetPos = targetPos;
    }
    
    private void performLightningDive(LivingEntity target) {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Lightning Dragon dive attack with electrical effects
        if (lightningDragon.canUseAbility()) {
            float abilityChance = lightningDragon.getRandom().nextFloat();
            
            if (abilityChance < 0.4f) {
                // 40% chance for lightning dive attack
                lightningDragon.tryActivateAbility(LightningDragonAbilities.ROAR);
            } else if (abilityChance < 0.7f) {
                // 30% chance for lightning beam during dive
                lightningDragon.tryActivateAbility(LightningDragonAbilities.LIGHTNING_BEAM);
            }
        }
        
        // Enhanced dive movement with electrical trail
        Vec3 diveDirection = target.position().subtract(dragon.position()).normalize();
        Vec3 diveTarget = target.position().add(diveDirection.scale(3.0));
        
        dragon.getMoveControl().setWantedPosition(diveTarget.x, diveTarget.y, diveTarget.z, 
            flightCapable.getFlightSpeed() * 2.0f); // Faster dive speed
        
        // Lightning effect on dive path
        if (lightningDragon.getRandom().nextFloat() < 0.3f) {
            lightningDragon.playLightningEffect(dragon.position());
        }
    }
    
    private void performLightningBeam(LivingEntity target) {
        LightningDragonEntity lightningDragon = (LightningDragonEntity) dragon;
        
        // Lightning Dragon ranged attack with precision
        if (lightningDragon.canUseAbility()) {
            float abilityChance = lightningDragon.getRandom().nextFloat();
            
            if (abilityChance < 0.6f) {
                // 60% chance for lightning beam attack
                lightningDragon.tryActivateAbility(LightningDragonAbilities.LIGHTNING_BEAM);
            } else if (abilityChance < 0.8f) {
                // 20% chance for lightning storm
                lightningDragon.tryActivateAbility(LightningDragonAbilities.ROAR);
            }
        }
        
        // Enhanced circling with electrical effects
        Vec3 targetPos = target.position();
        double radius = Math.max(20.0, dragon.distanceTo(target) * 0.7);
        double altitude = targetPos.y + flightCapable.getPreferredFlightAltitude() + 5.0;
        
        // More aggressive circling for Lightning Dragons
        double angle = (dragon.tickCount * 0.03) * (circleClockwise ? 1 : -1);
        Vec3 circlePos = new Vec3(
            targetPos.x + Math.cos(angle) * radius,
            altitude + Math.sin(dragon.tickCount * 0.08) * 5.0, // More altitude variation
            targetPos.z + Math.sin(angle) * radius
        );
        
        dragon.getMoveControl().setWantedPosition(circlePos.x, circlePos.y, circlePos.z, 
            flightCapable.getFlightSpeed() * 1.2f); // Faster circling
        
        // Lightning effects during circling
        if (lightningDragon.getRandom().nextFloat() < 0.2f) {
            lightningDragon.playLightningEffect(circlePos);
        }
    }
}
