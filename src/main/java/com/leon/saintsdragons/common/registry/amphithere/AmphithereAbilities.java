package com.leon.saintsdragons.common.registry.amphithere;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.ability.DieAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.amphithere.AmphithereFireBreathAbility;
import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;

/**
 * Amphithere specific abilities.
 * Contains all abilities unique to the Amphithere type.
 */
public final class AmphithereAbilities {
    private AmphithereAbilities() {}

    // Combat abilities
    public static final DragonAbilityType<AmphithereEntity, AmphithereFireBreathAbility> FIRE_BREATH =
            AbilityRegistry.register(new DragonAbilityType<>("amphithere_fire_breath", AmphithereFireBreathAbility::new));

    // Generic abilities (can be used by any dragon)
    public static final DragonAbilityType<AmphithereEntity, HurtAbility<AmphithereEntity>> HURT =
            AbilityRegistry.register(new DragonAbilityType<>("hurt", HurtAbility::new));

    public static final DragonAbilityType<AmphithereEntity, DieAbility<AmphithereEntity>> DIE =
            AbilityRegistry.register(new DragonAbilityType<>("die", DieAbility::new));
}
