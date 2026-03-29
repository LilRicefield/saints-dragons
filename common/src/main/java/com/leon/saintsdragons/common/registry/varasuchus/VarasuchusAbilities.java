package com.leon.saintsdragons.common.registry.varasuchus;

import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusPhaseShiftAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusBite2Ability;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusClawAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusRoarAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusHornGoreAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusSlashBarrageAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusTailAttackAbility;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.ability.DieAbility;

public final class VarasuchusAbilities {
    private VarasuchusAbilities() {}

    // Ability ID constants
    public static final String VARASUCHUS_BITE_ID = "varasuchus_bite";
    public static final String VARASUCHUS_BITE2_ID = "varasuchus_bite2";
    public static final String VARASUCHUS_CLAW_ID = "varasuchus_claw";
    public static final String VARASUCHUS_HORN_GORE_ID = "varasuchus_horn_gore";
    public static final String VARASUCHUS_TAIL_ATTACK_ID = "varasuchus_tail_attack";
    public static final String VARASUCHUS_ROAR_ID = "varasuchus_roar";
    public static final String VARASUCHUS_SLASH_BARRAGE_ID = "varasuchus_slash_barrage";
    public static final String VARASUCHUS_PHASE_SHIFT_ID = "varasuchus_phase_shift";
    public static final String VARASUCHUS_HURT_ID = "varasuchus_hurt";
    public static final String VARASUCHUS_DIE_ID = "varasuchus_die";

    // Phase 1 melee attack
    public static final DragonAbilityType<Varasuchus, VarasuchusBiteAbility> VARASUCHUS_BITE =
            AbilityRegistry.register(new DragonAbilityType<>(VARASUCHUS_BITE_ID, VarasuchusBiteAbility::new));

    // Phase 2 rage mode bite - faster
    public static final DragonAbilityType<Varasuchus, VarasuchusBite2Ability> VARASUCHUS_BITE2 =
            AbilityRegistry.register(new DragonAbilityType<>(VARASUCHUS_BITE2_ID, VarasuchusBite2Ability::new));

    // Phase 2 melee attack
    public static final DragonAbilityType<Varasuchus, VarasuchusClawAbility> VARASUCHUS_CLAW =
            AbilityRegistry.register(new DragonAbilityType<>(VARASUCHUS_CLAW_ID, VarasuchusClawAbility::new));

    // Horn gore - strong knockback melee (works in both phases)
    public static final DragonAbilityType<Varasuchus, VarasuchusHornGoreAbility> VARASUCHUS_HORN_GORE =
            AbilityRegistry.register(new DragonAbilityType<>(VARASUCHUS_HORN_GORE_ID, VarasuchusHornGoreAbility::new));

    // Phase 1 tail attack - standing tail swipe
    public static final DragonAbilityType<Varasuchus, VarasuchusTailAttackAbility> VARASUCHUS_TAIL_ATTACK =
            AbilityRegistry.register(new DragonAbilityType<>(VARASUCHUS_TAIL_ATTACK_ID, VarasuchusTailAttackAbility::new));

    // Roar - cosmetic ability
    public static final DragonAbilityType<Varasuchus, VarasuchusRoarAbility> VARASUCHUS_ROAR =
            AbilityRegistry.register(new DragonAbilityType<>(VARASUCHUS_ROAR_ID, VarasuchusRoarAbility::new));

    // Phase 2 slash barrage - multi-hit claw flurry
    public static final DragonAbilityType<Varasuchus, VarasuchusSlashBarrageAbility> VARASUCHUS_SLASH_BARRAGE =
            AbilityRegistry.register(new DragonAbilityType<>(VARASUCHUS_SLASH_BARRAGE_ID, VarasuchusSlashBarrageAbility::new));

    // Ultimate ability
    public static final DragonAbilityType<Varasuchus, VarasuchusPhaseShiftAbility> VARASUCHUS_PHASE_SHIFT =
            AbilityRegistry.register(new DragonAbilityType<>(VARASUCHUS_PHASE_SHIFT_ID, VarasuchusPhaseShiftAbility::new));

    // Generic abilities with unique IDs
    public static final DragonAbilityType<Varasuchus, HurtAbility<Varasuchus>> HURT =
            AbilityRegistry.register(new DragonAbilityType<>(VARASUCHUS_HURT_ID, HurtAbility::new));

    public static final DragonAbilityType<Varasuchus, DieAbility<Varasuchus>> DIE =
            AbilityRegistry.register(new DragonAbilityType<>(VARASUCHUS_DIE_ID, DieAbility::new));

    /**
     * No-op hook to trigger class loading for static registration.
     */
    public static void init() {
        // Intentionally empty
    }
}
