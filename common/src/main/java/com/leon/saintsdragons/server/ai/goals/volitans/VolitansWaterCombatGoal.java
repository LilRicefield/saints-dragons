package com.leon.saintsdragons.server.ai.goals.volitans;

import com.leon.saintsdragons.common.registry.volitans.VolitansAbilities;
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

    private final Volitans dragon;
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
        if (dragon.isAbilityActive(VolitansAbilities.VOLITANS_BREATH)) {
            dragon.forceEndActiveAbility();
        }
    }

    @Override
    public void tick() {
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

        if (!hasLineOfSight) {
            return;
        }

        if (dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0 || dragon.isGroundMobilityActive()) {
            return;
        }

        if (gap <= GORE_RANGE) {
            tryMelee(gap);
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
        if (dragon.isAbilityActive(VolitansAbilities.VOLITANS_BREATH)) {
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
        if (!canUseAiAbility(VolitansAbilities.VOLITANS_ROAR, true)) {
            return false;
        }
        dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_ROAR);
        dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_ROAR, 24, ROAR_COOLDOWN_TICKS, true, 120, 48);
        return true;
    }

    private boolean tryBreath(double gap) {
        if (gap < BREATH_MIN_RANGE || gap > BREATH_MAX_RANGE) {
            return false;
        }
        dragon.setBreathMode(0);
        if (!canUseAiAbility(VolitansAbilities.VOLITANS_BREATH, true)) {
            return false;
        }
        dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_BREATH);
        breathHoldTicks = 50 + dragon.getRandom().nextInt(20);
        dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_BREATH, 16, 140, true, 110, 42);
        return true;
    }

    private void tryMelee(double gap) {
        if (gap <= BITE_RANGE) {
            float roll = dragon.getRandom().nextFloat();
            if (roll < 0.42F) {
                if (canUseAiAbility(VolitansAbilities.VOLITANS_CLAW, false)) {
                    dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_CLAW);
                    dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_CLAW, 14, 18, false, 0, 20);
                }
            } else if (roll < 0.72F) {
                if (canUseAiAbility(VolitansAbilities.VOLITANS_BITE, false)) {
                    dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_BITE);
                    dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_BITE, 12, 16, false, 0, 18);
                }
            } else if (canUseAiAbility(VolitansAbilities.VOLITANS_HORN_GORE, false)) {
                dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_HORN_GORE);
                dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_HORN_GORE, 16, 22, false, 0, 24);
            }
            return;
        }

        if (gap <= CLAW_RANGE) {
            if (dragon.getRandom().nextFloat() < 0.58F) {
                if (canUseAiAbility(VolitansAbilities.VOLITANS_CLAW, false)) {
                    dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_CLAW);
                    dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_CLAW, 14, 18, false, 0, 20);
                }
            } else if (canUseAiAbility(VolitansAbilities.VOLITANS_HORN_GORE, false)) {
                dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_HORN_GORE);
                dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_HORN_GORE, 16, 22, false, 0, 24);
            }
            return;
        }

        if (gap <= GORE_RANGE && canUseAiAbility(VolitansAbilities.VOLITANS_HORN_GORE, false)) {
            dragon.combatManager.tryUseAbility(VolitansAbilities.VOLITANS_HORN_GORE);
            dragon.getAiCombatPacing().recordUse(VolitansAbilities.VOLITANS_HORN_GORE, 16, 22, false, 0, 24);
        }
    }

    private boolean canUseAiAbility(com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> abilityType, boolean majorAbility) {
        return dragon.combatManager.canStart(abilityType) && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
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
