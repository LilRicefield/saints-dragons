package com.leon.saintsdragons.server.entity.dragons.varasuchus.handlers;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.config.dragon.DragonTamingChance;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.dragons.handlers.AbstractDragonInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonBreedingInteractionHelper;
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

            dragon.triggerAnim("interaction", "eat");
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
        InteractionResult growthStuntResult = tryHandleGrowthStuntingFood(
                player,
                heldItem,
                "entity.saintsdragons.varasuchus",
                dragon.canFeed(),
                50,
                () -> {
                    dragon.triggerAnim("interaction", "eat");
                    playEatSound();
                },
                dragon::setFeedingCooldown
        );
        if (growthStuntResult != InteractionResult.PASS) {
            return growthStuntResult;
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
        var baby = dragon.getBabyComponent();

        if (baby != null && !baby.ensureCanFeed(player, "entity.saintsdragons.varasuchus", dragon.canFeed())) {
            return InteractionResult.CONSUME;
        }

        return DragonBreedingInteractionHelper.handleBreeding(
                dragon,
                player,
                food,
                dragon::canFeed,
                "entity.saintsdragons.varasuchus.still_eating",
                61,
                () -> {
                    dragon.triggerAnim("interaction", "eat");
                    playEatSound();
                },
                dragon::setFeedingCooldown
        );
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

            dragon.triggerAnim("interaction", "eat");
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
                    dragon.triggerAnim("interaction", "eat");
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
        return handleStandardMounting(player);
    }

    private boolean isVarasuchusFood(ItemStack itemstack) {
        return dragon.isFood(itemstack);
    }


    @Override
    protected Item getBinderItem() {
        return ModItems.VARASUCHUS_BINDER.get();
    }
}
