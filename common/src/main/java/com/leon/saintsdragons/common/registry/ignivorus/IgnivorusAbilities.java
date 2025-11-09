package com.leon.saintsdragons.common.registry.ignivorus;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireBreathAbility;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;

/**
 * Ability registrations for the Ignivorus.
 */
public final class IgnivorusAbilities {
    private IgnivorusAbilities() {}

    public static final String IGNIVORUS_FIRE_BREATH_ID = "ignivorus_fire_breath";

    public static final DragonAbilityType<Ignivorus, IgnivorusFireBreathAbility> IGNIVORUS_FIRE_BREATH =
            AbilityRegistry.register(new DragonAbilityType<>(IGNIVORUS_FIRE_BREATH_ID, IgnivorusFireBreathAbility::new));

    public static void init() {
        // Trigger static init
    }
}
