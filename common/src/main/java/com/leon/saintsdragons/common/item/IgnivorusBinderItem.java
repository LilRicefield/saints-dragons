package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
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
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Item used to bind an ignivorus for portable convenience.
 * Right-click on a tamed ignivorus to bind it to this item.
 * While carrying a bound ignivorus binder, the player can release the ignivorus.
 */
public class IgnivorusBinderItem extends Item {

    // NBT keys for storing bound dragon data
    private static final String BOUND_DRAGON_UUID = "BoundDragonUUID";
    private static final String BOUND_DRAGON_NAME = "BoundDragonName";
    private static final String BOUND_OWNER_UUID = "BoundOwnerUUID";
    private static final String BOUND_OWNER_NAME = "BoundOwnerName";
    private static final String BOUND_CUSTOM_NAME = "BoundCustomName";
    private static final String DRAGON_DATA_KEY = "IgnivorusData";
    private static final String IS_BOUND = "IsBound";

    public IgnivorusBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (target instanceof Ignivorus ignivorus) {
            // Check if player owns the ignivorus
            if (!ignivorus.isTame() || !ignivorus.isOwnedBy(player)) {
                player.displayClientMessage(
                    Component.translatable("saintsdragons.message.not_dragon_owner"),
                    true);
                return InteractionResult.FAIL;
            }

            // Check if ignivorus can be captured (not flying, not dying, etc.)
            if (!ignivorus.canBeBound()) {
                player.displayClientMessage(
                    Component.translatable("saintsdragons.message.dragon_cannot_be_captured"),
                    true);
                return InteractionResult.FAIL;
            }

            // Check if binder is already occupied
            if (isBound(stack)) {
                player.displayClientMessage(
                    Component.translatable("saintsdragons.message.binder_already_occupied"),
                    true);
                return InteractionResult.FAIL;
            }

            // Capture the ignivorus into the binder
            ItemStack newStack = captureIgnivorus(stack, ignivorus, player);

            // Replace the item in the player's hand
            if (hand == InteractionHand.MAIN_HAND) {
                player.getInventory().setItem(player.getInventory().selected, newStack);
            } else {
                player.getInventory().setItem(40, newStack); // Off-hand slot
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (isBound(stack)) {
            return InteractionResultHolder.pass(stack);
        }

        return super.use(level, player, hand);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player != null && isBound(stack)) {
            return releaseIgnivorus(stack, player, context.getClickedPos())
                ? InteractionResult.SUCCESS
                : InteractionResult.FAIL;
        }

        return super.useOn(context);
    }

    /**
     * Capture an ignivorus into this binder (Pokeball style)
     */
    private ItemStack captureIgnivorus(ItemStack stack, Ignivorus ignivorus, Player player) {
        // Create a new item stack with the modified data
        ItemStack newStack = stack.copy();
        CompoundTag tag = newStack.getOrCreateTag();

        // Store ignivorus UUID (preserved on release)
        tag.putUUID(BOUND_DRAGON_UUID, ignivorus.getUUID());
        tag.putString(BOUND_DRAGON_NAME, ignivorus.getName().getString());

        // Store custom name if present
        if (ignivorus.hasCustomName()) {
            Component customName = ignivorus.getCustomName();
            if (customName != null) {
                tag.putString(BOUND_CUSTOM_NAME, Component.Serializer.toJson(customName));
            } else {
                tag.remove(BOUND_CUSTOM_NAME);
            }
        } else {
            tag.remove(BOUND_CUSTOM_NAME);
        }
        tag.putBoolean(IS_BOUND, true);

        // Store owner data
        LivingEntity owner = ignivorus.getOwner();
        if (owner instanceof Player ownerPlayer) {
            tag.putUUID(BOUND_OWNER_UUID, ownerPlayer.getUUID());
            tag.putString(BOUND_OWNER_NAME, ownerPlayer.getName().getString());
        } else {
            tag.remove(BOUND_OWNER_UUID);
            tag.remove(BOUND_OWNER_NAME);
        }

        // Store ignivorus's current state
        CompoundTag ignivorusData = new CompoundTag();
        ignivorus.setBoundInBinder(true);
        ignivorus.addAdditionalSaveData(ignivorusData);
        tag.put(DRAGON_DATA_KEY, ignivorusData);

        // Set the tag on the new stack
        newStack.setTag(tag);

        // Remove the ignivorus from the world
        ignivorus.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);

        // Send success message
        player.displayClientMessage(
            Component.translatable("saintsdragons.message.ignivorus_captured", ignivorus.getName().getString()),
            true
        );

        return newStack;
    }

    /**
     * Release the bound ignivorus from this binder
     */
    private boolean releaseIgnivorus(ItemStack stack, Player player, BlockPos pos) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(BOUND_DRAGON_UUID)) {
            return false;
        }

        UUID ownerUUID = tag.contains(BOUND_OWNER_UUID) ? tag.getUUID(BOUND_OWNER_UUID) : null;
        if (ownerUUID != null && !player.getUUID().equals(ownerUUID)) {
            player.displayClientMessage(
                Component.translatable("saintsdragons.message.cannot_release_others_dragon"),
                true
            );
            return false;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        String ignivorusName = tag.getString(BOUND_DRAGON_NAME);
        UUID originalUUID = tag.getUUID(BOUND_DRAGON_UUID);

        Ignivorus newIgnivorus = new Ignivorus(
            ModEntities.IGNIVORUS.get(),
            serverLevel
        );

        // Restore dragon data with error handling
        if (tag.contains(DRAGON_DATA_KEY)) {
            try {
                CompoundTag ignivorusData = tag.getCompound(DRAGON_DATA_KEY);
                newIgnivorus.readAdditionalSaveData(ignivorusData);
                newIgnivorus.setBoundInBinder(false);
            } catch (Exception e) {
                player.displayClientMessage(
                    Component.translatable("saintsdragons.message.binder_data_corrupted"),
                    true
                );
                return false;
            }
        }

        // Preserve original UUID to maintain references
        newIgnivorus.setUUID(originalUUID);

        newIgnivorus.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);

        // Restore owner even if they're offline
        if (ownerUUID != null) {
            newIgnivorus.setTame(true);
            newIgnivorus.setOwnerUUID(ownerUUID);
        } else {
            newIgnivorus.tame(player);
        }

        // Restore custom name with error handling
        if (tag.contains(BOUND_CUSTOM_NAME)) {
            try {
                Component customName = Component.Serializer.fromJson(tag.getString(BOUND_CUSTOM_NAME));
                if (customName != null) {
                    newIgnivorus.setCustomName(customName);
                }
            } catch (Exception e) {
                // If custom name is corrupted, just skip it
            }
        }

        serverLevel.addFreshEntity(newIgnivorus);

        tag.remove(BOUND_DRAGON_UUID);
        tag.remove(BOUND_DRAGON_NAME);
        tag.remove(BOUND_OWNER_UUID);
        tag.remove(BOUND_OWNER_NAME);
        tag.remove(BOUND_CUSTOM_NAME);
        tag.remove(DRAGON_DATA_KEY);
        tag.putBoolean(IS_BOUND, false);

        player.displayClientMessage(
            Component.translatable("saintsdragons.message.ignivorus_released", ignivorusName),
            true
        );
        return true;
    }

    /**
     * Check if this binder has an ignivorus bound to it
     */
    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(IS_BOUND);
    }

    /**
     * Get the UUID of the bound ignivorus
     */
    @Nullable
    public static UUID getBoundIgnivorusUUID(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(BOUND_DRAGON_UUID)) {
            return tag.getUUID(BOUND_DRAGON_UUID);
        }
        return null;
    }

    /**
     * Get the name of the bound ignivorus
     */
    @Nullable
    public static String getBoundIgnivorusName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(BOUND_DRAGON_NAME)) {
            return tag.getString(BOUND_DRAGON_NAME);
        }
        return null;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("saintsdragons.tooltip.ignivorus_binder.description"));
        if (isBound(stack)) {
            String ignivorusName = getBoundIgnivorusName(stack);
            if (ignivorusName != null) {
                tooltip.add(Component.translatable("saintsdragons.tooltip.ignivorus_binder.bound", ignivorusName));
            }
            tooltip.add(Component.translatable("saintsdragons.tooltip.ignivorus_binder.right_click_to_release"));
        } else {
            tooltip.add(Component.translatable("saintsdragons.tooltip.ignivorus_binder.empty"));
            tooltip.add(Component.translatable("saintsdragons.tooltip.ignivorus_binder.right_click_ignivorus_to_bind"));
        }
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return isBound(stack);
    }
}
