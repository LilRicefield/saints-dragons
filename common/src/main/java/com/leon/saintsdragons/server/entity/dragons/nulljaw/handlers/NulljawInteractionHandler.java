package com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.handlers.AbstractDragonInteractionHandler;
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
public class NulljawInteractionHandler extends AbstractDragonInteractionHandler<Nulljaw> {

    public NulljawInteractionHandler(Nulljaw dragon) {
        super(dragon);
    }

    @Override
    protected InteractionResult handleUntamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
        // Check if legacy taming is enabled
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.NULLJAW_ID);
        boolean legacyTaming = config.extraBoolean("legacy_taming", false);

        if (dragon.isBaby()) {
            return handleBabyTaming(player, heldItem, config);
        }

        if (isNulljawFood(heldItem)) {
            if (legacyTaming) {
                // Legacy taming: simple food-based taming with RNG
                return handleLegacyTaming(player, heldItem);
            } else if (dragon.getHealth() < dragon.getMaxHealth()) {
                // Normal mode: only allow feeding for healing, not taming
                return handleFeeding(player, heldItem, true);
            }
        }

        // Rodeo taming (only in non-legacy mode)
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
                        Component.translatable("entity.saintsdragons.nulljaw.still_eating", dragon.getName()),
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
            dragon.setFeedingCooldown(40);

            boolean heartyMeal = food.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            boolean tropicalFish = food.is(net.minecraft.world.item.Items.TROPICAL_FISH);
            float healAmount = heartyMeal ? 35.0F : 5.0F;
            float newHealth = Math.min(dragon.getHealth() + healAmount, dragon.getMaxHealth());
            dragon.setHealth(newHealth);
            dragon.applyFeedingHunger(heartyMeal);

            if (heartyMeal) {
                dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }

            // Taming chance logic
            DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.NULLJAW_ID);
            double tameChance = heartyMeal
                    ? config.extraDoubles().getOrDefault("taming_chance", 6.0) / 2.0  // Hearty meal doubles chance
                    : tropicalFish
                        ? config.extraDoubles().getOrDefault("taming_chance_tropical", 4.0)
                        : config.extraDoubles().getOrDefault("taming_chance", 6.0);
            int tameRoll = (int) Math.round(tameChance);
            boolean success = dragon.getRandom().nextInt(Math.max(1, tameRoll)) == 0;

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

        if (player.isCrouching() && isNulljawFood(heldItem)) {
            return handleBreeding(player, heldItem);
        }

        if (isNulljawFood(heldItem)) {
            return handleFeeding(player, heldItem, false);
        }

        if (dragon.canOwnerCommand(player) && !isNulljawFood(heldItem) && hand == InteractionHand.MAIN_HAND) {
            return handleCommandCycling(player);
        }

        if (hand == InteractionHand.MAIN_HAND && !isNulljawFood(heldItem) && !player.isCrouching()) {
            return handleMounting(player);
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleBreeding(Player player, ItemStack food) {
        boolean client = dragon.level().isClientSide;

        if (!dragon.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("entity.saintsdragons.nulljaw.still_eating", dragon.getName()),
                        true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (dragon.isBaby()) {
            sendStatusMessage(player, "entity.saintsdragons.nulljaw.breeding_too_young");
            return InteractionResult.sidedSuccess(client);
        }

        if (dragon.getAge() != 0) {
            sendStatusMessage(player, "entity.saintsdragons.nulljaw.breeding_cooling_down");
            return InteractionResult.sidedSuccess(client);
        }

        if (dragon.isInLove()) {
            sendStatusMessage(player, "entity.saintsdragons.nulljaw.breeding_already_ready");
            return InteractionResult.sidedSuccess(client);
        }

        if (!client) {
            if (!player.getAbilities().instabuild) {
                food.shrink(1);
            }

            dragon.triggerAnim("action", "eat");
            dragon.setFeedingCooldown(61);
            dragon.setInLove(player);
            sendStatusMessage(player, "entity.saintsdragons.nulljaw.breeding_ready");
        }

        return InteractionResult.sidedSuccess(client);
    }

    private InteractionResult handleFeeding(Player player, ItemStack food, boolean untamed) {
        if (!dragon.canFeed()) {
            if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("entity.saintsdragons.nulljaw.still_eating", dragon.getName()),
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
            dragon.setFeedingCooldown(50);

            boolean heartyMeal = food.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            boolean wasHungry = dragon.isHungry();
            if (dragon.isBaby()) {
                int growthTicks = heartyMeal ? 4800 : 2400;
                int currentAge = dragon.getAge();
                int newAge = Math.min(0, currentAge + growthTicks);
                dragon.setAge(newAge);

                dragon.level().broadcastEntityEvent(dragon, (byte) 7);

                if (player instanceof ServerPlayer serverPlayer) {
                    String messageKey = (newAge == 0)
                            ? "entity.saintsdragons.nulljaw.baby_grown"
                            : "entity.saintsdragons.nulljaw.baby_fed";
                    serverPlayer.displayClientMessage(
                            Component.translatable(messageKey, dragon.getName()),
                            true
                    );
                }
                dragon.applyFeedingHunger(heartyMeal);
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
                        messageKey = wasHungry ? "entity.saintsdragons.dragon.feeding" : "entity.saintsdragons.nulljaw.fed";
                    } else {
                        messageKey = "entity.saintsdragons.nulljaw.fed_partial";
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
        boolean client = dragon.level().isClientSide;
        boolean heartyMeal = food.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
        boolean tropicalFish = food.is(net.minecraft.world.item.Items.TROPICAL_FISH);
        if (!isNulljawFood(food)) {
            return InteractionResult.PASS;
        }

        if (!dragon.canFeed()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("entity.saintsdragons.nulljaw.still_eating", dragon.getName()),
                        true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!client) {
            if (!player.getAbilities().instabuild) {
                food.shrink(1);
            }

            dragon.triggerAnim("action", "eat");
            dragon.setFeedingCooldown(50);

            if (heartyMeal) {
                dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            }
            dragon.applyFeedingHunger(heartyMeal);

            double tameChance = heartyMeal
                    ? config.extraDoubles().getOrDefault("taming_chance", 6.0) / 2.0
                    : tropicalFish
                        ? config.extraDoubles().getOrDefault("taming_chance_tropical", 4.0)
                        : config.extraDoubles().getOrDefault("taming_chance", 6.0);
            int tameRoll = (int) Math.round(tameChance);
            boolean success = dragon.getRandom().nextInt(Math.max(1, tameRoll)) == 0;

            if (success) {
                dragon.tame(player);
                dragon.setOrderedToSit(true);
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
                dragon.awardTamingAdvancement(player);
            } else {
                dragon.level().broadcastEntityEvent(dragon, (byte) 6);
            }
        }

        return InteractionResult.sidedSuccess(client);
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
        if (dragon.isInSitTransition()) {
            if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                String messageKey;
                if (dragon.isSittingDownAnimation()) {
                    messageKey = "entity.saintsdragons.nulljaw.sitting_down";
                } else if (dragon.isStandingUpAnimation()) {
                    messageKey = "entity.saintsdragons.nulljaw.standing_up";
                } else {
                    messageKey = "entity.saintsdragons.nulljaw.transitioning";
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
            case 0 -> dragon.setOrderedToSit(false); // Follow
            case 1 -> dragon.setOrderedToSit(true);  // Sit
            case 2 -> dragon.setOrderedToSit(false); // Wander
            default -> {
            }
        }
    }

    private boolean isNulljawFood(ItemStack itemstack) {
        return dragon.isFood(itemstack)
                || itemstack.is(net.minecraft.world.item.Items.SALMON)
                || itemstack.is(net.minecraft.world.item.Items.COD)
                || itemstack.is(net.minecraft.world.item.Items.TROPICAL_FISH)
                || itemstack.is(ModItems.HEARTY_DRAGON_MEAL.get());
    }


    @Override
    protected net.minecraft.world.item.Item getBinderItem() {
        return ModItems.NULLJAW_BINDER.get();
    }
}
