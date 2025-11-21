package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Daytime sleep goal for Raevyx.
 * Wild wyverns sleep through the day; tamed wyverns sleep when their owner is asleep.
 * Keeps the full enter/exit animation chain but without intermediate "rest" pauses.
 */
public class RaevyxSleepGoal extends Goal {

    private final Raevyx wyvern;
    private int retryCooldownTicks;
    private SleepPhase phase = SleepPhase.IDLE;
    private int phaseTimer;

    public RaevyxSleepGoal(Raevyx wyvern) {
        this.wyvern = wyvern;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (retryCooldownTicks > 0) {
            retryCooldownTicks--;
            return false;
        }

        // Already sleeping or mid transition? allow the goal to continue if daytime
        if (wyvern.isSleepLocked() || wyvern.isSleeping() || wyvern.isSleepTransitioning()) {
            return canSleepNow();
        }

        if (!canSleepNow()) return false;
        if (!wyvern.canSleepNow() || wyvern.isSleepSuppressed()) return false;
        if (wyvern.isInWaterOrBubble() || wyvern.isInLava()) return false;
        if (wyvern.isDying() || wyvern.isVehicle() || wyvern.isFlying()) return false;
        if (wyvern.getTarget() != null || wyvern.isAggressive()) return false;

        // Wild dragons sleep through the day. Tamed dragons sleep when their owner is asleep and nearby.
        return !wyvern.isTame() || ownerAsleepNearby();
    }

    @Override
    public boolean canContinueToUse() {
        if (phase == SleepPhase.IDLE) {
            return false;
        }

        // Allow exit/cleanup to run even if wake conditions are met
        if (phase == SleepPhase.EXITING) {
            return wyvern.isSleepLocked() || wyvern.isSleepTransitioning() || phaseTimer > 0;
        }

        boolean shouldStayAsleep = canSleepNow() && isEnvironmentCalm();
        if (wyvern.isTame()) {
            shouldStayAsleep = shouldStayAsleep && ownerAsleepNearby();
        }
        // While entering, keep going to finish the chain; while sleeping, stay if calm
        return (phase == SleepPhase.ENTERING && (wyvern.isSleepLocked() || wyvern.isSleepTransitioning()))
                || (phase == SleepPhase.SLEEPING && shouldStayAsleep && wyvern.isSleepLocked());
    }

    @Override
    public void start() {
        phase = SleepPhase.ENTERING;
        boolean alreadySitting = wyvern.isOrderedToSit() || wyvern.getSitProgress() >= wyvern.maxSitTicks();
        // If already seated (owner command), skip sit_down buffer
        phaseTimer = (alreadySitting ? 0 : wyvern.getSleepSitDownDuration())
                + wyvern.getSleepFallAsleepDuration() + 4; // small buffer
        wyvern.startSleepEnter();
    }

    @Override
    public void tick() {
        if (wyvern.level().isClientSide) return;

        boolean keepSitting = !(phase == SleepPhase.EXITING && phaseTimer <= 0 && !wyvern.isSleepTransitioning());
        freezeMotion(keepSitting);
        if (phaseTimer > 0) {
            phaseTimer--;
        }

        boolean calm = isEnvironmentCalm();
        boolean ownerOk = !wyvern.isTame() || ownerAsleepNearby();
        boolean sleepWindow = canSleepNow();

        // Any threat/aggro forces immediate wake sequencing
        if (!calm && phase != SleepPhase.IDLE && phase != SleepPhase.EXITING) {
            wyvern.startSleepExit();
            phase = SleepPhase.EXITING;
            boolean ownerWantsSit = wyvern.isTame() && wyvern.getCommand() == 1;
            phaseTimer = wyvern.getSleepWakeUpDuration()
                    + (ownerWantsSit ? 0 : wyvern.getSleepSitUpDuration()) + 8;
        }

        if (phase == SleepPhase.ENTERING) {
            // Promote to sleeping once the entity reports it (after fall_asleep finishes)
            if (wyvern.isSleeping()) {
                phase = SleepPhase.SLEEPING;
                phaseTimer = 0; // unmanaged until wake condition
            }
            return;
        }

        if (phase == SleepPhase.SLEEPING) {
            boolean shouldWake = !(calm && ownerOk && sleepWindow);
            if (shouldWake && !wyvern.isSleepTransitioning()) {
                wyvern.startSleepExit();
                phase = SleepPhase.EXITING;
                // If owner commanded sit, we stop at sit after wake_up (no stand)
                boolean ownerWantsSit = wyvern.isTame() && wyvern.getCommand() == 1;
                phaseTimer = wyvern.getSleepWakeUpDuration()
                        + (ownerWantsSit ? 0 : wyvern.getSleepSitUpDuration()) + 8;
            }
            return;
        }

        if (phase == SleepPhase.EXITING) {
            // Keep seated until stand-up begins; allow sit_up via orderedToSit(false) when timer elapses
            if (phaseTimer <= 0 && !wyvern.isSleepTransitioning()) {
                boolean ownerWantsSit = wyvern.isTame() && wyvern.getCommand() == 1;
                if (!ownerWantsSit) {
                    wyvern.setOrderedToSit(false);
                } else {
                    wyvern.setOrderedToSit(true);
                    wyvern.setGroundMoveStateFromAI(0);
                }
                phase = SleepPhase.IDLE;
            }
        }
    }

    @Override
    public void stop() {
        if (wyvern.isSleepLocked() && !wyvern.isSleepTransitioning()) {
            wyvern.startSleepExit();
        }
        phase = SleepPhase.IDLE;
        phaseTimer = 0;
        retryCooldownTicks = 60; // short buffer before re-evaluating
    }

    @Override
    public boolean isInterruptable() {
        // Allow interruption when dragon has a target (attacked) or is aggressive
        // This ensures combat goals can activate immediately without waiting for sleep goal to clean up
        return wyvern.getTarget() != null || wyvern.isAggressive();
    }

    private boolean canSleepNow() {
        // Raevyx are daylight sleepers; tamed wyverns also sleep when owner is asleep (night)
        boolean isDay = wyvern.level().isDay();
        boolean ownerSleeping = wyvern.isTame() && ownerAsleepNearby();
        if (wyvern.level().isThundering()) {
            return false;
        }
        return isDay || ownerSleeping;
    }

    private boolean ownerAsleepNearby() {
        LivingEntity owner = wyvern.getOwner();
        if (!(owner instanceof Player player)) {
            return false;
        }
        if (!player.isSleeping() || !player.isAlive()) {
            return false;
        }
        return player.level() == wyvern.level() && wyvern.distanceToSqr(player) <= 144.0; // 12 blocks
    }

    private boolean isEnvironmentCalm() {
        return wyvern.getTarget() == null
                && !wyvern.isAggressive()
                && !wyvern.isInWaterOrBubble()
                && !wyvern.isInLava()
                && wyvern.canSleepNow()
                && !wyvern.isSleepSuppressed();
    }

    private void freezeMotion(boolean keepSitting) {
        wyvern.getNavigation().stop();
        wyvern.setDeltaMovement(0, wyvern.getDeltaMovement().y, 0);
        wyvern.setRunning(false);
        wyvern.setGroundMoveStateFromAI(0);
        if (keepSitting) {
            wyvern.setOrderedToSit(true);
        }
        wyvern.setFlying(false);
        wyvern.setHovering(false);
        wyvern.setTakeoff(false);
        wyvern.setLanding(false);
    }

    private enum SleepPhase {
        IDLE, ENTERING, SLEEPING, EXITING
    }
}
