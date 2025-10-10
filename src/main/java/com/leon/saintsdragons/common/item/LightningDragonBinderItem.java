package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Item used to bind a Lightning Dragon for portable convenience.
 * Right-click on a tamed lightning dragon to bind it to this item.
 * While carrying a bound lightning dragon binder, the player can release the dragon.
 */
public class LightningDragonBinderItem extends Item {
    
    // NBT keys for storing bound dragon data
    private static final String BOUND_DRAGON_UUID = "BoundDragonUUID";
    private static final String BOUND_DRAGON_NAME = "BoundDragonName";
    private static final String BOUND_OWNER_UUID = "BoundOwnerUUID";
    private static final String BOUND_OWNER_NAME = "BoundOwnerName";
    private static final String IS_BOUND = "IsBound";
    
    public LightningDragonBinderItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (target instanceof LightningDragonEntity dragon) {
            // Check if player owns the dragon
            if (!dragon.isTame() || !dragon.isOwnedBy(player)) {
                player.displayClientMessage(
                    Component.translatable("saintsdragons.message.not_dragon_owner"), 
                    true);
                return InteractionResult.FAIL;
            }
            
            // Check if dragon can be captured (not playing dead, not sleeping, etc.)
            if (!dragon.canBeBound()) {
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
            
            // Capture the dragon into the binder
            ItemStack newStack = captureDragon(stack, dragon, player);
            
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
            return releaseDragon(stack, player, context.getClickedPos())
                ? InteractionResult.SUCCESS
                : InteractionResult.FAIL;
        }
        return super.useOn(context);
    }
    
    /**
     * Capture a dragon into this binder (Pokeball style)
     */
    private ItemStack captureDragon(ItemStack stack, LightningDragonEntity dragon, Player player) {
        // Create a new item stack with the modified data
        ItemStack newStack = stack.copy();
        CompoundTag tag = newStack.getOrCreateTag();
        
        // Store dragon data
        tag.putUUID(BOUND_DRAGON_UUID, dragon.getUUID());
        tag.putString(BOUND_DRAGON_NAME, dragon.getName().getString());
        if (dragon.hasCustomName()) {
            Component customName = dragon.getCustomName();
            if (customName != null) {
                tag.putString("BoundCustomName", net.minecraft.network.chat.Component.Serializer.toJson(customName));
            } else {
                tag.remove("BoundCustomName");
            }
        } else {
            tag.remove("BoundCustomName");
        }
        tag.putBoolean(IS_BOUND, true);

        // Store owner data
        LivingEntity owner = dragon.getOwner();
        if (owner instanceof Player ownerPlayer) {
            tag.putUUID(BOUND_OWNER_UUID, ownerPlayer.getUUID());
            tag.putString(BOUND_OWNER_NAME, ownerPlayer.getName().getString());
        } else {
            tag.remove(BOUND_OWNER_UUID);
            tag.remove(BOUND_OWNER_NAME);
        }

        // Store dragon's current state
        CompoundTag dragonData = new CompoundTag();
        dragon.addAdditionalSaveData(dragonData);
        tag.put("DragonData", dragonData);
        
        // Set the tag on the new stack
        newStack.setTag(tag);
        
        // Replace the old stack with the new one in the player's inventory
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // Find the slot containing the old stack and replace it
            for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
                if (serverPlayer.getInventory().getItem(i) == stack) {
                    serverPlayer.getInventory().setItem(i, newStack);
                    break;
                }
            }
        }
        
        // Remove the dragon from the world
        dragon.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        
        // Send success message
        player.displayClientMessage(
            Component.translatable("saintsdragons.message.lightning_dragon_captured", dragon.getName().getString()),
            true
        );
        
        return newStack;
    }
    
    /**
     * Release the bound dragon from this binder
     */
    private boolean releaseDragon(ItemStack stack, Player player, BlockPos pos) {
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
            return true;
        }

        String dragonName = tag.getString(BOUND_DRAGON_NAME);

        LightningDragonEntity newDragon = new LightningDragonEntity(
            com.leon.saintsdragons.common.registry.ModEntities.LIGHTNING_DRAGON.get(),
            serverLevel
        );

        if (tag.contains("DragonData")) {
            CompoundTag dragonData = tag.getCompound("DragonData");
            newDragon.readAdditionalSaveData(dragonData);
        }

        // Ensure newly released dragons default to follow command so they don't stay in previous wander state
        newDragon.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);

        if (ownerUUID != null) {
            Player owner = serverLevel.getPlayerByUUID(ownerUUID);
            if (owner != null) {
                newDragon.tame(owner);
            }
        } else {
            newDragon.tame(player);
        }

        if (tag.contains("BoundCustomName")) {
            net.minecraft.network.chat.Component customName = net.minecraft.network.chat.Component.Serializer.fromJson(tag.getString("BoundCustomName"));
            if (customName != null) {
                newDragon.setCustomName(customName);
            }
        }

        serverLevel.addFreshEntity(newDragon);

        tag.remove(BOUND_DRAGON_UUID);
        tag.remove(BOUND_DRAGON_NAME);
        tag.remove(BOUND_OWNER_UUID);
        tag.remove(BOUND_OWNER_NAME);
        tag.remove("BoundCustomName");
        tag.remove("DragonData");
        tag.putBoolean(IS_BOUND, false);

        player.displayClientMessage(
            Component.translatable("saintsdragons.message.lightning_dragon_released", dragonName),
            true
        );
        return true;
    }
    
    /**
     * Check if this binder has a dragon bound to it
     */
    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(IS_BOUND);
    }
    
    /**
     * Get the UUID of the bound dragon
     */
    @Nullable
    public static UUID getBoundDragonUUID(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(BOUND_DRAGON_UUID)) {
            return tag.getUUID(BOUND_DRAGON_UUID);
        }
        return null;
    }
    
    /**
     * Get the name of the bound dragon
     */
    @Nullable
    public static String getBoundDragonName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(BOUND_DRAGON_NAME)) {
            return tag.getString(BOUND_DRAGON_NAME);
        }
        return null;
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("saintsdragons.tooltip.lightning_dragon_binder.description"));
        if (isBound(stack)) {
            String dragonName = getBoundDragonName(stack);
            if (dragonName != null) {
                tooltip.add(Component.translatable("saintsdragons.tooltip.lightning_dragon_binder.bound", dragonName));
            }
            tooltip.add(Component.translatable("saintsdragons.tooltip.lightning_dragon_binder.right_click_to_release"));
        } else {
            tooltip.add(Component.translatable("saintsdragons.tooltip.lightning_dragon_binder.empty"));
            tooltip.add(Component.translatable("saintsdragons.tooltip.lightning_dragon_binder.right_click_dragon_to_bind"));
        }
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return isBound(stack);
    }

}
