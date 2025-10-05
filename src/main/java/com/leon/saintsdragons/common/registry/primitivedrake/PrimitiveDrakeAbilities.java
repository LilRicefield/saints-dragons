package com.leon.saintsdragons.common.registry.primitivedrake;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.ability.DieAbility;
import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;

/**
 * Ability registry entries for the Primitive Drake.
 */
public final class PrimitiveDrakeAbilities {
    private PrimitiveDrakeAbilities() {}

    public static final String HURT_ID = "primitive_drake_hurt";
    public static final String DIE_ID = "primitive_drake_die";

    public static final DragonAbilityType<PrimitiveDrakeEntity, HurtAbility<PrimitiveDrakeEntity>> HURT =
            AbilityRegistry.register(new DragonAbilityType<>(HURT_ID, HurtAbility::new));

    public static final DragonAbilityType<PrimitiveDrakeEntity, DieAbility<PrimitiveDrakeEntity>> DIE =
            AbilityRegistry.register(new DragonAbilityType<>(DIE_ID, DieAbility::new));
}
