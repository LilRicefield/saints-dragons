package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Item used to bind a Primitive Drake for portable resistance buff.
 * Right-click on a tamed primitive drake to bind it to this item.
 * While carrying a bound drake binder, the player gets resistance buff.
 */
public class DrakeBinderItem extends Item {
    
    // NBT keys for storing bound drake data
    private static final String BOUND_DRAGON_UUID = "BoundDragonUUID";
    private static final String BOUND_DRAGON_NAME = "BoundDragonName";
    private static final String IS_BOUND = "IsBound";
    
    public DrakeBinderItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof PrimitiveDrakeEntity drake) {
            // Check if player owns the drake
            if (!drake.isTame() || !drake.isOwnedBy(player)) {
                player.displayClientMessage(
                    Component.translatable("saintsdragons.message.not_dragon_owner"), 
                    true);
                return InteractionResult.FAIL;
            }
            
            // Check if drake can be captured (not playing dead, not sleeping, etc.)
            if (!drake.canBeBound()) {
                player.displayClientMessage(
                    Component.translatable("saintsdragons.message.drake_cannot_be_captured"), 
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
            
            // Capture the drake into the binder
            ItemStack newStack = captureDrake(stack, drake, player);
            
            // Replace the item in the player's hand
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
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        
        if (player != null && isBound(stack)) {
            // Release the drake at the clicked location
            releaseDrake(stack, player, context.getClickedPos());
            return InteractionResult.SUCCESS;
        }
        
        return super.useOn(context);
    }
    
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (isBound(stack)) {
            // Don't allow releasing drakes by right-clicking in air
            // Only allow release via useOn (right-clicking on blocks)
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        }
        
        return super.use(level, player, hand);
    }
    
    /**
     * Capture a drake into this binder (Pokeball style)
     */
    private ItemStack captureDrake(ItemStack stack, PrimitiveDrakeEntity drake, Player player) {
        // Create a new item stack with the modified data
        ItemStack newStack = stack.copy();
        CompoundTag tag = newStack.getOrCreateTag();
        
        // Store drake data
        tag.putUUID(BOUND_DRAGON_UUID, drake.getUUID());
        tag.putString(BOUND_DRAGON_NAME, drake.getName().getString());
        tag.putBoolean(IS_BOUND, true);
        
        // Store drake's current state
        CompoundTag drakeData = new CompoundTag();
        drake.addAdditionalSaveData(drakeData);
        tag.put("DrakeData", drakeData);
        
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
        
        
        // Remove the drake from the world
        drake.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        
        // Send success message
        player.displayClientMessage(
            Component.translatable("saintsdragons.message.drake_captured", drake.getName().getString()),
            true
        );
        
        return newStack;
    }
    
    /**
     * Release the drake from this binder (Pokeball style)
     */
    private void releaseDrake(ItemStack stack, Player player, net.minecraft.core.BlockPos pos) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(BOUND_DRAGON_UUID)) {
            return;
        }
        
        String drakeName = tag.getString(BOUND_DRAGON_NAME);
        CompoundTag drakeData = tag.getCompound("DrakeData");
        
        // Create new drake entity
        if (player.level() instanceof ServerLevel serverLevel) {
            PrimitiveDrakeEntity newDrake = new PrimitiveDrakeEntity(
                com.leon.saintsdragons.common.registry.ModEntities.PRIMITIVE_DRAKE.get(),
                serverLevel
            );
            
            // Set position
            newDrake.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            
            // Restore drake data
            newDrake.readAdditionalSaveData(drakeData);
            
            // Ensure it's still tamed to the player
            newDrake.tame(player);
            
            // Spawn the drake
            serverLevel.addFreshEntity(newDrake);
            
            // Clear binder data
            tag.remove(BOUND_DRAGON_UUID);
            tag.remove(BOUND_DRAGON_NAME);
            tag.remove("DrakeData");
            tag.putBoolean(IS_BOUND, false);
            
            // Send success message
            player.displayClientMessage(
                Component.translatable("saintsdragons.message.drake_released", drakeName),
                true
            );
        }
    }
    
    /**
     * Check if this binder has a drake bound to it
     */
    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(IS_BOUND);
    }
    
    /**
     * Get the UUID of the bound drake
     */
    @Nullable
    public static UUID getBoundDrakeUUID(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(BOUND_DRAGON_UUID)) {
            return tag.getUUID(BOUND_DRAGON_UUID);
        }
        return null;
    }
    
    /**
     * Get the name of the bound drake
     */
    @Nullable
    public static String getBoundDrakeName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(BOUND_DRAGON_NAME)) {
            return tag.getString(BOUND_DRAGON_NAME);
        }
        return null;
    }
    
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isBound(stack)) {
            String drakeName = getBoundDrakeName(stack);
            if (drakeName != null) {
                tooltip.add(Component.translatable("saintsdragons.tooltip.drake_binder.bound", drakeName));
                tooltip.add(Component.translatable("saintsdragons.tooltip.drake_binder.bound_desc"));
            }
        } else {
            tooltip.add(Component.translatable("saintsdragons.tooltip.drake_binder.unbound"));
            tooltip.add(Component.translatable("saintsdragons.tooltip.drake_binder.unbound_desc"));
        }
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        // Make bound binders have enchantment glint
        return isBound(stack);
    }
}
