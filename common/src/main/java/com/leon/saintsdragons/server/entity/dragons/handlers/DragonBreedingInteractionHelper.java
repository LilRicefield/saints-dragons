package com.leon.saintsdragons.server.entity.dragons.handlers;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.util.DragonBreedingRules;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

public final class DragonBreedingInteractionHelper {
    private static final String PREFIX = "entity.saintsdragons.dragon";

    private DragonBreedingInteractionHelper() {
    }

    public static InteractionResult handleBreeding(DragonEntity dragon,
                                                   Player player,
                                                   ItemStack food,
                                                   BooleanSupplier canFeed,
                                                   String stillEatingKey,
                                                   int feedingCooldownTicks,
                                                   Runnable eatFeedback,
                                                   IntConsumer feedingCooldownSetter) {
        boolean client = dragon.level().isClientSide;
        if (!dragon.canReceiveFoodFrom(player)) {
            return InteractionResult.PASS;
        }
        if (!client && !DragonBreedingRules.checkEnabled(player)) {
            return InteractionResult.CONSUME;
        }

        if (!canFeed.getAsBoolean()) {
            sendStatus(player, stillEatingKey, dragon);
            return InteractionResult.CONSUME;
        }

        if (dragon.isBaby()) {
            sendStatus(player, PREFIX + ".breeding_too_young", dragon);
            return InteractionResult.sidedSuccess(client);
        }

        if (dragon.getAge() != 0) {
            sendStatus(player, PREFIX + ".breeding_cooling_down", dragon);
            return InteractionResult.sidedSuccess(client);
        }

        if (dragon.isInLove()) {
            sendStatus(player, PREFIX + ".breeding_already_ready", dragon);
            return InteractionResult.sidedSuccess(client);
        }

        if (!client) {
            if (!player.getAbilities().instabuild) {
                food.shrink(1);
            }
            eatFeedback.run();
            if (feedingCooldownTicks > 0) {
                feedingCooldownSetter.accept(feedingCooldownTicks);
            }
            dragon.setInLove(player);
            sendStatus(player, PREFIX + ".breeding_ready", dragon);
        }

        return InteractionResult.sidedSuccess(client);
    }

    private static void sendStatus(Player player, String key, DragonEntity dragon) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable(key, dragon.getName()), true);
        }
    }
}
