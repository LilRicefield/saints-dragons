package com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.config.dragon.DragonTamingChance;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.entity.dragons.handlers.AbstractDragonInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonBreedingInteractionHelper;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class IgnivorusInteractionHandler extends AbstractDragonInteractionHandler<Ignivorus> {
    public IgnivorusInteractionHandler(Ignivorus dragon) {
        super(dragon);
    }

    @Override
    protected InteractionResult handleUntamedInteraction(Player player, InteractionHand hand, ItemStack itemstack) {
        boolean client = dragon.level().isClientSide;
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        boolean legacyTaming = config.extraBoolean("legacy_taming", false);

        if (dragon.isBaby()) {
            return handleBabyTaming(player, itemstack, config);
        }
        if (!isIgnivorusFood(itemstack)) {
            return InteractionResult.PASS;
        }

        if (!legacyTaming) {
            if (dragon.isTamingStunned()) {
                if (!dragon.isAwaitingTamingFeed()) {
                    sendStatusMessage(player, "entity.saintsdragons.ignivorus.taming_dazed");
                    return InteractionResult.CONSUME;
                }
            }
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

        if (!legacyTaming) {
            float minRequiredHealth = dragon.getTamingThreshold();
            if (dragon.getHealth() > minRequiredHealth + 1.0F) {
                sendStatusMessage(player, "entity.saintsdragons.ignivorus.taming_need_weakened", dragon.getName(), Math.round(minRequiredHealth));
                return InteractionResult.CONSUME;
            }
        }
        if (!client) {
            consumeHeldItem(player, itemstack);
            dragon.triggerAnim("interaction", "eat");
            playEatSound();
            dragon.setFeedingCooldown(20);
            boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            boolean beef = itemstack.is(net.minecraft.world.item.Items.BEEF);
            if (hearty) {
                dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }
            dragon.applyFeedingHunger(hearty);
            if (legacyTaming) {
                float healAmount = hearty ? 30.0f : (beef ? 16.0f : 10.0f);
                float newHealth = Math.min(dragon.getHealth() + healAmount, dragon.getMaxHealth());
                dragon.setHealth(newHealth);
            } else {
                dragon.enterTamingStun();
            }

            double tameChance = getTamingChance(itemstack, config);
            boolean success = DragonTamingChance.rollPercent(dragon.getRandom(), tameChance);

            if (success) {
                dragon.tame(player);
                dragon.setOrderedToSit(true);
                dragon.setCommand(1);
                dragon.combatManager.clearAbilityCooldown(ModAbilities.IGNIVORUS_ULTIMATE);
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
                if (!legacyTaming) {
                    dragon.resetTamingFailures();
                    dragon.clearTamingRecovery();
                }
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

    @Override
    protected InteractionResult handleTamedInteraction(Player player, InteractionHand hand, ItemStack itemstack) {
        boolean isOwner = player.equals(dragon.getOwner());
        InteractionResult growthStuntResult = tryHandleGrowthStuntingFood(
                player,
                itemstack,
                "entity.saintsdragons.ignivorus",
                dragon.canFeed(),
                23,
                () -> {
                    dragon.triggerAnim("interaction", "eat");
                    playEatSound();
                },
                dragon::setFeedingCooldown
        );
        if (growthStuntResult != InteractionResult.PASS) {
            return growthStuntResult;
        }
        if (isOwner) {
            if (player.isCrouching() && isIgnivorusFood(itemstack)) {
                return handleBreeding(player, itemstack);
            }
            if (player.isCrouching() && !isIgnivorusFood(itemstack) && hand == InteractionHand.MAIN_HAND) {
                return handleCommandCycling(player);
            }
            else if (!player.isCrouching() && !isIgnivorusFood(itemstack) && hand == InteractionHand.MAIN_HAND) {
                return handleMounting(player);
            }
        }
        if (isIgnivorusFood(itemstack)) {
            return handleFeeding(player, itemstack);
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleBreeding(Player player, ItemStack itemstack) {
        return DragonBreedingInteractionHelper.handleBreeding(
                dragon,
                player,
                itemstack,
                dragon::canFeed,
                "entity.saintsdragons.ignivorus.still_eating",
                61,
                () -> {
                    dragon.triggerAnim("interaction", "eat");
                    playEatSound();
                },
                dragon::setFeedingCooldown
        );
    }

    private InteractionResult handleBabyTaming(Player player, ItemStack itemstack, DragonAttributeConfig config) {
        var baby = dragon.getBabyComponent();
        boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
        boolean validFood = dragon.isFood(itemstack);
        if (baby == null) {
            return validFood ? InteractionResult.sidedSuccess(dragon.level().isClientSide) : InteractionResult.PASS;
        }

        double tameChance = getTamingChance(itemstack, config);
        return baby.tryHandleBabyFoodTaming(
                player,
                itemstack,
                "entity.saintsdragons.ignivorus",
                validFood,
                dragon.canFeed(),
                61,
                hearty,
                () -> {
                    dragon.triggerAnim("interaction", "eat");
                    playEatSound();
                },
                dragon::setFeedingCooldown,
                tameChance,
                () -> {
                    dragon.tame(player);
                    dragon.setOrderedToSit(true);
                    dragon.setCommand(1);
                    dragon.combatManager.clearAbilityCooldown(ModAbilities.IGNIVORUS_ULTIMATE);
                    triggerTamingAdvancement(player);
                }
        );
    }

    private InteractionResult handleFeeding(Player player, ItemStack itemstack) {
        var baby = dragon.getBabyComponent();
        if (baby != null && !baby.ensureCanFeed(player, "entity.saintsdragons.ignivorus", dragon.canFeed())) {
            return InteractionResult.CONSUME;
        }

        if (!dragon.level().isClientSide) {
            consumeHeldItem(player, itemstack);
            dragon.triggerAnim("interaction", "eat");
            playEatSound();
            dragon.setFeedingCooldown(23);
            boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            boolean beef = itemstack.is(net.minecraft.world.item.Items.BEEF);
            boolean wasHungry = dragon.isHungry();
            if (dragon.isBaby()) {
                if (baby != null) {
                    baby.applyBabyGrowth(player, hearty, "entity.saintsdragons.ignivorus", 2400, 4800);
                }
            } else {
                float currentHealth = dragon.getHealth();
                float healAmount = hearty ? 30.0F : (beef ? 16.0F : 10.0F);
                float newHealth = Math.min(currentHealth + healAmount, dragon.getMaxHealth());
                dragon.setHealth(newHealth);
                dragon.applyFeedingHunger(hearty);
                if (hearty) {
                    dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                }

                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
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


    private void playEatSound() {
        if (!dragon.level().isClientSide) {
            float pitch = dragon.isBaby() ? 1.6f : 1.0f;
            dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_EAT.get(), 1.0f, pitch, 75);
        }
    }

    private InteractionResult handleMounting(Player player) {
        return handleStandardMounting(player);
    }

    private Float nextFailureHealTarget() {
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
        return dragon.isFood(itemstack);
    }

    private double getTamingChance(ItemStack food, DragonAttributeConfig config) {
        if (food.is(ModItems.HEARTY_DRAGON_MEAL.get())) {
            return config.extraDouble("taming_chance_hearty", 25.0D);
        }
        if (food.is(net.minecraft.world.item.Items.BEEF)) {
            return config.extraDouble("taming_chance_beef", 20.0D);
        }
        if (food.is(net.minecraft.world.item.Items.MUTTON)) {
            return config.extraDouble("taming_chance_mutton", 14.2857D);
        }
        if (food.is(net.minecraft.world.item.Items.PORKCHOP)) {
            return config.extraDouble("taming_chance_porkchop", 14.2857D);
        }
        return config.extraDouble("taming_chance_base", 14.2857D);
    }
}
