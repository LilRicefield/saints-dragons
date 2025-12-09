package com.leon.saintsdragons.common.item.dragonfood;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Dragon-only hearty meal; players cannot eat it directly.
 */
public class HeartyDragonMealItem extends Item {
    public HeartyDragonMealItem(Properties props) {
        super(props);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        // Block player self-use; dragon feeding is handled via mobInteract on dragons.
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }
}
