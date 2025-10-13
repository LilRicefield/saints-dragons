package com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Handles all player interactions with Rift Drakes.
 * Extracted from RiftDrakeEntity to improve maintainability and reduce class size.
 */
public record NulljawInteractionHandler(Nulljaw drake) {

    public InteractionResult handleInteraction(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (!drake.isTame()) {
            return handleUntamedInteraction(player, hand, heldItem);
        } else {
            return handleTamedInteraction(player, hand, heldItem);
        }
    }

    private InteractionResult handleTamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
        if (!drake.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }

        // Handle feeding for healing
        if (drake.isFood(heldItem) && drake.getHealth() < drake.getMaxHealth()) {
            return handleFeeding(player, heldItem);
        }

        // Handle owner commands - Shift+Right-click cycles through commands
        if (drake.canOwnerCommand(player) && heldItem.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            return handleCommandCycling(player);
        }

        // Handle mounting
        if (hand == InteractionHand.MAIN_HAND && heldItem.isEmpty() && !player.isCrouching()) {
            if (drake.canOwnerMount(player) && !drake.isVehicle()) {
                if (!drake.level().isClientSide) {
                    if (drake.isOrderedToSit()) {
                        drake.setOrderedToSit(false);
                    }
                    drake.setTarget(null);
                    player.startRiding(drake);
                }
                return InteractionResult.sidedSuccess(drake.level().isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    private InteractionResult handleFeeding(Player player, ItemStack food) {
        if (!drake.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                food.shrink(1);
            }

            float healAmount = 5.0F;
            float newHealth = Math.min(drake.getHealth() + healAmount, drake.getMaxHealth());
            boolean fullyHealed = newHealth >= drake.getMaxHealth();

            drake.heal(healAmount);
            drake.level().broadcastEntityEvent(drake, (byte) 7);

            // Send appropriate message
            if (fullyHealed) {
                player.displayClientMessage(
                        Component.translatable("entity.saintsdragons.nulljaw.fed", drake.getName()),
                        true
                );
            } else {
                player.displayClientMessage(
                        Component.translatable("entity.saintsdragons.nulljaw.fed_partial", drake.getName()),
                        true
                );
            }
        }

        return InteractionResult.sidedSuccess(drake.level().isClientSide);
    }
    
    /**
     * Handle interactions with untamed Rift Drakes (taming attempts).
     */
    private InteractionResult handleUntamedInteraction(Player player, InteractionHand hand, ItemStack itemstack) {
        if (!drake.isFood(itemstack)) {
            return InteractionResult.PASS;
        }
        
        // Taming logic must be server-only to avoid client-only visual state changes
        if (!drake.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            
            // Rift Drakes have a 1 in 8 chance of taming (easier than Lightning Dragons)
            if (drake.getRandom().nextInt(8) == 0) {
                // Successful taming
                drake.tame(player);
                drake.setOrderedToSit(true);
                drake.setCommand(1); // Set command to Sit (1) to match the sitting state
                drake.level().broadcastEntityEvent(drake, (byte) 7); // Success particles
                
                // Trigger advancement for taming Rift Drake
                triggerTamingAdvancement(player);
            } else {
                // Failed taming attempt
                drake.level().broadcastEntityEvent(drake, (byte) 6); // Smoke particles
            }
        }
        
        return InteractionResult.sidedSuccess(drake.level().isClientSide);
    }
    
    /**
     * Handle interactions with tamed Rift Drakes (feeding, commands, mounting).
     */
    private InteractionResult handleTamedInteraction(Player player, ItemStack itemstack, InteractionHand hand) {
        if (drake.isFood(itemstack) && drake.getHealth() < drake.getMaxHealth()) {
            // Feed the drake to heal it
            if (!drake.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                
                float healAmount = 10.0f; // Heal 5 hearts per fish
                float oldHealth = drake.getHealth();
                float newHealth = Math.min(oldHealth + healAmount, drake.getMaxHealth());
                drake.setHealth(newHealth);
                
                // Show healing particles (green hearts)
                drake.level().broadcastEntityEvent(drake, (byte) 7);
            }
            return InteractionResult.sidedSuccess(drake.level().isClientSide);
        }
        
        // Handle mounting for tamed dragons
        if (hand == InteractionHand.MAIN_HAND && player.getItemInHand(hand).isEmpty()) {
            if (!player.isShiftKeyDown()) {
                return handleMounting(player);
            }
        }
        
        // Fall back to base implementation for command cycling
        return drake.superMobInteract(player, hand);
    }
    
    /**
     * Handle mounting the drake.
     */
    private InteractionResult handleMounting(Player player) {
        // Check if player can mount (owner check) and drake isn't already being ridden
        if (!canPlayerMount(player) || drake.isVehicle()) {
            return InteractionResult.PASS;
        }
        
        // Clear any AI states that might interfere with mounting
        if (!drake.level().isClientSide) {
            if (drake.isOrderedToSit()) {
                drake.setOrderedToSit(false);
            }
            drake.setTarget(null);
            drake.getNavigation().stop();
            
            // Start riding
            player.startRiding(drake);
        }
        
        return InteractionResult.sidedSuccess(drake.level().isClientSide);
    }


    private InteractionResult handleCommandCycling(Player player) {
        // Get current command and cycle to next
        int currentCommand = drake.getCommand();
        int nextCommand = (currentCommand + 1) % 3; // 0=Follow, 1=Sit, 2=Wander

        // Apply the new command
        drake.setCommand(nextCommand);
        applyCommandState(nextCommand);

        // Send feedback message to player (action bar), server-side only to avoid duplicates
        if (!drake.level().isClientSide) {
            player.displayClientMessage(
                    Component.translatable(
                            "entity.saintsdragons.all.command_" + nextCommand,
                            drake.getName()
                    ),
                    true
            );
        }
        return InteractionResult.SUCCESS;
    }

    private void applyCommandState(int command) {
        switch (command) {
            case 0: // Follow
                drake.setOrderedToSit(false);
                // Immediately reset sit progress when standing up
                drake.sitProgress = 0f;
                drake.getEntityData().set(DragonEntity.DATA_SIT_PROGRESS, 0f);
                break;
            case 1: // Sit
                drake.setOrderedToSit(true);
                break;
            case 2: // Wander
                drake.setOrderedToSit(false);
                // Immediately reset sit progress when standing up
                drake.sitProgress = 0f;
                drake.getEntityData().set(DragonEntity.DATA_SIT_PROGRESS, 0f);
                break;
        }
    }
    
    /**
     * Check if the player can mount the drake (owner check).
     */
    private boolean canPlayerMount(Player player) {
        return drake.isTame() && drake.isOwnedBy(player);
    }
    
    /**
     * Trigger taming advancement for the player
     */
    private void triggerTamingAdvancement(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var advancement = serverPlayer.server.getAdvancements()
                .getAdvancement(SaintsDragons.rl("tame_nulljaw"));
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "tame_nulljaw");
            }
        }
    }
}
