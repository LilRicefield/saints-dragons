package com.leon.saintsdragons.server.ai.goals.nulljaw;

import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Sleep goal for Nulljaw.
 * Sleeps at night; tamed dragons also sleep when their owner is asleep.
 * Keeps the full enter/exit animation chain but without intermediate "rest" pauses.
 */
public class NulljawSleepGoal extends Goal {

    private final Nulljaw drake;
    private int retryCooldownTicks;
    private SleepPhase phase = SleepPhase.IDLE;
    private int phaseTimer;

    public NulljawSleepGoal(Nulljaw drake) {
        this.drake = drake;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (retryCooldownTicks > 0) {
            retryCooldownTicks--;
            return false;
        }

        // Already sleeping or mid transition? allow the goal to continue if nighttime
        if (drake.isSleepLocked() || drake.isSleeping() || drake.isSleepTransitioning()) {
            return canSleepNow();
        }

        if (!canSleepNow()) return false;
        if (!drake.canSleepNow() || drake.isSleepSuppressed()) return false;
        if (drake.isInWaterOrBubble() || drake.isInLava()) return false;
        if (drake.isDying() || drake.isVehicle()) return false;
        if (drake.getTarget() != null || drake.isAggressive()) return false;

        // Wild dragons sleep at night. Tamed dragons sleep when their owner is asleep and nearby.
        return !drake.isTame() || ownerAsleepNearby();
    }

    @Override
    public boolean canContinueToUse() {
        if (phase == SleepPhase.IDLE) {
            return false;
        }

        // Allow exit/cleanup to run even if wake conditions are met
        if (phase == SleepPhase.EXITING) {
            return drake.isSleepLocked() || drake.isSleepTransitioning() || phaseTimer > 0;
        }

        boolean shouldStayAsleep = canSleepNow() && isEnvironmentCalm();
        if (drake.isTame()) {
            shouldStayAsleep = shouldStayAsleep && ownerAsleepNearby();
        }
        // While entering, keep going to finish the chain; while sleeping, stay if calm
        return (phase == SleepPhase.ENTERING && (drake.isSleepLocked() || drake.isSleepTransitioning()))
                || (phase == SleepPhase.SLEEPING && shouldStayAsleep && drake.isSleepLocked());
    }

    @Override
    public void start() {
        phase = SleepPhase.ENTERING;
        boolean alreadySitting = drake.isOrderedToSit() || drake.getSitProgress() >= drake.maxSitTicks();
        // If already seated (owner command), skip sit_down buffer
        phaseTimer = (alreadySitting ? 0 : drake.getSitDownAnimationTicks())
                + drake.getFallAsleepAnimationTicks() + 4; // small buffer
        drake.startSleepEnter();
    }

    @Override
    public void tick() {
        if (drake.level().isClientSide) return;

        boolean keepSitting = !(phase == SleepPhase.EXITING && phaseTimer <= 0 && !drake.isSleepTransitioning());
        freezeMotion(keepSitting);
        if (phaseTimer > 0) {
            phaseTimer--;
        }

        boolean calm = isEnvironmentCalm();
        boolean ownerOk = !drake.isTame() || ownerAsleepNearby();
        boolean sleepWindow = canSleepNow();

        // Any threat/aggro forces immediate wake sequencing
        if (!calm && phase != SleepPhase.IDLE && phase != SleepPhase.EXITING) {
            drake.startSleepExit();
            phase = SleepPhase.EXITING;
            boolean ownerWantsSit = drake.isTame() && drake.getCommand() == 1;
            phaseTimer = drake.getWakeUpAnimationTicks()
                    + (ownerWantsSit ? 0 : drake.getSitUpAnimationTicks()) + 8;
        }

        if (phase == SleepPhase.ENTERING) {
            // Promote to sleeping once the entity reports it (after fall_asleep finishes)
            if (drake.isSleeping()) {
                phase = SleepPhase.SLEEPING;
                phaseTimer = 0; // unmanaged until wake condition
            }
            return;
        }

        if (phase == SleepPhase.SLEEPING) {
            boolean shouldWake = !(calm && ownerOk && sleepWindow);
            if (shouldWake && !drake.isSleepTransitioning()) {
                drake.startSleepExit();
                phase = SleepPhase.EXITING;
                // If owner commanded sit, we stop at sit after wake_up (no stand)
                boolean ownerWantsSit = drake.isTame() && drake.getCommand() == 1;
                phaseTimer = drake.getWakeUpAnimationTicks()
                        + (ownerWantsSit ? 0 : drake.getSitUpAnimationTicks()) + 8;
            }
            return;
        }

        if (phase == SleepPhase.EXITING) {
            // Keep seated until stand-up begins; allow sit_up via orderedToSit(false) when timer elapses
            if (phaseTimer <= 0 && !drake.isSleepTransitioning()) {
                boolean ownerWantsSit = drake.isTame() && drake.getCommand() == 1;
                if (!ownerWantsSit) {
                    drake.setOrderedToSit(false);
                } else {
                    drake.setOrderedToSit(true);
                    drake.setGroundMoveStateFromAI(0);
                }
                phase = SleepPhase.IDLE;
            }
        }
    }

    @Override
    public void stop() {
        if (drake.isSleepLocked() && !drake.isSleepTransitioning()) {
            drake.startSleepExit();
        }
        phase = SleepPhase.IDLE;
        phaseTimer = 0;
        retryCooldownTicks = 60; // short buffer before re-evaluating
    }

    @Override
    public boolean isInterruptable() {
        // Allow interruption when dragon has a target (attacked) or is aggressive
        // This ensures combat goals can activate immediately without waiting for sleep goal to clean up
        return drake.getTarget() != null || drake.isAggressive();
    }

    private boolean canSleepNow() {
        // Nulljaw sleeps at night; tamed dragons also sleep when owner is asleep
        boolean isNight = !drake.level().isDay();
        boolean ownerSleeping = drake.isTame() && ownerAsleepNearby();
        if (drake.level().isThundering()) {
            return false;
        }
        return isNight || ownerSleeping;
    }

    private boolean ownerAsleepNearby() {
        LivingEntity owner = drake.getOwner();
        if (!(owner instanceof Player player)) {
            return false;
        }
        if (!player.isSleeping() || !player.isAlive()) {
            return false;
        }
        return player.level() == drake.level() && drake.distanceToSqr(player) <= 144.0; // 12 blocks
    }

    private boolean isEnvironmentCalm() {
        return drake.getTarget() == null
                && !drake.isAggressive()
                && !drake.isInWaterOrBubble()
                && !drake.isInLava()
                && drake.canSleepNow()
                && !drake.isSleepSuppressed();
    }

    private void freezeMotion(boolean keepSitting) {
        drake.getNavigation().stop();
        drake.setDeltaMovement(0, drake.getDeltaMovement().y, 0);
        drake.setRunning(false);
        drake.setGroundMoveStateFromAI(0);
        if (keepSitting) {
            drake.setOrderedToSit(true);
        }
    }

    private enum SleepPhase {
        IDLE, ENTERING, SLEEPING, EXITING
    }
}
