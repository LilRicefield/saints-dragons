package com.leon.saintsdragons.server.entity.dragons.primitivedrake.abilities;

import com.leon.saintsdragons.common.item.DrakeBinderItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles portable resistance buffs from bound Drake Binders.
 * When a player carries a bound Drake Binder, they and nearby allies get resistance buffs.
 */
public class DrakeBinderResistanceAbility {
    
    // Buff parameters
    private static final double BUFF_RANGE = 8.0; // Same range as the drake's original ability
    private static final int BUFF_DURATION = Integer.MAX_VALUE; // Infinite duration
    private static final int BUFF_AMPLIFIER = 0; // Resistance I
    private static final int UPDATE_INTERVAL = 20; // Update every second
    
    // Tracking
    private final Set<Entity> buffedEntities = new HashSet<>();
    private int updateTimer = 0;
    
    /**
     * Update the portable resistance buffs for all players carrying bound Drake Binders
     */
    public static void updateAllPortableBuffs(ServerLevel level) {
        // Find all players with bound Drake Binders
        for (Player player : level.players()) {
            if (player.isAlive()) {
                updatePlayerPortableBuffs(player);
            }
        }
    }
    
    /**
     * Update portable resistance buffs for a specific player
     */
    private static void updatePlayerPortableBuffs(Player player) {
        // Check if player has a bound Drake Binder
        ItemStack boundBinder = findBoundDrakeBinder(player);
        if (boundBinder == null) {
            return;
        }
        
        // Get the bound drake UUID
        UUID boundDrakeUUID = DrakeBinderItem.getBoundDrakeUUID(boundBinder);
        if (boundDrakeUUID == null) {
            return;
        }
        
        // For stored drakes, we don't need to check if they're alive in the world
        // since they're stored inside the binder
        
        // Apply resistance buffs to player and nearby allies
        applyPortableResistanceBuffs(player, boundDrakeUUID);
    }
    
    /**
     * Find a bound Drake Binder in the player's inventory
     */
    private static ItemStack findBoundDrakeBinder(Player player) {
        for (ItemStack item : player.getInventory().items) {
            if (item.getItem() instanceof DrakeBinderItem && DrakeBinderItem.isBound(item)) {
                return item;
            }
        }
        return null;
    }
    
    
    /**
     * Apply portable resistance buffs to the player and nearby allies
     */
    private static void applyPortableResistanceBuffs(Player player, UUID boundDrakeUUID) {
        // Apply buff to the player
        applyResistanceToEntity(player, boundDrakeUUID);
        
        // Find nearby allies and apply buffs to them
        var nearbyEntities = player.level().getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(BUFF_RANGE),
            entity -> entity != player && isEligibleForPortableBuff(entity, player)
        );
        
        for (LivingEntity entity : nearbyEntities) {
            applyResistanceToEntity(entity, boundDrakeUUID);
        }
    }
    
    /**
     * Check if an entity is eligible for portable resistance buff
     */
    private static boolean isEligibleForPortableBuff(LivingEntity entity, Player player) {
        // Players
        if (entity instanceof Player) {
            return true;
        }
        
        // Tamed dragons owned by the player
        if (entity instanceof com.leon.saintsdragons.server.entity.base.DragonEntity dragon) {
            return dragon.isTame() && dragon.isOwnedBy(player);
        }
        
        // Registered allies (if the bound drake is a primitive drake)
        if (entity instanceof com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity drake) {
            return drake.isTame() && drake.allyManager.isAlly(player);
        }
        
        return false;
    }
    
    /**
     * Apply resistance effect to an entity
     */
    private static void applyResistanceToEntity(LivingEntity entity, UUID boundDrakeUUID) {
        // Create resistance effect with no particles and infinite duration
        MobEffectInstance resistanceEffect = new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE,
            BUFF_DURATION,
            BUFF_AMPLIFIER,
            false, // ambient
            false, // visible particles
            false  // show icon
        );
        
        // Apply the effect
        entity.addEffect(resistanceEffect);
    }
    
    /**
     * Remove portable resistance buffs from an entity
     */
    public static void removePortableResistanceFromEntity(LivingEntity entity) {
        entity.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }
}
