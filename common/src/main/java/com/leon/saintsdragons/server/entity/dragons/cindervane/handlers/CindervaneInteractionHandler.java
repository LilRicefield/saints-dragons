package com.leon.saintsdragons.server.entity.dragons.cindervane.handlers;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Handles all interactions with Amphithere entities
 * Adapted from Lightning Dragon interaction handler
 */
public class CindervaneInteractionHandler {
    private final Cindervane dragon;

    public CindervaneInteractionHandler(Cindervane dragon) {
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
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.CINDERVANE_ID);

        if (dragon.isBaby()) {
            return handleBabyTaming(player, heldItem, config);
        }

        if (!dragon.isFood(heldItem)) {
            return InteractionResult.PASS;
        }

        // Check feeding cooldown to prevent spam-feeding
        if (!dragon.canFeed()) {
            if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                    Component.translatable("entity.saintsdragons.cindervane.still_eating", dragon.getName()),
                    true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!dragon.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                heldItem.shrink(1);
            }

            // Trigger eat animation
            dragon.triggerAnim("actions", "eat");

            // Set feeding cooldown (2.2083 seconds * 20 ticks/second = 44 ticks)
            dragon.setFeedingCooldown(44);

            boolean hearty = heldItem.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());

            double tameChance = hearty
                ? config.extraDoubles().getOrDefault("taming_chance_hearty", 2.0)
                : config.extraDoubles().getOrDefault("taming_chance_base", 4.0);
            int tameRoll = (int) Math.round(tameChance);

            if (hearty) {
                dragon.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 200, 1));
            }

            if (dragon.getRandom().nextInt(Math.max(1, tameRoll)) == 0) {
                dragon.tame(player);
                dragon.getNavigation().stop();
                dragon.setOrderedToSit(true);
                dragon.setCommand(1); // Set command to Sit (1) to match the sitting state
                dragon.setTarget(null);
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);

                triggerTamingAdvancement(player);
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

        if (isInteractionItem(heldItem)) {
            return InteractionResult.PASS;
        }

        // Owner-only interactions
        if (isOwner) {
            if (player.isCrouching() && dragon.isFood(heldItem)) {
                return handleBreeding(player, heldItem);
            }

            // Handle feeding for healing or growth
            if (dragon.isFood(heldItem)) {
                return handleFeeding(player, heldItem);
            }

            // Handle owner commands - Shift+Right-click cycles through commands
            if (dragon.canOwnerCommand(player) && !dragon.isFood(heldItem) && hand == InteractionHand.MAIN_HAND) {
                return handleCommandCycling(player);
            }
        }

        // Handle mounting - both owner and non-owners can mount
        if (hand == InteractionHand.MAIN_HAND && !dragon.isFood(heldItem) && !player.isCrouching()) {
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
                        Component.translatable("entity.saintsdragons.cindervane.mount_occupied"),
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
                        Component.translatable("entity.saintsdragons.cindervane.passenger_needs_owner"),
                        true
                    );
                }
                return InteractionResult.FAIL;
            }

            if (passengers.size() >= 2) {
                // Both seats are full
                if (!dragon.level().isClientSide) {
                    player.displayClientMessage(
                        Component.translatable("entity.saintsdragons.cindervane.seats_full"),
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
                        Component.translatable("entity.saintsdragons.cindervane.passenger_needs_owner"),
                        true
                    );
                }
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleBreeding(Player player, ItemStack itemstack) {
        boolean client = dragon.level().isClientSide;

        if (!dragon.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("entity.saintsdragons.cindervane.still_eating", dragon.getName()),
                        true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (dragon.isBaby()) {
            sendStatusMessage(player, "entity.saintsdragons.cindervane.breeding_too_young");
            return InteractionResult.sidedSuccess(client);
        }

        if (dragon.getAge() != 0) {
            sendStatusMessage(player, "entity.saintsdragons.cindervane.breeding_cooling_down");
            return InteractionResult.sidedSuccess(client);
        }

        if (dragon.isInLove()) {
            sendStatusMessage(player, "entity.saintsdragons.cindervane.breeding_already_ready");
            return InteractionResult.sidedSuccess(client);
        }

        if (!client) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            dragon.triggerAnim("actions", "eat");
            dragon.setFeedingCooldown(61);
            dragon.setInLove(player);
            sendStatusMessage(player, "entity.saintsdragons.cindervane.breeding_ready");
        }

        return InteractionResult.sidedSuccess(client);
    }

    /**
     * Handle feeding the dragon for healing
     */
    private InteractionResult handleFeeding(Player player, ItemStack food) {
        // Check feeding cooldown to prevent spam-feeding
        if (!dragon.canFeed()) {
            if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                    Component.translatable("entity.saintsdragons.cindervane.still_eating", dragon.getName()),
                    true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!dragon.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                food.shrink(1);
            }

            // Trigger eat animation
            dragon.triggerAnim("actions", "eat");

            // Set feeding cooldown (2.2083 seconds * 20 ticks/second = 44 ticks)
            dragon.setFeedingCooldown(44);

            boolean hearty = food.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            if (dragon.isBaby()) {
                int growthTicks = hearty ? 4800 : 2400;
                int currentAge = dragon.getAge();
                int newAge = Math.min(0, currentAge + growthTicks);
                dragon.setAge(newAge);

                dragon.level().broadcastEntityEvent(dragon, (byte) 7);

                if (player instanceof ServerPlayer serverPlayer) {
                    String messageKey = (newAge == 0)
                            ? "entity.saintsdragons.cindervane.baby_grown"
                            : "entity.saintsdragons.cindervane.baby_fed";
                    serverPlayer.displayClientMessage(
                            Component.translatable(messageKey, dragon.getName()),
                            true
                    );
                }
            } else {
                float healAmount = hearty ? 15.0F : 5.0F;
                float newHealth = Math.min(dragon.getHealth() + healAmount, dragon.getMaxHealth());
                boolean fullyHealed = newHealth >= dragon.getMaxHealth();

                dragon.heal(healAmount);
                if (hearty) {
                    dragon.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 200, 1));
                }
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);

                // Send appropriate message
                if (fullyHealed) {
                    player.displayClientMessage(
                        Component.translatable("entity.saintsdragons.cindervane.fed", dragon.getName()),
                        true
                    );
                } else {
                    player.displayClientMessage(
                        Component.translatable("entity.saintsdragons.cindervane.fed_partial", dragon.getName()),
                        true
                    );
                }
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.sidedSuccess(true);
    }

    /**
     * Handle command cycling (Follow/Sit/Wander).
     */
    private InteractionResult handleCommandCycling(Player player) {
        if (dragon.isInSitTransition()) {
            if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                String messageKey;
                if (dragon.isSittingDownAnimation()) {
                    messageKey = "entity.saintsdragons.cindervane.sitting_down";
                } else if (dragon.isStandingUpAnimation()) {
                    messageKey = "entity.saintsdragons.cindervane.standing_up";
                } else {
                    messageKey = "entity.saintsdragons.cindervane.transitioning";
                }
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

    private InteractionResult handleBabyTaming(Player player, ItemStack itemstack, DragonAttributeConfig config) {
        boolean client = dragon.level().isClientSide;
        boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
        if (!dragon.isFood(itemstack) && !itemstack.is(net.minecraft.world.item.Items.SALMON) && !hearty) {
            return InteractionResult.PASS;
        }

        if (!dragon.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                    Component.translatable("entity.saintsdragons.cindervane.still_eating", dragon.getName()),
                    true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!client) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            dragon.triggerAnim("actions", "eat");
            dragon.setFeedingCooldown(44);

            if (hearty) {
                dragon.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 200, 1));
            }

            double tameChance = hearty
                ? config.extraDoubles().getOrDefault("taming_chance_hearty", 2.0)
                : config.extraDoubles().getOrDefault("taming_chance_base", 4.0);
            int tameRoll = (int) Math.round(tameChance);

            if (dragon.getRandom().nextInt(Math.max(1, tameRoll)) == 0) {
                dragon.tame(player);
                dragon.getNavigation().stop();
                dragon.setOrderedToSit(true);
                dragon.setCommand(1);
                dragon.setTarget(null);
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);

                triggerTamingAdvancement(player);
            } else {
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
            }
        }

        return InteractionResult.sidedSuccess(client);
    }

    private void sendStatusMessage(Player player, String key) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable(key, dragon.getName()), true);
        }
    }

    private boolean isInteractionItem(ItemStack itemstack) {
        return itemstack.is(ModItems.CINDERVANE_BINDER.get())
                || itemstack.is(ModItems.DRAGON_ALLY_BOOK.get());
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

    private void triggerTamingAdvancement(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var advancement = serverPlayer.server.getAdvancements()
                    .getAdvancement(SaintsDragonsCommon.rl("tame_cindervane"));
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "tame_cindervane");
            }
        }
    }
}
