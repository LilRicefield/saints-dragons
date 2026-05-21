package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.item.util.BinderComponentUtil;
import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
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

public abstract class AbstractDragonBinderItem<T extends DragonEntity> extends Item {

    protected AbstractDragonBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player,
                                                           @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (!getDragonClass().isInstance(target)) {
            return InteractionResult.PASS;
        }

        T dragon = getDragonClass().cast(target);
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!dragon.isTame() || !dragon.isOwnedBy(player)) {
            player.displayClientMessage(Component.translatable(getNotOwnerMessageKey()), true);
            return InteractionResult.FAIL;
        }

        if (!canBind(dragon)) {
            player.displayClientMessage(Component.translatable(getCannotCaptureMessageKey()), true);
            return InteractionResult.FAIL;
        }

        if (BinderComponentUtil.isBound(stack)) {
            player.displayClientMessage(Component.translatable(getAlreadyOccupiedMessageKey()), true);
            return InteractionResult.FAIL;
        }

        ItemStack reboundStack = captureDragon(stack, dragon, player);
        if (reboundStack.isEmpty()) {
            return InteractionResult.FAIL;
        }

        player.setItemInHand(hand, reboundStack);
        syncPlayerInventory(player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return BinderComponentUtil.isBound(stack) ? InteractionResultHolder.pass(stack) : super.use(level, player, hand);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return super.useOn(context);
        }

        ItemStack stack = context.getItemInHand();
        if (!BinderComponentUtil.isBound(stack)) {
            return super.useOn(context);
        }

        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        boolean released = releaseDragon(stack, player, context.getClickedPos());
        if (released) {
            syncPlayerInventory(player);
        }
        return released ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    private ItemStack captureDragon(ItemStack stack, T dragon, Player player) {
        ItemStack copied = stack.copy();
        CompoundTag tag = copied.getOrCreateTag();

        tag.putUUID(BinderComponentUtil.BOUND_DRAGON_UUID, dragon.getUUID());
        tag.putString(BinderComponentUtil.BOUND_DRAGON_NAME, dragon.getName().getString());

        if (dragon.hasCustomName()) {
            Component customName = dragon.getCustomName();
            if (customName != null) {
                tag.putString(BinderComponentUtil.BOUND_CUSTOM_NAME, Component.Serializer.toJson(customName));
            } else {
                tag.remove(BinderComponentUtil.BOUND_CUSTOM_NAME);
            }
        } else {
            tag.remove(BinderComponentUtil.BOUND_CUSTOM_NAME);
        }
        tag.putBoolean(BinderComponentUtil.IS_BOUND, true);

        LivingEntity owner = dragon.getOwner();
        if (owner instanceof Player ownerPlayer) {
            tag.putUUID(BinderComponentUtil.BOUND_OWNER_UUID, ownerPlayer.getUUID());
            tag.putString(BinderComponentUtil.BOUND_OWNER_NAME, ownerPlayer.getName().getString());
        } else {
            tag.remove(BinderComponentUtil.BOUND_OWNER_UUID);
            tag.remove(BinderComponentUtil.BOUND_OWNER_NAME);
        }

        CompoundTag dragonData = new CompoundTag();
        prepareDragonForCapture(dragon, player);
        dragon.setBoundInBinder(true);
        dragon.saveWithoutId(dragonData);
        tag.put(getDragonDataKey(), dragonData);
        copied.setTag(tag);

        if (player.level() instanceof ServerLevel serverLevel) {
            DragonCodexSavedData.get(serverLevel).updateDragonBoundState(player.getUUID(), dragon.getUUID(), true);
        }

        dragon.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        if (!dragon.isRemoved()) {
            return ItemStack.EMPTY;
        }
        onDragonCaptured(dragon, player, copied);
        player.displayClientMessage(Component.translatable(getCapturedMessageKey(), dragon.getName().getString()), true);
        return copied;
    }

    private boolean releaseDragon(ItemStack stack, Player player, BlockPos pos) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(BinderComponentUtil.BOUND_DRAGON_UUID)) {
            return false;
        }

        UUID ownerUUID = tag.contains(BinderComponentUtil.BOUND_OWNER_UUID) ? tag.getUUID(BinderComponentUtil.BOUND_OWNER_UUID) : null;
        if (ownerUUID != null && !player.getUUID().equals(ownerUUID)) {
            player.displayClientMessage(Component.translatable(getReleaseNotOwnerMessageKey()), true);
            return false;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        String dragonName = tag.getString(BinderComponentUtil.BOUND_DRAGON_NAME);
        UUID originalUUID = tag.getUUID(BinderComponentUtil.BOUND_DRAGON_UUID);
        T newDragon = createDragon(serverLevel);

        if (tag.contains(getDragonDataKey())) {
            try {
                CompoundTag dragonData = tag.getCompound(getDragonDataKey());
                newDragon.load(dragonData);
                newDragon.setBoundInBinder(false);
            } catch (Exception e) {
                player.displayClientMessage(Component.translatable("saintsdragons.message.binder_data_corrupted"), true);
                return false;
            }
        }

        newDragon.setUUID(originalUUID);
        newDragon.setPos(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
        prepareDragonForRelease(newDragon, player);

        if (ownerUUID != null) {
            newDragon.setTame(true);
            newDragon.setOwnerUUID(ownerUUID);
        } else {
            newDragon.tame(player);
        }

        if (tag.contains(BinderComponentUtil.BOUND_CUSTOM_NAME)) {
            try {
                Component customName = Component.Serializer.fromJson(tag.getString(BinderComponentUtil.BOUND_CUSTOM_NAME));
                if (customName != null) {
                    newDragon.setCustomName(customName);
                }
            } catch (Exception ignored) {
            }
        }

        if (!serverLevel.addFreshEntity(newDragon)) {
            player.displayClientMessage(Component.translatable(getReleaseFailedMessageKey(), dragonName), true);
            return false;
        }

        DragonCodexSavedData.get(serverLevel).updateDragonBoundState(ownerUUID != null ? ownerUUID : player.getUUID(), originalUUID, false);
        clearBinderData(tag);
        onDragonReleased(newDragon, player, stack);
        player.displayClientMessage(Component.translatable(getReleasedMessageKey(), dragonName), true);
        return true;
    }

    private void syncPlayerInventory(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.containerMenu.broadcastChanges();
            serverPlayer.inventoryMenu.broadcastChanges();
        }
    }

    protected void clearBinderData(CompoundTag tag) {
        tag.remove(BinderComponentUtil.BOUND_DRAGON_UUID);
        tag.remove(BinderComponentUtil.BOUND_DRAGON_NAME);
        tag.remove(BinderComponentUtil.BOUND_OWNER_UUID);
        tag.remove(BinderComponentUtil.BOUND_OWNER_NAME);
        tag.remove(BinderComponentUtil.BOUND_CUSTOM_NAME);
        tag.remove(getDragonDataKey());
        tag.putBoolean(BinderComponentUtil.IS_BOUND, false);
    }

    protected void onDragonCaptured(T dragon, Player player, ItemStack binderStack) {
    }

    protected void onDragonReleased(T dragon, Player player, ItemStack binderStack) {
    }

    protected void prepareDragonForCapture(T dragon, Player player) {
    }

    protected void prepareDragonForRelease(T dragon, Player player) {
    }

    protected String getNotOwnerMessageKey() {
        return "saintsdragons.message.not_dragon_owner";
    }

    protected String getReleaseNotOwnerMessageKey() {
        return "saintsdragons.message.cannot_release_others_dragon";
    }

    protected String getAlreadyOccupiedMessageKey() {
        return "saintsdragons.message.binder_already_occupied";
    }

    protected String getBoundReleaseTooltipKey() {
        return getTooltipReleaseKey();
    }

    protected boolean shouldShowReleaseTooltipWhenBound() {
        return true;
    }

    protected void appendExtraBoundTooltip(@NotNull List<Component> tooltip, @Nullable String dragonName) {
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(getTooltipDescriptionKey()));
        if (BinderComponentUtil.isBound(stack)) {
            String dragonName = BinderComponentUtil.getBoundDragonName(stack);
            if (dragonName != null && !dragonName.isEmpty()) {
                tooltip.add(Component.translatable(getTooltipBoundKey(), dragonName));
            }
            appendExtraBoundTooltip(tooltip, dragonName);
            if (shouldShowReleaseTooltipWhenBound()) {
                tooltip.add(Component.translatable(getBoundReleaseTooltipKey()));
            }
        } else {
            tooltip.add(Component.translatable(getTooltipEmptyKey()));
            tooltip.add(Component.translatable(getTooltipBindHintKey()));
        }
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return BinderComponentUtil.isBound(stack);
    }

    @Override
    public void onDestroyed(@NotNull ItemEntity itemEntity) {
        BinderComponentUtil.handleDestroyedBoundBinder(itemEntity);
        super.onDestroyed(itemEntity);
    }

    protected abstract Class<T> getDragonClass();

    protected abstract T createDragon(ServerLevel level);

    protected abstract boolean canBind(T dragon);

    protected abstract String getDragonDataKey();

    protected String getCannotCaptureMessageKey() {
        return "saintsdragons.message.creature_cannot_be_captured";
    }

    protected String getCapturedMessageKey() {
        return "saintsdragons.message.creature_captured";
    }

    protected String getReleasedMessageKey() {
        return "saintsdragons.message.creature_released";
    }

    protected String getReleaseFailedMessageKey() {
        return "saintsdragons.message.creature_release_failed";
    }

    protected abstract String getTooltipDescriptionKey();

    protected String getTooltipBoundKey() {
        return "saintsdragons.tooltip.binder.bound";
    }

    protected String getTooltipEmptyKey() {
        return "saintsdragons.tooltip.binder.empty";
    }

    protected String getTooltipBindHintKey() {
        return "saintsdragons.tooltip.binder.right_click_to_bind";
    }

    protected String getTooltipReleaseKey() {
        return "saintsdragons.tooltip.binder.right_click_to_release";
    }
}
