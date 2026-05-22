package com.leon.saintsdragons.server.entity.dragons.raevyx.handlers;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.config.dragon.DragonTamingChance;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.dragons.handlers.AbstractDragonInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonBreedingInteractionHelper;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
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

public class RaevyxInteractionHandler extends AbstractDragonInteractionHandler<Raevyx> {
    public RaevyxInteractionHandler(Raevyx dragon) {
        super(dragon);
    }

    @Override
    protected InteractionResult handleUntamedInteraction(Player player, InteractionHand hand, ItemStack itemstack) {
        boolean client = dragon.level().isClientSide;

        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        boolean legacyTaming = config.extraBoolean("legacy_taming", false);

        if (dragon.isBaby()) {
            return handleBabyTaming(player, itemstack, config);
        }

        if (!legacyTaming && dragon.isTamingStunned() && player.isCrouching() && itemstack.isEmpty()) {
            if (!client) {
                dragon.abortTamingAttempt();
                sendStatusMessage(player, "entity.saintsdragons.raevyx.taming_aborted");
            }
            return InteractionResult.sidedSuccess(client);
        }

        if (!dragon.isFood(itemstack)) {
            return InteractionResult.PASS;
        }

        if (!legacyTaming) {
            if (dragon.isTamingStunned()) {
                if (!dragon.isAwaitingTamingFeed()) {
                    sendStatusMessage(player, "entity.saintsdragons.raevyx.taming_dazed");
                    return InteractionResult.CONSUME;
                }
            }
        }
        if (!dragon.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                    Component.translatable("entity.saintsdragons.raevyx.still_eating", dragon.getName()),
                    true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!legacyTaming) {
            float minRequiredHealth = dragon.getTamingThreshold();
            if (dragon.getHealth() > minRequiredHealth + 1.0F) {
                sendStatusMessage(player, "entity.saintsdragons.raevyx.taming_need_weakened", dragon.getName(), Math.round(minRequiredHealth));
                return InteractionResult.CONSUME;
            }
        }
        if (!client) {
            if (!player.getAbilities().instabuild) {
                consumeHeldItem(player, itemstack);
            }

            dragon.triggerAnim("interaction", "eat");
            dragon.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_EAT.get(), 1.0f, dragon.isBaby() ? 1.6f : 1.0f, 56);
            dragon.setFeedingCooldown(61);
            boolean hearty = itemstack.is(ModItems.HEARTY_DRAGON_MEAL.get());
            if (hearty) {
                dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }
            dragon.applyFeedingHunger(hearty);


            if (legacyTaming) {
                float healAmount = hearty ? 28.0f : 10.0f;
                float newHealth = Math.min(dragon.getHealth() + healAmount, dragon.getMaxHealth());
                dragon.setHealth(newHealth);
            } else {
                dragon.enterTamingStun();
            }
            double tameChance = hearty
                ? config.extraDoubles().getOrDefault("taming_chance_hearty", 3.0)
                : config.extraDoubles().getOrDefault("taming_chance_base", 5.0);
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
                    Float healTarget = nextFailureHealTarget();
                    dragon.setTamingRecoveryTarget(healTarget);
                    dragon.incrementTamingFailures();
                }
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
                sendStatusMessage(player, "entity.saintsdragons.raevyx.taming_failed");
            }
        }

        return InteractionResult.sidedSuccess(client);
    }

    private InteractionResult handleBabyTaming(Player player, ItemStack itemstack, DragonAttributeConfig config) {
        var baby = dragon.getBabyComponent();
        boolean hearty = itemstack.is(ModItems.HEARTY_DRAGON_MEAL.get());
        boolean validFood = dragon.isFood(itemstack);
        if (baby == null) {
            return validFood ? InteractionResult.sidedSuccess(dragon.level().isClientSide) : InteractionResult.PASS;
        }

        double tameChance = hearty
            ? config.extraDoubles().getOrDefault("taming_chance_hearty", 3.0)
            : config.extraDoubles().getOrDefault("taming_chance_base", 5.0);
        return baby.tryHandleBabyFoodTaming(
                player,
                itemstack,
                "entity.saintsdragons.raevyx",
                validFood,
                dragon.canFeed(),
                61,
                hearty,
                () -> {
                    dragon.triggerAnim("interaction", "eat");
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_EAT.get(), 1.0f, dragon.isBaby() ? 1.6f : 1.0f, 56);
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

    @Override
    protected InteractionResult handleTamedInteraction(Player player, InteractionHand hand, ItemStack itemstack) {
        boolean isOwner = player.equals(dragon.getOwner());
        InteractionResult growthStuntResult = tryHandleGrowthStuntingFood(
                player,
                itemstack,
                "entity.saintsdragons.raevyx",
                dragon.canFeed(),
                22,
                () -> {
                    dragon.triggerAnim("interaction", "eat");
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_EAT.get(), 1.0f, 1.6f, 56);
                },
                dragon::setFeedingCooldown
        );
        if (growthStuntResult != InteractionResult.PASS) {
            return growthStuntResult;
        }

        // Handle feeding for healing
        if (dragon.isFood(itemstack)) {
            if (player.isCrouching() && isOwner) {
                return handleBreeding(player, itemstack);
            }
            return handleFeeding(player, itemstack);
        }

        if (isOwner) {
            boolean isSleeping = dragon.isSleeping() || dragon.isSleepTransitioning();

            if (canOwnerCommand(player) && !dragon.isFood(itemstack) && hand == InteractionHand.MAIN_HAND) {
                if (isSleeping) {
                    if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.displayClientMessage(
                            Component.translatable("entity.saintsdragons.raevyx.sleeping", dragon.getName()),
                            true
                        );
                    }
                    return InteractionResult.sidedSuccess(dragon.level().isClientSide);
                }
                return handleCommandCycling(player);
            }
            else if (!player.isCrouching() && !dragon.isFood(itemstack) && hand == InteractionHand.MAIN_HAND && canOwnerMount(player)) {
                return handleMounting(player);
            }
        }
        
        return InteractionResult.PASS;
    }

    private InteractionResult handleBreeding(Player player, ItemStack itemstack) {
        var baby = dragon.getBabyComponent();
        if (baby != null && !baby.ensureCanFeed(player, "entity.saintsdragons.raevyx", dragon.canFeed())) {
            return InteractionResult.CONSUME;
        }

        return DragonBreedingInteractionHelper.handleBreeding(
                dragon,
                player,
                itemstack,
                dragon::canFeed,
                "entity.saintsdragons.raevyx.still_eating",
                61,
                () -> {
                    dragon.triggerAnim("interaction", "eat");
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_EAT.get(), 1.0f, dragon.isBaby() ? 1.6f : 1.0f, 56);
                },
                dragon::setFeedingCooldown
        );
    }

    private InteractionResult handleFeeding(Player player, ItemStack itemstack) {
        var baby = dragon.getBabyComponent();
        if (baby != null && !baby.ensureCanFeed(player, "entity.saintsdragons.raevyx", dragon.canFeed())) {
            return InteractionResult.CONSUME;
        }

        if (!dragon.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                consumeHeldItem(player, itemstack);
            }
            dragon.triggerAnim("interaction", "eat");
            dragon.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_EAT.get(), 1.0f, dragon.isBaby() ? 1.6f : 1.0f, 56);
            dragon.setFeedingCooldown(22);

            boolean heartyMeal = itemstack.is(ModItems.HEARTY_DRAGON_MEAL.get());
            if (heartyMeal) {
                dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }
            boolean wasHungry = dragon.isHungry();

            if (dragon.isBaby()) {
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
                if (baby != null) {
                    baby.applyBabyGrowth(player, heartyMeal, "entity.saintsdragons.raevyx", 2400, 4800);
                }
            } else {
                float healAmount = heartyMeal ? 28.0f : 10.0f;
                float oldHealth = dragon.getHealth();
                float newHealth = Math.min(oldHealth + healAmount, dragon.getMaxHealth());
                dragon.setHealth(newHealth);
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
                dragon.applyFeedingHunger(heartyMeal);
                sendFeedingMessage(player, newHealth, wasHungry);
            }
        }

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }

    private InteractionResult handleCommandCycling(Player player) {
        return super.handleCommandCycling(player, "entity.saintsdragons.raevyx");
    }

    private Float nextFailureHealTarget() {
        return dragon.getMaxHealth();
    }

    private InteractionResult handleMounting(Player player) {
        return handleStandardMounting(player);
    }

    private void triggerTamingAdvancement(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var advancement = serverPlayer.server.getAdvancements()
                .getAdvancement(SaintsDragonsCommon.rl("tame_raevyx"));
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "tame_raevyx");
            }
        }
    }

    private void sendFeedingMessage(Player player, float newHealth, boolean wasHungry) {
        if (player instanceof ServerPlayer serverPlayer) {
            String messageKey;
            if (newHealth >= dragon.getMaxHealth()) {
                messageKey = wasHungry ? "entity.saintsdragons.dragon.feeding" : "entity.saintsdragons.raevyx.fed";
            } else {
                messageKey = "entity.saintsdragons.raevyx.fed_partial";
            }
                
            serverPlayer.displayClientMessage(
                Component.translatable(messageKey, dragon.getName()),
                true
            );
        }
    }
    private boolean canOwnerCommand(Player player) {
        return dragon.canOwnerCommand(player);
    }

    private boolean canOwnerMount(Player player) {
        return dragon.canOwnerMount(player);
    }

    @Override
    protected Item getBinderItem() {
        return ModItems.RAEVYX_BINDER.get();
    }
}
