package com.leon.saintsdragons.common.registry.stegonaut;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.ability.DieAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.stegonaut.StegonautBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.stegonaut.StegonautChinSlamAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.stegonaut.StegonautGroundEatingAbility;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;

/**
 * Ability registry entries for the Primitive Drake.
 */
public final class StegonautAbilities {
    private StegonautAbilities() {}

    public static final String STEGONAUT_HURT_ID = "stegonaut_hurt";
    public static final String STEGONAUT_DIE_ID = "stegonaut_die";
    public static final String STEGONAUT_BITE_ID = "stegonaut_bite";
    public static final String STEGONAUT_CHIN_SLAM_ID = "stegonaut_chin_slam";
    public static final String STEGONAUT_GROUND_EATING_ID = "stegonaut_ground_eating";

    public static final DragonAbilityType<Stegonaut, StegonautBiteAbility> STEGONAUT_BITE =
            AbilityRegistry.register(new DragonAbilityType<>(STEGONAUT_BITE_ID, StegonautBiteAbility::new));

    public static final DragonAbilityType<Stegonaut, StegonautChinSlamAbility> STEGONAUT_CHIN_SLAM =
            AbilityRegistry.register(new DragonAbilityType<>(STEGONAUT_CHIN_SLAM_ID, StegonautChinSlamAbility::new));

    public static final DragonAbilityType<Stegonaut, StegonautGroundEatingAbility> STEGONAUT_GROUND_EATING =
            AbilityRegistry.register(new DragonAbilityType<>(STEGONAUT_GROUND_EATING_ID, StegonautGroundEatingAbility::new));

    public static final DragonAbilityType<Stegonaut, HurtAbility<Stegonaut>> STEGONAUT_HURT =
            AbilityRegistry.register(new DragonAbilityType<>(STEGONAUT_HURT_ID, HurtAbility::new));

    public static final DragonAbilityType<Stegonaut, DieAbility<Stegonaut>> STEGONAUT_DIE =
            AbilityRegistry.register(new DragonAbilityType<>(STEGONAUT_DIE_ID, DieAbility::new));

    /**
     * No-op hook to trigger class loading for static registration.
     */
    public static void init() {
        // Intentionally empty
    }
}
