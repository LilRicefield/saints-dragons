package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.ai.goals.base.DragonSleepBehavior;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;

public final class DragonSleepComponent {
    private final DragonEntity dragon;
    private final EntityDataAccessor<Boolean> dataSleeping;
    private final EntityDataAccessor<Boolean> dataSleepingEntering;
    private final EntityDataAccessor<Boolean> dataSleepingExiting;
    private final DragonSleepBehavior behavior;

    private boolean sleeping = false;
    private boolean sleepingEntering = false;
    private boolean sleepingExiting = false;
    private boolean sleepTransitioning = false;
    private boolean sleepFallAsleepTriggered = false;
    private boolean sleepSitUpTriggered = false;
    private boolean sleepLoopTriggered = false;
    private boolean sleepLocked = false;
    private int sleepCommandSnapshot = -1;
    private int sleepTransitionTicks = 0;
    private int sleepAmbientCooldownTicks = 0;
    private int sleepReentryCooldownTicks = 0;
    private int sleepCancelTicks = 0;

    public DragonSleepComponent(DragonEntity dragon,
                                EntityDataAccessor<Boolean> dataSleeping,
                                EntityDataAccessor<Boolean> dataSleepingEntering,
                                EntityDataAccessor<Boolean> dataSleepingExiting) {
        this.dragon = dragon;
        this.dataSleeping = dataSleeping;
        this.dataSleepingEntering = dataSleepingEntering;
        this.dataSleepingExiting = dataSleepingExiting;
        this.behavior = new DragonSleepBehavior(dragon);
    }

    public void tick() {
        behavior.tick();
        tickSleepTransitions();
        tickSleepCooldowns();
    }

    public DragonSleepBehavior getBehavior() {
        return behavior;
    }

    public boolean isSleeping() {
        return dragon.level() != null && dragon.level().isClientSide
                ? dragon.getEntityData().get(dataSleeping)
                : sleeping;
    }

    public boolean isSleepingEntering() {
        return dragon.level() != null && dragon.level().isClientSide
                ? dragon.getEntityData().get(dataSleepingEntering)
                : sleepingEntering;
    }

    public boolean isSleepingExiting() {
        return dragon.level() != null && dragon.level().isClientSide
                ? dragon.getEntityData().get(dataSleepingExiting)
                : sleepingExiting;
    }

    public boolean isSleepTransitioning() {
        if (dragon.level() != null && dragon.level().isClientSide) {
            return isSleepingEntering() || isSleepingExiting();
        }
        return sleepTransitioning || sleepingEntering || sleepingExiting;
    }

    public boolean isSleepLocked() {
        return sleepLocked || sleeping || sleepingEntering || sleepingExiting || sleepTransitioning;
    }

    public int getAmbientCooldownTicks() {
        return sleepAmbientCooldownTicks;
    }

    public void bumpAmbientCooldown(int ticks) {
        sleepAmbientCooldownTicks = Math.max(sleepAmbientCooldownTicks, ticks);
    }

    public void clearCooldowns() {
        sleepAmbientCooldownTicks = 0;
        sleepReentryCooldownTicks = 0;
        sleepCancelTicks = 0;
    }

    public void startSleepEnter() {
        if (sleeping || sleepingEntering || sleepingExiting || sleepTransitioning) {
            return;
        }
        sleepTransitioning = true;
        sleepingEntering = true;
        sleepingExiting = false;
        sleeping = false;
        sleepFallAsleepTriggered = false;
        sleepSitUpTriggered = false;
        sleepLoopTriggered = false;
        sleepLocked = true;
        sleepCommandSnapshot = dragon.getCommand();

        dragon.getEntityData().set(dataSleepingEntering, true);
        dragon.getEntityData().set(dataSleepingExiting, false);
        dragon.getEntityData().set(dataSleeping, false);

        boolean alreadySeated = dragon.sleepIsAlreadySeatedForSleep();
        boolean forceSitDown = dragon.sleepShouldForceSitDownOnEnter();
        dragon.sleepOnLockCommand(sleepCommandSnapshot);
        dragon.sleepOnFreezeTick();
        if (alreadySeated && !forceSitDown) {
            sleepTransitionTicks = dragon.sleepGetFallAsleepDuration();
            sleepFallAsleepTriggered = true;
            dragon.sleepOnFallAsleepAnimation();
        } else {
            if (dragon.sleepUseSitDownTimer()) {
                sleepTransitionTicks = dragon.sleepGetSitDownDuration();
            } else {
                sleepTransitionTicks = dragon.sleepGetFallAsleepDuration();
            }
            dragon.sleepOnSitDownAnimation();
        }
    }

    public void startSleepExit() {
        if ((!sleeping && !sleepingEntering) || sleepingExiting) {
            return;
        }
        sleeping = false;
        sleepingEntering = false;
        sleepingExiting = true;
        sleepTransitioning = true;
        sleepSitUpTriggered = false;
        sleepLoopTriggered = false;
        sleepTransitionTicks = dragon.sleepGetWakeUpDuration();

        dragon.getEntityData().set(dataSleeping, false);
        dragon.getEntityData().set(dataSleepingEntering, false);
        dragon.getEntityData().set(dataSleepingExiting, true);

        dragon.sleepOnWakeUpAnimation();
        dragon.sleepOnExitStarted();
        suppressSleep(dragon.sleepGetExitSuppressionTicks());
    }

    public void wakeUpImmediately() {
        suppressSleep(dragon.sleepGetWakeUpSuppressionTicks());
        sleepTransitionTicks = 0;
        sleepTransitioning = false;
        sleepFallAsleepTriggered = false;
        sleepSitUpTriggered = false;
        sleepLoopTriggered = false;
        sleeping = false;
        sleepingEntering = false;
        sleepingExiting = false;
        sleepLocked = false;
        sleepCommandSnapshot = -1;
        bumpAmbientCooldown(10);

        dragon.getEntityData().set(dataSleeping, false);
        dragon.getEntityData().set(dataSleepingEntering, false);
        dragon.getEntityData().set(dataSleepingExiting, false);
        dragon.sleepOnWakeUpImmediate();
        releaseSleepLock();
    }

    public void suppressSleep(int ticks) {
        sleepReentryCooldownTicks = Math.max(sleepReentryCooldownTicks, ticks);
    }

    public boolean isSleepSuppressed() {
        return sleepReentryCooldownTicks > 0;
    }

    public void saveToNBT(CompoundTag tag) {
        tag.putInt("SleepCancelTicks", sleepCancelTicks);
        if (sleepCommandSnapshot >= 0) {
            tag.putInt("SleepCommandSnapshot", sleepCommandSnapshot);
        }
    }

    public void loadFromNBT(CompoundTag tag) {
        sleepCancelTicks = tag.contains("SleepCancelTicks") ? tag.getInt("SleepCancelTicks") : 0;
        sleepCommandSnapshot = tag.contains("SleepCommandSnapshot") ? tag.getInt("SleepCommandSnapshot") : -1;

        sleepTransitionTicks = 0;
        sleepTransitioning = false;
        sleepFallAsleepTriggered = false;
        sleepSitUpTriggered = false;
        sleepLoopTriggered = false;
        sleeping = false;
        sleepingEntering = false;
        sleepingExiting = false;
        sleepLocked = false;

        dragon.getEntityData().set(dataSleeping, false);
        dragon.getEntityData().set(dataSleepingEntering, false);
        dragon.getEntityData().set(dataSleepingExiting, false);
    }

    private void tickSleepTransitions() {
        if (!(sleeping || sleepingEntering || sleepingExiting || sleepTransitioning)) {
            return;
        }

        dragon.sleepOnFreezeTick();

        if (sleepingEntering) {
            if (!sleepFallAsleepTriggered) {
                if (dragon.sleepUseSitDownTimer()) {
                    if (sleepTransitionTicks > 0) {
                        sleepTransitionTicks--;
                        if (sleepTransitionTicks > 0) {
                            return;
                        }
                    }
                    if (dragon.sleepRequireSeatedBeforeFallAsleep() && !dragon.sleepIsAlreadySeatedForSleep()) {
                        return;
                    }
                    sleepFallAsleepTriggered = true;
                    sleepTransitionTicks = dragon.sleepGetFallAsleepDuration();
                    dragon.sleepOnFallAsleepAnimation();
                    return;
                }

                if (!dragon.sleepIsAlreadySeatedForSleep()) {
                    sleepTransitionTicks = dragon.sleepGetFallAsleepDuration();
                    return;
                }

                sleepFallAsleepTriggered = true;
                sleepTransitionTicks = dragon.sleepGetFallAsleepDuration();
                dragon.sleepOnFallAsleepAnimation();
            }

            if (sleepTransitionTicks > 0) {
                sleepTransitionTicks--;
                int leadTicks = dragon.sleepGetLoopLeadTicks();
                if (leadTicks > 0 && !sleepLoopTriggered && sleepTransitionTicks == leadTicks) {
                    sleepLoopTriggered = true;
                    dragon.sleepOnLoopAnimation();
                }
                if (sleepTransitionTicks > 0) {
                    return;
                }
            }

            sleeping = true;
            sleepingEntering = false;
            sleepTransitioning = false;
            sleepFallAsleepTriggered = false;
            dragon.getEntityData().set(dataSleepingEntering, false);
            dragon.getEntityData().set(dataSleeping, true);
            if (!sleepLoopTriggered) {
                dragon.sleepOnLoopAnimation();
                sleepLoopTriggered = true;
            }
            dragon.sleepOnEntered();
            return;
        }

        if (sleepingExiting) {
            if (sleepTransitionTicks > 0) {
                sleepTransitionTicks--;
                if (sleepTransitionTicks > 0) {
                    return;
                }
            }

            if (dragon.sleepUseSitUpAfterWake()) {
                if (!sleepSitUpTriggered) {
                    if (dragon.sleepShouldStaySeatedAfterWake()) {
                        sleeping = false;
                        sleepSitUpTriggered = false;
                        sleepingExiting = false;
                        sleepTransitioning = false;
                        sleepTransitionTicks = 0;
                        bumpAmbientCooldown(10);
                        dragon.getEntityData().set(dataSleepingExiting, false);
                        dragon.getEntityData().set(dataSleeping, false);
                        dragon.sleepOnExitSeated();
                        releaseSleepLock();
                        return;
                    }

                    sleepSitUpTriggered = true;
                    sleepTransitionTicks = dragon.sleepGetSitUpDuration();
                    dragon.sleepOnSitUpAnimation();
                    return;
                }
            }

            sleeping = false;
            sleepingExiting = false;
            sleepTransitioning = false;
            sleepSitUpTriggered = false;
            dragon.getEntityData().set(dataSleepingExiting, false);
            dragon.getEntityData().set(dataSleeping, false);
            bumpAmbientCooldown(10);
            releaseSleepLock();
        }
    }

    private void tickSleepCooldowns() {
        if (sleepAmbientCooldownTicks > 0) sleepAmbientCooldownTicks--;
        if (sleepReentryCooldownTicks > 0) sleepReentryCooldownTicks--;
        if (sleepCancelTicks > 0) sleepCancelTicks--;
    }

    private void releaseSleepLock() {
        if (sleepLocked) {
            int desired = sleepCommandSnapshot;
            sleepCommandSnapshot = -1;
            sleepLocked = false;
            dragon.sleepOnUnlockCommand(desired);
        }
    }
}
