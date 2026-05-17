package com.leon.saintsdragons.server.entity.dragons.handlers;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.dragons.util.DragonBreedingRules;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.IntConsumer;

public abstract class AbstractDragonInteractionHandler<T extends RideableDragonBase> {
    protected final T dragon;

    protected AbstractDragonInteractionHandler(T dragon) {
        this.dragon = dragon;
    }

    public final InteractionResult handleInteraction(Player player, InteractionHand hand) {
        if (dragon.isDying()) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (ModItems.isDragonBrush(heldItem)) {
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

    protected void sendStatusMessage(Player player, String key, Object... args) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable(key, args), true);
        }
    }

    protected boolean checkBreedingEnabled(Player player) {
        return DragonBreedingRules.checkEnabled(player);
    }

    protected void consumeHeldItem(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    protected InteractionResult handleCommandCycling(Player player, String messagePrefix) {
        boolean client = dragon.level().isClientSide;
        if (dragon.isInSitTransition()) {
            if (!client) {
                String key = dragon.isSittingDownAnimation()
                        ? messagePrefix + ".sitting_down"
                        : dragon.isStandingUpAnimation()
                        ? messagePrefix + ".standing_up"
                        : messagePrefix + ".transitioning";
                player.displayClientMessage(Component.translatable(key, dragon.getName()), true);
            }
            return InteractionResult.sidedSuccess(client);
        }

        int nextCommand = dragon.getNextCommand();
        dragon.setCommand(nextCommand);
        if (!client) {
            player.displayClientMessage(
                    Component.translatable("entity.saintsdragons.all.command_" + nextCommand, dragon.getName()),
                    true
            );
        }
        return InteractionResult.SUCCESS;
    }

    protected InteractionResult handleStandardMounting(Player player) {
        if (!dragon.canOwnerMount(player) || dragon.isVehicle()) {
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }
        if (!dragon.level().isClientSide) {
            dragon.prepareForMounting();
            if (!player.startRiding(dragon)) {
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }

    protected InteractionResult tryHandleGrowthStuntingFood(Player player,
                                                            ItemStack heldItem,
                                                            String translationPrefix,
                                                            boolean canFeed,
                                                            int feedingCooldownTicks,
                                                            Runnable eatFeedback,
                                                            IntConsumer feedingCooldownSetter) {
        var baby = dragon.getBabyComponent();
        if (baby == null) {
            return InteractionResult.PASS;
        }
        return baby.tryStuntGrowth(
                player,
                heldItem,
                translationPrefix,
                canFeed,
                feedingCooldownTicks,
                eatFeedback,
                feedingCooldownSetter
        );
    }

    protected abstract Item getBinderItem();

    protected abstract InteractionResult handleUntamedInteraction(Player player, InteractionHand hand, ItemStack heldItem);

    protected abstract InteractionResult handleTamedInteraction(Player player, InteractionHand hand, ItemStack heldItem);
}
