package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.RangedAirCombatSettings;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.RangedAirCombatBehaviour;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireBreathAbility;
import net.minecraft.world.entity.LivingEntity;

public class IgnivorusAirCombatBehaviour extends RangedAirCombatBehaviour<Ignivorus> {
    private long nextRangedDecisionTick;
    private static final RangedAirCombatSettings COMBAT_SETTINGS = new RangedAirCombatSettings(
            4.25D,
            6.25D,
            7.0D,
            42.0D,
            16.0D,
            3.5D,
            20.0D,
            64.0D,
            25.0D,
            30,
            12,
            0 // The ability records its shared cooldown when the breath ends.
    );

    public IgnivorusAirCombatBehaviour() {
        super(COMBAT_SETTINGS);
    }

    @Override
    protected boolean checkExtraStartConditions(Ignivorus dragon, LivingEntity target) {
        return canUseAirCombat(dragon);
    }

    @Override
    protected boolean checkExtraContinueConditions(Ignivorus dragon, LivingEntity target) {
        return canUseAirCombat(dragon);
    }

    @Override
    protected boolean isMeleeAttackActive(Ignivorus dragon) {
        return dragon.isAbilityActive(ModAbilities.IGNIVORUS_BITE);
    }

    @Override
    protected boolean isRangedAttackActive(Ignivorus dragon) {
        return dragon.isAbilityActive(ModAbilities.IGNIVORUS_FIRE_BREATH)
                || dragon.isAbilityActive(ModAbilities.IGNIVORUS_FIREBALL);
    }

    @Override
    protected boolean isAdditionalAttackActive(Ignivorus dragon) {
        return dragon.isLeaping()
                || dragon.isLeapImpactRecovering()
                || dragon.isAbilityActive(ModAbilities.IGNIVORUS_ULTIMATE);
    }

    @Override
    protected boolean tryStartPriorityAttack(Ignivorus dragon,
                                             LivingEntity target,
                                             boolean hasLineOfSight) {
        if (!hasLineOfSight
                || DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)
                || !dragon.shouldTriggerWildUltimateAtCurrentHealth()
                || !canUseAiAbility(dragon, ModAbilities.IGNIVORUS_ULTIMATE, true)
                || !dragon.combatManager.tryUseAbility(ModAbilities.IGNIVORUS_ULTIMATE)) {
            return false;
        }
        dragon.getAiCombatPacing().recordUse(
                ModAbilities.IGNIVORUS_ULTIMATE,
                20,
                140,
                true,
                180,
                100
        );
        return true;
    }

    @Override
    protected boolean canUseRangedAttack(Ignivorus dragon, LivingEntity target) {
        return !DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)
                && (isRangedAttackActive(dragon)
                || canUseAiAbility(dragon, ModAbilities.IGNIVORUS_FIRE_BREATH, true)
                || (dragon.isPhase2Active() && canUseAiAbility(dragon, ModAbilities.IGNIVORUS_FIREBALL, true)));
    }

    @Override
    protected boolean tryStartMeleeAttack(Ignivorus dragon, LivingEntity target) {
        if (!canUseAiAbility(dragon, ModAbilities.IGNIVORUS_BITE, false)
                || !dragon.combatManager.tryUseAbility(ModAbilities.IGNIVORUS_BITE)) {
            return false;
        }
        dragon.getAiCombatPacing().recordUse(ModAbilities.IGNIVORUS_BITE, 30, 30, false, 0, 24);
        return true;
    }

    @Override
    protected boolean tryStartRangedAttack(Ignivorus dragon, LivingEntity target) {
        if (dragon.level().getGameTime() < nextRangedDecisionTick || !canUseRangedAttack(dragon, target)) {
            return false;
        }
        nextRangedDecisionTick = dragon.level().getGameTime() + 12;
        if (dragon.getRandom().nextFloat() >= 0.65F) return false;
        boolean breathReady = canUseAiAbility(dragon, ModAbilities.IGNIVORUS_FIRE_BREATH, true)
                && IgnivorusFireBreathAbility.canStartAiBreath(dragon, target);
        boolean fireballReady = dragon.isPhase2Active()
                && canUseAiAbility(dragon, ModAbilities.IGNIVORUS_FIREBALL, true)
                && dragon.hasAiFireBreathShot(target, 64.0D);
        if (fireballReady && (!breathReady || dragon.getRandom().nextFloat() < 0.4F)) {
            return dragon.combatManager.tryUseAiAbility(ModAbilities.IGNIVORUS_FIREBALL,
                    true, 12, 400, 60, 80);
        }
        if (!breathReady
                || !dragon.combatManager.tryUseAbility(ModAbilities.IGNIVORUS_FIRE_BREATH)) {
            return false;
        }
        dragon.getAiCombatPacing().recordUse(
                ModAbilities.IGNIVORUS_FIRE_BREATH,
                12,
                0,
                true,
                30,
                0
        );
        return true;
    }

    private boolean canUseAirCombat(Ignivorus dragon) {
        return dragon.getAiAirCombatBlockReason() == null;
    }

    private boolean canUseAiAbility(Ignivorus dragon,
                                    com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> abilityType,
                                    boolean majorAbility) {
        return !dragon.isTakeoff() && !dragon.isLanding()
                && dragon.combatManager.canStart(abilityType)
                && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
    }
}
