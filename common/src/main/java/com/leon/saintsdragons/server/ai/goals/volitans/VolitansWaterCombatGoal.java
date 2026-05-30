package com.leon.saintsdragons.server.ai.goals.volitans;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.goals.base.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class VolitansWaterCombatGoal extends Goal {
    private static final int ROAR_COOLDOWN_TICKS = 200;
    private static final double BITE_RANGE = 4.1D;
    private static final double CLAW_RANGE = 5.1D;
    private static final double GORE_RANGE = 6.2D;
    private static final double BREATH_MIN_RANGE = 6.0D;
    private static final double BREATH_MAX_RANGE = 16.0D;
    private static final double ROAR_MIN_RANGE = 4.5D;
    private static final double ROAR_MAX_RANGE = 12.0D;
    private static final int MELEE_CADENCE_TICKS = 30;

    private final Volitans dragon;
    private int attackCooldown = 0;
    private int breathHoldTicks = 0;

    public VolitansWaterCombatGoal(Volitans dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (dragon.isAiSpecialCombatActive() || dragon.isAiSpecialCombatReserved()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        if (!canFightTarget(target)) {
            return false;
        }
        return dragon.isInWaterOrBubble() || dragon.isUnderWater();
    }

    @Override
    public boolean canContinueToUse() {
        if (dragon.isAiSpecialCombatActive() || dragon.isAiSpecialCombatReserved()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        if (dragon.isGroundCombatAbilityActive()) {
            return true;
        }
        if (!canFightTarget(target)) {
            return false;
        }
        return dragon.isInWaterOrBubble() || dragon.isUnderWater();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        dragon.setAggressive(true);
    }

    @Override
    public void stop() {
        dragon.setAggressive(false);
        attackCooldown = 0;
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_BREATH)) {
            dragon.forceEndActiveAbility();
        }
    }

    @Override
    public void tick() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        LivingEntity target = dragon.getTarget();
        if (!dragon.isTargetValid(target)) {
            return;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double gap = getGapToTarget(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);

        if (handleActiveAbility(gap, hasLineOfSight)) {
            return;
        }

        if (attackCooldown > 0 || dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0 || dragon.isGroundMobilityActive()) {
            return;
        }

        if (gap <= GORE_RANGE) {
            tryMelee(target, gap);
            return;
        }

        if (!hasLineOfSight) {
            return;
        }

        if (tryRoar(gap)) {
            return;
        }

        tryBreath(gap);
    }

    private boolean canFightTarget(LivingEntity target) {
        if (!dragon.isTargetValid(target)) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        if (dragon.isVehicle() || dragon.isOrderedToSit() || dragon.isBaby()) {
            return false;
        }
        if (!(dragon.isInWaterOrBubble() || dragon.isUnderWater())) {
            return false;
        }
        return dragon.distanceToSqr(target) <= getMaxAggroDistanceSqr();
    }

    private boolean handleActiveAbility(double gap, boolean hasLineOfSight) {
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_BREATH)) {
            if (--breathHoldTicks <= 0 || !hasLineOfSight || gap < 4.5D || gap > 18.0D) {
                dragon.forceEndActiveAbility();
            }
            return true;
        }
        return dragon.isGroundCombatAbilityActive();
    }

    private boolean tryRoar(double gap) {
        if (gap < ROAR_MIN_RANGE || gap > ROAR_MAX_RANGE) {
            return false;
        }
        if (!canUseAiAbility(ModAbilities.VOLITANS_ROAR, true)) {
            return false;
        }
        return startAiAbility(ModAbilities.VOLITANS_ROAR, true, 24, ROAR_COOLDOWN_TICKS, 120, 48);
    }

    private boolean tryBreath(double gap) {
        if (gap < BREATH_MIN_RANGE || gap > BREATH_MAX_RANGE) {
            return false;
        }
        dragon.setBreathMode(0);
        if (!canUseAiAbility(ModAbilities.VOLITANS_BREATH, true)) {
            return false;
        }
        if (!startAiAbility(ModAbilities.VOLITANS_BREATH, true, 16, 140, 110, 42)) {
            return false;
        }
        breathHoldTicks = 50 + dragon.getRandom().nextInt(20);
        return true;
    }

    private void tryMelee(LivingEntity target, double gap) {
        if (gap <= BITE_RANGE) {
            if (DragonTargetingHelper.isBiteOnlyPreyTarget(target)) {
                if (canUseAiAbility(ModAbilities.VOLITANS_BITE, false)) {
                    startAiAbility(ModAbilities.VOLITANS_BITE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
                }
                return;
            }
            float roll = dragon.getRandom().nextFloat();
            if (roll < 0.42F) {
                if (canUseAiAbility(ModAbilities.VOLITANS_CLAW, false)) {
                    startAiAbility(ModAbilities.VOLITANS_CLAW, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
                }
            } else if (roll < 0.72F) {
                if (canUseAiAbility(ModAbilities.VOLITANS_BITE, false)) {
                    startAiAbility(ModAbilities.VOLITANS_BITE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
                }
            } else if (canUseAiAbility(ModAbilities.VOLITANS_HORN_GORE, false)) {
                startAiAbility(ModAbilities.VOLITANS_HORN_GORE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
            }
            return;
        }

        if (gap <= CLAW_RANGE) {
            if (dragon.getRandom().nextFloat() < 0.58F) {
                if (canUseAiAbility(ModAbilities.VOLITANS_CLAW, false)) {
                    startAiAbility(ModAbilities.VOLITANS_CLAW, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
                }
            } else if (canUseAiAbility(ModAbilities.VOLITANS_HORN_GORE, false)) {
                startAiAbility(ModAbilities.VOLITANS_HORN_GORE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
            }
            return;
        }

        if (gap <= GORE_RANGE && canUseAiAbility(ModAbilities.VOLITANS_HORN_GORE, false)) {
            startAiAbility(ModAbilities.VOLITANS_HORN_GORE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
        }
    }

    private boolean canUseAiAbility(com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> abilityType, boolean majorAbility) {
        return dragon.combatManager.canStart(abilityType) && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
    }

    private boolean startAiAbility(com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> abilityType,
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

    private double getGapToTarget(LivingEntity target) {
        double centerDistance = dragon.distanceTo(target);
        double combinedRadii = (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        return Math.max(0.0D, centerDistance - combinedRadii);
    }

    private double getMaxAggroDistanceSqr() {
        double followRange = dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 32.0D;
        }
        return followRange * followRange;
    }
}
