package com.leon.saintsdragons.common.registry;

/**
 * Central registry for all dragon abilities.
 * This class provides backward compatibility and easy access to dragon abilities.
 * 
 * For dragon-specific abilities, use the individual dragon ability classes:
 * - LightningDragonAbilities for Lightning Dragon abilities
 * - (Future dragon types will have their own ability classes)
 */
public final class ModAbilities {
    private ModAbilities() {}

    // ===== LIGHTNING DRAGON ABILITIES =====
    // These are kept here for backward compatibility
    // New code should use LightningDragonAbilities directly
    
    public static final com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> BITE = 
            LightningDragonAbilities.BITE;
    
    public static final com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> HORN_GORE = 
            LightningDragonAbilities.HORN_GORE;
    
    public static final com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> LIGHTNING_BEAM = 
            LightningDragonAbilities.LIGHTNING_BEAM;
    
    public static final com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> ROAR = 
            LightningDragonAbilities.ROAR;
    
    public static final com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> SUMMON_STORM = 
            LightningDragonAbilities.SUMMON_STORM;
    
    public static final com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> HURT = 
            BaseDragonAbilities.HURT;
    
    public static final com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> DIE = 
            BaseDragonAbilities.DIE;
}
