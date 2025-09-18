package com.leon.saintsdragons.server.entity.dragons.lightningdragon.handlers;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Handles all player interactions with Lightning Dragons.
 * Extracted from LightningDragonEntity to improve maintainability and reduce class size.
 */
public record LightningDragonInteractionHandler(LightningDragonEntity dragon) {
    
    /**
     * Main interaction entry point.
     * Delegates to specific handlers based on dragon state and interaction type.
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
        
        // Taming logic must be server-only to avoid client-only visual state changes
        if (!dragon.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            
            if (dragon.getRandom().nextInt(10) == 0) {
                // Successful taming
                dragon.tame(player);
                dragon.setOrderedToSit(true);
                dragon.setCommand(1); // Set command to Sit (1) to match the sitting state
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
                
                // Trigger advancement for taming Lightning Dragon
                triggerTamingAdvancement(player);
            } else {
                // Failed taming attempt
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
            }
        }
        
        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }
    
    /**
     * Handle interactions with tamed dragons (feeding, commands, mounting).
     */
    private InteractionResult handleTamedInteraction(Player player, ItemStack itemstack, InteractionHand hand) {
        // Handle feeding for healing
        if (dragon.isFood(itemstack)) {
            return handleFeeding(player, itemstack);
        }
        
        // Handle owner commands and mounting
        if (player.equals(dragon.getOwner())) {
            // Command cycling - Shift+Right-click cycles through commands
            if (canOwnerCommand(player) && itemstack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
                return handleCommandCycling(player);
            }
            // Mounting - Right-click without shift
            else if (!player.isCrouching() && itemstack.isEmpty() && hand == InteractionHand.MAIN_HAND && canOwnerMount(player)) {
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
            
            // Heal the dragon when fed
            float healAmount = 10.0f; // Heal 5 hearts per salmon
            float oldHealth = dragon.getHealth();
            float newHealth = Math.min(oldHealth + healAmount, dragon.getMaxHealth());
            dragon.setHealth(newHealth);
            
            // Play eating sound and particles
            dragon.level().broadcastEntityEvent(dragon, (byte) 6); // Eating sound
            dragon.level().broadcastEntityEvent(dragon, (byte) 7); // Hearts particles
            
            // Send appropriate feedback message
            sendFeedingMessage(player, newHealth);
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
        
        // Wake up immediately when mounting (bypass transitions/animations)
        if (dragon.isSleeping() || dragon.isSleepTransitioning()) {
            dragon.wakeUpImmediately();
        }
        
        // Clear all combat and AI states when mounting
        dragon.clearAllStatesForMounting();
        
        // Start riding
        if (player.startRiding(dragon)) {
            // Play excited sound when mounting
            dragon.playExcitedSound();
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }
        
        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }
    
    /**
     * Trigger the taming advancement for the player.
     */
    private void triggerTamingAdvancement(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var advancement = serverPlayer.server.getAdvancements()
                .getAdvancement(SaintsDragons.rl("tame_lightning_dragon"));
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "tame_lightning_dragon");
            }
        }
    }
    
    /**
     * Send appropriate feeding message based on healing result.
     */
    private void sendFeedingMessage(Player player, float newHealth) {
        if (player instanceof ServerPlayer serverPlayer) {
            String messageKey = (newHealth >= dragon.getMaxHealth()) 
                ? "entity.saintsdragons.lightning_dragon.fed"
                : "entity.saintsdragons.lightning_dragon.fed_partial";
                
            serverPlayer.displayClientMessage(
                Component.translatable(messageKey, dragon.getName()),
                true
            );
        }
    }
    
    /**
     * Check if the player can command the dragon (owner check).
     */
    private boolean canOwnerCommand(Player player) {
        return dragon.canOwnerCommand(player);
    }
    
    /**
     * Check if the player can mount the dragon (owner check).
     */
    private boolean canOwnerMount(Player player) {
        return dragon.canOwnerMount(player);
    }
}
