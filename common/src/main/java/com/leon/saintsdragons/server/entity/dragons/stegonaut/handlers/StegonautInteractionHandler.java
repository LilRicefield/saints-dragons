package com.leon.saintsdragons.server.entity.dragons.stegonaut.handlers;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.config.dragon.DragonTamingChance;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.handlers.AbstractDragonInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class StegonautInteractionHandler extends AbstractDragonInteractionHandler<Stegonaut> {
    public StegonautInteractionHandler(Stegonaut dragon) {
        super(dragon);
    }

    @Override
    protected Item getBinderItem() {
        return ModItems.STEGONAUT_BINDER.get();
    }

    @Override
    protected InteractionResult handleUntamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
        var baby = dragon.getBabyComponent();

        if (!dragon.isFood(heldItem)) {
            return InteractionResult.PASS;
        }

        if (dragon.isBaby() && baby != null) {
            DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.STEGONAUT_ID);
            boolean hearty = heldItem.is(ModItems.HEARTY_DRAGON_MEAL.get());
            double tamingChance = hearty
                    ? config.extraDoubles().getOrDefault("taming_chance_hearty", 1.0)
                    : config.extraDoubles().getOrDefault("taming_chance_base", 1.0);
            return baby.tryHandleBabyFoodTaming(
                    player,
                    heldItem,
                    "entity.saintsdragons.stegonaut",
                    true,
                    true,
                    22,
                    hearty,
                    () -> {
                        dragon.triggerAnim("interaction", "eat");
                        dragon.playEatMovingSound();
                    },
                    ticks -> {
                    },
                    tamingChance,
                    () -> {
                        dragon.tame(player);
                        dragon.setPackLeaderUuid(null);
                        dragon.setOrderedToSit(true);
                        dragon.setCommand(1);
                        player.displayClientMessage(
                                Component.translatable("entity.saintsdragons.stegonaut.tamed", dragon.getName()),
                                true
                        );
                        awardTamingAdvancement(player);
                    }
            );
        }

        if (!dragon.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                heldItem.shrink(1);
            }

            dragon.triggerAnim("interaction", "eat");
            dragon.playEatMovingSound();

            DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.STEGONAUT_ID);
            boolean hearty = heldItem.is(ModItems.HEARTY_DRAGON_MEAL.get());
            double tamingChance = hearty
                    ? config.extraDoubles().getOrDefault("taming_chance_hearty", 1.0)
                    : config.extraDoubles().getOrDefault("taming_chance_base", 1.0);
            if (DragonTamingChance.rollPercent(dragon.getRandom(), tamingChance)) {
                dragon.tame(player);
                dragon.setPackLeaderUuid(null);
                dragon.setOrderedToSit(true);
                dragon.setCommand(1);
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
                player.displayClientMessage(
                        Component.translatable("entity.saintsdragons.stegonaut.tamed", dragon.getName()),
                        true
                );
                awardTamingAdvancement(player);
            } else {
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
            }
        }

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }

    @Override
    protected InteractionResult handleTamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
        boolean isOwner = dragon.isOwnedBy(player);
        InteractionResult growthStuntResult = tryHandleGrowthStuntingFood(
                player,
                heldItem,
                "entity.saintsdragons.stegonaut",
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

        if (isOwner && player.isCrouching() && dragon.isFood(heldItem)) {
            return handleBreeding(player, heldItem);
        }

        if (dragon.isFood(heldItem)) {
            return handleFeeding(player, heldItem);
        }

        if (isOwner) {
            if (player.isCrouching() && dragon.canOwnerCommand(player) && hand == InteractionHand.MAIN_HAND) {
                return handleCommandCycling(player);
            }

            if (!player.isCrouching() && hand == InteractionHand.MAIN_HAND) {
                return handleMounting(player);
            }
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleMounting(Player player) {
        if (!dragon.canOwnerMount(player) || dragon.isVehicle()) {
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }

        if (!dragon.level().isClientSide) {
            dragon.prepareForMounting();
            player.startRiding(dragon);
        }

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }

    private InteractionResult handleBreeding(Player player, ItemStack heldItem) {
        boolean client = dragon.level().isClientSide;
        if (!client && !checkBreedingEnabled(player)) {
            return InteractionResult.CONSUME;
        }

        if (dragon.isBaby()) {
            sendStatusMessage(player, "entity.saintsdragons.stegonaut.breeding_too_young");
            return InteractionResult.sidedSuccess(client);
        }

        if (dragon.getAge() != 0) {
            sendStatusMessage(player, "entity.saintsdragons.stegonaut.breeding_cooling_down");
            return InteractionResult.sidedSuccess(client);
        }

        if (dragon.isInLove()) {
            sendStatusMessage(player, "entity.saintsdragons.stegonaut.breeding_already_ready");
            return InteractionResult.sidedSuccess(client);
        }

        if (!client) {
            if (!player.getAbilities().instabuild) {
                heldItem.shrink(1);
            }

            dragon.triggerAnim("interaction", "eat");
            dragon.playEatMovingSound();
            dragon.setInLove(player);
            sendStatusMessage(player, "entity.saintsdragons.stegonaut.breeding_ready");
        }

        return InteractionResult.sidedSuccess(client);
    }

    private InteractionResult handleFeeding(Player player, ItemStack heldItem) {
        var baby = dragon.getBabyComponent();
        if (!dragon.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                heldItem.shrink(1);
            }

            dragon.triggerAnim("interaction", "eat");
            dragon.playEatMovingSound();

            boolean hearty = heldItem.is(ModItems.HEARTY_DRAGON_MEAL.get());
            boolean wasHungry = dragon.isHungry();

            if (dragon.isBaby()) {
                if (baby != null) {
                    baby.applyBabyGrowth(player, hearty, "entity.saintsdragons.stegonaut", 2400, 4800);
                }
            } else {
                float healAmount = hearty ? 18.0f : 8.0f;
                float newHealth = Math.min(dragon.getHealth() + healAmount, dragon.getMaxHealth());
                dragon.setHealth(newHealth);
                dragon.applyFeedingHunger(hearty);
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);

                String messageKey = newHealth >= dragon.getMaxHealth()
                        ? (wasHungry ? "entity.saintsdragons.dragon.feeding" : "entity.saintsdragons.stegonaut.fed")
                        : "entity.saintsdragons.stegonaut.fed_partial";
                player.displayClientMessage(Component.translatable(messageKey, dragon.getName()), true);
            }
        }

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }

    private InteractionResult handleCommandCycling(Player player) {
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

    private void awardTamingAdvancement(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var advancement = serverPlayer.server.getAdvancements()
                    .getAdvancement(SaintsDragonsCommon.rl("tame_stegonaut"));
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "tame_stegonaut");
            }
        }
    }
}
