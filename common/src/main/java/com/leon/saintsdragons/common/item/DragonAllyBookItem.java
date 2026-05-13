package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.client.ui.DraconicCodexScreen;
import com.leon.saintsdragons.client.ui.codex.CodexTab;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class DragonAllyBookItem extends Item {
    public DragonAllyBookItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (target instanceof DragonEntity dragon) {
            if (player.level().isClientSide) {
                java.util.UUID selectionId = (dragon.isTame() && dragon.isOwnedBy(player)) ? dragon.getUUID() : null;
                openCodexScreen(selectionId, CodexTab.PHYSIOLOGY);
            }
            return InteractionResult.SUCCESS;
        }
        
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (level.isClientSide) {
            openCodexScreen(null, CodexTab.PHYSIOLOGY);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Environment(EnvType.CLIENT)
    private void openCodexScreen(@Nullable java.util.UUID preselectedDragonId, CodexTab initialTab) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new DraconicCodexScreen(preselectedDragonId, initialTab));
    }
}
