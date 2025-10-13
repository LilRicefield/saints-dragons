package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Item used to bind a Rift Drake for portable convenience.
 * Right-click on a tamed Rift Drake to bind it to this item.
 * While carrying a bound Rift Drake binder, the player can release the drake.
 */
public class NulljawBinderItem extends Item {

    private static final String BOUND_DRAGON_UUID = "BoundDragonUUID";
    private static final String BOUND_DRAGON_NAME = "BoundDragonName";
    private static final String BOUND_OWNER_UUID = "BoundOwnerUUID";
    private static final String BOUND_OWNER_NAME = "BoundOwnerName";
    private static final String BOUND_CUSTOM_NAME = "BoundCustomName";
    private static final String DRAGON_DATA_KEY = "RiftDrakeData";
    private static final String IS_BOUND = "IsBound";

    public NulljawBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack,
                                                           @NotNull Player player,
                                                           @NotNull LivingEntity target,
                                                           @NotNull InteractionHand hand) {
        if (!(target instanceof Nulljaw drake)) {
            return InteractionResult.PASS;
        }

        if (!drake.isTame() || !drake.isOwnedBy(player)) {
            player.displayClientMessage(
                    Component.translatable("saintsdragons.message.not_dragon_owner"),
                    true);
            return InteractionResult.FAIL;
        }

        if (!drake.canBeBound()) {
            player.displayClientMessage(
                    Component.translatable("saintsdragons.message.nulljaw_cannot_be_captured"),
                    true);
            return InteractionResult.FAIL;
        }

        if (isBound(stack)) {
            player.displayClientMessage(
                    Component.translatable("saintsdragons.message.binder_already_occupied"),
                    true);
            return InteractionResult.FAIL;
        }

        ItemStack newStack = captureDrake(stack, drake, player);

        if (hand == InteractionHand.MAIN_HAND) {
            player.getInventory().setItem(player.getInventory().selected, newStack);
        } else {
            player.getInventory().setItem(40, newStack);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level,
                                                           @NotNull Player player,
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

        return releaseDrake(stack, player, context.getClickedPos())
                ? InteractionResult.SUCCESS
                : InteractionResult.FAIL;
    }

    private ItemStack captureDrake(ItemStack stack, Nulljaw drake, Player player) {
        ItemStack copied = stack.copy();
        CompoundTag tag = copied.getOrCreateTag();

        tag.putUUID(BOUND_DRAGON_UUID, drake.getUUID());
        tag.putString(BOUND_DRAGON_NAME, drake.getName().getString());
        if (drake.hasCustomName()) {
            tag.putString(BOUND_CUSTOM_NAME, Component.Serializer.toJson(drake.getCustomName()));
        } else {
            tag.remove(BOUND_CUSTOM_NAME);
        }
        tag.putBoolean(IS_BOUND, true);

        LivingEntity owner = drake.getOwner();
        if (owner instanceof Player ownerPlayer) {
            tag.putUUID(BOUND_OWNER_UUID, ownerPlayer.getUUID());
            tag.putString(BOUND_OWNER_NAME, ownerPlayer.getName().getString());
        } else {
            tag.remove(BOUND_OWNER_UUID);
            tag.remove(BOUND_OWNER_NAME);
        }

        CompoundTag drakeData = new CompoundTag();
        drake.addAdditionalSaveData(drakeData);
        tag.put(DRAGON_DATA_KEY, drakeData);

        copied.setTag(tag);

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
                if (serverPlayer.getInventory().getItem(i) == stack) {
                    serverPlayer.getInventory().setItem(i, copied);
                    break;
                }
            }
        }

        drake.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);

        player.displayClientMessage(
                Component.translatable("saintsdragons.message.nulljaw_captured", drake.getName().getString()),
                true);

        return copied;
    }

    private boolean releaseDrake(ItemStack stack, Player player, BlockPos pos) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(BOUND_DRAGON_UUID)) {
            return false;
        }

        UUID ownerUUID = tag.contains(BOUND_OWNER_UUID) ? tag.getUUID(BOUND_OWNER_UUID) : null;
        if (ownerUUID != null && !player.getUUID().equals(ownerUUID)) {
            player.displayClientMessage(
                    Component.translatable("saintsdragons.message.cannot_release_others_dragon"),
                    true);
            return false;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return true;
        }

        String drakeName = tag.getString(BOUND_DRAGON_NAME);

        Nulljaw newDrake = new Nulljaw(ModEntities.NULLJAW.get(), serverLevel);

        if (tag.contains(DRAGON_DATA_KEY)) {
            CompoundTag drakeData = tag.getCompound(DRAGON_DATA_KEY);
            newDrake.readAdditionalSaveData(drakeData);
        }

        newDrake.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);

        if (ownerUUID != null) {
            Player owner = serverLevel.getPlayerByUUID(ownerUUID);
            if (owner != null) {
                newDrake.tame(owner);
            } else {
                newDrake.setTame(true);
                newDrake.setOwnerUUID(ownerUUID);
            }
        } else {
            newDrake.tame(player);
        }

        if (tag.contains(BOUND_CUSTOM_NAME)) {
            Component customName = Component.Serializer.fromJson(tag.getString(BOUND_CUSTOM_NAME));
            if (customName != null) {
                newDrake.setCustomName(customName);
            }
        }

        serverLevel.addFreshEntity(newDrake);

        tag.remove(BOUND_DRAGON_UUID);
        tag.remove(BOUND_DRAGON_NAME);
        tag.remove(BOUND_OWNER_UUID);
        tag.remove(BOUND_OWNER_NAME);
        tag.remove(BOUND_CUSTOM_NAME);
        tag.remove(DRAGON_DATA_KEY);
        tag.putBoolean(IS_BOUND, false);

        player.displayClientMessage(
                Component.translatable("saintsdragons.message.nulljaw_released", drakeName),
                true);

        return true;
    }

    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(IS_BOUND);
    }

    @Nullable
    public static UUID getBoundRiftDrakeUUID(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID(BOUND_DRAGON_UUID)) {
            return tag.getUUID(BOUND_DRAGON_UUID);
        }
        return null;
    }

    @Nullable
    public static String getBoundRiftDrakeName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(BOUND_DRAGON_NAME)) {
            return tag.getString(BOUND_DRAGON_NAME);
        }
        return null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack,
                                @Nullable Level level,
                                @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("saintsdragons.tooltip.nulljaw.description"));
        if (isBound(stack)) {
            String name = getBoundRiftDrakeName(stack);
            if (name != null && !name.isEmpty()) {
                tooltip.add(Component.translatable("saintsdragons.tooltip.nulljaw.bound", name));
            }
            tooltip.add(Component.translatable("saintsdragons.tooltip.nulljaw.right_click_to_release"));
        } else {
            tooltip.add(Component.translatable("saintsdragons.tooltip.nulljaw.empty"));
            tooltip.add(Component.translatable("saintsdragons.tooltip.nulljaw.right_click_nulljaw_to_bind"));
        }
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return isBound(stack);
    }
}
