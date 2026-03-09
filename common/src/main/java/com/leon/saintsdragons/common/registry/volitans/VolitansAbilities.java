package com.leon.saintsdragons.common.registry.volitans;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.ability.DieAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansBurrowAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansClawAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansHornGoreAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansPoisonBallAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansRoarAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansUltimateAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansBreathAbility;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;

public final class VolitansAbilities {
    private VolitansAbilities() {}

    public static final String VOLITANS_BITE_ID = "volitans_bite";
    public static final String VOLITANS_CLAW_ID = "volitans_claw";
    public static final String VOLITANS_HORN_GORE_ID = "volitans_horn_gore";
    public static final String VOLITANS_ROAR_ID = "volitans_roar";
    public static final String VOLITANS_BURROW_ID = "volitans_burrow";
    public static final String VOLITANS_POISON_BALL_ID = "volitans_poison_ball";
    public static final String VOLITANS_BREATH_ID = "volitans_breath";
    // Backward-compatible alias for existing input calls/references.
    public static final String VOLITANS_WATER_BREATH_ID = VOLITANS_BREATH_ID;
    public static final String VOLITANS_ULTIMATE_ID = "volitans_ultimate";
    public static final String VOLITANS_HURT_ID = "volitans_hurt";
    public static final String VOLITANS_DIE_ID = "volitans_die";

    public static final DragonAbilityType<Volitans, VolitansBiteAbility> VOLITANS_BITE =
            AbilityRegistry.register(new DragonAbilityType<>(VOLITANS_BITE_ID, VolitansBiteAbility::new));

    public static final DragonAbilityType<Volitans, VolitansClawAbility> VOLITANS_CLAW =
            AbilityRegistry.register(new DragonAbilityType<>(VOLITANS_CLAW_ID, VolitansClawAbility::new));

    public static final DragonAbilityType<Volitans, VolitansHornGoreAbility> VOLITANS_HORN_GORE =
            AbilityRegistry.register(new DragonAbilityType<>(VOLITANS_HORN_GORE_ID, VolitansHornGoreAbility::new));

    public static final DragonAbilityType<Volitans, VolitansRoarAbility> VOLITANS_ROAR =
            AbilityRegistry.register(new DragonAbilityType<>(VOLITANS_ROAR_ID, VolitansRoarAbility::new));

    public static final DragonAbilityType<Volitans, VolitansBurrowAbility> VOLITANS_BURROW =
            AbilityRegistry.register(new DragonAbilityType<>(VOLITANS_BURROW_ID, VolitansBurrowAbility::new));

    public static final DragonAbilityType<Volitans, VolitansPoisonBallAbility> VOLITANS_POISON_BALL =
            AbilityRegistry.register(new DragonAbilityType<>(VOLITANS_POISON_BALL_ID, VolitansPoisonBallAbility::new));

    public static final DragonAbilityType<Volitans, VolitansBreathAbility> VOLITANS_BREATH =
            AbilityRegistry.register(new DragonAbilityType<>(VOLITANS_BREATH_ID, VolitansBreathAbility::new));
    // Backward-compatible alias.
    public static final DragonAbilityType<Volitans, VolitansBreathAbility> VOLITANS_WATER_BREATH = VOLITANS_BREATH;

    public static final DragonAbilityType<Volitans, VolitansUltimateAbility> VOLITANS_ULTIMATE =
            AbilityRegistry.register(new DragonAbilityType<>(VOLITANS_ULTIMATE_ID, VolitansUltimateAbility::new));

    public static final DragonAbilityType<Volitans, HurtAbility<Volitans>> HURT =
            AbilityRegistry.register(new DragonAbilityType<>(VOLITANS_HURT_ID, HurtAbility::new));

    public static final DragonAbilityType<Volitans, DieAbility<Volitans>> DIE =
            AbilityRegistry.register(new DragonAbilityType<>(VOLITANS_DIE_ID, DieAbility::new));

    public static void init() {
        // Intentionally empty.
    }
}
