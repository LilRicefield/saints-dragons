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
    private SleepPhase currentPhase = SleepPhase.IDLE;

    public DragonSleepBehavior(DragonEntity dragon) {
        this.dragon = dragon;
        // Only apply delay if sleep conditions are NOT currently met
        // This allows immediate re-entry on chunk reload if it's still sleep time
        if (!shouldSleepBasedOnConditions()) {
            // Not sleep time - apply random delay before first sleep check
            delaySleep(100, 300); // 5-15 seconds of wandering before first sleep check
        }
    }

    /**
     * Check if dragon should be sleeping based on current conditions (time, weather, owner)
     * Used during construction to determine if initial delay should be applied
     * NOTE: Does NOT check environment (ground, water, etc.) because entity may not be settled yet on spawn/reload
     */
    private boolean shouldSleepBasedOnConditions() {
        // Don't check canSleepInCurrentEnvironment() here - entity might not be on ground yet during construction!
        // Environment checks happen during tick evaluation after entity has settled

        // Check sleep preferences (time/weather - stable conditions)
        DragonSleepPreferences prefs = dragon.getSleepPreferences();
        if (!prefs.canSleepDuringConditions(dragon.level())) {
            return false;
        }

        // Tamed dragons: only sleep if owner is sleeping nearby
        if (dragon.isTame()) {
            return isOwnerSleepingNearby();
        }

        // Wild dragons: should sleep based on time of day (if it's daytime and dragon is diurnal, etc.)
        return true;
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

        // Wild dragons should never persist a sit order after reloads; clear it so sleep logic can re-evaluate
        if (!dragon.isTame() && dragon.isOrderedToSit() && !dragon.isSleeping() && !dragon.isSleepTransitioning()) {
            dragon.setOrderedToSit(false);
            if (dragon.getCommand() == 1) {
                dragon.setCommand(0);
            }
            // Hard reset sit pose to avoid lingering T-poses on reload
            if (dragon.sitProgress > 0f) {
                dragon.sitProgress = 0f;
                dragon.prevSitProgress = 0f;
                dragon.getEntityData().set(DragonEntity.DATA_SIT_PROGRESS, 0f);
            }
        }

        // Update phase based on actual dragon state
        SleepPhase oldPhase = currentPhase;
        updatePhase();

        // Handle tamed dragon sleep based on owner commands
        if (dragon.isTame()) {
            handleTamedDragonSleep();
        }

        // Only attempt state changes when in stable phases
        if (currentPhase == SleepPhase.IDLE && shouldAttemptSleep()) {
            if (tryStartSleeping()) {
                currentPhase = SleepPhase.ENTERING;
            }
        } else if (currentPhase == SleepPhase.SLEEPING) {
            boolean shouldWake = shouldWakeUp();
            if (shouldWake) {
                if (tryWakeUp()) {
                    currentPhase = SleepPhase.EXITING;
                }
            }
        }
    }

    /**
     * Update internal phase based on dragon's actual sleep state
     */
    private void updatePhase() {
        if (dragon.isSleepTransitioning()) {
            // Dragon is transitioning - determine which direction
            if (dragon.isSleeping()) {
                // Still marked as sleeping but transitioning = waking up
                if (currentPhase != SleepPhase.EXITING) {
                    currentPhase = SleepPhase.EXITING;
                }
            } else {
                // Not sleeping yet but transitioning = entering sleep
                if (currentPhase != SleepPhase.ENTERING) {
                    currentPhase = SleepPhase.ENTERING;
                }
            }
        } else if (dragon.isSleeping()) {
            // Fully asleep
            currentPhase = SleepPhase.SLEEPING;
        } else {
            // Awake and not transitioning
            if (currentPhase != SleepPhase.IDLE) {
                currentPhase = SleepPhase.IDLE;
                // Add cooldown after waking to prevent immediate re-sleep
                delaySleep(40, 60);
            }
        }
    }

    /**
     * Handle tamed dragon sleep logic based on owner proximity
     * Tamed dragons sleep when owner is sleeping nearby (like wolves/cats in vanilla)
     */
    private void handleTamedDragonSleep() {
        // If dragon is sitting, prevent auto-sleep (sit is a separate command, not sleep)
        if (dragon.isOrderedToSit()) {
            // Wake up if currently sleeping while sitting (shouldn't happen, but safety check)
            if (dragon.isSleeping() || dragon.isSleepTransitioning()) {
                tryWakeUp();
            }
            return;
        }

        // If owner is not sleeping and dragon is sleeping, wake up
        if ((dragon.isSleeping() || dragon.isSleepTransitioning()) && !isOwnerSleepingNearby()) {
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

        // NEVER sleep while sitting (sit is a separate command, not sleep)
        if (dragon.isOrderedToSit()) {
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

        // Tamed dragons: only auto-sleep if owner is sleeping nearby
        if (dragon.isTame()) {
            return isOwnerSleepingNearby();
        }

        // Wild dragons: sleep according to their preferences (time of day, etc.)
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
     * Debug method to explain why dragon should wake
     */
    private String getWakeReason() {
        if (!dragon.isSleeping()) return "not sleeping";
        if (!canSleepInCurrentEnvironment()) return "environment unsafe: " + getEnvironmentFailure();

        DragonSleepPreferences prefs = dragon.getSleepPreferences();
        if (!prefs.canSleepDuringConditions(dragon.level())) {
            boolean isDay = dragon.level().isDay();
            if (dragon.level().isThundering()) return "thunderstorm";
            if (isDay && !prefs.canSleepDuringDay()) return "daytime (nocturnal)";
            if (!isDay && !prefs.canSleepAtNight()) return "nighttime (diurnal)";
            return "time/weather conditions";
        }

        if (dragon.isTame() && !dragon.isOrderedToSit() && !isOwnerSleepingNearby()) {
            return "owner not sleeping";
        }

        return "unknown";
    }

    private String getEnvironmentFailure() {
        if (dragon.isDying() || !dragon.isAlive() || dragon.isDeadOrDying()) return "dying/dead";
        if (dragon.isVehicle()) return "being ridden";
        if (dragon.getTarget() != null) return "has target";
        if (dragon.isInWaterOrBubble() || dragon.isInLava()) return "in water/lava";
        if (!dragon.onGround()) return "not on ground";

        if (dragon instanceof com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable flyer) {
            if (flyer.isFlying()) return "flying";
            if (flyer.isHovering()) return "hovering";
            if (flyer.isTakeoff()) return "taking off";
            if (flyer.isLanding()) return "landing";
        }

        if (dragon.isSleepSuppressed()) return "sleep suppressed";
        return "unknown";
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

        // CRITICAL: Only check ground/flight BEFORE entering sleep!
        // Once sleeping/transitioning, don't check (animations might lift dragon slightly)
        boolean alreadySleepingOrTransitioning = dragon.isSleeping() || dragon.isSleepTransitioning();

        if (!alreadySleepingOrTransitioning) {
            // Must be on ground to START sleeping
            if (!dragon.onGround()) {
                return false;
            }

            // Can't start sleep if actively flying
            if (dragon instanceof com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable flyer) {
                if (flyer.isFlying() || flyer.isHovering() || flyer.isTakeoff() || flyer.isLanding()) {
                    return false;
                }
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

    /**
     * Sleep phase tracking to prevent looping behavior
     */
    private enum SleepPhase {
        IDLE,      // Not sleeping, can attempt sleep
        ENTERING,  // Commanded sleep, waiting for transition
        SLEEPING,  // Fully asleep
        EXITING    // Commanded wake, waiting for transition
    }
}
