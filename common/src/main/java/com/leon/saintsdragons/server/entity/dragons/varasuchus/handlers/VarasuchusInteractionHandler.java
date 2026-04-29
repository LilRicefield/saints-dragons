package com.leon.saintsdragons.server.entity.dragons.varasuchus.handlers;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.config.dragon.DragonTamingChance;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.dragons.handlers.AbstractDragonInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
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


public class VarasuchusInteractionHandler extends AbstractDragonInteractionHandler<Varasuchus> {

    public VarasuchusInteractionHandler(Varasuchus dragon) {
        super(dragon);
    }

    @Override
    protected InteractionResult handleUntamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.VARASUCHUS_ID);
        boolean legacyTaming = config.extraBoolean("legacy_taming", false);

        if (dragon.isSleeping() || dragon.isSleepingEntering() || dragon.isSleepingExiting()) {
            sendStatusMessage(player, "entity.saintsdragons.varasuchus.sleeping");
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }

        if (dragon.isBaby()) {
            return handleBabyTaming(player, heldItem, config);
        }

        if (isVarasuchusFood(heldItem)) {
            if (legacyTaming) {
                return handleLegacyTaming(player, heldItem);
            } else if (dragon.getHealth() < dragon.getMaxHealth()) {
                return handleFeeding(player, heldItem, true);
            }
        }

        if (!legacyTaming && hand == InteractionHand.MAIN_HAND && heldItem.isEmpty() && !player.isCrouching()) {
            boolean started = dragon.beginUntamedRide(player);
            return started ? InteractionResult.sidedSuccess(dragon.level().isClientSide) : InteractionResult.PASS;
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleLegacyTaming(Player player, ItemStack food) {
        if (!dragon.canFeed()) {
            if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("entity.saintsdragons.varasuchus.still_eating", dragon.getName()),
                        true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!dragon.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                food.shrink(1);
            }

            dragon.triggerAnim("action", "eat");
            playEatSound();
            dragon.setFeedingCooldown(40);

            boolean heartyMeal = food.is(ModItems.HEARTY_DRAGON_MEAL.get());
            boolean tropicalFish = food.is(Items.TROPICAL_FISH);
            float healAmount = heartyMeal ? 35.0F : 5.0F;
            float newHealth = Math.min(dragon.getHealth() + healAmount, dragon.getMaxHealth());
            dragon.setHealth(newHealth);
            dragon.applyFeedingHunger(heartyMeal);

            if (heartyMeal) {
                dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }

            // Taming chance logic
            DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.VARASUCHUS_ID);
            double tameChance = heartyMeal
                    ? Math.min(100.0D, config.extraDoubles().getOrDefault("taming_chance", 16.6667D) * 2.0D)
                    : tropicalFish
                        ? config.extraDoubles().getOrDefault("taming_chance_tropical", 25.0D)
                        : config.extraDoubles().getOrDefault("taming_chance", 16.6667D);
            boolean success = DragonTamingChance.rollPercent(dragon.getRandom(), tameChance);

            if (success) {
                dragon.tame(player);
                dragon.setOrderedToSit(true);
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);  // Hearts
                dragon.awardTamingAdvancement(player);
            } else {
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);  // Smoke
            }
        }

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }

    @Override
    protected InteractionResult handleTamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
        if (!dragon.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }

        if (player.isCrouching() && isVarasuchusFood(heldItem)) {
            return handleBreeding(player, heldItem);
        }

        if (isVarasuchusFood(heldItem)) {
            return handleFeeding(player, heldItem, false);
        }

        if (dragon.canOwnerCommand(player) && !isVarasuchusFood(heldItem) && hand == InteractionHand.MAIN_HAND) {
            return handleCommandCycling(player);
        }

        if (hand == InteractionHand.MAIN_HAND && !isVarasuchusFood(heldItem) && !player.isCrouching()) {
            return handleMounting(player);
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleBreeding(Player player, ItemStack food) {
        boolean client = dragon.level().isClientSide;
        var baby = dragon.getBabyComponent();

        if (baby != null && !baby.ensureCanFeed(player, "entity.saintsdragons.varasuchus", dragon.canFeed())) {
            return InteractionResult.CONSUME;
        }

        if (dragon.isBaby()) {
            sendStatusMessage(player, "entity.saintsdragons.varasuchus.breeding_too_young");
            return InteractionResult.sidedSuccess(client);
        }

        if (dragon.getAge() != 0) {
            sendStatusMessage(player, "entity.saintsdragons.varasuchus.breeding_cooling_down");
            return InteractionResult.sidedSuccess(client);
        }

        if (dragon.isInLove()) {
            sendStatusMessage(player, "entity.saintsdragons.varasuchus.breeding_already_ready");
            return InteractionResult.sidedSuccess(client);
        }

        if (!client) {
            if (!player.getAbilities().instabuild) {
                food.shrink(1);
            }

            dragon.triggerAnim("action", "eat");
            playEatSound();
            dragon.setFeedingCooldown(61);
            dragon.setInLove(player);
            sendStatusMessage(player, "entity.saintsdragons.varasuchus.breeding_ready");
        }

        return InteractionResult.sidedSuccess(client);
    }

    private InteractionResult handleFeeding(Player player, ItemStack food, boolean untamed) {
        var baby = dragon.getBabyComponent();
        if (baby != null && !baby.ensureCanFeed(player, "entity.saintsdragons.varasuchus", dragon.canFeed())) {
            return InteractionResult.CONSUME;
        }

        if (!dragon.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                food.shrink(1);
            }

            dragon.triggerAnim("action", "eat");
            playEatSound();
            dragon.setFeedingCooldown(50);

            boolean heartyMeal = food.is(ModItems.HEARTY_DRAGON_MEAL.get());
            boolean wasHungry = dragon.isHungry();
            if (dragon.isBaby()) {
                if (baby != null) {
                    baby.applyBabyGrowth(player, heartyMeal, "entity.saintsdragons.varasuchus", 2400, 4800);
                }
            } else {
                float healAmount;
                if (heartyMeal) {
                    healAmount = 35.0F;
                } else {
                    healAmount = untamed ? 5.0F : 10.0F;
                }

                float newHealth = Math.min(dragon.getHealth() + healAmount, dragon.getMaxHealth());
                dragon.setHealth(newHealth);
                dragon.applyFeedingHunger(heartyMeal);

                if (heartyMeal) {
                    dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                }

                dragon.level().broadcastEntityEvent(dragon, (byte) 7);

                if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                    boolean fullyHealed = newHealth >= dragon.getMaxHealth();
                    String messageKey;
                    if (fullyHealed) {
                        messageKey = wasHungry ? "entity.saintsdragons.dragon.feeding" : "entity.saintsdragons.varasuchus.fed";
                    } else {
                        messageKey = "entity.saintsdragons.varasuchus.fed_partial";
                    }
                    serverPlayer.displayClientMessage(
                            Component.translatable(
                                    messageKey,
                                    dragon.getName()
                            ),
                            true
                    );
                }
            }
        }

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }

    private InteractionResult handleBabyTaming(Player player, ItemStack food, DragonAttributeConfig config) {
        var baby = dragon.getBabyComponent();
        boolean heartyMeal = food.is(ModItems.HEARTY_DRAGON_MEAL.get());
        boolean tropicalFish = food.is(Items.TROPICAL_FISH);
        boolean validFood = isVarasuchusFood(food);
        if (baby == null) {
            return validFood ? InteractionResult.sidedSuccess(dragon.level().isClientSide) : InteractionResult.PASS;
        }

        double tameChance = heartyMeal
                ? Math.min(100.0D, config.extraDoubles().getOrDefault("taming_chance", 16.6667D) * 2.0D)
                : tropicalFish
                    ? config.extraDoubles().getOrDefault("taming_chance_tropical", 25.0D)
                    : config.extraDoubles().getOrDefault("taming_chance", 16.6667D);
        return baby.tryHandleBabyFoodTaming(
                player,
                food,
                "entity.saintsdragons.varasuchus",
                validFood,
                dragon.canFeed(),
                50,
                heartyMeal,
                () -> {
                    dragon.triggerAnim("action", "eat");
                    playEatSound();
                },
                dragon::setFeedingCooldown,
                tameChance,
                () -> {
                    dragon.tame(player);
                    dragon.setOrderedToSit(true);
                    dragon.awardTamingAdvancement(player);
                }
        );
    }

    private void playEatSound() {
        if (!dragon.level().isClientSide) {
            float pitch = dragon.isBaby() ? 1.6f : 1.0f;
            dragon.getSoundHandler().playMovingEntitySound(ModSounds.VARASUCHUS_EAT.get(), 1.0f, pitch, 59);
        }
    }

    private InteractionResult handleMounting(Player player) {
        if (!dragon.canOwnerMount(player) || dragon.isVehicle()) {
            return InteractionResult.PASS;
        }

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

    private InteractionResult handleCommandCycling(Player player) {
        if (!dragon.isInWaterOrBubble() && dragon.isInSitTransition()) {
            if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                String messageKey;
                if (dragon.isSittingDownAnimation()) {
                    messageKey = "entity.saintsdragons.varasuchus.sitting_down";
                } else if (dragon.isStandingUpAnimation()) {
                    messageKey = "entity.saintsdragons.varasuchus.standing_up";
                } else {
                    messageKey = "entity.saintsdragons.varasuchus.transitioning";
                }
                serverPlayer.displayClientMessage(
                        Component.translatable(messageKey, dragon.getName()),
                        true
                );
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

    private void applyCommandState(int command) {
        switch (command) {
            case 0, 2 -> dragon.setOrderedToSit(false);
            case 1 -> dragon.setOrderedToSit(true);
            default -> {
            }
        }
    }

    private boolean isVarasuchusFood(ItemStack itemstack) {
        return dragon.isFood(itemstack)
                || itemstack.is(Items.SALMON)
                || itemstack.is(Items.COD)
                || itemstack.is(Items.TROPICAL_FISH)
                || itemstack.is(ModItems.HEARTY_DRAGON_MEAL.get());
    }


    @Override
    protected Item getBinderItem() {
        return ModItems.VARASUCHUS_BINDER.get();
    }
}