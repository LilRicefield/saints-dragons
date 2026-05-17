package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.config.dragon.DragonTamingChance;
import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.network.chat.Component;
import java.util.UUID;
import java.util.function.IntConsumer;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DragonBabyComponent {
    private final DragonEntity dragon;

    public DragonBabyComponent(DragonEntity dragon) {
        this.dragon = dragon;
    }

    @Nullable
    public UUID resolveEggOwnerUUID(@Nullable DragonEntity partner) {
        if (dragon.isTame() && dragon.getOwnerUUID() != null) {
            return dragon.getOwnerUUID();
        }
        if (partner != null && partner.isTame() && partner.getOwnerUUID() != null) {
            return partner.getOwnerUUID();
        }
        return null;
    }

    public void registerToOwnerCodex(@Nullable DragonEntity offspring, @Nullable ServerLevel level) {
        if (offspring == null || level == null || level.isClientSide) {
            return;
        }
        if (offspring.isTame() && offspring.getOwnerUUID() != null) {
            DragonCodexSavedData.get(level).addDragon(offspring.getOwnerUUID(), offspring);
        }
    }

    public boolean ensureCanFeed(Player player, String translationPrefix, boolean canFeed) {
        if (canFeed) {
            return true;
        }
        if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(
                    Component.translatable(translationPrefix + ".still_eating", dragon.getName()),
                    true
            );
        }
        return false;
    }

    public InteractionResult tryHandleBabyFoodTaming(Player player,
                                                     ItemStack food,
                                                     String translationPrefix,
                                                     boolean validFood,
                                                     boolean canFeed,
                                                     int feedingCooldownTicks,
                                                     boolean heartyMeal,
                                                     Runnable eatFeedback,
                                                     IntConsumer feedingCooldownSetter,
                                                     double tameChance,
                                                     Runnable onSuccess) {
        boolean client = dragon.level().isClientSide;
        if (!validFood) {
            return InteractionResult.PASS;
        }

        if (!ensureCanFeed(player, translationPrefix, canFeed)) {
            return InteractionResult.CONSUME;
        }

        if (!client) {
            handleBabyFoodTaming(
                    player,
                    food,
                    feedingCooldownTicks,
                    heartyMeal,
                    eatFeedback,
                    feedingCooldownSetter,
                    tameChance,
                    onSuccess
            );
        }

        return InteractionResult.sidedSuccess(client);
    }

    public void applyBabyGrowth(Player player, boolean heartyMeal, String translationPrefix, int normalGrowthTicks, int heartyGrowthTicks) {
        if (dragon.isGrowthStunted()) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable("entity.saintsdragons.dragon.growth_stunted", dragon.getName()), true);
            }
            dragon.applyFeedingHunger(heartyMeal);
            return;
        }

        int growthTicks = heartyMeal ? heartyGrowthTicks : normalGrowthTicks;
        int newAge = Math.min(0, dragon.getAge() + growthTicks);
        dragon.setAge(newAge);
        dragon.level().broadcastEntityEvent(dragon, (byte) 7);

        if (player instanceof ServerPlayer serverPlayer) {
            String messageKey = newAge == 0
                    ? translationPrefix + ".baby_grown"
                    : translationPrefix + ".baby_fed";
            serverPlayer.displayClientMessage(Component.translatable(messageKey, dragon.getName()), true);
        }

        dragon.applyFeedingHunger(heartyMeal);
    }

    public boolean isGrowthStuntingFood(ItemStack stack) {
        return stack.is(ModItems.RAW_MOOP.get());
    }

    public boolean canStuntGrowth(Player player, ItemStack stack) {
        return dragon.isBaby()
                && dragon.isTame()
                && dragon.isOwnedBy(player)
                && !dragon.isGrowthStunted()
                && isGrowthStuntingFood(stack);
    }

    public InteractionResult tryStuntGrowth(Player player,
                                            ItemStack food,
                                            String translationPrefix,
                                            boolean canFeed,
                                            int feedingCooldownTicks,
                                            Runnable eatFeedback,
                                            IntConsumer feedingCooldownSetter) {
        if (!isGrowthStuntingFood(food) || !dragon.isBaby() || !dragon.isTame()) {
            return InteractionResult.PASS;
        }
        if (!dragon.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }

        boolean client = dragon.level().isClientSide;
        if (dragon.isGrowthStunted()) {
            if (!client && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable("entity.saintsdragons.dragon.growth_stunted", dragon.getName()), true);
            }
            return InteractionResult.sidedSuccess(client);
        }

        if (!ensureCanFeed(player, translationPrefix, canFeed)) {
            return InteractionResult.CONSUME;
        }

        if (!client) {
            if (!player.getAbilities().instabuild) {
                food.shrink(1);
            }
            eatFeedback.run();
            feedingCooldownSetter.accept(feedingCooldownTicks);
            dragon.setGrowthStunted(true);
            dragon.applyFeedingHunger(false);
            dragon.level().broadcastEntityEvent(dragon, (byte) 7);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable("entity.saintsdragons.dragon.growth_stunted_now", dragon.getName()), true);
            }
        }

        return InteractionResult.sidedSuccess(client);
    }

    public void applyBabyTamingResult(Player player, boolean success, Runnable onSuccess) {
        if (success) {
            onSuccess.run();
            dragon.level().broadcastEntityEvent(dragon, (byte) 7);
        } else {
            dragon.level().broadcastEntityEvent(dragon, (byte) 6);
        }
    }

    public void handleBabyFoodTaming(Player player,
                                     ItemStack food,
                                     int feedingCooldownTicks,
                                     boolean heartyMeal,
                                     Runnable eatFeedback,
                                     IntConsumer feedingCooldownSetter,
                                     double tameChance,
                                     Runnable onSuccess) {
        if (!player.getAbilities().instabuild) {
            food.shrink(1);
        }

        eatFeedback.run();
        feedingCooldownSetter.accept(feedingCooldownTicks);

        if (heartyMeal) {
            dragon.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        }
        dragon.applyFeedingHunger(heartyMeal);

        boolean success = DragonTamingChance.rollPercent(dragon.getRandom(), tameChance);
        applyBabyTamingResult(player, success, onSuccess);
    }
}
