package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.server.entity.ability.DieAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.base.DragonEntity;

/**
 * Base dragon abilities that all dragon types can use.
 * These are generic abilities that work with any DragonEntity.
 */
public final class BaseDragonAbilities {
    private BaseDragonAbilities() {}

    // Generic abilities for all dragons
    public static final DragonAbilityType<DragonEntity, HurtAbility> HURT =
            AbilityRegistry.register(new DragonAbilityType<>("dragon_hurt", HurtAbility::new));

    public static final DragonAbilityType<DragonEntity, DieAbility> DIE =
            AbilityRegistry.register(new DragonAbilityType<>("dragon_die", DieAbility::new));
}
