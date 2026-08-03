package com.leon.saintsdragons.server.entity.dragons.handlers;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.DragonSaddleCarrier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DragonSaddleInteractionHelper {
    private DragonSaddleInteractionHelper() {
    }

    public static InteractionResult handle(
            RideableDragonBase dragon,
            DragonSaddleCarrier carrier,
            Player player,
            InteractionHand hand,
            ItemStack heldItem
    ) {
        if (hand != InteractionHand.MAIN_HAND || !dragon.isOwnedBy(player) || dragon.isBaby()) {
            return InteractionResult.PASS;
        }

        if (heldItem.is(Items.SADDLE) && !carrier.hasSaddle()) {
            if (!dragon.level().isClientSide) {
                carrier.setSaddle(true);
                consumeOne(player, heldItem);
                dragon.playSound(SoundEvents.HORSE_SADDLE, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }

        if (heldItem.is(Items.CHEST) && !carrier.hasAttachedChest()) {
            if (!carrier.hasSaddle()) {
                sendMessage(player, "entity.saintsdragons.all.chest_requires_saddle", dragon.getName());
                return InteractionResult.sidedSuccess(dragon.level().isClientSide);
            }
            if (!dragon.level().isClientSide) {
                carrier.setAttachedChest(true);
                consumeOne(player, heldItem);
                dragon.playSound(SoundEvents.DONKEY_CHEST, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }

        if (!player.isCrouching() && !carrier.hasSaddle()) {
            sendMessage(player, "entity.saintsdragons.all.mount_requires_saddle", dragon.getName());
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }

        return InteractionResult.PASS;
    }

    private static void consumeOne(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private static void sendMessage(Player player, String key, Object... args) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable(key, args), true);
        }
    }
}
