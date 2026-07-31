package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import net.minecraft.nbt.CompoundTag;
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

import javax.annotation.Nullable;
import java.util.UUID;

public final class IvyOctopusPlushieItem extends Item {
    private static final int USE_COOLDOWN_TICKS = 40;
    private static final String BOUND_IVY_UUID_TAG = "BoundIvyUUID";
    private static final String BOUND_OWNER_UUID_TAG = "BoundOwnerUUID";

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

        UUID boundOwnerUuid = getBoundOwnerUuid(stack);
        if (boundOwnerUuid != null && !boundOwnerUuid.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.saintsdragons.ivy_plushie.not_owner"), true);
            return InteractionResultHolder.fail(stack);
        }

        IvyTheDragonMerchant ivy = findLoadedIvy(serverPlayer, getBoundIvyUuid(stack));
        if (ivy == null) {
            player.displayClientMessage(Component.translatable("message.saintsdragons.ivy_plushie.not_loaded"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!ivy.summonNear(serverPlayer)) {
            player.displayClientMessage(Component.translatable("message.saintsdragons.ivy_plushie.no_safe_position"), true);
            return InteractionResultHolder.fail(stack);
        }

        bindTo(stack, ivy, serverPlayer);
        player.getCooldowns().addCooldown(this, USE_COOLDOWN_TICKS);
        return InteractionResultHolder.success(stack);
    }

    public static void bindTo(ItemStack stack, IvyTheDragonMerchant ivy, ServerPlayer owner) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(BOUND_IVY_UUID_TAG, ivy.getUUID());
        tag.putUUID(BOUND_OWNER_UUID_TAG, owner.getUUID());
    }

    public static boolean canRepresent(ItemStack stack, IvyTheDragonMerchant ivy, ServerPlayer owner) {
        if (!(stack.getItem() instanceof IvyOctopusPlushieItem)) {
            return false;
        }
        UUID boundOwnerUuid = getBoundOwnerUuid(stack);
        UUID boundIvyUuid = getBoundIvyUuid(stack);
        if (boundOwnerUuid == null && boundIvyUuid == null) {
            return true;
        }
        return owner.getUUID().equals(boundOwnerUuid) && ivy.getUUID().equals(boundIvyUuid);
    }

    @Nullable
    private static UUID getBoundIvyUuid(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(BOUND_IVY_UUID_TAG) ? tag.getUUID(BOUND_IVY_UUID_TAG) : null;
    }

    @Nullable
    private static UUID getBoundOwnerUuid(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(BOUND_OWNER_UUID_TAG) ? tag.getUUID(BOUND_OWNER_UUID_TAG) : null;
    }

    private static IvyTheDragonMerchant findLoadedIvy(ServerPlayer player, @Nullable UUID boundIvyUuid) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return null;
        }
        IvyTheDragonMerchant sameDimension = findLoadedIvy(player.serverLevel(), player, boundIvyUuid);
        if (sameDimension != null) {
            return sameDimension;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level == player.serverLevel()) {
                continue;
            }
            IvyTheDragonMerchant ivy = findLoadedIvy(level, player, boundIvyUuid);
            if (ivy != null) {
                return ivy;
            }
        }
        return null;
    }

    private static IvyTheDragonMerchant findLoadedIvy(ServerLevel level, ServerPlayer player,
                                                       @Nullable UUID boundIvyUuid) {
        if (boundIvyUuid != null) {
            Entity entity = level.getEntity(boundIvyUuid);
            return entity instanceof IvyTheDragonMerchant ivy
                    && ivy.isAlive()
                    && ivy.isTame()
                    && ivy.isOwnedBy(player)
                    ? ivy
                    : null;
        }
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
