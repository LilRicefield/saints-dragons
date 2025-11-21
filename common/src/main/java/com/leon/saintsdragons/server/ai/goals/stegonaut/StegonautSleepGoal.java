package com.leon.saintsdragons.server.ai.goals.stegonaut;

import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Nighttime sleep goal for Stegonaut mirroring Raevyx sleep flow.
 * Uses the same enter/sleep/exit phases but sleeps at night instead of during the day.
 */
public class StegonautSleepGoal extends Goal {

    private final Stegonaut drake;
    private int retryCooldownTicks;
    private SleepPhase phase = SleepPhase.IDLE;
    private int phaseTimer;

    public StegonautSleepGoal(Stegonaut drake) {
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
            return canSleepTonight();
        }

        if (!canSleepTonight()) return false;
        if (!drake.canSleepNow() || drake.isSleepSuppressed()) return false;
        if (drake.isInWaterOrBubble() || drake.isInLava()) return false;
        if (drake.isDying() || drake.isVehicle()) return false;
        if (drake.getTarget() != null || drake.isAggressive()) return false;

        // Wild dragons sleep through the night. Tamed dragons sleep when their owner is asleep and nearby.
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

        boolean shouldStayAsleep = canSleepTonight() && isEnvironmentCalm();
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
        phaseTimer = drake.getSleepSitDownDuration() + drake.getSleepFallAsleepDuration() + 4; // small buffer
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
        boolean nighttimeSleeper = canSleepTonight();

        if (phase == SleepPhase.ENTERING) {
            // Promote to sleeping once the entity reports it (after fall_asleep finishes)
            if (drake.isSleeping()) {
                phase = SleepPhase.SLEEPING;
                phaseTimer = 0; // unmanaged until wake condition
            }
            return;
        }

        if (phase == SleepPhase.SLEEPING) {
            boolean shouldWake = !(calm && ownerOk && nighttimeSleeper);
            if (shouldWake && !drake.isSleepTransitioning()) {
                drake.startSleepExit();
                phase = SleepPhase.EXITING;
                phaseTimer = drake.getSleepWakeUpDuration() + drake.getSleepSitUpDuration() + 8;
            }
            return;
        }

        if (phase == SleepPhase.EXITING) {
            // Keep seated until stand-up begins; allow sit_up via orderedToSit(false) when timer elapses
            if (phaseTimer <= 0 && !drake.isSleepTransitioning()) {
                drake.setOrderedToSit(false);
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
        return false; // keep the full transition chain intact
    }

    private boolean canSleepTonight() {
        // Mirror Raevyx logic but for nighttime sleepers; thunderstorms keep them wary
        return !drake.level().isDay() && !drake.level().isThundering();
    }

    private boolean ownerAsleepNearby() {
        LivingEntity owner = drake.getOwner();
        if (!(owner instanceof Player player)) {
            return false;
        }
        if (!player.isSleeping() || !player.isAlive()) {
            return false;
        }
        return player.level() == drake.level() && drake.distanceToSqr(player) <= 144.0;
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
        if (keepSitting) {
            drake.setOrderedToSit(true);
        }
    }

    private enum SleepPhase {
        IDLE, ENTERING, SLEEPING, EXITING
    }
}
