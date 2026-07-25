package com.leon.saintsdragons.server.entity.dragons.atroxiia.handlers;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.leon.saintsdragons.server.entity.dragons.handlers.AbstractDragonInteractionHandler;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class AtroxiiaInteractionHandler extends AbstractDragonInteractionHandler<Atroxiia> {
    public AtroxiiaInteractionHandler(Atroxiia dragon) {
        super(dragon);
    }

    @Override
    protected Item getBinderItem() {
        return ModItems.DRACONIC_CODEX.get();
    }

    @Override
    protected boolean isInteractionItem(ItemStack heldItem) {
        return heldItem.is(ModItems.DRACONIC_CODEX.get());
    }

    @Override
    protected InteractionResult handleUntamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
        if (dragon.isFood(heldItem)) {
            return handleFeeding(player, heldItem);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult handleTamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
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

    private InteractionResult handleFeeding(Player player, ItemStack heldItem) {
        if (!dragon.canFeed()) {
            sendStatusMessage(player, "entity.saintsdragons.atroxiia.still_eating");
            return InteractionResult.CONSUME;
        }

        if (!dragon.level().isClientSide) {
            boolean heartyMeal = heldItem.is(ModItems.HEARTY_DRAGON_MEAL.get());
            boolean wasHungry = dragon.isHungry();
            consumeHeldItem(player, heldItem);

            dragon.triggerAnim(AnimationHelper.INTERACTION_CONTROLLER, AnimationHelper.EAT);
            dragon.playEatMovingSound();
            dragon.setFeedingCooldown(Atroxiia.EAT_ANIMATION_TICKS);

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

        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }
}
