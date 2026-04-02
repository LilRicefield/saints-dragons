package com.leon.saintsdragons.server.entity.dragons.volitans.handlers;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class VolitansInteractionHandler {
    private final Volitans dragon;

    public VolitansInteractionHandler(Volitans dragon) {
        this.dragon = dragon;
    }

    public InteractionResult handleInteraction(Player player, InteractionHand hand) {
        if (dragon.isDying()) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (ModItems.isDragonBrush(heldItem)) {
            if (!dragon.level().isClientSide) {
                dragon.tryBrush(player, heldItem);
            }
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }

        if (heldItem.is(ModItems.DRACONIC_CODEX.get()) || heldItem.is(ModItems.VOLITANS_BINDER.get())) {
            return InteractionResult.PASS;
        }

        if (!dragon.isTame() || !dragon.isOwnedBy(player) || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (player.isCrouching() && isVolitansFood(heldItem)) {
            return handleBreeding(player, heldItem);
        }

        if (isVolitansFood(heldItem)) {
            return handleFeeding(player, heldItem);
        }

        if (player.isCrouching() && heldItem.isEmpty() && dragon.canOwnerCommand(player)) {
            return handleCommandCycling(player);
        }

        if (!player.isCrouching() && heldItem.isEmpty() && !dragon.isVehicle() && dragon.canOwnerMount(player)) {
            if (!dragon.level().isClientSide) {
                if (dragon.isOrderedToSit()) {
                    dragon.setOrderedToSit(false);
                }
                if (dragon.getCommand() == 1) {
                    dragon.setCommand(0);
                }
                dragon.setTarget(null);
                dragon.getNavigation().stop();
                player.startRiding(dragon);
            }
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleBreeding(Player player, ItemStack food) {
        boolean client = dragon.level().isClientSide;
        if (!dragon.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("entity.saintsdragons.volitans.still_eating", dragon.getName()),
                        true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (dragon.isBaby()) {
            sendStatusMessage(player, "entity.saintsdragons.volitans.breeding_too_young");
            return InteractionResult.sidedSuccess(client);
        }

        if (dragon.getAge() != 0) {
            sendStatusMessage(player, "entity.saintsdragons.volitans.breeding_cooling_down");
            return InteractionResult.sidedSuccess(client);
        }

        if (dragon.isInLove()) {
            sendStatusMessage(player, "entity.saintsdragons.volitans.breeding_already_ready");
            return InteractionResult.sidedSuccess(client);
        }

        if (!client) {
            consumeItem(player, food);
            dragon.setFeedingCooldown(61);
            dragon.setInLove(player);
            playEatFeedback(food);
            sendStatusMessage(player, "entity.saintsdragons.volitans.breeding_ready");
        }
        return InteractionResult.sidedSuccess(client);
    }

    private InteractionResult handleFeeding(Player player, ItemStack food) {
        if (!dragon.canFeed()) {
            if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("entity.saintsdragons.volitans.still_eating", dragon.getName()),
                        true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!dragon.level().isClientSide) {
            consumeItem(player, food);
            dragon.setFeedingCooldown(24);
            playEatFeedback(food);

            boolean heartyMeal = food.is(ModItems.HEARTY_DRAGON_MEAL.get());
            boolean wasHungry = dragon.isHungry();

            if (dragon.isBaby()) {
                int growthTicks = heartyMeal ? 4800 : 2400;
                int newAge = Math.min(0, dragon.getAge() + growthTicks);
                dragon.setAge(newAge);
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);

                if (player instanceof ServerPlayer serverPlayer) {
                    String messageKey = newAge == 0
                            ? "entity.saintsdragons.volitans.baby_grown"
                            : "entity.saintsdragons.volitans.baby_fed";
                    serverPlayer.displayClientMessage(Component.translatable(messageKey, dragon.getName()), true);
                }
                dragon.applyFeedingHunger(heartyMeal);
            } else {
                float healAmount = heartyMeal ? 28.0F : 10.0F;
                float newHealth = Math.min(dragon.getHealth() + healAmount, dragon.getMaxHealth());
                dragon.setHealth(newHealth);
                dragon.applyFeedingHunger(heartyMeal);
                if (heartyMeal) {
                    dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                }
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);

                if (player instanceof ServerPlayer serverPlayer) {
                    String messageKey = newHealth >= dragon.getMaxHealth()
                            ? (wasHungry ? "entity.saintsdragons.dragon.feeding" : "entity.saintsdragons.volitans.fed")
                            : "entity.saintsdragons.volitans.fed_partial";
                    serverPlayer.displayClientMessage(Component.translatable(messageKey, dragon.getName()), true);
                }
            }
        }

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }

    private InteractionResult handleCommandCycling(Player player) {
        if (dragon.isInSitTransition()) {
            if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                String messageKey;
                if (dragon.isSittingDownAnimation()) {
                    messageKey = "entity.saintsdragons.volitans.sitting_down";
                } else if (dragon.isStandingUpAnimation()) {
                    messageKey = "entity.saintsdragons.volitans.standing_up";
                } else {
                    messageKey = "entity.saintsdragons.volitans.transitioning";
                }
                serverPlayer.displayClientMessage(Component.translatable(messageKey, dragon.getName()), true);
            }
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }

        int currentCommand = dragon.getCommand();
        int nextCommand = (currentCommand + 1) % 3;
        dragon.setCommand(nextCommand);
        applyCommandState(nextCommand);

        if (!dragon.level().isClientSide) {
            player.displayClientMessage(
                    Component.translatable("entity.saintsdragons.all.command_" + nextCommand, dragon.getName()),
                    true
            );
        }

        return InteractionResult.SUCCESS;
    }

    private void playEatFeedback(ItemStack food) {
        dragon.triggerAnim("actions", "eat");
        dragon.playEatMovingSound();
        dragon.level().broadcastEntityEvent(dragon, (byte) 6);
        if (food.is(ModItems.HEARTY_DRAGON_MEAL.get())) {
            dragon.level().broadcastEntityEvent(dragon, (byte) 7);
        }
    }

    private void consumeItem(Player player, ItemStack food) {
        if (!player.getAbilities().instabuild) {
            food.shrink(1);
        }
    }

    private void sendStatusMessage(Player player, String key) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable(key, dragon.getName()), true);
        }
    }

    private boolean isVolitansFood(ItemStack stack) {
        return dragon.isFood(stack);
    }

    private void applyCommandState(int command) {
        switch (command) {
            case 0 -> dragon.setOrderedToSit(false);
            case 1 -> dragon.setOrderedToSit(true);
            case 2 -> {
                dragon.setOrderedToSit(false);
                dragon.setTarget(null);
                dragon.getNavigation().stop();
                if (dragon.isFlying() || dragon.isTakeoff() || dragon.isHovering()) {
                    dragon.setTakeoff(false);
                    dragon.setHovering(false);
                    dragon.setLanding(true);
                }
            }
            default -> {
            }
        }
    }
}
