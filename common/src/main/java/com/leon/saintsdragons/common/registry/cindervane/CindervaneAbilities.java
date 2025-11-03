package com.leon.saintsdragons.common.registry.cindervane;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.ability.DieAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.cindervane.CindervaneBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.cindervane.CindervaneFireBodyAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.cindervane.CindervaneVolleyAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.cindervane.CindervaneRoarAbility;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;

/**
 * Ability registry entries for the Amphithere.
 */
public final class CindervaneAbilities {
    private CindervaneAbilities() {}

    public static final String BITE_ID = "cindervane_bite";
    public static final String FIRE_BODY_ID = "cindervane_fire_body";
    public static final String ROAR_ID = "cindervane_roar";
    public static final String FIRE_BREATH_VOLLEY_ID = "cindervane_fire_breath_volley";
    public static final String HURT_ID = "cindervane_hurt";
    public static final String DIE_ID = "cindervane_die";


    public static final DragonAbilityType<Cindervane, CindervaneBiteAbility> BITE =
            AbilityRegistry.register(new DragonAbilityType<>(BITE_ID, CindervaneBiteAbility::new));

    public static final DragonAbilityType<Cindervane, CindervaneFireBodyAbility> FIRE_BODY =
            AbilityRegistry.register(new DragonAbilityType<>(FIRE_BODY_ID, CindervaneFireBodyAbility::new));

    public static final DragonAbilityType<Cindervane, CindervaneRoarAbility> ROAR =
            AbilityRegistry.register(new DragonAbilityType<>(ROAR_ID, CindervaneRoarAbility::new));

    public static final DragonAbilityType<Cindervane, CindervaneVolleyAbility> FIRE_BREATH_VOLLEY =
            AbilityRegistry.register(new DragonAbilityType<>(FIRE_BREATH_VOLLEY_ID, CindervaneVolleyAbility::new));

    public static final DragonAbilityType<Cindervane, HurtAbility<Cindervane>> HURT =
            AbilityRegistry.register(new DragonAbilityType<>(HURT_ID, HurtAbility::new));

    public static final DragonAbilityType<Cindervane, DieAbility<Cindervane>> DIE =
            AbilityRegistry.register(new DragonAbilityType<>(DIE_ID, DieAbility::new));
}

