package com.leon.saintsdragons.common.registry.raevyx;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxHornGoreAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxBeamAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxRoarAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxSummonStormAbility;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.ability.DieAbility;

/**
 * Contains all abilities unique to Raevyx.
 */
public final class RaevyxAbilities {
    private RaevyxAbilities() {}

    // Combat abilities
    public static final DragonAbilityType<Raevyx, RaevyxBiteAbility> RAEVYX_BITE =
            AbilityRegistry.register(new DragonAbilityType<>("raevyx_bite", RaevyxBiteAbility::new));

    public static final DragonAbilityType<Raevyx, RaevyxHornGoreAbility> RAEVYX_HORN_GORE =
            AbilityRegistry.register(new DragonAbilityType<>("raevyx_horn_gore", RaevyxHornGoreAbility::new));

    public static final DragonAbilityType<Raevyx, RaevyxBeamAbility> RAEVYX_LIGHTNING_BEAM =
            AbilityRegistry.register(new DragonAbilityType<>("raevyx_lightning_beam", RaevyxBeamAbility::new));

    public static final DragonAbilityType<Raevyx, RaevyxRoarAbility> RAEVYX_ROAR =
            AbilityRegistry.register(new DragonAbilityType<>("raevyx_roar", RaevyxRoarAbility::new));

    // Ultimate ability
    public static final DragonAbilityType<Raevyx, RaevyxSummonStormAbility> RAEVYX_SUMMON_STORM =
            AbilityRegistry.register(new DragonAbilityType<>("raevyx_summon_storm", RaevyxSummonStormAbility::new));

    // Generic abilities (can be used by any wyvern)
    public static final DragonAbilityType<Raevyx, HurtAbility<Raevyx>> HURT =
            AbilityRegistry.register(new DragonAbilityType<>("raevyx_hurt", HurtAbility::new));

    public static final DragonAbilityType<Raevyx, DieAbility<Raevyx>> DIE =
            AbilityRegistry.register(new DragonAbilityType<>("raevyx_die", DieAbility::new));

    // Baby-specific abilities
    public static final DragonAbilityType<Raevyx, HurtAbility<Raevyx>> BABY_HURT =
            AbilityRegistry.register(new DragonAbilityType<>("baby_raevyx_hurt", HurtAbility::new));

    public static final DragonAbilityType<Raevyx, DieAbility<Raevyx>> BABY_DIE =
            AbilityRegistry.register(new DragonAbilityType<>("baby_raevyx_die", DieAbility::new));
}
