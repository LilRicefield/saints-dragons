package com.leon.saintsdragons.common.registry.stegonaut;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.ability.DieAbility;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;

/**
 * Ability registry entries for the Primitive Drake.
 */
public final class StegonautAbilities {
    private StegonautAbilities() {}

    public static final String STEGONAUT_HURT_ID = "stegonaut_hurt";
    public static final String STEGONAUT_DIE_ID = "stegonaut_die";

    public static final DragonAbilityType<Stegonaut, HurtAbility<Stegonaut>> STEGONAUT_HURT =
            AbilityRegistry.register(new DragonAbilityType<>(STEGONAUT_HURT_ID, HurtAbility::new));

    public static final DragonAbilityType<Stegonaut, DieAbility<Stegonaut>> STEGONAUT_DIE =
            AbilityRegistry.register(new DragonAbilityType<>(STEGONAUT_DIE_ID, DieAbility::new));
}
