package com.leon.saintsdragons.common.registry.nulljaw;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;

public final class NulljawAbilities {
    private NulljawAbilities() {
    }

    public static final String NULLJAW_HURT_ID = "nulljaw_hurt";

    public static final DragonAbilityType<Nulljaw, HurtAbility<Nulljaw>> HURT =
            AbilityRegistry.register(new DragonAbilityType<>(NULLJAW_HURT_ID, HurtAbility::new));

    public static void init() {
        // Intentionally empty.
    }
}
