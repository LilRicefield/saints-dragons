package com.leon.saintsdragons.server.entity.dragons.raevyx.handlers;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
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
public record RaevyxInteractionHandler(Raevyx wyvern) {
    
    /**
     * Main interaction entry point.
     * Delegates to specific handlers based on wyvern state and interaction type.
     */
    public InteractionResult handleInteraction(Player player, InteractionHand hand) {
        if (wyvern.isDying()) {
            return InteractionResult.PASS;
        }
        
        ItemStack itemstack = player.getItemInHand(hand);
        
        if (!wyvern.isTame()) {
            return handleUntamedInteraction(player, itemstack);
        } else {
            return handleTamedInteraction(player, itemstack, hand);
        }
    }
    
    /**
     * Handle interactions with untamed dragons (taming attempts).
     */
    private InteractionResult handleUntamedInteraction(Player player, ItemStack itemstack) {
        if (!wyvern.isFood(itemstack)) {
            return InteractionResult.PASS;
        }
        
        // Taming logic must be server-only to avoid client-only visual state changes
        if (!wyvern.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            
            if (wyvern.getRandom().nextInt(10) == 0) {
                // Successful taming
                wyvern.tame(player);
                wyvern.setOrderedToSit(true);
                wyvern.setCommandManual(1); // Set command to Sit (1) to match the sitting state
                wyvern.level().broadcastEntityEvent(wyvern, (byte) 7);
                
                // Trigger advancement for taming Lightning Dragon
                triggerTamingAdvancement(player);
            } else {
                // Failed taming attempt
                wyvern.level().broadcastEntityEvent(wyvern, (byte) 6);
            }
        }
        
        return InteractionResult.sidedSuccess(wyvern.level().isClientSide);
    }
    
    /**
     * Handle interactions with tamed dragons (feeding, commands, mounting).
     */
    private InteractionResult handleTamedInteraction(Player player, ItemStack itemstack, InteractionHand hand) {
        boolean isOwner = player.equals(wyvern.getOwner());

        // Handle feeding for healing
        if (wyvern.isFood(itemstack)) {
            if (player.isCrouching() && isOwner) {
                return handleBreeding(player, itemstack);
            }
            return handleFeeding(player, itemstack);
        }
        
        // Handle owner commands and mounting
        if (isOwner) {
            boolean isSleeping = wyvern.isSleeping() || wyvern.isSleepTransitioning();
            // Command cycling - Shift+Right-click cycles through commands
            if (canOwnerCommand(player) && itemstack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
                if (isSleeping) {
                    if (!wyvern.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.displayClientMessage(
                            Component.translatable("entity.saintsdragons.raevyx.sleeping", wyvern.getName()),
                            true
                        );
                    }
                    return InteractionResult.sidedSuccess(wyvern.level().isClientSide);
                }
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
     * Handle initiating breeding when crouching with food.
     */
    private InteractionResult handleBreeding(Player player, ItemStack itemstack) {
        boolean client = wyvern.level().isClientSide;

        if (wyvern.isBaby()) {
            sendStatusMessage(player, "entity.saintsdragons.raevyx.breeding_too_young");
            return InteractionResult.sidedSuccess(client);
        }

        if (wyvern.getAge() != 0) { // still on cooldown from previous breeding
            sendStatusMessage(player, "entity.saintsdragons.raevyx.breeding_cooling_down");
            return InteractionResult.sidedSuccess(client);
        }

        if (wyvern.isInLove()) {
            sendStatusMessage(player, "entity.saintsdragons.raevyx.breeding_already_ready");
            return InteractionResult.sidedSuccess(client);
        }

        if (!client) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            wyvern.setInLove(player);
            sendStatusMessage(player, "entity.saintsdragons.raevyx.breeding_ready");
        }

        return InteractionResult.sidedSuccess(client);
    }
    
    /**
     * Handle feeding tamed dragons for healing or growth.
     */
    private InteractionResult handleFeeding(Player player, ItemStack itemstack) {
        if (!wyvern.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            // Babies: speed up growth instead of healing
            if (wyvern.isBaby()) {
                // Age baby by a fixed amount per feeding (same as vanilla animals)
                // 10% of base growth time = 2400 ticks = 2 minutes saved per fish
                int currentAge = wyvern.getAge();
                int newAge = Math.min(0, currentAge + 2400); // Cap at 0 (adult)
                wyvern.setAge(newAge);

                // Play eating sound and particles
                wyvern.level().broadcastEntityEvent(wyvern, (byte) 6); // Eating sound
                wyvern.level().broadcastEntityEvent(wyvern, (byte) 7); // Hearts particles

                // Send feedback message with remaining time
                if (player instanceof ServerPlayer serverPlayer) {
                    int remainingTicks = Math.abs(newAge);
                    int remainingMinutes = remainingTicks / 1200; // 1200 ticks = 1 minute
                    String messageKey = (newAge == 0)
                        ? "entity.saintsdragons.raevyx.baby_grown"
                        : "entity.saintsdragons.raevyx.baby_fed";
                    serverPlayer.displayClientMessage(
                        Component.translatable(messageKey, wyvern.getName()),
                        true
                    );
                }
            } else {
                // Adults: heal when fed
                float healAmount = 10.0f; // Heal 5 hearts per fish
                float oldHealth = wyvern.getHealth();
                float newHealth = Math.min(oldHealth + healAmount, wyvern.getMaxHealth());
                wyvern.setHealth(newHealth);

                // Play eating sound and particles
                wyvern.level().broadcastEntityEvent(wyvern, (byte) 6); // Eating sound
                wyvern.level().broadcastEntityEvent(wyvern, (byte) 7); // Hearts particles

                // Send appropriate feedback message
                sendFeedingMessage(player, newHealth);
            }
        }

        return InteractionResult.sidedSuccess(wyvern.level().isClientSide);
    }
    
    /**
     * Handle command cycling (Follow/Sit/Wander).
     */
    private InteractionResult handleCommandCycling(Player player) {
        // Prevent command changes during sit transitions (sitting down or standing up)
        boolean isTransitioning = wyvern.isInSitTransition();

        if (isTransitioning) {
            // Dragon is in the middle of sitting down or standing up - ignore command spam
            if (!wyvern.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                // Determine which transition is happening
                boolean sittingDown = wyvern.isSittingDownAnimation();
                String messageKey = sittingDown
                    ? "entity.saintsdragons.raevyx.sitting_down"
                    : "entity.saintsdragons.raevyx.standing_up";

                serverPlayer.displayClientMessage(
                    Component.translatable(messageKey, wyvern.getName()),
                    true
                );
            }
            return InteractionResult.sidedSuccess(wyvern.level().isClientSide);
        }

        // Get current command and cycle to next
        int currentCommand = wyvern.getCommand();
        int nextCommand = (currentCommand + 1) % 3; // 0=Follow, 1=Sit, 2=Wander

        // Apply the new command
        wyvern.setCommandManual(nextCommand);
        applyCommandState(nextCommand);

        // Send feedback message to player (action bar), server-side only to avoid duplicates
        if (!wyvern.level().isClientSide) {
            player.displayClientMessage(
                Component.translatable(
                    "entity.saintsdragons.all.command_" + nextCommand,
                        wyvern.getName()
                ),
                true
            );
        }

        return InteractionResult.SUCCESS;
    }
    
    /**
     * Apply the command state to the wyvern.
     */
    private void applyCommandState(int command) {
        switch (command) {
            case 0: // Follow
                wyvern.setOrderedToSit(false);
                // Immediately reset sit progress when standing up
                wyvern.sitProgress = 0f;
                wyvern.getEntityData().set(DragonEntity.DATA_SIT_PROGRESS, 0f);
                break;
            case 1: // Sit
                wyvern.setOrderedToSit(true);
                break;
            case 2: // Wander
                wyvern.setOrderedToSit(false);
                // Immediately reset sit progress when standing up
                wyvern.sitProgress = 0f;
                wyvern.getEntityData().set(DragonEntity.DATA_SIT_PROGRESS, 0f);
                break;
        }
    }
    
    private void sendStatusMessage(Player player, String key) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable(key, wyvern.getName()), true);
        }
    }
    
    /**
     * Handle mounting the wyvern.
     */
    private InteractionResult handleMounting(Player player) {
        if (wyvern.isVehicle()) {
            return InteractionResult.sidedSuccess(wyvern.level().isClientSide);
        }
        
        // Force the wyvern to stand if sitting
        if (wyvern.isOrderedToSit()) {
            wyvern.setOrderedToSit(false);
        }
        
        // Wake up immediately when mounting (bypass transitions/animations)
        if (wyvern.isSleeping() || wyvern.isSleepTransitioning()) {
            wyvern.wakeUpImmediately();
            wyvern.suppressSleep(300);
        }
        
        // Clear all combat and AI states when mounting
        wyvern.clearAllStatesForMounting();
        
        // Start riding
        if (player.startRiding(wyvern)) {
            // Play excited sound when mounting
            wyvern.playExcitedSound();
            return InteractionResult.sidedSuccess(wyvern.level().isClientSide);
        }
        
        return InteractionResult.sidedSuccess(wyvern.level().isClientSide);
    }
    
    /**
     * Trigger the taming advancement for the player.
     * Awards different advancements based on dragon gender.
     */
    private void triggerTamingAdvancement(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (wyvern.isFemale()) {
                // Female-specific advancement
                var femaleAdvancement = serverPlayer.server.getAdvancements()
                    .getAdvancement(SaintsDragons.rl("tame_raevyx_female"));
                if (femaleAdvancement != null) {
                    serverPlayer.getAdvancements().award(femaleAdvancement, "tame_raevyx_female");
                }
            } else {
                // Male-specific advancement
                var advancement = serverPlayer.server.getAdvancements()
                    .getAdvancement(SaintsDragons.rl("tame_raevyx"));
                if (advancement != null) {
                    serverPlayer.getAdvancements().award(advancement, "tame_raevyx");
                }
            }
        }
    }
    
    /**
     * Send appropriate feeding message based on healing result.
     */
    private void sendFeedingMessage(Player player, float newHealth) {
        if (player instanceof ServerPlayer serverPlayer) {
            String messageKey = (newHealth >= wyvern.getMaxHealth())
                ? "entity.saintsdragons.raevyx.fed"
                : "entity.saintsdragons.raevyx.fed_partial";
                
            serverPlayer.displayClientMessage(
                Component.translatable(messageKey, wyvern.getName()),
                true
            );
        }
    }
    
    /**
     * Check if the player can command the wyvern (owner check).
     */
    private boolean canOwnerCommand(Player player) {
        return wyvern.canOwnerCommand(player);
    }
    
    /**
     * Check if the player can mount the wyvern (owner check).
     */
    private boolean canOwnerMount(Player player) {
        return wyvern.canOwnerMount(player);
    }
}
