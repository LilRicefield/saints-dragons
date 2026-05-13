package com.leon.saintsdragons.server.entity.dragons.volitans.handlers;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.config.dragon.DragonTamingChance;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.handlers.AbstractDragonInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class VolitansInteractionHandler extends AbstractDragonInteractionHandler<Volitans> {
    public VolitansInteractionHandler(Volitans dragon) {
        super(dragon);
    }

    @Override
    protected Item getBinderItem() {
        return ModItems.VOLITANS_BINDER.get();
    }

    @Override
    protected InteractionResult handleUntamedInteraction(Player player, InteractionHand hand, ItemStack itemstack) {
        boolean client = dragon.level().isClientSide;
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.VOLITANS_ID);
        boolean legacyTaming = config.extraBoolean("legacy_taming", false);

        if (dragon.isBaby()) {
            return handleBabyTaming(player, itemstack, config);
        }

        if (!legacyTaming && dragon.isTamingStunned() && player.isCrouching() && itemstack.isEmpty()) {
            if (!client) {
                dragon.abortTamingAttempt();
                sendStatusMessage(player, "entity.saintsdragons.volitans.taming_aborted");
            }
            return InteractionResult.sidedSuccess(client);
        }

        if (!dragon.isFood(itemstack)) {
            return InteractionResult.PASS;
        }

        if (!legacyTaming && dragon.isTamingStunned() && !dragon.isAwaitingTamingFeed()) {
            sendStatusMessage(player, "entity.saintsdragons.volitans.taming_dazed");
            return InteractionResult.CONSUME;
        }

        if (!dragon.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("entity.saintsdragons.volitans.still_eating", dragon.getName()),
                        true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!legacyTaming) {
            float minRequiredHealth = dragon.getTamingThreshold();
            if (dragon.getHealth() > minRequiredHealth + 1.0F) {
                sendStatusMessage(player, "entity.saintsdragons.volitans.taming_need_weakened", dragon.getName(), Math.round(minRequiredHealth));
                return InteractionResult.CONSUME;
            }
        }

        if (!client) {
            consumeItem(player, itemstack);
            dragon.triggerAnim("actions", "eat");
            dragon.playEatMovingSound();
            dragon.setFeedingCooldown(61);

            boolean hearty = itemstack.is(ModItems.HEARTY_DRAGON_MEAL.get());
            if (hearty) {
                dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }
            dragon.applyFeedingHunger(hearty);

            if (legacyTaming) {
                float healAmount = hearty ? 28.0F : 10.0F;
                dragon.setHealth(Math.min(dragon.getHealth() + healAmount, dragon.getMaxHealth()));
            } else {
                dragon.enterTamingStun();
            }

            double tameChance = hearty
                    ? config.extraDouble("taming_chance_hearty", 3.0D)
                    : config.extraDouble("taming_chance_base", 5.0D);
            boolean success = DragonTamingChance.rollPercent(dragon.getRandom(), tameChance);
            if (success) {
                dragon.tame(player);
                dragon.setOrderedToSit(true);
                dragon.setCommand(1);
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
                if (!legacyTaming) {
                    dragon.resetTamingFailures();
                    dragon.clearTamingRecovery();
                }
                triggerTamingAdvancement(player);
            } else {
                if (!legacyTaming) {
                    dragon.setTamingRecoveryTarget(nextFailureHealTarget());
                    dragon.incrementTamingFailures();
                }
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
                sendStatusMessage(player, "entity.saintsdragons.volitans.taming_failed");
            }
        }

        return InteractionResult.sidedSuccess(client);
    }

    @Override
    protected InteractionResult handleTamedInteraction(Player player, InteractionHand hand, ItemStack itemstack) {
        boolean isOwner = player.equals(dragon.getOwner());

        if (isVolitansFood(itemstack)) {
            if (player.isCrouching() && isOwner) {
                return handleBreeding(player, itemstack);
            }
            return handleFeeding(player, itemstack);
        }

        if (isOwner) {
            if (player.isCrouching() && dragon.canOwnerCommand(player) && hand == InteractionHand.MAIN_HAND) {
                return handleCommandCycling(player);
            }
            if (!player.isCrouching() && !isVolitansFood(itemstack) && hand == InteractionHand.MAIN_HAND && dragon.canOwnerMount(player)) {
                return handleMounting(player);
            }
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleBabyTaming(Player player, ItemStack itemstack, DragonAttributeConfig config) {
        var baby = dragon.getBabyComponent();
        boolean hearty = itemstack.is(ModItems.HEARTY_DRAGON_MEAL.get());
        boolean validFood = dragon.isFood(itemstack);
        if (baby == null) {
            return validFood ? InteractionResult.sidedSuccess(dragon.level().isClientSide) : InteractionResult.PASS;
        }

        double tameChance = hearty
                ? config.extraDouble("taming_chance_hearty", 3.0D)
                : config.extraDouble("taming_chance_base", 5.0D);
        return baby.tryHandleBabyFoodTaming(
                player,
                itemstack,
                "entity.saintsdragons.volitans",
                validFood,
                dragon.canFeed(),
                61,
                hearty,
                () -> {
                    dragon.triggerAnim("actions", "eat");
                    dragon.playEatMovingSound();
                },
                dragon::setFeedingCooldown,
                tameChance,
                () -> {
                    dragon.tame(player);
                    dragon.setOrderedToSit(true);
                    dragon.setCommand(1);
                    triggerTamingAdvancement(player);
                }
        );
    }

    private InteractionResult handleBreeding(Player player, ItemStack food) {
        boolean client = dragon.level().isClientSide;
        if (!client && !checkBreedingEnabled(player)) {
            return InteractionResult.CONSUME;
        }

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
        var baby = dragon.getBabyComponent();
        if (baby != null && !baby.ensureCanFeed(player, "entity.saintsdragons.volitans", dragon.canFeed())) {
            return InteractionResult.CONSUME;
        }

        if (!dragon.level().isClientSide) {
            consumeItem(player, food);
            dragon.setFeedingCooldown(24);
            playEatFeedback(food);

            boolean heartyMeal = food.is(ModItems.HEARTY_DRAGON_MEAL.get());
            boolean wasHungry = dragon.isHungry();

            if (dragon.isBaby()) {
                if (baby != null) {
                    baby.applyBabyGrowth(player, heartyMeal, "entity.saintsdragons.volitans", 2400, 4800);
                }
            } else {
                float healAmount = heartyMeal ? 28.0F : 10.0F;
                float newHealth = Math.min(dragon.getHealth() + healAmount, dragon.getMaxHealth());
                dragon.setHealth(newHealth);
                dragon.applyFeedingHunger(heartyMeal);
                if (heartyMeal) {
                    dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                }
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
                sendFeedingMessage(player, newHealth, wasHungry);
            }
        }

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }

    private InteractionResult handleCommandCycling(Player player) {
        if (!dragon.isInWaterOrBubble() && dragon.isInSitTransition()) {
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

        int nextCommand = dragon.getNextCommand();
        dragon.setCommand(nextCommand);

        if (!dragon.level().isClientSide) {
            player.displayClientMessage(
                    Component.translatable("entity.saintsdragons.all.command_" + nextCommand, dragon.getName()),
                    true
            );
        }

        return InteractionResult.SUCCESS;
    }

    private InteractionResult handleMounting(Player player) {
        if (dragon.isVehicle()) {
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }

        if (!dragon.level().isClientSide) {
            dragon.prepareForMounting();
            dragon.combatManager.clearAllStates();
            dragon.setAggressive(false);
            player.startRiding(dragon);
        }
        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
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
        consumeHeldItem(player, food);
    }

    private boolean isVolitansFood(ItemStack stack) {
        return dragon.isFood(stack);
    }

    private Float nextFailureHealTarget() {
        return dragon.getMaxHealth();
    }

    private void sendFeedingMessage(Player player, float newHealth, boolean wasHungry) {
        if (player instanceof ServerPlayer serverPlayer) {
            String messageKey = newHealth >= dragon.getMaxHealth()
                    ? (wasHungry ? "entity.saintsdragons.dragon.feeding" : "entity.saintsdragons.volitans.fed")
                    : "entity.saintsdragons.volitans.fed_partial";
            serverPlayer.displayClientMessage(Component.translatable(messageKey, dragon.getName()), true);
        }
    }

    private void triggerTamingAdvancement(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var advancement = serverPlayer.server.getAdvancements()
                    .getAdvancement(SaintsDragonsCommon.rl("tame_volitans"));
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "tame_volitans");
            }
        }
    }

}
