package com.leon.saintsdragons.common.registry.nulljaw;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.nulljaw.NulljawPhaseShiftAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.nulljaw.NulljawBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.nulljaw.NulljawBite2Ability;
import com.leon.saintsdragons.server.entity.ability.abilities.nulljaw.NulljawClawAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.nulljaw.NulljawRoarAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.nulljaw.NulljawHornGoreAbility;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.ability.DieAbility;

/**
 * Rift Drake specific abilities.
 * Contains all abilities unique to the Rift Drake type.
 */
public final class NulljawAbilities {
    private NulljawAbilities() {}

    // Ability ID constants
    public static final String NULLJAW_BITE_ID = "nulljaw_bite";
    public static final String NULLJAW_BITE2_ID = "nulljaw_bite2";
    public static final String NULLJAW_CLAW_ID = "nulljaw_claw";
    public static final String NULLJAW_HORN_GORE_ID = "nulljaw_horn_gore";
    public static final String NULLJAW_ROAR_ID = "nulljaw_roar";
    public static final String NULLJAW_PHASE_SHIFT_ID = "nulljaw_phase_shift";
    public static final String NULLJAW_HURT_ID = "nulljaw_hurt";
    public static final String NULLJAW_DIE_ID = "nulljaw_die";

    // Phase 1 melee attack
    public static final DragonAbilityType<Nulljaw, NulljawBiteAbility> NULLJAW_BITE =
            AbilityRegistry.register(new DragonAbilityType<>(NULLJAW_BITE_ID, NulljawBiteAbility::new));

    // Phase 2 rage mode bite - faster
    public static final DragonAbilityType<Nulljaw, NulljawBite2Ability> NULLJAW_BITE2 =
            AbilityRegistry.register(new DragonAbilityType<>(NULLJAW_BITE2_ID, NulljawBite2Ability::new));

    // Phase 2 melee attack
    public static final DragonAbilityType<Nulljaw, NulljawClawAbility> NULLJAW_CLAW =
            AbilityRegistry.register(new DragonAbilityType<>(NULLJAW_CLAW_ID, NulljawClawAbility::new));

    // Horn gore - strong knockback melee (works in both phases)
    public static final DragonAbilityType<Nulljaw, NulljawHornGoreAbility> NULLJAW_HORN_GORE =
            AbilityRegistry.register(new DragonAbilityType<>(NULLJAW_HORN_GORE_ID, NulljawHornGoreAbility::new));

    // Roar - cosmetic ability
    public static final DragonAbilityType<Nulljaw, NulljawRoarAbility> NULLJAW_ROAR =
            AbilityRegistry.register(new DragonAbilityType<>(NULLJAW_ROAR_ID, NulljawRoarAbility::new));

    // Ultimate ability
    public static final DragonAbilityType<Nulljaw, NulljawPhaseShiftAbility> NULLJAW_PHASE_SHIFT =
            AbilityRegistry.register(new DragonAbilityType<>(NULLJAW_PHASE_SHIFT_ID, NulljawPhaseShiftAbility::new));

    // Generic abilities with unique IDs
    public static final DragonAbilityType<Nulljaw, HurtAbility<Nulljaw>> HURT =
            AbilityRegistry.register(new DragonAbilityType<>(NULLJAW_HURT_ID, HurtAbility::new));

    public static final DragonAbilityType<Nulljaw, DieAbility<Nulljaw>> DIE =
            AbilityRegistry.register(new DragonAbilityType<>(NULLJAW_DIE_ID, DieAbility::new));
}
