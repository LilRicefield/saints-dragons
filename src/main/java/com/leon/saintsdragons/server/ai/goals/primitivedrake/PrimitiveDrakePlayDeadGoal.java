package com.leon.saintsdragons.server.ai.goals.primitivedrake;

import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * AI Goal for Primitive Drakes to play dead when near Lightning Dragons.
 * This creates a realistic predator-prey interaction where drakes feign death
 * to avoid being noticed by the larger, more powerful lightning dragons.
 */
public class PrimitiveDrakePlayDeadGoal extends Goal {
    private final PrimitiveDrakeEntity drake;
    private final Level level;
    
    // Detection parameters
    private static final double DETECTION_RANGE = 12.0; // Blocks
    private static final double DETECTION_RANGE_SQR = DETECTION_RANGE * DETECTION_RANGE;
    
    // Play dead duration (randomized)
    private static final int MIN_PLAY_DEAD_DURATION = 200; // 10 seconds
    private static final int MAX_PLAY_DEAD_DURATION = 600; // 30 seconds
    
    // Cooldown between play dead sessions
    private static final int PLAY_DEAD_COOLDOWN = 1200; // 60 seconds
    
    // State tracking
    private boolean isPlayingDead = false;
    private int playDeadTicks = 0;
    private int cooldownTicks = 0;
    private LightningDragonEntity nearbyLightningDragon = null;
    
    // Targeting conditions for finding lightning dragons
    private final TargetingConditions lightningDragonTargeting = TargetingConditions.forNonCombat()
            .range(DETECTION_RANGE)
            .ignoreLineOfSight(); // Drakes can "sense" lightning dragons even through walls
    
    public PrimitiveDrakePlayDeadGoal(PrimitiveDrakeEntity drake) {
        this.drake = drake;
        this.level = drake.level();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); // Stop all movement and actions
    }
    
    @Override
    public boolean canUse() {
        // Don't play dead if already playing dead or on cooldown
        if (isPlayingDead || cooldownTicks > 0) {
            return false;
        }
        
        // Don't play dead if sleeping, dying, or being ridden
        if (drake.isSleeping() || drake.isDying() || drake.isVehicle()) {
            return false;
        }
        
        // Don't play dead if tamed and owner is nearby (they feel safe)
        if (drake.isTame()) {
            var owner = drake.getOwner();
            if (owner != null) {
                double distanceToOwner = drake.distanceToSqr(owner);
                if (distanceToOwner < 16.0) { // Within 4 blocks of owner
                    return false;
                }
            }
        }
        
        // Check for nearby lightning dragons
        return findNearbyLightningDragon();
    }
    
    @Override
    public boolean canContinueToUse() {
        // Don't continue if drake is dying, sleeping, or being ridden
        if (drake.isDying() || drake.isSleeping() || drake.isVehicle()) {
            return false;
        }
        
        // Don't continue if tamed and owner is nearby (they feel safe)
        if (drake.isTame()) {
            var owner = drake.getOwner();
            if (owner != null) {
                double distanceToOwner = drake.distanceToSqr(owner);
                if (distanceToOwner < 16.0) { // Within 4 blocks of owner
                    return false;
                }
            }
        }
        
        // Continue if still playing dead and haven't exceeded duration
        if (isPlayingDead && playDeadTicks > 0) {
            // Check if lightning dragon is still nearby - if not, continue playing dead for a bit longer
            if (nearbyLightningDragon != null && !nearbyLightningDragon.isRemoved()) {
                double distance = drake.distanceToSqr(nearbyLightningDragon);
                if (distance <= DETECTION_RANGE_SQR) {
                    return true; // Lightning dragon still nearby, keep playing dead
                }
            }
            // Lightning dragon moved away, but continue playing dead for a bit longer (realistic behavior)
            return true;
        }
        
        return false;
    }
    
    @Override
    public void start() {
        // Start playing dead
        isPlayingDead = true;
        playDeadTicks = MIN_PLAY_DEAD_DURATION + drake.getRandom().nextInt(MAX_PLAY_DEAD_DURATION - MIN_PLAY_DEAD_DURATION);
        
        // Register this goal with the drake for easy access
        drake.setPlayDeadGoal(this);
        
        // Stop all movement
        drake.getNavigation().stop();
        drake.getMoveControl().setWantedPosition(drake.getX(), drake.getY(), drake.getZ(), 0.0);
        
        // Set sitting pose for the "dead" look
        drake.setOrderedToSit(true);
        
        // Trigger the fake death animation
        drake.triggerAnim("action", "fake_death");
        
        // Play a scared sound
        drake.getSoundHandler().playVocal("primitivedrake_scared");
        
        // Debug message (can be removed in production)
        if (level.isClientSide) {
            System.out.println("Primitive Drake " + drake.getUUID() + " is playing dead!");
        }
    }
    
    @Override
    public void tick() {
        if (isPlayingDead) {
            // Decrease play dead timer
            playDeadTicks--;
            
            // Keep the drake completely still - stop all movement
            drake.getNavigation().stop();
            drake.getMoveControl().setWantedPosition(drake.getX(), drake.getY(), drake.getZ(), 0.0);
            
            // Stop any other movement goals from interfering
            drake.setDeltaMovement(0, drake.getDeltaMovement().y, 0); // Keep vertical movement for gravity
            
            // Ensure the drake stays in sitting pose
            if (!drake.isInSittingPose()) {
                drake.setOrderedToSit(true);
            }
            
            // Occasionally play scared sounds while playing dead
            if (playDeadTicks % 100 == 0 && drake.getRandom().nextFloat() < 0.3f) {
                drake.getSoundHandler().playVocal("primitivedrake_scared");
            }
        }
        
        // Decrease cooldown timer
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
    }
    
    @Override
    public void stop() {
        // Stop playing dead
        isPlayingDead = false;
        playDeadTicks = 0;
        nearbyLightningDragon = null;
        
        // Unregister this goal from the drake
        drake.clearPlayDeadGoal();
        
        // Start cooldown
        cooldownTicks = PLAY_DEAD_COOLDOWN + drake.getRandom().nextInt(600); // 60-90 seconds
        
        // Stand up
        drake.setOrderedToSit(false);
        drake.sitProgress = 0f;
        drake.getEntityData().set(com.leon.saintsdragons.server.entity.base.DragonEntity.DATA_SIT_PROGRESS, 0f);
        
        // Play a relieved sound
        drake.getSoundHandler().playVocal("primitivedrake_relieved");
        
        // Debug message (can be removed in production)
        if (level.isClientSide) {
            System.out.println("Primitive Drake " + drake.getUUID() + " stopped playing dead");
        }
    }
    
    /**
     * Find a nearby lightning dragon that could trigger the play dead behavior
     */
    private boolean findNearbyLightningDragon() {
        // Look for lightning dragons in range
        LightningDragonEntity foundDragon = level.getNearestEntity(
            LightningDragonEntity.class,
            lightningDragonTargeting,
            drake,
            drake.getX(),
            drake.getY(),
            drake.getZ(),
            drake.getBoundingBox().inflate(DETECTION_RANGE)
        );
        
        if (foundDragon != null) {
            nearbyLightningDragon = foundDragon;
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if the drake is currently playing dead
     */
    public boolean isPlayingDead() {
        return isPlayingDead;
    }
    
    /**
     * Get the remaining play dead duration in ticks
     */
    public int getRemainingPlayDeadTicks() {
        return playDeadTicks;
    }
    
    /**
     * Get the remaining cooldown in ticks
     */
    public int getRemainingCooldownTicks() {
        return cooldownTicks;
    }
}
