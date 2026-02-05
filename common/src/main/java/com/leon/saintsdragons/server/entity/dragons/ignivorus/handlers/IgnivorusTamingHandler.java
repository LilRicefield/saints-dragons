package com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Dedicated taming state machine for Ignivorus.
 * Keeps taming stun logic contained so transitions stay predictable.
 */
public class IgnivorusTamingHandler {
    private final Ignivorus dragon;

    private int failureCounter = 0;
    private float recoveryTargetHealth = -1.0F;
    private int stunGraceTicks = 0;
    private boolean aiLocked = false;
    private boolean awaitingFeed = false;
    private int stunTimeoutTicks = 0;

    private static final int STUN_TIMEOUT = 20 * 30; // 30 seconds

    public IgnivorusTamingHandler(Ignivorus dragon) {
        this.dragon = dragon;
    }

    public void tickServer() {
        tickRecovery();
    }

    public boolean isAwaitingFeed() {
        return awaitingFeed;
    }

    public void enterStun() {
        awaitingFeed = false;
        stunGraceTicks = Math.max(stunGraceTicks, 20);
        recoveryTargetHealth = -1.0F;
        stunTimeoutTicks = 0; // Reset timeout when entering stun (player fed it)
        ensureStunState();
    }

    public void enterHoldState() {
        awaitingFeed = true;
        stunGraceTicks = 0;
        recoveryTargetHealth = -1.0F;
        stunTimeoutTicks = STUN_TIMEOUT; // Start 30 second timeout
        ensureStunState();

        // Play stun sound effect to indicate dragon is now vulnerable
        if (!dragon.level().isClientSide) {
            dragon.playSound(net.minecraft.sounds.SoundEvents.ARROW_HIT_PLAYER, 1.5F, 0.8F);
        }
    }

    public void setRecoveryTarget(float targetHealth) {
        awaitingFeed = false;
        recoveryTargetHealth = Math.max(0.0F, Math.min(targetHealth, dragon.getMaxHealth()));
        stunGraceTicks = Math.max(stunGraceTicks, 40);
        stunTimeoutTicks = 0; // Reset timeout when healing starts
        ensureStunState();
    }

    public void clearRecovery() {
        recoveryTargetHealth = -1.0F;
        stunGraceTicks = 0;
        awaitingFeed = false;
        stunTimeoutTicks = 0; // Reset timeout
        boolean wasStunned = dragon.isTamingStunned();
        if (dragon.isTamingStunned()) {
            dragon.getEntityData().set(Ignivorus.DATA_TAMING_STUNNED, false);
        }
        if (aiLocked && !dragon.level().isClientSide) {
            aiLocked = false;
            dragon.setNoAi(false);
        }
        // No release animation; movement controller handles transitioning out
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
        tag.putBoolean("TamingStunned", dragon.isTamingStunned());
        tag.putInt("TamingStunTimeout", Math.max(0, stunTimeoutTicks));
    }

    public void load(CompoundTag tag) {
        failureCounter = Math.max(0, tag.getInt("TamingFailures"));
        recoveryTargetHealth = tag.contains("TamingTargetHealth") ? tag.getFloat("TamingTargetHealth") : -1.0F;
        stunGraceTicks = Math.max(0, tag.getInt("TamingStunGraceTicks"));
        aiLocked = tag.getBoolean("TamingAiLocked");
        awaitingFeed = tag.getBoolean("TamingAwaitingFeed");
        stunTimeoutTicks = Math.max(0, tag.getInt("TamingStunTimeout"));
        if (tag.getBoolean("TamingStunned")) {
            dragon.getEntityData().set(Ignivorus.DATA_TAMING_STUNNED, true);
            if (aiLocked && !dragon.level().isClientSide) {
                dragon.setNoAi(true);
            }
        } else {
            clearRecovery();
        }
    }

    private void tickRecovery() {
        Level level = dragon.level();
        if (level.isClientSide) {
            return;
        }

        // Wild babies are not part of combat-stun taming; keep them out of stun loop entirely.
        if (dragon.isBaby() && !dragon.isTame()) {
            if (dragon.isTamingStunned() || aiLocked || awaitingFeed) {
                clearRecovery();
            }
            return;
        }

        if (!dragon.isTame() && !dragon.isTamingStunned() && dragon.isBelowTamingThreshold()) {
            enterHoldState();
        }

        if (!dragon.isTamingStunned()) {
            return;
        }

        ensureStunState();

        // Tick down stun timeout if waiting for feed
        if (awaitingFeed) {
            if (stunTimeoutTicks > 0) {
                stunTimeoutTicks--;
                if (stunTimeoutTicks <= 0) {
                    // Timeout reached - player took too long to decide
                    // Heal dragon to full and release from stun
                    dragon.setHealth(dragon.getMaxHealth());

                    // Notify nearby players about the timeout
                    if (!level.isClientSide) {
                        var nearbyPlayers = level.getEntitiesOfClass(
                            net.minecraft.world.entity.player.Player.class,
                            dragon.getBoundingBox().inflate(32.0),
                            p -> p instanceof net.minecraft.server.level.ServerPlayer
                        );
                        for (var player : nearbyPlayers) {
                            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                                serverPlayer.displayClientMessage(
                                    net.minecraft.network.chat.Component.translatable(
                                        "entity.saintsdragons.ignivorus.taming_timeout",
                                        dragon.getName()
                                    ),
                                    true
                                );
                            }
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

    private void ensureStunState() {
        boolean wasStunned = dragon.isTamingStunned();
        if (!dragon.level().isClientSide && !aiLocked) {
            dragon.setNoAi(true);
            aiLocked = true;
        }
        dragon.setTarget(null);
        dragon.setAggressive(false);
        dragon.getNavigation().stop();
        dragon.setDeltaMovement(Vec3.ZERO);
        if (dragon.isVehicle()) {
            dragon.ejectPassengers();
        }
        dragon.getEntityData().set(Ignivorus.DATA_TAMING_STUNNED, true);
    }
}
