package com.leon.saintsdragons.server.ai.goals.cindervane;

import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Unified sleep goal for Cindervanes (tamed + wild).
 * Mirrors Stegonaut's enter/sleep/exit phases but uses night-time sleep windows.
 * Wild dragons have a small random chance to bed down when calm.
 */
public class CindervaneSleepGoal extends Goal {

    private final Cindervane amphithere;
    private int retryCooldownTicks;
    private SleepPhase phase = SleepPhase.IDLE;
    private int phaseTimer;

    public CindervaneSleepGoal(Cindervane amphithere) {
        this.amphithere = amphithere;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (retryCooldownTicks > 0) {
            retryCooldownTicks--;
            return false;
        }

        // Already sleeping or mid transition? allow the goal to continue if nighttime
        if (amphithere.isSleepLocked() || amphithere.isSleeping() || amphithere.isSleepTransitioning()) {
            return canSleepTonight();
        }

        if (!canSleepTonight()) return false;
        if (!amphithere.canSleepNow() || amphithere.isSleepSuppressed()) return false;
        if (amphithere.isInWaterOrBubble() || amphithere.isInLava()) return false;
        if (amphithere.isFlying() || amphithere.isDying() || amphithere.isVehicle()) return false;
        if (amphithere.getTarget() != null || amphithere.isAggressive()) return false;

        if (amphithere.isTame()) {
            return ownerAsleepNearby();
        }

        // Wild dragons: sleep whenever it's calm during the night
        return isEnvironmentCalm();
    }

    @Override
    public boolean canContinueToUse() {
        if (phase == SleepPhase.IDLE) {
            return false;
        }

        // Allow exit/cleanup to run even if wake conditions are met
        if (phase == SleepPhase.EXITING) {
            return amphithere.isSleepLocked() || amphithere.isSleepTransitioning() || phaseTimer > 0;
        }

        boolean shouldStayAsleep = canSleepTonight() && isEnvironmentCalm();
        if (amphithere.isTame()) {
            shouldStayAsleep = shouldStayAsleep && ownerAsleepNearby();
        }
        // While entering, keep going to finish the chain; while sleeping, stay if calm
        return (phase == SleepPhase.ENTERING && (amphithere.isSleepLocked() || amphithere.isSleepTransitioning()))
                || (phase == SleepPhase.SLEEPING && shouldStayAsleep && amphithere.isSleepLocked());
    }

    @Override
    public void start() {
        phase = SleepPhase.ENTERING;
        boolean alreadySitting = amphithere.isOrderedToSit() || amphithere.getSitProgress() >= amphithere.maxSitTicks();
        // If already seated (owner command), skip sit_down buffer
        phaseTimer = (alreadySitting ? 0 : amphithere.getSleepSitDownDuration())
                + amphithere.getSleepFallAsleepDuration() + 4; // small buffer
        amphithere.setOrderedToSit(true); // kick off sit_down immediately
        amphithere.startSleepEnter();
    }

    @Override
    public void tick() {
        if (amphithere.level().isClientSide) return;

        boolean keepSitting = !(phase == SleepPhase.EXITING && phaseTimer <= 0 && !amphithere.isSleepTransitioning());
        freezeMotion(keepSitting);
        if (phaseTimer > 0) {
            phaseTimer--;
        }

        boolean calm = isEnvironmentCalm();
        boolean ownerOk = !amphithere.isTame() || ownerAsleepNearby();
        boolean nighttimeSleeper = canSleepTonight();

        // Threats force an exit even mid-enter
        if (!calm && phase != SleepPhase.IDLE && phase != SleepPhase.EXITING && !amphithere.isSleepTransitioning()) {
            amphithere.startSleepExit();
            phase = SleepPhase.EXITING;
            phaseTimer = amphithere.getSleepWakeUpDuration() + amphithere.getSleepSitUpDuration() + 8;
            return;
        }

        if (phase == SleepPhase.ENTERING) {
            // Promote to sleeping once the entity reports it (after fall_asleep finishes)
            if (amphithere.isSleeping()) {
                phase = SleepPhase.SLEEPING;
                phaseTimer = 0; // unmanaged until wake condition
            }
            return;
        }

        if (phase == SleepPhase.SLEEPING) {
            boolean shouldWake = !(calm && ownerOk && nighttimeSleeper);
            if (shouldWake && !amphithere.isSleepTransitioning()) {
                amphithere.startSleepExit();
                phase = SleepPhase.EXITING;
                phaseTimer = amphithere.getSleepWakeUpDuration() + amphithere.getSleepSitUpDuration() + 8;
            }
            return;
        }

        if (phase == SleepPhase.EXITING) {
            // Keep seated until stand-up begins; allow sit_up via orderedToSit(false) when timer elapses
            if (phaseTimer <= 0 && !amphithere.isSleepTransitioning()) {
                amphithere.setOrderedToSit(false);
                phase = SleepPhase.IDLE;
            }
        }
    }

    @Override
    public void stop() {
        if (amphithere.isSleepLocked() && !amphithere.isSleepTransitioning()) {
            amphithere.startSleepExit();
        }
        phase = SleepPhase.IDLE;
        phaseTimer = 0;
        retryCooldownTicks = 60; // short buffer before re-evaluating
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    private boolean canSleepTonight() {
        return !amphithere.level().isDay() && !amphithere.level().isThundering();
    }

    private boolean ownerAsleepNearby() {
        LivingEntity owner = amphithere.getOwner();
        if (!(owner instanceof Player player)) {
            return false;
        }
        if (!player.isSleeping() || !player.isAlive()) {
            return false;
        }
        if (player.level() != amphithere.level()) {
            return false;
        }
        return amphithere.distanceToSqr(player) <= 144.0;
    }

    private boolean isEnvironmentCalm() {
        return amphithere.getTarget() == null
                && !amphithere.isAggressive()
                && !amphithere.isInWaterOrBubble()
                && !amphithere.isInLava()
                && !amphithere.isFlying()
                && amphithere.canSleepNow()
                && !amphithere.isSleepSuppressed();
    }

    private void freezeMotion(boolean keepSitting) {
        amphithere.getNavigation().stop();
        amphithere.setDeltaMovement(0, amphithere.getDeltaMovement().y, 0);
        if (keepSitting) {
            amphithere.setOrderedToSit(true);
        }
    }

    private enum SleepPhase {
        IDLE, ENTERING, SLEEPING, EXITING
    }
}
