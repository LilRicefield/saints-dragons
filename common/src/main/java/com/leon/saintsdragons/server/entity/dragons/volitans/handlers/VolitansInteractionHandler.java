package com.leon.saintsdragons.server.entity.dragons.volitans.handlers;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class VolitansInteractionHandler {
    private final Volitans dragon;

    public VolitansInteractionHandler(Volitans dragon) {
        this.dragon = dragon;
    }

    public InteractionResult handleInteraction(Player player, InteractionHand hand) {
        if (dragon.isDying()) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (ModItems.isDragonBrush(heldItem)) {
            if (!dragon.level().isClientSide) {
                dragon.tryBrush(player, heldItem);
            }
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }

        if (!dragon.isTame() || !dragon.isOwnedBy(player) || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (player.isCrouching() && heldItem.isEmpty() && dragon.canOwnerCommand(player)) {
            return handleCommandCycling(player);
        }

        if (!player.isCrouching() && heldItem.isEmpty() && !dragon.isVehicle() && dragon.canOwnerMount(player)) {
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

        return InteractionResult.PASS;
    }

    private InteractionResult handleCommandCycling(Player player) {
        if (dragon.isInSitTransition()) {
            if (!dragon.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                String messageKey;
                if (dragon.isSittingDownAnimation()) {
                    messageKey = "entity.saintsdragons.volitans.sitting_down";
                } else if (dragon.isStandingUpAnimation()) {
                    messageKey = "entity.saintsdragons.volitans.standing_up";
                } else {
                    messageKey = "entity.saintsdragons.volitans.transitioning";
                }
                serverPlayer.displayClientMessage(Component.translatable(messageKey, dragon.getName()), true);
            }
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }

        int currentCommand = dragon.getCommand();
        int nextCommand = (currentCommand + 1) % 3; // 0=Follow, 1=Sit, 2=Wander
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
}
