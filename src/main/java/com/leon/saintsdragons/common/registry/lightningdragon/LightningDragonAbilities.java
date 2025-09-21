package com.leon.saintsdragons.common.registry.lightningdragon;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.LightningDragonBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.LightningDragonHornGoreAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.LightningDragonBeamAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.LightningDragonRoarAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon.LightningDragonSummonStormAbility;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.ability.DieAbility;

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

    // Generic abilities (can be used by any dragon)
    public static final DragonAbilityType<LightningDragonEntity, HurtAbility<LightningDragonEntity>> HURT =
            AbilityRegistry.register(new DragonAbilityType<>("hurt", HurtAbility::new));

    public static final DragonAbilityType<LightningDragonEntity, DieAbility<LightningDragonEntity>> DIE =
            AbilityRegistry.register(new DragonAbilityType<>("die", DieAbility::new));
}