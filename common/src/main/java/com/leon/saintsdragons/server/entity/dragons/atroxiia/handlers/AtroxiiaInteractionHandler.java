package com.leon.saintsdragons.server.entity.dragons.atroxiia.handlers;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.config.dragon.DragonTamingChance;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.leon.saintsdragons.server.entity.dragons.handlers.AbstractDragonInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonBreedingInteractionHelper;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class AtroxiiaInteractionHandler extends AbstractDragonInteractionHandler<Atroxiia> {
    public AtroxiiaInteractionHandler(Atroxiia dragon) {
        super(dragon);
    }

    @Override
    protected Item getBinderItem() {
        return ModItems.ATROXIIA_BINDER.get();
    }

    @Override
    protected String getCommandStatusMessageKey(int command) {
        if (command == 1 && dragon.isInWaterOrBubble()) {
            return "entity.saintsdragons.all.command_1_staying";
        }
        return super.getCommandStatusMessageKey(command);
    }

    @Override
    protected InteractionResult handleUntamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
        if (!dragon.isFood(heldItem)) {
            return InteractionResult.PASS;
        }
        if (dragon.isBaby()) {
            return handleFeeding(player, heldItem);
        }

        boolean client = dragon.level().isClientSide;
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.ATROXIIA_ID);
        boolean legacyTaming = config.extraBoolean("legacy_taming", false);

        if (!legacyTaming && dragon.isTamingStunned() && !dragon.isReadyForTamingFeed()) {
            sendStatusMessage(player, "entity.saintsdragons.atroxiia.taming_dazed");
            return InteractionResult.CONSUME;
        }
        if (!dragon.canFeed()) {
            sendStatusMessage(player, "entity.saintsdragons.atroxiia.still_eating");
            return InteractionResult.CONSUME;
        }
        if (!legacyTaming
                && !dragon.isReadyForTamingFeed()
                && dragon.getHealth() > dragon.getTamingThreshold() + 1.0F) {
            sendStatusMessage(
                    player,
                    "entity.saintsdragons.atroxiia.taming_need_weakened",
                    dragon.getName(),
                    Math.round(dragon.getTamingThreshold())
            );
            return InteractionResult.CONSUME;
        }

        if (!client) {
            consumeHeldItem(player, heldItem);
            dragon.triggerAnim(AnimationHelper.INTERACTION_CONTROLLER, AnimationHelper.EAT);
            dragon.playEatMovingSound();
            dragon.setFeedingCooldown(Atroxiia.EAT_ANIMATION_TICKS);

            boolean heartyMeal = heldItem.is(ModItems.HEARTY_DRAGON_MEAL.get());
            if (legacyTaming && heartyMeal) {
                dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }
            dragon.applyFeedingHunger(heartyMeal);

            if (legacyTaming) {
                dragon.heal(heartyMeal ? 28.0F : 10.0F);
            } else {
                dragon.enterTamingStun();
            }

            double tameChance = heartyMeal
                    ? config.extraDouble("taming_chance_hearty", 33.3333D)
                    : config.extraDouble("taming_chance_base", 20.0D);
            if (DragonTamingChance.rollPercent(dragon.getRandom(), tameChance)) {
                dragon.tame(player);
                if (!legacyTaming && heartyMeal) {
                    dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                }
                dragon.setOrderedToSit(true);
                dragon.setCommand(1);
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
                awardTamingAdvancement(player);
                if (!legacyTaming) {
                    dragon.resetTamingFailures();
                    dragon.clearTamingRecovery();
                }
            } else {
                if (!legacyTaming) {
                    dragon.setTamingRecoveryTarget(dragon.getMaxHealth());
                    dragon.incrementTamingFailures();
                }
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
                sendStatusMessage(player, "entity.saintsdragons.atroxiia.taming_failed");
            }
        }

        return InteractionResult.sidedSuccess(client);
    }

    private void awardTamingAdvancement(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var advancement = serverPlayer.server.getAdvancements()
                .getAdvancement(SaintsDragonsCommon.rl("tame_atroxiia"));
        if (advancement != null) {
            serverPlayer.getAdvancements().award(advancement, "tame_atroxiia");
        }
    }

    @Override
    protected InteractionResult handleTamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
        InteractionResult growthStuntResult = tryHandleGrowthStuntingFood(
                player,
                heldItem,
                "entity.saintsdragons.atroxiia",
                dragon.canFeed(),
                Atroxiia.EAT_ANIMATION_TICKS,
                () -> {
                    dragon.triggerAnim(AnimationHelper.INTERACTION_CONTROLLER, AnimationHelper.EAT);
                    dragon.playEatMovingSound();
                },
                dragon::setFeedingCooldown
        );
        if (growthStuntResult != InteractionResult.PASS) {
            return growthStuntResult;
        }

        if (!dragon.isBaby() && dragon.isOwnedBy(player) && player.isCrouching() && dragon.isFood(heldItem)) {
            return handleBreeding(player, heldItem);
        }

        if (dragon.isFood(heldItem)) {
            return handleFeeding(player, heldItem);
        }

        if (!dragon.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }

        if (player.isCrouching() && dragon.canOwnerCommand(player) && hand == InteractionHand.MAIN_HAND) {
            return handleCommandCycling(player);
        }

        if (!player.isCrouching() && hand == InteractionHand.MAIN_HAND && heldItem.isEmpty()) {
            return handleStandardMounting(player);
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleBreeding(Player player, ItemStack heldItem) {
        return DragonBreedingInteractionHelper.handleBreeding(
                dragon,
                player,
                heldItem,
                dragon::canFeed,
                "entity.saintsdragons.dragon.still_eating",
                Atroxiia.EAT_ANIMATION_TICKS,
                () -> {
                    dragon.triggerAnim(AnimationHelper.INTERACTION_CONTROLLER, AnimationHelper.EAT);
                    dragon.playEatMovingSound();
                },
                dragon::setFeedingCooldown
        );
    }

    private InteractionResult handleFeeding(Player player, ItemStack heldItem) {
        var baby = dragon.getBabyComponent();
        if (baby != null && !baby.ensureCanFeed(player, "entity.saintsdragons.atroxiia", dragon.canFeed())) {
            return InteractionResult.CONSUME;
        }

        if (!dragon.level().isClientSide) {
            boolean heartyMeal = heldItem.is(ModItems.HEARTY_DRAGON_MEAL.get());
            boolean wasHungry = dragon.isHungry();
            consumeHeldItem(player, heldItem);

            dragon.triggerAnim(AnimationHelper.INTERACTION_CONTROLLER, AnimationHelper.EAT);
            dragon.playEatMovingSound();
            dragon.setFeedingCooldown(Atroxiia.EAT_ANIMATION_TICKS);

            if (dragon.isBaby()) {
                if (baby != null) {
                    baby.applyBabyGrowth(player, heartyMeal, "entity.saintsdragons.atroxiia", 2400, 4800);
                }
            } else {
                float healAmount = heartyMeal ? 28.0F : 10.0F;
                dragon.heal(healAmount);
                dragon.applyFeedingHunger(heartyMeal);
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);

                String messageKey = dragon.getHealth() >= dragon.getMaxHealth()
                        ? (wasHungry ? "entity.saintsdragons.dragon.feeding" : "entity.saintsdragons.atroxiia.fed")
                        : "entity.saintsdragons.atroxiia.fed_partial";
                sendStatusMessage(player, messageKey);
            }
        }

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }
}
