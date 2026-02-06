package com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.handlers.AbstractDragonInteractionHandler;
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
public class IgnivorusInteractionHandler extends AbstractDragonInteractionHandler<Ignivorus> {
    public IgnivorusInteractionHandler(Ignivorus dragon) {
        super(dragon);
    }

    /**
     * Handle interactions with untamed dragons (taming attempts).
     */
    @Override
    protected InteractionResult handleUntamedInteraction(Player player, InteractionHand hand, ItemStack itemstack) {
        boolean client = dragon.level().isClientSide;

        // Check if legacy taming is enabled
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        boolean legacyTaming = config.extraBoolean("legacy_taming", false);

        if (dragon.isBaby()) {
            return handleBabyTaming(player, itemstack, config);
        }

        // Allow players to abort a taming attempt by crouching with empty hands (only in normal mode)
        if (!legacyTaming && dragon.isTamingStunned() && player.isCrouching() && itemstack.isEmpty()) {
            if (!client) {
                dragon.abortTamingAttempt();
                sendStatusMessage(player, "entity.saintsdragons.ignivorus.taming_aborted");
            }
            return InteractionResult.sidedSuccess(client);
        }

        if (!isIgnivorusFood(itemstack)) {
            return InteractionResult.PASS;
        }

        if (!legacyTaming) {
            // Normal mode: check taming stun state
            if (dragon.isTamingStunned()) {
                if (!dragon.isAwaitingTamingFeed()) {
                    sendStatusMessage(player, "entity.saintsdragons.ignivorus.taming_dazed");
                    return InteractionResult.CONSUME;
                }
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

        if (!legacyTaming) {
            // Normal mode: require low health
            float minRequiredHealth = dragon.getTamingThreshold();
            // Add 1.0 HP buffer to prevent edge cases (e.g., small regeneration between ticks)
            if (dragon.getHealth() > minRequiredHealth + 1.0F) {
                sendStatusMessage(player, "entity.saintsdragons.ignivorus.taming_need_weakened");
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

            // Set feeding cooldown
            dragon.setFeedingCooldown(20);

            boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            boolean beef = itemstack.is(net.minecraft.world.item.Items.BEEF);
            if (hearty) {
                dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }
            dragon.applyFeedingHunger(hearty);

            // Legacy taming: heal the dragon instead of entering stun
            if (legacyTaming) {
                float healAmount = hearty ? 30.0f : (beef ? 16.0f : 10.0f);
                float newHealth = Math.min(dragon.getHealth() + healAmount, dragon.getMaxHealth());
                dragon.setHealth(newHealth);
            } else {
                // Normal mode: enter taming stun
                dragon.enterTamingStun();
            }

            double tameChance = hearty
                ? config.extraDoubles().getOrDefault("taming_chance_hearty", 4.0)
                : beef
                    ? config.extraDoubles().getOrDefault("taming_chance_beef", 5.0)
                    : config.extraDoubles().getOrDefault("taming_chance_base", 7.0);
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

                // Trigger advancement for taming Ignivorus
                triggerTamingAdvancement(player);
            } else {
                if (!legacyTaming) {
                    Float healTarget = nextFailureHealTarget();
                    dragon.setTamingRecoveryTarget(healTarget);
                    dragon.incrementTamingFailures();
                }
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
                sendStatusMessage(player, "entity.saintsdragons.ignivorus.taming_failed");
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

        // Handle owner commands and mounting
        if (isOwner) {
            // Breeding - Shift+Right-click with food
            if (player.isCrouching() && isIgnivorusFood(itemstack)) {
                return handleBreeding(player, itemstack);
            }
            // Command cycling - Shift+Right-click cycles through commands
            if (player.isCrouching() && !isIgnivorusFood(itemstack) && hand == InteractionHand.MAIN_HAND) {
                return handleCommandCycling(player);
            }
            // Mounting - Right-click without shift (allow any non-food item)
            else if (!player.isCrouching() && !isIgnivorusFood(itemstack) && hand == InteractionHand.MAIN_HAND) {
                return handleMounting(player);
            }
        }

        // Handle feeding for healing
        if (isIgnivorusFood(itemstack)) {
            return handleFeeding(player, itemstack);
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
                    Component.translatable("entity.saintsdragons.ignivorus.still_eating", dragon.getName()),
                    true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (dragon.isBaby()) {
            sendStatusMessage(player, "entity.saintsdragons.ignivorus.breeding_too_young");
            return InteractionResult.sidedSuccess(client);
        }

        if (dragon.getAge() != 0) { // still on cooldown from previous breeding
            sendStatusMessage(player, "entity.saintsdragons.ignivorus.breeding_cooling_down");
            return InteractionResult.sidedSuccess(client);
        }

        if (dragon.isInLove()) {
            sendStatusMessage(player, "entity.saintsdragons.ignivorus.breeding_already_ready");
            return InteractionResult.sidedSuccess(client);
        }

        if (!client) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            // Trigger eat animation
            dragon.triggerAnim("action", "eat");

            // Set feeding cooldown (3.0417 seconds * 20 ticks/second = 61 ticks)
            dragon.setFeedingCooldown(61);

            dragon.setInLove(player);
            sendStatusMessage(player, "entity.saintsdragons.ignivorus.breeding_ready");
        }

        return InteractionResult.sidedSuccess(client);
    }

    private InteractionResult handleBabyTaming(Player player, ItemStack itemstack, DragonAttributeConfig config) {
        boolean client = dragon.level().isClientSide;
        boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
        boolean cod = itemstack.is(net.minecraft.world.item.Items.COD);
        boolean salmon = itemstack.is(net.minecraft.world.item.Items.SALMON);
        boolean beef = itemstack.is(net.minecraft.world.item.Items.BEEF);
        if (!dragon.isFood(itemstack) && !cod && !salmon && !beef && !hearty) {
            return InteractionResult.PASS;
        }

        if (!dragon.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                    Component.translatable("entity.saintsdragons.ignivorus.still_eating", dragon.getName()),
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

            // Set feeding cooldown (3.0417 seconds * 20 ticks/second = 61 ticks)
            dragon.setFeedingCooldown(61);

            if (hearty) {
                dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }
            dragon.applyFeedingHunger(hearty);

            double tameChance = hearty
                ? config.extraDoubles().getOrDefault("taming_chance_hearty", 4.0)
                : beef
                    ? config.extraDoubles().getOrDefault("taming_chance_beef", 5.0)
                    : config.extraDoubles().getOrDefault("taming_chance_base", 7.0);
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
            dragon.setFeedingCooldown(23);

            boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            boolean beef = itemstack.is(net.minecraft.world.item.Items.BEEF);
            boolean wasHungry = dragon.isHungry();
            if (dragon.isBaby()) {
                int growthTicks = hearty ? 4800 : 2400;
                int currentAge = dragon.getAge();
                int newAge = Math.min(0, currentAge + growthTicks);
                dragon.setAge(newAge);

                dragon.level().broadcastEntityEvent(dragon, (byte) 7); // Hearts
                if (player instanceof ServerPlayer serverPlayer) {
                    String messageKey = (newAge == 0)
                        ? "entity.saintsdragons.ignivorus.baby_grown"
                        : "entity.saintsdragons.ignivorus.baby_fed";
                    serverPlayer.displayClientMessage(
                        Component.translatable(messageKey, dragon.getName()),
                        true
                    );
                }
                dragon.applyFeedingHunger(hearty);
            } else {
                float currentHealth = dragon.getHealth();
                float healAmount = hearty ? 30.0F : (beef ? 16.0F : 10.0F);
                float newHealth = Math.min(currentHealth + healAmount, dragon.getMaxHealth());
                dragon.setHealth(newHealth);
                dragon.applyFeedingHunger(hearty);
                if (hearty) {
                    dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                }

                dragon.level().broadcastEntityEvent(dragon, (byte) 7); // Hearts

                if (player instanceof ServerPlayer serverPlayer) {
                    String messageKey;
                    if (newHealth >= dragon.getMaxHealth()) {
                        messageKey = wasHungry ? "entity.saintsdragons.dragon.feeding" : "entity.saintsdragons.ignivorus.fed";
                    } else {
                        messageKey = "entity.saintsdragons.ignivorus.fed_partial";
                    }

                    serverPlayer.displayClientMessage(
                        Component.translatable(messageKey, dragon.getName()),
                        true
                    );
                }
            }
        }

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }

    /**
     * Handle command cycling (Follow/Sit/Wander).
     */
    private InteractionResult handleCommandCycling(Player player) {
        boolean isTransitioning = dragon.isInSitTransition();
        if (isTransitioning) {
            // Dragon is in the middle of sitting down or standing up - ignore command spam
            if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                // Determine which transition is happening
                boolean sittingDown = dragon.isSittingDownAnimation();
                boolean standingUp = dragon.isStandingUpAnimation();
                String messageKey = sittingDown
                        ? "entity.saintsdragons.ignivorus.sitting_down"
                        : standingUp
                        ? "entity.saintsdragons.ignivorus.standing_up"
                        : "entity.saintsdragons.ignivorus.transitioning";

                serverPlayer.displayClientMessage(
                        Component.translatable(messageKey, dragon.getName()),
                        true
                );
            }
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }
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
        if (!dragon.canOwnerMount(player) || dragon.isVehicle()) {
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }

        // Force the dragon to stand if sitting
        if (dragon.isOrderedToSit()) {
            dragon.setOrderedToSit(false);
        }
        if (dragon.getCommand() == 1) {
            dragon.setCommandManual(0);
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

    private void triggerTamingAdvancement(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var advancement = serverPlayer.server.getAdvancements()
                    .getAdvancement(SaintsDragonsCommon.rl("tame_ignivorus"));
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "tame_ignivorus");
            }
        }
    }

    @Override
    protected net.minecraft.world.item.Item getBinderItem() {
        return ModItems.IGNIVORUS_BINDER.get();
    }

    private boolean isIgnivorusFood(ItemStack itemstack) {
        return dragon.isFood(itemstack)
                || itemstack.is(net.minecraft.world.item.Items.SALMON)
                || itemstack.is(net.minecraft.world.item.Items.COD)
                || itemstack.is(net.minecraft.world.item.Items.BEEF)
                || itemstack.is(ModItems.HEARTY_DRAGON_MEAL.get());
    }
}
