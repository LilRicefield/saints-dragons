package com.leon.saintsdragons.common.registry.riftdrake;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.riftdrake.RiftDrakePhaseShiftAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.riftdrake.RiftDrakeBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.riftdrake.RiftDrakeBite2Ability;
import com.leon.saintsdragons.server.entity.ability.abilities.riftdrake.RiftDrakeClawAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.riftdrake.RiftDrakeRoarAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.riftdrake.RiftDrakeHornGoreAbility;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.ability.DieAbility;

/**
 * Rift Drake specific abilities.
 * Contains all abilities unique to the Rift Drake type.
 */
public final class RiftDrakeAbilities {
    private RiftDrakeAbilities() {}

    // Ability ID constants
    public static final String BITE_ID = "riftdrake_bite";
    public static final String BITE2_ID = "riftdrake_bite2";
    public static final String CLAW_ID = "riftdrake_claw";
    public static final String HORN_GORE_ID = "riftdrake_horn_gore";
    public static final String ROAR_ID = "riftdrake_roar";
    public static final String PHASE_SHIFT_ID = "phase_shift";
    public static final String HURT_ID = "riftdrake_hurt";
    public static final String DIE_ID = "riftdrake_die";

    // Phase 1 melee attack
    public static final DragonAbilityType<RiftDrakeEntity, RiftDrakeBiteAbility> BITE =
            AbilityRegistry.register(new DragonAbilityType<>(BITE_ID, RiftDrakeBiteAbility::new));

    // Phase 2 rage mode bite - faster
    public static final DragonAbilityType<RiftDrakeEntity, RiftDrakeBite2Ability> BITE2 =
            AbilityRegistry.register(new DragonAbilityType<>(BITE2_ID, RiftDrakeBite2Ability::new));

    // Phase 2 melee attack
    public static final DragonAbilityType<RiftDrakeEntity, RiftDrakeClawAbility> CLAW =
            AbilityRegistry.register(new DragonAbilityType<>(CLAW_ID, RiftDrakeClawAbility::new));

    // Horn gore - strong knockback melee (works in both phases)
    public static final DragonAbilityType<RiftDrakeEntity, RiftDrakeHornGoreAbility> HORN_GORE =
            AbilityRegistry.register(new DragonAbilityType<>(HORN_GORE_ID, RiftDrakeHornGoreAbility::new));

    // Roar - cosmetic ability
    public static final DragonAbilityType<RiftDrakeEntity, RiftDrakeRoarAbility> ROAR =
            AbilityRegistry.register(new DragonAbilityType<>(ROAR_ID, RiftDrakeRoarAbility::new));

    // Ultimate ability
    public static final DragonAbilityType<RiftDrakeEntity, RiftDrakePhaseShiftAbility> PHASE_SHIFT =
            AbilityRegistry.register(new DragonAbilityType<>(PHASE_SHIFT_ID, RiftDrakePhaseShiftAbility::new));

    // Generic abilities with unique IDs
    public static final DragonAbilityType<RiftDrakeEntity, HurtAbility<RiftDrakeEntity>> HURT =
            AbilityRegistry.register(new DragonAbilityType<>(HURT_ID, HurtAbility::new));

    public static final DragonAbilityType<RiftDrakeEntity, DieAbility<RiftDrakeEntity>> DIE =
            AbilityRegistry.register(new DragonAbilityType<>(DIE_ID, DieAbility::new));
}
