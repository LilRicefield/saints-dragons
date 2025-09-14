package com.leon.saintsdragons.server.entity.handler;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;

import java.util.HashMap;
import java.util.Map;

/**
 * Single responsibility: Track active ability and global cooldowns
 */
public class DragonCombatHandler {
    private final LightningDragonEntity dragon;
    
    private DragonAbility<?> activeAbility;
    private int globalCooldown = 0; // Global cooldown between any abilities
    private boolean processingAbility = false; // Prevent re-entry during ability start
    
    // Per-ability cooldown tracking
    private final Map<DragonAbilityType<?, ?>, Integer> abilityCooldowns = new HashMap<>();

    // ===== PERSISTENCE =====
    // Persist global + per-ability cooldowns across save/load
    public void saveToNBT(net.minecraft.nbt.CompoundTag tag) {
        tag.putInt("GlobalAbilityCooldown", Math.max(0, globalCooldown));
        net.minecraft.nbt.CompoundTag cd = new net.minecraft.nbt.CompoundTag();
        for (Map.Entry<DragonAbilityType<?, ?>, Integer> e : abilityCooldowns.entrySet()) {
            String name = com.leon.saintsdragons.common.registry.AbilityRegistry.getName(e.getKey());
            if (name != null && !name.isEmpty()) {
                cd.putInt(name, Math.max(0, e.getValue()));
            }
        }
        if (!cd.isEmpty()) {
            tag.put("AbilityCooldowns", cd);
        }
    }

    public void loadFromNBT(net.minecraft.nbt.CompoundTag tag) {
        this.globalCooldown = Math.max(0, tag.getInt("GlobalAbilityCooldown"));
        this.abilityCooldowns.clear();
        if (tag.contains("AbilityCooldowns", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            net.minecraft.nbt.CompoundTag cd = tag.getCompound("AbilityCooldowns");
            for (String key : cd.getAllKeys()) {
                var type = com.leon.saintsdragons.common.registry.AbilityRegistry.get(key);
                if (type != null) {
                    int val = Math.max(0, cd.getInt(key));
                    if (val > 0) this.abilityCooldowns.put(type, val);
                }
            }
        }
    }

    public DragonCombatHandler(LightningDragonEntity dragon) {
        this.dragon = dragon;
    }

    public DragonAbility<?> getActiveAbility() {
        return activeAbility;
    }
    
    public void setActiveAbility(DragonAbility<?> ability) {
        this.activeAbility = ability;
    }

    public DragonAbilityType<?, ?> getActiveAbilityType() {
        return activeAbility != null ? activeAbility.getAbilityType() : null;
    }

    public boolean canUseAbility() {
        return globalCooldown == 0 && (activeAbility == null || !activeAbility.isUsing()) && !processingAbility;
    }
    
    /**
     * Check if a specific ability type can be started (includes per-ability cooldown)
     */
    public boolean canStart(DragonAbilityType<?, ?> abilityType) {
        return globalCooldown == 0
            && activeAbility == null
            && !processingAbility
            && isAbilityCooldownReady(abilityType);
    }
    
    /**
     * Check if a specific ability's cooldown is ready
     */
    public boolean isAbilityCooldownReady(DragonAbilityType<?, ?> abilityType) {
        return abilityCooldowns.getOrDefault(abilityType, 0) <= 0;
    }
    
    /**
     * Set cooldown for a specific ability type
     */
    public void setAbilityCooldown(DragonAbilityType<?, ?> abilityType, int cooldownTicks) {
        abilityCooldowns.put(abilityType, cooldownTicks);
    }

    public void tryUseAbility(DragonAbilityType<?, ?> abilityType) {
        // Enforce global and per-ability cooldowns
        if (!canStart(abilityType)) return;
        
        processingAbility = true; // Guard against re-entry
        try {
            @SuppressWarnings("unchecked")
            var ability = ((DragonAbilityType<LightningDragonEntity, ?>) abilityType).makeInstance(dragon);
            
            if (ability.tryAbility()) {
                // Set ability active IMMEDIATELY to prevent race conditions
                setActiveAbility(ability);
                ability.start();
            }
        } finally {
            processingAbility = false;
        }
    }

    public void forceEndActiveAbility() {
        if (activeAbility != null) {
            activeAbility.interrupt();
            activeAbility = null;
        }
    }

    // Removed unused target validation stub

    public void tick() {
        if (globalCooldown > 0) {
            globalCooldown--;
        }
        
        // Tick down per-ability cooldowns
        abilityCooldowns.entrySet().removeIf(entry -> {
            int newValue = entry.getValue() - 1;
            if (newValue <= 0) {
                return true; // Remove from map when cooldown reaches 0
            } else {
                entry.setValue(newValue);
                return false;
            }
        });
        
        if (activeAbility != null) {
            if (activeAbility.isUsing()) {
                activeAbility.tick();
            } else {
                // Ability finished, set a small fixed global cooldown between abilities
                // ~0.3s between abilities
                globalCooldown = 6;
                // Apply per-ability cooldown based on the finished ability's configured cooldown
                DragonAbilityType<?, ?> finishedType = getActiveAbilityType();
                if (finishedType != null) {
                    setAbilityCooldown(finishedType, activeAbility.getMaxCooldown());
                }
                activeAbility = null;
            }
        }
    }
}
