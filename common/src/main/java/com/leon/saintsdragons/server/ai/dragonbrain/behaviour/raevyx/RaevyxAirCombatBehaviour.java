package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.RangedAirCombatSettings;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.RangedAirCombatBehaviour;
import com.leon.saintsdragons.server.ai.goals.base.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxBeamAbility;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.LivingEntity;

public class RaevyxAirCombatBehaviour extends RangedAirCombatBehaviour<Raevyx> {
    private static final RangedAirCombatSettings COMBAT_SETTINGS = new RangedAirCombatSettings(
            6.0D,
            7.5D,
            7.0D,
            42.0D,
            7.0D,
            3.5D,
            20.0D,
            70.0D,
            30.0D,
            20,
            60,
            2400
    );

    public RaevyxAirCombatBehaviour() {
        super(COMBAT_SETTINGS);
    }

    @Override
    protected boolean isMeleeAttackActive(Raevyx dragon) {
        return dragon.isAbilityActive(ModAbilities.RAEVYX_BITE);
    }

    @Override
    protected boolean isRangedAttackActive(Raevyx dragon) {
        return dragon.isAbilityActive(ModAbilities.RAEVYX_LIGHTNING_BEAM);
    }

    @Override
    protected boolean canUseRangedAttack(Raevyx dragon, LivingEntity target) {
        return !DragonTargetingHelper.isBiteOnlyPreyTarget(target)
                && !RaevyxBeamAbility.isAtAiBeamMercyThreshold(target);
    }

    @Override
    protected boolean tryStartMeleeAttack(Raevyx dragon, LivingEntity target) {
        return canUseAiAbility(dragon, ModAbilities.RAEVYX_BITE, false)
                && startAiAbility(dragon, ModAbilities.RAEVYX_BITE, false, 20, 20, 0, 18);
    }

    @Override
    protected boolean tryStartRangedAttack(Raevyx dragon, LivingEntity target) {
        return canUseAiAbility(dragon, ModAbilities.RAEVYX_LIGHTNING_BEAM, true)
                && startAiAbility(dragon, ModAbilities.RAEVYX_LIGHTNING_BEAM, true, 60, 2400, 160, 80);
    }

    private boolean canUseAiAbility(Raevyx dragon, DragonAbilityType<?, ?> abilityType, boolean majorAbility) {
        return dragon.combatManager.canStart(abilityType)
                && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
    }

    private boolean startAiAbility(Raevyx dragon,
                                   DragonAbilityType<?, ?> abilityType,
                                   boolean majorAbility,
                                   int cadenceTicks,
                                   int abilityCooldownTicks,
                                   int majorCooldownTicks,
                                   int repeatLockoutTicks) {
        return dragon.combatManager.tryUseAiAbility(
                abilityType,
                majorAbility,
                cadenceTicks,
                abilityCooldownTicks,
                majorCooldownTicks,
                repeatLockoutTicks
        );
    }
}
