package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Shared sleep coordinator; defers animations to individual dragons but decides when to sleep/wake.
 */
public class DragonSleepBehavior {

    private final DragonEntity dragon;
    private int sleepActionCooldown = 0;
    private SleepPhase currentPhase = SleepPhase.IDLE;

    public DragonSleepBehavior(DragonEntity dragon) {
        this.dragon = dragon;
        // Always apply an initial delay to prevent immediate sleep on spawn/time change
        // Use shorter delay if sleep conditions are met (chunk reload), longer if not (fresh spawn)
        if (shouldSleepBasedOnConditions()) {
            // Sleep time - short delay for chunk reload (allows quick re-entry but not instant)
            delaySleep(60, 80);
        } else {
            // Not sleep time - longer random delay before first sleep check
            delaySleep(100, 300); // 5-15 seconds
        }
    }

    /**
     * Check if dragon should be sleeping based on current conditions (time, weather, owner)
     * Used during construction to determine if initial delay should be applied
     * NOTE: Does NOT check environment (ground, water, etc.) because entity may not be settled yet on spawn/reload
     */
    private boolean shouldSleepBasedOnConditions() {
        if (!dragon.supportsSleep()) {
            return false;
        }
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

    public void tick() {
        if (dragon.level().isClientSide) {
            return;
        }
        if (!dragon.supportsSleep()) {
            return;
        }

        if (sleepActionCooldown > 0) {
            sleepActionCooldown--;
        }

        // Wild dragons: clear any persisted sit order/cmd after reload so sleep can re-evaluate
        if (!dragon.isTame() && dragon.isOrderedToSit() && !dragon.isSleeping() && !dragon.isSleepTransitioning()) {
            dragon.setOrderedToSit(false);
            if (dragon.getCommand() == 1) {
                dragon.setCommand(0);
            }
            if (dragon.sitProgress > 0f) {
                dragon.sitProgress = 0f;
                dragon.prevSitProgress = 0f;
                dragon.getEntityData().set(DragonEntity.DATA_SIT_PROGRESS, 0f);
            }
        }

        updatePhase();

        if (dragon.isTame()) {
            handleTamedDragonSleep();
        }

        if (currentPhase == SleepPhase.IDLE && shouldAttemptSleep()) {
            if (tryStartSleeping()) {
                currentPhase = SleepPhase.ENTERING;
            }
        } else if (currentPhase == SleepPhase.SLEEPING) {
            boolean shouldWake = shouldWakeUp();
            if (shouldWake && tryWakeUp()) {
                currentPhase = SleepPhase.EXITING;
            }
        }
    }

    private void updatePhase() {
        if (dragon.isSleepTransitioning()) {
            if (dragon.isSleeping()) {
                if (currentPhase != SleepPhase.EXITING) {
                    currentPhase = SleepPhase.EXITING;
                }
            } else if (currentPhase != SleepPhase.ENTERING) {
                currentPhase = SleepPhase.ENTERING;
            }
        } else if (dragon.isSleeping()) {
            currentPhase = SleepPhase.SLEEPING;
        } else if (currentPhase != SleepPhase.IDLE) {
            currentPhase = SleepPhase.IDLE;
            delaySleep(40, 60);
        }
    }

    private void handleTamedDragonSleep() {
        if (dragon.isOrderedToSit()) {
            if (dragon.isSleeping() || dragon.isSleepTransitioning()) {
                tryWakeUp();
            }
            return;
        }

        if ((dragon.isSleeping() || dragon.isSleepTransitioning()) && !isOwnerSleepingNearby()) {
            sleepActionCooldown = 0;
            tryWakeUp();
        }
    }

    private boolean shouldAttemptSleep() {
        if (!dragon.supportsSleep()) return false;
        if (dragon.isSleeping() || dragon.isSleepTransitioning()) return false;
        if (sleepActionCooldown > 0) return false;
        if (dragon.isOrderedToSit()) return false;
        if (!canSleepInCurrentEnvironment()) return false;

        DragonSleepPreferences prefs = dragon.getSleepPreferences();
        if (!prefs.canSleepDuringConditions(dragon.level())) return false;

        if (dragon.isTame()) {
            return isOwnerSleepingNearby();
        }

        return true;
    }

    private boolean shouldWakeUp() {
        if (!dragon.supportsSleep()) return false;
        if (!dragon.isSleeping()) return false;
        if (!canSleepInCurrentEnvironment()) return true;

        DragonSleepPreferences prefs = dragon.getSleepPreferences();
        if (!prefs.canSleepDuringConditions(dragon.level())) return true;

        if (dragon.isTame() && !dragon.isOrderedToSit()) {
            return !isOwnerSleepingNearby();
        }

        return false;
    }

    private boolean canSleepInCurrentEnvironment() {
        if (dragon.isDying() || !dragon.isAlive() || dragon.isDeadOrDying()) return false;
        if (dragon.isVehicle()) return false;
        if (dragon.getTarget() != null) return false;
        if (dragon.isInWaterOrBubble() || dragon.isInLava()) return false;

        boolean alreadySleepingOrTransitioning = dragon.isSleeping() || dragon.isSleepTransitioning();
        if (!alreadySleepingOrTransitioning) {
            if (!dragon.onGround()) return false;
            if (dragon instanceof com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable flyer) {
                if (flyer.isFlying() || flyer.isHovering() || flyer.isTakeoff() || flyer.isLanding()) {
                    return false;
                }
            }
        }

        if (dragon.isSleepSuppressed()) return false;
        return true;
    }

    public boolean tryStartSleeping() {
        if (sleepActionCooldown > 0) return false;
        if (!dragon.supportsSleep()) return false;
        if (!canSleepInCurrentEnvironment()) return false;
        if (dragon.isSleeping() || dragon.isSleepTransitioning()) return false;
        dragon.startSleepEnter();
        delaySleep(20, 40);
        return true;
    }

    public boolean tryWakeUp() {
        if (sleepActionCooldown > 0) return false;
        if (!dragon.supportsSleep()) return false;
        if (!dragon.isSleeping() && !dragon.isSleepTransitioning()) return false;
        dragon.startSleepExit();
        delaySleep(20, 40);
        return true;
    }

    public void forceWakeUp() {
        sleepActionCooldown = 0;
        if (!dragon.supportsSleep()) {
            return;
        }
        if (dragon.isSleeping() || dragon.isSleepTransitioning()) {
            dragon.wakeUpImmediately();
        }
        delaySleep(90, 120);
    }

    public void delaySleep(int min, int max) {
        this.sleepActionCooldown = min + dragon.getRandom().nextInt(max - min + 1);
    }

    private boolean isOwnerSleepingNearby() {
        LivingEntity owner = dragon.getOwner();
        if (!(owner instanceof Player player)) return false;
        if (!player.isSleeping() || !player.isAlive()) return false;
        return player.level() == dragon.level() && dragon.distanceToSqr(player) <= 144.0;
    }

    public int getSleepCooldown() {
        return sleepActionCooldown;
    }

    /**
     * Sleep preferences - each dragon defines their own.
     */
    public record DragonSleepPreferences(
            boolean canSleepAtNight,
            boolean canSleepDuringDay,
            boolean avoidsThunderstorms
    ) {
        public boolean canSleepDuringConditions(net.minecraft.world.level.Level level) {
            if (avoidsThunderstorms && level.isThundering()) return false;
            boolean isDay = level.isDay();
            if (isDay && !canSleepDuringDay) return false;
            if (!isDay && !canSleepAtNight) return false;
            return true;
        }

        public static DragonSleepPreferences DIURNAL() {
            return new DragonSleepPreferences(false, true, true);
        }

        public static DragonSleepPreferences NOCTURNAL() {
            return new DragonSleepPreferences(true, false, true);
        }

        public static DragonSleepPreferences FLEXIBLE() {
            return new DragonSleepPreferences(true, true, true);
        }
    }

    private enum SleepPhase {
        IDLE,
        ENTERING,
        SLEEPING,
        EXITING
    }
}
