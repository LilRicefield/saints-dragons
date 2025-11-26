package com.leon.saintsdragons.server.entity.behavior;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Handles sleep behavior for dragons - Dawn Era inspired but adapted for our dragon system.
 * Manages sleep timing, conditions, and ensures proper setup (landing, exiting water, etc.)
 */
public class DragonSleepBehavior {

    private final DragonEntity dragon;
    private int sleepActionCooldown = 0;

    public DragonSleepBehavior(DragonEntity dragon) {
        this.dragon = dragon;
    }

    /**
     * Main tick - call from dragon's customServerAiStep() or tick()
     */
    public void tick() {
        if (dragon.level().isClientSide) {
            return;
        }

        // Decrement cooldown
        if (sleepActionCooldown > 0) {
            sleepActionCooldown--;
        }

        // Handle tamed dragon sleep based on owner commands
        if (dragon.isTame()) {
            handleTamedDragonSleep();
        }

        // Check environment and attempt sleep if conditions are right
        if (shouldAttemptSleep()) {
            tryStartSleeping();
        }

        // Check if we should wake up
        if (shouldWakeUp()) {
            tryWakeUp();
        }
    }

    /**
     * Handle tamed dragon sleep logic based on owner commands
     */
    private void handleTamedDragonSleep() {
        // If owner commanded to sit/stay, allow sleeping
        if (dragon.isOrderedToSit()) {
            sleepActionCooldown = 0;
            // Dragon can sleep while sitting
        }
        // If owner commanded to follow and dragon is sleeping, wake up
        else if ((dragon.isSleeping() || dragon.isSleepTransitioning()) && !dragon.isOrderedToSit()) {
            sleepActionCooldown = 0;
            tryWakeUp();
        }
    }

    /**
     * Check if dragon should attempt to start sleeping
     */
    private boolean shouldAttemptSleep() {
        // Already sleeping or transitioning
        if (dragon.isSleeping() || dragon.isSleepTransitioning()) {
            return false;
        }

        // On cooldown
        if (sleepActionCooldown > 0) {
            return false;
        }

        // Basic environmental checks
        if (!canSleepInCurrentEnvironment()) {
            return false;
        }

        // Check sleep preferences (day/night, weather, etc.)
        DragonSleepPreferences prefs = dragon.getSleepPreferences();
        if (!prefs.canSleepDuringConditions(dragon.level())) {
            return false;
        }

        // Tamed dragons: only auto-sleep if owner is sleeping nearby OR if ordered to sit
        if (dragon.isTame()) {
            if (dragon.isOrderedToSit()) {
                return true; // Can sleep while sitting
            }
            return isOwnerSleepingNearby();
        }

        // Wild dragons: sleep according to their preferences
        return true;
    }

    /**
     * Check if dragon should wake up
     */
    private boolean shouldWakeUp() {
        // Not sleeping
        if (!dragon.isSleeping()) {
            return false;
        }

        // Environmental danger (combat, water, etc.)
        if (!canSleepInCurrentEnvironment()) {
            return true;
        }

        // Weather/time conditions changed
        DragonSleepPreferences prefs = dragon.getSleepPreferences();
        if (!prefs.canSleepDuringConditions(dragon.level())) {
            return true;
        }

        // Tamed dragon: wake if owner is no longer sleeping (unless ordered to sit)
        if (dragon.isTame() && !dragon.isOrderedToSit()) {
            if (!isOwnerSleepingNearby()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the current environment is safe for sleeping
     * This includes ground checks, water checks, flight checks, etc.
     */
    private boolean canSleepInCurrentEnvironment() {
        // Can't sleep if dying or dead
        if (dragon.isDying() || !dragon.isAlive() || dragon.isDeadOrDying()) {
            return false;
        }

        // Can't sleep while being ridden
        if (dragon.isVehicle()) {
            return false;
        }

        // Can't sleep with a target or while aggressive
        if (dragon.getTarget() != null) {
            return false;
        }

        // Can't sleep in water or lava (dragons need to exit water first)
        if (dragon.isInWaterOrBubble() || dragon.isInLava()) {
            return false;
        }

        // CRITICAL: Can't sleep while flying/airborne - must be on ground!
        if (!dragon.onGround()) {
            return false;
        }

        // Can't sleep if actively flying (even if briefly on ground)
        if (dragon instanceof com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable flyer) {
            if (flyer.isFlying() || flyer.isHovering() || flyer.isTakeoff() || flyer.isLanding()) {
                return false;
            }
        }

        // Check custom sleep suppression (combat cooldown, etc.)
        if (dragon.isSleepSuppressed()) {
            return false;
        }

        return true;
    }

    /**
     * Attempt to start the sleep sequence
     */
    public boolean tryStartSleeping() {
        if (sleepActionCooldown > 0) {
            return false;
        }

        if (!canSleepInCurrentEnvironment()) {
            return false;
        }

        // Already sleeping/transitioning
        if (dragon.isSleeping() || dragon.isSleepTransitioning()) {
            return false;
        }

        // Start sleep enter sequence (dragon handles animation chain)
        dragon.startSleepEnter();
        delaySleep(20, 40);
        return true;
    }

    /**
     * Attempt to wake up
     */
    public boolean tryWakeUp() {
        if (sleepActionCooldown > 0) {
            return false;
        }

        if (!dragon.isSleeping() && !dragon.isSleepTransitioning()) {
            return false;
        }

        // Start wake up sequence
        dragon.startSleepExit();
        delaySleep(20, 40);
        return true;
    }

    /**
     * Force immediate wake (e.g., on damage)
     */
    public void forceWakeUp() {
        sleepActionCooldown = 0;
        if (dragon.isSleeping() || dragon.isSleepTransitioning()) {
            dragon.wakeUpImmediately();
        }
        delaySleep(90, 120);
    }

    /**
     * Delay sleep actions to prevent spam
     */
    public void delaySleep(int min, int max) {
        this.sleepActionCooldown = min + dragon.getRandom().nextInt(max - min + 1);
    }

    /**
     * Check if owner is sleeping nearby
     */
    private boolean isOwnerSleepingNearby() {
        LivingEntity owner = dragon.getOwner();
        if (!(owner instanceof Player player)) {
            return false;
        }
        if (!player.isSleeping() || !player.isAlive()) {
            return false;
        }
        // Within 12 blocks
        return player.level() == dragon.level() && dragon.distanceToSqr(player) <= 144.0;
    }

    public int getSleepCooldown() {
        return sleepActionCooldown;
    }

    /**
     * Sleep preferences - each dragon defines their own
     */
    public record DragonSleepPreferences(
        boolean canSleepAtNight,
        boolean canSleepDuringDay,
        boolean avoidsThunderstorms
    ) {
        public boolean canSleepDuringConditions(net.minecraft.world.level.Level level) {
            // Check thunderstorm
            if (avoidsThunderstorms && level.isThundering()) {
                return false;
            }

            // Check time of day
            boolean isDay = level.isDay();
            if (isDay && !canSleepDuringDay) {
                return false;
            }
            if (!isDay && !canSleepAtNight) {
                return false;
            }

            return true;
        }

        // Common presets
        public static DragonSleepPreferences DIURNAL() {
            return new DragonSleepPreferences(false, true, true); // Day sleeper
        }

        public static DragonSleepPreferences NOCTURNAL() {
            return new DragonSleepPreferences(true, false, true); // Night sleeper
        }

        public static DragonSleepPreferences FLEXIBLE() {
            return new DragonSleepPreferences(true, true, true); // Sleeps anytime
        }
    }
}
