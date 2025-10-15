package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Custom goal for Lightning Dragons that handles both eating fish AND taming.
 * When an untamed wyvern eats fish from the ground, it has a chance to become tamed.
 */
public class RaevyxTemptGoal extends Goal {
    private final Raevyx dragon;
    private final double speedModifier;
    private final Ingredient items;
    private final boolean canScare;
    private int consumptionCooldown = 0;
    private ItemEntity targetFish = null;

    public RaevyxTemptGoal(Raevyx dragon, double speedModifier, Ingredient items, boolean canScare) {
        this.dragon = dragon;
        this.speedModifier = speedModifier;
        this.items = items;
        this.canScare = canScare;
    }

    @Override
    public boolean canUse() {
        // Don't use this goal if wyvern is sleeping, dying, or being ridden
        if (dragon.isSleeping() || dragon.isDying() || dragon.isVehicle()) {
            return false;
        }
        
        // Check for nearby fish items on the ground
        return findNearestFish() != null;
    }
    
    /**
     * Find the nearest fish item that the wyvern can eat
     */
    private ItemEntity findNearestFish() {
        var broadphase = dragon.getBoundingBox().inflate(12.0, 12.0, 12.0); // 12 block radius
        var nearbyItems = dragon.level().getEntitiesOfClass(
            ItemEntity.class,
            broadphase,
            item -> dragon.isFood(item.getItem())
        );
        
        if (nearbyItems.isEmpty()) {
            return null;
        }
        
        return nearbyItems.stream()
            .min((i1, i2) -> Double.compare(
                dragon.distanceToSqr(i1),
                dragon.distanceToSqr(i2)
            ))
            .orElse(null);
    }

    @Override
    public void start() {
        targetFish = findNearestFish();
        if (targetFish != null) {
            dragon.getNavigation().moveTo(targetFish, speedModifier);
        }
    }
    
    @Override
    public void stop() {
        targetFish = null;
        dragon.getNavigation().stop();
    }
    
    @Override
    public boolean canContinueToUse() {
        // Continue if target fish still exists and wyvern hasn't reached it
        if (targetFish == null || !targetFish.isAlive()) {
            return false;
        }
        
        // Stop if wyvern is too far from target
        if (dragon.distanceToSqr(targetFish) > 144.0) { // 12 blocks squared
            return false;
        }
        
        return true;
    }

    @Override
    public void tick() {
        // Decrement cooldown
        if (consumptionCooldown > 0) {
            consumptionCooldown--;
        }
        
        // Check if wyvern is close enough to consume the target fish
        if (!dragon.level().isClientSide && consumptionCooldown <= 0 && targetFish != null) {
            double distance = dragon.distanceToSqr(targetFish);
            
            // If wyvern is very close to target fish, consume it
            if (distance <= 2.25) { // 1.5 blocks squared
                handleFishConsumption(targetFish);
                consumptionCooldown = 20; // 1 second cooldown
                targetFish = null; // Clear target after consumption
            }
        }
    }

    /**
     * Handle fish consumption and potential taming when wyvern reaches fish items
     */
    private void handleFishConsumption(ItemEntity fishItem) {
        if (fishItem == null || !fishItem.isAlive()) {
            return;
        }
        
        // Find the nearest player who might be "feeding" the wyvern
        Player feedingPlayer = findNearestPlayer();
        
        // Consume the fish item
        fishItem.discard();
            
            // Trigger eat animation
            dragon.triggerAnim("action", "eat");
            
            // Play eating sound
            dragon.playSound(
                com.leon.saintsdragons.common.registry.ModSounds.RAEVYX_CHUFF.get(),
                1.0f,
                1.0f + dragon.getRandom().nextFloat() * 0.3f
            );
            
            // Handle different behavior for tamed vs untamed dragons
            if (dragon.isTame()) {
                // Tamed wyvern: heal when eating fish
                float healAmount = 10.0f; // Heal 5 hearts per fish
                float oldHealth = dragon.getHealth();
                float newHealth = Math.min(oldHealth + healAmount, dragon.getMaxHealth());
                dragon.setHealth(newHealth);
                
                // Show healing particles (green hearts)
                dragon.level().broadcastEntityEvent(dragon, (byte) 7);
                
                // Send appropriate feeding message to nearest player
                sendFeedingMessage(feedingPlayer, newHealth);
            } else {
                // Untamed wyvern: taming chance (same 1 in 10 chance as right-click taming)
                if (feedingPlayer != null && dragon.getRandom().nextInt(10) == 0) {
                    // Successful taming!
                    dragon.tame(feedingPlayer);
                    dragon.setOrderedToSit(true);
                    dragon.setCommandManual(1); // Set command to Sit (1)
                    dragon.level().broadcastEntityEvent(dragon, (byte) 7); // Success particles
                    
                    // Trigger taming advancement
                    if (feedingPlayer instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        var advancement = serverPlayer.server.getAdvancements()
                            .getAdvancement(com.leon.saintsdragons.SaintsDragons.rl("tame_lightning_dragon"));
                        if (advancement != null) {
                            serverPlayer.getAdvancements().award(advancement, "tame_lightning_dragon");
                        }
                    }
                } else {
                    // Failed taming - show smoke particles
                    dragon.level().broadcastEntityEvent(dragon, (byte) 6);
                }
            }
        }

    /**
     * Find the nearest player who might be "feeding" the wyvern by dropping fish
     */
    private Player findNearestPlayer() {
        var nearbyPlayers = dragon.level().getEntitiesOfClass(
            Player.class,
            dragon.getBoundingBox().inflate(10.0),
            player -> player.isAlive() && !player.isSpectator()
        );

        if (nearbyPlayers.isEmpty()) {
            return null;
        }

        // Return the closest player
        return nearbyPlayers.stream()
            .min((p1, p2) -> Double.compare(
                dragon.distanceToSqr(p1),
                dragon.distanceToSqr(p2)
            ))
            .orElse(null);
    }
    
    /**
     * Send appropriate feeding message based on healing result.
     * Same logic as LightningDragonInteractionHandler.sendFeedingMessage()
     */
    private void sendFeedingMessage(Player player, float newHealth) {
        if (player instanceof ServerPlayer serverPlayer) {
            String messageKey = (newHealth >= dragon.getMaxHealth()) 
                ? "entity.saintsdragons.raevyx.fed"
                : "entity.saintsdragons.raevyx.fed_partial";
                
            serverPlayer.displayClientMessage(
                Component.translatable(messageKey, dragon.getName()),
                true
            );
        }
    }
}
