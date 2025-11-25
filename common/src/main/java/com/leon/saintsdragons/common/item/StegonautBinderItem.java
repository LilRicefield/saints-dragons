package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.component.BinderData;
import com.leon.saintsdragons.common.item.util.BinderComponentUtil;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Item used to bind a Primitive Drake for portable resistance buff.
 * Right-click on a tamed primitive drake to bind it to this item.
 * While carrying a bound drake binder, the player gets resistance buff.
 */
public class StegonautBinderItem extends Item {

    public StegonautBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (target instanceof Stegonaut drake) {
            if (!drake.isTame() || !drake.isOwnedBy(player)) {
                player.displayClientMessage(Component.translatable("saintsdragons.message.not_dragon_owner"), true);
                return InteractionResult.FAIL;
            }

            if (!drake.canBeBound()) {
                player.displayClientMessage(Component.translatable("saintsdragons.message.stegonaut_cannot_be_captured"), true);
                return InteractionResult.FAIL;
            }

            if (BinderComponentUtil.isBound(stack)) {
                player.displayClientMessage(Component.translatable("saintsdragons.message.binder_already_occupied"), true);
                return InteractionResult.FAIL;
            }

            ItemStack newStack = captureDrake(stack, drake, player);

            if (hand == InteractionHand.MAIN_HAND) {
                player.getInventory().setItem(player.getInventory().selected, newStack);
            } else {
                player.getInventory().offhand.set(0, newStack);
            }

            return InteractionResult.SUCCESS;
        }

        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public @NotNull InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player != null && BinderComponentUtil.isBound(stack)) {
            return releaseDrake(stack, player, context.getClickedPos())
                    ? InteractionResult.SUCCESS
                    : InteractionResult.FAIL;
        }

        return super.useOn(context);
    }

    @Override
    public net.minecraft.world.@NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (BinderComponentUtil.isBound(stack)) {
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        }

        return super.use(level, player, hand);
    }

    private ItemStack captureDrake(ItemStack stack, Stegonaut drake, Player player) {
        ItemStack newStack = stack.copy();

        net.minecraft.nbt.CompoundTag drakeData = new net.minecraft.nbt.CompoundTag();
        drake.addAdditionalSaveData(drakeData);

        LivingEntity owner = drake.getOwner();
        UUID ownerId = owner instanceof Player ownerPlayer ? ownerPlayer.getUUID() : null;
        String ownerName = owner instanceof Player ownerPlayer ? ownerPlayer.getName().getString() : null;
        BinderData data = BinderData.bound(
                drake.getUUID(),
                drake.getName().getString(),
                ownerId,
                ownerName,
                drake.getCustomName(),
                drakeData
        );
        BinderComponentUtil.setData(newStack, data);

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
                if (serverPlayer.getInventory().getItem(i) == stack) {
                    serverPlayer.getInventory().setItem(i, newStack);
                    break;
                }
            }
        }

        drake.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);

        player.displayClientMessage(Component.translatable("saintsdragons.message.stegonaut_captured", drake.getName().getString()), true);

        return newStack;
    }

    private boolean releaseDrake(ItemStack stack, Player player, net.minecraft.core.BlockPos pos) {
        BinderData data = BinderComponentUtil.getData(stack);
        if (!data.isBound() || data.dragonUuid().isEmpty()) {
            return false;
        }

        String drakeName = data.dragonName().orElse("");
        net.minecraft.nbt.CompoundTag drakeData = data.dragonData().orElse(null);
        UUID ownerUUID = data.ownerUuid().orElse(null);

        if (ownerUUID != null && !player.getUUID().equals(ownerUUID)) {
            player.displayClientMessage(Component.translatable("saintsdragons.message.not_dragon_owner"), true);
            return false;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return true;
        }

        Stegonaut newDrake = new Stegonaut(com.leon.saintsdragons.common.registry.ModEntities.STEGONAUT.get(), serverLevel);
        newDrake.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);

        if (drakeData != null) {
            newDrake.readAdditionalSaveData(drakeData);
        }

        newDrake.setUUID(java.util.UUID.randomUUID());

        if (ownerUUID != null) {
        newDrake.setTame(true, true);
            newDrake.setOwnerUUID(ownerUUID);
        } else {
            newDrake.tame(player);
        }

        data.customName().ifPresent(newDrake::setCustomName);

        serverLevel.addFreshEntity(newDrake);

        BinderComponentUtil.setData(stack, BinderData.EMPTY);

        player.displayClientMessage(Component.translatable("saintsdragons.message.stegonaut_released", drakeName), true);
        return true;
    }

    public static boolean isBound(ItemStack stack) {
        return BinderComponentUtil.isBound(stack);
    }

    @Nullable
    public static UUID getBoundDrakeUUID(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonUUID(stack);
    }

    @Nullable
    public static String getBoundDrakeName(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonName(stack);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("saintsdragons.tooltip.stegonaut_binder.description"));
        if (BinderComponentUtil.isBound(stack)) {
            String drakeName = BinderComponentUtil.getBoundDragonName(stack);
            if (drakeName != null) {
                tooltip.add(Component.translatable("saintsdragons.tooltip.stegonaut_binder.bound", drakeName));
                tooltip.add(Component.translatable("saintsdragons.tooltip.stegonaut_binder.bound_desc"));
            }
        } else {
            tooltip.add(Component.translatable("saintsdragons.tooltip.stegonaut_binder.empty"));
            tooltip.add(Component.translatable("saintsdragons.tooltip.stegonaut_binder.right_click_to_release"));
        }
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return BinderComponentUtil.isBound(stack);
    }
}
