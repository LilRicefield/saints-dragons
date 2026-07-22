package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class IvyOctopusPlushieItem extends Item {
    private static final int USE_COOLDOWN_TICKS = 40;

    public IvyOctopusPlushieItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        IvyTheDragonMerchant ivy = findLoadedIvy(serverPlayer);
        if (ivy == null) {
            player.displayClientMessage(Component.translatable("message.saintsdragons.ivy_plushie.not_loaded"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!ivy.summonNear(serverPlayer)) {
            player.displayClientMessage(Component.translatable("message.saintsdragons.ivy_plushie.no_safe_position"), true);
            return InteractionResultHolder.fail(stack);
        }

        player.getCooldowns().addCooldown(this, USE_COOLDOWN_TICKS);
        return InteractionResultHolder.success(stack);
    }

    private static IvyTheDragonMerchant findLoadedIvy(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return null;
        }
        IvyTheDragonMerchant sameDimension = findLoadedIvy(player.serverLevel(), player);
        if (sameDimension != null) {
            return sameDimension;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level == player.serverLevel()) {
                continue;
            }
            IvyTheDragonMerchant ivy = findLoadedIvy(level, player);
            if (ivy != null) {
                return ivy;
            }
        }
        return null;
    }

    private static IvyTheDragonMerchant findLoadedIvy(ServerLevel level, ServerPlayer player) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof IvyTheDragonMerchant ivy
                    && ivy.isAlive()
                    && ivy.isTame()
                    && ivy.isOwnedBy(player)) {
                return ivy;
            }
        }
        return null;
    }
}
