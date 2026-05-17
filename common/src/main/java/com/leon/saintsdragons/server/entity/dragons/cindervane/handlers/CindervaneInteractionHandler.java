package com.leon.saintsdragons.server.entity.dragons.cindervane.handlers;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.config.dragon.DragonTamingChance;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.handlers.AbstractDragonInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CindervaneInteractionHandler extends AbstractDragonInteractionHandler<Cindervane> {
    public CindervaneInteractionHandler(Cindervane amphithere) {
        super(amphithere);
    }

    @Override
    protected InteractionResult handleUntamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.CINDERVANE_ID);

        if (dragon.isBaby()) {
            return handleBabyTaming(player, heldItem, config);
        }

        if (!dragon.isFood(heldItem)) {
            return InteractionResult.PASS;
        }

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
            dragon.triggerAnim("interaction", "eat");
            dragon.playEatMovingSound();
            dragon.setFeedingCooldown(44);
            boolean hearty = heldItem.is(ModItems.HEARTY_DRAGON_MEAL.get());
            dragon.applyFeedingHunger(hearty);
            double tameChance = resolveTamingChance(heldItem, config);
            if (hearty) {
                dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }

            if (DragonTamingChance.rollPercent(dragon.getRandom(), tameChance)) {
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
            return InteractionResult.sidedSuccess(false);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult handleTamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
        boolean isOwner = dragon.isOwnedBy(player);
        InteractionResult growthStuntResult = tryHandleGrowthStuntingFood(
                player,
                heldItem,
                "entity.saintsdragons.cindervane",
                dragon.canFeed(),
                44,
                () -> {
                    dragon.triggerAnim("interaction", "eat");
                    dragon.playEatMovingSound();
                },
                dragon::setFeedingCooldown
        );
        if (growthStuntResult != InteractionResult.PASS) {
            return growthStuntResult;
        }
        if (isOwner) {
            if (player.isCrouching() && dragon.isFood(heldItem)) {
                return handleBreeding(player, heldItem);
            }
            if (dragon.isFood(heldItem)) {
                return handleFeeding(player, heldItem);
            }
            if (dragon.canOwnerCommand(player) && !dragon.isFood(heldItem) && hand == InteractionHand.MAIN_HAND) {
                return handleCommandCycling(player);
            }
        }

        if (hand == InteractionHand.MAIN_HAND && !dragon.isFood(heldItem) && !player.isCrouching()) {
            return handleMounting(player, isOwner);
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleMounting(Player player, boolean isOwner) {
        var passengers = dragon.getPassengers();

        if (isOwner) {
            if (passengers.isEmpty() && dragon.canOwnerMount(player)) {
                if (!dragon.level().isClientSide) {
                    dragon.prepareForMounting();
                    if (!player.startRiding(dragon)) {
                        return InteractionResult.FAIL;
                    }
                }
                return InteractionResult.sidedSuccess(dragon.level().isClientSide);
            } else if (!passengers.isEmpty() && passengers.get(0) != player) {
                Entity firstPassenger = passengers.get(0);
                boolean seat0IsOwner = firstPassenger instanceof Player firstPlayer && dragon.isOwnedBy(firstPlayer);
                if (!seat0IsOwner && dragon.canOwnerMount(player)) {
                    if (!dragon.level().isClientSide) {
                        firstPassenger.stopRiding();
                        dragon.prepareForMounting();
                        if (!player.startRiding(dragon)) {
                            return InteractionResult.FAIL;
                        }
                    }
                    return InteractionResult.sidedSuccess(dragon.level().isClientSide);
                }

                if (!dragon.level().isClientSide) {
                    player.displayClientMessage(
                        Component.translatable("entity.saintsdragons.cindervane.mount_occupied"),
                        true
                    );
                }
                return InteractionResult.FAIL;
            }
        } else {
            if (passengers.isEmpty()) {
                if (!dragon.level().isClientSide) {
                    player.displayClientMessage(
                        Component.translatable("entity.saintsdragons.cindervane.passenger_needs_owner"),
                        true
                    );
                }
                return InteractionResult.FAIL;
            }

            if (passengers.size() >= 2) {
                if (!dragon.level().isClientSide) {
                    player.displayClientMessage(
                        Component.translatable("entity.saintsdragons.cindervane.seats_full"),
                        true
                    );
                }
                return InteractionResult.FAIL;
            }

            if (passengers.get(0) instanceof Player firstPlayer && dragon.isOwnedBy(firstPlayer)) {
                if (!dragon.level().isClientSide) {
                    if (!player.startRiding(dragon)) {
                        return InteractionResult.FAIL;
                    }
                }
                return InteractionResult.sidedSuccess(dragon.level().isClientSide);
            } else {
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
        if (!client && !checkBreedingEnabled(player)) {
            return InteractionResult.CONSUME;
        }
        var baby = dragon.getBabyComponent();

        if (baby != null && !baby.ensureCanFeed(player, "entity.saintsdragons.cindervane", dragon.canFeed())) {
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

            dragon.triggerAnim("interaction", "eat");
            dragon.playEatMovingSound();
            dragon.setFeedingCooldown(61);
            dragon.setInLove(player);
            sendStatusMessage(player, "entity.saintsdragons.cindervane.breeding_ready");
        }

        return InteractionResult.sidedSuccess(client);
    }

    private InteractionResult handleFeeding(Player player, ItemStack food) {
        var baby = dragon.getBabyComponent();
        if (baby != null && !baby.ensureCanFeed(player, "entity.saintsdragons.cindervane", dragon.canFeed())) {
            return InteractionResult.CONSUME;
        }

        if (!dragon.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                food.shrink(1);
            }
            dragon.triggerAnim("interaction", "eat");
            dragon.playEatMovingSound();
            dragon.setFeedingCooldown(44);

            boolean hearty = food.is(ModItems.HEARTY_DRAGON_MEAL.get());
            boolean wasHungry = dragon.isHungry();
            if (dragon.isBaby()) {
                if (baby != null) {
                    baby.applyBabyGrowth(player, hearty, "entity.saintsdragons.cindervane", 2400, 4800);
                }
            } else {
                float healAmount = hearty ? 15.0F : 5.0F;
                float newHealth = Math.min(dragon.getHealth() + healAmount, dragon.getMaxHealth());
                boolean fullyHealed = newHealth >= dragon.getMaxHealth();

                dragon.heal(healAmount);
                dragon.applyFeedingHunger(hearty);
                if (hearty) {
                    dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                }
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
                String messageKey;
                if (fullyHealed) {
                    messageKey = wasHungry ? "entity.saintsdragons.dragon.feeding" : "entity.saintsdragons.cindervane.fed";
                } else {
                    messageKey = "entity.saintsdragons.cindervane.fed_partial";
                }
                player.displayClientMessage(
                    Component.translatable(messageKey, dragon.getName()),
                    true
                );
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.sidedSuccess(true);
    }

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

        int nextCommand = dragon.getNextCommand();
        dragon.setCommand(nextCommand);
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
        var baby = dragon.getBabyComponent();
        boolean hearty = itemstack.is(ModItems.HEARTY_DRAGON_MEAL.get());
        boolean validFood = dragon.isFood(itemstack);
        if (baby == null) {
            return validFood ? InteractionResult.sidedSuccess(dragon.level().isClientSide) : InteractionResult.PASS;
        }

        double tameChance = resolveTamingChance(itemstack, config);
        return baby.tryHandleBabyFoodTaming(
                player,
                itemstack,
                "entity.saintsdragons.cindervane",
                validFood,
                dragon.canFeed(),
                44,
                hearty,
                () -> {
                    dragon.triggerAnim("interaction", "eat");
                    dragon.playEatMovingSound();
                },
                dragon::setFeedingCooldown,
                tameChance,
                () -> {
                    dragon.tame(player);
                    dragon.getNavigation().stop();
                    dragon.setOrderedToSit(true);
                    dragon.setCommand(1);
                    dragon.setTarget(null);
                    triggerTamingAdvancement(player);
                }
        );
    }

    private double resolveTamingChance(ItemStack food, DragonAttributeConfig config) {
        double baseChance = config.extraDoubles().getOrDefault("taming_chance_base", 4.0);
        double heartyChance = config.extraDoubles().getOrDefault("taming_chance_hearty", 2.0);

        if (food.is(ModItems.HEARTY_DRAGON_MEAL.get())) {
            return heartyChance;
        }

        if (food.is(Items.CHICKEN)) {
            Double chickenChance = config.extraDoubles().get("taming_chance_chicken");
            if (chickenChance != null) {
                return chickenChance;
            }
            if (baseChance > heartyChance) {
                return (baseChance + heartyChance) * 0.5;
            }
        }

        return baseChance;
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

    @Override
    protected Item getBinderItem() {
        return ModItems.CINDERVANE_BINDER.get();
    }
}
