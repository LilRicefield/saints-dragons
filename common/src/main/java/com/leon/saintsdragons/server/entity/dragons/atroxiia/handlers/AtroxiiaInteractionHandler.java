package com.leon.saintsdragons.server.entity.dragons.atroxiia.handlers;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.leon.saintsdragons.server.entity.dragons.handlers.AbstractDragonInteractionHandler;
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
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult handleTamedInteraction(Player player, InteractionHand hand, ItemStack heldItem) {
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
}
