package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class DragonTamingStunComponent<T extends DragonEntity> {
    protected final T dragon;

    private int failureCounter = 0;
    private float recoveryTargetHealth = -1.0F;
    private int stunGraceTicks = 0;
    private boolean aiLocked = false;
    private boolean awaitingFeed = false;
    private int stunTimeoutTicks = 0;

    private static final int STUN_TIMEOUT = 20 * 30; // 30 seconds

    protected DragonTamingStunComponent(T dragon) {
        this.dragon = dragon;
    }

    public void tickServer() {
        tickRecovery();
    }

    public boolean isAwaitingFeed() {
        return awaitingFeed;
    }

    public boolean tryEnterHoldStateFromDamage(DamageSource source, float amount) {
        if (dragon.level().isClientSide || source == null || amount <= 0.0F || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (dragon.isTame() || dragon.isBaby() || isTamingStunned() || !canUseTamingStun()) {
            return false;
        }

        float threshold = Math.max(0.0F, Math.min(getTamingThreshold(), dragon.getMaxHealth()));
        if (threshold <= 0.0F || dragon.getHealth() <= threshold || dragon.getHealth() - amount > threshold) {
            return false;
        }

        dragon.setHealth(Math.max(1.0F, threshold));
        enterHoldState();
        return true;
    }

    public void enterStun() {
        if (!canUseTamingStun()) {
            return;
        }
        awaitingFeed = false;
        stunGraceTicks = Math.max(stunGraceTicks, 20);
        recoveryTargetHealth = -1.0F;
        stunTimeoutTicks = 0;
        forceWakeForStun();
        ensureStunState();
    }

    public void enterHoldState() {
        if (!canUseTamingStun()) {
            return;
        }
        awaitingFeed = true;
        stunGraceTicks = 0;
        recoveryTargetHealth = -1.0F;
        stunTimeoutTicks = STUN_TIMEOUT;
        forceWakeForStun();
        ensureStunState();

        if (!dragon.level().isClientSide) {
            dragon.playSound(net.minecraft.sounds.SoundEvents.ARROW_HIT_PLAYER, 1.5F, 0.8F);
        }
    }

    public void setRecoveryTarget(float targetHealth) {
        if (!canUseTamingStun()) {
            return;
        }
        awaitingFeed = false;
        recoveryTargetHealth = Math.max(0.0F, Math.min(targetHealth, dragon.getMaxHealth()));
        stunGraceTicks = Math.max(stunGraceTicks, 40);
        stunTimeoutTicks = 0;
        forceWakeForStun();
        ensureStunState();
    }

    public void clearRecovery() {
        recoveryTargetHealth = -1.0F;
        stunGraceTicks = 0;
        awaitingFeed = false;
        stunTimeoutTicks = 0;
        if (isTamingStunned()) {
            setTamingStunned(false);
        }
        if (aiLocked && !dragon.level().isClientSide) {
            aiLocked = false;
            dragon.setNoAi(false);
        }
    }

    public void incrementFailures() {
        failureCounter++;
    }

    public void resetFailures() {
        failureCounter = 0;
    }

    public int getFailureCounter() {
        return failureCounter;
    }

    public void save(CompoundTag tag) {
        tag.putInt("TamingFailures", Math.max(0, failureCounter));
        tag.putFloat("TamingTargetHealth", recoveryTargetHealth);
        tag.putInt("TamingStunGraceTicks", Math.max(0, stunGraceTicks));
        tag.putBoolean("TamingAiLocked", aiLocked);
        tag.putBoolean("TamingAwaitingFeed", awaitingFeed);
        tag.putBoolean("TamingStunned", isTamingStunned());
        tag.putInt("TamingStunTimeout", Math.max(0, stunTimeoutTicks));
    }

    public void load(CompoundTag tag) {
        failureCounter = Math.max(0, tag.getInt("TamingFailures"));
        recoveryTargetHealth = tag.contains("TamingTargetHealth") ? tag.getFloat("TamingTargetHealth") : -1.0F;
        stunGraceTicks = Math.max(0, tag.getInt("TamingStunGraceTicks"));
        aiLocked = tag.getBoolean("TamingAiLocked");
        awaitingFeed = tag.getBoolean("TamingAwaitingFeed");
        stunTimeoutTicks = Math.max(0, tag.getInt("TamingStunTimeout"));

        if (tag.getBoolean("TamingStunned") && canUseTamingStun()) {
            setTamingStunned(true);
            if (aiLocked && !dragon.level().isClientSide) {
                dragon.setNoAi(true);
            }
        } else {
            clearRecovery();
        }
    }

    /**
     * Extra per-tick grounding guard for callers that want guaranteed airborne-drop behavior.
     */
    public void enforceGroundingTick() {
        if (dragon.level().isClientSide || !isTamingStunned()) {
            return;
        }
        enforceGroundedStunPhysics();
    }

    private void tickRecovery() {
        Level level = dragon.level();
        if (level.isClientSide) {
            return;
        }

        if (!canUseTamingStun()) {
            if (isTamingStunned() || aiLocked || awaitingFeed) {
                clearRecovery();
            }
            return;
        }

        if (dragon.isBaby() && !dragon.isTame()) {
            if (isTamingStunned() || aiLocked || awaitingFeed) {
                clearRecovery();
            }
            return;
        }

        if (!dragon.isTame() && !isTamingStunned() && isBelowTamingThreshold()) {
            enterHoldState();
        }

        if (!isTamingStunned()) {
            return;
        }

        ensureStunState();

        if (awaitingFeed) {
            if (stunTimeoutTicks > 0) {
                stunTimeoutTicks--;
                if (stunTimeoutTicks <= 0) {
                    dragon.setHealth(dragon.getMaxHealth());

                    var nearbyPlayers = level.getEntitiesOfClass(
                            Player.class,
                            dragon.getBoundingBox().inflate(32.0),
                            p -> p instanceof ServerPlayer
                    );
                    for (var player : nearbyPlayers) {
                        if (player instanceof ServerPlayer serverPlayer) {
                            serverPlayer.displayClientMessage(
                                    net.minecraft.network.chat.Component.translatable(
                                            getTamingTimeoutTranslationKey(),
                                            dragon.getName()
                                    ),
                                    true
                            );
                        }
                    }

                    clearRecovery();
                }
            }
            return;
        }

        if (stunGraceTicks > 0) {
            stunGraceTicks--;
        }

        if (recoveryTargetHealth > 0.0F && dragon.getHealth() + 0.5F < recoveryTargetHealth) {
            float missing = recoveryTargetHealth - dragon.getHealth();
            float healPerTick = Math.max(2.0F, dragon.getMaxHealth() * 0.02F);
            dragon.heal(Math.min(healPerTick, missing));
        } else if (stunGraceTicks <= 0) {
            clearRecovery();
        }
    }

    protected void ensureStunState() {
        if (!canUseTamingStun()) {
            clearRecovery();
            return;
        }

        boolean airborne = enforceGroundedStunPhysics();
        // Keep AI unlocked while airborne so physics integration can move the dragon downward.
        // Lock AI only once grounded to hold the stunned/downed posture.
        if (!dragon.level().isClientSide) {
            if (airborne) {
                if (aiLocked) {
                    dragon.setNoAi(false);
                    aiLocked = false;
                }
            } else if (!aiLocked) {
                dragon.setNoAi(true);
                aiLocked = true;
            }
        }

        dragon.setTarget(null);
        dragon.setAggressive(false);
        dragon.getNavigation().stop();
        if (!airborne) {
            dragon.setDeltaMovement(Vec3.ZERO);
        }
        if (dragon.isVehicle()) {
            dragon.ejectPassengers();
        }
        setTamingStunned(true);
    }

    private void forceWakeForStun() {
        if (!dragon.level().isClientSide) {
            dragon.wakeUpImmediately();
            dragon.suppressSleep(200);
        }
    }

    private boolean enforceGroundedStunPhysics() {
        if (!canUseTamingStun()) {
            return false;
        }

        boolean airborne = isInAerialStateForStun() || !dragon.onGround();
        dragon.setNoGravity(false);
        if (!airborne) {
            return false;
        }

        clearAerialStateForStun();
        stopActiveAbilitiesForStun();

        Vec3 currentVel = dragon.getDeltaMovement();
        dragon.setDeltaMovement(
                currentVel.x * 0.35D,
                Math.min(currentVel.y, -0.42D),
                currentVel.z * 0.35D
        );
        dragon.hasImpulse = true;
        return true;
    }

    private boolean canUseTamingStun() {
        return !dragon.isDying() && dragon.isAlive() && dragon.getHealth() > 0.0F;
    }

    protected abstract boolean isTamingStunned();
    protected abstract void setTamingStunned(boolean stunned);
    protected abstract boolean isBelowTamingThreshold();
    protected abstract float getTamingThreshold();
    protected abstract String getTamingTimeoutTranslationKey();
    protected abstract boolean isInAerialStateForStun();
    protected abstract void clearAerialStateForStun();
    protected abstract void stopActiveAbilitiesForStun();
}
