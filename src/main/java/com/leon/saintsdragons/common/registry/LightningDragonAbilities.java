package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.LightningDragonBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.LightningDragonHornGoreAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.LightningDragonBeamAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.LightningDragonRoarAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.LightningDragonSummonStormAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.LightningDragonHurtAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.LightningDragonDieAbility;

/**
 * Lightning Dragon specific abilities.
 * Contains all abilities unique to the Lightning Dragon type.
 */
public final class LightningDragonAbilities {
    private LightningDragonAbilities() {}

    // Combat abilities
    public static final DragonAbilityType<LightningDragonEntity, LightningDragonBiteAbility> BITE =
            AbilityRegistry.register(new DragonAbilityType<>("lightning_bite", LightningDragonBiteAbility::new));

    public static final DragonAbilityType<LightningDragonEntity, LightningDragonHornGoreAbility> HORN_GORE =
            AbilityRegistry.register(new DragonAbilityType<>("lightning_horn_gore", LightningDragonHornGoreAbility::new));

    public static final DragonAbilityType<LightningDragonEntity, LightningDragonBeamAbility> LIGHTNING_BEAM =
            AbilityRegistry.register(new DragonAbilityType<>("lightning_beam", LightningDragonBeamAbility::new));

    public static final DragonAbilityType<LightningDragonEntity, LightningDragonRoarAbility> ROAR =
            AbilityRegistry.register(new DragonAbilityType<>("lightning_roar", LightningDragonRoarAbility::new));

    // Ultimate ability
    public static final DragonAbilityType<LightningDragonEntity, LightningDragonSummonStormAbility> SUMMON_STORM =
            AbilityRegistry.register(new DragonAbilityType<>("lightning_summon_storm", LightningDragonSummonStormAbility::new));

    // Passive/triggered abilities
    public static final DragonAbilityType<LightningDragonEntity, LightningDragonHurtAbility> HURT =
            AbilityRegistry.register(new DragonAbilityType<>("lightning_hurt", LightningDragonHurtAbility::new));

    public static final DragonAbilityType<LightningDragonEntity, LightningDragonDieAbility> DIE =
            AbilityRegistry.register(new DragonAbilityType<>("lightning_die", LightningDragonDieAbility::new));
}
