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
                dragon.setOrderedToSit(true);
                dragon.setCommand(1); // Set command to Sit (1) to match the sitting state
                dragon.setTarget(null);
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
            } else {
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
            }
            return InteractionResult.sidedSuccess(false);
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * Handle interactions with tamed amphitheres (feeding, commands, mounting)
     */
    private InteractionResult handleTamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
        boolean isOwner = dragon.isOwnedBy(player);

        // Owner-only interactions
        if (isOwner) {
            // Handle feeding for healing
            if (dragon.isFood(heldItem) && dragon.getHealth() < dragon.getMaxHealth()) {
                return handleFeeding(player, heldItem);
            }

            // Handle owner commands - Shift+Right-click cycles through commands
            if (dragon.canOwnerCommand(player) && heldItem.isEmpty() && hand == InteractionHand.MAIN_HAND) {
                return handleCommandCycling(player);
            }
        }

        // Handle mounting - both owner and non-owners can mount
        if (hand == InteractionHand.MAIN_HAND && heldItem.isEmpty() && !player.isCrouching()) {
            return handleMounting(player, isOwner);
        }

        return InteractionResult.PASS;
    }

    /**
     * Handle mounting logic for both owner and passengers
     */
    private InteractionResult handleMounting(Player player, boolean isOwner) {
        var passengers = dragon.getPassengers();

        if (isOwner) {
            // Owner can mount if seat 0 is empty (or no passengers at all)
            if (passengers.isEmpty() && dragon.canOwnerMount(player)) {
                if (!dragon.level().isClientSide) {
                    dragon.prepareForMounting();
                    player.startRiding(dragon);
                }
                return InteractionResult.sidedSuccess(dragon.level().isClientSide);
            } else if (!passengers.isEmpty() && passengers.get(0) != player) {
                // Owner tried to mount but someone else is in seat 0 (shouldn't happen normally)
                if (!dragon.level().isClientSide) {
                    player.displayClientMessage(
                        Component.translatable("entity.saintsdragons.amphithere.mount_occupied"),
                        true
                    );
                }
                return InteractionResult.FAIL;
            }
        } else {
            // Non-owner can mount as passenger if:
            // 1. Seat 0 is occupied by the owner
            // 2. Seat 1 is empty (less than 2 passengers)
            // 3. Dragon is not sitting or doing something that would prevent riding

            if (passengers.isEmpty()) {
                // No one is riding - non-owners can't mount without owner
                if (!dragon.level().isClientSide) {
                    player.displayClientMessage(
                        Component.translatable("entity.saintsdragons.amphithere.passenger_needs_owner"),
                        true
                    );
                }
                return InteractionResult.FAIL;
            }

            if (passengers.size() >= 2) {
                // Both seats are full
                if (!dragon.level().isClientSide) {
                    player.displayClientMessage(
                        Component.translatable("entity.saintsdragons.amphithere.seats_full"),
                        true
                    );
                }
                return InteractionResult.FAIL;
            }

            // Check if owner is in seat 0
            if (passengers.get(0) instanceof Player firstPlayer && dragon.isOwnedBy(firstPlayer)) {
                // Owner is driving, non-owner can mount as passenger
                if (!dragon.level().isClientSide) {
                    player.startRiding(dragon);
                }
                return InteractionResult.sidedSuccess(dragon.level().isClientSide);
            } else {
                // Seat 0 is occupied by non-owner (shouldn't happen, but handle it)
                if (!dragon.level().isClientSide) {
                    player.displayClientMessage(
                        Component.translatable("entity.saintsdragons.amphithere.passenger_needs_owner"),
                        true
                    );
                }
                return InteractionResult.FAIL;
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
            return InteractionResult.CONSUME;
        }

        return InteractionResult.sidedSuccess(true);
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
