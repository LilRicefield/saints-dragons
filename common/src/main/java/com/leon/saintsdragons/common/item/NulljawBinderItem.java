package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.component.BinderData;
import com.leon.saintsdragons.common.item.util.BinderComponentUtil;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Item used to bind a Nulljaw for portable convenience.
 * Right-click on a tamed Nulljaw to bind it to this item.
 */
public class NulljawBinderItem extends Item {

    public NulljawBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (target instanceof Nulljaw dragon) {
            if (!dragon.isTame() || !dragon.isOwnedBy(player)) {
                player.displayClientMessage(Component.translatable("saintsdragons.message.not_dragon_owner"), true);
                return InteractionResult.FAIL;
            }

            if (!dragon.canBeBound()) {
                player.displayClientMessage(Component.translatable("saintsdragons.message.dragon_cannot_be_captured"), true);
                return InteractionResult.FAIL;
            }

            if (BinderComponentUtil.isBound(stack)) {
                player.displayClientMessage(Component.translatable("saintsdragons.message.nulljaw_already_occupied"), true);
                return InteractionResult.FAIL;
            }

            ItemStack newStack = captureDragon(stack, dragon, player);

            if (hand == InteractionHand.MAIN_HAND) {
                player.getInventory().setItem(player.getInventory().selected, newStack);
            } else {
                player.getInventory().setItem(40, newStack);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (BinderComponentUtil.isBound(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player != null && BinderComponentUtil.isBound(stack)) {
            return releaseDragon(stack, player, context.getClickedPos())
                ? InteractionResult.SUCCESS
                : InteractionResult.FAIL;
        }
        return super.useOn(context);
    }

    private ItemStack captureDragon(ItemStack stack, Nulljaw dragon, Player player) {
        ItemStack newStack = stack.copy();

        net.minecraft.nbt.CompoundTag dragonData = new net.minecraft.nbt.CompoundTag();
        dragon.addAdditionalSaveData(dragonData);

        LivingEntity owner = dragon.getOwner();
        UUID ownerId = owner instanceof Player ownerPlayer ? ownerPlayer.getUUID() : null;
        String ownerName = owner instanceof Player ownerPlayer ? ownerPlayer.getName().getString() : null;
        BinderData data = BinderData.bound(
                dragon.getUUID(),
                dragon.getName().getString(),
                ownerId,
                ownerName,
                dragon.getCustomName(),
                dragonData
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

        dragon.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);

        player.displayClientMessage(Component.translatable("saintsdragons.message.nulljaw_captured", dragon.getName().getString()), true);

        return newStack;
    }

    private boolean releaseDragon(ItemStack stack, Player player, BlockPos pos) {
        BinderData data = BinderComponentUtil.getData(stack);
        if (!data.isBound() || data.dragonUuid().isEmpty()) {
            return false;
        }

        UUID ownerUUID = data.ownerUuid().orElse(null);
        if (ownerUUID != null && !player.getUUID().equals(ownerUUID)) {
            player.displayClientMessage(Component.translatable("saintsdragons.message.cannot_release_others_dragon"), true);
            return false;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return true;
        }

        String dragonName = data.dragonName().orElse("");

        Nulljaw newDragon = new Nulljaw(ModEntities.NULLJAW.get(), serverLevel);

        data.dragonData().ifPresent(newDragon::readAdditionalSaveData);

        newDragon.setUUID(java.util.UUID.randomUUID());
        newDragon.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);

        if (ownerUUID != null) {
            Player owner = serverLevel.getPlayerByUUID(ownerUUID);
            if (owner != null) {
                newDragon.tame(owner);
            }
        } else {
            newDragon.tame(player);
        }

        data.customName().ifPresent(newDragon::setCustomName);

        serverLevel.addFreshEntity(newDragon);

        BinderComponentUtil.setData(stack, BinderData.EMPTY);

        player.displayClientMessage(Component.translatable("saintsdragons.message.nulljaw_released", dragonName), true);
        return true;
    }

    public static boolean isBound(ItemStack stack) {
        return BinderComponentUtil.isBound(stack);
    }

    @Nullable
    public static UUID getBoundDragonUUID(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonUUID(stack);
    }

    @Nullable
    public static String getBoundNulljawName(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonName(stack);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("saintsdragons.tooltip.nulljaw_binder.description"));
        if (BinderComponentUtil.isBound(stack)) {
            String dragonName = BinderComponentUtil.getBoundDragonName(stack);
            if (dragonName != null) {
                tooltip.add(Component.translatable("saintsdragons.tooltip.nulljaw_binder.bound", dragonName));
            }
            tooltip.add(Component.translatable("saintsdragons.tooltip.nulljaw_binder.right_click_to_release"));
        } else {
            tooltip.add(Component.translatable("saintsdragons.tooltip.nulljaw_binder.empty"));
            tooltip.add(Component.translatable("saintsdragons.tooltip.nulljaw_binder.right_click_dragon_to_bind"));
        }
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return BinderComponentUtil.isBound(stack);
    }
}
