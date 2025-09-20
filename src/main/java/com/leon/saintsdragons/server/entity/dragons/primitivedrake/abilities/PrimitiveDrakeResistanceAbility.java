package com.leon.saintsdragons.server.entity.dragons.primitivedrake.abilities;

import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ability for Primitive Drakes to provide resistance buffs to nearby players and allies.
 * This makes the drake a valuable companion that protects its friends.
 */
public class PrimitiveDrakeResistanceAbility {
    private final PrimitiveDrakeEntity drake;
    private final Level level;
    
    // Buff parameters
    private static final double BUFF_RANGE = 8.0; // 8 block radius
    private static final int BUFF_DURATION = Integer.MAX_VALUE; // Infinite duration
    private static final int BUFF_AMPLIFIER = 0; // Resistance I (20% damage reduction)
    
    // Performance optimization
    private int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 20; // Update every second
    
    // Track entities that already have the resistance buff
    private final java.util.Set<net.minecraft.world.entity.Entity> buffedEntities = new java.util.HashSet<>();
    
    public PrimitiveDrakeResistanceAbility(PrimitiveDrakeEntity drake) {
        this.drake = drake;
        this.level = drake.level();
    }
    
    /**
     * Called every tick to apply resistance buffs to nearby entities
     */
    public void tick() {
        // Only update every second for performance
        if (++tickCounter < UPDATE_INTERVAL) {
            return;
        }
        tickCounter = 0;
        
        // Only apply buffs if drake is alive and not playing dead
        if (!drake.isAlive() || drake.isPlayingDead()) {
            return;
        }
        
        // Apply buffs to nearby entities
        applyResistanceBuffs();
    }
    
    /**
     * Apply resistance buffs to all eligible entities within range
     */
    private void applyResistanceBuffs() {
        // Get all living entities within range
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
            LivingEntity.class,
            drake.getBoundingBox().inflate(BUFF_RANGE),
            this::isEligibleForBuff
        );
        
        // Create a set of currently nearby entities for cleanup
        java.util.Set<net.minecraft.world.entity.Entity> currentNearby = new java.util.HashSet<>(nearbyEntities);
        
        // Apply resistance buff to new entities
        for (LivingEntity entity : nearbyEntities) {
            if (!buffedEntities.contains(entity)) {
                applyResistanceToEntity(entity);
                buffedEntities.add(entity);
            }
        }
        
        // Remove buff from entities that are no longer nearby
        buffedEntities.removeIf(entity -> {
            if (!currentNearby.contains(entity)) {
                if (entity instanceof LivingEntity livingEntity) {
                    removeResistanceFromEntity(livingEntity);
                }
                return true;
            }
            return false;
        });
    }
    
    /**
     * Check if an entity is eligible for the resistance buff
     */
    private boolean isEligibleForBuff(LivingEntity entity) {
        // Don't buff the drake itself
        if (entity == drake) {
            return false;
        }
        
        // Don't buff dead entities
        if (!entity.isAlive()) {
            return false;
        }
        
        // Buff players
        if (entity instanceof Player) {
            return true;
        }
        
        // Buff tamed pets (dragons)
        if (entity instanceof DragonEntity dragon) {
            return dragon.isTame();
        }
        
        // Buff registered allies (only players can be allies)
        if (drake.isTame() && entity instanceof Player player) {
            // Check if this player is registered as an ally
            return drake.allyManager.isAlly(player);
        }
        
        return false;
    }
    
    /**
     * Apply resistance buff to a specific entity
     */
    private void applyResistanceToEntity(LivingEntity entity) {
        // Create resistance effect with infinite duration and no particles
        MobEffectInstance resistanceEffect = new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE,
            BUFF_DURATION,
            BUFF_AMPLIFIER,
            false, // Not ambient
            false, // No particles (no flashing!)
            true   // Show icon
        );
        
        // Apply the effect
        entity.addEffect(resistanceEffect);
    }
    
    /**
     * Remove resistance buff from a specific entity
     */
    private void removeResistanceFromEntity(LivingEntity entity) {
        // Remove the resistance effect
        entity.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }
    
    /**
     * Get the buff range for display purposes
     */
    public static double getBuffRange() {
        return BUFF_RANGE;
    }
    
    /**
     * Get the buff amplifier level
     */
    public static int getBuffAmplifier() {
        return BUFF_AMPLIFIER;
    }
    
    /**
     * Clean up all buffed entities when the drake is removed
     */
    public void cleanup() {
        // Remove resistance from all tracked entities
        for (net.minecraft.world.entity.Entity entity : buffedEntities) {
            if (entity instanceof LivingEntity livingEntity) {
                removeResistanceFromEntity(livingEntity);
            }
        }
        buffedEntities.clear();
    }
}
