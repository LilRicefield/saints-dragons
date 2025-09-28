package com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Handles all player interactions with Rift Drakes.
 * Extracted from RiftDrakeEntity to improve maintainability and reduce class size.
 */
public record RiftDrakeInteractionHandler(RiftDrakeEntity dragon) {
    
    /**
     * Main interaction entry point.
     * Delegates to specific handlers based on dragon state and interaction type.
     */
    public InteractionResult handleInteraction(Player player, InteractionHand hand) {
        if (dragon.isDying()) {
            return InteractionResult.PASS;
        }
        
        ItemStack itemstack = player.getItemInHand(hand);
        
        if (!dragon.isTame()) {
            return handleUntamedInteraction(player, itemstack);
        } else {
            return handleTamedInteraction(player, itemstack, hand);
        }
    }
    
    /**
     * Handle interactions with untamed Rift Drakes (taming attempts).
     */
    private InteractionResult handleUntamedInteraction(Player player, ItemStack itemstack) {
        if (!dragon.isFood(itemstack)) {
            return InteractionResult.PASS;
        }
        
        // Taming logic must be server-only to avoid client-only visual state changes
        if (!dragon.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            
            // Rift Drakes have a 1 in 8 chance of taming (easier than Lightning Dragons)
            if (dragon.getRandom().nextInt(8) == 0) {
                // Successful taming
                dragon.tame(player);
                dragon.setOrderedToSit(true);
                dragon.setCommand(1); // Set command to Sit (1) to match the sitting state
                dragon.level().broadcastEntityEvent(dragon, (byte) 7); // Success particles
                
                // Trigger advancement for taming Rift Drake
                triggerTamingAdvancement(player);
            } else {
                // Failed taming attempt
                dragon.level().broadcastEntityEvent(dragon, (byte) 6); // Smoke particles
            }
        }
        
        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }
    
    /**
     * Handle interactions with tamed Rift Drakes (feeding, commands, mounting).
     */
    private InteractionResult handleTamedInteraction(Player player, ItemStack itemstack, InteractionHand hand) {
        if (dragon.isFood(itemstack) && dragon.getHealth() < dragon.getMaxHealth()) {
            // Feed the dragon to heal it
            if (!dragon.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                
                float healAmount = 10.0f; // Heal 5 hearts per fish
                float oldHealth = dragon.getHealth();
                float newHealth = Math.min(oldHealth + healAmount, dragon.getMaxHealth());
                dragon.setHealth(newHealth);
                
                // Show healing particles (green hearts)
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
            }
            return InteractionResult.sidedSuccess(dragon.level().isClientSide);
        }
        
        // Handle mounting for tamed dragons
        if (hand == InteractionHand.MAIN_HAND && player.getItemInHand(hand).isEmpty()) {
            if (!player.isShiftKeyDown()) {
                return handleMounting(player);
            }
        }
        
        // Fall back to base implementation for command cycling
        return dragon.superMobInteract(player, hand);
    }
    
    /**
     * Handle mounting the dragon.
     */
    private InteractionResult handleMounting(Player player) {
        // Check if player can mount (owner check) and dragon isn't already being ridden
        if (!canPlayerMount(player) || dragon.isVehicle()) {
            return InteractionResult.PASS;
        }
        
        // Clear any AI states that might interfere with mounting
        if (!dragon.level().isClientSide) {
            if (dragon.isOrderedToSit()) {
                dragon.setOrderedToSit(false);
            }
            dragon.setTarget(null);
            dragon.getNavigation().stop();
            
            // Start riding
            player.startRiding(dragon);
        }
        
        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }
    
    /**
     * Check if the player can mount the dragon (owner check).
     */
    private boolean canPlayerMount(Player player) {
        return dragon.isTame() && dragon.isOwnedBy(player);
    }
    
    /**
     * Trigger taming advancement for the player
     */
    private void triggerTamingAdvancement(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var advancement = serverPlayer.server.getAdvancements()
                .getAdvancement(SaintsDragons.rl("tame_rift_drake"));
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "tame_rift_drake");
            }
        }
    }
}
