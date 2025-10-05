package com.leon.saintsdragons.common.registry.riftdrake;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.riftdrake.RiftDrakePhaseShiftAbility;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.ability.DieAbility;

/**
 * Rift Drake specific abilities.
 * Contains all abilities unique to the Rift Drake type.
 */
public final class RiftDrakeAbilities {
    private RiftDrakeAbilities() {}

    // Ultimate ability
    public static final DragonAbilityType<RiftDrakeEntity, RiftDrakePhaseShiftAbility> PHASE_SHIFT =
            AbilityRegistry.register(new DragonAbilityType<>("phase_shift", RiftDrakePhaseShiftAbility::new));

    // Generic abilities with unique IDs
    public static final DragonAbilityType<RiftDrakeEntity, HurtAbility<RiftDrakeEntity>> HURT =
            AbilityRegistry.register(new DragonAbilityType<>("riftdrake_hurt", HurtAbility::new));

    public static final DragonAbilityType<RiftDrakeEntity, DieAbility<RiftDrakeEntity>> DIE =
            AbilityRegistry.register(new DragonAbilityType<>("riftdrake_die", DieAbility::new));
}
