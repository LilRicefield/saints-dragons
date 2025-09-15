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
 * Simple holder for dragon ability types.
 */
public final class ModAbilities {
    private ModAbilities() {}

    public static final DragonAbilityType<LightningDragonEntity, LightningBiteAbility> BITE =
            AbilityRegistry.register(new DragonAbilityType<>("bite", LightningBiteAbility::new));

    public static final DragonAbilityType<LightningDragonEntity, HornGoreAbility> HORN_GORE =
            AbilityRegistry.register(new DragonAbilityType<>("horn_gore", HornGoreAbility::new));

    public static final DragonAbilityType<LightningDragonEntity, LightningBeamAbility> LIGHTNING_BEAM =
            AbilityRegistry.register(new DragonAbilityType<>("lightning_beam", LightningBeamAbility::new));

    public static final DragonAbilityType<LightningDragonEntity, LightningRoarAbility> ROAR =
            AbilityRegistry.register(new DragonAbilityType<>("roar", LightningRoarAbility::new));

    // Ultimate: Summon Storm
    public static final DragonAbilityType<LightningDragonEntity, SummonStormAbility> SUMMON_STORM =
            AbilityRegistry.register(new DragonAbilityType<>("summon_storm", SummonStormAbility::new));

    // Passive/triggered on damage; one-shot animation + sound
    public static final DragonAbilityType<LightningDragonEntity, HurtAbility> HURT =
            AbilityRegistry.register(new DragonAbilityType<>("hurt", HurtAbility::new));

    public static final DragonAbilityType<LightningDragonEntity, DieAbility> DIE =
            AbilityRegistry.register(new DragonAbilityType<>("die", DieAbility::new));
}
