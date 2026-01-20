package com.leon.saintsdragons.server.entity.dragons.raevyx.handlers;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
        boolean client = wyvern.level().isClientSide;

        // Check if legacy taming is enabled
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        boolean legacyTaming = config.extraBoolean("legacy_taming", false);

        if (wyvern.isBaby()) {
            return handleBabyTaming(player, itemstack, config);
        }

        // Allow players to abort a taming attempt by crouching with empty hands (only in normal mode)
        if (!legacyTaming && wyvern.isTamingStunned() && player.isCrouching() && itemstack.isEmpty()) {
            if (!client) {
                wyvern.abortTamingAttempt();
                sendStatusMessage(player, "entity.saintsdragons.raevyx.taming_aborted");
            }
            return InteractionResult.sidedSuccess(client);
        }

        if (!wyvern.isFood(itemstack)) {
            return InteractionResult.PASS;
        }

        if (!legacyTaming) {
            // Normal mode: check taming stun state
            if (wyvern.isTamingStunned()) {
                if (!wyvern.isAwaitingTamingFeed()) {
                    sendStatusMessage(player, "entity.saintsdragons.raevyx.taming_dazed");
                    return InteractionResult.CONSUME;
                }
            }
        }

        // Check feeding cooldown to prevent spam-feeding
        if (wyvern.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                    Component.translatable("entity.saintsdragons.raevyx.still_eating", wyvern.getName()),
                    true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!legacyTaming) {
            // Normal mode: require low health
            float minRequiredHealth = wyvern.getTamingThreshold();
            // Add 1.0 HP buffer to prevent edge cases (e.g., small regeneration between ticks)
            if (wyvern.getHealth() > minRequiredHealth + 1.0F) {
                sendStatusMessage(player, "entity.saintsdragons.raevyx.taming_need_weakened");
                return InteractionResult.CONSUME;
            }
        }

        // Taming logic must be server-only to avoid client-only visual state changes
        if (!client) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            // Trigger eat animation
            wyvern.triggerAnim("action", "eat");

            // Set feeding cooldown (3.0417 seconds * 20 ticks/second = 61 ticks)
            wyvern.setFeedingCooldown(61);

            boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            if (hearty) {
                wyvern.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }

            // Legacy taming: heal the dragon instead of entering stun
            if (legacyTaming) {
                float healAmount = hearty ? 28.0f : 10.0f;
                float newHealth = Math.min(wyvern.getHealth() + healAmount, wyvern.getMaxHealth());
                wyvern.setHealth(newHealth);
            } else {
                // Normal mode: enter taming stun
                wyvern.enterTamingStun();
            }

            double tameChance = hearty
                ? config.extraDoubles().getOrDefault("taming_chance_hearty", 3.0)
                : config.extraDoubles().getOrDefault("taming_chance_base", 5.0);
            int tameRoll = (int) Math.round(tameChance);
            boolean success = wyvern.getRandom().nextInt(Math.max(1, tameRoll)) == 0;

            if (success) {
                wyvern.tame(player);
                wyvern.setOrderedToSit(true);
                wyvern.setCommandManual(1); // Set command to Sit (1) to match the sitting state
                wyvern.level().broadcastEntityEvent(wyvern, (byte) 7);
                if (!legacyTaming) {
                    wyvern.resetTamingFailures();
                    wyvern.clearTamingRecovery();
                }

                // Trigger advancement for taming Lightning Dragon
                triggerTamingAdvancement(player);
            } else {
                if (!legacyTaming) {
                    Float healTarget = nextFailureHealTarget();
                    wyvern.setTamingRecoveryTarget(healTarget);
                    wyvern.incrementTamingFailures();
                }
                wyvern.level().broadcastEntityEvent(wyvern, (byte) 6);
                sendStatusMessage(player, "entity.saintsdragons.raevyx.taming_failed");
            }
        }

        return InteractionResult.sidedSuccess(client);
    }

    private InteractionResult handleBabyTaming(Player player, ItemStack itemstack, DragonAttributeConfig config) {
        boolean client = wyvern.level().isClientSide;
        boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
        if (!wyvern.isFood(itemstack) && !itemstack.is(net.minecraft.world.item.Items.SALMON) && !hearty) {
            return InteractionResult.PASS;
        }

        // Check feeding cooldown to prevent spam-feeding
        if (wyvern.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                    Component.translatable("entity.saintsdragons.raevyx.still_eating", wyvern.getName()),
                    true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!client) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            // Trigger eat animation
            wyvern.triggerAnim("action", "eat");

            // Set feeding cooldown (3.0417 seconds * 20 ticks/second = 61 ticks)
            wyvern.setFeedingCooldown(61);

            if (hearty) {
                wyvern.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }

            double tameChance = hearty
                ? config.extraDoubles().getOrDefault("taming_chance_hearty", 3.0)
                : config.extraDoubles().getOrDefault("taming_chance_base", 5.0);
            int tameRoll = (int) Math.round(tameChance);
            boolean success = wyvern.getRandom().nextInt(Math.max(1, tameRoll)) == 0;

            if (success) {
                wyvern.tame(player);
                wyvern.setOrderedToSit(true);
                wyvern.setCommandManual(1);
                wyvern.level().broadcastEntityEvent(wyvern, (byte) 7);
                triggerTamingAdvancement(player);
            } else {
                wyvern.level().broadcastEntityEvent(wyvern, (byte) 6);
            }
        }

        return InteractionResult.sidedSuccess(client);
    }
    
    /**
     * Handle interactions with tamed dragons (feeding, commands, mounting).
     */
    private InteractionResult handleTamedInteraction(Player player, ItemStack itemstack, InteractionHand hand) {
        boolean isOwner = player.equals(wyvern.getOwner());

        if (isInteractionItem(itemstack)) {
            return InteractionResult.PASS;
        }

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
            if (canOwnerCommand(player) && !wyvern.isFood(itemstack) && hand == InteractionHand.MAIN_HAND) {
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
            // Mounting - Right-click without shift (allow any non-food item)
            else if (!player.isCrouching() && !wyvern.isFood(itemstack) && hand == InteractionHand.MAIN_HAND && canOwnerMount(player)) {
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

        // Check feeding cooldown to prevent spam-feeding
        if (wyvern.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                    Component.translatable("entity.saintsdragons.raevyx.still_eating", wyvern.getName()),
                    true
                );
            }
            return InteractionResult.CONSUME;
        }

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

            // Trigger eat animation
            wyvern.triggerAnim("action", "eat");

            // Set feeding cooldown (3.0417 seconds * 20 ticks/second = 61 ticks)
            wyvern.setFeedingCooldown(61);

            wyvern.setInLove(player);
            sendStatusMessage(player, "entity.saintsdragons.raevyx.breeding_ready");
        }

        return InteractionResult.sidedSuccess(client);
    }
    
    /**
     * Handle feeding tamed dragons for healing or growth.
     */
    private InteractionResult handleFeeding(Player player, ItemStack itemstack) {
        // Check feeding cooldown to prevent spam-feeding
        if (wyvern.canFeed()) {
            if (!wyvern.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                    Component.translatable("entity.saintsdragons.raevyx.still_eating", wyvern.getName()),
                    true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!wyvern.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            // Trigger eat animation
            wyvern.triggerAnim("action", "eat");

            // Set feeding cooldown (3.0417 seconds * 20 ticks/second = 61 ticks)
            wyvern.setFeedingCooldown(22);

            boolean heartyMeal = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            if (heartyMeal) {
                wyvern.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }

            // Babies: speed up growth instead of healing
            if (wyvern.isBaby()) {
                int growthTicks = heartyMeal ? 4800 : 2400; // hearty meal doubles growth bonus
                int currentAge = wyvern.getAge();
                int newAge = Math.min(0, currentAge + growthTicks);
                wyvern.setAge(newAge);

                wyvern.level().broadcastEntityEvent(wyvern, (byte) 6); // Eating sound
                wyvern.level().broadcastEntityEvent(wyvern, (byte) 7); // Hearts particles

                if (player instanceof ServerPlayer serverPlayer) {
                    int remainingTicks = Math.abs(newAge);
                    int remainingMinutes = remainingTicks / 1200;
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
                float healAmount = heartyMeal ? 28.0f : 10.0f; // hearty meal heals more
                float oldHealth = wyvern.getHealth();
                float newHealth = Math.min(oldHealth + healAmount, wyvern.getMaxHealth());
                wyvern.setHealth(newHealth);

                // Slight taming bump on hearty meal even when already tamed is harmless; skip here.

                wyvern.level().broadcastEntityEvent(wyvern, (byte) 6); // Eating sound
                wyvern.level().broadcastEntityEvent(wyvern, (byte) 7); // Hearts particles

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
                boolean standingUp = wyvern.isStandingUpAnimation();
                String messageKey = sittingDown
                    ? "entity.saintsdragons.raevyx.sitting_down"
                    : standingUp
                        ? "entity.saintsdragons.raevyx.standing_up"
                        : "entity.saintsdragons.raevyx.transitioning";

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
                // Let updateSittingProgress() handle the "up" animation transition naturally
                break;
            case 1: // Sit
                wyvern.setOrderedToSit(true);
                break;
            case 2: // Wander
                wyvern.setOrderedToSit(false);
                // Let updateSittingProgress() handle the "up" animation transition naturally
                break;
        }
    }

    private Float nextFailureHealTarget() {
        // Always heal all the way back to max health (180 HP) after each failed attempt
        return wyvern.getMaxHealth();
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
     */
    private void triggerTamingAdvancement(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var advancement = serverPlayer.server.getAdvancements()
                .getAdvancement(SaintsDragonsCommon.rl("tame_raevyx"));
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "tame_raevyx");
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

    private boolean isInteractionItem(ItemStack itemstack) {
        return itemstack.is(ModItems.RAEVYX_BINDER.get())
                || itemstack.is(ModItems.DRAGON_ALLY_BOOK.get());
    }
}
