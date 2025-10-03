package com.leon.saintsdragons.server.ai.goals.primitivedrake;

import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.List;

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
    private boolean skipCooldownOnStop = false;
    
    public PrimitiveDrakePlayDeadGoal(PrimitiveDrakeEntity drake) {
        this.drake = drake;
        this.level = drake.level();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); // Stop all movement and actions
    }
    
    @Override
    public boolean canUse() {
        // Don't play dead if already playing dead
        if (isPlayingDead) {
            return false;
        }

        // Don't play dead if sleeping, dying, or being ridden
        if (drake.isSleeping() || drake.isDying() || drake.isVehicle()) {
            return false;
        }

        // Check for nearby lightning dragons first
        if (!findNearbyLightningDragon()) {
            return false;
        }

        // If tamed drake, only play dead around WILD lightning dragons
        if (drake.isTame()) {
            if (nearbyLightningDragon == null || nearbyLightningDragon.isTame()) {
                return false;
            }
        }

        // Skip cooldown when a real threat is nearby
        if (cooldownTicks > 0) {
            boolean hasThreat = drake.isTame()
                    ? (nearbyLightningDragon != null && !nearbyLightningDragon.isTame())
                    : nearbyLightningDragon != null;

            if (hasThreat) {
                cooldownTicks = 0;
            } else {
                return false;
            }
        }

        return true;
    }
    
    @Override
    public boolean canContinueToUse() {
        // Don't continue if drake is dying, sleeping, or being ridden
        if (drake.isDying() || drake.isSleeping() || drake.isVehicle()) {
            return false;
        }
        
        // Continue if still playing dead and haven't exceeded duration
        if (isPlayingDead && playDeadTicks > 0) {
            // If tamed drake, only continue playing dead around WILD lightning dragons
            if (drake.isTame()) {
                // Tamed drakes don't continue playing dead around tamed lightning dragons
                if (nearbyLightningDragon != null && nearbyLightningDragon.isTame()) {
                    skipCooldownOnStop = true;
                    return false;
                }

                // For tamed drakes, re-check if there are any wild lightning dragons nearby
                // If not, stop playing dead immediately
                var nearbyWildLightningDragons = drake.level().getEntitiesOfClass(
                    com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity.class,
                    drake.getBoundingBox().inflate(DETECTION_RANGE),
                    dragon -> dragon != null && !dragon.isRemoved() && !dragon.isTame()
                );

                if (nearbyWildLightningDragons.isEmpty()) {
                    skipCooldownOnStop = true;
                    return false; // No wild lightning dragons nearby, stop playing dead
                }

                LightningDragonEntity closestWild = null;
                double closestDistance = Double.MAX_VALUE;
                for (LightningDragonEntity dragon : nearbyWildLightningDragons) {
                    double distance = drake.distanceToSqr(dragon);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestWild = dragon;
                    }
                }
                if (closestWild != null) {
                    nearbyLightningDragon = closestWild;
                }
            }
            // Wild drakes continue playing dead around ANY lightning dragon (wild or tamed)
            
            // Check if lightning dragon is still nearby
            if (nearbyLightningDragon != null && !nearbyLightningDragon.isRemoved()) {
                double distance = drake.distanceToSqr(nearbyLightningDragon);
                if (distance <= DETECTION_RANGE_SQR) {
                    return true; // Lightning dragon still nearby, keep playing dead
                }
            }
            // Lightning dragon moved away - stop playing dead soon (give it a few seconds to be safe)
            if (playDeadTicks > 60) { // If more than 3 seconds left, reduce it
                playDeadTicks = 40 + drake.getRandom().nextInt(40); // 2-4 seconds to "wake up"
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    public void start() {
        // Start playing dead
        isPlayingDead = true;
        skipCooldownOnStop = false;
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
        
        // Sync state to client
        drake.getEntityData().set(com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity.DATA_PLAYING_DEAD, true);
        
        // Play a scared sound
        drake.getSoundHandler().playVocal("primitivedrake_scared");

    }
    
    @Override
    public void tick() {
        if (isPlayingDead) {
            // Check if lightning dragon is still nearby
            boolean threatNearby = false;
            if (nearbyLightningDragon != null && !nearbyLightningDragon.isRemoved()) {
                double distance = drake.distanceToSqr(nearbyLightningDragon);
                if (distance <= DETECTION_RANGE_SQR) {
                    threatNearby = true;
                }
            }

            // Only decrease timer if threat is gone - play dead indefinitely while threat is nearby
            if (!threatNearby) {
                playDeadTicks--;
            } else {
                // Keep timer from running out while threat is present
                if (playDeadTicks < 100) {
                    playDeadTicks = 100; // Maintain minimum time
                }
            }

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
            if (drake.tickCount % 100 == 0 && drake.getRandom().nextFloat() < 0.3f) {
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
        
        
        // Start cooldown
        if (skipCooldownOnStop) {
            cooldownTicks = 0;
        } else {
            cooldownTicks = PLAY_DEAD_COOLDOWN + drake.getRandom().nextInt(600); // 60-90 seconds
        }
        skipCooldownOnStop = false;
        
        // Stand up
        drake.setOrderedToSit(false);
        drake.triggerAnim("action", "clear_fake_death");
        drake.sitProgress = 0f;
        drake.getEntityData().set(com.leon.saintsdragons.server.entity.base.DragonEntity.DATA_SIT_PROGRESS, 0f);
        
        // Sync state to client
        drake.getEntityData().set(com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity.DATA_PLAYING_DEAD, false);
        
        // Play a relieved sound
        drake.getSoundHandler().playVocal("primitivedrake_relieved");

    }
    
    /**
     * Find a nearby lightning dragon that could trigger the play dead behavior
     */
    private boolean findNearbyLightningDragon() {
        // Look for lightning dragons in range
        List<LightningDragonEntity> dragonsInRange = level.getEntitiesOfClass(
            LightningDragonEntity.class,
            drake.getBoundingBox().inflate(DETECTION_RANGE),
            dragon -> dragon != null && !dragon.isRemoved()
        );

        if (dragonsInRange.isEmpty()) {
            nearbyLightningDragon = null;
            return false;
        }

        LightningDragonEntity closestDragon = null;
        LightningDragonEntity closestWildDragon = null;
        double closestDistance = Double.MAX_VALUE;
        double closestWildDistance = Double.MAX_VALUE;

        for (LightningDragonEntity dragon : dragonsInRange) {
            double distance = drake.distanceToSqr(dragon);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestDragon = dragon;
            }

            if (!dragon.isTame() && distance < closestWildDistance) {
                closestWildDistance = distance;
                closestWildDragon = dragon;
            }
        }

        if (drake.isTame()) {
            if (closestWildDragon != null) {
                nearbyLightningDragon = closestWildDragon;
                return true;
            }

            nearbyLightningDragon = closestDragon;
            return nearbyLightningDragon != null;
        }

        nearbyLightningDragon = closestWildDragon != null ? closestWildDragon : closestDragon;
        return nearbyLightningDragon != null;
    }

    /**
     * Skip the cooldown when the goal stops, used when retreating from friendly dragons.
     */
    public void markSkipCooldownOnNextStop() {
        this.skipCooldownOnStop = true;
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
    
    /**
     * Restore play dead state from saved data
     * Called when the entity is loaded from NBT
     */
    public void restorePlayDeadState(int playDeadTicks, int cooldownTicks) {
        if (playDeadTicks > 0) {
            this.isPlayingDead = true;
            this.playDeadTicks = playDeadTicks;
            this.cooldownTicks = cooldownTicks;
            this.skipCooldownOnStop = false;
            
            // Register this goal with the drake
            drake.setPlayDeadGoal(this);
            
            // Set sitting pose for the "dead" look
            drake.setOrderedToSit(true);
            
            // Trigger the fake death animation
            drake.triggerAnim("action", "fake_death");
            
            // Sync state to client
            drake.getEntityData().set(com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity.DATA_PLAYING_DEAD, true);
        }
    }
}
