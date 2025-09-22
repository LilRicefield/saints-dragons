package com.leon.saintsdragons.server.entity.dragons.amphithere.handlers;

import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Handles all interactions with Amphithere entities
 * Adapted from Lightning Dragon interaction handler
 */
public class AmphithereInteractionHandler {
    private final AmphithereEntity dragon;

    public AmphithereInteractionHandler(AmphithereEntity dragon) {
        this.dragon = dragon;
    }

    /**
     * Main interaction entry point - called from mobInteract
     */
    public InteractionResult handleInteraction(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (!dragon.isTame()) {
            return handleUntamedInteraction(player, hand, heldItem);
        } else {
            return handleTamedInteraction(player, hand, heldItem);
        }
    }

    /**
     * Handle interactions with untamed amphitheres (taming)
     */
    private InteractionResult handleUntamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
        if (!dragon.isFood(heldItem)) {
            return InteractionResult.PASS;
        }

        if (!dragon.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                heldItem.shrink(1);
            }

            if (dragon.getRandom().nextInt(5) == 0) {
                dragon.tame(player);
                dragon.getNavigation().stop();
                dragon.setCommand(1); // Sit by default on taming
                dragon.setOrderedToSit(true);
                dragon.setTarget(null);
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
            } else {
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
            }
        }

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }

    /**
     * Handle interactions with tamed amphitheres (feeding, commands, mounting)
     */
    private InteractionResult handleTamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
        if (!dragon.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }

        // Handle feeding for healing
        if (dragon.isFood(heldItem) && dragon.getHealth() < dragon.getMaxHealth()) {
            return handleFeeding(player, heldItem);
        }

        // Handle owner commands - Shift+Right-click cycles through commands
        if (dragon.canOwnerCommand(player) && heldItem.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            return handleCommandCycling(player);
        }

        // Handle mounting
        if (hand == InteractionHand.MAIN_HAND && heldItem.isEmpty() && !player.isCrouching()) {
            if (dragon.canOwnerMount(player) && !dragon.isVehicle()) {
                if (!dragon.level().isClientSide) {
                    if (dragon.isOrderedToSit()) {
                        dragon.setOrderedToSit(false);
                    }
                    dragon.setTarget(null);
                    player.startRiding(dragon);
                }
                return InteractionResult.sidedSuccess(dragon.level().isClientSide);
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * Handle feeding the dragon for healing
     */
    private InteractionResult handleFeeding(Player player, ItemStack food) {
        if (!dragon.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                food.shrink(1);
            }

            float healAmount = 5.0F;
            float newHealth = Math.min(dragon.getHealth() + healAmount, dragon.getMaxHealth());
            boolean fullyHealed = newHealth >= dragon.getMaxHealth();

            dragon.heal(healAmount);
            dragon.level().broadcastEntityEvent(dragon, (byte) 7);

            // Send appropriate message
            if (fullyHealed) {
                player.displayClientMessage(
                    Component.translatable("entity.saintsdragons.amphithere.fed", dragon.getName()),
                    true
                );
            } else {
                player.displayClientMessage(
                    Component.translatable("entity.saintsdragons.amphithere.fed_partial", dragon.getName()),
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
        dragon.setCommand(nextCommand);
        applyCommandState(nextCommand);
        
        // Send feedback message to player (action bar), server-side only to avoid duplicates
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
                // Immediately reset sit progress when standing up
                dragon.sitProgress = 0f;
                dragon.getEntityData().set(DragonEntity.DATA_SIT_PROGRESS, 0f);
                break;
            case 1: // Sit
                dragon.setOrderedToSit(true);
                break;
            case 2: // Wander
                dragon.setOrderedToSit(false);
                // Immediately reset sit progress when standing up
                dragon.sitProgress = 0f;
                dragon.getEntityData().set(DragonEntity.DATA_SIT_PROGRESS, 0f);
                break;
        }
    }
}
