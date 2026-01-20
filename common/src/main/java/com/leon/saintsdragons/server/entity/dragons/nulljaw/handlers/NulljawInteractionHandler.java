package com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Handles all player interactions with Rift Drakes.
 */
public record NulljawInteractionHandler(Nulljaw drake) {

    public InteractionResult handleInteraction(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (!drake.isTame()) {
            return handleUntamedInteraction(player, hand, heldItem);
        }
        return handleTamedInteraction(player, hand, heldItem);
    }

    private InteractionResult handleUntamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
        // Check if legacy taming is enabled
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.NULLJAW_ID);
        boolean legacyTaming = config.extraBoolean("legacy_taming", false);

        if (drake.isBaby()) {
            return handleBabyTaming(player, heldItem, config);
        }

        if (drake.isFood(heldItem)) {
            if (legacyTaming) {
                // Legacy taming: simple food-based taming with RNG
                return handleLegacyTaming(player, heldItem);
            } else if (drake.getHealth() < drake.getMaxHealth()) {
                // Normal mode: only allow feeding for healing, not taming
                return handleFeeding(player, heldItem, true);
            }
        }

        // Rodeo taming (only in non-legacy mode)
        if (!legacyTaming && hand == InteractionHand.MAIN_HAND && heldItem.isEmpty() && !player.isCrouching()) {
            boolean started = drake.beginUntamedRide(player);
            return started ? InteractionResult.sidedSuccess(drake.level().isClientSide) : InteractionResult.PASS;
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleLegacyTaming(Player player, ItemStack food) {
        if (!drake.canFeed()) {
            if (!drake.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("entity.saintsdragons.nulljaw.still_eating", drake.getName()),
                        true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!drake.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                food.shrink(1);
            }

            drake.triggerAnim("action", "eat");
            drake.setFeedingCooldown(40);

            boolean heartyMeal = food.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            float healAmount = heartyMeal ? 35.0F : 5.0F;
            float newHealth = Math.min(drake.getHealth() + healAmount, drake.getMaxHealth());
            drake.setHealth(newHealth);

            if (heartyMeal) {
                drake.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }

            // Taming chance logic
            DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.NULLJAW_ID);
            double tameChance = heartyMeal
                    ? config.extraDoubles().getOrDefault("taming_chance", 6.0) / 2.0  // Hearty meal doubles chance
                    : config.extraDoubles().getOrDefault("taming_chance", 6.0);
            int tameRoll = (int) Math.round(tameChance);
            boolean success = drake.getRandom().nextInt(Math.max(1, tameRoll)) == 0;

            if (success) {
                drake.tame(player);
                drake.setOrderedToSit(true);
                drake.level().broadcastEntityEvent(drake, (byte) 7);  // Hearts
                drake.awardTamingAdvancement(player);
            } else {
                drake.level().broadcastEntityEvent(drake, (byte) 6);  // Smoke
            }
        }

        return InteractionResult.sidedSuccess(drake.level().isClientSide);
    }

    private InteractionResult handleTamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
        if (!drake.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }

        if (isInteractionItem(heldItem)) {
            return InteractionResult.PASS;
        }

        if (player.isCrouching() && drake.isFood(heldItem)) {
            return handleBreeding(player, heldItem);
        }

        if (drake.isFood(heldItem)) {
            return handleFeeding(player, heldItem, false);
        }

        if (drake.canOwnerCommand(player) && !drake.isFood(heldItem) && hand == InteractionHand.MAIN_HAND) {
            return handleCommandCycling(player);
        }

        if (hand == InteractionHand.MAIN_HAND && !drake.isFood(heldItem) && !player.isCrouching()) {
            return handleMounting(player);
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleBreeding(Player player, ItemStack food) {
        boolean client = drake.level().isClientSide;

        if (!drake.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("entity.saintsdragons.nulljaw.still_eating", drake.getName()),
                        true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (drake.isBaby()) {
            sendStatusMessage(player, "entity.saintsdragons.nulljaw.breeding_too_young");
            return InteractionResult.sidedSuccess(client);
        }

        if (drake.getAge() != 0) {
            sendStatusMessage(player, "entity.saintsdragons.nulljaw.breeding_cooling_down");
            return InteractionResult.sidedSuccess(client);
        }

        if (drake.isInLove()) {
            sendStatusMessage(player, "entity.saintsdragons.nulljaw.breeding_already_ready");
            return InteractionResult.sidedSuccess(client);
        }

        if (!client) {
            if (!player.getAbilities().instabuild) {
                food.shrink(1);
            }

            drake.triggerAnim("action", "eat");
            drake.setFeedingCooldown(61);
            drake.setInLove(player);
            sendStatusMessage(player, "entity.saintsdragons.nulljaw.breeding_ready");
        }

        return InteractionResult.sidedSuccess(client);
    }

    private InteractionResult handleFeeding(Player player, ItemStack food, boolean untamed) {
        if (!drake.canFeed()) {
            if (!drake.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("entity.saintsdragons.nulljaw.still_eating", drake.getName()),
                        true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!drake.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                food.shrink(1);
            }

            drake.triggerAnim("action", "eat");
            drake.setFeedingCooldown(50);

            boolean heartyMeal = food.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            if (drake.isBaby()) {
                int growthTicks = heartyMeal ? 4800 : 2400;
                int currentAge = drake.getAge();
                int newAge = Math.min(0, currentAge + growthTicks);
                drake.setAge(newAge);

                drake.level().broadcastEntityEvent(drake, (byte) 7);

                if (player instanceof ServerPlayer serverPlayer) {
                    String messageKey = (newAge == 0)
                            ? "entity.saintsdragons.nulljaw.baby_grown"
                            : "entity.saintsdragons.nulljaw.baby_fed";
                    serverPlayer.displayClientMessage(
                            Component.translatable(messageKey, drake.getName()),
                            true
                    );
                }
            } else {
                float healAmount;
                if (heartyMeal) {
                    healAmount = 35.0F;
                } else {
                    healAmount = untamed ? 5.0F : 10.0F;
                }

                float newHealth = Math.min(drake.getHealth() + healAmount, drake.getMaxHealth());
                drake.setHealth(newHealth);

                if (heartyMeal) {
                    drake.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                }

                drake.level().broadcastEntityEvent(drake, (byte) 7);

                if (!drake.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                    boolean fullyHealed = newHealth >= drake.getMaxHealth();
                    serverPlayer.displayClientMessage(
                            Component.translatable(
                                    fullyHealed ? "entity.saintsdragons.nulljaw.fed" : "entity.saintsdragons.nulljaw.fed_partial",
                                    drake.getName()
                            ),
                            true
                    );
                }
            }
        }

        return InteractionResult.sidedSuccess(drake.level().isClientSide);
    }

    private InteractionResult handleBabyTaming(Player player, ItemStack food, DragonAttributeConfig config) {
        boolean client = drake.level().isClientSide;
        boolean heartyMeal = food.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
        if (!drake.isFood(food) && !food.is(net.minecraft.world.item.Items.SALMON) && !heartyMeal) {
            return InteractionResult.PASS;
        }

        if (!drake.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("entity.saintsdragons.nulljaw.still_eating", drake.getName()),
                        true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!client) {
            if (!player.getAbilities().instabuild) {
                food.shrink(1);
            }

            drake.triggerAnim("action", "eat");
            drake.setFeedingCooldown(50);

            if (heartyMeal) {
                drake.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }

            double tameChance = heartyMeal
                    ? config.extraDoubles().getOrDefault("taming_chance", 6.0) / 2.0
                    : config.extraDoubles().getOrDefault("taming_chance", 6.0);
            int tameRoll = (int) Math.round(tameChance);
            boolean success = drake.getRandom().nextInt(Math.max(1, tameRoll)) == 0;

            if (success) {
                drake.tame(player);
                drake.setOrderedToSit(true);
                drake.level().broadcastEntityEvent(drake, (byte) 7);
                drake.awardTamingAdvancement(player);
            } else {
                drake.level().broadcastEntityEvent(drake, (byte) 6);
            }
        }

        return InteractionResult.sidedSuccess(client);
    }

    private InteractionResult handleMounting(Player player) {
        if (!drake.canOwnerMount(player) || drake.isVehicle()) {
            return InteractionResult.PASS;
        }

        if (!drake.level().isClientSide) {
            if (drake.isOrderedToSit()) {
                drake.setOrderedToSit(false);
            }
            drake.setTarget(null);
            drake.getNavigation().stop();
            player.startRiding(drake);
        }

        return InteractionResult.sidedSuccess(drake.level().isClientSide);
    }

    private InteractionResult handleCommandCycling(Player player) {
        if (drake.isInSitTransition()) {
            if (!drake.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                String messageKey;
                if (drake.isSittingDownAnimation()) {
                    messageKey = "entity.saintsdragons.nulljaw.sitting_down";
                } else if (drake.isStandingUpAnimation()) {
                    messageKey = "entity.saintsdragons.nulljaw.standing_up";
                } else {
                    messageKey = "entity.saintsdragons.nulljaw.transitioning";
                }
                serverPlayer.displayClientMessage(
                        Component.translatable(messageKey, drake.getName()),
                        true
                );
            }
            return InteractionResult.sidedSuccess(drake.level().isClientSide);
        }

        int currentCommand = drake.getCommand();
        int nextCommand = (currentCommand + 1) % 3;
        drake.setCommand(nextCommand);
        applyCommandState(nextCommand);

        if (!drake.level().isClientSide) {
            player.displayClientMessage(
                    Component.translatable("entity.saintsdragons.all.command_" + nextCommand, drake.getName()),
                    true
            );
        }
        return InteractionResult.SUCCESS;
    }

    private void applyCommandState(int command) {
        switch (command) {
            case 0 -> drake.setOrderedToSit(false); // Follow
            case 1 -> drake.setOrderedToSit(true);  // Sit
            case 2 -> drake.setOrderedToSit(false); // Wander
            default -> {
            }
        }
    }

    private void sendStatusMessage(Player player, String key) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable(key, drake.getName()), true);
        }
    }

    private boolean isInteractionItem(ItemStack itemstack) {
        return itemstack.is(ModItems.NULLJAW_BINDER.get())
                || itemstack.is(ModItems.DRAGON_ALLY_BOOK.get());
    }
}
