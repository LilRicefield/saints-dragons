package com.leon.saintsdragons.server.entity.dragons.raevyx.handlers;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.dragons.handlers.AbstractDragonInteractionHandler;
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
public class RaevyxInteractionHandler extends AbstractDragonInteractionHandler<Raevyx> {
    public RaevyxInteractionHandler(Raevyx dragon) {
        super(dragon);
    }
    
    /**
     * Main interaction entry point.
     * Delegates to specific handlers based on dragon state and interaction type.
     */
    @Override
    protected InteractionResult handleUntamedInteraction(Player player, InteractionHand hand, ItemStack itemstack) {
        boolean client = dragon.level().isClientSide;

        // Check if legacy taming is enabled
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        boolean legacyTaming = config.extraBoolean("legacy_taming", false);

        if (dragon.isBaby()) {
            return handleBabyTaming(player, itemstack, config);
        }

        // Allow players to abort a taming attempt by crouching with empty hands (only in normal mode)
        if (!legacyTaming && dragon.isTamingStunned() && player.isCrouching() && itemstack.isEmpty()) {
            if (!client) {
                dragon.abortTamingAttempt();
                sendStatusMessage(player, "entity.saintsdragons.raevyx.taming_aborted");
            }
            return InteractionResult.sidedSuccess(client);
        }

        if (!dragon.isFood(itemstack)) {
            return InteractionResult.PASS;
        }

        if (!legacyTaming) {
            // Normal mode: check taming stun state
            if (dragon.isTamingStunned()) {
                if (!dragon.isAwaitingTamingFeed()) {
                    sendStatusMessage(player, "entity.saintsdragons.raevyx.taming_dazed");
                    return InteractionResult.CONSUME;
                }
            }
        }

        // Check feeding cooldown to prevent spam-feeding
        if (!dragon.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                    Component.translatable("entity.saintsdragons.raevyx.still_eating", dragon.getName()),
                    true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!legacyTaming) {
            // Normal mode: require low health
            float minRequiredHealth = dragon.getTamingThreshold();
            // Add 1.0 HP buffer to prevent edge cases (e.g., small regeneration between ticks)
            if (dragon.getHealth() > minRequiredHealth + 1.0F) {
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
            dragon.triggerAnim("action", "eat");
            dragon.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_EAT.get(), 1.0f, dragon.isBaby() ? 1.6f : 1.0f, 56);

            // Set feeding cooldown (3.0417 seconds * 20 ticks/second = 61 ticks)
            dragon.setFeedingCooldown(61);

            boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            if (hearty) {
                dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }
            dragon.applyFeedingHunger(hearty);

            // Legacy taming: heal the dragon instead of entering stun
            if (legacyTaming) {
                float healAmount = hearty ? 28.0f : 10.0f;
                float newHealth = Math.min(dragon.getHealth() + healAmount, dragon.getMaxHealth());
                dragon.setHealth(newHealth);
            } else {
                // Normal mode: enter taming stun
                dragon.enterTamingStun();
            }

            double tameChance = hearty
                ? config.extraDoubles().getOrDefault("taming_chance_hearty", 3.0)
                : config.extraDoubles().getOrDefault("taming_chance_base", 5.0);
            int tameRoll = (int) Math.round(tameChance);
            boolean success = dragon.getRandom().nextInt(Math.max(1, tameRoll)) == 0;

            if (success) {
                dragon.tame(player);
                dragon.setOrderedToSit(true);
                dragon.setCommandManual(1); // Set command to Sit (1) to match the sitting state
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
                if (!legacyTaming) {
                    dragon.resetTamingFailures();
                    dragon.clearTamingRecovery();
                }

                // Trigger advancement for taming Lightning Dragon
                triggerTamingAdvancement(player);
            } else {
                if (!legacyTaming) {
                    Float healTarget = nextFailureHealTarget();
                    dragon.setTamingRecoveryTarget(healTarget);
                    dragon.incrementTamingFailures();
                }
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
                sendStatusMessage(player, "entity.saintsdragons.raevyx.taming_failed");
            }
        }

        return InteractionResult.sidedSuccess(client);
    }

    private InteractionResult handleBabyTaming(Player player, ItemStack itemstack, DragonAttributeConfig config) {
        boolean client = dragon.level().isClientSide;
        boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
        if (!dragon.isFood(itemstack) && !itemstack.is(net.minecraft.world.item.Items.SALMON) && !hearty) {
            return InteractionResult.PASS;
        }

        // Check feeding cooldown to prevent spam-feeding
        if (!dragon.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                    Component.translatable("entity.saintsdragons.raevyx.still_eating", dragon.getName()),
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
            dragon.triggerAnim("action", "eat");
            dragon.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_EAT.get(), 1.0f, dragon.isBaby() ? 1.6f : 1.0f, 56);

            // Set feeding cooldown (3.0417 seconds * 20 ticks/second = 61 ticks)
            dragon.setFeedingCooldown(61);

            if (hearty) {
                dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }
            dragon.applyFeedingHunger(hearty);

            double tameChance = hearty
                ? config.extraDoubles().getOrDefault("taming_chance_hearty", 3.0)
                : config.extraDoubles().getOrDefault("taming_chance_base", 5.0);
            int tameRoll = (int) Math.round(tameChance);
            boolean success = dragon.getRandom().nextInt(Math.max(1, tameRoll)) == 0;

            if (success) {
                dragon.tame(player);
                dragon.setOrderedToSit(true);
                dragon.setCommandManual(1);
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
                triggerTamingAdvancement(player);
            } else {
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
            }
        }

        return InteractionResult.sidedSuccess(client);
    }
    
    /**
     * Handle interactions with tamed dragons (feeding, commands, mounting).
     */
    @Override
    protected InteractionResult handleTamedInteraction(Player player, InteractionHand hand, ItemStack itemstack) {
        boolean isOwner = player.equals(dragon.getOwner());

        // Handle feeding for healing
        if (dragon.isFood(itemstack)) {
            if (player.isCrouching() && isOwner) {
                return handleBreeding(player, itemstack);
            }
            return handleFeeding(player, itemstack);
        }
        
        // Handle owner commands and mounting
        if (isOwner) {
            boolean isSleeping = dragon.isSleeping() || dragon.isSleepTransitioning();
            // Command cycling - Shift+Right-click cycles through commands
            if (canOwnerCommand(player) && !dragon.isFood(itemstack) && hand == InteractionHand.MAIN_HAND) {
                if (isSleeping) {
                    if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.displayClientMessage(
                            Component.translatable("entity.saintsdragons.raevyx.sleeping", dragon.getName()),
                            true
                        );
                    }
                    return InteractionResult.sidedSuccess(dragon.level().isClientSide);
                }
                return handleCommandCycling(player);
            }
            // Mounting - Right-click without shift (allow any non-food item)
            else if (!player.isCrouching() && !dragon.isFood(itemstack) && hand == InteractionHand.MAIN_HAND && canOwnerMount(player)) {
                return handleMounting(player);
            }
        }
        
        return InteractionResult.PASS;
    }
    
    /**
     * Handle initiating breeding when crouching with food.
     */
    private InteractionResult handleBreeding(Player player, ItemStack itemstack) {
        boolean client = dragon.level().isClientSide;

        // Check feeding cooldown to prevent spam-feeding
        if (!dragon.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                    Component.translatable("entity.saintsdragons.raevyx.still_eating", dragon.getName()),
                    true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (dragon.isBaby()) {
            sendStatusMessage(player, "entity.saintsdragons.raevyx.breeding_too_young");
            return InteractionResult.sidedSuccess(client);
        }

        if (dragon.getAge() != 0) { // still on cooldown from previous breeding
            sendStatusMessage(player, "entity.saintsdragons.raevyx.breeding_cooling_down");
            return InteractionResult.sidedSuccess(client);
        }

        if (dragon.isInLove()) {
            sendStatusMessage(player, "entity.saintsdragons.raevyx.breeding_already_ready");
            return InteractionResult.sidedSuccess(client);
        }

        if (!client) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            // Trigger eat animation
            dragon.triggerAnim("action", "eat");
            dragon.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_EAT.get(), 1.0f, dragon.isBaby() ? 1.6f : 1.0f, 56);

            // Set feeding cooldown (3.0417 seconds * 20 ticks/second = 61 ticks)
            dragon.setFeedingCooldown(61);

            dragon.setInLove(player);
            sendStatusMessage(player, "entity.saintsdragons.raevyx.breeding_ready");
        }

        return InteractionResult.sidedSuccess(client);
    }
    
    /**
     * Handle feeding tamed dragons for healing or growth.
     */
    private InteractionResult handleFeeding(Player player, ItemStack itemstack) {
        // Check feeding cooldown to prevent spam-feeding
        if (!dragon.canFeed()) {
            if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                    Component.translatable("entity.saintsdragons.raevyx.still_eating", dragon.getName()),
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
            dragon.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_EAT.get(), 1.0f, dragon.isBaby() ? 1.6f : 1.0f, 56);

            // Set feeding cooldown (3.0417 seconds * 20 ticks/second = 61 ticks)
            dragon.setFeedingCooldown(22);

            boolean heartyMeal = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            if (heartyMeal) {
                dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }
            boolean wasHungry = dragon.isHungry();

            // Babies: speed up growth instead of healing
            if (dragon.isBaby()) {
                int growthTicks = heartyMeal ? 4800 : 2400; // hearty meal doubles growth bonus
                int currentAge = dragon.getAge();
                int newAge = Math.min(0, currentAge + growthTicks);
                dragon.setAge(newAge);

                dragon.level().broadcastEntityEvent(dragon, (byte) 6); // Eating sound
                dragon.level().broadcastEntityEvent(dragon, (byte) 7); // Hearts particles

                if (player instanceof ServerPlayer serverPlayer) {
                    int remainingTicks = Math.abs(newAge);
                    int remainingMinutes = remainingTicks / 1200;
                    String messageKey = (newAge == 0)
                        ? "entity.saintsdragons.raevyx.baby_grown"
                        : "entity.saintsdragons.raevyx.baby_fed";
                    serverPlayer.displayClientMessage(
                        Component.translatable(messageKey, dragon.getName()),
                        true
                    );
                }
                dragon.applyFeedingHunger(heartyMeal);
            } else {
                // Adults: heal when fed
                float healAmount = heartyMeal ? 28.0f : 10.0f; // hearty meal heals more
                float oldHealth = dragon.getHealth();
                float newHealth = Math.min(oldHealth + healAmount, dragon.getMaxHealth());
                dragon.setHealth(newHealth);

                // Slight taming bump on hearty meal even when already tamed is harmless; skip here.

                dragon.level().broadcastEntityEvent(dragon, (byte) 6); // Eating sound
                dragon.level().broadcastEntityEvent(dragon, (byte) 7); // Hearts particles

                dragon.applyFeedingHunger(heartyMeal);
                sendFeedingMessage(player, newHealth, wasHungry);
            }
        }

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }
    
    /**
     * Handle command cycling (Follow/Sit/Wander).
     */
    private InteractionResult handleCommandCycling(Player player) {
        // Prevent command changes during sit transitions (sitting down or standing up)
        boolean isTransitioning = dragon.isInSitTransition();

        if (isTransitioning) {
            // Dragon is in the middle of sitting down or standing up - ignore command spam
            if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                // Determine which transition is happening
                boolean sittingDown = dragon.isSittingDownAnimation();
                boolean standingUp = dragon.isStandingUpAnimation();
                String messageKey = sittingDown
                    ? "entity.saintsdragons.raevyx.sitting_down"
                    : standingUp
                        ? "entity.saintsdragons.raevyx.standing_up"
                        : "entity.saintsdragons.raevyx.transitioning";

                serverPlayer.displayClientMessage(
                    Component.translatable(messageKey, dragon.getName()),
                    true
                );
            }
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }

        // Get current command and cycle to next
        int currentCommand = dragon.getCommand();
        int nextCommand = (currentCommand + 1) % 3; // 0=Follow, 1=Sit, 2=Wander

        // Apply the new command
        dragon.setCommandManual(nextCommand);
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
                // Let updateSittingProgress() handle the "up" animation transition naturally
                break;
            case 1: // Sit
                dragon.setOrderedToSit(true);
                break;
            case 2: // Wander
                dragon.setOrderedToSit(false);
                // Let updateSittingProgress() handle the "up" animation transition naturally
                break;
        }
    }

    private Float nextFailureHealTarget() {
        // Always heal all the way back to max health (180 HP) after each failed attempt
        return dragon.getMaxHealth();
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
            dragon.suppressSleep(300);
        }
        
        // Clear all combat and AI states when mounting
        dragon.clearAllStatesForMounting();
        
        // Start riding only on server to avoid client/server mount desync.
        if (!dragon.level().isClientSide && player.startRiding(dragon)) {
            // Play excited sound when mounting
            dragon.playExcitedSound();
        }

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
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
    private void sendFeedingMessage(Player player, float newHealth, boolean wasHungry) {
        if (player instanceof ServerPlayer serverPlayer) {
            String messageKey;
            if (newHealth >= dragon.getMaxHealth()) {
                messageKey = wasHungry ? "entity.saintsdragons.dragon.feeding" : "entity.saintsdragons.raevyx.fed";
            } else {
                messageKey = "entity.saintsdragons.raevyx.fed_partial";
            }
                
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

    @Override
    protected net.minecraft.world.item.Item getBinderItem() {
        return ModItems.RAEVYX_BINDER.get();
    }
}

