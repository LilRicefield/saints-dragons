package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.client.screen.DragonAllyScreen;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Item used to open the wyvern ally management GUI.
 * Right-click on a wyvern to manage its allies.
 */
public class DragonAllyBookItem extends Item {
    public DragonAllyBookItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (target instanceof DragonEntity dragon) {
            // Check if player owns the wyvern
            if (!dragon.isTame() || !dragon.isOwnedBy(player)) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("saintsdragons.message.not_dragon_owner"), 
                    true);
                return InteractionResult.FAIL;
            }
            
            // Open ally management GUI on client
            if (player.level().isClientSide) {
                openAllyScreen(dragon, player);
            }
            
            return InteractionResult.SUCCESS;
        }
        
        return super.interactLivingEntity(stack, player, target, hand);
    }
    
    @OnlyIn(Dist.CLIENT)
    private void openAllyScreen(DragonEntity dragon, Player player) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new DragonAllyScreen(dragon));
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<net.minecraft.network.chat.Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("saintsdragons.tooltip.dragon_ally_book.line1"));
        tooltip.add(net.minecraft.network.chat.Component.translatable("saintsdragons.tooltip.dragon_ally_book.line2"));
        tooltip.add(net.minecraft.network.chat.Component.translatable("saintsdragons.tooltip.dragon_ally_book.line3"));
    }
}
