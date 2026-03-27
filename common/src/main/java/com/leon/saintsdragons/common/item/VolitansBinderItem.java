package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.item.util.BinderComponentUtil;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

public class VolitansBinderItem extends Item {
    private static final String BOUND_DRAGON_UUID = "BoundDragonUUID";
    private static final String BOUND_DRAGON_NAME = "BoundDragonName";
    private static final String BOUND_OWNER_UUID = "BoundOwnerUUID";
    private static final String BOUND_OWNER_NAME = "BoundOwnerName";
    private static final String BOUND_CUSTOM_NAME = "BoundCustomName";
    private static final String DRAGON_DATA_KEY = "VolitansData";
    private static final String IS_BOUND = "IsBound";

    public VolitansBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player,
                                                           @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (!(target instanceof Volitans dragon)) {
            return InteractionResult.PASS;
        }

        if (!dragon.isTame() || !dragon.isOwnedBy(player)) {
            player.displayClientMessage(Component.translatable("saintsdragons.message.not_dragon_owner"), true);
            return InteractionResult.FAIL;
        }

        if (!dragon.canBeBound()) {
            player.displayClientMessage(Component.translatable("saintsdragons.message.volitans_cannot_be_captured"), true);
            return InteractionResult.FAIL;
        }

        if (isBound(stack)) {
            player.displayClientMessage(Component.translatable("saintsdragons.message.binder_already_occupied"), true);
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

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return isBound(stack) ? InteractionResultHolder.pass(stack) : super.use(level, player, hand);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return super.useOn(context);
        }

        ItemStack stack = context.getItemInHand();
        if (!isBound(stack)) {
            return super.useOn(context);
        }

        return releaseDragon(stack, player, context.getClickedPos()) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    private ItemStack captureDragon(ItemStack stack, Volitans dragon, Player player) {
        ItemStack copied = stack.copy();
        CompoundTag tag = copied.getOrCreateTag();

        tag.putUUID(BOUND_DRAGON_UUID, dragon.getUUID());
        tag.putString(BOUND_DRAGON_NAME, dragon.getName().getString());
        if (dragon.hasCustomName()) {
            tag.putString(BOUND_CUSTOM_NAME, Component.Serializer.toJson(dragon.getCustomName()));
        } else {
            tag.remove(BOUND_CUSTOM_NAME);
        }
        tag.putBoolean(IS_BOUND, true);

        LivingEntity owner = dragon.getOwner();
        if (owner instanceof Player ownerPlayer) {
            tag.putUUID(BOUND_OWNER_UUID, ownerPlayer.getUUID());
            tag.putString(BOUND_OWNER_NAME, ownerPlayer.getName().getString());
        } else {
            tag.remove(BOUND_OWNER_UUID);
            tag.remove(BOUND_OWNER_NAME);
        }

        CompoundTag dragonData = new CompoundTag();
        dragon.setBoundInBinder(true);
        dragon.addAdditionalSaveData(dragonData);
        tag.put(DRAGON_DATA_KEY, dragonData);
        copied.setTag(tag);

        if (player.level() instanceof ServerLevel serverLevel) {
            DragonCodexSavedData.get(serverLevel).updateDragonBoundState(player.getUUID(), dragon.getUUID(), true);
        }

        dragon.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        player.displayClientMessage(Component.translatable("saintsdragons.message.volitans_captured", dragon.getName().getString()), true);
        return copied;
    }

    private boolean releaseDragon(ItemStack stack, Player player, BlockPos pos) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(BOUND_DRAGON_UUID)) {
            return false;
        }

        UUID ownerUUID = tag.contains(BOUND_OWNER_UUID) ? tag.getUUID(BOUND_OWNER_UUID) : null;
        if (ownerUUID != null && !player.getUUID().equals(ownerUUID)) {
            player.displayClientMessage(Component.translatable("saintsdragons.message.cannot_release_others_dragon"), true);
            return false;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        String dragonName = tag.getString(BOUND_DRAGON_NAME);
        UUID originalUUID = tag.getUUID(BOUND_DRAGON_UUID);

        Volitans newDragon = new Volitans(ModEntities.VOLITANS.get(), serverLevel);
        if (tag.contains(DRAGON_DATA_KEY)) {
            try {
                CompoundTag dragonData = tag.getCompound(DRAGON_DATA_KEY);
                newDragon.readAdditionalSaveData(dragonData);
                newDragon.setBoundInBinder(false);
            } catch (Exception e) {
                player.displayClientMessage(Component.translatable("saintsdragons.message.binder_data_corrupted"), true);
                return false;
            }
        }

        newDragon.setUUID(originalUUID);
        newDragon.setPos(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);

        if (ownerUUID != null) {
            newDragon.setTame(true);
            newDragon.setOwnerUUID(ownerUUID);
        } else {
            newDragon.tame(player);
        }

        if (tag.contains(BOUND_CUSTOM_NAME)) {
            try {
                Component customName = Component.Serializer.fromJson(tag.getString(BOUND_CUSTOM_NAME));
                if (customName != null) {
                    newDragon.setCustomName(customName);
                }
            } catch (Exception ignored) {
            }
        }

        serverLevel.addFreshEntity(newDragon);
        DragonCodexSavedData.get(serverLevel).updateDragonBoundState(ownerUUID != null ? ownerUUID : player.getUUID(), originalUUID, false);

        tag.remove(BOUND_DRAGON_UUID);
        tag.remove(BOUND_DRAGON_NAME);
        tag.remove(BOUND_OWNER_UUID);
        tag.remove(BOUND_OWNER_NAME);
        tag.remove(BOUND_CUSTOM_NAME);
        tag.remove(DRAGON_DATA_KEY);
        tag.putBoolean(IS_BOUND, false);

        player.displayClientMessage(Component.translatable("saintsdragons.message.volitans_released", dragonName), true);
        return true;
    }

    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(IS_BOUND);
    }

    @Nullable
    public static UUID getBoundVolitansUUID(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID(BOUND_DRAGON_UUID)) {
            return tag.getUUID(BOUND_DRAGON_UUID);
        }
        return null;
    }

    @Nullable
    public static String getBoundVolitansName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(BOUND_DRAGON_NAME)) {
            return tag.getString(BOUND_DRAGON_NAME);
        }
        return null;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("saintsdragons.tooltip.volitans.description"));
        if (isBound(stack)) {
            String name = getBoundVolitansName(stack);
            if (name != null && !name.isEmpty()) {
                tooltip.add(Component.translatable("saintsdragons.tooltip.volitans.bound", name));
            }
            tooltip.add(Component.translatable("saintsdragons.tooltip.volitans.right_click_to_release"));
        } else {
            tooltip.add(Component.translatable("saintsdragons.tooltip.volitans.empty"));
            tooltip.add(Component.translatable("saintsdragons.tooltip.volitans.right_click_volitans_to_bind"));
        }
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return isBound(stack);
    }

    @Override
    public void onDestroyed(@NotNull ItemEntity itemEntity) {
        BinderComponentUtil.handleDestroyedBoundBinder(itemEntity);
        super.onDestroyed(itemEntity);
    }
}
