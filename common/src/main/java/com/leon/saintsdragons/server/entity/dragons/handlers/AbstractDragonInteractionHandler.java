package com.leon.saintsdragons.server.entity.dragons.handlers;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractDragonInteractionHandler<T extends DragonEntity> {
    protected final T dragon;

    protected AbstractDragonInteractionHandler(T dragon) {
        this.dragon = dragon;
    }

    public final InteractionResult handleInteraction(Player player, InteractionHand hand) {
        if (dragon.isDying()) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.is(ModItems.DRAGON_BRUSH.get())) {
            // Always acknowledge brush use on client so server interaction still runs.
            if (!dragon.level().isClientSide) {
                dragon.tryBrush(player, heldItem);
            }
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }

        if (isInteractionItem(heldItem)) {
            return InteractionResult.PASS;
        }

        if (!dragon.isTame()) {
            return handleUntamedInteraction(player, hand, heldItem);
        }
        return handleTamedInteraction(player, hand, heldItem);
    }

    protected boolean isInteractionItem(ItemStack heldItem) {
        return heldItem.is(ModItems.DRACONIC_CODEX.get()) || heldItem.is(getBinderItem());
    }

    protected void sendStatusMessage(Player player, String key) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable(key, dragon.getName()), true);
        }
    }

    protected abstract Item getBinderItem();

    protected abstract InteractionResult handleUntamedInteraction(Player player, InteractionHand hand, ItemStack heldItem);

    protected abstract InteractionResult handleTamedInteraction(Player player, InteractionHand hand, ItemStack heldItem);
}
