package com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Handles all player interactions with Ignivorus dragons.
 * Simplified version for testing - no breeding or advanced features yet.
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
        if (!dragon.isFood(itemstack)) {
            return InteractionResult.PASS;
        }

        if (!dragon.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            int tameRoll = hearty ? 6 : 15; // hearty meal improves taming odds

            // 1 in tameRoll chance to tame per feeding
            if (dragon.getRandom().nextInt(tameRoll) == 0) {
                // Successful taming
                dragon.tame(player);
                dragon.setOrderedToSit(true);
                dragon.setCommandManual(1); // Sit command
                dragon.level().broadcastEntityEvent(dragon, (byte) 7); // Hearts
            } else {
                // Failed taming attempt
                dragon.level().broadcastEntityEvent(dragon, (byte) 6); // Smoke
            }
        }

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }

    /**
     * Handle interactions with tamed dragons.
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
        if (!dragon.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            // Heal the dragon
            float currentHealth = dragon.getHealth();
            boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            float healAmount = hearty ? 30.0F : 10.0F;
            float newHealth = Math.min(currentHealth + healAmount, dragon.getMaxHealth());
            dragon.setHealth(newHealth);

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
}
