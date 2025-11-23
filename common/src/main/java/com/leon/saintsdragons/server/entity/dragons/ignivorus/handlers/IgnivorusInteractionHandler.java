package com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Handles all player interactions with Ignivorus dragons.
 */
public record IgnivorusInteractionHandler(Ignivorus dragon) {

    /**
     * Main interaction entry point.
     */
    public InteractionResult handleInteraction(Player player, InteractionHand hand) {
        if (dragon.isDying()) {
            return InteractionResult.PASS;
        }

        ItemStack itemstack = player.getItemInHand(hand);

        if (!dragon.isTame()) {
            return handleUntamedInteraction(player, itemstack);
        } else {
            return handleTamedInteraction(player, itemstack, hand);
        }
    }

    /**
     * Handle interactions with untamed dragons (taming attempts).
     */
    private InteractionResult handleUntamedInteraction(Player player, ItemStack itemstack) {
        boolean client = dragon.level().isClientSide;

        // Allow players to abort a taming attempt by crouching with empty hands
        if (dragon.isTamingStunned() && player.isCrouching() && itemstack.isEmpty()) {
            if (!client) {
                dragon.abortTamingAttempt();
                sendStatusMessage(player, "entity.saintsdragons.ignivorus.taming_aborted");
            }
            return InteractionResult.sidedSuccess(client);
        }

        if (!dragon.isFood(itemstack)) {
            return InteractionResult.PASS;
        }

        if (dragon.isTamingStunned()) {
            if (!dragon.isAwaitingTamingFeed()) {
                sendStatusMessage(player, "entity.saintsdragons.ignivorus.taming_dazed");
                return InteractionResult.CONSUME;
            }
        }

        // Check feeding cooldown to prevent spam-feeding
        if (!dragon.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                    Component.translatable("entity.saintsdragons.ignivorus.still_eating", dragon.getName()),
                    true
                );
            }
            return InteractionResult.CONSUME;
        }

        float minRequiredHealth = dragon.getTamingThreshold();
        // Add 1.0 HP buffer to prevent edge cases (e.g., small regeneration between ticks)
        if (dragon.getHealth() > minRequiredHealth + 1.0F) {
            sendStatusMessage(player, "entity.saintsdragons.ignivorus.taming_need_weakened");
            return InteractionResult.CONSUME;
        }

        // Taming logic must be server-only to avoid client-only visual state changes
        if (!client) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            // Trigger eat animation
            dragon.triggerAnim("action", "eat");

            // Set feeding cooldown
            dragon.setFeedingCooldown(20);

            boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            if (hearty) {
                dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }

            dragon.enterTamingStun();

            DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
            double tameChance = hearty
                ? config.extraDoubles().getOrDefault("taming_chance_hearty", 4.0)
                : config.extraDoubles().getOrDefault("taming_chance_base", 7.0);
            int tameRoll = (int) Math.round(tameChance);
            boolean success = dragon.getRandom().nextInt(Math.max(1, tameRoll)) == 0;

            if (success) {
                dragon.tame(player);
                dragon.setOrderedToSit(true);
                dragon.setCommandManual(1); // Set command to Sit (1) to match the sitting state
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
                dragon.resetTamingFailures();
                dragon.clearTamingRecovery();

                // Trigger advancement for taming Ignivorus
                triggerTamingAdvancement(player);
            } else {
                Float healTarget = nextFailureHealTarget();
                dragon.setTamingRecoveryTarget(healTarget);
                dragon.incrementTamingFailures();
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
                sendStatusMessage(player, "entity.saintsdragons.ignivorus.taming_failed");
            }
        }

        return InteractionResult.sidedSuccess(client);
    }

    /**
     * Handle interactions with tamed dragons (feeding, commands, mounting).
     */
    private InteractionResult handleTamedInteraction(Player player, ItemStack itemstack, InteractionHand hand) {
        boolean isOwner = player.equals(dragon.getOwner());

        // Handle feeding for healing
        if (dragon.isFood(itemstack)) {
            return handleFeeding(player, itemstack);
        }

        // Handle owner commands and mounting
        if (isOwner) {
            // Command cycling - Shift+Right-click cycles through commands
            if (player.isCrouching() && itemstack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
                return handleCommandCycling(player);
            }
            // Mounting - Right-click without shift
            else if (!player.isCrouching() && itemstack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
                return handleMounting(player);
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * Handle feeding tamed dragons for healing.
     */
    private InteractionResult handleFeeding(Player player, ItemStack itemstack) {
        // Check feeding cooldown to prevent spam-feeding
        if (!dragon.canFeed()) {
            if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                    Component.translatable("entity.saintsdragons.ignivorus.still_eating", dragon.getName()),
                    true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!dragon.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            // Trigger eat animation
            dragon.triggerAnim("action", "eat");

            // Set feeding cooldown
            dragon.setFeedingCooldown(20);

            // Heal the dragon
            float currentHealth = dragon.getHealth();
            boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            float healAmount = hearty ? 30.0F : 10.0F;
            float newHealth = Math.min(currentHealth + healAmount, dragon.getMaxHealth());
            dragon.setHealth(newHealth);
            if (hearty) {
                dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }

            // Play effects
            dragon.level().broadcastEntityEvent(dragon, (byte) 7); // Hearts

            // Send feedback message
            if (player instanceof ServerPlayer serverPlayer) {
                String messageKey = (newHealth >= dragon.getMaxHealth())
                    ? "entity.saintsdragons.ignivorus.fed"
                    : "entity.saintsdragons.ignivorus.fed_partial";

                serverPlayer.displayClientMessage(
                    Component.translatable(messageKey, dragon.getName()),
                    true
                );
            }
        }

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }

    /**
     * Handle command cycling (Follow/Sit/Wander).
     */
    private InteractionResult handleCommandCycling(Player player) {
        // Get current command and cycle to next
        int currentCommand = dragon.getCommand();
        int nextCommand = (currentCommand + 1) % 3; // 0=Follow, 1=Sit, 2=Wander

        // Apply the new command
        dragon.setCommandManual(nextCommand);
        applyCommandState(nextCommand);

        // Send feedback message to player
        if (!dragon.level().isClientSide) {
            player.displayClientMessage(
                Component.translatable(
                    "entity.saintsdragons.all.command_" + nextCommand,
                    dragon.getName()
                ),
                true
            );
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * Apply the command state to the dragon.
     */
    private void applyCommandState(int command) {
        switch (command) {
            case 0: // Follow
                dragon.setOrderedToSit(false);
                break;
            case 1: // Sit
                dragon.setOrderedToSit(true);
                break;
            case 2: // Wander
                dragon.setOrderedToSit(false);
                break;
        }
    }

    /**
     * Handle mounting the dragon.
     */
    private InteractionResult handleMounting(Player player) {
        if (dragon.isVehicle()) {
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }

        // Force the dragon to stand if sitting
        if (dragon.isOrderedToSit()) {
            dragon.setOrderedToSit(false);
        }

        // Start riding
        if (player.startRiding(dragon)) {
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }

    private Float nextFailureHealTarget() {
        // Always heal all the way back to max health after each failed attempt
        return dragon.getMaxHealth();
    }

    private void sendStatusMessage(Player player, String key) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable(key, dragon.getName()), true);
        }
    }

    private void triggerTamingAdvancement(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var advancement = serverPlayer.server.getAdvancements()
                    .getAdvancement(SaintsDragonsCommon.rl("tame_ignivorus"));
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "tame_ignivorus");
            }
        }
    }
}
