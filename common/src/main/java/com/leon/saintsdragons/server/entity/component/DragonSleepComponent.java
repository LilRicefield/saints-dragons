package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class DragonSleepComponent {
    private static final int RECENT_COMBAT_SLEEP_BLOCK_TICKS = 20 * 30;
    private final DragonEntity dragon;
    private final EntityDataAccessor<Boolean> dataSleeping;
    private final EntityDataAccessor<Boolean> dataSleepingEntering;
    private final EntityDataAccessor<Boolean> dataSleepingExiting;
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
    private int sleepActionCooldown = 0;
    private SleepPhase currentPhase = SleepPhase.IDLE;

    public DragonSleepComponent(DragonEntity dragon,
                                EntityDataAccessor<Boolean> dataSleeping,
                                EntityDataAccessor<Boolean> dataSleepingEntering,
                                EntityDataAccessor<Boolean> dataSleepingExiting) {
        this.dragon = dragon;
        this.dataSleeping = dataSleeping;
        this.dataSleepingEntering = dataSleepingEntering;
        this.dataSleepingExiting = dataSleepingExiting;
        if (shouldSleepBasedOnConditions()) {
            delaySleep(60, 80);
        } else {
            delaySleep(100, 300);
        }
    }

    public void tick() {
        tickSleepDecisions();
        tickSleepTransitions();
        tickSleepCooldowns();
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
    }

    private void tickSleepDecisions() {
        if (dragon.level().isClientSide) {
            return;
        }
        if (!dragon.supportsSleep()) {
            return;
        }

        if (sleepActionCooldown > 0) {
            sleepActionCooldown--;
        }

        if (!dragon.isTame() && dragon.isOrderedToSit() && !dragon.isSleeping() && !dragon.isSleepTransitioning()) {
            dragon.setOrderedToSit(false);
            if (dragon.getCommand() == 1) {
                dragon.setCommand(0);
            }
            if (dragon.getSitProgress() > 0f || dragon.getPrevSitProgress() > 0f) {
                dragon.clearSitProgress();
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
        if (shouldFollowOwnerSleepNow()) {
            sleepActionCooldown = 0;
            sleepReentryCooldownTicks = 0;
            return;
        }

        if (dragon.isSleeping() || dragon.isSleepTransitioning()) {
            sleepActionCooldown = 0;
            tryWakeUp();
        }
    }

    private boolean shouldSleepBasedOnConditions() {
        if (!dragon.supportsSleep()) {
            return false;
        }

        if (dragon.isTame()) {
            return shouldFollowOwnerSleepNow();
        }

        DragonEntity.DragonSleepPreferences prefs = dragon.getSleepPreferences();
        if (!prefs.canSleepDuringConditions(dragon.level())) {
            return false;
        }

        return true;
    }

    private boolean shouldFollowOwnerSleepNow() {

        return dragon.isTame()
                && !DragonEntity.DragonSleepPreferences.isNaturalDay(dragon.level())
                && isOwnerSleeping();
    }

    private boolean shouldAttemptSleep() {
        if (!dragon.supportsSleep()) {
            return false;
        }
        if (dragon.isSleeping() || dragon.isSleepTransitioning()) {
            return false;
        }
        if (sleepActionCooldown > 0) {
            return false;
        }
        if (!dragon.canSleepNow()) {
            return false;
        }
        if (!canSleepInCurrentEnvironment()) {
            return false;
        }

        if (dragon.isTame()) {
            return shouldFollowOwnerSleepNow();
        }

        DragonEntity.DragonSleepPreferences prefs = dragon.getSleepPreferences();
        if (!prefs.canSleepDuringConditions(dragon.level())) {
            return false;
        }

        return true;
    }

    private boolean shouldWakeUp() {
        if (!dragon.supportsSleep()) return false;
        if (!dragon.isSleeping()) return false;
        if (!dragon.canSleepNow()) return true;
        if (!canSleepInCurrentEnvironment()) return true;

        if (dragon.isTame()) {
            return !shouldFollowOwnerSleepNow();
        }

        DragonEntity.DragonSleepPreferences prefs = dragon.getSleepPreferences();
        if (!prefs.canSleepDuringConditions(dragon.level())) return true;

        return false;
    }

    private boolean canSleepInCurrentEnvironment() {
        if (dragon.isDying() || !dragon.isAlive() || dragon.isDeadOrDying()) return false;
        if (dragon.isVehicle()) return false;
        if (dragon.getTarget() != null) return false;
        int recentCombatTick = Math.max(dragon.getLastDamagerTimestamp(), dragon.getLastHurtByMobTimestamp());
        if (recentCombatTick > 0 && dragon.tickCount - recentCombatTick < RECENT_COMBAT_SLEEP_BLOCK_TICKS) return false;
        if ((dragon.isInWaterOrBubble() && !dragon.canSleepInWater()) || dragon.isInLava()) return false;
        boolean alreadySleepingOrTransitioning = dragon.isSleeping() || dragon.isSleepTransitioning();
        boolean ownerBedSleep = shouldFollowOwnerSleepNow();
        boolean ownerSitCommand = dragon.isTame() && (dragon.isOrderedToSit() || dragon.getCommand() == 1);
        boolean waterSleeperAtRest = dragon.canSleepInWater() && dragon.isInWaterOrBubble();
        if (!alreadySleepingOrTransitioning) {
            if (!dragon.onGround() && !waterSleeperAtRest && !(ownerBedSleep && ownerSitCommand)) return false;
            if (dragon.isAerial()) {
                return false;
            }
        }

        if (dragon.isSleepSuppressed() && !shouldFollowOwnerSleepNow()) return false;
        return true;
    }

    public boolean tryStartSleeping() {
        if (sleepActionCooldown > 0) return false;
        if (!dragon.supportsSleep()) return false;
        if (!dragon.canSleepNow()) return false;
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

    public int getSleepCooldown() {
        return sleepActionCooldown;
    }

    private boolean isOwnerSleeping() {
        LivingEntity owner = dragon.getOwner();
        if (!(owner instanceof Player player)) return false;
        if (!player.isSleeping() || !player.isAlive()) return false;
        return player.level() == dragon.level();
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
        if (dragon.isTame() && (dragon.isOrderedToSit() || dragon.getCommand() == 1) && alreadySeated) {
            forceSitDown = false;
        }
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
        if (sleepCommandSnapshot >= 0) {
            tag.putInt("SleepCommandSnapshot", sleepCommandSnapshot);
        }
    }

    public void loadFromNBT(CompoundTag tag) {
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
    }

    private void releaseSleepLock() {
        if (sleepLocked) {
            int desired = sleepCommandSnapshot;
            sleepCommandSnapshot = -1;
            sleepLocked = false;
            dragon.sleepOnUnlockCommand(desired);
        }
    }

    private enum SleepPhase {
        IDLE,
        ENTERING,
        SLEEPING,
        EXITING
    }
}
