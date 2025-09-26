package com.leon.saintsdragons.common.registry.amphithere;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.amphithere.AmphithereFireBodyAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.amphithere.AmphithereRoarAbility;
import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;

/**
 * Ability registry entries for the Amphithere.
 */
public final class AmphithereAbilities {
    private AmphithereAbilities() {}

    public static final String FIRE_BODY_ID = "amphithere_fire_body";
    public static final String ROAR_ID = "amphithere_roar";

    public static final DragonAbilityType<AmphithereEntity, AmphithereFireBodyAbility> FIRE_BODY =
            AbilityRegistry.register(new DragonAbilityType<>(FIRE_BODY_ID, AmphithereFireBodyAbility::new));

    public static final DragonAbilityType<AmphithereEntity, AmphithereRoarAbility> ROAR =
            AbilityRegistry.register(new DragonAbilityType<>(ROAR_ID, AmphithereRoarAbility::new));
}
