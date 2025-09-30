package com.leon.saintsdragons.common.registry.amphithere;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.amphithere.AmphithereBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.amphithere.AmphithereFireBodyAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.amphithere.AmphithereMagmaVolleyAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.amphithere.AmphithereRoarAbility;
import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;

/**
 * Ability registry entries for the Amphithere.
 */
public final class AmphithereAbilities {
    private AmphithereAbilities() {}

    public static final String BITE_ID = "amphithere_bite";
    public static final String FIRE_BODY_ID = "amphithere_fire_body";
    public static final String ROAR_ID = "amphithere_roar";
    public static final String FIRE_BREATH_VOLLEY_ID = "amphithere_fire_breath_volley";

    public static final DragonAbilityType<AmphithereEntity, AmphithereBiteAbility> BITE =
            AbilityRegistry.register(new DragonAbilityType<>(BITE_ID, AmphithereBiteAbility::new));

    public static final DragonAbilityType<AmphithereEntity, AmphithereFireBodyAbility> FIRE_BODY =
            AbilityRegistry.register(new DragonAbilityType<>(FIRE_BODY_ID, AmphithereFireBodyAbility::new));

    public static final DragonAbilityType<AmphithereEntity, AmphithereRoarAbility> ROAR =
            AbilityRegistry.register(new DragonAbilityType<>(ROAR_ID, AmphithereRoarAbility::new));

    public static final DragonAbilityType<AmphithereEntity, AmphithereMagmaVolleyAbility> FIRE_BREATH_VOLLEY =
            AbilityRegistry.register(new DragonAbilityType<>(FIRE_BREATH_VOLLEY_ID, AmphithereMagmaVolleyAbility::new));
}
