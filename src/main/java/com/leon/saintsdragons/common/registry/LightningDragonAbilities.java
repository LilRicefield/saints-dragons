package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.LightningBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.HornGoreAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.LightningBeamAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.LightningRoarAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.SummonStormAbility;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.ability.DieAbility;

/**
 * Lightning Dragon specific abilities.
 * Contains all abilities unique to the Lightning Dragon type.
 */
public final class LightningDragonAbilities {
    private LightningDragonAbilities() {}

    // Combat abilities
    public static final DragonAbilityType<LightningDragonEntity, LightningBiteAbility> BITE =
            AbilityRegistry.register(new DragonAbilityType<>("lightning_bite", LightningBiteAbility::new));

    public static final DragonAbilityType<LightningDragonEntity, HornGoreAbility> HORN_GORE =
            AbilityRegistry.register(new DragonAbilityType<>("lightning_horn_gore", HornGoreAbility::new));

    public static final DragonAbilityType<LightningDragonEntity, LightningBeamAbility> LIGHTNING_BEAM =
            AbilityRegistry.register(new DragonAbilityType<>("lightning_beam", LightningBeamAbility::new));

    public static final DragonAbilityType<LightningDragonEntity, LightningRoarAbility> ROAR =
            AbilityRegistry.register(new DragonAbilityType<>("lightning_roar", LightningRoarAbility::new));

    // Ultimate ability
    public static final DragonAbilityType<LightningDragonEntity, SummonStormAbility> SUMMON_STORM =
            AbilityRegistry.register(new DragonAbilityType<>("lightning_summon_storm", SummonStormAbility::new));

    // Passive/triggered abilities (use base abilities)
    public static final DragonAbilityType<?, ?> HURT = BaseDragonAbilities.HURT;
    public static final DragonAbilityType<?, ?> DIE = BaseDragonAbilities.DIE;
}
