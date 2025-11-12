package com.leon.saintsdragons.common.registry.ignivorus;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.ability.DieAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusBodySlamAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireBreathAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusRoarAbility;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;

/**
 * Ability registrations for the Ignivorus.
 */
public final class IgnivorusAbilities {
    private IgnivorusAbilities() {}

    public static final String IGNIVORUS_FIRE_BREATH_ID = "ignivorus_fire_breath";
    public static final String IGNIVORUS_BITE_ID = "ignivorus_bite";
    public static final String IGNIVORUS_ROAR_ID = "ignivorus_roar";
    public static final String IGNIVORUS_BODY_SLAM_ID = "ignivorus_body_slam";
    public static final String IGNIVORUS_HURT_ID = "ignivorus_hurt";
    public static final String IGNIVORUS_DIE_ID = "ignivorus_die";

    public static final DragonAbilityType<Ignivorus, IgnivorusFireBreathAbility> IGNIVORUS_FIRE_BREATH =
            AbilityRegistry.register(new DragonAbilityType<>(IGNIVORUS_FIRE_BREATH_ID, IgnivorusFireBreathAbility::new));

    public static final DragonAbilityType<Ignivorus, IgnivorusBiteAbility> IGNIVORUS_BITE =
            AbilityRegistry.register(new DragonAbilityType<>(IGNIVORUS_BITE_ID, IgnivorusBiteAbility::new));

    public static final DragonAbilityType<Ignivorus, IgnivorusRoarAbility> IGNIVORUS_ROAR =
            AbilityRegistry.register(new DragonAbilityType<>(IGNIVORUS_ROAR_ID, IgnivorusRoarAbility::new));

    public static final DragonAbilityType<Ignivorus, IgnivorusBodySlamAbility> IGNIVORUS_BODY_SLAM =
            AbilityRegistry.register(new DragonAbilityType<>(IGNIVORUS_BODY_SLAM_ID, IgnivorusBodySlamAbility::new));

    public static final DragonAbilityType<Ignivorus, HurtAbility<Ignivorus>> IGNIVORUS_HURT =
            AbilityRegistry.register(new DragonAbilityType<>(IGNIVORUS_HURT_ID, HurtAbility::new));

    public static final DragonAbilityType<Ignivorus, DieAbility<Ignivorus>> IGNIVORUS_DIE =
            AbilityRegistry.register(new DragonAbilityType<>(IGNIVORUS_DIE_ID, DieAbility::new));

    public static void init() {
        // Trigger static init
    }
}
