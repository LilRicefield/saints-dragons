package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AirCombatMovementBehaviour;
import com.leon.saintsdragons.server.ai.goals.base.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.entity.LivingEntity;

public class VolitansAirCombatBehaviour extends AirCombatMovementBehaviour<Volitans> {
    private static final double MELEE_RANGE = 6.0D;
    private static final double BREATH_MIN_RANGE = 10.0D;
    private static final double BREATH_MAX_RANGE = 24.0D;
    private static final double POISON_MAX_RANGE = 32.0D;
    private static final double ROAR_MAX_RANGE = 12.0D;
    private static final double CHASE_HEIGHT_OFFSET = 2.0D;
    private static final double CHASE_SPEED = 2.0D;
    private static final double DIVE_CHASE_SPEED = 3.1D;
    private static final double DIVE_CHASE_MIN_HEIGHT_ADVANTAGE = 7.0D;
    private static final double DIVE_CHASE_MAX_HORIZONTAL_DISTANCE = 42.0D;
    private static final double POSITION_SPEED = 0.85D;
    private static final double BITE_APPROACH_DISTANCE = 3.5D;
    private static final int MELEE_CADENCE_TICKS = 30;

    private int attackCooldown;
    private int breathHoldTicks;
    private int poisonHoldTicks;

    @Override
    protected boolean checkExtraStartConditions(Volitans dragon, LivingEntity target) {
        return canUseAirCombat(dragon);
    }

    @Override
    protected boolean checkExtraContinueConditions(Volitans dragon, LivingEntity target) {
        return canUseAirCombat(dragon);
    }

    @Override
    protected void tickAirCombat(DragonBrainContext<Volitans> context,
                                 LivingEntity target,
                                 boolean hasLineOfSight) {
        Volitans dragon = context.dragon();
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        double distance = dragon.distanceTo(target);
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_POISON_BALL)) {
            setAbilityApproachIntent(context, target, POSITION_SPEED * 0.75D);
            if (--poisonHoldTicks <= 0 || !hasLineOfSight || distance < 8.0D || distance > 36.0D) {
                dragon.requestPoisonBallRelease();
            }
            return;
        }

        if (dragon.isAbilityActive(ModAbilities.VOLITANS_BREATH)) {
            setAbilityApproachIntent(context, target, POSITION_SPEED * 0.65D);
            if (--breathHoldTicks <= 0 || !hasLineOfSight || distance < 7.0D || distance > 26.0D) {
                dragon.forceEndActiveAbility();
            }
            return;
        }

        if (dragon.isAbilityActive(ModAbilities.VOLITANS_ROAR)) {
            setAbilityApproachIntent(context, target, POSITION_SPEED * 0.85D);
            return;
        }

        if (distance <= MELEE_RANGE && hasLineOfSight) {
            if (attackCooldown <= 0 && dragon.getAiCombatPacing().getCadenceCooldownTicks() <= 0) {
                tryMelee(dragon, target);
            }
            setMeleePositionIntent(context, target, 0.0D, BITE_APPROACH_DISTANCE, 1.2D, 0.7D);
            return;
        }

        if (attackCooldown <= 0
                && distance > ROAR_MAX_RANGE
                && distance <= POISON_MAX_RANGE
                && hasLineOfSight
                && canUseAiAbility(dragon, ModAbilities.VOLITANS_POISON_BALL, true)
                && startAiAbility(dragon, ModAbilities.VOLITANS_POISON_BALL, true, 14, 120, 90, 36)) {
            poisonHoldTicks = 20 + dragon.getRandom().nextInt(8);
            setAbilityApproachIntent(context, target, POSITION_SPEED * 0.8D);
            return;
        }

        if (attackCooldown <= 0
                && distance >= BREATH_MIN_RANGE
                && distance <= BREATH_MAX_RANGE
                && hasLineOfSight
                && canUseAiAbility(dragon, ModAbilities.VOLITANS_BREATH, true)) {
            dragon.setBreathMode(dragon.getRandom().nextFloat() < 0.65F ? 1 : 0);
            if (startAiAbility(dragon, ModAbilities.VOLITANS_BREATH, true, 16, 140, 110, 42)) {
                breathHoldTicks = 50 + dragon.getRandom().nextInt(30);
                setAbilityApproachIntent(context, target, POSITION_SPEED * 0.7D);
                return;
            }
        }

        if (attackCooldown <= 0
                && distance <= ROAR_MAX_RANGE
                && hasLineOfSight
                && canUseAiAbility(dragon, ModAbilities.VOLITANS_ROAR, true)
                && startAiAbility(dragon, ModAbilities.VOLITANS_ROAR, true, 12, 140, 120, 48)) {
            setAbilityApproachIntent(context, target, POSITION_SPEED * 0.9D);
            return;
        }

        if (shouldDiveChase(
                dragon,
                target,
                DIVE_CHASE_MIN_HEIGHT_ADVANTAGE,
                DIVE_CHASE_MAX_HORIZONTAL_DISTANCE
        )) {
            setPredictedChaseIntent(context, target, 3.0D, -0.25D, 0.08D, 0.12D, DIVE_CHASE_SPEED);
        } else {
            setPredictedChaseIntent(context, target, 5.0D, CHASE_HEIGHT_OFFSET, 0.12D, 0.5D, CHASE_SPEED);
        }
    }

    @Override
    protected void stopAirCombat(DragonBrainContext<Volitans> context) {
        Volitans dragon = context.dragon();
        dragon.setAiSpecialCombatReserved(false);
        attackCooldown = 0;
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_POISON_BALL)) {
            dragon.requestPoisonBallRelease();
        }
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_BREATH)) {
            dragon.forceEndActiveAbility();
        }
    }

    private void tryMelee(Volitans dragon, LivingEntity target) {
        if (DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)) {
            if (canUseAiAbility(dragon, ModAbilities.VOLITANS_BITE, false)) {
                startAiAbility(
                        dragon,
                        ModAbilities.VOLITANS_BITE,
                        false,
                        MELEE_CADENCE_TICKS,
                        MELEE_CADENCE_TICKS,
                        0,
                        24
                );
            }
            return;
        }

        float roll = dragon.getRandom().nextFloat();
        if (roll < 0.40F && canUseAiAbility(dragon, ModAbilities.VOLITANS_BITE, false)) {
            startMeleeAbility(dragon, ModAbilities.VOLITANS_BITE);
        } else if (roll < 0.72F && canUseAiAbility(dragon, ModAbilities.VOLITANS_CLAW, false)) {
            startMeleeAbility(dragon, ModAbilities.VOLITANS_CLAW);
        } else if (canUseAiAbility(dragon, ModAbilities.VOLITANS_HORN_GORE, false)) {
            startMeleeAbility(dragon, ModAbilities.VOLITANS_HORN_GORE);
        }
    }

    private void startMeleeAbility(Volitans dragon, DragonAbilityType<?, ?> abilityType) {
        startAiAbility(
                dragon,
                abilityType,
                false,
                MELEE_CADENCE_TICKS,
                MELEE_CADENCE_TICKS,
                0,
                24
        );
    }

    private void setAbilityApproachIntent(DragonBrainContext<Volitans> context,
                                          LivingEntity target,
                                          double speed) {
        setPredictedChaseIntent(context, target, 5.0D, CHASE_HEIGHT_OFFSET, 0.12D, 0.5D, speed);
    }

    private boolean canUseAirCombat(Volitans dragon) {
        return !dragon.isBaby()
                && !dragon.isInWater()
                && !dragon.isInWaterOrBubble()
                && !dragon.isInLava()
                && !dragon.isAiSpecialCombatActive()
                && !dragon.isAiSpecialCombatReserved();
    }

    private boolean canUseAiAbility(Volitans dragon,
                                    DragonAbilityType<?, ?> abilityType,
                                    boolean majorAbility) {
        return dragon.combatManager.canStart(abilityType)
                && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
    }

    private boolean startAiAbility(Volitans dragon,
                                   DragonAbilityType<?, ?> abilityType,
                                   boolean majorAbility,
                                   int cadenceTicks,
                                   int abilityCooldownTicks,
                                   int majorCooldownTicks,
                                   int repeatLockoutTicks) {
        boolean started = dragon.combatManager.tryUseAiAbility(
                abilityType,
                majorAbility,
                cadenceTicks,
                abilityCooldownTicks,
                majorCooldownTicks,
                repeatLockoutTicks
        );
        if (started) {
            attackCooldown = Math.max(attackCooldown, cadenceTicks);
        }
        return started;
    }
}
