package com.leon.saintsdragons.server.ai.goals.ignivorus;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;


public class IgnivorusSleepGoal extends Goal {

    private final Ignivorus dragon;
    private int retryCooldownTicks;
    private SleepPhase phase = SleepPhase.IDLE;
    private int phaseTimer;

    public IgnivorusSleepGoal(Ignivorus dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (retryCooldownTicks > 0) {
            retryCooldownTicks--;
            return false;
        }

        // Already sleeping or mid transition? allow the goal to continue if sleep conditions met
        if (dragon.isSleepLocked() || dragon.isSleeping() || dragon.isSleepTransitioning()) {
            return canSleepNow();
        }

        if (!canSleepNow()) return false;
        if (!dragon.canSleepNow() || dragon.isSleepSuppressed()) return false;
        if (dragon.isInWaterOrBubble() || dragon.isInLava()) return false;
        if (dragon.isDying() || dragon.isVehicle()) return false;
        if (!isEffectivelyGrounded()) return false;
        if (dragon.getTarget() != null || dragon.isAggressive()) return false;

        // Wild dragons sleep at night or during thunder. Tamed dragons sleep when their owner is asleep and nearby.
        return !dragon.isTame() || ownerAsleepNearby();
    }

    @Override
    public boolean canContinueToUse() {
        if (phase == SleepPhase.IDLE) {
            return false;
        }

        // Allow exit/cleanup to run even if wake conditions are met
        if (phase == SleepPhase.EXITING) {
            return dragon.isSleepLocked() || dragon.isSleepTransitioning() || phaseTimer > 0;
        }

        boolean shouldStayAsleep = canSleepNow() && isEnvironmentCalm();
        if (dragon.isTame()) {
            shouldStayAsleep = shouldStayAsleep && ownerAsleepNearby();
        }
        // While entering, keep going to finish the chain; while sleeping, stay if calm
        return (phase == SleepPhase.ENTERING && (dragon.isSleepLocked() || dragon.isSleepTransitioning()))
                || (phase == SleepPhase.SLEEPING && shouldStayAsleep && dragon.isSleepLocked());
    }

    @Override
    public void start() {
        phase = SleepPhase.ENTERING;
        boolean alreadySitting = dragon.isOrderedToSit() || dragon.getSitProgress() >= dragon.maxSitTicks();
        // If already seated (owner command), skip sit_down buffer
        phaseTimer = (alreadySitting ? 0 : dragon.getSleepSitDownDuration())
                + dragon.getSleepFallAsleepDuration() + 4; // small buffer
        dragon.startSleepEnter();
    }

    @Override
    public void tick() {
        if (dragon.level().isClientSide) return;

        boolean keepSitting = !(phase == SleepPhase.EXITING && phaseTimer <= 0 && !dragon.isSleepTransitioning());
        freezeMotion(keepSitting);
        if (phaseTimer > 0) {
            phaseTimer--;
        }

        boolean calm = isEnvironmentCalm();
        boolean ownerOk = !dragon.isTame() || ownerAsleepNearby();
        boolean sleepWindow = canSleepNow();

        // Abort sleep if airborne; wake immediately and wait to retry after landing
        if (!isEffectivelyGrounded() && phase != SleepPhase.IDLE) {
            phase = SleepPhase.IDLE;
            phaseTimer = 0;
            retryCooldownTicks = 40;
            dragon.wakeUpImmediately();
            return;
        }

        // Any threat/aggro forces immediate wake sequencing
        if (!calm && phase != SleepPhase.IDLE && phase != SleepPhase.EXITING) {
            dragon.startSleepExit();
            phase = SleepPhase.EXITING;
            boolean ownerWantsSit = dragon.isTame() && dragon.getCommand() == 1;
            phaseTimer = dragon.getSleepWakeUpDuration()
                    + (ownerWantsSit ? 0 : dragon.getSleepSitUpDuration()) + 8;
        }

        if (phase == SleepPhase.ENTERING) {
            // Promote to sleeping once the entity reports it (after fall_asleep finishes)
            if (dragon.isSleeping()) {
                phase = SleepPhase.SLEEPING;
                phaseTimer = 0; // unmanaged until wake condition
            }
            return;
        }

        if (phase == SleepPhase.SLEEPING) {
            boolean shouldWake = !(calm && ownerOk && sleepWindow);
            if (shouldWake && !dragon.isSleepTransitioning()) {
                dragon.startSleepExit();
                phase = SleepPhase.EXITING;
                // If owner commanded sit, we stop at sit after wake_up (no stand)
                boolean ownerWantsSit = dragon.isTame() && dragon.getCommand() == 1;
                phaseTimer = dragon.getSleepWakeUpDuration()
                        + (ownerWantsSit ? 0 : dragon.getSleepSitUpDuration()) + 8;
            }
            return;
        }

        if (phase == SleepPhase.EXITING) {
            // Keep seated until stand-up begins; allow sit_up via orderedToSit(false) when timer elapses
            if (phaseTimer <= 0 && !dragon.isSleepTransitioning()) {
                boolean ownerWantsSit = dragon.isTame() && dragon.getCommand() == 1;
                if (!ownerWantsSit) {
                    dragon.setOrderedToSit(false);
                } else {
                    dragon.setOrderedToSit(true);
                    dragon.setGroundMoveStateFromAI(0);
                }
                phase = SleepPhase.IDLE;
            }
        }
    }

    @Override
    public void stop() {
        if (phase != SleepPhase.IDLE
                && (dragon.isSleeping() || dragon.isSleepTransitioning() || dragon.isSleepingEntering() || dragon.isSleepingExiting())) {
            dragon.startSleepExit();
        }
        phase = SleepPhase.IDLE;
        phaseTimer = 0;
        retryCooldownTicks = 60; // short buffer before re-evaluating
    }

    @Override
    public boolean isInterruptable() {
        // Allow interruption when dragon has a target (attacked) or is aggressive
        // This ensures combat goals can activate immediately without waiting for sleep goal to clean up
        return dragon.getTarget() != null || dragon.isAggressive();
    }

    private boolean canSleepNow() {
        // Ignivorus sleeps at night OR during thunderstorms; active in morning
        boolean isNight = !dragon.level().isDay();
        boolean isThundering = dragon.level().isThundering();
        boolean ownerSleeping = dragon.isTame() && ownerAsleepNearby();

        return isNight || isThundering || ownerSleeping;
    }

    private boolean ownerAsleepNearby() {
        LivingEntity owner = dragon.getOwner();
        if (!(owner instanceof Player player)) {
            return false;
        }
        if (!player.isSleeping() || !player.isAlive()) {
            return false;
        }
        return player.level() == dragon.level() && dragon.distanceToSqr(player) <= 144.0; // 12 blocks
    }

    private boolean isEnvironmentCalm() {
        return dragon.getTarget() == null
                && !dragon.isAggressive()
                && !dragon.isInWaterOrBubble()
                && !dragon.isInLava()
                && dragon.canSleepNow()
                && !dragon.isSleepSuppressed();
    }

    private void freezeMotion(boolean keepSitting) {
        dragon.getNavigation().stop();
        dragon.setDeltaMovement(0, dragon.getDeltaMovement().y, 0);
        dragon.setRunning(false);
        dragon.setGroundMoveStateFromAI(0);
        if (keepSitting) {
            dragon.setOrderedToSit(true);
        }
        dragon.setFlying(false);
        dragon.setHovering(false);
        dragon.setTakeoff(false);
        dragon.setLanding(false);
    }

    private enum SleepPhase {
        IDLE, ENTERING, SLEEPING, EXITING
    }

    private boolean isGrounded() {
        return dragon.onGround();
    }

    private boolean isEffectivelyGrounded() {
        if (dragon.onGround()) {
            return true;
        }
        if (dragon.isFlying() || dragon.isHovering() || dragon.isLanding()) {
            return false;
        }
        // Treat as grounded if standing over a solid block, even if the game hasn't set onGround yet
        return !dragon.level().getBlockState(dragon.blockPosition().below()).isAir();
    }
}
